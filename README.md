# Burp Suite MCP Server Extension

## Overview

Integrate Burp Suite with AI Clients using the Model Context Protocol (MCP).

For more information about the protocol visit: [modelcontextprotocol.io](https://modelcontextprotocol.io/)

## Features

- Connect Burp Suite to AI clients through MCP
- Automatic installation for Claude Desktop
- Comes with packaged Stdio MCP proxy server

## Usage

- Install the extension in Burp Suite
- Configure your Burp MCP server in the extension settings
- Configure your MCP client to use the Burp SSE MCP server or stdio proxy
- Interact with Burp through your client!

## Installation

### Prerequisites

Ensure that the following prerequisites are met before building and installing the extension:

1. **Java**: Java must be installed and available in your system's PATH. You can verify this by running `java --version` in your terminal.
2. **jar Command**: The `jar` command must be executable and available in your system's PATH. You can verify this by running `jar --version` in your terminal. This is required for building and installing the extension.

### Building the Extension

1. **Clone the Repository**: Obtain the source code for the MCP Server Extension.
   ```
   git clone https://github.com/PortSwigger/mcp-server.git
   ```

2. **Navigate to the Project Directory**: Move into the project's root directory.
   ```
   cd mcp-server
   ```

3. **Build the JAR File**: Use Gradle to build the extension.
   ```
   ./gradlew embedProxyJar
   ```

   This command compiles the source code and packages it into a JAR file located in `build/libs/burp-mcp-all.jar`.

### Loading the Extension into Burp Suite

1. **Open Burp Suite**: Launch your Burp Suite application.
2. **Access the Extensions Tab**: Navigate to the `Extensions` tab.
3. **Add the Extension**:
    - Click on `Add`.
    - Set `Extension Type` to `Java`.
    - Click `Select file ...` and choose the JAR file built in the previous step.
    - Click `Next` to load the extension.

Upon successful loading, the MCP Server Extension will be active within Burp Suite.

## Configuration

### Configuring the Extension
Configuration for the extension is done through the Burp Suite UI in the `MCP` tab.
- **Toggle the MCP Server**: The `Enabled` checkbox controls whether the MCP server is active.
- **Enable config editing**: The `Enable tools that can edit your config` checkbox allows the MCP server to expose tools which can edit Burp configuration files.
- **Advanced options**: You can configure the port and host for the MCP server. By default, it listens on `http://127.0.0.1:9876`.

### Codex plugin

The desktop-only plugin starts its bundled Java stdio proxy, which connects to
Burp's local SSE endpoint at `http://127.0.0.1:9876`. Install Java 21 or newer
and make `java` available on your PATH. Burp and the updated extension must be
running on the same computer. Other MCP clients can still use Streamable HTTP
at `http://127.0.0.1:9876/mcp` directly.

1. Build the updated extension and plugin ZIP from this repository:
   ```bash
   ./gradlew embedProxyJar codexPluginZip
   ```
2. Load the updated extension JAR into Burp Suite and enable its MCP server.
3. Add this repository as a local Codex plugin marketplace:
   ```bash
   codex plugin marketplace add /absolute/path/to/mcp-server
   ```
4. Restart Codex desktop, open Plugins, select **Burp Suite Local**, and install
   **Burp Suite**. Start a new chat and ask Codex to list Burp tools.

The repository marketplace is `.agents/plugins/marketplace.json`; plugin sources
are in `plugins/burp-suite`. The distributable ZIP is
`build/distributions/burp-suite-codex-plugin.zip`. It contains the plugin
manifest and the bundled stdio proxy, but not Burp or the extension. If you
change Burp's port, update the `--sse-url` argument in
`plugins/burp-suite/mcp.json` before reinstalling or packaging the plugin.
Keep the server bound to loopback. Existing Burp approval settings still apply
to tool calls.

#### Submit to the OpenAI Plugin Directory

This is a [desktop-only local MCP plugin](https://developers.openai.com/plugins/deploy/desktop-only).
The repository marketplace is for local testing and does not publish the plugin
to the public directory.

1. Ask the OpenAI representative to confirm local MCP partner eligibility and
   allowlist the submitting PortSwigger OpenAI Platform organization ID.
2. Build `./gradlew codexPluginZip` and test an installation of that ZIP in a
   clean ChatGPT desktop environment with Burp running. Check tool discovery,
   read tools, a Burp-approved HTTP request, and a denied request.
3. In the [plugin submission portal](https://platform.openai.com/plugins),
   create a plugin using **Skills only**, then upload the ZIP. Approved local
   MCP submissions do not need a skill. Resolve automated findings and supply
   the required publisher identity, review cases, and demonstration materials.
4. Submit for review. After approval, publish the plugin from the portal.

The ZIP uses the default Burp port. Update `mcp.json` and rebuild it if the
review environment uses another port. The public listing requires a verified
PortSwigger publisher identity and an allowlisted organization; building the
ZIP alone does not make it public.

### Claude Desktop Client

To fully utilize the MCP Server Extension with Claude, you need to configure your Claude client settings appropriately.
The extension has an installer which will automatically configure the client settings for you.

1. Currently, Claude Desktop only support STDIO MCP Servers
   for the service it needs.
   This approach isn't ideal for desktop apps like Burp, so instead, Claude will start a proxy server that points to the
   Burp instance,  
   which hosts a web server at a known port (`localhost:9876`).

2. **Configure Claude to use the Burp MCP server**  
   You can do this in one of two ways:

    - **Option 1: Run the installer from the extension**
      This will add the Burp MCP server to the Claude Desktop config.

    - **Option 2: Manually edit the config file**  
      Open the file located at `~/Library/Application Support/Claude/claude_desktop_config.json`,
      and replace or update it with the following:
      ```json
      {
        "mcpServers": {
          "burp": {
            "command": "<path to Java executable packaged with Burp>",
            "args": [
                "-jar",
                "/path/to/mcp/proxy/jar/mcp-proxy-all.jar",
                "--sse-url",
                "<your Burp MCP server URL configured in the extension>"
            ]
          }
        }
      }
      ```

3. **Restart Claude Desktop** - assuming Burp is running with the extension loaded.

## Manual installations
If you want to install the MCP server manually you can either use the extension's SSE server directly or the packaged
Stdio proxy server.

### SSE MCP Server
To use the SSE server directly, provide the configured server URL to your MCP client:
```
http://127.0.0.1:9876
```

### Stdio MCP Proxy Server
The source code for the proxy server can be found here: [MCP Proxy Server](https://github.com/PortSwigger/mcp-proxy)

In order to support MCP Clients which only support Stdio MCP Servers, the extension comes packaged with a proxy server for
passing requests to the SSE MCP server extension.

If you want to use the Stdio proxy server you can use the extension's installer option to extract the proxy server jar.
Once you have the jar you can add the following command and args to your client configuration:
```
/path/to/packaged/burp/java -jar /path/to/proxy/jar/mcp-proxy-all.jar --sse-url http://127.0.0.1:9876
```

If you modify the proxy source, rebuild and copy it into this project before packaging the extension:
```bash
# From mcp-proxy
./gradlew shadowJar
cp build/libs/mcp-proxy-all.jar /path/to/mcp-server/plugins/burp-suite/server/mcp-proxy-all.jar

# From mcp-server
./gradlew embedProxyJar
```

### Creating / modifying tools

Tools are defined in `src/main/kotlin/net/portswigger/mcp/tools/Tools.kt`. To define new tools, create a new serializable
data class with the required parameters which will come from the LLM.

The tool name is auto-derived from its parameters data class. A description is also needed for the LLM. You can return
a string or a `List<ContentBlock>` to provide data back to the LLM.

Extend the Paginated interface to add auto-pagination support.
