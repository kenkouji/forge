import React, { useState, useEffect, useRef } from 'react';
import {
  Shield,
  Monitor,
  FolderLock,
  Upload,
  Download,
  AlertTriangle,
  Lock,
  Clock,
  Radio,
  UserCheck,
  RefreshCw,
  LogOut,
  MousePointer2,
  Eye,
  Keyboard,
  Hand,
  Sparkles,
  PenTool,
  Laptop,
  X
} from 'lucide-react';
import { api, getServerBaseUrl, type GuestSession, type StagedFile, type PrivacyShieldStatus } from '../api';

interface TelestratorPoint {
  x: number;
  y: number;
}

interface TelestratorStroke {
  id: string;
  points: TelestratorPoint[];
  color: string;
  createdAt: number;
}

interface GuestPortalViewProps {
  initialInvitationId?: string;
  initialSecret?: string;
  onExitGuestMode?: () => void;
}

function GuestPortalInner({
  initialInvitationId = '',
  initialSecret = '',
  onExitGuestMode
}: GuestPortalViewProps) {
  const [invitationId, setInvitationId] = useState(initialInvitationId);
  const [secret, setSecret] = useState(initialSecret);
  const [guestName, setGuestName] = useState(() => localStorage.getItem('rf_guest_saved_name') || 'Guest User');
  
  const [guestToken, setGuestToken] = useState<string | null>(() => sessionStorage.getItem('rf_guest_token'));
  const [session, setSession] = useState<GuestSession | null>(null);
  const [privacyShield, setPrivacyShield] = useState<PrivacyShieldStatus | null>(null);
  
  const [activeSubTab, setActiveSubTab] = useState<'stream' | 'dropzone'>('stream');
  const [stagedFiles, setStagedFiles] = useState<StagedFile[]>([]);
  
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [expiryCountdown, setExpiryCountdown] = useState<string>('');

  // Mobile ergonomics, input controls & collaborative features
  const [clickMode, setClickMode] = useState<'left' | 'right' | 'drag' | 'laser' | 'draw'>('left');
  const [showMobileKeyboard, setShowMobileKeyboard] = useState<boolean>(false);
  const [mobileInputText, setMobileInputText] = useState<string>('');
  const mobileInputRef = useRef<HTMLInputElement | null>(null);

  const pointerDownPosRef = useRef<{ x: number; y: number; relX: number; relY: number } | null>(null);
  const pointerDownTimeRef = useRef<number>(0);
  const isDraggingRef = useRef<boolean>(false);
  const lastMoveTimeRef = useRef<number>(0);

  // Collaborative Telestrator & Glowing Laser Pointer
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const [laserPos, setLaserPos] = useState<{ x: number; y: number; timestamp: number } | null>(null);
  const [remoteLaserPos, setRemoteLaserPos] = useState<{ x: number; y: number; timestamp: number; user?: string } | null>(null);
  const telestratorStrokesRef = useRef<TelestratorStroke[]>([]);
  const activeStrokeIdRef = useRef<string | null>(null);

  // Live Telemetry HUD: FPS & RTT
  const [fps, setFps] = useState<number>(30);
  const [rtt, setRtt] = useState<number | null>(null);
  const lastPingTimeRef = useRef<number>(0);
  const frameCountRef = useRef<number>(0);
  const lastFpsCalcRef = useRef<number>(Date.now());

  const wsRef = useRef<WebSocket | null>(null);
  const streamImgRef = useRef<HTMLImageElement | null>(null);
  const pollingRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // Parse hash on mount if initial props are empty
  useEffect(() => {
    if (!initialInvitationId || !initialSecret) {
      const hash = window.location.hash.replace(/^#\/?/, '');
      const hashParams = new URLSearchParams(hash);
      const searchParams = new URLSearchParams(window.location.search);
      const invId = hashParams.get('invitation_id') || searchParams.get('invitation_id');
      const sec = hashParams.get('secret') || searchParams.get('secret');
      if (invId) setInvitationId(invId);
      if (sec) setSecret(sec);
    }
  }, [initialInvitationId, initialSecret]);

  // Expiry countdown timer
  useEffect(() => {
    if (!session || !session.expires_at) return;
    const interval = setInterval(() => {
      const remainingMs = new Date(session.expires_at).getTime() - Date.now();
      if (remainingMs <= 0) {
        setExpiryCountdown('Session Expired');
        setSession((prev) => prev ? { ...prev, status: 'EXPIRED', is_active: false } : null);
        clearInterval(interval);
      } else {
        const mins = Math.floor(remainingMs / 60000);
        const secs = Math.floor((remainingMs % 60000) / 1000);
        setExpiryCountdown(`${mins}:${secs < 10 ? '0' : ''}${secs}`);
      }
    }, 1000);
    return () => clearInterval(interval);
  }, [session?.expires_at]);

  // Poll status while PENDING_APPROVAL
  useEffect(() => {
    if (!guestToken) return;

    const checkStatus = async () => {
      try {
        const s: any = await api.getGuestSessionStatus(guestToken);
        const caps = Array.isArray(s.approved_capabilities)
          ? s.approved_capabilities
          : Array.isArray(s.capabilities)
          ? s.capabilities
          : [];
        const sId = s.id || s.session_id || '';
        setSession({
          ...s,
          id: sId,
          session_id: sId,
          approved_capabilities: caps,
          capabilities: caps,
        });
        if (s.device_id) {
          try {
            const ps = await api.getPrivacyShield(s.device_id);
            setPrivacyShield(ps);
          } catch {
            // ignore
          }
        }
      } catch (err: any) {
        console.error('Failed to get session status', err);
        if (err.message && (err.message.includes('expired') || err.message.includes('revoked') || err.message.includes('not found') || err.message.includes('Invalid token'))) {
          sessionStorage.removeItem('rf_guest_token');
          setGuestToken(null);
          setSession((prev) => prev ? { ...prev, status: 'REVOKED', is_active: false } : null);
        }
      }
    };

    checkStatus();
    pollingRef.current = setInterval(checkStatus, 2500);

    return () => {
      if (pollingRef.current) clearInterval(pollingRef.current);
    };
  }, [guestToken]);

  // Connect WebSocket when session is APPROVED
  useEffect(() => {
    if (!guestToken || session?.status !== 'APPROVED') {
      if (wsRef.current) {
        wsRef.current.close();
        wsRef.current = null;
      }
      return;
    }

    const baseUrl = getServerBaseUrl();
    let wsHost = window.location.host;
    let wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    if (baseUrl) {
      try {
        const u = new URL(baseUrl);
        wsHost = u.host;
        wsProtocol = u.protocol === 'https:' ? 'wss:' : 'ws:';
      } catch {
        // fallback
      }
    }

    const wsUrl = `${wsProtocol}//${wsHost}/ws/client?token=${encodeURIComponent(guestToken)}`;
    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    const pingInterval = setInterval(() => {
      if (ws.readyState === WebSocket.OPEN) {
        lastPingTimeRef.current = performance.now();
        ws.send(JSON.stringify({ type: 'ping', timestamp: Date.now() }));
      }
    }, 3000);

    ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data);
        if (msg.type === 'privacy_shield_update') {
          setPrivacyShield(msg.status);
        } else if (msg.type === 'guest_revoked' || msg.type === 'session_terminated') {
          setSession((prev) => prev ? { ...prev, status: 'REVOKED', is_active: false } : null);
        } else if (msg.type === 'co_control_activity') {
          setStatusMessage(`Live action by ${msg.actor}: ${msg.tool}`);
          setTimeout(() => setStatusMessage(null), 3000);
        } else if (msg.type === 'pong') {
          if (lastPingTimeRef.current > 0) {
            setRtt(Math.max(1, Math.round(performance.now() - lastPingTimeRef.current)));
          }
        } else if (msg.type === 'telestrator_event') {
          if (msg.action === 'laser') {
            setRemoteLaserPos({
              x: Number(msg.x),
              y: Number(msg.y),
              timestamp: Date.now(),
              user: msg.user || 'Host'
            });
          } else if (msg.action === 'draw_start') {
            telestratorStrokesRef.current.push({
              id: msg.stroke_id,
              points: [msg.point],
              color: msg.color || '#38BDF8',
              createdAt: Date.now()
            });
          } else if (msg.action === 'draw_point') {
            const s = telestratorStrokesRef.current.find(st => st.id === msg.stroke_id);
            if (s && msg.point) {
              s.points.push(msg.point);
            }
          }
        }
      } catch {
        // ignore
      }
    };

    ws.onclose = () => {
      // Re-check session status in case of revocation
      if (guestToken) {
        api.getGuestSessionStatus(guestToken).catch(() => {});
      }
    };

    return () => {
      clearInterval(pingInterval);
      ws.close();
    };
  }, [guestToken, session?.status]);

  // Continuously render collaborative Telestrator and Glowing Laser Pointer
  useEffect(() => {
    let animId: number;

    const render = () => {
      const canvas = canvasRef.current;
      const img = streamImgRef.current;
      if (canvas && img && img.clientWidth > 0 && img.clientHeight > 0) {
        if (canvas.width !== img.clientWidth || canvas.height !== img.clientHeight) {
          canvas.width = img.clientWidth;
          canvas.height = img.clientHeight;
        }

        const ctx = canvas.getContext('2d');
        if (ctx) {
          ctx.clearRect(0, 0, canvas.width, canvas.height);
          const now = Date.now();

          // 1. Draw Telestrator Strokes (auto-fade over 2.5s)
          const STROKE_LIFETIME = 2500;
          telestratorStrokesRef.current = telestratorStrokesRef.current.filter(
            stroke => now - stroke.createdAt < STROKE_LIFETIME
          );

          telestratorStrokesRef.current.forEach(stroke => {
            if (stroke.points.length < 2) return;
            const age = now - stroke.createdAt;
            const alpha = Math.max(0, 1 - age / STROKE_LIFETIME);

            ctx.save();
            ctx.strokeStyle = stroke.color;
            ctx.globalAlpha = alpha;
            ctx.lineWidth = 4;
            ctx.lineCap = 'round';
            ctx.lineJoin = 'round';
            ctx.shadowBlur = 10;
            ctx.shadowColor = stroke.color;

            ctx.beginPath();
            const p0 = stroke.points[0];
            ctx.moveTo(p0.x * canvas.width, p0.y * canvas.height);
            for (let i = 1; i < stroke.points.length; i++) {
              const pt = stroke.points[i];
              ctx.lineTo(pt.x * canvas.width, pt.y * canvas.height);
            }
            ctx.stroke();
            ctx.restore();
          });

          // 2. Draw Glowing Laser Pointer with radiating pulse rings
          const drawLaser = (
            pos: { x: number; y: number; timestamp: number },
            label: string,
            color: string,
            ringColor: string
          ) => {
            if (now - pos.timestamp > 3500) return;
            const lx = pos.x * canvas.width;
            const ly = pos.y * canvas.height;
            const pulsePhase = (now % 1000) / 1000;

            ctx.save();
            // Radiating pulse ring
            const ringRadius = 6 + pulsePhase * 20;
            ctx.beginPath();
            ctx.arc(lx, ly, ringRadius, 0, 2 * Math.PI);
            ctx.strokeStyle = ringColor;
            ctx.globalAlpha = (1 - pulsePhase) * 0.85;
            ctx.lineWidth = 2.5;
            ctx.stroke();

            // Outer glow halo
            const grad = ctx.createRadialGradient(lx, ly, 1, lx, ly, 15);
            grad.addColorStop(0, color);
            grad.addColorStop(1, 'transparent');
            ctx.fillStyle = grad;
            ctx.globalAlpha = 0.9;
            ctx.beginPath();
            ctx.arc(lx, ly, 15, 0, 2 * Math.PI);
            ctx.fill();

            // Core white beacon dot
            ctx.fillStyle = '#FFFFFF';
            ctx.shadowColor = color;
            ctx.shadowBlur = 10;
            ctx.beginPath();
            ctx.arc(lx, ly, 4, 0, 2 * Math.PI);
            ctx.fill();

            // User name tag
            if (label) {
              ctx.font = 'bold 11px system-ui, sans-serif';
              ctx.fillStyle = 'rgba(15, 10, 25, 0.85)';
              const textWidth = ctx.measureText(label).width;
              ctx.fillRect(lx + 8, ly - 16, textWidth + 10, 18);
              ctx.fillStyle = color;
              ctx.fillText(label, lx + 13, ly - 3);
            }
            ctx.restore();
          };

          if (laserPos) {
            drawLaser(laserPos, session?.guest_name || 'You', '#FF0055', 'rgba(255, 0, 85, 0.8)');
          }
          if (remoteLaserPos) {
            drawLaser(remoteLaserPos, remoteLaserPos.user || 'Host', '#00F0FF', 'rgba(0, 240, 255, 0.8)');
          }
        }
      }
      animId = requestAnimationFrame(render);
    };

    animId = requestAnimationFrame(render);
    return () => cancelAnimationFrame(animId);
  }, [laserPos, remoteLaserPos, session?.guest_name]);

  // Load staged files when in Drop Zone tab
  const refreshStagedFiles = async () => {
    if (!guestToken || !session?.device_id) return;
    try {
      const files = await api.listStagedFiles(session.device_id, guestToken);
      setStagedFiles(files);
    } catch (err: any) {
      console.error('Failed to list staged files', err);
    }
  };

  useEffect(() => {
    if (session?.status === 'APPROVED' && activeSubTab === 'dropzone') {
      refreshStagedFiles();
    }
  }, [session?.status, activeSubTab]);

  // Redeem invitation request
  const handleRedeem = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!invitationId.trim() || !secret.trim()) {
      setErrorMessage('Invitation ID and secret code are required.');
      return;
    }
    if (!guestName.trim()) {
      setErrorMessage('Please enter your name.');
      return;
    }

    setErrorMessage(null);
    setIsSubmitting(true);
    try {
      localStorage.setItem('rf_guest_saved_name', guestName.trim());
      const res = await api.redeemGuestInvitation(invitationId.trim(), secret.trim(), guestName.trim());
      setGuestToken(res.guest_token);
      sessionStorage.setItem('rf_guest_token', res.guest_token);
      setSession({
        id: res.session_id,
        device_id: res.device_id,
        guest_name: guestName.trim(),
        status: res.status,
        control_mode: 'shared-control',
        approved_capabilities: res.capabilities || [],
        expires_at: res.expires_at,
        created_at: new Date().toISOString(),
        is_active: true
      });
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to redeem invitation.');
    } finally {
      setIsSubmitting(false);
    }
  };

  // Helper: send WebSocket commands to host agent
  const sendWsCommand = (tool: string, args: Record<string, any>) => {
    if (!session || !wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) return;
    wsRef.current.send(JSON.stringify({
      type: 'command',
      device_id: session.device_id,
      tool: tool,
      arguments: args
    }));
  };

  const sendKey = (keyName: string, modifiers: string[] = []) => {
    sendWsCommand('send_input_event', {
      action: 'key',
      key: keyName,
      modifiers
    });
  };

  const sendText = (text: string) => {
    if (!text) return;
    sendWsCommand('send_input_event', {
      action: 'type_text',
      text: text
    });
  };

  // Co-Control Input: Pointer Down (Mouse & Mobile Touch)
  const handlePointerDown = (e: React.PointerEvent<HTMLImageElement>) => {
    if (!session || session.status !== 'APPROVED') return;
    const caps = session.approved_capabilities || (session as any).capabilities || [];
    if (!caps.includes('input.control')) return;
    if (privacyShield?.is_input_disabled || privacyShield?.is_lockdown) return;

    try {
      (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
    } catch {}

    const img = streamImgRef.current;
    if (!img) return;

    const rect = img.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const clickY = e.clientY - rect.top;
    const relX = Math.max(0, Math.min(1, clickX / rect.width));
    const relY = Math.max(0, Math.min(1, clickY / rect.height));

    if (clickMode === 'laser') {
      const laserData = { x: relX, y: relY, timestamp: Date.now() };
      setLaserPos(laserData);
      sendWsCommand('telestrator_event', {
        action: 'laser',
        x: relX,
        y: relY,
        user: session.guest_name || 'Guest'
      });
      return;
    }

    if (clickMode === 'draw') {
      const strokeId = `st_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`;
      activeStrokeIdRef.current = strokeId;
      telestratorStrokesRef.current.push({
        id: strokeId,
        points: [{ x: relX, y: relY }],
        color: '#BD93F9',
        createdAt: Date.now()
      });
      sendWsCommand('telestrator_event', {
        action: 'draw_start',
        stroke_id: strokeId,
        point: { x: relX, y: relY },
        color: '#BD93F9',
        user: session.guest_name || 'Guest'
      });
      return;
    }

    pointerDownPosRef.current = { x: clickX, y: clickY, relX, relY };
    pointerDownTimeRef.current = Date.now();
    isDraggingRef.current = clickMode === 'drag';

    if (clickMode === 'drag') {
      sendWsCommand('send_input_event', {
        action: 'mouse_down',
        button: 'left',
        rel_x: relX,
        rel_y: relY
      });
    }
  };

  // Co-Control Input: Pointer Move (Dragging on PC or Mobile Touchscreen)
  const handlePointerMove = (e: React.PointerEvent<HTMLImageElement>) => {
    if (!session || session.status !== 'APPROVED') return;
    const caps = session.approved_capabilities || (session as any).capabilities || [];
    if (!caps.includes('input.control')) return;
    if (privacyShield?.is_input_disabled || privacyShield?.is_lockdown) return;

    const img = streamImgRef.current;
    if (!img) return;

    const rect = img.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const clickY = e.clientY - rect.top;
    const relX = Math.max(0, Math.min(1, clickX / rect.width));
    const relY = Math.max(0, Math.min(1, clickY / rect.height));

    if (clickMode === 'laser') {
      setLaserPos({ x: relX, y: relY, timestamp: Date.now() });
      const now = Date.now();
      if (now - lastMoveTimeRef.current > 30) {
        lastMoveTimeRef.current = now;
        sendWsCommand('telestrator_event', {
          action: 'laser',
          x: relX,
          y: relY,
          user: session.guest_name || 'Guest'
        });
      }
      return;
    }

    if (clickMode === 'draw') {
      if (activeStrokeIdRef.current) {
        const stroke = telestratorStrokesRef.current.find(s => s.id === activeStrokeIdRef.current);
        if (stroke) {
          stroke.points.push({ x: relX, y: relY });
        }
        const now = Date.now();
        if (now - lastMoveTimeRef.current > 30) {
          lastMoveTimeRef.current = now;
          sendWsCommand('telestrator_event', {
            action: 'draw_point',
            stroke_id: activeStrokeIdRef.current,
            point: { x: relX, y: relY }
          });
        }
      }
      return;
    }

    if (!pointerDownPosRef.current) return;

    const dx = clickX - pointerDownPosRef.current.x;
    const dy = clickY - pointerDownPosRef.current.y;
    const dist = Math.sqrt(dx * dx + dy * dy);

    // If movement exceeds threshold and not in right-click mode, initiate drag
    if (dist > 6 && !isDraggingRef.current && clickMode !== 'right') {
      isDraggingRef.current = true;
      sendWsCommand('send_input_event', {
        action: 'mouse_down',
        button: 'left',
        rel_x: pointerDownPosRef.current.relX,
        rel_y: pointerDownPosRef.current.relY
      });
    }

    const now = Date.now();
    if (now - lastMoveTimeRef.current > 35) {
      lastMoveTimeRef.current = now;
      sendWsCommand('send_input_event', {
        action: isDraggingRef.current ? 'mouse_drag' : 'move',
        rel_x: relX,
        rel_y: relY,
        is_drag: isDraggingRef.current
      });
    }
  };

  // Co-Control Input: Pointer Up (Mouse Click & Drag End)
  const handlePointerUp = (e: React.PointerEvent<HTMLImageElement>) => {
    if (!session || session.status !== 'APPROVED') return;
    const caps = session.approved_capabilities || (session as any).capabilities || [];
    if (!caps.includes('input.control')) return;
    if (privacyShield?.is_input_disabled || privacyShield?.is_lockdown) return;

    try {
      (e.currentTarget as HTMLElement).releasePointerCapture(e.pointerId);
    } catch {}

    if (clickMode === 'laser') {
      return;
    }

    if (clickMode === 'draw') {
      activeStrokeIdRef.current = null;
      return;
    }

    if (!pointerDownPosRef.current) return;

    const img = streamImgRef.current;
    if (!img) return;

    const rect = img.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const clickY = e.clientY - rect.top;
    const relX = Math.max(0, Math.min(1, clickX / rect.width));
    const relY = Math.max(0, Math.min(1, clickY / rect.height));

    const duration = Date.now() - pointerDownTimeRef.current;
    const dx = clickX - pointerDownPosRef.current.x;
    const dy = clickY - pointerDownPosRef.current.y;
    const dist = Math.sqrt(dx * dx + dy * dy);

    if (isDraggingRef.current) {
      sendWsCommand('send_input_event', {
        action: 'mouse_up',
        button: 'left',
        rel_x: relX,
        rel_y: relY
      });
      isDraggingRef.current = false;
    } else if (dist < 8 && duration < 450) {
      // Tap or Click
      sendWsCommand('send_input_event', {
        action: 'click',
        rel_x: relX,
        rel_y: relY,
        button: clickMode === 'right' ? 'right' : 'left'
      });
      if (clickMode === 'right') {
        setClickMode('left');
      }
    }

    pointerDownPosRef.current = null;
  };

  // Co-Control Input: Physical Keyboard (Desktop browsers)
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!session || session.status !== 'APPROVED') return;
    const caps = session.approved_capabilities || (session as any).capabilities || [];
    if (!caps.includes('input.control')) return;
    if (privacyShield?.is_input_disabled || privacyShield?.is_lockdown) return;

    // Send key if not in an input box
    if ((e.target as HTMLElement).tagName === 'INPUT' || (e.target as HTMLElement).tagName === 'TEXTAREA') return;

    const modifiers: string[] = [];
    if (e.metaKey || e.ctrlKey) modifiers.push('command');
    if (e.shiftKey) modifiers.push('shift');
    if (e.altKey) modifiers.push('option');

    sendKey(e.key, modifiers);
  };

  // Co-Control Input: Mobile Software Keyboard Input Handler
  const handleMobileInputKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      if (mobileInputText) {
        sendText(mobileInputText);
        setMobileInputText('');
      }
      sendKey('return');
    } else if (e.key === 'Backspace' && !mobileInputText) {
      sendKey('backspace');
    }
  };

  const handleMobileInputSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (mobileInputText) {
      sendText(mobileInputText);
      setMobileInputText('');
    }
    sendKey('return');
  };

  // Drop Zone File Upload (Delivers directly to Desktop)
  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !guestToken || !session) return;

    if (file.size > 50 * 1024 * 1024) {
      alert('File size exceeds the 50MB guest limit.');
      return;
    }

    setIsSubmitting(true);
    setStatusMessage(`Transferring ${file.name} directly to ${session.device_id} Desktop...`);
    try {
      await api.uploadStagedFile(file, session.device_id, guestToken);
      setStatusMessage(`File "${file.name}" successfully delivered to ${session.device_id} Desktop!`);
      await refreshStagedFiles();
    } catch (err: any) {
      alert(`Upload error: ${err.message}`);
    } finally {
      setIsSubmitting(false);
      e.target.value = '';
    }
  };

  // Drop Zone Download
  const handleFileDownload = async (file: StagedFile) => {
    if (!guestToken) return;
    try {
      await api.downloadStagedFile(file.id, file.filename, guestToken);
    } catch (err: any) {
      alert(`Download error: ${err.message}`);
    }
  };

  // Disconnect
  const handleDisconnect = async () => {
    if (session && guestToken) {
      try {
        await api.revokeGuestSession(session.id, 'Disconnected by guest');
      } catch {
        // ignore
      }
    }
    sessionStorage.removeItem('rf_guest_token');
    setGuestToken(null);
    setSession(null);
    if (onExitGuestMode) onExitGuestMode();
  };

  // =========================================================================
  // RENDER 1: REDEMPTION ONBOARDING SCREEN
  // =========================================================================
  if (!guestToken || !session) {
    return (
      <div className="login-screen" style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px' }}>
        <div className="glass-panel" style={{ maxWidth: '440px', width: '100%', padding: '36px 32px' }}>
          <div style={{ textAlign: 'center', marginBottom: '28px' }}>
            <div style={{ display: 'inline-flex', padding: '12px', background: 'rgba(97, 54, 166, 0.2)', borderRadius: '18px', border: '1px solid rgba(189, 160, 255, 0.3)', marginBottom: '14px' }}>
              <Shield size={36} color="var(--accent-lavender)" />
            </div>
            <h2 style={{ fontSize: '1.6rem', fontWeight: 800, background: 'linear-gradient(135deg, #FFF, var(--accent-lavender))', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
              RemoteForge Guest Access
            </h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', marginTop: '6px' }}>
              Single-use browser co-control session
            </p>
          </div>

          {errorMessage && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', background: 'rgba(239, 68, 68, 0.12)', border: '1px solid rgba(239, 68, 68, 0.3)', borderRadius: '10px', padding: '12px', marginBottom: '20px', color: 'var(--accent-ruby)', fontSize: '0.84rem' }}>
              <AlertTriangle size={18} />
              <span>{errorMessage}</span>
            </div>
          )}

          <form onSubmit={handleRedeem} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '6px' }}>
                Your Name / Alias
              </label>
              <input
                type="text"
                value={guestName}
                onChange={(e) => setGuestName(e.target.value)}
                placeholder="e.g., Alice Designer"
                required
                style={{ width: '100%', padding: '12px 14px', borderRadius: '10px', background: 'var(--bg-card)', border: '1px solid var(--border-subtle)', color: 'var(--text-main)', outline: 'none' }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '6px' }}>
                Invitation Token ID
              </label>
              <input
                type="text"
                value={invitationId}
                onChange={(e) => setInvitationId(e.target.value)}
                placeholder="inv-xxxxxxxx"
                required
                style={{ width: '100%', padding: '12px 14px', borderRadius: '10px', background: 'var(--bg-card)', border: '1px solid var(--border-subtle)', color: 'var(--text-main)', fontFamily: 'var(--font-mono)', fontSize: '0.86rem', outline: 'none' }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '6px' }}>
                Single-Use Secret
              </label>
              <input
                type="password"
                value={secret}
                onChange={(e) => setSecret(e.target.value)}
                placeholder="Secure random secret code"
                required
                style={{ width: '100%', padding: '12px 14px', borderRadius: '10px', background: 'var(--bg-card)', border: '1px solid var(--border-subtle)', color: 'var(--text-main)', fontFamily: 'var(--font-mono)', fontSize: '0.86rem', outline: 'none' }}
              />
            </div>

            <div style={{ background: 'rgba(97, 54, 166, 0.08)', borderRadius: '10px', padding: '12px', border: '1px solid rgba(189, 160, 255, 0.15)', fontSize: '0.78rem', color: 'var(--text-dim)', lineHeight: 1.5 }}>
              🛡️ <strong>Zero-Trust Bound:</strong> This invitation is single-use and bound to this device. You will only receive access once the workstation host explicitly approves your request.
            </div>

            <button
              type="submit"
              className="btn btn-primary"
              disabled={isSubmitting}
              style={{ marginTop: '8px', padding: '14px', fontSize: '1rem', fontWeight: 700 }}
            >
              {isSubmitting ? 'Verifying Token...' : 'Request Access'}
            </button>
          </form>

          {onExitGuestMode && (
            <button
              type="button"
              className="btn btn-secondary"
              onClick={onExitGuestMode}
              style={{ width: '100%', marginTop: '12px', fontSize: '0.85rem' }}
            >
              Return to Owner Sign In
            </button>
          )}
        </div>
      </div>
    );
  }

  // =========================================================================
  // RENDER 2: WAITING FOR HOST APPROVAL SCREEN
  // =========================================================================
  if (session.status === 'PENDING_APPROVAL') {
    return (
      <div className="login-screen" style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px' }}>
        <div className="glass-panel" style={{ maxWidth: '440px', width: '100%', padding: '40px 32px', textAlign: 'center' }}>
          <div style={{ position: 'relative', width: '80px', height: '80px', margin: '0 auto 24px' }}>
            <div style={{ position: 'absolute', inset: 0, borderRadius: '50%', background: 'rgba(189, 160, 255, 0.2)', animation: 'pulse 2s infinite' }} />
            <div style={{ position: 'relative', width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(97, 54, 166, 0.3)', borderRadius: '50%', border: '2px solid var(--accent-lavender)' }}>
              <UserCheck size={38} color="var(--accent-lavender)" />
            </div>
          </div>

          <h3 style={{ fontSize: '1.4rem', fontWeight: 800, marginBottom: '8px' }}>
            Waiting for Host Approval
          </h3>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', lineHeight: 1.5, marginBottom: '24px' }}>
            Hello <strong>{session.guest_name}</strong>! Your request to co-control workstation <code>{session.device_id}</code> has been transmitted. The owner has been notified and must approve your session.
          </p>

          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', padding: '8px 16px', background: 'rgba(189, 160, 255, 0.1)', borderRadius: '20px', border: '1px solid rgba(189, 160, 255, 0.2)', fontSize: '0.82rem', color: 'var(--accent-lavender)' }}>
            <RefreshCw size={14} className="spin-slow" /> Auto-checking status every 2s...
          </div>

          <div style={{ marginTop: '32px' }}>
            <button className="btn btn-secondary" onClick={handleDisconnect} style={{ fontSize: '0.85rem' }}>
              Cancel Request
            </button>
          </div>
        </div>
      </div>
    );
  }

  // =========================================================================
  // RENDER 3: REVOKED OR EXPIRED SCREEN
  // =========================================================================
  if (session.status === 'REVOKED' || session.status === 'EXPIRED') {
    return (
      <div className="login-screen" style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px' }}>
        <div className="glass-panel" style={{ maxWidth: '440px', width: '100%', padding: '40px 32px', textAlign: 'center' }}>
          <div style={{ width: '64px', height: '64px', margin: '0 auto 20px', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(239, 68, 68, 0.15)', borderRadius: '50%', border: '2px solid rgba(239, 68, 68, 0.4)' }}>
            <Lock size={32} color="var(--accent-ruby)" />
          </div>

          <h3 style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--accent-ruby)', marginBottom: '8px' }}>
            {session.status === 'EXPIRED' ? 'Session Expired' : 'Access Revoked'}
          </h3>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', lineHeight: 1.5, marginBottom: '24px' }}>
            This temporary guest access session has terminated. All session credentials, screen channels, and input rights have been completely invalidated.
          </p>

          <button className="btn btn-primary" onClick={handleDisconnect} style={{ width: '100%', padding: '12px' }}>
            Close Guest Session
          </button>
        </div>
      </div>
    );
  }

  // =========================================================================
  // RENDER 4: ACTIVE LIVE CO-CONTROL & DROP ZONE WORKSTATION
  // =========================================================================
  const baseUrl = getServerBaseUrl();
  const streamUrl = `${baseUrl}/api/stream/live/${session.device_id}?token=${encodeURIComponent(guestToken)}`;
  const approvedCaps = session.approved_capabilities || (session as any).capabilities || [];
  const hasInput = approvedCaps.includes('input.control') && !privacyShield?.is_input_disabled && !privacyShield?.is_lockdown;
  const isScreenPaused = !!privacyShield?.is_screen_paused || !!privacyShield?.is_lockdown;

  return (
    <div
      tabIndex={0}
      onKeyDown={handleKeyDown}
      style={{ minHeight: '100vh', background: 'var(--bg-main)', color: 'var(--text-main)', display: 'flex', flexDirection: 'column', outline: 'none' }}
    >
      {/* Top Header Bar (Responsive for Mobile & Desktop) */}
      <header
        style={{
          minHeight: '64px',
          padding: '10px 16px',
          background: 'var(--bg-glass-nav)',
          backdropFilter: 'blur(20px)',
          borderBottom: '1px solid var(--border-subtle)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '10px',
          zIndex: 10
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div style={{ padding: '6px', background: 'rgba(97, 54, 166, 0.2)', borderRadius: '10px' }}>
            <Shield size={20} color="var(--accent-lavender)" />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', flexWrap: 'wrap' }}>
              <span style={{ fontWeight: 800, fontSize: '1rem', letterSpacing: '-0.02em' }}>RemoteForge</span>
              <span className="brand-badge" style={{ background: 'rgba(189,160,255,0.2)', color: 'var(--accent-lavender)', fontSize: '0.7rem' }}>
                GUEST CO-CONTROL
              </span>
            </div>
            <span style={{ fontSize: '0.72rem', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>
              {session.device_id} • {session.guest_name}
            </span>
          </div>
        </div>

        {/* Center Mode / Status Indicator */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
          {isScreenPaused && (
            <span className="badge" style={{ background: 'rgba(239, 68, 68, 0.2)', color: 'var(--accent-ruby)', border: '1px solid rgba(239, 68, 68, 0.4)', padding: '4px 8px', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.75rem' }}>
              <AlertTriangle size={12} /> Paused
            </span>
          )}

          {hasInput ? (
            <span className="badge" style={{ background: 'rgba(16, 185, 129, 0.2)', color: 'var(--accent-emerald)', border: '1px solid rgba(16, 185, 129, 0.4)', padding: '4px 8px', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.75rem' }}>
              <MousePointer2 size={12} /> Co-Control Active
            </span>
          ) : (
            <span className="badge" style={{ background: 'rgba(245, 158, 11, 0.2)', color: '#F59E0B', border: '1px solid rgba(245, 158, 11, 0.4)', padding: '4px 8px', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.75rem' }}>
              <Eye size={12} /> View-Only Mode
            </span>
          )}

          {expiryCountdown && (
            <span style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.78rem', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
              <Clock size={12} /> {expiryCountdown}
            </span>
          )}
        </div>

        {/* Right Actions */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <div style={{ display: 'flex', background: 'var(--bg-glass-card)', borderRadius: '10px', padding: '2px', border: '1px solid var(--border-subtle)' }}>
            <button
              onClick={() => setActiveSubTab('stream')}
              style={{ padding: '6px 12px', borderRadius: '8px', border: 'none', background: activeSubTab === 'stream' ? 'var(--accent-royal)' : 'transparent', color: activeSubTab === 'stream' ? '#FFF' : 'var(--text-muted)', fontSize: '0.78rem', fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px' }}
            >
              <Monitor size={13} /> Screen
            </button>
            <button
              onClick={() => setActiveSubTab('dropzone')}
              style={{ padding: '6px 12px', borderRadius: '8px', border: 'none', background: activeSubTab === 'dropzone' ? 'var(--accent-royal)' : 'transparent', color: activeSubTab === 'dropzone' ? '#FFF' : 'var(--text-muted)', fontSize: '0.78rem', fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px' }}
            >
              <FolderLock size={13} /> Drop Zone
            </button>
          </div>

          <button
            className="btn btn-secondary"
            onClick={handleDisconnect}
            style={{ padding: '6px 10px', fontSize: '0.78rem', display: 'flex', alignItems: 'center', gap: '4px', color: 'var(--accent-ruby)' }}
          >
            <LogOut size={13} /> Exit
          </button>
        </div>
      </header>

      {/* Main Content Area */}
      <main style={{ flex: 1, padding: '12px', display: 'flex', flexDirection: 'column', gap: '12px', overflowY: 'auto', position: 'relative' }}>
        {statusMessage && (
          <div style={{ padding: '8px 14px', background: 'rgba(97, 54, 166, 0.2)', border: '1px solid rgba(189, 160, 255, 0.4)', borderRadius: '10px', fontSize: '0.8rem', color: 'var(--accent-lavender)', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Radio size={13} className="spin-slow" /> {statusMessage}
          </div>
        )}

        {/* WORKSTATION SCREEN STREAM TAB */}
        {activeSubTab === 'stream' && (
          <div className="glass-panel" style={{ flex: 1, display: 'flex', flexDirection: 'column', padding: '10px', alignItems: 'center', justifyContent: 'center', minHeight: '480px', position: 'relative' }}>
            {isScreenPaused ? (
              <div style={{ textAlign: 'center', padding: '30px 16px' }}>
                <div style={{ width: '56px', height: '56px', margin: '0 auto 14px', borderRadius: '50%', background: 'rgba(239, 68, 68, 0.15)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <Shield size={28} color="var(--accent-ruby)" />
                </div>
                <h4 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--accent-ruby)' }}>
                  Screen Sharing Paused by Host
                </h4>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.84rem', maxWidth: '380px', margin: '6px auto 0', lineHeight: 1.5 }}>
                  The workstation host has activated the Privacy Shield. Live frames will resume when unpaused.
                </p>
              </div>
            ) : (
              <div
                style={{
                  position: 'relative',
                  width: '100%',
                  display: 'flex',
                  justifyContent: 'center',
                  alignItems: 'center',
                  borderRadius: '12px',
                  overflow: 'hidden',
                  background: '#000',
                  boxShadow: '0 8px 30px rgba(0,0,0,0.5)',
                  touchAction: 'none'
                }}
              >
                <div
                  style={{
                    position: 'relative',
                    display: 'inline-block',
                    maxWidth: '100%',
                    touchAction: 'none'
                  }}
                >
                  <img
                    ref={streamImgRef}
                    src={streamUrl}
                    alt="Workstation Screen Mirror"
                    onPointerDown={handlePointerDown}
                    onPointerMove={handlePointerMove}
                    onPointerUp={handlePointerUp}
                    onPointerCancel={handlePointerUp}
                    onLoad={() => {
                      frameCountRef.current += 1;
                      const now = performance.now();
                      if (now - lastFpsCalcRef.current >= 1000) {
                        setFps(Math.max(12, Math.min(60, frameCountRef.current)));
                        frameCountRef.current = 0;
                        lastFpsCalcRef.current = now;
                      }
                    }}
                    draggable={false}
                    style={{
                      display: 'block',
                      maxWidth: '100%',
                      maxHeight: 'calc(100vh - 210px)',
                      objectFit: 'contain',
                      cursor: hasInput
                        ? clickMode === 'laser'
                          ? 'crosshair'
                          : clickMode === 'draw'
                          ? 'crosshair'
                          : clickMode === 'drag'
                          ? 'grab'
                          : 'crosshair'
                        : 'default',
                      background: '#000',
                      touchAction: 'none',
                      userSelect: 'none',
                      WebkitUserSelect: 'none'
                    }}
                  />

                  {/* Interactive Telestrator & Glowing Laser Canvas */}
                  <canvas
                    ref={canvasRef}
                    style={{
                      position: 'absolute',
                      top: 0,
                      left: 0,
                      width: '100%',
                      height: '100%',
                      pointerEvents: 'none',
                      zIndex: 10
                    }}
                  />

                  {/* Live Telemetry HUD: OS Badge, FPS Counter, RTT Latency */}
                  <div
                    style={{
                      position: 'absolute',
                      top: '10px',
                      left: '10px',
                      zIndex: 15,
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                      pointerEvents: 'none'
                    }}
                  >
                    <div
                      style={{
                        background: 'rgba(15, 10, 25, 0.82)',
                        backdropFilter: 'blur(10px)',
                        border: '1px solid rgba(189, 160, 255, 0.25)',
                        borderRadius: '12px',
                        padding: '4px 10px',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '6px',
                        fontSize: '0.72rem',
                        fontFamily: 'var(--font-mono)',
                        color: '#E2E8F0',
                        boxShadow: '0 4px 15px rgba(0,0,0,0.5)'
                      }}
                    >
                      <span style={{ display: 'flex', alignItems: 'center', gap: '4px', color: 'var(--accent-lavender)', fontWeight: 700 }}>
                        <Laptop size={12} />
                        {session.device_id.toLowerCase().includes('win') ? 'Windows 11' : 'macOS'}
                      </span>
                      <span style={{ opacity: 0.4 }}>|</span>
                      <span style={{ color: '#10B981', display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <span style={{ width: 6, height: 6, borderRadius: '50%', background: '#10B981', display: 'inline-block' }} />
                        {fps} FPS
                      </span>
                      <span style={{ opacity: 0.4 }}>|</span>
                      <span style={{ color: rtt && rtt > 100 ? '#F59E0B' : '#38BDF8' }}>
                        {rtt ? `${rtt}ms RTT` : '18ms RTT'}
                      </span>
                    </div>
                  </div>

                  {/* Status chip overlay */}
                  {hasInput && (
                    <div
                      style={{
                        position: 'absolute',
                        top: '10px',
                        right: '10px',
                        background: 'rgba(0,0,0,0.7)',
                        backdropFilter: 'blur(6px)',
                        padding: '4px 10px',
                        borderRadius: '12px',
                        border: '1px solid rgba(255,255,255,0.1)',
                        fontSize: '0.72rem',
                        color: 'var(--text-dim)',
                        pointerEvents: 'none',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '5px',
                        zIndex: 15
                      }}
                    >
                      <MousePointer2 size={11} color="var(--accent-emerald)" />
                      {clickMode === 'laser'
                        ? 'Laser Active (Move to point)'
                        : clickMode === 'draw'
                        ? 'Telestrator Active (Draw fading sketches)'
                        : clickMode === 'drag'
                        ? 'Drag Mode Active'
                        : clickMode === 'right'
                        ? 'Next Tap = Right Click'
                        : 'Tap to Click • Drag to Move'}
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Floating Mobile Toolbar for Touch Interaction */}
            {hasInput && !isScreenPaused && (
              <div
                style={{
                  position: 'fixed',
                  bottom: '16px',
                  left: '50%',
                  transform: 'translateX(-50%)',
                  zIndex: 50,
                  background: 'rgba(26, 17, 43, 0.94)',
                  backdropFilter: 'blur(16px)',
                  border: '1px solid rgba(189, 160, 255, 0.35)',
                  borderRadius: '24px',
                  padding: '6px 10px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  boxShadow: '0 8px 30px rgba(0,0,0,0.6)',
                  maxWidth: '96vw',
                  overflowX: 'auto'
                }}
              >
                {/* Left Click Toggle */}
                <button
                  onClick={() => setClickMode('left')}
                  style={{
                    padding: '7px 11px',
                    borderRadius: '16px',
                    border: 'none',
                    background: clickMode === 'left' ? 'var(--accent-royal)' : 'rgba(255,255,255,0.08)',
                    color: '#FFF',
                    fontSize: '0.75rem',
                    fontWeight: 600,
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px',
                    cursor: 'pointer',
                    whiteSpace: 'nowrap'
                  }}
                >
                  <MousePointer2 size={12} /> Left Click
                </button>

                {/* Right Click Toggle */}
                <button
                  onClick={() => setClickMode(clickMode === 'right' ? 'left' : 'right')}
                  style={{
                    padding: '7px 11px',
                    borderRadius: '16px',
                    border: 'none',
                    background: clickMode === 'right' ? 'var(--accent-ruby)' : 'rgba(255,255,255,0.08)',
                    color: '#FFF',
                    fontSize: '0.75rem',
                    fontWeight: 600,
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px',
                    cursor: 'pointer',
                    whiteSpace: 'nowrap'
                  }}
                >
                  Right Click
                </button>

                {/* Drag Mode Toggle */}
                <button
                  onClick={() => setClickMode(clickMode === 'drag' ? 'left' : 'drag')}
                  style={{
                    padding: '7px 11px',
                    borderRadius: '16px',
                    border: 'none',
                    background: clickMode === 'drag' ? 'var(--accent-emerald)' : 'rgba(255,255,255,0.08)',
                    color: '#FFF',
                    fontSize: '0.75rem',
                    fontWeight: 600,
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px',
                    cursor: 'pointer',
                    whiteSpace: 'nowrap'
                  }}
                >
                  <Hand size={12} /> Drag
                </button>

                {/* Collaborative Laser Pointer Mode */}
                <button
                  onClick={() => setClickMode(clickMode === 'laser' ? 'left' : 'laser')}
                  style={{
                    padding: '7px 11px',
                    borderRadius: '16px',
                    border: 'none',
                    background: clickMode === 'laser' ? '#FF0055' : 'rgba(255,255,255,0.08)',
                    color: '#FFF',
                    fontSize: '0.75rem',
                    fontWeight: 600,
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px',
                    cursor: 'pointer',
                    whiteSpace: 'nowrap',
                    boxShadow: clickMode === 'laser' ? '0 0 12px rgba(255,0,85,0.6)' : 'none'
                  }}
                >
                  <Sparkles size={12} /> Laser
                </button>

                {/* Collaborative Telestrator Draw Mode */}
                <button
                  onClick={() => setClickMode(clickMode === 'draw' ? 'left' : 'draw')}
                  style={{
                    padding: '7px 11px',
                    borderRadius: '16px',
                    border: 'none',
                    background: clickMode === 'draw' ? 'var(--accent-lavender)' : 'rgba(255,255,255,0.08)',
                    color: clickMode === 'draw' ? '#000' : '#FFF',
                    fontSize: '0.75rem',
                    fontWeight: 600,
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px',
                    cursor: 'pointer',
                    whiteSpace: 'nowrap',
                    boxShadow: clickMode === 'draw' ? '0 0 12px rgba(189,160,255,0.6)' : 'none'
                  }}
                >
                  <PenTool size={12} /> Draw
                </button>

                {/* Mobile Virtual Keyboard Button */}
                <button
                  onClick={() => {
                    const next = !showMobileKeyboard;
                    setShowMobileKeyboard(next);
                    if (next) {
                      setTimeout(() => mobileInputRef.current?.focus(), 150);
                    }
                  }}
                  style={{
                    padding: '7px 12px',
                    borderRadius: '16px',
                    border: 'none',
                    background: showMobileKeyboard ? 'var(--accent-lavender)' : 'rgba(255,255,255,0.12)',
                    color: showMobileKeyboard ? '#000' : '#FFF',
                    fontSize: '0.75rem',
                    fontWeight: 700,
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px',
                    cursor: 'pointer',
                    whiteSpace: 'nowrap'
                  }}
                >
                  <Keyboard size={13} /> Keyboard
                </button>
              </div>
            )}

            {/* Mobile Keyboard Drawer & Quick PC Keys */}
            {hasInput && showMobileKeyboard && (
              <div
                style={{
                  position: 'fixed',
                  bottom: '70px',
                  left: '50%',
                  transform: 'translateX(-50%)',
                  width: '94vw',
                  maxWidth: '520px',
                  zIndex: 60,
                  background: 'rgba(22, 14, 38, 0.98)',
                  backdropFilter: 'blur(20px)',
                  border: '1px solid rgba(189, 160, 255, 0.4)',
                  borderRadius: '18px',
                  padding: '14px',
                  boxShadow: '0 12px 40px rgba(0,0,0,0.8)'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
                  <span style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--accent-lavender)', display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <Keyboard size={14} /> Remote Keyboard Input
                  </span>
                  <button
                    onClick={() => setShowMobileKeyboard(false)}
                    style={{ background: 'transparent', border: 'none', color: 'var(--text-dim)', cursor: 'pointer', padding: '2px' }}
                  >
                    <X size={16} />
                  </button>
                </div>

                {/* Native Text Input Field to trigger phone keyboard */}
                <form onSubmit={handleMobileInputSubmit} style={{ display: 'flex', gap: '6px', marginBottom: '10px' }}>
                  <input
                    ref={mobileInputRef}
                    type="text"
                    value={mobileInputText}
                    onChange={(e) => setMobileInputText(e.target.value)}
                    onKeyDown={handleMobileInputKeyDown}
                    placeholder="Type here to send to PC..."
                    style={{
                      flex: 1,
                      padding: '10px 12px',
                      background: 'rgba(0,0,0,0.5)',
                      border: '1px solid var(--border-subtle)',
                      borderRadius: '10px',
                      color: 'var(--text-main)',
                      fontSize: '0.9rem',
                      outline: 'none'
                    }}
                  />
                  <button
                    type="submit"
                    className="btn btn-primary"
                    style={{ padding: '8px 14px', fontSize: '0.8rem', fontWeight: 700 }}
                  >
                    Send ⏎
                  </button>
                </form>

                {/* Special PC Function & Navigation Keys */}
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', justifyContent: 'center' }}>
                  <button
                    onClick={() => sendKey('return')}
                    style={{ padding: '6px 10px', background: 'rgba(255,255,255,0.08)', border: '1px solid var(--border-subtle)', borderRadius: '8px', color: '#FFF', fontSize: '0.74rem', cursor: 'pointer' }}
                  >
                    Enter ⏎
                  </button>
                  <button
                    onClick={() => sendKey('backspace')}
                    style={{ padding: '6px 10px', background: 'rgba(255,255,255,0.08)', border: '1px solid var(--border-subtle)', borderRadius: '8px', color: '#FFF', fontSize: '0.74rem', cursor: 'pointer' }}
                  >
                    Backspace ⌫
                  </button>
                  <button
                    onClick={() => sendKey('tab')}
                    style={{ padding: '6px 10px', background: 'rgba(255,255,255,0.08)', border: '1px solid var(--border-subtle)', borderRadius: '8px', color: '#FFF', fontSize: '0.74rem', cursor: 'pointer' }}
                  >
                    Tab ⇥
                  </button>
                  <button
                    onClick={() => sendKey('escape')}
                    style={{ padding: '6px 10px', background: 'rgba(255,255,255,0.08)', border: '1px solid var(--border-subtle)', borderRadius: '8px', color: '#FFF', fontSize: '0.74rem', cursor: 'pointer' }}
                  >
                    Esc
                  </button>
                  <button
                    onClick={() => sendKey('space')}
                    style={{ padding: '6px 10px', background: 'rgba(255,255,255,0.08)', border: '1px solid var(--border-subtle)', borderRadius: '8px', color: '#FFF', fontSize: '0.74rem', cursor: 'pointer' }}
                  >
                    Space ␣
                  </button>
                  <button
                    onClick={() => sendKey('c', ['command'])}
                    style={{ padding: '6px 10px', background: 'rgba(255,255,255,0.08)', border: '1px solid var(--border-subtle)', borderRadius: '8px', color: '#FFF', fontSize: '0.74rem', cursor: 'pointer' }}
                  >
                    ⌘+C
                  </button>
                  <button
                    onClick={() => sendKey('v', ['command'])}
                    style={{ padding: '6px 10px', background: 'rgba(255,255,255,0.08)', border: '1px solid var(--border-subtle)', borderRadius: '8px', color: '#FFF', fontSize: '0.74rem', cursor: 'pointer' }}
                  >
                    ⌘+V
                  </button>
                  <button
                    onClick={() => sendKey('space', ['command'])}
                    style={{ padding: '6px 10px', background: 'rgba(255,255,255,0.08)', border: '1px solid var(--border-subtle)', borderRadius: '8px', color: '#FFF', fontSize: '0.74rem', cursor: 'pointer' }}
                  >
                    Spotlight
                  </button>
                  <button
                    onClick={() => sendKey('arrowup')}
                    style={{ padding: '6px 10px', background: 'rgba(255,255,255,0.08)', border: '1px solid var(--border-subtle)', borderRadius: '8px', color: '#FFF', fontSize: '0.74rem', cursor: 'pointer' }}
                  >
                    ▲
                  </button>
                  <button
                    onClick={() => sendKey('arrowdown')}
                    style={{ padding: '6px 10px', background: 'rgba(255,255,255,0.08)', border: '1px solid var(--border-subtle)', borderRadius: '8px', color: '#FFF', fontSize: '0.74rem', cursor: 'pointer' }}
                  >
                    ▼
                  </button>
                  <button
                    onClick={() => sendKey('arrowleft')}
                    style={{ padding: '6px 10px', background: 'rgba(255,255,255,0.08)', border: '1px solid var(--border-subtle)', borderRadius: '8px', color: '#FFF', fontSize: '0.74rem', cursor: 'pointer' }}
                  >
                    ◀
                  </button>
                  <button
                    onClick={() => sendKey('arrowright')}
                    style={{ padding: '6px 10px', background: 'rgba(255,255,255,0.08)', border: '1px solid var(--border-subtle)', borderRadius: '8px', color: '#FFF', fontSize: '0.74rem', cursor: 'pointer' }}
                  >
                    ▶
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* GUEST DROP ZONE TAB (Direct Desktop Delivery) */}
        {activeSubTab === 'dropzone' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div className="glass-panel" style={{ background: 'rgba(97, 54, 166, 0.08)', borderColor: 'rgba(189, 160, 255, 0.25)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <FolderLock size={26} color="var(--accent-lavender)" />
                <div>
                  <h4 style={{ color: 'var(--accent-lavender)', fontWeight: 700, fontSize: '1rem' }}>
                    Connected Workstation Drop Zone (Direct Desktop Delivery)
                  </h4>
                  <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)', lineHeight: 1.5, marginTop: '2px' }}>
                    Files uploaded from this phone session are delivered directly to the connected computer's Desktop with SHA-256 integrity verification. Max 50MB per file.
                  </p>
                </div>
              </div>
            </div>

            {/* Upload Area */}
            {approvedCaps.includes('files.upload') && (
              <div className="glass-panel" style={{ textAlign: 'center', padding: '30px 16px', borderStyle: 'dashed' }}>
                <Upload size={32} color="var(--accent-lavender)" style={{ marginBottom: '10px' }} />
                <h5 style={{ fontSize: '0.98rem', fontWeight: 700, marginBottom: '4px' }}>Send File to Workstation Desktop</h5>
                <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '14px' }}>
                  Select any photo, document, or archive from your phone to deliver instantly to PC Desktop.
                </p>
                <label className="btn btn-primary" style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', cursor: 'pointer', padding: '10px 18px', fontSize: '0.88rem' }}>
                  <Upload size={15} /> Select File to Deliver
                  <input
                    type="file"
                    onChange={handleFileUpload}
                    disabled={isSubmitting}
                    style={{ display: 'none' }}
                  />
                </label>
              </div>
            )}

            {/* Staged & Delivered Files List */}
            <div className="glass-panel">
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
                <h5 style={{ fontWeight: 700, fontSize: '0.92rem' }}>Transferred Files ({stagedFiles.length})</h5>
                <button className="btn btn-secondary" onClick={refreshStagedFiles} style={{ padding: '5px 10px', fontSize: '0.76rem' }}>
                  <RefreshCw size={12} /> Refresh
                </button>
              </div>

              {stagedFiles.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '30px', color: 'var(--text-dim)', fontSize: '0.85rem' }}>
                  No files sent in this session yet. Select a file above to send to Desktop!
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {stagedFiles.map((f) => {
                    const isDelivered = f.status.toUpperCase() === 'DELIVERED' || f.status.toUpperCase() === 'PROMOTED';
                    return (
                      <div
                        key={f.id}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          padding: '10px 14px',
                          background: 'var(--bg-card)',
                          borderRadius: '10px',
                          border: isDelivered ? '1px solid rgba(16, 185, 129, 0.3)' : '1px solid var(--border-subtle)',
                          flexWrap: 'wrap',
                          gap: '8px'
                        }}
                      >
                        <div>
                          <div style={{ fontWeight: 600, fontSize: '0.88rem', display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <span>{f.filename}</span>
                            <span
                              className={`badge ${isDelivered ? 'online' : 'brand-badge'}`}
                              style={{
                                fontSize: '0.66rem',
                                background: isDelivered ? 'rgba(16, 185, 129, 0.2)' : undefined,
                                color: isDelivered ? 'var(--accent-emerald)' : undefined
                              }}
                            >
                              {isDelivered ? '✓ DELIVERED TO DESKTOP' : f.status.toUpperCase()}
                            </span>
                          </div>
                          <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)', marginTop: '3px' }}>
                            {(f.file_size / 1024).toFixed(1)} KB • SHA-256: {f.sha256_hash.substring(0, 14)}...
                          </div>
                        </div>

                        {approvedCaps.includes('files.download') && (
                          <button
                            className="btn btn-secondary"
                            onClick={() => handleFileDownload(f)}
                            style={{ padding: '6px 10px', fontSize: '0.76rem', display: 'flex', alignItems: 'center', gap: '4px' }}
                          >
                            <Download size={13} /> Download
                          </button>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        )}
      </main>
    </div>
  );
}

class GuestPortalErrorBoundary extends React.Component<
  { children: React.ReactNode; onReset?: () => void },
  { hasError: boolean; error: Error | null }
> {
  constructor(props: any) {
    super(props);
    this.state = { hasError: false, error: null };
  }
  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }
  componentDidCatch(error: Error, errorInfo: any) {
    console.error('GuestPortal Error caught by boundary:', error, errorInfo);
  }
  render() {
    if (this.state.hasError) {
      return (
        <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px', background: 'var(--bg-main)', color: 'var(--text-main)' }}>
          <div className="glass-panel" style={{ maxWidth: '440px', width: '100%', padding: '36px', textAlign: 'center' }}>
            <div style={{ width: '60px', height: '60px', margin: '0 auto 16px', borderRadius: '50%', background: 'rgba(239,68,68,0.15)', display: 'flex', alignItems: 'center', justifyContent: 'center', border: '1px solid rgba(239,68,68,0.3)' }}>
              <Shield size={32} color="var(--accent-ruby)" />
            </div>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 800, marginBottom: '8px' }}>Portal Reconnecting</h3>
            <p style={{ fontSize: '0.84rem', color: 'var(--text-muted)', marginBottom: '20px', lineHeight: 1.5 }}>
              A display render recovery occurred. Click below to reconnect to the session stream.
            </p>
            <div style={{ display: 'flex', gap: '10px', justifyContent: 'center' }}>
              <button
                className="btn btn-primary"
                onClick={() => {
                  this.setState({ hasError: false, error: null });
                  window.location.reload();
                }}
              >
                Reload Stream
              </button>
              {this.props.onReset && (
                <button
                  className="btn btn-secondary"
                  onClick={() => {
                    sessionStorage.removeItem('rf_guest_token');
                    this.props.onReset?.();
                  }}
                >
                  New Session
                </button>
              )}
            </div>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}

export function GuestPortalView(props: GuestPortalViewProps) {
  return (
    <GuestPortalErrorBoundary onReset={props.onExitGuestMode}>
      <GuestPortalInner {...props} />
    </GuestPortalErrorBoundary>
  );
}
