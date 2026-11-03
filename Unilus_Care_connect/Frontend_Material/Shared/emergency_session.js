function priorityCounsellorIndex(){
  return CNS.findIndex((_,i)=>cstat(i)[0]==='Available');
}

function nextPrioritySlots(count=4,counsellorIndex=-1){
  const first=new Date();
  first.setMinutes(0,0,0);
  first.setHours(first.getHours()+1);
  const slots=[];
  for(let i=0;i<24*14&&slots.length<count;i++){
    const date=new Date(first);
    date.setHours(date.getHours()+i);
    if(date.getDay()===0||date.getDay()===6)continue;
    if(date.getHours()<8||date.getHours()>=18)continue;
    const day=`${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
    const time=`${String(date.getHours()).padStart(2,'0')}:00`;
    if(counsellorIndex>=0&&S.prioritySessions.some(session=>session.cns===CNS[counsellorIndex].n&&session.date===day&&session.time===time))continue;
    slots.push({day,time,label:date.toLocaleString(undefined,{weekday:'short',month:'short',day:'numeric',hour:'numeric',minute:'2-digit'})});
  }
  return slots;
}

function sosBody(){
  const counsellorIndex=priorityCounsellorIndex();
  const counsellor=counsellorIndex<0?'No counsellor is currently available.':CNS[counsellorIndex].n;
  const slots=nextPrioritySlots(4,counsellorIndex);
  return `<h3 class="priority-session-heading">Need urgent support?</h3>
    <p class="small">Book a priority session without leaving this page. If you are in immediate danger, contact campus security or go to someone you trust.</p>
    ${counsellorIndex<0||!slots.length?'<div class="small priority-error">No weekday session time is currently available. Please try again later.</div>':`<div class="small priority-booking-info">First available counsellor: <b>${counsellor}</b></div>
    <label class="small priority-time-label" for="priority-time">Choose a time (nearest whole-hour slot first)</label>
    <select class="fld" id="priority-time">${slots.map((slot,i)=>`<option value="${slot.day}|${slot.time}" ${i===0?'selected':''}>${slot.label}</option>`).join('')}</select>
    <div class="small">Times are demo estimates and are not checked against a live calendar.</div>
    <button class="btn" type="button" onclick="bookPrioritySession(${counsellorIndex})">Book priority session</button>`}
    <button class="btn o" type="button" onclick="close_()">Close</button>`;
}

function openEmergencySession(){
  modal(sosBody());
}

function bookPrioritySession(counsellorIndex){
  if(S.role!=='s'||!S.in){
    modal('<h3>Sign in as a student first</h3><p class="small">Sign in to book a priority session, then open the emergency-session button again.</p><button class="btn o" type="button" onclick="close_()">Close</button>');
    return;
  }
  if(counsellorIndex<0||!CNS[counsellorIndex]||cstat(counsellorIndex)[0]!=='Available'){
    modal('<h3>No counsellor available</h3><p class="small">There is no available counsellor to book right now. Please try again later.</p><button class="btn o" type="button" onclick="close_()">Close</button>');
    return;
  }
  const selection=$('priority-time');
  if(!selection||!selection.value){
    modal('<h3>Choose a session time</h3><p class="small">Select an available time before booking.</p><button class="btn o" type="button" onclick="openEmergencySession()">Back</button>');
    return;
  }
  const [day,time]=selection.value.split('|');
  if(!/^\d{4}-\d{2}-\d{2}$/.test(day)||!/^\d{2}:00$/.test(time)){
    modal('<h3>Invalid session time</h3><p class="small">Please choose a valid time and try again.</p><button class="btn o" type="button" onclick="openEmergencySession()">Back</button>');
    return;
  }
  const hour=Number(time.slice(0,2));
  if(hour<8||hour>=18){
    modal('<h3>Outside booking hours</h3><p class="small">Priority sessions are available from 8:00 to 17:00. Choose another time.</p><button class="btn o" type="button" onclick="openEmergencySession()">Choose another time</button>');
    return;
  }
  const selectedDate=new Date(`${day}T12:00:00`);
  if(selectedDate.getDay()===0||selectedDate.getDay()===6){
    modal('<h3>Weekend bookings are unavailable</h3><p class="small">Choose a weekday for your priority session.</p><button class="btn o" type="button" onclick="openEmergencySession()">Choose another time</button>');
    return;
  }
  S.prioritySessions=Array.isArray(S.prioritySessions)?S.prioritySessions:[];
  if(S.prioritySessions.some(session=>session.cns===CNS[counsellorIndex].n&&session.date===day&&session.time===time)){
    modal('<h3>That time is no longer available</h3><p class="small">Choose another time to continue.</p><button class="btn o" type="button" onclick="openEmergencySession()">Choose another time</button>');
    return;
  }

  const tomorrow=new Date(`${day}T12:00:00`);
  tomorrow.setDate(tomorrow.getDate()+1);
  while(tomorrow.getDay()===0||tomorrow.getDay()===6)tomorrow.setDate(tomorrow.getDate()+1);
  const tomorrowKey=`${tomorrow.getFullYear()}-${String(tomorrow.getMonth()+1).padStart(2,'0')}-${String(tomorrow.getDate()).padStart(2,'0')}`;
  let rescheduled=false;
  if(S.booked&&S.cns===CNS[counsellorIndex].n&&S.appointmentDate===day&&S.appointmentTime===time){
    S.appointmentDate=tomorrowKey;
    S.slot=`${tomorrow.toLocaleDateString(undefined,{weekday:'short',month:'short',day:'numeric',year:'numeric'})} · ${time}`;
    S.transfers++;
    rescheduled=true;
  }
  S.prioritySessions.push({cns:CNS[counsellorIndex].n,date:day,time,bookingStatus:'approved'});
  save();

  const sessionDate=new Date(`${day}T12:00:00`).toLocaleDateString(undefined,{weekday:'long',month:'long',day:'numeric',year:'numeric'});
  const rescheduleMessage=rescheduled
    ? `<p class="small">Your existing appointment with ${CNS[counsellorIndex].n} was moved to ${tomorrow.toLocaleDateString(undefined,{weekday:'long',month:'long',day:'numeric',year:'numeric'})} at ${time}.</p>`
    : '';
  modal(`<h3 class="priority-session-heading">Priority session successfully booked</h3>
    <p class="small">${sessionDate} at ${time} with ${CNS[counsellorIndex].n}.</p>
    ${rescheduleMessage}
    <p class="small">This demo booking is saved in this browser. Rescheduling other students’ bookings requires the shared booking backend.</p>
    <button class="btn" type="button" onclick="close_()">Done</button>`);
}
