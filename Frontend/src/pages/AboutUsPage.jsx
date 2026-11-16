import React from 'react';
import { 
  Film, 
  Shield, 
  TrendingUp, 
  Smartphone, 
  Users, 
  Building2, 
  MapPin, 
  Mail, 
  Phone, 
  ExternalLink,
  ArrowRight,
  CheckCircle2
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { OFFICIAL_CONTACT } from '../constants/contactInfo';
import OfficeContactCard from '../components/common/OfficeContactCard';

export default function AboutUsPage() {
  return (
    <div style={{ backgroundColor: 'var(--color-bg-page)', minHeight: '85vh', paddingBottom: '80px' }}>
      
      {/* Banner */}
      <section 
        style={{ 
          backgroundColor: 'var(--color-navy)', 
          color: 'white', 
          padding: '60px 0', 
          borderBottom: '3px solid var(--color-maroon)' 
        }}
      >
        <div className="container">
          <div style={{ maxWidth: '800px' }}>
            <span 
              className="badge badge-maroon" 
              style={{ marginBottom: '12px', display: 'inline-flex', alignItems: 'center', gap: '6px' }}
            >
              <Film size={13} />
              <span>About TrueHunt • truehunt.in</span>
            </span>
            <h1 style={{ fontSize: 'clamp(32px, 4vw, 44px)', color: 'white', marginBottom: '14px', fontWeight: 800 }}>
              The Cinema Industry Professional Network
            </h1>
            <p style={{ fontSize: '17px', color: '#CBD5E1', lineHeight: '1.6', margin: 0 }}>
              TrueHunt connects cinema creators, technicians, production houses, and film financiers 
              on a verified professional platform purpose-built for the film and entertainment ecosystem.
            </p>
          </div>
        </div>
      </section>

      <div className="container" style={{ marginTop: '40px' }}>
        
        {/* Mission & Purpose */}
        <div 
          className="card"
          style={{
            padding: '36px',
            backgroundColor: 'var(--color-white)',
            borderRadius: 'var(--radius-md)',
            border: '1px solid var(--color-border)',
            marginBottom: '40px',
            boxShadow: 'var(--shadow-card)'
          }}
        >
          <div style={{ maxWidth: '840px' }}>
            <span style={{ fontSize: '12px', fontWeight: 700, color: 'var(--color-maroon)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Our Mission
            </span>
            <h2 style={{ fontSize: '26px', color: 'var(--color-navy)', margin: '8px 0 16px 0', fontWeight: 700 }}>
              Empowering Filmmaking with Structure, Governance & Verified Opportunity
            </h2>
            <p style={{ fontSize: '15px', color: 'var(--color-text-secondary)', lineHeight: '1.8', marginBottom: '16px' }}>
              Unlike generic consumer social networks, TrueHunt recognizes that cinema production is an intricate, 
              high-capital industry requiring authentic credentials, confidential casting pipelines, and rigorous 
              production lifecycle management.
            </p>
            <p style={{ fontSize: '15px', color: 'var(--color-text-secondary)', lineHeight: '1.8', margin: 0 }}>
              From indie short films and television serials to pan-India theatrical blockbusters, TrueHunt provides 
              the digital infrastructure to discover talent, recruit crews, coordinate on-set operations with the 
              LCM App, and protect producer investments with the Protect Producer module.
            </p>
          </div>
        </div>

        {/* 4 Core Pillars Grid */}
        <div 
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))',
            gap: '24px',
            marginBottom: '48px'
          }}
        >
          
          <div className="card" style={{ padding: '24px', backgroundColor: 'var(--color-white)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)' }}>
            <Users size={28} color="var(--color-navy)" style={{ marginBottom: '12px' }} />
            <h3 style={{ fontSize: '18px', color: 'var(--color-navy)', marginBottom: '8px', fontWeight: 700 }}>
              Talent Directory
            </h3>
            <p style={{ fontSize: '13px', color: 'var(--color-text-secondary)', lineHeight: '1.6' }}>
              Verified portfolios for actors, cinematographers, directors, composers, and 12 distinct film crafts across Indian and international cinema.
            </p>
          </div>

          <div className="card" style={{ padding: '24px', backgroundColor: 'var(--color-white)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)' }}>
            <Shield size={28} color="var(--color-maroon)" style={{ marginBottom: '12px' }} />
            <h3 style={{ fontSize: '18px', color: 'var(--color-navy)', marginBottom: '8px', fontWeight: 700 }}>
              Protect Producer
            </h3>
            <p style={{ fontSize: '13px', color: 'var(--color-text-secondary)', lineHeight: '1.6' }}>
              End-to-end production governance tracking Pre-Production, Production, Post-Production, and Release Management across Movies, Serials, Web Series, and Shorts.
            </p>
          </div>

          <div className="card" style={{ padding: '24px', backgroundColor: 'var(--color-white)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)' }}>
            <Smartphone size={28} color="#0284C7" style={{ marginBottom: '12px' }} />
            <h3 style={{ fontSize: '18px', color: 'var(--color-navy)', marginBottom: '8px', fontWeight: 700 }}>
              LCM Production App
            </h3>
            <p style={{ fontSize: '13px', color: 'var(--color-text-secondary)', lineHeight: '1.6' }}>
              Location, Crew & Movie Management system powering daily call sheets, digital production reports (DPR), and on-set workflows.
            </p>
          </div>

          <div className="card" style={{ padding: '24px', backgroundColor: 'var(--color-white)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)' }}>
            <TrendingUp size={28} color="#16A34A" style={{ marginBottom: '12px' }} />
            <h3 style={{ fontSize: '18px', color: 'var(--color-navy)', marginBottom: '8px', fontWeight: 700 }}>
              Investors Connect
            </h3>
            <p style={{ fontSize: '13px', color: 'var(--color-text-secondary)', lineHeight: '1.6' }}>
              Facilitates transparent discovery between packaged cinematic slates and accredited media financiers and production banners.
            </p>
          </div>

        </div>

        {/* Corporate Headquarters & Office Location Section */}
        <div style={{ marginBottom: '40px' }}>
          <div style={{ marginBottom: '20px' }}>
            <span className="badge badge-navy" style={{ marginBottom: '8px' }}>Official Company Details</span>
            <h2 style={{ fontSize: '24px', color: 'var(--color-navy)', margin: 0, fontWeight: 700 }}>
              Corporate Headquarters & Office Information
            </h2>
          </div>

          <div 
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
              gap: '28px'
            }}
          >
            {/* Reusable Office Contact Card */}
            <OfficeContactCard variant="card" showMapButton={true} />

            {/* Office Context & Operational Hub */}
            <div 
              className="card"
              style={{
                padding: '32px',
                backgroundColor: 'var(--color-white)',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--color-border)'
              }}
            >
              <h3 style={{ fontSize: '18px', color: 'var(--color-navy)', marginBottom: '14px', fontWeight: 700 }}>
                Operations & Technology Hub
              </h3>
              <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)', lineHeight: '1.7', marginBottom: '20px' }}>
                TrueHunt is headquartered in the technology corridor of Hyderabad, Telangana. 
                Our team collaborates with film studios, talent agencies, regional production guilds, 
                and technology partners to bring enterprise-grade stability to media workflows.
              </p>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginBottom: '24px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '14px', color: 'var(--color-navy)' }}>
                  <CheckCircle2 size={16} color="#16A34A" />
                  <span>Central production governance & media infrastructure desk</span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '14px', color: 'var(--color-navy)' }}>
                  <CheckCircle2 size={16} color="#16A34A" />
                  <span>Cinema technology engineering & verified roster management</span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '14px', color: 'var(--color-navy)' }}>
                  <CheckCircle2 size={16} color="#16A34A" />
                  <span>Executive inquiries & studio enterprise agreements</span>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '12px' }}>
                <Link to="/contact-us" className="btn btn-maroon btn-sm">
                  <span>Contact Our Office</span>
                  <ArrowRight size={14} />
                </Link>
                <Link to="/get-support" className="btn btn-outline-navy btn-sm">
                  <span>Support Center</span>
                </Link>
              </div>
            </div>

          </div>
        </div>

      </div>

    </div>
  );
}
