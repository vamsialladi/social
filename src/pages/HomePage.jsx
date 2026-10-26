import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { 
  Search, 
  Sparkles, 
  ArrowRight, 
  Users,
  Briefcase,
  TrendingUp,
  Smartphone,
  Film, 
  CheckCircle2, 
  Clapperboard
} from 'lucide-react';
import TalentCard from '../components/cards/TalentCard';
import OpportunityCard from '../components/cards/OpportunityCard';
import CraftCard from '../components/cards/CraftCard';
import { 
  PLATFORM_STATS, 
  FILM_CRAFTS, 
  FEATURED_TALENTS, 
  LATEST_OPPORTUNITIES 
} from '../services/mockData';
import { api } from '../services/api';
import cinemaHeroImg from '../assets/cinema-hero.jpg';

export default function HomePage() {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedQuickRole, setSelectedQuickRole] = useState('');
  const [talents, setTalents] = useState(FEATURED_TALENTS);
  const [jobs, setJobs] = useState(LATEST_OPPORTUNITIES);
  const [stats, setStats] = useState(PLATFORM_STATS);
  const navigate = useNavigate();

  useEffect(() => {
    // Attempt async backend fetch with fallback to mock data
    async function loadData() {
      try {
        const fetchedStats = await api.stats.get();
        if (fetchedStats) setStats(fetchedStats);
        const fetchedTalents = await api.talents.getAll();
        if (fetchedTalents) setTalents(fetchedTalents);
        const fetchedJobs = await api.jobs.getAll();
        if (fetchedJobs) setJobs(fetchedJobs);
      } catch (e) {
        console.log('Using baseline demo data');
      }
    }
    loadData();
  }, []);

  const popularRoles = [
    { label: 'Actors', category: 'acting' },
    { label: 'Directors', category: 'direction' },
    { label: 'Writers', category: 'screenwriting' },
    { label: 'Technicians', category: 'technicians' },
    { label: 'Music', category: 'music' },
    { label: 'Editors', category: 'editing' },
    { label: 'DOP', category: 'cinematography' },
    { label: 'Production', category: 'production' },
    { label: 'VFX', category: 'vfx-animation' },
    { label: 'Casting', category: 'casting' },
    { label: 'Art Directors', category: 'art-direction' },
    { label: 'Choreographers', category: 'dance' }
  ];

  const handleHeroSearch = (e) => {
    e.preventDefault();
    if (!searchTerm.trim()) {
      navigate('/talents');
      return;
    }
    navigate(`/talents?q=${encodeURIComponent(searchTerm.trim())}`);
  };

  const handleQuickRoleClick = (roleObj) => {
    setSelectedQuickRole(roleObj.label);
    navigate(`/talents?category=${encodeURIComponent(roleObj.category)}`);
  };

  const featuredTalent = talents.find(t => t.name.toLowerCase().includes('aarav')) || talents[0] || {
    name: 'Aarav Varma',
    role: 'Cinematographer (DoP)',
    avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80',
    verified: true
  };

  const featuredAudition = jobs[0] || {
    title: 'Lead Actor — Feature Film',
    productionCompany: 'Aura Cinema Works',
    department: 'Cast'
  };

  return (
    <div className="home-page">
      
      {/* =========================================================================
          SECTION 1: HERO SECTION — CINEMA INDUSTRY REDESIGN
          ========================================================================= */}
      <section className="hero-section">
        {/* Subtle cinematic gradient aura background */}
        <div className="hero-aura-bg" />

        <div className="container">
          <div className="hero-grid">
            
            {/* Hero Left Content */}
            <div className="hero-left-content">
              
              {/* Badge */}
              <div className="hero-badge">
                <Sparkles size={14} />
                <span>The Film & Entertainment Industry Network • truehunt.in</span>
              </div>

              {/* Main Headline */}
              <h1 className="hero-title">
                Connect. Grow. <br />
                <span className="hero-title-accent">Create the Next Story.</span>
              </h1>

              {/* Supporting Text */}
              <p className="hero-description">
                Discover talented professionals, explore film-industry opportunities, and connect 
                with the people who bring stories to life. Built specifically for actors, technicians, 
                directors, and cinema creators.
              </p>

              {/* Talent Search Box */}
              <form onSubmit={handleHeroSearch} className="hero-search-form">
                <Search size={20} color="var(--color-text-muted)" style={{ marginRight: '10px', flexShrink: 0 }} />
                <input 
                  type="text"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  placeholder="Search by talent name, professional role, skill, or opportunity..."
                  aria-label="Search by talent name, professional role, skill, or opportunity..."
                  className="hero-search-input"
                />
                <button 
                  type="submit"
                  className="hero-search-btn"
                >
                  <span>Search</span>
                  <ArrowRight size={16} />
                </button>
              </form>

              {/* Popular Professional Roles */}
              <div className="hero-roles-section">
                <span className="hero-roles-label">
                  POPULAR ROLES:
                </span>
                <div className="hero-roles-list">
                  {popularRoles.map((role) => (
                    <button
                      key={role.label}
                      type="button"
                      onClick={() => handleQuickRoleClick(role)}
                      className={`hero-role-pill ${selectedQuickRole === role.label ? 'active' : ''}`}
                    >
                      {role.label}
                    </button>
                  ))}
                </div>
              </div>

            </div>

            {/* Hero Right Visual: Cinema Industry Set & Floating Cards */}
            <div className="hero-visual-column">
              <div className="hero-visual-wrapper">

                {/* Floating Talent Card 1: Aarav Varma (Top Left Overlap) */}
                <div className="hero-floating-card hero-card-talent">
                  <img 
                    src={featuredTalent.avatar} 
                    alt={featuredTalent.name}
                    className="hero-card-avatar"
                  />
                  <div>
                    <div className="hero-card-name-row">
                      <span className="hero-card-name">{featuredTalent.name}</span>
                      <CheckCircle2 size={15} fill="#2563EB" color="#FFFFFF" />
                    </div>
                    <span className="hero-card-role">{featuredTalent.role}</span>
                  </div>
                </div>

                {/* Main Cinematic Backdrop Frame */}
                <div className="hero-image-frame">
                  <img 
                    src={cinemaHeroImg} 
                    alt="Cinema Film Production Set with Professional Camera, Director's Chair, Lights and Crew"
                    className="hero-main-image"
                  />
                  {/* Dark cinematic gradient overlay */}
                  <div className="hero-image-overlay" />
                  
                  {/* Film Production Information Card (Inside Image, Lower Left) */}
                  <div className="hero-production-card">
                    <span className="hero-production-pill">On-Set Production</span>
                    <h4 className="hero-production-title">TrueHunt Cinema Network</h4>
                    <p className="hero-production-subtitle">Connecting crew & cast from script to premiere</p>
                  </div>
                </div>

                {/* Floating Audition Card 2: Lead Actor (Bottom Right Overlap) */}
                <div className="hero-floating-card hero-card-audition">
                  <div className="hero-card-icon-box">
                    <Clapperboard size={18} color="#FFFFFF" />
                  </div>
                  <div>
                    <span className="badge badge-maroon" style={{ fontSize: '10px', padding: '2px 8px', fontWeight: 700 }}>
                      New Audition
                    </span>
                    <div className="hero-audition-title">
                      {featuredAudition.title || 'Lead Actor — Feature Film'}
                    </div>
                    <span className="hero-audition-company">
                      {featuredAudition.productionCompany || 'Aura Cinema Works'}
                    </span>
                  </div>
                </div>

              </div>
            </div>

          </div>
        </div>
      </section>

      {/* =========================================================================
          SECTION 2: PLATFORM STATISTICS
          ========================================================================= */}
      <section 
        style={{
          backgroundColor: 'var(--color-white)',
          paddingTop: '36px',
          paddingBottom: '36px',
          borderBottom: '1px solid var(--color-border)'
        }}
      >
        <div className="container">
          
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '8px' }}>
            <span className="badge-demo">Illustrative Demo Figures</span>
          </div>

          <div 
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
              gap: '24px'
            }}
          >
            {stats.map((stat) => (
              <div 
                key={stat.id}
                style={{
                  padding: '20px 24px',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: 'var(--color-bg-page)',
                  border: '1px solid var(--color-border)',
                  textAlign: 'center'
                }}
              >
                <div 
                  style={{
                    fontSize: '36px',
                    fontWeight: 800,
                    color: 'var(--color-navy)',
                    fontFamily: 'var(--font-display)',
                    lineHeight: '1.1',
                    marginBottom: '4px'
                  }}
                >
                  {stat.value}
                </div>
                <div style={{ fontSize: '15px', fontWeight: 600, color: 'var(--color-navy-surface)', marginBottom: '4px' }}>
                  {stat.label}
                </div>
                <div style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>
                  {stat.detail}
                </div>
              </div>
            ))}
          </div>

        </div>
      </section>

      {/* =========================================================================
          SECTION 3: EXPLORE OPPORTUNITIES
          ========================================================================= */}
      <section className="section-padding" style={{ backgroundColor: 'var(--color-bg-page)' }}>
        <div className="container">
          
          <div className="section-header">
            <h2 className="section-title">Explore Opportunities on TrueHunt</h2>
            <p className="section-subtitle">
              Everything you need to discover talent, pitch to investors, secure crew roles, 
              or manage on-set operations in one unified cinematic network.
            </p>
          </div>

          <div 
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))',
              gap: '24px'
            }}
          >
            {/* Card 1: Find Talents */}
            <div 
              className="card card-hoverable" 
              style={{
                padding: '32px 24px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                borderRadius: 'var(--radius-md)'
              }}
            >
              <div>
                <div 
                  style={{
                    width: '54px',
                    height: '54px',
                    borderRadius: 'var(--radius-sm)',
                    backgroundColor: '#EFF6FF',
                    color: '#1D4ED8',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    marginBottom: '20px'
                  }}
                >
                  <Users size={28} />
                </div>
                <h3 style={{ fontSize: '20px', color: 'var(--color-navy)', marginBottom: '10px' }}>Find Talents</h3>
                <p style={{ fontSize: '14px', color: 'var(--color-text-muted)', lineHeight: '1.5', marginBottom: '24px' }}>
                  Discover professionals based on skills, experience, location, and film-industry role.
                </p>
              </div>
              <Link to="/talents" className="btn btn-outline-navy" style={{ width: '100%' }}>
                <span>Explore Talents</span>
                <ArrowRight size={15} />
              </Link>
            </div>

            {/* Card 2: Job Opportunities */}
            <div 
              className="card card-hoverable" 
              style={{
                padding: '32px 24px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                borderRadius: 'var(--radius-md)'
              }}
            >
              <div>
                <div 
                  style={{
                    width: '54px',
                    height: '54px',
                    borderRadius: 'var(--radius-sm)',
                    backgroundColor: 'var(--color-maroon-light)',
                    color: 'var(--color-maroon)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    marginBottom: '20px'
                  }}
                >
                  <Briefcase size={28} />
                </div>
                <h3 style={{ fontSize: '20px', color: 'var(--color-navy)', marginBottom: '10px' }}>Job Opportunities</h3>
                <p style={{ fontSize: '14px', color: 'var(--color-text-muted)', lineHeight: '1.5', marginBottom: '24px' }}>
                  Discover auditions, film projects, crew requirements, and entertainment-industry jobs.
                </p>
              </div>
              <Link to="/jobs" className="btn btn-outline-navy" style={{ width: '100%' }}>
                <span>Explore Jobs</span>
                <ArrowRight size={15} />
              </Link>
            </div>

            {/* Card 3: Investors Connect */}
            <div 
              className="card card-hoverable" 
              style={{
                padding: '32px 24px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                borderRadius: 'var(--radius-md)'
              }}
            >
              <div>
                <div 
                  style={{
                    width: '54px',
                    height: '54px',
                    borderRadius: 'var(--radius-sm)',
                    backgroundColor: '#FEF3C7',
                    color: '#B45309',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    marginBottom: '20px'
                  }}
                >
                  <TrendingUp size={28} />
                </div>
                <h3 style={{ fontSize: '20px', color: 'var(--color-navy)', marginBottom: '10px' }}>Investors Connect</h3>
                <p style={{ fontSize: '14px', color: 'var(--color-text-muted)', lineHeight: '1.5', marginBottom: '24px' }}>
                  Help suitable production teams and entertainment businesses discover investor opportunities.
                </p>
              </div>
              <Link to="/investors-connect" className="btn btn-outline-navy" style={{ width: '100%' }}>
                <span>Explore Investors</span>
                <ArrowRight size={15} />
              </Link>
            </div>

            {/* Card 4: LCM App */}
            <div 
              className="card card-hoverable" 
              style={{
                padding: '32px 24px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                borderRadius: 'var(--radius-md)'
              }}
            >
              <div>
                <div 
                  style={{
                    width: '54px',
                    height: '54px',
                    borderRadius: 'var(--radius-sm)',
                    backgroundColor: '#F3E8FF',
                    color: '#7E22CE',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    marginBottom: '20px'
                  }}
                >
                  <Smartphone size={28} />
                </div>
                <h3 style={{ fontSize: '20px', color: 'var(--color-navy)', marginBottom: '10px' }}>LCM App</h3>
                <p style={{ fontSize: '14px', color: 'var(--color-text-muted)', lineHeight: '1.5', marginBottom: '24px' }}>
                  Introduce the Location, Crew & Movie Management application through its dedicated portal.
                </p>
              </div>
              <Link to="/lcm-app" className="btn btn-outline-navy" style={{ width: '100%' }}>
                <span>Explore LCM App</span>
                <ArrowRight size={15} />
              </Link>
            </div>

          </div>

        </div>
      </section>

      {/* =========================================================================
          SECTION 4: FEATURED TALENTS
          ========================================================================= */}
      <section className="section-padding" style={{ backgroundColor: 'var(--color-white)', borderBottom: '1px solid var(--color-border)' }}>
        <div className="container">
          
          <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '40px', gap: '16px' }}>
            <div>
              <span className="badge badge-maroon" style={{ marginBottom: '8px' }}>Verified Directory</span>
              <h2 className="section-title" style={{ marginBottom: '6px' }}>Featured Talents</h2>
              <p className="section-subtitle" style={{ margin: 0, textAlign: 'left' }}>
                Browse top film-industry professionals vetted for verified showreels and on-screen credits.
              </p>
            </div>
            <Link to="/talents" className="btn btn-outline-navy">
              <span>View All Talents ({talents.length}+)</span>
              <ArrowRight size={15} />
            </Link>
          </div>

          <div 
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
              gap: '24px'
            }}
          >
            {talents.slice(0, 4).map((talent) => (
              <TalentCard key={talent.id} talent={talent} />
            ))}
          </div>

        </div>
      </section>

      {/* =========================================================================
          SECTION 5: BUILD YOUR CAREER IN CINEMA
          ========================================================================= */}
      <section className="section-padding" style={{ backgroundColor: '#F8FAFC' }}>
        <div className="container">
          
          <div className="section-header">
            <h2 className="section-title">Build Your Career in Cinema</h2>
            <p className="section-subtitle">
              TrueHunt provides the professional scaffolding you need to showcase verified filmography, 
              find auditions, and get hired by prestigious studios.
            </p>
          </div>

          <div 
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
              gap: '28px'
            }}
          >
            {/* Step 1 */}
            <div 
              className="card"
              style={{
                padding: '32px 28px',
                borderRadius: 'var(--radius-md)',
                position: 'relative',
                borderTop: '4px solid var(--color-navy)'
              }}
            >
              <div 
                style={{
                  fontSize: '14px',
                  fontWeight: 800,
                  color: 'var(--color-navy)',
                  backgroundColor: 'var(--color-bg-subtle)',
                  width: '32px',
                  height: '32px',
                  borderRadius: '50%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: '18px'
                }}
              >
                01
              </div>
              <h3 style={{ fontSize: '20px', color: 'var(--color-navy)', marginBottom: '12px' }}>
                Create Your Talent Profile
              </h3>
              <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)', lineHeight: '1.6', marginBottom: '24px' }}>
                Add your official film department, skills, verified experience, physical traits (for actors), 
                and private contact preferences.
              </p>
              <Link to="/join-now" className="btn btn-sm btn-outline-navy">
                <span>Start Your Profile</span>
              </Link>
            </div>

            {/* Step 2 */}
            <div 
              className="card"
              style={{
                padding: '32px 28px',
                borderRadius: 'var(--radius-md)',
                position: 'relative',
                borderTop: '4px solid var(--color-maroon)'
              }}
            >
              <div 
                style={{
                  fontSize: '14px',
                  fontWeight: 800,
                  color: 'var(--color-maroon)',
                  backgroundColor: 'var(--color-maroon-light)',
                  width: '32px',
                  height: '32px',
                  borderRadius: '50%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: '18px'
                }}
              >
                02
              </div>
              <h3 style={{ fontSize: '20px', color: 'var(--color-navy)', marginBottom: '12px' }}>
                Showcase Your Work
              </h3>
              <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)', lineHeight: '1.6', marginBottom: '24px' }}>
                Display high-definition showreels, Vimeo links, theatrical credits, festival laurels, 
                and behind-the-scenes portfolios.
              </p>
              <Link to="/talents" className="btn btn-sm btn-outline-maroon">
                <span>See Profile Samples</span>
              </Link>
            </div>

            {/* Step 3 */}
            <div 
              className="card"
              style={{
                padding: '32px 28px',
                borderRadius: 'var(--radius-md)',
                position: 'relative',
                borderTop: '4px solid var(--color-brown-arrow)'
              }}
            >
              <div 
                style={{
                  fontSize: '14px',
                  fontWeight: 800,
                  color: 'var(--color-brown-arrow)',
                  backgroundColor: 'var(--color-brown-light)',
                  width: '32px',
                  height: '32px',
                  borderRadius: '50%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: '18px'
                }}
              >
                03
              </div>
              <h3 style={{ fontSize: '20px', color: 'var(--color-navy)', marginBottom: '12px' }}>
                Connect with Filmmakers
              </h3>
              <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)', lineHeight: '1.6', marginBottom: '24px' }}>
                Directly pitch to production houses, apply for verified auditions, collaborate with cinematographers, 
                and receive opportunities.
              </p>
              <Link to="/jobs" className="btn btn-sm btn-navy">
                <span>Browse Opportunities</span>
              </Link>
            </div>

          </div>

        </div>
      </section>

      {/* =========================================================================
          SECTION 6: MADE FOR EVERY FILM CRAFT
          ========================================================================= */}
      <section className="section-padding" style={{ backgroundColor: 'var(--color-white)', borderBottom: '1px solid var(--color-border)' }}>
        <div className="container">
          
          <div className="section-header">
            <span className="badge badge-navy" style={{ marginBottom: '8px' }}>12 Specialized Disciplines</span>
            <h2 className="section-title">Made for Every Film Craft</h2>
            <p className="section-subtitle">
              Cinema thrives when every department excels. Explore dedicated directories for each artistic and technical role.
            </p>
          </div>

          <div 
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))',
              gap: '20px'
            }}
          >
            {FILM_CRAFTS.map((craft) => (
              <CraftCard key={craft.id} craft={craft} />
            ))}
          </div>

        </div>
      </section>

      {/* =========================================================================
          SECTION 7: LATEST INDUSTRY OPPORTUNITIES
          ========================================================================= */}
      <section className="section-padding" style={{ backgroundColor: 'var(--color-bg-page)' }}>
        <div className="container">
          
          <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '40px', gap: '16px' }}>
            <div>
              <span className="badge badge-maroon" style={{ marginBottom: '8px' }}>Active Production Calls</span>
              <h2 className="section-title" style={{ marginBottom: '6px' }}>Latest Industry Opportunities</h2>
              <p className="section-subtitle" style={{ margin: 0, textAlign: 'left' }}>
                Auditions, crew call sheets, and technical openings currently recruiting cast & crew.
              </p>
            </div>
            <Link to="/jobs" className="btn btn-outline-navy">
              <span>View All Opportunities ({jobs.length}+)</span>
              <ArrowRight size={15} />
            </Link>
          </div>

          <div 
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))',
              gap: '24px'
            }}
          >
            {jobs.slice(0, 3).map((job) => (
              <OpportunityCard key={job.id} job={job} />
            ))}
          </div>

        </div>
      </section>

      {/* =========================================================================
          SECTION 8: CALL TO ACTION BANNER (MAROON)
          ========================================================================= */}
      <section 
        style={{
          backgroundColor: 'var(--color-maroon)',
          color: 'var(--color-white)',
          paddingTop: '72px',
          paddingBottom: '72px',
          textAlign: 'center',
          position: 'relative',
          overflow: 'hidden'
        }}
      >
        {/* Subtle decorative cinema graphic */}
        <div 
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundImage: 'radial-gradient(circle at 80% 20%, rgba(255, 255, 255, 0.12) 0%, transparent 60%)',
            pointerEvents: 'none'
          }}
        />

        <div className="container" style={{ position: 'relative', zIndex: 1, maxWidth: '780px' }}>
          
          <div 
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              padding: '6px 14px',
              borderRadius: 'var(--radius-full)',
              backgroundColor: 'rgba(255, 255, 255, 0.15)',
              color: 'var(--color-white)',
              fontSize: '13px',
              fontWeight: 600,
              marginBottom: '20px'
            }}
          >
            <Film size={15} />
            <span>truehunt.in • Join 50,000+ Film Creators</span>
          </div>

          <h2 
            style={{
              fontSize: 'clamp(32px, 4.5vw, 46px)',
              fontWeight: 800,
              color: 'var(--color-white)',
              marginBottom: '18px',
              letterSpacing: '-0.02em',
              lineHeight: '1.2'
            }}
          >
            Your next story starts here.
          </h2>

          <p 
            style={{
              fontSize: '18px',
              color: '#FEE2E2',
              lineHeight: '1.6',
              marginBottom: '36px',
              maxWidth: '620px',
              marginLeft: 'auto',
              marginRight: 'auto'
            }}
          >
            Join a growing professional community built exclusively for the people behind cinema. 
            Claim your talent profile or post your project opportunities today.
          </p>

          <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'center', gap: '16px' }}>
            <Link 
              to="/join-now" 
              className="btn btn-lg"
              style={{
                backgroundColor: 'var(--color-white)',
                color: 'var(--color-maroon)',
                fontWeight: 700,
                boxShadow: '0 8px 20px rgba(0, 0, 0, 0.2)'
              }}
            >
              <span>Join TrueHunt</span>
              <ArrowRight size={18} />
            </Link>

            <Link 
              to="/talents" 
              className="btn btn-lg"
              style={{
                backgroundColor: 'transparent',
                color: 'var(--color-white)',
                border: '1.5px solid var(--color-white)',
                fontWeight: 600
              }}
            >
              <span>Explore Talents</span>
            </Link>
          </div>

        </div>
      </section>

    </div>
  );
}
