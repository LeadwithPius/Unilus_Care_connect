/* install_app.js: "Download the app" button (Add to home screen). */
let dl=null;addEventListener('beforeinstallprompt',e=>{e.preventDefault();dl=e});
function installApp(){if(dl){dl.prompt();dl=null;return}modal(`<h3>Add CareConnect to your phone</h3><p class="small"><b>iPhone (Safari):</b> tap Share, then Add to Home Screen.<br><b>Android (Chrome):</b> tap the menu (three dots), then Install app or Add to Home screen.</p><button class="btn" onclick="close_()">Got it</button>`)}
