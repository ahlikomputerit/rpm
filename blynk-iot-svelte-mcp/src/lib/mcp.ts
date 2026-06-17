/**
 * Blynk MCP Server Client
 * 
 * Implements all 11 MCP tools for interacting with Blynk IoT platform:
 * - search_devices
 * - get_device
 * - create_device
 * - update_datastream_value
 * - get_all_templates
 * - get_template
 * - create_template
 * - create_datastream
 * - edit_datastream
 * - create_event
 * - edit_event
 */

const MCP_ENDPOINT = 'https://blynk.cloud/mcp'

export interface MCPRequest {
  jsonrpc: '2.0'
  method: string
  params: Record<string, any>
  id: string | number
}

export interface MCPResponse<T = any> {
  jsonrpc: '2.0'
  result?: T
  error?: {
    code: number
    message: string
    data?: any
  }
  id: string | number
}

export interface Device {
  id: string
  name: string
  template?: string
  status?: string
  datastreams?: Record<string, any>
  metadata?: Record<string, any>
}

export interface Template {
  id: string
  name: string
  description?: string
  connectionType?: string
  datastreams?: DataStream[]
  events?: Event[]
}

export interface DataStream {
  id: string
  name: string
  alias: string
  dataType: string
  min?: number
  max?: number
  unit?: string
  decimals?: number
}

export interface Event {
  id: string
  name: string
  description?: string
  notificationSettings?: Record<string, any>
}

/**
 * Base MCP call function
 */
async function callMCP<T = any>(
  method: string,
  params: Record<string, any>,
  accessToken: string,
): Promise<T> {
  const requestId = `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
  
  const request: MCPRequest = {
    jsonrpc: '2.0',
    method,
    params,
    id: requestId,
  }

  try {
    const response = await fetch(MCP_ENDPOINT, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${accessToken}`,
      },
      body: JSON.stringify(request),
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }

    const data: MCPResponse<T> = await response.json()

    if (data.error) {
      throw new Error(`MCP Error [${data.error.code}]: ${data.error.message}`)
    }

    if (!data.result) {
      throw new Error('No result in MCP response')
    }

    return data.result
  } catch (error) {
    console.error(`MCP call failed for method "${method}":`, error)
    throw error
  }
}

/**
 * Search Devices
 * Finds and lists devices, optionally filtered by name, template, or status
 */
export async function searchDevices(
  accessToken: string,
  options?: {
    name?: string
    template?: string
    status?: string
  },
): Promise<Device[]> {
  const params: Record<string, any> = {}
  if (options?.name) params.name = options.name
  if (options?.template) params.template = options.template
  if (options?.status) params.status = options.status

  return callMCP<Device[]>('search_devices', params, accessToken)
}

/**
 * Get a Device
 * Get detailed information about a specific device
 */
export async function getDevice(
  accessToken: string,
  deviceId: string,
): Promise<Device> {
  return callMCP<Device>('get_device', { device_id: deviceId }, accessToken)
}

/**
 * Create a Device
 * Creates a new device under a template and generates an Auth Token
 */
export async function createDevice(
  accessToken: string,
  options: {
    name: string
    template: string
    description?: string
  },
): Promise<{ device_id: string; auth_token: string }> {
  return callMCP<{ device_id: string; auth_token: string }>(
    'create_device',
    {
      name: options.name,
      template: options.template,
      description: options.description || '',
    },
    accessToken,
  )
}

/**
 * Update DataStream Value
 * Update a datastream value on the device
 */
export async function updateDatastreamValue(
  accessToken: string,
  options: {
    device_id: string
    datastream: string
    value: string | number | boolean
  },
): Promise<{ success: boolean }> {
  return callMCP<{ success: boolean }>(
    'update_datastream_value',
    {
      device_id: options.device_id,
      datastream: options.datastream,
      value: String(options.value),
    },
    accessToken,
  )
}

/**
 * Get all Templates
 * Retrieves information of all templates
 */
export async function getAllTemplates(
  accessToken: string,
): Promise<Template[]> {
  return callMCP<Template[]>('get_all_templates', {}, accessToken)
}

/**
 * Get Template
 * Retrieves detailed information of a specific template
 */
export async function getTemplate(
  accessToken: string,
  templateId: string,
): Promise<Template> {
  return callMCP<Template>(
    'get_template',
    { template_id: templateId },
    accessToken,
  )
}

/**
 * Create a Template
 * Creates a new template with specified configuration
 */
export async function createTemplate(
  accessToken: string,
  options: {
    name: string
    description?: string
    connectionType?: 'WiFi' | 'Cellular' | 'Ethernet' | 'BLE'
  },
): Promise<{ template_id: string }> {
  return callMCP<{ template_id: string }>(
    'create_template',
    {
      name: options.name,
      description: options.description || '',
      connection_type: options.connectionType || 'WiFi',
    },
    accessToken,
  )
}

/**
 * Create a DataStream
 * Adds a new virtual data channel to a specific template
 */
export async function createDatastream(
  accessToken: string,
  options: {
    template_id: string
    alias: string
    name?: string
    dataType?: 'numeric' | 'string' | 'boolean'
    min?: number
    max?: number
    unit?: string
    decimals?: number
  },
): Promise<{ datastream_id: string }> {
  return callMCP<{ datastream_id: string }>(
    'create_datastream',
    {
      template_id: options.template_id,
      alias: options.alias,
      name: options.name || options.alias,
      data_type: options.dataType || 'numeric',
      min: options.min,
      max: options.max,
      unit: options.unit,
      decimals: options.decimals,
    },
    accessToken,
  )
}

/**
 * Edit a DataStream
 * Updates the configuration of an existing data channel
 */
export async function editDatastream(
  accessToken: string,
  options: {
    template_id: string
    datastream_id: string
    name?: string
    unit?: string
    min?: number
    max?: number
    decimals?: number
  },
): Promise<{ success: boolean }> {
  const params: Record<string, any> = {
    template_id: options.template_id,
    datastream_id: options.datastream_id,
  }
  if (options.name) params.name = options.name
  if (options.unit) params.unit = options.unit
  if (options.min !== undefined) params.min = options.min
  if (options.max !== undefined) params.max = options.max
  if (options.decimals !== undefined) params.decimals = options.decimals

  return callMCP<{ success: boolean }>(
    'edit_datastream',
    params,
    accessToken,
  )
}

/**
 * Create an Event
 * Adds a new event to a specific template
 */
export async function createEvent(
  accessToken: string,
  options: {
    template_id: string
    name: string
    description?: string
    notificationPriority?: 'low' | 'medium' | 'high'
  },
): Promise<{ event_id: string }> {
  return callMCP<{ event_id: string }>(
    'create_event',
    {
      template_id: options.template_id,
      name: options.name,
      description: options.description || '',
      notification_priority: options.notificationPriority || 'medium',
    },
    accessToken,
  )
}

/**
 * Edit an Event
 * Modifies the parameters or notification settings of an existing event
 */
export async function editEvent(
  accessToken: string,
  options: {
    template_id: string
    event_id: string
    name?: string
    description?: string
    notificationPriority?: 'low' | 'medium' | 'high'
  },
): Promise<{ success: boolean }> {
  const params: Record<string, any> = {
    template_id: options.template_id,
    event_id: options.event_id,
  }
  if (options.name) params.name = options.name
  if (options.description) params.description = options.description
  if (options.notificationPriority) params.notification_priority = options.notificationPriority

  return callMCP<{ success: boolean }>(
    'edit_event',
    params,
    accessToken,
  )
}

/**
 * MCP Manager class for easier state management
 */
export class MCPManager {
  private accessToken: string

  constructor(accessToken: string) {
    this.accessToken = accessToken
  }

  setAccessToken(token: string) {
    this.accessToken = token
  }

  async searchDevices(options?: Parameters<typeof searchDevices>[1]) {
    return searchDevices(this.accessToken, options)
  }

  async getDevice(deviceId: string) {
    return getDevice(this.accessToken, deviceId)
  }

  async createDevice(options: Parameters<typeof createDevice>[1]) {
    return createDevice(this.accessToken, options)
  }

  async updateDatastreamValue(options: Parameters<typeof updateDatastreamValue>[1]) {
    return updateDatastreamValue(this.accessToken, options)
  }

  async getAllTemplates() {
    return getAllTemplates(this.accessToken)
  }

  async getTemplate(templateId: string) {
    return getTemplate(this.accessToken, templateId)
  }

  async createTemplate(options: Parameters<typeof createTemplate>[1]) {
    return createTemplate(this.accessToken, options)
  }

  async createDatastream(options: Parameters<typeof createDatastream>[1]) {
    return createDatastream(this.accessToken, options)
  }

  async editDatastream(options: Parameters<typeof editDatastream>[1]) {
    return editDatastream(this.accessToken, options)
  }

  async createEvent(options: Parameters<typeof createEvent>[1]) {
    return createEvent(this.accessToken, options)
  }

  async editEvent(options: Parameters<typeof editEvent>[1]) {
    return editEvent(this.accessToken, options)
  }
}
