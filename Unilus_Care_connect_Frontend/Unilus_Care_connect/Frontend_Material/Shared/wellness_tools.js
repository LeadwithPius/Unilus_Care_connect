/* wellness_tools.js: Breathe, Finch (self-care pet) and Snake Xenzia (grounding game). */
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
let sk;function stopSnake(){clearInterval(sk);sk=null}
function snake(){
 modal(`<h3>Snake Xenzia 🐍</h3><canvas id="cv" width="200" height="200"></canvas><div class="small" id="sc">Use the arrows. Slow breaths.</div><div style="display:grid;grid-template-columns:repeat(3,1fr);gap:6px;margin-top:6px"><span></span><button class="pill" data-d="u">↑</button><span></span><button class="pill" data-d="l">←</button><button class="pill" data-d="d">↓</button><button class="pill" data-d="r">→</button></div><button class="btn o" id="sx">Close</button>`);
 const cx=$('cv').getContext('2d');let sn=[[5,5],[4,5],[3,5]],d=[1,0],f=[10,10],pts=0;
 const M={u:[0,-1],d:[0,1],l:[-1,0],r:[1,0]};
 document.querySelectorAll('[data-d]').forEach(b=>b.onclick=()=>{const n=M[b.dataset.d];if(n[0]!==-d[0]||n[1]!==-d[1])d=n});
 onkeydown=e=>{const k={ArrowUp:'u',ArrowDown:'d',ArrowLeft:'l',ArrowRight:'r'}[e.key];if(k){const n=M[k];if(n[0]!==-d[0]||n[1]!==-d[1])d=n}};
 clearInterval(sk);sk=setInterval(()=>{const h=[(sn[0][0]+d[0]+20)%20,(sn[0][1]+d[1]+20)%20];
  if(sn.some(p=>p[0]===h[0]&&p[1]===h[1])){sn=[[5,5],[4,5],[3,5]];pts=0}
  sn.unshift(h);if(h[0]===f[0]&&h[1]===f[1]){pts++;f=[Math.random()*20|0,Math.random()*20|0]}else sn.pop();
  cx.fillStyle='#0e2b26';cx.fillRect(0,0,200,200);cx.fillStyle='#e0b33a';cx.fillRect(f[0]*10,f[1]*10,10,10);cx.fillStyle='#7fd1b9';sn.forEach(p=>cx.fillRect(p[0]*10,p[1]*10,9,9));$('sc').textContent='Score: '+pts},140);
 $('sx').onclick=()=>{stopSnake();close()};
}
CC.cleanups.push(()=>{clearInterval(bt);stopSnake()});
