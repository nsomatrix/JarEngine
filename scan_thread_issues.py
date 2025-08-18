import os
import re

# Define patterns that could cause thread blocking or freezes
patterns = [
    (r'System\.exit', 'System exit call detected'),
    (r'Thread\.sleep\(\d+\)', 'Fixed-duration Thread.sleep detected'),
    (r'Thread\.sleep\(Math\.max\(0, [^)]+\)\)', 'Latency-simulated Thread.sleep detected'),
]

def scan_file(file_path):
    """Scan a single file for potential thread-blocking issues."""
    issues = []
    with open(file_path, 'r', encoding='utf-8') as file:
        lines = file.readlines()
        for line_number, line in enumerate(lines, start=1):
            for pattern, description in patterns:
                if re.search(pattern, line):
                    issues.append((file_path, line_number, line.strip(), description))
    return issues

def scan_directory(directory):
    """Recursively scan a directory for Java files and check for issues."""
    all_issues = []
    for root, _, files in os.walk(directory):
        for file in files:
            if file.endswith('.java'):
                file_path = os.path.join(root, file)
                issues = scan_file(file_path)
                all_issues.extend(issues)
    return all_issues

def main():
    """Main function to scan the codebase and report issues."""
    directory = '/home/mackruize/JarEngine/je-javase/src/main/java'
    issues = scan_directory(directory)
    
    if issues:
        print("Potential thread-blocking issues found:")
        for file_path, line_number, line, description in issues:
            print(f"{file_path}:{line_number} - {description}")
            print(f"  {line}\n")
    else:
        print("No potential thread-blocking issues found.")

if __name__ == "__main__":
    main()