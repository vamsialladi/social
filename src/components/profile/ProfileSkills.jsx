import React, { useState } from 'react';
import { Plus, X, Award, Check } from 'lucide-react';
import { useNotification } from '../../context/NotificationContext';

export default function ProfileSkills({ skills = [], isOwner, onAddSkill, onRemoveSkill }) {
  const { showNotification } = useNotification();
  const [newSkill, setNewSkill] = useState('');
  const [isAdding, setIsAdding] = useState(false);

  const handleAdd = async (e) => {
    e.preventDefault();
    if (!newSkill.trim()) return;
    try {
      await onAddSkill(newSkill.trim());
      setNewSkill('');
      setIsAdding(false);
      showNotification(`Added "${newSkill.trim()}" to skills`, 'success');
    } catch (err) {
      showNotification('Failed to add skill', 'error');
    }
  };

  const handleRemove = async (skill) => {
    try {
      await onRemoveSkill(skill);
      showNotification(`Removed "${skill}"`, 'info');
    } catch (err) { 
      showNotification('Failed to remove skill', 'error');
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
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <div>
          <h3 style={{ fontSize: '19px', color: 'var(--color-navy)', margin: 0, fontWeight: 700 }}>
            Skills & Craft Expertise
          </h3>
          <span style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>
            Technical competencies, specialized cinema camera systems & software tools
          </span>
        </div>

        {isOwner && !isAdding && (
          <button 
            onClick={() => setIsAdding(true)}
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
            <span>Add Skill</span>
          </button>
        )}
      </div>

      {/* Owner Inline Add Form */}
      {isOwner && isAdding && (
        <form onSubmit={handleAdd} style={{ display: 'flex', gap: '8px', marginBottom: '18px', maxWidth: '420px' }}>
          <input 
            type="text"
            className="form-control"
            placeholder="e.g. DaVinci Resolve, RED V-Raptor, Steadicam"
            value={newSkill}
            onChange={(e) => setNewSkill(e.target.value)}
            autoFocus
          />
          <button type="submit" className="btn btn-sm btn-maroon" disabled={!newSkill.trim()}>
            Add
          </button>
          <button type="button" onClick={() => setIsAdding(false)} className="btn btn-sm btn-ghost">
            Cancel
          </button>
        </form>
      )}

      {/* Skill Chips List */}
      {skills.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '24px 16px', backgroundColor: 'var(--color-bg-page)', borderRadius: 'var(--radius-md)' }}>
          <p style={{ fontSize: '13px', color: 'var(--color-text-muted)', margin: 0 }}>
            {isOwner ? 'Add your camera systems, direction skills, software, or acting methods.' : 'No skills listed yet.'}
          </p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '10px' }}>
          {skills.map((skill, index) => (
            <div 
              key={index}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '8px',
                padding: '8px 14px',
                borderRadius: 'var(--radius-full)',
                backgroundColor: 'var(--color-bg-page)',
                border: '1px solid var(--color-border)',
                fontSize: '13px',
                fontWeight: 600,
                color: 'var(--color-navy)',
                transition: 'all var(--transition-fast)'
              }}
            >
              <Award size={14} color="var(--color-maroon)" />
              <span>{skill}</span>
              {isOwner && (
                <button
                  type="button"
                  onClick={() => handleRemove(skill)}
                  style={{
                    background: 'none',
                    border: 'none',
                    padding: 0,
                    marginLeft: '2px',
                    color: 'var(--color-text-muted)',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center'
                  }}
                  title={`Remove ${skill}`}
                >
                  <X size={14} />
                </button>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

