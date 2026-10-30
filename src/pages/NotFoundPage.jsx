import React from 'react';
import { Link } from 'react-router-dom';
import { Film, Home, Users, Briefcase } from 'lucide-react';
import TrueHuntLogo from '../components/common/TrueHuntLogo';

export default function NotFoundPage() {
  return (
    <div 
      style={{
        minHeight: '75vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '60px 20px',
        textAlign: 'center',
        backgroundColor: 'var(--color-bg-page)'
      }}
    >
      <div 
        className="card"
        style={{
          maxWidth: '520px',
          padding: '48px 36px',
          borderRadius: 'var(--radius-lg)',
          backgroundColor: 'var(--color-white)',
          boxShadow: 'var(--shadow-card)'
        }}
      >
        <div 
          style={{
            width: '64px',
            height: '64px',
            borderRadius: '50%',
            backgroundColor: 'var(--color-maroon-light)',
            color: 'var(--color-maroon)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 20px auto'
          }}
        >
          <Film size={32} />
        </div>

        <span className="badge badge-maroon" style={{ marginBottom: '10px' }}>Scene 404 • Missing Script</span>
        
        <h1 style={{ fontSize: '32px', color: 'var(--color-navy)', marginBottom: '10px' }}>
          Scene Not Found
        </h1>

        <p style={{ fontSize: '15px', color: 'var(--color-text-secondary)', lineHeight: '1.6', marginBottom: '32px' }}>
          The page or film record you are looking for has been cut from the final edit or relocated.
        </p>

        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '10px', justifyContent: 'center' }}>
          <Link to="/" className="btn btn-navy">
            <Home size={16} /> Return Home
          </Link>
          <Link to="/talents" className="btn btn-outline-navy">
            <Users size={16} /> Browse Talents
          </Link>
          <Link to="/jobs" className="btn btn-outline-subtle">
            <Briefcase size={16} /> Explore Jobs
          </Link>
        </div>
      </div>
    </div>
  );
}
