# Blynk IoT Dashboard — Svelte Edition with MCP Integration

This is an enhanced version of the Blynk IoT Dashboard that includes full **Model Context Protocol (MCP)** server integration for managing devices and templates programmatically.

## What's New: MCP Integration

### New Files Added

1. **`src/lib/mcp.ts`** — Complete MCP client library
   - Type-safe wrappers for all 11 MCP tools
   - `MCPManager` class for convenient API access
   - Full TypeScript support with interfaces

2. **`src/MCPIntegration.svelte`** — Standalone MCP UI component
   - Authentication with Blynk access token
   - Device search and management
   - Template creation and management
   - Create device/template forms
   - Real-time feedback with toast notifications

3. **`src/lib/mcp.examples.ts`** — Comprehensive examples
   - 15+ practical examples
   - Complete workflow demonstrations
   - Error handling patterns
   - Usage for each MCP tool

4. **`MCP_INTEGRATION.md`** — Detailed integration guide
   - Architecture overview
   - All 11 tool specifications
   - Usage examples
   - Security considerations
   - Troubleshooting guide

## MCP Tools Implemented

All 11 Blynk MCP tools are fully implemented:

| Tool | Description |
|------|-------------|
| `search_devices` | Find and list devices with filtering |
| `get_device` | Get detailed device information |
| `create_device` | Create new device under template |
| `update_datastream_value` | Update device datastream values |
| `get_all_templates` | List all templates |
| `get_template` | Get template details |
| `create_template` | Create new template |
| `create_datastream` | Add datastream to template |
| `edit_datastream` | Update datastream configuration |
| `create_event` | Add event to template |
| `edit_event` | Modify event settings |

## Quick Start

### 1. Get Your Access Token

1. Go to [Blynk Console](https://blynk.cloud)
2. Navigate to **Settings → API Tokens**
3. Copy your access token

### 2. Use the MCP Client

```typescript
import { MCPManager } from './lib/mcp'

const mcpManager = new MCPManager('your-access-token')

// Search devices
const devices = await mcpManager.searchDevices({ name: 'Pump' })

// Get all templates
const templates = await mcpManager.getAllTemplates()

// Create a device
const newDevice = await mcpManager.createDevice({
  name: 'Living Room Sensor',
  template: 'TEMPLATE_ID'
})

// Update datastream value
await mcpManager.updateDatastreamValue({
  device_id: 'DEVICE_ID',
  datastream: 'Power',
  value: 1
})
```

### 3. Use the UI Component

```svelte
<script>
  import MCPIntegration from './MCPIntegration.svelte'
</script>

<MCPIntegration />
```

## Integration with Dashboard

To integrate MCP features into the main dashboard:

1. **Add MCP tab** to Home.svelte navigation
2. **Import MCPManager** for device management
3. **Add template-based device creation** as alternative to manual token entry
4. **Enable AI-driven workflows** through MCP tools

Example:

```svelte
<script>
  import { MCPManager } from './lib/mcp'
  
  let mcpManager: MCPManager | null = null
  let mcpToken = $state('')
  
  function initMCP() {
    if (mcpToken) {
      mcpManager = new MCPManager(mcpToken)
    }
  }
</script>

{#if mcpManager}
  <!-- MCP controls -->
{/if}
```

## Architecture

```
src/
├── lib/
│   ├── mcp.ts              ← Core MCP client library (11 tools)
│   ├── mcp.examples.ts     ← 15+ usage examples
│   ├── mqtt.ts             ← MQTT connection (existing)
│   ├── utils.ts            ← Utilities (existing)
│   └── storage.ts          ← LocalStorage (existing)
├── MCPIntegration.svelte   ← Standalone MCP UI component
├── Home.svelte             ← Main dashboard (existing)
├── App.svelte              ← Root component (existing)
└── main.ts                 ← Entry point (existing)

MCP_INTEGRATION.md          ← Detailed documentation
README_MCP.md               ← This file
```

## Key Features

### MCP Client Library (`src/lib/mcp.ts`)

- ✅ **Type-safe API** — Full TypeScript support
- ✅ **All 11 tools** — Complete MCP specification
- ✅ **Error handling** — Comprehensive error messages
- ✅ **MCPManager class** — Convenient wrapper
- ✅ **Flexible parameters** — Optional and required fields
- ✅ **JSON-RPC 2.0** — Proper protocol implementation

### MCP UI Component (`src/MCPIntegration.svelte`)

- ✅ **Authentication** — Token-based login
- ✅ **Device management** — Search, view, create
- ✅ **Template management** — Browse and create
- ✅ **Tab navigation** — Organized interface
- ✅ **Toast notifications** — Real-time feedback
- ✅ **Responsive design** — Mobile-friendly
- ✅ **LocalStorage** — Token persistence

### Examples (`src/lib/mcp.examples.ts`)

- ✅ **15+ examples** — Every tool demonstrated
- ✅ **Complete workflow** — End-to-end example
- ✅ **Error handling** — Exception patterns
- ✅ **Best practices** — Recommended usage

## Security Notes

1. **Token Management**
   - Store tokens in environment variables for production
   - Never commit tokens to version control
   - UI component saves to localStorage (demo only)

2. **CORS**
   - Blynk MCP endpoint supports CORS
   - Requests made directly from browser

3. **Permissions**
   - Each tool requires specific permissions
   - Developer Mode needed for template/datastream creation

## Performance

- **Response Time**: 200-500ms per request
- **Rate Limiting**: Follow Blynk API limits
- **Caching**: Consider local caching for lists

## Stack

| Layer | Technology | Size |
|-------|-----------|------|
| UI Framework | **Svelte 5** | 24 KB gzip |
| Styling | **Tailwind CSS 4** | 5 KB gzip |
| IoT Protocol | **mqtt.js** | 112 KB gzip |
| MCP Client | **TypeScript** | ~15 KB gzip |
| Build | **Vite 6** | — |

**Total**: ~156 KB gzip (first load)

## Running the Project

```bash
# Install dependencies
npm install

# Development server
npm run dev       # → http://localhost:5173

# Production build
npm run build     # → ./dist/

# Preview build
npm run preview
```

## File Structure

```
blynk-iot-svelte-mcp/
├── src/
│   ├── lib/
│   │   ├── mcp.ts              (NEW) MCP client library
│   │   ├── mcp.examples.ts     (NEW) Examples
│   │   ├── mqtt.ts             (existing) MQTT manager
│   │   ├── utils.ts            (existing) Utilities
│   │   └── storage.ts          (existing) Storage
│   ├── MCPIntegration.svelte   (NEW) MCP UI component
│   ├── Home.svelte             (existing) Dashboard
│   ├── App.svelte              (existing) Root
│   ├── main.ts                 (existing) Entry
│   ├── types.ts                (existing) Types
│   └── app.css                 (existing) Styles
├── MCP_INTEGRATION.md          (NEW) Documentation
├── README_MCP.md               (NEW) This file
├── README.md                   (existing) Original README
├── package.json
├── vite.config.ts
├── svelte.config.js
├── tsconfig.json
└── index.html
```

## Next Steps

1. **Integrate into main dashboard**
   - Add MCP tab to Home.svelte
   - Connect device creation workflow

2. **Add advanced features**
   - Batch device operations
   - Device synchronization
   - Automated provisioning

3. **Implement caching**
   - Cache device/template lists
   - Reduce API calls

4. **Add WebSocket support**
   - Real-time device updates
   - Event streaming

## Documentation

- **`MCP_INTEGRATION.md`** — Complete integration guide
- **`src/lib/mcp.examples.ts`** — 15+ code examples
- **[Blynk MCP Docs](https://docs.blynk.io/en/getting-started/mcp-server)** — Official documentation
- **[MCP Protocol](https://modelcontextprotocol.io/)** — Protocol specification

## Troubleshooting

### "Authorization failed"
- Verify access token is correct
- Check token hasn't expired
- Ensure API access is enabled

### "CORS error"
- Verify endpoint URL: `https://blynk.cloud/mcp`
- Check browser console for details
- Blynk should support CORS

### "No devices found"
- Create devices in Blynk Console first
- Check search filters
- Verify token permissions

## Support

- [Blynk Documentation](https://docs.blynk.io/)
- [Blynk Community](https://community.blynk.cc/)
- [MCP Protocol](https://modelcontextprotocol.io/)

## License

Same as original Blynk IoT Dashboard project.

---

**Version**: 2.1.0 (with MCP Integration)  
**Last Updated**: 2026-06-17
