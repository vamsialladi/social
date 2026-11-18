import { useNavigate } from 'react-router-dom';
import { Building2, MapPin, Clock, Send } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export default function OpportunityCard({ job }) {
  const navigate = useNavigate();
  const { isAuthenticated, loading: authLoading } = useAuth();

  const openOpportunity = () => {
    const returnTo = job.source === 'social'
      ? `/social/opportunities?opportunityId=${encodeURIComponent(job.socialPostId)}`
      : `/jobs/${job.id}`;
    if (!isAuthenticated) {
      navigate('/sign-in', { state: { returnTo } });
      return;
    }

    navigate(returnTo);
  };

  return (
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
          <button
            type="button"
            onClick={openOpportunity}
            disabled={authLoading}
            style={{
              display: 'block',
              width: '100%',
              padding: 0,
              border: 0,
              background: 'none',
              cursor: authLoading ? 'wait' : 'pointer',
              textAlign: 'left',
            }}
          >
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
          </button>

          {/* Production House & Location */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', marginBottom: '14px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy-surface)' }}>
              <Building2 size={15} color="var(--color-navy)" />
              <span>{job.source === 'social' ? `Posted by ${job.productionCompany}` : job.productionCompany}</span>
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
          <button 
            type="button"
            onClick={openOpportunity}
            disabled={authLoading}
            className="btn btn-sm btn-outline-subtle"
            style={{ width: '100%' }}
          >
            <span>View Details</span>
          </button>

          <button
            onClick={openOpportunity}
            disabled={authLoading}
            className="btn btn-sm btn-maroon"
            style={{ width: '100%' }}
          >
            <Send size={14} />
            <span>Express Interest</span>
          </button>
        </div>
    </div>
  );
}
