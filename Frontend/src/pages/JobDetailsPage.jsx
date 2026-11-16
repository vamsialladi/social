import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { 
  Building2, 
  MapPin, 
  Clock, 
  Calendar, 
  ArrowLeft, 
  CheckCircle2, 
  Send, 
  Share2, 
  Briefcase, 
  ShieldCheck, 
  DollarSign 
} from 'lucide-react';
import { LATEST_OPPORTUNITIES } from '../services/mockData';
import { api } from '../services/api';
import { useNotification } from '../context/NotificationContext';

export default function JobDetailsPage() {
  const { id } = useParams();
  const [job, setJob] = useState(null);
  const [loading, setLoading] = useState(true);
  const [showApplyModal, setShowApplyModal] = useState(false);
  const [applicantName, setApplicantName] = useState('');
  const [applicantEmail, setApplicantEmail] = useState('');
  const [portfolioLink, setPortfolioLink] = useState('');
  const [pitchNote, setPitchNote] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [applied, setApplied] = useState(false);
  const { showNotification } = useNotification();

  useEffect(() => {
    async function loadJob() {
      setLoading(true);
      try {
        const data = await api.jobs.getById(id);
        setJob(data || LATEST_OPPORTUNITIES[0]);
      } catch (e) {
        setJob(LATEST_OPPORTUNITIES[0]);
      } finally {
        setLoading(false);
      }
    }
    loadJob();
  }, [id]);

  const handleApply = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await api.jobs.apply(job.id, {
        applicantName,
        applicantEmail,
        portfolioLink,
        pitchNote,
        jobTitle: job.title
      });
      setApplied(true);
      setShowApplyModal(false);
      showNotification(`Application submitted successfully for ${job.title}! (Demo Sync)`, 'success');
    } catch (e) {
      showNotification('Failed to submit application', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading || !job) {
    return (
      <div className="container" style={{ padding: '80px 20px', textAlign: 'center' }}>
        <p style={{ color: 'var(--color-text-muted)' }}>Loading opportunity breakdown...</p>
      </div>
    );
  }

  return (
    <div style={{ backgroundColor: 'var(--color-bg-page)', minHeight: '85vh', paddingBottom: '80px' }}>
      
      {/* Top Breadcrumb */}
      <div style={{ backgroundColor: 'var(--color-white)', borderBottom: '1px solid var(--color-border)', padding: '12px 0' }}>
        <div className="container" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Link to="/jobs" style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)' }}>
            <ArrowLeft size={16} /> Back to Job & Audition Board
          </Link>
          <button 
            onClick={() => {
              if (navigator.clipboard) {
                navigator.clipboard.writeText(window.location.href);
                showNotification('Opportunity link copied to clipboard', 'info');
              }
            }} 
            className="btn btn-sm btn-ghost" 
            style={{ fontSize: '12px' }}
          >
            <Share2 size={14} /> Share Opening
          </button>
        </div>
      </div>

      <div className="container" style={{ marginTop: '28px' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '28px' }}>
          
          {/* Main Details (Left 2 cols) */}
          <div style={{ gridColumn: 'span 2', display: 'flex', flexDirection: 'column', gap: '24px' }}>
            
            {/* Header Card */}
            <div className="card" style={{ padding: '32px', borderRadius: 'var(--radius-md)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '12px', marginBottom: '14px' }}>
                <span className="badge badge-maroon" style={{ fontSize: '13px', padding: '6px 12px' }}>
                  {job.roleCategory}
                </span>
                <span style={{ fontSize: '13px', color: 'var(--color-text-muted)', display: 'flex', alignItems: 'center', gap: '4px' }}>
                  <Clock size={14} /> Posted {job.postedDate}
                </span>
              </div>

              <h1 style={{ fontSize: '28px', color: 'var(--color-navy)', marginBottom: '12px' }}>
                {job.title}
              </h1>

              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '16px', fontSize: '14px', color: 'var(--color-text-secondary)', marginBottom: '24px' }}>
                <span style={{ display: 'flex', alignItems: 'center', gap: '6px', fontWeight: 600, color: 'var(--color-navy)' }}>
                  <Building2 size={16} color="var(--color-maroon)" /> {job.productionCompany}
                </span>
                <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <MapPin size={16} /> {job.location}
                </span>
                <span style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--color-brown-arrow)', fontWeight: 600 }}>
                  <Briefcase size={16} /> {job.type}
                </span>
              </div>

              {job.budgetOrPay && (
                <div style={{ padding: '12px 16px', backgroundColor: '#F0FDF4', borderRadius: 'var(--radius-sm)', border: '1px solid #BBF7D0', color: '#166534', fontWeight: 600, display: 'inline-block' }}>
                  Budget / Honorarium: {job.budgetOrPay}
                </div>
              )}
            </div>

            {/* Description */}
            <div className="card" style={{ padding: '32px', borderRadius: 'var(--radius-md)' }}>
              <h3 style={{ fontSize: '20px', color: 'var(--color-navy)', marginBottom: '16px' }}>
                Project Overview & Role Scope
              </h3>
              <p style={{ fontSize: '15px', color: 'var(--color-text-secondary)', lineHeight: '1.7', marginBottom: '24px' }}>
                {job.description}
              </p>

              <h4 style={{ fontSize: '18px', color: 'var(--color-navy)', marginBottom: '14px' }}>
                Eligibility & Candidate Requirements
              </h4>
              <ul style={{ listStyle: 'none', padding: 0, display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {job.requirements?.map((req, i) => (
                  <li key={i} style={{ display: 'flex', alignItems: 'flex-start', gap: '10px', fontSize: '14px', color: 'var(--color-text-secondary)' }}>
                    <CheckCircle2 size={18} color="var(--color-maroon)" style={{ flexShrink: 0, marginTop: '2px' }} />
                    <span>{req}</span>
                  </li>
                ))}
              </ul>
            </div>

          </div>

          {/* Sidebar (Right) */}
          <aside style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
            
            {/* Apply CTA Card */}
            <div className="card" style={{ padding: '28px', borderRadius: 'var(--radius-md)', textAlign: 'center' }}>
              <h3 style={{ fontSize: '18px', color: 'var(--color-navy)', marginBottom: '8px' }}>
                Apply for this Position
              </h3>
              <p style={{ fontSize: '13px', color: 'var(--color-text-muted)', marginBottom: '14px' }}>
                Application Deadline: <strong>{job.deadline}</strong>
              </p>
              <button 
                onClick={() => applied ? null : setShowApplyModal(true)}
                disabled={applied}
                className={`btn btn-lg ${applied ? 'btn-outline-subtle' : 'btn-maroon'}`}
                style={{ width: '100%', marginBottom: '12px' }}
              >
                {applied ? '✓ Application Submitted' : 'Express Interest / Apply'}
              </button>
              <span style={{ fontSize: '11px', color: 'var(--color-text-muted)', display: 'block' }}>
                Submissions are sent directly to the casting director & AD team.
              </span>
            </div>

            {/* Production Team Transparency */}
            <div className="card" style={{ padding: '24px', borderRadius: 'var(--radius-md)', backgroundColor: '#F8FAFC' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--color-navy)', fontWeight: 700, marginBottom: '8px' }}>
                <ShieldCheck size={18} color="#2563EB" />
                <span>Verified Studio Listing</span>
              </div>
              <p style={{ fontSize: '13px', color: 'var(--color-text-secondary)', lineHeight: '1.5', margin: 0 }}>
                This production call has been validated for legitimacy. TrueHunt strictly bans pay-to-audition 
                schemes and preserves professional labor standards across film crafts.
              </p>
            </div>

          </aside>

        </div>
      </div>

      {/* Modal */}
      {showApplyModal && (
        <div className="modal-overlay" onClick={() => setShowApplyModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h3 style={{ fontSize: '20px', color: 'var(--color-navy)', marginBottom: '6px' }}>
              Apply for {job.title}
            </h3>
            <p style={{ fontSize: '13px', color: 'var(--color-text-muted)', marginBottom: '16px' }}>
              {job.productionCompany} • {job.location}
            </p>

            <form onSubmit={handleApply}>
              <div className="form-group">
                <label className="form-label">Full Name *</label>
                <input 
                  type="text" 
                  className="form-control" 
                  value={applicantName}
                  onChange={(e) => setApplicantName(e.target.value)}
                  placeholder="e.g. Meera Nambiar" 
                  required 
                />
              </div>

              <div className="form-group">
                <label className="form-label">Email Address *</label>
                <input 
                  type="email" 
                  className="form-control" 
                  value={applicantEmail}
                  onChange={(e) => setApplicantEmail(e.target.value)}
                  placeholder="meera@cinema.com" 
                  required 
                />
              </div>

              <div className="form-group">
                <label className="form-label">Showreel / Portfolio URL (YouTube, Vimeo, Behance) *</label>
                <input 
                  type="url" 
                  className="form-control" 
                  value={portfolioLink}
                  onChange={(e) => setPortfolioLink(e.target.value)}
                  placeholder="https://vimeo.com/showreel" 
                  required 
                />
              </div>

              <div className="form-group">
                <label className="form-label">Brief Pitch & Availability *</label>
                <textarea 
                  className="form-control" 
                  rows={3}
                  value={pitchNote}
                  onChange={(e) => setPitchNote(e.target.value)}
                  placeholder="Mention previous feature films, certifications, or character fit..."
                  required 
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '16px' }}>
                <button type="button" onClick={() => setShowApplyModal(false)} className="btn btn-outline-subtle">
                  Cancel
                </button>
                <button type="submit" disabled={submitting} className="btn btn-maroon">
                  {submitting ? 'Submitting...' : 'Submit Audition / Application'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}
