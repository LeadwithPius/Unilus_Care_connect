/* core.js: loaded by EVERY page. Saved demo state, the phone frame, popups, navigation. */
const $=id=>document.getElementById(id);
const KEY='careconnect_demo_v1';   // demo data lives in this browser only (localStorage)
const S0=()=>({role:'',booked:false,step:0,cat:'',mode:'Face-to-Face',why:'',cns:'',slot:'',consent:false,hist:[],frozen:false,cview:'dash',crisis:2,noshow:[],snake:null,cin:'',cout:'',transfers:0,re:false,calls:[],game:{},in:false,lr:'s',lm:'in',me:'',noClose:false,acc:{'m.lungu@unilus.edu.zm':{pw:'Temp1234',role:'c',codes:[],must:1},'e.sishemi@unilus.edu.zm':{pw:'Temp1234',role:'c',codes:[],must:1},'admin@unilus.edu.zm':{pw:'Admin1234',role:'a',codes:[],must:1}},avail:'AVAILABLE',name:'',cs:[{n:'Mr. Lungu',e:'m.lungu@unilus.edu.zm',on:1},{n:'Ms. Sishemi',e:'e.sishemi@unilus.edu.zm',on:1}],flash:'',fac:'',yr:'',camp:''});
let S=S0();
try{const j=JSON.parse(localStorage.getItem(KEY));if(j)S=Object.assign(S0(),j)}catch(e){}
function save(){try{localStorage.setItem(KEY,JSON.stringify(S))}catch(e){}}
function resetDemo(){try{localStorage.removeItem(KEY)}catch(e){}location.reload()}

/* where each role lands after logging in (paths are relative to index.html) */
const ROUTES={s:'Frontend_Material/Students_Portal/Education_Hub.html',c:'Frontend_Material/Counsellors_Portal/Counsellors_FronPage.html',a:'Frontend_Material/Admin_Portal/Admin_Dashboard.html'};
/* student bottom tabs and counsellor menu (files sit in the same folder as the page using them) */
const PAGES={learn:'Education_Hub.html',home:'Home.html',support:'Support_Booking.html',me:'profile.html'};
const NAV=[['learn','🎓','Learn'],['home','🏠','Home'],['support','💬','Support'],['me','👤','Me']];
const CNAV=[['Counsellors_FronPage.html','📊 Dashboard & triage'],['Counsellors_Profile.html','🧑 My profile & socials'],['Student_Progress.html','📈 Student progress & stats'],['Counsellors_Calendar.html','📅 Calendar & Google sync']];
function go(tab){save();location.href=PAGES[tab]}

function toast(m){const t=$('ts');t.textContent=m;t.style.display='block';clearTimeout(t._t);t._t=setTimeout(()=>t.style.display='none',3000)}
function modal(h){$('mdc').innerHTML=h;$('md').classList.add('on')}
function close(){$('md').classList.remove('on')}
window.close_=close;

const CC={
  cleanups:[],   // tool files register timers to stop when a popup is dismissed
  closeAll(){CC.cleanups.forEach(f=>f());close()},
  logout(){S.in=false;S.cview='dash';save();location.href=PAGE.root+'index.html'},
  mount(P){
    window.PAGE=P;
    if(!P.public&&(!S.in||S.role!==P.role)){location.replace(P.root+'index.html');return}
    const c=P.role==='c';
    $('phone').innerHTML=`<div class="top">${c?'<button id="hb" style="background:none;font-size:22px;color:var(--g)" aria-label="Menu">☰</button>':'<span>9:41</span>'}<b>${P.title}</b><button class="sos" id="sos">🚨 Emergency hotline</button></div>
    <div class="body" id="body"></div><div class="nav" id="nav"></div>
    ${c?'<div class="drawer" id="dr"><nav><h3 style="color:#fff;font-size:16px;margin-bottom:10px">Counsellor menu</h3><div id="drn"></div></nav><i id="dri"></i></div>':''}
    <div class="modal" id="md"><div class="card" id="mdc"></div></div><div class="toast" id="ts"></div>`;
    $('sos').onclick=()=>modal(sosBody()+'<button class="btn o" onclick="close_()">Close</button>');
    $('md').onclick=e=>{if(e.target.id==='md'&&!S.noClose)CC.closeAll()};
    if(P.role==='s')$('nav').innerHTML=NAV.map(n=>`<button class="${P.nav===n[0]?'on':''}" onclick="go('${n[0]}')"><span>${n[1]}</span>${n[2]}</button>`).join('');
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
