import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate, useLocation } from 'react-router-dom';
import {
  Shield,
  Film,
  Tv,
  Clapperboard,
  ArrowLeft,
  Calendar,
  DollarSign,
  Clock,
  CheckCircle2,
  AlertTriangle,
  FileText,
  FileCheck,
  Plus,
  ChevronRight,
  Users,
  Sliders,
  Lock,
  Sparkles,
  Check,
  X,
  AlertCircle,
  Upload,
  CheckSquare,
  Square,
  Eye,
  TrendingUp,
  Play,
  Video,
  Layers,
  Music,
  Globe,
  Flag,
  Share2,
  Trash2,
  Edit3
} from 'lucide-react';
import { api } from '../services/api';
import { useNotification } from '../context/NotificationContext';

export default function ProjectDashboardPage() {
  const { category, projectId, stage: routeStage } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { showNotification } = useNotification();

  const [project, setProject] = useState(null);
  const [loading, setLoading] = useState(true);

  // Active Tab: 'overview', 'pre-production', 'production', 'post-production', 'release-management'
  const activeTab = routeStage || 'overview';

  // Modals
  const [showTaskModal, setShowTaskModal] = useState(false);
  const [showDocModal, setShowDocModal] = useState(false);
  const [showRiskModal, setShowRiskModal] = useState(false);
  const [showEpisodeModal, setShowEpisodeModal] = useState(false);
  const [showStageModal, setShowStageModal] = useState(false);
  const [showEditProjectModal, setShowEditProjectModal] = useState(false);

  // Form states
  const [taskTitle, setTaskTitle] = useState('');
  const [taskDesc, setTaskDesc] = useState('');
  const [taskAssignedTo, setTaskAssignedTo] = useState('');
  const [taskPriority, setTaskPriority] = useState('MEDIUM');
  const [taskDueDate, setTaskDueDate] = useState('');
  const [taskApprovalReq, setTaskApprovalReq] = useState(false);

  const [docTitle, setDocTitle] = useState('');
  const [docType, setDocType] = useState('SCRIPT');
  const [docUploadedBy, setDocUploadedBy] = useState('');
  const [docNotes, setDocNotes] = useState('');

  const [riskTitle, setRiskTitle] = useState('');
  const [riskSeverity, setRiskSeverity] = useState('MEDIUM');
  const [riskMitigation, setRiskMitigation] = useState('');
  const [riskOwner, setRiskOwner] = useState('');

  const [epNumber, setEpNumber] = useState('');
  const [epTitle, setEpTitle] = useState('');
  const [epDuration, setEpDuration] = useState('45 mins');
  const [epAirDate, setEpAirDate] = useState('');

  const [editStage, setEditStage] = useState('PRE_PRODUCTION');
  const [editProgress, setEditProgress] = useState(0);
  const [editStatus, setEditStatus] = useState('PRE_PRODUCTION');

  // Edit Project Form States
  const [editTitle, setEditTitle] = useState('');
  const [editProductionCompany, setEditProductionCompany] = useState('');
  const [editProducerName, setEditProducerName] = useState('');
  const [editProducerEmail, setEditProducerEmail] = useState('');
  const [editSynopsis, setEditSynopsis] = useState('');
  const [editTotalBudget, setEditTotalBudget] = useState('');
  const [editSpentBudget, setEditSpentBudget] = useState('');
  const [editStartDate, setEditStartDate] = useState('');
  const [editTargetReleaseDate, setEditTargetReleaseDate] = useState('');

  // Pre-production readiness checklist state (saved in project state)
  const [readinessChecklist, setReadinessChecklist] = useState([
    { id: 'rc-1', label: 'Screenplay Final Locked Draft Approved by Director', checked: true },
    { id: 'rc-2', label: 'Line Budget Itemized & Financing Escrow Verified', checked: true },
    { id: 'rc-3', label: 'Principal Cast Contracts & Exclusivity Executed', checked: true },
    { id: 'rc-4', label: 'Key Department Heads (DOP, Sound, Art) Onboarded', checked: true },
    { id: 'rc-5', label: 'Filming Permits & Local Municipality Clearances Locked', checked: false },
    { id: 'rc-6', label: 'Production Insurance & Indemnity Policy Active', checked: false },
    { id: 'rc-7', label: 'Call Sheets & Location Recce Safety Audit Passed', checked: false }
  ]);

  // Post-Production QC checklist
  const [qcChecklist, setQcChecklist] = useState([
    { id: 'qc-1', label: 'Picture Lock & Conformed EDL Verification', checked: true },
    { id: 'qc-2', label: 'Dolby Atmos Audio Loudness Calibrated (-24 LKFS)', checked: true },
    { id: 'qc-3', label: 'Color Gamut & HDR 1000 Nits Compliance Pass', checked: true },
    { id: 'qc-4', label: 'Zero Dead Pixels / Digital Artifacts Sensor Check', checked: true },
    { id: 'qc-5', label: 'Multilingual Subtitle Sync & Dialogue Timecode Proofing', checked: false },
    { id: 'qc-6', label: 'DCI Compliant 2K/4K Theatrical DCP Hash Verification', checked: false }
  ]);

  // Release Readiness checklist
  const [releaseChecklist, setReleaseChecklist] = useState([
    { id: 'rel-1', label: 'CBFC / Regional Censor Certificate Rating Acquired', checked: false },
    { id: 'rel-2', label: 'Music Sync & Archival Footage Copyright Clearances', checked: true },
    { id: 'rel-3', label: 'Final IMF/DCP Master Dispatched to Distribution Servers', checked: false },
    { id: 'rel-4', label: 'Approved Theatrical Posters & Trailer Packaged', checked: true },
    { id: 'rel-5', label: 'Press & Media Screening Passes Distributed', checked: false }
  ]);

  const loadProject = async () => {
    setLoading(true);
    try {
      const proj = await api.protectProducer.getProjectById(projectId);
      if (proj) {
        setProject(proj);
        setEditStage(proj.currentStage);
        setEditProgress(proj.stageProgress || 0);
        setEditStatus(proj.status);
        setEditTitle(proj.title || '');
        setEditProductionCompany(proj.productionCompany || '');
        setEditProducerName(proj.producerName || '');
        setEditProducerEmail(proj.producerEmail || '');
        setEditSynopsis(proj.synopsis || '');
        setEditTotalBudget(proj.totalBudget || '');
        setEditSpentBudget(proj.spentBudget || '');
        setEditStartDate(proj.startDate || '');
        setEditTargetReleaseDate(proj.targetReleaseDate || '');
      }
    } catch (err) {
      showNotification('Failed to load project: ' + err.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProject();
  }, [projectId]);

  // Reset scroll position when switching between stages or returning to overview
  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
  }, [routeStage]);

  // Dynamic progress calculation from real tasks and milestones
  const getStageCalculatedProgress = (targetStageEnum) => {
    if (!project) return 0;
    const stgTasks = (project.tasks || []).filter(t => t.stage === targetStageEnum);
    const stgMilestones = (project.milestones || []).filter(m => m.stage === targetStageEnum);
    const totalItems = stgTasks.length + stgMilestones.length;
    if (totalItems === 0) return project.stageProgress || 0;
    const completedTasks = stgTasks.filter(t => t.status === 'COMPLETED').length;
    const completedMilestones = stgMilestones.filter(m => m.isCompleted).length;
    return Math.round(((completedTasks + completedMilestones) / totalItems) * 100);
  };

  // Stage Gate Prerequisites Validator
  const getStageGatePrerequisites = (stageEnum) => {
    if (!project) return null;
    switch (stageEnum) {
      case 'PRE_PRODUCTION': {
        const hasScriptDoc = (project.documents || []).some(d => d.documentType === 'SCRIPT' && d.verified);
        const hasAgreements = (project.documents || []).some(d => d.documentType === 'AGREEMENT');
        const noPendingApprovals = !(project.tasks || []).some(t => t.stage === 'PRE_PRODUCTION' && t.approvalStatus === 'PENDING');
        const scriptLockMilestone = (project.milestones || []).find(m => m.stage === 'PRE_PRODUCTION' && m.isCompleted);

        const checks = [
          { label: 'Master Screenplay Draft Verified in Archive', met: hasScriptDoc, required: true },
          { label: 'Principal Cast & Key Talent Agreements Registered', met: hasAgreements, required: true },
          { label: 'Script Lock & Budget Signoff Milestone Completed', met: !!scriptLockMilestone, required: true },
          { label: 'All Pre-Production Signoffs & Approvals Cleared', met: noPendingApprovals, required: true }
        ];

        return {
          currentStageName: 'Pre-Production',
          nextStageEnum: 'PRODUCTION',
          nextStageKey: 'production',
          nextStageLabel: 'Stage 2: Production',
          checks,
          canAdvance: checks.every(c => c.met)
        };
      }
      case 'PRODUCTION': {
        const principalPhotographyMilestone = (project.milestones || []).find(m => m.stage === 'PRODUCTION' && m.isCompleted);
        const hasCallSheetsOrDpr = (project.documents || []).some(d => (d.documentType === 'CALL_SHEET' || d.documentType === 'DPR') && d.verified);
        const noCriticalRisks = !(project.risks || []).some(r => r.stage === 'PRODUCTION' && r.severity === 'CRITICAL' && r.status === 'OPEN');
        const allProductionTasksDone = (project.tasks || []).filter(t => t.stage === 'PRODUCTION').every(t => t.status === 'COMPLETED');

        const checks = [
          { label: 'Principal Photography / Shooting Wrap Milestone Achieved', met: !!principalPhotographyMilestone, required: true },
          { label: 'Verified Daily Call Sheets & DPR Records Synchronized', met: hasCallSheetsOrDpr || (project.tasks || []).some(t => t.stage === 'PRODUCTION' && t.status === 'COMPLETED'), required: true },
          { label: 'No Open Critical Production Risk Incidents', met: noCriticalRisks, required: true },
          { label: 'Production Tasks Finalized & Signed Off', met: allProductionTasksDone || (project.tasks || []).filter(t => t.stage === 'PRODUCTION').length === 0, required: false }
        ];

        return {
          currentStageName: 'Production',
          nextStageEnum: 'POST_PRODUCTION',
          nextStageKey: 'post-production',
          nextStageLabel: 'Stage 3: Post-Production',
          checks,
          canAdvance: checks.filter(c => c.required).every(c => c.met)
        };
      }
      case 'POST_PRODUCTION': {
        const pictureLockMilestone = (project.milestones || []).find(m => m.stage === 'POST_PRODUCTION' && m.isCompleted);
        const hasQcDoc = (project.documents || []).some(d => d.documentType === 'QC_REPORT' && d.verified);
        const hasPostApprovals = !(project.tasks || []).some(t => t.stage === 'POST_PRODUCTION' && t.approvalStatus === 'PENDING');

        const checks = [
          { label: 'Picture Lock & DI Color Grade Signoff Milestone Locked', met: !!pictureLockMilestone, required: true },
          { label: 'Technical QC Master Pass Certification Registered & Verified', met: hasQcDoc, required: true },
          { label: 'Post-Production Sound & Dubbing Signoffs Approved', met: hasPostApprovals, required: true }
        ];

        return {
          currentStageName: 'Post-Production',
          nextStageEnum: 'RELEASE_MANAGEMENT',
          nextStageKey: 'release-management',
          nextStageLabel: 'Stage 4: Release Management',
          checks,
          canAdvance: checks.every(c => c.met)
        };
      }
      case 'RELEASE_MANAGEMENT': {
        const hasCensorDoc = (project.documents || []).some(d => (d.documentType === 'CENSOR_CERT' || d.title.toLowerCase().includes('censor') || d.title.toLowerCase().includes('cbfc')) && d.verified);
        const premiereMilestone = (project.milestones || []).find(m => m.stage === 'RELEASE_MANAGEMENT' && m.isCompleted);
        const releaseScorePass = Math.round((releaseChecklist.filter(i => i.checked).length / releaseChecklist.length) * 100) >= 80;

        const checks = [
          { label: 'CBFC Censor Board Rating Certificate Verified', met: hasCensorDoc || (project.documents || []).some(d => d.verified), required: true },
          { label: 'Release Readiness Audit Score >= 80%', met: releaseScorePass, required: true },
          { label: 'Theatrical / OTT Premiere Deliverable Dispatched', met: !!premiereMilestone || (project.tasks || []).some(t => t.stage === 'RELEASE_MANAGEMENT' && t.status === 'COMPLETED'), required: true }
        ];

        return {
          currentStageName: 'Release Management',
          nextStageEnum: 'COMPLETED',
          nextStageKey: 'overview',
          nextStageLabel: 'Project Completed / Released',
          checks,
          canAdvance: checks.every(c => c.met)
        };
      }
      default:
        return null;
    }
  };

  const handleAdvanceStageGate = async (prereq) => {
    if (!prereq.canAdvance) {
      const missing = prereq.checks.filter(c => !c.met).map(c => c.label).join('\n• ');
      showNotification(`Cannot advance stage yet. Pending prerequisites:\n• ${missing}`, 'error', 6000);
      return;
    }

    try {
      const newStatus = prereq.nextStageEnum === 'COMPLETED' ? 'RELEASED' : (prereq.nextStageEnum === 'PRODUCTION' ? 'IN_PRODUCTION' : prereq.nextStageEnum);
      await api.protectProducer.updateStage(projectId, prereq.nextStageEnum === 'COMPLETED' ? project.currentStage : prereq.nextStageEnum, 100, newStatus);
      showNotification(`Stage gate certified! Project successfully advanced to ${prereq.nextStageLabel}!`, 'success');
      loadProject();
      if (prereq.nextStageKey !== 'overview') {
        handleStageNavigation(prereq.nextStageKey);
      }
      window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
    } catch (err) {
      showNotification('Failed to advance stage: ' + err.message, 'error');
    }
  };

  const handleStageNavigation = (newStage) => {
    if (newStage === 'overview') {
      navigate(`/protect-producer/${category}/projects/${projectId}`);
    } else {
      navigate(`/protect-producer/${category}/projects/${projectId}/${newStage}`);
    }
    window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
  };

  const handleStageProgressUpdate = async (e) => {
    e.preventDefault();
    try {
      await api.protectProducer.updateStage(projectId, editStage, editProgress, editStatus);
      showNotification(`Stage updated to ${editStage} (${editProgress}%)`, 'success');
      setShowStageModal(false);
      loadProject();
    } catch (err) {
      showNotification('Failed to update stage: ' + err.message, 'error');
    }
  };

  const handleEditProjectSubmit = async (e) => {
    e.preventDefault();
    try {
      await api.protectProducer.updateProject(projectId, {
        title: editTitle,
        productionCompany: editProductionCompany,
        producerName: editProducerName,
        producerEmail: editProducerEmail,
        synopsis: editSynopsis,
        totalBudget: editTotalBudget,
        spentBudget: editSpentBudget,
        startDate: editStartDate,
        targetReleaseDate: editTargetReleaseDate
      });
      showNotification('Project details updated successfully!', 'success');
      setShowEditProjectModal(false);
      loadProject();
    } catch (err) {
      showNotification('Failed to update project: ' + err.message, 'error');
    }
  };

  // Task actions
  const handleCreateTask = async (e) => {
    e.preventDefault();
    if (!taskTitle.trim()) return;
    try {
      const currentWorkspaceStage = activeTab === 'overview' ? project.currentStage : activeTab.toUpperCase().replace('-', '_');
      await api.protectProducer.createTask(projectId, {
        stage: currentWorkspaceStage,
        title: taskTitle.trim(),
        description: taskDesc.trim(),
        assignedTo: taskAssignedTo.trim() || 'Production Office',
        status: 'NOT_STARTED',
        priority: taskPriority,
        dueDate: taskDueDate || '7 Days',
        approvalStatus: taskApprovalReq ? 'PENDING' : 'NONE'
      });
      showNotification('Task added to workspace!', 'success');
      setShowTaskModal(false);
      setTaskTitle('');
      setTaskDesc('');
      setTaskAssignedTo('');
      setTaskApprovalReq(false);
      loadProject();
    } catch (err) {
      showNotification('Failed to add task', 'error');
    }
  };

  const handleTaskStatusChange = async (taskId, newStatus) => {
    try {
      await api.protectProducer.updateTask(projectId, taskId, { status: newStatus });
      showNotification(`Task status updated to ${newStatus}`, 'success');
      loadProject();
    } catch (err) {
      showNotification('Failed to update task status', 'error');
    }
  };

  const handleTaskApproval = async (taskId, approvalStatus) => {
    try {
      await api.protectProducer.updateTask(projectId, taskId, {
        approvalStatus,
        approvedBy: project.producerName || 'Lead Producer'
      });
      showNotification(`Task ${approvalStatus.toLowerCase()} with recorded timestamp!`, 'success');
      loadProject();
    } catch (err) {
      showNotification('Failed to process approval', 'error');
    }
  };

  // Document actions
  const handleCreateDocument = async (e) => {
    e.preventDefault();
    if (!docTitle.trim()) return;
    try {
      const currentWorkspaceStage = activeTab === 'overview' ? project.currentStage : activeTab.toUpperCase().replace('-', '_');
      await api.protectProducer.createDocument(projectId, {
        stage: currentWorkspaceStage,
        documentType: docType,
        title: docTitle.trim(),
        uploadedBy: docUploadedBy.trim() || project.producerName || 'Production Legal',
        verified: true,
        notes: docNotes.trim(),
        fileUrl: '#'
      });
      showNotification('Document registered in chain-of-custody archive!', 'success');
      setShowDocModal(false);
      setDocTitle('');
      setDocNotes('');
      setDocUploadedBy('');
      loadProject();
    } catch (err) {
      showNotification('Failed to register document', 'error');
    }
  };

  const handleToggleDocVerification = async (docId, currentVerified) => {
    try {
      await api.protectProducer.verifyDocument(projectId, docId, !currentVerified);
      showNotification(`Document ${!currentVerified ? 'verified' : 'marked unverified'}`, 'success');
      loadProject();
    } catch (err) {
      showNotification('Failed to update document verification', 'error');
    }
  };

  // Milestone actions
  const handleToggleMilestone = async (mlsId) => {
    try {
      await api.protectProducer.toggleMilestone(projectId, mlsId);
      showNotification('Milestone status toggled!', 'success');
      loadProject();
    } catch (err) {
      showNotification('Failed to update milestone', 'error');
    }
  };

  // Risk actions
  const handleCreateRisk = async (e) => {
    e.preventDefault();
    if (!riskTitle.trim()) return;
    try {
      const currentWorkspaceStage = activeTab === 'overview' ? project.currentStage : activeTab.toUpperCase().replace('-', '_');
      await api.protectProducer.createRisk(projectId, {
        stage: currentWorkspaceStage,
        title: riskTitle.trim(),
        severity: riskSeverity,
        mitigationPlan: riskMitigation.trim(),
        owner: riskOwner.trim() || 'Risk Officer',
        status: 'OPEN'
      });
      showNotification('Risk logged with mitigation plan!', 'success');
      setShowRiskModal(false);
      setRiskTitle('');
      setRiskMitigation('');
      setRiskOwner('');
      loadProject();
    } catch (err) {
      showNotification('Failed to log risk', 'error');
    }
  };

  const handleUpdateRiskStatus = async (riskId, newStatus) => {
    try {
      await api.protectProducer.updateRisk(projectId, riskId, { status: newStatus });
      showNotification(`Risk marked as ${newStatus}`, 'success');
      loadProject();
    } catch (err) {
      showNotification('Failed to update risk status', 'error');
    }
  };

  // Episode actions (Serials / Web Series)
  const handleCreateEpisode = async (e) => {
    e.preventDefault();
    if (!epTitle.trim()) return;
    try {
      await api.protectProducer.createEpisode(projectId, {
        episodeNumber: epNumber ? parseInt(epNumber) : (project.episodes?.length || 0) + 1,
        title: epTitle.trim(),
        status: 'SCRIPT_LOCKED',
        duration: epDuration.trim() || '45 mins',
        airDate: epAirDate || project.targetReleaseDate
      });
      showNotification('Episode record added!', 'success');
      setShowEpisodeModal(false);
      setEpTitle('');
      setEpNumber('');
      loadProject();
    } catch (err) {
      showNotification('Failed to add episode', 'error');
    }
  };

  const handleUpdateEpisodeStatus = async (epId, newStatus) => {
    try {
      await api.protectProducer.updateEpisode(projectId, epId, { status: newStatus });
      showNotification(`Episode updated to ${newStatus}`, 'success');
      loadProject();
    } catch (err) {
      showNotification('Failed to update episode status', 'error');
    }
  };

  // Checklist toggles
  const toggleChecklistItem = (setChecklist, id) => {
    setChecklist(prev => prev.map(item => item.id === id ? { ...item, checked: !item.checked } : item));
    showNotification('Checklist item updated', 'info');
  };

  if (loading) {
    return (
      <div style={{ backgroundColor: 'var(--color-bg-page)', minHeight: '85vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div className="container" style={{ padding: '80px 20px', textAlign: 'center' }}>
          <div className="badge badge-maroon" style={{ marginBottom: '12px' }}>Protect Producer</div>
          <p style={{ color: 'var(--color-text-muted)', fontSize: '16px' }}>Loading project workspace...</p>
        </div>
      </div>
    );
  }

  if (!project) {
    return (
      <div style={{ backgroundColor: 'var(--color-bg-page)', minHeight: '85vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div className="container" style={{ padding: '80px 20px', textAlign: 'center' }}>
          <h2 style={{ color: 'var(--color-navy)', marginBottom: '8px' }}>Project Not Found</h2>
          <p style={{ color: 'var(--color-text-muted)', marginBottom: '16px' }}>The requested production project could not be found.</p>
          <Link to={`/protect-producer/${category}`} className="btn btn-maroon">
            Back to {category}
          </Link>
        </div>
      </div>
    );
  }

  // Filter tasks and docs for current stage if on a stage workspace
  const currentStageEnum = activeTab === 'overview' ? null : activeTab.toUpperCase().replace('-', '_');
  const visibleTasks = currentStageEnum ? (project.tasks || []).filter(t => t.stage === currentStageEnum) : (project.tasks || []);
  const visibleDocuments = currentStageEnum ? (project.documents || []).filter(d => d.stage === currentStageEnum) : (project.documents || []);
  const visibleMilestones = currentStageEnum ? (project.milestones || []).filter(m => m.stage === currentStageEnum) : (project.milestones || []);
  const visibleRisks = currentStageEnum ? (project.risks || []).filter(r => r.stage === currentStageEnum) : (project.risks || []);

  const pendingApprovalsList = (project.tasks || []).filter(t => t.approvalStatus === 'PENDING');
  const unverifiedDocs = (project.documents || []).filter(d => !d.verified);

  const stagesList = [
    { key: 'pre-production', enum: 'PRE_PRODUCTION', num: 1, label: 'Pre-Production', subtitle: 'Script, Cast, Budget & Permits' },
    { key: 'production', enum: 'PRODUCTION', num: 2, label: 'Production', subtitle: 'Call Sheets, Scenes & Expense Logs' },
    { key: 'post-production', enum: 'POST_PRODUCTION', num: 3, label: 'Post-Production', subtitle: 'Editing, VFX, Atmos & QC' },
    { key: 'release-management', enum: 'RELEASE_MANAGEMENT', num: 4, label: 'Release Management', subtitle: 'Censor, Deliverables & Launch' }
  ];

  const getStageIndex = (stageKey) => {
    switch (stageKey) {
      case 'PRE_PRODUCTION': return 0;
      case 'PRODUCTION': return 1;
      case 'POST_PRODUCTION': return 2;
      case 'RELEASE_MANAGEMENT': return 3;
      default: return 0;
    }
  };

  const currentProjectStageIdx = getStageIndex(project.currentStage);

  return (
    <div style={{ backgroundColor: 'var(--color-bg-page)', minHeight: '90vh', paddingBottom: '90px' }}>

      {/* Top Project Context Banner */}
      <section
        style={{
          backgroundColor: 'var(--color-navy)',
          color: 'var(--color-white)',
          padding: '28px 0 24px 0',
          borderBottom: '4px solid var(--color-maroon)'
        }}
      >
        <div className="container">

          {/* Breadcrumbs */}
          <nav style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', color: '#94A3B8', marginBottom: '16px' }}>
            <Link to="/" style={{ color: '#CBD5E1' }}>Home</Link>
            <span>/</span>
            <Link to="/protect-producer" style={{ color: '#CBD5E1' }}>Protect Producer</Link>
            <span>/</span>
            <Link to={`/protect-producer/${category}`} style={{ color: '#CBD5E1' }}>
              {category.charAt(0).toUpperCase() + category.slice(1).replace('-', ' ')}
            </Link>
            <span>/</span>
            <span style={{ color: '#FED7AA', fontWeight: 600 }}>{project.title}</span>
          </nav>

          <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'flex-start', justifyContent: 'space-between', gap: '20px' }}>

            <div style={{ maxWidth: '780px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '10px' }}>
                <span
                  style={{
                    backgroundColor: 'rgba(128, 24, 35, 0.4)',
                    color: '#FED7AA',
                    fontSize: '11px',
                    fontWeight: 700,
                    padding: '3px 10px',
                    borderRadius: 'var(--radius-full)',
                    border: '1px solid rgba(232, 180, 184, 0.4)',
                    textTransform: 'uppercase'
                  }}
                >
                  {project.category.replace('_', ' ')}
                </span>

                <span
                  style={{
                    backgroundColor: '#1E3A5F',
                    color: '#93C5FD',
                    fontSize: '11px',
                    fontWeight: 700,
                    padding: '3px 10px',
                    borderRadius: 'var(--radius-full)'
                  }}
                >
                  STATUS: {project.status.replace('_', ' ')}
                </span>
              </div>

              <h1 style={{ fontSize: 'clamp(26px, 3.5vw, 36px)', color: 'white', fontWeight: 800, marginBottom: '6px' }}>
                {project.title}
              </h1>

              <p style={{ fontSize: '14px', color: '#CBD5E1', marginBottom: '16px' }}>
                <strong>Production House:</strong> {project.productionCompany} • <strong>Producer:</strong> {project.producerName} ({project.producerEmail})
              </p>
            </div>

            {/* Stage Progress & Action Buttons */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', minWidth: '220px' }}>
              <div
                style={{
                  backgroundColor: 'rgba(255, 255, 255, 0.08)',
                  border: '1px solid rgba(255, 255, 255, 0.15)',
                  borderRadius: 'var(--radius-md)',
                  padding: '12px 16px',
                  backdropFilter: 'blur(4px)'
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', color: '#CBD5E1', marginBottom: '6px' }}>
                  <span>Stage Completion</span>
                  <strong style={{ color: '#FED7AA' }}>{getStageCalculatedProgress(project.currentStage)}%</strong>
                </div>
                <div style={{ width: '100%', height: '6px', backgroundColor: 'rgba(255, 255, 255, 0.2)', borderRadius: 'var(--radius-full)', overflow: 'hidden' }}>
                  <div style={{ width: `${getStageCalculatedProgress(project.currentStage)}%`, height: '100%', backgroundColor: 'var(--color-maroon)' }} />
                </div>
              </div>

              <div style={{ display: 'flex', gap: '8px' }}>
                <button
                  onClick={() => setShowStageModal(true)}
                  className="btn btn-sm btn-maroon"
                  style={{ flex: 1, gap: '6px' }}
                >
                  <Sliders size={14} />
                  <span>Update Stage</span>
                </button>
                <button
                  onClick={() => setShowEditProjectModal(true)}
                  className="btn btn-sm btn-outline-subtle"
                  style={{ color: 'white', borderColor: 'rgba(255, 255, 255, 0.3)', gap: '4px' }}
                >
                  <Edit3 size={14} />
                  <span>Edit</span>
                </button>
              </div>
            </div>

          </div>

          {/* Quick Stats Ribbon */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))',
              gap: '12px',
              marginTop: '22px',
              paddingTop: '18px',
              borderTop: '1px solid rgba(255, 255, 255, 0.12)'
            }}
          >
            <div>
              <div style={{ color: '#94A3B8', fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.04em' }}>Allocated Budget</div>
              <div style={{ fontSize: '16px', fontWeight: 700, color: 'white' }}>{project.totalBudget || '—'}</div>
            </div>
            <div>
              <div style={{ color: '#94A3B8', fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.04em' }}>Spent to Date</div>
              <div style={{ fontSize: '16px', fontWeight: 700, color: '#FCA5A5' }}>{project.spentBudget || '—'}</div>
            </div>
            <div>
              <div style={{ color: '#94A3B8', fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.04em' }}>Planned Start</div>
              <div style={{ fontSize: '15px', fontWeight: 600, color: 'white' }}>{project.startDate || '—'}</div>
            </div>
            <div>
              <div style={{ color: '#94A3B8', fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.04em' }}>Target Release</div>
              <div style={{ fontSize: '15px', fontWeight: 600, color: '#FED7AA' }}>{project.targetReleaseDate || 'TBD'}</div>
            </div>
            <div>
              <div style={{ color: '#94A3B8', fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.04em' }}>Pending Approvals</div>
              <div style={{ fontSize: '15px', fontWeight: 700, color: pendingApprovalsList.length > 0 ? '#FBBF24' : '#86EFAC' }}>
                {pendingApprovalsList.length} Pending
              </div>
            </div>
          </div>

        </div>
      </section>

      {/* Lifecycle Navigation Bar (4 Management Stages) */}
      <section
        style={{
          backgroundColor: 'var(--color-white)',
          borderBottom: '1px solid var(--color-border)',
          boxShadow: 'var(--shadow-subtle)',
          position: 'sticky',
          top: '70px',
          zIndex: 80
        }}
      >
        <div className="container" style={{ padding: '0 20px', overflowX: 'auto' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '4px', minWidth: '780px' }}>

            {/* Overview Tab */}
            <button
              id="stage-tab-overview"
              onClick={() => handleStageNavigation('overview')}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '16px 16px',
                fontSize: '14px',
                fontWeight: activeTab === 'overview' ? 700 : 500,
                color: activeTab === 'overview' ? 'var(--color-maroon)' : 'var(--color-navy)',
                borderBottom: activeTab === 'overview' ? '3px solid var(--color-maroon)' : '3px solid transparent',
                backgroundColor: 'transparent',
                cursor: 'pointer',
                whiteSpace: 'nowrap',
                transition: 'all var(--transition-fast)'
              }}
            >
              <TrendingUp size={16} />
              <span>Project Dashboard</span>
            </button>

            <div style={{ width: '1px', height: '24px', backgroundColor: 'var(--color-border)', margin: '0 8px' }} />

            {/* 4 Stage Tabs */}
            {stagesList.map((stg, idx) => {
              const isSelected = activeTab === stg.key;
              const isStagePassed = idx < currentProjectStageIdx;
              const isCurrentProjectStage = idx === currentProjectStageIdx;

              return (
                <button
                  key={stg.key}
                  id={`stage-tab-${stg.key}`}
                  onClick={() => handleStageNavigation(stg.key)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '10px',
                    padding: '16px 18px',
                    fontSize: '13.5px',
                    fontWeight: isSelected ? 700 : 500,
                    color: isSelected ? 'var(--color-maroon)' : 'var(--color-navy)',
                    borderBottom: isSelected ? '3px solid var(--color-maroon)' : '3px solid transparent',
                    backgroundColor: isSelected ? 'var(--color-maroon-light)' : 'transparent',
                    cursor: 'pointer',
                    whiteSpace: 'nowrap',
                    transition: 'all var(--transition-fast)'
                  }}
                >
                  <span
                    style={{
                      width: '22px',
                      height: '22px',
                      borderRadius: '50%',
                      backgroundColor: isStagePassed ? '#15803D' : (isCurrentProjectStage ? 'var(--color-maroon)' : 'var(--color-border)'),
                      color: isStagePassed || isCurrentProjectStage ? 'white' : 'var(--color-text-secondary)',
                      fontSize: '11px',
                      fontWeight: 700,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center'
                    }}
                  >
                    {isStagePassed ? '✓' : stg.num}
                  </span>

                  <div style={{ textAlign: 'left' }}>
                    <div style={{ lineHeight: 1.2 }}>{stg.label}</div>
                  </div>
                </button>
              );
            })}

          </div>
        </div>
      </section>

      {/* Main Workspace Body */}
      <main className="container" style={{ marginTop: '32px' }}>

        {/* ========================================================
            TAB 0: EXECUTIVE PROJECT DASHBOARD
           ======================================================== */}
        {activeTab === 'overview' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '28px' }}>

            {/* Alerts Section (Missing documents & Pending approvals) */}
            {(unverifiedDocs.length > 0 || pendingApprovalsList.length > 0) && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {unverifiedDocs.length > 0 && (
                  <div
                    style={{
                      backgroundColor: '#FFFBEB',
                      border: '1px solid #FDE68A',
                      padding: '14px 20px',
                      borderRadius: 'var(--radius-md)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      gap: '14px'
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                      <AlertTriangle size={20} color="#B45309" />
                      <div style={{ fontSize: '13.5px', color: '#92400E' }}>
                        <strong>Missing / Unverified Documents Alert:</strong> {unverifiedDocs.length} critical project {unverifiedDocs.length === 1 ? 'document requires' : 'documents require'} verification in the project register before stage gate advancement.
                      </div>
                    </div>
                    <button
                      onClick={() => handleStageNavigation('pre-production')}
                      className="btn btn-sm btn-outline-subtle"
                      style={{ color: '#92400E', borderColor: '#FDE68A' }}
                    >
                      Review Documents
                    </button>
                  </div>
                )}

                {pendingApprovalsList.length > 0 && (
                  <div
                    style={{
                      backgroundColor: 'var(--color-maroon-light)',
                      border: '1px solid var(--color-maroon-border)',
                      padding: '14px 20px',
                      borderRadius: 'var(--radius-md)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      gap: '14px'
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                      <Clock size={20} color="var(--color-maroon)" />
                      <div style={{ fontSize: '13.5px', color: 'var(--color-maroon)' }}>
                        <strong>Pending Producer Approvals:</strong> {pendingApprovalsList.length} operational {pendingApprovalsList.length === 1 ? 'task is' : 'tasks are'} waiting for your authorized sign-off.
                      </div>
                    </div>
                    <div style={{ display: 'flex', gap: '8px' }}>
                      {pendingApprovalsList.slice(0, 2).map(task => (
                        <button
                          key={task.id}
                          onClick={() => handleTaskApproval(task.id, 'APPROVED')}
                          className="btn btn-sm btn-maroon"
                          style={{ fontSize: '12px', padding: '4px 10px' }}
                        >
                          Approve "{task.title.slice(0, 18)}..."
                        </button>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* Dashboard 3-Column Layout */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '24px' }}>

              {/* Card 1: Project Information & Synopsis */}
              <div className="card" style={{ padding: '24px' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
                  <h3 style={{ fontSize: '17px', color: 'var(--color-navy)', fontWeight: 700 }}>
                    Project Information
                  </h3>
                  <button
                    onClick={() => setShowEditProjectModal(true)}
                    style={{ color: 'var(--color-maroon)', fontSize: '13px', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '4px' }}
                  >
                    <Edit3 size={14} /> Edit
                  </button>
                </div>
                <p style={{ fontSize: '13.5px', color: 'var(--color-text-secondary)', lineHeight: 1.6, marginBottom: '20px' }}>
                  {project.synopsis || 'No logline added yet. Click edit to provide narrative overview.'}
                </p>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', fontSize: '13px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--color-border-subtle)' }}>
                    <span style={{ color: 'var(--color-text-muted)' }}>Format & Category</span>
                    <span style={{ fontWeight: 600, color: 'var(--color-navy)' }}>{project.category}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--color-border-subtle)' }}>
                    <span style={{ color: 'var(--color-text-muted)' }}>Current Stage</span>
                    <span style={{ fontWeight: 600, color: 'var(--color-maroon)' }}>{project.currentStage.replace('_', ' ')}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--color-border-subtle)' }}>
                    <span style={{ color: 'var(--color-text-muted)' }}>Production Banner</span>
                    <span style={{ fontWeight: 600, color: 'var(--color-navy)' }}>{project.productionCompany}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--color-border-subtle)' }}>
                    <span style={{ color: 'var(--color-text-muted)' }}>Lead Producer</span>
                    <span style={{ fontWeight: 600, color: 'var(--color-navy)' }}>{project.producerName}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: 'var(--color-text-muted)' }}>Contact Email</span>
                    <span style={{ fontWeight: 600, color: 'var(--color-navy)' }}>{project.producerEmail}</span>
                  </div>
                </div>
              </div>

              {/* Card 2: Financial Summary & Budget Breakdown */}
              <div className="card" style={{ padding: '24px' }}>
                <h3 style={{ fontSize: '17px', color: 'var(--color-navy)', fontWeight: 700, marginBottom: '16px' }}>
                  Budget & Expenditure Summary
                </h3>

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: '12px' }}>
                  <span style={{ fontSize: '13px', color: 'var(--color-text-muted)' }}>Spent of Total Allocation:</span>
                  <span style={{ fontSize: '18px', fontWeight: 800, color: 'var(--color-maroon)' }}>
                    {project.spentBudget} <span style={{ fontSize: '13px', color: 'var(--color-text-muted)', fontWeight: 400 }}>/ {project.totalBudget}</span>
                  </span>
                </div>

                {/* Progress Meter */}
                <div style={{ width: '100%', height: '10px', backgroundColor: 'var(--color-border)', borderRadius: 'var(--radius-full)', overflow: 'hidden', marginBottom: '20px' }}>
                  <div style={{ width: '48%', height: '100%', backgroundColor: 'var(--color-maroon)' }} />
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', fontSize: '12.5px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 10px', backgroundColor: 'var(--color-bg-page)', borderRadius: '4px' }}>
                    <span>Above-the-Line (Cast, Director, Story)</span>
                    <strong>35% Allocated</strong>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 10px', backgroundColor: 'var(--color-bg-page)', borderRadius: '4px' }}>
                    <span>Below-the-Line (Crew, Gear, Locations)</span>
                    <strong>45% Allocated</strong>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 10px', backgroundColor: 'var(--color-bg-page)', borderRadius: '4px' }}>
                    <span>Post-Production & Sound Design</span>
                    <strong>15% Allocated</strong>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 10px', backgroundColor: '#DCFCE7', color: '#15803D', borderRadius: '4px' }}>
                    <span>Emergency Contingency Reserve</span>
                    <strong>5% Uncommitted</strong>
                  </div>
                </div>
              </div>

              {/* Card 3: Upcoming Milestones */}
              <div className="card" style={{ padding: '24px' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
                  <h3 style={{ fontSize: '17px', color: 'var(--color-navy)', fontWeight: 700 }}>
                    Key Lifecycle Milestones
                  </h3>
                  <span style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>
                    {(project.milestones || []).filter(m => m.isCompleted).length} / {(project.milestones || []).length} Locked
                  </span>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  {(project.milestones || []).slice(0, 5).map(m => (
                    <div
                      key={m.id}
                      onClick={() => handleToggleMilestone(m.id)}
                      style={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: '12px',
                        padding: '10px 12px',
                        borderRadius: 'var(--radius-sm)',
                        backgroundColor: m.isCompleted ? '#F0FDF4' : 'var(--color-bg-page)',
                        border: `1px solid ${m.isCompleted ? '#BBF7D0' : 'var(--color-border-subtle)'}`,
                        cursor: 'pointer',
                        transition: 'all var(--transition-fast)'
                      }}
                      title="Click to toggle milestone completion"
                    >
                      <div style={{ marginTop: '2px', color: m.isCompleted ? '#16A34A' : 'var(--color-text-muted)' }}>
                        {m.isCompleted ? <CheckCircle2 size={16} /> : <Clock size={16} />}
                      </div>
                      <div style={{ flex: 1 }}>
                        <div style={{ fontSize: '13px', fontWeight: 600, color: m.isCompleted ? '#166534' : 'var(--color-navy)', textDecoration: m.isCompleted ? 'line-through' : 'none' }}>
                          {m.title}
                        </div>
                        <div style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>
                          Target: {m.targetDate} {m.completedDate ? `• Completed on ${m.completedDate}` : ''}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

            </div>

            {/* Stage Workspaces Quick Launcher Cards */}
            <div>
              <h3 style={{ fontSize: '20px', color: 'var(--color-navy)', fontWeight: 800, marginBottom: '16px' }}>
                Stage Workspaces Navigation
              </h3>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '20px' }}>
                {stagesList.map((stg, idx) => (
                  <div
                    key={stg.key}
                    id={`overview-card-${stg.key}`}
                    className="card card-hoverable"
                    style={{
                      padding: '20px',
                      borderRadius: 'var(--radius-md)',
                      display: 'flex',
                      flexDirection: 'column',
                      cursor: 'pointer'
                    }}
                    onClick={() => handleStageNavigation(stg.key)}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
                      <span
                        style={{
                          width: '28px',
                          height: '28px',
                          borderRadius: '50%',
                          backgroundColor: idx <= currentProjectStageIdx ? 'var(--color-maroon)' : 'var(--color-border)',
                          color: 'white',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          fontSize: '12px',
                          fontWeight: 800
                        }}
                      >
                        {stg.num}
                      </span>
                      <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--color-maroon)' }}>
                        Open Workspace →
                      </span>
                    </div>

                    <h4 style={{ fontSize: '16px', color: 'var(--color-navy)', fontWeight: 700, marginBottom: '6px' }}>
                      {stg.label}
                    </h4>
                    <p style={{ fontSize: '13px', color: 'var(--color-text-secondary)', lineHeight: 1.5, marginBottom: '16px' }}>
                      {stg.subtitle}
                    </p>

                    <div style={{ marginTop: 'auto', paddingTop: '12px', borderTop: '1px solid var(--color-border-subtle)', display: 'flex', justifyContent: 'space-between', fontSize: '12px', color: 'var(--color-text-muted)' }}>
                      <span>{(project.tasks || []).filter(t => t.stage === stg.enum).length} Tasks</span>
                      <span>{(project.documents || []).filter(d => d.stage === stg.enum).length} Documents</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Recent Activity Audit Trail */}
            <div className="card" style={{ padding: '24px' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
                <h3 style={{ fontSize: '17px', color: 'var(--color-navy)', fontWeight: 700 }}>
                  Recent Project Governance & Audit Log
                </h3>
                <span style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>
                  Immutable production ledger
                </span>
              </div>

              {(project.recentActivity || []).length === 0 ? (
                <p style={{ fontSize: '13.5px', color: 'var(--color-text-muted)' }}>No recorded activities yet.</p>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                  {(project.recentActivity || []).map((act, idx) => (
                    <div
                      key={act.id || idx}
                      style={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: '14px',
                        paddingBottom: '12px',
                        borderBottom: idx < (project.recentActivity.length - 1) ? '1px solid var(--color-border-subtle)' : 'none'
                      }}
                    >
                      <div
                        style={{
                          width: '32px',
                          height: '32px',
                          borderRadius: '50%',
                          backgroundColor: 'var(--color-bg-subtle)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          color: 'var(--color-navy)',
                          flexShrink: 0,
                          marginTop: '2px'
                        }}
                      >
                        <Shield size={16} />
                      </div>
                      <div style={{ flex: 1 }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: '2px' }}>
                          <span style={{ fontWeight: 700, fontSize: '13.5px', color: 'var(--color-navy)' }}>
                            {act.action}
                          </span>
                          <span style={{ fontSize: '11.5px', color: 'var(--color-text-muted)' }}>
                            {act.timestamp ? new Date(act.timestamp).toLocaleString() : 'Recent'}
                          </span>
                        </div>
                        <p style={{ fontSize: '13px', color: 'var(--color-text-secondary)', lineHeight: 1.5 }}>
                          {act.details}
                        </p>
                        <span style={{ fontSize: '11px', color: 'var(--color-maroon)', fontWeight: 600 }}>
                          Action by: {act.performedBy}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

          </div>
        )}

        {/* ========================================================
            STAGE WORKSPACES (1, 2, 3, 4)
           ======================================================== */}
        {activeTab !== 'overview' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }}>

            {/* Workspace Header Sub-banner */}
            <div
              style={{
                backgroundColor: 'var(--color-white)',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-lg)',
                padding: '24px',
                display: 'flex',
                flexWrap: 'wrap',
                alignItems: 'center',
                justifyContent: 'space-between',
                gap: '20px',
                boxShadow: 'var(--shadow-subtle)'
              }}
            >
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
                  <span className="badge badge-maroon">
                    Stage {stagesList.find(s => s.key === activeTab)?.num} of 4
                  </span>
                  <span style={{ fontSize: '13px', color: 'var(--color-text-muted)' }}>
                    Project Stage: <strong>{project.currentStage.replace('_', ' ')}</strong>
                  </span>
                </div>
                <h2 style={{ fontSize: '24px', color: 'var(--color-navy)', fontWeight: 800 }}>
                  {stagesList.find(s => s.key === activeTab)?.label} Workspace
                </h2>
                <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)' }}>
                  {stagesList.find(s => s.key === activeTab)?.subtitle}
                </p>
              </div>

              {/* Action Toolbar */}
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '10px' }}>
                <button
                  onClick={() => setShowTaskModal(true)}
                  className="btn btn-sm btn-maroon"
                  style={{ gap: '6px' }}
                >
                  <Plus size={15} />
                  <span>Add Task</span>
                </button>
                <button
                  onClick={() => setShowDocModal(true)}
                  className="btn btn-sm btn-outline-navy"
                  style={{ gap: '6px' }}
                >
                  <Upload size={15} />
                  <span>Register Document</span>
                </button>
                <button
                  onClick={() => setShowRiskModal(true)}
                  className="btn btn-sm btn-outline-subtle"
                  style={{ gap: '6px' }}
                >
                  <AlertTriangle size={15} color="var(--color-maroon)" />
                  <span>Log Risk</span>
                </button>
                {(project.category === 'SERIAL' || project.category === 'WEB_SERIES') && activeTab === 'production' && (
                  <button
                    onClick={() => setShowEpisodeModal(true)}
                    className="btn btn-sm btn-navy"
                    style={{ gap: '6px' }}
                  >
                    <Plus size={15} />
                    <span>Add Episode</span>
                  </button>
                )}
              </div>
            </div>

            {/* STAGE 1: PRE-PRODUCTION SPECIFIC SECTIONS */}
            {activeTab === 'pre-production' && (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '24px' }}>

                {/* Script & Screenplay Details */}
                <div className="card" style={{ padding: '24px' }}>
                  <h3 style={{ fontSize: '16px', color: 'var(--color-navy)', fontWeight: 700, marginBottom: '16px' }}>
                    Screenplay & Script Breakdown Lock
                  </h3>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', fontSize: '13px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--color-border-subtle)' }}>
                      <span style={{ color: 'var(--color-text-muted)' }}>Script Draft</span>
                      <strong>Draft v3.2 (Production Locked)</strong>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--color-border-subtle)' }}>
                      <span style={{ color: 'var(--color-text-muted)' }}>Registered Writer</span>
                      <span>Karan Mehta (Screenwriters Guild)</span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--color-border-subtle)' }}>
                      <span style={{ color: 'var(--color-text-muted)' }}>Estimated Page Count</span>
                      <span>132 Pages (Approx 145 Mins)</span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <span style={{ color: 'var(--color-text-muted)' }}>Scene Breakdown</span>
                      <span style={{ color: '#15803D', fontWeight: 600 }}>Completed & Budgeted</span>
                    </div>
                  </div>
                </div>

                {/* Pre-Production Readiness Checklist */}
                <div className="card" style={{ padding: '24px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
                    <h3 style={{ fontSize: '16px', color: 'var(--color-navy)', fontWeight: 700 }}>
                      Pre-Production Readiness Gate
                    </h3>
                    <span style={{ fontSize: '12px', fontWeight: 700, color: 'var(--color-maroon)' }}>
                      {Math.round((readinessChecklist.filter(i => i.checked).length / readinessChecklist.length) * 100)}% Ready
                    </span>
                  </div>

                  <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                    {readinessChecklist.map(item => (
                      <div
                        key={item.id}
                        onClick={() => toggleChecklistItem(setReadinessChecklist, item.id)}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: '10px',
                          fontSize: '13px',
                          color: item.checked ? 'var(--color-navy)' : 'var(--color-text-muted)',
                          cursor: 'pointer'
                        }}
                      >
                        {item.checked ? <CheckCircle2 size={16} color="#15803D" /> : <Square size={16} color="#CBD5E1" />}
                        <span style={{ textDecoration: item.checked ? 'none' : 'none' }}>{item.label}</span>
                      </div>
                    ))}
                  </div>
                </div>

              </div>
            )}

            {/* STAGE 2: PRODUCTION SPECIFIC SECTIONS */}
            {activeTab === 'production' && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>

                {/* Serials / Web Series Episode-Oriented Workflow */}
                {(project.category === 'SERIAL' || project.category === 'WEB_SERIES') && (
                  <div className="card" style={{ padding: '24px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
                      <div>
                        <h3 style={{ fontSize: '17px', color: 'var(--color-navy)', fontWeight: 700 }}>
                          Episode-Wise Production Progress
                        </h3>
                        <p style={{ fontSize: '12.5px', color: 'var(--color-text-muted)' }}>
                          Track script status, shooting days, and episode delivery schedules.
                        </p>
                      </div>
                      <button
                        onClick={() => setShowEpisodeModal(true)}
                        className="btn btn-sm btn-maroon"
                        style={{ gap: '6px' }}
                      >
                        <Plus size={14} /> Add Episode
                      </button>
                    </div>

                    <div style={{ overflowX: 'auto' }}>
                      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px', textAlign: 'left' }}>
                        <thead>
                          <tr style={{ borderBottom: '2px solid var(--color-border)', color: 'var(--color-navy)' }}>
                            <th style={{ padding: '10px 12px' }}>#</th>
                            <th style={{ padding: '10px 12px' }}>Episode Title</th>
                            <th style={{ padding: '10px 12px' }}>Status</th>
                            <th style={{ padding: '10px 12px' }}>Duration</th>
                            <th style={{ padding: '10px 12px' }}>Air / Target Date</th>
                            <th style={{ padding: '10px 12px', textAlign: 'right' }}>Update Progress</th>
                          </tr>
                        </thead>
                        <tbody>
                          {(project.episodes || []).map(ep => (
                            <tr key={ep.id} style={{ borderBottom: '1px solid var(--color-border-subtle)' }}>
                              <td style={{ padding: '12px', fontWeight: 700 }}>Ep {ep.episodeNumber}</td>
                              <td style={{ padding: '12px', fontWeight: 600, color: 'var(--color-navy)' }}>{ep.title}</td>
                              <td style={{ padding: '12px' }}>
                                <span
                                  style={{
                                    fontSize: '11px',
                                    fontWeight: 700,
                                    padding: '3px 8px',
                                    borderRadius: '4px',
                                    backgroundColor: ep.status === 'DELIVERED' ? '#DCFCE7' : (ep.status === 'SHOOTING' ? 'var(--color-maroon-light)' : '#EFF6FF'),
                                    color: ep.status === 'DELIVERED' ? '#15803D' : (ep.status === 'SHOOTING' ? 'var(--color-maroon)' : '#1D4ED8')
                                  }}
                                >
                                  {ep.status.replace('_', ' ')}
                                </span>
                              </td>
                              <td style={{ padding: '12px', color: 'var(--color-text-muted)' }}>{ep.duration}</td>
                              <td style={{ padding: '12px', color: 'var(--color-navy)' }}>{ep.airDate || 'TBD'}</td>
                              <td style={{ padding: '12px', textAlign: 'right' }}>
                                <select
                                  value={ep.status}
                                  onChange={(e) => handleUpdateEpisodeStatus(ep.id, e.target.value)}
                                  style={{
                                    padding: '4px 8px',
                                    borderRadius: '4px',
                                    border: '1px solid var(--color-border)',
                                    fontSize: '12px',
                                    cursor: 'pointer'
                                  }}
                                >
                                  <option value="PLANNED">Planned</option>
                                  <option value="SCRIPT_LOCKED">Script Locked</option>
                                  <option value="SHOOTING">Shooting</option>
                                  <option value="EDITING">Editing</option>
                                  <option value="QC_APPROVED">QC Approved</option>
                                  <option value="DELIVERED">Delivered</option>
                                </select>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                )}

                {/* Film Shooting Schedules & Daily Call Sheets */}
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '24px' }}>

                  <div className="card" style={{ padding: '24px' }}>
                    <h3 style={{ fontSize: '16px', color: 'var(--color-navy)', fontWeight: 700, marginBottom: '14px' }}>
                      Shooting Schedules & Call Sheet Status
                    </h3>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', fontSize: '13px' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--color-border-subtle)' }}>
                        <span style={{ color: 'var(--color-text-muted)' }}>Active Shooting Day</span>
                        <strong style={{ color: 'var(--color-maroon)' }}>Day 24 of 48 Scheduled Days</strong>
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--color-border-subtle)' }}>
                        <span style={{ color: 'var(--color-text-muted)' }}>Location Focus</span>
                        <span>Harbor Warehouse, Visakhapatnam</span>
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--color-border-subtle)' }}>
                        <span style={{ color: 'var(--color-text-muted)' }}>Call Time & Units</span>
                        <span>05:30 AM (Unit 1 Principal & Stunts)</span>
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                        <span style={{ color: 'var(--color-text-muted)' }}>1st AD Daily Signoff</span>
                        <span style={{ color: '#15803D', fontWeight: 600 }}>Verified & Dispatched</span>
                      </div>
                    </div>
                  </div>

                  {/* Daily Production Reports (DPR) Summary */}
                  <div className="card" style={{ padding: '24px' }}>
                    <h3 style={{ fontSize: '16px', color: 'var(--color-navy)', fontWeight: 700, marginBottom: '14px' }}>
                      Daily Production Progress (DPR)
                    </h3>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', fontSize: '13px' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--color-border-subtle)' }}>
                        <span style={{ color: 'var(--color-text-muted)' }}>Scenes Planned vs Shot</span>
                        <span>4 Planned / 4 Completed</span>
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--color-border-subtle)' }}>
                        <span style={{ color: 'var(--color-text-muted)' }}>Screen Time Recorded</span>
                        <span>4m 12s Useful Footage</span>
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--color-border-subtle)' }}>
                        <span style={{ color: 'var(--color-text-muted)' }}>Sound Rolls / Camera Cards</span>
                        <span>12 Cards (DIT Hash Matched)</span>
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                        <span style={{ color: 'var(--color-text-muted)' }}>DPR Audit Status</span>
                        <span style={{ color: '#15803D', fontWeight: 600 }}>Archived & Synchronized</span>
                      </div>
                    </div>
                  </div>

                </div>
              </div>
            )}

            {/* STAGE 3: POST-PRODUCTION SPECIFIC SECTIONS */}
            {activeTab === 'post-production' && (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '24px' }}>

                {/* Editing Workflow & Revision Status */}
                <div className="card" style={{ padding: '24px' }}>
                  <h3 style={{ fontSize: '16px', color: 'var(--color-navy)', fontWeight: 700, marginBottom: '16px' }}>
                    Editing Workflow & Final Cut Matrix
                  </h3>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 12px', backgroundColor: 'var(--color-bg-page)', borderRadius: '4px' }}>
                      <div>
                        <div style={{ fontSize: '13px', fontWeight: 700, color: 'var(--color-navy)' }}>Assembly Cut v1.0</div>
                        <div style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>Ingested all raw cards & sound sync</div>
                      </div>
                      <span style={{ fontSize: '11px', fontWeight: 700, color: '#15803D', backgroundColor: '#DCFCE7', padding: '3px 8px', borderRadius: '4px' }}>Approved</span>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 12px', backgroundColor: 'var(--color-bg-page)', borderRadius: '4px' }}>
                      <div>
                        <div style={{ fontSize: '13px', fontWeight: 700, color: 'var(--color-navy)' }}>Director's Rough Cut v2.1</div>
                        <div style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>Scene pacing and score scratch track</div>
                      </div>
                      <span style={{ fontSize: '11px', fontWeight: 700, color: '#15803D', backgroundColor: '#DCFCE7', padding: '3px 8px', borderRadius: '4px' }}>Approved</span>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 12px', backgroundColor: 'var(--color-maroon-light)', borderRadius: '4px', border: '1px solid var(--color-maroon-border)' }}>
                      <div>
                        <div style={{ fontSize: '13px', fontWeight: 700, color: 'var(--color-maroon)' }}>Locked Cut (Final Cut)</div>
                        <div style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>Producer & Executive Signoff</div>
                      </div>
                      <span style={{ fontSize: '11px', fontWeight: 700, color: '#B45309', backgroundColor: '#FEF3C7', padding: '3px 8px', borderRadius: '4px' }}>In Review</span>
                    </div>
                  </div>
                </div>

                {/* Post-Production Technical QC Checklist */}
                <div className="card" style={{ padding: '24px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
                    <h3 style={{ fontSize: '16px', color: 'var(--color-navy)', fontWeight: 700 }}>
                      Quality-Control (QC) Technical Checklist
                    </h3>
                    <span style={{ fontSize: '12px', fontWeight: 700, color: '#15803D' }}>
                      {Math.round((qcChecklist.filter(i => i.checked).length / qcChecklist.length) * 100)}% Pass
                    </span>
                  </div>

                  <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                    {qcChecklist.map(item => (
                      <div
                        key={item.id}
                        onClick={() => toggleChecklistItem(setQcChecklist, item.id)}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: '10px',
                          fontSize: '13px',
                          color: item.checked ? 'var(--color-navy)' : 'var(--color-text-muted)',
                          cursor: 'pointer'
                        }}
                      >
                        {item.checked ? <CheckCircle2 size={16} color="#15803D" /> : <Square size={16} color="#CBD5E1" />}
                        <span>{item.label}</span>
                      </div>
                    ))}
                  </div>
                </div>

              </div>
            )}

            {/* STAGE 4: RELEASE MANAGEMENT SPECIFIC SECTIONS */}
            {activeTab === 'release-management' && (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '24px' }}>

                {/* Distribution Channels & Calendar */}
                <div className="card" style={{ padding: '24px' }}>
                  <h3 style={{ fontSize: '16px', color: 'var(--color-navy)', fontWeight: 700, marginBottom: '16px' }}>
                    Distribution Channels & Territory Records
                  </h3>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', fontSize: '13px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--color-border-subtle)' }}>
                      <span style={{ color: 'var(--color-text-muted)' }}>Theatrical Release</span>
                      <strong>Pan-India 1,800+ Screens (Mythri Distribution)</strong>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--color-border-subtle)' }}>
                      <span style={{ color: 'var(--color-text-muted)' }}>Streaming OTT Window</span>
                      <span>Worldwide Streaming 8-Week Window</span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--color-border-subtle)' }}>
                      <span style={{ color: 'var(--color-text-muted)' }}>Satellite Broadcast</span>
                      <span>Regional Hindi & South Satellite Network</span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <span style={{ color: 'var(--color-text-muted)' }}>Overseas Theatrical</span>
                      <span>North America & Middle East Circuits Locked</span>
                    </div>
                  </div>
                </div>

                {/* Release Readiness Audit */}
                <div className="card" style={{ padding: '24px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
                    <h3 style={{ fontSize: '16px', color: 'var(--color-navy)', fontWeight: 700 }}>
                      Release Readiness Audit Score
                    </h3>
                    <span style={{ fontSize: '12px', fontWeight: 700, color: 'var(--color-maroon)' }}>
                      {Math.round((releaseChecklist.filter(i => i.checked).length / releaseChecklist.length) * 100)}% Ready
                    </span>
                  </div>

                  <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                    {releaseChecklist.map(item => (
                      <div
                        key={item.id}
                        onClick={() => toggleChecklistItem(setReleaseChecklist, item.id)}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: '10px',
                          fontSize: '13px',
                          color: item.checked ? 'var(--color-navy)' : 'var(--color-text-muted)',
                          cursor: 'pointer'
                        }}
                      >
                        {item.checked ? <CheckCircle2 size={16} color="#15803D" /> : <Square size={16} color="#CBD5E1" />}
                        <span>{item.label}</span>
                      </div>
                    ))}
                  </div>
                </div>

              </div>
            )}

            {/* ========================================================
                SHARED STAGE WORKSPACE COMPONENTS:
                TASKS, DOCUMENTS, RISKS REGISTERS
               ======================================================== */}

            {/* 0. Stage Specific Milestones */}
            <div className="card" style={{ padding: '24px' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '18px' }}>
                <div>
                  <h3 style={{ fontSize: '17px', color: 'var(--color-navy)', fontWeight: 700 }}>
                    {stagesList.find(s => s.key === activeTab)?.label} Milestones
                  </h3>
                  <p style={{ fontSize: '12.5px', color: 'var(--color-text-muted)' }}>
                    Track and lock key contractual and deliverable milestones for this production phase.
                  </p>
                </div>
                <span style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>
                  {visibleMilestones.filter(m => m.isCompleted).length} / {visibleMilestones.length} Locked
                </span>
              </div>

              {visibleMilestones.length === 0 ? (
                <div style={{ padding: '24px 16px', textAlign: 'center', color: 'var(--color-text-muted)', border: '1px dashed var(--color-border)', borderRadius: 'var(--radius-sm)' }}>
                  No milestones recorded for this stage yet.
                </div>
              ) : (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '12px' }}>
                  {visibleMilestones.map(m => (
                    <div
                      key={m.id}
                      onClick={() => handleToggleMilestone(m.id)}
                      style={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: '12px',
                        padding: '12px 14px',
                        borderRadius: 'var(--radius-sm)',
                        backgroundColor: m.isCompleted ? '#F0FDF4' : 'var(--color-bg-page)',
                        border: `1px solid ${m.isCompleted ? '#BBF7D0' : 'var(--color-border-subtle)'}`,
                        cursor: 'pointer',
                        transition: 'all var(--transition-fast)'
                      }}
                      title="Click to toggle milestone completion"
                    >
                      <div style={{ marginTop: '2px', color: m.isCompleted ? '#16A34A' : 'var(--color-text-muted)' }}>
                        {m.isCompleted ? <CheckCircle2 size={16} /> : <Clock size={16} />}
                      </div>
                      <div style={{ flex: 1 }}>
                        <div style={{ fontSize: '13px', fontWeight: 600, color: m.isCompleted ? '#166534' : 'var(--color-navy)', textDecoration: m.isCompleted ? 'line-through' : 'none' }}>
                          {m.title}
                        </div>
                        <div style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>
                          Target: {m.targetDate} {m.completedDate ? `• Completed on ${m.completedDate}` : ''}
                        </div>
                      </div>
                      <span style={{ fontSize: '10.5px', fontWeight: 700, color: m.isCompleted ? '#15803D' : 'var(--color-text-muted)' }}>
                        {m.isCompleted ? 'LOCKED' : 'PENDING'}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* 1. Stage Tasks Register */}
            <div className="card" style={{ padding: '24px' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '18px' }}>
                <div>
                  <h3 style={{ fontSize: '17px', color: 'var(--color-navy)', fontWeight: 700 }}>
                    Stage Operational Tasks & Approvals
                  </h3>
                  <p style={{ fontSize: '12.5px', color: 'var(--color-text-muted)' }}>
                    Track tasks, assigned team leads, deadlines, and approval signoffs with recorded timestamps.
                  </p>
                </div>
                <button
                  onClick={() => setShowTaskModal(true)}
                  className="btn btn-sm btn-maroon"
                  style={{ gap: '6px' }}
                >
                  <Plus size={14} /> Add Task
                </button>
              </div>

              {visibleTasks.length === 0 ? (
                <div style={{ padding: '32px 16px', textAlign: 'center', color: 'var(--color-text-muted)', border: '1px dashed var(--color-border)', borderRadius: 'var(--radius-sm)' }}>
                  No tasks recorded for this stage yet. Click "Add Task" to create one.
                </div>
              ) : (
                <div style={{ overflowX: 'auto' }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px', textAlign: 'left' }}>
                    <thead>
                      <tr style={{ borderBottom: '2px solid var(--color-border)', color: 'var(--color-navy)' }}>
                        <th style={{ padding: '10px 12px' }}>Task Title</th>
                        <th style={{ padding: '10px 12px' }}>Assigned To</th>
                        <th style={{ padding: '10px 12px' }}>Priority</th>
                        <th style={{ padding: '10px 12px' }}>Due Date</th>
                        <th style={{ padding: '10px 12px' }}>Status</th>
                        <th style={{ padding: '10px 12px' }}>Approval Status</th>
                        <th style={{ padding: '10px 12px', textAlign: 'right' }}>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {visibleTasks.map(t => (
                        <tr key={t.id} style={{ borderBottom: '1px solid var(--color-border-subtle)' }}>
                          <td style={{ padding: '12px', fontWeight: 600, color: 'var(--color-navy)' }}>
                            {t.title}
                            {t.description && (
                              <div style={{ fontSize: '11.5px', color: 'var(--color-text-muted)', fontWeight: 400 }}>{t.description}</div>
                            )}
                          </td>
                          <td style={{ padding: '12px' }}>{t.assignedTo}</td>
                          <td style={{ padding: '12px' }}>
                            <span
                              style={{
                                fontSize: '10.5px',
                                fontWeight: 700,
                                padding: '2px 6px',
                                borderRadius: '4px',
                                backgroundColor: t.priority === 'CRITICAL' ? '#FEE2E2' : (t.priority === 'HIGH' ? '#FEF3C7' : '#F1F5F9'),
                                color: t.priority === 'CRITICAL' ? '#991B1B' : (t.priority === 'HIGH' ? '#B45309' : '#475569')
                              }}
                            >
                              {t.priority}
                            </span>
                          </td>
                          <td style={{ padding: '12px', color: 'var(--color-text-muted)' }}>{t.dueDate || '—'}</td>
                          <td style={{ padding: '12px' }}>
                            <select
                              value={t.status}
                              onChange={(e) => handleTaskStatusChange(t.id, e.target.value)}
                              style={{
                                padding: '4px 8px',
                                borderRadius: '4px',
                                border: '1px solid var(--color-border)',
                                fontSize: '12px',
                                fontWeight: 600,
                                backgroundColor: t.status === 'COMPLETED' ? '#DCFCE7' : 'white',
                                color: t.status === 'COMPLETED' ? '#15803D' : 'var(--color-navy)',
                                cursor: 'pointer'
                              }}
                            >
                              <option value="NOT_STARTED">Not Started</option>
                              <option value="IN_PROGRESS">In Progress</option>
                              <option value="BLOCKED">Blocked</option>
                              <option value="COMPLETED">Completed</option>
                            </select>
                          </td>
                          <td style={{ padding: '12px' }}>
                            {t.approvalStatus === 'PENDING' ? (
                              <span style={{ fontSize: '11px', fontWeight: 700, color: '#B45309', backgroundColor: '#FEF3C7', padding: '3px 8px', borderRadius: '4px' }}>
                                ⏳ Pending Signoff
                              </span>
                            ) : t.approvalStatus === 'APPROVED' ? (
                              <div>
                                <span style={{ fontSize: '11px', fontWeight: 700, color: '#15803D', backgroundColor: '#DCFCE7', padding: '3px 8px', borderRadius: '4px' }}>
                                  ✓ Signed by {t.approvedBy || 'Producer'}
                                </span>
                                {t.approvedAt && (
                                  <div style={{ fontSize: '10.5px', color: 'var(--color-text-muted)' }}>{new Date(t.approvedAt).toLocaleDateString()}</div>
                                )}
                              </div>
                            ) : t.approvalStatus === 'REJECTED' ? (
                              <span style={{ fontSize: '11px', fontWeight: 700, color: '#991B1B', backgroundColor: '#FEE2E2', padding: '3px 8px', borderRadius: '4px' }}>
                                ✕ Rejected
                              </span>
                            ) : (
                              <span style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>Not Required</span>
                            )}
                          </td>
                          <td style={{ padding: '12px', textAlign: 'right' }}>
                            {t.approvalStatus === 'PENDING' && (
                              <div style={{ display: 'flex', gap: '6px', justifyContent: 'flex-end' }}>
                                <button
                                  onClick={() => handleTaskApproval(t.id, 'APPROVED')}
                                  className="btn btn-sm btn-maroon"
                                  style={{ padding: '3px 8px', fontSize: '11px' }}
                                >
                                  Approve
                                </button>
                                <button
                                  onClick={() => handleTaskApproval(t.id, 'REJECTED')}
                                  className="btn btn-sm btn-outline-subtle"
                                  style={{ padding: '3px 8px', fontSize: '11px' }}
                                >
                                  Reject
                                </button>
                              </div>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            {/* 2. Stage Document Register (Chain-of-Custody Archive) */}
            <div className="card" style={{ padding: '24px' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '18px' }}>
                <div>
                  <h3 style={{ fontSize: '17px', color: 'var(--color-navy)', fontWeight: 700 }}>
                    Stage Document Register & Chain-of-Custody
                  </h3>
                  <p style={{ fontSize: '12.5px', color: 'var(--color-text-muted)' }}>
                    Verified legal agreements, permits, call sheets, and technical QC certifications.
                  </p>
                </div>
                <button
                  onClick={() => setShowDocModal(true)}
                  className="btn btn-sm btn-outline-navy"
                  style={{ gap: '6px' }}
                >
                  <Upload size={14} /> Register Document
                </button>
              </div>

              {visibleDocuments.length === 0 ? (
                <div style={{ padding: '32px 16px', textAlign: 'center', color: 'var(--color-text-muted)', border: '1px dashed var(--color-border)', borderRadius: 'var(--radius-sm)' }}>
                  No documents registered for this stage. Register screenplay, contract, or permit records.
                </div>
              ) : (
                <div style={{ overflowX: 'auto' }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px', textAlign: 'left' }}>
                    <thead>
                      <tr style={{ borderBottom: '2px solid var(--color-border)', color: 'var(--color-navy)' }}>
                        <th style={{ padding: '10px 12px' }}>Document Name</th>
                        <th style={{ padding: '10px 12px' }}>Category</th>
                        <th style={{ padding: '10px 12px' }}>Uploaded By</th>
                        <th style={{ padding: '10px 12px' }}>Verification Status</th>
                        <th style={{ padding: '10px 12px' }}>Audit Notes</th>
                        <th style={{ padding: '10px 12px', textAlign: 'right' }}>Verify / Toggle</th>
                      </tr>
                    </thead>
                    <tbody>
                      {visibleDocuments.map(d => (
                        <tr key={d.id} style={{ borderBottom: '1px solid var(--color-border-subtle)' }}>
                          <td style={{ padding: '12px', fontWeight: 600, color: 'var(--color-navy)' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                              <FileText size={16} color="var(--color-navy)" />
                              <span>{d.title}</span>
                            </div>
                          </td>
                          <td style={{ padding: '12px' }}>
                            <span style={{ fontSize: '11px', fontWeight: 600, padding: '2px 8px', borderRadius: '4px', backgroundColor: 'var(--color-bg-page)', color: 'var(--color-navy)' }}>
                              {d.documentType}
                            </span>
                          </td>
                          <td style={{ padding: '12px', color: 'var(--color-text-secondary)' }}>{d.uploadedBy}</td>
                          <td style={{ padding: '12px' }}>
                            {d.verified ? (
                              <span style={{ fontSize: '11px', fontWeight: 700, color: '#15803D', backgroundColor: '#DCFCE7', padding: '3px 8px', borderRadius: '4px' }}>
                                ✓ Verified Archive
                              </span>
                            ) : (
                              <span style={{ fontSize: '11px', fontWeight: 700, color: '#B45309', backgroundColor: '#FEF3C7', padding: '3px 8px', borderRadius: '4px' }}>
                                ⚠️ Missing / Unverified
                              </span>
                            )}
                          </td>
                          <td style={{ padding: '12px', color: 'var(--color-text-muted)' }}>{d.notes || '—'}</td>
                          <td style={{ padding: '12px', textAlign: 'right' }}>
                            <button
                              onClick={() => handleToggleDocVerification(d.id, d.verified)}
                              className="btn btn-sm btn-outline-subtle"
                              style={{ fontSize: '11px', padding: '4px 8px' }}
                            >
                              {d.verified ? 'Mark Unverified' : 'Verify Document'}
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            {/* 3. Stage Risk & Incident Register */}
            <div className="card" style={{ padding: '24px' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '18px' }}>
                <div>
                  <h3 style={{ fontSize: '17px', color: 'var(--color-navy)', fontWeight: 700 }}>
                    Identified Production Risks & Incident Mitigations
                  </h3>
                  <p style={{ fontSize: '12.5px', color: 'var(--color-text-muted)' }}>
                    Early risk logging, severity tracking, mitigation plans, and resolution accountability.
                  </p>
                </div>
                <button
                  onClick={() => setShowRiskModal(true)}
                  className="btn btn-sm btn-outline-maroon"
                  style={{ gap: '6px' }}
                >
                  <AlertTriangle size={14} /> Log Risk
                </button>
              </div>

              {visibleRisks.length === 0 ? (
                <div style={{ padding: '32px 16px', textAlign: 'center', color: 'var(--color-text-muted)', border: '1px dashed var(--color-border)', borderRadius: 'var(--radius-sm)' }}>
                  No active risks logged for this stage. Proactively log weather, location, or scheduling delays.
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  {visibleRisks.map(r => (
                    <div
                      key={r.id}
                      style={{
                        padding: '16px',
                        borderRadius: 'var(--radius-sm)',
                        border: '1px solid var(--color-border)',
                        backgroundColor: r.status === 'RESOLVED' ? '#F8FAFC' : 'var(--color-white)',
                        display: 'flex',
                        flexWrap: 'wrap',
                        alignItems: 'flex-start',
                        justifyContent: 'space-between',
                        gap: '14px'
                      }}
                    >
                      <div style={{ maxWidth: '640px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '6px' }}>
                          <span
                            style={{
                              fontSize: '10.5px',
                              fontWeight: 700,
                              padding: '2px 8px',
                              borderRadius: '4px',
                              backgroundColor: r.severity === 'CRITICAL' ? '#FEE2E2' : (r.severity === 'HIGH' ? '#FEF3C7' : '#EFF6FF'),
                              color: r.severity === 'CRITICAL' ? '#991B1B' : (r.severity === 'HIGH' ? '#B45309' : '#1D4ED8')
                            }}
                          >
                            {r.severity} SEVERITY
                          </span>
                          <strong style={{ fontSize: '14px', color: 'var(--color-navy)' }}>{r.title}</strong>
                        </div>
                        <p style={{ fontSize: '13px', color: 'var(--color-text-secondary)', lineHeight: 1.5, marginBottom: '6px' }}>
                          <strong>Mitigation Plan:</strong> {r.mitigationPlan}
                        </p>
                        <span style={{ fontSize: '11.5px', color: 'var(--color-text-muted)' }}>
                          Risk Owner: {r.owner} • Status: <strong>{r.status}</strong>
                        </span>
                      </div>

                      <div>
                        <select
                          value={r.status}
                          onChange={(e) => handleUpdateRiskStatus(r.id, e.target.value)}
                          style={{
                            padding: '6px 10px',
                            borderRadius: '4px',
                            border: '1px solid var(--color-border)',
                            fontSize: '12px',
                            fontWeight: 600,
                            cursor: 'pointer'
                          }}
                        >
                          <option value="OPEN">Open</option>
                          <option value="IN_REVIEW">In Review</option>
                          <option value="MITIGATED">Mitigated</option>
                          <option value="RESOLVED">Resolved</option>
                        </select>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* 4. Stage Gate Readiness & Advancement Gate */}
            {(() => {
              const prereq = getStageGatePrerequisites(currentStageEnum);
              if (!prereq) return null;

              return (
                <div
                  className="card"
                  style={{
                    padding: '28px',
                    backgroundColor: prereq.canAdvance ? '#F0FDF4' : '#FFFBEB',
                    border: `2px solid ${prereq.canAdvance ? '#86EFAC' : '#FDE68A'}`,
                    borderRadius: 'var(--radius-lg)',
                    boxShadow: 'var(--shadow-card)'
                  }}
                >
                  <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '16px', marginBottom: '20px' }}>
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '4px' }}>
                        <span
                          style={{
                            fontSize: '11px',
                            fontWeight: 800,
                            padding: '3px 10px',
                            borderRadius: 'var(--radius-full)',
                            backgroundColor: prereq.canAdvance ? '#DCFCE7' : '#FEF3C7',
                            color: prereq.canAdvance ? '#15803D' : '#B45309',
                            textTransform: 'uppercase'
                          }}
                        >
                          {prereq.canAdvance ? 'Gate Certified • Ready to Advance' : 'Gate Verification • Pending Prerequisites'}
                        </span>
                      </div>
                      <h3 style={{ fontSize: '19px', color: 'var(--color-navy)', fontWeight: 800 }}>
                        {prereq.currentStageName} → {prereq.nextStageLabel} Stage Gate
                      </h3>
                      <p style={{ fontSize: '13px', color: 'var(--color-text-secondary)', maxWidth: '680px' }}>
                        In accordance with cinema production governance, all mandatory deliverables, contracts, and clearances below must be locked before official advancement to the next phase.
                      </p>
                    </div>

                    <button
                      onClick={() => handleAdvanceStageGate(prereq)}
                      className={`btn btn-lg ${prereq.canAdvance ? 'btn-maroon' : 'btn-outline-subtle'}`}
                      style={{
                        gap: '8px',
                        boxShadow: prereq.canAdvance ? '0 4px 14px rgba(128, 24, 35, 0.35)' : 'none',
                        cursor: prereq.canAdvance ? 'pointer' : 'not-allowed',
                        opacity: prereq.canAdvance ? 1 : 0.75
                      }}
                    >
                      {prereq.canAdvance ? (
                        <>
                          <CheckCircle2 size={18} />
                          <span>Advance to {prereq.nextStageLabel}</span>
                        </>
                      ) : (
                        <>
                          <AlertTriangle size={18} />
                          <span>Prerequisites Incomplete ({prereq.checks.filter(c => c.met).length}/{prereq.checks.length})</span>
                        </>
                      )}
                    </button>
                  </div>

                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '12px' }}>
                    {prereq.checks.map((chk, cIdx) => (
                      <div
                        key={cIdx}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: '12px',
                          padding: '12px 14px',
                          borderRadius: 'var(--radius-md)',
                          backgroundColor: 'white',
                          border: `1px solid ${chk.met ? '#BBF7D0' : '#FED7AA'}`
                        }}
                      >
                        <div style={{ color: chk.met ? '#16A34A' : '#D97706', display: 'flex', flexShrink: 0 }}>
                          {chk.met ? <CheckCircle2 size={18} /> : <AlertCircle size={18} />}
                        </div>
                        <div style={{ flex: 1, fontSize: '13px', fontWeight: 600, color: chk.met ? '#166534' : 'var(--color-navy)' }}>
                          {chk.label}
                        </div>
                        <span
                          style={{
                            fontSize: '11px',
                            fontWeight: 700,
                            color: chk.met ? '#15803D' : '#B45309'
                          }}
                        >
                          {chk.met ? 'LOCKED' : 'PENDING'}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              );
            })()}

          </div>
        )}

      </main>

      {/* ========================================================
          MODAL: ADD TASK
         ======================================================== */}
      {showTaskModal && (
        <div
          style={{ position: 'fixed', inset: 0, zIndex: 1000, backgroundColor: 'rgba(10, 25, 47, 0.75)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px', backdropFilter: 'blur(4px)' }}
          onClick={() => setShowTaskModal(false)}
        >
          <div
            style={{ backgroundColor: 'white', borderRadius: 'var(--radius-lg)', maxWidth: '560px', width: '100%', padding: '24px', boxShadow: 'var(--shadow-modal)' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
              <h3 style={{ fontSize: '17px', color: 'var(--color-navy)', fontWeight: 800 }}>
                Add Stage Task & Assignment
              </h3>
              <button onClick={() => setShowTaskModal(false)} style={{ cursor: 'pointer' }}><X size={18} /></button>
            </div>

            <form onSubmit={handleCreateTask} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Task Title *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Daily Call Sheet Lock for Harbor Shoot"
                  value={taskTitle}
                  onChange={(e) => setTaskTitle(e.target.value)}
                  style={{ width: '100%', padding: '9px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13.5px' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Description / Specific Deliverables</label>
                <textarea
                  rows={2}
                  placeholder="Specify task scope, scene references, or protocol requirements..."
                  value={taskDesc}
                  onChange={(e) => setTaskDesc(e.target.value)}
                  style={{ width: '100%', padding: '9px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13.5px' }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Assigned Lead</label>
                  <input
                    type="text"
                    placeholder="e.g. 1st AD / Line Producer"
                    value={taskAssignedTo}
                    onChange={(e) => setTaskAssignedTo(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13px' }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Priority</label>
                  <select
                    value={taskPriority}
                    onChange={(e) => setTaskPriority(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13px' }}
                  >
                    <option value="LOW">Low Priority</option>
                    <option value="MEDIUM">Medium Priority</option>
                    <option value="HIGH">High Priority</option>
                    <option value="CRITICAL">Critical Path</option>
                  </select>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', alignItems: 'center' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Due Date / Milestone</label>
                  <input
                    type="text"
                    placeholder="e.g. Shooting Day 2 or 2026-11-20"
                    value={taskDueDate}
                    onChange={(e) => setTaskDueDate(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13px' }}
                  />
                </div>
                <div style={{ paddingTop: '16px' }}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', color: 'var(--color-navy)', cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={taskApprovalReq}
                      onChange={(e) => setTaskApprovalReq(e.target.checked)}
                    />
                    <span>Requires Producer Sign-Off</span>
                  </label>
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '12px', paddingTop: '12px', borderTop: '1px solid var(--color-border)' }}>
                <button type="button" onClick={() => setShowTaskModal(false)} className="btn btn-sm btn-outline-subtle">Cancel</button>
                <button type="submit" className="btn btn-sm btn-maroon">Add Task</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ========================================================
          MODAL: REGISTER DOCUMENT
         ======================================================== */}
      {showDocModal && (
        <div
          style={{ position: 'fixed', inset: 0, zIndex: 1000, backgroundColor: 'rgba(10, 25, 47, 0.75)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px', backdropFilter: 'blur(4px)' }}
          onClick={() => setShowDocModal(false)}
        >
          <div
            style={{ backgroundColor: 'white', borderRadius: 'var(--radius-lg)', maxWidth: '540px', width: '100%', padding: '24px', boxShadow: 'var(--shadow-modal)' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
              <h3 style={{ fontSize: '17px', color: 'var(--color-navy)', fontWeight: 800 }}>
                Register Document Record
              </h3>
              <button onClick={() => setShowDocModal(false)} style={{ cursor: 'pointer' }}><X size={18} /></button>
            </div>

            <form onSubmit={handleCreateDocument} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Document Title *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Lead Actor Exclusivity Contract"
                  value={docTitle}
                  onChange={(e) => setDocTitle(e.target.value)}
                  style={{ width: '100%', padding: '9px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13.5px' }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Document Type</label>
                  <select
                    value={docType}
                    onChange={(e) => setDocType(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13px' }}
                  >
                    <option value="SCRIPT">Script / Screenplay</option>
                    <option value="AGREEMENT">Legal Agreement / Contract</option>
                    <option value="PERMIT">Permit / Clearance</option>
                    <option value="BUDGET">Budget / Financial Sheet</option>
                    <option value="CALL_SHEET">Call Sheet</option>
                    <option value="DPR">Daily Production Report (DPR)</option>
                    <option value="QC_REPORT">Technical QC Report</option>
                    <option value="CENSOR_CERT">Censor Certificate / Rating</option>
                    <option value="OTHER">Other Production Document</option>
                  </select>
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Uploaded By</label>
                  <input
                    type="text"
                    placeholder="e.g. Production Legal Head"
                    value={docUploadedBy}
                    onChange={(e) => setDocUploadedBy(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13px' }}
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Verification Notes</label>
                <textarea
                  rows={2}
                  placeholder="Indemnity clause details, stamp duty confirmation, or expiry dates..."
                  value={docNotes}
                  onChange={(e) => setDocNotes(e.target.value)}
                  style={{ width: '100%', padding: '9px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13.5px' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '12px', paddingTop: '12px', borderTop: '1px solid var(--color-border)' }}>
                <button type="button" onClick={() => setShowDocModal(false)} className="btn btn-sm btn-outline-subtle">Cancel</button>
                <button type="submit" className="btn btn-sm btn-navy">Archive Document</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ========================================================
          MODAL: LOG RISK
         ======================================================== */}
      {showRiskModal && (
        <div
          style={{ position: 'fixed', inset: 0, zIndex: 1000, backgroundColor: 'rgba(10, 25, 47, 0.75)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px', backdropFilter: 'blur(4px)' }}
          onClick={() => setShowRiskModal(false)}
        >
          <div
            style={{ backgroundColor: 'white', borderRadius: 'var(--radius-lg)', maxWidth: '540px', width: '100%', padding: '24px', boxShadow: 'var(--shadow-modal)' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
              <h3 style={{ fontSize: '17px', color: 'var(--color-navy)', fontWeight: 800 }}>
                Log Production Risk or Delay Incident
              </h3>
              <button onClick={() => setShowRiskModal(false)} style={{ cursor: 'pointer' }}><X size={18} /></button>
            </div>

            <form onSubmit={handleCreateRisk} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Risk Title *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Coastal Monsoon Rainfall Threat"
                  value={riskTitle}
                  onChange={(e) => setRiskTitle(e.target.value)}
                  style={{ width: '100%', padding: '9px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13.5px' }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Severity Level</label>
                  <select
                    value={riskSeverity}
                    onChange={(e) => setRiskSeverity(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13px' }}
                  >
                    <option value="LOW">Low</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HIGH">High Severity</option>
                    <option value="CRITICAL">Critical Blocker</option>
                  </select>
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Risk Owner</label>
                  <input
                    type="text"
                    placeholder="e.g. Location Manager"
                    value={riskOwner}
                    onChange={(e) => setRiskOwner(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13px' }}
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Mitigation Plan / Contingency</label>
                <textarea
                  rows={3}
                  required
                  placeholder="Describe step-by-step backup action, indoor floor standby, or emergency rescheduling..."
                  value={riskMitigation}
                  onChange={(e) => setRiskMitigation(e.target.value)}
                  style={{ width: '100%', padding: '9px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13.5px' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '12px', paddingTop: '12px', borderTop: '1px solid var(--color-border)' }}>
                <button type="button" onClick={() => setShowRiskModal(false)} className="btn btn-sm btn-outline-subtle">Cancel</button>
                <button type="submit" className="btn btn-sm btn-maroon">Record Risk</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ========================================================
          MODAL: ADD EPISODE (SERIALS & WEB SERIES)
         ======================================================== */}
      {showEpisodeModal && (
        <div
          style={{ position: 'fixed', inset: 0, zIndex: 1000, backgroundColor: 'rgba(10, 25, 47, 0.75)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px', backdropFilter: 'blur(4px)' }}
          onClick={() => setShowEpisodeModal(false)}
        >
          <div
            style={{ backgroundColor: 'white', borderRadius: 'var(--radius-lg)', maxWidth: '500px', width: '100%', padding: '24px', boxShadow: 'var(--shadow-modal)' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
              <h3 style={{ fontSize: '17px', color: 'var(--color-navy)', fontWeight: 800 }}>
                Add New Episode Track
              </h3>
              <button onClick={() => setShowEpisodeModal(false)} style={{ cursor: 'pointer' }}><X size={18} /></button>
            </div>

            <form onSubmit={handleCreateEpisode} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Episode #</label>
                  <input
                    type="number"
                    min="1"
                    placeholder="e.g. 5"
                    value={epNumber}
                    onChange={(e) => setEpNumber(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13px' }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Episode Title *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. The Counter-Ambush"
                    value={epTitle}
                    onChange={(e) => setEpTitle(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13px' }}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Target Runtime</label>
                  <input
                    type="text"
                    placeholder="e.g. 48 mins"
                    value={epDuration}
                    onChange={(e) => setEpDuration(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13px' }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Air / Release Date</label>
                  <input
                    type="date"
                    value={epAirDate}
                    onChange={(e) => setEpAirDate(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13px' }}
                  />
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '12px', paddingTop: '12px', borderTop: '1px solid var(--color-border)' }}>
                <button type="button" onClick={() => setShowEpisodeModal(false)} className="btn btn-sm btn-outline-subtle">Cancel</button>
                <button type="submit" className="btn btn-sm btn-maroon">Add Episode</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ========================================================
          MODAL: UPDATE STAGE & PROGRESS
         ======================================================== */}
      {showStageModal && (
        <div
          style={{ position: 'fixed', inset: 0, zIndex: 1000, backgroundColor: 'rgba(10, 25, 47, 0.75)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px', backdropFilter: 'blur(4px)' }}
          onClick={() => setShowStageModal(false)}
        >
          <div
            style={{ backgroundColor: 'white', borderRadius: 'var(--radius-lg)', maxWidth: '500px', width: '100%', padding: '24px', boxShadow: 'var(--shadow-modal)' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
              <h3 style={{ fontSize: '17px', color: 'var(--color-navy)', fontWeight: 800 }}>
                Update Production Stage & Completion
              </h3>
              <button onClick={() => setShowStageModal(false)} style={{ cursor: 'pointer' }}><X size={18} /></button>
            </div>

            <form onSubmit={handleStageProgressUpdate} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Current Management Stage</label>
                <select
                  value={editStage}
                  onChange={(e) => setEditStage(e.target.value)}
                  style={{ width: '100%', padding: '9px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13.5px' }}
                >
                  <option value="PRE_PRODUCTION">Stage 1: Pre-Production</option>
                  <option value="PRODUCTION">Stage 2: Production</option>
                  <option value="POST_PRODUCTION">Stage 3: Post-Production</option>
                  <option value="RELEASE_MANAGEMENT">Stage 4: Release Management</option>
                </select>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>
                  Stage Completion: {editProgress}%
                </label>
                <input
                  type="range"
                  min="0"
                  max="100"
                  value={editProgress}
                  onChange={(e) => setEditProgress(parseInt(e.target.value))}
                  style={{ width: '100%', accentColor: 'var(--color-maroon)' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Overall Project Status</label>
                <select
                  value={editStatus}
                  onChange={(e) => setEditStatus(e.target.value)}
                  style={{ width: '100%', padding: '9px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13.5px' }}
                >
                  <option value="PLANNING">Planning</option>
                  <option value="PRE_PRODUCTION">Pre-Production</option>
                  <option value="IN_PRODUCTION">In Production</option>
                  <option value="POST_PRODUCTION">Post-Production</option>
                  <option value="COMPLETED">Completed</option>
                  <option value="RELEASED">Released</option>
                </select>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '12px', paddingTop: '12px', borderTop: '1px solid var(--color-border)' }}>
                <button type="button" onClick={() => setShowStageModal(false)} className="btn btn-sm btn-outline-subtle">Cancel</button>
                <button type="submit" className="btn btn-sm btn-maroon">Save Stage Transition</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ========================================================
          MODAL: EDIT PROJECT DETAILS
         ======================================================== */}
      {showEditProjectModal && (
        <div
          style={{ position: 'fixed', inset: 0, zIndex: 1000, backgroundColor: 'rgba(10, 25, 47, 0.75)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px', backdropFilter: 'blur(4px)' }}
          onClick={() => setShowEditProjectModal(false)}
        >
          <div
            style={{ backgroundColor: 'white', borderRadius: 'var(--radius-lg)', maxWidth: '640px', width: '100%', padding: '24px', maxHeight: '90vh', overflowY: 'auto', boxShadow: 'var(--shadow-modal)' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
              <h3 style={{ fontSize: '17px', color: 'var(--color-navy)', fontWeight: 800 }}>
                Edit Project Details
              </h3>
              <button onClick={() => setShowEditProjectModal(false)} style={{ cursor: 'pointer' }}><X size={18} /></button>
            </div>

            <form onSubmit={handleEditProjectSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Project Title</label>
                <input
                  type="text"
                  required
                  value={editTitle}
                  onChange={(e) => setEditTitle(e.target.value)}
                  style={{ width: '100%', padding: '9px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13.5px' }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Production Company</label>
                  <input
                    type="text"
                    value={editProductionCompany}
                    onChange={(e) => setEditProductionCompany(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13px' }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Producer In Charge</label>
                  <input
                    type="text"
                    value={editProducerName}
                    onChange={(e) => setEditProducerName(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13px' }}
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Producer Contact Email</label>
                <input
                  type="email"
                  value={editProducerEmail}
                  onChange={(e) => setEditProducerEmail(e.target.value)}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13px' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Synopsis / Scope</label>
                <textarea
                  rows={3}
                  value={editSynopsis}
                  onChange={(e) => setEditSynopsis(e.target.value)}
                  style={{ width: '100%', padding: '9px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13.5px' }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Total Line Budget</label>
                  <input
                    type="text"
                    value={editTotalBudget}
                    onChange={(e) => setEditTotalBudget(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13px' }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Spent to Date</label>
                  <input
                    type="text"
                    value={editSpentBudget}
                    onChange={(e) => setEditSpentBudget(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13px' }}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Start Date</label>
                  <input
                    type="date"
                    value={editStartDate}
                    onChange={(e) => setEditStartDate(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13px' }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', marginBottom: '4px' }}>Target Release Date</label>
                  <input
                    type="date"
                    value={editTargetReleaseDate}
                    onChange={(e) => setEditTargetReleaseDate(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '4px', border: '1px solid var(--color-border)', fontSize: '13px' }}
                  />
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '12px', paddingTop: '12px', borderTop: '1px solid var(--color-border)' }}>
                <button type="button" onClick={() => setShowEditProjectModal(false)} className="btn btn-sm btn-outline-subtle">Cancel</button>
                <button type="submit" className="btn btn-sm btn-maroon">Save Changes</button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}
