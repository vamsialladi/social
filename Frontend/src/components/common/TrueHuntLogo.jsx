import React from 'react';
import { Link } from 'react-router-dom';
import logoImg from '../../assets/truehunt-logo.png';
import emblemImg from '../../assets/truehunt-emblem.png';

/**
 * TrueHunt Official Brand Logo Component
 * Uses the official TrueHunt brand logo provided:
 * - Rounded emblem with TH monogram, curling cinema film strip, and brown arrow
 * - Distinctive "TrueHunt" typography wordmark
 */
export default function TrueHuntLogo({ 
  size = 'md', 
  variant = 'light', // 'light' (on white/light pages) or 'dark' (on dark navy footer/banner)
  to = '/',
  showWordmark = true,
  className = '' 
}) {
  // Height configurations for different contexts
  const heights = {
    sm: { height: 32, emblemHeight: 32 },
    md: { height: 42, emblemHeight: 42 },
    lg: { height: 52, emblemHeight: 52 },
    xl: { height: 64, emblemHeight: 64 }
  };

  const dim = heights[size] || heights.md;
  const isDark = variant === 'dark';

  const logoElement = showWordmark ? (
    <img 
      src={logoImg} 
      alt="TrueHunt - Film & Entertainment Professional Network" 
      style={{
        height: `${dim.height}px`,
        width: 'auto',
        objectFit: 'contain',
        display: 'block',
        // If on dark background, give it a neat white backdrop pill so the navy text and emblem pop cleanly
        backgroundColor: isDark ? '#FFFFFF' : 'transparent',
        padding: isDark ? '4px 10px' : '0',
        borderRadius: isDark ? '8px' : '0',
        boxShadow: isDark ? '0 2px 8px rgba(0, 0, 0, 0.2)' : 'none'
      }}
    />
  ) : (
    <img 
      src={emblemImg} 
      alt="TrueHunt Emblem" 
      style={{
        height: `${dim.emblemHeight}px`,
        width: `${dim.emblemHeight}px`,
        objectFit: 'contain',
        display: 'block',
        backgroundColor: isDark ? '#FFFFFF' : 'transparent',
        padding: isDark ? '2px' : '0',
        borderRadius: '8px'
      }}
    />
  );

  const containerStyle = {
    display: 'inline-flex',
    alignItems: 'center',
    textDecoration: 'none'
  };

  if (to) {
    return (
      <Link to={to} className={`truehunt-brand-link ${className}`} style={containerStyle}>
        {logoElement}
      </Link>
    );
  }

  return (
    <div className={`truehunt-brand-container ${className}`} style={containerStyle}>
      {logoElement}
    </div>
  );
}
