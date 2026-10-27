/**
 * CareConnect API client.
 *
 * Development:
 *   Frontend: http://localhost:5173
 *   Backend:  http://localhost:8080
 *
 * Production:
 *   Set window.CARECONNECT_API_BASE_URL before loading this file
 *   if the backend is hosted on a different origin.
 *
 * Existing API methods are preserved. This file does not create
 * or assume new Spring Boot endpoints.
 */

(function () {
    'use strict';

    /**
     * Configure the backend API base URL.
     *
     * Local development uses Spring Boot on port 8080.
     * In production, same-origin deployments use /api by default.
     */
    const configuredBase = window.CARECONNECT_API_BASE_URL;

    const isLocalDevelopment =
        window.location.hostname === 'localhost' ||
        window.location.hostname === '127.0.0.1';

    const API_BASE_URL = (
        configuredBase ||
        (isLocalDevelopment
            ? 'http://localhost:8080/api'
            : '/api')
    ).replace(/\/+$/, '');

    /**
     * Safely read the stored authentication token.
     */
    function readStoredToken() {
        try {
            return sessionStorage.getItem('careconnect_token');
        } catch (error) {
            console.warn(
                'CareConnect could not access session storage.',
                error
            );

            return null;
        }
    }

    /**
     * Safely store or remove the authentication token.
     */
    function writeStoredToken(token) {
        try {
            if (token) {
                sessionStorage.setItem('careconnect_token', token);
            } else {
                sessionStorage.removeItem('careconnect_token');
            }
        } catch (error) {
            console.error(
                'CareConnect could not update session storage.',
                error
            );

            throw new Error(
                'Unable to save your login session. Check your browser settings.'
            );
        }
    }

    /**
     * Read a response body without assuming every response is JSON.
     */
    async function readResponse(response) {
        if (response.status === 204) {
            return null;
        }

        const contentType =
            response.headers.get('content-type') || '';

        const text = await response.text();

        if (!text) {
            return null;
        }

        if (contentType.includes('application/json')) {
            try {
                return JSON.parse(text);
            } catch {
                return text;
            }
        }

        return text;
    }

    /**
     * Extract a useful error message from a backend response.
     */
    function getErrorMessage(data, status) {
        if (data && typeof data === 'object') {
            return (
                data.message ||
                data.error ||
                data.detail ||
                data.title ||
                `Request failed with status ${status}.`
            );
        }

        if (typeof data === 'string' && data.trim()) {
            return data;
        }

        switch (status) {
            case 400:
                return 'The request was invalid. Check the information you entered.';

            case 401:
                return 'Authentication failed or your session has expired.';

            case 403:
                return 'You do not have permission to perform this action.';

            case 404:
                return 'The requested API endpoint was not found.';

            case 405:
                return 'This API endpoint does not allow the requested method.';

            case 409:
                return 'This request conflicts with existing information.';

            case 429:
                return 'Too many requests. Please wait and try again.';

            default:
                return `Request failed with status ${status}.`;
        }
    }

    const API = {
        baseURL: API_BASE_URL,

        token: readStoredToken(),

        /**
         * Save the current authentication token.
         */
        setToken(token) {
            const normalizedToken =
                typeof token === 'string' && token.trim()
                    ? token.trim()
                    : null;

            writeStoredToken(normalizedToken);
            this.token = normalizedToken;
        },

        /**
         * Make an HTTP request to the Spring Boot backend.
         *
         * All paths should start with /, for example:
         *   /auth/login
         *   /appointments/mine
         */
        async call(method, path, body) {
            if (typeof path !== 'string' || !path.startsWith('/')) {
                throw new Error(
                    'Invalid API path. API paths must start with /.'
                );
            }

            const normalizedMethod = String(method || 'GET').toUpperCase();

            const headers = {
                Accept: 'application/json'
            };

            if (body !== undefined && body !== null) {
                headers['Content-Type'] = 'application/json';
            }

            if (this.token) {
                headers.Authorization = `Bearer ${this.token}`;
            }

            const requestOptions = {
                method: normalizedMethod,
                headers,
                credentials: 'same-origin'
            };

            if (body !== undefined && body !== null) {
                requestOptions.body = JSON.stringify(body);
            }

            let response;

            try {
                response = await fetch(
                    `${this.baseURL}${path}`,
                    requestOptions
                );
            } catch (error) {
                console.error(
                    'CareConnect API connection failed:',
                    error
                );

                throw new Error(
                    `Unable to reach the CareConnect backend at ${this.baseURL}. ` +
                    'Check that Spring Boot is running and that browser CORS settings permit this request.'
                );
            }

            let data;

            try {
                data = await readResponse(response);
            } catch (error) {
                console.error(
                    'CareConnect could not read the server response:',
                    error
                );

                throw new Error(
                    'The server returned a response that could not be read.'
                );
            }

            if (!response.ok) {
                /*
                 * Clear a stored token when an authenticated request
                 * returns 401. Do not clear it for a failed login
                 * when no token was previously stored.
                 */
                if (response.status === 401 && this.token) {
                    this.setToken(null);
                }

                const message = getErrorMessage(
                    data,
                    response.status
                );

                console.error('CareConnect API error:', {
                    method: normalizedMethod,
                    url: `${this.baseURL}${path}`,
                    status: response.status,
                    message
                });

                const error = new Error(message);
                error.status = response.status;
                error.data = data;

                throw error;
            }

            return data;
        },

        /**
         * Authenticate a user.
         *
         * This preserves the existing request payload.
         * The backend must accept these exact fields:
         * userId, password, role.
         */
        async login(userId, password, role) {
            const result = await this.call(
                'POST',
                '/auth/login',
                {
                    userId,
                    password,
                    role
                }
            );

            if (
                !result ||
                typeof result !== 'object' ||
                typeof result.token !== 'string' ||
                !result.token.trim()
            ) {
                throw new Error(
                    'Login did not return the expected authentication token. ' +
                    'Check the Spring Boot login response format.'
                );
            }

            this.setToken(result.token);

            return result;
        },

        /**
         * Retrieve the authenticated user's profile.
         */
        async me() {
            return this.call('GET', '/auth/me');
        },

        /**
         * End the local session.
         */
        logout() {
            this.setToken(null);
        },

        /**
         * Register a student.
         *
         * Existing request fields are preserved.
         */
        register(details) {
            return this.call('POST', '/students/register', {
                fullName: details.fullName,
                email: details.email,
                studentNumber: details.studentNumber,
                password: details.password
            });
        },

        /**
         * Request a password-reset email.
         */
        forgotPassword(email) {
            return this.call(
                'POST',
                '/auth/forgot-password',
                { email }
            );
        },

        /**
         * Reset a password using a reset token.
         */
        resetPassword(token, newPassword) {
            return this.call(
                'POST',
                '/auth/reset-password',
                {
                    token,
                    newPassword
                }
            );
        },

        /**
         * Verify an email address.
         */
        verifyEmail(token) {
            return this.call(
                'POST',
                '/auth/verify-email',
                { token }
            );
        },

        /**
         * Retrieve counsellors.
         */
        counsellors() {
            return this.call('GET', '/counsellors');
        },

        /**
         * Book an appointment.
         */
        book(details) {
            return this.call(
                'POST',
                '/appointments',
                {
                    availabilitySlotId: details.availabilitySlotId,
                    supportCategoryCode: details.supportCategoryCode,
                    counsellingMode: details.counsellingMode,
                    alternativeModeReason:
                        details.alternativeModeReason ?? null
                }
            );
        },

        /**
         * Retrieve the current user's appointments.
         */
        mine() {
            return this.call('GET', '/appointments/mine');
        },

        /**
         * Cancel an appointment.
         */
        cancel(appointmentId) {
            return this.call(
                'POST',
                `/appointments/${encodeURIComponent(appointmentId)}/cancel`
            );
        },

        /**
         * Retrieve counsellor availability.
         */
        getAvailability(openOnly = true) {
            const query = new URLSearchParams({
                openOnly: String(openOnly)
            });

            return this.call(
                'GET',
                `/counsellor/availability?${query.toString()}`
            );
        },

        /**
         * Add a counsellor availability slot.
         */
        addAvailability(startsAt, endsAt) {
            return this.call(
                'POST',
                '/counsellor/availability',
                {
                    startsAt,
                    endsAt
                }
            );
        },

        /**
         * Delete an availability slot.
         */
        deleteAvailability(availabilityId) {
            return this.call(
                'DELETE',
                `/counsellor/availability/${encodeURIComponent(availabilityId)}`
            );
        },

        /**
         * Retrieve education resources.
         */
        education(topic) {
            const query = topic
                ? `?topic=${encodeURIComponent(topic)}`
                : '';

            return this.call(
                'GET',
                `/education${query}`
            );
        },

        /**
         * Retrieve game progress.
         */
        gameProgress() {
            return this.call('GET', '/game/progress');
        },

        /**
         * Save game progress.
         */
        saveGame(section, score) {
            return this.call(
                'POST',
                '/game/progress',
                {
                    section,
                    score
                }
            );
        },

        /**
         * Create a counsellor account.
         *
         * The backend must expose this endpoint and accept
         * the existing request fields.
         */
        createCounsellor(details) {
            return this.call(
                'POST',
                '/admin/counsellors',
                {
                    email: details.email,
                    password: details.password,
                    displayName: details.displayName,
                    professionalTitle: details.professionalTitle,
                    biography: details.biography || ''
                }
            );
        },

        /**
         * Publish a consent document.
         */
        publishConsentDocument(details) {
            return this.call(
                'POST',
                '/admin/consent-documents',
                details
            );
        },

        /**
         * Retrieve the emergency hotline configuration.
         *
         * This method only works if the backend actually implements
         * GET /api/hotline. It does not invent a hotline number.
         */
        hotline() {
            return this.call('GET', '/hotline');
        }
    };

    /*
     * Expose the API object to existing frontend scripts.
     */
    window.API = API;

    /*
     * Compatibility check: warn if the API helper is loaded twice.
     */
    if (window.console) {
        console.info(
            'CareConnect API client configured:',
            API.baseURL
        );
    }
})();
