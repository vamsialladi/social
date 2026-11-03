import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Search, Briefcase, Filter, MapPin, Building2, Clock, RotateCcw } from 'lucide-react';
import OpportunityCard from '../components/cards/OpportunityCard';
import { LATEST_OPPORTUNITIES, FILM_CRAFTS } from '../services/mockData';
import { api } from '../services/api';

export default function JobsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [jobs, setJobs] = useState(LATEST_OPPORTUNITIES);
  const [searchQuery, setSearchQuery] = useState(searchParams.get('q') || '');
  const [selectedRole, setSelectedRole] = useState(searchParams.get('category') || 'All');
  const [selectedType, setSelectedType] = useState('All');
  const [selectedLocation, setSelectedLocation] = useState('All');

  useEffect(() => {
    async function loadJobs() {
      try {
        const data = await api.jobs.getAll();
        if (data) setJobs(data);
      } catch (e) {
        console.warn('Using mock jobs');
      }
    }
    loadJobs();
  }, []);

  const handleRoleFilter = (role) => {
    setSelectedRole(role);
    const p = new URLSearchParams(searchParams);
    if (role !== 'All') p.set('category', role);
    else p.delete('category');
    setSearchParams(p);
  };

  const resetFilters = () => {
    setSearchQuery('');
    setSelectedRole('All');
    setSelectedType('All');
    setSelectedLocation('All');
    setSearchParams({});
  };

  const filteredJobs = jobs.filter((job) => {
    const matchQuery = !searchQuery || 
      job.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      job.productionCompany.toLowerCase().includes(searchQuery.toLowerCase()) ||
      job.description.toLowerCase().includes(searchQuery.toLowerCase());
    
    const matchRole = selectedRole === 'All' || job.roleCategory.toLowerCase() === selectedRole.toLowerCase();
    const matchType = selectedType === 'All' || job.type.toLowerCase().includes(selectedType.toLowerCase());
    const matchLocation = selectedLocation === 'All' || job.location.toLowerCase().includes(selectedLocation.toLowerCase());

    return matchQuery && matchRole && matchType && matchLocation;
  });

  return (
    <div style={{ backgroundColor: 'var(--color-bg-page)', minHeight: '85vh', paddingBottom: '80px' }}>
      
      {/* Banner */}
      <section style={{ backgroundColor: 'var(--color-navy)', color: 'white', padding: '48px 0', borderBottom: '3px solid var(--color-maroon)' }}>
        <div className="container">
          <span className="badge badge-maroon" style={{ marginBottom: '10px' }}>Auditions & Casting Board</span>
          <h1 style={{ fontSize: '36px', color: 'white', marginBottom: '10px' }}>
            Film Opportunities & Industry Jobs
          </h1>
          <p style={{ fontSize: '16px', color: '#CBD5E1', maxWidth: '680px', lineHeight: '1.6' }}>
            Discover auditions, feature film crew calls, post-production contracts, and entertainment careers 
            posted by verified production houses and casting directors.
          </p>
        </div>
      </section>

      {/* Main Layout */}
      <div className="container" style={{ marginTop: '32px' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '32px' }}>
          
          {/* Left: Filter Sidebar */}
          <aside 
            className="card"
            style={{
              padding: '24px',
              borderRadius: 'var(--radius-md)',
              backgroundColor: 'var(--color-white)',
              border: '1px solid var(--color-border)',
              height: 'fit-content',
              position: 'sticky',
              top: '90px'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px', paddingBottom: '12px', borderBottom: '1px solid var(--color-border)' }}>
              <span style={{ fontWeight: 700, color: 'var(--color-navy)', fontSize: '16px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Filter size={16} /> Filter Openings
              </span>
              <button onClick={resetFilters} className="btn btn-sm btn-ghost" style={{ fontSize: '12px', color: 'var(--color-maroon)' }}>
                <RotateCcw size={12} /> Reset
              </button>
            </div>

            {/* Department Filter */}
            <div className="form-group">
              <label className="form-label">Department / Craft</label>
              <select 
                className="form-control"
                value={selectedRole}
                onChange={(e) => handleRoleFilter(e.target.value)}
              >
                <option value="All">All Departments</option>
                {FILM_CRAFTS.map(c => (
                  <option key={c.id} value={c.title}>{c.title}</option>
                ))}
              </select>
            </div>

            {/* Opportunity Type */}
            <div className="form-group">
              <label className="form-label">Engagement Type</label>
              <select 
                className="form-control"
                value={selectedType}
                onChange={(e) => setSelectedType(e.target.value)}
              >
                <option value="All">All Types</option>
                <option value="Production Contract">Feature Production Contract</option>
                <option value="Writers Room">Writers Room Retainer</option>
                <option value="Independent Production">Independent Film Contract</option>
                <option value="Project Contract">Studio / VFX Project Contract</option>
              </select>
            </div>

            {/* Location */}
            <div className="form-group">
              <label className="form-label">Shoot Location / Hub</label>
              <select 
                className="form-control"
                value={selectedLocation}
                onChange={(e) => setSelectedLocation(e.target.value)}
              >
                <option value="All">All Locations</option>
                <option value="Hyderabad">Hyderabad</option>
                <option value="Mumbai">Mumbai</option>
                <option value="Shimla">Shimla & Hills</option>
                <option value="Bengaluru">Bengaluru</option>
                <option value="Chennai">Chennai</option>
              </select>
            </div>

          </aside>

          {/* Right: Results */}
          <main style={{ flex: 1, gridColumn: 'span 2' }}>
            
            {/* Search Bar */}
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
                placeholder="Search job title, director requirement, or production banner..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="form-control"
                style={{ border: 'none', padding: '6px 0', fontSize: '15px' }}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
              <span style={{ fontSize: '14px', fontWeight: 600, color: 'var(--color-navy)' }}>
                {filteredJobs.length} Active Opportunities Available
              </span>
              <span className="badge badge-demo">Verified Studio Calls</span>
            </div>

            {filteredJobs.length === 0 ? (
              <div className="card" style={{ padding: '48px 24px', textAlign: 'center', borderRadius: 'var(--radius-md)', backgroundColor: 'var(--color-white)' }}>
                <Briefcase size={44} color="var(--color-text-light)" style={{ margin: '0 auto 16px auto' }} />
                <h3 style={{ fontSize: '18px', color: 'var(--color-navy)', marginBottom: '8px' }}>No opportunities found</h3>
                <p style={{ fontSize: '14px', color: 'var(--color-text-muted)', marginBottom: '20px' }}>
                  Try resetting your search query or selecting a different department.
                </p>
                <button onClick={resetFilters} className="btn btn-sm btn-maroon">
                  Reset Filters
                </button>
              </div>
            ) : (
              <div 
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
                  gap: '24px'
                }}
              >
                {filteredJobs.map((job) => (
                  <OpportunityCard key={job.id} job={job} />
                ))}
              </div>
            )}

          </main>

        </div>
      </div>

    </div>
  );
}
