import { useMemo } from 'react';
import CRAFT_ADDITIONAL_IMAGES from './craftImages';

/**
 * ============================================================================
 * FilmStrip3D – 3D Curved Golden Cinematic Film Strip with Circular Craft Images
 * ============================================================================
 * Features:
 * - 3D Elliptical geometry revolving around the central Earth globe
 * - Golden metallic ribbon body with continuous film sprocket perforations
 * - Circular shaped craft images orbiting in 3D perspective space
 * - Distinct Front Arc (in front of globe) & Back Arc (behind globe)
 * - Clickable circular frames to navigate directly to any craft
 */
export default function FilmStrip3D({
  crafts = [],
  rotationOffset = 0,
  photoSlideIndex = 0,
  onSelectCraft,
  onCraftClick,
  width = 960,
  height = 540
}) {
  const N = crafts.length || 24;
  const CX = width / 2;
  const CY = height / 2 ;
  const RX = Math.min(width * 0.35, 340);
  const RY = Math.min(height * 0.26, 140);
  const TILT = -0.01;

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

  // Calculates 3D perspective positions for all 24 circular frames
  const framesData = useMemo(() => {
    return crafts.map((craft, i) => {
      // Angle for frame i with user-driven rotation offset
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
      const depthScale = 0.65 + 0.35 * ((sn + 1) / 2);
      
      // Calculate angular distance to the front center
      const angleDiff = Math.abs(((t - Math.PI / 2) % (2 * Math.PI) + 3 * Math.PI) % (2 * Math.PI) - Math.PI);
      const isFrontThree = angleDiff < 0.65;
      const isCenter = angleDiff < 0.22;

      return {
        craft,
        index: i,
        x,
        y,
        rotAngle: rotAngle * 0.4,
        scale: depthScale * (isCenter ? 1.3 : isFrontThree ? 1.12 : 0.85),
        zIndex: isFront ? 30 + Math.round(sn * 20) + (isCenter ? 15 : 0) : 2 + Math.round((sn + 1) * 3),
        opacity: isFront ? 1 : 0.55,
        isFront,
        isCenter,
        isFrontThree
      };
    });
  }, [crafts, rotationOffset, CX, CY, RX, RY, N, TILT]);

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

      {/* 2. Orbiting 24 Craft Circular Film Frames */}
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
          const gallery = [
            { person: `${craft.name} featured professional`, image: craft.image },
            ...(CRAFT_ADDITIONAL_IMAGES[craft.name] || []),
          ];
          const photo = gallery[photoSlideIndex % gallery.length];
          
          return (
            <div
              key={craft.name}
              onClick={() => {
                if (onSelectCraft) {
                  onSelectCraft(index);
                } else if (onCraftClick) {
                  onCraftClick(craft);
                }
              }}
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
                transition: 'transform 0.35s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.35s ease'
              }}
              className={`filmstrip-frame-slot ${isCenter ? 'frame-center-active' : ''}`}
            >
              {/* Circular Shaped Miniature Frame */}
              <div
                style={{
                  width: isCenter ? '70px' : '58px',
                  height: isCenter ? '70px' : '58px',
                  borderRadius: '50%',
                  backgroundColor: '#0F172A',
                  border: isCenter ? '2.5px solid #F59E0B' : '2px solid rgba(217, 119, 6, 0.65)',
                  boxShadow: isCenter
                    ? '0 0 22px rgba(214, 188, 144, 0.9), 0 8px 16px rgba(0,0,0,0.8)'
                    : '0 4px 12px rgba(0,0,0,0.6)',
                  overflow: 'hidden',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  background: 'linear-gradient(145deg, #1E293B 0%, #0B1120 100%)',
                  position: 'relative'
                }}
              >
                {/* Circular Craft Image */}
                {photo.image ? (
                  <img
                    src={photo.image}
                    alt={`${photo.person} — ${craft.name}`}
                    title={`${photo.person} — ${craft.name}`}
                    style={{
                      width: '100%',
                      height: '100%',
                      objectFit: 'cover',
                      borderRadius: '50%',
                      display: 'block'
                    }}
                    loading="eager"
                    decoding="async"
                    onError={(e) => {
                      e.currentTarget.style.display = 'none';
                    }}
                  />
                ) : (
                  <div style={{ color: '#F59E0B', fontSize: '18px' }}>
                    🎬
                  </div>
                )}

                {/* Subtle golden rim overlay */}
                <div
                  style={{
                    position: 'absolute',
                    inset: 0,
                    borderRadius: '50%',
                    border: '1px solid rgba(254, 243, 199, 0.3)',
                    pointerEvents: 'none'
                  }}
                />
              </div>

              {/* Miniature Craft Name Tag */}
              <div
                style={{
                  position: 'absolute',
                  bottom: '-16px',
                  left: '50%',
                  transform: 'translateX(-50%)',
                  fontSize: '8px',
                  fontWeight: 800,
                  color: isCenter ? '#FDE68A' : '#CBD5E1',
                  background: 'rgba(15, 23, 42, 0.85)',
                  padding: '1px 6px',
                  borderRadius: '4px',
                  border: isCenter ? '1px solid #F59E0B' : '1px solid rgba(255,255,255,0.1)',
                  whiteSpace: 'nowrap',
                  textTransform: 'uppercase',
                  letterSpacing: '0.04em'
                }}
              >
                {craft.shortName || craft.name}
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
        </defs>
        <g transform={`translate(${CX} ${CY}) rotate(${(TILT * 180) / Math.PI})`}>
          {/* Front Golden Ribbon with Metallic Sheen */}
          {createArc(RX, RY, 68, 'url(#goldFrontGrad)', '', 0)}
          {/* Film Edge Borders */}
          {createArc(RX + 34, RY + 34, 2, '#FDE68A', '', 0)}
          {createArc(RX - 34, RY - 34, 2, '#cabc86', '', 0)}
          {/* Top Perforation Sprocket Holes */}
          {createArc(RX + 26, RY + 26, 4, '#1E293B', '5 6', 0)}
          {/* Bottom Perforation Sprocket Holes */}
          {createArc(RX - 26, RY - 26, 4, '#1E293B', '5 6', 0)}
        </g>
      </svg>
    </div>
  );
}
