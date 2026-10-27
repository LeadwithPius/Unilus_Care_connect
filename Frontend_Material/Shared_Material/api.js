/**
 * CareConnect API client.
 * Matches the current Spring Boot backend endpoints.
 */
const API = {
    token: sessionStorage.getItem('careconnect_token'),

    setToken(token) {
        this.token = token || null;

        if (this.token) {
            sessionStorage.setItem('careconnect_token', this.token);
        } else {
            sessionStorage.removeItem('careconnect_token');
        }
    },

    async call(method, path, body) {
        const headers = {
            Accept: 'application/json'
        };

        if (body !== undefined && body !== null) {
            headers['Content-Type'] = 'application/json';
        }

        if (this.token) {
            headers.Authorization = `Bearer ${this.token}`;
        }

        let response;

        try {
            response = await fetch(`/api${path}`, {
                method,
                headers,
                body: body !== undefined && body !== null
                    ? JSON.stringify(body)
                    : undefined
            });
        } catch {
            throw new Error(
                'Unable to connect to CareConnect. Check that the backend is running.'
            );
        }

        const contentType = response.headers.get('content-type') || '';
        let data;

        if (contentType.includes('application/json')) {
            data = await response.json();
        } else {
            data = await response.text();
        }

        if (!response.ok) {
            const message =
                data && typeof data === 'object'
                    ? data.error || data.message
                    : data;

            if (response.status === 401 && this.token) {
                this.setToken(null);
            }

            throw new Error(
                message || `Request failed with status ${response.status}.`
            );
        }

        return data;
    },

    async login(userId, password, role) {
        const result = await this.call('POST', '/auth/login', {
            userId,
            password,
            role
        });

        if (!result.token) {
            throw new Error('The server did not return an authentication token.');
        }

        this.setToken(result.token);
        return result;
    },

    async me() {
        return this.call('GET', '/auth/me');
    },

    logout() {
        this.setToken(null);
    },

    register(details) {
        return this.call('POST', '/students/register', {
            fullName: details.fullName,
            email: details.email,
            studentNumber: details.studentNumber,
            password: details.password
        });
    },

    forgotPassword(email) {
        return this.call('POST', '/auth/forgot-password', { email });
    },

    resetPassword(token, newPassword) {
        return this.call('POST', '/auth/reset-password', {
            token,
            newPassword
        });
    },

    verifyEmail(token) {
        return this.call('POST', '/auth/verify-email', { token });
    },

    counsellors() {
        return this.call('GET', '/counsellors');
    },

    book(details) {
        return this.call('POST', '/appointments', {
            availabilitySlotId: details.availabilitySlotId,
            supportCategoryCode: details.supportCategoryCode,
            counsellingMode: details.counsellingMode,
            alternativeModeReason: details.alternativeModeReason ?? null
        });
    },

    mine() {
        return this.call('GET', '/appointments/mine');
    },

    cancel(appointmentId) {
        return this.call(
            'POST',
            `/appointments/${encodeURIComponent(appointmentId)}/cancel`
        );
    },

    getAvailability(openOnly = true) {
        return this.call(
            'GET',
            `/counsellor/availability?openOnly=${openOnly}`
        );
    },

    addAvailability(startsAt, endsAt) {
        return this.call('POST', '/counsellor/availability', {
            startsAt,
            endsAt
        });
    },

    deleteAvailability(availabilityId) {
        return this.call(
            'DELETE',
            `/counsellor/availability/${encodeURIComponent(availabilityId)}`
        );
    },

    education(topic) {
        const query = topic
            ? `?topic=${encodeURIComponent(topic)}`
            : '';

        return this.call('GET', `/education${query}`);
    },

    gameProgress() {
        return this.call('GET', '/game/progress');
    },

    saveGame(section, score) {
        return this.call('POST', '/game/progress', { section, score });
    },

    createCounsellor(details) {
        return this.call('POST', '/admin/counsellors', {
            email: details.email,
            password: details.password,
            displayName: details.displayName,
            professionalTitle: details.professionalTitle,
            biography: details.biography || ''
        });
    },

    publishConsentDocument(details) {
        return this.call('POST', '/admin/consent-documents', details);
    }
};
