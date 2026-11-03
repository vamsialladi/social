import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { 
  ArrowRight, 
  Users,
  TrendingUp,
  Smartphone
} from 'lucide-react';
import CinematicHero from '../components/hero/CinematicHero';
import { PLATFORM_STATS } from '../services/mockData';
import { api } from '../services/api';

export default function HomePage() {
  const [stats, setStats] = useState(PLATFORM_STATS);

  useEffect(() => {
    async function loadData() {
  try {
    const fetchedStats = await api.stats.get();
    if (fetchedStats) setStats(fetchedStats);
  } catch {
    console.log('Using baseline demo data');
  }
    }
    loadData();
  }, []);

  return (
    <div className="landing-page">

      {/* 3D Cinematic Earth Globe & Rotating Film Strip Hero Banner */}
      <CinematicHero />

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

    </div>
  );
}
