/* Gemeinsame Daten-Logik für App und Schnelleingabe */
window.CATS = [
  {id:'essen',     n:'Essen',     c:'#d71921'},
  {id:'einkauf',   n:'Einkauf',   c:'var(--ink)', w:'#f2f2f2'},
  {id:'transport', n:'Sprit',     c:'#8a8a8a'},
  {id:'freizeit',  n:'Freizeit',  c:'#f08c90'},
  {id:'wohnen',    n:'Versicherung', c:'#4d4d4d'},
  {id:'sonst',     n:'Sonstiges', c:'#c4c4bf'}
];
window.NATIVE = typeof window.Android !== 'undefined';
window.KEY = 'budget-proto-v1';

window.loadState = function(){
  try{
    const raw = NATIVE ? Android.getData() : localStorage.getItem(KEY);
    if(raw){ const s = JSON.parse(raw); if(s && Array.isArray(s.entries)){ window.__SD = Math.min(28, Math.max(1, Math.round(s.startDay || 1))); return s; } }
  }catch(e){}
  return {budget:0, entries:[]};
};
window.saveState = function(S){
  const j = JSON.stringify(S);
  try{ if(NATIVE) Android.setData(j); else localStorage.setItem(KEY, j); }catch(e){}
};

/* €-Zeichen eng an die Zahl */
const _eurFmt = new Intl.NumberFormat('de-DE',{style:'currency',currency:'EUR'});
window.eur = {format: v => _eurFmt.format(v).replace(/\s€$/,'\u202F€')};
window.eur0 = v => eur.format(v).replace(/,00(?=\s)/,'');
window.num = s => {
  s = String(s).trim().replace(/\s|€/g,'');
  if(s.includes(',')) s = s.replace(/\./g,'').replace(',','.');
  const v = parseFloat(s);
  return isFinite(v) ? Math.round(v*100)/100 : NaN;
};
/* Budget-Monat ab Starttag (z. B. 26.): Schlüssel "JJJJ-MM" des Startmonats */
window.periodKey = t => { const d=new Date(t), D=window.__SD||1; const x = d.getDate()>=D ? d : new Date(d.getFullYear(), d.getMonth()-1, 1);
  return x.getFullYear()+'-'+String(x.getMonth()+1).padStart(2,'0'); };
window.inMonth = e => periodKey(e.ts) === periodKey(Date.now());
window.newId = () => Date.now() + '' + Math.random().toString(36).slice(2,6);
/* Dauerbeträge, die diesen Monat noch nicht als Eintrag angelegt wurden (das macht die App beim Öffnen) */
window.fixedPending = function(S){
  const k=periodKey(Date.now());
  return (S.fixed||[]).filter(f=>f.from<=k && !(f.skip||[]).includes(k) &&
    !S.entries.some(e=>e.fixed==f.id && inMonth(e))).reduce((a,f)=>a+f.amt,0);
};
