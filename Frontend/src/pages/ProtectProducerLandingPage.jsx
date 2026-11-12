import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { 
  Shield, 
  CheckCircle2, 
  FileCheck, 
  DollarSign, 
  AlertTriangle, 
  Lock,
  ArrowRight,
} from 'lucide-react';
import { api } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { getProfileStorageKey } from '../services/profileStorage';

export default function ProtectProducerLandingPage() {
  const [productionProfile, setProductionProfile] = useState(null);
  const { user } = useAuth();
  const profileStorageKey = getProfileStorageKey(user?.userId || user?.id || user?.email);

  useEffect(() => {
    let isCurrentAccount = true;

    async function loadProductionProfile() {
      let savedProfile = null;
      try {
        if (profileStorageKey) {
          const storedProfile = localStorage.getItem(profileStorageKey);
          savedProfile = storedProfile ? JSON.parse(storedProfile) : null;
        }

        const accountProfile = await api.profile.getMe();
        const companyName = accountProfile.company || accountProfile.companyName;
        const isProductionProfile = Boolean(
          companyName
          || accountProfile.role === 'Production House'
          || accountProfile.primaryCraft === 'Production'
        );
        const profile = {
          ...savedProfile,
          userId: accountProfile.userId || accountProfile.id || user?.userId || user?.id,
          name: (isProductionProfile ? accountProfile.name : savedProfile?.name) || user?.name || '',
          companyName: companyName || savedProfile?.companyName || '',
          company: companyName || savedProfile?.company || '',
          headquarters: accountProfile.address
            || (accountProfile.location && !accountProfile.location.startsWith('Add your') ? accountProfile.location : '')
            || savedProfile?.headquarters
            || '',
          email: accountProfile.email || savedProfile?.email || user?.email || '',
          phone: accountProfile.phone || savedProfile?.phone || '',
          role: accountProfile.role || savedProfile?.role || 'Production House',
          primaryCraft: accountProfile.primaryCraft || savedProfile?.primaryCraft || 'Production',
        };

        if (isCurrentAccount) {
          setProductionProfile(profile);
          if (profileStorageKey) {
            localStorage.setItem(profileStorageKey, JSON.stringify(profile));
            localStorage.setItem('truehunt_producer_profile', JSON.stringify(profile));
          }
        }
      } catch (error) {
        console.warn('Could not load the Production profile:', error);
        if (isCurrentAccount && savedProfile) {
          setProductionProfile(savedProfile);
        }
      }
    }

    loadProductionProfile();
    return () => {
      isCurrentAccount = false;
    };
  }, [profileStorageKey, user?.email, user?.id, user?.name, user?.userId]);

  return (
    <div style={{ backgroundColor: 'var(--color-bg-page)', textAlign: 'center' }}>
      
      {/* Hero Section */}
      <section 
        style={{ 
          backgroundColor: 'var(--color-navy)', 
          color: 'var(--color-white)', 
          padding: '70px 0 80px 0', 
          borderBottom: '4px solid var(--color-maroon)',
          position: 'relative',
          overflow: 'hidden'
        }}
      >
        {/* Subtle background glow */}
        <div 
          style={{
            position: 'absolute',
            top: '-20%',
            right: '-10%',
            width: '600px',
            height: '600px',
            borderRadius: '50%',
            background: 'radial-gradient(circle, rgba(128, 24, 35, 0.25) 0%, rgba(10, 25, 47, 0) 70%)',
            pointerEvents: 'none'
          }}
        />

        <div className="container" style={{ position: 'relative', zIndex: 2 }}>
          <div style={{ maxWidth: '840px', margin: '0 auto' }}>
            
            {/* Module Badge */}
            <div 
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '8px',
                padding: '6px 14px',
                borderRadius: 'var(--radius-full)',
                backgroundColor: 'rgba(128, 24, 35, 0.25)',
                border: '1px solid rgba(232, 180, 184, 0.4)',
                color: '#FED7AA',
                fontSize: '13px',
                fontWeight: 600,
                marginBottom: '20px'
              }}
            >
              <Shield size={16} color="var(--color-maroon-border)" />
              <span>TrueHunt Production Lifecycle & Risk Governance • Protect Producer</span>
            </div>

            {/* Headline */}
            <h1 
              style={{ 
                fontSize: 'clamp(34px, 4.5vw, 52px)', 
                color: 'var(--color-white)', 
                fontWeight: 800, 
                lineHeight: 1.18, 
                letterSpacing: '-0.02em',
                marginBottom: '20px' 
              }}
            >
              Your Production. Your Investment. Your Control.
            </h1>

            {/* Supporting Text */}
            <p 
              style={{ 
                fontSize: '18px', 
                color: '#CBD5E1', 
                lineHeight: 1.65, 
                marginBottom: '36px',
                maxWidth: '720px',
                margin: '0 auto 36px'
              }}
            >
              Manage every stage of your film and content production lifecycle in one organized workspace, 
              from initial planning to final release. Ensure financial transparency, script and cast chain-of-custody, 
              milestone accountability, and pre-release audit readiness.
            </p>

            <div style={{ display: 'flex', justifyContent: 'center' }}>
              <button
                type="button"
                className="btn btn-lg btn-maroon"
                style={{
                  boxShadow: '0 4px 18px rgba(128, 24, 35, 0.35)',
                  gap: '10px'
                }}
              >
                <span>Explore Production Categories</span>
                <ArrowRight size={18} />
              </button>
            </div>

          </div>
        </div>
      </section>

      {user?.role === 'STUDIO' && (
        <section aria-labelledby="production-profile-heading" style={{ padding: '28px 0', backgroundColor: 'var(--color-white)', borderBottom: '1px solid var(--color-border)' }}>
          <div className="container">
            <div className="card" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '20px', padding: '22px 24px' }}>
              <div>
                <span className="badge badge-maroon" style={{ marginBottom: '8px' }}>Production Profile</span>
                <h2 id="production-profile-heading" style={{ color: 'var(--color-navy)', fontSize: '22px', marginBottom: '6px' }}>
                  {productionProfile?.companyName || productionProfile?.company || user.companyName || user.name || 'Your Production Profile'}
                </h2>
                <p style={{ color: 'var(--color-text-secondary)', margin: 0 }}>
                  {productionProfile?.name || user.name}
                  {productionProfile?.headquarters ? ` · ${productionProfile.headquarters}` : ''}
                  {productionProfile?.email ? ` · ${productionProfile.email}` : ''}
                </p>
              </div>
              <Link to="/social/profile" className="btn btn-outline-subtle" style={{ textDecoration: 'none' }}>
                Open Social Profile
              </Link>
            </div>
          </div>
        </section>
      )}

    </div>
  );
}
