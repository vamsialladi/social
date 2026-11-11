import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Building2, MapPin, Calendar, Clock, Send, CheckCircle2, ChevronRight, Briefcase } from 'lucide-react';
import { useNotification } from '../../context/NotificationContext';
import { api } from '../../services/api';

export default function OpportunityCard({ job }) {
  const [showApplyModal, setShowApplyModal] = useState(false);
  const [applicantName, setApplicantName] = useState('');
  const [applicantEmail, setApplicantEmail] = useState('');
  const [portfolioLink, setPortfolioLink] = useState('');
  const [pitchNote, setPitchNote] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [applied, setApplied] = useState(false);
  const { showNotification } = useNotification();

  const handleApplySubmit = async (e) => {
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
      showNotification(`Application submitted for "${job.title}"! (Demo Sync)`, 'success');
    } catch (err) {
      showNotification('Could not submit application', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <div 
        className="card card-hoverable"
        style={{
          padding: '24px',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between',
          height: '100%',
          backgroundColor: 'var(--color-white)',
          borderRadius: 'var(--radius-md)',
          border: '1px solid var(--color-border)'
        }}
      >
        <div>
          {/* Header Row: Category Badge + Posted Date */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <span className="badge badge-maroon" style={{ fontWeight: 600 }}>
              {job.roleCategory}
            </span>
            <div style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '12px', color: 'var(--color-text-muted)' }}>
              <Clock size={13} />
              <span>{job.postedDate}</span>
            </div>
          </div>

          {/* Job Title */}
          <Link to={`/jobs/${job.id}`} style={{ textDecoration: 'none' }}>
            <h3 
              style={{
                fontSize: '18px',
                fontWeight: 700,
                color: 'var(--color-navy)',
                marginBottom: '8px',
                lineHeight: '1.3'
              }}
            >
              {job.title}
            </h3>
          </Link>

          {/* Production House & Location */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', marginBottom: '14px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy-surface)' }}>
              <Building2 size={15} color="var(--color-navy)" />
              <span>{job.productionCompany}</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', color: 'var(--color-text-muted)' }}>
              <MapPin size={14} />
              <span>{job.location}</span>
              <span>•</span>
              <span style={{ color: 'var(--color-brown-arrow)', fontWeight: 600 }}>{job.type}</span>
            </div>
          </div>

          {/* Description */}
          <p 
            style={{
              fontSize: '13px',
              color: 'var(--color-text-secondary)',
              lineHeight: '1.5',
              marginBottom: '16px',
              display: '-webkit-box',
              WebkitLineClamp: 3,
              WebkitBoxOrient: 'vertical',
              overflow: 'hidden'
            }}
          >
            {job.description}
          </p>

          {/* Pay/Budget tag if present */}
          {job.budgetOrPay && (
            <div style={{ marginBottom: '16px', fontSize: '12px', color: '#166534', backgroundColor: '#F0FDF4', padding: '4px 10px', borderRadius: 'var(--radius-xs)', display: 'inline-block', fontWeight: 600 }}>
              {job.budgetOrPay}
            </div>
          )}
        </div>

        {/* Card CTA Actions */}
        <div 
          style={{
            display: 'grid',
            gridTemplateColumns: '1fr 1fr',
            gap: '10px',
            paddingTop: '16px',
            borderTop: '1px solid var(--color-border)',
            marginTop: '8px'
          }}
        >
          <Link 
            to={`/jobs/${job.id}`} 
            className="btn btn-sm btn-outline-subtle"
            style={{ width: '100%' }}
          >
            <span>View Details</span>
          </Link>

          <button
            onClick={() => applied ? null : setShowApplyModal(true)}
            disabled={applied}
            className={`btn btn-sm ${applied ? 'btn-outline-subtle' : 'btn-maroon'}`}
            style={{ width: '100%' }}
          >
            {applied ? (
              <>
                <CheckCircle2 size={14} color="#15803D" />
                <span>Applied</span>
              </>
            ) : (
              <>
                <Send size={14} />
                <span>Express Interest</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* Application / Express Interest Modal */}
      {showApplyModal && (
        <div className="modal-overlay" onClick={() => setShowApplyModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div style={{ marginBottom: '16px', borderBottom: '1px solid var(--color-border)', paddingBottom: '12px' }}>
              <span className="badge badge-maroon" style={{ marginBottom: '6px' }}>{job.roleCategory}</span>
              <h3 style={{ fontSize: '20px', color: 'var(--color-navy)' }}>Apply for {job.title}</h3>
              <p style={{ fontSize: '13px', color: 'var(--color-text-muted)' }}>
                {job.productionCompany} • {job.location}
              </p>
            </div>

            <form onSubmit={handleApplySubmit}>
              <div className="form-group">
                <label className="form-label">Full Name *</label>
                <input 
                  type="text" 
                  className="form-control" 
                  value={applicantName}
                  onChange={(e) => setApplicantName(e.target.value)}
                  placeholder="e.g. Ramesh Kumar" 
                  required 
                />
              </div>

              <div className="form-group">
                <label className="form-label">Contact Email *</label>
                <input 
                  type="email" 
                  className="form-control" 
                  value={applicantEmail}
                  onChange={(e) => setApplicantEmail(e.target.value)}
                  placeholder="ramesh@studio.com" 
                  required 
                />
              </div>

              <div className="form-group">
                <label className="form-label">Portfolio / Showreel Link (Vimeo, YouTube, Drive) *</label>
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
                <label className="form-label">Why are you suitable for this role? *</label>
                <textarea 
                  className="form-control" 
                  rows={3}
                  value={pitchNote}
                  onChange={(e) => setPitchNote(e.target.value)}
                  placeholder="Mention previous film credits or relevant equipment experience..."
                  required 
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
                <button 
                  type="button" 
                  onClick={() => setShowApplyModal(false)}
                  className="btn btn-outline-subtle"
                >
                  Cancel
                </button>
                <button 
                  type="submit" 
                  disabled={submitting}
                  className="btn btn-maroon"
                >
                  {submitting ? 'Submitting...' : 'Submit Application'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  );
}
