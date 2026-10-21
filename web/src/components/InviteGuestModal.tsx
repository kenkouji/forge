import { useState, useEffect } from 'react';
import {
  X,
  QrCode,
  Copy,
  Check,
  Clock,
  Trash2,
  AlertTriangle,
  Monitor,
  MousePointer2,
  Upload,
  Download
} from 'lucide-react';
import { api, type GuestInvitation } from '../api';
import { QRCodeView } from './QRCodeView';

interface InviteGuestModalProps {
  deviceId: string;
  deviceName: string;
  isOpen: boolean;
  onClose: () => void;
}

export function InviteGuestModal({
  deviceId,
  deviceName,
  isOpen,
  onClose
}: InviteGuestModalProps) {
  const [capabilities, setCapabilities] = useState<string[]>([
    'screen.view',
    'input.control',
    'files.upload',
    'files.download'
  ]);
  const [durationMinutes, setDurationMinutes] = useState<number>(15);
  const [createdInvitation, setCreatedInvitation] = useState<GuestInvitation | null>(null);
  const [existingInvitations, setExistingInvitations] = useState<GuestInvitation[]>([]);
  
  const [isLoading, setIsLoading] = useState(false);
  const [copied, setCopied] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen && deviceId) {
      loadExistingInvitations();
      setCreatedInvitation(null);
      setErrorMessage(null);
    }
  }, [isOpen, deviceId]);

  const loadExistingInvitations = async () => {
    try {
      const list = await api.listGuestInvitations(deviceId);
      setExistingInvitations(list);
    } catch (err: any) {
      console.error('Failed to load guest invitations', err);
    }
  };

  const handleToggleCapability = (cap: string) => {
    if (cap === 'screen.view') return; // Mandatory
    setCapabilities((prev) =>
      prev.includes(cap) ? prev.filter((c) => c !== cap) : [...prev, cap]
    );
  };

  const handleCreate = async () => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const res = await api.createGuestInvitation(deviceId, capabilities, durationMinutes);
      setCreatedInvitation(res);
      await loadExistingInvitations();
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to generate invitation.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleRevoke = async (invId: string) => {
    try {
      await api.revokeGuestInvitation(invId);
      if (createdInvitation?.id === invId) {
        setCreatedInvitation(null);
      }
      await loadExistingInvitations();
    } catch (err: any) {
      alert(`Revocation error: ${err.message}`);
    }
  };

  const handleCopyLink = () => {
    if (!createdInvitation?.qr_payload) return;
    navigator.clipboard.writeText(createdInvitation.qr_payload);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay" onClick={onClose} style={{ zIndex: 1100 }}>
      <div
        className="modal-content glass-panel"
        onClick={(e) => e.stopPropagation()}
        style={{ maxWidth: '580px', width: '100%', maxHeight: '90vh', overflowY: 'auto' }}
      >
        <div className="modal-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div style={{ padding: '8px', background: 'rgba(97, 54, 166, 0.25)', borderRadius: '10px' }}>
              <QrCode size={20} color="var(--accent-lavender)" />
            </div>
            <div>
              <h4 style={{ fontWeight: 800, fontSize: '1.15rem' }}>Invite Guest to Workstation</h4>
              <span style={{ fontSize: '0.78rem', color: 'var(--text-dim)' }}>
                {deviceName} ({deviceId})
              </span>
            </div>
          </div>
          <button className="btn btn-secondary" onClick={onClose} style={{ padding: '6px' }}>
            <X size={18} />
          </button>
        </div>

        {errorMessage && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '10px 14px', background: 'rgba(239, 68, 68, 0.12)', border: '1px solid rgba(239, 68, 68, 0.3)', borderRadius: '10px', color: 'var(--accent-ruby)', fontSize: '0.82rem', marginBottom: '16px' }}>
            <AlertTriangle size={16} />
            <span>{errorMessage}</span>
          </div>
        )}

        {/* QR Code Presentation if created */}
        {createdInvitation ? (
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center', gap: '16px', padding: '10px 0' }}>
            <div style={{ padding: '12px', background: '#FFF', borderRadius: '18px', boxShadow: '0 12px 32px rgba(97, 54, 166, 0.3)' }}>
              <QRCodeView text={createdInvitation.qr_payload} size={220} />
            </div>

            <div style={{ maxWidth: '440px' }}>
              <h5 style={{ fontWeight: 700, fontSize: '1rem', marginBottom: '4px' }}>
                Scan with Phone Camera or Send Link
              </h5>
              <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)', lineHeight: 1.5 }}>
                The guest opens this in any modern browser without installing RemoteForge. Single-use token expires in {durationMinutes} minutes.
              </p>
              <div style={{ marginTop: '6px', padding: '6px 12px', background: 'rgba(168, 85, 247, 0.12)', border: '1px solid rgba(168, 85, 247, 0.25)', borderRadius: '8px', color: 'var(--accent-lavender)', fontSize: '0.78rem', fontWeight: 600 }}>
                ⚡ Hotspot / Wi-Fi Access: Guests must connect to your Wi-Fi or Hotspot to access this workstation.
              </div>
            </div>

            <div style={{ display: 'flex', width: '100%', gap: '8px' }}>
              <input
                type="text"
                readOnly
                value={createdInvitation.qr_payload}
                style={{ flex: 1, padding: '10px 12px', borderRadius: '10px', background: 'var(--bg-card)', border: '1px solid var(--border-subtle)', color: 'var(--text-main)', fontSize: '0.76rem', fontFamily: 'var(--font-mono)' }}
              />
              <button className="btn btn-primary" onClick={handleCopyLink} style={{ display: 'flex', alignItems: 'center', gap: '6px', whiteSpace: 'nowrap' }}>
                {copied ? <Check size={14} /> : <Copy size={14} />}
                {copied ? 'Copied!' : 'Copy Link'}
              </button>
            </div>

            <div style={{ background: 'rgba(97, 54, 166, 0.08)', borderRadius: '10px', padding: '10px 14px', border: '1px solid rgba(189, 160, 255, 0.15)', fontSize: '0.76rem', color: 'var(--text-dim)', textAlign: 'left', width: '100%' }}>
              🔒 <strong>Fragment Privacy Protected:</strong> The invitation secret is stored in the URL fragment (<code>#secret=...</code>) and is never transmitted in HTTP headers or access logs.
            </div>

            <button
              className="btn btn-secondary"
              onClick={() => setCreatedInvitation(null)}
              style={{ marginTop: '8px', fontSize: '0.85rem' }}
            >
              Generate Another Invitation
            </button>
          </div>
        ) : (
          /* Invitation Configuration Form */
          <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 700, marginBottom: '8px', color: 'var(--text-main)' }}>
                Permitted Capabilities
              </label>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '10px' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '10px 14px', background: 'var(--bg-card)', borderRadius: '10px', border: '1px solid var(--border-subtle)', cursor: 'not-allowed', opacity: 0.9 }}>
                  <input type="checkbox" checked disabled />
                  <div>
                    <div style={{ fontSize: '0.85rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <Monitor size={14} color="var(--accent-lavender)" /> Screen View
                    </div>
                    <span style={{ fontSize: '0.72rem', color: 'var(--text-dim)' }}>Mandatory for guest streaming</span>
                  </div>
                </label>

                <label
                  onClick={() => handleToggleCapability('input.control')}
                  style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '10px 14px', background: 'var(--bg-card)', borderRadius: '10px', border: `1px solid ${capabilities.includes('input.control') ? 'var(--accent-lavender)' : 'var(--border-subtle)'}`, cursor: 'pointer' }}
                >
                  <input type="checkbox" checked={capabilities.includes('input.control')} onChange={() => {}} />
                  <div>
                    <div style={{ fontSize: '0.85rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <MousePointer2 size={14} color="var(--accent-emerald)" /> Co-Control Input
                    </div>
                    <span style={{ fontSize: '0.72rem', color: 'var(--text-dim)' }}>Allow mouse clicks & keystrokes</span>
                  </div>
                </label>

                <label
                  onClick={() => handleToggleCapability('files.upload')}
                  style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '10px 14px', background: 'var(--bg-card)', borderRadius: '10px', border: `1px solid ${capabilities.includes('files.upload') ? 'var(--accent-lavender)' : 'var(--border-subtle)'}`, cursor: 'pointer' }}
                >
                  <input type="checkbox" checked={capabilities.includes('files.upload')} onChange={() => {}} />
                  <div>
                    <div style={{ fontSize: '0.85rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <Upload size={14} color="var(--accent-royal)" /> Drop Zone Upload
                    </div>
                    <span style={{ fontSize: '0.72rem', color: 'var(--text-dim)' }}>Upload to quarantine staging</span>
                  </div>
                </label>

                <label
                  onClick={() => handleToggleCapability('files.download')}
                  style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '10px 14px', background: 'var(--bg-card)', borderRadius: '10px', border: `1px solid ${capabilities.includes('files.download') ? 'var(--accent-lavender)' : 'var(--border-subtle)'}`, cursor: 'pointer' }}
                >
                  <input type="checkbox" checked={capabilities.includes('files.download')} onChange={() => {}} />
                  <div>
                    <div style={{ fontSize: '0.85rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <Download size={14} color="var(--accent-lavender)" /> Drop Zone Download
                    </div>
                    <span style={{ fontSize: '0.72rem', color: 'var(--text-dim)' }}>Download from quarantine staging</span>
                  </div>
                </label>
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 700, marginBottom: '8px', color: 'var(--text-main)' }}>
                Invitation Lifetime
              </label>
              <div style={{ display: 'flex', gap: '10px' }}>
                {[15, 30, 60].map((mins) => (
                  <button
                    key={mins}
                    type="button"
                    onClick={() => setDurationMinutes(mins)}
                    className={`btn ${durationMinutes === mins ? 'btn-primary' : 'btn-secondary'}`}
                    style={{ flex: 1, padding: '10px', fontSize: '0.85rem' }}
                  >
                    <Clock size={14} style={{ marginRight: '6px' }} /> {mins} Minutes
                  </button>
                ))}
              </div>
            </div>

            <button
              className="btn btn-primary"
              disabled={isLoading}
              onClick={handleCreate}
              style={{ padding: '14px', fontSize: '0.95rem', fontWeight: 700, marginTop: '8px' }}
            >
              {isLoading ? 'Generating Invitation...' : 'Create QR Code Invitation'}
            </button>
          </div>
        )}

        {/* Existing Active Invitations Table */}
        {existingInvitations.length > 0 && (
          <div style={{ marginTop: '28px', borderTop: '1px solid var(--border-subtle)', paddingTop: '18px' }}>
            <h5 style={{ fontSize: '0.85rem', fontWeight: 700, marginBottom: '10px', color: 'var(--text-muted)' }}>
              Active Invitations ({existingInvitations.length})
            </h5>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              {existingInvitations.map((inv) => (
                <div
                  key={inv.id}
                  style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 14px', background: 'var(--bg-card)', borderRadius: '10px', border: '1px solid var(--border-subtle)', fontSize: '0.8rem' }}
                >
                  <div>
                    <span style={{ fontFamily: 'var(--font-mono)', fontWeight: 600 }}>{inv.id}</span>
                    <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)', marginTop: '2px' }}>
                      Expires: {new Date(inv.expires_at).toLocaleTimeString()} • {inv.capabilities.join(', ')}
                    </div>
                  </div>
                  <button
                    className="btn btn-danger"
                    onClick={() => handleRevoke(inv.id)}
                    style={{ padding: '6px 10px', fontSize: '0.74rem', display: 'flex', alignItems: 'center', gap: '4px' }}
                  >
                    <Trash2 size={12} /> Revoke
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
