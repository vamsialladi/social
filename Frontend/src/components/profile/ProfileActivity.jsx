import React, { useState } from 'react';
import { 
  Plus, 
  Heart, 
  MessageCircle, 
  Share2, 
  Image as ImageIcon, 
  Film, 
  Send, 
  Clock, 
  Sparkles 
} from 'lucide-react';
import { useNotification } from '../../context/NotificationContext';

export default function ProfileActivity({ profile, isOwner, onOpenPostModal }) {
  const { showNotification } = useNotification();
  const [activeTab, setActiveTab] = useState('posts');
  const [likedPosts, setLikedPosts] = useState({});

  const posts = profile?.posts || [];
  const mediaPosts = posts.filter(p => p.mediaUrl);

  const displayedPosts = activeTab === 'media' ? mediaPosts : posts;

  const handleLike = (postId, initialLikes) => {
    const isLiked = !!likedPosts[postId];
    setLikedPosts(prev => ({
      ...prev,
      [postId]: !isLiked
    }));
  };

  const handleSharePost = (postId) => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(`${window.location.origin}/profile/${profile.id}#post-${postId}`);
      showNotification('Post link copied to clipboard!', 'info');
    }
  };

  const formatTimestamp = (dateString) => {
    if (!dateString) return 'Recent';
    try {
      const date = new Date(dateString);
      const now = new Date();
      const diffMs = now - date;
      const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
      if (diffDays <= 0) return 'Today';
      if (diffDays === 1) return 'Yesterday';
      if (diffDays < 30) return `${diffDays}d ago`;
      return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    } catch (e) {
      return 'Recently';
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
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px', flexWrap: 'wrap', gap: '10px' }}>
        <div>
          <h3 style={{ fontSize: '19px', color: 'var(--color-navy)', margin: 0, fontWeight: 700 }}>
            Activity & Industry Updates
          </h3>
          <span style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>
            {profile?.followersCount || 0} followers · {posts.length} published updates
          </span>
        </div>

        {isOwner && (
          <button 
            onClick={onOpenPostModal}
            className="btn btn-sm btn-outline-navy"
            style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', borderRadius: 'var(--radius-full)', fontWeight: 600 }}
          >
            <Plus size={15} />
            <span>Create a Post</span>
          </button>
        )}
      </div>

      {/* Owner In-Place Quick Composer Bar */}
      {isOwner && (
        <div 
          onClick={onOpenPostModal}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            padding: '12px 16px',
            backgroundColor: 'var(--color-bg-page)',
            borderRadius: 'var(--radius-full)',
            border: '1px solid var(--color-border)',
            cursor: 'pointer',
            marginBottom: '18px',
            transition: 'border-color var(--transition-fast)'
          }}
        >
          <img 
            src={profile?.avatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80'} 
            alt={profile?.name}
            style={{ width: '36px', height: '36px', borderRadius: '50%', objectFit: 'cover' }}
          />
          <span style={{ fontSize: '14px', color: 'var(--color-text-muted)', flex: 1 }}>
            Start a post about your feature shoot, script, or festival news...
          </span>
          <ImageIcon size={18} color="var(--color-navy)" />
        </div>
      )}

      {/* Tabs */}
      <div style={{ display: 'flex', gap: '10px', borderBottom: '1px solid var(--color-border-subtle)', marginBottom: '18px' }}>
        <button
          onClick={() => setActiveTab('posts')}
          style={{
            padding: '8px 16px',
            fontSize: '14px',
            fontWeight: 600,
            background: 'none',
            border: 'none',
            borderBottom: activeTab === 'posts' ? '2px solid var(--color-maroon)' : '2px solid transparent',
            color: activeTab === 'posts' ? 'var(--color-maroon)' : 'var(--color-text-muted)',
            cursor: 'pointer'
          }}
        >
          Posts ({posts.length})
        </button>
        <button
          onClick={() => setActiveTab('media')}
          style={{
            padding: '8px 16px',
            fontSize: '14px',
            fontWeight: 600,
            background: 'none',
            border: 'none',
            borderBottom: activeTab === 'media' ? '2px solid var(--color-maroon)' : '2px solid transparent',
            color: activeTab === 'media' ? 'var(--color-maroon)' : 'var(--color-text-muted)',
            cursor: 'pointer'
          }}
        >
          Media ({mediaPosts.length})
        </button>
      </div>

      {/* Post Stream Feed */}
      {displayedPosts.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '32px 16px', backgroundColor: 'var(--color-bg-page)', borderRadius: 'var(--radius-md)' }}>
          <Film size={32} color="var(--color-text-muted)" style={{ margin: '0 auto 8px auto', display: 'block', opacity: 0.6 }} />
          <h4 style={{ fontSize: '15px', color: 'var(--color-navy)', margin: '0 0 4px 0' }}>
            {activeTab === 'media' ? 'No media updates yet' : 'No posts shared yet'}
          </h4>
          <p style={{ fontSize: '13px', color: 'var(--color-text-muted)', margin: 0 }}>
            {isOwner ? 'Share a behind-the-scenes shot or production wrap note with the TrueHunt network.' : `${profile?.name} has not published updates yet.`}
          </p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {displayedPosts.map((post) => {
            const isLiked = !!likedPosts[post.id];
            const currentLikes = (post.likesCount || 0) + (isLiked ? 1 : 0);

            return (
              <div 
                key={post.id}
                id={`post-${post.id}`}
                style={{
                  padding: '18px',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: 'var(--color-bg-page)',
                  border: '1px solid var(--color-border)'
                }}
              >
                {/* Author Info */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '12px' }}>
                  <img 
                    src={post.authorAvatar || profile?.avatar} 
                    alt={post.authorName || profile?.name}
                    style={{ width: '42px', height: '42px', borderRadius: '50%', objectFit: 'cover' }}
                  />
                  <div>
                    <h4 style={{ fontSize: '15px', color: 'var(--color-navy)', margin: 0, fontWeight: 700 }}>
                      {post.authorName || profile?.name}
                    </h4>
                    <span style={{ fontSize: '12px', color: 'var(--color-text-muted)', display: 'block', lineHeight: 1.3 }}>
                      {post.authorRole || profile?.headline || 'Film Professional'}
                    </span>
                    <span style={{ fontSize: '11px', color: 'var(--color-text-light)', display: 'inline-flex', alignItems: 'center', gap: '4px', marginTop: '2px' }}>
                      <Clock size={11} /> {formatTimestamp(post.createdAt)}
                    </span>
                  </div>
                </div>

                {/* Post Body */}
                <p style={{ fontSize: '14px', color: 'var(--color-navy-surface)', lineHeight: 1.6, margin: '0 0 12px 0', whiteSpace: 'pre-line' }}>
                  {post.content}
                </p>

                {/* Post Image Media */}
                {post.mediaUrl && (
                  <div style={{ marginBottom: '14px', borderRadius: 'var(--radius-sm)', overflow: 'hidden', border: '1px solid var(--color-border)', maxHeight: '380px' }}>
                    <img 
                      src={post.mediaUrl} 
                      alt="Post media" 
                      style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }}
                      onError={(e) => { e.target.style.display = 'none'; }}
                    />
                  </div>
                )}

                {/* Engagement Bar */}
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingTop: '10px', borderTop: '1px solid var(--color-border-subtle)' }}>
                  <div style={{ display: 'flex', gap: '16px' }}>
                    <button
                      onClick={() => handleLike(post.id, post.likesCount)}
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '6px',
                        background: 'none',
                        border: 'none',
                        fontSize: '13px',
                        fontWeight: 600,
                        color: isLiked ? 'var(--color-maroon)' : 'var(--color-text-secondary)',
                        cursor: 'pointer'
                      }}
                    >
                      <Heart size={16} fill={isLiked ? 'var(--color-maroon)' : 'none'} color={isLiked ? 'var(--color-maroon)' : 'currentColor'} />
                      <span>{currentLikes}</span>
                    </button>

                    <button
                      onClick={() => showNotification('Comments are open for verified network peers.', 'info')}
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '6px',
                        background: 'none',
                        border: 'none',
                        fontSize: '13px',
                        color: 'var(--color-text-secondary)',
                        cursor: 'pointer'
                      }}
                    >
                      <MessageCircle size={16} />
                      <span>{post.commentsCount || 0} comments</span>
                    </button>
                  </div>

                  <button
                    onClick={() => handleSharePost(post.id)}
                    style={{
                      background: 'none',
                      border: 'none',
                      fontSize: '13px',
                      color: 'var(--color-text-muted)',
                      cursor: 'pointer',
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '4px'
                    }}
                    title="Share post"
                  >
                    <Share2 size={14} />
                    <span className="hide-on-mobile">Share</span>
                  </button>
                </div>

              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

