'use strict';
const Planner = (() => {
  const themes = [
    ['sage','Шалфей','#526b50','#f7f4ed'],['night','Тихий вечер','#a9c39a','#222c27'],
    ['lavender','Лаванда','#776191','#f4f0f8'],['rose','Чайная роза','#9b626c','#faf0f1'],
    ['ocean','У моря','#3e7284','#eff6f7'],['sand','Песчаный берег','#8a7148','#f8f2e6'],
    ['peach','Персиковый сад','#a46c49','#fcf1e8'],['forest','Лесная тропа','#99bfa5','#1c302a']
  ];
  const fonts = [['system','Системный'],['book','Книжный'],['soft','Мягкий · Open Sans'],['mono','Моноширинный']];
  const avatars = ['🌱','🌷','🌻','🍀','🦊','🐈','🌙','☀️'];
  function fresh() { return {version:2,theme:'sage',font:'system',motion:true,
    profile:{name:'',avatar:'🌱',height:null,bio:''},wellness:[],
    topics:[{id:'personal',name:'Личное',type:'personal'},{id:'work',name:'Работа',type:'work'},{id:'study',name:'Учёба',type:'study'}],tasks:[],events:[]}; }
  function day(s) { return new Date(s+'T12:00:00'); }
  function key(d) { return d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0')+'-'+String(d.getDate()).padStart(2,'0'); }
  function validDate(s) { return typeof s==='string' && /^\d{4}-\d{2}-\d{2}$/.test(s) && !isNaN(+day(s)) && key(day(s))===s; }
  function validTime(v) { return typeof v==='string' && /^([01]\d|2[0-3]):[0-5]\d$/.test(v); }
  function text(v,n,required=false) {return typeof v==='string' && v.length<=n && (!required||v.trim().length>0);}
  function number(v,min,max,integer=false) {return v===null || typeof v==='number' && Number.isFinite(v) && v>=min && v<=max && (!integer||Number.isInteger(v));}
  function validate(input) {
    if(!input || ![1,2].includes(input.version)) throw Error('Неверная версия копии');
    const s=JSON.parse(JSON.stringify(input));
    if(s.version===1) {s.version=2;s.font='system';s.motion=true;s.profile=fresh().profile;s.wellness=[];}
    if(!themes.some(t=>t[0]===s.theme)||!fonts.some(t=>t[0]===s.font)||typeof s.motion!=='boolean'||!Array.isArray(s.topics)||!s.topics.length||s.topics.length>200||!Array.isArray(s.tasks)||s.tasks.length>50000||!Array.isArray(s.events)||s.events.length>10000)throw Error('Неверный формат копии');
    const ids=new Set();for(const x of [...s.topics,...s.tasks,...s.events]){if(!x||typeof x.id!=='string'||!x.id||ids.has(x.id))throw Error('Неверные идентификаторы');ids.add(x.id);}
    const topics=new Set(s.topics.map(t=>t.id));
    for(const t of s.topics)if(!text(t.name,80,true)||!['personal','work','study'].includes(t.type))throw Error('Неверная тема');
    for(const t of s.tasks)if(!text(t.title,200,true)||!text(t.note,4000)||!topics.has(t.topic)||!validDate(t.date)||!['low','normal','high'].includes(t.priority)||![0,50,100].includes(t.progress)||(t.completedAt!==null&&!validDate(t.completedAt))||(t.progress===100&&!t.completedAt)||(t.progress!==100&&t.completedAt!==null))throw Error('Неверная задача');
    for(const e of s.events)if(!text(e.title,200,true)||!text(e.place,200)||!['work','study'].includes(e.type)||!validDate(e.date)||!validTime(e.start)||!validTime(e.end)||e.end<=e.start||!Array.isArray(e.days)||e.days.some(d=>!Number.isInteger(d)||d<0||d>6))throw Error('Неверное расписание');
    const p=s.profile;if(!p||!text(p.name,80)||!avatars.includes(p.avatar)||!number(p.height,30,260)||!text(p.bio,500))throw Error('Неверный профиль');
    if(!Array.isArray(s.wellness)||s.wellness.length>20000)throw Error('Неверный дневник');
    const dates=new Set();for(const w of s.wellness){if(!w||!validDate(w.date)||dates.has(w.date)||!number(w.weight,1,500)||!number(w.sleep,0,24)||!number(w.mood,1,5,true)||!number(w.energy,1,5,true)||!number(w.stress,1,5,true)||!text(w.note,2000))throw Error('Неверная запись самочувствия');dates.add(w.date);}
    return s;
  }
  function datesForMonth(month,weekdays) {
    if(typeof month!=='string'||!/^\d{4}-\d{2}$/.test(month)||!validDate(month+'-01')||!Array.isArray(weekdays)||weekdays.some(d=>!Number.isInteger(d)||d<0||d>6))throw Error('Выберите месяц и дни недели.');
    const dates=[];for(let i=1;i<=31;i++){const d=month+'-'+String(i).padStart(2,'0');if(validDate(d)&&weekdays.includes(day(d).getDay()))dates.push(d);}return dates;
  }
  function planCopies(tasks,ids,{mode,date,month,weekdays}) {
    if(!Array.isArray(ids)||!ids.length)throw Error('Выберите хотя бы одну задачу.');
    const chosen=[...new Set(ids)].map(id=>tasks.find(t=>t.id===id));if(chosen.some(t=>!t))throw Error('Задача уже удалена.');
    const dates=mode==='month'?datesForMonth(month,weekdays):validDate(date)?[date]:[];
    if(!dates.length)throw Error('Выберите дату или дни недели.');
    const plan=chosen.flatMap(source=>dates.filter(d=>mode!=='month'||d!==source.date).map(date=>({source,date})));
    if(!plan.length)throw Error('На выбранных датах уже находятся исходные задачи.');
    if(tasks.length+plan.length>50000)throw Error('В копии может быть не больше 50 000 задач.');
    return plan;
  }
  function makeCopies(plan,newId) {return plan.map(({source,date})=>({id:newId(),title:source.title,note:source.note,topic:source.topic,date,priority:source.priority,progress:0,completedAt:null}));}
  return {themes,fonts,avatars,fresh,validate,datesForMonth,planCopies,makeCopies,validDate,validTime};
})();
if(typeof module!=='undefined')module.exports=Planner;
