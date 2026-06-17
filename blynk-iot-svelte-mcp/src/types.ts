export interface MQTTMessage {
  topic: string;
  payload: string;
  timestamp: Date;
}

export interface Module {
  id: string;
  name: string;
  authToken: string;
  virtualPin: string;
  lampState: boolean;
  manualMode: boolean;
  pagiHour: number;
  pagiDuration: number; // menit
  soreHour: number;
  soreDuration: number; // menit
  messages: MQTTMessage[];
  isConnected: boolean;
  isConnecting: boolean;
}

// Hanya field ini yang disimpan ke localStorage
export type SavedModule = Pick<
  Module,
  'id' | 'name' | 'authToken' | 'virtualPin' | 'manualMode' |
  'pagiHour' | 'pagiDuration' | 'soreHour' | 'soreDuration'
>
