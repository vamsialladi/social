import React, { useMemo } from 'react';

/**
 * ============================================================================
 * FilmStrip3D – 3D Curved Golden Cinematic Film Strip Revolving Around Globe
 * ============================================================================
 * Features:
 * - 3D Elliptical geometry revolving around the central Earth globe
 * - Golden metallic ribbon body with continuous film sprocket perforations
 * - Distinct Front Arc (in front of globe) & Back Arc (behind globe)
 * - 24 film frame cards with realistic perspective scaling, depth & rotation
 * - Interactive click on any orbiting frame to focus that craft
 */
export default function FilmStrip3D({
  crafts = [],
  currentIndex = 0,
  rotationOffset = 0,
  onSelectCraft,
  isPaused = false,
  width = 960,
  height = 540
}) {
  const N = crafts.length || 24;
  const CX = width / 2;
  const CY = height / 2 + 10;
  const RX = Math.min(width * 0.44, 420);
  const RY = Math.min(height * 0.26, 140);
  const TILT = -0.16; // -9 degrees downward inclination angle for 3D perspective

  // Generates SVG elliptical arcs for the golden film band
  const createArc = (rx, ry, strokeWidth, strokeColor, dashArray, sweepFlag) => {
    return (
      <path
        d={`M ${-rx} 0 A ${rx} ${ry} 0 0 ${sweepFlag} ${rx} 0`}
        fill="none"
        stroke={strokeColor}
        strokeWidth={strokeWidth}
        strokeDasharray={dashArray || undefined}
        strokeLinecap="round"
      />
    );
  };

  // Calculates 3D perspective positions for all 24 frames
  const framesData = useMemo(() => {
    return crafts.map((craft, i) => {
      // Angle for frame i with continuous rotation offset
      const t = (i / N) * 2 * Math.PI + rotationOffset;
      const sn = Math.sin(t);
      const cs = Math.cos(t);

      // 2D unrotated ellipse coordinates
      const x0 = RX * cs;
      const y0 = RY * sn;

      // Apply 3D perspective tilt
      const cosT = Math.cos(TILT);
      const sinT = Math.sin(TILT);
      const x = CX + x0 * cosT - y0 * sinT;
      const y = CY + x0 * sinT + y0 * cosT;

      // Tangent angle along the curved ribbon
      const tx = -RX * sn;
      const ty = RY * cs;
      const tx2 = tx * cosT - ty * sinT;
      const ty2 = tx * sinT + ty * cosT;
      let rotAngle = Math.atan2(ty2, tx2);
      if (Math.abs(rotAngle) > Math.PI / 2) {
        rotAngle += rotAngle > 0 ? -Math.PI : Math.PI;
      }

      // Depth perception: sn > 0 is front, sn < 0 is back
      const isFront = sn > -0.05;
      const depthScale = 0.62 + 0.38 * ((sn + 1) / 2);
      
      // Calculate angular distance to the front center
      const angleDiff = Math.abs(((t - Math.PI / 2) % (2 * Math.PI) + 3 * Math.PI) % (2 * Math.PI) - Math.PI);
      const isFrontThree = angleDiff < 0.65;
      const isCenter = angleDiff < 0.22;

      return {
        craft,
        index: i,
        x,
        y,
        rotAngle: rotAngle * 0.45,
        scale: depthScale * (isCenter ? 1.35 : isFrontThree ? 1.15 : 0.85),
        zIndex: isFront ? 30 + Math.round(sn * 20) + (isCenter ? 15 : 0) : 2 + Math.round((sn + 1) * 3),
        opacity: isFront ? 1 : 0.55,
        isFront,
        isCenter,
        isFrontThree
      };
    });
  }, [crafts, currentIndex, rotationOffset, CX, CY, RX, RY, N]);

  return (
    <div
      className="filmstrip-3d-container"
      style={{
        position: 'absolute',
        inset: 0,
        pointerEvents: 'none',
        overflow: 'visible'
      }}
    >
      {/* 1. Golden Film Strip Back Arc (Renders BEHIND the 3D Globe) */}
      <svg
        className="filmstrip-svg-back"
        viewBox={`0 0 ${width} ${height}`}
        style={{
          position: 'absolute',
          left: 0,
          top: 0,
          width: '100%',
          height: '100%',
          zIndex: 4,
          pointerEvents: 'none'
        }}
      >
        <defs>
          <linearGradient id="goldBackGrad" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stopColor="#785012" stopOpacity="0.4" />
            <stop offset="50%" stopColor="#B47F28" stopOpacity="0.7" />
            <stop offset="100%" stopColor="#785012" stopOpacity="0.4" />
          </linearGradient>
        </defs>
        <g transform={`translate(${CX} ${CY}) rotate(${(TILT * 180) / Math.PI})`}>
          {/* Back Solid Golden Ribbon */}
          {createArc(RX, RY, 64, 'url(#goldBackGrad)', '', 1)}
          {/* Top Perforations */}
          {createArc(RX + 26, RY + 26, 3.5, 'rgba(254, 243, 199, 0.45)', '6 6', 1)}
          {/* Bottom Perforations */}
          {createArc(RX - 26, RY - 26, 3.5, 'rgba(254, 243, 199, 0.45)', '6 6', 1)}
        </g>
      </svg>

      {/* 2. Orbiting 24 Craft Miniature Film Frames */}
      <div
        className="filmstrip-frames-layer"
        style={{
          position: 'absolute',
          inset: 0,
          pointerEvents: 'none'
        }}
      >
        {framesData.map((item) => {
          const { craft, index, x, y, rotAngle, scale, zIndex, opacity, isCenter } = item;
          
          return (
            <div
              key={craft.name}
              onClick={() => onSelectCraft && onSelectCraft(index)}
              title={`${index + 1}. ${craft.name} – ${craft.tagline}`}
              style={{
                position: 'absolute',
                left: `${x}px`,
                top: `${y}px`,
                transform: `translate(-50%, -50%) rotate(${rotAngle}rad) scale(${scale})`,
                zIndex,
                opacity,
                pointerEvents: 'auto',
                cursor: 'pointer',
                transition: 'transform 0.15s ease-out, opacity 0.15s ease-out'
              }}
              className={`filmstrip-frame-slot ${isCenter ? 'frame-center-active' : ''}`}
            >
              <div
                style={{
                  width: '54px',
                  height: '58px',
                  borderRadius: '6px',
                  backgroundColor: '#0F172A',
                  border: isCenter ? '2px solid #F59E0B' : '1.5px solid rgba(217, 119, 6, 0.6)',
                  boxShadow: isCenter
                    ? '0 0 20px rgba(245, 158, 11, 0.8), 0 8px 16px rgba(0,0,0,0.8)'
                    : '0 4px 10px rgba(0,0,0,0.6)',
                  overflow: 'hidden',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                  background: 'linear-gradient(145deg, #1E293B 0%, #0B1120 100%)',
                  position: 'relative'
                }}
              >
                {/* Perforation Sprocket Dots on Left and Right edges of frame */}
                <div style={{ position: 'absolute', top: '3px', left: '2px', width: '3px', height: '4px', background: '#F59E0B', borderRadius: '1px' }} />
                <div style={{ position: 'absolute', bottom: '3px', left: '2px', width: '3px', height: '4px', background: '#F59E0B', borderRadius: '1px' }} />
                <div style={{ position: 'absolute', top: '3px', right: '2px', width: '3px', height: '4px', background: '#F59E0B', borderRadius: '1px' }} />
                <div style={{ position: 'absolute', bottom: '3px', right: '2px', width: '3px', height: '4px', background: '#F59E0B', borderRadius: '1px' }} />

                {/* Craft Photo or Icon */}
                {craft.image ? (
                  <img
                    src={craft.image}
                    alt={craft.name}
                    style={{
                      width: '100%',
                      height: '38px',
                      objectFit: 'cover',
                      display: 'block'
                    }}
                    onError={(e) => {
                      e.currentTarget.style.display = 'none';
                    }}
                  />
                ) : (
                  <div style={{ color: '#F59E0B', fontSize: '18px', padding: '4px' }}>
                    🎬
                  </div>
                )}

                {/* Craft Label */}
                <span
                  style={{
                    fontSize: '8px',
                    fontWeight: 700,
                    color: isCenter ? '#FDE68A' : '#E2E8F0',
                    lineHeight: 1.1,
                    textAlign: 'center',
                    padding: '2px 4px',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    maxWidth: '48px',
                    textTransform: 'uppercase'
                  }}
                >
                  {craft.shortName || craft.name}
                </span>
              </div>
            </div>
          );
        })}
      </div>

      {/* 3. Golden Film Strip Front Arc (Renders IN FRONT of the 3D Globe) */}
      <svg
        className="filmstrip-svg-front"
        viewBox={`0 0 ${width} ${height}`}
        style={{
          position: 'absolute',
          left: 0,
          top: 0,
          width: '100%',
          height: '100%',
          zIndex: 25,
          pointerEvents: 'none'
        }}
      >
        <defs>
          <linearGradient id="goldFrontGrad" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stopColor="#92621C" stopOpacity="0.8" />
            <stop offset="25%" stopColor="#F59E0B" stopOpacity="0.95" />
            <stop offset="50%" stopColor="#FEF3C7" stopOpacity="1" />
            <stop offset="75%" stopColor="#F59E0B" stopOpacity="0.95" />
            <stop offset="100%" stopColor="#92621C" stopOpacity="0.8" />
          </linearGradient>
          <filter id="goldGlow" x="-20%" y="-20%" width="140%" height="140%">
            <feGaussianBlur stdDeviation="4" result="blur" />
            <feComposite in="SourceGraphic" in2="blur" operator="over" />
          </filter>
        </defs>
        <g transform={`translate(${CX} ${CY}) rotate(${(TILT * 180) / Math.PI})`}>
          {/* Front Golden Ribbon with Metallic Glow */}
          {createArc(RX, RY, 68, 'url(#goldFrontGrad)', '', 0)}
          {/* Film Edge Borders */}
          {createArc(RX + 34, RY + 34, 2, '#FDE68A', '', 0)}
          {createArc(RX - 34, RY - 34, 2, '#FDE68A', '', 0)}
          {/* Top Perforation Sprocket Holes */}
          {createArc(RX + 26, RY + 26, 4, '#1E293B', '5 6', 0)}
          {/* Bottom Perforation Sprocket Holes */}
          {createArc(RX - 26, RY - 26, 4, '#1E293B', '5 6', 0)}
        </g>
      </svg>
    </div>
  );
}
