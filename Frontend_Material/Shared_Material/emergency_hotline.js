/*
 * CareConnect emergency hotline panel.
 * The /api/hotline integration is temporarily disabled.
 * Set HOTLINE to an approved emergency number when one is configured.
 */

let HOTLINE = '';

function sosBody() {
    const call = HOTLINE
        ? `<a href="tel:${HOTLINE}" class="btn r" style="display:block;text-align:center;text-decoration:none">Call ${HOTLINE} now</a>`
        : `<div class="small" style="background:#fbeeea;border-radius:10px;padding:10px;margin-top:8px">The UNILUS emergency line number will appear here once it is set.</div>`;

    return `
        <div style="background:var(--r);color:#fff;margin:-14px -14px 12px;padding:12px 14px;border-radius:14px 14px 0 0">
            <b style="font-size:15px">🚨 EMERGENCY HOTLINE</b>
        </div>
        <h3>Are you or someone else in immediate danger?</h3>
        <p class="small">
            Use this only for emergencies: thoughts of harming yourself or others,
            or a medical crisis. Call now, or go to the nearest safe person or
            campus security. You can also contact your local emergency services.
        </p>
        ${call}
        <p class="small" style="margin:10px 0">
            Not an emergency? Close this and book a counsellor from the Support tab.
        </p>
    `;
}

function openEmergencyHotline() {
    // Use an existing CareConnect modal if one is available.
    if (typeof window.openModal === 'function') {
        window.openModal(sosBody());
        return;
    }

    // Otherwise, create a simple standalone overlay.
    let overlay = document.getElementById('careconnect-emergency-overlay');

    if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'careconnect-emergency-overlay';

        Object.assign(overlay.style, {
            position: 'fixed',
            inset: '0',
            background: 'rgba(0,0,0,0.6)',
            zIndex: '99999',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '20px'
        });

        overlay.innerHTML = `
            <div style="background:white;color:#222;border-radius:14px;padding:20px;max-width:440px;width:100%;box-sizing:border-box;position:relative">
                <button id="careconnect-emergency-close" type="button" aria-label="Close emergency panel" style="position:absolute;right:12px;top:12px;border:0;background:transparent;font-size:22px;cursor:pointer">&times;</button>
                <div id="careconnect-emergency-content"></div>
            </div>
        `;

        document.body.appendChild(overlay);

        overlay.addEventListener('click', function (event) {
            if (event.target === overlay) {
                overlay.remove();
            }
        });

        overlay.querySelector('#careconnect-emergency-close').addEventListener('click', function () {
            overlay.remove();
        });
    }

    overlay.querySelector('#careconnect-emergency-content').innerHTML = sosBody();
    overlay.style.display = 'flex';
}

// Connect the existing emergency hotline button, if present.
document.addEventListener('DOMContentLoaded', function () {
    const button = document.getElementById('sos');

    if (button && !button.dataset.emergencyHotlineBound) {
        button.dataset.emergencyHotlineBound = 'true';

        button.addEventListener('click', function (event) {
            event.preventDefault();
            openEmergencyHotline();
        });
    }
});