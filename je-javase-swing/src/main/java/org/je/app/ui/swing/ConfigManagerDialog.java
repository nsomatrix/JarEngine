package org.je.app.ui.swing;

import org.je.app.Config;
import org.je.log.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import org.je.app.util.MidletURLReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import org.je.util.JadProperties;
import org.je.util.JadMidletEntry;

/**
 * Config Manager UI: manage installed MIDlet suites (RMS folders) and export/import emulator configuration.
 */
public class ConfigManagerDialog extends SwingDialogPanel {

    private final DefaultListModel<String> suitesModel = new DefaultListModel<>();
    private final JList<String> suitesList = new JList<>(suitesModel);
    private final JButton uninstallBtn = new JButton("Uninstall Selected");
    private final JButton refreshBtn = new JButton("Refresh");
    private final JButton openFolderBtn = new JButton("Open Config Folder");
    private final Map<String, ImageIcon> iconCache = new ConcurrentHashMap<String, ImageIcon>() {
        private static final int MAX_CACHE_SIZE = 100;
        
        @Override
        public ImageIcon put(String key, ImageIcon value) {
            if (size() >= MAX_CACHE_SIZE) {
                // Remove oldest entries when cache is full
                Iterator<String> iterator = keySet().iterator();
                for (int i = 0; i < 10 && iterator.hasNext(); i++) {
                    iterator.next();
                    iterator.remove();
                }
            }
            return super.put(key, value);
        }
    };
    private final ImageIcon placeholderIcon = loadPlaceholderIcon();
    private volatile boolean iconsLoading = false;

    // Dynamic export/import selection
    private final JPanel exportSelectionPanel = new JPanel();
    private final java.util.List<ItemGroup> exportGroups = new ArrayList<>();
    private final JButton selectAllBtn = new JButton("Select All");
    private final JButton selectNoneBtn = new JButton("Select None");
    private final JButton refreshItemsBtn = new JButton("Refresh List");

    public ConfigManagerDialog() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Installed Apps", buildInstalledAppsPanel());
        tabs.addTab("Export / Import", buildExportImportPanel());
        add(tabs, BorderLayout.CENTER);

        // Initial load
        SwingUtilities.invokeLater(this::loadSuites);
    }

    private JPanel buildInstalledAppsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
    suitesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        suitesList.setVisibleRowCount(10);
    suitesList.setCellRenderer(new SuiteCellRenderer());
    panel.add(new JScrollPane(suitesList), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        uninstallBtn.addActionListener(e -> uninstallSelectedSuites());
        refreshBtn.addActionListener(e -> loadSuites());
        openFolderBtn.addActionListener(e -> openConfigFolder());
        buttons.add(openFolderBtn);
        buttons.add(refreshBtn);
        buttons.add(uninstallBtn);
        panel.add(buttons, BorderLayout.SOUTH);
        panel.add(new JLabel("Select one or more suites to uninstall (removes RMS data under ~/.je/suite-<name>)."), BorderLayout.NORTH);
        return panel;
    }

    private JPanel buildExportImportPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.WEST;
    panel.add(new JLabel("Select what to include when exporting or importing configuration items:"), c);

    // Selection area
    c.gridy++;
    c.fill = GridBagConstraints.BOTH; // Allow both horizontal and vertical expansion
    c.weightx = 1.0; // Allow horizontal expansion
    c.weighty = 1.0; // Allow vertical expansion
    exportSelectionPanel.setLayout(new BoxLayout(exportSelectionPanel, BoxLayout.Y_AXIS));
    JScrollPane scroll = new JScrollPane(exportSelectionPanel,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    // Set stable sizing constraints to prevent layout issues when content changes
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    // Use industry standard responsive sizing ratios
    int maxWidth = Math.min(screenSize.width * 40 / 100, screenSize.width / 3);  // 40% or 1/3 of screen
    int maxHeight = Math.min(screenSize.height * 25 / 100, screenSize.height / 3); // 25% or 1/3 of screen
    int minWidth = Math.max(250, screenSize.width / 8);  // Minimum 250px or 1/8 screen width
    int minHeight = Math.max(120, maxHeight / 3);        // Minimum 120px or 1/3 of max height
    
    // Set consistent sizing to prevent shrinking when content changes
    scroll.setPreferredSize(new Dimension(maxWidth, maxHeight));
    scroll.setMinimumSize(new Dimension(minWidth, minHeight));
    scroll.setMaximumSize(new Dimension(maxWidth, maxHeight));
    panel.add(scroll, c);

    // Selection controls
    c.gridy++;
    c.fill = GridBagConstraints.NONE; // Reset fill for buttons
    c.weightx = 0.0; // Reset weight for buttons
    c.weighty = 0.0; // Reset weight for buttons
    c.anchor = GridBagConstraints.WEST; // Align to the left
    JPanel selButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    selButtons.add(selectAllBtn);
    selButtons.add(selectNoneBtn);
    selButtons.add(refreshItemsBtn);
    panel.add(selButtons, c);

        JButton exportBtn = new JButton("Export Configuration");
        JButton importBtn = new JButton("Import Configuration");
        c.gridy++;
        panel.add(exportBtn, c);
        c.gridy++;
        panel.add(importBtn, c);

    // Wire selection controls
    selectAllBtn.addActionListener(e -> setAllExportSelections(true));
    selectNoneBtn.addActionListener(e -> setAllExportSelections(false));
    refreshItemsBtn.addActionListener(e -> refreshExportSelection());

    // Initial list
    SwingUtilities.invokeLater(this::refreshExportSelection);

    exportBtn.addActionListener(e -> doExportSelected());
    importBtn.addActionListener(e -> doImportSelective());

        return panel;
    }

    private void loadSuites() {
        suitesModel.clear();
        File base = Config.getConfigPath();
        if (base == null) return;
        File[] children = base.listFiles((dir, name) -> name != null && name.startsWith("suite-") && new File(dir, name).isDirectory());
        if (children == null || children.length == 0) {
            suitesModel.addElement("<no suites found>");
            suitesList.setEnabled(false);
            uninstallBtn.setEnabled(false);
            return;
        }
        Arrays.sort(children, Comparator.comparing(File::getName));
        for (File f : children) {
            String name = f.getName();
            suitesModel.addElement(name.substring("suite-".length()));
        }
        suitesList.setEnabled(true);
        uninstallBtn.setEnabled(true);
    loadIconsAsync();
    }

    private void uninstallSelectedSuites() {
        List<String> selected = suitesList.getSelectedValuesList();
        if (selected == null || selected.isEmpty()) return;
        int res = JOptionPane.showConfirmDialog(this,
                "Uninstall selected suites? This deletes RMS data under the config folder.\nThis cannot be undone.",
                "Confirm Uninstall", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        File base = Config.getConfigPath();
        int ok = 0, fail = 0;
        for (String suite : selected) {
            File dir = new File(base, "suite-" + suite);
            try {
                deleteRecursive(dir.toPath());
                ok++;
            } catch (IOException ex) {
                Logger.error("Failed to delete suite " + suite, ex);
                fail++;
            }
        }
        loadSuites();
        JOptionPane.showMessageDialog(this,
                String.format("Uninstall complete. Deleted: %d, Failed: %d", ok, fail),
                "Uninstall", fail == 0 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
    }

    private void loadIconsAsync() {
        if (iconsLoading) return;
        iconsLoading = true;
        new Thread(() -> {
            try {
                // Map suite name to most recent local URL (from MRU)
                Map<String, URL> nameToUrl = new HashMap<>();
                java.util.List mru = org.je.app.Config.getUrlsMRU().getItemsSnapshot();
                if (mru != null) {
                    for (Object o : mru) {
                        if (o instanceof MidletURLReference) {
                            MidletURLReference ref = (MidletURLReference) o;
                            try {
                                URL u = new URL(ref.getUrl());
                                if ("file".equalsIgnoreCase(u.getProtocol())) {
                                    nameToUrl.putIfAbsent(ref.getName(), u);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }

                // Snapshot suite names on EDT to avoid concurrent access to DefaultListModel
                final java.util.List<String> suites = new ArrayList<>();
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        for (int i = 0; i < suitesModel.size(); i++) {
                            String s = suitesModel.get(i);
                            if (s != null && !s.startsWith("<")) suites.add(s);
                        }
                    });
                } catch (Exception ignored) {}

                for (int i = 0; i < suites.size(); i++) {
                    String suite = suites.get(i);
                    if (iconCache.containsKey(suite)) continue;
                    ImageIcon icon = resolveIconForSuite(suite, nameToUrl.get(suite));
                    if (icon != null) {
                        iconCache.put(suite, icon);
                        final int idx = i;
                        SwingUtilities.invokeLater(() -> {
                            Rectangle cell = suitesList.getCellBounds(idx, idx);
                            if (cell != null) suitesList.repaint(cell);
                        });
                    }
                }
            } catch (Exception ex) {
                Logger.error("loadIconsAsync", ex);
            } finally {
                iconsLoading = false;
            }
        }, "cfg-icons").start();
    }

    private ImageIcon resolveIconForSuite(String suite, URL url) {
        try {
            if (url == null) return null;
            File file = new File(url.toURI());
            if (!file.exists()) return null;
            String name = file.getName().toLowerCase(Locale.ENGLISH);
            if (name.endsWith(".jad")) {
                return loadIconFromJad(file);
            } else if (name.endsWith(".jar")) {
                return loadIconFromJar(file);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private ImageIcon loadIconFromJad(File jadFile) {
        FileInputStream fis = null;
        try {
            JadProperties props = new JadProperties();
            fis = new FileInputStream(jadFile);
            props.read(fis);
            java.util.Vector entries = props.getMidletEntries();
            if (entries == null || entries.isEmpty()) return null;
            JadMidletEntry e = (JadMidletEntry) entries.elementAt(0);
            String iconPath = getJadEntryIcon(e);
            if (iconPath == null) return null;
            String jarUrl = props.getJarURL();
            if (jarUrl == null) return null;
            File jarFile = jarUrl.startsWith(File.separator) || jarUrl.contains(":")
                    ? new File(jarUrl)
                    : new File(jadFile.getParentFile(), jarUrl);
            return loadIconFromJar(jarFile, iconPath);
        } catch (Exception ex) {
            return null;
        } finally {
            try { if (fis != null) fis.close(); } catch (IOException ignored) {}
        }
    }

    private ImageIcon loadIconFromJar(File jarFile) {
        if (jarFile == null || !jarFile.isFile()) return null;
        JarFile jar = null;
        InputStream in = null;
        try {
            jar = new JarFile(jarFile);
            JarEntry mf = jar.getJarEntry("META-INF/MANIFEST.MF");
            if (mf == null) return null;
            in = jar.getInputStream(mf);
            JadProperties props = new JadProperties();
            props.read(in);
            java.util.Vector entries = props.getMidletEntries();
            if (entries == null || entries.isEmpty()) return null;
            JadMidletEntry e = (JadMidletEntry) entries.elementAt(0);
            String iconPath = getJadEntryIcon(e);
            if (iconPath == null) return null;
            return loadIconFromJar(jarFile, iconPath);
        } catch (Exception ex) {
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (IOException ignored) {}
            try { if (jar != null) jar.close(); } catch (IOException ignored) {}
        }
    }

    private ImageIcon loadIconFromJar(File jarFile, String iconPath) {
        if (jarFile == null || !jarFile.isFile() || iconPath == null || iconPath.isEmpty()) return null;
        String norm = iconPath.startsWith("/") ? iconPath.substring(1) : iconPath;
        JarFile jar = null;
        InputStream in = null;
        try {
            jar = new JarFile(jarFile);
            JarEntry entry = jar.getJarEntry(norm);
            if (entry == null) {
                entry = jar.getJarEntry(norm.toLowerCase(Locale.ENGLISH));
                if (entry == null) return null;
            }
            in = jar.getInputStream(entry);
            BufferedImage img = ImageIO.read(in);
            if (img == null) return null;
            return new ImageIcon(scaleToHeight(img, 24));
        } catch (Exception ex) {
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (IOException ignored) {}
            try { if (jar != null) jar.close(); } catch (IOException ignored) {}
        }
    }

    private static Image scaleToHeight(BufferedImage img, int h) {
        int w = Math.max(1, img.getWidth() * h / Math.max(1, img.getHeight()));
        return img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
    }

    private ImageIcon loadPlaceholderIcon() {
        try {
            java.net.URL res = ConfigManagerDialog.class.getResource("/org/je/icon.png");
            if (res != null) {
                BufferedImage img = ImageIO.read(res);
                if (img != null) return new ImageIcon(scaleToHeight(img, 24));
            }
        } catch (Exception ignored) {}
        Icon f = UIManager.getIcon("FileView.fileIcon");
        return f instanceof ImageIcon ? (ImageIcon) f : null;
    }

    private class SuiteCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            String suite = String.valueOf(value);
            ImageIcon icon = iconCache.get(suite);
            if (icon == null) icon = placeholderIcon;
            c.setIcon(icon);
            c.setIconTextGap(10);
            return c;
        }
    }

    private static String getJadEntryIcon(JadMidletEntry e) {
        try {
            java.lang.reflect.Field f = JadMidletEntry.class.getDeclaredField("icon");
            f.setAccessible(true);
            Object v = f.get(e);
            return v != null ? v.toString() : null;
        } catch (Throwable t) { return null; }
    }

    private void openConfigFolder() {
        try {
            File base = Config.getConfigPath();
            if (base == null) return;
            Desktop d = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (d != null && d.isSupported(Desktop.Action.OPEN)) {
                d.open(base);
            } else {
                JOptionPane.showMessageDialog(this, base.getAbsolutePath(), "Config Folder", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            Logger.error("Open config folder", ex);
        }
    }

    // ===== Export (selected) =====
    private void doExportSelected() {
        // Collect selected paths
        File base = Config.getConfigPath();
        if (base == null) return;
        java.util.List<Path> include = new ArrayList<>();
        for (ItemGroup g : exportGroups) {
            if (g.checkbox.isSelected()) include.addAll(g.paths);
        }
        if (include.isEmpty()) {
            int r = JOptionPane.showConfirmDialog(this, "No items selected. Export everything instead?", "Export", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (r != JOptionPane.OK_OPTION) return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export Configuration");
        fc.setSelectedFile(new File("JarEngine-config-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".zip"));
        int rv = fc.showSaveDialog(this);
        if (rv != JFileChooser.APPROVE_OPTION) return;
        File target = fc.getSelectedFile();
        if (target.exists()) {
            int res = JOptionPane.showConfirmDialog(this, "Overwrite existing file?", "Export", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return;
        }
        try {
            if (include.isEmpty()) {
                zipDirectory(base.toPath(), target.toPath());
            } else {
                zipSelected(base.toPath(), include, target.toPath());
            }
            JOptionPane.showMessageDialog(this, "Exported to: " + target.getAbsolutePath(), "Export", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            Logger.error("Export config", ex);
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== Import (selective) =====
    private void doImportSelective() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Import Configuration");
        int rv = fc.showOpenDialog(this);
        if (rv != JFileChooser.APPROVE_OPTION) return;
        File zip = fc.getSelectedFile();
        if (!zip.isFile()) return;

        File base = Config.getConfigPath();
        if (base == null) return;

        // Read entries from zip and propose a selection
        Map<String, List<String>> groups = groupZipEntries(zip.toPath());
        if (groups.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No importable entries found.", "Import", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Map<String, Boolean> selection = promptImportSelection(groups);
        if (selection == null) return; // cancelled

        int res = JOptionPane.showConfirmDialog(this,
                "Import will replace selected files under: \n" + base.getAbsolutePath() + "\nA backup will be created. Continue?",
                "Import Configuration", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        // Backup existing
        File backup = new File(base.getParentFile(), base.getName() + ".bak-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()));
        try {
            copyDirectory(base.toPath(), backup.toPath());
        } catch (IOException ex) {
            Logger.error("Backup before import failed", ex);
            JOptionPane.showMessageDialog(this, "Backup failed: " + ex.getMessage(), "Import Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Extract only selected groups
        try {
            unzipSelected(zip.toPath(), base.toPath(), selection);
            JOptionPane.showMessageDialog(this, "Import complete. Restart is recommended.", "Import", JOptionPane.INFORMATION_MESSAGE);
            loadSuites();
        } catch (IOException ex) {
            Logger.error("Import config", ex);
            JOptionPane.showMessageDialog(this, "Import failed: " + ex.getMessage() + "\nBackup at: " + backup.getAbsolutePath(), "Import Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== Selection building =====
    private void refreshExportSelection() {
        exportGroups.clear();
        exportSelectionPanel.removeAll();
        File base = Config.getConfigPath();
        if (base == null) return;
        File[] files = base.listFiles();
        if (files == null) files = new File[0];

        Map<String,String> niceNames = new HashMap<>();
        niceNames.put("performance.properties", "Performance");
        niceNames.put("network.properties", "Network");
        niceNames.put("filters.properties", "X-Render");
        niceNames.put("config2.xml", "Main Configuration");

        // Known singleton files
        for (File f : files) {
            if (f.isFile() && (f.getName().endsWith(".properties") || f.getName().equals("config2.xml"))) {
                String label = niceNames.getOrDefault(f.getName(), humanizeName(stripExt(f.getName())));
                exportGroups.add(new ItemGroup(label, Collections.singletonList(f.toPath())));
            }
        }

        // Installed apps (RMS)
        List<Path> rms = new ArrayList<>();
        for (File f : files) {
            if (f.isDirectory() && f.getName().startsWith("suite-")) rms.add(f.toPath());
        }
        if (!rms.isEmpty()) exportGroups.add(new ItemGroup("Installed Apps (RMS data) [" + rms.size() + "]", rms));

        // JARs (extensions/devices)
        List<Path> jars = new ArrayList<>();
        for (File f : files) {
            if (f.isFile() && f.getName().toLowerCase(Locale.ENGLISH).endsWith(".jar")) jars.add(f.toPath());
        }
        if (!jars.isEmpty()) exportGroups.add(new ItemGroup("Extensions / Device JARs [" + jars.size() + "]", jars));

        // Build UI
        for (ItemGroup g : exportGroups) {
            JCheckBox cb = new JCheckBox(g.label, true);
            g.checkbox = cb;
            exportSelectionPanel.add(cb);
        }
        // Ensure proper layout refresh to maintain scroll pane size
        exportSelectionPanel.revalidate();
        exportSelectionPanel.repaint();
        // Also revalidate the parent container to ensure scroll pane maintains proper size
        SwingUtilities.invokeLater(() -> {
            Container parent = exportSelectionPanel.getParent();
            while (parent != null && !(parent instanceof JScrollPane)) {
                parent = parent.getParent();
            }
            if (parent != null) {
                parent.revalidate();
            }
        });
    }

    private void setAllExportSelections(boolean selected) {
        for (ItemGroup g : exportGroups) {
            if (g.checkbox != null) g.checkbox.setSelected(selected);
        }
    }

    private static String stripExt(String n) {
        int i = n.lastIndexOf('.');
        return i > 0 ? n.substring(0, i) : n;
    }

    private static String humanizeName(String base) {
        String[] parts = base.replace('_', ' ').replace('-', ' ').split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    // ===== Helpers =====
    private static void deleteRecursive(Path path) throws IOException {
        if (!Files.exists(path)) return;
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
    }

    private static void copyDirectory(Path src, Path dst) throws IOException {
        if (!Files.exists(dst)) Files.createDirectories(dst);
        Files.walk(src).forEach(p -> {
            try {
                Path rel = src.relativize(p);
                Path q = dst.resolve(rel);
                if (Files.isDirectory(p)) {
                    Files.createDirectories(q);
                } else {
                    if (!Files.exists(q.getParent())) Files.createDirectories(q.getParent());
                    Files.copy(p, q, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                }
            } catch (IOException ignored) {}
        });
    }

    private static void zipDirectory(Path srcDir, Path zipFile) throws IOException {
        if (Files.exists(zipFile)) Files.delete(zipFile);
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(zipFile)))) {
            Files.walk(srcDir).forEach(p -> {
                try {
                    String entryName = srcDir.relativize(p).toString().replace('\\', '/');
                    if (entryName.isEmpty()) return; // skip root
                    ZipEntry ze = new ZipEntry(entryName + (Files.isDirectory(p) ? "/" : ""));
                    zos.putNextEntry(ze);
                    if (Files.isRegularFile(p)) {
                        Files.copy(p, zos);
                    }
                    zos.closeEntry();
                } catch (IOException ignored) {}
            });
        }
    }

    private static void zipSelected(Path base, List<Path> items, Path zipFile) throws IOException {
        if (Files.exists(zipFile)) Files.delete(zipFile);
        Set<String> added = new HashSet<>();
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(zipFile)))) {
            for (Path item : items) {
                Path abs = item;
                if (!abs.isAbsolute()) abs = base.resolve(item);
                if (!Files.exists(abs)) continue;
                if (Files.isDirectory(abs)) {
                    Files.walk(abs).forEach(p -> {
                        try {
                            String rel = base.relativize(p).toString().replace('\\', '/');
                            if (rel.isEmpty()) return;
                            if (Files.isDirectory(p)) {
                                // ensure directory entry once
                                if (added.add(rel + "/")) {
                                    zos.putNextEntry(new ZipEntry(rel + "/"));
                                    zos.closeEntry();
                                }
                            } else {
                                if (added.add(rel)) {
                                    zos.putNextEntry(new ZipEntry(rel));
                                    Files.copy(p, zos);
                                    zos.closeEntry();
                                }
                            }
                        } catch (IOException ignored) {}
                    });
                } else {
                    String rel = base.relativize(abs).toString().replace('\\', '/');
                    if (added.add(rel)) {
                        zos.putNextEntry(new ZipEntry(rel));
                        Files.copy(abs, zos);
                        zos.closeEntry();
                    }
                }
            }
        }
    }

    private static void unzipInto(Path zip, Path dstDir) throws IOException {
        if (!Files.exists(dstDir)) Files.createDirectories(dstDir);
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zip)))) {
            ZipEntry e;
            byte[] buf = new byte[8192];
            while ((e = zis.getNextEntry()) != null) {
                Path out = dstDir.resolve(e.getName()).normalize();
                if (!out.startsWith(dstDir)) {
                    // protect against zip slip
                    zis.closeEntry();
                    continue;
                }
                if (e.isDirectory() || e.getName().endsWith("/")) {
                    Files.createDirectories(out);
                } else {
                    if (!Files.exists(out.getParent())) Files.createDirectories(out.getParent());
                    try (OutputStream os = Files.newOutputStream(out, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        int n; while ((n = zis.read(buf)) > 0) os.write(buf, 0, n);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private static Map<String, List<String>> groupZipEntries(Path zip) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zip)))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                String name = e.getName();
                if (name == null || name.isEmpty()) { zis.closeEntry(); continue; }
                String group = classifyEntry(name);
                if (group == null) { zis.closeEntry(); continue; }
                map.computeIfAbsent(group, k -> new ArrayList<>()).add(name);
                zis.closeEntry();
            }
        } catch (IOException ignored) {}
        return map;
    }

    private static String classifyEntry(String name) {
        String n = name;
        if (n.endsWith("/")) n = n.substring(0, n.length()-1);
        String base = n.contains("/") ? n.substring(0, n.indexOf('/')) : n;
        if (base.equals("performance.properties")) return "Performance";
        if (base.equals("network.properties")) return "Network";
        if (base.equals("filters.properties")) return "X-Render";
        if (base.equals("config2.xml")) return "Main Configuration";
        if (base.startsWith("suite-")) return "Installed Apps";
        if (base.toLowerCase(Locale.ENGLISH).endsWith(".jar")) return "Extensions / Device JARs";
        if (base.toLowerCase(Locale.ENGLISH).endsWith(".properties")) return humanizeName(stripExt(base));
        return null; // skip miscellaneous unless recognized
    }

    private Map<String, Boolean> promptImportSelection(Map<String, List<String>> groups) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        Map<String, JCheckBox> map = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : groups.entrySet()) {
            String label = e.getKey() + " [" + e.getValue().size() + "]";
            JCheckBox cb = new JCheckBox(label, true);
            map.put(e.getKey(), cb);
            panel.add(cb);
        }
        JScrollPane sp = new JScrollPane(panel);
        // Use natural sizing with reasonable constraints
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int maxWidth = Math.min(500, screenSize.width / 3);
        int maxHeight = Math.min(350, screenSize.height / 3);
        sp.setMaximumSize(new Dimension(maxWidth, maxHeight));
        int res = JOptionPane.showConfirmDialog(this, sp, "Select items to import", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return null;
        Map<String, Boolean> sel = new LinkedHashMap<>();
        for (Map.Entry<String, JCheckBox> e : map.entrySet()) sel.put(e.getKey(), e.getValue().isSelected());
        return sel;
    }

    private static void unzipSelected(Path zip, Path dstDir, Map<String, Boolean> selection) throws IOException {
        if (!Files.exists(dstDir)) Files.createDirectories(dstDir);
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zip)))) {
            ZipEntry e;
            byte[] buf = new byte[8192];
            while ((e = zis.getNextEntry()) != null) {
                String name = e.getName();
                String group = classifyEntry(name);
                if (group == null || !Boolean.TRUE.equals(selection.get(group))) { zis.closeEntry(); continue; }
                Path out = dstDir.resolve(name).normalize();
                if (!out.startsWith(dstDir)) { zis.closeEntry(); continue; }
                if (e.isDirectory() || name.endsWith("/")) {
                    Files.createDirectories(out);
                } else {
                    if (!Files.exists(out.getParent())) Files.createDirectories(out.getParent());
                    try (OutputStream os = Files.newOutputStream(out, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        int n; while ((n = zis.read(buf)) > 0) os.write(buf, 0, n);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    // Helper structure for selection groups
    private static class ItemGroup {
        final String label;
        final List<Path> paths;
        JCheckBox checkbox;
        ItemGroup(String label, List<Path> paths) { this.label = label; this.paths = paths; }
    }
}
