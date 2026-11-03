/* CareConnect API client. All application data requests go to the Spring Boot backend. */
(function () {
  'use strict';
  const localHost = ['localhost', '127.0.0.1'].includes(window.location.hostname);
  const API_BASE_URL = (window.CARECONNECT_API_BASE_URL || (localHost ? 'http://localhost:8080/api' : '/api')).replace(/\/+$/, '');
  const TOKEN_KEY = 'careconnect_token';
  function storedToken(){try{return sessionStorage.getItem(TOKEN_KEY)}catch(_){return null}}
  const API = {
    baseURL: API_BASE_URL,
    token: storedToken(),
    setToken(token){
      this.token = typeof token === 'string' && token.trim() ? token.trim() : null;
      try { if(this.token)sessionStorage.setItem(TOKEN_KEY,this.token);else sessionStorage.removeItem(TOKEN_KEY); }
      catch(error){this.token=null;throw new Error('Your browser could not save the login session. Enable session storage and try again.')}
    },
    logout(){this.setToken(null)},
    async call(method,path,body){
      if(typeof path!=='string'||!path.startsWith('/'))throw new Error('API paths must start with /.');
      const headers={Accept:'application/json'};
      if(body!==undefined&&body!==null)headers['Content-Type']='application/json';
      if(this.token)headers.Authorization='Bearer '+this.token;
      let response;
      try{response=await fetch(this.baseURL+path,{method:String(method||'GET').toUpperCase(),headers,
        body:body===undefined||body===null?undefined:JSON.stringify(body),credentials:'omit'});}
      catch(error){console.error('CareConnect backend connection failed:',error);throw new Error('Cannot reach the CareConnect backend at '+this.baseURL+'. Check that Spring Boot is running.');}
      const text=response.status===204?'':await response.text();
      let data=null;if(text){try{data=JSON.parse(text)}catch(_){data=text}}
      if(!response.ok){
        if(response.status===401&&this.token)this.logout();
        const message=data&&typeof data==='object'?(data.message||data.error||data.detail||data.title):(typeof data==='string'?data:null);
        const error=new Error(message||('Request failed with HTTP '+response.status+'.'));error.status=response.status;error.data=data;throw error;
      }
      return data;
    },
    async login(userId,password,role){
      const result=await this.call('POST','/auth/login',{userId,password,role});
      if(!result||typeof result.token!=='string'||!result.token.trim())throw new Error('The backend did not return an authentication token.');
      this.setToken(result.token);return result;
    },
    me(){return this.call('GET','/auth/me')},
    register(details){return this.call('POST','/students/register',{fullName:details.fullName,email:details.email,studentNumber:details.studentNumber,password:details.password})},
    forgotPassword(email){return this.call('POST','/auth/forgot-password',{email})},
    resetPassword(token,newPassword){return this.call('POST','/auth/reset-password',{token,newPassword})},
    verifyEmail(token){return this.call('POST','/auth/verify-email',{token})},
    createCounsellor(details){return this.call('POST','/admin/counsellors',{email:details.email,password:details.password,displayName:details.displayName,professionalTitle:details.professionalTitle||'',biography:details.biography||''})},
    education(topic){return this.call('GET','/education'+(topic?'?topic='+encodeURIComponent(topic):''))},
    gameProgress(){return this.call('GET','/game/progress')},
    saveGame(section,score){return this.call('POST','/game/progress',{section,score})}
  };
  window.API=API;
  console.info('CareConnect API configured:',API.baseURL);
})();
