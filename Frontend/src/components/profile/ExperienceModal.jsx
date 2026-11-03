import React, { useState } from 'react';
import { X, Briefcase, Loader2 } from 'lucide-react';
import { useNotification } from '../../context/NotificationContext';

export default function ExperienceModal({ initialData, onClose, onSave }) {
  const { showNotification } = useNotification();
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    title: initialData?.title || '',
    company: initialData?.company || '',
    employmentType: initialData?.employmentType || 'Full-time',
    location: initialData?.location || '',
    startDate: initialData?.startDate || '',
    endDate: initialData?.endDate || '',
    isCurrent: initialData?.isCurrent || false,
    description: initialData?.description || '',
    companyLogoUrl: initialData?.companyLogoUrl || ''
  });

  const employmentTypes = ['Full-time', 'Part-time', 'Contract', 'Freelance', 'Internship', 'Seasonal'];

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.title.trim() || !formData.company.trim() || !formData.startDate.trim()) {
      showNotification('Title, Company, and Start Date are required', 'error');
      return;
    }
    setLoading(true);
    try {
      await onSave(formData);
      showNotification(initialData ? 'Experience updated!' : 'Experience added!', 'success');
      onClose();
    } catch (err) {
      showNotification(err.message || 'Failed to save experience', 'error');
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
              <Briefcase size={20} />
            </div>
            <div>
              <h3 style={{ fontSize: '18px', color: 'var(--color-navy)', margin: 0 }}>
                {initialData ? 'Edit Experience' : 'Add Experience'}
              </h3>
              <p style={{ fontSize: '12px', color: 'var(--color-text-muted)', margin: '2px 0 0 0' }}>
                Showcase film shoots, studio roles, and production engagements.
              </p>
            </div>
          </div>
          <button type="button" onClick={onClose} className="btn btn-sm btn-ghost" style={{ padding: '6px', borderRadius: '50%' }}>
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group" style={{ marginBottom: '14px' }}>
            <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>Title / Role *</label>
            <input 
              type="text" 
              required
              className="form-control" 
              placeholder="e.g. Director of Photography (DoP), 1st AD, Lead Actor"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px', marginBottom: '14px' }}>
            <div className="form-group" style={{ margin: 0 }}>
              <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>Company / Production Banner *</label>
              <input 
                type="text" 
                required
                className="form-control" 
                placeholder="e.g. Red Chillies VFX, Netflix India"
                value={formData.company}
                onChange={(e) => setFormData({ ...formData, company: e.target.value })}
              />
            </div>
            <div className="form-group" style={{ margin: 0 }}>
              <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>Employment Type</label>
              <select 
                className="form-control"
                value={formData.employmentType}
                onChange={(e) => setFormData({ ...formData, employmentType: e.target.value })}
              >
                {employmentTypes.map(t => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
          </div>

          <div className="form-group" style={{ marginBottom: '14px' }}>
            <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>Location</label>
            <input 
              type="text" 
              className="form-control" 
              placeholder="e.g. Mumbai, India · On-site / Remote"
              value={formData.location}
              onChange={(e) => setFormData({ ...formData, location: e.target.value })}
            />
          </div>

          {/* Dates */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px', marginBottom: '12px' }}>
            <div className="form-group" style={{ margin: 0 }}>
              <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>Start Date *</label>
              <input 
                type="text" 
                required
                className="form-control" 
                placeholder="e.g. Jan 2024"
                value={formData.startDate}
                onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
              />
            </div>
            <div className="form-group" style={{ margin: 0 }}>
              <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>End Date</label>
              <input 
                type="text" 
                className="form-control" 
                placeholder={formData.isCurrent ? 'Present' : 'e.g. Dec 2025'}
                disabled={formData.isCurrent}
                value={formData.isCurrent ? 'Present' : formData.endDate}
                onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
              />
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '14px' }}>
            <input 
              type="checkbox" 
              id="isCurrentRole"
              checked={formData.isCurrent}
              onChange={(e) => setFormData({ 
                ...formData, 
                isCurrent: e.target.checked,
                endDate: e.target.checked ? 'Present' : ''
              })}
              style={{ width: '16px', height: '16px', accentColor: 'var(--color-maroon)' }}
            />
            <label htmlFor="isCurrentRole" style={{ fontSize: '13px', color: 'var(--color-navy)', cursor: 'pointer', userSelect: 'none' }}>
              I am currently working in this role
            </label>
          </div>

          <div className="form-group" style={{ marginBottom: '20px' }}>
            <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>Description & Key Achievements</label>
            <textarea 
              rows={3}
              className="form-control" 
              placeholder="Detail key responsibilities, cameras or tools used, team size, theatrical or OTT distribution details..."
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
                'Save Experience'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

