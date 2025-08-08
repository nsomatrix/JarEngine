# JarEngine Proxy Functionality

This document describes the proxy functionality that has been implemented in the JarEngine emulator to route all network connections through a configurable proxy server.

## Features

- **Full Proxy Support**: Routes HTTP, HTTPS, Socket, and SSL connections through proxy
- **Authentication Support**: Supports proxy authentication with username/password
- **Protocol Selection**: Choose which protocols to route through proxy
- **Easy Configuration**: User-friendly GUI for proxy settings
- **Connection Testing**: Built-in proxy connection testing
- **Status Indicator**: Shows proxy status in the emulator status bar
- **Persistent Settings**: Proxy settings are saved and restored between sessions

## How to Use

### 1. Accessing the Proxy Tool

1. Start the JarEngine emulator
2. Go to **Tools** → **Proxy** in the menu bar
3. The Proxy Configuration window will open

### 2. Configuring Proxy Settings

#### Basic Settings
- **Enable Proxy**: Check this to enable proxy functionality
- **Host**: Enter your proxy server hostname or IP address (default: 127.0.0.1)
- **Port**: Enter your proxy server port (default: 8080)

#### Authentication (Optional)
- **Use Authentication**: Check if your proxy requires authentication
- **Username**: Enter your proxy username
- **Password**: Enter your proxy password

#### Protocol Settings
- **HTTP**: Route HTTP connections through proxy
- **HTTPS**: Route HTTPS connections through proxy  
- **Socket/SOCKS**: Route Socket and SOCKS connections through proxy

### 3. Testing the Connection

1. Click the **Test Connection** button to verify your proxy settings
2. The tool will attempt to connect to your proxy server
3. You'll see a success or failure message

### 4. Applying Settings

1. Click **Apply** to save your settings
2. The settings will be saved to `~/.jarengine/proxy.properties`
3. Some changes may require restarting the emulator to take full effect

## Status Indicator

When proxy is enabled, you'll see the proxy status in the emulator's status bar at the bottom:
```
Status [PROXY: 127.0.0.1:8080]
```

## Technical Details

### Supported Protocols

- **HTTP**: Uses HTTP proxy for HTTP connections
- **HTTPS**: Uses HTTP proxy for HTTPS connections
- **Socket**: Uses SOCKS proxy for TCP socket connections
- **SSL**: Uses SOCKS proxy for SSL connections

### Configuration File

Proxy settings are stored in: `~/.jarengine/proxy.properties`

Example configuration:
```properties
# Enable/disable proxy
proxy.enabled=true

# Proxy server settings
proxy.host=127.0.0.1
proxy.port=8080

# Authentication settings
proxy.username=myuser
proxy.password=mypass
proxy.auth=true

# Protocol settings
proxy.http.enabled=true
proxy.https.enabled=true
proxy.socket.enabled=true
```

### Implementation Details

The proxy functionality is implemented through:

1. **ProxyConfig**: Singleton class managing proxy settings
2. **ProxyConnection Classes**: Protocol-specific connection wrappers
3. **ConnectorImpl Integration**: Modified to use proxy connections when enabled
4. **ProxyTool UI**: Swing-based configuration interface

### Connection Flow

When proxy is enabled:
1. MIDlet requests connection (e.g., `Connector.open("http://example.com")`)
2. ConnectorImpl checks if proxy is enabled
3. If enabled, uses ProxyConnection class instead of regular Connection
4. ProxyConnection routes through configured proxy server
5. Connection is established through proxy

## Troubleshooting

### Common Issues

1. **Connection Timeout**
   - Verify proxy server is running and accessible
   - Check host and port settings
   - Test with a simple HTTP client first

2. **Authentication Failed**
   - Verify username and password are correct
   - Check if proxy requires authentication
   - Try connecting without authentication first

3. **Protocol Not Working**
   - Ensure the protocol is enabled in settings
   - Check if your proxy supports the protocol
   - Verify proxy server configuration

### Testing

You can test the proxy functionality by:

1. Running the test class: `org.je.app.ProxyTest`
2. Using a MIDlet that makes network connections
3. Checking the emulator logs for connection details

### Logs

Proxy-related logs can be found in the emulator's log console:
- Go to **Tools** → **Log console** to view logs
- Look for proxy connection attempts and errors

## Security Considerations

- Proxy passwords are stored in plain text in the configuration file
- Consider file permissions on the configuration file
- Proxy settings apply to all MIDlets running in the emulator
- Disable proxy when not needed for security

## Examples

### HTTP Connection Example
```java
// This will automatically route through proxy if enabled
HttpConnection conn = (HttpConnection) Connector.open("http://example.com");
```

### HTTPS Connection Example
```java
// This will automatically route through proxy if enabled
HttpsConnection conn = (HttpsConnection) Connector.open("https://example.com");
```

### Socket Connection Example
```java
// This will automatically route through proxy if enabled
SocketConnection conn = (SocketConnection) Connector.open("socket://example.com:80");
```

## Requirements

- Java 8 or higher
- Network access to proxy server
- Proxy server supporting required protocols

## Support

For issues or questions about the proxy functionality:
1. Check the troubleshooting section above
2. Review the emulator logs
3. Test with a simple proxy server first
4. Verify your proxy server configuration 