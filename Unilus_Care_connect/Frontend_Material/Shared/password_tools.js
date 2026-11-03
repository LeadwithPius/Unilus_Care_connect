/* password_tools.js: password rules, recovery codes, change password, first-login password change, temporary passwords. */
const PWMSG='Password must be 8 to 72 characters with at least one letter and one number.';
const pwOk=p=>/^(?=.*[A-Za-z])(?=.*\d).{8,72}$/.test(p||'');
const CODEA='ABCDEFGHJKMNPQRSTUVWXYZ23456789';
function mkCodes(){return Array.from({length:8},()=>{let t='';for(let i=0;i<10;i++)t+=CODEA[Math.random()*CODEA.length|0];return t.slice(0,5)+'-'+t.slice(5)})}
function mkTemp(){let t;do{t='';for(let i=0;i<10;i++)t+=CODEA[Math.random()*CODEA.length|0]}while(!/\d/.test(t)||!/[A-Z]/.test(t));return t}
const inp=(id,ph,ty,ac)=>`<input class="fld" id="${id}" type="${ty||'text'}" placeholder="${ph||''}" autocomplete="${ac||'off'}">`;
const lab=t=>`<label class="small">${t}</label>`;
function showCodes(codes,next){
 S.noClose=true;
 modal(`<h3>Save your recovery codes</h3><p class="small">If you forget your password, one of these codes lets you set a new one. Each code works once and we can’t show them again. Keep them somewhere private.</p><div style="display:grid;grid-template-columns:1fr 1fr;gap:6px;font-family:ui-monospace,monospace;font-size:13px;margin:10px 0">${codes.map(c=>`<div style="background:#e8f3f1;border-radius:8px;padding:7px;text-align:center">${c}</div>`).join('')}</div><button class="btn o" id="cpc">Copy codes</button><button class="btn" id="svd">I’ve saved them</button>`);
 $('cpc').onclick=()=>{try{navigator.clipboard.writeText(codes.join('\n')).then(()=>toast('Copied.'),()=>toast('Copy failed. Write them down.'))}catch(e){toast('Copy failed. Write them down.')}};
 $('svd').onclick=()=>{S.noClose=false;save();close();if(next)next()}}
function forceChange(){
 S.noClose=true;
 modal(`<h3>Set your own password</h3><p class="small">Your password was set by the administrator. Choose your own to continue.</p>${inp('np1','New password','password','new-password')}${inp('np2','Repeat new password','password','new-password')}<button class="btn" id="npgo">Save and continue</button>`);
 $('npgo').onclick=()=>{const a=S.acc[S.me],p=$('np1').value;if(!pwOk(p))return toast(PWMSG);if(p!==$('np2').value)return toast('The two passwords don’t match.');if(p===a.pw)return toast('Choose a different password.');a.pw=p;a.must=0;a.codes=mkCodes();S.in=true;S.cview='dash';save();showCodes(a.codes,enter)}}
function changePw(){
 modal(`<h3>Change password</h3>${lab('Current password')}${inp('cp0','','password','current-password')}${lab('New password')}${inp('cp1','','password','new-password')}${lab('Repeat new password')}${inp('cp2','','password','new-password')}<button class="btn" id="cpg">Change password</button><button class="btn o" id="cpr">Get new recovery codes</button><button class="btn o" onclick="close_()">Cancel</button>`);
 const a=S.acc[S.me];
 $('cpg').onclick=()=>{if(!a||$('cp0').value!==a.pw)return toast('Current password is incorrect.');const p=$('cp1').value;if(!pwOk(p))return toast(PWMSG);if(p!==$('cp2').value)return toast('The two new passwords don’t match.');if(p===a.pw)return toast('Choose a different password.');a.pw=p;save();close();toast('Password changed.')};
 $('cpr').onclick=()=>{if(!a||$('cp0').value!==a.pw)return toast('Enter your current password first.');a.codes=mkCodes();save();showCodes(a.codes)}}
function tempModal(n,t){modal(`<h3>Temporary password</h3><p class="small">Give this to ${n} in person or by a secure channel. They must choose their own password the first time they log in.</p><div style="font-family:ui-monospace,monospace;font-size:18px;text-align:center;background:#e8f3f1;border-radius:10px;padding:12px;margin:10px 0">${t}</div><button class="btn" onclick="close_()">Done</button>`)}
function resetFor(i){const c=S.cs[i],a=S.acc[c.e],t=mkTemp();a.pw=t;a.must=1;a.lock=0;a.fails=0;save();tempModal(c.n,t)}
