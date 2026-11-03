/* wellness_tools.js: Breathe, Finch, Snake Xenzia and Goal achiever. */
let bt;
function breathe(){
 modal(`<h3>Breathe</h3><div class="small">Breathe in as the circle grows, out as it shrinks.</div><div class="breathe" id="bc2">Breathe in</div><button class="btn" id="bx">Done</button>`);
 let big=false;const el=$('bc2');const tick=()=>{big=!big;el.style.transform=big?'scale(1.5)':'scale(1)';el.textContent=big?'Breathe in':'Breathe out'};
 setTimeout(tick,50);bt=setInterval(tick,4000);$('bx').onclick=()=>{clearInterval(bt);close()};
}
function finch(){
 const g=['Drink water','Write one journal line','Go outside for 5 minutes','Message a friend'];let n=0;
 modal(`<h3>Finch 🐣</h3><div id="pet" style="font-size:54px;text-align:center">🥚</div><div class="small" style="text-align:center;margin-bottom:6px" id="pm">Complete goals to help your pet grow.</div>${g.map(x=>`<label class="row"><span>${x}</span><input type="checkbox" class="gk"></label>`).join('')}<button class="btn" onclick="close_()">Close</button>`);
 document.querySelectorAll('.gk').forEach(c=>c.onchange=()=>{n=document.querySelectorAll('.gk:checked').length;$('pet').textContent=['🥚','🐣','🐥','🐤','🦜'][n];$('pm').textContent=n===4?'Your pet is thriving. So are you.':n+' of 4 goals done.'});
}
let sk,snakeLimit,snakeKeyHandler;
const SNAKE_COOLDOWN_MS=12*60*60*1000;
function stopSnake(){
 clearInterval(sk);sk=null;
 clearTimeout(snakeLimit);snakeLimit=null;
 if(snakeKeyHandler){window.removeEventListener('keydown',snakeKeyHandler);snakeKeyHandler=null}
}
function snake(){
 const remaining=S.snakeBlockedUntil-Date.now();
 if(remaining>0){
  const minutes=Math.ceil(remaining/60000),hours=Math.floor(minutes/60),minutesLeft=minutes%60;
  const timeLeft=hours?`${hours} hr${hours===1?'':'s'}${minutesLeft?` ${minutesLeft} min`:''}`:`${minutes} min`;
  modal(`<h3>Snake Xenzia is resting</h3><p class="small">You’ve reached today’s play limit. You can play again in ${timeLeft}.</p><button class="btn o" id="sx">Close</button>`);
  $('sx').onclick=close;
  return;
 }
 if(S.snakeBlockedUntil){S.snakeBlockedUntil=0;save()}
 modal(`<h3>Snake Xenzia 🐍</h3><canvas id="cv" width="200" height="200"></canvas><div class="small" id="sc">Use the arrows. Slow breaths.</div><div style="display:grid;grid-template-columns:repeat(3,1fr);gap:6px;margin-top:6px"><span></span><button class="pill" data-d="u">↑</button><span></span><button class="pill" data-d="l">←</button><button class="pill" data-d="d">↓</button><button class="pill" data-d="r">→</button></div><button class="btn o" id="sx">Close</button>`);
 stopSnake();
 const cx=$('cv').getContext('2d');let sn=[[5,5],[4,5],[3,5]],d=[1,0],f=[10,10],pts=0;
 const M={u:[0,-1],d:[0,1],l:[-1,0],r:[1,0]};
 document.querySelectorAll('[data-d]').forEach(b=>b.onclick=()=>{const n=M[b.dataset.d];if(n[0]!==-d[0]||n[1]!==-d[1])d=n});
 snakeKeyHandler=e=>{const k={ArrowUp:'u',ArrowDown:'d',ArrowLeft:'l',ArrowRight:'r'}[e.key];if(k){e.preventDefault();const n=M[k];if(n[0]!==-d[0]||n[1]!==-d[1])d=n}};
 window.addEventListener('keydown',snakeKeyHandler);
 sk=setInterval(()=>{const h=[(sn[0][0]+d[0]+20)%20,(sn[0][1]+d[1]+20)%20];
  if(sn.some(p=>p[0]===h[0]&&p[1]===h[1])){sn=[[5,5],[4,5],[3,5]];pts=0}
  sn.unshift(h);if(h[0]===f[0]&&h[1]===f[1]){pts++;f=[Math.random()*20|0,Math.random()*20|0]}else sn.pop();
  cx.fillStyle='#0e2b26';cx.fillRect(0,0,200,200);cx.fillStyle='#e0b33a';cx.fillRect(f[0]*10,f[1]*10,10,10);cx.fillStyle='#7fd1b9';sn.forEach(p=>cx.fillRect(p[0]*10,p[1]*10,9,9));$('sc').textContent='Score: '+pts},140);
 snakeLimit=setTimeout(()=>{stopSnake();S.snakeBlockedUntil=Date.now()+SNAKE_COOLDOWN_MS;save();modal(`<h3>Time's up</h3><p class="small">Your 5-minute Snake Xenzia session has ended. You can play again in 12 hours.</p><button class="btn o" id="sx">Close</button>`);$('sx').onclick=close},5*60*1000);
 $('sx').onclick=()=>{stopSnake();close()};
}
CC.cleanups.push(()=>{clearInterval(bt);stopSnake()});

/* Goal achiever: a dependency-free goal garden using CareConnect's shared modal. */
const GROWDAY_STORAGE_KEY='careconnect-growday-v1';
let growdayState=loadGrowday();

function loadGrowday(){
 try{
  const saved=JSON.parse(localStorage.getItem(GROWDAY_STORAGE_KEY));
  if(saved&&Array.isArray(saved.objectives)){
   return {
    goal:typeof saved.goal==='string'?saved.goal:'',
    flower:['sunflower','orchid','lily'].includes(saved.flower)?saved.flower:'sunflower',
    objectives:saved.objectives.filter(item=>item&&typeof item.title==='string').map(item=>({
     id:item.id||Date.now()+Math.random(),
     title:item.title,
     done:Boolean(item.done),
     xp:Number(item.xp)||25
    }))
   };
  }
 }catch(error){
  console.warn('Goal achiever progress could not be loaded.',error);
 }
 return {goal:'',flower:'sunflower',objectives:[]};
}

function saveGrowday(){
 try{
  localStorage.setItem(GROWDAY_STORAGE_KEY,JSON.stringify(growdayState));
 }catch(error){
  console.warn('Goal achiever progress could not be saved.',error);
 }
}

function growdayEscape(value){
 return String(value).replace(/[&<>"']/g,char=>({
  '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'
 })[char]);
}

function growdayProgress(){
 if(!growdayState.objectives.length)return 0;
 const done=growdayState.objectives.filter(item=>item.done).length;
 return Math.round(done/growdayState.objectives.length*100);
}

function growdayPlant(stage){
 const colors={
  sunflower:{petal:'#e0b33a',shadow:'#b28722',center:'#0e2b26'},
  orchid:{petal:'#7fd1b9',shadow:'#134f47',center:'#e0b33a'},
  lily:{petal:'#e8faf5',shadow:'#7fd1b9',center:'#e0b33a'}
 }[growdayState.flower];
 const stemTop=[118,112,101,87,70,55,47,42][stage];
 const petals=[0,45,90,135,180,225,270,315].map((angle,index)=>
  `<ellipse cx="90" cy="27" rx="10" ry="19" fill="${index%2?colors.shadow:colors.petal}" transform="rotate(${angle} 90 48)"/>`
 ).join('');
 const bloom=stage>=6?`<g transform="${stage===6?'translate(29 15) scale(.68)':''}">
  ${petals}<circle cx="90" cy="48" r="13" fill="${colors.center}"/><circle cx="86" cy="44" r="3" fill="rgba(255,255,255,.25)"/>
 </g>`:'';
 return `<div class="gd-plant" role="img" aria-label="${['Seed','Germinating','Sprout','Young shoot','Leafy shoot','Budding','Small flower','Full bloom'][stage]}">
  <svg viewBox="0 0 180 200" aria-hidden="true">
   ${stage===0?'<ellipse cx="90" cy="124" rx="8" ry="5" fill="#e0b33a"/>':''}
   ${stage>=1?`<rect x="86" y="${stemTop}" width="8" height="${128-stemTop}" fill="#134f47"/>`:''}
   ${stage>=2?'<path d="M87 109H72v-7H61v-7h19v7h7Z" fill="#7fd1b9"/>':''}
   ${stage>=4?'<path d="M93 88h13v-8h18v8h-9v8H93Z" fill="#134f47"/>':''}
   ${stage===5?`<path d="M80 54h20l5 9-5 10H80l-5-10Z" fill="${colors.shadow}"/>`:''}
   ${bloom}
   <rect x="55" y="126" width="70" height="15" rx="2" fill="#134f47"/>
   <path d="M63 141h54l-9 43H72Z" fill="#7fd1b9"/>
   <path d="M101 141h16l-9 43H98Z" fill="#4c9f8a"/>
   <rect x="56" y="119" width="68" height="10" fill="#0e2b26"/>
  </svg>
  <span>${['Seed','Germinating','Sprout','Young shoot','Leafy shoot','Budding','Small flower','Full bloom'][stage]}</span>
 </div>`;
}

function growdayStyles(){
 if(document.getElementById('growdayStyles'))return;
 const style=document.createElement('style');
 style.id='growdayStyles';
 style.textContent=`
  #growdayApp{color:#0e2b26;font-family:Inter,sans-serif}
  #growdayApp *{box-sizing:border-box}
  .gd-kicker{display:block;color:#48766f;font-size:9px;font-weight:700;letter-spacing:.13em}
  .gd-setup{display:grid;gap:13px;margin:15px 0;padding:14px;border:1px solid rgba(19,79,71,.2);border-radius:12px;background:rgba(127,209,185,.1)}
  .gd-field{display:grid;gap:6px}.gd-field label,.gd-picker legend{color:#134f47;font-size:9px;font-weight:700;letter-spacing:.11em}
  .gd-field input,.gd-add input{width:100%;padding:10px 11px;border:1px solid rgba(19,79,71,.28);border-radius:9px;background:#fff;color:#0e2b26;font:12px Inter,sans-serif;outline:none}
  .gd-field input:focus,.gd-add input:focus{border-color:#134f47;box-shadow:0 0 0 3px rgba(127,209,185,.3)}
  .gd-picker{margin:0;padding:0;border:0}.gd-picker legend{margin-bottom:6px}.gd-picker div{display:grid;grid-template-columns:repeat(3,1fr);gap:5px}
  .gd-flower{padding:8px 5px;border:1px solid rgba(19,79,71,.25);border-radius:8px;background:#fff;color:#134f47;font:600 10px Inter,sans-serif;cursor:pointer}
  .gd-flower.active{border-color:#134f47;background:#134f47;color:#fff;box-shadow:inset 0 -3px #e0b33a}
  .gd-card{overflow:hidden;border:1px solid rgba(19,79,71,.2);border-radius:14px;background:#fff}
  .gd-main{padding:16px}.gd-title-row{display:flex;justify-content:space-between;gap:10px}.gd-title-row h3{margin:5px 0 0;color:#0e2b26;font:400 20px Gloock,serif}.gd-streak{align-self:start;padding:5px 7px;border-radius:6px;background:rgba(224,179,58,.18);color:#765d13;font-size:8px;font-weight:700}
  .gd-progress-meta{display:flex;justify-content:space-between;margin:18px 0 7px;color:#48766f;font-size:10px}.gd-progress{height:7px;overflow:hidden;border-radius:6px;background:rgba(127,209,185,.24)}.gd-progress i{display:block;height:100%;border-radius:inherit;background:#e0b33a;transition:width .3s ease}
  .gd-list{display:grid;gap:7px;margin-top:16px}.gd-empty{padding:20px 14px;border:1px dashed rgba(19,79,71,.35);border-radius:10px;background:rgba(127,209,185,.08);text-align:center}.gd-empty strong{display:block;color:#134f47;font-size:12px}.gd-empty span{display:block;margin-top:4px;color:#48766f;font-size:10px;line-height:1.45}
  .gd-objective{display:grid;grid-template-columns:auto 1fr auto auto;align-items:center;gap:8px;padding:10px;border:1px solid rgba(19,79,71,.16);border-radius:10px;background:#fff}.gd-objective.done{border-color:#7fd1b9;background:rgba(127,209,185,.1)}.gd-check{display:grid;place-items:center;width:24px;height:24px;border:2px solid #7fd1b9;border-radius:7px;background:#fff;color:#fff;cursor:pointer}.gd-objective.done .gd-check{border-color:#134f47;background:#134f47}.gd-objective strong{font-size:11px}.gd-objective.done strong{text-decoration:line-through;color:#48766f}.gd-xp{padding:4px 6px;border-radius:5px;background:rgba(224,179,58,.2);color:#765d13;font-size:8px;font-weight:700}.gd-remove{border:0;background:transparent;color:#48766f;font-size:17px;cursor:pointer}
  .gd-add{display:grid;grid-template-columns:1fr auto;gap:7px;margin-top:9px}.gd-add button{padding:0 15px;border:0;border-radius:8px;background:#134f47;color:#fff;font:700 11px Inter,sans-serif;cursor:pointer}
  .gd-garden{display:flex;flex-direction:column;padding:15px;background:#0e2b26}.gd-garden-head{display:flex;justify-content:space-between;color:#7fd1b9;font-size:8px;font-weight:700;letter-spacing:.12em}.gd-plant{position:relative;display:grid;place-items:center;min-height:190px}.gd-plant svg{width:min(180px,80%);filter:drop-shadow(0 6px 0 rgba(0,0,0,.16))}.gd-plant>span{position:absolute;bottom:5px;padding:4px 7px;border-radius:5px;background:rgba(127,209,185,.15);color:#7fd1b9;font-size:8px;font-weight:700;text-transform:uppercase}.gd-message{padding:10px;border:1px solid rgba(127,209,185,.2);border-radius:9px;background:rgba(127,209,185,.08);text-align:center}.gd-message strong{display:block;color:#e0b33a;font:400 13px Gloock,serif}.gd-message span{display:block;margin-top:3px;color:#b8ddd3;font-size:9px;line-height:1.4}
  .gd-modal-close{width:100%;margin-top:12px}
  @media(max-width:380px){.gd-streak,.gd-xp{display:none}.gd-objective{grid-template-columns:auto 1fr auto}}
  @media(prefers-reduced-motion:reduce){#growdayApp *{transition:none!important}}
 `;
 document.head.appendChild(style);
}

function renderGrowday(){
 const root=document.getElementById('growdayApp');
 if(!root)return;
 const total=growdayState.objectives.length;
 const done=growdayState.objectives.filter(item=>item.done).length;
 const progress=growdayProgress();
 const stage=progress===0?0:Math.min(7,Math.ceil(progress/14.3));
 const allDone=total>0&&done===total;
 root.innerHTML=`
  <section class="gd-setup">
   <div class="gd-field"><label for="growdayGoal">YOUR GOAL</label><input id="growdayGoal" maxlength="100" value="${growdayEscape(growdayState.goal)}" placeholder="What do you want to achieve?"></div>
   <fieldset class="gd-picker"><legend>CHOOSE YOUR FLOWER</legend><div>
    ${[['sunflower','Sunflower'],['orchid','Orchid'],['lily','Lily']].map(([id,label])=>`<button class="gd-flower ${growdayState.flower===id?'active':''}" type="button" data-growday-flower="${id}" aria-pressed="${growdayState.flower===id}">${label}</button>`).join('')}
   </div></fieldset>
  </section>
  <section class="gd-card">
   <div class="gd-main">
    <div class="gd-title-row"><div><span class="gd-kicker">TODAY'S FOCUS</span><h3>${growdayEscape(growdayState.goal||'My new goal')}</h3></div><span class="gd-streak">ONE STEP AT A TIME</span></div>
    <div class="gd-progress-meta"><span>${total?`${done} of ${total} objectives`:'No objectives yet'}</span><strong>${progress}%</strong></div>
    <div class="gd-progress"><i style="width:${progress}%"></i></div>
    <div class="gd-list">
     ${total?growdayState.objectives.map(item=>`<div class="gd-objective ${item.done?'done':''}">
      <button class="gd-check" type="button" data-growday-toggle="${item.id}" aria-label="${item.done?'Mark incomplete':'Mark complete'}">${item.done?'✓':''}</button>
      <strong>${growdayEscape(item.title)}</strong><span class="gd-xp">+${item.xp} XP</span>
      <button class="gd-remove" type="button" data-growday-remove="${item.id}" aria-label="Remove ${growdayEscape(item.title)}">×</button>
     </div>`).join(''):'<div class="gd-empty"><strong>Plant your first objective</strong><span>Add a small, achievable step below to begin growing your plant.</span></div>'}
    </div>
    <form class="gd-add" id="growdayAdd"><input id="growdayNewObjective" maxlength="100" aria-label="New objective" placeholder="${total?'Add another objective...':'Enter your first objective...'}"><button type="submit">Add</button></form>
   </div>
   <aside class="gd-garden">
    <div class="gd-garden-head"><span>YOUR PLANT</span><span>${stage+1}/8</span></div>
    ${growdayPlant(stage)}
    <div class="gd-message"><strong>${!total?'Ready to grow':allDone?'Goal complete!':'Keep it growing'}</strong><span>${!total?'Add your first objective to plant the seed.':allDone?'Your hard work is in full bloom.':'Complete an objective to help your plant grow.'}</span></div>
   </aside>
  </section>
  <button class="btn o gd-modal-close" id="goalAchieverClose" type="button">Close</button>`;
 document.getElementById('growdayGoal').oninput=event=>{
  growdayState.goal=event.target.value;saveGrowday();
  root.querySelector('.gd-title-row h3').textContent=growdayState.goal||'My new goal';
 };
 root.querySelectorAll('[data-growday-flower]').forEach(button=>button.onclick=()=>{
  growdayState.flower=button.dataset.growdayFlower;saveGrowday();renderGrowday();
 });
 root.querySelectorAll('[data-growday-toggle]').forEach(button=>button.onclick=()=>{
  const item=growdayState.objectives.find(objective=>String(objective.id)===button.dataset.growdayToggle);
  if(item)item.done=!item.done;saveGrowday();renderGrowday();
 });
 root.querySelectorAll('[data-growday-remove]').forEach(button=>button.onclick=()=>{
  growdayState.objectives=growdayState.objectives.filter(objective=>String(objective.id)!==button.dataset.growdayRemove);
  saveGrowday();renderGrowday();
 });
 document.getElementById('growdayAdd').onsubmit=event=>{
  event.preventDefault();
  const input=document.getElementById('growdayNewObjective');
  const title=input.value.trim();
  if(!title)return;
  growdayState.objectives.push({id:Date.now(),title,done:false,xp:25});
  saveGrowday();renderGrowday();
  document.getElementById('growdayNewObjective').focus();
 };
 document.getElementById('goalAchieverClose').onclick=close_;
}

function goalAchiever(){
 growdayStyles();
 modal(`<h3 style="font-family:'Gloock',serif">Goal achiever</h3><div class="small">Small steps grow into something meaningful.</div><div id="growdayApp"></div>`);
 renderGrowday();
}
