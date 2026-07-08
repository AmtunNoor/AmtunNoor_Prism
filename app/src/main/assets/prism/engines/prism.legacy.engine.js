/* =========================================================
   PRISM V6.8 PLUGIN HOST ENGINE
   Fixes:
   - Quran listen/learn folders merge
   - Learn badge on Quran listen tiles
   - Quran 5x/Hifz from slices.json
   - Allah Names 5x/Hifz chunk fallback by duration
   - Landing tiles uncluttered
   - Salah external URL
   - Months full-screen direct audio, no inner tile
   ========================================================= */

const surahs = [
{id:"A1",name:"الفاتحة",eng:"Al Fatiha",emoji:"🌟",file:"A5.mp3",color:"linear-gradient(135deg,#f59e0b,#ef4444)",surahNo:"1"},
{id:"A2",name:"الإخلاص",eng:"Al Ikhlas",emoji:"💖",file:"A6.mp3",color:"linear-gradient(135deg,#ec4899,#8b5cf6)",surahNo:"112"},
{id:"A3",name:"الفلق",eng:"Al Falaq",emoji:"🌙",file:"A7.mp3",color:"linear-gradient(135deg,#06b6d4,#3b82f6)",surahNo:"113"},
{id:"A4",name:"الناس",eng:"An Naas",emoji:"⭐",file:"A8.mp3",color:"linear-gradient(135deg,#10b981,#14b8a6)",surahNo:"114"},
{id:"A5",name:"الفيل",eng:"Al Feel",emoji:"🐘",file:"A9.mp3",color:"linear-gradient(135deg,#8b5cf6,#6366f1)",surahNo:"105"},
{id:"A6",name:"قريش",eng:"Quraish",emoji:"🕋",file:"A10.mp3",color:"linear-gradient(135deg,#0ea5e9,#2563eb)",surahNo:"106"},
{id:"A7",name:"الماعون",eng:"Al Ma'un",emoji:"🤲",file:"A11.mp3",color:"linear-gradient(135deg,#14b8a6,#22c55e)",surahNo:"107"},
{id:"A8",name:"الكوثر",eng:"Al Kawther",emoji:"💧",file:"A12.mp3",color:"linear-gradient(135deg,#f97316,#fb7185)",surahNo:"108"},
{id:"A9",name:"الكافرون",eng:"Al Kafiroon",emoji:"☀️",file:"A13.mp3",color:"linear-gradient(135deg,#84cc16,#22c55e)",surahNo:"109"},
{id:"A10",name:"النصر",eng:"An Nasr",emoji:"🏆",file:"A14.mp3",color:"linear-gradient(135deg,#38bdf8,#818cf8)",surahNo:"110"},
{id:"A11",name:"المسد",eng:"Al Masad",emoji:"🔥",file:"A15.mp3",color:"linear-gradient(135deg,#ef4444,#f97316)",surahNo:"111"},
{id:"A12",name:"تكبير",eng:"Takbir",emoji:"🕌",file:"A16.mp3",color:"linear-gradient(135deg,#6366f1,#8b5cf6)"},
{id:"A13",name:"الأذان",eng:"Azan",emoji:"📢",file:"A17.mp3",color:"linear-gradient(135deg,#06b6d4,#0ea5e9)"},
{id:"A14",name:"الكلمة الثانية",eng:"Second Kalima",emoji:"🌹",file:"A18.mp3",color:"linear-gradient(135deg,#14b8a6,#0f766e)"},
{id:"A15",name:"الضحى",eng:"Ad Duha",emoji:"☀️",file:"A19.mp3",color:"linear-gradient(135deg,#facc15,#fb7185)",surahNo:"93"},
{id:"A16",name:"إِنَّ الْمُسْلِمِينَ",eng:"Innal Muslimeena",emoji:"💚",file:"A20.mp3",color:"linear-gradient(135deg,#22c55e,#16a34a)"}
];


const FILE_META = {
"02":   {name:"آية الكرسي", eng:"Ayat al Kursi", emoji:"👑", color:"linear-gradient(135deg,#f59e0b,#facc15)"},
"02_1": {name:"البقرة", eng:"Al Baqarah", emoji:"🐄", color:"linear-gradient(135deg,#16a34a,#0ea5e9)"}
};

const SURAH_META = {
"2":   {name:"البقرة", eng:"Al Baqarah", emoji:"🐄", color:"linear-gradient(135deg,#16a34a,#0ea5e9)"},
"55":  {name:"الرحمن", eng:"Ar Rahman", emoji:"🌿", color:"linear-gradient(135deg,#22c55e,#14b8a6)"},
"93":  {name:"الضحى", eng:"Ad Duha", emoji:"☀️", color:"linear-gradient(135deg,#facc15,#fb7185)"},
"94":  {name:"الشرح", eng:"Ash Sharh", emoji:"🌸", color:"linear-gradient(135deg,#f9a8d4,#a78bfa)"},
"95":  {name:"التين", eng:"At Teen", emoji:"🫒", color:"linear-gradient(135deg,#84cc16,#22c55e)"},
"96":  {name:"العلق", eng:"Al Alaq", emoji:"📜", color:"linear-gradient(135deg,#f97316,#facc15)"},
"97":  {name:"القدر", eng:"Al Qadr", emoji:"🌌", color:"linear-gradient(135deg,#312e81,#8b5cf6)"},
"98":  {name:"البينة", eng:"Al Bayyinah", emoji:"📖", color:"linear-gradient(135deg,#0ea5e9,#6366f1)"},
"99":  {name:"الزلزلة", eng:"Az Zalzalah", emoji:"🌍", color:"linear-gradient(135deg,#92400e,#f97316)"},
"100": {name:"العاديات", eng:"Al Adiyat", emoji:"🐎", color:"linear-gradient(135deg,#ef4444,#f97316)"},
"101": {name:"القارعة", eng:"Al Qariah", emoji:"⚡", color:"linear-gradient(135deg,#facc15,#ef4444)"},
"102": {name:"التكاثر", eng:"At Takathur", emoji:"💰", color:"linear-gradient(135deg,#f59e0b,#f97316)"},
"103": {name:"العصر", eng:"Al Asr", emoji:"⏳", color:"linear-gradient(135deg,#38bdf8,#6366f1)"},
"104": {name:"الهمزة", eng:"Al Humazah", emoji:"🛡️", color:"linear-gradient(135deg,#8b5cf6,#ec4899)"},
"105": {name:"الفيل", eng:"Al Feel", emoji:"🐘", color:"linear-gradient(135deg,#8b5cf6,#6366f1)"},
"106": {name:"قريش", eng:"Quraish", emoji:"🕋", color:"linear-gradient(135deg,#0ea5e9,#2563eb)"},
"107": {name:"الماعون", eng:"Al Ma'un", emoji:"🤲", color:"linear-gradient(135deg,#14b8a6,#22c55e)"},
"108": {name:"الكوثر", eng:"Al Kawther", emoji:"💧", color:"linear-gradient(135deg,#f97316,#fb7185)"},
"109": {name:"الكافرون", eng:"Al Kafiroon", emoji:"☀️", color:"linear-gradient(135deg,#84cc16,#22c55e)"},
"110": {name:"النصر", eng:"An Nasr", emoji:"🏆", color:"linear-gradient(135deg,#38bdf8,#818cf8)"},
"111": {name:"المسد", eng:"Al Masad", emoji:"🔥", color:"linear-gradient(135deg,#ef4444,#f97316)"},
"112": {name:"الإخلاص", eng:"Al Ikhlas", emoji:"💖", color:"linear-gradient(135deg,#ec4899,#8b5cf6)"},
"113": {name:"الفلق", eng:"Al Falaq", emoji:"🌙", color:"linear-gradient(135deg,#06b6d4,#3b82f6)"},
"114": {name:"الناس", eng:"An Naas", emoji:"⭐", color:"linear-gradient(135deg,#10b981,#14b8a6)"}
};

const FALLBACK_MENU = [
{title:"Quran",mainUrl:"file:///data/user/0/com.noor.prism/files/quran/index.html?id=A1",plugin:{id:"quran",title:"Quran",theme:"quran",engine:"learning"}},
{title:"Salah",mainUrl:"https://busymommh.github.io/SalahSteps/SalahStepsIndex.html",plugin:{id:"salah",title:"Salah",theme:"salah",engine:"external"}},
{title:"Dua",mainUrl:"index.html?plugin=dua",plugin:{id:"dua",title:"Dua",folder:"dua",theme:"dua",engine:"gallery",supports:["listen"]}},
{title:"Islamic Months",mainUrl:"index.html?plugin=months&autoplay=1",plugin:{id:"months",title:"Islamic Months",folder:"months",theme:"months",engine:"directAudio",playFromLanding:true,supports:["listen"],effect:{type:"aquarium",waterRays:true,bubbles:true,fishZones:true,mouthMove:true,tailGlow:true}}},
{title:"Allah Names",mainUrl:"index.html?plugin=names",plugin:{id:"names",title:"Allah Names",folder:"names",theme:"names",engine:"spotlight",supports:["listen","repeat5","hifz"],chunkMode:{enabled:true,namesPerChunk:4,repeatCount:5}}}
];

const THEME = {
quran:["#f59e0b","#ef4444","📖"],
salah:["#22c55e","#0ea5e9","🕌"],
dua:["#f472b6","#8b5cf6","🤲"],
months:["#38bdf8","#6366f1","🗓"],
names:["#facc15","#fb7185","ﷲ"],
numbers:["#f97316","#22c55e","🔢"],
"salah-names":["#fb923c","#1d4ed8","🌙"],
angels:["#312e81","#06b6d4","🪽"],
pillars:["#f59e0b","#14b8a6","🏛️"],
letters:["#a855f7","#0ea5e9","🔠"],
default:["#a78bfa","#22d3ee","✨"]
};

const DEFAULT_PALETTE = [
"linear-gradient(135deg,#f97316,#facc15)",
"linear-gradient(135deg,#22c55e,#14b8a6)",
"linear-gradient(135deg,#38bdf8,#6366f1)",
"linear-gradient(135deg,#ec4899,#8b5cf6)",
"linear-gradient(135deg,#ef4444,#fb7185)",
"linear-gradient(135deg,#84cc16,#06b6d4)",
"linear-gradient(135deg,#a855f7,#f472b6)",
"linear-gradient(135deg,#0ea5e9,#22c55e)"
];

const TV = {
cols: window.innerWidth <= 768 ? 2 : 4,
focused: 0,
isTV: /Android TV|TV|SmartTV|MiTV/i.test(navigator.userAgent) || !("ontouchstart" in window)
};

if(TV.isTV && window.innerWidth > 768){document.body.classList.add("tv-mode");}

window.addEventListener("resize",()=>{
TV.cols = window.innerWidth <= 768 ? 2 : 4;
if(TV.isTV && window.innerWidth > 768){document.body.classList.add("tv-mode");}
else{document.body.classList.remove("tv-mode");}
});

const grid = document.getElementById("grid");
const map = {};
let items = [];
let menuData = [];
let slicesData = {};
let currentAudio = null;
let currentButton = null;
let currentCard = null;
let currentItem = null;
let loopAll = false;
let currentView = "landing";
let currentPlugin = null;
let sequenceCancelToken = 0;
let storyTimer = null;

function params(){return new URLSearchParams(location.search);}
function getMode(){return document.getElementById("mode").value;}
function setModeListen(){
const m = document.getElementById("mode");
if(m) m.value = "listen";
}

function shouldSuppressSpeech(item){
const pid = (currentPlugin && currentPlugin.id) ? String(currentPlugin.id).toLowerCase() : "";
const engine = (currentPlugin && currentPlugin.engine) ? String(currentPlugin.engine).toLowerCase() : "";
const suppressIds = new Set(["months","numbers","salah-names","salahnames"]);
if(suppressIds.has(pid)) return true;
if(engine === "story") return true;
if(engine === "spotlight" && pid !== "names") return true;
return false;
}

function speak(text){
try{
const u = new SpeechSynthesisUtterance(text);
u.rate = .9;
speechSynthesis.cancel();
speechSynthesis.speak(u);
}catch(e){}
}


async function fetchFirstJson(urls, fallback){
for(const url of urls){
  try{
    const res = await fetch(url,{cache:"no-store"});
    if(res.ok) return await res.json();
  }catch(e){}
}
return fallback;
}

async function fetchJson(url,fallback){
try{
const res = await fetch(url,{cache:"no-store"});
if(!res.ok) throw new Error("not found");
return await res.json();
}catch(e){return fallback;}
}


/* ================= PRISM V6.8 TRUE PLUGIN HOST =================
   index.html is now only a host. New cards are discovered from:
   1) plugins/plugins.json registry
   2) plugins/<id>/plugin.json
   3) legacy fallback plugins/<id>.plugin.json
   Add a future card by adding its plugin folder + plugin.json and listing id in plugins/plugins.json.
*/
const PRISM_PLUGIN_HOST_VERSION = "v690_frozen_last_polish_20260704";

function cleanPluginId(id){
  return String(id || "").trim().toLowerCase().replace(/[^a-z0-9_-]/g, "");
}

function pluginEntryToId(entry){
  if(typeof entry === "string") return cleanPluginId(entry);
  if(entry && typeof entry === "object") return cleanPluginId(entry.id || entry.plugin || entry.folder || entry.name);
  return "";
}

function pluginConfigUrls(id){
  const clean = cleanPluginId(id);
  // V6.9 RC2: clean plugin host only.
  // Old root-level plugins/*.plugin.json files are ignored to prevent stale configs.
  return clean ? [`plugins/${clean}/plugin.json`, `../plugins/${clean}/plugin.json`] : [];
}

function normalizePluginEntry(plugin, menuEntry){
  if(!plugin || !plugin.id) return null;
  const id = cleanPluginId(plugin.id);
  const merged = {...plugin, id};
  if(menuEntry && menuEntry.plugin){
    Object.keys(menuEntry.plugin).forEach(k=>{
      if(merged[k] === undefined) merged[k] = menuEntry.plugin[k];
    });
  }
  if(!merged.title) merged.title = id;
  if(!merged.theme) merged.theme = id;
  if(merged.enabled === undefined) merged.enabled = true;
  if(merged.showOnLanding === undefined) merged.showOnLanding = true;
  if(menuEntry && Array.isArray(menuEntry.syncFiles)) merged.__syncFiles = menuEntry.syncFiles;
  if(!merged.syncFiles && Array.isArray((plugin||{}).syncFiles)) merged.syncFiles = plugin.syncFiles;
  return {
    title: merged.title || id,
    mainUrl: merged.mainUrl || (id === "salah" ? "https://busymommh.github.io/SalahSteps/SalahStepsIndex.html" : `index.html?plugin=${encodeURIComponent(id)}`),
    plugin: merged,
    syncFiles: (menuEntry && Array.isArray(menuEntry.syncFiles)) ? menuEntry.syncFiles : []
  };
}

async function loadOnePlugin(id, legacyMenu){
  const clean = cleanPluginId(id);
  if(!clean) return null;
  const menuEntry = (legacyMenu || []).find(m => cleanPluginId((m.plugin||{}).id || m.title) === clean);
  for(const url of pluginConfigUrls(clean)){
    try{
      const res = await fetch(url + (url.includes("?") ? "&" : "?") + "v=" + encodeURIComponent(PRISM_PLUGIN_HOST_VERSION), {cache:"no-store"});
      if(res.ok){
        const plugin = await res.json();
        return normalizePluginEntry(plugin, menuEntry);
      }
    }catch(e){}
  }
  if(menuEntry && menuEntry.plugin) return normalizePluginEntry(menuEntry.plugin, menuEntry);
  return null;
}

async function loadPluginRegistry(){
  const registry = await fetchFirstJson([
    `plugins/plugins.json?v=${PRISM_PLUGIN_HOST_VERSION}`,
    `../plugins/plugins.json?v=${PRISM_PLUGIN_HOST_VERSION}`
  ], null);
  if(Array.isArray(registry)) return registry.map(pluginEntryToId).filter(Boolean);
  if(registry && Array.isArray(registry.plugins)) return registry.plugins.map(pluginEntryToId).filter(Boolean);
  return null;
}

async function loadPluginHostMenu(legacyMenu){
  const registry = await loadPluginRegistry();
  const legacyIds = (legacyMenu || []).map(m => cleanPluginId((m.plugin||{}).id || m.title)).filter(Boolean);
  const ids = registry && registry.length ? registry : legacyIds;
  const seen = new Set();
  const out = [];
  for(const id of ids){
    if(!id || seen.has(id)) continue;
    seen.add(id);
    const entry = await loadOnePlugin(id, legacyMenu);
    if(entry) out.push(entry);
  }
  out.sort((a,b)=>{
    const pa = Number((a.plugin||{}).priority ?? 999);
    const pb = Number((b.plugin||{}).priority ?? 999);
    if(pa !== pb) return pa - pb;
    return String(a.title||"").localeCompare(String(b.title||""));
  });
  return out.length ? out : (legacyMenu && legacyMenu.length ? legacyMenu : FALLBACK_MENU);
}

function gradientFor(theme){
const t = THEME[theme] || THEME.default;
const a=t[0], b=t[1], c=t[3] || t[1];
return `radial-gradient(circle at 28% 18%,rgba(255,255,255,.24),transparent 28%), linear-gradient(135deg,${a} 0%,${b} 52%,${c} 100%)`;
}

function iconFor(theme){
return (THEME[theme] || THEME.default)[2];
}

function applyImageOrGradient(card, image, theme){
  const setGradient = ()=>{
    card.style.background = gradientFor(theme);
    card.style.backgroundImage = "";
    card.dataset.tileImageStatus = "gradient";
    card.dataset.iconOnly = "false";
  };
  if(image && image.type === "gradient" && Array.isArray(image.colors)){
    card.style.background = `linear-gradient(135deg,${image.colors.join(",")})`;
    card.dataset.tileImageStatus = "gradient";
    card.dataset.iconOnly = "false";
    return;
  }
  const raw = Array.isArray(image) ? image : [image];
  const candidates = raw
    .map(v => typeof v === "string" ? {src:v, iconOnly:false} : v)
    .filter(v => v && typeof v.src === "string" && v.src.trim())
    .map(v => ({src:v.src.trim(), iconOnly:!!v.iconOnly}));
  if(!candidates.length){ setGradient(); return; }

  setGradient();
  let i = 0;
  const tryNext = ()=>{
    if(i >= candidates.length) return;
    const cand = candidates[i++];
    const src = cand.src;
    const probe = new Image();
    probe.onload = ()=>{
      card.style.backgroundImage = `linear-gradient(rgba(2,6,23,.04),rgba(2,6,23,.12)), url('${src}')`;
      card.style.backgroundColor = "transparent";
      card.dataset.tileImageStatus = "image";
      card.dataset.iconOnly = cand.iconOnly ? "true" : "false";
    };
    probe.onerror = tryNext;
    probe.src = src + (src.includes("?") ? "&" : "?") + "v=" + encodeURIComponent(PRISM_PLUGIN_HOST_VERSION);
  };
  tryNext();
}

function landingTileCandidates(plugin, id){
  const clean = cleanPluginId(id || plugin.id || plugin.folder || "");
  const folder = String(plugin.folder || clean || "").replace(/^\/+|\/+$/g,"");
  const out = [];
  const add = (src, iconOnly=false)=>{
    if(typeof src !== "string" || !src.trim()) return;
    const entry = {src:src.trim(), iconOnly};
    if(!out.some(x=>x.src === entry.src)) out.push(entry);
  };

  const keepTextModules = new Set(["quran","salah","dua","salah-names","salahnames"]);
  const keepText = keepTextModules.has(clean);

  // Explicit landing tile image. If it loads, most plugins use icon-only.
  if(typeof plugin.landingTileImage === "string") add(plugin.landingTileImage, !keepText);

  // Future-proof convention: folder/folder_icon.webp/jpeg/png.
  if(folder && clean){
    add(`${folder}/${clean}_icon.webp`, !keepText);
    add(`${folder}/${clean}_icon.jpeg`, !keepText);
    add(`${folder}/${clean}_icon.jpg`, !keepText);
    add(`${folder}/${clean}_icon.png`, !keepText);
    add(`plugins/${clean}/${clean}_icon.webp`, !keepText);
    add(`plugins/${clean}/${clean}_icon.jpeg`, !keepText);
    add(`plugins/${clean}/${clean}_icon.jpg`, !keepText);
    add(`plugins/${clean}/${clean}_icon.png`, !keepText);
  }

  // Existing legacy modules intentionally keep their artwork + title.
  const canUseTileImage = plugin.landingTileImage === true || keepText;
  if(canUseTileImage) add(plugin.tileImage, !keepText);

  return out;
}

function setHeader(title, subtitle){
document.getElementById("pageTitle").innerText = title;
document.getElementById("pageSubtitle").innerText = subtitle;
}

function setTopbar(show, showHome){
document.getElementById("topbar").style.display = show ? "flex" : "none";
document.getElementById("btnBack").style.display = showHome ? "inline-block" : "none";
}

function clearGrid(){
Object.keys(map).forEach(k=>delete map[k]);
items = [];
grid.innerHTML = "";
TV.focused = 0;
clearTimers();
}


function setScreenMode(mode, pluginId){
document.body.classList.remove("plugin-ui-final","plugin-months","plugin-numbers","plugin-salahnames","plugin-names","plugin-external");
if(mode === "plugin" || mode === "external"){
  document.body.classList.add("plugin-ui-final");
  if(pluginId) document.body.classList.add("plugin-" + String(pluginId).replace(/[^a-z0-9_-]/gi,"").toLowerCase());
}
}


function setPrismViewportHeight(){
document.documentElement.style.setProperty("--prism-vh", window.innerHeight + "px");
}
setPrismViewportHeight();
window.addEventListener("resize",()=>setTimeout(setPrismViewportHeight,60));
window.addEventListener("orientationchange",()=>setTimeout(setPrismViewportHeight,250));

function clearTimers(){
sequenceCancelToken++;
if(storyTimer){ clearTimeout(storyTimer); storyTimer = null; }
}

function pluginEntry(pluginId){
return menuData.find(m => (m.plugin||{}).id === pluginId) || {};
}

function quranSyncFiles(){
const q = menuData.find(m => (m.plugin||{}).id === "quran" || m.title === "Quran");
return (q && ((q.plugin && q.plugin.syncFiles) || q.syncFiles)) ? ((q.plugin && q.plugin.syncFiles) || q.syncFiles) : [];
}

function pluginSyncFiles(plugin){
return (plugin && (plugin.__syncFiles || plugin.syncFiles)) || (pluginEntry(plugin.id).syncFiles || []);
}

/* ================= LANDING PAGE ================= */
function buildLanding(){
currentView = "landing";
currentPlugin = null;
document.body.classList.remove("plugin-names");
document.body.classList.remove("quran-learn-mode");
document.body.classList.add("landing-mode");
document.body.classList.remove("plugin-stage-mode");
setScreenMode("landing");
setStageGridMode(false);
clearGrid();
stopAll(false);
setHeader("🌙 Mariam & Hamza's 🌙","✨Choose Your Prism Adventure!✨");
setTopbar(false,false);

const cards = (menuData.length ? menuData : FALLBACK_MENU)
.filter(entry => (entry.plugin || {}).enabled !== false)
.filter(entry => (entry.plugin || {}).showOnLanding !== false);

cards.forEach((entry,idx)=>{
const plugin = entry.plugin || {};
const id = plugin.id || entry.title.toLowerCase().replace(/\s+/g,"-");
const theme = plugin.theme || id || "default";
const card = document.createElement("div");
card.className = "card prism-audio-tile";
card.dataset.pluginId = id;
const tileCandidates = landingTileCandidates(plugin, id);
applyImageOrGradient(card, tileCandidates, theme);
card.innerHTML = `
<div class="card-info-box">
  <div class="emoji">${iconFor(theme)}</div>
  <div class="arabic">${entry.title}</div>
  <div class="english"></div>
</div>
<button class="play-btn">▶</button>
`;
card.onclick = ()=>openMenuEntry(entry);
card.querySelector("button").onclick = (e)=>{e.stopPropagation();openMenuEntry(entry);};
grid.appendChild(card);
const item = {id,entry,card,index:idx,type:"landing"};
items.push(item);
map[id] = item;
});
updateFocus();
}

function openMenuEntry(entry){
const plugin = entry.plugin || {};
const id = plugin.id;

if(id === "quran" || entry.title === "Quran"){
history.pushState(null,"","?plugin=quran");
buildQuran();
return;
}

if(id === "salah" || plugin.engine === "external" || entry.title === "Salah"){
history.pushState(null,"",`?plugin=${encodeURIComponent(id || "salah")}`);
buildExternalBridge(entry);
return;
}

if(plugin.playFromLanding || plugin.engine === "directAudio"){
history.pushState(null,"",`?plugin=${encodeURIComponent(id)}&autoplay=1`);
buildPlugin(plugin,true);
return;
}

if(id){
history.pushState(null,"",`?plugin=${encodeURIComponent(id)}`);
buildPlugin(plugin,false);
return;
}

if(entry.mainUrl){ location.href = entry.mainUrl; }
}

function goHome(){
history.pushState(null,"","index.html");
buildLanding();
try{PrismAudioCache.start();}catch(e){}
}


/* ================= EXTERNAL URL BRIDGE ================= */
function buildExternalBridge(entry){
currentView = "external";
currentPlugin = entry.plugin || {id:"salah",title:"Salah"};
document.body.classList.remove("landing-mode");
document.body.classList.remove("quran-learn-mode");
document.body.classList.add("plugin-stage-mode");
setScreenMode("external","external");
setStageGridMode(true);
clearGrid();
stopAll(false);
setHeader("🕌 Salah"," ");
setTopbar(true,true);

const url = (entry.plugin && entry.plugin.externalUrl) || entry.mainUrl || "https://busymommh.github.io/SalahSteps/SalahStepsIndex.html";

const wrap = document.createElement("div");
wrap.className = "external-bridge remote-focus";
wrap.innerHTML = `<iframe src="${url}" allow="autoplay; fullscreen"></iframe>`;
grid.appendChild(wrap);

const item = {id:"salah-bridge",card:wrap,index:0,type:"external"};
items.push(item);
map[item.id]=item;
TV.focused=0;
updateFocus();
}


/* ================= QURAN VIEW ================= */
function buildQuran(){
disableAutoTopbar();
setModeListen();
currentView = "quran";
currentPlugin = {id:"quran",title:"Quran",theme:"quran",engine:"learning"};
document.body.classList.remove("landing-mode");
document.body.classList.remove("plugin-names");
document.body.classList.remove("plugin-stage-mode");
setScreenMode("quran");
setStageGridMode(false);
setHeader("🌙 Mariam & Hamza's 🌙","✨Quran Adventures with Mom!✨");
setTopbar(true,true);
renderQuranItems();
}

function normalizeFileName(path){
return String(path).split("/").pop().replace(/\.(mp3|m4a|wav|ogg)$/i,"");
}

function surahNumberFromPath(path){
const raw = normalizeFileName(path);
const first = raw.split(/[_-]/)[0];
const m = first.match(/\d+/);
if(!m) return "";
return String(Number(m[0]));
}

function titleFromPath(path){
const meta = metaForPath(path);
if(meta) return meta.eng;
const raw = normalizeFileName(path);
return raw.replace(/[-_]/g," ").replace(/\b\w/g,m=>m.toUpperCase());
}

function metaForSurahNumber(no){
return SURAH_META[String(Number(no))] || null;
}

function metaForPath(path){
const file = normalizeFileName(path);
if(typeof FILE_META !== "undefined" && FILE_META[file]) return FILE_META[file];
return metaForSurahNumber(surahNumberFromPath(path));
}

function makeSurahDisplayFromPath(path, fallbackIndex){
const no = surahNumberFromPath(path);
const meta = metaForPath(path);
return {
  name: meta?.name || `سورة ${no || fallbackIndex + 1}`,
  eng: meta?.eng || `Surah ${no || titleFromPath(path)}`,
  emoji: meta?.emoji || "📖",
  color: meta?.color || DEFAULT_PALETTE[fallbackIndex % DEFAULT_PALETTE.length],
  surahNo: no
};
}

function getListenFiles(){
return quranSyncFiles().map(f=>f.path).filter(p=>/^listen\/.+\.(mp3|m4a|wav|ogg)$/i.test(p));
}

function getLearnFiles(){
const audioRe = /^(learn|listen)\/.+\.(mp3|m4a|wav|ogg)$/i;
// Slices/config is the source of truth for Learn mode.
// If a listen/<surah>.mp3 entry exists here, Learn reuses the existing Listen audio.
const fromSlices = Object.keys(slicesData || {}).filter(k=>audioRe.test(k));
const sliceSurahs = new Set(fromSlices.map(p=>surahNumberFromPath(p)).filter(Boolean));
const fromSync = quranSyncFiles()
  .map(f=>f.path)
  .filter(p=>/^learn\/.+\.(mp3|m4a|wav|ogg)$/i.test(p))
  .filter(p=>!sliceSurahs.has(surahNumberFromPath(p)));
return [...new Set([...fromSlices,...fromSync])];
}

function findLearnForSurah(s){
const learns = getLearnFiles();
const candidates = [];
if(s.surahNo) candidates.push(String(s.surahNo));
candidates.push(String(s.id).replace(/[^\d]/g,""));
for(const c of candidates){
const hit = learns.find(p => surahNumberFromPath(p) === c);
if(hit) return hit;
}
return null;
}

function buildQuranListenSource(){
const base = surahs.map(s=>{
const learn = findLearnForSurah(s);
return {
...s,
listenFile:s.file,
learnFile:learn,
slices:learn ? slicesData[learn] : null,
hasLearn:!!learn
};
});

const usedNums = new Set(base.map(s=>s.surahNo).filter(Boolean));
const extraListen = getListenFiles()
.filter(p => !usedNums.has(surahNumberFromPath(p)))
.sort((a,b)=>{
  const sa = surahNumberFromPath(a);
  const sb = surahNumberFromPath(b);
  if(sa === "55" && sb !== "55") return 1;
  if(sb === "55" && sa !== "55") return -1;
  return Number(sa || 9999) - Number(sb || 9999);
})
.map((p,idx)=>{
const display = makeSurahDisplayFromPath(p, idx);
const learnMatch = getLearnFiles().find(l => surahNumberFromPath(l) === surahNumberFromPath(p));
return {
id:`listen-${normalizeFileName(p) || idx}`,
...display,
file:p,
listenFile:p,
learnFile:learnMatch,
slices:learnMatch ? slicesData[learnMatch] : null,
hasLearn:!!learnMatch
};
});

return [...base,...extraListen];
}

function buildQuranLearnSource(){
const learnPaths = getLearnFiles().sort((a,b)=>{
const sa = surahNumberFromPath(a);
const sb = surahNumberFromPath(b);
if(sa === "55" && sb !== "55") return 1;
if(sb === "55" && sa !== "55") return -1;
return Number(sa || 9999) - Number(sb || 9999);
});
const existingAudio = new Set(quranSyncFiles().map(f=>f.path));
const listenFiles = getListenFiles();
return learnPaths.map((p,idx)=>{
const display = makeSurahDisplayFromPath(p, idx);
const surahNo = surahNumberFromPath(p);
const hasOwnLearnRecording = /^learn\//i.test(p) && existingAudio.has(p);
const listenFallback = listenFiles.find(l => surahNumberFromPath(l) === surahNo);
const playbackFile = hasOwnLearnRecording ? p : (listenFallback || p);
return {
id:`learn-${normalizeFileName(p) || idx}`,
...display,
emoji:display.emoji || "🎓",
file:playbackFile,
listenFile:listenFallback || playbackFile,
learnFile:playbackFile,
learnConfigKey:p,
reuseListenAudio:!hasOwnLearnRecording,
slices:slicesData[p] || null,
learningMode:slicesData[p] ? "slices" : "durationChunks",
hasLearn:true
};
});
}

function renderQuranItems(){
const mode = getMode();
clearGrid();
stopAll(false);

const isMainListen = mode === "listen";
document.body.classList.toggle("quran-learn-mode", !isMainListen);
let source = isMainListen ? buildQuranListenSource() : buildQuranLearnSource();

if(!source.length) source = surahs;

source.forEach((s,idx)=>{
const item = {...s};
if(isMainListen && item.listenFile) item.file = item.listenFile;
if(!isMainListen && item.learnFile) item.file = item.learnFile;
createAudioCard(item,idx,"quran", isMainListen && item.hasLearn);
});

updateFocus();
prewarmVisibleAudios(6);

const requested = params().get("id") || params().get("surah");
if(requested && map[requested]){
TV.focused = map[requested].index;
qrFix();
}
}


/* ================= PRISM V6.7 FINAL EFFECT LIBRARY =================
   Locked reusable built-in effect names for future plugin-only cards.

   Current active engines:
   - spotlight          : image + focus/glow based on coordinates/timestamps
   - aquarium           : months-style ambient water/bubbles/fish layer
   - storyTimeline      : SalahNames-style time-based visual transition
   - timedTileFocus     : future Angels-style active tile cueing
   - pillarFocus        : future Five Pillars-style active pillar cueing
   - particleField      : reusable soft sparkles/bubbles/stars background
   - softPulse          : gentle active object pulse
   - orbit              : reserved for future Allah Names / grouped concepts
   - carousel           : reserved for future card sets

   New cards must select an effect in plugin JSON.
   Do not edit core files for normal new cards.
*/
const PRISM_EFFECT_LIBRARY = Object.freeze([
  "spotlight",
  "aquarium",
  "storyTimeline",
  "timedTileFocus",
  "pillarFocus",
  "particleField",
  "softPulse",
  "orbit",
  "carousel"
]);

function prismEffectType(plugin){
  const type = plugin && plugin.effect && plugin.effect.type ? String(plugin.effect.type) : "";
  if(type === "coordinateSpotlight" || type === "numberSpotlight") return "spotlight";
  if(type === "timeStory") return "storyTimeline";
  return type || "spotlight";
}

/* ================= PLUGIN ROUTER ================= */



/* ================= PRISM CORE LOCK V6.6 FINAL ================= */
const PRISM_APP_LOGO_URL = "https://raw.githubusercontent.com/AmtunNoor/AmtunNoor_Prism/main/app/src/main/res/drawable/app_icon.png";
function prismLogoUrl(){
  return PRISM_APP_LOGO_URL;
}

/* ================= PRISM V6.6 FLOATING PRISM CONTROL ================= */
let prismControlHideTimer = null;

function ensurePrismControlButton(){
  let btn = document.getElementById("prismControlButton");
  if(btn) return btn;
  btn = document.createElement("button");
  btn.id = "prismControlButton";
  btn.className = "prism-control-button";
  btn.type = "button";
  btn.setAttribute("aria-label", "Prism controls");
  btn.innerHTML = `<img src="${prismLogoUrl()}" alt="Prism">`;
  btn.onclick = (e)=>{
    e.stopPropagation();
    showPrismControls(true);
  };
  document.body.appendChild(btn);
  return btn;
}

function showPrismControls(force){
  const body = document.body;
  if(!body.classList.contains("prism-float-control")) return;
  ensurePrismControlButton();
  body.classList.add("show-topbar");
  clearTimeout(prismControlHideTimer);
  prismControlHideTimer = setTimeout(()=>{
    body.classList.remove("show-topbar");
  }, force ? 4200 : 2400);
}

function setupAutoTopbar(){
  const body = document.body;
  const bar = document.querySelector(".topbar");
  if(!bar) return;
  body.classList.add("auto-topbar","prism-float-control");
  ensurePrismControlButton();

  const showSmall = ()=>{
    body.classList.add("show-prism-control");
    clearTimeout(prismControlHideTimer);
    prismControlHideTimer = setTimeout(()=>body.classList.remove("show-topbar"),2400);
  };

  ["mousemove","pointerdown","touchstart","keydown"].forEach(ev=>{
    window.addEventListener(ev, showSmall, {passive:true});
  });

  body.classList.add("show-prism-control");
  showPrismControls(false);
}

function disableAutoTopbar(){
  document.body.classList.remove("auto-topbar","prism-float-control","show-topbar","show-prism-control");
  const btn = document.getElementById("prismControlButton");
  if(btn) btn.remove();
  clearTimeout(prismControlHideTimer);
}


function buildPlugin(plugin, autoplay){
setModeListen();
try{ speechSynthesis.cancel(); }catch(e){}
currentView = "plugin";
currentPlugin = plugin;
const v66FloatControlIds = new Set(["months","numbers","salah-names","salahnames","letters"]);
const wantsFloating = String(plugin.controls || "").toLowerCase() === "floating" || v66FloatControlIds.has(String(plugin.id||"").toLowerCase());
if(wantsFloating) setupAutoTopbar(); else disableAutoTopbar();
document.body.classList.remove("landing-mode");
document.body.classList.add("plugin-stage-mode");
setScreenMode("plugin", plugin.id);
setStageGridMode(true);
const title = plugin.title || plugin.id || "Prism";
setHeader(`✨ ${title} ✨`, subtitleForEngine(plugin));
setTopbar(true,true);

const engine = plugin.engine || "gallery";
if(engine === "directAudio") return buildDirectAudio(plugin, autoplay);
if(engine === "guidedInteraction" || (plugin.effect && plugin.effect.type === "guidedInteraction")) return buildGuidedInteraction(plugin, autoplay);
if(engine === "spotlight") return buildSpotlight(plugin, autoplay);
if(engine === "story") return buildStory(plugin, autoplay || plugin.autoStart === true || plugin.id === "salah-names" || plugin.id === "salahnames");
return buildGallery(plugin, autoplay);
}

function subtitleForEngine(plugin){
if(plugin.engine === "directAudio") return " ";
if(plugin.engine === "spotlight") return " ";
if(plugin.engine === "story") return " ";
return " ";
}

function pluginMp3s(plugin){
const folder = plugin.folder || plugin.id;
const syncFiles = pluginSyncFiles(plugin);
let mp3s = [];
if(Array.isArray(plugin.audios) && plugin.audios.length) mp3s = plugin.audios;
else mp3s = syncFiles.map(f=>f.path).filter(p=>p && p.startsWith(folder + "/") && /\.(mp3|m4a|wav|ogg)$/i.test(p));
if(!mp3s.length && plugin.primaryAudio) mp3s = [plugin.primaryAudio];
return [...new Set(mp3s)];
}

function pluginImages(plugin){
const folder = plugin.folder || plugin.id;
const syncFiles = pluginSyncFiles(plugin);
let imgs = [];
if(Array.isArray(plugin.images) && plugin.images.length) imgs = plugin.images;
else imgs = syncFiles.map(f=>f.path).filter(p=>p && p.startsWith(folder + "/") && /\.(png|jpg|jpeg|webp|gif|svg)$/i.test(p));
if(plugin.backgroundImage && typeof plugin.backgroundImage === "string") imgs.unshift(plugin.backgroundImage);
if(plugin.tileImage && typeof plugin.tileImage === "string") imgs.unshift(plugin.tileImage);
return [...new Set(imgs)];
}



/* ================= FINAL STAGE RENDER HELPERS ================= */
function setStageGridMode(on){
if(on) grid.classList.add("stage-render");
else grid.classList.remove("stage-render");
}

function makeFullVisualStage(className, imagePath){
const stage = document.createElement("div");
stage.className = className + " full-visual";
if(imagePath){
  const bg = document.createElement("div");
  bg.className = "stage-bg-cover";
  bg.style.backgroundImage = `url('${imagePath}')`;
  stage.appendChild(bg);

  const img = document.createElement("img");
  img.className = "stage-img";
  img.src = imagePath;
  img.alt = "";
  img.decoding = "async";
  img.loading = "eager";
  img.onload = ()=>{ if(typeof recalcVisualModuleSoon === "function") recalcVisualModuleSoon(); };
  stage.appendChild(img);
}
return stage;
}

function startStoryAutoSequence(seq, audio){
runStorySequence(seq);
if(audio){
  audio.addEventListener("play",()=>runStorySequence(seq));
}
}

/* ================= PLUGIN EFFECT SYSTEM ================= */
const DEFAULT_MONTHS_FISH_ZONES = [
{id:1,name:"محرّم",x:2.2,y:6.2,w:22.5,h:25.9,mouthX:90,mouthY:56,tailX:2,tailY:35},
{id:2,name:"صفر",x:26.1,y:8.4,w:21.8,h:24.0,mouthX:90,mouthY:56,tailX:2,tailY:35},
{id:3,name:"ربيع الأول",x:49.1,y:8.3,w:22.4,h:24.1,mouthX:90,mouthY:56,tailX:2,tailY:35},
{id:4,name:"ربيع الآخر",x:72.5,y:8.3,w:21.7,h:24.0,mouthX:90,mouthY:56,tailX:2,tailY:35},
{id:5,name:"جمادى الأولى",x:2.7,y:36.1,w:22.7,h:23.8,mouthX:90,mouthY:56,tailX:2,tailY:35},
{id:6,name:"جمادى الآخرة",x:25.9,y:37.4,w:22.4,h:22.9,mouthX:90,mouthY:56,tailX:2,tailY:35},
{id:7,name:"رجب",x:49.5,y:36.8,w:21.6,h:22.9,mouthX:90,mouthY:56,tailX:2,tailY:35},
{id:8,name:"شعبان",x:72.1,y:37.2,w:21.6,h:22.4,mouthX:90,mouthY:56,tailX:2,tailY:35},
{id:9,name:"رمضان",x:2.7,y:63.9,w:22.2,h:23.8,mouthX:90,mouthY:56,tailX:2,tailY:35},
{id:10,name:"شوّال",x:25.8,y:64.4,w:21.6,h:23.5,mouthX:90,mouthY:56,tailX:2,tailY:35},
{id:11,name:"ذو القعدة",x:49.1,y:64.3,w:22.5,h:23.1,mouthX:90,mouthY:56,tailX:2,tailY:35},
{id:12,name:"ذو الحجة",x:71.5,y:64.3,w:22.5,h:23.5,mouthX:90,mouthY:56,tailX:2,tailY:35}
];

function applyPluginEffect(stage, plugin){
const effect = plugin.effect || {};
if(!effect || !effect.type) return;

if(effect.type === "aquarium"){
applyAquariumEffect(stage, effect);
}
}

function applyAquariumEffect(stage, effect){
const layer = document.createElement("div");
layer.className = "aquarium-layer";

// V6.9 RC2: slanted/water-ray overlays disabled.

if(effect.bubbles !== false){
addBubblesToLayer(layer, effect.bubbleCount || 16);
}

if(effect.fishZones !== false){
const zones = Array.isArray(effect.fishCoordinates) && effect.fishCoordinates.length
  ? effect.fishCoordinates
  : DEFAULT_MONTHS_FISH_ZONES;

zones.forEach((fish,index)=>{
const zone = document.createElement("div");
zone.className = "fish-zone";
zone.style.left = fish.x + "%";
zone.style.top = fish.y + "%";
zone.style.width = fish.w + "%";
zone.style.height = fish.h + "%";
zone.style.setProperty("--mouthLeft",(fish.mouthX ?? 90) + "%");
zone.style.setProperty("--mouthTop",(fish.mouthY ?? 56) + "%");
zone.style.setProperty("--tailLeft",(fish.tailX ?? 2) + "%");
zone.style.setProperty("--tailTop",(fish.tailY ?? 35) + "%");
zone.style.setProperty("--d",(index * .45) + "s");
zone.dataset.month = fish.name || "";
layer.appendChild(zone);
});
}

stage.appendChild(layer);
}

function addBubblesToLayer(layer,count){
for(let i=0;i<count;i++){
const b=document.createElement("div");
b.className="bubble";
b.style.left=(5+Math.random()*90)+"%";
b.style.bottom=(5+Math.random()*65)+"%";
b.style.animationDelay=(Math.random()*5)+"s";
b.style.width=b.style.height=(24+Math.random()*36)+"px";
layer.appendChild(b);
}
}

/* ================= DIRECT AUDIO ENGINE ================= */
function buildDirectAudio(plugin, autoplay){
clearGrid();
setStageGridMode(true);
const mp3s = pluginMp3s(plugin);
const audioFile = plugin.primaryAudio || mp3s[0];
const theme = plugin.theme || plugin.id;
if(!audioFile) return renderComingSoon(plugin, `Add audio in ${plugin.folder || plugin.id}/`);

const bg = typeof plugin.backgroundImage === "string" ? plugin.backgroundImage : (typeof plugin.tileImage === "string" ? plugin.tileImage : "");
const stage = makeFullVisualStage("stage", bg);

stage.innerHTML += `<audio src="${audioFile}" preload="auto"></audio>`;

if(plugin.effect && plugin.effect.type){
  applyPluginEffect(stage, plugin);
} else {
  addBubbles(stage,10);
}

grid.appendChild(stage);
ensureStoryImageLoaded(stage, bg);

const audio = stage.querySelector("audio");
const item = {id:plugin.id,card:stage,audio,btn:null,s:{...plugin,file:audioFile,name:plugin.title,eng:""},index:0,type:audio ? "audio" : "visual"};
items.push(item);
map[plugin.id]=item;
stage.onclick = ()=>{ if(audio) play(plugin.id); };
updateFocus();
if(autoplay || params().get("autoplay")==="1") setTimeout(()=>play(plugin.id),400);
}

/* ================= GALLERY ENGINE ================= */
function buildGallery(plugin, autoplay){
clearGrid();
setStageGridMode(false);
const mp3s = pluginMp3s(plugin);
if(!mp3s.length) return renderComingSoon(plugin, `Add audio files in ${plugin.folder || plugin.id}/`);

mp3s.forEach((file,idx)=>{
const clean = normalizeFileName(file).replace(/[-_]/g," ");
const item = {
id:`${plugin.id}-${idx}`,
name:plugin.title,
eng:clean,
emoji:iconFor(plugin.theme || plugin.id),
file,
color:(plugin.tilePalette || DEFAULT_PALETTE)[idx % (plugin.tilePalette || DEFAULT_PALETTE).length],
slices:slicesData[file] || null,
plugin
};
createAudioCard(item,idx,plugin.theme || plugin.id,false);
});
updateFocus();
if(autoplay && items[0]) setTimeout(()=>play(items[0].id),400);
}

/* ================= SPOTLIGHT ENGINE ================= */

function waitForStableVisualStage(wrap){
return new Promise(resolve=>{
  const img = wrap ? wrap.querySelector(".stage-img") : null;
  const finish = ()=>{
    requestAnimationFrame(()=>{
      requestAnimationFrame(()=>{
        requestAnimationFrame(()=>{
          if(typeof recalcVisualModuleSoon === "function") recalcVisualModuleSoon();
          resolve();
        });
      });
    });
  };
  if(!img) return finish();
  if(img.complete && img.naturalWidth > 0){
    if(img.decode){ img.decode().then(finish).catch(finish); }
    else finish();
  }else{
    img.addEventListener("load", ()=>{
      if(img.decode){ img.decode().then(finish).catch(finish); }
      else finish();
    }, {once:true});
  }
});
}


function primeVisualAudio(audio){
try{
  if(!audio) return;
  audio.preload = "auto";
  audio.load();
}catch(e){}
}


/* ================= PRISM V6.4 STRICT VISUAL LIFECYCLE ================= */
function waitImageDecodedAndMeasured(img, host){
return new Promise(resolve=>{
  let done=false;
  const finish=()=>{
    if(done) return;
    requestAnimationFrame(()=>requestAnimationFrame(()=>{
      const r=img ? img.getBoundingClientRect() : {width:100,height:100};
      if(r.width>20 && r.height>20){
        done=true;
        if(host){host.classList.remove("v64-visual-wait");host.classList.add("v64-visual-ready");}
        if(typeof recalcVisualModuleSoon==="function") recalcVisualModuleSoon();
        requestAnimationFrame(()=>{if(typeof recalcVisualModuleSoon==="function") recalcVisualModuleSoon();});
        resolve();
      }else setTimeout(finish,80);
    }));
  };
  if(!img) return finish();
  if(img.complete && img.naturalWidth>0){
    if(img.decode) img.decode().then(finish).catch(finish); else finish();
  }else{
    img.addEventListener("load",()=>{if(img.decode) img.decode().then(finish).catch(finish); else finish();},{once:true});
    img.addEventListener("error",finish,{once:true});
  }
  setTimeout(finish,1800);
});
}
function prepareVisualModuleStrict(wrap, plugin, audio, autoplay){
const img = wrap ? wrap.querySelector(".stage-img") : null;
if(wrap) wrap.classList.add("v64-visual-wait");
return waitImageDecodedAndMeasured(img, wrap).then(()=>{
  if(typeof recalcVisualModuleSoon==="function") recalcVisualModuleSoon();
  if(audio){try{audio.preload="auto";audio.load();}catch(e){}}
  if((autoplay || plugin.autoStart) && audio) setTimeout(()=>play(plugin.id),260);
});
}


/* ================= PRISM V6.6 FINAL NUMBERS AUDIO FALLBACK ================= */
function showNumbersAudioFallback(audio){
try{
  const pid = (currentPlugin && currentPlugin.id) ? String(currentPlugin.id).toLowerCase() : "";
  if(pid !== "numbers") return;
  const existing = document.getElementById("numbersAudioFallback");
  if(existing) existing.remove();

  const overlay = document.createElement("button");
  overlay.id = "numbersAudioFallback";
  overlay.className = "tap-start-overlay numbers-audio-fallback";
  overlay.type = "button";
  overlay.textContent = "▶ Tap to start numbers";
  overlay.onclick = (e)=>{
    e.stopPropagation();
    try{ speechSynthesis.cancel(); }catch(err){}
    try{
      const src = audio.getAttribute("src") || audio.src || "numbers/numbers.mp3";
      if(!audio.src) audio.src = src;
      audio.preload = "auto";
      audio.load();
      audio.play().then(()=>overlay.remove()).catch(()=>{});
    }catch(err){}
  };

  const host = document.querySelector(".spotlight-wrap,.stage,#grid") || document.body;
  host.appendChild(overlay);
}catch(e){}
}

function attachNumbersAudioFallback(audio){
if(!audio || audio.dataset.numbersFallbackAttached === "1") return;
audio.dataset.numbersFallbackAttached = "1";
["error","stalled","suspend","waiting","abort"].forEach(ev=>{
  audio.addEventListener(ev, ()=>{
    const pid = (currentPlugin && currentPlugin.id) ? String(currentPlugin.id).toLowerCase() : "";
    if(pid === "numbers" && audio.paused) setTimeout(()=>showNumbersAudioFallback(audio),700);
  });
});
}


/* ================= PRISM V6.7 VISUAL AUDIO FALLBACK ================= */
function isVisualFallbackPlugin(){
const pid = (currentPlugin && currentPlugin.id) ? String(currentPlugin.id).toLowerCase() : "";
return pid === "numbers" || pid === "months" || pid === "angels" || pid === "pillars";
}

function showVisualAudioFallback(audio){
try{
  if(!audio || !isVisualFallbackPlugin()) return;
  const src = audio.currentSrc || audio.getAttribute("src") || "";
  if(!src.trim()) return;
  if(!audio.paused && !audio.ended) return;

  const existing = document.getElementById("visualAudioFallback");
  if(existing) existing.remove();

  const pid = String(currentPlugin.id).toLowerCase();
  const overlay = document.createElement("button");
  overlay.id = "visualAudioFallback";
  overlay.className = "tap-start-overlay visual-audio-fallback";
  overlay.type = "button";
  overlay.textContent = pid === "numbers" ? "▶ Tap to start numbers" : "▶ Tap to start audio";

  overlay.onclick = (e)=>{
    e.stopPropagation();
    try{ speechSynthesis.cancel(); }catch(err){}
    if(!audio.paused && !audio.ended){
      overlay.remove();
      return;
    }
    try{
      audio.preload = "auto";
      audio.play().then(()=>overlay.remove()).catch(()=>{});
    }catch(err){}
  };

  const host = document.querySelector(".spotlight-wrap,.stage,#grid") || document.body;
  host.appendChild(overlay);
}catch(e){}
}

function attachVisualAudioFallback(audio){
if(!audio || audio.dataset.visualFallbackAttached === "1") return;
audio.dataset.visualFallbackAttached = "1";

audio.addEventListener("playing", ()=>{
  const existing = document.getElementById("visualAudioFallback");
  if(existing) existing.remove();
});

audio.addEventListener("error", ()=>{
  // Audio missing/404 = visual-only mode. Do not show Tap overlay unless playback of an existing audio was blocked.
  const existing = document.getElementById("visualAudioFallback");
  if(existing) existing.remove();
});

audio.addEventListener("ended", ()=>{
  const existing = document.getElementById("visualAudioFallback");
  if(existing) existing.remove();
});
}


/* ================= GUIDED INTERACTION ENGINE (V6.9 FROZEN) =================
   First mode: catchTile for Arabic Alphabets.
   Image/effects work without audio. Cue animations run only when valid audio plays.
*/
function buildGuidedInteraction(plugin, autoplay){
  clearGrid();
  setStageGridMode(true);
  const bg = plugin.backgroundImage || plugin.tileImage || plugin.image || "";
  const mp3s = pluginMp3s(plugin);
  const audioFile = plugin.primaryAudio || mp3s[0] || "";
  const stage = document.createElement("div");
  stage.className = "guided-interaction-stage";
  if(bg){ stage.style.setProperty("--guided-bg-url", `url("${bg}")`); }
  stage.innerHTML = `
    ${bg ? `<div class="stage-bg-cover guided-bg-cover" style="background-image:url('${bg}')"></div><img class="guided-scene" src="${bg}" alt="${plugin.title || "Guided learning"}">` : ""}
    <div class="guided-letter-glow"></div>
    <div class="guided-flying-tile"></div>
    <div class="guided-butterfly" aria-hidden="true">🦋</div>
    ${audioFile ? `<audio src="${audioFile}" preload="auto"></audio>` : ""}
  `;
  grid.appendChild(stage);

  const audio = stage.querySelector("audio");
  const item = {id:plugin.id,card:stage,audio,btn:null,s:{...plugin,file:audioFile,name:plugin.title,eng:"Guided",plugin},index:0,type:audio ? "audio" : "guided"};
  items.push(item); map[plugin.id]=item;
  updateFocus();

  const glow = stage.querySelector(".guided-letter-glow");
  const flyingTile = stage.querySelector(".guided-flying-tile");
  const coordinates = plugin.coordinates || {};
  const letters = Array.isArray(plugin.letters) ? plugin.letters : orderedCoordinateKeys(plugin).map(k=>({key:k,l:(plugin.labels&&plugin.labels[k])||k,...coordinates[k]}));
  const timings = (plugin.coordinateTiming && plugin.coordinateTiming.starts) ? orderedCoordinateKeys(plugin).map(k=>Number(plugin.coordinateTiming.starts[k])) : (Array.isArray(plugin.timings) ? plugin.timings.map(Number) : []);
  const catchPoint = plugin.catchPoint || {x:28,y:56};
  let active = -1;
  let audioUnlocked = true; // V6.9.6: per-letter audio should attempt immediately after card selection
  let currentLetterAudio = null;
  function letterAudioSrc(item){
    if(!item) return "";
    const map = plugin.audioMap || {};
    const key = item.l || item.label || item.key || "";
    const file = item.audio || map[key] || map[item.key] || "";
    if(!file) return "";
    const base = plugin.audioBase || "";
    return /^https?:\/\//i.test(file) ? file : (base ? base.replace(/\/$/,"/") + file.replace(/^\//,"") : file);
  }
  function playLetterAudio(item){
    const src = letterAudioSrc(item);
    if(!src || !audioUnlocked) return;
    try{
      if(currentLetterAudio){ currentLetterAudio.pause(); currentLetterAudio.currentTime = 0; }
      currentLetterAudio = new Audio(src);
      currentLetterAudio.preload = "auto";
      const rate = Number(plugin.audioPlaybackRate || plugin.letterAudioRate || 1);
      if(Number.isFinite(rate) && rate > 0.5 && rate < 1.8) currentLetterAudio.playbackRate = rate;
      currentLetterAudio.play().catch(()=>{});
    }catch(e){}
  }

  function imageToStagePercent(x, y){
    const img = stage.querySelector(".guided-scene");
    const stageRect = stage.getBoundingClientRect();
    if(!img || !stageRect.width || !stageRect.height || !img.naturalWidth || !img.naturalHeight){
      return {x:Number(x), y:Number(y), w:(plugin.tileWidth||8.5), h:(plugin.tileHeight||10)};
    }
    const imageRatio = img.naturalWidth / img.naturalHeight;
    const stageRatio = stageRect.width / stageRect.height;
    let renderW, renderH, offsetX, offsetY;
    if(stageRatio > imageRatio){
      renderH = stageRect.height;
      renderW = renderH * imageRatio;
      offsetX = (stageRect.width - renderW) / 2;
      offsetY = 0;
    }else{
      renderW = stageRect.width;
      renderH = renderW / imageRatio;
      offsetX = 0;
      offsetY = (stageRect.height - renderH) / 2;
    }
    return {
      x: ((offsetX + (Number(x)/100)*renderW) / stageRect.width) * 100,
      y: ((offsetY + (Number(y)/100)*renderH) / stageRect.height) * 100,
      w: ((plugin.tileWidth||8.5) * renderW / stageRect.width),
      h: ((plugin.tileHeight||10) * renderH / stageRect.height)
    };
  }

  function setTileBox(el, x, y){
    const p = imageToStagePercent(x,y);
    el.style.left = p.x + "%";
    el.style.top = p.y + "%";
    el.style.width = p.w + "%";
    el.style.height = p.h + "%";
    return p;
  }

  function playGuidedEffect(index){
    const item = letters[index];
    if(!item) return;
    playLetterAudio(item);
    const x = Number(item.x); const y = Number(item.y);
    if(!Number.isFinite(x) || !Number.isFinite(y)) return;
    const p = setTileBox(glow,x,y);
    setTileBox(flyingTile,x,y);
    flyingTile.textContent = item.l || item.label || item.key || "";
    flyingTile.style.fontSize = "min(5.2vw, 5.9vh)";

    const holdSometimes = index % 4 === 1; // occasional “bunny holds it” backup behaviour
    const bunny = imageToStagePercent((catchPoint && catchPoint.x) || 28, (catchPoint && catchPoint.y) || 56);
    const liftY = Math.max(5, p.y - 7);
    const midX = holdSometimes ? (p.x * .72 + bunny.x * .28) : p.x;
    const midY = holdSometimes ? (p.y * .76 + bunny.y * .24) : liftY;

    glow.animate([
      {opacity:0,transform:"translate(-50%,-50%) scale(.92)"},
      {opacity:1,transform:"translate(-50%,-50%) scale(1.04)"},
      {opacity:.9,transform:"translate(-50%,-50%) scale(1)"},
      {opacity:0,transform:"translate(-50%,-50%) scale(.98)"}
    ],{duration:2600,easing:"ease-in-out",fill:"forwards"});

    flyingTile.animate([
      {opacity:0,left:p.x+"%",top:p.y+"%",transform:"translate(-50%,-50%) scale(.86) rotate(0deg)"},
      {opacity:1,left:p.x+"%",top:p.y+"%",transform:"translate(-50%,-50%) scale(1.04) rotate(-1deg)"},
      {opacity:1,left:midX+"%",top:midY+"%",transform:"translate(-50%,-50%) scale(1.02) rotate(2deg)"},
      {opacity:1,left:midX+"%",top:midY+"%",transform:"translate(-50%,-50%) scale(1.00) rotate(0deg)"},
      {opacity:0,left:p.x+"%",top:p.y+"%",transform:"translate(-50%,-50%) scale(.90) rotate(0deg)"}
    ],{duration:2850,easing:"cubic-bezier(.22,.75,.25,1)",fill:"forwards"});

    stage.classList.remove("bunnyJump");
    void stage.offsetWidth;
    stage.classList.add("bunnyJump");
    setTimeout(()=>{glow.style.opacity=0;stage.classList.remove("bunnyJump");},2900);
  }


  function startGuidedVisualDemo(){
    if(audio || !letters.length) return;
    let i = 0;
    setTimeout(()=>playGuidedEffect(i++), 900);
    setInterval(()=>{
      if(!document.body.contains(stage)) return;
      playGuidedEffect(i++ % letters.length);
    }, 3700);
  }

  window.playArabicLetterEffect = playGuidedEffect;
  if(audio){
    audio.addEventListener("timeupdate",()=>{
      const t = audio.currentTime || 0;
      for(let i=timings.length-1;i>=0;i--){
        if(t >= timings[i] && active !== i){ active = i; playGuidedEffect(i); break; }
      }
    });
    audio.addEventListener("ended",()=>{ active=-1; glow.style.opacity=0; flyingTile.style.opacity=0; });
    stage.onclick=()=>{ audioUnlocked = true; if(audio) play(plugin.id); else playGuidedEffect(active >= 0 ? active : 0); };
    if(audio && (autoplay || plugin.autoStart || plugin.id === "angels" || plugin.id === "pillars")) setTimeout(()=>play(plugin.id),420);
  }else{
    startGuidedVisualDemo();
  }
}

function buildSpotlight(plugin, autoplay){
clearGrid();
const mp3s = pluginMp3s(plugin);
const theme = plugin.theme || plugin.id;

if(plugin && (plugin.id === "angels" || plugin.id === "pillars") && plugin.renderer === "reference-visual"){
  return buildReferenceVisualPlugin(plugin, autoplay);
}

if(plugin.id === "names" && mp3s.length){
document.body.classList.remove("plugin-ui-final");
document.body.classList.remove("plugin-stage-mode");
document.body.classList.add("plugin-names");
setStageGridMode(false);
mp3s.forEach((file,idx)=>{
const label = normalizeFileName(file).replace("_","–");
const item = {
id:`${plugin.id}-${idx}`,
name:"ﷲ",
eng:label,
emoji:"✨",
file,
color:(plugin.tilePalette || DEFAULT_PALETTE)[idx % (plugin.tilePalette || DEFAULT_PALETTE).length],
slices:slicesData[file] || null,
plugin,
learningMode:slicesData[file] ? "slices" : "durationChunks"
};
createAudioCard(item,idx,theme,false);
});
updateFocus();
prewarmVisibleAudios(3);
return;
}

setStageGridMode(true);
const audioFile = plugin.primaryAudio || mp3s[0] || "";
const bg = typeof plugin.backgroundImage === "string" ? plugin.backgroundImage : (typeof plugin.tileImage === "string" ? plugin.tileImage : "");
const effectType = plugin.effect && plugin.effect.type;
const isCoordinateSpotlight = hasCoordinateEffect(plugin);

const wrap = makeFullVisualStage("spotlight-wrap", bg);
if(isCoordinateSpotlight) wrap.classList.add("coordinate-contain-mode");
wrap.innerHTML += `
${isCoordinateSpotlight ? coordinateEffectMarkup(plugin) : `<div class="spotlight-number" id="spotlightText">${iconFor(theme)}</div>`}
${audioFile ? `<audio src="${audioFile}" preload="auto"></audio>` : ""}
`;

// Ambient effects are plugin-selected. These never require index.html edits.
if(plugin.effect){
  applyAmbientEffect(wrap, plugin.effect);
}

grid.appendChild(wrap);
if(typeof recalcVisualModuleSoon === "function") recalcVisualModuleSoon();

const audio = wrap.querySelector("audio");
attachVisualAudioFallback(audio);
primeVisualAudio(audio);
const item = {id:plugin.id,card:wrap,audio,btn:null,s:{...plugin,file:audioFile,name:plugin.title,eng:"Spotlight",plugin,learningMode:plugin.segmentDetection==="silence" ? "silence" : "durationChunks"},index:0,type:audio ? "audio" : "spotlight"};
items.push(item);
map[plugin.id] = item;
wrap.onclick = ()=>{ if(audio) play(plugin.id); else advanceCoordinateSpotlight(plugin); };
updateFocus();

if(isCoordinateSpotlight){
positionCoordinateFocus(plugin,0);
// Always show a gentle demo first. If audio exists and starts, the demo switches to audio-timed focus.
startCoordinateDemo(plugin);
if(audio){
  audio.addEventListener("play",()=>startCoordinateSpotlight(plugin,audio));
  audio.addEventListener("pause",()=>{ if(!audio.ended) startCoordinateDemo(plugin); });
  audio.addEventListener("ended",()=>startCoordinateDemo(plugin));
}
}

if(isCoordinateSpotlight){ prepareVisualModuleStrict(wrap, plugin, audio, autoplay); } else if((autoplay || plugin.autoStart) && audio){ setTimeout(()=>play(plugin.id),400); }
}


/* ================= REFERENCE VISUAL PLUGINS: ANGELS + PILLARS =================
   Uses exact hotspot rectangles from plugin.json. Does not alter Quran, Salah,
   Landing, Months, Numbers, Names, or SalahNames behavior.
*/
let referenceDemoTimer = null;
function stopReferenceDemo(){ if(referenceDemoTimer){ clearInterval(referenceDemoTimer); referenceDemoTimer=null; } }

function buildReferenceVisualPlugin(plugin, autoplay){
  clearGrid();
  setStageGridMode(true);
  const pid = String(plugin.id || "").toLowerCase();
  const bg = typeof plugin.backgroundImage === "string" ? plugin.backgroundImage : (typeof plugin.tileImage === "string" ? plugin.tileImage : "");
  const mp3s = pluginMp3s(plugin);
  const audioFile = plugin.primaryAudio || mp3s[0] || "";
  const keys = orderedCoordinateKeys(plugin);
  const stage = document.createElement("div");
  stage.className = `reference-stage reference-${pid}-stage`;
  const canvas = document.createElement("div");
  canvas.className = `reference-canvas ${pid}-canvas`;
  canvas.innerHTML = `${bg ? `<img class="reference-bg" src="${bg}" alt="${plugin.title || pid}">` : ""}<div class="reference-hotspot-layer"></div><div class="reference-sparkles" id="referenceSparkles"></div>${audioFile ? `<audio src="${audioFile}" preload="auto"></audio>` : ""}`;
  stage.appendChild(canvas);
  grid.appendChild(stage);

  const layer = canvas.querySelector(".reference-hotspot-layer");
  const effectMap = (plugin.effect && plugin.effect.tileEffects) || {};
  const colorMap = referenceColorMap(pid);
  keys.forEach((key, idx)=>{
    const r = plugin.coordinates && plugin.coordinates[key] ? plugin.coordinates[key] : null;
    if(!r) return;
    const hot = document.createElement("div");
    hot.className = `reference-hotspot ${pid === "angels" ? "angel-hotspot" : "pillar-hotspot"}`;
    hot.dataset.key = key;
    hot.dataset.index = String(idx);
    hot.dataset.effect = effectMap[key] || "";
    hot.style.left = Number(r.left ?? r.x ?? 0) + "%";
    hot.style.top = Number(r.top ?? r.y ?? 0) + "%";
    hot.style.width = Number(r.width ?? r.w ?? 12) + "%";
    hot.style.height = Number(r.height ?? r.h ?? 12) + "%";
    hot.style.setProperty("--focus-color", colorMap[key] || "#facc15");
    hot.innerHTML = referenceEffectMarkup(pid, effectMap[key] || "");
    layer.appendChild(hot);
  });

  const audio = canvas.querySelector("audio");
  const item = {id:plugin.id, card:stage, audio, btn:null, s:{...plugin,file:audioFile,name:plugin.title,eng:""}, index:0, type:audio ? "audio" : "reference"};
  items.push(item); map[plugin.id]=item;
  stage.onclick = ()=>{ if(audio) play(plugin.id); };
  updateFocus();
  attachVisualAudioFallback(audio);
  if(audio){
    primeVisualAudio(audio);
    audio.addEventListener("play",()=>startReferenceAudioFocus(plugin,audio,stage));
    audio.addEventListener("pause",()=>{ if(!audio.ended) clearReferenceFocus(stage); });
    audio.addEventListener("ended",()=>{ clearReferenceFocus(stage); stopReferenceDemo(); });
  }
  if(pid === "pillars") startReferenceSparkles(stage);
  startReferenceDemo(plugin,stage);
  if(audio && (autoplay || plugin.autoStart || plugin.id === "angels" || plugin.id === "pillars")) setTimeout(()=>play(plugin.id),420);
}

function referenceEffectMarkup(pid, effect){
  if(pid === "angels"){
    if(effect === "soundWave") return '<div class="reference-effect sound-wave"></div>';
    if(effect === "questionGlow" || effect === "questionPulse") return '<div class="reference-effect question-glow"><span class="floating-question">?</span></div>';
    if(effect === "softRain") return '<div class="reference-effect soft-rain"></div>';
    if(effect === "malikFire" || effect === "fireHeat") return '<div class="reference-effect malik-fire"></div><div class="reference-effect malik-heat"></div>';
    if(effect === "ridwanGlow") return '<div class="reference-effect ridwan-glow"></div><div class="reference-effect ridwan-sparkle"></div>';
    if(effect === "gardenGlow") return '<div class="reference-effect garden-glow"></div>';
    if(effect === "messageLight") return '<div class="reference-effect message-light"></div>';
    if(effect === "writingSpark") return '<div class="reference-effect writing-spark"></div>';
    return '<div class="reference-effect light-ray"></div>';
  }
  return '<div class="reference-effect pillar-glow"></div>';
}

function referenceColorMap(pid){
  if(pid === "angels") return {mikaeel:"#86efac",israfil:"#fde68a",malik:"#fb923c",ridwan:"#bbf7d0",paradise:"#bbf7d0",munkar:"#f97316",jibraeel:"#bae6fd",kiraman:"#67e8f9"};
  // Pillars: match the pillar artwork colors (teal, gold, purple, green, blue).
  return {shahadah:"#14b8a6",salah:"#f59e0b",sawm:"#c084fc",zakat:"#84cc16",hajj:"#38bdf8"};
}

function referenceKeysForPlugin(plugin){ return orderedCoordinateKeys(plugin); }
function referenceIndexForTime(plugin,audio){
  const keys = referenceKeysForPlugin(plugin);
  const startsObj = plugin.coordinateTiming && plugin.coordinateTiming.starts ? plugin.coordinateTiming.starts : null;
  if(startsObj){
    let x = 0;
    for(let i=0;i<keys.length;i++) if((audio.currentTime||0) >= Number(startsObj[keys[i]])) x=i;
    return x;
  }
  const d = audio.duration && isFinite(audio.duration) ? audio.duration : 0;
  return d>0 ? Math.min(keys.length-1, Math.floor((audio.currentTime/d)*keys.length)) : 0;
}

function setReferenceFocus(stage, plugin, index){
  const keys = referenceKeysForPlugin(plugin);
  if(!keys.length) return;
  const activeKey = keys[((index % keys.length)+keys.length)%keys.length];
  const dimOthers = !!(plugin.effect && plugin.effect.dimOthers);
  const now = Date.now();
  stage.querySelectorAll(".reference-hotspot").forEach(el=>{
    const on = el.dataset.key === activeKey;
    el.classList.toggle("active", on);
    el.classList.toggle("dim", !on && dimOthers);
    if(!on) return;

    // Munkar/Nakeer: exactly one red question mark, random position, clipped inside the tile.
    if(el.dataset.effect === "questionGlow"){
      let q = el.querySelector(".floating-question");
      if(!q){
        const fx = el.querySelector(".question-glow") || el;
        q = document.createElement("span");
        q.className = "floating-question";
        q.textContent = "?";
        fx.appendChild(q);
      }
      const last = Number(el.dataset.lastQuestionPop || 0);
      const firstForKey = stage.dataset.lastActiveKey !== activeKey;
      if(firstForKey || now - last > 950){
        q.style.left = (18 + Math.random() * 64) + "%";
        q.style.top = (20 + Math.random() * 54) + "%";
        q.style.animation = "none";
        q.style.setProperty("opacity", "1", "important");
        q.style.setProperty("transform", "translate(-50%,-50%) scale(1.08)", "important");
        void q.offsetWidth;
        q.style.animation = "floatingQuestionPop 1.15s ease-in-out 1";
        clearTimeout(el._munkarQuestionHideTimer);
        el._munkarQuestionHideTimer = setTimeout(()=>{
          try{
            q.style.setProperty("opacity", "0", "important");
            q.style.setProperty("transform", "translate(-50%,-50%) scale(.72)", "important");
          }catch(e){}
        }, 1050);
        el.dataset.lastQuestionPop = String(now);
      }
    }
  });
  stage.dataset.lastActiveKey = activeKey;
}
function clearReferenceFocus(stage){ if(stage) stage.querySelectorAll(".reference-hotspot").forEach(el=>el.classList.remove("active","dim")); }
function startReferenceDemo(plugin, stage){
  stopReferenceDemo();
  const audio = stage ? stage.querySelector("audio") : null;
  if(audio){ clearReferenceFocus(stage); return; }
  // Visual-only mode: effects are allowed to run gently even before audio is added.
  // One active hotspot at a time; no global effects, no full image movement.
  let i = 0;
  setReferenceFocus(stage, plugin, i++);
  referenceDemoTimer = setInterval(()=>{
    if(!stage || !document.body.contains(stage)){ stopReferenceDemo(); return; }
    setReferenceFocus(stage, plugin, i++);
  }, 2400);
}

function startReferenceAudioFocus(plugin,audio,stage){
  stopReferenceDemo();
  setReferenceFocus(stage, plugin, referenceIndexForTime(plugin,audio));
  referenceDemoTimer=setInterval(()=>{
    if(!audio || audio.paused || audio.ended){ stopReferenceDemo(); return; }
    setReferenceFocus(stage, plugin, referenceIndexForTime(plugin,audio));
  },120);
}
function startReferenceSparkles(stage){
  const box = stage.querySelector(".reference-sparkles");
  if(!box || box.dataset.started === "1") return;
  box.dataset.started="1";
  setInterval(()=>{
    if(!document.body.contains(box)) return;
    const s=document.createElement("div");
    s.className="reference-sparkle";
    s.style.left=(8+Math.random()*84)+"%";
    s.style.top=(10+Math.random()*55)+"%";
    box.appendChild(s);
    setTimeout(()=>s.remove(),5000);
  },1000);
}

let coordinateSpotlightTimer = null;
let coordinateDemoIndex = 0;

function stopCoordinateSpotlight(){
if(coordinateSpotlightTimer){
clearInterval(coordinateSpotlightTimer);
coordinateSpotlightTimer = null;
}
}

function hasCoordinateEffect(plugin){
  const effectType = plugin && plugin.effect && plugin.effect.type ? String(plugin.effect.type) : "";
  return !!(plugin && plugin.coordinates && (
    effectType === "coordinateSpotlight" ||
    effectType === "numberSpotlight" ||
    effectType === "timedTileFocus" ||
    effectType === "pillarFocus" ||
    effectType === "softPulse" ||
    effectType === "spotlight" ||
    effectType === "particleField"
  ));
}

function coordinateEffectMarkup(plugin){
  const effectType = prismEffectType(plugin);
  const keys = orderedCoordinateKeys(plugin);
  const labels = plugin.labels || plugin.coordinateLabels || {};
  return `
    <div class="coordinate-focus-dot" id="coordinateFocusDot"></div>
    <div class="coordinate-hotspot-layer" data-effect="${effectType}">
      ${keys.map((key,idx)=>{
        const label = labels[key] || "";
        const tileEffect = plugin.effect && plugin.effect.tileEffects ? (plugin.effect.tileEffects[key] || "") : "";
        return `<div class="coordinate-hotspot" data-key="${key}" data-index="${idx}" data-tile-effect="${tileEffect}"><span>${label}</span></div>`;
      }).join("")}
    </div>`;
}

function orderedCoordinateKeys(plugin){
const coords = plugin.coordinates || {};
const keys = Object.keys(coords);
const startsObj = plugin.coordinateTiming && plugin.coordinateTiming.starts ? plugin.coordinateTiming.starts : null;
if(startsObj){
  return keys.sort((a,b)=>{
    const av = Number(startsObj[a]);
    const bv = Number(startsObj[b]);
    if(Number.isFinite(av) && Number.isFinite(bv)) return av - bv;
    if(Number.isFinite(av)) return -1;
    if(Number.isFinite(bv)) return 1;
    return a.localeCompare(b, undefined, {numeric:true});
  });
}
return keys.sort((a,b)=>a.localeCompare(b, undefined, {numeric:true}));
}

function positionCoordinateFocus(plugin, index){
const dot = document.getElementById("coordinateFocusDot");
if(!dot) return;

const keys = orderedCoordinateKeys(plugin);
if(!keys.length) return;

const key = keys[((index % keys.length) + keys.length) % keys.length];
const p = plugin.coordinates[key];
if(!p) return;

const stage = dot.closest(".spotlight-wrap,.stage");
const img = stage ? stage.querySelector(".stage-img") : null;

if(img && stage){
  if(!img.complete || !img.naturalWidth){
    img.onload = ()=>positionCoordinateFocus(plugin,index);
  }

  const stageRect = stage.getBoundingClientRect();
  const naturalW = img.naturalWidth || 16;
  const naturalH = img.naturalHeight || 9;
  const stageW = stageRect.width;
  const stageH = stageRect.height;

  const imageRatio = naturalW / naturalH;
  const stageRatio = stageW / stageH;

  let renderW, renderH, offsetX, offsetY;

  if(stageRatio > imageRatio){
    renderH = stageH;
    renderW = renderH * imageRatio;
    offsetX = (stageW - renderW) / 2;
    offsetY = 0;
  } else {
    renderW = stageW;
    renderH = renderW / imageRatio;
    offsetX = 0;
    offsetY = (stageH - renderH) / 2;
  }

  const x = offsetX + (Number(p.x) / 100) * renderW;
  const y = offsetY + (Number(p.y) / 100) * renderH;

  dot.style.left = x + "px";
  dot.style.top = y + "px";
} else {
  dot.style.left = p.x + "%";
  dot.style.top = p.y + "%";
}

updateCoordinateHotspots(stage, key, index, p, plugin);

dot.style.opacity = "1";
dot.dataset.index = String(index);
dot.dataset.key = key;
}

function updateCoordinateHotspots(stage, activeKey, activeIndex, point, plugin){
  if(!stage) return;
  const layer = stage.querySelector(".coordinate-hotspot-layer");
  if(!layer) return;
  const hotspots = [...layer.querySelectorAll(".coordinate-hotspot")];
  const img = stage.querySelector(".stage-img");
  let mapper = null;

  if(img && img.naturalWidth && stage){
    const stageRect = stage.getBoundingClientRect();
    const naturalW = img.naturalWidth || 16;
    const naturalH = img.naturalHeight || 9;
    const stageW = stageRect.width;
    const stageH = stageRect.height;
    const imageRatio = naturalW / naturalH;
    const stageRatio = stageW / stageH;
    let renderW, renderH, offsetX, offsetY;
    if(stageRatio > imageRatio){
      renderH = stageH;
      renderW = renderH * imageRatio;
      offsetX = (stageW - renderW) / 2;
      offsetY = 0;
    }else{
      renderW = stageW;
      renderH = renderW / imageRatio;
      offsetX = 0;
      offsetY = (stageH - renderH) / 2;
    }
    mapper = (p)=>({
      x: offsetX + (Number(p.x) / 100) * renderW,
      y: offsetY + (Number(p.y) / 100) * renderH
    });
  }

  hotspots.forEach(h=>{
    const on = h.dataset.key === String(activeKey);
    h.classList.toggle("active", on);
    h.classList.toggle("dimmed", !on && !!(plugin && plugin.effect && plugin.effect.dimOthers));
  });

  const coords = plugin && plugin.coordinates ? plugin.coordinates : {};
  Object.keys(coords).forEach(k=>{
    const h = layer.querySelector(`.coordinate-hotspot[data-key="${CSS.escape(String(k))}"]`);
    const p = coords[k];
    if(h && p){
      if(mapper){
        const xy = mapper(p);
        h.style.left = xy.x + "px";
        h.style.top = xy.y + "px";
      }else{
        h.style.left = (Number(p.x) || 50) + "%";
        h.style.top = (Number(p.y) || 50) + "%";
      }
    }
  });
}



function advanceCoordinateSpotlight(plugin){
coordinateDemoIndex++;
positionCoordinateFocus(plugin, coordinateDemoIndex);
}

function startCoordinateDemo(plugin){
  stopCoordinateSpotlight();
  coordinateDemoIndex = 0;
  // No pre-audio focus. Idle ambience may run, but cue effects wait for real audio/timing.
  const dot = document.getElementById("coordinateFocusDot");
  if(dot) dot.style.opacity = "0";
  document.querySelectorAll(".coordinate-hotspot").forEach(h=>h.classList.remove("active","dimmed"));
}


function startCoordinateSpotlight(plugin,audio){
stopCoordinateSpotlight();
const keys=orderedCoordinateKeys(plugin);
if(!keys.length) return;
const startsObj=plugin.coordinateTiming&&plugin.coordinateTiming.starts?plugin.coordinateTiming.starts:null;
const starts=startsObj?keys.map(k=>Number(startsObj[k])):[];
function valid(){return starts.length===keys.length&&starts.every(v=>Number.isFinite(v));}
function idx(t){
 if(valid()){let x=0;for(let i=0;i<starts.length;i++){if(t>=starts[i]) x=i; else break;}return x;}
 const d=audio.duration&&isFinite(audio.duration)?audio.duration:0;
 return d>0?Math.min(keys.length-1,Math.floor((audio.currentTime/d)*keys.length)):0;
}
positionCoordinateFocus(plugin,idx(audio.currentTime||0));
coordinateSpotlightTimer=setInterval(()=>{
 if(!audio||audio.paused||audio.ended){stopCoordinateSpotlight();return;}
 positionCoordinateFocus(plugin,idx(audio.currentTime||0));
},120);
}

function applyAmbientEffect(stage, effect){
const type = String(effect.type || "");
const layer = document.createElement("div");
layer.className = "prism-ambient-layer";

// Slanted/water-ray overlays are disabled in V6.8.1 because they block the learning image.

if(effect.bubbles || type === "aquarium"){
  addBubblesToLayer(layer, effect.bubbleCount || 16);
}

if(effect.sparkles || type === "pillarFocus" || type === "softPulse" || type === "particleField"){
  addSparklesToLayer(layer, effect.sparkleCount || 18);
}

if(type === "orbit"){
  const orbit = document.createElement("div");
  orbit.className = "prism-orbit-ring";
  layer.appendChild(orbit);
}

stage.appendChild(layer);
}

function addSparklesToLayer(layer,count){
for(let i=0;i<count;i++){
  const s=document.createElement("div");
  s.className="prism-sparkle";
  s.style.left=(5+Math.random()*90)+"%";
  s.style.top=(8+Math.random()*80)+"%";
  s.style.animationDelay=(Math.random()*4)+"s";
  s.style.setProperty("--s", (6+Math.random()*10)+"px");
  layer.appendChild(s);
}
}

/* ================= STORY ENGINE ================= */


/* ================= PRISM V6.6 FINAL STORY IMAGE RETRY ================= */
function ensureStoryImageLoaded(stage,imagePath){
 if(!stage||!imagePath) return;
 const img=stage.querySelector(".stage-img, img");
 if(!img) return;
 let tries=0,maxTries=5;
 const ready=()=>{stage.classList.remove("story-image-waiting");stage.classList.add("story-image-ready");};
 const retry=()=>{
  if(img.complete&&img.naturalWidth>0){ready();return;}
  if(tries>=maxTries){ready();return;}
  tries++;
  const sep=imagePath.includes("?")?"&":"?";
  img.src=imagePath+sep+"retry="+Date.now()+"_"+tries;
 };
 stage.classList.add("story-image-waiting");
 img.addEventListener("load",ready);
 img.addEventListener("error",()=>setTimeout(retry,450));
 [1000,2400,4200,7000].forEach(ms=>setTimeout(retry,ms));
}


function buildStory(plugin, autoplay){
clearGrid();
setStageGridMode(true);
const theme = plugin.theme || plugin.id;
const mp3s = pluginMp3s(plugin);
const audioFile = plugin.primaryAudio || mp3s[0] || "";
const seq = Array.isArray(plugin.sequence) && plugin.sequence.length ? plugin.sequence : defaultSalahNamesSequence();

const bg = typeof plugin.backgroundImage === "string" ? plugin.backgroundImage : (typeof plugin.tileImage === "string" ? plugin.tileImage : "");
const stage = makeFullVisualStage("stage story-stage", bg);

stage.innerHTML += `
<div class="story-overlay story-dawn" id="storyOverlay"></div>
<div class="stage-content">
  <div class="stage-title" id="storyArabic">${seq[0].arabic || ""}</div>
  <div class="stage-sub" id="storyName">${seq[0].name || plugin.title}</div>
  <div class="stage-badges" id="storyBadges"></div>
</div>
${audioFile ? `<audio src="${audioFile}" preload="auto"></audio>` : ""}
`;
grid.appendChild(stage);
ensureStoryImageLoaded(stage, bg);

const audio = stage.querySelector("audio");
const item = {id:plugin.id,card:stage,audio,btn:null,s:{...plugin,file:audioFile,name:plugin.title,eng:"Story",plugin,learningMode:plugin.segmentDetection==="silence" ? "silence" : "durationChunks"},index:0,type:audio ? "audio" : "story"};
items.push(item);
map[plugin.id] = item;
stage.onclick = ()=>{ if(audio) play(plugin.id); else runStorySequence(seq,null); };
updateFocus();
renderStoryStep(seq[0]);
if(audio){
  audio.addEventListener("play",()=>runStorySequence(seq,audio));
  audio.addEventListener("loadedmetadata",()=>{ if(!audio.paused) runStorySequence(seq,audio); });
  audio.addEventListener("canplay",()=>{ if(!audio.paused) runStorySequence(seq,audio); });
} else {
  runStorySequence(seq,null);
}
if(autoplay){ setTimeout(()=>{ if(audio) play(plugin.id); },400); }
}

function defaultSalahNamesSequence(){
return [
{name:"Fajr",arabic:"الفجر",effect:"dawn",rakah:["2 Sunnah","2 Fard"]},
{name:"Dhuhr",arabic:"الظهر",effect:"bright-day",rakah:["4 Sunnah","4 Fard","2 Sunnah"]},
{name:"Asr",arabic:"العصر",effect:"afternoon",rakah:["4 Fard"]},
{name:"Maghrib",arabic:"المغرب",effect:"sunset",rakah:["3 Fard","2 Sunnah"]},
{name:"Isha",arabic:"العشاء",effect:"night",rakah:["4 Fard","2 Sunnah","Witr"]}
];
}

function runStorySequence(seq,audio){
clearTimeout(storyTimer);
if(!seq || !seq.length) return;

let lastIndex = -1;

const tick = ()=>{
  if(!audio){
    return;
  }

  if(!audio.duration || !isFinite(audio.duration)){
    storyTimer = setTimeout(tick,120);
    return;
  }

  const i = Math.min(seq.length - 1, Math.floor((audio.currentTime / audio.duration) * seq.length));

  if(i !== lastIndex){
    lastIndex = i;
    renderStoryStep(seq[i]);
  }

  if(!audio.paused && !audio.ended){
    storyTimer = setTimeout(tick,160);
  }
};

if(audio){
  renderStoryStep(seq[0]);
  tick();
  return;
}

let i=0;
const step=()=>{
  renderStoryStep(seq[i % seq.length]);
  i++;
  storyTimer = setTimeout(step,4200);
};
step();
}

function renderStoryStep(step){
const overlay = document.getElementById("storyOverlay");
const arabic = document.getElementById("storyArabic");
const name = document.getElementById("storyName");
const badges = document.getElementById("storyBadges");
if(!overlay || !arabic || !name || !badges) return;
overlay.className = "story-overlay story-" + (step.effect || "dawn");
arabic.innerText = step.arabic || "";
name.innerText = step.name || "";
badges.innerHTML = (step.rakah || []).map(r=>`<div class="rakah-badge">${r}</div>`).join("");
}

/* ================= CARD RENDERER ================= */

function prewarmVisibleAudios(limit=24){
try{
  items.slice(0,limit).forEach(item=>{
    if(item.audio){
      item.audio.preload = "auto";
      item.audio.load();
    }
  });
}catch(e){}
}


function attachAudioRecovery(audio, item){
if(!audio || audio.dataset.recoveryAttached === "1") return;
audio.dataset.recoveryAttached = "1";
let recovering = false;
let lastGoodTime = 0;

audio.addEventListener("timeupdate", ()=>{
  if(!audio.paused && !audio.seeking && isFinite(audio.currentTime)){
    lastGoodTime = audio.currentTime;
  }
});

const recover = ()=>{
  if(recovering || audio.ended) return;
  recovering = true;
  const wasCurrent = currentAudio === audio;
  const resumeAt = isFinite(audio.currentTime) ? audio.currentTime : lastGoodTime;

  const tryResume = ()=>{
    if(!wasCurrent || currentAudio !== audio || audio.ended){
      recovering = false;
      return;
    }
    try{
      if(isFinite(resumeAt) && resumeAt > 0 && Math.abs(audio.currentTime - resumeAt) > 1.2){
        audio.currentTime = resumeAt;
      }
    }catch(e){}
    audio.play().catch(()=>{});
    recovering = false;
  };

  if(audio.readyState >= 3){
    setTimeout(tryResume, 250);
  }else{
    const once = ()=>setTimeout(tryResume, 200);
    audio.addEventListener("canplay", once, {once:true});
    audio.addEventListener("canplaythrough", once, {once:true});
    setTimeout(tryResume, 1800);
  }
};

["waiting","stalled","suspend","emptied","abort"].forEach(ev=>{
  audio.addEventListener(ev, recover);
});

audio.addEventListener("error", ()=>{
  if(currentAudio !== audio) return;
  const src = audio.getAttribute("src");
  const resumeAt = audio.currentTime || lastGoodTime || 0;
  setTimeout(()=>{
    try{
      audio.src = src;
      audio.load();
      audio.addEventListener("canplay", ()=>{
        try{ audio.currentTime = resumeAt; }catch(e){}
        audio.play().catch(()=>{});
      }, {once:true});
    }catch(e){}
  }, 800);
});
}

function createAudioCard(s, idx, theme, showLearnBadge){
const card = document.createElement("div");
card.className = "card";
card.style.background = s.color || gradientFor(theme);

const badge = showLearnBadge ? `<div class="badge">🎓</div>` : "";

card.innerHTML = `
${badge}
<div class="card-info-box">
  <div class="emoji">${s.emoji || iconFor(theme)}</div>
  <div class="arabic">${s.name || s.eng}</div>
  <div class="english">${s.eng || s.file}</div>
</div>
<button class="play-btn">⏸ Pause</button>
<audio src="${s.file}" preload="auto"></audio>
`;

const btn = card.querySelector("button");
const audio = card.querySelector("audio");

const item = {card,audio,btn,s,index:idx,id:s.id,type:"audio"};
attachAudioRecovery(audio, item);
items.push(item);
map[s.id] = item;

card.onclick = ()=>{
TV.focused = idx;
updateFocus();
play(s.id);
};

btn.onclick = (e)=>{
e.stopPropagation();
pauseCurrent();
};

grid.appendChild(card);
}

function renderComingSoon(plugin,msg){
setStageGridMode(false);
const theme = plugin.theme || plugin.id;
const card = document.createElement("div");
card.className = "card";
applyImageOrGradient(card, plugin.tileImage, theme);
card.innerHTML = `
<div class="card-info-box">
  <div class="emoji">${iconFor(theme)}</div>
  <div class="arabic">${plugin.title || plugin.id}</div>
  <div class="english">Coming Soon</div>
</div>
<button class="play-btn">✨ Coming Soon</button>
`;
grid.appendChild(card);
const item = {id:plugin.id,card,index:0,type:"empty"};
items.push(item);
map[plugin.id]=item;
updateFocus();
}

/* ================= AUDIO + LEARN/HIFZ ================= */

function showTapToStartOverlay(audio, label){
try{
  const existing = document.getElementById("tapStartOverlay");
  if(existing) existing.remove();
  const overlay = document.createElement("button");
  overlay.id = "tapStartOverlay";
  overlay.className = "tap-start-overlay";
  overlay.textContent = label || "▶ Tap to start";
  overlay.onclick = (e)=>{
    e.stopPropagation();
    try{ speechSynthesis.cancel(); }catch(err){}
    audio.play().then(()=>overlay.remove()).catch(()=>{});
  };
  const host = document.querySelector(".spotlight-wrap,.stage,main,#grid") || document.body;
  host.appendChild(overlay);
}catch(e){}
}

function play(id){
const item = map[id];
if(!item || item.type !== "audio") return;

sequenceCancelToken++;
const token = sequenceCancelToken;

if(currentAudio && currentAudio !== item.audio){
currentAudio.pause();
currentAudio.currentTime = 0;
if(currentCard) currentCard.classList.remove("active-tile");
}

currentAudio = item.audio;
currentButton = item.btn;
currentCard = item.card;
currentItem = item;

item.card.classList.add("active-tile");

const mode = getMode();
const hasSlices = Array.isArray(item.s.slices) && item.s.slices.length;

if((mode === "repeat5" || mode === "hifz") && (hasSlices || item.s.learningMode === "durationChunks" || item.s.learningMode === "silence")){
runLearningSequence(item, mode, token);
} else {
item.audio.loop = loopAll;
item.audio.play().catch(()=>{
  if(isVisualFallbackPlugin()) showVisualAudioFallback(item.audio);
  else if(shouldSuppressSpeech(item)) showTapToStartOverlay(item.audio, "▶ Tap to start audio");
});
}

if(!shouldSuppressSpeech(item)){
  if(!shouldSuppressSpeech(item)){
  speak(item.s.name || item.s.eng || "");
} else {
  try{ speechSynthesis.cancel(); }catch(e){}
}
} else {
  try{ speechSynthesis.cancel(); }catch(e){}
}
}

function toSeconds(t){
if(typeof t === "number") return t;
const parts = String(t).split(":").map(Number);
if(parts.length === 2) return parts[0]*60 + parts[1];
if(parts.length === 3) return parts[0]*3600 + parts[1]*60 + parts[2];
return Number(t) || 0;
}

function getChunkEnds(item){
if(Array.isArray(item.s.slices) && item.s.slices.length) return item.s.slices.map(toSeconds);

const audio = item.audio;
const duration = audio.duration || 0;
if(!duration || !isFinite(duration)) return [];

const plugin = item.s.plugin || currentPlugin || {};
const chunkMode = plugin.chunkMode || item.s.chunkMode || {};
let chunks = chunkMode.chunkCount || 0;

if(!chunks && plugin.id === "names"){
const label = item.s.eng || "";
const nums = label.match(/\d+/g) || [];
if(nums.length >= 2){
const count = Math.abs(Number(nums[1]) - Number(nums[0])) + 1;
chunks = Math.max(1, Math.ceil(count / (chunkMode.namesPerChunk || 4)));
}
}

if(!chunks && Array.isArray(plugin.spotlight?.items)) chunks = plugin.spotlight.items.length;
if(!chunks && Array.isArray(plugin.sequence)) chunks = plugin.sequence.length;
if(!chunks) chunks = 5;

const step = duration / chunks;
return Array.from({length:chunks},(_,i)=>Math.min(duration, step*(i+1)));
}

function playSegment(audio,start,end,token){
return new Promise(resolve=>{
  if(token !== sequenceCancelToken) return resolve();
  start = Math.max(0, Number(start) || 0);
  end = Math.max(start + 0.25, Number(end) || start + 0.25);
  const expectedMs = Math.max(350, (end - start) * 1000);
  let done = false;
  let watchdog = null;
  let tick = null;
  const cleanup = ()=>{
    if(watchdog) clearTimeout(watchdog);
    if(tick) clearInterval(tick);
    audio.removeEventListener("ended", finish);
    audio.removeEventListener("error", finish);
  };
  const finish = ()=>{
    if(done) return;
    done = true;
    cleanup();
    try{ audio.pause(); }catch(e){}
    resolve();
  };
  const check = ()=>{
    if(done) return;
    if(token !== sequenceCancelToken) return finish();
    const t = Number(audio.currentTime || 0);
    if(t >= end - 0.035) return finish();
  };
  const begin = ()=>{
    if(done || token !== sequenceCancelToken) return finish();
    audio.loop = false;
    audio.addEventListener("ended", finish);
    audio.addEventListener("error", finish);
    tick = setInterval(check, 35);
    watchdog = setTimeout(finish, expectedMs + 900);
    const p = audio.play();
    if(p && p.catch) p.catch(()=>finish());
  };
  const seekAndBegin = ()=>{
    try{ audio.pause(); }catch(e){}
    const onSeeked = ()=>{ audio.removeEventListener("seeked", onSeeked); setTimeout(begin, 40); };
    audio.addEventListener("seeked", onSeeked, {once:true});
    try{ audio.currentTime = start; }catch(e){ audio.removeEventListener("seeked", onSeeked); }
    setTimeout(()=>{ if(!done) begin(); }, 220);
  };
  seekAndBegin();
});
}

async function runLearningSequence(item, mode, token){
attachAudioRecovery(item.audio, item); // V62 recovery
const audio = item.audio;
try{ item.audio.preload = "auto"; }catch(e){}

if(!audio.duration || !isFinite(audio.duration)){
await new Promise(resolve=>{
  let done=false;
  const finish=()=>{ if(done) return; done=true; resolve(); };
  audio.addEventListener("loadedmetadata",finish,{once:true});
  audio.addEventListener("canplay",finish,{once:true});
  setTimeout(finish, 1200);
});
}

const ends = getChunkEnds(item);
if(!ends.length){
audio.play().catch(()=>{});
return;
}

const starts = [0].concat(ends.slice(0,-1));
const repeat = ((item.s.plugin || currentPlugin || {}).chunkMode || {}).repeatCount || 5;

if(mode === "repeat5"){
for(let i=0;i<ends.length;i++){
for(let r=0;r<repeat;r++){
if(token !== sequenceCancelToken) return;
await playSegment(audio,starts[i],ends[i],token);
await sleep(250);
}
}
}

if(mode === "hifz"){
for(let upto=0;upto<ends.length;upto++){
for(let r=0;r<repeat;r++){
if(token !== sequenceCancelToken) return;
await playSegment(audio,starts[upto],ends[upto],token);
await sleep(250);
}
if(token !== sequenceCancelToken) return;
await playSegment(audio,0,ends[upto],token);
await sleep(450);
}
}

if(loopAll && token === sequenceCancelToken){
runLearningSequence(item,mode,token);
}
}

function sleep(ms){return new Promise(r=>setTimeout(r,ms));}

function pauseCurrent(){
sequenceCancelToken++;
if(currentAudio){ currentAudio.pause(); }
}

function stopAll(clearFocus=true){
sequenceCancelToken++;
if(currentAudio){
currentAudio.pause();
currentAudio.currentTime = 0;
}
if(currentCard) currentCard.classList.remove("active-tile");
currentAudio = null;
currentButton = null;
currentCard = null;
currentItem = null;
if(clearFocus) updateFocus();
}

function toggleLoopAll(){
loopAll = !loopAll;
document.getElementById("loopBtn").innerText = loopAll ? "🔁 Loop ON" : "🔁 Loop OFF";
if(currentAudio) currentAudio.loop = loopAll;
}

document.getElementById("mode").addEventListener("change",()=>{
stopAll(false);
if(currentView === "quran") renderQuranItems();
if(currentView === "plugin" && currentPlugin){
  if(currentPlugin.id !== "names") setModeListen();
  updateFocus();
}
});

/* ================= NAVIGATION ================= */
function move(dir){
const max = items.length - 1;
if(max < 0) return;

if(dir === "right") TV.focused = Math.min(max,TV.focused + 1);
if(dir === "left") TV.focused = Math.max(0,TV.focused - 1);
if(dir === "down") TV.focused = Math.min(max,TV.focused + TV.cols);
if(dir === "up") TV.focused = Math.max(0,TV.focused - TV.cols);

updateFocus();
}

function updateFocus(){
const cards = document.querySelectorAll(".card,.stage,.spotlight-wrap");
cards.forEach(c=>c.classList.remove("remote-focus"));

const el = cards[TV.focused];
if(!el) return;

el.classList.add("remote-focus");

if(TV.isTV){
el.scrollIntoView({block:"center",behavior:"smooth"});
}
}

function handleOverlayDismissal(){
const overlay = document.getElementById("overlay");
if(overlay && overlay.style.display !== "none"){
const id = params().get("id") || params().get("surah");
if(id && map[id]) play(id);
overlay.style.display = "none";
return true;
}
return false;
}

document.addEventListener("keydown",(e)=>{
const k = e.keyCode;
const activeEl = document.activeElement;

if(document.getElementById("overlay").style.display === "flex"){
const okKeys = ["Enter","NumpadEnter"," ","OK","Select"];
if(k===23 || k===66 || okKeys.includes(e.key) || k===19 || k===20 || k===21 || k===22 || (e.key||"").startsWith("Arrow")){
e.preventDefault();
handleOverlayDismissal();
return;
}
}

if(activeEl && (activeEl.id==="btnBack" || activeEl.id==="btnStop" || activeEl.id==="loopBtn" || activeEl.id==="mode")){
if(k===22 || e.key==="ArrowRight"){
e.preventDefault();
if(activeEl.id==="btnBack") document.getElementById("btnStop").focus();
else if(activeEl.id==="btnStop") document.getElementById("loopBtn").focus();
else if(activeEl.id==="loopBtn") document.getElementById("mode").focus();
}
if(k===21 || e.key==="ArrowLeft"){
e.preventDefault();
if(activeEl.id==="btnStop" && document.getElementById("btnBack").style.display !== "none") document.getElementById("btnBack").focus();
else if(activeEl.id==="loopBtn") document.getElementById("btnStop").focus();
else if(activeEl.id==="mode") document.getElementById("loopBtn").focus();
}
if(k===20 || e.key==="ArrowDown"){
e.preventDefault();
activeEl.blur();
updateFocus();
}
return;
}

if(k===21 || e.key==="ArrowLeft"){
e.preventDefault();
if(TV.focused % TV.cols === 0) return;
move("left");
}

if(k===22 || e.key==="ArrowRight"){
e.preventDefault();
if((TV.focused + 1) % TV.cols === 0) return;
move("right");
}

if(k===20 || e.key==="ArrowDown"){
e.preventDefault();
move("down");
}

if(k===19 || e.key==="ArrowUp"){
e.preventDefault();
if(TV.focused < TV.cols && currentView !== "landing"){
const cards = document.querySelectorAll(".card,.stage,.spotlight-wrap");
cards.forEach(c=>c.classList.remove("remote-focus"));
const first = document.getElementById("btnBack").style.display !== "none" ? "btnBack" : "btnStop";
document.getElementById(first).focus();
} else {
move("up");
}
}

const okKeys = ["Enter","NumpadEnter"," ","OK","Select"];
if(k===23 || k===66 || okKeys.includes(e.key)){
if(activeEl && !activeEl.closest(".topbar")){
e.preventDefault();
const item = items[TV.focused];
if(!item) return;
if(item.type === "landing") openMenuEntry(item.entry);
else if(item.type === "audio") play(item.id);
else if(item.type === "spotlight" && currentPlugin) { advanceCoordinateSpotlight(currentPlugin); }
else if(item.type === "story") runStorySequence(defaultSalahNamesSequence());
}
}
});

/* ================= BACKGROUND PARTICLES ================= */
const starsContainer = document.querySelector(".stars");
for(let i=0;i<80;i++){
const d = document.createElement("div");
d.className = "star";
d.style.left = Math.random()*100 + "%";
d.style.top = Math.random()*100 + "%";
starsContainer.appendChild(d);
}
for(let i=0;i<40;i++){
const s = document.createElement("div");
s.className = "sparkle";
s.style.left = Math.random()*100 + "%";
s.style.top = Math.random()*100 + "%";
starsContainer.appendChild(s);
}

/* ================= QR LINK HANDLING ================= */
function qrFix(){
const id = params().get("id") || params().get("surah");

if(!id || !map[id]){
updateFocus();
return;
}

const item = map[id];
TV.focused = item.index;

setTimeout(()=>{
item.card.classList.add("active-tile");
item.card.scrollIntoView({behavior:"smooth",block:"center"});

currentCard = item.card;
currentAudio = item.audio;
currentButton = item.btn;
currentItem = item;

document.getElementById("overlay").style.display = "flex";
setTimeout(()=>speak(item.s.name),600);
updateFocus();
},500);
}

document.getElementById("overlay").onclick = ()=>handleOverlayDismissal();

window.addEventListener("popstate",()=>route());


async function fetchPluginConfigById(pluginId){
  const legacyMenu = Array.isArray(menuData) ? menuData : [];
  return await loadOnePlugin(pluginId, legacyMenu);
}

async function route(){
const legacyMenuData = await fetchFirstJson(["menu.json","../menu.json"], FALLBACK_MENU);
menuData = await loadPluginHostMenu(legacyMenuData);
slicesData = await fetchFirstJson(["slices.json","../slices.json","learn.master.slices.json","../learn.master.slices.json"], {});
if(slicesData && slicesData.QURAN){
  const flat = {};
  Object.values(slicesData.QURAN).forEach(entry=>{
    if(entry && entry.file && Array.isArray(entry.slices)) flat[entry.file] = entry.slices;
  });
  slicesData = flat;
}

const p = params();
const id = p.get("id") || p.get("surah");
const pluginId = p.get("plugin");

if(id){
buildQuran();
return;
}

if(pluginId && pluginId !== "landing"){
let foundEntry = menuData.find(m => (m.plugin||{}).id === pluginId);
if(!foundEntry) foundEntry = await fetchPluginConfigById(pluginId);
const found = foundEntry ? foundEntry.plugin : null;
if(pluginId === "quran") buildQuran();
else if(pluginId === "salah" && foundEntry) buildExternalBridge(foundEntry);
else if(found) buildPlugin(found, p.get("autoplay") === "1");
else buildLanding();
return;
}

buildLanding();
}

window.addEventListener("load",route);

window.addEventListener("resize",()=>{
const dot = document.getElementById("coordinateFocusDot");
if(dot && currentPlugin && currentPlugin.coordinates){
  const idx = Number(dot.dataset.index || 0);
  setTimeout(()=>positionCoordinateFocus(currentPlugin, idx),80);
}
});


/* ================= V5 VISUAL FIRST-LOAD STABILITY ================= */
function recalcVisualModuleSoon(){
  try{ setPrismViewportHeight(); }catch(e){}
  const run = ()=>{
    const dot = document.getElementById("coordinateFocusDot");
    if(dot && currentPlugin && currentPlugin.coordinates){
      const idx = Number(dot.dataset.index || 0);
      try{ positionCoordinateFocus(currentPlugin, idx); }catch(e){}
    }
  };
  requestAnimationFrame(run);
  requestAnimationFrame(()=>run());
  requestAnimationFrame(()=>requestAnimationFrame(run));
  [80,180,320,650,1000,1600,2400].forEach(ms=>{
    setTimeout(()=>{
      try{ setPrismViewportHeight(); }catch(e){}
      run();
    }, ms);
  });
}
window.addEventListener("orientationchange", ()=>{
  setTimeout(recalcVisualModuleSoon,80);
  setTimeout(recalcVisualModuleSoon,360);
  setTimeout(recalcVisualModuleSoon,900);
});
window.addEventListener("resize", recalcVisualModuleSoon);
window.addEventListener("load", recalcVisualModuleSoon);


/* ================= V6 BACKGROUND AUDIO CACHE ================= */
const PrismAudioCache = {
  done:false,
  queue:[],
  key:"prism-audio-cache-v6",
  collect(){
    const paths = new Set();
    try{
      (menuData || []).forEach(entry=>{
        const p = entry.plugin || {};
        const title = String(entry.title || p.title || "").toLowerCase();
        const isTarget = p.id === "quran" || p.id === "names" || title.includes("quran") || title.includes("allah");
        if(!isTarget) return;
        (entry.syncFiles || []).forEach(f=>{
          const path = f.path || "";
          if(/\.(mp3|m4a|wav|ogg)$/i.test(path)) paths.add(path);
        });
        (p.audios || []).forEach(a=>paths.add(a));
        if(p.primaryAudio) paths.add(p.primaryAudio);
      });
    }catch(e){}
    return [...paths].sort((a,b)=>{
      const aa = a.includes("55.mp3") ? 1 : 0;
      const bb = b.includes("55.mp3") ? 1 : 0;
      return aa-bb || a.localeCompare(b);
    });
  },
  start(){
    if(this.done) return;
    this.done = true;
    this.queue = this.collect();
    const run = async ()=>{
      if(!this.queue.length) return;
      const path = this.queue.shift();
      try{
        if("caches" in window){
          const c = await caches.open(this.key);
          const hit = await c.match(path);
          if(!hit){
            const res = await fetch(path, {cache:"force-cache"});
            if(res && res.ok) await c.put(path, res.clone());
          }
        } else {
          const a = new Audio(path);
          a.preload = "auto";
          a.load();
        }
      }catch(e){}
      setTimeout(run, 220);
    };
    setTimeout(run, 800);
  }
};

