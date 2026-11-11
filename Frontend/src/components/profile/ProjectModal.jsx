import React, { useState } from 'react';
import { X, Film, Loader2 } from 'lucide-react';
import { useNotification } from '../../context/NotificationContext';

export default function ProjectModal({ initialData, onClose, onSave }) {
  const { showNotification } = useNotification();
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    title: initialData?.title || '',
    technologies: initialData?.technologies || '',
    projectUrl: initialData?.projectUrl || '',
    startDate: initialData?.startDate || '',
    endDate: initialData?.endDate || '',
    description: initialData?.description || ''
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.title.trim()) {
      showNotification('Project title is required', 'error');
      return;
    }
    setLoading(true);
    try {
      await onSave(formData);
      showNotification(initialData ? 'Project updated!' : 'Project added!', 'success');
      onClose();
    } catch (err) {
      showNotification(err.message || 'Failed to save project', 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose} style={{ zIndex: 1100 }}>
      <div 
        className="modal-content" 
        onClick={(e) => e.stopPropagation()} 
        style={{ maxWidth: '580px', maxHeight: '90vh', overflowY: 'auto', padding: '28px', borderRadius: 'var(--radius-lg)' }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px', borderBottom: '1px solid var(--color-border)', paddingBottom: '12px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div style={{ width: '36px', height: '36px', borderRadius: 'var(--radius-sm)', backgroundColor: 'var(--color-navy-transparent)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--color-navy)' }}>
              <Film size={20} />
            </div>
            <div>
              <h3 style={{ fontSize: '18px', color: 'var(--color-navy)', margin: 0 }}>
                {initialData ? 'Edit Project / Film Credit' : 'Add Project / Film Credit'}
              </h3>
              <p style={{ fontSize: '12px', color: 'var(--color-text-muted)', margin: '2px 0 0 0' }}>
                Feature films, OTT series, short films, or creative projects.
              </p>
            </div>
          </div>
          <button type="button" onClick={onClose} className="btn btn-sm btn-ghost" style={{ padding: '6px', borderRadius: '50%' }}>
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group" style={{ marginBottom: '14px' }}>
            <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>Project / Film Title *</label>
            <input 
              type="text" 
              required
              className="form-control" 
              placeholder="e.g. Shadows of Malabar (Feature Film)"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
            />
          </div>

          <div className="form-group" style={{ marginBottom: '14px' }}>
            <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>Craft, Gear or Technologies</label>
            <input 
              type="text" 
              className="form-control" 
              placeholder="e.g. ARRI Alexa LF, Master Anamorphics, DaVinci Resolve"
              value={formData.technologies}
              onChange={(e) => setFormData({ ...formData, technologies: e.target.value })}
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px', marginBottom: '14px' }}>
            <div className="form-group" style={{ margin: 0 }}>
              <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>Start Year / Period</label>
              <input 
                type="text" 
                className="form-control" 
                placeholder="e.g. 2024"
                value={formData.startDate}
                onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
              />
            </div>
            <div className="form-group" style={{ margin: 0 }}>
              <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>Release / End Year</label>
              <input 
                type="text" 
                className="form-control" 
                placeholder="e.g. 2025"
                value={formData.endDate}
                onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
              />
            </div>
          </div>

          <div className="form-group" style={{ marginBottom: '14px' }}>
            <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>Project / Trailer / IMDb URL</label>
            <input 
              type="url" 
              className="form-control" 
              placeholder="https://imdb.com/title/... or YouTube trailer"
              value={formData.projectUrl}
              onChange={(e) => setFormData({ ...formData, projectUrl: e.target.value })}
            />
          </div>

          <div className="form-group" style={{ marginBottom: '20px' }}>
            <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>Description & Critical Recognition</label>
            <textarea 
              rows={3}
              className="form-control" 
              placeholder="Highlight shooting schedule, festival selections, box office response, or artistic achievements..."
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', borderTop: '1px solid var(--color-border)', paddingTop: '14px' }}>
            <button type="button" onClick={onClose} className="btn btn-outline-subtle" disabled={loading}>
              Cancel
            </button>
            <button type="submit" className="btn btn-maroon" disabled={loading}>
              {loading ? (
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                  <Loader2 size={15} className="spin-animation" /> Saving...
                </span>
              ) : (
                'Save Project'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

