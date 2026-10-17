/* wellbeing_checkin.js: the 5-question, non-diagnostic check-in. A low score points to the Emergency hotline and priority booking. */
function checkin(){
 const ans=Array(QS.length).fill(null);
 modal(`<h3>Wellbeing check-in</h3><div class="small" style="margin-bottom:6px">0 = not at all, 3 = very much</div>${QS.map((q,i)=>`<div class="small" style="margin-top:8px">${q}</div><div style="display:flex;gap:6px;margin-top:4px">${[0,1,2,3].map(n=>`<button class="pill" style="flex:1;background:#cfdcd8;color:var(--g)" data-q="${i}" data-n="${n}">${n}</button>`).join('')}</div>`).join('')}<button class="btn" id="sub">See result</button>`);
 document.querySelectorAll('[data-q]').forEach(b=>b.onclick=()=>{ans[b.dataset.q]=+b.dataset.n;document.querySelectorAll(`[data-q="${b.dataset.q}"]`).forEach(x=>{x.style.background='#cfdcd8';x.style.color='var(--g)'});b.style.background='var(--g)';b.style.color='#fff'});
 $('sub').onclick=()=>{if(ans.includes(null))return toast('Please answer every question.');const s=ans.reduce((a,b)=>a+b,0);S.hist.push({i:S.hist.length+1,s});
  if(s<=6)modal(`<h3 style="color:var(--r)">It sounds like a hard week</h3><p class="small">Your score was ${s}/15. You don’t have to handle this alone. If you need to speak to someone now, tap Emergency hotline.</p><button class="btn r" onclick="close_();go('support')">Book priority session</button><button class="btn o" onclick="close_();render()">Not now</button>`);
  else modal(`<h3>You’re coping well</h3><p class="small">Your score was ${s}/15. Keep it up with a few minutes in the wellness toolkit.</p><button class="btn" onclick="close_();go('learn')">Open toolkit</button>`);
  S.role==='s'&&render()}
}
