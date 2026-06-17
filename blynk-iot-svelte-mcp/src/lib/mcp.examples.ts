/**
 * Blynk MCP Examples
 * 
 * This file contains practical examples of how to use each MCP function.
 * Copy and adapt these examples to your needs.
 */

import {
  MCPManager,
  searchDevices,
  getDevice,
  createDevice,
  updateDatastreamValue,
  getAllTemplates,
  getTemplate,
  createTemplate,
  createDatastream,
  editDatastream,
  createEvent,
  editEvent,
} from './mcp'

const ACCESS_TOKEN = 'your-blynk-access-token'

// ─── Example 1: Initialize MCPManager ──────────────────────────────────────
export async function example1_initMCPManager() {
  const mcpManager = new MCPManager(ACCESS_TOKEN)
  console.log('✅ MCPManager initialized')
  return mcpManager
}

// ─── Example 2: Search Devices ────────────────────────────────────────────
export async function example2_searchDevices() {
  // Search all devices
  const allDevices = await searchDevices(ACCESS_TOKEN)
  console.log('All devices:', allDevices)

  // Search devices by name
  const pumpDevices = await searchDevices(ACCESS_TOKEN, { name: 'Pump' })
  console.log('Devices with "Pump" in name:', pumpDevices)

  // Search devices by template
  const sensorDevices = await searchDevices(ACCESS_TOKEN, {
    template: 'TEMPLATE_ID',
  })
  console.log('Devices using template:', sensorDevices)

  // Search devices by status
  const onlineDevices = await searchDevices(ACCESS_TOKEN, { status: 'online' })
  console.log('Online devices:', onlineDevices)
}

// ─── Example 3: Get Device Details ────────────────────────────────────────
export async function example3_getDeviceDetails() {
  const deviceId = 'DEVICE_ID'
  const device = await getDevice(ACCESS_TOKEN, deviceId)

  console.log('Device name:', device.name)
  console.log('Device status:', device.status)
  console.log('Device datastreams:', device.datastreams)
  console.log('Device metadata:', device.metadata)
}

// ─── Example 4: Create Device ─────────────────────────────────────────────
export async function example4_createDevice() {
  const result = await createDevice(ACCESS_TOKEN, {
    name: 'Living Room Sensor',
    template: 'TEMPLATE_ID',
    description: 'Temperature and humidity sensor',
  })

  console.log('New device ID:', result.device_id)
  console.log('Auth Token:', result.auth_token)
  console.log('Save this token to configure your device!')
}

// ─── Example 5: Update Datastream Value ───────────────────────────────────
export async function example5_updateDatastreamValue() {
  // Turn on a device (set Power datastream to 1)
  await updateDatastreamValue(ACCESS_TOKEN, {
    device_id: 'DEVICE_ID',
    datastream: 'Power',
    value: 1,
  })
  console.log('✅ Device turned ON')

  // Turn off a device
  await updateDatastreamValue(ACCESS_TOKEN, {
    device_id: 'DEVICE_ID',
    datastream: 'Power',
    value: 0,
  })
  console.log('✅ Device turned OFF')

  // Set temperature value
  await updateDatastreamValue(ACCESS_TOKEN, {
    device_id: 'DEVICE_ID',
    datastream: 'Temperature',
    value: 23.5,
  })
  console.log('✅ Temperature set to 23.5°C')

  // Set string value
  await updateDatastreamValue(ACCESS_TOKEN, {
    device_id: 'DEVICE_ID',
    datastream: 'Status',
    value: 'active',
  })
  console.log('✅ Status set to "active"')
}

// ─── Example 6: Get All Templates ─────────────────────────────────────────
export async function example6_getAllTemplates() {
  const templates = await getAllTemplates(ACCESS_TOKEN)

  console.log(`Found ${templates.length} templates:`)
  templates.forEach((template) => {
    console.log(`- ${template.name} (ID: ${template.id})`)
    console.log(`  Description: ${template.description}`)
    console.log(`  Datastreams: ${template.datastreams?.length || 0}`)
  })
}

// ─── Example 7: Get Template Details ──────────────────────────────────────
export async function example7_getTemplateDetails() {
  const templateId = 'TEMPLATE_ID'
  const template = await getTemplate(ACCESS_TOKEN, templateId)

  console.log('Template name:', template.name)
  console.log('Connection type:', template.connectionType)
  console.log('Datastreams:')
  template.datastreams?.forEach((ds) => {
    console.log(`  - ${ds.name} (${ds.alias}): ${ds.dataType}`)
  })
  console.log('Events:')
  template.events?.forEach((event) => {
    console.log(`  - ${event.name}`)
  })
}

// ─── Example 8: Create Template ───────────────────────────────────────────
export async function example8_createTemplate() {
  const result = await createTemplate(ACCESS_TOKEN, {
    name: 'Smart Light Controller',
    description: 'Template for controlling smart lights',
    connectionType: 'WiFi',
  })

  console.log('✅ Template created with ID:', result.template_id)
  return result.template_id
}

// ─── Example 9: Create Datastream ─────────────────────────────────────────
export async function example9_createDatastream() {
  const templateId = 'TEMPLATE_ID'

  // Create numeric datastream for temperature
  const tempResult = await createDatastream(ACCESS_TOKEN, {
    template_id: templateId,
    alias: 'Temperature',
    name: 'Temperature Sensor',
    dataType: 'numeric',
    min: -50,
    max: 50,
    unit: '°C',
    decimals: 1,
  })
  console.log('✅ Temperature datastream created:', tempResult.datastream_id)

  // Create boolean datastream for power
  const powerResult = await createDatastream(ACCESS_TOKEN, {
    template_id: templateId,
    alias: 'Power',
    name: 'Power Switch',
    dataType: 'boolean',
  })
  console.log('✅ Power datastream created:', powerResult.datastream_id)

  // Create string datastream for status
  const statusResult = await createDatastream(ACCESS_TOKEN, {
    template_id: templateId,
    alias: 'Status',
    name: 'Device Status',
    dataType: 'string',
  })
  console.log('✅ Status datastream created:', statusResult.datastream_id)
}

// ─── Example 10: Edit Datastream ──────────────────────────────────────────
export async function example10_editDatastream() {
  const result = await editDatastream(ACCESS_TOKEN, {
    template_id: 'TEMPLATE_ID',
    datastream_id: 'DATASTREAM_ID',
    name: 'Updated Temperature Sensor',
    unit: '°F',
    min: -58,
    max: 122,
    decimals: 2,
  })

  console.log('✅ Datastream updated:', result.success)
}

// ─── Example 11: Create Event ─────────────────────────────────────────────
export async function example11_createEvent() {
  const result = await createEvent(ACCESS_TOKEN, {
    template_id: 'TEMPLATE_ID',
    name: 'High Temperature Alert',
    description: 'Triggered when temperature exceeds 40°C',
    notificationPriority: 'high',
  })

  console.log('✅ Event created with ID:', result.event_id)
}

// ─── Example 12: Edit Event ───────────────────────────────────────────────
export async function example12_editEvent() {
  const result = await editEvent(ACCESS_TOKEN, {
    template_id: 'TEMPLATE_ID',
    event_id: 'EVENT_ID',
    name: 'Critical Temperature Alert',
    description: 'Triggered when temperature exceeds 50°C',
    notificationPriority: 'high',
  })

  console.log('✅ Event updated:', result.success)
}

// ─── Example 13: Complete Workflow ────────────────────────────────────────
export async function example13_completeWorkflow() {
  console.log('=== Complete MCP Workflow Example ===\n')

  // Step 1: Create a template
  console.log('Step 1: Creating template...')
  const templateResult = await createTemplate(ACCESS_TOKEN, {
    name: 'Smart Room Controller',
    description: 'Controls lights, temperature, and humidity',
    connectionType: 'WiFi',
  })
  const templateId = templateResult.template_id
  console.log(`✅ Template created: ${templateId}\n`)

  // Step 2: Add datastreams to template
  console.log('Step 2: Adding datastreams...')
  const datastreams = [
    {
      alias: 'Power',
      name: 'Power Switch',
      dataType: 'boolean' as const,
    },
    {
      alias: 'Temperature',
      name: 'Temperature',
      dataType: 'numeric' as const,
      min: 15,
      max: 35,
      unit: '°C',
      decimals: 1,
    },
    {
      alias: 'Humidity',
      name: 'Humidity',
      dataType: 'numeric' as const,
      min: 0,
      max: 100,
      unit: '%',
      decimals: 0,
    },
  ]

  for (const ds of datastreams) {
    await createDatastream(ACCESS_TOKEN, {
      template_id: templateId,
      ...ds,
    })
    console.log(`✅ Datastream added: ${ds.alias}`)
  }
  console.log()

  // Step 3: Add events to template
  console.log('Step 3: Adding events...')
  const eventResult = await createEvent(ACCESS_TOKEN, {
    template_id: templateId,
    name: 'Temperature Alert',
    description: 'Alert when temperature is out of range',
    notificationPriority: 'high',
  })
  console.log(`✅ Event created: ${eventResult.event_id}\n`)

  // Step 4: Create a device from template
  console.log('Step 4: Creating device from template...')
  const deviceResult = await createDevice(ACCESS_TOKEN, {
    name: 'Living Room Controller',
    template: templateId,
    description: 'Main living room smart controller',
  })
  const deviceId = deviceResult.device_id
  console.log(`✅ Device created: ${deviceId}`)
  console.log(`📝 Auth Token: ${deviceResult.auth_token}\n`)

  // Step 5: Update device datastreams
  console.log('Step 5: Setting initial datastream values...')
  await updateDatastreamValue(ACCESS_TOKEN, {
    device_id: deviceId,
    datastream: 'Power',
    value: 1,
  })
  console.log('✅ Power: ON')

  await updateDatastreamValue(ACCESS_TOKEN, {
    device_id: deviceId,
    datastream: 'Temperature',
    value: 22.5,
  })
  console.log('✅ Temperature: 22.5°C')

  await updateDatastreamValue(ACCESS_TOKEN, {
    device_id: deviceId,
    datastream: 'Humidity',
    value: 55,
  })
  console.log('✅ Humidity: 55%\n')

  // Step 6: Verify device
  console.log('Step 6: Verifying device...')
  const device = await getDevice(ACCESS_TOKEN, deviceId)
  console.log(`✅ Device verified: ${device.name}`)
  console.log(`   Status: ${device.status}`)
  console.log(`   Datastreams: ${device.datastreams ? Object.keys(device.datastreams).length : 0}`)

  console.log('\n=== Workflow Complete ===')
}

// ─── Example 14: Using MCPManager Class ───────────────────────────────────
export async function example14_mcpManagerClass() {
  const mcpManager = new MCPManager(ACCESS_TOKEN)

  // All methods are available on the manager
  const devices = await mcpManager.searchDevices()
  const templates = await mcpManager.getAllTemplates()

  console.log(`Found ${devices.length} devices and ${templates.length} templates`)

  // You can also change the token
  mcpManager.setAccessToken('new-access-token')
}

// ─── Example 15: Error Handling ───────────────────────────────────────────
export async function example15_errorHandling() {
  try {
    // This will fail with invalid device ID
    await getDevice(ACCESS_TOKEN, 'invalid-device-id')
  } catch (error: any) {
    console.error('Error caught:', error.message)
    // Handle error appropriately
  }

  try {
    // This will fail with invalid token
    await searchDevices('invalid-token')
  } catch (error: any) {
    console.error('Authentication error:', error.message)
    // Prompt user to re-authenticate
  }
}

// ─── Run Examples ──────────────────────────────────────────────────────────
export async function runAllExamples() {
  console.log('🚀 Running all MCP examples...\n')

  try {
    // Uncomment examples to run them
    // await example2_searchDevices()
    // await example6_getAllTemplates()
    // await example13_completeWorkflow()

    console.log('\n✅ Examples completed successfully!')
  } catch (error) {
    console.error('❌ Error running examples:', error)
  }
}
