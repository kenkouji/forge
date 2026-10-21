import { useState, useEffect } from 'react';
import {
  FolderLock,
  Download,
  Trash2,
  RefreshCw,
  FolderCheck,
  X
} from 'lucide-react';
import { api, type StagedFile } from '../api';

interface GuestDropZoneQueueProps {
  deviceId: string;
}

export function GuestDropZoneQueue({ deviceId }: GuestDropZoneQueueProps) {
  const [files, setFiles] = useState<StagedFile[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [promotingFile, setPromotingFile] = useState<StagedFile | null>(null);
  const [destinationDir, setDestinationDir] = useState<string>('/Users/g.aasish/Desktop');

  useEffect(() => {
    if (deviceId) {
      loadStagedFiles();
    }
  }, [deviceId]);

  const loadStagedFiles = async () => {
    setIsLoading(true);
    try {
      const list = await api.listStagedFiles(deviceId);
      setFiles(list);
    } catch (err: any) {
      console.error('Failed to load staged files', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handlePromote = async (targetPath?: string) => {
    const fileToPromote = promotingFile;
    const dest = (targetPath || destinationDir).trim();
    if (!fileToPromote || !dest) return;
    setIsLoading(true);
    try {
      await api.promoteStagedFile(fileToPromote.id, dest);
      alert(`File "${fileToPromote.filename}" delivered successfully to ${dest}!`);
      setPromotingFile(null);
      await loadStagedFiles();
    } catch (err: any) {
      alert(`Promotion error: ${err.message}`);
    } finally {
      setIsLoading(false);
    }
  };

  const handleQuickDeliverToDesktop = async (file: StagedFile) => {
    setIsLoading(true);
    try {
      await api.promoteStagedFile(file.id, '/Users/g.aasish/Desktop');
      alert(`File "${file.filename}" delivered to Desktop!`);
      await loadStagedFiles();
    } catch (err: any) {
      alert(`Delivery error: ${err.message}`);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (fileId: string) => {
    const confirm = window.confirm('Are you sure you want to delete this file?');
    if (!confirm) return;
    try {
      await api.deleteStagedFile(fileId);
      await loadStagedFiles();
    } catch (err: any) {
      alert(`Delete error: ${err.message}`);
    }
  };

  const handleDownload = async (file: StagedFile) => {
    try {
      await api.downloadStagedFile(file.id, file.filename);
    } catch (err: any) {
      alert(`Download error: ${err.message}`);
    }
  };

  return (
    <div className="glass-panel" style={{ padding: '20px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px', flexWrap: 'wrap', gap: '10px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div style={{ padding: '8px', background: 'rgba(97, 54, 166, 0.25)', borderRadius: '10px' }}>
            <FolderLock size={20} color="var(--accent-lavender)" />
          </div>
          <div>
            <h4 style={{ fontWeight: 800, fontSize: '1.05rem' }}>Connected Device File Transfers & Drop Zone</h4>
            <span style={{ fontSize: '0.78rem', color: 'var(--text-dim)' }}>
              Files delivered directly to workstation Desktop or pending review
            </span>
          </div>
        </div>

        <button className="btn btn-secondary" onClick={loadStagedFiles} disabled={isLoading} style={{ padding: '6px 12px', fontSize: '0.78rem', display: 'flex', alignItems: 'center', gap: '6px' }}>
          <RefreshCw size={12} className={isLoading ? 'spin-slow' : ''} /> Refresh
        </button>
      </div>

      {files.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '36px', color: 'var(--text-dim)', fontSize: '0.88rem' }}>
          No files currently transferred or in staging.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          {files.map((f) => {
            const isDelivered = f.status.toUpperCase() === 'DELIVERED' || f.status.toUpperCase() === 'PROMOTED';
            return (
              <div
                key={f.id}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '12px 16px',
                  background: 'var(--bg-card)',
                  borderRadius: '10px',
                  border: isDelivered ? '1px solid rgba(16, 185, 129, 0.3)' : '1px solid var(--border-subtle)',
                  flexWrap: 'wrap',
                  gap: '10px'
                }}
              >
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span style={{ fontWeight: 700, fontSize: '0.92rem' }}>{f.filename}</span>
                    <span
                      className={`badge ${isDelivered ? 'online' : 'brand-badge'}`}
                      style={{
                        fontSize: '0.68rem',
                        background: isDelivered ? 'rgba(16, 185, 129, 0.2)' : undefined,
                        color: isDelivered ? 'var(--accent-emerald)' : undefined
                      }}
                    >
                      {isDelivered ? 'DELIVERED TO DESKTOP' : f.status.toUpperCase()}
                    </span>
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)', marginTop: '4px' }}>
                    {(f.file_size / 1024).toFixed(1)} KB • Session: {f.session_id.substring(0, 8)} • SHA-256: {f.sha256_hash.substring(0, 16)}...
                  </div>
                  {f.promoted_to_path && (
                    <div style={{ fontSize: '0.74rem', color: 'var(--accent-emerald)', marginTop: '2px', fontWeight: 600 }}>
                      ✓ Desktop Path: {f.promoted_to_path}
                    </div>
                  )}
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <button
                    className="btn btn-secondary"
                    onClick={() => handleDownload(f)}
                    style={{ padding: '6px 12px', fontSize: '0.76rem', display: 'flex', alignItems: 'center', gap: '6px' }}
                  >
                    <Download size={13} /> Download
                  </button>

                  {!isDelivered && (
                    <button
                      className="btn btn-primary"
                      onClick={() => handleQuickDeliverToDesktop(f)}
                      style={{ padding: '6px 12px', fontSize: '0.76rem', display: 'flex', alignItems: 'center', gap: '6px', background: 'var(--accent-emerald)' }}
                    >
                      <FolderCheck size={13} /> Deliver to Desktop
                    </button>
                  )}

                  {!isDelivered && (
                    <button
                      className="btn btn-secondary"
                      onClick={() => setPromotingFile(f)}
                      style={{ padding: '6px 12px', fontSize: '0.76rem', display: 'flex', alignItems: 'center', gap: '6px' }}
                    >
                      <FolderCheck size={13} /> Move to Folder
                    </button>
                  )}

                  <button
                    className="btn btn-danger"
                    onClick={() => handleDelete(f.id)}
                    style={{ padding: '6px 10px', fontSize: '0.76rem' }}
                  >
                    <Trash2 size={13} />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Promote Modal */}
      {promotingFile && (
        <div className="modal-overlay" onClick={() => setPromotingFile(null)} style={{ zIndex: 1200 }}>
          <div
            className="modal-content glass-panel"
            onClick={(e) => e.stopPropagation()}
            style={{ maxWidth: '480px', width: '100%', padding: '24px' }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
              <h4 style={{ fontWeight: 800, fontSize: '1.1rem' }}>Promote File to Workstation</h4>
              <button className="btn btn-secondary" onClick={() => setPromotingFile(null)} style={{ padding: '4px' }}>
                <X size={16} />
              </button>
            </div>

            <p style={{ fontSize: '0.84rem', color: 'var(--text-muted)', marginBottom: '16px' }}>
              Promoting <strong>{promotingFile.filename}</strong> out of quarantine into an allowed directory on the host PC.
            </p>

            <div style={{ marginBottom: '20px' }}>
              <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '6px' }}>
                Target Directory (Must be in allowed allowlist)
              </label>
              <input
                type="text"
                value={destinationDir}
                onChange={(e) => setDestinationDir(e.target.value)}
                placeholder="/path/to/allowed/directory"
                style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', background: 'var(--bg-card)', border: '1px solid var(--border-subtle)', color: 'var(--text-main)', fontSize: '0.85rem', outline: 'none' }}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
              <button className="btn btn-secondary" onClick={() => setPromotingFile(null)}>
                Cancel
              </button>
              <button className="btn btn-primary" onClick={() => handlePromote()} disabled={isLoading}>
                {isLoading ? 'Promoting...' : 'Confirm Promotion'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
