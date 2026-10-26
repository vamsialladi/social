import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { 
  ArrowLeft, 
  Share2, 
  Loader2, 
  AlertCircle, 
  RefreshCw,
  Sparkles
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useNotification } from '../context/NotificationContext';
import { api } from '../services/api';

import ProfileHeader from '../components/profile/ProfileHeader';
import ProfileAbout from '../components/profile/ProfileAbout';
import ProfileActivity from '../components/profile/ProfileActivity';
import ProfileExperience from '../components/profile/ProfileExperience';
import ProfileEducation from '../components/profile/ProfileEducation';
import ProfileSkills from '../components/profile/ProfileSkills';
import ProfileProjects from '../components/profile/ProfileProjects';
import ProfileSidebar from '../components/profile/ProfileSidebar';

import EditProfileModal from '../components/profile/EditProfileModal';
import ExperienceModal from '../components/profile/ExperienceModal';
import EducationModal from '../components/profile/EducationModal';
import ProjectModal from '../components/profile/ProjectModal';
import PostModal from '../components/profile/PostModal';

export default function ProfilePage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuth();
  const { showNotification } = useNotification();

  const [profile, setProfile] = useState(null);
  const [suggestions, setSuggestions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [connected, setConnected] = useState(false);

  // Modal visibility states
  const [showEditProfileModal, setShowEditProfileModal] = useState(false);
  const [experienceModalState, setExperienceModalState] = useState({ open: false, data: null });
  const [educationModalState, setEducationModalState] = useState({ open: false, data: null });
  const [projectModalState, setProjectModalState] = useState({ open: false, data: null });
  const [showPostModal, setShowPostModal] = useState(false);

  // Load profile data
  const loadProfile = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      let data;
      if (!id) {
        // Current logged in user profile
        data = await api.profile.getMe();
      } else {
        // Specific talent or user profile
        data = await api.profile.getById(id);
      }

      setProfile(data);

      // Load side recommendations
      const suggs = await api.profile.getSuggestions(data?.id);
      setSuggestions(suggs || []);
    } catch (err) {
      console.error('Error loading profile:', err);
      setError('Could not load profile. Please verify network connection.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  // Determine ownership
  const isOwner = Boolean(
    profile?.isOwner || 
    (!id && isAuthenticated) || 
    (user && (user.id === profile?.userId || user.id === profile?.id || user.email === profile?.userId))
  );

  // Profile Save Handler
  const handleSaveProfile = async (formData) => {
    const res = await api.profile.updateMe(formData);
    if (res.data) {
      setProfile(prev => ({ ...prev, ...formData, ...res.data }));
    } else {
      setProfile(prev => ({ ...prev, ...formData }));
    }
  };

  // Experience Handlers
  const handleSaveExperience = async (expData) => {
    if (experienceModalState.data) {
      // Update
      const res = await api.profile.updateExperience(experienceModalState.data.id, expData);
      setProfile(prev => ({
        ...prev,
        experiences: (prev.experiences || []).map(e => e.id === experienceModalState.data.id ? { ...e, ...expData } : e)
      }));
    } else {
      // Create
      const res = await api.profile.addExperience(expData);
      const newExp = res.data || { id: `exp-${Date.now()}`, ...expData };
      setProfile(prev => ({
        ...prev,
        experiences: [newExp, ...(prev.experiences || [])]
      }));
    }
  };

  const handleDeleteExperience = async (expId) => {
    await api.profile.deleteExperience(expId);
    setProfile(prev => ({
      ...prev,
      experiences: (prev.experiences || []).filter(e => e.id !== expId)
    }));
  };

  // Education Handlers
  const handleSaveEducation = async (eduData) => {
    if (educationModalState.data) {
      await api.profile.updateEducation(educationModalState.data.id, eduData);
      setProfile(prev => ({
        ...prev,
        educations: (prev.educations || []).map(e => e.id === educationModalState.data.id ? { ...e, ...eduData } : e)
      }));
    } else {
      const res = await api.profile.addEducation(eduData);
      const newEdu = res.data || { id: `edu-${Date.now()}`, ...eduData };
      setProfile(prev => ({
        ...prev,
        educations: [newEdu, ...(prev.educations || [])]
      }));
    }
  };

  const handleDeleteEducation = async (eduId) => {
    await api.profile.deleteEducation(eduId);
    setProfile(prev => ({
      ...prev,
      educations: (prev.educations || []).filter(e => e.id !== eduId)
    }));
  };

  // Project Handlers
  const handleSaveProject = async (projData) => {
    if (projectModalState.data) {
      await api.profile.updateProject(projectModalState.data.id, projData);
      setProfile(prev => ({
        ...prev,
        projects: (prev.projects || []).map(p => p.id === projectModalState.data.id ? { ...p, ...projData } : p)
      }));
    } else {
      const res = await api.profile.addProject(projData);
      const newProj = res.data || { id: `proj-${Date.now()}`, ...projData };
      setProfile(prev => ({
        ...prev,
        projects: [newProj, ...(prev.projects || [])]
      }));
    }
  };

  const handleDeleteProject = async (projId) => {
    await api.profile.deleteProject(projId);
    setProfile(prev => ({
      ...prev,
      projects: (prev.projects || []).filter(p => p.id !== projId)
    }));
  };

  // Skills Handlers
  const handleAddSkill = async (skill) => {
    await api.profile.addSkill(skill);
    setProfile(prev => ({
      ...prev,
      skills: prev.skills?.includes(skill) ? prev.skills : [...(prev.skills || []), skill]
    }));
  };

  const handleRemoveSkill = async (skill) => {
    await api.profile.deleteSkill(skill);
    setProfile(prev => ({
      ...prev,
      skills: (prev.skills || []).filter(s => s !== skill)
    }));
  };

  // Post Creator Handler
  const handleCreatePost = async (postData) => {
    const res = await api.profile.createPost(postData);
    const newPost = res.data || {
      id: `post-${Date.now()}`,
      authorId: profile.id,
      authorName: profile.name,
      authorAvatar: profile.avatar,
      authorRole: profile.headline || profile.role,
      likesCount: 0,
      commentsCount: 0,
      createdAt: new Date().toISOString(),
      ...postData
    };
    setProfile(prev => ({
      ...prev,
      posts: [newPost, ...(prev.posts || [])]
    }));
  };

  // Connect Handler for Visitors
  const handleConnect = async () => {
    try {
      await api.talents.connect(profile.id, 'I would like to connect on TrueHunt.');
      setConnected(true);
      showNotification(`Invitation sent to ${profile.name}!`, 'success');
    } catch (err) {
      showNotification('Failed to send connection invitation', 'error');
    }
  };

  if (loading) {
    return (
      <div style={{ backgroundColor: 'var(--color-bg-page)', minHeight: '85vh', padding: '60px 20px' }}>
        <div className="container" style={{ maxWidth: '1120px', textAlign: 'center' }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '10px', padding: '16px 28px', backgroundColor: 'var(--color-white)', borderRadius: 'var(--radius-lg)', boxShadow: 'var(--shadow-card)' }}>
            <Loader2 size={24} className="spin-animation" color="var(--color-maroon)" />
            <span style={{ fontSize: '15px', color: 'var(--color-navy)', fontWeight: 600 }}>
              Loading professional profile...
            </span>
          </div>
        </div>
      </div>
    );
  }

  if (!id && !isAuthenticated) {
    return (
      <div style={{ backgroundColor: 'var(--color-bg-page)', minHeight: '85vh', padding: '60px 20px' }}>
        <div className="container" style={{ maxWidth: '560px', textAlign: 'center' }}>
          <div className="card" style={{ padding: '40px', borderRadius: 'var(--radius-lg)', boxShadow: 'var(--shadow-card)' }}>
            <Sparkles size={44} color="var(--color-maroon)" style={{ margin: '0 auto 16px auto' }} />
            <h3 style={{ fontSize: '22px', color: 'var(--color-navy)', marginBottom: '8px', fontWeight: 700 }}>
              Sign In to View Your Profile
            </h3>
            <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)', marginBottom: '24px', lineHeight: 1.6 }}>
              Access your personal cinema credentials, manage production credits, showcase your portfolio, and connect with film industry peers.
            </p>
            <div style={{ display: 'flex', justifyContent: 'center', gap: '14px', flexWrap: 'wrap' }}>
              <Link to="/sign-in" className="btn btn-maroon" style={{ padding: '10px 24px', fontWeight: 600 }}>
                Sign In to TrueHunt
              </Link>
              <Link to="/join-now" className="btn btn-outline-navy" style={{ padding: '10px 22px', fontWeight: 600 }}>
                Create Account
              </Link>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (error || !profile) {
    return (
      <div style={{ backgroundColor: 'var(--color-bg-page)', minHeight: '85vh', padding: '60px 20px' }}>
        <div className="container" style={{ maxWidth: '600px', textAlign: 'center' }}>
          <div className="card" style={{ padding: '40px', borderRadius: 'var(--radius-lg)' }}>
            <AlertCircle size={40} color="var(--color-maroon)" style={{ margin: '0 auto 12px auto' }} />
            <h3 style={{ fontSize: '20px', color: 'var(--color-navy)', marginBottom: '8px' }}>
              Profile Unavailable
            </h3>
            <p style={{ fontSize: '14px', color: 'var(--color-text-muted)', marginBottom: '20px' }}>
              {error || 'The requested cinema professional profile could not be found.'}
            </p>
            <div style={{ display: 'flex', justifyContent: 'center', gap: '12px' }}>
              <button onClick={loadProfile} className="btn btn-outline-navy" style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                <RefreshCw size={15} /> Retry
              </button>
              <Link to="/talents" className="btn btn-maroon">
                Browse Talents Directory
              </Link>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div style={{ backgroundColor: 'var(--color-bg-page)', minHeight: '90vh', paddingBottom: '80px' }}>
      
      {/* Top Breadcrumb Bar */}
      <div style={{ backgroundColor: 'var(--color-white)', borderBottom: '1px solid var(--color-border)', padding: '10px 0' }}>
        <div className="container" style={{ maxWidth: '1160px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Link 
            to="/talents" 
            style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)' }}
          >
            <ArrowLeft size={16} /> Talents Directory
          </Link>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>
              {isOwner ? 'Viewing your public profile preview' : 'Cinema Professional Portfolio'}
            </span>
          </div>
        </div>
      </div>

      {/* Main Profile Grid Container */}
      <div className="container" style={{ maxWidth: '1160px', marginTop: '24px' }}>
        
        <div className="profile-layout-grid">
          
          {/* Main Profile Column (Left / Center) */}
          <main className="profile-main-column">
            
            {/* Header & Cover Card */}
            <ProfileHeader 
              profile={profile} 
              isOwner={isOwner}
              onEditProfile={() => setShowEditProfileModal(true)}
              onConnect={handleConnect}
              connected={connected}
            />

            {/* About Section */}
            <ProfileAbout 
              profile={profile} 
              isOwner={isOwner}
              onEdit={() => setShowEditProfileModal(true)}
            />

            {/* Activity & Posts */}
            <ProfileActivity 
              profile={profile} 
              isOwner={isOwner}
              onOpenPostModal={() => setShowPostModal(true)}
            />

            {/* Experience Section */}
            <ProfileExperience 
              experiences={profile?.experiences || []}
              isOwner={isOwner}
              onAdd={() => setExperienceModalState({ open: true, data: null })}
              onEdit={(exp) => setExperienceModalState({ open: true, data: exp })}
              onDelete={handleDeleteExperience}
            />

            {/* Education Section */}
            <ProfileEducation 
              educations={profile?.educations || []}
              isOwner={isOwner}
              onAdd={() => setEducationModalState({ open: true, data: null })}
              onEdit={(edu) => setEducationModalState({ open: true, data: edu })}
              onDelete={handleDeleteEducation}
            />

            {/* Skills & Expertise Section */}
            <ProfileSkills 
              skills={profile?.skills || []}
              isOwner={isOwner}
              onAddSkill={handleAddSkill}
              onRemoveSkill={handleRemoveSkill}
            />

            {/* Projects & Film Credits Section */}
            <ProfileProjects 
              projects={profile?.projects || []}
              credits={profile?.credits || []}
              isOwner={isOwner}
              onAdd={() => setProjectModalState({ open: true, data: null })}
              onEdit={(proj) => setProjectModalState({ open: true, data: proj })}
              onDelete={handleDeleteProject}
            />

          </main>

          {/* Right Sidebar Column */}
          <div className="profile-sidebar-column">
            <ProfileSidebar 
              suggestions={suggestions} 
              profile={profile} 
              isOwner={isOwner} 
            />
          </div>

        </div>

      </div>

      {/* Modals */}
      {showEditProfileModal && (
        <EditProfileModal 
          profile={profile} 
          onClose={() => setShowEditProfileModal(false)}
          onSave={handleSaveProfile}
        />
      )}

      {experienceModalState.open && (
        <ExperienceModal 
          initialData={experienceModalState.data}
          onClose={() => setExperienceModalState({ open: false, data: null })}
          onSave={handleSaveExperience}
        />
      )}

      {educationModalState.open && (
        <EducationModal 
          initialData={educationModalState.data}
          onClose={() => setEducationModalState({ open: false, data: null })}
          onSave={handleSaveEducation}
        />
      )}

      {projectModalState.open && (
        <ProjectModal 
          initialData={projectModalState.data}
          onClose={() => setProjectModalState({ open: false, data: null })}
          onSave={handleSaveProject}
        />
      )}

      {showPostModal && (
        <PostModal 
          author={profile}
          onClose={() => setShowPostModal(false)}
          onPostCreated={handleCreatePost}
        />
      )}

      {/* Responsive layout styling */}
      <style>{`
        .profile-layout-grid {
          display: grid;
          grid-template-columns: 1fr;
          gap: 24px;
        }

        @media (min-width: 992px) {
          .profile-layout-grid {
            grid-template-columns: minmax(0, 1fr) 330px;
          }
        }

        .profile-main-column {
          display: flex;
          flex-direction: column;
          gap: 0;
        }

        @media (max-width: 768px) {
          .hide-on-mobile {
            display: none !important;
          }
        }

        .spin-animation {
          animation: spin 1s linear infinite;
        }

        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
      `}</style>

    </div>
  );
}

