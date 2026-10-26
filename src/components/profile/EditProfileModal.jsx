import React, { useState } from 'react';
import { X, Camera, Image as ImageIcon, Loader2 } from 'lucide-react';
import { useNotification } from '../../context/NotificationContext';

export default function EditProfileModal({ profile, onClose, onSave }) {
  const { showNotification } = useNotification();
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    name: profile?.name || '',
    headline: profile?.headline || '',
    role: profile?.role || '',
    category: profile?.category || 'Cinematography',
    company: profile?.company || '',
    location: profile?.location || '',
    industry: profile?.industry || 'Motion Pictures & Film',
    websiteUrl: profile?.websiteUrl || '',
    experience: profile?.experience || '5+ Years',
    availability: profile?.availability || 'Available for Projects',
    bio: profile?.bio || '',
    avatar: profile?.avatar || '',
    coverImage: profile?.coverImage || ''
  });

  const categories = [
    'Acting',
    'Direction',
    'Cinematography',
    'Editing',
    'Screenwriting',
    'Art Direction',
    'Sound Design',
    'Music',
    'Dance & Choreography',
    'Production Management',
    'Makeup & Costume',
    'VFX & Animation'
  ];

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.name.trim()) {
      showNotification('Full name is required', 'error');
      return;
    }
    setLoading(true);
    try {
      await onSave(formData);
      showNotification('Profile updated successfully!', 'success');
      onClose();
    } catch (err) {
      showNotification(err.message || 'Failed to update profile', 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose} style={{ zIndex: 1100 }}>
      <div 
        className="modal-content" 
        onClick={(e) => e.stopPropagation()} 
        style={{ 
          maxWidth: '680px', 
          maxHeight: '90vh', 
          overflowY: 'auto',
          padding: '28px',
          borderRadius: 'var(--radius-lg)'
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', borderBottom: '1px solid var(--color-border)', paddingBottom: '14px' }}>
          <div>
            <h3 style={{ fontSize: '20px', color: 'var(--color-navy)', margin: 0 }}>Edit Profile Introduction</h3>
            <p style={{ fontSize: '13px', color: 'var(--color-text-muted)', margin: '4px 0 0 0' }}>
              Keep your professional identity and contact details up to date for recruiters and collaborators.
            </p>
          </div>
          <button 
            type="button" 
            onClick={onClose} 
            className="btn btn-sm btn-ghost" 
            style={{ padding: '6px', borderRadius: '50%' }}
            aria-label="Close"
          >
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          {/* Photos URL preview */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '18px' }}>
            <div className="form-group" style={{ margin: 0 }}>
              <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>
                <Camera size={13} style={{ verticalAlign: 'middle', marginRight: '4px' }} /> Profile Photo URL
              </label>
              <input 
                type="url" 
                className="form-control" 
                placeholder="https://images.unsplash.com/..." 
                value={formData.avatar}
                onChange={(e) => setFormData({ ...formData, avatar: e.target.value })}
              />
            </div>
            <div className="form-group" style={{ margin: 0 }}>
              <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>
                <ImageIcon size={13} style={{ verticalAlign: 'middle', marginRight: '4px' }} /> Cover Banner URL
              </label>
              <input 
                type="url" 
                className="form-control" 
                placeholder="https://images.unsplash.com/..." 
                value={formData.coverImage}
                onChange={(e) => setFormData({ ...formData, coverImage: e.target.value })}
              />
            </div>
          </div>

          {/* Full Name & Headline */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '16px' }}>
            <div className="form-group" style={{ margin: 0 }}>
              <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>
                Full Name *
              </label>
              <input 
                type="text" 
                required 
                className="form-control" 
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              />
            </div>
            <div className="form-group" style={{ margin: 0 }}>
              <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>
                Primary Craft / Category
              </label>
              <select 
                className="form-control"
                value={formData.category}
                onChange={(e) => setFormData({ ...formData, category: e.target.value })}
              >
                {categories.map((c) => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Headline */}
          <div className="form-group" style={{ marginBottom: '16px' }}>
            <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>
              Professional Headline
            </label>
            <input 
              type="text" 
              className="form-control" 
              placeholder="e.g. Award-Winning Cinematographer (DoP) | ISC Member | Feature Films"
              value={formData.headline}
              onChange={(e) => setFormData({ ...formData, headline: e.target.value })}
            />
            <span style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>
              Appears right below your name across TrueHunt search and connections.
            </span>
          </div>

          {/* Current Role & Company */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '16px' }}>
            <div className="form-group" style={{ margin: 0 }}>
              <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>
                Current Role / Title *
              </label>
              <input 
                type="text" 
                required 
                className="form-control" 
                placeholder="e.g. Cinematographer (DoP)"
                value={formData.role}
                onChange={(e) => setFormData({ ...formData, role: e.target.value })}
              />
            </div>
            <div className="form-group" style={{ margin: 0 }}>
              <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>
                Current Studio / Production Company
              </label>
              <input 
                type="text" 
                className="form-control" 
                placeholder="e.g. Aura Cinema Works & Netflix India"
                value={formData.company}
                onChange={(e) => setFormData({ ...formData, company: e.target.value })}
              />
            </div>
          </div>

          {/* Location & Industry */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '16px' }}>
            <div className="form-group" style={{ margin: 0 }}>
              <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>
                Location (City, State/Country)
              </label>
              <input 
                type="text" 
                className="form-control" 
                placeholder="e.g. Mumbai, Maharashtra, India"
                value={formData.location}
                onChange={(e) => setFormData({ ...formData, location: e.target.value })}
              />
            </div>
            <div className="form-group" style={{ margin: 0 }}>
              <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>
                Availability Status
              </label>
              <input 
                type="text" 
                className="form-control" 
                placeholder="e.g. Available from Nov 2026 / Open for Auditions"
                value={formData.availability}
                onChange={(e) => setFormData({ ...formData, availability: e.target.value })}
              />
            </div>
          </div>

          {/* Bio / About */}
          <div className="form-group" style={{ marginBottom: '16px' }}>
            <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>
              About / Professional Biography
            </label>
            <textarea 
              rows={4}
              className="form-control" 
              placeholder="Highlight your cinema journey, directorial vision, key gear specializations, or theatrical achievements..."
              value={formData.bio}
              onChange={(e) => setFormData({ ...formData, bio: e.target.value })}
            />
          </div>

          {/* Website / Portfolio Link */}
          <div className="form-group" style={{ marginBottom: '24px' }}>
            <label className="form-label" style={{ fontSize: '12px', fontWeight: 600 }}>
              Portfolio / Website Link
            </label>
            <input 
              type="url" 
              className="form-control" 
              placeholder="https://yourportfolio.com or IMDb profile"
              value={formData.websiteUrl}
              onChange={(e) => setFormData({ ...formData, websiteUrl: e.target.value })}
            />
          </div>

          {/* Footer Actions */}
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', borderTop: '1px solid var(--color-border)', paddingTop: '16px' }}>
            <button 
              type="button" 
              onClick={onClose} 
              className="btn btn-outline-subtle" 
              disabled={loading}
            >
              Cancel
            </button>
            <button 
              type="submit" 
              className="btn btn-maroon" 
              disabled={loading}
              style={{ minWidth: '110px', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }}
            >
              {loading ? (
                <>
                  <Loader2 size={16} className="spin-animation" />
                  <span>Saving...</span>
                </>
              ) : (
                'Save Profile'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

