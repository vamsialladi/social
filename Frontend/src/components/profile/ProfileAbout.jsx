import React, { useState } from 'react';
import { Pencil, Sparkles, Award, Target, Compass } from 'lucide-react';

export default function ProfileAbout({ profile, isOwner, onEdit }) {
  const [expanded, setExpanded] = useState(false);
  const bio = profile?.bio || (isOwner 
    ? 'Add your bio to introduce your cinema career, artistic achievements, craft expertise, and production experience.'
    : 'No biography added yet.');
  
  // Clamp text preview threshold
  const isLong = bio.length > 240;
  const displayBio = (isLong && !expanded) ? bio.slice(0, 240) + '...' : bio;

  return (
    <div 
      className="card"
      style={{
        borderRadius: 'var(--radius-lg)',
        padding: '24px 28px',
        backgroundColor: 'var(--color-white)',
        border: '1px solid var(--color-border)',
        boxShadow: 'var(--shadow-card)',
        marginBottom: '20px'
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
        <h3 style={{ fontSize: '19px', color: 'var(--color-navy)', margin: 0, fontWeight: 700 }}>
          About
        </h3>
        {isOwner && (
          <button 
            onClick={onEdit}
            className="btn btn-sm btn-ghost"
            style={{ padding: '6px', borderRadius: '50%', color: 'var(--color-navy)' }}
            title="Edit about summary"
          >
            <Pencil size={17} />
          </button>
        )}
      </div>

      {/* Main Biography Body */}
      <p style={{ fontSize: '15px', color: 'var(--color-text-secondary)', lineHeight: 1.65, margin: 0, whiteSpace: 'pre-line' }}>
        {displayBio}
      </p>

      {/* See More / See Less Toggle */}
      {isLong && (
        <button 
          onClick={() => setExpanded(!expanded)}
          style={{
            marginTop: '6px',
            fontSize: '13px',
            fontWeight: 600,
            color: 'var(--color-maroon)',
            background: 'none',
            border: 'none',
            padding: 0,
            cursor: 'pointer'
          }}
        >
          {expanded ? '...see less' : '...see more'}
        </button>
      )}

      {/* Key Focus & Highlights Tags */}
      <div 
        style={{ 
          marginTop: '18px', 
          paddingTop: '16px', 
          borderTop: '1px solid var(--color-border-subtle)', 
          display: 'grid', 
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', 
          gap: '12px' 
        }}
      >
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: '8px' }}>
          <Target size={16} color="var(--color-maroon)" style={{ marginTop: '2px', flexShrink: 0 }} />
          <div>
            <span style={{ fontSize: '11px', textTransform: 'uppercase', color: 'var(--color-text-muted)', fontWeight: 700, display: 'block' }}>
              Specialization
            </span>
            <span style={{ fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)' }}>
              {profile?.category || 'Film & Production'}
            </span>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'flex-start', gap: '8px' }}>
          <Compass size={16} color="var(--color-maroon)" style={{ marginTop: '2px', flexShrink: 0 }} />
          <div>
            <span style={{ fontSize: '11px', textTransform: 'uppercase', color: 'var(--color-text-muted)', fontWeight: 700, display: 'block' }}>
              Representation & Guild
            </span>
            <span style={{ fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)' }}>
              {profile?.verified ? 'Verified Guild Member' : 'Independent Artist'}
            </span>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'flex-start', gap: '8px' }}>
          <Award size={16} color="var(--color-maroon)" style={{ marginTop: '2px', flexShrink: 0 }} />
          <div>
            <span style={{ fontSize: '11px', textTransform: 'uppercase', color: 'var(--color-text-muted)', fontWeight: 700, display: 'block' }}>
              Industry Experience
            </span>
            <span style={{ fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)' }}>
              {profile?.experience || '5+ Years'}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}

