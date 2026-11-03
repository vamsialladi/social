import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { 
  HelpCircle, 
  ChevronDown, 
  ChevronUp, 
  Send, 
  CheckCircle2, 
  MessageSquare, 
  Mail, 
  Phone,
  Building2,
  MapPin,
  ExternalLink,
  ShieldCheck 
} from 'lucide-react';
import { FAQS_DATA } from '../services/mockData';
import { api } from '../services/api';
import { useNotification } from '../context/NotificationContext';
import { OFFICIAL_CONTACT } from '../constants/contactInfo';

export default function GetSupportPage() {
  const [faqs] = useState(FAQS_DATA);
  const [openFaqIndex, setOpenFaqIndex] = useState(0);

  // Form State
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [category, setCategory] = useState('Account & Profile');
  const [subject, setSubject] = useState('');
  const [description, setDescription] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const { showNotification } = useNotification();

  const handleSupportSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await api.support.submit({
        fullName,
        email,
        category,
        subject,
        description,
        timestamp: new Date().toISOString()
      });
      setSubmitted(true);
      showNotification('Your support request has been registered! Ticket ID generated (Demo Sync).', 'success');
    } catch (err) {
      showNotification('Unable to submit support ticket', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const toggleFaq = (index) => {
    setOpenFaqIndex(openFaqIndex === index ? null : index);
  };

  return (
    <div style={{ backgroundColor: 'var(--color-bg-page)', minHeight: '85vh', paddingBottom: '80px' }}>
      
      {/* Banner */}
      <section style={{ backgroundColor: 'var(--color-navy)', color: 'white', padding: '52px 0', borderBottom: '3px solid var(--color-maroon)' }}>
        <div className="container">
          <div style={{ maxWidth: '720px' }}>
            <span className="badge badge-maroon" style={{ marginBottom: '10px' }}>Support & Knowledge Center</span>
            <h1 style={{ fontSize: '36px', color: 'white', marginBottom: '12px' }}>
              How Can We Help You?
            </h1>
            <p style={{ fontSize: '16px', color: '#CBD5E1', lineHeight: '1.6' }}>
              Find quick answers to common questions about TrueHunt film portfolios, casting calls, 
              or submit a request directly to our cinema support team.
            </p>
          </div>
        </div>
      </section>

      <div className="container" style={{ marginTop: '40px' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '40px' }}>
          
          {/* Left Column: Frequently Asked Questions */}
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '20px' }}>
              <HelpCircle size={22} color="var(--color-maroon)" />
              <h2 style={{ fontSize: '24px', color: 'var(--color-navy)', margin: 0 }}>
                Frequently Asked Questions
              </h2>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              {faqs.map((faq, index) => {
                const isOpen = openFaqIndex === index;
                return (
                  <div 
                    key={index}
                    className="card"
                    style={{
                      borderRadius: 'var(--radius-sm)',
                      backgroundColor: 'var(--color-white)',
                      border: '1px solid var(--color-border)',
                      overflow: 'hidden',
                      transition: 'border-color var(--transition-fast)'
                    }}
                  >
                    <button
                      onClick={() => toggleFaq(index)}
                      style={{
                        width: '100%',
                        padding: '18px 20px',
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        textAlign: 'left',
                        backgroundColor: 'transparent',
                        cursor: 'pointer',
                        gap: '12px'
                      }}
                      aria-expanded={isOpen}
                    >
                      <span style={{ fontSize: '15px', fontWeight: 600, color: 'var(--color-navy)' }}>
                        {faq.q}
                      </span>
                      {isOpen ? <ChevronUp size={18} color="var(--color-maroon)" /> : <ChevronDown size={18} color="var(--color-text-muted)" />}
                    </button>

                    {isOpen && (
                      <div 
                        style={{
                          padding: '0 20px 20px 20px',
                          fontSize: '14px',
                          color: 'var(--color-text-secondary)',
                          lineHeight: '1.6',
                          borderTop: '1px solid var(--color-border-subtle)',
                          paddingTop: '14px'
                        }}
                      >
                        {faq.a}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>

            {/* Official Support & Corporate Office Contact Card */}
            <div 
              className="card"
              style={{
                marginTop: '28px',
                padding: '24px',
                borderRadius: 'var(--radius-sm)',
                backgroundColor: 'var(--color-white)',
                border: '1px solid var(--color-border)',
                boxShadow: 'var(--shadow-card)'
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
                <Building2 size={20} color="var(--color-maroon)" />
                <div>
                  <h4 style={{ fontSize: '16px', color: 'var(--color-navy)', margin: 0, fontWeight: 700 }}>
                    Official Support & Corporate Office
                  </h4>
                  <span style={{ fontSize: '12px', color: 'var(--color-maroon)', fontWeight: 600 }}>
                    {OFFICIAL_CONTACT.organization} • {OFFICIAL_CONTACT.locationName}
                  </span>
                </div>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginBottom: '18px' }}>
                {/* Email */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <Mail size={16} color="var(--color-maroon)" style={{ flexShrink: 0 }} />
                  <div>
                    <span style={{ fontSize: '11px', color: 'var(--color-text-muted)', display: 'block', textTransform: 'uppercase', fontWeight: 600 }}>
                      Official Email
                    </span>
                    <a 
                      href={`mailto:${OFFICIAL_CONTACT.email}`}
                      style={{ fontSize: '14px', color: 'var(--color-navy)', fontWeight: 600, textDecoration: 'none' }}
                      onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--color-maroon)')}
                      onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--color-navy)')}
                    >
                      {OFFICIAL_CONTACT.email}
                    </a>
                  </div>
                </div>

                {/* Phone */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <Phone size={16} color="var(--color-maroon)" style={{ flexShrink: 0 }} />
                  <div>
                    <span style={{ fontSize: '11px', color: 'var(--color-text-muted)', display: 'block', textTransform: 'uppercase', fontWeight: 600 }}>
                      Direct Telephone
                    </span>
                    <a 
                      href={`tel:${OFFICIAL_CONTACT.phoneTel}`}
                      style={{ fontSize: '14px', color: 'var(--color-navy)', fontWeight: 600, textDecoration: 'none' }}
                      onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--color-maroon)')}
                      onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--color-navy)')}
                    >
                      {OFFICIAL_CONTACT.phone}
                    </a>
                  </div>
                </div>

                {/* Office Address */}
                <div style={{ display: 'flex', alignItems: 'flex-start', gap: '10px' }}>
                  <MapPin size={16} color="var(--color-maroon)" style={{ flexShrink: 0, marginTop: '2px' }} />
                  <div>
                    <span style={{ fontSize: '11px', color: 'var(--color-text-muted)', display: 'block', textTransform: 'uppercase', fontWeight: 600 }}>
                      Office Address
                    </span>
                    <a 
                      href={OFFICIAL_CONTACT.mapUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      style={{ fontSize: '13px', color: 'var(--color-text-secondary)', lineHeight: '1.5', textDecoration: 'none', display: 'inline-block' }}
                      onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--color-navy)')}
                      onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--color-text-secondary)')}
                      title="Open in Google Maps"
                    >
                      {OFFICIAL_CONTACT.address}
                      <ExternalLink size={11} style={{ marginLeft: '4px', verticalAlign: 'middle', color: 'var(--color-maroon)' }} />
                    </a>
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '10px' }}>
                <a 
                  href={OFFICIAL_CONTACT.mapUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="btn btn-outline-navy btn-sm"
                  style={{ flex: 1, justifyContent: 'center', gap: '6px' }}
                >
                  <MapPin size={13} />
                  <span>Map Location</span>
                  <ExternalLink size={11} />
                </a>
                <Link 
                  to="/contact-us"
                  className="btn btn-maroon btn-sm"
                  style={{ flex: 1, justifyContent: 'center' }}
                >
                  Contact Page
                </Link>
              </div>

            </div>

          </div>

          {/* Right Column: Contact & Support Request Form */}
          <div>
            <div className="card" style={{ padding: '32px', borderRadius: 'var(--radius-md)', backgroundColor: 'var(--color-white)' }}>
              
              <div style={{ marginBottom: '20px', paddingBottom: '14px', borderBottom: '1px solid var(--color-border)' }}>
                <h3 style={{ fontSize: '22px', color: 'var(--color-navy)', marginBottom: '6px' }}>
                  Submit a Support Request
                </h3>
                <p style={{ fontSize: '13px', color: 'var(--color-text-muted)', margin: 0 }}>
                  Our entertainment operations team replies within 24 business hours.
                </p>
              </div>

              {submitted ? (
                <div style={{ textAlign: 'center', padding: '40px 10px' }}>
                  <CheckCircle2 size={56} color="#16A34A" style={{ margin: '0 auto 16px auto' }} />
                  <h4 style={{ fontSize: '20px', color: 'var(--color-navy)', marginBottom: '8px' }}>
                    Request Received!
                  </h4>
                  <p style={{ fontSize: '14px', color: 'var(--color-text-muted)', marginBottom: '24px' }}>
                    Your ticket for "<strong>{subject}</strong>" has been queued. You will receive email confirmation shortly.
                  </p>
                  <button 
                    onClick={() => {
                      setSubmitted(false);
                      setSubject('');
                      setDescription('');
                    }}
                    className="btn btn-outline-navy"
                  >
                    Submit Another Query
                  </button>
                </div>
              ) : (
                <form onSubmit={handleSupportSubmit}>
                  
                  <div className="form-group">
                    <label className="form-label">Full Name *</label>
                    <input 
                      type="text"
                      className="form-control"
                      value={fullName}
                      onChange={(e) => setFullName(e.target.value)}
                      placeholder="e.g. Anand Murthy"
                      required
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">Email Address *</label>
                    <input 
                      type="email"
                      className="form-control"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder="anand@production.com"
                      required
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">Issue Category *</label>
                    <select 
                      className="form-control"
                      value={category}
                      onChange={(e) => setCategory(e.target.value)}
                      required
                    >
                      <option value="Account & Profile">Account & Film Profile Verification</option>
                      <option value="Casting Calls">Posting Auditions / Crew Roles</option>
                      <option value="Showreel & Media">Video Showreel / Portfolio Uploads</option>
                      <option value="Investors Connect">Investors Connect Inquiries</option>
                      <option value="LCM App">LCM Production Mobile App</option>
                      <option value="Technical Bug">Technical Platform Issue</option>
                    </select>
                  </div>

                  <div className="form-group">
                    <label className="form-label">Subject *</label>
                    <input 
                      type="text"
                      className="form-control"
                      value={subject}
                      onChange={(e) => setSubject(e.target.value)}
                      placeholder="Brief summary of your query"
                      required
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">Detailed Description *</label>
                    <textarea 
                      className="form-control"
                      rows={4}
                      value={description}
                      onChange={(e) => setDescription(e.target.value)}
                      placeholder="Describe what occurred, project name, or what you need assistance with..."
                      required
                    />
                  </div>

                  <button 
                    type="submit" 
                    disabled={submitting}
                    className="btn btn-maroon btn-lg" 
                    style={{ width: '100%', marginTop: '10px' }}
                  >
                    {submitting ? 'Submitting...' : 'Submit Support Ticket'}
                  </button>

                  <span style={{ fontSize: '11px', color: 'var(--color-text-muted)', display: 'block', textAlign: 'center', marginTop: '12px' }}>
                    * Stores into Spring Boot PostgreSQL database when backend is operational.
                  </span>

                </form>
              )}

            </div>
          </div>

        </div>
      </div>

    </div>
  );
}
