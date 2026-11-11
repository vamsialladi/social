import React from 'react';
import { FileText, ArrowLeft, AlertTriangle } from 'lucide-react';
import { Link } from 'react-router-dom';
import { OFFICIAL_CONTACT } from '../constants/contactInfo';

export default function TermsPage() {
  return (
    <div style={{ backgroundColor: 'var(--color-bg-page)', minHeight: '85vh', paddingBottom: '80px' }}>
      
      {/* Banner */}
      <section style={{ backgroundColor: 'var(--color-navy)', color: 'white', padding: '48px 0', borderBottom: '3px solid var(--color-maroon)' }}>
        <div className="container">
          <Link to="/" style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '13px', color: '#93C5FD', marginBottom: '14px' }}>
            <ArrowLeft size={14} /> Back to Home
          </Link>
          <h1 style={{ fontSize: '36px', color: 'white', marginBottom: '8px' }}>
            Terms & Conditions
          </h1>
          <p style={{ fontSize: '15px', color: '#CBD5E1' }}>
            TrueHunt Platform Agreement • truehunt.in • Version 1.0
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
            <FileText size={24} color="var(--color-maroon)" />
            <h2 style={{ fontSize: '22px', margin: 0 }}>Terms of Service for Entertainment Industry Users</h2>
          </div>

          <p style={{ marginBottom: '20px' }}>
            Please read these Terms and Conditions ("Terms") carefully before using TrueHunt (truehunt.in), 
            operated by TrueHunt Technologies. By creating an account or accessing platform directories, 
            you agree to be bound by these Terms.
          </p>

          <h3 style={{ fontSize: '18px', color: 'var(--color-navy)', marginTop: '28px', marginBottom: '12px' }}>
            1. Talent Representation & Credential Accuracy
          </h3>
          <p style={{ marginBottom: '16px' }}>
            Professionals registering on TrueHunt agree that all stated film credits, theatrical releases, 
            awards, and showreel media represent genuine creative work in which the registrant actively participated. 
            Impersonation of film industry figures or fraudulent claim of screen credits will result in immediate termination.
          </p>

          <h3 style={{ fontSize: '18px', color: 'var(--color-navy)', marginTop: '28px', marginBottom: '12px' }}>
            2. Strict Ban on "Pay-to-Audition" Schemes
          </h3>
          <p style={{ marginBottom: '16px' }}>
            TrueHunt enforces a zero-tolerance policy against fraudulent casting agencies charging audition fees 
            or registration levies to aspiring actors or crew members. Any production listing demanding payment 
            from talent will be permanently banned and reported to relevant trade bodies.
          </p>

          <h3 style={{ fontSize: '18px', color: 'var(--color-navy)', marginTop: '28px', marginBottom: '12px' }}>
            3. Disclaimer Regarding Investors Connect
          </h3>
          <div 
            style={{
              padding: '16px',
              borderRadius: 'var(--radius-sm)',
              backgroundColor: '#FEF3C7',
              border: '1px solid #FDE68A',
              color: '#92400E',
              marginBottom: '16px',
              fontSize: '14px',
              lineHeight: '1.6'
            }}
          >
            TrueHunt does not broker film financing, does not endorse investment offerings, and does not promise funding outcomes. 
            All co-production terms, equity agreements, and slate commitments must be drafted with independent legal counsel.
          </div>

          <h3 style={{ fontSize: '18px', color: 'var(--color-navy)', marginTop: '28px', marginBottom: '12px' }}>
            4. User Code of Conduct
          </h3>
          <p style={{ marginBottom: '20px' }}>
            Users must maintain professional etiquette in all networking interactions. Harassment, unsolicited spam, 
            unauthorized redistribution of unreleased scripts, or unauthorized sharing of contact information will result in account suspension.
          </p>

          <h3 style={{ fontSize: '18px', color: 'var(--color-navy)', marginTop: '28px', marginBottom: '12px' }}>
            5. Registered Office & Legal Notice
          </h3>
          <div style={{ padding: '20px', borderRadius: 'var(--radius-sm)', backgroundColor: '#F8FAFC', border: '1px solid var(--color-border)', fontSize: '13px', color: 'var(--color-text-secondary)', lineHeight: '1.6' }}>
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
              <strong>Official Email:</strong>{' '}
              <a href={`mailto:${OFFICIAL_CONTACT.email}`} style={{ color: 'var(--color-navy)' }}>
                {OFFICIAL_CONTACT.email}
              </a>
              {' • '}
              <strong>Direct Phone:</strong>{' '}
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
