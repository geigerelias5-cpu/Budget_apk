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
    if(raw){ const s = JSON.parse(raw); if(s && Array.isArray(s.entries)) return s; }
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
window.inMonth = e => { const d=new Date(e.ts), n=new Date(); return d.getMonth()==n.getMonth() && d.getFullYear()==n.getFullYear(); };
window.newId = () => Date.now() + '' + Math.random().toString(36).slice(2,6);
/* Dauerbeträge, die diesen Monat noch nicht als Eintrag angelegt wurden (das macht die App beim Öffnen) */
window.fixedPending = function(S){
  const n=new Date(), k=n.getFullYear()+'-'+String(n.getMonth()+1).padStart(2,'0');
  return (S.fixed||[]).filter(f=>f.from<=k && !(f.skip||[]).includes(k) &&
    !S.entries.some(e=>e.fixed==f.id && inMonth(e))).reduce((a,f)=>a+f.amt,0);
};
