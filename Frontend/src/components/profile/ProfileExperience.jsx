import React from 'react';
import { Plus, Briefcase, Pencil, Trash2, Calendar, MapPin } from 'lucide-react';
import { useNotification } from '../../context/NotificationContext';

export default function ProfileExperience({ experiences = [], isOwner, onAdd, onEdit, onDelete }) {
  const { showNotification } = useNotification();

  const handleDelete = async (id, title) => {
    if (window.confirm(`Are you sure you want to remove "${title}" from your experience history?`)) {
      try {
        await onDelete(id);
        showNotification('Experience removed successfully', 'info');
      } catch (e) {
        showNotification('Failed to delete experience', 'error');
      }
    }
  };

  return (
    <div 
      className="card"
      style={{
        borderRadius: 'var(--radius-lg)',
        padding: '24px 28px',
        backgroundColor: 'var(--color-white)',
        border: '1px solid var(--color-border)',
        boxShadow: 'var(--shadow-card)',
        marginBottom: '20px'
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
        <h3 style={{ fontSize: '19px', color: 'var(--color-navy)', margin: 0, fontWeight: 700 }}>
          Experience
        </h3>

        {isOwner && (
          <button 
            onClick={onAdd}
            className="btn btn-sm btn-ghost"
            style={{ 
              display: 'inline-flex', 
              alignItems: 'center', 
              gap: '6px', 
              color: 'var(--color-maroon)', 
              fontWeight: 600,
              padding: '6px 12px',
              borderRadius: 'var(--radius-full)'
            }}
          >
            <Plus size={16} />
            <span>Add Experience</span>
          </button>
        )}
      </div>

      {experiences.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '32px 16px', backgroundColor: 'var(--color-bg-page)', borderRadius: 'var(--radius-md)' }}>
          <Briefcase size={32} color="var(--color-text-muted)" style={{ margin: '0 auto 8px auto', display: 'block', opacity: 0.6 }} />
          <h4 style={{ fontSize: '15px', color: 'var(--color-navy)', margin: '0 0 4px 0' }}>No experience listed yet</h4>
          <p style={{ fontSize: '13px', color: 'var(--color-text-muted)', margin: 0 }}>
            {isOwner ? 'Add your production credits, camera department roles, or studio contracts.' : 'No employment history added.'}
          </p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '22px' }}>
          {experiences.map((exp, index) => {
            const isLast = index === experiences.length - 1;
            return (
              <div 
                key={exp.id || index}
                style={{
                  display: 'flex',
                  gap: '16px',
                  position: 'relative',
                  paddingBottom: isLast ? 0 : '18px',
                  borderBottom: isLast ? 'none' : '1px solid var(--color-border-subtle)'
                }}
              >
                {/* Company / Craft Avatar Icon */}
                <div 
                  style={{
                    width: '46px',
                    height: '46px',
                    borderRadius: 'var(--radius-sm)',
                    backgroundColor: 'var(--color-bg-page)',
                    border: '1px solid var(--color-border)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: 'var(--color-navy)',
                    flexShrink: 0
                  }}
                >
                  {exp.companyLogoUrl ? (
                    <img src={exp.companyLogoUrl} alt={exp.company} style={{ width: '100%', height: '100%', objectFit: 'contain' }} />
                  ) : (
                    <Briefcase size={22} color="var(--color-navy)" />
                  )}
                </div>

                {/* Details Content */}
                <div style={{ flex: 1 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <div>
                      <h4 style={{ fontSize: '16px', color: 'var(--color-navy)', margin: '0 0 2px 0', fontWeight: 700 }}>
                        {exp.title}
                      </h4>
                      <div style={{ fontSize: '14px', color: 'var(--color-navy-surface)', fontWeight: 600 }}>
                        {exp.company}
                        {exp.employmentType && (
                          <span style={{ fontWeight: 400, color: 'var(--color-text-muted)' }}>
                            {' '}· {exp.employmentType}
                          </span>
                        )}
                      </div>
                    </div>

                    {isOwner && (
                      <div style={{ display: 'flex', gap: '4px' }}>
                        <button 
                          onClick={() => onEdit(exp)}
                          className="btn btn-sm btn-ghost"
                          style={{ padding: '6px', borderRadius: '50%', color: 'var(--color-navy)' }}
                          title="Edit experience"
                        >
                          <Pencil size={15} />
                        </button>
                        <button 
                          onClick={() => handleDelete(exp.id, exp.title)}
                          className="btn btn-sm btn-ghost"
                          style={{ padding: '6px', borderRadius: '50%', color: 'var(--color-maroon)' }}
                          title="Delete experience"
                        >
                          <Trash2 size={15} />
                        </button>
                      </div>
                    )}
                  </div>

                  {/* Dates & Location */}
                  <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '10px', fontSize: '12px', color: 'var(--color-text-muted)', margin: '4px 0 8px 0' }}>
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                      <Calendar size={13} />
                      {exp.startDate} – {exp.isCurrent ? 'Present' : exp.endDate || 'Present'}
                    </span>
                    {exp.location && (
                      <>
                        <span>•</span>
                        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                          <MapPin size={13} />
                          {exp.location}
                        </span>
                      </>
                    )}
                  </div>

                  {/* Description */}
                  {exp.description && (
                    <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)', lineHeight: 1.6, margin: 0, whiteSpace: 'pre-line' }}>
                      {exp.description}
                    </p>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

