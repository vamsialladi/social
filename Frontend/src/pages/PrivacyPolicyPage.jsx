import React from 'react';
import { Shield, Lock, Eye, FileText, ArrowLeft, Mail, Phone, MapPin, Building2, ExternalLink } from 'lucide-react';
import { Link } from 'react-router-dom';
import { OFFICIAL_CONTACT } from '../constants/contactInfo';

export default function PrivacyPolicyPage() {
  return (
    <div style={{ backgroundColor: 'var(--color-bg-page)', minHeight: '85vh', paddingBottom: '80px' }}>
      
      {/* Banner */}
      <section style={{ backgroundColor: 'var(--color-navy)', color: 'white', padding: '48px 0', borderBottom: '3px solid var(--color-maroon)' }}>
        <div className="container">
          <Link to="/" style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '13px', color: '#93C5FD', marginBottom: '14px' }}>
            <ArrowLeft size={14} /> Back to Home
          </Link>
          <h1 style={{ fontSize: '36px', color: 'white', marginBottom: '8px' }}>
            TrueHunt Privacy Policy
          </h1>
          <p style={{ fontSize: '15px', color: '#CBD5E1' }}>
            Domain: truehunt.in • Effective Date: October 2026 • Dedicated Film Industry Protections
          </p>
        </div>
      </section>

      <div className="container" style={{ marginTop: '36px' }}>
        <div 
          className="card"
          style={{
            maxWidth: '860px',
            margin: '0 auto',
            padding: '40px',
            borderRadius: 'var(--radius-md)',
            backgroundColor: 'var(--color-white)',
            border: '1px solid var(--color-border)',
            lineHeight: '1.8',
            fontSize: '15px',
            color: 'var(--color-text-secondary)'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', color: 'var(--color-navy)', marginBottom: '16px' }}>
            <Shield size={24} color="var(--color-maroon)" />
            <h2 style={{ fontSize: '22px', margin: 0 }}>Commitment to Cinema Creator Privacy</h2>
          </div>

          <p style={{ marginBottom: '20px' }}>
            TrueHunt (<strong>truehunt.in</strong>) operates a professional networking platform 
            built specifically for actors, filmmakers, writers, technicians, production houses, and film financiers. 
            Unlike general consumer social networks, cinema professionals face unique privacy concerns regarding 
            unreleased script materials, confidential casting calls, and personal contact telephone numbers.
          </p>

          <h3 style={{ fontSize: '18px', color: 'var(--color-navy)', marginTop: '28px', marginBottom: '12px' }}>
            1. Non-Disclosure of Personal Contact Information
          </h3>
          <p style={{ marginBottom: '16px' }}>
            TrueHunt does not publicly expose your personal phone number, private residential address, 
            or personal unlisted email to public search engine crawlers or non-verified viewers. 
            All contact initiation takes place through verified TrueHunt invitation channels.
          </p>

          <h3 style={{ fontSize: '18px', color: 'var(--color-navy)', marginTop: '28px', marginBottom: '12px' }}>
            2. Intellectual Property & Showreel Protection
          </h3>
          <p style={{ marginBottom: '16px' }}>
            Filmmakers, writers, and technicians retain 100% intellectual property ownership of their uploaded showreels, 
            screenplay samples, lookbooks, and pitch decks. TrueHunt does not sell your creative assets to third-party 
            generative AI model training vendors.
          </p>

          <h3 style={{ fontSize: '18px', color: 'var(--color-navy)', marginTop: '28px', marginBottom: '12px' }}>
            3. Information We Collect
          </h3>
          <ul style={{ paddingLeft: '20px', marginBottom: '20px' }}>
            <li><strong>Profile Information:</strong> Professional name, craft category, verified filmography credits, and location.</li>
            <li><strong>Project Submissions:</strong> Pitch decks, lookbooks, and audition reels voluntarily submitted to auditions or investors.</li>
            <li><strong>Communication Data:</strong> Connection requests and support inquiries processed securely.</li>
          </ul>

          <h3 style={{ fontSize: '18px', color: 'var(--color-navy)', marginTop: '28px', marginBottom: '12px' }}>
            4. Data Retention & Deletion Rights
          </h3>
          <p style={{ marginBottom: '20px' }}>
            You may request complete erasure of your TrueHunt talent profile, uploaded media, and connection logs 
            at any time by submitting a request via our Support center or directly emailing our governance desk at{' '}
            <a href={`mailto:${OFFICIAL_CONTACT.email}`} style={{ color: 'var(--color-navy)', fontWeight: 600 }}>
              {OFFICIAL_CONTACT.email}
            </a>.
          </p>

          <div style={{ padding: '20px', borderRadius: 'var(--radius-sm)', backgroundColor: '#F8FAFC', border: '1px solid var(--color-border)', fontSize: '13px', color: 'var(--color-text-secondary)', lineHeight: '1.6' }}>
            <div style={{ fontWeight: 700, color: 'var(--color-navy)', marginBottom: '6px', fontSize: '14px' }}>
              Official Data Governance & Corporate Contact:
            </div>
            <div>
              <strong>Organization:</strong> {OFFICIAL_CONTACT.organization} ({OFFICIAL_CONTACT.locationName})
            </div>
            <div>
              <strong>Office Address:</strong>{' '}
              <a href={OFFICIAL_CONTACT.mapUrl} target="_blank" rel="noopener noreferrer" style={{ color: 'var(--color-navy)', textDecoration: 'underline' }}>
                {OFFICIAL_CONTACT.address}
              </a>
            </div>
            <div>
              <strong>Email:</strong>{' '}
              <a href={`mailto:${OFFICIAL_CONTACT.email}`} style={{ color: 'var(--color-navy)' }}>
                {OFFICIAL_CONTACT.email}
              </a>
              {' • '}
              <strong>Phone:</strong>{' '}
              <a href={`tel:${OFFICIAL_CONTACT.phoneTel}`} style={{ color: 'var(--color-navy)' }}>
                {OFFICIAL_CONTACT.phone}
              </a>
            </div>
          </div>
        </div>
      </div>

    </div>
  );
}
