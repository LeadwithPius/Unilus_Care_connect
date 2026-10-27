/* core.js: loaded by EVERY page. Saved demo state, the phone frame, popups, navigation. */
const $=id=>document.getElementById(id);
const KEY='careconnect_demo_v1';   // demo data lives in this browser only (localStorage)
const S0=()=>({role:'',booked:false,bookingStatus:'',supportRestart:false,followup:false,followupSession:null,sessionHistory:[],callCompleted:false,step:0,cat:'',mode:'Face-to-Face',why:'',cns:'',slot:'',consent:false,hist:[],frozen:false,cview:'dash',crisis:2,noshow:[],snake:null,cin:'',cout:'',transfers:0,re:false,calls:[],game:{},in:false,lr:'s',lm:'in',me:'',noClose:false,acc:{'m.lungu@unilus.edu.zm':{pw:'Temp1234',role:'c',codes:[],must:1},'e.sishemi@unilus.edu.zm':{pw:'Temp1234',role:'c',codes:[],must:1},'admin@unilus.edu.zm':{pw:'Admin1234',role:'a',codes:[],must:1}},counsellorAvailability:{'Mr. A. Lungu':'AVAILABLE','Ms. E. Sishemi':'AVAILABLE','Dr. C. Phiri':'AVAILABLE'},avail:'AVAILABLE',name:'',cs:[{n:'Mr. Lungu',e:'m.lungu@unilus.edu.zm',on:1},{n:'Ms. E. Sishemi',e:'e.sishemi@unilus.edu.zm',on:1}],flash:'',fac:'',yr:'',camp:''});
let S=S0();
try{const j=JSON.parse(localStorage.getItem(KEY));if(j)S=Object.assign(S0(),j)}catch(e){}
S.appointmentDate=S.appointmentDate||'';S.appointmentTime=S.appointmentTime||'';S.calendarMonth=S.calendarMonth||'';
S.sessionHistory=Array.isArray(S.sessionHistory)?S.sessionHistory:[];
if(S.counsellorAvailability&&S.counsellorAvailability['Dr. C. Phiri']==='AWAY')S.counsellorAvailability['Dr. C. Phiri']='AVAILABLE';
function save(){try{localStorage.setItem(KEY,JSON.stringify(S))}catch(e){}}
function resetDemo(){try{localStorage.removeItem(KEY)}catch(e){}location.reload()}

/* where each role lands after logging in (paths are relative to index.html) */
const ROUTES={s:'Frontend_Material/Students_Portal/Education_Hub.html',c:'Frontend_Material/Counsellors_Portal/Counsellors_FronPage.html',a:'Frontend_Material/Admin_Portal/Admin_Dashboard.html'};
/* student bottom tabs and counsellor menu (files sit in the same folder as the page using them) */
const PAGES={learn:'Education_Hub.html',home:'Home.html',support:'Support_Booking.html',me:'profile.html'};
const NAV=[['learn', '<img src="Vector_Images___Browse_156_929_409_Stock_Photos__Vectors__and_Video-removebg-preview.png" alt="Learn" class="nav-learn">','Learn'],['home','<img src="Home_Basic_black_and_white_app_icon-removebg-preview.png" class="nav-home" alt="Home">','Home'],['support','<img src="Vector_Chat_Icon__Chat_Icons__Comment_Icon__Typing_Icon_PNG_and_Vector_with_Transparent_Background_for_Free_Download-removebg-preview.png" alt="Support" class="nav-support">','Support'],['me','<img src="Profile-removebg-preview.png" alt="Me" class="nav-me">','Me']];
const CNAV=[['Counsellors_FronPage.html','📊 Dashboard & triage'],['Counsellors_Profile.html','🧑 My profile & socials'],['Student_Progress.html','📈 Student progress & stats'],['Counsellors_Calendar.html','📅 Calendar & Google sync']];
const STUDENT_NAV_HISTORY='careconnect_student_nav_history';
function readStudentNavHistory(){try{return JSON.parse(sessionStorage.getItem(STUDENT_NAV_HISTORY)||'[]')}catch(e){return []}}
function go(tab){
  if(PAGE&&PAGE.nav!==tab){
    const history=readStudentNavHistory();
    history.push(PAGE.nav);
    sessionStorage.setItem(STUDENT_NAV_HISTORY,JSON.stringify(history));
  }
  save();location.href=PAGES[tab];
}
function appBack(){
  if(PAGE&&PAGE.nav==='support'&&S.supportRestart){
    S.supportRestart=false;S.step=0;S.cat='';S.mode='Face-to-Face';S.why='';S.cns='';S.slot='';S.appointmentDate=S.appointmentTime='';S.calendarMonth='';S.consent=false;S.re=false;save();render();return;
  }
  if(PAGE&&PAGE.nav==='support'&&S.step>0){
    S.step--;
    render();
    return;
  }
  const history=readStudentNavHistory();
  const previous=history.pop()||'learn';
  sessionStorage.setItem(STUDENT_NAV_HISTORY,JSON.stringify(history));
  location.href=PAGES[previous];
}

function toast(m){const t=$('ts');t.textContent=m;t.style.display='block';clearTimeout(t._t);t._t=setTimeout(()=>t.style.display='none',3000)}
function modal(h){$('mdc').innerHTML=h;$('md').classList.add('on')}
function close(){$('md').classList.remove('on')}
window.close_=close;

const CC={
  cleanups:[],   // tool files register timers to stop when a popup is dismissed
  closeAll(){CC.cleanups.forEach(f=>f());close()},
  logout(){S.in=false;S.cview='dash';sessionStorage.removeItem(STUDENT_NAV_HISTORY);save();location.href=PAGE.root+'index.html'},
  mount(P){
    window.PAGE=P;
    if(!P.public&&(!S.in||S.role!==P.role)){location.replace(P.root+'index.html');return}
    const c=P.role==='c';
    const hideSos = c || P.role==='a';
    const R=$('app')||$('phone');   /* FIX: the page box is <div id="app"> (login) or <div id="phone"> (student/counsellor pages); the old code only knew "phone", so the login page crashed */
    if(P.public)document.body.classList.add('public');   /* the login page puts class "public" on <body>; styles.css uses it to hide the top bar and centre the form */
    const studentBack=P.role==='s'?`<button class="back-btn student-back" id="student-back" type="button" aria-label="Back" hidden><img src="${P.root}Vector_Back_Icon__Back_Icons__Arrow__Back_PNG_and_Vector_with_Transparent_Background_for_Free_Download-removebg-preview.png" alt="Back"></button>`:'';
    const topBar = P.public ? '' : `${studentBack}<div class="top">${c?'<button id="hb" style="background:none;font-size:22px;color:var(--g)" aria-label="Menu">☰</button>':''}</div>${hideSos?'':'<button class="btn-sos" id="sos" type="button"><img src="icons8-phone-50.png" alt="" class="sos-img"><span>Emergency hotline</span></button>'}`;
    R.innerHTML=`${topBar}
    <div class="body${P.role==='s'?` student-body student-body-${P.nav}`:''}" id="body"></div><div class="nav" id="nav"></div>
    ${c?'<div class="drawer" id="dr"><nav><h3 style="color:#fff;font-size:16px;margin-bottom:10px">Counsellor menu</h3><div id="drn"></div></nav><i id="dri"></i></div>':''}
    <div class="modal" id="md"><div class="card" id="mdc"></div></div><div class="toast" id="ts"></div>`;
    if(!P.public){
      $('sos').onclick=()=>modal(sosBody()+'<button class="btn o" onclick="close_()">Close</button>');
    }
    $('md').onclick=e=>{if(e.target.id==='md'&&!S.noClose)CC.closeAll()};
    if(P.role==='s'){
      $('nav').innerHTML=NAV.map(n=>`<button class="${P.nav===n[0]?'on':''}" onclick="go('${n[0]}')"><span>${n[1]}</span>${n[2]}</button>`).join('');
      const back=$('student-back');
      back.hidden=P.nav==='learn'||(readStudentNavHistory().length===0&&!(P.nav==='support'&&S.step>0));
      back.onclick=appBack;
    }
    else $('nav').style.display='none';
    if(c){
      $('drn').innerHTML=CNAV.map(a=>`<button data-href="${a[0]}">${a[1]}</button>`).join('')+'<button data-x="pw">🔑 Change password</button><button data-x="out">🚪 Log out</button>';
      $('hb').onclick=()=>$('dr').classList.add('on');$('dri').onclick=()=>$('dr').classList.remove('on');
      document.querySelectorAll('#drn button').forEach(b=>b.onclick=()=>{if(b.dataset.href){save();location.href=b.dataset.href}else{$('dr').classList.remove('on');b.dataset.x==='pw'?changePw():CC.logout()}});
    }
    render();
    if(S.flash){toast(S.flash);S.flash='';save()}
  }
};

/* every page defines view() (returns the screen's HTML), act(name) and optionally after() */
function render(){
  if(typeof stopSnake==='function')stopSnake();
  $('body').innerHTML=PAGE.view();
  document.querySelectorAll('[data-a]').forEach(b=>b.onclick=()=>{const a=b.dataset.a;if(a==='logout')CC.logout();else if(a==='chpw')changePw();else PAGE.act(a)});
  if(PAGE.after)PAGE.after();
  save();
}