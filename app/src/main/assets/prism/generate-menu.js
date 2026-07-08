/**
 * Prism V3.4 FINAL generate-menu.js
 * Repo: AmtunNoor/Quran
 *
 * Purpose:
 * - Never manually edit menu.json again.
 * - Auto-discover plugin folders, images, audio, slices, and engine metadata.
 * - Preserve existing Quran root audio + QR behavior.
 * - Keep Salah opening external SalahSteps URL.
 * - Support existing repo naming AND future standard names.
 
 */

const fs = require("fs");
const path = require("path");

const REPO_RAW = "https://raw.githubusercontent.com/AmtunNoor/Quran/main";
const LOCAL_BASE = "file:///data/user/0/com.noor.prism/files";

const ROOT = __dirname;
const MENU_FILE = path.join(ROOT, "menu.json");
const PLUGINS_DIR = path.join(ROOT, "plugins");

const MASTER_SLICES = "learn.master.slices.json";
const LEGACY_SLICES = "slices.json";

const IMAGE_EXTS = [".webp", ".png", ".jpg", ".jpeg", ".gif", ".svg"];
const AUDIO_EXTS = [".mp3", ".m4a", ".wav", ".ogg"];

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

const FALLBACK_TILE = {
  type: "gradient",
  style: "colorful",
  colors: ["#ff9a9e", "#fad0c4", "#fbc2eb", "#a18cd1"],
  icon: "✨"
};

const FALLBACK_BACKGROUND = {
  type: "gradient",
  style: "soft",
  colors: ["#fff1eb", "#ace0f9", "#fbc2eb"]
};

function exists(p) {
  return fs.existsSync(path.join(ROOT, p));
}

function existsAbs(p) {
  return fs.existsSync(p);
}

function readJson(relPath, fallback) {
  try {
    if (!exists(relPath)) return fallback;
    return JSON.parse(fs.readFileSync(path.join(ROOT, relPath), "utf8"));
  } catch (err) {
    console.warn(`⚠️ Could not read JSON: ${relPath}`);
    return fallback;
  }
}

function writeJson(absPath, data) {
  fs.writeFileSync(absPath, JSON.stringify(data, null, 2));
}

function ext(file) {
  return path.extname(file).toLowerCase();
}

function isImage(file) {
  return IMAGE_EXTS.includes(ext(file));
}

function isAudio(file) {
  return AUDIO_EXTS.includes(ext(file));
}

function repoUrl(relPath) {
  return `${REPO_RAW}/${relPath}`;
}

function walkDir(relDir, baseRel = relDir) {
  const absDir = path.join(ROOT, relDir);
  const baseAbs = path.join(ROOT, baseRel);
  if (!existsAbs(absDir)) return [];

  let out = [];

  for (const item of fs.readdirSync(absDir)) {
    if (item === ".DS_Store") continue;
    const full = path.join(absDir, item);
    const stat = fs.statSync(full);

    if (stat.isDirectory()) {
      out = out.concat(walkDir(path.relative(ROOT, full).replace(/\\/g, "/"), baseRel));
    } else {
      out.push(path.relative(baseAbs, full).replace(/\\/g, "/"));
    }
  }

  return out.sort((a, b) => a.localeCompare(b, undefined, { numeric: true }));
}

function folderFiles(folder) {
  if (!folder || !exists(folder)) return [];
  return walkDir(folder).map(f => `${folder}/${f}`);
}

function imageFiles(folder) {
  return folderFiles(folder).filter(isImage);
}

function audioFiles(folder) {
  return folderFiles(folder).filter(isAudio);
}

function addUniqueSync(syncFiles, relPath, localPath = relPath) {
  if (!relPath || !exists(relPath)) return;
  const item = { url: repoUrl(relPath), path: localPath };
  const key = `${item.url}|${item.path}`;
  if (!syncFiles.some(x => `${x.url}|${x.path}` === key)) syncFiles.push(item);
}

function addFolderSync(syncFiles, folder) {
  folderFiles(folder).forEach(f => addUniqueSync(syncFiles, f, f));
}

function pluginEntryToId(entry) {
  if (typeof entry === "string") return entry.trim();
  if (entry && typeof entry === "object") return String(entry.id || entry.plugin || entry.folder || entry.name || "").trim();
  return "";
}

function loadPlugins() {
  if (!exists("plugins")) return [];

  // V6.9: plugins/plugins.json is the registry. Root-level *.plugin.json files
  // are legacy and are intentionally ignored because stale placeholder files
  // caused broken menu.json generation.
  let ids = [];
  const registry = readJson("plugins/plugins.json", null);
  if (Array.isArray(registry)) ids = registry.map(pluginEntryToId).filter(Boolean);
  else if (registry && Array.isArray(registry.plugins)) ids = registry.plugins.map(pluginEntryToId).filter(Boolean);

  if (!ids.length) {
    ids = fs.readdirSync(PLUGINS_DIR, { withFileTypes: true })
      .filter(d => d.isDirectory())
      .map(d => d.name)
      .filter(id => exists(`plugins/${id}/plugin.json`));
  }

  const seen = new Set();
  return ids
    .filter(id => {
      if (!id || seen.has(id)) return false;
      seen.add(id);
      return true;
    })
    .map(id => {
      const rel = `plugins/${id}/plugin.json`;
      const data = readJson(rel, null);
      if (data) data.__pluginFile = rel;
      return data;
    })
    .filter(Boolean)
    .filter(p => p.enabled !== false)
    .sort((a, b) => (a.priority || 999) - (b.priority || 999));
}

/**
 * Folder aliases for your current repo.
 * Supports old names and future standard folder names.
 */
function resolveFolder(plugin) {
  if (plugin.folder && exists(plugin.folder)) return plugin.folder;

  const id = plugin.id || "";
  const title = (plugin.title || "").toLowerCase().replace(/[^a-z0-9]+/g, "");

  const candidates = [
    id,
    id.replace(/-/g, ""),
    id.replace(/_/g, ""),
    `${id}s`,
    title,
    title.replace(/-/g, ""),
    title.replace(/_/g, "")
  ];

  const aliases = {
    dua: ["duas", "dua"],
    duas: ["duas", "dua"],
    months: ["months"],
    names: ["names"],
    numbers: ["numbers"],
    salahnames: ["salahnames", "salah-names", "salah_names"],
    "salah-names": ["salahnames", "salah-names", "salah_names"],
    salah: ["salah"],
    quran: ["quran"]
  };

  const all = [...(aliases[id] || []), ...candidates];

  for (const c of all) {
    if (c && exists(c)) return c;
  }

  return plugin.folder || id;
}

function scoreTileImage(file, folder, plugin) {
  const name = path.basename(file).toLowerCase();
  const folderLower = folder.toLowerCase();
  const id = (plugin.id || "").toLowerCase();

  if (/^tile\.(webp|png|jpg|jpeg|gif|svg)$/.test(name)) return 100;
  if (name.includes("tile")) return 95;
  if (name.includes("_icon") || name.includes("-icon")) return 90;
  if (name.includes("icon")) return 85;
  if (name.includes("cover")) return 80;
  if (name.includes("card")) return 75;
  if (name.includes("logo")) return 70;
  if (name.includes(folderLower) || name.includes(id)) return 50;
  return 10;
}

function scoreBackgroundImage(file, folder, plugin) {
  const name = path.basename(file).toLowerCase();
  const folderLower = folder.toLowerCase();
  const id = (plugin.id || "").toLowerCase();

  if (/^background\.(webp|png|jpg|jpeg|gif|svg)$/.test(name)) return 100;
  if (name.includes("background")) return 98;
  if (name.includes("_bg") || name.includes("-bg")) return 90;
  if (name.includes("bg")) return 85;
  if (name.includes("hero")) return 80;
  if (name.includes("stage")) return 75;
  if (name.includes("scene")) return 70;
  if (name.includes(folderLower) || name.includes(id)) return 45;
  return 10;
}

function pickTileImage(plugin, folder) {
  if (plugin.tileImage && typeof plugin.tileImage === "string" && exists(plugin.tileImage)) return plugin.tileImage;

  const imgs = imageFiles(folder);
  if (!imgs.length) return { ...FALLBACK_TILE, label: plugin.title || plugin.id };

  return imgs
    .slice()
    .sort((a, b) => scoreTileImage(b, folder, plugin) - scoreTileImage(a, folder, plugin))[0];
}

function pickBackgroundImage(plugin, folder, tileImage) {
  if (plugin.backgroundImage && typeof plugin.backgroundImage === "string" && exists(plugin.backgroundImage)) return plugin.backgroundImage;

  const imgs = imageFiles(folder);
  if (!imgs.length) return FALLBACK_BACKGROUND;

  const sorted = imgs
    .slice()
    .sort((a, b) => scoreBackgroundImage(b, folder, plugin) - scoreBackgroundImage(a, folder, plugin));

  const best = sorted[0];
  if (best) return best;

  if (typeof tileImage === "string") return tileImage;
  return FALLBACK_BACKGROUND;
}

function pickPrimaryAudio(plugin, folder, audios) {
  if (plugin.primaryAudio && exists(plugin.primaryAudio)) return plugin.primaryAudio;
  if (plugin.audioFile && exists(plugin.audioFile)) return plugin.audioFile;
  if (plugin.audio && typeof plugin.audio === "string" && exists(plugin.audio)) return plugin.audio;
  if (plugin.audio && plugin.audio.file && exists(plugin.audio.file)) return plugin.audio.file;

  if (!audios.length) return null;
  if (audios.length === 1) return audios[0];

  const id = (plugin.id || "").toLowerCase();
  const folderLower = folder.toLowerCase();

  return audios.find(a => path.basename(a).toLowerCase().startsWith(folderLower)) ||
         audios.find(a => path.basename(a).toLowerCase().startsWith(id)) ||
         audios.find(a => /main|audio|listen|months|numbers|salahnames|salah/.test(path.basename(a).toLowerCase())) ||
         audios[0];
}

function normalizeEngine(plugin) {
  const id = plugin.id || "";
  const raw = plugin.engine || plugin.action || "";

  if (id === "quran" || plugin.type === "core-quran") return "learning";
  if (id === "salah" || plugin.type === "core-salah") return "external";
  if (raw === "directAudio" || raw === "direct-audio") return "directAudio";
  if (raw === "spotlight" || id === "numbers" || id === "names") return "spotlight";
  if (raw === "story" || id === "salahnames" || id === "salah-names") return "story";
  if (raw === "sequence") return "sequence";
  if (raw === "gallery") return "gallery";
  return "gallery";
}

function readSlices() {
  const master = readJson(MASTER_SLICES, null);
  if (master) return { file: MASTER_SLICES, data: master };

  const legacy = readJson(LEGACY_SLICES, null);
  if (legacy) return { file: LEGACY_SLICES, data: legacy };

  return { file: null, data: {} };
}

function defaultEffectFor(plugin, engine) {
  if (plugin.effect) return plugin.effect;

  if (plugin.id === "months") {
    return {
      type: "aquarium",
      waterRays: true,
      bubbles: true,
      fishZones: true,
      mouthMove: true,
      tailGlow: true
    };
  }

  if (plugin.id === "numbers") {
    return {
      type: "numberSpotlight",
      backgroundStatic: true,
      focusOneNumber: true,
      followAudio: true,
      activeScale: 1.25,
      glow: true,
      sparkles: true,
      float: true
    };
  }

  if (engine === "spotlight") {
    return {
      type: "spotlight",
      backgroundStatic: true,
      focusOneItem: true,
      glow: true,
      sparkles: true
    };
  }

  if (engine === "story") {
    return {
      type: "timeStory",
      skyOverlay: true,
      badges: true
    };
  }

  return null;
}

function defaultSalahNamesSequence(plugin) {
  if (Array.isArray(plugin.sequence) && plugin.sequence.length) return plugin.sequence;
  if (!(plugin.id === "salahnames" || plugin.id === "salah-names")) return plugin.sequence || [];

  return [
    { name: "Fajr", arabic: "الفجر", effect: "dawn", rakah: ["2 Sunnah", "2 Fard"] },
    { name: "Dhuhr", arabic: "الظهر", effect: "bright-day", rakah: ["4 Sunnah", "4 Fard", "2 Sunnah"] },
    { name: "Asr", arabic: "العصر", effect: "afternoon", rakah: ["4 Fard"] },
    { name: "Maghrib", arabic: "المغرب", effect: "sunset", rakah: ["3 Fard", "2 Sunnah"] },
    { name: "Isha", arabic: "العشاء", effect: "night", rakah: ["4 Fard", "2 Sunnah", "Witr"] }
  ];
}

function buildQuran(plugin, slicesInfo) {
  const syncFiles = [];

  // Existing app behavior: root index.html is copied to quran/index.html locally.
  addUniqueSync(syncFiles, "index.html", "quran/index.html");

  // Preserve published QR root audio.
  fs.readdirSync(ROOT)
    .filter(f => /^A\d+\.mp3$/i.test(f))
    .sort((a, b) => a.localeCompare(b, undefined, { numeric: true }))
    .forEach(mp3 => addUniqueSync(syncFiles, mp3, `quran/${mp3}`));

  // New folders.
  addFolderSync(syncFiles, "listen");
  addFolderSync(syncFiles, "learn");

  // Slices file.
  addFolderSync(syncFiles, "engines");
  addFolderSync(syncFiles, "styles");
  addFolderSync(syncFiles, "engines");
  addFolderSync(syncFiles, "styles");
  if (slicesInfo.file) addUniqueSync(syncFiles, slicesInfo.file, slicesInfo.file);

  return {
    title: plugin.title || "Quran",
    mainUrl: plugin.mainUrl || `${LOCAL_BASE}/quran/index.html?id=A1`,
    plugin: {
      id: "quran",
      title: plugin.title || "Quran",
      type: "core",
      engine: "learning",
      theme: "quran",
      supports: plugin.supports || ["listen", "repeat5", "hifz"],
      slicesFile: slicesInfo.file,
      locked: true
    },
    syncFiles
  };
}

function buildSalah(plugin) {
  const externalUrl = plugin.externalUrl || plugin.mainUrl || "https://busymommh.github.io/SalahSteps/SalahStepsIndex.html";

  return {
    title: plugin.title || "Salah",
    // Local bridge gives Home button and keeps Prism navigation instead of trapping user on external page.
    mainUrl: `${LOCAL_BASE}/index.html?plugin=salah`,
    plugin: {
      id: "salah",
      title: plugin.title || "Salah",
      type: "core",
      engine: "external",
      theme: "salah",
      externalUrl,
      locked: true
    },
    syncFiles: [
      {
        url: repoUrl("index.html"),
        path: "index.html"
      }
    ]
  };
}

function buildPlugin(plugin, slicesInfo) {
  const folder = resolveFolder(plugin);
  const engine = normalizeEngine(plugin);
  const syncFiles = [];

  // Plugin pages need root index.html locally.
  addUniqueSync(syncFiles, "index.html", "index.html");

  // Assets.
  addFolderSync(syncFiles, folder);
  addFolderSync(syncFiles, "engines");
  addFolderSync(syncFiles, "styles");
  addFolderSync(syncFiles, "engines");
  addFolderSync(syncFiles, "styles");

  // Plugin file.
  if (plugin.__pluginFile) addUniqueSync(syncFiles, plugin.__pluginFile, plugin.__pluginFile);

  // Slices.
  if (slicesInfo.file) addUniqueSync(syncFiles, slicesInfo.file, slicesInfo.file);

  const imgs = imageFiles(folder);
  const auds = audioFiles(folder);
  const tileImage = pickTileImage(plugin, folder);
  const backgroundImage = pickBackgroundImage(plugin, folder, tileImage);
  const primaryAudio = pickPrimaryAudio(plugin, folder, auds);

  const playFromLanding = engine === "directAudio";
  const autoStart = plugin.autoStart === true || plugin.autoplay === true;

  const pluginConfig = {
    ...plugin,

    // Internal build fields removed/replaced.
    __pluginFile: undefined,

    id: plugin.id,
    title: plugin.title,
    type: plugin.type || "plugin",
    folder,
    engine,
    theme: plugin.theme || plugin.id,
    renderer: plugin.renderer || engine,

    tileImage,
    backgroundImage,

    primaryAudio,
    audios: auds,
    images: imgs,

    playFromLanding,
    autoStart,
    directAudio: playFromLanding,
    action: playFromLanding ? "direct-audio" : "open-plugin",

    effect: defaultEffectFor(plugin, engine),

    supports: plugin.supports || ["listen"],

    audioMode:
      plugin.audioMode ||
      (plugin.audio && plugin.audio.mode) ||
      (auds.length > 1 ? "multiple-files" : "single-file-pauses"),

    segmentDetection:
      plugin.segmentDetection ||
      (plugin.audio && plugin.audio.segmentDetection) ||
      (plugin.id === "months" ? "normal" : "silence"),

    fallback:
      plugin.fallback ||
      (plugin.audio && plugin.audio.fallback) ||
      "duration",

    learningMode:
      plugin.learningMode ||
      (slicesInfo.data && Object.keys(slicesInfo.data).some(k => k.startsWith(`${folder}/`)) ? "slices" : null),

    tilePalette: plugin.tilePalette || DEFAULT_PALETTE,

    sequence: defaultSalahNamesSequence(plugin),
    coordinates: plugin.coordinates || {},
    numbers: plugin.numbers || {},
    spotlight: plugin.spotlight || null,
    displayMode: plugin.displayMode || null,
    chunkMode: plugin.chunkMode || null,

    slices: Object.keys(slicesInfo.data || {}).filter(k => k.startsWith(`${folder}/`)),
    slicesFile: slicesInfo.file,

    lockedDesign: true
  };

  // Remove undefined key after spread.
  delete pluginConfig.__pluginFile;

  return {
    title: plugin.title,
    mainUrl: (playFromLanding || autoStart)
      ? `${LOCAL_BASE}/index.html?plugin=${plugin.id}&autoplay=1`
      : `${LOCAL_BASE}/index.html?plugin=${plugin.id}`,
    plugin: pluginConfig,
    syncFiles
  };
}

function fallbackPlugins() {
  return [
    { id: "quran", title: "Quran", type: "core-quran", priority: 1 },
    {
      id: "salah",
      title: "Salah",
      type: "core-salah",
      priority: 2,
      mainUrl: "https://busymommh.github.io/SalahSteps/SalahStepsIndex.html"
    }
  ];
}

function buildMenu() {
  let plugins = loadPlugins();
  if (!plugins.length) {
    console.warn("⚠️ No plugin files found. Building fallback Quran/Salah menu.");
    plugins = fallbackPlugins();
  }

  const slicesInfo = readSlices();
  const menu = [];

  for (const plugin of plugins) {
    const id = plugin.id;

    if (id === "quran" || plugin.type === "core-quran") {
      menu.push(buildQuran(plugin, slicesInfo));
    } else if (id === "salah" || plugin.type === "core-salah") {
      menu.push(buildSalah(plugin));
    } else {
      menu.push(buildPlugin(plugin, slicesInfo));
    }
  }

  writeJson(MENU_FILE, menu);
  console.log(`✅ Prism V3.4 menu.json generated with ${menu.length} items`);
}

buildMenu();
