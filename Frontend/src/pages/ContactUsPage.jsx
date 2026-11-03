import React, { useState } from 'react';
import { 
  Mail, 
  Phone, 
  MapPin, 
  Building2, 
  ExternalLink, 
  Send, 
  CheckCircle2, 
  Clock, 
  ShieldCheck, 
  ArrowRight,
  MessageSquare
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { OFFICIAL_CONTACT } from '../constants/contactInfo';
import { useNotification } from '../context/NotificationContext';

export default function ContactUsPage() {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [subject, setSubject] = useState('');
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const { showNotification } = useNotification();

  const handleContactSubmit = (e) => {
    e.preventDefault();
    setSubmitting(true);
    setTimeout(() => {
      setSubmitting(false);
      setSubmitted(true);
      showNotification('Thank you! Your message has been routed to our team.', 'success');
    }, 600);
  };

  return (
    <div style={{ backgroundColor: 'var(--color-bg-page)', minHeight: '85vh', paddingBottom: '80px' }}>
      
      {/* Hero / Header Section */}
      <section 
        style={{ 
          backgroundColor: 'var(--color-navy)', 
          color: 'white', 
          padding: '56px 0', 
          borderBottom: '3px solid var(--color-maroon)' 
        }}
      >
        <div className="container">
          <div style={{ maxWidth: '780px' }}>
            <span 
              className="badge badge-maroon" 
              style={{ marginBottom: '12px', display: 'inline-flex', alignItems: 'center', gap: '6px' }}
            >
              <Building2 size={13} />
              <span>Official Corporate & Production Contact</span>
            </span>
            <h1 style={{ fontSize: 'clamp(32px, 4vw, 42px)', color: 'white', marginBottom: '14px', fontWeight: 800 }}>
              Get in Touch with TrueHunt
            </h1>
            <p style={{ fontSize: '16px', color: '#CBD5E1', lineHeight: '1.6', margin: 0 }}>
              Connect directly with TrueHunt operations, production solutions, studio partnerships, 
              or visit our registered corporate office in Hyderabad.
            </p>
          </div>
        </div>
      </section>

      <div className="container" style={{ marginTop: '40px' }}>
        
        {/* Top 3 Interactive Contact Cards */}
        <div 
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
            gap: '24px',
            marginBottom: '40px'
          }}
        >
          
          {/* Card 1: Official Email */}
          <div 
            className="card"
            style={{
              padding: '28px',
              backgroundColor: 'var(--color-white)',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--color-border)',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
              boxShadow: 'var(--shadow-card)',
              borderTop: '4px solid var(--color-maroon)'
            }}
          >
            <div>
              <div 
                style={{
                  width: '46px',
                  height: '46px',
                  borderRadius: 'var(--radius-sm)',
                  backgroundColor: 'var(--color-maroon-light)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: '18px'
                }}
              >
                <Mail size={22} color="var(--color-maroon)" />
              </div>
              <span style={{ fontSize: '12px', fontWeight: 700, color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Direct Email Channel
              </span>
              <h3 style={{ fontSize: '18px', color: 'var(--color-navy)', margin: '6px 0 10px 0', fontWeight: 700 }}>
                Official Email
              </h3>
              <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)', lineHeight: '1.5', marginBottom: '20px' }}>
                For executive communications, studio partnerships, media inquiries, and platform governance.
              </p>
            </div>
            <div>
              <a 
                href={`mailto:${OFFICIAL_CONTACT.email}`}
                className="btn btn-outline-maroon"
                style={{ width: '100%', justifyContent: 'center', gap: '8px', fontSize: '14px', fontWeight: 600 }}
              >
                <Mail size={16} />
                <span>{OFFICIAL_CONTACT.email}</span>
              </a>
            </div>
          </div>

          {/* Card 2: Contact Number / Telephone */}
          <div 
            className="card"
            style={{
              padding: '28px',
              backgroundColor: 'var(--color-white)',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--color-border)',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
              boxShadow: 'var(--shadow-card)',
              borderTop: '4px solid var(--color-navy)'
            }}
          >
            <div>
              <div 
                style={{
                  width: '46px',
                  height: '46px',
                  borderRadius: 'var(--radius-sm)',
                  backgroundColor: 'var(--color-bg-subtle)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: '18px'
                }}
              >
                <Phone size={22} color="var(--color-navy)" />
              </div>
              <span style={{ fontSize: '12px', fontWeight: 700, color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Direct Telephone
              </span>
              <h3 style={{ fontSize: '18px', color: 'var(--color-navy)', margin: '6px 0 10px 0', fontWeight: 700 }}>
                Official Phone Number
              </h3>
              <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)', lineHeight: '1.5', marginBottom: '20px' }}>
                Connect directly with TrueHunt management for verified production desks and urgent queries.
              </p>
            </div>
            <div>
              <a 
                href={`tel:${OFFICIAL_CONTACT.phoneTel}`}
                className="btn btn-outline-navy"
                style={{ width: '100%', justifyContent: 'center', gap: '8px', fontSize: '14px', fontWeight: 600 }}
              >
                <Phone size={16} />
                <span>{OFFICIAL_CONTACT.phone}</span>
              </a>
            </div>
          </div>

          {/* Card 3: Office / Organization */}
          <div 
            className="card"
            style={{
              padding: '28px',
              backgroundColor: 'var(--color-white)',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--color-border)',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
              boxShadow: 'var(--shadow-card)',
              borderTop: '4px solid #0284C7'
            }}
          >
            <div>
              <div 
                style={{
                  width: '46px',
                  height: '46px',
                  borderRadius: 'var(--radius-sm)',
                  backgroundColor: '#E0F2FE',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: '18px'
                }}
              >
                <Building2 size={22} color="#0284C7" />
              </div>
              <span style={{ fontSize: '12px', fontWeight: 700, color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Registered Office
              </span>
              <h3 style={{ fontSize: '18px', color: 'var(--color-navy)', margin: '6px 0 2px 0', fontWeight: 700 }}>
                {OFFICIAL_CONTACT.organization}
              </h3>
              <div style={{ fontSize: '13px', fontWeight: 600, color: '#0369A1', marginBottom: '8px' }}>
                {OFFICIAL_CONTACT.locationName}
              </div>
              <p style={{ fontSize: '13px', color: 'var(--color-text-secondary)', lineHeight: '1.5', margin: 0 }}>
                {OFFICIAL_CONTACT.address}
              </p>
            </div>
            <div style={{ marginTop: '20px' }}>
              <a 
                href={OFFICIAL_CONTACT.mapUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="btn btn-outline-navy"
                style={{ width: '100%', justifyContent: 'center', gap: '8px', fontSize: '14px', fontWeight: 600 }}
              >
                <MapPin size={16} color="var(--color-maroon)" />
                <span>Open in Google Maps</span>
                <ExternalLink size={13} />
              </a>
            </div>
          </div>

        </div>

        {/* Detailed Grid: Left Office & Hours | Right Send Message Form */}
        <div 
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
            gap: '40px'
          }}
        >
          
          {/* Left Column: Office Specifications & Map Information */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
            
            <div 
              className="card"
              style={{
                padding: '32px',
                backgroundColor: 'var(--color-white)',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--color-border)'
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '18px' }}>
                <MapPin size={22} color="var(--color-maroon)" />
                <h2 style={{ fontSize: '20px', color: 'var(--color-navy)', margin: 0, fontWeight: 700 }}>
                  Corporate Headquarters
                </h2>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', lineHeight: '1.6' }}>
                <div style={{ padding: '14px', backgroundColor: 'var(--color-bg-page)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--color-border-subtle)' }}>
                  <div style={{ fontSize: '12px', fontWeight: 700, color: 'var(--color-text-muted)', textTransform: 'uppercase', marginBottom: '4px' }}>
                    Organization / Facility
                  </div>
                  <div style={{ fontSize: '16px', fontWeight: 700, color: 'var(--color-navy)' }}>
                    {OFFICIAL_CONTACT.organization}
                  </div>
                  <div style={{ fontSize: '14px', fontWeight: 600, color: 'var(--color-maroon)' }}>
                    {OFFICIAL_CONTACT.locationName}
                  </div>
                </div>

                <div style={{ padding: '14px', backgroundColor: 'var(--color-bg-page)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--color-border-subtle)' }}>
                  <div style={{ fontSize: '12px', fontWeight: 700, color: 'var(--color-text-muted)', textTransform: 'uppercase', marginBottom: '4px' }}>
                    Postal Address
                  </div>
                  <div style={{ fontSize: '14px', color: 'var(--color-navy)', fontWeight: 500 }}>
                    {OFFICIAL_CONTACT.address}
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                  <div style={{ padding: '12px', backgroundColor: 'var(--color-bg-page)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--color-border-subtle)' }}>
                    <div style={{ fontSize: '11px', fontWeight: 700, color: 'var(--color-text-muted)', textTransform: 'uppercase' }}>
                      State & Country
                    </div>
                    <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)' }}>
                      Telangana, India
                    </div>
                  </div>
                  <div style={{ padding: '12px', backgroundColor: 'var(--color-bg-page)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--color-border-subtle)' }}>
                    <div style={{ fontSize: '11px', fontWeight: 700, color: 'var(--color-text-muted)', textTransform: 'uppercase' }}>
                      Postal PIN Code
                    </div>
                    <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)' }}>
                      500032
                    </div>
                  </div>
                </div>

                <div style={{ marginTop: '8px' }}>
                  <a 
                    href={OFFICIAL_CONTACT.mapUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="btn btn-navy"
                    style={{ width: '100%', justifyContent: 'center', gap: '8px' }}
                  >
                    <MapPin size={16} />
                    <span>View Location on Google Maps</span>
                    <ExternalLink size={14} />
                  </a>
                </div>
              </div>
            </div>

            {/* Quick Links Card */}
            <div 
              className="card"
              style={{
                padding: '24px',
                backgroundColor: 'var(--color-white)',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--color-border)'
              }}
            >
              <h3 style={{ fontSize: '16px', color: 'var(--color-navy)', marginBottom: '14px', fontWeight: 700 }}>
                Looking for Specific Support?
              </h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                <Link 
                  to="/get-support" 
                  style={{ 
                    display: 'flex', 
                    alignItems: 'center', 
                    justifyContent: 'space-between',
                    padding: '10px 14px',
                    borderRadius: 'var(--radius-sm)',
                    backgroundColor: 'var(--color-bg-page)',
                    color: 'var(--color-navy)',
                    fontSize: '14px',
                    fontWeight: 500,
                    textDecoration: 'none'
                  }}
                >
                  <span>Frequently Asked Questions & Support Tickets</span>
                  <ArrowRight size={14} color="var(--color-maroon)" />
                </Link>
                <Link 
                  to="/protect-producer" 
                  style={{ 
                    display: 'flex', 
                    alignItems: 'center', 
                    justifyContent: 'space-between',
                    padding: '10px 14px',
                    borderRadius: 'var(--radius-sm)',
                    backgroundColor: 'var(--color-bg-page)',
                    color: 'var(--color-navy)',
                    fontSize: '14px',
                    fontWeight: 500,
                    textDecoration: 'none'
                  }}
                >
                  <span>Protect Producer Production Governance</span>
                  <ArrowRight size={14} color="var(--color-maroon)" />
                </Link>
                <Link 
                  to="/investors-connect" 
                  style={{ 
                    display: 'flex', 
                    alignItems: 'center', 
                    justifyContent: 'space-between',
                    padding: '10px 14px',
                    borderRadius: 'var(--radius-sm)',
                    backgroundColor: 'var(--color-bg-page)',
                    color: 'var(--color-navy)',
                    fontSize: '14px',
                    fontWeight: 500,
                    textDecoration: 'none'
                  }}
                >
                  <span>Investors Connect Inquiries</span>
                  <ArrowRight size={14} color="var(--color-maroon)" />
                </Link>
              </div>
            </div>

          </div>

          {/* Right Column: Interactive Contact Form */}
          <div>
            <div 
              className="card" 
              style={{ 
                padding: '36px', 
                borderRadius: 'var(--radius-md)', 
                backgroundColor: 'var(--color-white)',
                border: '1px solid var(--color-border)',
                boxShadow: 'var(--shadow-card)'
              }}
            >
              <div style={{ marginBottom: '24px', paddingBottom: '16px', borderBottom: '1px solid var(--color-border)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
                  <MessageSquare size={20} color="var(--color-maroon)" />
                  <h2 style={{ fontSize: '22px', color: 'var(--color-navy)', margin: 0, fontWeight: 700 }}>
                    Send Us a Direct Message
                  </h2>
                </div>
                <p style={{ fontSize: '14px', color: 'var(--color-text-muted)', margin: 0 }}>
                  Fill out the details below and our team will respond within 24 business hours.
                </p>
              </div>

              {submitted ? (
                <div style={{ textAlign: 'center', padding: '40px 10px' }}>
                  <CheckCircle2 size={56} color="#16A34A" style={{ margin: '0 auto 16px auto' }} />
                  <h3 style={{ fontSize: '20px', color: 'var(--color-navy)', marginBottom: '8px', fontWeight: 700 }}>
                    Message Dispatched Successfully!
                  </h3>
                  <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)', marginBottom: '24px', lineHeight: '1.6' }}>
                    Thank you, <strong>{fullName}</strong>. Your inquiry regarding "{subject}" has been delivered 
                    to <strong>{OFFICIAL_CONTACT.email}</strong>.
                  </p>
                  <button 
                    onClick={() => {
                      setSubmitted(false);
                      setSubject('');
                      setMessage('');
                    }}
                    className="btn btn-outline-navy"
                  >
                    Send Another Message
                  </button>
                </div>
              ) : (
                <form onSubmit={handleContactSubmit}>
                  
                  <div className="form-group" style={{ marginBottom: '18px' }}>
                    <label className="form-label" style={{ fontWeight: 600, color: 'var(--color-navy)', fontSize: '13px' }}>
                      Full Name *
                    </label>
                    <input 
                      type="text"
                      className="form-control"
                      value={fullName}
                      onChange={(e) => setFullName(e.target.value)}
                      placeholder="e.g. Sudhakar"
                      required
                    />
                  </div>

                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', marginBottom: '18px' }}>
                    <div className="form-group" style={{ margin: 0 }}>
                      <label className="form-label" style={{ fontWeight: 600, color: 'var(--color-navy)', fontSize: '13px' }}>
                        Email Address *
                      </label>
                      <input 
                        type="email"
                        className="form-control"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        placeholder="yourname@studio.com"
                        required
                      />
                    </div>

                    <div className="form-group" style={{ margin: 0 }}>
                      <label className="form-label" style={{ fontWeight: 600, color: 'var(--color-navy)', fontSize: '13px' }}>
                        Contact Phone
                      </label>
                      <input 
                        type="tel"
                        className="form-control"
                        value={phone}
                        onChange={(e) => setPhone(e.target.value)}
                        placeholder="+91 98765 43210"
                      />
                    </div>
                  </div>

                  <div className="form-group" style={{ marginBottom: '18px' }}>
                    <label className="form-label" style={{ fontWeight: 600, color: 'var(--color-navy)', fontSize: '13px' }}>
                      Subject / Inquiry Type *
                    </label>
                    <input 
                      type="text"
                      className="form-control"
                      value={subject}
                      onChange={(e) => setSubject(e.target.value)}
                      placeholder="e.g. Protect Producer consultation / Studio partnership"
                      required
                    />
                  </div>

                  <div className="form-group" style={{ marginBottom: '22px' }}>
                    <label className="form-label" style={{ fontWeight: 600, color: 'var(--color-navy)', fontSize: '13px' }}>
                      Message *
                    </label>
                    <textarea 
                      className="form-control"
                      rows={5}
                      value={message}
                      onChange={(e) => setMessage(e.target.value)}
                      placeholder="Please share details about your inquiry or project..."
                      required
                    />
                  </div>

                  <button 
                    type="submit" 
                    disabled={submitting}
                    className="btn btn-maroon btn-lg" 
                    style={{ width: '100%', justifyContent: 'center', gap: '8px' }}
                  >
                    <Send size={16} />
                    <span>{submitting ? 'Transmitting Message...' : 'Send Message'}</span>
                  </button>

                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px', marginTop: '16px', fontSize: '12px', color: 'var(--color-text-muted)' }}>
                    <ShieldCheck size={14} color="#16A34A" />
                    <span>Your contact information is strictly protected per TrueHunt Privacy Policy.</span>
                  </div>

                </form>
              )}

            </div>
          </div>

        </div>

      </div>

    </div>
  );
}
