// RemoteForge Frontend API & WebSocket Client

export interface DeviceInfo {
  id: string;
  name: string;
  os_name: string;
  os_version: string;
  agent_version: string;
  is_online: boolean;
  last_seen: string;
  created_at: string;
}

export interface SystemStatusData {
  os_name: string;
  os_version: string;
  machine_architecture: string;
  cpu: {
    percent: number;
    logical_cores: number;
    physical_cores: number;
  };
  ram: {
    total_gb: number;
    used_gb: number;
    percent: number;
  };
  disk: {
    total_gb: number;
    used_gb: number;
    percent: number;
  };
  uptime_seconds: number;
  uptime_formatted: string;
  agent_version: string;
  timestamp: string;
}

export interface ScreenshotData {
  width: number;
  height: number;
  format: string;
  data_base64: string;
}

export interface CommandResponse {
  request_id: string;
  device_id: string;
  tool: string;
  permission_level: string;
  requires_confirmation: boolean;
  status: string;
  result?: any;
  error?: string;
}

export interface AuditLogItem {
  id: number;
  timestamp: string;
  actor: string;
  device_id?: string;
  task_id?: string;
  action: string;
  tool?: string;
  arguments_safe?: string;
  permission_level: string;
  confirmation_status: string;
  execution_status: string;
  details?: string;
}

export interface GuestInvitation {
  id: string;
  device_id: string;
  invite_secret?: string;
  qr_payload: string;
  expires_at: string;
  capabilities: string[];
  is_redeemed: boolean;
  is_revoked: boolean;
}

export interface GuestSession {
  id: string;
  session_id?: string;
  device_id: string;
  guest_name: string;
  status: string;
  control_mode: string;
  approved_capabilities: string[];
  capabilities?: string[];
  expires_at: string;
  created_at?: string;
  is_active?: boolean;
}

export interface PrivacyShieldStatus {
  device_id: string;
  is_lockdown: boolean;
  is_screen_paused: boolean;
  is_input_disabled: boolean;
  is_transfers_stopped: boolean;
  pending_host_sync: boolean;
  active_guests_count: number;
  updated_at: string;
}

export interface StagedFile {
  id: string;
  session_id: string;
  device_id: string;
  filename: string;
  file_size: number;
  sha256_hash: string;
  status: string;
  uploaded_at: string;
  promoted_to_path?: string;
}


export interface AuditDayStat {
  date: string;
  total: number;
  bugs: number;
  success: number;
}

export interface AuditStatsResponse {
  total_logs: number;
  total_bugs: number;
  total_success: number;
  retention_limit_days: number;
  days_breakdown: AuditDayStat[];
}

export interface AuditCleanResponse {
  deleted_count: number;
  message: string;
  retention_days: number;
}

export interface FileItem {
  name: string;
  path: string;
  is_directory: boolean;
  size_bytes?: number;
  modified_at?: string;
}

// Configurable Server Public Base URL
export function getServerBaseUrl(): string {
  const custom = localStorage.getItem("rf_server_url");
  if (custom && custom.trim()) {
    return custom.trim().replace(/\/$/, "");
  }
  const envUrl = import.meta.env.VITE_API_BASE_URL;
  if (envUrl && envUrl.trim()) {
    return envUrl.trim().replace(/\/$/, "");
  }
  // Default to same origin or port 8090 if frontend is served by Vite dev on 5173
  if (window.location.port === "5173") {
    return `http://${window.location.hostname}:8090`;
  }
  return "";
}

export function setServerBaseUrl(url: string) {
  if (url.trim()) {
    localStorage.setItem("rf_server_url", url.trim().replace(/\/$/, ""));
  } else {
    localStorage.removeItem("rf_server_url");
  }
}

export function getAuthToken(): string | null {
  return localStorage.getItem("rf_auth_token");
}

export function setAuthToken(token: string) {
  localStorage.setItem("rf_auth_token", token);
}

export function clearAuthToken() {
  localStorage.removeItem("rf_auth_token");
}

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const baseUrl = getServerBaseUrl();
  const token = getAuthToken();
  const headers = new Headers(options.headers || {});
  if (!headers.has("Content-Type") && !(options.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }
  if (!headers.has("Authorization") && token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const url = `${baseUrl}${endpoint}`;
  const response = await fetch(url, { ...options, headers });

  if (response.status === 401) {
    // Only clear global auth token if the request was made using the global auth token
    if (!options.headers || !new Headers(options.headers).has("Authorization")) {
      clearAuthToken();
      window.dispatchEvent(new Event("rf_auth_unauthorized"));
    }
    throw new Error("Authentication session expired or unauthorized.");
  }

  if (!response.ok) {
    let errDetail = response.statusText;
    try {
      const errJson = await response.json();
      errDetail = errJson.detail || errJson.message || JSON.stringify(errJson);
    } catch {
      // ignore
    }
    throw new Error(errDetail);
  }

  return response.json();
}

export const api = {
  getHealth: () => request<{ status: string; server: string; version: string }>("/api/health"),

  login: async (username: string, password: string) => {
    const data = await request<{ access_token: string; username: string; role: string }>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ username, password }),
    });
    setAuthToken(data.access_token);
    return data;
  },

  getDevices: () => request<DeviceInfo[]>("/api/devices"),

  getDevice: (id: string) => request<DeviceInfo>(`/api/devices/${id}`),

  sendCommand: (deviceId: string, tool: string, args: Record<string, any> = {}, confirmed = false) => {
    return request<CommandResponse>(`/api/devices/${deviceId}/commands`, {
      method: "POST",
      body: JSON.stringify({ tool, arguments: args, confirmed }),
    });
  },

  getAuditLogs: (deviceId?: string, category = "all", timeframe = "14d", days?: number) => {
    const params = new URLSearchParams();
    if (deviceId) params.set("device_id", deviceId);
    if (category) params.set("category", category);
    if (timeframe) params.set("timeframe", timeframe);
    if (days !== undefined) params.set("days", days.toString());
    return request<AuditLogItem[]>(`/api/audit?${params.toString()}`);
  },

  getAuditStats: (deviceId?: string, days = 14) => {
    const params = new URLSearchParams();
    if (deviceId) params.set("device_id", deviceId);
    params.set("days", days.toString());
    return request<AuditStatsResponse>(`/api/audit/stats?${params.toString()}`);
  },

  cleanAuditLogs: (days?: number) => {
    return request<AuditCleanResponse>(`/api/audit/clean`, {
      method: "POST",
      body: JSON.stringify({ days: days ?? 0 }),
    });
  },

  chatWithAI: (message: string, deviceId?: string, confirmed = false) => {
    return request<{
      reply: string;
      tool_called?: string;
      tool_arguments?: any;
      tool_result?: any;
      requires_confirmation?: boolean;
      confirmation_prompt?: string;
    }>('/api/ai/chat', {
      method: 'POST',
      body: JSON.stringify({ message, device_id: deviceId, confirmed }),
    });
  },

  listFiles: (deviceId: string, path?: string) => {
    const query = `device_id=${encodeURIComponent(deviceId)}${path ? `&path=${encodeURIComponent(path)}` : ""}`;
    return request<{ success: boolean; result: { current_path: string; items: FileItem[] } }>(`/api/files/list?${query}`);
  },

  searchFiles: (deviceId: string, queryText: string, directory?: string) => {
    let query = `device_id=${encodeURIComponent(deviceId)}&query=${encodeURIComponent(queryText)}`;
    if (directory) {
      query += `&directory=${encodeURIComponent(directory)}`;
    }
    return request<{ success: boolean; result: { query: string; count: number; matches: any[] } }>(`/api/files/search?${query}`);
  },

  previewFile: (deviceId: string, filePath: string) => {
    const query = `device_id=${encodeURIComponent(deviceId)}&path=${encodeURIComponent(filePath)}`;
    return request<any>(`/api/files/preview?${query}`);
  },

  downloadFile: async (deviceId: string, filePath: string, filename?: string) => {
    const baseUrl = getServerBaseUrl();
    const token = getAuthToken();
    const url = `${baseUrl}/api/files/download?device_id=${encodeURIComponent(deviceId)}&path=${encodeURIComponent(filePath)}`;

    const response = await fetch(url, {
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      }
    });

    if (!response.ok) {
      let err = response.statusText;
      try {
        const j = await response.json();
        err = j.detail || err;
      } catch {
        // ignore
      }
      throw new Error(err);
    }

    const blob = await response.blob();
    const downloadUrl = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = downloadUrl;
    a.download = filename || filePath.split("/").pop() || "downloaded_file";
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(downloadUrl);
  },

  uploadFile: (deviceId: string, targetDirectory: string, file: File): Promise<any> => {
    const baseUrl = getServerBaseUrl();
    const token = getAuthToken();
    const url = `${baseUrl}/api/files/upload`;

    const formData = new FormData();
    formData.append("device_id", deviceId);
    formData.append("target_directory", targetDirectory);
    formData.append("file", file);

    return fetch(url, {
      method: "POST",
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: formData
    }).then(async (res) => {
      if (!res.ok) {
        let err = res.statusText;
        try {
          const j = await res.json();
          err = j.detail || err;
        } catch {
          // ignore
        }
        throw new Error(err);
      }
      return res.json();
    });
  },

  createWebSocketClient: (onMessage: (msg: any) => void): WebSocket | null => {
    const token = getAuthToken();
    if (!token) return null;

    const baseUrl = getServerBaseUrl();
    let wsHost = window.location.host;
    let wsProtocol = window.location.protocol === "https:" ? "wss:" : "ws:";

    if (baseUrl) {
      try {
        const u = new URL(baseUrl);
        wsHost = u.host;
        wsProtocol = u.protocol === "https:" ? "wss:" : "ws:";
      } catch {
        // fallback
      }
    }

    const wsUrl = `${wsProtocol}//${wsHost}/ws/client?token=${token}`;
    const ws = new WebSocket(wsUrl);

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        onMessage(data);
      } catch {
        // ignore
      }
    };

    return ws;
  },

  // --- Guest Access & Co-Control APIs ---
  createGuestInvitation: (deviceId: string, capabilities: string[] = ["screen.view", "input.control"], expiresInMinutes = 15) => {
    return request<GuestInvitation>("/api/guest/invitations", {
      method: "POST",
      body: JSON.stringify({
        device_id: deviceId,
        capabilities,
        expires_in_minutes: expiresInMinutes,
      }),
    });
  },

  listGuestInvitations: (deviceId: string) => {
    return request<GuestInvitation[]>(`/api/guest/invitations/${encodeURIComponent(deviceId)}`);
  },

  revokeGuestInvitation: (invitationId: string) => {
    return request<{ status: string; message: string }>(`/api/guest/invitations/${encodeURIComponent(invitationId)}`, {
      method: "DELETE",
    });
  },

  redeemGuestInvitation: (invitationId: string, inviteSecret: string, guestName: string) => {
    return request<{
      guest_token: string;
      session_id: string;
      device_id: string;
      status: string;
      capabilities: string[];
      expires_at: string;
    }>("/api/guest/redeem", {
      method: "POST",
      body: JSON.stringify({
        invitation_id: invitationId,
        invite_secret: inviteSecret,
        guest_name: guestName,
      }),
    });
  },

  getGuestSessionStatus: async (guestToken: string): Promise<GuestSession> => {
    const data = await request<any>("/api/guest/session/status", {
      headers: {
        Authorization: `Bearer ${guestToken}`,
      },
    });
    const caps = data.approved_capabilities || data.capabilities || [];
    const id = data.id || data.session_id || "";
    return {
      ...data,
      id,
      session_id: id,
      approved_capabilities: caps,
      capabilities: caps,
    };
  },

  listGuestSessions: (deviceId: string) => {
    return request<GuestSession[]>(`/api/guest/sessions/${encodeURIComponent(deviceId)}`);
  },

  approveGuestSession: (sessionId: string, approvedCapabilities: string[], controlMode = "shared-control", durationMinutes = 30) => {
    return request<GuestSession>(`/api/guest/sessions/${encodeURIComponent(sessionId)}/approve`, {
      method: "POST",
      body: JSON.stringify({
        approve: true,
        approved_capabilities: approvedCapabilities,
        capabilities: approvedCapabilities,
        control_mode: controlMode,
        duration_minutes: durationMinutes,
      }),
    });
  },

  revokeGuestSession: (sessionId: string, reason = "Revoked by owner") => {
    return request<{ status: string; message: string }>(`/api/guest/sessions/${encodeURIComponent(sessionId)}/revoke?reason=${encodeURIComponent(reason)}`, {
      method: "POST",
    });
  },

  getPrivacyShield: (deviceId: string) => {
    return request<PrivacyShieldStatus>(`/api/guest/privacy-shield/${encodeURIComponent(deviceId)}`);
  },

  updatePrivacyShield: (deviceId: string, update: {
    is_lockdown?: boolean;
    is_screen_paused?: boolean;
    is_input_disabled?: boolean;
    is_transfers_stopped?: boolean;
    action?: string;
  }) => {
    return request<PrivacyShieldStatus>(`/api/guest/privacy-shield/${encodeURIComponent(deviceId)}`, {
      method: "POST",
      body: JSON.stringify(update),
    });
  },

  listStagedFiles: (deviceId?: string, customToken?: string) => {
    const params = new URLSearchParams();
    if (deviceId) params.set("device_id", deviceId);
    const headers: Record<string, string> = {};
    if (customToken) headers["Authorization"] = `Bearer ${customToken}`;
    return request<StagedFile[]>(`/api/guest/staging/files?${params.toString()}`, { headers });
  },

  uploadStagedFile: async (file: File, deviceId?: string, customToken?: string): Promise<StagedFile> => {
    const baseUrl = getServerBaseUrl();
    const token = customToken || getAuthToken();
    const url = `${baseUrl}/api/guest/staging/upload`;

    const formData = new FormData();
    formData.append("file", file);
    if (deviceId) {
      formData.append("device_id", deviceId);
    }

    const headers: Record<string, string> = {};
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }

    const response = await fetch(url, {
      method: "POST",
      headers,
      body: formData,
    });

    if (!response.ok) {
      let err = response.statusText;
      try {
        const j = await response.json();
        err = j.detail || err;
      } catch {
        // ignore
      }
      throw new Error(err);
    }
    return response.json();
  },

  downloadStagedFile: async (fileId: string, filename?: string, customToken?: string) => {
    const baseUrl = getServerBaseUrl();
    const token = customToken || getAuthToken();
    const url = `${baseUrl}/api/guest/staging/download/${encodeURIComponent(fileId)}`;

    const headers: Record<string, string> = {};
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }

    const response = await fetch(url, { headers });

    if (!response.ok) {
      let err = response.statusText;
      try {
        const j = await response.json();
        err = j.detail || err;
      } catch {
        // ignore
      }
      throw new Error(err);
    }

    const blob = await response.blob();
    const downloadUrl = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = downloadUrl;
    a.download = filename || "staged_file";
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(downloadUrl);
  },

  promoteStagedFile: (fileId: string, destinationDir: string) => {
    return request<StagedFile>(`/api/guest/staging/promote/${encodeURIComponent(fileId)}`, {
      method: "POST",
      body: JSON.stringify({ destination_dir: destinationDir }),
    });
  },

  deleteStagedFile: (fileId: string, customToken?: string) => {
    const headers: Record<string, string> = {};
    if (customToken) headers["Authorization"] = `Bearer ${customToken}`;
    return request<{ status: string; message: string }>(`/api/guest/staging/files/${encodeURIComponent(fileId)}`, {
      method: "DELETE",
      headers,
    });
  },
};
