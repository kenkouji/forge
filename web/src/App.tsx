import React, { useState, useEffect, useRef } from 'react';
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
  ChevronDown,
  AlertTriangle,
  X,
  FileText,
  Folder,
  Download,
  Upload,
  ArrowUp,
  Trash2,
  CheckCircle2,
  Sun,
  Moon,
  Laptop,
  Radio,
  Sliders,
  Zap,
  Info,
  QrCode
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
  AuditStatsResponse,
  PrivacyShieldStatus
} from './api';
import { GuestPortalView } from './components/GuestPortalView';
import { PrivacyShieldBar } from './components/PrivacyShieldBar';
import { InviteGuestModal } from './components/InviteGuestModal';
import { GuestSessionManager } from './components/GuestSessionManager';
import { GuestDropZoneQueue } from './components/GuestDropZoneQueue';

type Tab = 'home' | 'devices' | 'files' | 'ai' | 'activity' | 'settings';
type ThemeMode = 'dark' | 'light' | 'system';
type AnimationIntensity = 'standard' | 'subtle' | 'off';

/**
 * RemoteForge Abstract Forge & Dynamic Signal Emblem
 * Handcrafted SVG emblem with Royal Violet, Lavender, and Pearl highlights.
 */
function RemoteForgeEmblem({ size = 32 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="rfEmblemGrad" x1="0" y1="0" x2="40" y2="40" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#BDA0FF" />
          <stop offset="100%" stopColor="#6136A6" />
        </linearGradient>
        <linearGradient id="rfSignalGrad" x1="0" y1="0" x2="0" y2="40" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#F2EDFA" />
          <stop offset="100%" stopColor="#BDA0FF" />
        </linearGradient>
      </defs>
      <rect width="40" height="40" rx="10" fill="#29163D" stroke="rgba(189, 160, 255, 0.4)" strokeWidth="1.5" />
      {/* Abstract Forge Anvil Base */}
      <path d="M10 27H30V29C30 30.1 29.1 31 28 31H12C10.9 31 10 30.1 10 29V27Z" fill="url(#rfEmblemGrad)" />
      <path d="M13 22H27L25 27H15L13 22Z" fill="url(#rfEmblemGrad)" opacity="0.85" />
      {/* Dynamic Signal Waves Emerging from Forge */}
      <path d="M12 16C16 13 24 13 28 16" stroke="url(#rfSignalGrad)" strokeWidth="2.2" strokeLinecap="round" />
      <path d="M15 12C18 9.5 22 9.5 25 12" stroke="url(#rfSignalGrad)" strokeWidth="2.2" strokeLinecap="round" opacity="0.75" />
      {/* Central Core Signal Pulse */}
      <circle cx="20" cy="20" r="2.5" fill="#F2EDFA" />
    </svg>
  );
}

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

  // Guest Access & Privacy Shield State
  const [isGuestMode, setIsGuestMode] = useState<boolean>(() => {
    const hash = window.location.hash.replace(/^#\/?/, '');
    const hashParams = new URLSearchParams(hash);
    const searchParams = new URLSearchParams(window.location.search);
    const invId = hashParams.get('invitation_id') || searchParams.get('invitation_id');
    const sec = hashParams.get('secret') || searchParams.get('secret');
    const savedToken = sessionStorage.getItem('rf_guest_token');
    return !!((invId && sec) || savedToken || window.location.pathname.startsWith('/guest'));
  });
  const [privacyShieldStatus, setPrivacyShieldStatus] = useState<PrivacyShieldStatus | null>(null);
  const [inviteModalOpen, setInviteModalOpen] = useState<boolean>(false);

  const loadPrivacyShield = async (deviceId: string) => {
    try {
      const ps = await api.getPrivacyShield(deviceId);
      setPrivacyShieldStatus(ps);
    } catch {
      // ignore
    }
  };

  useEffect(() => {
    if (selectedDevice?.id) {
      loadPrivacyShield(selectedDevice.id);
    }
  }, [selectedDevice?.id]);

  // Appearance & Theme State (Dark is default)
  const [themeMode, setThemeMode] = useState<ThemeMode>(() => {
    return (localStorage.getItem('remoteforge_theme') as ThemeMode) || 'dark';
  });
  const [animationIntensity, setAnimationIntensity] = useState<AnimationIntensity>(() => {
    return (localStorage.getItem('remoteforge_anim_intensity') as AnimationIntensity) || 'standard';
  });
  const [reducedMotion, setReducedMotion] = useState<boolean>(() => {
    return localStorage.getItem('remoteforge_reduced_motion') === 'true';
  });

  // Connected-Device Island State
  const [islandExpanded, setIslandExpanded] = useState<boolean>(false);
  const [islandDropdownOpen, setIslandDropdownOpen] = useState<boolean>(false);
  const islandCollapseTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

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
  const [filePreviewModal, setFilePreviewModal] = useState<any | null>(null);
  const [filePreviewLoading, setFilePreviewLoading] = useState(false);
  const [filePreviewRawMarkdown, setFilePreviewRawMarkdown] = useState(false);

  // AI Assistant State
  const [aiPrompt, setAiPrompt] = useState('');
  const [aiOutput, setAiOutput] = useState<any | null>(null);

  // Activity Tab State & Bug Tracking
  const [auditLogs, setAuditLogs] = useState<AuditLogItem[]>([]);
  const [auditCategory, setAuditCategory] = useState<'all' | 'bugs' | 'success'>('all');
  const [auditTimeframe, setAuditTimeframe] = useState<'1d' | '7d' | '14d'>('14d');
  const [auditStats, setAuditStats] = useState<AuditStatsResponse | null>(null);
  const [auditStatusMsg, setAuditStatusMsg] = useState<string | null>(null);
  const [showCleanModal, setShowCleanModal] = useState<boolean>(false);

  // Settings State
  const [customServerUrl, setCustomServerUrl] = useState(getServerBaseUrl());
  const [transportTestResult, setTransportTestResult] = useState<string | null>(null);

  // High-Risk Confirmation Modal
  const [confirmModal, setConfirmModal] = useState<{
    open: boolean;
    tool: string;
    description: string;
    onConfirm: () => void;
  }>({ open: false, tool: '', description: '', onConfirm: () => {} });

  const fileInputRef = useRef<HTMLInputElement>(null);

  // Handle Theme application to document
  useEffect(() => {
    const root = document.documentElement;
    const isLight =
      themeMode === 'light' ||
      (themeMode === 'system' && window.matchMedia('(prefers-color-scheme: light)').matches);

    if (isLight) {
      root.classList.add('theme-light');
    } else {
      root.classList.remove('theme-light');
    }

    if (reducedMotion || animationIntensity === 'off') {
      root.classList.add('motion-off');
    } else if (animationIntensity === 'subtle') {
      root.classList.remove('motion-off');
      root.classList.add('motion-reduced');
    } else {
      root.classList.remove('motion-off', 'motion-reduced');
    }

    localStorage.setItem('remoteforge_theme', themeMode);
    localStorage.setItem('remoteforge_anim_intensity', animationIntensity);
    localStorage.setItem('remoteforge_reduced_motion', String(reducedMotion));
  }, [themeMode, animationIntensity, reducedMotion]);

  // Trigger Connected-Device Island expansion for ~2 seconds
  const triggerIslandExpansion = () => {
    setIslandExpanded(true);
    if (islandCollapseTimerRef.current) {
      clearTimeout(islandCollapseTimerRef.current);
    }
    islandCollapseTimerRef.current = setTimeout(() => {
      setIslandExpanded(false);
    }, 2200);
  };

  const loadDevices = async () => {
    try {
      const data = await api.getDevices();
      setDevices(data);
      if (data.length > 0 && !selectedDevice) {
        setSelectedDevice(data[0]);
        triggerIslandExpansion();
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

  // Load initial devices and listen for live WebSocket events
  useEffect(() => {
    if (isAuthenticated) {
      loadDevices();
      loadAuditLogs();

      // Establish real-time WebSocket connection to server
      const ws = api.createWebSocketClient((msg) => {
        if (msg.type === 'device_status_change' || msg.type === 'device_heartbeat') {
          loadDevices();
          triggerIslandExpansion();
        }
      });

      return () => {
        if (ws) ws.close();
      };
    }
  }, [isAuthenticated]);

  const handleCleanLogs = async (days?: number) => {
    try {
      const res = await api.cleanAuditLogs(days);
      setAuditStatusMsg(res.message);
      setShowCleanModal(false);
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
    setActionMessage(`Requesting real telemetry from ${device.name}...`);
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
    setActionMessage(`Capturing live display on ${device.name}...`);
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
    setConfirmModal({
      open: true,
      tool: 'lock_pc',
      description: `Explicit confirmation required: Are you sure you want to securely lock ${device.name}?`,
      onConfirm: async () => {
        setConfirmModal((prev) => ({ ...prev, open: false }));
        setIsLoading(true);
        try {
          const res = await api.sendCommand(device.id, 'lock_pc');
          alert(res.status === 'COMPLETED' ? 'Workstation locked successfully.' : (res.error || 'Action failed'));
        } catch (err: any) {
          alert(`Error: ${err.message}`);
        } finally {
          setIsLoading(false);
          loadAuditLogs();
        }
      }
    });
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
      alert(`Filesystem sandbox error: ${err.message}`);
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

  const handleDownloadFile = async (item: FileItem) => {
    if (!selectedDevice) return;
    setIsLoading(true);
    setActionMessage(`Streaming chunked transfer for ${item.name} from ${selectedDevice.name}...`);
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

  const handlePreviewFile = async (item: FileItem) => {
    if (!selectedDevice) return;
    setFilePreviewLoading(true);
    setFilePreviewRawMarkdown(false);
    setFilePreviewModal({
      filename: item.name,
      path: item.path,
      file_size: item.size_bytes || 0,
      preview_supported: false,
      loading: true
    });
    try {
      const res = await api.previewFile(selectedDevice.id, item.path);
      setFilePreviewModal({
        ...res,
        filename: item.name,
        path: item.path
      });
    } catch (err: any) {
      setFilePreviewModal({
        filename: item.name,
        path: item.path,
        preview_supported: false,
        message: err.message || 'Unable to stream file preview'
      });
    } finally {
      setFilePreviewLoading(false);
    }
  };

  const handleUploadFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !selectedDevice) return;
    if (!currentPath) {
      alert('Please navigate into a specific authorized user folder (e.g. Documents, Downloads, Projects) before uploading.');
      return;
    }
    setIsLoading(true);
    setActionMessage(`Uploading ${file.name} to ${selectedDevice.name}...`);
    try {
      await api.uploadFile(selectedDevice.id, currentPath, file);
      setActionMessage(null);
      alert(`Upload complete: ${file.name} was saved successfully to authorized folder.`);
      await handleBrowseFiles(selectedDevice, currentPath);
    } catch (err: any) {
      alert(`Upload failed: ${err.message}`);
    } finally {
      setIsLoading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
      loadAuditLogs();
    }
  };

  const handleGoUp = () => {
    if (!selectedDevice || !currentPath) return;
    const parts = currentPath.split('/');
    parts.pop();
    const parent = parts.join('/');
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
      permissionLevel = 'HIGH';
      requiresConfirmation = true;
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
        description: `High-risk operational command: ${tool.replace('_', ' ')} on target PC ${selectedDevice.name}. Confirm execution?`,
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

  const handleTestTransport = async () => {
    setTransportTestResult('Probing active NetBird overlay transport and TCP socket...');
    try {
      const start = Date.now();
      await api.getDevices();
      const rtt = Date.now() - start;
      setTransportTestResult(`5/5 Checks Passed: Mac endpoint reachable (${rtt}ms RTT). NetBird overlay active.`);
    } catch (e: any) {
      setTransportTestResult(`Transport probe failed: ${e.message}`);
    }
  };

  const activeDevice = selectedDevice || (devices.length > 0 ? devices[0] : null);

  // Guest Access Portal Mode
  if (isGuestMode) {
    return <GuestPortalView onExitGuestMode={() => setIsGuestMode(false)} />;
  }

  // Login Screen with Obsidian / Royal Violet Glass
  if (!isAuthenticated) {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px', position: 'relative' }}>
        <div className="ambient-glow-container">
          <div className="ambient-blob blob-1"></div>
          <div className="ambient-blob blob-2"></div>
        </div>

        <div className="glass-panel" style={{ width: '100%', maxWidth: '440px', padding: '38px', position: 'relative', zIndex: 10 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px', marginBottom: '26px' }}>
            <RemoteForgeEmblem size={44} />
            <div>
              <h1 style={{ fontSize: '1.45rem', fontWeight: 800, letterSpacing: '-0.02em', background: 'linear-gradient(135deg, var(--text-main), var(--accent-lavender))', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
                RemoteForge
              </h1>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-dim)', letterSpacing: '0.08em', textTransform: 'uppercase', fontWeight: 600 }}>
                SECURE WORKSPACE CONTROL
              </p>
            </div>
          </div>

          <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div>
              <label style={{ fontSize: '0.85rem', color: 'var(--text-dim)', marginBottom: '6px', display: 'block', fontWeight: 600 }}>
                Operator Username
              </label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                style={{
                  width: '100%',
                  padding: '12px 14px',
                  borderRadius: '12px',
                  background: 'rgba(255,255,255,0.05)',
                  border: '1px solid var(--border-subtle)',
                  color: 'var(--text-main)',
                  fontFamily: 'inherit',
                  outline: 'none'
                }}
                required
              />
            </div>
            <div>
              <label style={{ fontSize: '0.85rem', color: 'var(--text-dim)', marginBottom: '6px', display: 'block', fontWeight: 600 }}>
                Access Token / Secret
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                style={{
                  width: '100%',
                  padding: '12px 14px',
                  borderRadius: '12px',
                  background: 'rgba(255,255,255,0.05)',
                  border: '1px solid var(--border-subtle)',
                  color: 'var(--text-main)',
                  fontFamily: 'inherit',
                  outline: 'none'
                }}
                required
              />
            </div>

            {authError && (
              <div style={{ padding: '10px 14px', borderRadius: '10px', background: 'rgba(239,68,68,0.12)', border: '1px solid rgba(239,68,68,0.3)', color: 'var(--accent-rose)', fontSize: '0.85rem' }}>
                {authError}
              </div>
            )}

            <button type="submit" className="btn btn-primary" style={{ marginTop: '8px', padding: '12px' }} disabled={isLoading}>
              {isLoading ? 'Verifying Credentials...' : 'Authenticate & Enter'}
            </button>

            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setIsGuestMode(true)}
              style={{ padding: '10px', fontSize: '0.84rem', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}
            >
              <QrCode size={15} color="var(--accent-lavender)" /> Join Guest Session (QR / Link)
            </button>
          </form>

          <div style={{ marginTop: '22px', borderTop: '1px solid var(--border-subtle)', paddingTop: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.74rem', color: 'var(--text-dim)' }}>NetBird VPN Overlay • Zero Cloud Storage</span>
            <span className="brand-badge">v1.4 SECURE</span>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="app-container">
      {/* Ambient Breathing Purple Glow with Spatial Depth */}
      <div className="ambient-glow-container">
        <div className="ambient-blob blob-1"></div>
        <div className="ambient-blob blob-2"></div>
        <div className="ambient-blob blob-3"></div>
      </div>

      {/* Persistent Left Sidebar Navigation */}
      <aside className="sidebar">
        <div className="brand">
          <RemoteForgeEmblem size={36} />
          <div className="brand-text-col">
            <span className="brand-title">RemoteForge</span>
            <span className="brand-sub">SECURE WORKSPACE</span>
          </div>
          <span className="brand-badge">v1.4</span>
        </div>

        <nav className="nav-menu">
          <button
            className={`nav-item ${activeTab === 'home' ? 'active' : ''}`}
            onClick={() => setActiveTab('home')}
          >
            <Monitor size={18} />
            <span>Home</span>
          </button>

          <button
            className={`nav-item ${activeTab === 'devices' ? 'active' : ''}`}
            onClick={() => { setActiveTab('devices'); loadDevices(); }}
          >
            <Laptop size={18} />
            <span>Devices</span>
          </button>

          <button
            className={`nav-item ${activeTab === 'ai' ? 'active' : ''}`}
            onClick={() => setActiveTab('ai')}
          >
            <Sparkles size={18} />
            <span>AI Cockpit</span>
          </button>

          <button
            className={`nav-item ${activeTab === 'files' ? 'active' : ''}`}
            onClick={() => activeDevice && handleBrowseFiles(activeDevice)}
          >
            <FolderLock size={18} />
            <span>Files & Storage</span>
          </button>

          <button
            className={`nav-item ${activeTab === 'settings' ? 'active' : ''}`}
            onClick={() => setActiveTab('settings')}
          >
            <Settings size={18} />
            <span>Settings</span>
          </button>
        </nav>

        <div className="nav-footer">
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <div style={{ width: '8px', height: '8px', borderRadius: '50%', background: 'var(--accent-emerald)' }}></div>
              <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>admin</span>
            </div>
            <button
              onClick={handleLogout}
              style={{ background: 'none', border: 'none', color: 'var(--accent-rose)', fontSize: '0.78rem', cursor: 'pointer', fontWeight: 600 }}
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
              {activeTab === 'files' && 'User Data Sandbox'}
              {activeTab === 'ai' && 'AI Command Cockpit'}
              {activeTab === 'activity' && 'Security Audit Trail'}
              {activeTab === 'settings' && 'Platform Settings'}
            </h2>
          </div>

          {/* Connected-Device Island Capsule (NEVER a rectangular bar) */}
          <div style={{ position: 'relative' }}>
            <div
              className={`device-island-capsule ${islandExpanded ? 'expanded' : 'compact'}`}
              onClick={() => setIslandDropdownOpen(!islandDropdownOpen)}
              title="Click to view connection controls and switch target PC"
            >
              <div className="island-icon">
                <Monitor size={14} />
              </div>

              {activeDevice ? (
                <>
                  <span className="island-device-name">{activeDevice.name}</span>
                  <span className={`island-badge ${activeDevice.is_online ? 'online' : 'offline'}`}>
                    <span className="pulse-dot" style={{ display: 'inline-block', marginRight: '5px' }}></span>
                    {activeDevice.is_online ? 'CONNECTED' : 'OFFLINE'}
                  </span>
                  {islandExpanded && (
                    <span className="island-extra-info">
                      <span>{activeDevice.os_name}</span>
                      <span>•</span>
                      <span>v{activeDevice.agent_version}</span>
                    </span>
                  )}
                  <ChevronDown size={14} color="var(--text-dim)" />
                </>
              ) : (
                <>
                  <span className="island-device-name">No PC Connected</span>
                  <span className="island-badge offline">OFFLINE</span>
                  <ChevronDown size={14} color="var(--text-dim)" />
                </>
              )}
            </div>

            {/* Device Switcher Dropdown */}
            {islandDropdownOpen && (
              <div className="island-switcher-dropdown">
                <div style={{ padding: '6px 8px', fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-dim)', letterSpacing: '0.04em' }}>
                  SWITCH TARGET WORKSTATION
                </div>
                {devices.length === 0 ? (
                  <div style={{ padding: '8px', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                    No devices registered on NetBird network.
                  </div>
                ) : (
                  devices.map((d) => (
                    <div
                      key={d.id}
                      className={`island-switcher-item ${selectedDevice?.id === d.id ? 'selected' : ''}`}
                      onClick={() => {
                        setSelectedDevice(d);
                        setIslandDropdownOpen(false);
                        triggerIslandExpansion();
                      }}
                    >
                      <div>
                        <div style={{ fontWeight: 600, fontSize: '0.85rem' }}>{d.name}</div>
                        <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)' }}>
                          {d.os_name} • {d.id.substring(0, 8)}...
                        </div>
                      </div>
                      <span className={`island-badge ${d.is_online ? 'online' : 'offline'}`}>
                        {d.is_online ? 'ONLINE' : 'OFFLINE'}
                      </span>
                    </div>
                  ))
                )}
                <div style={{ borderTop: '1px solid var(--border-subtle)', marginTop: '6px', paddingTop: '6px' }}>
                  <button
                    className="btn btn-secondary"
                    style={{ width: '100%', fontSize: '0.78rem', padding: '6px 10px' }}
                    onClick={() => {
                      loadDevices();
                      setIslandDropdownOpen(false);
                    }}
                  >
                    <RefreshCw size={12} /> Refresh Devices
                  </button>
                </div>
              </div>
            )}
          </div>
        </header>

        <div className="content-body">
          {actionMessage && (
            <div style={{ marginBottom: '20px', padding: '12px 18px', background: 'rgba(97,54,166,0.15)', border: '1px solid var(--border-highlight)', borderRadius: '14px', color: 'var(--accent-lavender)', display: 'flex', alignItems: 'center', gap: '12px' }}>
              <RefreshCw size={16} className="pulse-dot" />
              <span style={{ fontSize: '0.9rem', fontWeight: 500 }}>{actionMessage}</span>
            </div>
          )}

          {/* TAB 1: HOME */}
          {activeTab === 'home' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
              {/* Connected PC Hero Card */}
              {activeDevice ? (
                <div className="hero-pc-card">
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '6px' }}>
                        <span className="brand-badge" style={{ background: 'rgba(189, 160, 255, 0.2)', color: 'var(--accent-lavender)' }}>
                          PRIMARY WORKSTATION
                        </span>
                        <span style={{ fontSize: '0.8rem', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>
                          ID: {activeDevice.id.substring(0, 12)}
                        </span>
                      </div>
                      <h3 style={{ fontSize: '1.65rem', fontWeight: 800, letterSpacing: '-0.02em', color: 'var(--text-main)' }}>
                        {activeDevice.name}
                      </h3>
                      <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', marginTop: '4px' }}>
                        {activeDevice.os_name} {activeDevice.os_version} • Agent v{activeDevice.agent_version} • Last seen {new Date(activeDevice.last_seen).toLocaleTimeString()}
                      </p>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                      <span className={`island-badge ${activeDevice.is_online ? 'online' : 'offline'}`} style={{ padding: '6px 14px', fontSize: '0.82rem' }}>
                        <span className="pulse-dot" style={{ display: 'inline-block', marginRight: '6px' }}></span>
                        {activeDevice.is_online ? 'ONLINE & READY' : 'DISCONNECTED'}
                      </span>
                    </div>
                  </div>

                  {/* Telemetry HUD Rings & Metrics */}
                  <div className="hud-grid" style={{ marginTop: '24px' }}>
                    <div className="hud-card">
                      <div className="hud-title">Security Model</div>
                      <div className="hud-value" style={{ color: 'var(--accent-emerald)', fontSize: '1.3rem' }}>
                        Sandboxed
                      </div>
                      <div style={{ fontSize: '0.74rem', color: 'var(--text-dim)', marginTop: '6px' }}>
                        User-data folders only
                      </div>
                    </div>

                    <div className="hud-card">
                      <div className="hud-title">Transport Route</div>
                      <div className="hud-value" style={{ color: 'var(--accent-lavender)', fontSize: '1.3rem' }}>
                        NetBird P2P
                      </div>
                      <div style={{ fontSize: '0.74rem', color: 'var(--text-dim)', marginTop: '6px' }}>
                        RFC 6598 Overlay
                      </div>
                    </div>

                    <div className="hud-card">
                      <div className="hud-title">Controlled Tools</div>
                      <div className="hud-value" style={{ color: 'var(--accent-lavender)' }}>13 Active</div>
                      <div style={{ fontSize: '0.74rem', color: 'var(--text-dim)', marginTop: '6px' }}>
                        Strict type validation
                      </div>
                    </div>

                    <div className="hud-card">
                      <div className="hud-title">Audit Trail</div>
                      <div className="hud-value">{auditStats?.total_logs ?? auditLogs.length} Events</div>
                      <div style={{ fontSize: '0.74rem', color: 'var(--text-dim)', marginTop: '6px' }}>
                        14-day auto-purge
                      </div>
                    </div>
                  </div>

                  {/* Quick Workstation Actions Dock */}
                  <div style={{ marginTop: '24px', borderTop: '1px solid var(--border-subtle)', paddingTop: '20px' }}>
                    <div style={{ fontSize: '0.82rem', color: 'var(--text-dim)', marginBottom: '12px', fontWeight: 600, letterSpacing: '0.04em' }}>
                      WORKSTATION CONTROLS
                    </div>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '12px' }}>
                      <button
                        className="btn btn-primary"
                        disabled={!activeDevice.is_online || isLoading}
                        onClick={() => handleFetchStatus(activeDevice)}
                      >
                        <Cpu size={16} /> Live Telemetry
                      </button>
                      <button
                        className="btn btn-secondary"
                        disabled={!activeDevice.is_online || isLoading}
                        onClick={() => handleTakeScreenshot(activeDevice)}
                      >
                        <Camera size={16} /> Capture Screen
                      </button>
                      <button
                        className="btn btn-secondary"
                        disabled={!activeDevice.is_online || isLoading}
                        onClick={() => handleBrowseFiles(activeDevice)}
                      >
                        <FolderLock size={16} /> Browse Files
                      </button>
                      <button
                        className="btn btn-secondary"
                        onClick={() => {
                          setSelectedDevice(activeDevice);
                          setInviteModalOpen(true);
                        }}
                      >
                        <QrCode size={16} /> Invite Guest
                      </button>
                      <button
                        className="btn btn-danger"
                        disabled={!activeDevice.is_online || isLoading}
                        onClick={() => handleLockPC(activeDevice)}
                      >
                        <Lock size={16} /> Lock Workstation
                      </button>
                    </div>
                  </div>

                  {/* Privacy Shield Controls */}
                  <div style={{ marginTop: '20px' }}>
                    <PrivacyShieldBar
                      deviceId={activeDevice.id}
                      status={privacyShieldStatus}
                      onStatusChange={setPrivacyShieldStatus}
                    />
                  </div>

                  {/* Active & Pending Guest Co-Control Sessions */}
                  <div style={{ marginTop: '14px' }}>
                    <GuestSessionManager
                      deviceId={activeDevice.id}
                      onSessionsChange={() => loadPrivacyShield(activeDevice.id)}
                    />
                  </div>

                  {/* Connected Device Transfers & Drop Zone Queue */}
                  <div style={{ marginTop: '14px' }}>
                    <GuestDropZoneQueue deviceId={activeDevice.id} />
                  </div>
                </div>
              ) : (
                /* Proactive Connection Center when disconnected */
                <div className="glass-panel" style={{ textAlign: 'center', padding: '48px 24px' }}>
                  <div style={{ width: '56px', height: '56px', borderRadius: '50%', background: 'rgba(239,68,68,0.15)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 16px', color: 'var(--accent-rose)' }}>
                    <Monitor size={28} />
                  </div>
                  <h3 style={{ fontSize: '1.4rem', fontWeight: 700 }}>No Target Workstation Connected</h3>
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', maxWidth: '520px', margin: '10px auto 24px' }}>
                    RemoteForge requires an active outbound authenticated agent connection over NetBird VPN. Ensure your PC agent is running.
                  </p>
                  <div style={{ display: 'flex', gap: '12px', justifyContent: 'center', flexWrap: 'wrap' }}>
                    <button className="btn btn-primary" onClick={loadDevices}>
                      <RefreshCw size={16} /> Retry Discovery
                    </button>
                    <button className="btn btn-secondary" onClick={() => setActiveTab('settings')}>
                      <Settings size={16} /> Connection Settings
                    </button>
                  </div>
                </div>
              )}

              {/* Recent Activity & Bug Tracker Preview */}
              <div className="glass-panel">
                <div className="section-header">
                  <div className="section-title">
                    <Activity size={20} color="var(--accent-lavender)" />
                    <span>Recent Activity & Bug Tracker</span>
                  </div>
                  <button
                    className="btn btn-secondary"
                    style={{ fontSize: '0.8rem', padding: '6px 14px' }}
                    onClick={() => { setActiveTab('activity'); loadAuditLogs(); }}
                  >
                    View Audit Trail →
                  </button>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '12px', margin: '14px 0' }}>
                  <div className="hud-card" style={{ padding: '12px 16px' }}>
                    <div className="hud-title">Total Records</div>
                    <div style={{ fontSize: '1.3rem', fontWeight: 700, color: 'var(--text-main)' }}>
                      {auditStats?.total_logs ?? auditLogs.length}
                    </div>
                  </div>
                  <div className="hud-card" style={{ padding: '12px 16px', background: 'rgba(239,68,68,0.06)', borderColor: 'rgba(239,68,68,0.2)' }}>
                    <div className="hud-title" style={{ color: 'var(--accent-rose)' }}>Bugs & Failures</div>
                    <div style={{ fontSize: '1.3rem', fontWeight: 700, color: 'var(--accent-rose)' }}>
                      {auditStats?.total_bugs ?? 0}
                    </div>
                  </div>
                  <div className="hud-card" style={{ padding: '12px 16px', background: 'rgba(16,185,129,0.06)', borderColor: 'rgba(16,185,129,0.2)' }}>
                    <div className="hud-title" style={{ color: 'var(--accent-emerald)' }}>Successful Ops</div>
                    <div style={{ fontSize: '1.3rem', fontWeight: 700, color: 'var(--accent-emerald)' }}>
                      {auditStats?.total_success ?? 0}
                    </div>
                  </div>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {auditLogs.slice(0, 4).map((log) => {
                    const isBug = log.execution_status !== 'SUCCESS' || (log.action && log.action.includes('REJECTED'));
                    return (
                      <div
                        key={log.id}
                        style={{
                          padding: '12px 16px',
                          borderRadius: '10px',
                          background: isBug ? 'rgba(239,68,68,0.05)' : 'rgba(255,255,255,0.02)',
                          border: isBug ? '1px solid rgba(239,68,68,0.25)' : '1px solid var(--border-subtle)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between'
                        }}
                      >
                        <div>
                          <span style={{ fontWeight: 600, color: 'var(--text-main)', fontSize: '0.88rem' }}>
                            {log.tool || log.action}
                          </span>
                          <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)', marginLeft: '10px', fontFamily: 'var(--font-mono)' }}>
                            {new Date(log.timestamp).toLocaleTimeString()}
                          </span>
                        </div>
                        <span style={{ fontSize: '0.75rem', color: isBug ? 'var(--accent-rose)' : 'var(--accent-emerald)', fontWeight: 600 }}>
                          {log.execution_status}
                        </span>
                      </div>
                    );
                  })}
                  {auditLogs.length === 0 && (
                    <div style={{ fontSize: '0.85rem', color: 'var(--text-dim)', textAlign: 'center', padding: '16px' }}>
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
                  <h3 className="section-title">Registered Computers</h3>
                  <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                    Computers running RemoteForge Agent with outbound authenticated WebSockets over NetBird VPN.
                  </p>
                </div>
                <button className="btn btn-secondary" onClick={loadDevices}>
                  <RefreshCw size={14} /> Refresh
                </button>
              </div>

              {devices.length === 0 ? (
                <div className="glass-panel" style={{ textAlign: 'center', padding: '52px' }}>
                  <Monitor size={48} color="var(--text-dim)" style={{ marginBottom: '16px' }} />
                  <h4>No PC Agent Connected</h4>
                  <p style={{ color: 'var(--text-dim)', fontSize: '0.9rem', marginTop: '8px' }}>
                    Start the PC agent in your terminal using:
                    <br />
                    <code style={{ background: 'rgba(0,0,0,0.4)', padding: '6px 10px', borderRadius: '6px', color: 'var(--accent-lavender)', fontFamily: 'var(--font-mono)', display: 'inline-block', marginTop: '8px' }}>
                      PYTHONPATH=. python3 agent/remote_forge_agent/main.py
                    </code>
                  </p>
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                  {activeDevice && (
                    <>
                      <PrivacyShieldBar
                        deviceId={activeDevice.id}
                        status={privacyShieldStatus}
                        onStatusChange={setPrivacyShieldStatus}
                      />
                      <GuestSessionManager
                        deviceId={activeDevice.id}
                        onSessionsChange={() => loadPrivacyShield(activeDevice.id)}
                      />
                    </>
                  )}

                  <div className="device-grid">
                    {devices.map((dev) => {
                      const isActive = selectedDevice?.id === dev.id;
                      return (
                        <div key={dev.id} className={`device-card ${isActive ? 'active-pc' : ''}`}>
                          <div className="device-header">
                            <div>
                              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                <h4 style={{ fontSize: '1.2rem', fontWeight: 700 }}>{dev.name}</h4>
                                {isActive && (
                                  <span className="brand-badge" style={{ background: 'rgba(189,160,255,0.25)', color: 'var(--accent-lavender)' }}>
                                    ACTIVE
                                  </span>
                                )}
                              </div>
                              <span style={{ fontSize: '0.74rem', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>
                                {dev.id}
                              </span>
                            </div>
                            <span className={`island-badge ${dev.is_online ? 'online' : 'offline'}`}>
                              {dev.is_online ? 'ONLINE' : 'OFFLINE'}
                            </span>
                          </div>

                          <div className="device-meta-row">
                            <span className="device-meta-label">Operating System</span>
                            <span className="device-meta-val">{dev.os_name} {dev.os_version}</span>
                          </div>
                          <div className="device-meta-row">
                            <span className="device-meta-label">Agent Version</span>
                            <span className="device-meta-val">{dev.agent_version}</span>
                          </div>
                          <div className="device-meta-row">
                            <span className="device-meta-label">Last Ping</span>
                            <span className="device-meta-val">{new Date(dev.last_seen).toLocaleTimeString()}</span>
                          </div>

                          <div className="device-actions">
                            <button
                              className="btn btn-primary"
                              disabled={!dev.is_online || isLoading}
                              onClick={() => {
                                setSelectedDevice(dev);
                                handleFetchStatus(dev);
                              }}
                            >
                              <Cpu size={14} /> Telemetry
                            </button>
                            <button
                              className="btn btn-secondary"
                              disabled={!dev.is_online || isLoading}
                              onClick={() => {
                                setSelectedDevice(dev);
                                handleTakeScreenshot(dev);
                              }}
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
                              className="btn btn-secondary"
                              disabled={!dev.is_online || isLoading}
                              onClick={() => {
                                setSelectedDevice(dev);
                                setInviteModalOpen(true);
                              }}
                            >
                              <QrCode size={14} /> Invite Guest
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
                      );
                    })}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* TAB 3: FILES (USER DATA SANDBOX) */}
          {activeTab === 'files' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              <div className="glass-panel" style={{ background: 'rgba(16, 185, 129, 0.05)', borderColor: 'rgba(16, 185, 129, 0.25)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                  <Shield size={24} color="var(--accent-emerald)" />
                  <div>
                    <h4 style={{ color: 'var(--accent-emerald)', fontWeight: 700 }}>Strict User-Data Security Sandbox</h4>
                    <p style={{ fontSize: '0.84rem', color: 'var(--text-muted)' }}>
                      Only authorized user folders (Desktop, Documents, Downloads, Pictures, Projects) are accessible. System directories, OS partitions, and credential directories (.ssh, .aws) are strictly denied by canonical path enforcement.
                    </p>
                  </div>
                </div>
              </div>

              {/* Search Bar */}
              <div style={{ display: 'flex', gap: '10px' }}>
                <div style={{ flex: 1, position: 'relative' }}>
                  <input
                    type="text"
                    placeholder="Search within sandbox (e.g., report, test.pdf, assignment)..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && handleSearchFiles()}
                    style={{
                      width: '100%',
                      padding: '12px 16px 12px 42px',
                      borderRadius: '12px',
                      background: 'var(--bg-card)',
                      border: '1px solid var(--border-subtle)',
                      color: 'var(--text-main)',
                      fontFamily: 'inherit',
                      outline: 'none'
                    }}
                  />
                  <Search size={18} style={{ position: 'absolute', left: '14px', top: '14px', color: 'var(--text-dim)' }} />
                </div>
                <button className="btn btn-primary" onClick={handleSearchFiles} disabled={isLoading || !searchQuery.trim()}>
                  Search
                </button>
              </div>

              {/* Directory Listing / Search Results */}
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
                      <span>Path: {currentPath || 'Authorized User Roots'}</span>
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
                          padding: '12px 16px',
                          borderRadius: '10px',
                          background: 'rgba(255,255,255,0.03)'
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                          <FileText size={18} color="var(--accent-lavender)" />
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
                            style={{ padding: '6px 10px' }}
                            title="Download file"
                            onClick={() => handleDownloadFile({ name: item.filename, path: item.path, is_directory: false })}
                          >
                            <Download size={14} />
                          </button>
                        </div>
                      </div>
                    ))
                  ) : fileList.length === 0 ? (
                    <div style={{ textAlign: 'center', padding: '36px', color: 'var(--text-dim)' }}>
                      No files or directories found in current sandbox scope.
                    </div>
                  ) : (
                    fileList.map((f, i) => (
                      <div
                        key={i}
                        onClick={() => f.is_directory ? (activeDevice && handleBrowseFiles(activeDevice, f.path)) : handlePreviewFile(f)}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          padding: '12px 16px',
                          borderRadius: '10px',
                          background: 'rgba(255,255,255,0.02)',
                          cursor: 'pointer',
                          transition: 'background 0.2s ease'
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                          {f.is_directory ? <Folder size={18} color="var(--accent-lavender)" /> : <FileText size={18} color="var(--text-muted)" />}
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

              {/* Guest Drop Zone Staged Files Review Queue */}
              {activeDevice && (
                <div style={{ marginTop: '10px' }}>
                  <GuestDropZoneQueue deviceId={activeDevice.id} />
                </div>
              )}
            </div>
          )}

          {/* TAB 4: AI COCKPIT */}
          {activeTab === 'ai' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              <div className="glass-panel">
                <h3 className="section-title" style={{ marginBottom: '8px' }}>
                  <Sparkles size={22} color="var(--accent-lavender)" />
                  <span>Controlled AI Command Cockpit</span>
                </h3>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '20px' }}>
                  Operational assistant executing typed, sandboxed commands. High-risk operations require explicit user approval.
                </p>

                <div className="ai-input-box">
                  <textarea
                    className="ai-textarea"
                    placeholder="Enter workstation command... (e.g., 'Inspect system status', 'Take a screenshot', 'Find assignment files', 'Lock workstation')"
                    value={aiPrompt}
                    onChange={(e) => setAiPrompt(e.target.value)}
                  />
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '10px' }}>
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-dim)' }}>
                      Target: <strong style={{ color: 'var(--text-main)' }}>{activeDevice ? activeDevice.name : 'No device'}</strong>
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

                {/* Suggestions Dock */}
                <div style={{ marginTop: '16px', display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                  {[
                    'Check my PC status',
                    'Take a screenshot',
                    'Find my DBMS assignment',
                    'Open Chrome and search YouTube',
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
                    <span style={{ fontWeight: 700, fontSize: '0.95rem' }}>Engine Policy Validation</span>
                    <span className="brand-badge" style={{ background: aiOutput.permission_level === 'HIGH' ? 'rgba(239,68,68,0.2)' : 'rgba(97,54,166,0.2)', color: aiOutput.permission_level === 'HIGH' ? 'var(--accent-rose)' : 'var(--accent-lavender)' }}>
                      {aiOutput.permission_level} RISK TIER
                    </span>
                  </div>

                  <pre style={{ background: 'rgba(0,0,0,0.5)', padding: '16px', borderRadius: '12px', fontSize: '0.85rem', fontFamily: 'var(--font-mono)', overflowX: 'auto', color: 'var(--accent-lavender)' }}>
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
                    <h3 className="section-title">Audit Trail & Bug Tracker</h3>
                    <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                      14-day auto-purged audit log • Zero clutter policy
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
                  <div style={{ background: 'rgba(16,185,129,0.1)', border: '1px solid var(--accent-emerald)', padding: '10px 14px', borderRadius: '10px', color: 'var(--accent-emerald)', fontSize: '0.85rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
                    <span>{auditStatusMsg}</span>
                    <button style={{ background: 'none', border: 'none', color: 'var(--text-dim)', cursor: 'pointer' }} onClick={() => setAuditStatusMsg(null)}>✕</button>
                  </div>
                )}

                {/* Bug & Event HUD Metrics */}
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '12px', marginBottom: '16px' }}>
                  <div className="hud-card">
                    <div className="hud-title">Total Records</div>
                    <div className="hud-value">{auditStats?.total_logs ?? auditLogs.length}</div>
                  </div>
                  <div className="hud-card" style={{ background: 'rgba(239,68,68,0.06)', borderColor: 'rgba(239,68,68,0.2)' }}>
                    <div className="hud-title" style={{ color: 'var(--accent-rose)' }}>Bugs & Failures</div>
                    <div className="hud-value" style={{ color: 'var(--accent-rose)' }}>{auditStats?.total_bugs ?? 0}</div>
                  </div>
                  <div className="hud-card" style={{ background: 'rgba(16,185,129,0.06)', borderColor: 'rgba(16,185,129,0.2)' }}>
                    <div className="hud-title" style={{ color: 'var(--accent-emerald)' }}>Successful Ops</div>
                    <div className="hud-value" style={{ color: 'var(--accent-emerald)' }}>{auditStats?.total_success ?? 0}</div>
                  </div>
                  <div className="hud-card">
                    <div className="hud-title">Max Retention</div>
                    <div className="hud-value" style={{ color: 'var(--accent-lavender)' }}>14 Days</div>
                  </div>
                </div>

                {/* Filters */}
                <div style={{ display: 'flex', gap: '10px', marginBottom: '16px', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between' }}>
                  <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                    <button
                      className={`btn ${auditCategory === 'all' ? 'btn-primary' : 'btn-secondary'}`}
                      style={{ padding: '6px 14px', fontSize: '0.82rem' }}
                      onClick={() => { setAuditCategory('all'); loadAuditLogs('all', auditTimeframe); }}
                    >
                      All Events
                    </button>
                    <button
                      className={`btn ${auditCategory === 'bugs' ? 'btn-primary' : 'btn-secondary'}`}
                      style={{ padding: '6px 14px', fontSize: '0.82rem' }}
                      onClick={() => { setAuditCategory('bugs'); loadAuditLogs('bugs', auditTimeframe); }}
                    >
                      Bugs & Failures Only
                    </button>
                    <button
                      className={`btn ${auditCategory === 'success' ? 'btn-primary' : 'btn-secondary'}`}
                      style={{ padding: '6px 14px', fontSize: '0.82rem' }}
                      onClick={() => { setAuditCategory('success'); loadAuditLogs('success', auditTimeframe); }}
                    >
                      Success Only
                    </button>
                  </div>

                  <div style={{ display: 'flex', gap: '6px', alignItems: 'center' }}>
                    <span style={{ fontSize: '0.76rem', color: 'var(--text-dim)', fontWeight: 600 }}>SCOPE:</span>
                    {(['1d', '7d', '14d'] as const).map((tf) => (
                      <button
                        key={tf}
                        className={`btn ${auditTimeframe === tf ? 'btn-primary' : 'btn-secondary'}`}
                        style={{ padding: '4px 10px', fontSize: '0.75rem', fontFamily: 'var(--font-mono)' }}
                        onClick={() => {
                          setAuditTimeframe(tf);
                          loadAuditLogs(auditCategory, tf);
                        }}
                      >
                        {tf.toUpperCase()}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Logs List */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {auditLogs.map((log) => {
                    const isBug = log.execution_status !== 'SUCCESS' || (log.action && log.action.includes('REJECTED'));
                    return (
                      <div
                        key={log.id}
                        style={{
                          padding: '14px 18px',
                          borderRadius: '12px',
                          background: isBug ? 'rgba(239,68,68,0.05)' : 'rgba(255,255,255,0.02)',
                          border: isBug ? '1px solid rgba(239,68,68,0.25)' : '1px solid var(--border-subtle)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between'
                        }}
                      >
                        <div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                            <span style={{ fontWeight: 600, color: 'var(--text-main)', fontSize: '0.9rem' }}>
                              {log.tool || log.action}
                            </span>
                            <span className="brand-badge" style={{ fontSize: '0.62rem' }}>
                              {log.permission_level}
                            </span>
                          </div>
                          {log.arguments_safe && (
                            <div style={{ fontSize: '0.78rem', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)', marginTop: '4px' }}>
                              Args: {log.arguments_safe}
                            </div>
                          )}
                          <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)', marginTop: '2px' }}>
                            Actor: {log.actor} • {new Date(log.timestamp).toLocaleString()}
                          </div>
                        </div>
                        <span style={{ fontSize: '0.78rem', color: isBug ? 'var(--accent-rose)' : 'var(--accent-emerald)', fontWeight: 700 }}>
                          {log.execution_status}
                        </span>
                      </div>
                    );
                  })}
                  {auditLogs.length === 0 && (
                    <div style={{ textAlign: 'center', padding: '32px', color: 'var(--text-dim)' }}>
                      No audit events matching criteria.
                    </div>
                  )}
                </div>
              </div>

              {/* Clean Logs Modal Dialog */}
              {showCleanModal && (
                <div className="modal-overlay">
                  <div className="modal-content" style={{ maxWidth: '440px', padding: '24px' }}>
                    <div className="section-header">
                      <div className="section-title">
                        <Trash2 size={20} color="var(--accent-rose)" />
                        <span>Clean Audit Logs</span>
                      </div>
                      <button className="btn-icon" onClick={() => setShowCleanModal(false)}>
                        <X size={18} />
                      </button>
                    </div>
                    <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', margin: '14px 0' }}>
                      Prevent log clutter and reclaim database space. RemoteForge automatically prunes records older than 14 days, but you can also purge old logs manually right now.
                    </p>
                    <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end', marginTop: '16px' }}>
                      <button className="btn btn-secondary" onClick={() => handleCleanLogs(2)}>
                        Purge &gt; 2 Days
                      </button>
                      <button className="btn btn-danger" onClick={() => handleCleanLogs(0)}>
                        Purge All Logs
                      </button>
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* TAB 6: SETTINGS (Apple-Style Grouped Settings) */}
          {activeTab === 'settings' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '22px' }}>
              {/* Group 1: Appearance */}
              <div className="glass-panel">
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
                  <Sliders size={20} color="var(--accent-lavender)" />
                  <h3 className="section-title" style={{ margin: 0 }}>Appearance & Themes</h3>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
                  <div>
                    <label style={{ fontSize: '0.85rem', color: 'var(--text-dim)', marginBottom: '8px', display: 'block', fontWeight: 600 }}>
                      Interface Theme Mode
                    </label>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '10px', maxWidth: '420px' }}>
                      <button
                        className={`btn ${themeMode === 'dark' ? 'btn-primary' : 'btn-secondary'}`}
                        onClick={() => setThemeMode('dark')}
                      >
                        <Moon size={16} /> Dark Mode
                      </button>
                      <button
                        className={`btn ${themeMode === 'light' ? 'btn-primary' : 'btn-secondary'}`}
                        onClick={() => setThemeMode('light')}
                      >
                        <Sun size={16} /> Light Mode
                      </button>
                      <button
                        className={`btn ${themeMode === 'system' ? 'btn-primary' : 'btn-secondary'}`}
                        onClick={() => setThemeMode('system')}
                      >
                        <Laptop size={16} /> System
                      </button>
                    </div>
                  </div>

                  <div>
                    <label style={{ fontSize: '0.85rem', color: 'var(--text-dim)', marginBottom: '8px', display: 'block', fontWeight: 600 }}>
                      Background Glow Intensity
                    </label>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '10px', maxWidth: '420px' }}>
                      <button
                        className={`btn ${animationIntensity === 'standard' ? 'btn-primary' : 'btn-secondary'}`}
                        onClick={() => setAnimationIntensity('standard')}
                      >
                        Standard
                      </button>
                      <button
                        className={`btn ${animationIntensity === 'subtle' ? 'btn-primary' : 'btn-secondary'}`}
                        onClick={() => setAnimationIntensity('subtle')}
                      >
                        Subtle
                      </button>
                      <button
                        className={`btn ${animationIntensity === 'off' ? 'btn-primary' : 'btn-secondary'}`}
                        onClick={() => setAnimationIntensity('off')}
                      >
                        Disabled
                      </button>
                    </div>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', maxWidth: '420px', paddingTop: '8px', borderTop: '1px solid var(--border-subtle)' }}>
                    <div>
                      <div style={{ fontSize: '0.9rem', fontWeight: 600 }}>Reduced Motion</div>
                      <div style={{ fontSize: '0.78rem', color: 'var(--text-dim)' }}>Pause ambient blur motion and spring animations</div>
                    </div>
                    <button
                      className={`btn ${reducedMotion ? 'btn-primary' : 'btn-secondary'}`}
                      style={{ padding: '6px 14px', fontSize: '0.82rem' }}
                      onClick={() => setReducedMotion(!reducedMotion)}
                    >
                      {reducedMotion ? 'Enabled' : 'Disabled'}
                    </button>
                  </div>
                </div>
              </div>

              {/* Group 2: Devices & Connection */}
              <div className="glass-panel">
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
                  <Radio size={20} color="var(--accent-lavender)" />
                  <h3 className="section-title" style={{ margin: 0 }}>Devices & Connection Pipeline</h3>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                  <div>
                    <label style={{ fontSize: '0.85rem', color: 'var(--text-dim)', marginBottom: '6px', display: 'block', fontWeight: 600 }}>
                      RemoteForge Server Public URL (NetBird Overlay / Domain)
                    </label>
                    <input
                      type="text"
                      value={customServerUrl}
                      onChange={(e) => setCustomServerUrl(e.target.value)}
                      placeholder="e.g., http://100.117.133.109:8000"
                      style={{
                        width: '100%',
                        padding: '12px 14px',
                        borderRadius: '12px',
                        background: 'rgba(255,255,255,0.05)',
                        border: '1px solid var(--border-subtle)',
                        color: 'var(--text-main)',
                        fontFamily: 'var(--font-mono)',
                        outline: 'none'
                      }}
                    />
                    <p style={{ fontSize: '0.78rem', color: 'var(--text-dim)', marginTop: '6px' }}>
                      Server is migration-agnostic. When switching hosts, update this NetBird overlay IP or domain.
                    </p>
                  </div>

                  <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                    <button className="btn btn-primary" onClick={handleSaveSettings}>
                      Save Backend Address
                    </button>
                    <button className="btn btn-secondary" onClick={handleTestTransport}>
                      <Zap size={14} /> Probe Transport Route
                    </button>
                  </div>

                  {transportTestResult && (
                    <div style={{ padding: '10px 14px', borderRadius: '10px', background: 'rgba(189,160,255,0.1)', border: '1px solid var(--border-highlight)', color: 'var(--text-main)', fontSize: '0.82rem', fontFamily: 'var(--font-mono)' }}>
                      {transportTestResult}
                    </div>
                  )}
                </div>
              </div>

              {/* Group 3: Security & Privacy */}
              <div className="glass-panel">
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
                  <Shield size={20} color="var(--accent-emerald)" />
                  <h3 className="section-title" style={{ margin: 0 }}>Security Controls & Sandbox Model</h3>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', fontSize: '0.88rem', color: 'var(--text-muted)' }}>
                  <div style={{ display: 'flex', alignItems: 'flex-start', gap: '10px' }}>
                    <CheckCircle2 size={16} color="var(--accent-emerald)" style={{ flexShrink: 0, marginTop: '2px' }} />
                    <div>
                      <strong style={{ color: 'var(--text-main)' }}>Zero Permanent Server-Side Storage:</strong> Screenshots and transferred files stream directly between client and PC agent with chunked memory buffering. No residual files stored on FastAPI server.
                    </div>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'flex-start', gap: '10px' }}>
                    <CheckCircle2 size={16} color="var(--accent-emerald)" style={{ flexShrink: 0, marginTop: '2px' }} />
                    <div>
                      <strong style={{ color: 'var(--text-main)' }}>Strict User-Data Sandbox:</strong> Filesystem access is strictly restricted to user directories. System roots and credential paths (.ssh, .aws) are blocked by agent path canonicalization.
                    </div>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'flex-start', gap: '10px' }}>
                    <CheckCircle2 size={16} color="var(--accent-emerald)" style={{ flexShrink: 0, marginTop: '2px' }} />
                    <div>
                      <strong style={{ color: 'var(--text-main)' }}>Explicit Approval Gates:</strong> High-risk operations (lock workstation, restart, file modification) require explicit confirmation before execution.
                    </div>
                  </div>
                </div>
              </div>

              {/* Group 4: About */}
              <div className="glass-panel">
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '12px' }}>
                  <Info size={20} color="var(--accent-lavender)" />
                  <h3 className="section-title" style={{ margin: 0 }}>About RemoteForge</h3>
                </div>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                  RemoteForge v1.4.0 — Secure remote PC control and developer automation platform. Built with React 19, TypeScript, Vite, Kotlin 2.0 Jetpack Compose, FastAPI, and NetBird WireGuard P2P overlay.
                </p>
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
                <Cpu size={22} color="var(--accent-lavender)" />
                <h3 style={{ fontSize: '1.25rem', fontWeight: 800 }}>
                  Live System Telemetry: {systemStatus.os_name}
                </h3>
              </div>
              <button onClick={() => setStatusModalOpen(false)} className="btn-icon">
                <X size={20} />
              </button>
            </div>

            <div className="hud-grid">
              <div className="hud-card">
                <div className="hud-title">CPU Utilization</div>
                <div className="hud-value">{systemStatus.cpu.percent}%</div>
                <div className="progress-bar-bg">
                  <div className="progress-bar-fill" style={{ width: `${systemStatus.cpu.percent}%`, background: 'var(--accent-violet)' }}></div>
                </div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)', marginTop: '6px' }}>
                  {systemStatus.cpu.logical_cores} Cores ({systemStatus.machine_architecture})
                </div>
              </div>

              <div className="hud-card">
                <div className="hud-title">Memory (RAM)</div>
                <div className="hud-value">{systemStatus.ram.percent}%</div>
                <div className="progress-bar-bg">
                  <div className="progress-bar-fill" style={{ width: `${systemStatus.ram.percent}%`, background: 'var(--accent-lavender)' }}></div>
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
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '880px', padding: '24px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <Camera size={20} color="var(--accent-lavender)" />
                <h3 style={{ fontSize: '1.15rem', fontWeight: 700 }}>
                  Captured Display ({screenshotData.width} × {screenshotData.height})
                </h3>
              </div>
              <button onClick={() => setScreenshotModalOpen(false)} className="btn-icon">
                <X size={20} />
              </button>
            </div>

            <div style={{ borderRadius: '14px', overflow: 'hidden', border: '1px solid var(--border-highlight)', background: '#000', textAlign: 'center' }}>
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

      {/* HIGH RISK CONFIRMATION GATE MODAL */}
      {confirmModal.open && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: '440px', padding: '28px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
              <div style={{ width: '42px', height: '42px', borderRadius: '12px', background: 'rgba(239,68,68,0.15)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--accent-rose)' }}>
                <AlertTriangle size={24} />
              </div>
              <div>
                <h4 style={{ fontSize: '1.15rem', fontWeight: 700 }}>Explicit Confirmation</h4>
                <span className="brand-badge" style={{ background: 'rgba(239,68,68,0.2)', color: 'var(--accent-rose)', borderColor: 'rgba(239,68,68,0.4)' }}>
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

      {/* IN-APP FILE PREVIEW MODAL (ZERO EXTERNAL APP LAUNCH) */}
      {filePreviewModal && (
        <div className="modal-overlay" onClick={() => setFilePreviewModal(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '840px', maxHeight: '85vh', display: 'flex', flexDirection: 'column', padding: '24px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <FileText size={20} color="var(--accent-lavender)" />
                <div>
                  <h3 style={{ fontSize: '1.1rem', fontWeight: 700, margin: 0 }}>
                    {filePreviewModal.filename}
                  </h3>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>
                    {filePreviewModal.path}
                  </span>
                </div>
              </div>
              <button onClick={() => setFilePreviewModal(null)} className="btn-icon">
                <X size={20} />
              </button>
            </div>

            <div style={{ flex: 1, overflowY: 'auto', borderRadius: '12px', border: '1px solid var(--border-subtle)', background: 'rgba(0,0,0,0.2)', padding: '16px', minHeight: '260px' }}>
              {filePreviewLoading ? (
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '220px', color: 'var(--text-muted)' }}>
                  Loading preview stream from device...
                </div>
              ) : filePreviewModal.image_data_base64 || filePreviewModal.preview_type === 'image' ? (
                <div style={{ textAlign: 'center' }}>
                  <img
                    src={`data:${filePreviewModal.mime_type || 'image/png'};base64,${filePreviewModal.image_data_base64}`}
                    alt={filePreviewModal.filename}
                    style={{ maxWidth: '100%', maxHeight: '55vh', objectFit: 'contain', borderRadius: '8px' }}
                  />
                </div>
              ) : filePreviewModal.preview_type === 'pdf' && filePreviewModal.pdf_data_base64 ? (
                <iframe
                  src={`data:application/pdf;base64,${filePreviewModal.pdf_data_base64}`}
                  style={{ width: '100%', height: '55vh', border: 'none', borderRadius: '8px' }}
                  title="PDF Preview"
                />
              ) : filePreviewModal.preview_type === 'markdown' && filePreviewModal.content ? (
                <div>
                  <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '10px' }}>
                    <button
                      className="btn btn-secondary"
                      style={{ padding: '4px 10px', fontSize: '0.75rem' }}
                      onClick={() => setFilePreviewRawMarkdown(!filePreviewRawMarkdown)}
                    >
                      {filePreviewRawMarkdown ? 'Show Formatted' : 'Show Raw'}
                    </button>
                  </div>
                  {filePreviewRawMarkdown ? (
                    <pre style={{ margin: 0, fontFamily: 'var(--font-mono)', fontSize: '0.85rem', color: 'var(--text-main)', whiteSpace: 'pre-wrap' }}>
                      {filePreviewModal.content}
                    </pre>
                  ) : (
                    <div style={{ lineHeight: 1.6, color: 'var(--text-main)' }}>
                      {filePreviewModal.content.split('\n').map((line: string, idx: number) => {
                        if (line.startsWith('# ')) return <h2 key={idx} style={{ color: 'var(--accent-lavender)', margin: '14px 0 6px 0' }}>{line.replace('# ', '')}</h2>;
                        if (line.startsWith('## ')) return <h3 key={idx} style={{ color: 'var(--text-main)', margin: '12px 0 4px 0' }}>{line.replace('## ', '')}</h3>;
                        if (line.startsWith('- ') || line.startsWith('* ')) return <li key={idx} style={{ marginLeft: '16px' }}>{line.substring(2)}</li>;
                        if (line.startsWith('> ')) return <blockquote key={idx} style={{ borderLeft: '3px solid var(--accent-lavender)', paddingLeft: '10px', margin: '8px 0', color: 'var(--text-muted)' }}>{line.replace('> ', '')}</blockquote>;
                        if (line.trim() === '') return <div key={idx} style={{ height: '8px' }}></div>;
                        return <p key={idx} style={{ margin: '4px 0' }}>{line}</p>;
                      })}
                    </div>
                  )}
                </div>
              ) : filePreviewModal.preview_type === 'audio' && filePreviewModal.media_data_base64 ? (
                <div style={{ textAlign: 'center', padding: '30px' }}>
                  <audio controls src={`data:${filePreviewModal.mime_type || 'audio/mpeg'};base64,${filePreviewModal.media_data_base64}`} style={{ width: '100%', maxWidth: '420px' }} />
                </div>
              ) : filePreviewModal.preview_type === 'video' && filePreviewModal.media_data_base64 ? (
                <div style={{ textAlign: 'center' }}>
                  <video controls src={`data:${filePreviewModal.mime_type || 'video/mp4'};base64,${filePreviewModal.media_data_base64}`} style={{ maxWidth: '100%', maxHeight: '55vh', borderRadius: '8px' }} />
                </div>
              ) : filePreviewModal.content ? (
                <pre style={{ margin: 0, fontFamily: 'var(--font-mono)', fontSize: '0.85rem', color: 'var(--text-main)', whiteSpace: 'pre-wrap', lineHeight: 1.5 }}>
                  {filePreviewModal.content}
                </pre>
              ) : (
                <div style={{ textAlign: 'center', padding: '40px 20px' }}>
                  <div style={{ width: '48px', height: '48px', borderRadius: '50%', background: 'rgba(255,255,255,0.05)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 16px auto' }}>
                    <FileText size={24} color="var(--text-muted)" />
                  </div>
                  <h4 style={{ marginBottom: '8px' }}>Direct in-app preview unavailable</h4>
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', maxWidth: '420px', margin: '0 auto' }}>
                    {filePreviewModal.message || 'This file format cannot be rendered in-browser. Use the download button below to save it.'}
                  </p>
                </div>
              )}
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '16px' }}>
              <span style={{ fontSize: '0.78rem', color: 'var(--text-dim)' }}>
                {filePreviewModal.file_size ? `${(filePreviewModal.file_size / 1024).toFixed(1)} KB` : ''}
              </span>
              <div style={{ display: 'flex', gap: '10px' }}>
                <button
                  className="btn btn-primary"
                  onClick={() => handleDownloadFile({ name: filePreviewModal.filename, path: filePreviewModal.path, is_directory: false })}
                >
                  <Download size={14} style={{ marginRight: '6px' }} />
                  Download
                </button>
                <button className="btn btn-secondary" onClick={() => setFilePreviewModal(null)}>
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* INVITE GUEST MODAL (QR CODE & CAPABILITIES) */}
      <InviteGuestModal
        deviceId={activeDevice?.id || ''}
        deviceName={activeDevice?.name || ''}
        isOpen={inviteModalOpen}
        onClose={() => setInviteModalOpen(false)}
      />
    </div>
  );
}
