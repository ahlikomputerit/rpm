# Blynk MCP Integration Guide

This document describes the integration of **Blynk Model Context Protocol (MCP) Server** into the Blynk IoT Svelte Dashboard.

## Overview

The Blynk MCP server enables AI agents and applications to interact with your Blynk IoT platform programmatically. This integration provides a complete TypeScript client library and UI component for managing devices and templates through the MCP API.

## Architecture

### Components

1. **`src/lib/mcp.ts`** — Core MCP client library
   - Low-level MCP API calls
   - Type-safe function wrappers for all 11 MCP tools
   - `MCPManager` class for convenient state management

2. **`src/MCPIntegration.svelte`** — UI component
   - Authentication interface
   - Device and template management UI
   - Create/search functionality
   - Real-time feedback with toast notifications

### MCP Endpoint

- **URL**: `https://blynk.cloud/mcp`
- **Authentication**: OAuth 2.0 Bearer token
- **Protocol**: JSON-RPC 2.0

## Implemented Tools

All 11 Blynk MCP tools are fully implemented:

### 1. Search Devices
```typescript
searchDevices(accessToken, options?: {
  name?: string
  template?: string
  status?: string
})
```
Finds and lists devices with optional filtering.

### 2. Get Device
```typescript
getDevice(accessToken, deviceId)
```
Retrieves detailed information about a specific device.

### 3. Create Device
```typescript
createDevice(accessToken, {
  name: string
  template: string
  description?: string
})
```
Creates a new device under a template and generates an Auth Token.

### 4. Update DataStream Value
```typescript
updateDatastreamValue(accessToken, {
  device_id: string
  datastream: string
  value: string | number | boolean
})
```
Updates a datastream value on the device.

### 5. Get All Templates
```typescript
getAllTemplates(accessToken)
```
Retrieves information of all templates.

### 6. Get Template
```typescript
getTemplate(accessToken, templateId)
```
Retrieves detailed information of a specific template.

### 7. Create Template
```typescript
createTemplate(accessToken, {
  name: string
  description?: string
  connectionType?: 'WiFi' | 'Cellular' | 'Ethernet' | 'BLE'
})
```
Creates a new template with specified configuration.

### 8. Create DataStream
```typescript
createDatastream(accessToken, {
  template_id: string
  alias: string
  name?: string
  dataType?: 'numeric' | 'string' | 'boolean'
  min?: number
  max?: number
  unit?: string
  decimals?: number
})
```
Adds a new virtual data channel to a template.

### 9. Edit DataStream
```typescript
editDatastream(accessToken, {
  template_id: string
  datastream_id: string
  name?: string
  unit?: string
  min?: number
  max?: number
  decimals?: number
})
```
Updates the configuration of an existing data channel.

### 10. Create Event
```typescript
createEvent(accessToken, {
  template_id: string
  name: string
  description?: string
  notificationPriority?: 'low' | 'medium' | 'high'
})
```
Adds a new event to a template.

### 11. Edit Event
```typescript
editEvent(accessToken, {
  template_id: string
  event_id: string
  name?: string
  description?: string
  notificationPriority?: 'low' | 'medium' | 'high'
})
```
Modifies the parameters or notification settings of an existing event.

## Usage

### Basic Setup

1. **Get your Blynk Access Token**:
   - Go to Blynk Console
   - Navigate to Settings → API Tokens
   - Copy your access token

2. **Using the MCP Client Library**:

```typescript
import { MCPManager } from './lib/mcp'

const mcpManager = new MCPManager('your-access-token')

// Search devices
const devices = await mcpManager.searchDevices({ name: 'Pump' })

// Get all templates
const templates = await mcpManager.getAllTemplates()

// Create a new device
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

### Using the UI Component

Import and use the `MCPIntegration` component in your app:

```svelte
<script>
  import MCPIntegration from './MCPIntegration.svelte'
</script>

<MCPIntegration />
```

The component provides:
- **Authentication**: Login with access token
- **Device Management**: Search, view, and create devices
- **Template Management**: View and create templates
- **Create Operations**: Create new devices and templates with UI forms

## Integration with Existing Dashboard

To integrate MCP features into the main dashboard (`Home.svelte`):

1. **Add MCP tab** to the main navigation
2. **Import MCPManager** in Home.svelte
3. **Add device creation from templates** as an alternative to manual token entry
4. **Enable AI-driven device management** through MCP tools

Example integration:

```svelte
<script>
  import { MCPManager } from './lib/mcp'
  
  let mcpManager: MCPManager | null = null
  let mcpToken = $state('')
  
  function initMCP() {
    if (mcpToken) {
      mcpManager = new MCPManager(mcpToken)
      // Now use mcpManager to manage devices
    }
  }
</script>

<!-- Add MCP section to UI -->
{#if mcpManager}
  <!-- MCP controls here -->
{/if}
```

## Error Handling

All MCP functions include comprehensive error handling:

```typescript
try {
  const devices = await mcpManager.searchDevices()
} catch (error) {
  console.error('MCP Error:', error.message)
  // Handle error appropriately
}
```

Common errors:
- **Authentication Failed**: Invalid or expired access token
- **HTTP Errors**: Network or server issues
- **MCP Errors**: Invalid parameters or permission issues

## Security Considerations

1. **Access Token Storage**:
   - Store tokens securely (use environment variables for production)
   - Never commit tokens to version control
   - The UI component saves token to localStorage (for demo purposes only)

2. **CORS**:
   - Blynk MCP endpoint supports CORS
   - Requests are made directly from the browser

3. **Permissions**:
   - Each tool requires specific permissions (View Devices, Edit Templates, etc.)
   - Developer Mode must be enabled for template/datastream creation

## Performance

- **Response Time**: Typically 200-500ms per request
- **Rate Limiting**: Follow Blynk API rate limits
- **Caching**: Consider caching device/template lists locally

## TypeScript Support

All functions are fully typed:

```typescript
import type { Device, Template, DataStream, Event } from './lib/mcp'

const device: Device = await mcpManager.getDevice('device-id')
const templates: Template[] = await mcpManager.getAllTemplates()
```

## Testing

Example test scenarios:

```typescript
// Test device search
const devices = await mcpManager.searchDevices({ name: 'Pump' })
console.assert(Array.isArray(devices), 'Should return array')

// Test template creation
const template = await mcpManager.createTemplate({
  name: 'Test Template',
  connectionType: 'WiFi'
})
console.assert(template.template_id, 'Should return template ID')

// Test device creation
const device = await mcpManager.createDevice({
  name: 'Test Device',
  template: template.template_id
})
console.assert(device.device_id, 'Should return device ID')
```

## Troubleshooting

### "Authorization failed" error
- Verify your access token is correct
- Check that your Blynk account has API access enabled
- Ensure the token hasn't expired

### "CORS error"
- Blynk MCP endpoint should support CORS
- Check browser console for specific CORS error
- Verify the endpoint URL is correct

### "No devices found"
- Ensure you have created devices in Blynk Console
- Check device names if using search filters
- Verify your token has "View Devices" permission

## Future Enhancements

Potential improvements:
- WebSocket support for real-time updates
- Batch operations for multiple devices
- Advanced filtering and pagination
- Device/template synchronization
- Automated device provisioning workflows
- AI-powered device recommendations

## References

- [Blynk MCP Documentation](https://docs.blynk.io/en/getting-started/mcp-server)
- [Model Context Protocol](https://modelcontextprotocol.io/)
- [Blynk IoT Platform](https://blynk.io/)
- [JSON-RPC 2.0 Specification](https://www.jsonrpc.org/specification)

## Support

For issues or questions:
1. Check the [Blynk Documentation](https://docs.blynk.io/)
2. Visit [Blynk Community](https://community.blynk.cc/)
3. Review MCP error messages in browser console
