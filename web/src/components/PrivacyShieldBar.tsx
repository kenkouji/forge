import { useState } from 'react';
import {
  ShieldAlert,
  ShieldCheck,
  Pause,
  Play,
  Lock,
  Unlock,
  AlertTriangle,
  RotateCcw,
  FolderLock
} from 'lucide-react';
import { api, type PrivacyShieldStatus } from '../api';

interface PrivacyShieldBarProps {
  deviceId: string;
  status: PrivacyShieldStatus | null;
  onStatusChange: (newStatus: PrivacyShieldStatus) => void;
}

export function PrivacyShieldBar({
  deviceId,
  status,
  onStatusChange
}: PrivacyShieldBarProps) {
  const [isUpdating, setIsUpdating] = useState(false);

  if (!deviceId) return null;

  const isLockdown = !!status?.is_lockdown;
  const isScreenPaused = !!status?.is_screen_paused;
  const isInputDisabled = !!status?.is_input_disabled;
  const isTransfersStopped = !!status?.is_transfers_stopped;
  const hasPendingSync = !!status?.pending_host_sync;
  const activeGuestsCount = status?.active_guests_count || 0;

  const handleToggle = async (key: 'is_screen_paused' | 'is_input_disabled' | 'is_transfers_stopped', currentVal: boolean) => {
    setIsUpdating(true);
    try {
      const res = await api.updatePrivacyShield(deviceId, {
        [key]: !currentVal,
        action: `Toggle ${key} to ${!currentVal}`
      });
      onStatusChange(res);
    } catch (err: any) {
      alert(`Privacy Shield error: ${err.message}`);
    } finally {
      setIsUpdating(false);
    }
  };

  const handleEmergencyLockdown = async () => {
    const confirm = window.confirm(
      'EMERGENCY LOCKDOWN:\n\nThis will immediately terminate all active guest sessions, block all guest input and screen sharing, and block new guest invitations.\n\nAre you sure you want to activate Emergency Lockdown?'
    );
    if (!confirm) return;

    setIsUpdating(true);
    try {
      const res = await api.updatePrivacyShield(deviceId, {
        action: 'emergency_lockdown'
      });
      onStatusChange(res);
    } catch (err: any) {
      alert(`Emergency Lockdown error: ${err.message}`);
    } finally {
      setIsUpdating(false);
    }
  };

  const handleResumeAccess = async () => {
    setIsUpdating(true);
    try {
      const res = await api.updatePrivacyShield(deviceId, {
        action: 'resume_access'
      });
      onStatusChange(res);
    } catch (err: any) {
      alert(`Resume Access error: ${err.message}`);
    } finally {
      setIsUpdating(false);
    }
  };

  return (
    <div
      className={`glass-panel ${isLockdown ? 'lockdown-active' : ''}`}
      style={{
        padding: '14px 20px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: '12px',
        borderLeft: isLockdown ? '4px solid var(--accent-ruby)' : '4px solid var(--accent-lavender)',
        background: isLockdown ? 'rgba(239, 68, 68, 0.12)' : 'var(--bg-glass-card)',
        transition: 'all 0.3s cubic-bezier(0.16, 1, 0.3, 1)'
      }}
    >
      {/* Left Shield Status Badge */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
        <div
          style={{
            padding: '8px',
            borderRadius: '12px',
            background: isLockdown ? 'rgba(239, 68, 68, 0.25)' : 'rgba(97, 54, 166, 0.25)',
            border: `1px solid ${isLockdown ? 'rgba(239, 68, 68, 0.5)' : 'rgba(189, 160, 255, 0.4)'}`
          }}
        >
          {isLockdown ? (
            <ShieldAlert size={22} color="var(--accent-ruby)" />
          ) : (
            <ShieldCheck size={22} color="var(--accent-lavender)" />
          )}
        </div>

        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontWeight: 800, fontSize: '0.92rem', letterSpacing: '-0.01em' }}>
              PRIVACY SHIELD
            </span>
            {isLockdown ? (
              <span className="badge" style={{ background: 'rgba(239, 68, 68, 0.25)', color: 'var(--accent-ruby)', border: '1px solid var(--accent-ruby)', fontSize: '0.7rem', fontWeight: 800 }}>
                EMERGENCY LOCKDOWN ACTIVE
              </span>
            ) : (
              <span className="badge brand-badge" style={{ fontSize: '0.7rem' }}>
                PROTECTION ARMED
              </span>
            )}
            {activeGuestsCount > 0 && (
              <span className="badge" style={{ background: 'rgba(189, 160, 255, 0.2)', color: 'var(--accent-lavender)', fontSize: '0.7rem' }}>
                {activeGuestsCount} {activeGuestsCount === 1 ? 'Guest Connected' : 'Guests Connected'}
              </span>
            )}
          </div>

          <div style={{ fontSize: '0.76rem', color: 'var(--text-dim)', marginTop: '2px', display: 'flex', alignItems: 'center', gap: '6px' }}>
            {hasPendingSync ? (
              <span style={{ color: '#F59E0B', display: 'flex', alignItems: 'center', gap: '4px', fontWeight: 600 }}>
                <AlertTriangle size={12} /> Pending host agent sync (PC will enforce upon reconnect)
              </span>
            ) : (
              <span>Host PC agent verified & synchronized</span>
            )}
          </div>
        </div>
      </div>

      {/* Control Actions */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
        {/* Pause Screen Sharing */}
        <button
          className={`btn ${isScreenPaused ? 'btn-danger' : 'btn-secondary'}`}
          disabled={isUpdating || isLockdown}
          onClick={() => handleToggle('is_screen_paused', isScreenPaused)}
          style={{ padding: '8px 12px', fontSize: '0.78rem', display: 'flex', alignItems: 'center', gap: '6px' }}
          title="Prevent guest browsers from receiving live screen frames"
        >
          {isScreenPaused ? <Play size={13} /> : <Pause size={13} />}
          {isScreenPaused ? 'Resume Screen' : 'Pause Screen'}
        </button>

        {/* Disable Guest Input */}
        <button
          className={`btn ${isInputDisabled ? 'btn-danger' : 'btn-secondary'}`}
          disabled={isUpdating || isLockdown}
          onClick={() => handleToggle('is_input_disabled', isInputDisabled)}
          style={{ padding: '8px 12px', fontSize: '0.78rem', display: 'flex', alignItems: 'center', gap: '6px' }}
          title="Block guest mouse clicks and keyboard strokes"
        >
          {isInputDisabled ? <Unlock size={13} /> : <Lock size={13} />}
          {isInputDisabled ? 'Enable Guest Input' : 'Block Guest Input'}
        </button>

        {/* Stop Transfers */}
        <button
          className={`btn ${isTransfersStopped ? 'btn-danger' : 'btn-secondary'}`}
          disabled={isUpdating || isLockdown}
          onClick={() => handleToggle('is_transfers_stopped', isTransfersStopped)}
          style={{ padding: '8px 12px', fontSize: '0.78rem', display: 'flex', alignItems: 'center', gap: '6px' }}
          title="Suspend guest file uploads and downloads"
        >
          <FolderLock size={13} />
          {isTransfersStopped ? 'Resume Transfers' : 'Stop Transfers'}
        </button>

        {/* Emergency Lockdown / Resume Access */}
        {isLockdown ? (
          <button
            className="btn btn-primary"
            disabled={isUpdating}
            onClick={handleResumeAccess}
            style={{ padding: '8px 14px', fontSize: '0.78rem', display: 'flex', alignItems: 'center', gap: '6px', background: 'var(--accent-royal)' }}
          >
            <RotateCcw size={13} /> Resume Access
          </button>
        ) : (
          <button
            className="btn btn-danger"
            disabled={isUpdating}
            onClick={handleEmergencyLockdown}
            style={{ padding: '8px 14px', fontSize: '0.78rem', display: 'flex', alignItems: 'center', gap: '6px', fontWeight: 700 }}
          >
            <ShieldAlert size={14} /> Emergency Lockdown
          </button>
        )}
      </div>
    </div>
  );
}
