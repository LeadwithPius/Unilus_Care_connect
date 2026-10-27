/* emergency_hotline.js: the red "Emergency hotline" panel. BACKEND TEAM: the number comes from GET /api/hotline (HOTLINE setting). */
let HOTLINE='';
const hotlinePattern=/^[+\d ()-]{5,20}$/;
function setHotline(value){
  const cleaned=(value||'').trim();
  HOTLINE=hotlinePattern.test(cleaned)?cleaned:'';
}
try{
  fetch('/api/hotline')
    .then(r=>r.ok?r.json():Promise.reject(new Error('No hotline')))
    .then(d=>setHotline(d && d.hotline))
    .catch(()=>{});
}catch(e){}
function sosBody(){
  const call = HOTLINE
    ? `<a href="tel:${HOTLINE}" class="btn r" style="display:block;text-align:center;text-decoration:none;line-height:1.2"><img src="icons8-phone-50.png" alt="" class="sos-img" style="vertical-align:middle;margin-right:6px">Call ${HOTLINE} now</a>`
    : '<div class="small" style="background:#fbeeea;border-radius:10px;padding:10px;margin-top:9px">The UNILUS emergency line number will appear here once it is set.</div>';
  return `<h3>Need immediate help?</h3><p class="small">If you are in immediate danger, use this number to contact emergency support, or go to the nearest safe person or campus security.</p>${call}<p class="small" style="margin:10px 0"></p>`;
}
