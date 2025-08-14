package org.je.app.ui.swing;

import org.je.app.util.MidletURLReference;
import org.je.log.Logger;
import org.je.util.JadMidletEntry;
import org.je.util.JadProperties;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utility to resolve a MIDlet icon from a local JAD/JAR pointed by a MidletURLReference.
 * For non-file URLs or failures, returns a small placeholder icon.
 */
public final class MidletIconLoader {

    private static final ConcurrentHashMap<String, ImageIcon> CACHE = new ConcurrentHashMap<>();

    private MidletIconLoader() {}

    public static ImageIcon resolveIcon(MidletURLReference ref) {
        if (ref == null || ref.getUrl() == null) return placeholder();
        String key = ref.getUrl();
        ImageIcon cached = CACHE.get(key);
        if (cached != null) return cached;
        try {
            URL u = new URL(ref.getUrl());
            if (!"file".equalsIgnoreCase(u.getProtocol())) {
                return putCache(key, placeholder());
            }
            File f = new File(u.toURI());
            if (!f.exists()) return putCache(key, placeholder());
            String name = f.getName().toLowerCase(Locale.ENGLISH);
            ImageIcon icon = null;
            if (name.endsWith(".jad")) {
                icon = loadIconFromJad(f);
            } else if (name.endsWith(".jar")) {
                icon = loadIconFromJar(f);
            }
            if (icon == null) icon = placeholder();
            return putCache(key, icon);
        } catch (Exception e) {
            Logger.debug("MidletIconLoader", e);
            return putCache(ref.getUrl(), placeholder());
        }
    }

    private static ImageIcon loadIconFromJad(File jadFile) {
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
        } catch (Exception ignore) {
            return null;
        } finally {
            if (fis != null) try { fis.close(); } catch (IOException ignore) {}
        }
    }

    private static ImageIcon loadIconFromJar(File jarFile) {
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
        } catch (Exception ignore) {
            return null;
        } finally {
            if (in != null) try { in.close(); } catch (IOException ignore) {}
            if (jar != null) try { jar.close(); } catch (IOException ignore) {}
        }
    }

    private static ImageIcon loadIconFromJar(File jarFile, String iconPath) {
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
            return new ImageIcon(scaleToHeight(img, 16));
        } catch (Exception ignore) {
            return null;
        } finally {
            if (in != null) try { in.close(); } catch (IOException ignore) {}
            if (jar != null) try { jar.close(); } catch (IOException ignore) {}
        }
    }

    private static Image scaleToHeight(BufferedImage img, int h) {
        int w = Math.max(1, img.getWidth() * h / Math.max(1, img.getHeight()));
        return img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
    }

    private static String getJadEntryIcon(JadMidletEntry e) {
        try {
            java.lang.reflect.Field f = JadMidletEntry.class.getDeclaredField("icon");
            f.setAccessible(true);
            Object v = f.get(e);
            return v != null ? v.toString() : null;
        } catch (Throwable t) { return null; }
    }

    private static ImageIcon placeholder() {
        try {
            java.net.URL res = MidletIconLoader.class.getResource("/org/je/icon.png");
            if (res != null) {
                BufferedImage img = ImageIO.read(res);
                if (img != null) return new ImageIcon(scaleToHeight(img, 16));
            }
        } catch (Exception ignore) {}
        return new ImageIcon();
    }

    private static ImageIcon putCache(String key, ImageIcon icon) {
        CACHE.put(key, icon);
        return icon;
    }
}
