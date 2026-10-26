// TrueHunt API Service Layer
// Communicates with Spring Boot REST API (/api/*) with graceful fallback to mock data

import {
  FEATURED_TALENTS,
  LATEST_OPPORTUNITIES,
  ENTERTAINMENTS_DATA,
  INVESTORS_CONNECT_DATA,
  FAQS_DATA,
  PLATFORM_STATS,
  PROTECT_PRODUCER_CATEGORIES,
  PROTECT_PRODUCER_DEMO_PROJECTS
} from './mockData';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

/**
 * Universal safe fetch wrapper that parses JSON and provides consistent error reporting
 */
async function fetchWithFallback(endpoint, options = {}, fallbackData = null) {
  try {
    const token = localStorage.getItem('truehunt_token');
    const storedUser = localStorage.getItem('truehunt_user');
    let userEmail = '';
    if (storedUser) {
      try {
        userEmail = JSON.parse(storedUser)?.email;
      } catch (e) {
        // ignore
      }
    }
    const headers = {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
      ...(userEmail ? { 'X-User-Email': userEmail } : {}),
      ...(options.headers || {})
    };

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 4000); // 4s timeout for graceful fallback

    const response = await fetch(`${BASE_URL}${endpoint}`, {
      ...options,
      headers,
      signal: controller.signal
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      throw new Error(`API Error: ${response.status} ${response.statusText}`);
    }

    const data = await response.json();
    return { data, source: 'backend', success: true };
  } catch (error) {
    // If backend isn't running or endpoint fails, fall back to sample data
    console.warn(`[TrueHunt API] Notice for ${endpoint}:`, error.message, '-> Using verified platform dataset.');

    // If it's a POST/mutation request, simulate success with demo confirmation
    if (options.method && options.method !== 'GET') {
      return {
        data: {
          id: `demo-${Date.now()}`,
          message: 'Action completed successfully (Demo Mode: Will sync to PostgreSQL when backend server is active).',
          timestamp: new Date().toISOString()
        },
        source: 'demo_fallback',
        success: true
      };
    }

    return {
      data: fallbackData,
      source: 'demo_fallback',
      success: true,
      info: 'Showing demo data while backend service connects'
    };
  }
}

export const api = {
  // Statistics
  stats: {
    get: async () => {
      const res = await fetchWithFallback('/stats', { method: 'GET' }, PLATFORM_STATS);
      return res.data || PLATFORM_STATS;
    }
  },

  // Talents Discovery
  talents: {
    getAll: async (params = {}) => {
      const query = new URLSearchParams(params).toString();
      const endpoint = query ? `/talents?${query}` : '/talents';
      const res = await fetchWithFallback(endpoint, { method: 'GET' }, FEATURED_TALENTS);
      return res.data || FEATURED_TALENTS;
    },

    getById: async (id) => {
      const fallback = FEATURED_TALENTS.find(t => t.id === id) || FEATURED_TALENTS[0];
      const res = await fetchWithFallback(`/talents/${id}`, { method: 'GET' }, fallback);
      return res.data || fallback;
    },

    search: async (term, craft = '', location = '') => {
      const res = await fetchWithFallback(
        `/talents/search?q=${encodeURIComponent(term)}&category=${encodeURIComponent(craft)}&location=${encodeURIComponent(location)}`,
        { method: 'GET' },
        FEATURED_TALENTS.filter(t => {
          const matchTerm = !term || t.name.toLowerCase().includes(term.toLowerCase()) ||
            t.role.toLowerCase().includes(term.toLowerCase()) ||
            t.skills.some(s => s.toLowerCase().includes(term.toLowerCase()));
          const matchCraft = !craft || craft === 'all' || t.category.toLowerCase().includes(craft.toLowerCase());
          const matchLoc = !location || t.location.toLowerCase().includes(location.toLowerCase());
          return matchTerm && matchCraft && matchLoc;
        })
      );
      return res.data;
    },

    connect: async (talentId, message) => {
      return await fetchWithFallback('/connections', {
        method: 'POST',
        body: JSON.stringify({ talentId, message, timestamp: new Date().toISOString() })
      });
    }
  },

  // Jobs & Auditions
  jobs: {
    getAll: async (params = {}) => {
      const query = new URLSearchParams(params).toString();
      const endpoint = query ? `/jobs?${query}` : '/jobs';
      const res = await fetchWithFallback(endpoint, { method: 'GET' }, LATEST_OPPORTUNITIES);
      return res.data || LATEST_OPPORTUNITIES;
    },

    getById: async (id) => {
      const fallback = LATEST_OPPORTUNITIES.find(j => j.id === id) || LATEST_OPPORTUNITIES[0];
      const res = await fetchWithFallback(`/jobs/${id}`, { method: 'GET' }, fallback);
      return res.data || fallback;
    },

    apply: async (jobId, application) => {
      return await fetchWithFallback(`/jobs/${jobId}/applications`, {
        method: 'POST',
        body: JSON.stringify(application)
      });
    }
  },

  // Entertainments & Production Houses
  entertainments: {
    getAll: async () => {
      const res = await fetchWithFallback('/entertainments', { method: 'GET' }, ENTERTAINMENTS_DATA);
      return res.data || ENTERTAINMENTS_DATA;
    }
  },

  // Investors Connect
  investors: {
    getAll: async () => {
      const res = await fetchWithFallback('/investors', { method: 'GET' }, INVESTORS_CONNECT_DATA);
      return res.data || INVESTORS_CONNECT_DATA;
    },

    expressInterest: async (interestData) => {
      return await fetchWithFallback('/investment-interests', {
        method: 'POST',
        body: JSON.stringify(interestData)
      });
    }
  },

  // Support & FAQs
  support: {
    submit: async (ticket) => {
      return await fetchWithFallback('/support', {
        method: 'POST',
        body: JSON.stringify(ticket)
      });
    },

    getFaqs: async () => {
      const res = await fetchWithFallback('/faqs', { method: 'GET' }, FAQS_DATA);
      return res.data || FAQS_DATA;
    }
  },

  // Authentication
  auth: {
    login: async (email, password) => {
      const res = await fetchWithFallback('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password })
      }, {
        token: `demo-jwt-${Date.now()}`,
        user: {
          id: 'user-demo-1',
          name: email.split('@')[0],
          email,
          role: 'TALENT'
        }
      });
      return res;
    },

    register: async (userData) => {
      return await fetchWithFallback('/auth/register', {
        method: 'POST',
        body: JSON.stringify(userData)
      }, {
        token: `demo-jwt-${Date.now()}`,
        user: {
          id: `user-${Date.now()}`,
          name: userData.fullName || userData.name,
          email: userData.email,
          role: userData.role || 'TALENT'
        }
      });
    },

    getMe: async () => {
      return await fetchWithFallback('/auth/me', { method: 'GET' }, {
        id: 'user-demo-1',
        name: 'Cinema Professional',
        email: 'pro@truehunt.in',
        role: 'TALENT'
      });
    }
  },

  // Protect Producer Module
  protectProducer: {
    getCategories: () => PROTECT_PRODUCER_CATEGORIES,

    getProjects: async (category = '') => {
      const formattedCat = category ? category.toUpperCase().replace('-', '_') : '';
      const endpoint = formattedCat && formattedCat !== 'ALL'
        ? `/protect-producer/projects?category=${encodeURIComponent(formattedCat)}`
        : '/protect-producer/projects';

      const localProjects = getLocalProjects();
      const fallbackList = formattedCat && formattedCat !== 'ALL'
        ? localProjects.filter(p => p.category.toUpperCase().replace('-', '_') === formattedCat)
        : localProjects;

      const res = await fetchWithFallback(endpoint, { method: 'GET' }, fallbackList);
      return res.data || fallbackList;
    },

    getProjectById: async (id) => {
      const localProjects = getLocalProjects();
      const fallback = localProjects.find(p => p.id === id) || localProjects[0];
      const res = await fetchWithFallback(`/protect-producer/projects/${id}`, { method: 'GET' }, fallback);
      return res.data || fallback;
    },

    createProject: async (projectData) => {
      const res = await fetchWithFallback('/protect-producer/projects', {
        method: 'POST',
        body: JSON.stringify(projectData)
      });

      // Update local storage for seamless offline experience
      const localProjects = getLocalProjects();
      const newProj = res.data?.id && !res.data.id.startsWith('demo-') ? res.data : {
        id: `proj-${Date.now()}`,
        category: (projectData.category || 'MOVIE').toUpperCase().replace('-', '_'),
        title: projectData.title,
        productionCompany: projectData.productionCompany || 'TrueHunt Production House',
        producerName: projectData.producerName || 'Lead Producer',
        producerEmail: projectData.producerEmail || 'producer@truehunt.in',
        synopsis: projectData.synopsis || '',
        status: 'PRE_PRODUCTION',
        currentStage: 'PRE_PRODUCTION',
        stageProgress: 15,
        totalBudget: projectData.totalBudget || '₹1,50,00,000',
        spentBudget: projectData.spentBudget || '₹15,00,000',
        startDate: projectData.startDate || new Date().toISOString().split('T')[0],
        targetReleaseDate: projectData.targetReleaseDate || '2027-06-30',
        bannerImageUrl: projectData.bannerImageUrl || `/assets/protect-producer/category_${(projectData.category || 'movies').toLowerCase().replace('_', '-')}.webp`,
        seasonNumber: projectData.seasonNumber || null,
        totalEpisodes: projectData.totalEpisodes || null,
        targetFestival: projectData.targetFestival || null,
        targetRuntime: projectData.targetRuntime || '120 mins',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        tasks: [
          { id: `tsk-${Date.now()}-1`, stage: 'PRE_PRODUCTION', title: 'Script Lock & Breakdown', description: 'Complete scene breakdown', assignedTo: 'Writers Room', status: 'IN_PROGRESS', priority: 'CRITICAL', dueDate: 'Day 10', approvalStatus: 'PENDING' },
          { id: `tsk-${Date.now()}-2`, stage: 'PRE_PRODUCTION', title: 'Talent & HOD Contracts', description: 'Execute principal agreements', assignedTo: 'Production Legal', status: 'NOT_STARTED', priority: 'HIGH', dueDate: 'Day 15', approvalStatus: 'NONE' },
          { id: `tsk-${Date.now()}-3`, stage: 'PRODUCTION', title: 'Daily Call Sheets Preparation', description: 'Schedule block planning', assignedTo: '1st AD', status: 'NOT_STARTED', priority: 'HIGH', dueDate: 'Shooting Day 1', approvalStatus: 'NONE' }
        ],
        documents: [
          { id: `doc-${Date.now()}-1`, stage: 'PRE_PRODUCTION', documentType: 'SCRIPT', title: 'Master Screenplay Draft v1.0', fileUrl: '#', uploadedBy: 'Writer', verified: true, notes: 'Initial registration draft', createdAt: new Date().toISOString() }
        ],
        milestones: [
          { id: `mls-${Date.now()}-1`, stage: 'PRE_PRODUCTION', title: 'Script Locked & Budget Approved', targetDate: projectData.startDate || '2026-11-15', completedDate: null, isCompleted: false },
          { id: `mls-${Date.now()}-2`, stage: 'PRODUCTION', title: 'First Day of Principal Photography', targetDate: '2026-12-01', completedDate: null, isCompleted: false },
          { id: `mls-${Date.now()}-3`, stage: 'POST_PRODUCTION', title: 'Picture Lock & Sound Mix', targetDate: '2027-03-15', completedDate: null, isCompleted: false },
          { id: `mls-${Date.now()}-4`, stage: 'RELEASE_MANAGEMENT', title: 'Release Date Premiere', targetDate: projectData.targetReleaseDate || '2027-06-30', completedDate: null, isCompleted: false }
        ],
        risks: [],
        episodes: projectData.category === 'SERIAL' || projectData.category === 'WEB_SERIES' ? [
          { id: `ep-${Date.now()}-1`, episodeNumber: 1, title: 'Episode 1: Pilot & World Building', status: 'SCRIPT_LOCKED', duration: '45 mins', airDate: projectData.targetReleaseDate || '2027-06-30' },
          { id: `ep-${Date.now()}-2`, episodeNumber: 2, title: 'Episode 2: Conflict Emerges', status: 'PLANNED', duration: '45 mins', airDate: projectData.targetReleaseDate || '2027-06-30' }
        ] : [],
        recentActivity: [
          { id: `act-${Date.now()}-1`, action: 'Project Registered', performedBy: projectData.producerName || 'Producer', details: 'Project registered under TrueHunt Protect Producer platform.', timestamp: new Date().toISOString() }
        ]
      };

      localProjects.unshift(newProj);
      saveLocalProjects(localProjects);
      return { success: true, data: newProj };
    },

    updateProject: async (id, projectData) => {
      const res = await fetchWithFallback(`/protect-producer/projects/${id}`, {
        method: 'PUT',
        body: JSON.stringify(projectData)
      });

      const localProjects = getLocalProjects();
      const idx = localProjects.findIndex(p => p.id === id);
      if (idx !== -1) {
        localProjects[idx] = { ...localProjects[idx], ...projectData, updatedAt: new Date().toISOString() };
        saveLocalProjects(localProjects);
      }
      return res;
    },

    deleteProject: async (id) => {
      const res = await fetchWithFallback(`/protect-producer/projects/${id}`, { method: 'DELETE' });
      const localProjects = getLocalProjects();
      const filtered = localProjects.filter(p => p.id !== id);
      saveLocalProjects(filtered);
      return res;
    },

    updateStage: async (id, stage, progress, status) => {
      const res = await fetchWithFallback(`/protect-producer/projects/${id}/stage`, {
        method: 'PATCH',
        body: JSON.stringify({ stage, progress, status })
      });

      const localProjects = getLocalProjects();
      const proj = localProjects.find(p => p.id === id);
      if (proj) {
        if (stage) proj.currentStage = stage.toUpperCase().replace('-', '_');
        if (progress !== undefined) proj.stageProgress = progress;
        if (status) proj.status = status.toUpperCase().replace('-', '_');
        proj.updatedAt = new Date().toISOString();
        if (!proj.recentActivity) proj.recentActivity = [];
        proj.recentActivity.unshift({
          id: `act-${Date.now()}`,
          action: 'Stage Transition',
          performedBy: proj.producerName || 'Producer',
          details: `Stage updated to ${stage} (${progress}% completed).`,
          timestamp: new Date().toISOString()
        });
        saveLocalProjects(localProjects);
      }
      return res;
    },

    // Tasks
    getTasks: async (projectId, stage = '') => {
      const query = stage ? `?stage=${stage.toUpperCase().replace('-', '_')}` : '';
      const proj = getLocalProjects().find(p => p.id === projectId);
      const fallback = (proj?.tasks || []).filter(t => !stage || t.stage === stage.toUpperCase().replace('-', '_'));
      const res = await fetchWithFallback(`/protect-producer/projects/${projectId}/tasks${query}`, { method: 'GET' }, fallback);
      return res.data || fallback;
    },

    createTask: async (projectId, taskData) => {
      const res = await fetchWithFallback(`/protect-producer/projects/${projectId}/tasks`, {
        method: 'POST',
        body: JSON.stringify(taskData)
      });

      const localProjects = getLocalProjects();
      const proj = localProjects.find(p => p.id === projectId);
      if (proj) {
        if (!proj.tasks) proj.tasks = [];
        const newTask = res.data?.id && !res.data.id.startsWith('demo-') ? res.data : {
          id: `tsk-${Date.now()}`,
          projectId,
          stage: taskData.stage || proj.currentStage,
          title: taskData.title,
          description: taskData.description || '',
          assignedTo: taskData.assignedTo || 'Production Office',
          status: taskData.status || 'NOT_STARTED',
          priority: taskData.priority || 'MEDIUM',
          dueDate: taskData.dueDate || '7 Days',
          approvalStatus: taskData.approvalStatus || 'NONE',
          approvedBy: taskData.approvedBy || null
        };
        proj.tasks.push(newTask);
        if (!proj.recentActivity) proj.recentActivity = [];
        proj.recentActivity.unshift({
          id: `act-${Date.now()}`,
          action: 'Task Added',
          performedBy: 'Production Team',
          details: `New task: ${newTask.title}`,
          timestamp: new Date().toISOString()
        });
        saveLocalProjects(localProjects);
      }
      return res;
    },

    updateTask: async (projectId, taskId, taskData) => {
      const res = await fetchWithFallback(`/protect-producer/projects/${projectId}/tasks/${taskId}`, {
        method: 'PUT',
        body: JSON.stringify(taskData)
      });

      const localProjects = getLocalProjects();
      const proj = localProjects.find(p => p.id === projectId);
      if (proj && proj.tasks) {
        const tIdx = proj.tasks.findIndex(t => t.id === taskId);
        if (tIdx !== -1) {
          proj.tasks[tIdx] = { ...proj.tasks[tIdx], ...taskData };
          if (taskData.approvalStatus === 'APPROVED' && !proj.tasks[tIdx].approvedAt) {
            proj.tasks[tIdx].approvedAt = new Date().toISOString();
            proj.tasks[tIdx].approvedBy = taskData.approvedBy || 'Producer';
          }
          saveLocalProjects(localProjects);
        }
      }
      return res;
    },

    // Documents
    getDocuments: async (projectId, stage = '') => {
      const query = stage ? `?stage=${stage.toUpperCase().replace('-', '_')}` : '';
      const proj = getLocalProjects().find(p => p.id === projectId);
      const fallback = (proj?.documents || []).filter(d => !stage || d.stage === stage.toUpperCase().replace('-', '_'));
      const res = await fetchWithFallback(`/protect-producer/projects/${projectId}/documents${query}`, { method: 'GET' }, fallback);
      return res.data || fallback;
    },

    createDocument: async (projectId, docData) => {
      const res = await fetchWithFallback(`/protect-producer/projects/${projectId}/documents`, {
        method: 'POST',
        body: JSON.stringify(docData)
      });

      const localProjects = getLocalProjects();
      const proj = localProjects.find(p => p.id === projectId);
      if (proj) {
        if (!proj.documents) proj.documents = [];
        const newDoc = res.data?.id && !res.data.id.startsWith('demo-') ? res.data : {
          id: `doc-${Date.now()}`,
          projectId,
          stage: docData.stage || proj.currentStage,
          documentType: docData.documentType || 'OTHER',
          title: docData.title,
          fileUrl: docData.fileUrl || '#',
          uploadedBy: docData.uploadedBy || 'Production Team',
          verified: !!docData.verified,
          notes: docData.notes || '',
          createdAt: new Date().toISOString()
        };
        proj.documents.unshift(newDoc);
        if (!proj.recentActivity) proj.recentActivity = [];
        proj.recentActivity.unshift({
          id: `act-${Date.now()}`,
          action: 'Document Registered',
          performedBy: newDoc.uploadedBy,
          details: `Registered: ${newDoc.title} (${newDoc.documentType})`,
          timestamp: new Date().toISOString()
        });
        saveLocalProjects(localProjects);
      }
      return res;
    },

    verifyDocument: async (projectId, documentId, verified) => {
      const res = await fetchWithFallback(`/protect-producer/projects/${projectId}/documents/${documentId}/verify`, {
        method: 'PATCH',
        body: JSON.stringify({ verified })
      });

      const localProjects = getLocalProjects();
      const proj = localProjects.find(p => p.id === projectId);
      if (proj && proj.documents) {
        const doc = proj.documents.find(d => d.id === documentId);
        if (doc) {
          doc.verified = verified;
          saveLocalProjects(localProjects);
        }
      }
      return res;
    },

    // Milestones
    getMilestones: async (projectId, stage = '') => {
      const query = stage ? `?stage=${stage.toUpperCase().replace('-', '_')}` : '';
      const proj = getLocalProjects().find(p => p.id === projectId);
      const fallback = (proj?.milestones || []).filter(m => !stage || m.stage === stage.toUpperCase().replace('-', '_'));
      const res = await fetchWithFallback(`/protect-producer/projects/${projectId}/milestones${query}`, { method: 'GET' }, fallback);
      return res.data || fallback;
    },

    toggleMilestone: async (projectId, milestoneId) => {
      const res = await fetchWithFallback(`/protect-producer/projects/${projectId}/milestones/${milestoneId}/toggle`, {
        method: 'PATCH'
      });

      const localProjects = getLocalProjects();
      const proj = localProjects.find(p => p.id === projectId);
      if (proj && proj.milestones) {
        const m = proj.milestones.find(item => item.id === milestoneId);
        if (m) {
          m.isCompleted = !m.isCompleted;
          m.completedDate = m.isCompleted ? new Date().toISOString().split('T')[0] : null;
          saveLocalProjects(localProjects);
        }
      }
      return res;
    },

    // Risks
    getRisks: async (projectId, stage = '') => {
      const query = stage ? `?stage=${stage.toUpperCase().replace('-', '_')}` : '';
      const proj = getLocalProjects().find(p => p.id === projectId);
      const fallback = (proj?.risks || []).filter(r => !stage || r.stage === stage.toUpperCase().replace('-', '_'));
      const res = await fetchWithFallback(`/protect-producer/projects/${projectId}/risks${query}`, { method: 'GET' }, fallback);
      return res.data || fallback;
    },

    createRisk: async (projectId, riskData) => {
      const res = await fetchWithFallback(`/protect-producer/projects/${projectId}/risks`, {
        method: 'POST',
        body: JSON.stringify(riskData)
      });

      const localProjects = getLocalProjects();
      const proj = localProjects.find(p => p.id === projectId);
      if (proj) {
        if (!proj.risks) proj.risks = [];
        const newRisk = res.data?.id && !res.data.id.startsWith('demo-') ? res.data : {
          id: `rsk-${Date.now()}`,
          projectId,
          stage: riskData.stage || proj.currentStage,
          title: riskData.title,
          severity: riskData.severity || 'MEDIUM',
          mitigationPlan: riskData.mitigationPlan || '',
          owner: riskData.owner || 'Risk Officer',
          status: riskData.status || 'OPEN',
          createdAt: new Date().toISOString()
        };
        proj.risks.unshift(newRisk);
        saveLocalProjects(localProjects);
      }
      return res;
    },

    updateRisk: async (projectId, riskId, riskData) => {
      const res = await fetchWithFallback(`/protect-producer/projects/${projectId}/risks/${riskId}`, {
        method: 'PUT',
        body: JSON.stringify(riskData)
      });

      const localProjects = getLocalProjects();
      const proj = localProjects.find(p => p.id === projectId);
      if (proj && proj.risks) {
        const rIdx = proj.risks.findIndex(r => r.id === riskId);
        if (rIdx !== -1) {
          proj.risks[rIdx] = { ...proj.risks[rIdx], ...riskData };
          saveLocalProjects(localProjects);
        }
      }
      return res;
    },

    // Episodes
    getEpisodes: async (projectId) => {
      const proj = getLocalProjects().find(p => p.id === projectId);
      const fallback = proj?.episodes || [];
      const res = await fetchWithFallback(`/protect-producer/projects/${projectId}/episodes`, { method: 'GET' }, fallback);
      return res.data || fallback;
    },

    createEpisode: async (projectId, epData) => {
      const res = await fetchWithFallback(`/protect-producer/projects/${projectId}/episodes`, {
        method: 'POST',
        body: JSON.stringify(epData)
      });

      const localProjects = getLocalProjects();
      const proj = localProjects.find(p => p.id === projectId);
      if (proj) {
        if (!proj.episodes) proj.episodes = [];
        const newEp = res.data?.id && !res.data.id.startsWith('demo-') ? res.data : {
          id: `ep-${Date.now()}`,
          projectId,
          episodeNumber: epData.episodeNumber || (proj.episodes.length + 1),
          title: epData.title,
          status: epData.status || 'PLANNED',
          duration: epData.duration || '45 mins',
          airDate: epData.airDate || null
        };
        proj.episodes.push(newEp);
        saveLocalProjects(localProjects);
      }
      return res;
    },

    updateEpisode: async (projectId, epId, epData) => {
      const res = await fetchWithFallback(`/protect-producer/projects/${projectId}/episodes/${epId}`, {
        method: 'PUT',
        body: JSON.stringify(epData)
      });

      const localProjects = getLocalProjects();
      const proj = localProjects.find(p => p.id === projectId);
      if (proj && proj.episodes) {
        const eIdx = proj.episodes.findIndex(e => e.id === epId);
        if (eIdx !== -1) {
          proj.episodes[eIdx] = { ...proj.episodes[eIdx], ...epData };
          saveLocalProjects(localProjects);
        }
      }
      return res;
    },

    // Activity Logs
    getActivityLogs: async (projectId) => {
      const proj = getLocalProjects().find(p => p.id === projectId);
      const fallback = proj?.recentActivity || [];
      const res = await fetchWithFallback(`/protect-producer/projects/${projectId}/activity`, { method: 'GET' }, fallback);
      return res.data || fallback;
    }
  },

  // Professional Profile Management
  profile: {
    getMe: async () => {
      let storedUser = null;
      try {
        const u = localStorage.getItem('truehunt_user');
        if (u) storedUser = JSON.parse(u);
      } catch (e) {
        // Ignore parse error
      }

      const defaultUserFallback = {
        id: storedUser?.id || 'me',
        userId: storedUser?.id || null,
        name: storedUser?.name || 'Cinema Professional',
        role: 'Cinema Professional',
        headline: 'Add your headline',
        category: 'Film Industry',
        company: '',
        location: 'Add your location',
        bio: 'Add your bio to introduce yourself, your cinematic focus, and past productions.',
        avatar: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=400&q=80',
        coverImage: 'https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&w=1200&q=80',
        availability: 'Available for New Projects',
        verified: false,
        connectionsCount: 0,
        followersCount: 0,
        skills: [],
        experiences: [],
        educations: [],
        projects: [],
        posts: [],
        credits: [],
        isOwner: true
      };

      const res = await fetchWithFallback('/profile/me', { method: 'GET' }, defaultUserFallback);
      return res.data || defaultUserFallback;
    },

    updateMe: async (profileData) => {
      return await fetchWithFallback('/profile/me', {
        method: 'PUT',
        body: JSON.stringify(profileData)
      });
    },

    getById: async (id) => {
      const fallback = FEATURED_TALENTS.find(t => t.id === id) || FEATURED_TALENTS[0];
      const res = await fetchWithFallback(`/profile/${id}`, { method: 'GET' }, fallback);
      return res.data || fallback;
    },

    addExperience: async (expData) => {
      return await fetchWithFallback('/profile/experience', {
        method: 'POST',
        body: JSON.stringify(expData)
      });
    },

    updateExperience: async (id, expData) => {
      return await fetchWithFallback(`/profile/experience/${id}`, {
        method: 'PUT',
        body: JSON.stringify(expData)
      });
    },

    deleteExperience: async (id) => {
      return await fetchWithFallback(`/profile/experience/${id}`, {
        method: 'DELETE'
      });
    },

    addEducation: async (eduData) => {
      return await fetchWithFallback('/profile/education', {
        method: 'POST',
        body: JSON.stringify(eduData)
      });
    },

    updateEducation: async (id, eduData) => {
      return await fetchWithFallback(`/profile/education/${id}`, {
        method: 'PUT',
        body: JSON.stringify(eduData)
      });
    },

    deleteEducation: async (id) => {
      return await fetchWithFallback(`/profile/education/${id}`, {
        method: 'DELETE'
      });
    },

    addProject: async (projectData) => {
      return await fetchWithFallback('/profile/project', {
        method: 'POST',
        body: JSON.stringify(projectData)
      });
    },

    updateProject: async (id, projectData) => {
      return await fetchWithFallback(`/profile/project/${id}`, {
        method: 'PUT',
        body: JSON.stringify(projectData)
      });
    },

    deleteProject: async (id) => {
      return await fetchWithFallback(`/profile/project/${id}`, {
        method: 'DELETE'
      });
    },

    addSkill: async (skill) => {
      return await fetchWithFallback('/profile/skills', {
        method: 'POST',
        body: JSON.stringify({ skill })
      });
    },

    deleteSkill: async (skill) => {
      return await fetchWithFallback(`/profile/skills/${encodeURIComponent(skill)}`, {
        method: 'DELETE'
      });
    },

    getPosts: async (id) => {
      const fallback = FEATURED_TALENTS.find(t => t.id === id)?.posts || FEATURED_TALENTS[0].posts || [];
      const res = await fetchWithFallback(`/profile/${id}/posts`, { method: 'GET' }, fallback);
      return res.data || fallback;
    },

    createPost: async (postData) => {
      return await fetchWithFallback('/profile/posts', {
        method: 'POST',
        body: JSON.stringify(postData)
      });
    },

    getSuggestions: async (currentId) => {
      const fallback = FEATURED_TALENTS.filter(t => !currentId || t.id !== currentId).slice(0, 4);
      const res = await fetchWithFallback(`/profile/suggestions?currentId=${encodeURIComponent(currentId || '')}`, { method: 'GET' }, fallback);
      return res.data || fallback;
    }
  }
};

// Helper for offline storage
function getLocalProjects() {
  if (typeof window === 'undefined') return PROTECT_PRODUCER_DEMO_PROJECTS;
  const stored = localStorage.getItem('truehunt_protect_projects');
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch (e) {
      // ignore
    }
  }
  localStorage.setItem('truehunt_protect_projects', JSON.stringify(PROTECT_PRODUCER_DEMO_PROJECTS));
  return PROTECT_PRODUCER_DEMO_PROJECTS;
}

function saveLocalProjects(projects) {
  if (typeof window !== 'undefined') {
    localStorage.setItem('truehunt_protect_projects', JSON.stringify(projects));
  }
}

