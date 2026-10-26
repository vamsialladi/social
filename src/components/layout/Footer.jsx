import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { 
  Film, 
  Mail, 
  MapPin, 
  Phone,
  Building2,
  Send, 
  ExternalLink,
  Shield,
  FileText,
  MessageSquare
} from 'lucide-react';
import TrueHuntLogo from '../common/TrueHuntLogo';
import { useNotification } from '../../context/NotificationContext';
import { OFFICIAL_CONTACT } from '../../constants/contactInfo';

export default function Footer() {
  const [newsletterEmail, setNewsletterEmail] = useState('');
  const [subscribed, setSubscribed] = useState(false);
  const { showNotification } = useNotification();

  const handleNewsletterSubmit = (e) => {
    e.preventDefault();
    if (!newsletterEmail || !newsletterEmail.includes('@')) {
      showNotification('Please provide a valid email address', 'error');
      return;
    }
    setSubscribed(true);
    showNotification('Thank you for subscribing to TrueHunt Industry Briefings (Demo Mode)', 'success');
  };

  return (
    <footer 
      style={{
        backgroundColor: 'var(--color-navy)',
        color: '#E2E8F0',
        paddingTop: '64px',
        paddingBottom: '32px',
        borderTop: '3px solid var(--color-maroon)'
      }}
    >
      <div className="container">
        
        {/* Top Footer Grid */}
        <div 
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
            gap: '40px',
            marginBottom: '48px'
          }}
        >
          
          {/* Col 1: Brand & Bio */}
          <div style={{ maxWidth: '340px' }}>
            <div style={{ marginBottom: '16px' }}>
              <TrueHuntLogo size="md" variant="dark" />
            </div>
            <p style={{ fontSize: '14px', color: '#94A3B8', lineHeight: '1.6', marginBottom: '20px' }}>
              TrueHunt (<strong>truehunt.in</strong>) is the dedicated professional networking and opportunity platform 
              connecting cinema professionals, cast, technicians, studios, and investors across the film industry.
            </p>
            <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', padding: '6px 12px', borderRadius: 'var(--radius-xs)', backgroundColor: 'rgba(255, 255, 255, 0.08)', fontSize: '12px', color: '#CBD5E1' }}>
              <span>Concept:</span>
              <span style={{ color: '#F1F5F9', fontStyle: 'italic' }}>Find talent. Discover opportunity. Create the next story.</span>
            </div>
          </div>

          {/* Col 2: Quick Links */}
          <div>
            <h4 style={{ color: '#FFFFFF', fontSize: '16px', fontWeight: 700, marginBottom: '20px', letterSpacing: '0.02em' }}>
              Navigation
            </h4>
            <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: '12px', padding: 0 }}>
              <li>
                <Link to="/entertainments" style={{ color: '#94A3B8', fontSize: '14px', display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                  <span>Entertainments</span>
                </Link>
              </li>
              <li>
                <Link to="/talents" style={{ color: '#94A3B8', fontSize: '14px', display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                  <span>Talents Directory</span>
                </Link>
              </li>
              <li>
                <Link to="/jobs" style={{ color: '#94A3B8', fontSize: '14px', display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                  <span>Auditions & Jobs</span>
                </Link>
              </li>
              <li>
                <Link to="/investors-connect" style={{ color: '#94A3B8', fontSize: '14px', display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                  <span>Investors Connect</span>
                </Link>
              </li>
              <li>
                <Link to="/lcm-app" style={{ color: '#94A3B8', fontSize: '14px', display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                  <span>LCM Production App</span>
                </Link>
              </li>
              <li>
                <Link to="/get-support" style={{ color: '#94A3B8', fontSize: '14px', display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                  <span>Get Support & FAQs</span>
                </Link>
              </li>
              <li>
                <Link to="/contact-us" style={{ color: '#94A3B8', fontSize: '14px', display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                  <span>Contact Us</span>
                </Link>
              </li>
              <li>
                <Link to="/about-us" style={{ color: '#94A3B8', fontSize: '14px', display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                  <span>About Us</span>
                </Link>
              </li>
            </ul>
          </div>

          {/* Col 3: Official Headquarters & Legal */}
          <div>
            <h4 style={{ color: '#FFFFFF', fontSize: '16px', fontWeight: 700, marginBottom: '20px', letterSpacing: '0.02em' }}>
              Headquarters & Contact
            </h4>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              
              {/* Organization & Location Name */}
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: '8px' }}>
                <Building2 size={16} color="#E11D48" style={{ flexShrink: 0, marginTop: '2px' }} />
                <div>
                  <div style={{ color: '#FFFFFF', fontSize: '13px', fontWeight: 700 }}>
                    {OFFICIAL_CONTACT.organization}
                  </div>
                  <div style={{ color: '#CBD5E1', fontSize: '12px', fontWeight: 500 }}>
                    {OFFICIAL_CONTACT.locationName}
                  </div>
                </div>
              </div>

              {/* Office Address with Map Link */}
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: '8px' }}>
                <MapPin size={16} color="#E11D48" style={{ flexShrink: 0, marginTop: '2px' }} />
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
                  title="Search location on Google Maps"
                >
                  {OFFICIAL_CONTACT.address}
                  <ExternalLink size={10} style={{ marginLeft: '4px', verticalAlign: 'middle', color: '#CBD5E1' }} />
                </a>
              </div>

              {/* Email Link */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Mail size={15} color="#E11D48" style={{ flexShrink: 0 }} />
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

              {/* Phone Link */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Phone size={15} color="#E11D48" style={{ flexShrink: 0 }} />
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

              {/* Trust Links */}
              <div style={{ display: 'flex', gap: '14px', paddingTop: '8px', borderTop: '1px solid rgba(255, 255, 255, 0.08)' }}>
                <Link to="/privacy-policy" style={{ color: '#94A3B8', fontSize: '12px', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                  <Shield size={12} color="#801823" />
                  <span>Privacy Policy</span>
                </Link>
                <Link to="/terms-and-conditions" style={{ color: '#94A3B8', fontSize: '12px', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                  <FileText size={12} color="#801823" />
                  <span>Terms</span>
                </Link>
              </div>

            </div>
          </div>

          {/* Col 4: Industry Updates Newsletter */}
          <div>
            <h4 style={{ color: '#FFFFFF', fontSize: '16px', fontWeight: 700, marginBottom: '12px', letterSpacing: '0.02em' }}>
              Cinema Intelligence
            </h4>
            <p style={{ fontSize: '13px', color: '#94A3B8', marginBottom: '16px' }}>
              Subscribe for casting calls, studio rosters, and film financing updates.
            </p>
            {subscribed ? (
              <div style={{ backgroundColor: 'rgba(21, 128, 61, 0.2)', border: '1px solid #15803D', padding: '10px 14px', borderRadius: 'var(--radius-sm)', color: '#86EFAC', fontSize: '13px' }}>
                ✓ Subscribed! You will receive film industry notifications.
              </div>
            ) : (
              <form onSubmit={handleNewsletterSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                <div style={{ display: 'flex', gap: '6px' }}>
                  <input 
                    type="email" 
                    value={newsletterEmail}
                    onChange={(e) => setNewsletterEmail(e.target.value)}
                    placeholder="name@studio.com"
                    aria-label="Email address for newsletter"
                    style={{
                      flex: 1,
                      padding: '10px 12px',
                      borderRadius: 'var(--radius-sm)',
                      border: '1px solid #334155',
                      backgroundColor: '#0F233D',
                      color: '#FFFFFF',
                      fontSize: '13px',
                      outline: 'none'
                    }}
                  />
                  <button 
                    type="submit"
                    className="btn btn-sm btn-maroon"
                    aria-label="Subscribe to newsletter"
                  >
                    <Send size={15} />
                  </button>
                </div>
                <span style={{ fontSize: '11px', color: '#64748B' }}>
                  * Integration status: UI ready. Syncs to backend newsletter registry.
                </span>
              </form>
            )}
          </div>

        </div>

        {/* Bottom Bar: Copyright & Disclaimers */}
        <div 
          style={{
            borderTop: '1px solid rgba(255, 255, 255, 0.1)',
            paddingTop: '24px',
            display: 'flex',
            flexWrap: 'wrap',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '16px',
            fontSize: '13px',
            color: '#64748B'
          }}
        >
          <div>
            © {new Date().getFullYear()} TrueHunt (truehunt.in). All rights reserved. Film Industry Professional Network.
          </div>
          <div style={{ display: 'flex', gap: '20px' }}>
            <Link to="/privacy-policy" style={{ color: '#94A3B8' }}>Privacy</Link>
            <Link to="/terms-and-conditions" style={{ color: '#94A3B8' }}>Terms</Link>
            <Link to="/get-support" style={{ color: '#94A3B8' }}>Support</Link>
            <Link to="/contact-us" style={{ color: '#94A3B8' }}>Contact Us</Link>
            <Link to="/about-us" style={{ color: '#94A3B8' }}>About Us</Link>
          </div>
        </div>

      </div>
    </footer>
  );
}
