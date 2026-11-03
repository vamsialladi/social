import React, { useState } from 'react';
import { X, Image as ImageIcon, Send, Loader2 } from 'lucide-react';
import { useNotification } from '../../context/NotificationContext';

export default function PostModal({ author, onClose, onPostCreated }) {
  const { showNotification } = useNotification();
  const [loading, setLoading] = useState(false);
  const [content, setContent] = useState('');
  const [mediaUrl, setMediaUrl] = useState('');
  const [showMediaInput, setShowMediaInput] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!content.trim()) {
      showNotification('Post content cannot be empty', 'error');
      return;
    }
    setLoading(true);
    try {
      await onPostCreated({
        content: content.trim(),
        mediaUrl: mediaUrl.trim() || null,
        mediaType: mediaUrl.trim() ? 'image' : null
      });
      showNotification('Post shared with your network!', 'success');
      onClose();
    } catch (err) {
      showNotification(err.message || 'Failed to publish post', 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose} style={{ zIndex: 1100 }}>
      <div 
        className="modal-content" 
        onClick={(e) => e.stopPropagation()} 
        style={{ maxWidth: '580px', maxHeight: '90vh', overflowY: 'auto', padding: '24px', borderRadius: 'var(--radius-lg)' }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', borderBottom: '1px solid var(--color-border)', paddingBottom: '12px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <img 
              src={author?.avatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80'} 
              alt={author?.name} 
              style={{ width: '42px', height: '42px', borderRadius: '50%', objectFit: 'cover' }}
            />
            <div>
              <h4 style={{ fontSize: '15px', color: 'var(--color-navy)', margin: 0, fontWeight: 700 }}>
                {author?.name || 'Cinema Professional'}
              </h4>
              <span style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>
                Post to Anyone in Film Network
              </span>
            </div>
          </div>
          <button type="button" onClick={onClose} className="btn btn-sm btn-ghost" style={{ padding: '6px', borderRadius: '50%' }}>
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <textarea 
            rows={5}
            className="form-control"
            style={{ 
              border: 'none', 
              boxShadow: 'none', 
              fontSize: '15px', 
              lineHeight: 1.6, 
              padding: '8px 0', 
              resize: 'none',
              outline: 'none'
            }}
            placeholder="What project, casting update, festival win, or cinema insight do you want to share with the industry?"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            autoFocus
          />

          {showMediaInput && (
            <div style={{ marginTop: '12px', padding: '12px', backgroundColor: 'var(--color-bg-page)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--color-border)' }}>
              <label style={{ fontSize: '12px', fontWeight: 600, color: 'var(--color-navy)', display: 'block', marginBottom: '6px' }}>
                Image / Behind-The-Scenes URL:
              </label>
              <input 
                type="url"
                className="form-control"
                placeholder="https://images.unsplash.com/photo-..."
                value={mediaUrl}
                onChange={(e) => setMediaUrl(e.target.value)}
              />
              {mediaUrl && (
                <div style={{ marginTop: '10px', height: '140px', borderRadius: 'var(--radius-sm)', overflow: 'hidden' }}>
                  <img src={mediaUrl} alt="Preview" style={{ width: '100%', height: '100%', objectFit: 'cover' }} onError={() => showNotification('Could not load image preview from URL', 'warning')} />
                </div>
              )}
            </div>
          )}

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '18px', borderTop: '1px solid var(--color-border)', paddingTop: '14px' }}>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button 
                type="button" 
                onClick={() => setShowMediaInput(!showMediaInput)}
                className={`btn btn-sm ${showMediaInput ? 'btn-outline-navy' : 'btn-ghost'}`}
                style={{ fontSize: '13px', display: 'inline-flex', alignItems: 'center', gap: '6px' }}
              >
                <ImageIcon size={16} /> Add Media
              </button>
            </div>

            <button 
              type="submit" 
              className="btn btn-maroon" 
              disabled={loading || !content.trim()}
              style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', borderRadius: 'var(--radius-full)', padding: '8px 20px' }}
            >
              {loading ? (
                <>
                  <Loader2 size={16} className="spin-animation" /> Posting...
                </>
              ) : (
                <>
                  <Send size={15} /> Post
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

