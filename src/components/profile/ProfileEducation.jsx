import React from 'react';
import { Plus, GraduationCap, Pencil, Trash2, Calendar } from 'lucide-react';
import { useNotification } from '../../context/NotificationContext';

export default function ProfileEducation({ educations = [], isOwner, onAdd, onEdit, onDelete }) {
  const { showNotification } = useNotification();

  const handleDelete = async (id, institution) => {
    if (window.confirm(`Are you sure you want to remove education at "${institution}"?`)) {
      try {
        await onDelete(id);
        showNotification('Education record removed successfully', 'info');
      } catch (e) {
        showNotification('Failed to delete education', 'error');
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
          Education
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
            <span>Add Education</span>
          </button>
        )}
      </div>

      {educations.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '32px 16px', backgroundColor: 'var(--color-bg-page)', borderRadius: 'var(--radius-md)' }}>
          <GraduationCap size={32} color="var(--color-text-muted)" style={{ margin: '0 auto 8px auto', display: 'block', opacity: 0.6 }} />
          <h4 style={{ fontSize: '15px', color: 'var(--color-navy)', margin: '0 0 4px 0' }}>No education listed yet</h4>
          <p style={{ fontSize: '13px', color: 'var(--color-text-muted)', margin: 0 }}>
            {isOwner ? 'Add film school diplomas, masterclasses, or degree qualifications.' : 'No education history added.'}
          </p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '22px' }}>
          {educations.map((edu, index) => {
            const isLast = index === educations.length - 1;
            return (
              <div 
                key={edu.id || index}
                style={{
                  display: 'flex',
                  gap: '16px',
                  position: 'relative',
                  paddingBottom: isLast ? 0 : '18px',
                  borderBottom: isLast ? 'none' : '1px solid var(--color-border-subtle)'
                }}
              >
                {/* Institution Avatar Icon */}
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
                  <GraduationCap size={22} color="var(--color-navy)" />
                </div>

                {/* Details Content */}
                <div style={{ flex: 1 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <div>
                      <h4 style={{ fontSize: '16px', color: 'var(--color-navy)', margin: '0 0 2px 0', fontWeight: 700 }}>
                        {edu.institution}
                      </h4>
                      <div style={{ fontSize: '14px', color: 'var(--color-navy-surface)', fontWeight: 600 }}>
                        {edu.degree}
                        {edu.fieldOfStudy && (
                          <span style={{ fontWeight: 400, color: 'var(--color-text-muted)' }}>
                            {', '}{edu.fieldOfStudy}
                          </span>
                        )}
                      </div>
                    </div>

                    {isOwner && (
                      <div style={{ display: 'flex', gap: '4px' }}>
                        <button 
                          onClick={() => onEdit(edu)}
                          className="btn btn-sm btn-ghost"
                          style={{ padding: '6px', borderRadius: '50%', color: 'var(--color-navy)' }}
                          title="Edit education"
                        >
                          <Pencil size={15} />
                        </button>
                        <button 
                          onClick={() => handleDelete(edu.id, edu.institution)}
                          className="btn btn-sm btn-ghost"
                          style={{ padding: '6px', borderRadius: '50%', color: 'var(--color-maroon)' }}
                          title="Delete education"
                        >
                          <Trash2 size={15} />
                        </button>
                      </div>
                    )}
                  </div>

                  {/* Dates & Grade */}
                  <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '10px', fontSize: '12px', color: 'var(--color-text-muted)', margin: '4px 0 8px 0' }}>
                    {(edu.startDate || edu.endDate) && (
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                        <Calendar size={13} />
                        {edu.startDate} – {edu.endDate || 'Present'}
                      </span>
                    )}
                    {edu.grade && (
                      <>
                        <span>•</span>
                        <span>Grade: <strong>{edu.grade}</strong></span>
                      </>
                    )}
                  </div>

                  {/* Description */}
                  {edu.description && (
                    <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)', lineHeight: 1.6, margin: 0, whiteSpace: 'pre-line' }}>
                      {edu.description}
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

