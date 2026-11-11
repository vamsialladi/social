import React from 'react';
import { Link } from 'react-router-dom';
import { 
  Film, 
  Video, 
  Camera, 
  Scissors, 
  PenTool, 
  Palette, 
  Mic2, 
  Music, 
  Sparkles, 
  Briefcase, 
  Shirt, 
  Layers, 
  ArrowRight 
} from 'lucide-react';

const iconMap = {
  Film,
  Video,
  Camera,
  Scissors,
  PenTool,
  Palette,
  Mic2,
  Music,
  Sparkles,
  Briefcase,
  Shirt,
  Layers
};

export default function CraftCard({ craft }) {
  const IconComponent = iconMap[craft.icon] || Film;

  return (
    <Link 
      to={`/talents?category=${craft.title.toLowerCase()}`}
      className="card card-hoverable"
      style={{
        padding: '24px',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        borderRadius: 'var(--radius-md)',
        backgroundColor: 'var(--color-white)',
        border: '1px solid var(--color-border)',
        textDecoration: 'none',
        transition: 'all var(--transition-fast)'
      }}
    >
      <div>
        <div 
          style={{
            width: '48px',
            height: '48px',
            borderRadius: 'var(--radius-sm)',
            backgroundColor: 'var(--color-navy-transparent)',
            color: 'var(--color-navy)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            marginBottom: '16px',
            border: '1px solid #E2E8F0'
          }}
        >
          <IconComponent size={24} strokeWidth={1.8} />
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: '6px' }}>
          <h3 
            style={{
              fontSize: '17px',
              fontWeight: 700,
              color: 'var(--color-navy)'
            }}
          >
            {craft.title}
          </h3>
          <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--color-maroon)' }}>
            {craft.count}
          </span>
        </div>

        <p style={{ fontSize: '13px', color: 'var(--color-text-muted)', lineHeight: '1.4' }}>
          {craft.desc}
        </p>
      </div>

      <div 
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '6px',
          fontSize: '13px',
          fontWeight: 600,
          color: 'var(--color-navy)',
          marginTop: '16px',
          paddingTop: '12px',
          borderTop: '1px solid var(--color-border-subtle)'
        }}
      >
        <span>Browse Professionals</span>
        <ArrowRight size={14} color="var(--color-maroon)" />
      </div>
    </Link>
  );
}
