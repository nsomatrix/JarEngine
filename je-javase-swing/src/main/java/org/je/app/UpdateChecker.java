package org.je.app;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * Update checker for JarEngine
 * Handles checking for updates, downloading, and applying them
 * 
 * Cross-platform compatible update system that works on:
 * - Windows (using .bat scripts)
 * - macOS (using bash scripts)
 * - Linux (using bash scripts)
 * - Unknown systems (fallback to bash)
 */
public class UpdateChecker {

    private static final String UPDATE_URL = "https://raw.githubusercontent.com/nsomatrix/JarEngine/refs/heads/main/version.txt";
    private static final String DOWNLOAD_BASE_URL = "https://github.com/nsomatrix/JarEngine/releases/download/";

    /**
     * Get the latest version from the remote repository
     */
    public static String getLatestVersion() throws Exception {
        URL url = new URL(UPDATE_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000); // 10 second timeout
        connection.setReadTimeout(10000);

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                return content.toString().trim();
            }
        } else {
            throw new RuntimeException("Failed to fetch update information. HTTP error code: " + responseCode);
        }
    }

    /**
     * Check if an update is available
     */
    public static boolean isUpdateAvailable(String currentVersion, String latestVersion) {
        if (currentVersion == null || currentVersion.equals("n/a") || latestVersion == null) {
            return false;
        }
        return latestVersion.compareTo(currentVersion) > 0;
    }

    /**
     * Download the update JAR file
     */
    public static void downloadUpdate(String version, File destinationFile) throws IOException {
        String downloadUrl = DOWNLOAD_BASE_URL + "v" + version + "/JarEngine-" + version + ".jar";
        
        URL url = new URL(downloadUrl);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000); // 30 second timeout for download
        connection.setReadTimeout(30000);

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to download update. HTTP error code: " + responseCode);
        }

        try (InputStream in = connection.getInputStream()) {
            Files.copy(in, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        
        // Verify the downloaded file
        if (!destinationFile.exists()) {
            throw new IOException("Downloaded file does not exist after copy operation");
        }
    }

    /**
     * Apply the update and restart the application
     */
    public static void applyUpdateAndRestart(File downloadedJar, String latestVersion) throws IOException {
        String javaBin = getJavaExecutablePath();
        File currentJar;

        try {
            currentJar = new File(org.je.app.Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (java.net.URISyntaxException e) {
            throw new IOException("Failed to get current JAR location: " + e.getMessage(), e);
        }

        // Verify the downloaded JAR exists and is readable
        if (!downloadedJar.exists() || !downloadedJar.canRead()) {
            throw new IOException("Downloaded JAR file is not accessible: " + downloadedJar.getAbsolutePath());
        }

        // Determine the new JAR filename
        String newJarName = "JarEngine-" + latestVersion + ".jar";
        File newJarFile = new File(currentJar.getParentFile(), newJarName);

        // Check if we have write permissions to the target directory
        if (!currentJar.getParentFile().canWrite()) {
            throw new IOException("No write permission to directory: " + currentJar.getParentFile().getAbsolutePath());
        }
        
        // Try direct Java-based update first
        try {
            // Check available disk space
            long requiredSpace = downloadedJar.length();
            long availableSpace = currentJar.getParentFile().getUsableSpace();
            if (availableSpace < requiredSpace) {
                throw new IOException("Insufficient disk space. Required: " + requiredSpace + " bytes, Available: " + availableSpace + " bytes");
            }
            
            // First, try to move the downloaded file to the new location
            if (Files.exists(newJarFile.toPath())) {
                try {
                    Files.delete(newJarFile.toPath());
                } catch (IOException e) {
                    // Continue anyway, the move operation should overwrite it
                }
            }
            
            // Try to move the downloaded file
            try {
                Files.move(downloadedJar.toPath(), newJarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                // If move fails, try copy then delete
                Files.copy(downloadedJar.toPath(), newJarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                downloadedJar.delete(); // Clean up temp file
            }
            
            // Verify the new file exists and is readable
            if (!newJarFile.exists() || !newJarFile.canRead()) {
                throw new IOException("New JAR file is not accessible after copy/move operation");
            }
            
            // Now try to delete the old JAR
            if (Files.exists(currentJar.toPath())) {
                try {
                    Files.delete(currentJar.toPath());
                } catch (IOException e) {
                    // This is not critical, continue with the update
                }
            }
            
            // Start the new version
            ProcessBuilder pb = new ProcessBuilder(javaBin, "-jar", newJarFile.getAbsolutePath());
            pb.directory(currentJar.getParentFile());
            
            // Set environment variables for better compatibility
            Map<String, String> env = pb.environment();
            env.put("JARENGINE_UPDATED", "true");
            env.put("JARENGINE_OLD_VERSION", getCurrentVersion());
            env.put("JARENGINE_NEW_VERSION", latestVersion);
            
            Process process = pb.start();
            
            System.exit(0);
            
        } catch (Exception e) {
            // Fall back to script-based approach
            applyUpdateWithScript(downloadedJar, currentJar, newJarFile, javaBin);
        }
    }
    
    /**
     * Fallback method using external scripts
     */
    private static void applyUpdateWithScript(File downloadedJar, File currentJar, File newJarFile, String javaBin) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("win");
        boolean isMac = os.contains("mac");
        boolean isLinux = os.contains("linux") || os.contains("unix");

        File tempScript;
        String scriptContent;
        String[] command;

        if (isWindows) {
            tempScript = File.createTempFile("updater", ".bat");
            scriptContent =
                    "@echo off\n" +
                    "timeout /t 3 /nobreak > NUL\n" +
                    "if exist \"" + currentJar.getAbsolutePath() + "\" (\n" +
                    "    del \"" + currentJar.getAbsolutePath() + "\"\n" +
                    ")\n" +
                    "if exist \"" + downloadedJar.getAbsolutePath() + "\" (\n" +
                    "    move /Y \"" + downloadedJar.getAbsolutePath() + "\" \"" + newJarFile.getAbsolutePath() + "\"\n" +
                    "    if exist \"" + newJarFile.getAbsolutePath() + "\" (\n" +
                    "        start \"\" /B \"" + javaBin + "\" -jar \"" + newJarFile.getAbsolutePath() + "\"\n" +
                    "    )\n" +
                    ")\n" +
                    "del \"" + tempScript.getAbsolutePath() + "\"\n" +
                    "exit\n";
            command = new String[]{"cmd.exe", "/c", tempScript.getAbsolutePath()};
        } else if (isMac || isLinux) { // macOS or Linux
            tempScript = File.createTempFile("updater", ".sh");
            tempScript.setExecutable(true);
            
            String shell = isMac ? "/bin/bash" : "/bin/bash";
            
            scriptContent =
                    "#!/bin/bash\n" +
                    "set -e\n" +
                    "sleep 3\n" +
                    "if [ -f \"" + currentJar.getAbsolutePath() + "\" ]; then\n" +
                    "    rm -f \"" + currentJar.getAbsolutePath() + "\"\n" +
                    "fi\n" +
                    "if [ -f \"" + downloadedJar.getAbsolutePath() + "\" ]; then\n" +
                    "    mv \"" + downloadedJar.getAbsolutePath() + "\" \"" + newJarFile.getAbsolutePath() + "\"\n" +
                    "    if [ -f \"" + newJarFile.getAbsolutePath() + "\" ]; then\n" +
                    "        nohup \"" + javaBin + "\" -jar \"" + newJarFile.getAbsolutePath() + "\" > /dev/null 2>&1 &\n" +
                    "    fi\n" +
                    "fi\n" +
                    "rm -f \"" + tempScript.getAbsolutePath() + "\"\n";
            command = new String[]{shell, tempScript.getAbsolutePath()};
        } else {
            // Fallback for unknown OS - try to use bash
            tempScript = File.createTempFile("updater", ".sh");
            tempScript.setExecutable(true);
            scriptContent =
                    "#!/bin/bash\n" +
                    "sleep 3\n" +
                    "if [ -f \"" + currentJar.getAbsolutePath() + "\" ]; then\n" +
                    "    rm -f \"" + currentJar.getAbsolutePath() + "\"\n" +
                    "fi\n" +
                    "if [ -f \"" + downloadedJar.getAbsolutePath() + "\" ]; then\n" +
                    "    mv \"" + downloadedJar.getAbsolutePath() + "\" \"" + newJarFile.getAbsolutePath() + "\"\n" +
                    "    if [ -f \"" + newJarFile.getAbsolutePath() + "\" ]; then\n" +
                    "        nohup \"" + javaBin + "\" -jar \"" + newJarFile.getAbsolutePath() + "\" > /dev/null 2>&1 &\n" +
                    "    fi\n" +
                    "fi\n" +
                    "rm -f \"" + tempScript.getAbsolutePath() + "\"\n";
            command = new String[]{"/bin/bash", tempScript.getAbsolutePath()};
        }

        try (PrintWriter pw = new PrintWriter(tempScript, "UTF-8")) {
            pw.println(scriptContent);
        }
        
        // Execute the script with output capture
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        
        // Set working directory to the JAR directory for better compatibility
        pb.directory(currentJar.getParentFile());
        
        Process process = pb.start();
        
        // Read the output in a separate thread before exiting
        Thread outputReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Silent output reading to prevent blocking
                }
            } catch (IOException e) {
                // Silent error handling
            }
        });
        outputReader.setDaemon(true);
        outputReader.start();
        
        // Give the script a moment to start, then exit
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.exit(0);
    }

    /**
     * Get the Java executable path in a cross-platform way
     */
    private static String getJavaExecutablePath() {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("win");
        
        String javaHome = System.getProperty("java.home");
        if (javaHome == null) {
            javaHome = System.getenv("JAVA_HOME");
        }
        
        if (javaHome == null) {
            // Fallback: try to find java in PATH
            return "java";
        }
        
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        if (isWindows) {
            javaBin += ".exe";
        }
        
        // Verify the java executable exists
        File javaFile = new File(javaBin);
        if (javaFile.exists() && javaFile.canExecute()) {
            return javaBin;
        }
        
        // If not found, fall back to just "java" and hope it's in PATH
        return "java";
    }
    
    /**
     * Get current version dynamically
     */
    private static String getCurrentVersion() {
        try {
            return org.je.app.util.BuildVersion.getVersion();
        } catch (Exception e) {
            return "unknown";
        }
    }
}