/**
 * RapidResQ Web Client & Global UI Utility Layer
 * Manages JWT/Bearer session state, REST API calls, toast notifications,
 * and common navigation bar rendering.
 */

const RapidResQ = (() => {
  const API_BASE = '/api';
  const TOKEN_KEY = 'rrq_token';
  const USER_KEY = 'rrq_user';
  const THEME_KEY = 'rrq_theme';

  // -------------------------------------------------------------
  // Theme: Mission-Control dark by default, user-toggleable
  // -------------------------------------------------------------
  function initTheme() {
    const saved = localStorage.getItem(THEME_KEY) || 'dark';
    document.documentElement.setAttribute('data-theme', saved);
  }
  initTheme();

  function toggleTheme() {
    const next = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', next);
    localStorage.setItem(THEME_KEY, next);
    const btn = document.querySelector('.theme-toggle');
    if (btn) btn.textContent = next === 'dark' ? '☀️' : '🌙';
  }

  // Cursor-follow spotlight on cards (Mission-Control glow)
  function initSpotlight() {
    document.addEventListener('mousemove', (e) => {
      const card = e.target.closest ? e.target.closest('.card, .kpi-card') : null;
      if (!card) return;
      const rect = card.getBoundingClientRect();
      card.style.setProperty('--mx', `${e.clientX - rect.left}px`);
      card.style.setProperty('--my', `${e.clientY - rect.top}px`);
    }, { passive: true });
  }
  if (document.readyState !== 'loading') initSpotlight();
  else document.addEventListener('DOMContentLoaded', initSpotlight);

  // -------------------------------------------------------------
  // Session & Authentication Management
  // -------------------------------------------------------------
  function getToken() {
    return localStorage.getItem(TOKEN_KEY);
  }

  function setSession(token, user) {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    if (user) localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  function getUser() {
    const raw = localStorage.getItem(USER_KEY);
    try {
      return raw ? JSON.parse(raw) : null;
    } catch (e) {
      return null;
    }
  }

  function clearSession() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  function isAuthenticated() {
    return !!getToken() && !!getUser();
  }

  function hasRole(role) {
    const user = getUser();
    return user && user.role === role;
  }

  // -------------------------------------------------------------
  // HTTP Fetch Wrapper with Auth Header
  // -------------------------------------------------------------
  async function apiFetch(endpoint, options = {}) {
    const headers = options.headers || {};
    const token = getToken();

    if (token && !headers['Authorization']) {
      headers['Authorization'] = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
    }

    if (options.body && typeof options.body === 'object' && !(options.body instanceof FormData)) {
      headers['Content-Type'] = 'application/json';
      options.body = JSON.stringify(options.body);
    }

    options.headers = headers;

    try {
      const response = await fetch(`${API_BASE}${endpoint}`, options);
      const data = await response.json().catch(() => null);

      if (!response.ok) {
        const errorMsg = data && (data.message || data.error) ? (data.message || data.error) : `Error: ${response.statusText}`;
        throw new Error(errorMsg);
      }

      return data;
    } catch (err) {
      console.error(`API Error [${endpoint}]:`, err);
      throw err;
    }
  }

  // -------------------------------------------------------------
  // API Endpoints
  // -------------------------------------------------------------
  const api = {
    // Auth
    async login(username, password) {
      const res = await apiFetch('/auth/login', {
        method: 'POST',
        body: { username, password }
      });
      setSession(res.token, {
        username: res.username,
        role: res.role,
        trustScore: res.trustScore
      });
      return res;
    },

    async register(username, password, role, volunteerTeamType) {
      const body = { username, password, role };
      if (volunteerTeamType) body.volunteerTeamType = volunteerTeamType;
      const res = await apiFetch('/auth/register', {
        method: 'POST',
        body
      });
      setSession(res.token, {
        username: res.username,
        role: res.role,
        trustScore: res.trustScore
      });
      return res;
    },

    async getMe() {
      return await apiFetch('/auth/me');
    },

    async logout() {
      try {
        await apiFetch('/auth/logout', { method: 'POST' });
      } catch (ignored) {}
      clearSession();
      window.location.href = '/index.html';
    },

    // Quick demo auto-login
    async quickLogin(username, password) {
      try {
        await this.login(username, password);
        showToast(`Logged in as ${username}`, 'success');
        setTimeout(() => window.location.reload(), 300);
      } catch (err) {
        showToast(`Demo login failed: ${err.message}`, 'error');
      }
    },

    // Incidents
    async getIncidents() {
      return await apiFetch('/incidents');
    },

    async getIncident(id) {
      return await apiFetch(`/incidents/${id}`);
    },

    async reportIncident(payload) {
      return await apiFetch('/incidents', {
        method: 'POST',
        body: payload
      });
    },

    async confirmIncident(id) {
      return await apiFetch(`/incidents/${id}/confirm`, {
        method: 'POST'
      });
    },

    async assignTeam(id) {
      return await apiFetch(`/incidents/${id}/assign-team`, {
        method: 'POST'
      });
    },

    async resolveIncident(id) {
      return await apiFetch(`/incidents/${id}/resolve`, {
        method: 'POST'
      });
    },

    async markFalseAlarm(id) {
      return await apiFetch(`/incidents/${id}/false-alarm`, {
        method: 'POST'
      });
    },

    // Teams
    async getTeams() {
      return await apiFetch('/teams');
    },

    async getTeam(id) {
      return await apiFetch(`/teams/${id}`);
    },

    // Relief & Donations
    async getReliefNeeds() {
      return await apiFetch('/donations/needs');
    },

    async donate(needId, donationData) {
      return await apiFetch(`/donations/needs/${needId}/contribute`, {
        method: 'POST',
        body: donationData
      });
    },

    async getDonorRegistry() {
      return await apiFetch('/admin/donors');
    },

    // Admin & Analytics
    async getAdminUsers() {
      return await apiFetch('/admin/users');
    },

    async updateUserRole(username, role) {
      return await apiFetch(`/admin/users/${username}/role`, {
        method: 'PUT',
        body: { role }
      });
    },

    async updateUserTrust(username, trustScore) {
      return await apiFetch(`/admin/users/${username}/trust`, {
        method: 'PUT',
        body: { trustScore: parseInt(trustScore) }
      });
    },

    async toggleUserStatus(username) {
      return await apiFetch(`/admin/users/${username}/status`, {
        method: 'PUT'
      });
    },

    async overridePriority(incidentId, priority) {
      return await apiFetch(`/admin/incidents/${incidentId}/priority`, {
        method: 'PUT',
        body: { priority: parseFloat(priority) }
      });
    },

    async reassignTeam(incidentId, teamId) {
      return await apiFetch(`/admin/incidents/${incidentId}/reassign`, {
        method: 'POST',
        body: { teamId }
      });
    },

    async forceResolve(incidentId) {
      return await apiFetch(`/admin/incidents/${incidentId}/force-resolve`, {
        method: 'POST'
      });
    },

    async getEscalationLogs() {
      return await apiFetch('/admin/escalation-logs');
    },

    async getAuditLogs() {
      return await apiFetch('/admin/audit-logs');
    },

    async getAnalytics() {
      return await apiFetch('/admin/analytics');
    },

    // --- Operations (admin) & volunteer portal ---
    async getVolunteerRegistry() {
      return await apiFetch('/ops/volunteers');
    },

    async sendVolunteerSignal(payload) {
      return await apiFetch('/ops/volunteers/signal', {
        method: 'POST',
        body: payload
      });
    },

    async runVerificationCheck(incidentId, method, passed, notes) {
      return await apiFetch(`/ops/incidents/${incidentId}/verify`, {
        method: 'POST',
        body: { method, passed, notes }
      });
    },

    async getVerificationChecks(incidentId) {
      return await apiFetch(`/ops/incidents/${incidentId}/verify`);
    },

    async getMissionReports() {
      return await apiFetch('/ops/mission-reports');
    },

    async broadcastVolunteers(message, teamTypes) {
      return await apiFetch('/ops/volunteers/broadcast', {
        method: 'POST',
        body: { message, teamTypes }
      });
    },

    async teamAction(teamId, action, payload = {}) {
      return await apiFetch(`/ops/teams/${teamId}/${action}`, {
        method: 'POST',
        body: payload
      });
    },

    async getVolunteerInbox() {
      return await apiFetch('/volunteer/inbox');
    },

    async ackSignal(signalId) {
      return await apiFetch(`/volunteer/inbox/${signalId}/ack`, {
        method: 'POST'
      });
    }
  };

  // -------------------------------------------------------------
  // Toast Notifications
  // -------------------------------------------------------------
  function showToast(message, type = 'info') {
    let container = document.getElementById('rrq-toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'rrq-toast-container';
      container.className = 'toast-container';
      document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    let icon = 'ℹ️';
    if (type === 'success') icon = '✅';
    if (type === 'error') icon = '❌';
    if (type === 'warning') icon = '⚠️';

    toast.innerHTML = `
      <div style="display:flex; align-items:center; gap:0.5rem;">
        <span>${icon}</span>
        <span>${message}</span>
      </div>
      <button style="background:none; border:none; cursor:pointer; color:#94A3B8; font-size:1.1rem;" onclick="this.parentElement.remove()">&times;</button>
    `;

    container.appendChild(toast);
    setTimeout(() => {
      if (toast.parentElement) toast.remove();
    }, 4500);
  }

  // -------------------------------------------------------------
  // Formatters & UI Badges
  // -------------------------------------------------------------
  function getDisasterIcon(type) {
    switch (type) {
      case 'FIRE': return '🔥';
      case 'FLOOD': return '🌊';
      case 'ACCIDENT': return '🚗';
      case 'EARTHQUAKE': return '🌋';
      default: return '⚠️';
    }
  }

  function getDisasterLabel(type) {
    switch (type) {
      case 'FIRE': return 'Fire Outbreak';
      case 'FLOOD': return 'Water Flood';
      case 'ACCIDENT': return 'Transport Accident';
      case 'EARTHQUAKE': return 'Earthquake Tremor';
      default: return type;
    }
  }

  function formatSeverityBadge(severity) {
    const s = (severity || 'LOW').toUpperCase();
    let badgeClass = 'badge-low';
    if (s === 'CRITICAL') badgeClass = 'badge-critical';
    else if (s === 'HIGH') badgeClass = 'badge-high';
    else if (s === 'MODERATE') badgeClass = 'badge-moderate';
    return `<span class="badge ${badgeClass}">${s}</span>`;
  }

  function formatStatusBadge(status) {
    const s = (status || 'REPORTED').toUpperCase();
    let badgeClass = 'badge-status-reported';
    if (s === 'VERIFIED') badgeClass = 'badge-status-verified';
    else if (s === 'IN_PROGRESS') badgeClass = 'badge-status-progress';
    else if (s === 'RESOLVED') badgeClass = 'badge-status-resolved';
    return `<span class="badge ${badgeClass}">${s.replace('_', ' ')}</span>`;
  }

  function formatCurrency(amount) {
    return '৳' + Number(amount || 0).toLocaleString('en-US');
  }

  function formatDate(isoStr) {
    if (!isoStr) return 'N/A';
    try {
      const d = new Date(isoStr);
      return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });
    } catch (e) {
      return isoStr;
    }
  }

  // -------------------------------------------------------------
  // Universal Navbar & Demo Bar Injector
  // -------------------------------------------------------------
  function renderNavbar(activePage = 'home') {
    const navPlaceholder = document.getElementById('navbar-placeholder');
    if (!navPlaceholder) return;

    const user = getUser();
    const userRole = user ? user.role : null;

    let authSection = '';
    if (user) {
      authSection = `
        <div style="display:flex; align-items:center; gap:0.75rem;">
          <div style="text-align:right;">
            <div style="font-weight:700; font-size:0.85rem; color:var(--primary);">${user.username}</div>
            <div style="font-size:0.75rem; color:var(--text-muted); display:flex; align-items:center; gap:0.25rem;">
              <span class="badge" style="background:#E2E8F0; padding:1px 6px; font-size:0.65rem;">${user.role}</span>
              <span>• Trust: ${user.trustScore}%</span>
            </div>
          </div>
          <button class="btn btn-outline btn-sm" onclick="RapidResQ.api.logout()">Sign Out</button>
          <button class="theme-toggle" onclick="RapidResQ.toggleTheme()" title="Toggle light / dark theme">${document.documentElement.getAttribute('data-theme') === 'dark' ? '☀️' : '🌙'}</button>
        </div>
      `;
    } else {
      authSection = `
        <a href="/login.html" class="btn btn-outline btn-sm">Sign In</a>
        <a href="/register.html" class="btn btn-primary btn-sm">Get Started</a>
        <button class="theme-toggle" onclick="RapidResQ.toggleTheme()" title="Toggle light / dark theme">${document.documentElement.getAttribute('data-theme') === 'dark' ? '☀️' : '🌙'}</button>
      `;
    }

    const demoBar = `
      <div class="demo-role-bar">
        <span style="font-weight:700; color:var(--text-muted); display:flex; align-items:center; gap:0.35rem;">
          <span class="beacon"></span> 1-Click Demo Login:
        </span>
        <button class="role-tag-btn" onclick="RapidResQ.api.quickLogin('admin', 'admin123')">Admin (100% Trust)</button>
        <button class="role-tag-btn" onclick="RapidResQ.api.quickLogin('citizen1', 'pass123')">Citizen (75% Trust)</button>
        <button class="role-tag-btn" onclick="RapidResQ.api.quickLogin('volunteer1', 'pass123')">Volunteer (85% Trust)</button>
        <button class="role-tag-btn" onclick="RapidResQ.api.quickLogin('team1', 'pass123')">Response Team</button>
      </div>
    `;

    const navHtml = `
      ${demoBar}
      <nav class="navbar">
        <div class="nav-container">
          <a href="/index.html" class="nav-brand">
            <span class="brand-badge">🛡️</span>
            <span style="display:flex; flex-direction:column; line-height:1.15;">
              <span style="font-weight:800; letter-spacing:-0.01em;">RapidResQ</span>
              <span style="font-size:0.6rem; font-weight:600; color:var(--text-muted); letter-spacing:0.08em; text-transform:uppercase;">Rapid Disaster Response System</span>
            </span>
            <span style="font-size:0.65rem; font-weight:700; color:var(--accent-red); background:var(--accent-red-soft); padding:2px 8px; border-radius:999px; letter-spacing:0.06em;">BD</span>
          </a>

          <ul class="nav-links">
            <li><a href="/index.html" class="nav-link ${activePage === 'home' ? 'active' : ''}">Overview</a></li>
            <li><a href="/dashboard.html" class="nav-link ${activePage === 'dashboard' ? 'active' : ''}">Live Operations</a></li>
            <li><a href="/report-incident.html" class="nav-link ${activePage === 'report' ? 'active' : ''}" style="color:var(--accent-red); font-weight:700;">🚨 Report Emergency</a></li>
            <li><a href="/teams.html" class="nav-link ${activePage === 'teams' ? 'active' : ''}">Response Units</a></li>
            ${userRole === 'VOLUNTEER' ? `<li><a href="/volunteer-portal.html" class="nav-link ${activePage === 'volunteer' ? 'active' : ''}" style="color:var(--emerald); font-weight:700;">📨 My Signals</a></li>` : ''}
            <li><a href="/donations.html" class="nav-link ${activePage === 'donations' ? 'active' : ''}">Relief Campaigns</a></li>
            ${userRole === 'ADMIN' ? `<li><a href="/admin.html" class="nav-link ${activePage === 'admin' ? 'active' : ''}" style="color:#7C3AED; font-weight:700;">⚙️ Command Center</a></li>` : ''}
            ${userRole === 'ADMIN' ? `<li><a href="/ops-console.html" class="nav-link ${activePage === 'ops' ? 'active' : ''}" style="color:#0E7490; font-weight:700;">🛰️ Team Ops</a></li>` : ''}
            ${userRole === 'RESPONSE_TEAM' ? `<li><a href="/team-portal.html" class="nav-link ${activePage === 'team-portal' ? 'active' : ''}" style="color:#2563EB; font-weight:700;">🧑‍🤝‍🧑 Volunteer Program</a></li>` : ''}
            <li><a href="/mission-reports.html" class="nav-link ${activePage === 'reports' ? 'active' : ''}">Situation Reports</a></li>
          </ul>

          <div class="nav-actions">
            ${authSection}
          </div>
        </div>
      </nav>
    `;

    navPlaceholder.innerHTML = navHtml;
  }

  // -------------------------------------------------------------
  // Route Guards
  // -------------------------------------------------------------
  function requireAuth() {
    if (!isAuthenticated()) {
      showToast('Please login to access this section.', 'warning');
      setTimeout(() => {
        window.location.href = `/login.html?redirect=${encodeURIComponent(window.location.pathname)}`;
      }, 500);
      return false;
    }
    return true;
  }

  function requireAdmin() {
    if (!requireAuth()) return false;
    const user = getUser();
    if (!user || user.role !== 'ADMIN') {
      alert('Access Denied: Administrator role required.');
      window.location.href = '/dashboard.html';
      return false;
    }
    return true;
  }

  return {
    api,
    getToken,
    getUser,
    isAuthenticated,
    hasRole,
    toggleTheme,
    showToast,
    getDisasterIcon,
    getDisasterLabel,
    formatSeverityBadge,
    formatStatusBadge,
    formatCurrency,
    formatDate,
    renderNavbar,
    requireAuth,
    requireAdmin
  };
})();
