// Thin client for the CareConnect REST API. The screens in index.html call these instead of changing local state.
const API = {
  token: null,
  async call(method, path, body) {
    const r = await fetch('/api' + path, { method,
      headers: { 'Content-Type': 'application/json', ...(this.token ? { Authorization: 'Bearer ' + this.token } : {}) },
      body: body ? JSON.stringify(body) : undefined });
    const type = r.headers.get('content-type') || '';
    const data = type.includes('json') ? await r.json() : await r.text();
    if (!r.ok) throw new Error((data && data.error) || 'Request failed');
    return data;
  },
  async login(userId, password, role) {   // may return {mustChangePassword, changeToken}: then call changePassword with that token
    const d = await this.call('POST', '/auth/login', { userId, password, role });
    this.token = d.token || d.changeToken; return d; },
  async register(b) { const d = await this.call('POST', '/auth/register', b); this.token = d.token; return d; },   // b: {userId, password, faculty, studyYear, campus}; show d.recoveryCodes ONCE
  forgotReset: (userId, recoveryCode, newPassword) => API.call('POST', '/auth/forgot/reset', { userId, recoveryCode, newPassword }),
  async changePassword(currentPassword, newPassword) { const d = await this.call('POST', '/auth/change-password', { currentPassword, newPassword }); this.token = d.token; return d; },
  newRecoveryCodes: (currentPassword) => API.call('POST', '/auth/recovery-codes', { currentPassword }),
  adminResetPassword: (userId) => API.call('POST', '/admin/accounts/reset-password', { userId }),
  hotline: () => API.call('GET', '/hotline'),
  consent: () => API.call('POST', '/consents', { type: 'DATA_HANDLING' }),
  counsellors: () => API.call('GET', '/counsellors'),
  book: (b) => API.call('POST', '/appointments', b),      // {counsellorId, category, mode, justification, slotTime}
  mine: () => API.call('GET', '/appointments/mine'),
  reschedule: (id, slotTime) => API.call('PUT', `/appointments/${id}/reschedule`, { slotTime }),
  cancel: (id) => API.call('DELETE', `/appointments/${id}`),
  checkIn: (id) => API.call('POST', `/appointments/${id}/check-in`),
  checkOut: (id) => API.call('POST', `/appointments/${id}/check-out`),
  wellbeing: (answers) => API.call('POST', '/wellbeing', { answers }),
  education: (topic) => API.call('GET', '/education' + (topic ? '?topic=' + encodeURIComponent(topic) : '')),
  gameProgress: () => API.call('GET', '/game/progress'),
  saveGame: (section, score) => API.call('POST', '/game/progress', { section, score }),
  startCall: (apptId) => API.call('POST', `/calls/${apptId}/start`),
  endCall: (callId) => API.call('POST', `/calls/${callId}/end`),
  setAvailability: (available) => API.call('PUT', '/counsellor/availability?available=' + available),
  triage: () => API.call('GET', '/counsellor/triage'),
  emergency: () => API.call('POST', '/counsellor/emergency'),
  saveNotes: (id, text, framework) => API.call('POST', `/counsellor/appointments/${id}/notes`, { text, framework }),
  readNotes: (id) => API.call('GET', `/counsellor/appointments/${id}/notes`),
  stats: (q = '') => API.call('GET', '/admin/stats' + q),
  reportCsv: (q = '') => API.call('GET', '/admin/report.csv' + q),
  accounts: () => API.call('GET', '/admin/counsellors'),
  addCounsellor: (email, fullName, specialisation) => API.call('POST', '/admin/counsellors', { email, fullName, specialisation }),
  setCounsellorActive: (id, active) => API.call('PUT', `/admin/counsellors/${id}/active?active=${active}`),
};
