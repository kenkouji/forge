import { useState, useEffect } from 'react';
import {
  Users,
  UserCheck,
  UserX,
  Check,
  X
} from 'lucide-react';
import { api, type GuestSession } from '../api';

interface GuestSessionManagerProps {
  deviceId: string;
  onSessionsChange?: () => void;
}

export function GuestSessionManager({
  deviceId,
  onSessionsChange
}: GuestSessionManagerProps) {
  const [sessions, setSessions] = useState<GuestSession[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [selectedModes, setSelectedModes] = useState<Record<string, string>>({});

  useEffect(() => {
    if (deviceId) {
      loadSessions();
      const interval = setInterval(loadSessions, 3000);
      return () => clearInterval(interval);
    }
  }, [deviceId]);

  const loadSessions = async () => {
    try {
      const list = await api.listGuestSessions(deviceId);
      setSessions(list);
    } catch (err: any) {
      console.error('Failed to load guest sessions', err);
    }
  };

  const handleApprove = async (s: GuestSession) => {
    setIsLoading(true);
    try {
      const mode = selectedModes[s.id] || 'shared-control';
      await api.approveGuestSession(s.id, s.approved_capabilities.length ? s.approved_capabilities : ['screen.view', 'input.control', 'files.upload', 'files.download'], mode, 30);
      await loadSessions();
      if (onSessionsChange) onSessionsChange();
    } catch (err: any) {
      alert(`Approval error: ${err.message}`);
    } finally {
      setIsLoading(false);
    }
  };

  const handleRevoke = async (sessionId: string) => {
    setIsLoading(true);
    try {
      await api.revokeGuestSession(sessionId, 'Revoked by owner');
      await loadSessions();
      if (onSessionsChange) onSessionsChange();
    } catch (err: any) {
      alert(`Revocation error: ${err.message}`);
    } finally {
      setIsLoading(false);
    }
  };

  const pendingSessions = sessions.filter((s) => s.status === 'PENDING_APPROVAL');
  const activeSessions = sessions.filter((s) => s.status === 'APPROVED');

  if (sessions.length === 0) return null;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
      {/* PENDING APPROVAL REQUESTS BANNER */}
      {pendingSessions.length > 0 && (
        <div
          className="glass-panel"
          style={{
            borderLeft: '4px solid var(--accent-lavender)',
            background: 'rgba(97, 54, 166, 0.15)',
            padding: '16px 20px'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '12px' }}>
            <UserCheck size={20} color="var(--accent-lavender)" />
            <h5 style={{ fontWeight: 800, fontSize: '0.95rem' }}>
              Pending Guest Access Requests ({pendingSessions.length})
            </h5>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {pendingSessions.map((s) => (
              <div
                key={s.id}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  flexWrap: 'wrap',
                  gap: '12px',
                  padding: '12px 16px',
                  background: 'var(--bg-card)',
                  borderRadius: '12px',
                  border: '1px solid var(--border-subtle)'
                }}
              >
                <div>
                  <div style={{ fontWeight: 700, fontSize: '0.92rem' }}>
                    {s.guest_name}
                  </div>
                  <div style={{ fontSize: '0.74rem', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)', marginTop: '2px' }}>
                    Session: {s.id} • Requested: {new Date(s.created_at || Date.now()).toLocaleTimeString()}
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <select
                    value={selectedModes[s.id] || 'shared-control'}
                    onChange={(e) => setSelectedModes({ ...selectedModes, [s.id]: e.target.value })}
                    style={{
                      padding: '8px 12px',
                      borderRadius: '8px',
                      background: 'var(--bg-card)',
                      border: '1px solid var(--border-subtle)',
                      color: 'var(--text-main)',
                      fontSize: '0.8rem',
                      outline: 'none'
                    }}
                  >
                    <option value="shared-control">Co-Control (Input + View)</option>
                    <option value="view-only">View-Only (No Input)</option>
                    <option value="presenter">Presenter Mode</option>
                  </select>

                  <button
                    className="btn btn-primary"
                    disabled={isLoading}
                    onClick={() => handleApprove(s)}
                    style={{ padding: '8px 14px', fontSize: '0.8rem', display: 'flex', alignItems: 'center', gap: '6px' }}
                  >
                    <Check size={14} /> Approve
                  </button>

                  <button
                    className="btn btn-danger"
                    disabled={isLoading}
                    onClick={() => handleRevoke(s.id)}
                    style={{ padding: '8px 12px', fontSize: '0.8rem', display: 'flex', alignItems: 'center', gap: '6px' }}
                  >
                    <X size={14} /> Reject
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ACTIVE CONNECTED GUESTS PANEL */}
      {activeSessions.length > 0 && (
        <div className="glass-panel" style={{ padding: '16px 20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Users size={18} color="var(--accent-lavender)" />
              <h5 style={{ fontWeight: 700, fontSize: '0.92rem' }}>
                Active Guest Participants ({activeSessions.length})
              </h5>
            </div>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            {activeSessions.map((s) => (
              <div
                key={s.id}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '12px 16px',
                  background: 'var(--bg-card)',
                  borderRadius: '10px',
                  border: '1px solid var(--border-subtle)'
                }}
              >
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span style={{ fontWeight: 700, fontSize: '0.9rem' }}>{s.guest_name}</span>
                    <span className="badge" style={{ background: 'rgba(189,160,255,0.2)', color: 'var(--accent-lavender)', fontSize: '0.68rem' }}>
                      {s.control_mode.toUpperCase()}
                    </span>
                  </div>
                  <div style={{ fontSize: '0.74rem', color: 'var(--text-dim)', marginTop: '4px' }}>
                    Capabilities: {(s.approved_capabilities || (s as any).capabilities || []).join(', ')} • Expires: {new Date(s.expires_at).toLocaleTimeString()}
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <button
                    className="btn btn-danger"
                    disabled={isLoading}
                    onClick={() => handleRevoke(s.id)}
                    style={{ padding: '6px 12px', fontSize: '0.76rem', display: 'flex', alignItems: 'center', gap: '6px' }}
                    title="Disconnect this guest immediately without stopping your session"
                  >
                    <UserX size={13} /> Disconnect
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
