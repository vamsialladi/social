import React, { useState } from 'react';
import { X, GraduationCap, Loader2 } from 'lucide-react';
import { useNotification } from '../../context/NotificationContext';

export default function EducationModal({ initialData, onClose, onSave }) {
  const { showNotification } = useNotification();
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    institution: initialData?.institution || '',
    degree: initialData?.degree || '',
    fieldOfStudy: initialData?.fieldOfStudy || '',
    startDate: initialData?.startDate || '',
    endDate: initialData?.endDate || '',
    grade: initialData?.grade || '',
    activities: initialData?.activities || '',
    description: initialData?.description || ''
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.institution.trim() || !formData.degree.trim()) {
      showNotification('School/Institution and Degree are required', 'error');
      return;
    }
    setLoading(true);
    try {
      await onSave(formData);
      showNotification(initialData ? 'Education updated!' : 'Education added!', 'success');
      onClose();
    } catch (err) {
      showNotification(err.message || 'Failed to save education', 'error');
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
              <GraduationCap size={20} />
            </div>
            <div>
              <h3 style={{ fontSize: '18px', color: 'var(--color-navy)', margin: 0 }}>
                {initialData ? 'Edit Education' : 'Add Education'}
              </h3>
              <p style={{ fontSize: '12px', color: 'var(--color-text-muted)', margin: '2px 0 0 0' }}>
                Film schools, university degrees, or certified craft diplomas.
              </p>
            </div>
          </div>
          <button type="button" onClick={onClose} className="btn btn-sm btn-ghost" style={{ padding: '6px', borderRadius: '50%' }}>
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group" style={{ marginBottom: '14px' }}>
            <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>School / Institution *</label>
            <input 
              type="text" 
              required
              className="form-control" 
              placeholder="e.g. Film and Television Institute of India (FTII), Whistling Woods"
              value={formData.institution}
              onChange={(e) => setFormData({ ...formData, institution: e.target.value })}
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px', marginBottom: '14px' }}>
            <div className="form-group" style={{ margin: 0 }}>
              <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>Degree / Diploma *</label>
              <input 
                type="text" 
                required
                className="form-control" 
                placeholder="e.g. Post-Graduate Diploma, Bachelor of Fine Arts"
                value={formData.degree}
                onChange={(e) => setFormData({ ...formData, degree: e.target.value })}
              />
            </div>
            <div className="form-group" style={{ margin: 0 }}>
              <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>Field of Study</label>
              <input 
                type="text" 
                className="form-control" 
                placeholder="e.g. Cinematography, Screen Acting, Sound"
                value={formData.fieldOfStudy}
                onChange={(e) => setFormData({ ...formData, fieldOfStudy: e.target.value })}
              />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px', marginBottom: '14px' }}>
            <div className="form-group" style={{ margin: 0 }}>
              <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>Start Year</label>
              <input 
                type="text" 
                className="form-control" 
                placeholder="e.g. 2015"
                value={formData.startDate}
                onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
              />
            </div>
            <div className="form-group" style={{ margin: 0 }}>
              <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>End Year (or Expected)</label>
              <input 
                type="text" 
                className="form-control" 
                placeholder="e.g. 2018"
                value={formData.endDate}
                onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
              />
            </div>
          </div>

          <div className="form-group" style={{ marginBottom: '14px' }}>
            <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>Grade / Honors</label>
            <input 
              type="text" 
              className="form-control" 
              placeholder="e.g. First Class with Distinction, Magna Cum Laude"
              value={formData.grade}
              onChange={(e) => setFormData({ ...formData, grade: e.target.value })}
            />
          </div>

          <div className="form-group" style={{ marginBottom: '20px' }}>
            <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>Activities & Societies / Description</label>
            <textarea 
              rows={3}
              className="form-control" 
              placeholder="Thesis film direction, student film festival curation, specialized camera labs..."
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
                'Save Education'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

