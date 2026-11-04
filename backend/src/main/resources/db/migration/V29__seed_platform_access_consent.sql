-- Platform access guidelines shown to students after successful login.
-- Publish a new version rather than modifying an existing consent document.
INSERT INTO wellness.consent_documents
    (purpose_code, version_label, title, body)
VALUES
    (
        'PLATFORM_ACCESS',
        '1.0',
        'Welcome to UNILUS CareConnect',
        'Welcome to Unilus Care Connect. Our goal is to provide a seamless technology bridge between you and the University of Lusaka’s dedicated counseling professionals, making it easier to access the support you need.

Before you book your first session, please review and accept our platform guidelines:

- Your Care Providers: Care Connect is a scheduling and communication tool. All counseling, guidance, and mental health support are provided exclusively by the official UNILUS counseling staff.
- Our Role as a Facilitator: Your mental health journey is guided entirely by the university’s professionals. Because we strictly provide the software to connect you to these campus resources, the app developers and platform administrators do not oversee clinical practices and are not responsible for the nature, quality, or outcomes of the counseling services provided.
- Not for Emergencies: Care Connect is not an emergency response service and is not monitored 24/7 for crises. If you are experiencing an emergency, please contact emergency services or the UNILUS emergency helpline directly.
- Privacy: Your booking details are routed directly to the counseling department to ensure your privacy and confidentiality.

By clicking I Agree, you acknowledge that Care Connect operates solely as a booking facilitator and you consent to these terms.'
    )
ON CONFLICT (purpose_code, version_label) DO NOTHING;
