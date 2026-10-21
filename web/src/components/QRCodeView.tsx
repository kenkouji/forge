import { useEffect, useState } from 'react';
import QRCode from 'qrcode';

interface QRCodeViewProps {
  text: string;
  size?: number;
  darkColor?: string;
  lightColor?: string;
}

export function QRCodeView({
  text,
  size = 200,
  darkColor = '#6136A6',
  lightColor = '#FFFFFF'
}: QRCodeViewProps) {
  const [dataUrl, setDataUrl] = useState<string>('');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!text) {
      setDataUrl('');
      return;
    }

    QRCode.toDataURL(text, {
      width: size,
      margin: 2,
      color: {
        dark: darkColor,
        light: lightColor,
      },
      errorCorrectionLevel: 'M',
    })
      .then((url) => {
        setDataUrl(url);
        setError(null);
      })
      .catch((err) => {
        console.error('Failed to generate QR code', err);
        setError('Failed to generate QR Code');
      });
  }, [text, size, darkColor, lightColor]);

  if (error) {
    return (
      <div style={{ width: size, height: size, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(239, 68, 68, 0.1)', color: 'var(--accent-ruby)', borderRadius: '12px', fontSize: '0.8rem', padding: '12px', textAlign: 'center' }}>
        {error}
      </div>
    );
  }

  if (!dataUrl) {
    return (
      <div style={{ width: size, height: size, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--bg-glass-card)', borderRadius: '12px' }}>
        <span style={{ fontSize: '0.8rem', color: 'var(--text-dim)' }}>Generating QR...</span>
      </div>
    );
  }

  return (
    <div style={{ display: 'inline-flex', padding: '10px', background: lightColor, borderRadius: '16px', boxShadow: '0 8px 32px rgba(97, 54, 166, 0.25)', border: '2px solid rgba(189, 160, 255, 0.4)' }}>
      <img src={dataUrl} alt="RemoteForge Guest Access QR Code" width={size} height={size} style={{ display: 'block', borderRadius: '8px' }} />
    </div>
  );
}
