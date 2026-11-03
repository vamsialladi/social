import React from 'react';
import { Plus, Film, ExternalLink, Pencil, Trash2, Calendar, Award } from 'lucide-react';
import { useNotification } from '../../context/NotificationContext';

export default function ProfileProjects({ projects = [], credits = [], isOwner, onAdd, onEdit, onDelete }) {
  const { showNotification } = useNotification();

  const handleDelete = async (id, title) => {
    if (window.confirm(`Are you sure you want to remove project "${title}"?`)) {
      try {
        await onDelete(id);
        showNotification('Project removed successfully', 'info');
      } catch (e) {
        showNotification('Failed to delete project', 'error');
      }
    }
  };

  const hasAnyItems = projects.length > 0 || credits.length > 0;

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
        <div>
          <h3 style={{ fontSize: '19px', color: 'var(--color-navy)', margin: 0, fontWeight: 700 }}>
            Projects & Filmography Credits
          </h3>
          <span style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>
            Theatrical releases, feature film credits, and creative production portfolios
          </span>
        </div>

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
            <span>Add Project</span>
          </button>
        )}
      </div>

      {!hasAnyItems ? (
        <div style={{ textAlign: 'center', padding: '32px 16px', backgroundColor: 'var(--color-bg-page)', borderRadius: 'var(--radius-md)' }}>
          <Film size={32} color="var(--color-text-muted)" style={{ margin: '0 auto 8px auto', display: 'block', opacity: 0.6 }} />
          <h4 style={{ fontSize: '15px', color: 'var(--color-navy)', margin: '0 0 4px 0' }}>No projects or film credits listed</h4>
          <p style={{ fontSize: '13px', color: 'var(--color-text-muted)', margin: 0 }}>
            {isOwner ? 'Add feature films, commercials, or portfolio links to showcase your reel.' : 'No projects listed.'}
          </p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
          
          {/* Projects List */}
          {projects.map((proj, idx) => (
            <div 
              key={proj.id || idx}
              style={{
                padding: '18px',
                borderRadius: 'var(--radius-md)',
                backgroundColor: 'var(--color-bg-page)',
                border: '1px solid var(--color-border)'
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
                  <div 
                    style={{
                      width: '40px',
                      height: '40px',
                      borderRadius: 'var(--radius-sm)',
                      backgroundColor: 'var(--color-navy)',
                      color: 'white',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      flexShrink: 0,
                      marginTop: '2px'
                    }}
                  >
                    <Film size={20} />
                  </div>
                  <div>
                    <h4 style={{ fontSize: '16px', color: 'var(--color-navy)', margin: '0 0 2px 0', fontWeight: 700 }}>
                      {proj.title}
                    </h4>
                    {(proj.startDate || proj.endDate) && (
                      <span style={{ fontSize: '12px', color: 'var(--color-text-muted)', display: 'block', marginBottom: '6px' }}>
                        {proj.startDate} – {proj.endDate || 'Present'}
                      </span>
                    )}
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  {proj.projectUrl && (
                    <a 
                      href={proj.projectUrl} 
                      target="_blank" 
                      rel="noreferrer"
                      className="btn btn-sm btn-ghost"
                      style={{ padding: '6px', color: 'var(--color-info)' }}
                      title="Open external link"
                    >
                      <ExternalLink size={16} />
                    </a>
                  )}
                  {isOwner && (
                    <>
                      <button 
                        onClick={() => onEdit(proj)}
                        className="btn btn-sm btn-ghost"
                        style={{ padding: '6px', borderRadius: '50%', color: 'var(--color-navy)' }}
                        title="Edit project"
                      >
                        <Pencil size={15} />
                      </button>
                      <button 
                        onClick={() => handleDelete(proj.id, proj.title)}
                        className="btn btn-sm btn-ghost"
                        style={{ padding: '6px', borderRadius: '50%', color: 'var(--color-maroon)' }}
                        title="Delete project"
                      >
                        <Trash2 size={15} />
                      </button>
                    </>
                  )}
                </div>
              </div>

              {proj.technologies && (
                <div style={{ margin: '8px 0', fontSize: '13px', color: 'var(--color-maroon)', fontWeight: 600 }}>
                  Craft & Tech: <span style={{ fontWeight: 400, color: 'var(--color-text-secondary)' }}>{proj.technologies}</span>
                </div>
              )}

              {proj.description && (
                <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)', lineHeight: 1.6, margin: '6px 0 0 0' }}>
                  {proj.description}
                </p>
              )}
            </div>
          ))}

          {/* Film Credits List (if any from talent profile) */}
          {credits.length > 0 && projects.length === 0 && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {credits.map((c, i) => (
                <div 
                  key={i}
                  style={{
                    padding: '14px 18px',
                    borderRadius: 'var(--radius-sm)',
                    backgroundColor: 'var(--color-bg-page)',
                    border: '1px solid var(--color-border)',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <Film size={18} color="var(--color-navy)" />
                    <div>
                      <h5 style={{ fontSize: '15px', color: 'var(--color-navy)', margin: 0, fontWeight: 700 }}>{c.title}</h5>
                      <span style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>Role: {c.role}</span>
                    </div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <span className="badge badge-maroon" style={{ fontSize: '11px', display: 'block', marginBottom: '2px' }}>{c.format}</span>
                    <span style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>{c.year}</span>
                  </div>
                </div>
              ))}
            </div>
          )}

        </div>
      )}
    </div>
  );
}

