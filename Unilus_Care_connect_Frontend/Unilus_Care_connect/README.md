# UNILUS CareConnect: Frontend

Plain HTML, CSS and JavaScript. No build step, no libraries. Installable as a PWA.

## Folder layout
```
Unilus_Care_connect/
├── index.html                       Log in / create account / forgot password (all roles) + "Download the app"
├── manifest.json                    PWA install info
├── sw.js                            offline copy of pages you have opened (never caches /api)
├── icon-192.png, icon-512.png
└── Frontend_Material/
    ├── Students_Portal/
    │   ├── Education_Hub.html       first screen after login: topics, Breathe, Finch, Snake, 20-section game, videos
    │   ├── Home.html                welcome and shortcuts
    │   ├── Support_Booking.html     book (category, consent, mode, counsellor, time) and reschedule
    │   └── profile.html             upcoming session, check-in/out, masked call, progress, wellbeing check-in
    ├── Counsellors_Portal/
    │   ├── Counsellors_FronPage.html   dashboard: availability, emergency freeze, triage queue, attendance log
    │   ├── Counsellors_Calendar.html   month grid and Google Calendar sync
    │   ├── Counsellors_Profile.html    what students see (no phone numbers)
    │   └── Student_Progress.html       notes by student tag, anonymised trends (n >= 5)
    ├── Admin_Portal/
    │   └── Admin_Dashboard.html     counsellor accounts, password resets, anonymised stats
    └── Shared/                      code used by several pages
        ├── styles.css               all styling
        ├── core.js                  saved demo data, phone frame, popups, navigation, page loader
        ├── care_data.js             counsellors, time slots, topics, questions, availability badges
        ├── emergency_hotline.js     red Emergency hotline panel (number from GET /api/hotline)
        ├── install_app.js           "Download the app" button
        ├── password_tools.js        password rules, recovery codes, change password
        ├── wellbeing_checkin.js     5-question non-diagnostic check-in
        ├── wellness_tools.js        Breathe, Finch, Snake Xenzia
        ├── learning_game.js         20-section game, video list
        ├── masked_call.js           masked call screen with duration log
        └── api.js                   client for the Spring Boot backend (not used by the demo yet)
```

## Run it
Open `index.html` in a browser (in IntelliJ: right-click it, Open in, Browser) or serve the folder with any static web server. Use Safari or Chrome. Do not open files from a phone's Files preview: it does not run JavaScript.

## How a page works
Each page file is small: it loads `core.js` and the shared files it needs, defines `view()` (the screen's HTML) and `act(name)` (what the buttons do), then calls `CC.mount({...})`. `CC.mount` builds the phone frame, the top bar with the Emergency hotline button, the student tabs or counsellor menu, and sends anyone who isn't logged in as the right role back to `index.html`.

## Demo data
Everything is stored in this browser (`localStorage`, key `careconnect_demo_v1`) so the pages can share it. Passwords and recovery codes are stored **in plain text** here. That is for the demo only. Use "Reset demo data" on the login page to start again.

Demo logins: counsellor `m.lungu@unilus.edu.zm` / `Temp1234`, admin `admin@unilus.edu.zm` / `Admin1234` (both must set a new password at first login). Students create their own account.

## Connecting to the backend
Replace the demo logic with calls from `Shared/api.js` (login, register, book, check-in, triage, stats and so on). Remove the demo accounts and the plain-text password storage when you do. Set the real emergency number with the backend `HOTLINE` setting.
