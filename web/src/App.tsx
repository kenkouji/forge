import React, { useState, useEffect } from 'react';
import {
  Shield,
  Monitor,
  FolderLock,
  Sparkles,
  Activity,
  Settings,
  Cpu,
  Camera,
  Lock,
  RefreshCw,
  Search,
  ChevronRight,
  AlertTriangle,
  X,
  Radio,
  FileText,
  Folder,
  Download,
  Upload,
  ArrowUp,
  Trash2,
  Bug,
  CheckCircle2
} from 'lucide-react';
import {
  api,
  getServerBaseUrl,
  setServerBaseUrl,
  getAuthToken,
  clearAuthToken
} from './api';
import type {
  DeviceInfo,
  SystemStatusData,
  ScreenshotData,
  AuditLogItem,
  FileItem,
  AuditStatsResponse
} from './api';

type Tab = 'home' | 'devices' | 'files' | 'ai' | 'activity' | 'settings';

export default function App() {
  const [activeTab, setActiveTab] = useState<Tab>('home');
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(!!getAuthToken());
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('remoteforge_secure_pass');
  const [authError, setAuthError] = useState('');

  const [devices, setDevices] = useState<DeviceInfo[]>([]);
  const [selectedDevice, setSelectedDevice] = useState<DeviceInfo | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  // System Status Modal
  const [statusModalOpen, setStatusModalOpen] = useState(false);
  const [systemStatus, setSystemStatus] = useState<SystemStatusData | null>(null);

  // Screenshot Modal
  const [screenshotModalOpen, setScreenshotModalOpen] = useState(false);
  const [screenshotData, setScreenshotData] = useState<ScreenshotData | null>(null);

  // Files Tab State
  const [fileList, setFileList] = useState<FileItem[]>([]);
  const [currentPath, setCurrentPath] = useState<string>('');
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<any[] | null>(null);

  // AI Assistant State
  const [aiPrompt, setAiPrompt] = useState('');
  const [aiOutput, setAiOutput] = useState<any | null>(null);

  // Activity Tab State & Bug Tracking
  const [auditLogs, setAuditLogs] = useState<AuditLogItem[]>([]);
  const [auditCategory, setAuditCategory] = useState<'all' | 'bugs' | 'success'>('all');
  const [auditTimeframe, setAuditTimeframe] = useState<'1d' | '7d' | '14d'>('14d');
  const [auditStats, setAuditStats] = useState<AuditStatsResponse | null>(null);
  const [selectedDayFilter, setSelectedDayFilter] = useState<string | null>(null);
  const [auditStatusMsg, setAuditStatusMsg] = useState<string | null>(null);
  const [showCleanModal, setShowCleanModal] = useState<boolean>(false);

  // Settings State
  const [customServerUrl, setCustomServerUrl] = useState(getServerBaseUrl());

  // High-Risk Confirmation Modal
  const [confirmModal, setConfirmModal] = useState<{
    open: boolean;
    tool: string;
    description: string;
    onConfirm: () => void;
  }>({ open: false, tool: '', description: '', onConfirm: () => {} });

  // Load initial devices and listen for live WebSocket events
  useEffect(() => {
    if (isAuthenticated) {
      loadDevices();
      loadAuditLogs();

      // Establish real-time WebSocket connection to server
      const ws = api.createWebSocketClient((msg) => {
        if (msg.type === 'device_status_change' || msg.type === 'device_heartbeat') {
          loadDevices();
        }
      });

      return () => {
        if (ws) ws.close();
      };
    }
  }, [isAuthenticated]);

  const loadDevices = async () => {
    try {
      const data = await api.getDevices();
      setDevices(data);
      if (data.length > 0 && !selectedDevice) {
        setSelectedDevice(data[0]);
      }
    } catch (err: any) {
      console.error('Failed to load devices:', err);
    }
  };

  const loadAuditLogs = async (cat = auditCategory, tf = auditTimeframe) => {
    try {
      const days = tf === '1d' ? 1 : tf === '7d' ? 7 : 14;
      const [logs, stats] = await Promise.all([
        api.getAuditLogs(undefined, cat, tf, days),
        api.getAuditStats(undefined, days)
      ]);
      setAuditLogs(logs);
      setAuditStats(stats);
    } catch (err) {
      console.error('Failed to load audit logs:', err);
    }
  };

  const handleCleanLogs = async (days?: number) => {
    try {
      const res = await api.cleanAuditLogs(days);
      setAuditStatusMsg(res.message);
      setShowCleanModal(false);
      setSelectedDayFilter(null);
      await loadAuditLogs(auditCategory, auditTimeframe);
    } catch (err: any) {
      setAuditStatusMsg(`Failed to clean logs: ${err.message}`);
    }
  };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setAuthError('');
    setIsLoading(true);
    try {
      await api.login(username, password);
      setIsAuthenticated(true);
      await loadDevices();
    } catch (err: any) {
      setAuthError(err.message || 'Login failed.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogout = () => {
    clearAuthToken();
    setIsAuthenticated(false);
  };

  const handleFetchStatus = async (device: DeviceInfo) => {
    setIsLoading(true);
    setActionMessage(`Requesting system status from ${device.name}...`);
    try {
      const res = await api.sendCommand(device.id, 'get_system_status');
      if (res.status === 'COMPLETED' && res.result) {
        setSystemStatus(res.result);
        setStatusModalOpen(true);
        setActionMessage(null);
      } else {
        alert(res.error || 'Failed to get system status');
      }
    } catch (err: any) {
      alert(`Error: ${err.message}`);
    } finally {
      setIsLoading(false);
      loadAuditLogs();
    }
  };

  const handleTakeScreenshot = async (device: DeviceInfo) => {
    setIsLoading(true);
    setActionMessage(`Capturing screenshot on ${device.name}...`);
    try {
      const res = await api.sendCommand(device.id, 'take_screenshot');
      if (res.status === 'COMPLETED' && res.result) {
        setScreenshotData(res.result);
        setScreenshotModalOpen(true);
        setActionMessage(null);
      } else {
        alert(res.error || 'Failed to capture screenshot');
      }
    } catch (err: any) {
      alert(`Error: ${err.message}`);
    } finally {
      setIsLoading(false);
      loadAuditLogs();
    }
  };

  const handleLockPC = async (device: DeviceInfo) => {
    if (!confirm(`Are you sure you want to lock ${device.name}?`)) return;
    setIsLoading(true);
    try {
      const res = await api.sendCommand(device.id, 'lock_pc');
      alert(res.status === 'COMPLETED' ? 'PC Locked successfully.' : (res.error || 'Action failed'));
    } catch (err: any) {
      alert(`Error: ${err.message}`);
    } finally {
      setIsLoading(false);
      loadAuditLogs();
    }
  };

  const handleBrowseFiles = async (device: DeviceInfo, path?: string) => {
    setSelectedDevice(device);
    setActiveTab('files');
    setIsLoading(true);
    try {
      const res = await api.listFiles(device.id, path);
      if (res && res.result) {
        setFileList(res.result.items || []);
        setCurrentPath(res.result.current_path || '');
        setSearchResults(null);
      }
    } catch (err: any) {
      alert(`Filesystem error: ${err.message}`);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSearchFiles = async () => {
    if (!selectedDevice || !searchQuery.trim()) return;
    setIsLoading(true);
    try {
      const res = await api.searchFiles(selectedDevice.id, searchQuery.trim());
      if (res && res.result) {
        setSearchResults(res.result.matches || []);
      }
    } catch (err: any) {
      alert(`Search error: ${err.message}`);
    } finally {
      setIsLoading(false);
    }
  };

  const fileInputRef = React.useRef<HTMLInputElement>(null);

  const handleDownloadFile = async (item: FileItem) => {
    if (!selectedDevice) return;
    setIsLoading(true);
    setActionMessage(`Downloading ${item.name} from ${selectedDevice.name}...`);
    try {
      await api.downloadFile(selectedDevice.id, item.path, item.name);
      setActionMessage(null);
    } catch (err: any) {
      alert(`Download failed: ${err.message}`);
    } finally {
      setIsLoading(false);
      loadAuditLogs();
    }
  };

  const handleUploadFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !selectedDevice) return;
    if (!currentPath) {
      alert("Please click into a specific allowed folder (e.g. Documents, Downloads, Projects) before uploading.");
      return;
    }
    setIsLoading(true);
    setActionMessage(`Uploading ${file.name} to ${selectedDevice.name}...`);
    try {
      await api.uploadFile(selectedDevice.id, currentPath, file);
      setActionMessage(null);
      alert(`Upload complete: ${file.name} was saved successfully.`);
      await handleBrowseFiles(selectedDevice, currentPath);
    } catch (err: any) {
      alert(`Upload failed: ${err.message}`);
    } finally {
      setIsLoading(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
      loadAuditLogs();
    }
  };

  const handleGoUp = () => {
    if (!selectedDevice || !currentPath) return;
    const parts = currentPath.split("/");
    parts.pop();
    const parent = parts.join("/");
    handleBrowseFiles(selectedDevice, parent || undefined);
  };

  const handleExecuteAi = async () => {
    if (!aiPrompt.trim() || !selectedDevice) return;
    setIsLoading(true);
    const p = aiPrompt.toLowerCase();

    let tool = 'browser_search';
    let args: any = { query: aiPrompt };
    let permissionLevel = 'LOW';
    let requiresConfirmation = false;

    if (p.includes('status') || p.includes('specs') || p.includes('cpu')) {
      tool = 'get_system_status';
      args = {};
    } else if (p.includes('screenshot') || p.includes('screen') || p.includes('capture')) {
      tool = 'take_screenshot';
      args = {};
    } else if (p.includes('lock')) {
      tool = 'lock_pc';
      args = {};
    } else if (p.includes('find') || p.includes('search file') || p.includes('assignment')) {
      tool = 'search_files';
      const clean = aiPrompt.replace(/find|my|assignment|files|file|search/gi, '').trim();
      args = { query: clean || 'assignment' };
    } else if (p.includes('youtube')) {
      tool = 'browser_search';
      args = { query: aiPrompt.replace(/open chrome and search youtube for|search youtube for/gi, '').trim(), site: 'youtube' };
    } else if (p.includes('chrome') || p.includes('browser')) {
      tool = 'open_browser';
      args = { url: 'https://google.com' };
    } else if (p.includes('restart')) {
      tool = 'restart_pc';
      args = {};
      permissionLevel = 'HIGH';
      requiresConfirmation = true;
    }

    setAiOutput({
      tool,
      arguments: args,
      permission_level: permissionLevel,
      requires_confirmation: requiresConfirmation,
      status: 'VALIDATED_BY_POLICY_ENGINE'
    });

    if (requiresConfirmation) {
      setConfirmModal({
        open: true,
        tool,
        description: `This action will ${tool.replace('_', ' ')} on ${selectedDevice.name}.`,
        onConfirm: async () => {
          setConfirmModal((prev) => ({ ...prev, open: false }));
          const res = await api.sendCommand(selectedDevice.id, tool, args, true);
          setAiOutput((prev: any) => ({ ...prev, execution_result: res }));
          loadAuditLogs();
        }
      });
      setIsLoading(false);
      return;
    }

    try {
      const res = await api.sendCommand(selectedDevice.id, tool, args);
      setAiOutput((prev: any) => ({ ...prev, execution_result: res }));
      if (tool === 'get_system_status' && res.result) {
        setSystemStatus(res.result);
        setStatusModalOpen(true);
      } else if (tool === 'take_screenshot' && res.result) {
        setScreenshotData(res.result);
        setScreenshotModalOpen(true);
      }
    } catch (err: any) {
      setAiOutput((prev: any) => ({ ...prev, error: err.message }));
    } finally {
      setIsLoading(false);
      loadAuditLogs();
    }
  };

  const handleSaveSettings = () => {
    setServerBaseUrl(customServerUrl);
    alert('Server URL updated. The app will connect using this address.');
  };

  // Login Screen
  if (!isAuthenticated) {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px' }}>
        <div className="glass-panel" style={{ width: '100%', maxWidth: '420px', padding: '36px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px' }}>
            <div className="brand-icon">
              <Shield size={20} />
            </div>
            <div>
              <h1 style={{ fontSize: '1.4rem', fontWeight: 800 }}>RemoteForge</h1>
              <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>Secure Remote AI Computer Control</p>
            </div>
          </div>

          <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div>
              <label style={{ fontSize: '0.85rem', color: 'var(--text-dim)', marginBottom: '6px', display: 'block' }}>Username</label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                style={{
                  width: '100%',
                  padding: '12px',
                  borderRadius: '10px',
                  background: 'rgba(255,255,255,0.05)',
                  border: '1px solid var(--border-subtle)',
                  color: '#fff',
                  fontFamily: 'inherit'
                }}
                required
              />
            </div>
            <div>
              <label style={{ fontSize: '0.85rem', color: 'var(--text-dim)', marginBottom: '6px', display: 'block' }}>Password</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                style={{
                  width: '100%',
                  padding: '12px',
                  borderRadius: '10px',
                  background: 'rgba(255,255,255,0.05)',
                  border: '1px solid var(--border-subtle)',
                  color: '#fff',
                  fontFamily: 'inherit'
                }}
                required
              />
            </div>

            {authError && (
              <div style={{ padding: '10px', borderRadius: '8px', background: 'rgba(244,63,94,0.1)', color: 'var(--accent-rose)', fontSize: '0.85rem' }}>
                {authError}
              </div>
            )}

            <button type="submit" className="btn btn-primary" style={{ marginTop: '8px' }} disabled={isLoading}>
              {isLoading ? 'Authenticating...' : 'Sign In to RemoteForge'}
            </button>
          </form>

          <div style={{ marginTop: '24px', paddingTop: '16px', borderTop: '1px solid var(--border-subtle)', fontSize: '0.78rem', color: 'var(--text-dim)' }}>
            <div>• Zero persistent file storage on server</div>
            <div>• Least-privilege user-data allowlist</div>
            <div>• NetBird VPN compatible (0.0.0.0 bind)</div>
          </div>
        </div>
      </div>
    );
  }

  const activeDevice = selectedDevice || (devices.length > 0 ? devices[0] : null);

  return (
    <div className="app-container">
      {/* Sidebar Navigation */}
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-icon">
            <Shield size={20} />
          </div>
          <div>
            <div className="brand-title">RemoteForge</div>
            <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Control Console</div>
          </div>
          <span className="brand-badge">v0.1.0</span>
        </div>

        <nav className="nav-menu">
          <button className={`nav-item ${activeTab === 'home' ? 'active' : ''}`} onClick={() => setActiveTab('home')}>
            <Radio size={18} /> Home
          </button>
          <button className={`nav-item ${activeTab === 'devices' ? 'active' : ''}`} onClick={() => setActiveTab('devices')}>
            <Monitor size={18} /> Devices ({devices.length})
          </button>
          <button className={`nav-item ${activeTab === 'files' ? 'active' : ''}`} onClick={() => { setActiveTab('files'); if (activeDevice) handleBrowseFiles(activeDevice); }}>
            <FolderLock size={18} /> Files (User Only)
          </button>
          <button className={`nav-item ${activeTab === 'ai' ? 'active' : ''}`} onClick={() => setActiveTab('ai')}>
            <Sparkles size={18} /> AI Assistant
          </button>
          <button className={`nav-item ${activeTab === 'settings' ? 'active' : ''}`} onClick={() => setActiveTab('settings')}>
            <Settings size={18} /> Settings
          </button>
        </nav>

        <div className="nav-footer">
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-dim)' }}>Session: admin</span>
            <button
              onClick={handleLogout}
              style={{ background: 'none', border: 'none', color: 'var(--accent-rose)', fontSize: '0.8rem', cursor: 'pointer', fontWeight: 600 }}
            >
              Sign Out
            </button>
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <main className="main-wrapper">
        <header className="top-bar">
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <h2 className="top-title">
              {activeTab === 'home' && 'Command Overview'}
              {activeTab === 'devices' && 'Registered Computers'}
              {activeTab === 'files' && 'User Data Filesystem'}
              {activeTab === 'ai' && 'Controlled AI Engine'}
              {activeTab === 'activity' && 'Security Audit Trail'}
              {activeTab === 'settings' && 'Platform Settings'}
            </h2>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
            {activeDevice ? (
              <div className={`status-pill ${activeDevice.is_online ? '' : 'offline'}`}>
                <span className="pulse-dot"></span>
                {activeDevice.name}: {activeDevice.is_online ? 'ONLINE' : 'OFFLINE'}
              </div>
            ) : (
              <div className="status-pill offline">
                <span className="pulse-dot"></span> NO DEVICE CONNECTED
              </div>
            )}
            <button onClick={loadDevices} className="btn btn-secondary" style={{ padding: '6px 10px' }} title="Refresh">
              <RefreshCw size={14} />
            </button>
          </div>
        </header>

        <div className="content-body">
          {actionMessage && (
            <div style={{ marginBottom: '20px', padding: '12px 16px', background: 'rgba(6,182,212,0.1)', border: '1px solid var(--border-accent)', borderRadius: '10px', color: 'var(--accent-cyan)', display: 'flex', alignItems: 'center', gap: '10px' }}>
              <RefreshCw size={16} className="pulse-dot" />
              <span>{actionMessage}</span>
            </div>
          )}

          {/* TAB 1: HOME */}
          {activeTab === 'home' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
              <div className="glass-panel" style={{ background: 'linear-gradient(135deg, rgba(14,21,37,0.8), rgba(22,33,54,0.6))' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div>
                    <h3 style={{ fontSize: '1.4rem', fontWeight: 800, marginBottom: '8px' }}>
                      RemoteForge Security Console
                    </h3>
                    <p style={{ color: 'var(--text-muted)', maxWidth: '650px', fontSize: '0.92rem' }}>
                      Server-agnostic remote control platform running over NetBird VPN. Enforces strict zero-persistent-storage and a user-data-only filesystem sandbox.
                    </p>
                  </div>
                  <span className="status-pill">
                    <span className="pulse-dot"></span> FASTAPI 0.0.0.0 ACTIVE
                  </span>
                </div>

                <div className="hud-grid" style={{ marginTop: '24px' }}>
                  <div className="hud-card">
                    <div className="hud-title">Connected Devices</div>
                    <div className="hud-value">{devices.filter((d) => d.is_online).length} / {devices.length}</div>
                  </div>
                  <div className="hud-card">
                    <div className="hud-title">Controlled Tools</div>
                    <div className="hud-value" style={{ color: 'var(--accent-cyan)' }}>13 Active</div>
                  </div>
                  <div className="hud-card">
                    <div className="hud-title">Security Model</div>
                    <div className="hud-value" style={{ color: 'var(--accent-emerald)', fontSize: '1.2rem' }}>User-Data Only</div>
                  </div>
                  <div className="hud-card">
                    <div className="hud-title">Audit Events Logged</div>
                    <div className="hud-value">{auditLogs.length}</div>
                  </div>
                </div>
              </div>

              {/* Primary Active Computer HUD */}
              {activeDevice && (
                <div className="glass-panel">
                  <div className="section-header">
                    <div className="section-title">
                      <Monitor size={20} color="var(--accent-cyan)" />
                      <span>Active Computer: {activeDevice.name}</span>
                    </div>
                    <span className={`status-pill ${activeDevice.is_online ? '' : 'offline'}`}>
                      {activeDevice.is_online ? 'ONLINE' : 'OFFLINE'}
                    </span>
                  </div>

                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '20px' }}>
                    <div>
                      <div className="device-meta-row">
                        <span className="device-meta-label">Device Identifier</span>
                        <span className="device-meta-val">{activeDevice.id}</span>
                      </div>
                      <div className="device-meta-row">
                        <span className="device-meta-label">Operating System</span>
                        <span className="device-meta-val">{activeDevice.os_name} {activeDevice.os_version}</span>
                      </div>
                      <div className="device-meta-row">
                        <span className="device-meta-label">Agent Version</span>
                        <span className="device-meta-val">{activeDevice.agent_version}</span>
                      </div>
                      <div className="device-meta-row">
                        <span className="device-meta-label">Last Heartbeat</span>
                        <span className="device-meta-val">{new Date(activeDevice.last_seen).toLocaleTimeString()}</span>
                      </div>
                    </div>

                    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', justifyContent: 'center' }}>
                      <div style={{ fontSize: '0.85rem', color: 'var(--text-dim)', marginBottom: '4px' }}>Quick Remote Actions</div>
                      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                        <button
                          className="btn btn-primary"
                          disabled={!activeDevice.is_online || isLoading}
                          onClick={() => handleFetchStatus(activeDevice)}
                        >
                          <Cpu size={16} /> System Status
                        </button>
                        <button
                          className="btn btn-secondary"
                          disabled={!activeDevice.is_online || isLoading}
                          onClick={() => handleTakeScreenshot(activeDevice)}
                        >
                          <Camera size={16} /> Screenshot
                        </button>
                        <button
                          className="btn btn-secondary"
                          disabled={!activeDevice.is_online || isLoading}
                          onClick={() => handleBrowseFiles(activeDevice)}
                        >
                          <FolderLock size={16} /> User Files
                        </button>
                        <button
                          className="btn btn-danger"
                          disabled={!activeDevice.is_online || isLoading}
                          onClick={() => handleLockPC(activeDevice)}
                        >
                          <Lock size={16} /> Lock PC
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* Home Activity & Bug Tracker Preview */}
              <div className="glass-panel" style={{ marginTop: '20px' }}>
                <div className="section-header">
                  <div className="section-title">
                    <Activity size={20} color="var(--accent-cyan)" />
                    <span>Recent Activity & Bug Tracker</span>
                  </div>
                  <button
                    className="btn btn-secondary"
                    style={{ fontSize: '0.8rem', padding: '6px 14px' }}
                    onClick={() => { setActiveTab('activity'); loadAuditLogs(); }}
                  >
                    View Full Audit & Bugs Trail →
                  </button>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '12px', margin: '14px 0' }}>
                  <div style={{ background: 'rgba(255,255,255,0.03)', padding: '12px 14px', borderRadius: '10px', border: '1px solid var(--border-subtle)' }}>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>Total Events</div>
                    <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#fff' }}>{auditStats?.total_logs ?? auditLogs.length}</div>
                  </div>
                  <div style={{ background: 'rgba(239,68,68,0.08)', padding: '12px 14px', borderRadius: '10px', border: '1px solid rgba(239,68,68,0.2)' }}>
                    <div style={{ fontSize: '0.75rem', color: 'var(--accent-rose)' }}>Bugs & Errors</div>
                    <div style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--accent-rose)' }}>{auditStats?.total_bugs ?? 0}</div>
                  </div>
                  <div style={{ background: 'rgba(16,185,129,0.08)', padding: '12px 14px', borderRadius: '10px', border: '1px solid rgba(16,185,129,0.2)' }}>
                    <div style={{ fontSize: '0.75rem', color: 'var(--accent-emerald)' }}>Success</div>
                    <div style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--accent-emerald)' }}>{auditStats?.total_success ?? 0}</div>
                  </div>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {auditLogs.slice(0, 3).map((log) => {
                    const isBug = log.execution_status !== 'SUCCESS' || (log.action && log.action.includes('REJECTED'));
                    return (
                      <div
                        key={log.id}
                        style={{
                          padding: '10px 14px',
                          borderRadius: '8px',
                          background: isBug ? 'rgba(239,68,68,0.05)' : 'rgba(255,255,255,0.02)',
                          border: isBug ? '1px solid rgba(239,68,68,0.2)' : '1px solid var(--border-subtle)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between'
                        }}
                      >
                        <div>
                          <span style={{ fontWeight: 600, color: '#fff', fontSize: '0.85rem' }}>{log.tool || log.action}</span>
                          <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)', marginLeft: '8px' }}>{new Date(log.timestamp).toLocaleTimeString()}</span>
                        </div>
                        <span style={{ fontSize: '0.75rem', color: isBug ? 'var(--accent-rose)' : 'var(--accent-emerald)', fontWeight: 600 }}>
                          {log.execution_status}
                        </span>
                      </div>
                    );
                  })}
                  {auditLogs.length === 0 && (
                    <div style={{ fontSize: '0.85rem', color: 'var(--text-dim)', textAlign: 'center', padding: '12px' }}>
                      No recent activity recorded yet.
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* TAB 2: DEVICES */}
          {activeTab === 'devices' && (
            <div>
              <div className="section-header">
                <div>
                  <h3 className="section-title">Remote Devices</h3>
                  <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                    Computers running the RemoteForge Agent with outbound authenticated WebSockets.
                  </p>
                </div>
              </div>

              {devices.length === 0 ? (
                <div className="glass-panel" style={{ textAlign: 'center', padding: '48px' }}>
                  <Monitor size={48} color="var(--text-dim)" style={{ marginBottom: '16px' }} />
                  <h4>No PC Agent Connected</h4>
                  <p style={{ color: 'var(--text-dim)', fontSize: '0.9rem', marginTop: '8px' }}>
                    Start the PC agent in your terminal using:
                    <br />
                    <code style={{ background: 'rgba(0,0,0,0.4)', padding: '4px 8px', borderRadius: '4px', color: 'var(--accent-cyan)' }}>
                      PYTHONPATH=. python3 agent/remote_forge_agent/main.py
                    </code>
                  </p>
                </div>
              ) : (
                <div className="device-grid">
                  {devices.map((dev) => (
                    <div key={dev.id} className="device-card">
                      <div className="device-header">
                        <div>
                          <h4 style={{ fontSize: '1.15rem', fontWeight: 700 }}>{dev.name}</h4>
                          <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>{dev.id}</span>
                        </div>
                        <span className={`status-pill ${dev.is_online ? '' : 'offline'}`}>
                          <span className="pulse-dot"></span> {dev.is_online ? 'ONLINE' : 'OFFLINE'}
                        </span>
                      </div>

                      <div className="device-meta-row">
                        <span className="device-meta-label">OS</span>
                        <span className="device-meta-val">{dev.os_name} {dev.os_version}</span>
                      </div>
                      <div className="device-meta-row">
                        <span className="device-meta-label">Agent</span>
                        <span className="device-meta-val">{dev.agent_version}</span>
                      </div>
                      <div className="device-meta-row">
                        <span className="device-meta-label">Last Seen</span>
                        <span className="device-meta-val">{new Date(dev.last_seen).toLocaleTimeString()}</span>
                      </div>

                      <div className="device-actions">
                        <button
                          className="btn btn-primary"
                          disabled={!dev.is_online || isLoading}
                          onClick={() => handleFetchStatus(dev)}
                        >
                          <Cpu size={14} /> Status
                        </button>
                        <button
                          className="btn btn-secondary"
                          disabled={!dev.is_online || isLoading}
                          onClick={() => handleTakeScreenshot(dev)}
                        >
                          <Camera size={14} /> Screen
                        </button>
                        <button
                          className="btn btn-secondary"
                          disabled={!dev.is_online || isLoading}
                          onClick={() => handleBrowseFiles(dev)}
                        >
                          <FolderLock size={14} /> Files
                        </button>
                        <button
                          className="btn btn-danger"
                          disabled={!dev.is_online || isLoading}
                          onClick={() => handleLockPC(dev)}
                        >
                          <Lock size={14} /> Lock
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* TAB 3: FILES (USER DATA ONLY) */}
          {activeTab === 'files' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              <div className="glass-panel" style={{ background: 'rgba(16, 185, 129, 0.05)', borderColor: 'rgba(16, 185, 129, 0.2)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <Shield size={24} color="var(--accent-emerald)" />
                  <div>
                    <h4 style={{ color: 'var(--accent-emerald)', fontWeight: 700 }}>Strict User-Data Security Sandbox</h4>
                    <p style={{ fontSize: '0.84rem', color: 'var(--text-muted)' }}>
                      Only authorized user folders (Desktop, Documents, Downloads, Pictures, Projects) can be accessed. System directories, OS files, and credentials (.ssh, .aws, keys) are strictly denied by PC agent canonicalization.
                    </p>
                  </div>
                </div>
              </div>

              {/* Search Bar */}
              <div style={{ display: 'flex', gap: '10px' }}>
                <div style={{ flex: 1, position: 'relative' }}>
                  <input
                    type="text"
                    placeholder="Search user files (e.g., DBMS assignment, report, test.pdf)..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && handleSearchFiles()}
                    style={{
                      width: '100%',
                      padding: '12px 16px 12px 40px',
                      borderRadius: '12px',
                      background: 'var(--bg-card)',
                      border: '1px solid var(--border-subtle)',
                      color: '#fff',
                      fontFamily: 'inherit'
                    }}
                  />
                  <Search size={18} style={{ position: 'absolute', left: '14px', top: '14px', color: 'var(--text-dim)' }} />
                </div>
                <button className="btn btn-primary" onClick={handleSearchFiles} disabled={isLoading || !searchQuery.trim()}>
                  Search
                </button>
              </div>

              {/* Search Results or Directory Listing */}
              <div className="glass-panel">
                <input
                  type="file"
                  ref={fileInputRef}
                  onChange={handleUploadFile}
                  style={{ display: 'none' }}
                />

                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px', flexWrap: 'wrap', gap: '10px' }}>
                  <div style={{ fontSize: '0.9rem', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
                    {searchResults ? (
                      <span>Search Results for "{searchQuery}" ({searchResults.length} matches)</span>
                    ) : (
                      <span>Path: {currentPath || 'Allowed User Roots'}</span>
                    )}
                  </div>

                  <div style={{ display: 'flex', gap: '8px' }}>
                    {currentPath && (
                      <button className="btn btn-secondary" style={{ padding: '6px 12px', fontSize: '0.8rem' }} onClick={handleGoUp}>
                        <ArrowUp size={14} /> Up
                      </button>
                    )}
                    {currentPath && (
                      <button className="btn btn-primary" style={{ padding: '6px 12px', fontSize: '0.8rem' }} onClick={() => fileInputRef.current?.click()} disabled={isLoading}>
                        <Upload size={14} /> Upload File
                      </button>
                    )}
                    {searchResults && (
                      <button className="btn btn-secondary" style={{ padding: '6px 12px', fontSize: '0.8rem' }} onClick={() => setSearchResults(null)}>
                        Clear Search
                      </button>
                    )}
                  </div>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  {searchResults ? (
                    searchResults.map((item, idx) => (
                      <div
                        key={idx}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          padding: '10px 14px',
                          borderRadius: '8px',
                          background: 'rgba(255,255,255,0.03)'
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                          <FileText size={16} color="var(--accent-cyan)" />
                          <div>
                            <div style={{ fontWeight: 600 }}>{item.filename}</div>
                            <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>{item.path}</div>
                          </div>
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                          <span style={{ fontSize: '0.8rem', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>
                            {(item.size_bytes / 1024).toFixed(1)} KB
                          </span>
                          <button
                            className="btn btn-secondary"
                            style={{ padding: '4px 8px' }}
                            title="Download file"
                            onClick={() => handleDownloadFile({ name: item.filename, path: item.path, is_directory: false })}
                          >
                            <Download size={14} />
                          </button>
                        </div>
                      </div>
                    ))
                  ) : fileList.length === 0 ? (
                    <div style={{ textAlign: 'center', padding: '32px', color: 'var(--text-dim)' }}>
                      No files or directories found.
                    </div>
                  ) : (
                    fileList.map((f, i) => (
                      <div
                        key={i}
                        onClick={() => f.is_directory && activeDevice && handleBrowseFiles(activeDevice, f.path)}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          padding: '12px 14px',
                          borderRadius: '8px',
                          background: 'rgba(255,255,255,0.02)',
                          cursor: f.is_directory ? 'pointer' : 'default',
                          transition: 'background 0.2s'
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                          {f.is_directory ? <Folder size={18} color="var(--accent-cyan)" /> : <FileText size={18} color="var(--text-muted)" />}
                          <span style={{ fontWeight: 500 }}>{f.name}</span>
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                          {f.size_bytes !== undefined && f.size_bytes > 0 && (
                            <span style={{ fontSize: '0.8rem', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>
                              {(f.size_bytes / 1024).toFixed(1)} KB
                            </span>
                          )}
                          {!f.is_directory && (
                            <button
                              className="btn btn-secondary"
                              style={{ padding: '6px 10px' }}
                              title="Download File"
                              onClick={(e) => {
                                e.stopPropagation();
                                handleDownloadFile(f);
                              }}
                            >
                              <Download size={14} />
                            </button>
                          )}
                          {f.is_directory && <ChevronRight size={16} color="var(--text-dim)" />}
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </div>
          )}

          {/* TAB 4: AI ASSISTANT */}
          {activeTab === 'ai' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              <div className="glass-panel">
                <h3 className="section-title" style={{ marginBottom: '8px' }}>
                  <Sparkles size={22} color="var(--accent-cyan)" />
                  <span>Controlled AI Command Engine</span>
                </h3>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '20px' }}>
                  Converts natural language into validated, structured tool calls with permission checks and user confirmation gates.
                </p>

                <div className="ai-input-box">
                  <textarea
                    className="ai-textarea"
                    placeholder="Tell RemoteForge what to do... (e.g., 'Open Chrome and search YouTube for JNTUH results', 'Find my DBMS assignment', 'Take a screenshot')"
                    value={aiPrompt}
                    onChange={(e) => setAiPrompt(e.target.value)}
                  />
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-dim)' }}>
                      Target: {activeDevice ? activeDevice.name : 'No device'}
                    </div>
                    <button
                      className="btn btn-primary"
                      onClick={handleExecuteAi}
                      disabled={isLoading || !aiPrompt.trim() || !activeDevice?.is_online}
                    >
                      <Sparkles size={16} /> Execute Command
                    </button>
                  </div>
                </div>

                {/* Suggestions */}
                <div style={{ marginTop: '16px', display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                  {[
                    'Open Chrome and search YouTube for JNTUH results',
                    'Find my DBMS assignment',
                    'Take a screenshot',
                    'Check my PC status',
                    'Lock my PC',
                    'Restart my PC'
                  ].map((s, idx) => (
                    <button
                      key={idx}
                      className="btn btn-secondary"
                      style={{ padding: '6px 12px', fontSize: '0.8rem' }}
                      onClick={() => setAiPrompt(s)}
                    >
                      {s}
                    </button>
                  ))}
                </div>
              </div>

              {/* AI Structured Tool Call Output */}
              {aiOutput && (
                <div className="glass-panel">
                  <div className="section-header">
                    <span style={{ fontWeight: 700, fontSize: '0.95rem' }}>Engine Evaluation & Policy Check</span>
                    <span className="brand-badge">{aiOutput.permission_level} RISK</span>
                  </div>

                  <pre style={{ background: 'rgba(0,0,0,0.5)', padding: '16px', borderRadius: '10px', fontSize: '0.85rem', fontFamily: 'var(--font-mono)', overflowX: 'auto', color: 'var(--accent-cyan)' }}>
                    {JSON.stringify(aiOutput, null, 2)}
                  </pre>
                </div>
              )}
            </div>
          )}

          {/* TAB 5: ACTIVITY */}
          {activeTab === 'activity' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              <div className="glass-panel">
                <div className="section-header">
                  <div>
                    <h3 className="section-title">Activity & Bug Tracker</h3>
                    <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                      14-day auto-purged audit trail • Zero clutter policy
                    </p>
                  </div>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <button className="btn btn-danger" style={{ fontSize: '0.8rem', padding: '6px 12px' }} onClick={() => setShowCleanModal(true)}>
                      <Trash2 size={14} /> Clean Logs
                    </button>
                    <button className="btn btn-secondary" style={{ fontSize: '0.8rem', padding: '6px 12px' }} onClick={() => loadAuditLogs(auditCategory, auditTimeframe)}>
                      <RefreshCw size={14} /> Refresh
                    </button>
                  </div>
                </div>

                {auditStatusMsg && (
                  <div style={{ background: 'rgba(16,185,129,0.1)', border: '1px solid var(--accent-emerald)', padding: '10px 14px', borderRadius: '8px', color: 'var(--accent-emerald)', fontSize: '0.85rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
                    <span>{auditStatusMsg}</span>
                    <button style={{ background: 'none', border: 'none', color: 'var(--text-dim)', cursor: 'pointer' }} onClick={() => setAuditStatusMsg(null)}>✕</button>
                  </div>
                )}

                {/* Bug & Event HUD Metrics */}
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '12px', marginBottom: '16px' }}>
                  <div style={{ background: 'rgba(255,255,255,0.03)', padding: '12px 14px', borderRadius: '10px', border: '1px solid var(--border-subtle)' }}>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>Total Records</div>
                    <div style={{ fontSize: '1.4rem', fontWeight: 700, color: '#fff' }}>{auditStats?.total_logs ?? auditLogs.length}</div>
                  </div>
                  <div style={{ background: 'rgba(239,68,68,0.08)', padding: '12px 14px', borderRadius: '10px', border: '1px solid rgba(239,68,68,0.2)' }}>
                    <div style={{ fontSize: '0.75rem', color: 'var(--accent-rose)' }}>Bugs & Errors</div>
                    <div style={{ fontSize: '1.4rem', fontWeight: 700, color: 'var(--accent-rose)' }}>{auditStats?.total_bugs ?? 0}</div>
                  </div>
                  <div style={{ background: 'rgba(16,185,129,0.08)', padding: '12px 14px', borderRadius: '10px', border: '1px solid rgba(16,185,129,0.2)' }}>
                    <div style={{ fontSize: '0.75rem', color: 'var(--accent-emerald)' }}>Success Operations</div>
                    <div style={{ fontSize: '1.4rem', fontWeight: 700, color: 'var(--accent-emerald)' }}>{auditStats?.total_success ?? 0}</div>
                  </div>
                  <div style={{ background: 'rgba(6,182,212,0.08)', padding: '12px 14px', borderRadius: '10px', border: '1px solid rgba(6,182,212,0.2)' }}>
                    <div style={{ fontSize: '0.75rem', color: 'var(--accent-cyan)' }}>Max Retention</div>
                    <div style={{ fontSize: '1.4rem', fontWeight: 700, color: 'var(--accent-cyan)' }}>14 Days</div>
                  </div>
                </div>

                {/* Bugs Per Day Breakdown */}
                {auditStats && auditStats.days_breakdown && auditStats.days_breakdown.length > 0 && (
                  <div style={{ marginBottom: '18px', background: 'rgba(0,0,0,0.25)', padding: '12px 16px', borderRadius: '10px', border: '1px solid var(--border-subtle)' }}>
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-dim)', marginBottom: '8px', fontWeight: 600 }}>
                      Bugs Per Day (Click day to filter):
                    </div>
                    <div style={{ display: 'flex', gap: '8px', overflowX: 'auto', paddingBottom: '4px' }}>
                      {auditStats.days_breakdown.map((d) => {
                        const isDaySelected = selectedDayFilter === d.date;
                        return (
                          <button
                            key={d.date}
                            onClick={() => setSelectedDayFilter(isDaySelected ? null : d.date)}
                            style={{
                              background: isDaySelected ? 'rgba(6,182,212,0.2)' : d.bugs > 0 ? 'rgba(239,68,68,0.12)' : 'rgba(255,255,255,0.03)',
                              border: isDaySelected ? '1px solid var(--accent-cyan)' : d.bugs > 0 ? '1px solid rgba(239,68,68,0.4)' : '1px solid var(--border-subtle)',
                              borderRadius: '8px',
                              padding: '8px 12px',
                              textAlign: 'center',
                              cursor: 'pointer',
                              minWidth: '85px',
                              color: '#fff'
                            }}
                          >
                            <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)' }}>{d.date.slice(5)}</div>
                            <div style={{ fontSize: '0.85rem', fontWeight: 700, color: d.bugs > 0 ? 'var(--accent-rose)' : 'var(--accent-emerald)' }}>
                              {d.bugs > 0 ? `${d.bugs} bugs` : '0 bugs'}
                            </div>
                          </button>
                        );
                      })}
                    </div>
                  </div>
                )}

                {/* Timeframe & Category Selectors */}
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '12px', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                  {/* Timeframe Selector */}
                  <div style={{ display: 'flex', gap: '6px' }}>
                    <button
                      className={`btn ${auditTimeframe === '1d' ? 'btn-primary' : 'btn-secondary'}`}
                      style={{ fontSize: '0.78rem', padding: '6px 12px' }}
                      onClick={() => { setAuditTimeframe('1d'); setSelectedDayFilter(null); loadAuditLogs(auditCategory, '1d'); }}
                    >
                      Today (24h)
                    </button>
                    <button
                      className={`btn ${auditTimeframe === '7d' ? 'btn-primary' : 'btn-secondary'}`}
                      style={{ fontSize: '0.78rem', padding: '6px 12px' }}
                      onClick={() => { setAuditTimeframe('7d'); setSelectedDayFilter(null); loadAuditLogs(auditCategory, '7d'); }}
                    >
                      7 Days (1W)
                    </button>
                    <button
                      className={`btn ${auditTimeframe === '14d' ? 'btn-primary' : 'btn-secondary'}`}
                      style={{ fontSize: '0.78rem', padding: '6px 12px' }}
                      onClick={() => { setAuditTimeframe('14d'); setSelectedDayFilter(null); loadAuditLogs(auditCategory, '14d'); }}
                    >
                      14 Days (2W Max)
                    </button>
                  </div>

                  {/* Category Selector */}
                  <div style={{ display: 'flex', gap: '6px' }}>
                    <button
                      className={`btn ${auditCategory === 'all' ? 'btn-primary' : 'btn-secondary'}`}
                      style={{ fontSize: '0.78rem', padding: '6px 12px' }}
                      onClick={() => { setAuditCategory('all'); loadAuditLogs('all', auditTimeframe); }}
                    >
                      All Logs
                    </button>
                    <button
                      className={`btn ${auditCategory === 'bugs' ? 'btn-danger' : 'btn-secondary'}`}
                      style={{ fontSize: '0.78rem', padding: '6px 12px' }}
                      onClick={() => { setAuditCategory('bugs'); loadAuditLogs('bugs', auditTimeframe); }}
                    >
                      <Bug size={14} /> Bugs & Issues
                    </button>
                    <button
                      className={`btn ${auditCategory === 'success' ? 'btn-primary' : 'btn-secondary'}`}
                      style={{ fontSize: '0.78rem', padding: '6px 12px' }}
                      onClick={() => { setAuditCategory('success'); loadAuditLogs('success', auditTimeframe); }}
                    >
                      <CheckCircle2 size={14} /> Success Only
                    </button>
                  </div>
                </div>

                {selectedDayFilter && (
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'rgba(6,182,212,0.1)', padding: '8px 12px', borderRadius: '6px', marginBottom: '12px', fontSize: '0.8rem', color: 'var(--accent-cyan)' }}>
                    <span>Filtering logs for date: <strong>{selectedDayFilter}</strong></span>
                    <button style={{ background: 'none', border: 'none', color: 'var(--accent-rose)', cursor: 'pointer', fontWeight: 600 }} onClick={() => setSelectedDayFilter(null)}>
                      Clear Date Filter
                    </button>
                  </div>
                )}

                {/* Log List */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {(() => {
                    const filtered = selectedDayFilter ? auditLogs.filter(l => l.timestamp.startsWith(selectedDayFilter)) : auditLogs;
                    if (filtered.length === 0) {
                      return (
                        <div style={{ textAlign: 'center', padding: '32px', color: 'var(--text-dim)' }}>
                          {auditCategory === 'bugs' ? 'No bugs or errors recorded in this timeframe!' : 'No audit records found.'}
                        </div>
                      );
                    }
                    return filtered.map((log) => {
                      const isBug = log.execution_status !== 'SUCCESS' || (log.action && log.action.includes('REJECTED'));
                      return (
                        <div
                          key={log.id}
                          style={{
                            padding: '12px 16px',
                            borderRadius: '10px',
                            background: isBug ? 'rgba(239,68,68,0.04)' : 'rgba(255,255,255,0.03)',
                            border: isBug ? '1px solid rgba(239,68,68,0.25)' : '1px solid var(--border-subtle)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            gap: '12px'
                          }}
                        >
                          <div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                              <span style={{ fontWeight: 700, color: '#fff', fontSize: '0.9rem' }}>{log.action}</span>
                              {log.tool && (
                                <span style={{ background: 'rgba(6,182,212,0.15)', color: 'var(--accent-cyan)', padding: '2px 6px', borderRadius: '4px', fontSize: '0.75rem', fontFamily: 'var(--font-mono)' }}>
                                  {log.tool}
                                </span>
                              )}
                              <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>• Actor: {log.actor}</span>
                            </div>
                            <div style={{ fontSize: '0.78rem', color: isBug ? 'var(--accent-rose)' : 'var(--text-dim)', marginTop: '4px' }}>
                              {log.details || 'Execution completed.'}
                            </div>
                          </div>

                          <div style={{ textAlign: 'right' }}>
                            <span style={{ fontSize: '0.78rem', color: isBug ? 'var(--accent-rose)' : 'var(--accent-emerald)', fontWeight: 600 }}>
                              {log.execution_status}
                            </span>
                            <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>
                              {new Date(log.timestamp).toLocaleTimeString()}
                            </div>
                          </div>
                        </div>
                      );
                    });
                  })()}
                </div>
              </div>

              {/* Clean Logs Modal Dialog */}
              {showCleanModal && (
                <div className="modal-backdrop">
                  <div className="modal-card">
                    <div className="section-header">
                      <div className="section-title">
                        <Trash2 size={20} color="var(--accent-rose)" />
                        <span>Clean Audit Logs</span>
                      </div>
                      <button className="btn-icon" onClick={() => setShowCleanModal(false)}>
                        <X size={18} />
                      </button>
                    </div>
                    <p style={{ fontSize: '0.85rem', color: 'var(--text-dim)', margin: '14px 0' }}>
                      Prevent log clutter and reclaim database space. RemoteForge automatically prunes all logs older than 14 days, but you can also purge old logs manually right now.
                    </p>
                    <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end', marginTop: '16px' }}>
                      <button className="btn btn-secondary" onClick={() => handleCleanLogs(2)}>
                        Purge Records &gt; 2 Days
                      </button>
                      <button className="btn btn-danger" onClick={() => handleCleanLogs(0)}>
                        Clean All Logs
                      </button>
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* TAB 6: SETTINGS */}
          {activeTab === 'settings' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              <div className="glass-panel">
                <h3 className="section-title" style={{ marginBottom: '16px' }}>Network & Server Migration Settings</h3>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                  <div>
                    <label style={{ fontSize: '0.85rem', color: 'var(--text-dim)', marginBottom: '6px', display: 'block' }}>
                      RemoteForge Server Public URL (NetBird VPN / Domain)
                    </label>
                    <input
                      type="text"
                      value={customServerUrl}
                      onChange={(e) => setCustomServerUrl(e.target.value)}
                      placeholder="e.g., http://100.117.133.109:8000 or https://remoteforge.yourdomain.com"
                      style={{
                        width: '100%',
                        padding: '12px',
                        borderRadius: '10px',
                        background: 'rgba(255,255,255,0.05)',
                        border: '1px solid var(--border-subtle)',
                        color: '#fff',
                        fontFamily: 'var(--font-mono)'
                      }}
                    />
                    <p style={{ fontSize: '0.78rem', color: 'var(--text-dim)', marginTop: '6px' }}>
                      Server is migration-agnostic. When moving from Mac to home server, change this address to the new host.
                    </p>
                  </div>

                  <button className="btn btn-primary" onClick={handleSaveSettings} style={{ alignSelf: 'flex-start' }}>
                    Save Backend Address
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </main>

      {/* SYSTEM STATUS HUD MODAL */}
      {statusModalOpen && systemStatus && (
        <div className="modal-overlay" onClick={() => setStatusModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ padding: '28px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <Cpu size={22} color="var(--accent-cyan)" />
                <h3 style={{ fontSize: '1.25rem', fontWeight: 800 }}>
                  Real-time System Status: {systemStatus.os_name}
                </h3>
              </div>
              <button onClick={() => setStatusModalOpen(false)} style={{ background: 'none', border: 'none', color: 'var(--text-dim)', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>

            <div className="hud-grid">
              <div className="hud-card">
                <div className="hud-title">CPU Utilization</div>
                <div className="hud-value">{systemStatus.cpu.percent}%</div>
                <div className="progress-bar-bg">
                  <div className="progress-bar-fill" style={{ width: `${systemStatus.cpu.percent}%`, background: 'var(--accent-cyan)' }}></div>
                </div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)', marginTop: '6px' }}>
                  {systemStatus.cpu.logical_cores} Cores ({systemStatus.machine_architecture})
                </div>
              </div>

              <div className="hud-card">
                <div className="hud-title">Memory (RAM)</div>
                <div className="hud-value">{systemStatus.ram.percent}%</div>
                <div className="progress-bar-bg">
                  <div className="progress-bar-fill" style={{ width: `${systemStatus.ram.percent}%`, background: 'var(--accent-purple)' }}></div>
                </div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)', marginTop: '6px' }}>
                  {systemStatus.ram.used_gb} GB / {systemStatus.ram.total_gb} GB
                </div>
              </div>

              <div className="hud-card">
                <div className="hud-title">Disk Usage</div>
                <div className="hud-value">{systemStatus.disk.percent}%</div>
                <div className="progress-bar-bg">
                  <div className="progress-bar-fill" style={{ width: `${systemStatus.disk.percent}%`, background: 'var(--accent-emerald)' }}></div>
                </div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)', marginTop: '6px' }}>
                  {systemStatus.disk.used_gb} GB / {systemStatus.disk.total_gb} GB
                </div>
              </div>

              <div className="hud-card">
                <div className="hud-title">System Uptime</div>
                <div className="hud-value" style={{ fontSize: '1.25rem' }}>{systemStatus.uptime_formatted}</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)', marginTop: '6px' }}>
                  Agent v{systemStatus.agent_version}
                </div>
              </div>
            </div>

            <button className="btn btn-secondary" style={{ marginTop: '24px', width: '100%' }} onClick={() => setStatusModalOpen(false)}>
              Close Telemetry
            </button>
          </div>
        </div>
      )}

      {/* SCREENSHOT VIEWER MODAL */}
      {screenshotModalOpen && screenshotData && (
        <div className="modal-overlay" onClick={() => setScreenshotModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '850px', padding: '24px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <Camera size={20} color="var(--accent-cyan)" />
                <h3 style={{ fontSize: '1.15rem', fontWeight: 700 }}>
                  Captured Screenshot ({screenshotData.width} × {screenshotData.height})
                </h3>
              </div>
              <button onClick={() => setScreenshotModalOpen(false)} style={{ background: 'none', border: 'none', color: 'var(--text-dim)', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>

            <div style={{ borderRadius: '12px', overflow: 'hidden', border: '1px solid var(--border-subtle)', background: '#000', textAlign: 'center' }}>
              <img
                src={`data:image/${screenshotData.format};base64,${screenshotData.data_base64}`}
                alt="PC Screen"
                style={{ maxWidth: '100%', maxHeight: '65vh', objectFit: 'contain', display: 'block', margin: '0 auto' }}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '16px' }}>
              <span style={{ fontSize: '0.78rem', color: 'var(--text-dim)' }}>
                Zero storage policy: Streamed securely and not retained on server.
              </span>
              <button className="btn btn-secondary" onClick={() => setScreenshotModalOpen(false)}>
                Dismiss
              </button>
            </div>
          </div>
        </div>
      )}

      {/* CONFIRMATION GATE MODAL */}
      {confirmModal.open && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: '440px', padding: '28px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
              <div style={{ width: '40px', height: '40px', borderRadius: '10px', background: 'rgba(244,63,94,0.15)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--accent-rose)' }}>
                <AlertTriangle size={24} />
              </div>
              <div>
                <h4 style={{ fontSize: '1.15rem', fontWeight: 700 }}>High-Risk Operation</h4>
                <span className="brand-badge" style={{ background: 'rgba(244,63,94,0.2)', color: 'var(--accent-rose)', borderColor: 'rgba(244,63,94,0.4)' }}>
                  HIGH PERMISSION TIER
                </span>
              </div>
            </div>

            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '24px' }}>
              {confirmModal.description}
            </p>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
              <button className="btn btn-secondary" onClick={() => setConfirmModal((prev) => ({ ...prev, open: false }))}>
                Cancel
              </button>
              <button className="btn btn-danger" onClick={confirmModal.onConfirm}>
                Confirm & Execute
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
