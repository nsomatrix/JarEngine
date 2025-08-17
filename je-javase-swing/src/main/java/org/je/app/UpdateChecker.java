package org.je.app;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * Industry-standard update checker for JarEngine
 * Handles checking for updates, downloading, and applying them
 * 
 * Cross-platform compatible update system that works on:
 * - Windows (using .bat scripts)
 * - macOS (using bash scripts)
 * - Linux (using bash scripts)
 * - Unknown systems (fallback to bash)
 */
public class UpdateChecker {

    private static final String UPDATE_URL = "https://raw.githubusercontent.com/nsomatrix/JarEngine/main/version.txt";
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
        connection.setRequestProperty("User-Agent", "JarEngine-Updater/1.0");

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
     * Download the update JAR file with progress tracking
     */
    public static void downloadUpdate(String version, File destinationFile) throws IOException {
        String downloadUrl = DOWNLOAD_BASE_URL + "v" + version + "/JarEngine-" + version + ".jar";
        
        URL url = new URL(downloadUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000); // 30 second timeout for download
        connection.setReadTimeout(30000);
        connection.setRequestProperty("User-Agent", "JarEngine-Updater/1.0");

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to download update. HTTP error code: " + responseCode + 
                               " for URL: " + downloadUrl);
        }

        // Get content length for progress tracking
        int contentLength = connection.getContentLength();
        
        try (InputStream in = connection.getInputStream()) {
            if (contentLength > 0) {
                // Use buffered copy with progress tracking
                copyWithProgress(in, destinationFile, contentLength);
            } else {
                // Fallback to simple copy if content length unknown
                Files.copy(in, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        
        // Verify the downloaded file
        if (!destinationFile.exists() || destinationFile.length() == 0) {
            throw new IOException("Downloaded file is invalid or empty: " + destinationFile.getAbsolutePath());
        }
        
        // Verify it's a valid JAR file (check for ZIP header)
        if (!isValidJarFile(destinationFile)) {
            throw new IOException("Downloaded file is not a valid JAR file");
        }
    }

    /**
     * Copy input stream to file with progress tracking
     */
    private static void copyWithProgress(InputStream in, File destinationFile, int contentLength) throws IOException {
        try (FileOutputStream out = new FileOutputStream(destinationFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            int totalBytesRead = 0;
            
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                
                // Log progress every 100KB
                if (totalBytesRead % (100 * 1024) == 0) {
                    int progress = (int) ((totalBytesRead * 100.0) / contentLength);
                    System.out.println("Download progress: " + progress + "%");
                }
            }
        }
    }

    /**
     * Validate that a file is a valid JAR file
     */
    private static boolean isValidJarFile(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[4];
            if (fis.read(header) != 4) {
                return false;
            }
            // Check for ZIP file header (PK\x03\x04)
            return header[0] == 0x50 && header[1] == 0x4B && 
                   header[2] == 0x03 && header[3] == 0x04;
        } catch (IOException e) {
            return false;
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
        
        // Check available disk space
        long requiredSpace = downloadedJar.length();
        long availableSpace = currentJar.getParentFile().getUsableSpace();
        if (availableSpace < requiredSpace) {
            throw new IOException("Insufficient disk space. Required: " + requiredSpace + 
                               " bytes, Available: " + availableSpace + " bytes");
        }

        // Try direct Java-based update first (more reliable)
        try {
            System.out.println("Attempting direct Java-based update...");
            applyUpdateDirectly(downloadedJar, currentJar, newJarFile, javaBin, latestVersion);
        } catch (Exception e) {
            System.out.println("Direct update failed, falling back to script-based approach: " + e.getMessage());
            // Fall back to script-based approach
            applyUpdateWithScript(downloadedJar, currentJar, newJarFile, javaBin, latestVersion);
        }
    }
    
    /**
     * Apply update directly using Java file operations (more reliable)
     */
    private static void applyUpdateDirectly(File downloadedJar, File currentJar, File newJarFile, String javaBin, String latestVersion) throws IOException {
        System.out.println("Starting direct update process...");
        System.out.println("Current JAR: " + currentJar.getAbsolutePath());
        System.out.println("Downloaded JAR: " + downloadedJar.getAbsolutePath());
        System.out.println("New JAR: " + newJarFile.getAbsolutePath());
        
        // First, try to move the downloaded file to the new location
        if (Files.exists(newJarFile.toPath())) {
            try {
                System.out.println("Removing existing new JAR file...");
                Files.delete(newJarFile.toPath());
            } catch (IOException e) {
                System.out.println("Warning: Could not remove existing new JAR: " + e.getMessage());
            }
        }
        
        // Try to move the downloaded file
        try {
            System.out.println("Moving downloaded JAR to new location...");
            Files.move(downloadedJar.toPath(), newJarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Downloaded JAR moved successfully");
        } catch (IOException e) {
            System.out.println("Move failed, trying copy then delete: " + e.getMessage());
            // If move fails, try copy then delete
            Files.copy(downloadedJar.toPath(), newJarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            downloadedJar.delete(); // Clean up temp file
            System.out.println("Downloaded JAR copied successfully");
        }
        
        // Verify the new file exists and is readable
        if (!newJarFile.exists() || !newJarFile.canRead()) {
            throw new IOException("New JAR file is not accessible after copy/move operation");
        }
        
        System.out.println("New JAR file verified successfully");
        
        // Now try to delete the old JAR
        if (Files.exists(currentJar.toPath())) {
            try {
                System.out.println("Removing old JAR file...");
                Files.delete(currentJar.toPath());
                System.out.println("Old JAR removed successfully");
            } catch (IOException e) {
                System.out.println("Warning: Could not remove old JAR: " + e.getMessage());
                // This is not critical, continue with the update
            }
        }
        
        // Start the new version
        System.out.println("Starting new application...");
        ProcessBuilder pb = new ProcessBuilder(javaBin, "-jar", newJarFile.getAbsolutePath());
        pb.directory(currentJar.getParentFile());
        
        // Set environment variables for better compatibility
        Map<String, String> env = pb.environment();
        env.put("JARENGINE_UPDATED", "true");
        env.put("JARENGINE_OLD_VERSION", getCurrentVersion());
        env.put("JARENGINE_NEW_VERSION", latestVersion);
        
        Process process = pb.start();
        System.out.println("New application started successfully");
        
        // Exit the current application
        System.exit(0);
    }
    
    /**
     * Apply update using external scripts for cross-platform compatibility
     */
    private static void applyUpdateWithScript(File downloadedJar, File currentJar, File newJarFile, String javaBin, String latestVersion) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("win");

        // Create a log file to track what happens
        File logFile = File.createTempFile("updater-log", ".txt");
        logFile.deleteOnExit();

        File tempScript;
        String scriptContent;
        String[] command;

        if (isWindows) {
            tempScript = File.createTempFile("updater", ".bat");
            scriptContent =
                    "@echo off\n" +
                    "setlocal enabledelayedexpansion\n" +
                    "echo Starting update process... > \"" + logFile.getAbsolutePath() + "\"\n" +
                    "echo Current JAR: " + currentJar.getAbsolutePath() + " >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "echo Downloaded JAR: " + downloadedJar.getAbsolutePath() + " >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "echo New JAR: " + newJarFile.getAbsolutePath() + " >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "echo Waiting for app to exit... >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "timeout /t 5 /nobreak > NUL\n" +
                    "echo Checking if current JAR exists... >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "if exist \"" + currentJar.getAbsolutePath() + "\" (\n" +
                    "    echo Attempting to remove old JAR... >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "    del /F /Q \"" + currentJar.getAbsolutePath() + "\" 2>NUL\n" +
                    "    if exist \"" + currentJar.getAbsolutePath() + "\" (\n" +
                    "        echo Old JAR still exists, trying force delete... >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "        del /F /Q \"" + currentJar.getAbsolutePath() + "\" 2>NUL\n" +
                    "    )\n" +
                    "    if not exist \"" + currentJar.getAbsolutePath() + "\" (\n" +
                    "        echo Old JAR removed successfully >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "    ) else (\n" +
                    "        echo Warning: Could not remove old JAR >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "    )\n" +
                    ") else (\n" +
                    "    echo Old JAR not found >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    ")\n" +
                    "echo Checking if downloaded JAR exists... >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "if exist \"" + downloadedJar.getAbsolutePath() + "\" (\n" +
                    "    echo Moving downloaded JAR to new location... >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "    move /Y \"" + downloadedJar.getAbsolutePath() + "\" \"" + newJarFile.getAbsolutePath() + "\" 2>NUL\n" +
                    "    if exist \"" + newJarFile.getAbsolutePath() + "\" (\n" +
                    "        echo New JAR created successfully >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "        echo Starting new application... >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "        start \"\" /B \"" + javaBin + "\" -jar \"" + newJarFile.getAbsolutePath() + "\"\n" +
                    "        echo New application started >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "    ) else (\n" +
                    "        echo Failed to create new JAR >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "    )\n" +
                    ") else (\n" +
                    "    echo Downloaded JAR not found >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    ")\n" +
                    "echo Cleaning up script... >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "del \"" + tempScript.getAbsolutePath() + "\"\n" +
                    "echo Update process completed >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "exit\n";
            command = new String[]{"cmd.exe", "/c", tempScript.getAbsolutePath()};
        } else { // Linux, macOS, or other Unix-like systems
            tempScript = File.createTempFile("updater", ".sh");
            tempScript.setExecutable(true);
            scriptContent =
                    "#!/bin/bash\n" +
                    "set -e\n" +
                    "echo \"Starting update process...\" > \"" + logFile.getAbsolutePath() + "\"\n" +
                    "echo \"Current JAR: " + currentJar.getAbsolutePath() + "\" >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "echo \"Downloaded JAR: " + downloadedJar.getAbsolutePath() + "\" >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "echo \"New JAR: " + newJarFile.getAbsolutePath() + "\" >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "echo \"Waiting for app to exit...\" >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "sleep 5\n" +
                    "echo \"Checking if current JAR exists...\" >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "if [ -f \"" + currentJar.getAbsolutePath() + "\" ]; then\n" +
                    "    echo \"Removing old JAR...\" >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "    rm -f \"" + currentJar.getAbsolutePath() + "\" 2>/dev/null || true\n" +
                    "    if [ -f \"" + currentJar.getAbsolutePath() + "\" ]; then\n" +
                    "        echo \"Warning: Could not remove old JAR, trying force delete...\" >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "        rm -f \"" + currentJar.getAbsolutePath() + "\" 2>/dev/null || true\n" +
                    "    fi\n" +
                    "    if [ ! -f \"" + currentJar.getAbsolutePath() + "\" ]; then\n" +
                    "        echo \"Old JAR removed successfully\" >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "    else\n" +
                    "        echo \"Warning: Could not remove old JAR\" >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "    fi\n" +
                    "else\n" +
                    "    echo \"Old JAR not found\" >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "fi\n" +
                    "echo \"Checking if downloaded JAR exists...\" >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "if [ -f \"" + downloadedJar.getAbsolutePath() + "\" ]; then\n" +
                    "    echo \"Moving downloaded JAR to new location...\" >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "    mv \"" + downloadedJar.getAbsolutePath() + "\" \"" + newJarFile.getAbsolutePath() + "\" 2>/dev/null || true\n" +
                    "    if [ -f \"" + newJarFile.getAbsolutePath() + "\" ]; then\n" +
                    "        echo \"New JAR created successfully\" >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "        echo \"Starting new application...\" >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "        nohup \"" + javaBin + "\" -jar \"" + newJarFile.getAbsolutePath() + "\" > /dev/null 2>&1 &\n" +
                    "        echo \"New application started\" >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "    else\n" +
                    "        echo \"Failed to create new JAR\" >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "    fi\n" +
                    "else\n" +
                    "    echo \"Downloaded JAR not found\" >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "fi\n" +
                    "echo \"Cleaning up script...\" >> \"" + logFile.getAbsolutePath() + "\"\n" +
                    "rm -f \"" + tempScript.getAbsolutePath() + "\"\n" +
                    "echo \"Update process completed\" >> \"" + logFile.getAbsolutePath() + "\"\n";
            command = new String[]{"/bin/bash", tempScript.getAbsolutePath()};
        }

        try (PrintWriter pw = new PrintWriter(tempScript, "UTF-8")) {
            pw.println(scriptContent);
        }
        
        // Log the script content for debugging
        System.out.println("Update script created: " + tempScript.getAbsolutePath());
        System.out.println("Log file will be created at: " + logFile.getAbsolutePath());
        
        // Execute the script and exit the current application
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(currentJar.getParentFile());
        
        // Set environment variables for better compatibility
        Map<String, String> env = pb.environment();
        env.put("JARENGINE_UPDATED", "true");
        env.put("JARENGINE_OLD_VERSION", getCurrentVersion());
        env.put("JARENGINE_NEW_VERSION", latestVersion);
        
        System.out.println("Starting update script...");
        Process process = pb.start();
        
        // Give the script more time to start and execute
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Check if the log file was created and show its contents
        if (logFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                String line;
                System.out.println("Update script log:");
                while ((line = reader.readLine()) != null) {
                    System.out.println("  " + line);
                }
            } catch (IOException e) {
                System.out.println("Could not read log file: " + e.getMessage());
            }
        }
        
        System.out.println("Exiting application for update...");
        System.exit(0);
    }

    /**
     * Get the Java executable path in a cross-platform way
     */
    private static String getJavaExecutablePath() {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("win");
        boolean isMac = os.contains("mac");
        
        // Try multiple approaches to find Java
        String javaBin = null;
        
        // Approach 1: Use java.home system property
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            javaBin = javaHome + File.separator + "bin" + File.separator + "java";
            if (isWindows) {
                javaBin += ".exe";
            }
            
            // Verify the java executable exists
            File javaFile = new File(javaBin);
            if (javaFile.exists() && javaFile.canExecute()) {
                return javaBin;
            }
        }
        
        // Approach 2: Try JAVA_HOME environment variable
        String javaHomeEnv = System.getenv("JAVA_HOME");
        if (javaHomeEnv != null) {
            javaBin = javaHomeEnv + File.separator + "bin" + File.separator + "java";
            if (isWindows) {
                javaBin += ".exe";
            }
            
            File javaFile = new File(javaBin);
            if (javaFile.exists() && javaFile.canExecute()) {
                return javaBin;
            }
        }
        
        // Approach 3: macOS specific paths
        if (isMac) {
            String[] macJavaPaths = {
                "/Library/Java/JavaVirtualMachines/jdk-*/Contents/Home/bin/java",
                "/System/Library/Java/JavaVirtualMachines/*.jdk/Contents/Home/bin/java",
                "/usr/libexec/java_home"
            };
            
            for (String path : macJavaPaths) {
                try {
                    if (path.contains("*")) {
                        // Handle wildcard paths
                        File dir = new File(path.substring(0, path.lastIndexOf("/")));
                        if (dir.exists()) {
                            File[] files = dir.listFiles((d, name) -> name.startsWith("jdk-") || name.endsWith(".jdk"));
                            if (files != null && files.length > 0) {
                                String javaPath = files[0].getAbsolutePath() + "/Contents/Home/bin/java";
                                if (new File(javaPath).exists()) {
                                    return javaPath;
                                }
                            }
                        }
                    } else if (path.contains("java_home")) {
                        // Use java_home utility
                        try {
                            Process process = Runtime.getRuntime().exec("java_home");
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                                String home = reader.readLine();
                                if (home != null) {
                                    String javaPath = home + "/bin/java";
                                    if (new File(javaPath).exists()) {
                                        return javaPath;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Ignore and continue
                        }
                    }
                } catch (Exception e) {
                    // Ignore and continue
                }
            }
        }
        
        // Approach 4: Fallback to PATH
        try {
            Process process = Runtime.getRuntime().exec(isWindows ? "where java" : "which java");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String javaPath = reader.readLine();
                if (javaPath != null && !javaPath.trim().isEmpty()) {
                    return javaPath.trim();
                }
            }
        } catch (Exception e) {
            // Ignore and continue
        }
        
        // Final fallback: just "java" and hope it's in PATH
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