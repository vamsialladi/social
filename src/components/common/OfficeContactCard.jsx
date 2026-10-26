import React from 'react';
import { Building2, MapPin, Mail, Phone, ExternalLink } from 'lucide-react';
import { OFFICIAL_CONTACT } from '../../constants/contactInfo';

export default function OfficeContactCard({ variant = 'card', showMapButton = true }) {
  const isCompact = variant === 'compact';

  if (isCompact) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: '8px' }}>
          <Building2 size={15} color="#801823" style={{ flexShrink: 0, marginTop: '2px' }} />
          <div>
            <div style={{ color: '#F1F5F9', fontWeight: 600, fontSize: '13px' }}>
              {OFFICIAL_CONTACT.organization}
            </div>
            <div style={{ color: '#94A3B8', fontSize: '12px' }}>
              {OFFICIAL_CONTACT.locationName}
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'flex-start', gap: '8px' }}>
          <MapPin size={15} color="#801823" style={{ flexShrink: 0, marginTop: '2px' }} />
          <a
            href={OFFICIAL_CONTACT.mapUrl}
            target="_blank"
            rel="noopener noreferrer"
            style={{
              color: '#94A3B8',
              fontSize: '12px',
              lineHeight: '1.5',
              textDecoration: 'none',
              transition: 'color var(--transition-fast)'
            }}
            onMouseEnter={(e) => (e.currentTarget.style.color = '#FFFFFF')}
            onMouseLeave={(e) => (e.currentTarget.style.color = '#94A3B8')}
            title="Open in Google Maps"
          >
            {OFFICIAL_CONTACT.address}
            <ExternalLink size={10} style={{ marginLeft: '4px', verticalAlign: 'middle' }} />
          </a>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Mail size={15} color="#801823" style={{ flexShrink: 0 }} />
          <a
            href={`mailto:${OFFICIAL_CONTACT.email}`}
            style={{
              color: '#94A3B8',
              fontSize: '13px',
              textDecoration: 'none',
              transition: 'color var(--transition-fast)'
            }}
            onMouseEnter={(e) => (e.currentTarget.style.color = '#FFFFFF')}
            onMouseLeave={(e) => (e.currentTarget.style.color = '#94A3B8')}
          >
            {OFFICIAL_CONTACT.email}
          </a>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Phone size={15} color="#801823" style={{ flexShrink: 0 }} />
          <a
            href={`tel:${OFFICIAL_CONTACT.phoneTel}`}
            style={{
              color: '#94A3B8',
              fontSize: '13px',
              textDecoration: 'none',
              transition: 'color var(--transition-fast)'
            }}
            onMouseEnter={(e) => (e.currentTarget.style.color = '#FFFFFF')}
            onMouseLeave={(e) => (e.currentTarget.style.color = '#94A3B8')}
          >
            {OFFICIAL_CONTACT.phone}
          </a>
        </div>
      </div>
    );
  }

  return (
    <div
      className="card"
      style={{
        padding: '28px',
        backgroundColor: 'var(--color-white)',
        borderRadius: 'var(--radius-md)',
        border: '1px solid var(--color-border)',
        boxShadow: 'var(--shadow-card)'
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '18px' }}>
        <div
          style={{
            width: '40px',
            height: '40px',
            borderRadius: 'var(--radius-sm)',
            backgroundColor: 'var(--color-maroon-light)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}
        >
          <Building2 size={20} color="var(--color-maroon)" />
        </div>
        <div>
          <h3 style={{ fontSize: '18px', color: 'var(--color-navy)', margin: 0, fontWeight: 700 }}>
            {OFFICIAL_CONTACT.organization}
          </h3>
          <span style={{ fontSize: '13px', color: 'var(--color-maroon)', fontWeight: 600 }}>
            {OFFICIAL_CONTACT.locationName}
          </span>
        </div>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '14px', marginBottom: showMapButton ? '20px' : 0 }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: '10px' }}>
          <MapPin size={18} color="var(--color-maroon)" style={{ flexShrink: 0, marginTop: '2px' }} />
          <div>
            <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Office Address
            </div>
            <div style={{ fontSize: '14px', color: 'var(--color-navy)', lineHeight: '1.5' }}>
              {OFFICIAL_CONTACT.address}
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'flex-start', gap: '10px' }}>
          <Mail size={18} color="var(--color-maroon)" style={{ flexShrink: 0, marginTop: '2px' }} />
          <div>
            <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Official Email
            </div>
            <a
              href={`mailto:${OFFICIAL_CONTACT.email}`}
              style={{
                fontSize: '14px',
                color: 'var(--color-navy)',
                fontWeight: 600,
                textDecoration: 'none'
              }}
              onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--color-maroon)')}
              onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--color-navy)')}
            >
              {OFFICIAL_CONTACT.email}
            </a>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'flex-start', gap: '10px' }}>
          <Phone size={18} color="var(--color-maroon)" style={{ flexShrink: 0, marginTop: '2px' }} />
          <div>
            <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Telephone / Direct Line
            </div>
            <a
              href={`tel:${OFFICIAL_CONTACT.phoneTel}`}
              style={{
                fontSize: '14px',
                color: 'var(--color-navy)',
                fontWeight: 600,
                textDecoration: 'none'
              }}
              onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--color-maroon)')}
              onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--color-navy)')}
            >
              {OFFICIAL_CONTACT.phone}
            </a>
          </div>
        </div>
      </div>

      {showMapButton && (
        <a
          href={OFFICIAL_CONTACT.mapUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="btn btn-outline-navy btn-sm"
          style={{ width: '100%', justifyContent: 'center', gap: '6px' }}
        >
          <MapPin size={14} />
          <span>Open Office Location in Google Maps</span>
          <ExternalLink size={12} />
        </a>
      )}
    </div>
  );
}
