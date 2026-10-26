import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { 
  Shield, 
  Film, 
  Tv, 
  Clapperboard, 
  Sparkles, 
  ArrowRight, 
  CheckCircle2, 
  FileCheck, 
  DollarSign, 
  Calendar, 
  AlertTriangle, 
  Clock, 
  Users, 
  Lock,
  ChevronRight,
  TrendingUp,
  FolderPlus
} from 'lucide-react';
import { api } from '../services/api';

export default function ProtectProducerLandingPage() {
  const [categories, setCategories] = useState([]);
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    async function loadData() {
      try {
        const cats = api.protectProducer.getCategories();
        setCategories(cats);
        const projs = await api.protectProducer.getProjects();
        setProjects(projs);
      } catch (err) {
        console.error('Error loading Protect Producer data:', err);
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, []);

  const scrollToCategories = () => {
    const el = document.getElementById('production-categories');
    if (el) {
      const navOffset = 80;
      const elementPosition = el.getBoundingClientRect().top;
      const offsetPosition = elementPosition + window.pageYOffset - navOffset;
      window.scrollTo({
        top: offsetPosition,
        behavior: 'smooth'
      });
    }
  };

  const getCategoryCount = (catId) => {
    const formatted = catId.toUpperCase().replace('-', '_');
    return projects.filter(p => p.category.toUpperCase().replace('-', '_') === formatted).length;
  };

  return (
    <div style={{ backgroundColor: 'var(--color-bg-page)', minHeight: '90vh', paddingBottom: '90px' }}>
      
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
          <div style={{ maxWidth: '840px' }}>
            
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
                maxWidth: '720px'
              }}
            >
              Manage every stage of your film and content production lifecycle in one organized workspace, 
              from initial planning to final release. Ensure financial transparency, script and cast chain-of-custody, 
              milestone accountability, and pre-release audit readiness.
            </p>

            {/* Actions */}
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '14px', alignItems: 'center' }}>
              <button 
                id="explore-categories-btn"
                onClick={scrollToCategories} 
                className="btn btn-lg btn-maroon"
                style={{
                  boxShadow: '0 4px 18px rgba(128, 24, 35, 0.35)',
                  gap: '10px'
                }}
              >
                <span>Explore Production Categories</span>
                <ArrowRight size={18} />
              </button>

              <Link 
                to="/protect-producer/movies" 
                className="btn btn-lg btn-outline-subtle"
                style={{
                  backgroundColor: 'rgba(255, 255, 255, 0.08)',
                  color: 'white',
                  borderColor: 'rgba(255, 255, 255, 0.25)',
                  backdropFilter: 'blur(4px)'
                }}
              >
                <span>Browse Active Projects</span>
              </Link>
            </div>

          </div>
        </div>
      </section>

      {/* Trust & Transparency Feature Band */}
      <section style={{ backgroundColor: 'var(--color-white)', borderBottom: '1px solid var(--color-border)', padding: '24px 0' }}>
        <div className="container">
          <div 
            style={{ 
              display: 'grid', 
              gridTemplateColumns: 'repeat(auto-fit, minmax(230px, 1fr))', 
              gap: '20px',
              alignItems: 'center'
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <div style={{ width: '40px', height: '40px', borderRadius: '8px', backgroundColor: 'var(--color-maroon-light)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--color-maroon)', flexShrink: 0 }}>
                <FileCheck size={20} />
              </div>
              <div>
                <div style={{ fontWeight: 700, fontSize: '14px', color: 'var(--color-navy)' }}>Chain-of-Custody</div>
                <div style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>Verified agreements & permits</div>
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <div style={{ width: '40px', height: '40px', borderRadius: '8px', backgroundColor: '#EFF6FF', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#1D4ED8', flexShrink: 0 }}>
                <DollarSign size={20} />
              </div>
              <div>
                <div style={{ fontWeight: 700, fontSize: '14px', color: 'var(--color-navy)' }}>Financial Traceability</div>
                <div style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>Allocated budget vs daily actuals</div>
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <div style={{ width: '40px', height: '40px', borderRadius: '8px', backgroundColor: '#DCFCE7', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#15803D', flexShrink: 0 }}>
                <CheckCircle2 size={20} />
              </div>
              <div>
                <div style={{ fontWeight: 700, fontSize: '14px', color: 'var(--color-navy)' }}>Stage Gate Approvals</div>
                <div style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>Authorized producer sign-offs</div>
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <div style={{ width: '40px', height: '40px', borderRadius: '8px', backgroundColor: '#FEF3C7', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#B45309', flexShrink: 0 }}>
                <AlertTriangle size={20} />
              </div>
              <div>
                <div style={{ fontWeight: 700, fontSize: '14px', color: 'var(--color-navy)' }}>Risk Anticipation</div>
                <div style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>Weather & delay mitigation</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Main 4 Content Categories Section */}
      <section id="production-categories" style={{ padding: '64px 0 40px 0' }}>
        <div className="container">
          
          <div className="section-header">
            <span className="badge badge-maroon" style={{ marginBottom: '10px' }}>Content Categorization</span>
            <h2 className="section-title">Four Tailored Content Frameworks</h2>
            <p className="section-subtitle">
              Choose your content format to access specialized production lifecycle workflows, 
              milestone requirements, budget structures, and release protocols.
            </p>
          </div>

          {/* 4 Large Responsive Content Cards */}
          <div 
            style={{ 
              display: 'grid', 
              gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', 
              gap: '28px',
              marginTop: '32px'
            }}
          >
            {categories.map((cat) => {
              const count = getCategoryCount(cat.id);
              return (
                <div 
                  key={cat.id} 
                  id={`category-card-${cat.id}`}
                  className="card card-hoverable"
                  onClick={() => navigate(`/protect-producer/${cat.id}`)}
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    overflow: 'hidden',
                    borderRadius: 'var(--radius-lg)',
                    border: '1px solid var(--color-border)',
                    backgroundColor: 'var(--color-white)',
                    transition: 'all var(--transition-normal)',
                    cursor: 'pointer'
                  }}
                >
                  {/* Card Visual / Image Banner */}
                  <div 
                    style={{ 
                      position: 'relative', 
                      height: '210px', 
                      overflow: 'hidden', 
                      backgroundColor: 'var(--color-navy-dark)' 
                    }}
                  >
                    <img 
                      src={cat.image} 
                      alt={`${cat.title} Production Category`} 
                      style={{ 
                        width: '100%', 
                        height: '100%', 
                        objectFit: 'cover',
                        transition: 'transform 0.4s ease'
                      }}
                      className="category-card-img"
                    />
                    
                    {/* Dark gradient overlay for typography readability */}
                    <div 
                      style={{
                        position: 'absolute',
                        inset: 0,
                        background: 'linear-gradient(to top, rgba(10, 25, 47, 0.92) 0%, rgba(10, 25, 47, 0.3) 60%, transparent 100%)'
                      }}
                    />

                    {/* Category Top Badge */}
                    <div style={{ position: 'absolute', top: '16px', left: '16px', zIndex: 2 }}>
                      <span 
                        style={{
                          backgroundColor: 'rgba(10, 25, 47, 0.85)',
                          color: '#F8FAFC',
                          fontSize: '11px',
                          fontWeight: 700,
                          padding: '4px 10px',
                          borderRadius: 'var(--radius-full)',
                          border: '1px solid rgba(255, 255, 255, 0.2)',
                          backdropFilter: 'blur(4px)',
                          letterSpacing: '0.04em',
                          textTransform: 'uppercase'
                        }}
                      >
                        {cat.badge}
                      </span>
                    </div>

                    {/* Category Title & Tagline inside banner */}
                    <div style={{ position: 'absolute', bottom: '16px', left: '16px', right: '16px', zIndex: 2 }}>
                      <h3 style={{ fontSize: '24px', color: 'white', fontWeight: 800, marginBottom: '4px' }}>
                        {cat.title}
                      </h3>
                      <p style={{ fontSize: '13px', color: '#FED7AA', fontWeight: 600, fontStyle: 'italic' }}>
                        "{cat.tagline}"
                      </p>
                    </div>
                  </div>

                  {/* Card Content Body */}
                  <div style={{ padding: '22px', flex: 1, display: 'flex', flexDirection: 'column' }}>
                    <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)', lineHeight: 1.6, marginBottom: '18px' }}>
                      {cat.description}
                    </p>

                    {/* Workflow Focus highlight */}
                    <div 
                      style={{ 
                        backgroundColor: 'var(--color-bg-subtle)', 
                        padding: '12px 14px', 
                        borderRadius: 'var(--radius-sm)',
                        fontSize: '12.5px',
                        color: 'var(--color-navy)',
                        borderLeft: '3px solid var(--color-maroon)',
                        marginBottom: '20px'
                      }}
                    >
                      <strong>Workflow Focus:</strong> {cat.workflowFocus}
                    </div>

                    {/* Card Footer with Action */}
                    <div style={{ marginTop: 'auto', paddingTop: '16px', borderTop: '1px solid var(--color-border-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <span style={{ fontSize: '12.5px', fontWeight: 600, color: 'var(--color-text-muted)' }}>
                        {count} Active {count === 1 ? 'Project' : 'Projects'}
                      </span>
                      
                      <Link 
                        id={`manage-projects-${cat.id}`}
                        to={`/protect-producer/${cat.id}`} 
                        className="btn btn-sm btn-maroon"
                        style={{ gap: '6px' }}
                        aria-label={`Manage ${cat.title} Projects`}
                      >
                        <span>Manage Projects</span>
                        <ChevronRight size={15} />
                      </Link>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>

        </div>
      </section>

      {/* Four Stages Lifecycle Walkthrough */}
      <section style={{ backgroundColor: 'var(--color-white)', padding: '64px 0', borderTop: '1px solid var(--color-border)', borderBottom: '1px solid var(--color-border)' }}>
        <div className="container">
          <div className="section-header">
            <span className="badge badge-navy" style={{ marginBottom: '10px' }}>End-to-End Governance</span>
            <h2 className="section-title">The Four Production Lifecycle Workspaces</h2>
            <p className="section-subtitle">
              Every project moves through four synchronized workspaces, safeguarding deliverables and budget at every transition.
            </p>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '24px' }}>
            
            <div style={{ padding: '24px', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)', backgroundColor: 'var(--color-bg-page)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '14px' }}>
                <span style={{ width: '28px', height: '28px', borderRadius: '50%', backgroundColor: 'var(--color-navy)', color: 'white', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 800, fontSize: '13px' }}>1</span>
                <h4 style={{ fontSize: '18px', color: 'var(--color-navy)' }}>Pre-Production</h4>
              </div>
              <p style={{ fontSize: '13.5px', color: 'var(--color-text-secondary)', lineHeight: 1.5, marginBottom: '14px' }}>
                Script breakdown, cast & crew agreements, shoot schedule planning, line budget allocations, and readiness gate signoffs.
              </p>
              <ul style={{ fontSize: '12.5px', color: 'var(--color-text-muted)', listStyle: 'none', paddingLeft: 0, display: 'flex', flexDirection: 'column', gap: '6px' }}>
                <li>✓ Screenplay lock & breakdown</li>
                <li>✓ Location & municipal permits</li>
                <li>✓ Cast exclusivity contracts</li>
              </ul>
            </div>

            <div style={{ padding: '24px', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)', backgroundColor: 'var(--color-bg-page)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '14px' }}>
                <span style={{ width: '28px', height: '28px', borderRadius: '50%', backgroundColor: 'var(--color-maroon)', color: 'white', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 800, fontSize: '13px' }}>2</span>
                <h4 style={{ fontSize: '18px', color: 'var(--color-navy)' }}>Production</h4>
              </div>
              <p style={{ fontSize: '13.5px', color: 'var(--color-text-secondary)', lineHeight: 1.5, marginBottom: '14px' }}>
                Digital call sheets, daily scene coverage, DPR logs, on-set expense tracking against allocated budget, and incident mitigation.
              </p>
              <ul style={{ fontSize: '12.5px', color: 'var(--color-text-muted)', listStyle: 'none', paddingLeft: 0, display: 'flex', flexDirection: 'column', gap: '6px' }}>
                <li>✓ Daily call sheets & DPR logs</li>
                <li>✓ Episode & scene progress</li>
                <li>✓ Budget vs actuals audit</li>
              </ul>
            </div>

            <div style={{ padding: '24px', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)', backgroundColor: 'var(--color-bg-page)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '14px' }}>
                <span style={{ width: '28px', height: '28px', borderRadius: '50%', backgroundColor: '#1D4ED8', color: 'white', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 800, fontSize: '13px' }}>3</span>
                <h4 style={{ fontSize: '18px', color: 'var(--color-navy)' }}>Post-Production</h4>
              </div>
              <p style={{ fontSize: '13.5px', color: 'var(--color-text-secondary)', lineHeight: 1.5, marginBottom: '14px' }}>
                Editorial cuts, VFX pipeline plates, Dolby Atmos audio mix, dubbing supervision, timecoded review notes, and final-cut signoffs.
              </p>
              <ul style={{ fontSize: '12.5px', color: 'var(--color-text-muted)', listStyle: 'none', paddingLeft: 0, display: 'flex', flexDirection: 'column', gap: '6px' }}>
                <li>✓ Rough cut to locked picture</li>
                <li>✓ VFX & Atmos sound design</li>
                <li>✓ Multi-language dubbing QC</li>
              </ul>
            </div>

            <div style={{ padding: '24px', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)', backgroundColor: 'var(--color-bg-page)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '14px' }}>
                <span style={{ width: '28px', height: '28px', borderRadius: '50%', backgroundColor: '#15803D', color: 'white', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 800, fontSize: '13px' }}>4</span>
                <h4 style={{ fontSize: '18px', color: 'var(--color-navy)' }}>Release Management</h4>
              </div>
              <p style={{ fontSize: '13.5px', color: 'var(--color-text-secondary)', lineHeight: 1.5, marginBottom: '14px' }}>
                CBFC censor certificate verification, distributor agreements, theatrical/OTT IMF master packages, promotional milestones, and launch readiness.
              </p>
              <ul style={{ fontSize: '12.5px', color: 'var(--color-text-muted)', listStyle: 'none', paddingLeft: 0, display: 'flex', flexDirection: 'column', gap: '6px' }}>
                <li>✓ Censor / rating clearances</li>
                <li>✓ Theatrical & OTT delivery specs</li>
                <li>✓ Release readiness audit score</li>
              </ul>
            </div>

          </div>
        </div>
      </section>

      {/* Production Risk & Compliance Disclaimer Banner */}
      <section className="container" style={{ marginTop: '48px' }}>
        <div 
          style={{
            backgroundColor: '#FFFBEB',
            border: '1px solid #FDE68A',
            padding: '20px 24px',
            borderRadius: 'var(--radius-md)',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '16px'
          }}
        >
          <Lock size={24} color="#B45309" style={{ flexShrink: 0, marginTop: '2px' }} />
          <div>
            <h4 style={{ fontSize: '15px', color: '#92400E', fontWeight: 700, marginBottom: '4px' }}>
              TrueHunt Protection & Traceability Protocol
            </h4>
            <p style={{ fontSize: '13.5px', color: '#92400E', lineHeight: 1.6 }}>
              Protect Producer provides operational risk management, chain-of-custody tracking, and verified milestone audit trails. 
              The module enforces internal accountability through recorded timestamps, document verification registers, and approval histories. 
              TrueHunt does not act as an escrow agent, does not promise financial return guarantees, and all legal agreements remain subject to independent legal counsel.
            </p>
          </div>
        </div>
      </section>

      {/* Embedded CSS for category card image hover */}
      <style>{`
        .category-card-img:hover {
          transform: scale(1.04);
        }
      `}</style>
    </div>
  );
}
