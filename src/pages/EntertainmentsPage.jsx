import React, { useState, useEffect } from 'react';
import { Building2, Film, MapPin, CheckCircle2, Search, ExternalLink, Users, Calendar } from 'lucide-react';
import { ENTERTAINMENTS_DATA } from '../services/mockData';
import { api } from '../services/api';

export default function EntertainmentsPage() {
  const [studios, setStudios] = useState(ENTERTAINMENTS_DATA);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState('All');
  const [selectedStudio, setSelectedStudio] = useState(null);

  useEffect(() => {
    async function loadStudios() {
      try {
        const data = await api.entertainments.getAll();
        if (data) setStudios(data);
      } catch (e) {
        console.warn('Using mock entertainment data');
      }
    }
    loadStudios();
  }, []);

  const filteredStudios = studios.filter((s) => {
    const matchSearch = s.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      s.headquarters.toLowerCase().includes(searchTerm.toLowerCase()) ||
      s.recentProjects.some(p => p.toLowerCase().includes(searchTerm.toLowerCase()));
    const matchType = filterType === 'All' || s.type.toLowerCase().includes(filterType.toLowerCase());
    return matchSearch && matchType;
  });

  return (
    <div style={{ backgroundColor: 'var(--color-bg-page)', minHeight: '80vh', paddingBottom: '80px' }}>

      {/* Header Banner */}
      <section style={{ backgroundColor: 'var(--color-navy)', color: 'white', padding: '54px 0', borderBottom: '3px solid var(--color-maroon)' }}>
        <div className="container">
          <div style={{ maxWidth: '720px' }}>
            <span className="badge badge-maroon" style={{ marginBottom: '12px' }}>Film Studios & Banners</span>
            <h1 style={{ fontSize: '36px', color: 'white', marginBottom: '14px' }}>
              Entertainments & Production Houses
            </h1>
            <p style={{ fontSize: '16px', color: '#CBD5E1', lineHeight: '1.6' }}>
              Discover prominent film studios, independent banners, and VFX houses actively developing
              theatrical, OTT, and regional cinematic projects.
            </p>
          </div>
        </div>
      </section>

      {/* Main Content & Search Filters */}
      <div className="container" style={{ marginTop: '-24px' }}>

        {/* Search & Filter Bar */}
        <div
          className="card"
          style={{
            padding: '18px 24px',
            borderRadius: 'var(--radius-md)',
            boxShadow: 'var(--shadow-card)',
            display: 'flex',
            flexWrap: 'wrap',
            gap: '16px',
            alignItems: 'center',
            justifyContent: 'space-between',
            marginBottom: '32px'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flex: '1 1 300px' }}>
            <Search size={18} color="var(--color-text-muted)" />
            <input
              type="text"
              placeholder="Search by production house name, city, or blockbuster title..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="form-control"
              style={{ border: 'none', padding: '6px 0', fontSize: '14px' }}
            />
          </div>

          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
            {['All', 'Major Production House', 'Studio & VFX Facility', 'Independent Film Studio'].map((type) => (
              <button
                key={type}
                onClick={() => setFilterType(type)}
                className={`btn btn-sm ${filterType === type ? 'btn-maroon' : 'btn-outline-subtle'}`}
              >
                {type}
              </button>
            ))}
          </div>
        </div>

        {/* Studios Grid */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(360px, 1fr))',
            gap: '28px'
          }}
        >
          {filteredStudios.map((studio) => (
            <div
              key={studio.id}
              className="card card-hoverable"
              style={{
                borderRadius: 'var(--radius-md)',
                overflow: 'hidden',
                display: 'flex',
                flexDirection: 'column',
                height: '100%'
              }}
            >
              {/* Studio Banner */}
              <div
                style={{
                  height: '140px',
                  backgroundImage: `url(${studio.bannerImage})`,
                  backgroundSize: 'cover',
                  backgroundPosition: 'center',
                  position: 'relative'
                }}
              >
                <div style={{ position: 'absolute', inset: 0, backgroundColor: 'rgba(10, 25, 47, 0.45)' }} />
                <span
                  className="badge"
                  style={{
                    position: 'absolute',
                    top: '12px',
                    right: '12px',
                    backgroundColor: 'rgba(255, 255, 255, 0.9)',
                    color: 'var(--color-navy)',
                    fontWeight: 600
                  }}
                >
                  {studio.type}
                </span>
              </div>

              {/* Studio Body */}
              <div style={{ padding: '24px', flex: 1, display: 'flex', flexDirection: 'column' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '8px' }}>
                  <h3 style={{ fontSize: '20px', color: 'var(--color-navy)', margin: 0 }}>
                    {studio.name}
                  </h3>
                  {studio.verified && (
                    <CheckCircle2 size={18} fill="#2563EB" color="#FFFFFF" title="Verified Studio Banner" />
                  )}
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px', color: 'var(--color-text-muted)', marginBottom: '14px' }}>
                  <MapPin size={14} />
                  <span>{studio.headquarters}</span>
                </div>

                <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)', lineHeight: '1.5', marginBottom: '18px', flex: 1 }}>
                  {studio.description}
                </p>

                {/* Recent Projects Tag */}
                <div style={{ marginBottom: '20px' }}>
                  <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--color-text-muted)', display: 'block', marginBottom: '6px' }}>
                    Notable Releases & Projects:
                  </span>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                    {studio.recentProjects.map((p, idx) => (
                      <span key={idx} className="badge" style={{ backgroundColor: '#F1F5F9', color: 'var(--color-navy)' }}>
                        <Film size={11} /> {p}
                      </span>
                    ))}
                  </div>
                </div>

                {/* Bottom Actions */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '16px', borderTop: '1px solid var(--color-border)' }}>
                  <span style={{ fontSize: '13px', color: 'var(--color-maroon)', fontWeight: 600 }}>
                    {studio.openRosters} Active Production Calls
                  </span>
                  <button
                    onClick={() => setSelectedStudio(studio)}
                    className="btn btn-sm btn-outline-navy"
                  >
                    <span>View Roster</span>
                  </button>
                </div>

              </div>
            </div>
          ))}
        </div>

      </div>

      {/* Studio Detail Modal */}
      {selectedStudio && (
        <div className="modal-overlay" onClick={() => setSelectedStudio(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div style={{ marginBottom: '16px' }}>
              <span className="badge badge-maroon">{selectedStudio.type}</span>
              <h2 style={{ fontSize: '22px', color: 'var(--color-navy)', marginTop: '8px' }}>
                {selectedStudio.name}
              </h2>
              <p style={{ fontSize: '13px', color: 'var(--color-text-muted)' }}>
                Headquarters: {selectedStudio.headquarters}
              </p>
            </div>

            <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)', lineHeight: '1.6', marginBottom: '20px' }}>
              {selectedStudio.description}
            </p>

            <h4 style={{ fontSize: '16px', color: 'var(--color-navy)', marginBottom: '10px' }}>Production Pipeline & Calls</h4>
            <div style={{ backgroundColor: '#F8FAFC', padding: '16px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--color-border)', marginBottom: '20px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#166534', fontWeight: 600, fontSize: '14px', marginBottom: '6px' }}>
                <CheckCircle2 size={16} /> Currently accepting showreels for upcoming feature projects
              </div>
              <p style={{ fontSize: '13px', color: 'var(--color-text-muted)', margin: 0 }}>
                Department leads review TrueHunt profiles directly when filling open positions.
              </p>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
              <button onClick={() => setSelectedStudio(null)} className="btn btn-outline-subtle">
                Close
              </button>
              <a href="/jobs" className="btn btn-maroon">
                Browse Active Casting Calls
              </a>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
