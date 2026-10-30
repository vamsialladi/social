import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import {
  Shield,
  Film,
  Tv,
  Clapperboard,
  ArrowLeft,
  Plus,
  Search,
  Filter,
  Calendar,
  DollarSign,
  CheckCircle2,
  AlertTriangle,
  FileText,
  ChevronRight,
  Users,
  Clock,
  Sparkles,
  X
} from 'lucide-react';
import { api } from '../services/api';
import { useNotification } from '../context/NotificationContext';

export default function ProtectCategoryPage() {
  const { category } = useParams();
  const navigate = useNavigate();
  const { showNotification } = useNotification();

  const [categoryData, setCategoryData] = useState(null);
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [showCreateModal, setShowCreateModal] = useState(false);

  // Form states for new project
  const [title, setTitle] = useState('');
  const [productionCompany, setProductionCompany] = useState('');
  const [producerName, setProducerName] = useState('');
  const [producerEmail, setProducerEmail] = useState('');
  const [synopsis, setSynopsis] = useState('');
  const [totalBudget, setTotalBudget] = useState('');
  const [spentBudget, setSpentBudget] = useState('');
  const [startDate, setStartDate] = useState('');
  const [targetReleaseDate, setTargetReleaseDate] = useState('');
  const [seasonNumber, setSeasonNumber] = useState('');
  const [totalEpisodes, setTotalEpisodes] = useState('');
  const [targetFestival, setTargetFestival] = useState('');
  const [targetRuntime, setTargetRuntime] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    loadCategoryAndProjects();
  }, [category]);

  const loadCategoryAndProjects = async () => {
    setLoading(true);
    try {
      const allCategories = api.protectProducer.getCategories();
      const currentCat = allCategories.find(c => c.id === category) || allCategories[0];
      setCategoryData(currentCat);

      const projs = await api.protectProducer.getProjects(currentCat.id);
      setProjects(projs);
    } catch (err) {
      console.error('Failed to load category projects:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateProject = async (e) => {
    e.preventDefault();
    if (!title.trim()) {
      showNotification('Project title is required', 'error');
      return;
    }
    setSubmitting(true);
    try {
      const res = await api.protectProducer.createProject({
        category: categoryData.id.toUpperCase().replace('-', '_'),
        title: title.trim(),
        productionCompany: productionCompany.trim() || 'TrueHunt Production House',
        producerName: producerName.trim() || 'Lead Producer',
        producerEmail: producerEmail.trim() || 'producer@truehunt.in',
        synopsis: synopsis.trim(),
        totalBudget: totalBudget.trim() || '₹1,50,00,000',
        spentBudget: spentBudget.trim() || '₹15,00,000',
        startDate: startDate || new Date().toISOString().split('T')[0],
        targetReleaseDate: targetReleaseDate || '2027-06-30',
        seasonNumber: seasonNumber ? parseInt(seasonNumber) : null,
        totalEpisodes: totalEpisodes ? parseInt(totalEpisodes) : null,
        targetFestival: targetFestival.trim() || null,
        targetRuntime: targetRuntime.trim() || '120 mins',
        bannerImageUrl: categoryData.image
      });

      showNotification(`Project "${title}" registered successfully in ${categoryData.title}!`, 'success');
      setShowCreateModal(false);
      resetForm();
      loadCategoryAndProjects();

      // Navigate to the new project dashboard if ID returned
      if (res?.data?.id) {
        navigate(`/protect-producer/${categoryData.id}/projects/${res.data.id}`);
      }
    } catch (err) {
      showNotification('Failed to create project: ' + err.message, 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const resetForm = () => {
    setTitle('');
    setProductionCompany('');
    setProducerName('');
    setProducerEmail('');
    setSynopsis('');
    setTotalBudget('');
    setSpentBudget('');
    setStartDate('');
    setTargetReleaseDate('');
    setSeasonNumber('');
    setTotalEpisodes('');
    setTargetFestival('');
    setTargetRuntime('');
  };

  const filteredProjects = projects.filter(p => {
    const matchesSearch = !searchQuery ||
      p.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.productionCompany?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.producerName?.toLowerCase().includes(searchQuery.toLowerCase());

    const matchesStatus = statusFilter === 'ALL' || p.status.toUpperCase() === statusFilter.toUpperCase();
    return matchesSearch && matchesStatus;
  });

  const getStageBadgeStyle = (stage) => {
    switch (stage) {
      case 'PRE_PRODUCTION': return { bg: '#EFF6FF', text: '#1D4ED8', label: 'Stage 1: Pre-Production' };
      case 'PRODUCTION': return { bg: 'var(--color-maroon-light)', text: 'var(--color-maroon)', label: 'Stage 2: Production' };
      case 'POST_PRODUCTION': return { bg: '#FDF4FF', text: '#9333EA', label: 'Stage 3: Post-Production' };
      case 'RELEASE_MANAGEMENT': return { bg: '#DCFCE7', text: '#15803D', label: 'Stage 4: Release Management' };
      default: return { bg: '#F1F5F9', text: '#475569', label: stage };
    }
  };

  if (!categoryData) {
    return (
      <div style={{ backgroundColor: 'var(--color-bg-page)', minHeight: '85vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div className="container" style={{ padding: '80px 20px', textAlign: 'center' }}>
          <div className="badge badge-maroon" style={{ marginBottom: '12px' }}>Protect Producer</div>
          <p style={{ color: 'var(--color-text-muted)', fontSize: '16px' }}>Loading category workspace...</p>
        </div>
      </div>
    );
  }

  return (
    <div style={{ backgroundColor: 'var(--color-bg-page)', minHeight: '90vh', paddingBottom: '90px' }}>

      {/* Category Banner Header */}
      <section
        style={{
          backgroundColor: 'var(--color-navy)',
          color: 'var(--color-white)',
          padding: '48px 0 52px 0',
          borderBottom: '4px solid var(--color-maroon)',
          position: 'relative',
          overflow: 'hidden'
        }}
      >
        <div className="container" style={{ position: 'relative', zIndex: 2 }}>

          {/* Breadcrumbs */}
          <nav
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              fontSize: '13px',
              color: '#94A3B8',
              marginBottom: '20px'
            }}
            aria-label="Breadcrumb navigation"
          >
            <Link to="/" style={{ color: '#CBD5E1' }}>Home</Link>
            <span>/</span>
            <Link to="/protect-producer" style={{ color: '#CBD5E1' }}>Protect Producer</Link>
            <span>/</span>
            <span style={{ color: '#FED7AA', fontWeight: 600 }}>{categoryData.title}</span>
          </nav>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '32px', alignItems: 'center' }}>

            <div style={{ maxWidth: '680px' }}>
              <div
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '8px',
                  padding: '4px 12px',
                  borderRadius: 'var(--radius-full)',
                  backgroundColor: 'rgba(128, 24, 35, 0.3)',
                  border: '1px solid rgba(232, 180, 184, 0.4)',
                  color: '#FED7AA',
                  fontSize: '12px',
                  fontWeight: 600,
                  marginBottom: '14px'
                }}
              >
                <Shield size={14} />
                <span>{categoryData.badge}</span>
              </div>

              <h1 style={{ fontSize: 'clamp(28px, 3.8vw, 42px)', color: 'white', fontWeight: 800, marginBottom: '10px' }}>
                {categoryData.title} Production Workspace
              </h1>

              <p style={{ fontSize: '15px', color: '#FED7AA', fontStyle: 'italic', fontWeight: 600, marginBottom: '12px' }}>
                "{categoryData.tagline}"
              </p>

              <p style={{ fontSize: '15px', color: '#CBD5E1', lineHeight: 1.6, marginBottom: '24px' }}>
                {categoryData.description}
              </p>

              {/* Action Buttons */}
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '12px', alignItems: 'center' }}>
                <button
                  id="create-project-btn"
                  onClick={() => setShowCreateModal(true)}
                  className="btn btn-maroon"
                  style={{ gap: '8px' }}
                >
                  <Plus size={16} />
                  <span>Create New {categoryData.title.slice(0, -1)} Project</span>
                </button>

                <Link
                  to="/protect-producer"
                  className="btn btn-outline-subtle"
                  style={{
                    backgroundColor: 'rgba(255, 255, 255, 0.08)',
                    color: 'white',
                    borderColor: 'rgba(255, 255, 255, 0.25)',
                    gap: '6px'
                  }}
                >
                  <ArrowLeft size={16} />
                  <span>All Categories</span>
                </Link>
              </div>
            </div>

            {/* Workflow highlights card */}
            <div
              style={{
                backgroundColor: 'rgba(255, 255, 255, 0.05)',
                border: '1px solid rgba(255, 255, 255, 0.15)',
                borderRadius: 'var(--radius-lg)',
                padding: '24px',
                backdropFilter: 'blur(8px)'
              }}
            >
              <h3 style={{ fontSize: '16px', color: 'white', fontWeight: 700, marginBottom: '12px' }}>
                How Protect Producer Supports {categoryData.title}:
              </h3>
              <p style={{ fontSize: '13.5px', color: '#CBD5E1', lineHeight: 1.6, marginBottom: '16px' }}>
                {categoryData.workflowFocus}
              </p>
              <div style={{ fontSize: '12px', color: '#94A3B8', borderTop: '1px solid rgba(255, 255, 255, 0.1)', paddingTop: '12px' }}>
                🔒 <strong>Accountability:</strong> All stage changes, budgets, and document audits are recorded with immutable timestamps and producer accountability.
              </div>
            </div>

          </div>
        </div>
      </section>

      {/* Filter & Search Bar */}
      <div className="container" style={{ marginTop: '32px' }}>

        <div
          style={{
            backgroundColor: 'var(--color-white)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-md)',
            padding: '16px 20px',
            display: 'flex',
            flexWrap: 'wrap',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '16px',
            marginBottom: '32px',
            boxShadow: 'var(--shadow-subtle)'
          }}
        >
          {/* Search Box */}
          <div style={{ position: 'relative', flex: 1, minWidth: '260px' }}>
            <Search
              size={18}
              color="var(--color-text-muted)"
              style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }}
            />
            <input
              id="search-projects-input"
              type="text"
              placeholder={`Search ${categoryData.title.toLowerCase()} by title, banner, or producer...`}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              style={{
                width: '100%',
                padding: '10px 14px 10px 38px',
                borderRadius: 'var(--radius-sm)',
                border: '1px solid var(--color-border)',
                backgroundColor: 'var(--color-bg-page)',
                color: 'var(--color-navy)',
                fontSize: '14px',
                outline: 'none'
              }}
            />
          </div>

          {/* Status Filter */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <Filter size={16} color="var(--color-navy)" />
            <span style={{ fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)' }}>Status:</span>
            <select
              id="status-filter-select"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              style={{
                padding: '9px 14px',
                borderRadius: 'var(--radius-sm)',
                border: '1px solid var(--color-border)',
                backgroundColor: 'var(--color-bg-page)',
                color: 'var(--color-navy)',
                fontSize: '13px',
                fontWeight: 600,
                outline: 'none',
                cursor: 'pointer'
              }}
            >
              <option value="ALL">All Statuses</option>
              <option value="PLANNING">Planning</option>
              <option value="PRE_PRODUCTION">Pre-Production</option>
              <option value="IN_PRODUCTION">In Production</option>
              <option value="POST_PRODUCTION">Post-Production</option>
              <option value="COMPLETED">Completed</option>
              <option value="RELEASED">Released</option>
            </select>
          </div>

          <div style={{ fontSize: '13px', color: 'var(--color-text-muted)', fontWeight: 600 }}>
            Showing {filteredProjects.length} {filteredProjects.length === 1 ? 'Project' : 'Projects'}
          </div>
        </div>

        {/* Projects Grid */}
        {loading ? (
          <div style={{ padding: '60px 0', textAlign: 'center', color: 'var(--color-text-muted)' }}>
            Loading projects...
          </div>
        ) : filteredProjects.length === 0 ? (
          /* Empty State */
          <div
            style={{
              backgroundColor: 'var(--color-white)',
              border: '2px dashed var(--color-border)',
              borderRadius: 'var(--radius-lg)',
              padding: '64px 20px',
              textAlign: 'center',
              maxWidth: '640px',
              margin: '0 auto'
            }}
          >
            <div
              style={{
                width: '64px',
                height: '64px',
                borderRadius: '50%',
                backgroundColor: 'var(--color-maroon-light)',
                color: 'var(--color-maroon)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto 20px auto'
              }}
            >
              <Clapperboard size={32} />
            </div>
            <h3 style={{ fontSize: '20px', color: 'var(--color-navy)', fontWeight: 700, marginBottom: '8px' }}>
              No {categoryData.title} Projects Found
            </h3>
            <p style={{ fontSize: '14px', color: 'var(--color-text-muted)', lineHeight: 1.6, marginBottom: '24px' }}>
              {searchQuery || statusFilter !== 'ALL'
                ? 'No projects match your search criteria. Try resetting the filters.'
                : `There are currently no active projects under ${categoryData.title}. Register your first production project to initiate the four-stage governance workflow.`}
            </p>
            <button
              onClick={() => {
                setSearchQuery('');
                setStatusFilter('ALL');
                setShowCreateModal(true);
              }}
              className="btn btn-maroon"
              style={{ gap: '8px' }}
            >
              <Plus size={16} />
              <span>Create Your First {categoryData.title.slice(0, -1)} Project</span>
            </button>
          </div>
        ) : (
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(360px, 1fr))',
              gap: '28px'
            }}
          >
            {filteredProjects.map((p) => {
              const stageStyle = getStageBadgeStyle(p.currentStage);
              const progress = p.stageProgress || 0;
              const pendingApprovals = (p.tasks || []).filter(t => t.approvalStatus === 'PENDING').length;
              const openRisks = (p.risks || []).filter(r => r.status === 'OPEN' || r.status === 'IN_REVIEW').length;

              return (
                <div
                  key={p.id}
                  className="card card-hoverable"
                  onClick={() => navigate(`/protect-producer/${categoryData.id}/projects/${p.id}`)}
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    borderRadius: 'var(--radius-lg)',
                    overflow: 'hidden',
                    backgroundColor: 'var(--color-white)',
                    border: '1px solid var(--color-border)',
                    boxShadow: 'var(--shadow-card)',
                    cursor: 'pointer'
                  }}
                >
                  {/* Card Header Media */}
                  <div style={{ position: 'relative', height: '180px', backgroundColor: 'var(--color-navy-dark)', overflow: 'hidden' }}>
                    <img
                      src={p.bannerImageUrl || categoryData.image}
                      alt={p.title}
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                    />
                    <div
                      style={{
                        position: 'absolute',
                        inset: 0,
                        background: 'linear-gradient(to top, rgba(10, 25, 47, 0.9) 0%, rgba(10, 25, 47, 0.3) 60%, transparent 100%)'
                      }}
                    />

                    {/* Stage Badge on Image */}
                    <div style={{ position: 'absolute', top: '14px', left: '14px', zIndex: 2 }}>
                      <span
                        style={{
                          backgroundColor: stageStyle.bg,
                          color: stageStyle.text,
                          fontSize: '11px',
                          fontWeight: 700,
                          padding: '4px 10px',
                          borderRadius: 'var(--radius-full)',
                          border: `1px solid ${stageStyle.text}33`
                        }}
                      >
                        {stageStyle.label}
                      </span>
                    </div>

                    {/* Status Badge */}
                    <div style={{ position: 'absolute', top: '14px', right: '14px', zIndex: 2 }}>
                      <span
                        style={{
                          backgroundColor: 'rgba(10, 25, 47, 0.85)',
                          color: '#F8FAFC',
                          fontSize: '11px',
                          fontWeight: 700,
                          padding: '4px 10px',
                          borderRadius: 'var(--radius-full)',
                          backdropFilter: 'blur(4px)'
                        }}
                      >
                        {p.status.replace('_', ' ')}
                      </span>
                    </div>

                    {/* Title inside bottom of media */}
                    <div style={{ position: 'absolute', bottom: '14px', left: '16px', right: '16px', zIndex: 2 }}>
                      <h3 style={{ fontSize: '20px', color: 'white', fontWeight: 800, marginBottom: '2px' }}>
                        {p.title}
                      </h3>
                      <p style={{ fontSize: '12.5px', color: '#FED7AA', fontWeight: 500 }}>
                        {p.productionCompany} • {p.producerName}
                      </p>
                    </div>
                  </div>

                  {/* Card Body */}
                  <div style={{ padding: '20px', flex: 1, display: 'flex', flexDirection: 'column' }}>

                    {/* Synopsis */}
                    <p
                      style={{
                        fontSize: '13.5px',
                        color: 'var(--color-text-secondary)',
                        lineHeight: 1.55,
                        marginBottom: '18px',
                        display: '-webkit-box',
                        WebkitLineClamp: 2,
                        WebkitBoxOrient: 'vertical',
                        overflow: 'hidden'
                      }}
                    >
                      {p.synopsis || 'Comprehensive production lifecycle management active with verified milestones and audit records.'}
                    </p>

                    {/* Stage Progress Gauge */}
                    <div style={{ marginBottom: '18px' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '6px' }}>
                        <span>Current Stage Progress</span>
                        <span>{progress}%</span>
                      </div>
                      <div style={{ width: '100%', height: '7px', backgroundColor: 'var(--color-border)', borderRadius: 'var(--radius-full)', overflow: 'hidden' }}>
                        <div
                          style={{
                            width: `${progress}%`,
                            height: '100%',
                            backgroundColor: 'var(--color-maroon)',
                            borderRadius: 'var(--radius-full)',
                            transition: 'width 0.4s ease'
                          }}
                        />
                      </div>
                    </div>

                    {/* Key Details Grid */}
                    <div
                      style={{
                        display: 'grid',
                        gridTemplateColumns: '1fr 1fr',
                        gap: '10px',
                        padding: '12px',
                        backgroundColor: 'var(--color-bg-page)',
                        borderRadius: 'var(--radius-sm)',
                        fontSize: '12.5px',
                        marginBottom: '16px'
                      }}
                    >
                      <div>
                        <div style={{ color: 'var(--color-text-muted)', fontSize: '11px' }}>Total Budget</div>
                        <div style={{ fontWeight: 700, color: 'var(--color-navy)' }}>{p.totalBudget || '—'}</div>
                      </div>
                      <div>
                        <div style={{ color: 'var(--color-text-muted)', fontSize: '11px' }}>Spent to Date</div>
                        <div style={{ fontWeight: 700, color: 'var(--color-maroon)' }}>{p.spentBudget || '—'}</div>
                      </div>
                      <div>
                        <div style={{ color: 'var(--color-text-muted)', fontSize: '11px' }}>Target Release</div>
                        <div style={{ fontWeight: 600, color: 'var(--color-navy)' }}>{p.targetReleaseDate || 'TBD'}</div>
                      </div>
                      <div>
                        <div style={{ color: 'var(--color-text-muted)', fontSize: '11px' }}>
                          {category === 'serials' || category === 'web-series' ? 'Episodes' : 'Runtime'}
                        </div>
                        <div style={{ fontWeight: 600, color: 'var(--color-navy)' }}>
                          {p.totalEpisodes ? `${p.totalEpisodes} Episodes` : p.targetRuntime || 'Feature'}
                        </div>
                      </div>
                    </div>

                    {/* Alerts / Notice indicators */}
                    <div style={{ display: 'flex', gap: '10px', marginBottom: '20px' }}>
                      {pendingApprovals > 0 ? (
                        <span style={{ fontSize: '11.5px', fontWeight: 600, color: '#B45309', backgroundColor: '#FEF3C7', padding: '3px 8px', borderRadius: '4px' }}>
                          ⏳ {pendingApprovals} Pending {pendingApprovals === 1 ? 'Approval' : 'Approvals'}
                        </span>
                      ) : (
                        <span style={{ fontSize: '11.5px', fontWeight: 600, color: '#15803D', backgroundColor: '#DCFCE7', padding: '3px 8px', borderRadius: '4px' }}>
                          ✓ Approvals Up to Date
                        </span>
                      )}

                      {openRisks > 0 && (
                        <span style={{ fontSize: '11.5px', fontWeight: 600, color: 'var(--color-maroon)', backgroundColor: 'var(--color-maroon-light)', padding: '3px 8px', borderRadius: '4px' }}>
                          ⚠️ {openRisks} Open {openRisks === 1 ? 'Risk' : 'Risks'}
                        </span>
                      )}
                    </div>

                    {/* Card Actions */}
                    <div style={{ marginTop: 'auto', paddingTop: '14px', borderTop: '1px solid var(--color-border-subtle)' }}>
                      <Link
                        id={`open-project-${p.id}`}
                        to={`/protect-producer/${categoryData.id}/projects/${p.id}`}
                        className="btn btn-navy"
                        style={{ width: '100%', gap: '8px' }}
                      >
                        <span>Open Project Dashboard</span>
                        <ChevronRight size={16} />
                      </Link>
                    </div>

                  </div>
                </div>
              );
            })}
          </div>
        )}

      </div>

      {/* Create New Project Modal */}
      {showCreateModal && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            zIndex: 1000,
            backgroundColor: 'rgba(10, 25, 47, 0.75)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '20px',
            backdropFilter: 'blur(4px)'
          }}
          onClick={() => setShowCreateModal(false)}
        >
          <div
            style={{
              backgroundColor: 'var(--color-white)',
              borderRadius: 'var(--radius-lg)',
              maxWidth: '680px',
              width: '100%',
              maxHeight: '90vh',
              overflowY: 'auto',
              boxShadow: 'var(--shadow-modal)',
              border: '1px solid var(--color-border)'
            }}
            onClick={(e) => e.stopPropagation()}
          >
            {/* Modal Header */}
            <div
              style={{
                padding: '20px 24px',
                borderBottom: '1px solid var(--color-border)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                backgroundColor: 'var(--color-bg-page)'
              }}
            >
              <div>
                <h3 style={{ fontSize: '18px', color: 'var(--color-navy)', fontWeight: 800 }}>
                  Register New {categoryData.title.slice(0, -1)} Project
                </h3>
                <p style={{ fontSize: '12.5px', color: 'var(--color-text-muted)' }}>
                  Initiate complete production lifecycle governance, budget auditing, and milestone tracking.
                </p>
              </div>
              <button
                onClick={() => setShowCreateModal(false)}
                style={{ padding: '6px', color: 'var(--color-text-muted)', cursor: 'pointer' }}
                aria-label="Close dialog"
              >
                <X size={20} />
              </button>
            </div>

            {/* Modal Form */}
            <form onSubmit={handleCreateProject} style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '6px' }}>
                  Project Title *
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. The Coastal Syndicate"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '10px 14px',
                    borderRadius: 'var(--radius-sm)',
                    border: '1px solid var(--color-border)',
                    fontSize: '14px',
                    color: 'var(--color-navy)'
                  }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '6px' }}>
                    Production House / Banner
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. Mythri Movie Makers"
                    value={productionCompany}
                    onChange={(e) => setProductionCompany(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '10px 14px',
                      borderRadius: 'var(--radius-sm)',
                      border: '1px solid var(--color-border)',
                      fontSize: '14px'
                    }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '6px' }}>
                    Producer In Charge
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. Vikramaditya Roy"
                    value={producerName}
                    onChange={(e) => setProducerName(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '10px 14px',
                      borderRadius: 'var(--radius-sm)',
                      border: '1px solid var(--color-border)',
                      fontSize: '14px'
                    }}
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '6px' }}>
                  Logline / Project Synopsis
                </label>
                <textarea
                  rows={3}
                  placeholder="Brief narrative summary, genre, and production scope..."
                  value={synopsis}
                  onChange={(e) => setSynopsis(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '10px 14px',
                    borderRadius: 'var(--radius-sm)',
                    border: '1px solid var(--color-border)',
                    fontSize: '14px',
                    resize: 'vertical'
                  }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '6px' }}>
                    Estimated Line Budget
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. ₹5,00,00,000"
                    value={totalBudget}
                    onChange={(e) => setTotalBudget(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '10px 14px',
                      borderRadius: 'var(--radius-sm)',
                      border: '1px solid var(--color-border)',
                      fontSize: '14px'
                    }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '6px' }}>
                    Pre-Production Spent
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. ₹45,00,000"
                    value={spentBudget}
                    onChange={(e) => setSpentBudget(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '10px 14px',
                      borderRadius: 'var(--radius-sm)',
                      border: '1px solid var(--color-border)',
                      fontSize: '14px'
                    }}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '6px' }}>
                    Planned Start Date
                  </label>
                  <input
                    type="date"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '10px 14px',
                      borderRadius: 'var(--radius-sm)',
                      border: '1px solid var(--color-border)',
                      fontSize: '14px'
                    }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '6px' }}>
                    Target Release Date
                  </label>
                  <input
                    type="date"
                    value={targetReleaseDate}
                    onChange={(e) => setTargetReleaseDate(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '10px 14px',
                      borderRadius: 'var(--radius-sm)',
                      border: '1px solid var(--color-border)',
                      fontSize: '14px'
                    }}
                  />
                </div>
              </div>

              {/* Category-Tailored Inputs */}
              {(category === 'serials' || category === 'web-series') && (
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px', backgroundColor: 'var(--color-bg-subtle)', padding: '12px', borderRadius: 'var(--radius-sm)' }}>
                  <div>
                    <label style={{ display: 'block', fontSize: '12.5px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>
                      Season Number
                    </label>
                    <input
                      type="number"
                      min="1"
                      placeholder="e.g. 1"
                      value={seasonNumber}
                      onChange={(e) => setSeasonNumber(e.target.value)}
                      style={{
                        width: '100%',
                        padding: '8px 12px',
                        borderRadius: 'var(--radius-xs)',
                        border: '1px solid var(--color-border)',
                        fontSize: '13px'
                      }}
                    />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: '12.5px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>
                      Total Episode Count
                    </label>
                    <input
                      type="number"
                      min="1"
                      placeholder="e.g. 8 or 120"
                      value={totalEpisodes}
                      onChange={(e) => setTotalEpisodes(e.target.value)}
                      style={{
                        width: '100%',
                        padding: '8px 12px',
                        borderRadius: 'var(--radius-xs)',
                        border: '1px solid var(--color-border)',
                        fontSize: '13px'
                      }}
                    />
                  </div>
                </div>
              )}

              {category === 'short-films' && (
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px', backgroundColor: 'var(--color-bg-subtle)', padding: '12px', borderRadius: 'var(--radius-sm)' }}>
                  <div>
                    <label style={{ display: 'block', fontSize: '12.5px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>
                      Target Film Festival(s)
                    </label>
                    <input
                      type="text"
                      placeholder="e.g. Clermont-Ferrand / IFFI Goa"
                      value={targetFestival}
                      onChange={(e) => setTargetFestival(e.target.value)}
                      style={{
                        width: '100%',
                        padding: '8px 12px',
                        borderRadius: 'var(--radius-xs)',
                        border: '1px solid var(--color-border)',
                        fontSize: '13px'
                      }}
                    />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: '12.5px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>
                      Target Runtime
                    </label>
                    <input
                      type="text"
                      placeholder="e.g. 20 mins"
                      value={targetRuntime}
                      onChange={(e) => setTargetRuntime(e.target.value)}
                      style={{
                        width: '100%',
                        padding: '8px 12px',
                        borderRadius: 'var(--radius-xs)',
                        border: '1px solid var(--color-border)',
                        fontSize: '13px'
                      }}
                    />
                  </div>
                </div>
              )}

              {/* Form Actions */}
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '16px', paddingTop: '16px', borderTop: '1px solid var(--color-border)' }}>
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="btn btn-outline-subtle"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="btn btn-maroon"
                >
                  {submitting ? 'Registering...' : 'Register Project & Initialize Workspaces'}
                </button>
              </div>

            </form>
          </div>
        </div>
      )}

    </div>
  );
}
