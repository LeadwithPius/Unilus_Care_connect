const localHost = ['localhost', '127.0.0.1'].includes(
  window.location.hostname
)

const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL ||
  (localHost ? 'http://localhost:8080/api' : '/api')
).replace(/\/+$/, '')

const TOKEN_KEY = 'careconnect_token'

function getStoredToken() {
  try {
    return sessionStorage.getItem(TOKEN_KEY)
  } catch {
    return null
  }
}

function saveToken(token) {
  try {
    if (token) {
      sessionStorage.setItem(TOKEN_KEY, token)
    } else {
      sessionStorage.removeItem(TOKEN_KEY)
    }
  } catch {
    throw new Error(
      'Your browser could not save the login session. Enable session storage and try again.'
    )
  }
}

async function request(method, path, body) {
  if (typeof path !== 'string' || !path.startsWith('/')) {
    throw new Error('API paths must start with /.')
  }

  const headers = {
    Accept: 'application/json',
  }

  if (body !== undefined && body !== null) {
    headers['Content-Type'] = 'application/json'
  }

  const token = getStoredToken()

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  let response

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method: method.toUpperCase(),
      headers,
      body:
        body === undefined || body === null
          ? undefined
          : JSON.stringify(body),
      credentials: 'omit',
    })
  } catch (error) {
    console.error('CareConnect backend connection failed:', error)

    throw new Error(
      `Cannot reach the CareConnect backend at ${API_BASE_URL}. Check that Spring Boot is running.`
    )
  }

  const responseText =
    response.status === 204 ? '' : await response.text()

  let data = null

  if (responseText) {
    try {
      data = JSON.parse(responseText)
    } catch {
      data = responseText
    }
  }

  if (!response.ok) {
    if (response.status === 401 && token) {
      saveToken(null)
    }

    const message =
      data && typeof data === 'object'
        ? data.message || data.error || data.detail || data.title
        : typeof data === 'string'
          ? data
          : null

    const error = new Error(
      message || `Request failed with HTTP ${response.status}.`
    )

    error.status = response.status
    error.data = data

    throw error
  }

  return data
}

export const api = {
  baseURL: API_BASE_URL,

  get token() {
    return getStoredToken()
  },

  setToken(token) {
    const cleanToken =
      typeof token === 'string' && token.trim()
        ? token.trim()
        : null

    saveToken(cleanToken)
  },

  logout() {
    saveToken(null)
  },

  call: request,

  async login(userId, password, role) {
    const result = await request('POST', '/auth/login', {
      userId,
      password,
      role,
    })

    if (
      !result ||
      typeof result.token !== 'string' ||
      !result.token.trim()
    ) {
      throw new Error(
        'The backend did not return an authentication token.'
      )
    }

    this.setToken(result.token)

    return result
  },

  me() {
    return request('GET', '/auth/me')
  },

  register(details) {
    return request('POST', '/students/register', {
      fullName: details.fullName,
      email: details.email,
      studentNumber: details.studentNumber,
      password: details.password,
    })
  },

  forgotPassword(email) {
    return request('POST', '/auth/forgot-password', { email })
  },

  resetPassword(token, newPassword) {
    return request('POST', '/auth/reset-password', {
      token,
      newPassword,
    })
  },

  verifyEmail(token) {
    return request('POST', '/auth/verify-email', { token })
  },

  createCounsellor(details) {
    return request('POST', '/admin/counsellors', {
      email: details.email,
      password: details.password,
      displayName: details.displayName,
      professionalTitle: details.professionalTitle || '',
      biography: details.biography || '',
    })
  },

  adminCounsellors() {
    return request('GET', '/admin/counsellors')
  },

  counsellors() {
    return request('GET', '/counsellors')
  },

  education(topic, search) {
    const params = new URLSearchParams()

    if (topic) params.set('topic', topic)
    if (search) params.set('q', search)

    const query = params.toString()

    return request(
      'GET',
      `/education${query ? `?${query}` : ''}`
    )
  },

  studentProfile() {
    return request('GET', '/students/me')
  },

  appointmentCategories() {
    return request('GET', '/appointments/categories')
  },

  platformConsentStatus() {
    return request('GET', '/consents/status?purposeCode=PLATFORM_ACCESS')
  },

  currentCounsellingConsent() {
    return request('GET', '/consents/current?purposeCode=COUNSELLING')
  },

  recordConsent(consentDocumentId, decision) {
    return request('POST', '/consents', { consentDocumentId, decision })
  },

  bookAppointment(details) {
    return request('POST', '/appointments', details)
  },

  myAppointments() {
    return request('GET', '/appointments/mine')
  },

  cancelAppointment(appointmentId) {
    return request('POST', `/appointments/${appointmentId}/cancel`)
  },

  counsellorProfile() {
    return request('GET', '/counsellor/profile')
  },

  setAcceptingAppointments(acceptingAppointments) {
    return request('PATCH', '/counsellor/profile/accepting-appointments', {
      acceptingAppointments,
    })
  },

  counsellorAppointments(status) {
    const query = status ? `?status=${encodeURIComponent(status)}` : ''
    return request('GET', `/counsellor/appointments${query}`)
  },

  decideCounsellorAppointment(appointmentId, decision) {
    return request('POST', `/counsellor/appointments/${appointmentId}/decision`, {
      decision,
    })
  },

  updateCounsellorAttendance(appointmentId, action) {
    return request('POST', `/counsellor/appointments/${appointmentId}/attendance`, {
      action,
    })
  },

  counsellorAvailability(openOnly = false) {
    return request('GET', `/counsellor/availability?openOnly=${openOnly}`)
  },

  addCounsellorAvailability(startsAt, endsAt) {
    return request('POST', '/counsellor/availability', { startsAt, endsAt })
  },

  deleteCounsellorAvailability(availabilityId) {
    return request('DELETE', `/counsellor/availability/${availabilityId}`)
  },

  wellbeingQuestionnaire() {
    return request('GET', '/triage/questionnaire')
  },

  submitWellbeingCheckIn(questionnaireVersion, responses) {
    return request('POST', '/triage/check-ins', {
      questionnaireVersion,
      responses,
    })
  },

  myWellbeingCheckIns() {
    return request('GET', '/triage/check-ins/mine')
  },

  submitTriageAssessment(details) {
    return request('POST', '/triage/assessments', details)
  },

  myTriageAssessments() {
    return request('GET', '/triage/assessments/mine')
  },

  gameProgress() {
    return request('GET', '/game/progress')
  },

  saveGame(section, score) {
    return request('POST', '/game/progress', {
      section,
      score,
    })
  },
}

console.info('CareConnect API configured:', API_BASE_URL)
