/* Gemeinsame Daten-Logik für App und Schnelleingabe */
window.CATS = [
  {id:'essen',     n:'Essen',     c:'#d71921'},
  {id:'einkauf',   n:'Einkauf',   c:'var(--ink)', w:'#f2f2f2'},
  {id:'transport', n:'Transport', c:'#8a8a8a'},
  {id:'freizeit',  n:'Freizeit',  c:'#f08c90'},
  {id:'wohnen',    n:'Wohnen',    c:'#6b6b6b'},
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

window.eur = new Intl.NumberFormat('de-DE',{style:'currency',currency:'EUR'});
window.eur0 = v => eur.format(v).replace(/,00(\s)/,'$1');
window.num = s => {
  s = String(s).trim().replace(/\s|€/g,'');
  if(s.includes(',')) s = s.replace(/\./g,'').replace(',','.');
  const v = parseFloat(s);
  return isFinite(v) ? Math.round(v*100)/100 : NaN;
};
window.inMonth = e => { const d=new Date(e.ts), n=new Date(); return d.getMonth()==n.getMonth() && d.getFullYear()==n.getFullYear(); };
window.newId = () => Date.now() + '' + Math.random().toString(36).slice(2,6);
