(function(){
 var UN={thousand:1e3,lakh:1e5,lakhs:1e5,crore:1e7,crores:1e7,million:1e6,billion:1e9};
 function nums(t){return (t.match(/\d[\d,]*/g)||[]).map(function(s){return +s.replace(/,/g,'');});}
 function vals(t){var re=/(\d[\d,]*(?:\.\d+)?)\s*(thousand|lakhs?|crores?|million|billion)?/gi,m,o=[];while((m=re.exec(t))){if(!m[1])continue;var b=parseFloat(m[1].replace(/,/g,''));if(isNaN(b))continue;var u=(m[2]||'').toLowerCase();o.push(Math.round(b*(u?(UN[u]||UN[u.replace(/s$/,'')]||1):1)));}return o;}
 function fmtIN(n){var s=''+Math.abs(n);if(s.length<=3)return s;var h=s.slice(0,-3),tl=s.slice(-3),g='',c=0;for(var i=h.length-1;i>=0;i--){g+=h[i];c++;if(c%2===0&&i)g+=',';}return g.split('').reverse().join('')+','+tl;}
 function fmtINTL(n){return (''+n).replace(/\B(?=(\d{3})+(?!\d))/g,',');}
 function bC(seq){if(seq.length<2)return [];var d0=seq[0],r=seq[1]/seq[0],df=seq[1]-seq[0],o=[];if(seq.every(function(x,i){return i===0||x===seq[i-1]*10+d0;}))o.push(seq[seq.length-1]*10+d0);if(seq[0]!==0&&r===Math.floor(r)&&seq.every(function(x,i){return i===0||x===seq[i-1]*r;}))o.push(Math.round(seq[seq.length-1]*r));if(seq.every(function(x,i){return i===0||x-seq[i-1]===df;}))o.push(seq[seq.length-1]+df);return o;}
 function pC(seq){var o=bC(seq).slice();var rt=seq.map(function(n){return Math.round(Math.sqrt(n));});if(rt.length>=2&&rt.every(function(r,i){return r*r===seq[i];})){var b=bC(rt);if(b.length)o.push(b[0]*b[0]);}return o;}
 function fB(re){return [].slice.call(document.querySelectorAll('button,[role=button]')).filter(function(b){return re.test((b.innerText||'').trim());})[0]||null;}
 function publish(){
  var mi=document.querySelector('.mission, .problem, .question');if(!mi)return null;
  var prompt=(mi.innerText||'').replace(/\s+/g,' ').trim();
  var els=[].slice.call(document.querySelectorAll('.opt,.choice,button[data-v]'));
  if(/km\/day|daily speed/i.test(prompt)){[].slice.call(document.querySelectorAll('button,[role=button],.card,.option')).forEach(function(b){if(/^[\d,]+\s*km\s*\/\s*day$/i.test((b.innerText||'').trim())&&els.indexOf(b)<0)els.push(b);});}
  var opts=els.map(function(o){return {id:o.getAttribute('data-v')||o.innerText.trim(),label:(o.innerText||'').trim().replace(/\s+/g,' '),value:o.getAttribute('data-v'),el:o,selected:/(^|\s)(sel|selected|active|chosen|picked)(\s|$)/.test(o.className||'')};}).filter(function(o){return o.label;});
  var fb=((document.querySelector('.msg,.result,.feedback')||{}).innerText||'').trim();
  return {prompt:prompt,opts:opts,submitEl:fB(/check|lock|submit/i),resetEl:fB(/reset/i),nextEl:fB(/next/i),feedback:fb,round:((document.querySelector('.v')||{}).innerText||'').trim(),phase:/correct|right|not\b|wrong|bigger|smaller|exact/i.test(fb)?'result':'answer'};
 }
 function solve(r){
  var t=r.prompt.toLowerCase(),L=r.opts.map(function(o){return o.label;});
  if(r.opts.length&&r.opts.every(function(o){return ['<','=','>'].indexOf(o.label.trim())>=0;})){var v=vals(r.prompt),a=v[0],b=v[1],an=a<b?'<':a>b?'>':'=';return {glow:an,submitOnPick:1,line:an==='='?'They are equal - tap "=".':'Tap "'+an+'" - the first number is '+(a<b?'smaller':'larger')+'.'};}
  if(r.opts.length&&r.opts.every(function(o){return /^\d+(\.\d+)?\s*x$/i.test(o.label.trim());})){var vv=vals(r.prompt),rr=vv[0]/vv[1],bt=r.opts.reduce(function(m,o){return Math.abs(parseFloat(o.label)-rr)<Math.abs(parseFloat(m.label)-rr)?o:m;});return {glow:bt.label,submitOnPick:1,line:'About '+rr.toFixed(1)+' times - tap "'+bt.label+'".'};}
  if(L.some(function(l){return /km\/day/i.test(l);})&&r.opts.length){var d=nums(r.prompt)[0],dy=nums(r.prompt)[1],nd=d/dy,bs=r.opts.reduce(function(m,o){return Math.abs(nums(o.label)[0]-nd)<Math.abs(nums(m.label)[0]-nd)?o:m;});return {glow:bs.label,submitOnPick:1,line:'Need about '+Math.round(nd)+' km/day - tap "'+bs.label+'".'};}
  if(L.some(function(l){return /indian|international/i.test(l);})){var N=nums(r.prompt).sort(function(a,b){return (''+b).length-(''+a).length;})[0],IN=fmtIN(N),IT=fmtINTL(N),cre=/indian\s+([\d,]+)\s+international\s+([\d,]+)/i,co=r.opts.filter(function(o){var mm=cre.exec(o.label);return mm&&mm[1].trim()===IN&&mm[2].trim()===IT;})[0];return {glow:co&&co.label,line:'Indian '+IN+', International '+IT+' - pick that card.'};}
  if(/round|nearest/.test(t)){var ov=r.opts.map(function(o){return nums(o.label)[0];}).filter(function(x){return !isNaN(x);}).sort(function(a,b){return a-b;}),gp=[];for(var i=1;i<ov.length;i++)gp.push(ov[i]-ov[i-1]);var pl=Math.min.apply(null,gp.filter(function(g){return g>0;})),Nn=Math.max.apply(null,nums(r.prompt).filter(function(x){return x!==pl&&ov.indexOf(x)<0;})),ans=Math.round(Nn/pl)*pl,cor=r.opts.filter(function(o){return nums(o.label)[0]===ans;})[0];return {glow:cor&&cor.label,submitOnPick:1,line:'Rounds to '+fmtIN(ans)+' - tap it.'};}
  if(/pattern|next product|next term/.test(t)){var all=nums(r.prompt).filter(function(x){return x>0;}),ovp=r.opts.map(function(o){return nums(o.label)[0];});for(var st=0;st<all.length-1;st++){var sq=all.slice(st);if(sq.length<2)break;var hit=pC(sq).filter(function(c){return ovp.indexOf(c)>=0;})[0];if(hit!=null){var cp=r.opts.filter(function(o){return nums(o.label)[0]===hit;})[0];return {glow:cp&&cp.label,digits:(''+hit).length,submitOnPick:1,line:'Next is '+hit+' ('+(''+hit).length+' digits).'};}}return {line:'Find the rule between the terms.'};}
  var cm=r.prompt.match(/current[:\s]*([\d,]+)/i),tm=r.prompt.match(/target[:\s]*([\d,]+)/i);
  if(cm&&tm){var tg=+tm[1].replace(/,/g,''),c=+cm[1].replace(/,/g,'');if(c===tg)return {ctrl:'submit',line:'You matched the target - tap Lock Build.'};if(c>tg)return {ctrl:'reset',line:'Over the target - tap Reset.'};var bt2=r.opts.map(function(o){return +o.value;}).filter(function(x){return !isNaN(x);}).sort(function(a,b){return b-a;}),fit=bt2.filter(function(x){return c+x<=tg;})[0],cb=r.opts.filter(function(o){return +o.value===fit;})[0];return {glow:cb&&cb.label,line:'Add +'+fmtIN(fit)+' ('+fmtIN(c)+' of '+fmtIN(tg)+').'};}
  return {line:''};
 }
 var G=[],bar=null,lv='',lrk='',pv=null,hi=null;
 function pkV(){if(pv)return pv;try{var vs=speechSynthesis.getVoices()||[];pv=vs.filter(function(v){return /en[-_]?IN/i.test(v.lang);})[0]||vs.filter(function(v){return /^en/i.test(v.lang);})[0]||null;}catch(e){}return pv;}
 function say(t){if(!t)return;try{speechSynthesis.cancel();var u=new SpeechSynthesisUtterance(t);u.rate=1.05;var v=pkV();if(v)u.voice=v;speechSynthesis.speak(u);}catch(e){}}
 function cg(){G.forEach(function(e){try{e.style.outline='';}catch(x){}});G=[];if(hi){var o=hi.getAttribute('data-ph');if(o!==null){hi.setAttribute('placeholder',o);hi.removeAttribute('data-ph');}hi=null;}}
 function gl(el,c){if(!el)return;el.style.outline='3px solid '+c;el.style.outlineOffset='2px';el.style.borderRadius='8px';G.push(el);}
 function setBar(t){if(!bar){bar=document.createElement('div');bar.id='__eduBar';bar.style.cssText='position:fixed;left:8px;right:8px;bottom:8px;z-index:2147483647;background:#0e1230;color:#fff;font:600 15px system-ui,sans-serif;padding:12px 16px;border-radius:12px;box-shadow:0 -6px 24px rgba(0,0,0,.45)';document.body.appendChild(bar);}bar.textContent=t;}
 function tick(){var r;try{r=publish();}catch(e){return;}if(!r)return;window.__eduRound=r;var p;try{p=solve(r);}catch(e){p={line:''};}cg();var isR=r.phase==='result';var picked=r.opts.some(function(o){return o.selected;});
  if(!isR){
   if(p.glow){var ge=r.opts.filter(function(o){return o.label===p.glow;})[0];gl(ge&&ge.el,'#ff9500');}
   if(p.ctrl==='submit')gl(r.submitEl,'#2e9e6b');
   if(p.ctrl==='reset')gl(r.resetEl||r.nextEl,'#ff9500');
   if(p.submitOnPick&&picked)gl(r.submitEl,'#2e9e6b');
   if(p.digits){var inp=document.querySelector('input[type=number],input.digit,.digit input,input:not([type=hidden])');if(inp){gl(inp,'#5b8bff');if(inp.getAttribute('data-ph')===null)inp.setAttribute('data-ph',inp.getAttribute('placeholder')||'');if(!inp.value)inp.setAttribute('placeholder','Type '+p.digits);hi=inp;}}
  }
  setBar(isR?('Coach: '+r.feedback):('Coach: '+(p.line||'Look at the problem.')));
  var rk=r.prompt.replace(/current.*/i,'')+'#'+r.round;if(rk!==lrk){lrk=rk;lv='';}
  var vln=isR?r.feedback:p.line;if(vln&&vln!==lv){lv=vln;say(vln);}
 }
 if(window.__eduIv)clearInterval(window.__eduIv);window.__eduIv=setInterval(tick,300);tick();
 window.EduCoach={stop:function(){clearInterval(window.__eduIv);cg();if(bar)bar.remove();try{speechSynthesis.cancel();}catch(e){}}};
})();
