package org.je.app.tools;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.RadialGradientPaint;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.je.app.Config;

/**
 * Centralized filter manager for post-processing the scaled emulator frame.
 * Lightweight, reusable, and optional: if filters are off, render path is unchanged.
 */
public final class FilterManager {

    public enum ColorMode {
        FULL_COLOR,
        GRAYSCALE,
        MONOCHROME
    }

    public enum PaletteMode {
        NONE,
        RGB565,
        RGB444,
        RGB332,
        FIXED_16
    }

    public enum DitherMode {
        NONE,
        ORDERED_2x2,
        ORDERED_4x4,
        FLOYD_STEINBERG
    }

    private static volatile ColorMode colorMode = ColorMode.FULL_COLOR;
    private static volatile boolean scanlines;
    private static volatile float scanlinesIntensity = 0.12f;
    private static volatile boolean vignette;
    private static volatile float vignetteIntensity = 0.2f;
    private static volatile boolean bloom;
    private static volatile float bloomThreshold = 0.7f; // 0..1
    private static volatile float bloomIntensity = 0.6f; // 0..2 (additive)
    private static volatile int bloomRadius = 2; // 1..5 (box blur radius)

    private static volatile PaletteMode paletteMode = PaletteMode.NONE;
    private static volatile DitherMode ditherMode = DitherMode.NONE;

    private static volatile float brightness = 1.0f; // 1.0 = neutral
    private static volatile float contrast = 1.0f;   // 1.0 = neutral
    private static volatile float gamma = 1.0f;      // 1.0 = neutral
    private static volatile float saturation = 1.0f; // 1.0 = neutral

    // Thread-local scratch buffers to avoid synchronization and per-frame allocations
    private static final ThreadLocal<BufferedImage> scratchBig = new ThreadLocal<>();
    private static final ThreadLocal<BufferedImage> scratchSmall = new ThreadLocal<>();
    private static final ThreadLocal<BufferedImage> bloomA = new ThreadLocal<>();
    private static final ThreadLocal<BufferedImage> bloomB = new ThreadLocal<>();

    private static final ColorConvertOp GRAY_OP = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);

    // Cached gamma LUT
    private static float lastGamma = -1f;
    private static int[] gammaLUT; // 256 entries

    // Persistence
    private static final String FILE_NAME = "filters.properties";
    private static volatile boolean preferencesLoaded = false;
    private static volatile boolean pendingSave = false;
    private static volatile long lastSaveTime = 0L;

    static {
        try { loadPreferences(); } catch (Throwable ignored) {}
    }

    private FilterManager() {}

    public static ColorMode getColorMode() { return colorMode; }
    public static void setColorMode(ColorMode m) { if (m != null) { colorMode = m; savePreferencesAsync(); } }

    public static boolean isScanlines() { return scanlines; }
    public static void setScanlines(boolean v) { scanlines = v; savePreferencesAsync(); }

    public static float getScanlinesIntensity() { return scanlinesIntensity; }
    public static void setScanlinesIntensity(float v) { scanlinesIntensity = clamp01(v); savePreferencesAsync(); }

    public static boolean isVignette() { return vignette; }
    public static void setVignette(boolean v) { vignette = v; savePreferencesAsync(); }

    public static float getVignetteIntensity() { return vignetteIntensity; }
    public static void setVignetteIntensity(float v) { vignetteIntensity = clamp01(v); savePreferencesAsync(); }

    public static boolean isBloom() { return bloom; }
    public static void setBloom(boolean v) { bloom = v; savePreferencesAsync(); }
    public static float getBloomThreshold() { return bloomThreshold; }
    public static void setBloomThreshold(float v) { bloomThreshold = clamp01(v); savePreferencesAsync(); }
    public static float getBloomIntensity() { return bloomIntensity; }
    public static void setBloomIntensity(float v) { bloomIntensity = clamp(v, 0f, 2f); savePreferencesAsync(); }
    public static int getBloomRadius() { return bloomRadius; }
    public static void setBloomRadius(int r) { bloomRadius = Math.max(1, Math.min(r, 5)); savePreferencesAsync(); }

    public static PaletteMode getPaletteMode() { return paletteMode; }
    public static void setPaletteMode(PaletteMode m) { if (m != null) { paletteMode = m; savePreferencesAsync(); } }
    public static DitherMode getDitherMode() { return ditherMode; }
    public static void setDitherMode(DitherMode m) { if (m != null) { ditherMode = m; savePreferencesAsync(); } }

    public static float getBrightness() { return brightness; }
    public static void setBrightness(float v) { brightness = clamp(v, 0.2f, 2.0f); savePreferencesAsync(); }

    public static float getContrast() { return contrast; }
    public static void setContrast(float v) { contrast = clamp(v, 0.2f, 2.0f); savePreferencesAsync(); }

    public static float getGamma() { return gamma; }
    public static void setGamma(float v) { gamma = clamp(v, 0.2f, 3.0f); savePreferencesAsync(); }

    public static float getSaturation() { return saturation; }
    public static void setSaturation(float v) { saturation = clamp(v, 0.0f, 2.0f); savePreferencesAsync(); }

    public static boolean hasActiveFilters() {
        // Dither alone has no effect without a palette; ignore dither-only case.
        boolean paletteActive = (paletteMode != PaletteMode.NONE);
        return colorMode != ColorMode.FULL_COLOR || scanlines || vignette || bloom ||
               paletteActive ||
               Math.abs(brightness - 1.0f) > 0.001f || Math.abs(contrast - 1.0f) > 0.001f ||
               Math.abs(gamma - 1.0f) > 0.001f || Math.abs(saturation - 1.0f) > 0.001f;
    }

    /**
     * Render source image scaled into dest size and apply configured filters.
     * Returns an internal BufferedImage; caller must not mutate its raster.
     * 
     * Performance: Removed synchronization to prevent blocking main render thread.
     * Uses thread-local buffers for thread safety.
     */
    public static BufferedImage renderFiltered(BufferedImage src, int destW, int destH, Object interpHint) {
        if (src == null || destW <= 0 || destH <= 0) return null;
        final int srcW = src.getWidth();
        final int srcH = src.getHeight();

        // Ensure thread-local buffers
        BufferedImage scratchSmallBuf = scratchSmall.get();
        if (scratchSmallBuf == null || scratchSmallBuf.getWidth() != srcW || scratchSmallBuf.getHeight() != srcH) {
            scratchSmallBuf = new BufferedImage(srcW, srcH, BufferedImage.TYPE_INT_ARGB);
            scratchSmall.set(scratchSmallBuf);
        }
        
        BufferedImage scratchBigBuf = scratchBig.get();
        if (scratchBigBuf == null || scratchBigBuf.getWidth() != destW || scratchBigBuf.getHeight() != destH) {
            scratchBigBuf = new BufferedImage(destW, destH, BufferedImage.TYPE_INT_ARGB);
            scratchBig.set(scratchBigBuf);
        }
        
        BufferedImage bloomABuf = bloomA.get();
        BufferedImage bloomBBuf = bloomB.get();
        if (bloomABuf == null || bloomABuf.getWidth() != destW || bloomABuf.getHeight() != destH) {
            bloomABuf = new BufferedImage(destW, destH, BufferedImage.TYPE_INT_ARGB);
            bloomBBuf = new BufferedImage(destW, destH, BufferedImage.TYPE_INT_ARGB);
            bloomA.set(bloomABuf);
            bloomB.set(bloomBBuf);
        }

        boolean needsPreOps = colorMode != ColorMode.FULL_COLOR ||
                              Math.abs(brightness - 1.0f) > 0.001f ||
                              Math.abs(contrast - 1.0f) > 0.001f ||
                              Math.abs(gamma - 1.0f) > 0.001f ||
                              Math.abs(saturation - 1.0f) > 0.001f;

        BufferedImage toScale;
        if (needsPreOps) {
            // Copy src to small scratch
            Graphics2D gS = scratchSmallBuf.createGraphics();
            try {
                gS.drawImage(src, 0, 0, null);
            } finally { gS.dispose(); }

            // Apply color mode
            if (colorMode == ColorMode.GRAYSCALE) {
                GRAY_OP.filter(scratchSmallBuf, scratchSmallBuf);
            } else if (colorMode == ColorMode.MONOCHROME) {
                GRAY_OP.filter(scratchSmallBuf, scratchSmallBuf);
                thresholdToMonochromeInPlace(scratchSmallBuf, 0x80);
            }

            // Brightness/Contrast via RescaleOp (linear): out = in*scale + offset
            if (Math.abs(brightness - 1.0f) > 0.001f || Math.abs(contrast - 1.0f) > 0.001f) {
                float scale = brightness * contrast;
                float offset = 128f * (1f - contrast);
                float[] scales = new float[]{scale, scale, scale, 1.0f};
                float[] offsets = new float[]{offset, offset, offset, 0f};
                RescaleOp op = new RescaleOp(scales, offsets, null);
                op.filter(scratchSmallBuf, scratchSmallBuf);
            }

            // Gamma via LUT (RGB only)
            if (Math.abs(gamma - 1.0f) > 0.001f) {
                ensureGammaLUT();
                applyLUTInPlace(scratchSmallBuf, gammaLUT);
            }

            // Saturation adjustment in RGB space using luminance mix
            if (Math.abs(saturation - 1.0f) > 0.001f) {
                adjustSaturationInPlace(scratchSmallBuf, saturation);
            }
            toScale = scratchSmallBuf;
        } else {
            toScale = src;
        }

        // Scale to destination
        Graphics2D gB = scratchBigBuf.createGraphics();
        try {
            if (interpHint != null) {
                gB.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpHint);
            }
            gB.drawImage(toScale, 0, 0, destW, destH, null);
        } finally { gB.dispose(); }

        // Bloom before palette/dither so palette can clamp final look if desired
        if (bloom) {
            applyBloom(scratchBigBuf, bloomABuf, bloomBBuf, bloomThreshold, bloomIntensity, bloomRadius);
        }

        // Palette + Dithering at destination scale
        if (paletteMode != PaletteMode.NONE) {
            applyPaletteAndDither(scratchBigBuf, paletteMode, ditherMode);
        }

        // Overlays at destination scale
        if (scanlines) {
            drawScanlines(scratchBigBuf, scanlinesIntensity);
        }
        if (vignette) {
            drawVignette(scratchBigBuf, vignetteIntensity);
        }

        return scratchBigBuf;
    }

    private static void thresholdToMonochromeInPlace(BufferedImage img, int threshold) {
        final int w = img.getWidth();
        final int h = img.getHeight();
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            img.getRGB(0, y, w, 1, row, 0, w);
            for (int x = 0; x < w; x++) {
                int argb = row[x];
                int a = (argb >>> 24) & 0xFF;
                int gray = argb & 0xFF; // after gray convert, R=G=B
                int bw = (gray >= threshold) ? 0x00FFFFFF : 0x00000000;
                row[x] = (a << 24) | bw;
            }
            img.setRGB(0, y, w, 1, row, 0, w);
        }
    }

    private static void drawScanlines(BufferedImage img, float intensity) {
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setComposite(AlphaComposite.SrcOver.derive(clamp01(intensity)));
            g2.setColor(Color.BLACK);
            int h = img.getHeight();
            int w = img.getWidth();
            for (int y = 0; y < h; y += 2) {
                g2.drawLine(0, y, w, y);
            }
        } finally {
            g2.dispose();
        }
    }

    private static void drawVignette(BufferedImage img, float intensity) {
        if (intensity <= 0f) return;
        int w = img.getWidth();
        int h = img.getHeight();
        float radius = Math.max(w, h) * 0.6f;
        float cx = w * 0.5f;
        float cy = h * 0.5f;
        float alpha = clamp01(intensity);
        // Radial gradient from transparent center to black edges
        float[] dist = new float[]{0.0f, 0.7f, 1.0f};
        Color[] colors = new Color[]{
            new Color(0,0,0,0),
            new Color(0,0,0,(int)(alpha*80)),
            new Color(0,0,0,(int)(alpha*150))
        };
        RadialGradientPaint paint = new RadialGradientPaint(cx, cy, radius, dist, colors, CycleMethod.NO_CYCLE);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setPaint(paint);
            g2.fillRect(0, 0, w, h);
        } finally { g2.dispose(); }
    }

    private static void applyBloom(BufferedImage srcDst, BufferedImage tmpA, BufferedImage tmpB, float threshold, float intensity, int radius) {
        final int w = srcDst.getWidth();
        final int h = srcDst.getHeight();
        // Extract bright areas into tmpA
        int th = (int)(clamp01(threshold) * 255f);
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            srcDst.getRGB(0, y, w, 1, row, 0, w);
            for (int x = 0; x < w; x++) {
                int argb = row[x];
                int a = (argb >>> 24) & 0xFF;
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;
                int lum = (int)(0.2126*r + 0.7152*g + 0.0722*b);
                if (lum >= th) {
                    // keep bright color, else transparent
                    row[x] = (a << 24) | (r << 16) | (g << 8) | b;
                } else {
                    row[x] = 0x00000000;
                }
            }
            tmpA.setRGB(0, y, w, 1, row, 0, w);
        }
        // Blur tmpA into tmpB with simple separable box blur of given radius
        boxBlur(tmpA, tmpB, radius);
        boxBlur(tmpB, tmpA, radius);
        // Additively composite tmpA over srcDst
        Graphics2D g2 = srcDst.createGraphics();
        try {
            g2.setComposite(AlphaComposite.SrcOver.derive(clamp(intensity, 0f, 1f)));
            g2.drawImage(tmpA, 0, 0, null);
        } finally { g2.dispose(); }
    }

    private static void boxBlur(BufferedImage src, BufferedImage dst, int radius) {
        radius = Math.max(1, Math.min(radius, 8));
        final int w = src.getWidth();
        final int h = src.getHeight();
        int[] in = new int[w*h];
        int[] tmp = new int[w*h];
        src.getRGB(0, 0, w, h, in, 0, w);
        int r = radius;
        int size = r*2 + 1;
        // Horizontal
        for (int y = 0; y < h; y++) {
            int idx = y*w;
            int ar=0, ag=0, ab=0, aa=0;
            for (int i = -r; i <= r; i++) {
                int xx = clampIndex(i, 0, w-1);
                int argb = in[idx + xx];
                aa += (argb >>> 24) & 0xFF;
                ar += (argb >>> 16) & 0xFF;
                ag += (argb >>> 8) & 0xFF;
                ab += argb & 0xFF;
            }
            for (int x = 0; x < w; x++) {
                tmp[idx + x] = ((aa/size) << 24) | ((ar/size) << 16) | ((ag/size) << 8) | (ab/size);
                int xOut = x - r;
                int xIn  = x + r + 1;
                int argbOut = in[idx + clampIndex(xOut, 0, w-1)];
                int argbIn  = in[idx + clampIndex(xIn, 0, w-1)];
                aa += ((argbIn >>> 24) & 0xFF) - ((argbOut >>> 24) & 0xFF);
                ar += ((argbIn >>> 16) & 0xFF) - ((argbOut >>> 16) & 0xFF);
                ag += ((argbIn >>> 8) & 0xFF) - ((argbOut >>> 8) & 0xFF);
                ab += (argbIn & 0xFF) - (argbOut & 0xFF);
            }
        }
        // Vertical
        int[] out = new int[w*h];
        for (int x = 0; x < w; x++) {
            int ar=0, ag=0, ab=0, aa=0;
            for (int i = -r; i <= r; i++) {
                int yy = clampIndex(i, 0, h-1);
                int argb = tmp[yy*w + x];
                aa += (argb >>> 24) & 0xFF;
                ar += (argb >>> 16) & 0xFF;
                ag += (argb >>> 8) & 0xFF;
                ab += argb & 0xFF;
            }
            for (int y = 0; y < h; y++) {
                out[y*w + x] = ((aa/size) << 24) | ((ar/size) << 16) | ((ag/size) << 8) | (ab/size);
                int yOut = y - r;
                int yIn  = y + r + 1;
                int argbOut = tmp[clampIndex(yOut, 0, h-1)*w + x];
                int argbIn  = tmp[clampIndex(yIn, 0, h-1)*w + x];
                aa += ((argbIn >>> 24) & 0xFF) - ((argbOut >>> 24) & 0xFF);
                ar += ((argbIn >>> 16) & 0xFF) - ((argbOut >>> 16) & 0xFF);
                ag += ((argbIn >>> 8) & 0xFF) - ((argbOut >>> 8) & 0xFF);
                ab += (argbIn & 0xFF) - (argbOut & 0xFF);
            }
        }
        dst.setRGB(0, 0, w, h, out, 0, w);
    }

    private static int clampIndex(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }

    private static void applyPaletteAndDither(BufferedImage img, PaletteMode palette, DitherMode dither) {
        switch (palette) {
            case RGB565: quantizeBitmask(img, 5, 6, 5, dither); break;
            case RGB444: quantizeBitmask(img, 4, 4, 4, dither); break;
            case RGB332: quantizeBitmask(img, 3, 3, 2, dither); break;
            case FIXED_16: quantizeFixed16(img, dither); break;
            case NONE:
            default: break;
        }
    }

    private static void quantizeBitmask(BufferedImage img, int rBits, int gBits, int bBits, DitherMode dither) {
        final int w = img.getWidth();
        final int h = img.getHeight();
        int[] row = new int[w];
        int rLevels = (1 << rBits) - 1;
        int gLevels = (1 << gBits) - 1;
        int bLevels = (1 << bBits) - 1;

        int[][] bayer2 = {{0,2},{3,1}};
        int[][] bayer4 = {{0,8,2,10},{12,4,14,6},{3,11,1,9},{15,7,13,5}};

        float scale2 = 1f/4f;
        float scale4 = 1f/16f;

        if (dither == DitherMode.FLOYD_STEINBERG) {
            // Error diffusion
            float[][] errR = new float[h][w];
            float[][] errG = new float[h][w];
            float[][] errB = new float[h][w];
            for (int y=0;y<h;y++) {
                img.getRGB(0, y, w, 1, row, 0, w);
                boolean leftToRight = true; // simple L->R only to keep code compact
                for (int x=0;x<w;x++) {
                    int argb = row[x];
                    int a = (argb>>>24)&0xFF;
                    int r = ((argb>>>16)&0xFF) + Math.round(errR[y][x]);
                    int g = ((argb>>>8)&0xFF) + Math.round(errG[y][x]);
                    int b = (argb&0xFF) + Math.round(errB[y][x]);
                    r = clampInt(r); g = clampInt(g); b = clampInt(b);
                    int rq = (int)Math.round((rLevels) * (r/255f));
                    int gq = (int)Math.round((gLevels) * (g/255f));
                    int bq = (int)Math.round((bLevels) * (b/255f));
                    int rOut = (int)Math.round(255f * rq / (float)rLevels);
                    int gOut = (int)Math.round(255f * gq / (float)gLevels);
                    int bOut = (int)Math.round(255f * bq / (float)bLevels);
                    row[x] = (a<<24) | (rOut<<16) | (gOut<<8) | bOut;
                    // distribute error
                    int er = r - rOut;
                    int eg = g - gOut;
                    int eb = b - bOut;
                    distributeFS(errR, errG, errB, y, x, w, h, er, eg, eb);
                }
                img.setRGB(0, y, w, 1, row, 0, w);
            }
            return;
        }

        for (int y = 0; y < h; y++) {
            img.getRGB(0, y, w, 1, row, 0, w);
            for (int x = 0; x < w; x++) {
                int argb = row[x];
                int a = (argb>>>24)&0xFF;
                int r = (argb>>>16)&0xFF;
                int g = (argb>>>8)&0xFF;
                int b = argb&0xFF;
                float bias = 0f;
                if (dither == DitherMode.ORDERED_2x2) {
                    bias = (bayer2[y&1][x&1] + 0.5f) * scale2 - 0.5f; // -0.5..+0.5
                } else if (dither == DitherMode.ORDERED_4x4) {
                    bias = (bayer4[y&3][x&3] + 0.5f) * scale4 - 0.5f;
                }
                // Apply bias per channel scaled to step size
                int rOut = quantizeWithBias(r, rLevels, bias);
                int gOut = quantizeWithBias(g, gLevels, bias);
                int bOut = quantizeWithBias(b, bLevels, bias);
                row[x] = (a<<24) | (rOut<<16) | (gOut<<8) | bOut;
            }
            img.setRGB(0, y, w, 1, row, 0, w);
        }
    }

    private static void distributeFS(float[][] er, float[][] eg, float[][] eb, int y, int x, int w, int h, int dr, int dg, int db) {
        // Floyd–Steinberg weights:
        //       x   7/16
        // 3/16  5/16 1/16
        addErr(er, eg, eb, y, x+1, w, h, dr*7/16f, dg*7/16f, db*7/16f);
        addErr(er, eg, eb, y+1, x-1, w, h, dr*3/16f, dg*3/16f, db*3/16f);
        addErr(er, eg, eb, y+1, x,   w, h, dr*5/16f, dg*5/16f, db*5/16f);
        addErr(er, eg, eb, y+1, x+1, w, h, dr*1/16f, dg*1/16f, db*1/16f);
    }

    private static void addErr(float[][] er, float[][] eg, float[][] eb, int y, int x, int w, int h, float dr, float dg, float db) {
        if (y < 0 || y >= h || x < 0 || x >= w) return;
        er[y][x] += dr; eg[y][x] += dg; eb[y][x] += db;
    }

    private static int quantizeWithBias(int v, int levels, float bias) {
        float vf = clamp(v/255f + bias*0.25f, 0f, 1f); // small bias scale
        int q = Math.round(levels * vf);
        return clampInt(Math.round(255f * q / (float)levels));
    }

    private static void quantizeFixed16(BufferedImage img, DitherMode dither) {
        final int[] pal = FIXED_16_PALETTE;
        final int w = img.getWidth();
        final int h = img.getHeight();
        int[] row = new int[w];
        int[][] bayer2 = {{0,2},{3,1}};
        int[][] bayer4 = {{0,8,2,10},{12,4,14,6},{3,11,1,9},{15,7,13,5}};
        float scale2 = 1f/4f;
        float scale4 = 1f/16f;
        for (int y=0;y<h;y++) {
            img.getRGB(0, y, w, 1, row, 0, w);
            for (int x=0;x<w;x++) {
                int argb = row[x];
                int a = (argb>>>24)&0xFF;
                int r = (argb>>>16)&0xFF;
                int g = (argb>>>8)&0xFF;
                int b = argb&0xFF;
                float bias = 0f;
                if (dither == DitherMode.ORDERED_2x2) bias = (bayer2[y&1][x&1] + 0.5f) * scale2 - 0.5f;
                else if (dither == DitherMode.ORDERED_4x4) bias = (bayer4[y&3][x&3] + 0.5f) * scale4 - 0.5f;
                int rr = clampInt(Math.round(r + bias*16));
                int gg = clampInt(Math.round(g + bias*16));
                int bb = clampInt(Math.round(b + bias*16));
                int nearest = nearestColor(rr, gg, bb, pal);
                row[x] = (a<<24) | (nearest & 0x00FFFFFF);
            }
            img.setRGB(0, y, w, 1, row, 0, w);
        }
    }

    private static int nearestColor(int r, int g, int b, int[] pal) {
        int best = pal[0];
        int bestD = Integer.MAX_VALUE;
        for (int c : pal) {
            int pr = (c>>>16)&0xFF;
            int pg = (c>>>8)&0xFF;
            int pb = c&0xFF;
            int dr = pr - r, dg = pg - g, db = pb - b;
            int d = dr*dr + dg*dg + db*db;
            if (d < bestD) { bestD = d; best = c; }
        }
        return best;
    }

    private static final int[] FIXED_16_PALETTE = new int[] {
        0x000000, 0x808080, 0xC0C0C0, 0xFFFFFF, // black, gray, silver, white
        0x800000, 0xFF0000, 0x808000, 0xFFFF00, // maroon, red, olive, yellow
        0x008000, 0x00FF00, 0x008080, 0x00FFFF, // green, lime, teal, aqua
        0x000080, 0x0000FF, 0x800080, 0xFF00FF  // navy, blue, purple, fuchsia
    };

    private static void ensureGammaLUT() {
        if (gammaLUT != null && Math.abs(lastGamma - gamma) < 1e-3) return;
        gammaLUT = new int[256];
        double inv = 1.0 / Math.max(0.001, gamma);
        for (int i = 0; i < 256; i++) {
            int v = (int)Math.round(255.0 * Math.pow(i / 255.0, inv));
            if (v < 0) v = 0; else if (v > 255) v = 255;
            gammaLUT[i] = v;
        }
        lastGamma = gamma;
    }

    private static void applyLUTInPlace(BufferedImage img, int[] lut) {
        final int w = img.getWidth();
        final int h = img.getHeight();
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            img.getRGB(0, y, w, 1, row, 0, w);
            for (int x = 0; x < w; x++) {
                int argb = row[x];
                int a = (argb >>> 24) & 0xFF;
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;
                r = lut[r]; g = lut[g]; b = lut[b];
                row[x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
            img.setRGB(0, y, w, 1, row, 0, w);
        }
    }

    private static void adjustSaturationInPlace(BufferedImage img, float sat) {
        final int w = img.getWidth();
        final int h = img.getHeight();
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            img.getRGB(0, y, w, 1, row, 0, w);
            for (int x = 0; x < w; x++) {
                int argb = row[x];
                int a = (argb >>> 24) & 0xFF;
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;
                // Luminance per Rec. 709
                int lum = (int)Math.round(0.2126*r + 0.7152*g + 0.0722*b);
                r = clampInt(lum + (int)Math.round((r - lum) * sat));
                g = clampInt(lum + (int)Math.round((g - lum) * sat));
                b = clampInt(lum + (int)Math.round((b - lum) * sat));
                row[x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
            img.setRGB(0, y, w, 1, row, 0, w);
        }
    }

    private static int clampInt(int v) { return v < 0 ? 0 : (v > 255 ? 255 : v); }
    private static float clamp(float v, float lo, float hi) { return (v < lo) ? lo : (v > hi ? hi : v); }
    private static float clamp01(float v) { return clamp(v, 0f, 1f); }

    // Persistence
    private static File getPreferencesFile() {
        File dir = Config.getConfigPath();
        if (dir != null) {
            dir.mkdirs();
            return new File(dir, FILE_NAME);
        }
        // Fallback to current working directory if config path is unavailable
        return new File(FILE_NAME);
    }

    public static synchronized void loadPreferences() {
        if (preferencesLoaded) return;
        File f = getPreferencesFile();
        if (f.isFile()) {
            Properties p = new Properties();
            try (FileInputStream in = new FileInputStream(f)) {
                p.load(in);
                String cm = p.getProperty("colorMode", colorMode.name());
                try { colorMode = ColorMode.valueOf(cm); } catch (IllegalArgumentException ignored) {}
                scanlines = Boolean.parseBoolean(p.getProperty("scanlines", Boolean.toString(scanlines)));
                scanlinesIntensity = parseFloat(p.getProperty("scanlinesIntensity"), scanlinesIntensity);
                vignette = Boolean.parseBoolean(p.getProperty("vignette", Boolean.toString(vignette)));
                vignetteIntensity = parseFloat(p.getProperty("vignetteIntensity"), vignetteIntensity);
                // Bloom
                bloom = Boolean.parseBoolean(p.getProperty("bloom", Boolean.toString(bloom)));
                bloomThreshold = parseFloat(p.getProperty("bloomThreshold"), bloomThreshold);
                bloomIntensity = parseFloat(p.getProperty("bloomIntensity"), bloomIntensity);
                try { bloomRadius = Integer.parseInt(p.getProperty("bloomRadius", Integer.toString(bloomRadius))); } catch (NumberFormatException ignored) {}
                // Palette & Dither
                String pm = p.getProperty("paletteMode", paletteMode.name());
                try { paletteMode = PaletteMode.valueOf(pm); } catch (IllegalArgumentException ignored) {}
                String dm = p.getProperty("ditherMode", ditherMode.name());
                try { ditherMode = DitherMode.valueOf(dm); } catch (IllegalArgumentException ignored) {}
                brightness = parseFloat(p.getProperty("brightness"), brightness);
                contrast = parseFloat(p.getProperty("contrast"), contrast);
                gamma = parseFloat(p.getProperty("gamma"), gamma);
                saturation = parseFloat(p.getProperty("saturation"), saturation);
            } catch (IOException ignored) {}
        }
        preferencesLoaded = true;
    }

    // Debounced saver state
    private static volatile Thread saveThread;

    public static void savePreferencesAsync() {
        pendingSave = true;
        long now = System.currentTimeMillis();
        if (now - lastSaveTime > 750) {
            savePreferences();
            return;
        }
        // Schedule a delayed save if one isn't already running
        if (saveThread == null || !saveThread.isAlive()) {
            saveThread = new Thread(() -> {
                try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                savePreferences();
            }, "FilterPrefsSaver");
            saveThread.setDaemon(true);
            try { saveThread.start(); } catch (IllegalThreadStateException ignored) {}
        }
    }

    private static synchronized void savePreferences() {
        if (!pendingSave) return;
        pendingSave = false;
        lastSaveTime = System.currentTimeMillis();
        Properties p = new Properties();
        p.setProperty("colorMode", colorMode.name());
        p.setProperty("scanlines", Boolean.toString(scanlines));
        p.setProperty("scanlinesIntensity", Float.toString(scanlinesIntensity));
        p.setProperty("vignette", Boolean.toString(vignette));
        p.setProperty("vignetteIntensity", Float.toString(vignetteIntensity));
    // Bloom
    p.setProperty("bloom", Boolean.toString(bloom));
    p.setProperty("bloomThreshold", Float.toString(bloomThreshold));
    p.setProperty("bloomIntensity", Float.toString(bloomIntensity));
    p.setProperty("bloomRadius", Integer.toString(bloomRadius));
    // Palette & Dither
    p.setProperty("paletteMode", paletteMode.name());
    p.setProperty("ditherMode", ditherMode.name());
        p.setProperty("brightness", Float.toString(brightness));
        p.setProperty("contrast", Float.toString(contrast));
        p.setProperty("gamma", Float.toString(gamma));
        p.setProperty("saturation", Float.toString(saturation));
        File f = getPreferencesFile();
        try (FileOutputStream out = new FileOutputStream(f)) {
            p.store(out, "JarEngine Filters Preferences");
        } catch (IOException ignored) {}
    }

    private static float parseFloat(String s, float def) {
        try { return s == null ? def : Float.parseFloat(s); } catch (Throwable t) { return def; }
    }

    // ======= Defaults reset =======
    public static synchronized void resetToDefaults() {
        // Reset all filters to default values
        colorMode = ColorMode.FULL_COLOR;
        scanlines = false;
        scanlinesIntensity = 0.12f;
        vignette = false;
        vignetteIntensity = 0.2f;
        bloom = false;
        bloomThreshold = 0.7f;
        bloomIntensity = 0.6f;
        bloomRadius = 2;
        paletteMode = PaletteMode.NONE;
        ditherMode = DitherMode.NONE;
        brightness = 1.0f;
        contrast = 1.0f;
        gamma = 1.0f;
        saturation = 1.0f;
        // Persist the reset values
        savePreferences();
        // Trigger display refresh to apply changes immediately
        triggerDisplayRefresh();
    }

    /**
     * Trigger a display refresh to apply filter changes immediately
     */
    private static void triggerDisplayRefresh() {
        try {
            // Try to get the device display and trigger a repaint
            if (org.je.device.DeviceFactory.getDevice() != null) {
                org.je.device.DeviceDisplay display = org.je.device.DeviceFactory.getDevice().getDeviceDisplay();
                if (display instanceof org.je.device.j2se.J2SEDeviceDisplay) {
                    org.je.device.j2se.J2SEDeviceDisplay j2seDisplay = (org.je.device.j2se.J2SEDeviceDisplay) display;
                    // Repaint the entire display area
                    j2seDisplay.repaint(0, 0, j2seDisplay.getFullWidth(), j2seDisplay.getFullHeight());
                }
            }
            
            // Also try to refresh the Swing display component
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    // Force repaint of all windows to ensure filter changes are visible
                    for (java.awt.Window window : java.awt.Window.getWindows()) {
                        window.repaint();
                    }
                } catch (Throwable ignored) {}
            });
        } catch (Throwable ignored) {
            // Ignore errors - display refresh is best effort
        }
    }
}
