#!/bin/bash

# Variables
JAR_URL="https://github.com/nsomatrix/JarEngine/releases/download/v1.0.0/JarEngine-1.0.0.jar"
DEST_DIR="$HOME/Downloads/JarEngine"
JAR_FILE="$DEST_DIR/JarEngine-1.0.0.jar"
LAUNCHER="$HOME/Desktop/JarEngine.command"

# Create destination directory
mkdir -p "$DEST_DIR"

# Download JAR file
echo "Downloading JarEngine..."
curl -L -o "$JAR_FILE" "$JAR_URL"

# Create launcher script on Desktop
cat > "$LAUNCHER" <<EOL
#!/bin/bash
cd "$DEST_DIR"
exec java -jar "$JAR_FILE"
exit 0
EOL

# Make launcher executable
chmod +x "$LAUNCHER"

echo "Setup complete!"
echo "You can run JarEngine by double-clicking 'JarEngine.command' on your Desktop."
