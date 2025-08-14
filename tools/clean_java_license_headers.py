#!/usr/bin/env python3
"""
Clean leading license headers from Java files.

By default, removes only the very first comment block (/* ... */) or contiguous //-style
comments at the top of a .java file if they contain any of the known license keywords
(e.g., MicroEmulator, GNU Lesser General Public License, Apache License, Copyright).

Safety features:
- Dry run mode (default): shows planned changes without modifying files.
- Backup files on write unless --no-backup is provided.
- Skips common build/output folders.

Usage examples:
  # Show what would change repo-wide
  python3 tools/clean_java_license_headers.py --root . --dry-run --verbose

  # Actually modify files, with backups (*.bak)
  python3 tools/clean_java_license_headers.py --root . --in-place

  # Only a specific file
  python3 tools/clean_java_license_headers.py --paths je-javase-swing/src/main/java/org/je/applet/CookieRecordStoreManager.java --in-place

  # Remove any leading comment regardless of keywords (use with care)
  python3 tools/clean_java_license_headers.py --root . --in-place --aggressive
"""
from __future__ import annotations

import argparse
import difflib
import io
import os
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, List, Optional, Sequence, Tuple


DEFAULT_SKIP_DIRS = {
    '.git', '.gradle', '.idea', '.vscode', 'build', 'out', 'target', 'node_modules', 'tmp', '.svn'
}

DEFAULT_EXTENSIONS = {'.java'}

DEFAULT_KEYWORDS = [
    'microemulator',
    'gnu lesser general public license',
    'lgpl',
    'apache license',
    'copyright',
]

BLOCK_HEADER_RE = re.compile(r"^\ufeff?\s*/\*.*?\*/\s*", re.DOTALL)
LINE_HEADER_RE = re.compile(r"^\ufeff?\s*(?://[^\n]*\n)+\s*", re.DOTALL)


@dataclass
class Change:
    path: Path
    changed: bool
    reason: str = ''
    before: Optional[str] = None
    after: Optional[str] = None


def read_text(path: Path) -> Optional[str]:
    try:
        return path.read_text(encoding='utf-8')
    except UnicodeDecodeError:
        # Try latin-1 as a fallback; if still fails, skip file.
        try:
            return path.read_text(encoding='latin-1')
        except Exception:
            return None
    except Exception:
        return None


def write_text(path: Path, content: str, backup: bool, backup_ext: str) -> None:
    if backup:
        bak = path.with_suffix(path.suffix + backup_ext)
        try:
            if bak.exists():
                bak.unlink()
        except Exception:
            pass
        try:
            path.replace(bak)
        except Exception:
            # If replace fails (cross-device), copy contents instead
            data = read_text(path)
            if data is not None:
                bak.write_text(data, encoding='utf-8')
    # Ensure parent exists
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding='utf-8')


def _strip_first_comment_if_matches(src: str, keywords: Sequence[str], aggressive: bool) -> Tuple[str, bool, str]:
    """Remove only the very first comment block or contiguous // header if it matches.

    Returns: (new_src, did_change, reason)
    """
    text = src
    # Normalize newlines for detection (preserve original on write)
    # But here we just analyze; we will write using the transformed content directly.

    m_block = BLOCK_HEADER_RE.match(text)
    m_line = LINE_HEADER_RE.match(text)
    match = None
    kind = ''
    if m_block and (not m_line or m_block.end() >= (m_line.end())):
        match = m_block
        kind = 'block'
    elif m_line:
        match = m_line
        kind = 'line'

    if not match:
        return src, False, 'no-leading-comment'

    comment_text = match.group(0)
    check_text = comment_text.lower()

    # Only remove if keyword detected, unless aggressive.
    if not aggressive:
        if not any(kw in check_text for kw in keywords):
            return src, False, 'no-keyword'

    # Remove the matched header and any leading blank lines that follow.
    remainder = text[match.end():]
    # Trim leading blank lines to get to package/import
    remainder = re.sub(r"^(?:\s*\n)+", "", remainder, count=1)

    return remainder, True, f'removed-{kind}-comment'


def find_files(root: Path, paths: Sequence[Path], extensions: Sequence[str], skip_dirs: Sequence[str]) -> Iterable[Path]:
    exts = {e.lower() if e.startswith('.') else f'.{e.lower()}' for e in extensions}
    skip = set(skip_dirs)

    if paths:
        for p in paths:
            p = p.resolve()
            if p.is_file() and p.suffix.lower() in exts:
                yield p
            elif p.is_dir():
                yield from _walk_dir(p, exts, skip)
    else:
        yield from _walk_dir(root.resolve(), exts, skip)


def _walk_dir(root: Path, exts: set[str], skip: set[str]) -> Iterable[Path]:
    for dirpath, dirnames, filenames in os.walk(root):
        # Prune directories in-place
        dirnames[:] = [d for d in dirnames if d not in skip and not d.startswith('.') or d in {'.git'}]
        for fname in filenames:
            p = Path(dirpath) / fname
            if p.suffix.lower() in exts:
                yield p


def make_diff(a: str, b: str, path: Path) -> str:
    a_lines = a.splitlines(keepends=True)
    b_lines = b.splitlines(keepends=True)
    diff = difflib.unified_diff(a_lines, b_lines, fromfile=str(path), tofile=str(path), lineterm='')
    return ''.join(diff)


def process_file(path: Path, keywords: Sequence[str], aggressive: bool, show_diff: bool, in_place: bool, backup: bool, backup_ext: str, verbose: bool) -> Change:
    src = read_text(path)
    if src is None:
        return Change(path, False, 'unreadable')

    new_src, changed, reason = _strip_first_comment_if_matches(src, keywords, aggressive)

    if changed and in_place:
        write_text(path, new_src, backup=backup, backup_ext=backup_ext)

    before, after = (src, new_src) if (changed and show_diff) else (None, None)
    if verbose and not changed:
        # Provide a hint if file has no leading comment at all.
        if reason == 'no-leading-comment':
            hint = 'no-leading-comment'
        elif reason == 'no-keyword':
            hint = 'leading-comment-without-keyword'
        else:
            hint = reason
        return Change(path, False, hint)

    return Change(path, changed, reason, before, after)


def parse_args(argv: Optional[Sequence[str]] = None) -> argparse.Namespace:
    p = argparse.ArgumentParser(description='Remove license headers from Java files safely.')
    p.add_argument('--root', type=str, default='.', help='Root directory to scan (default: .)')
    p.add_argument('--paths', nargs='*', default=None, help='Specific files or directories to process')
    p.add_argument('--ext', nargs='*', default=list(DEFAULT_EXTENSIONS), help='File extensions to include (default: .java)')
    p.add_argument('--skip-dirs', nargs='*', default=list(DEFAULT_SKIP_DIRS), help='Directories to skip by name')
    p.add_argument('--keywords', nargs='*', default=DEFAULT_KEYWORDS, help='Keywords that must appear in the leading comment to remove it (case-insensitive)')
    p.add_argument('--aggressive', action='store_true', help='Remove any leading comment regardless of keywords (use with care)')
    p.add_argument('--in-place', action='store_true', help='Write changes to files (default is dry-run)')
    p.add_argument('--no-backup', action='store_true', help='Do not create .bak backups when writing')
    p.add_argument('--backup-ext', type=str, default='.bak', help='Backup extension to use (default: .bak)')
    p.add_argument('--show-diff', action='store_true', help='Show unified diff for changes')
    p.add_argument('--verbose', action='store_true', help='Verbose logging for unchanged files')
    return p.parse_args(argv)


def main(argv: Optional[Sequence[str]] = None) -> int:
    args = parse_args(argv)

    root = Path(args.root)
    explicit_paths = [Path(p) for p in (args.paths or [])]

    changed_count = 0
    total_count = 0

    for path in find_files(root, explicit_paths, args.ext, args.skip_dirs):
        total_count += 1
        ch = process_file(
            path=path,
            keywords=[k.lower() for k in args.keywords],
            aggressive=args.aggressive,
            show_diff=args.show_diff,
            in_place=args.in_place,
            backup=(not args.no_backup),
            backup_ext=args.backup_ext,
            verbose=args.verbose,
        )
        if ch.changed:
            changed_count += 1
            action = 'MODIFY' if args.in_place else 'WOULD MODIFY'
            print(f"{action}: {ch.path} ({ch.reason})")
            if args.show_diff and ch.before is not None and ch.after is not None:
                print(make_diff(ch.before, ch.after, ch.path))
        else:
            if args.verbose:
                print(f"SKIP: {ch.path} ({ch.reason})")

    mode = 'wrote changes' if args.in_place else 'dry run only'
    print(f"\nDone: {mode}. {changed_count}/{total_count} files affected.")

    return 0


if __name__ == '__main__':
    raise SystemExit(main())
