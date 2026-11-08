-- Initial plain-language counselling disclosure. Publish a new version rather
-- than editing this record when the disclosure changes.
INSERT INTO wellness.consent_documents
    (purpose_code, version_label, title, body)
VALUES
    (
        'COUNSELLING',
        '1.0',
        'Counselling privacy and consent',
        'Before requesting counselling, please understand how your information is used. CareConnect stores your appointment and counselling-session records against your student account so that you can view your own appointments and the service can manage your booking. The counsellor assigned to an appointment sees a counsellor-specific student reference, the appointment time, counselling mode, and the support category. The counsellor-facing appointment screen does not display your name, student number, or email address. This is pseudonymous counselling, not absolute anonymity: CareConnect retains an internal link between your student account and your records. By accepting, you agree that the information described above may be used to arrange and provide the counselling appointment. Do not use CareConnect appointment booking for an emergency or immediate danger; seek urgent help from an appropriate emergency service or trusted university support contact.'
    )
ON CONFLICT (purpose_code, version_label) DO NOTHING;
