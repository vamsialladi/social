import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Search, Filter, SlidersHorizontal, Users, MapPin, X, RotateCcw } from 'lucide-react';
import TalentCard from '../components/cards/TalentCard';
import { FEATURED_TALENTS, FILM_CRAFTS } from '../services/mockData';
import { api } from '../services/api';

export default function TalentsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [talents, setTalents] = useState(FEATURED_TALENTS);
  const [loading, setLoading] = useState(false);

  // Filter States
  const [searchQuery, setSearchQuery] = useState(searchParams.get('q') || '');
  const [selectedCategory, setSelectedCategory] = useState(searchParams.get('category') || 'All');
  const [selectedLocation, setSelectedLocation] = useState(searchParams.get('location') || 'All');
  const [experienceLevel, setExperienceLevel] = useState('All');
  const [onlyVerified, setOnlyVerified] = useState(false);

  useEffect(() => {
    // If search params change externally, update filters
    const q = searchParams.get('q');
    const cat = searchParams.get('category');
    if (q !== null) setSearchQuery(q);
    if (cat !== null) {
      // Find matching craft
      const matched = FILM_CRAFTS.find(c => c.title.toLowerCase() === cat.toLowerCase() || c.id === cat.toLowerCase());
      setSelectedCategory(matched ? matched.title : cat);
    }
  }, [searchParams]);

  useEffect(() => {
    async function fetchTalents() {
      setLoading(true);
      try {
        const data = await api.talents.search(searchQuery, selectedCategory === 'All' ? '' : selectedCategory, selectedLocation === 'All' ? '' : selectedLocation);
        if (data) setTalents(data);
      } catch (err) {
        console.warn('Fallback search');
      } finally {
        setLoading(false);
      }
    }
    fetchTalents();
  }, [searchQuery, selectedCategory, selectedLocation]);

  const handleFilterChange = (key, value) => {
    const params = new URLSearchParams(searchParams);
    if (value && value !== 'All') {
      params.set(key, value);
    } else {
      params.delete(key);
    }
    setSearchParams(params);
  };

  const resetFilters = () => {
    setSearchQuery('');
    setSelectedCategory('All');
    setSelectedLocation('All');
    setExperienceLevel('All');
    setOnlyVerified(false);
    setSearchParams({});
  };

  const locationsList = ['All', 'Mumbai', 'Hyderabad', 'Chennai', 'Bengaluru', 'Kolkata', 'Pune', 'Kochi'];

  const filteredTalents = talents.filter(t => {
    if (onlyVerified && !t.verified) return false;
    if (experienceLevel !== 'All') {
      const years = parseInt(t.experience);
      if (experienceLevel === '1-3' && (years < 1 || years > 3)) return false;
      if (experienceLevel === '4-7' && (years < 4 || years > 7)) return false;
      if (experienceLevel === '8+' && years < 8) return false;
    }
    return true;
  });

  return (
    <div style={{ backgroundColor: 'var(--color-bg-page)', minHeight: '85vh', paddingBottom: '80px' }}>
      
      {/* Banner */}
      <section style={{ backgroundColor: 'var(--color-navy)', color: 'white', padding: '48px 0', borderBottom: '3px solid var(--color-maroon)' }}>
        <div className="container">
          <span className="badge badge-maroon" style={{ marginBottom: '10px' }}>Talent Discovery Directory</span>
          <h1 style={{ fontSize: '36px', color: 'white', marginBottom: '10px' }}>
            Film & Entertainment Talents
          </h1>
          <p style={{ fontSize: '16px', color: '#CBD5E1', maxWidth: '680px', lineHeight: '1.6' }}>
            Discover verified actors, cinematographers, directors, technicians, and post-production masters 
            across India and international cinema industries.
          </p>
        </div>
      </section>

      {/* Main Filter & Results */}
      <div className="container" style={{ marginTop: '32px' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '32px' }}>
          
          {/* Left Column: Filter Sidebar */}
          <aside 
            className="card"
            style={{
              padding: '24px',
              borderRadius: 'var(--radius-md)',
              height: 'fit-content',
              backgroundColor: 'var(--color-white)',
              border: '1px solid var(--color-border)',
              position: 'sticky',
              top: '90px'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', paddingBottom: '12px', borderBottom: '1px solid var(--color-border)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 700, color: 'var(--color-navy)', fontSize: '16px' }}>
                <SlidersHorizontal size={18} />
                <span>Filter Directory</span>
              </div>
              <button 
                onClick={resetFilters} 
                className="btn btn-sm btn-ghost" 
                style={{ fontSize: '12px', color: 'var(--color-maroon)', padding: '4px 8px' }}
                title="Reset all filters"
              >
                <RotateCcw size={12} /> Reset
              </button>
            </div>

            {/* Role / Craft Category Filter */}
            <div className="form-group">
              <label className="form-label">Film Craft / Department</label>
              <select 
                className="form-control"
                value={selectedCategory}
                onChange={(e) => {
                  setSelectedCategory(e.target.value);
                  handleFilterChange('category', e.target.value);
                }}
              >
                <option value="All">All Departments</option>
                {FILM_CRAFTS.map(c => (
                  <option key={c.id} value={c.title}>{c.title}</option>
                ))}
              </select>
            </div>

            {/* Location Filter */}
            <div className="form-group">
              <label className="form-label">Location / Hub</label>
              <select 
                className="form-control"
                value={selectedLocation}
                onChange={(e) => {
                  setSelectedLocation(e.target.value);
                  handleFilterChange('location', e.target.value);
                }}
              >
                {locationsList.map(loc => (
                  <option key={loc} value={loc}>{loc === 'All' ? 'All Locations' : loc}</option>
                ))}
              </select>
            </div>

            {/* Experience Level */}
            <div className="form-group">
              <label className="form-label">Experience Range</label>
              <select 
                className="form-control"
                value={experienceLevel}
                onChange={(e) => setExperienceLevel(e.target.value)}
              >
                <option value="All">Any Experience</option>
                <option value="1-3">1 - 3 Years (Emerging)</option>
                <option value="4-7">4 - 7 Years (Mid-Level)</option>
                <option value="8+">8+ Years (Veteran / Senior)</option>
              </select>
            </div>

            {/* Only Verified Checkbox */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginTop: '10px', paddingTop: '14px', borderTop: '1px solid var(--color-border)' }}>
              <input 
                type="checkbox"
                id="onlyVerified"
                checked={onlyVerified}
                onChange={(e) => setOnlyVerified(e.target.checked)}
                style={{ width: '16px', height: '16px', accentColor: 'var(--color-navy)', cursor: 'pointer' }}
              />
              <label htmlFor="onlyVerified" style={{ fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', cursor: 'pointer' }}>
                Verified TrueHunt Profiles Only
              </label>
            </div>

          </aside>

          {/* Right Column: Search & Results Grid */}
          <main style={{ flex: 1, gridColumn: 'span 2' }}>
            
            {/* Search Input Bar */}
            <div 
              className="card"
              style={{
                padding: '12px 18px',
                borderRadius: 'var(--radius-md)',
                display: 'flex',
                alignItems: 'center',
                gap: '12px',
                marginBottom: '24px',
                backgroundColor: 'var(--color-white)',
                border: '1px solid var(--color-border)'
              }}
            >
              <Search size={20} color="var(--color-text-muted)" />
              <input 
                type="text"
                placeholder="Search talent name, specific camera (e.g. Alexa), skill, or dialect..."
                value={searchQuery}
                onChange={(e) => {
                  setSearchQuery(e.target.value);
                  handleFilterChange('q', e.target.value);
                }}
                className="form-control"
                style={{ border: 'none', padding: '6px 0', fontSize: '15px' }}
              />
              {searchQuery && (
                <button 
                  onClick={() => {
                    setSearchQuery('');
                    handleFilterChange('q', '');
                  }}
                  style={{ color: 'var(--color-text-muted)', cursor: 'pointer' }}
                >
                  <X size={18} />
                </button>
              )}
            </div>

            {/* Result Stats Bar */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
              <span style={{ fontSize: '14px', fontWeight: 600, color: 'var(--color-navy)' }}>
                Showing {filteredTalents.length} Cinema Professionals
              </span>
              <span style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>
                Sorted by: Filmography Activity & Availability
              </span>
            </div>

            {/* Talents Grid */}
            {filteredTalents.length === 0 ? (
              <div 
                className="card" 
                style={{ 
                  padding: '48px 24px', 
                  textAlign: 'center', 
                  borderRadius: 'var(--radius-md)', 
                  backgroundColor: 'var(--color-white)' 
                }}
              >
                <Users size={48} color="var(--color-text-light)" style={{ margin: '0 auto 16px auto' }} />
                <h3 style={{ fontSize: '18px', color: 'var(--color-navy)', marginBottom: '8px' }}>
                  No profiles matched your filter criteria
                </h3>
                <p style={{ fontSize: '14px', color: 'var(--color-text-muted)', marginBottom: '20px' }}>
                  Try broadening your search term or resetting the craft filters.
                </p>
                <button onClick={resetFilters} className="btn btn-sm btn-maroon">
                  Reset All Filters
                </button>
              </div>
            ) : (
              <div 
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
                  gap: '24px'
                }}
              >
                {filteredTalents.map((talent) => (
                  <TalentCard key={talent.id} talent={talent} />
                ))}
              </div>
            )}

          </main>

        </div>
      </div>

    </div>
  );
}
