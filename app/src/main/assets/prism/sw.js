const PRISM_CACHE = "prism-v690-frozen-release";
const AUDIO_RE = /\.(mp3|m4a|wav|ogg)$/i;
const ASSET_RE = /\.(png|jpg|jpeg|webp|gif|svg|ttf|woff2?)$/i;
const CODE_RE = /\.(css|js)$/i;
const JSON_RE = /\/(menu|plugins|.+\.plugin)\.json$/i;

self.addEventListener("install", event => {
  self.skipWaiting();
});

self.addEventListener("activate", event => {
  event.waitUntil((async () => {
    const keys = await caches.keys();
    await Promise.all(keys.filter(k => k.startsWith("prism-") && k !== PRISM_CACHE).map(k => caches.delete(k)));
    await self.clients.claim();
  })());
});

self.addEventListener("fetch", event => {
  const req = event.request;
  if(req.method !== "GET") return;
  const url = new URL(req.url);
  const pathname = url.pathname;

  // Code + JSON are network-first so new cards/effects appear without stale cache problems.
  if(CODE_RE.test(pathname) || JSON_RE.test(pathname) || pathname.endsWith("/plugins/plugins.json")){
    event.respondWith((async () => {
      const cache = await caches.open(PRISM_CACHE);
      try{
        const fresh = await fetch(req, {cache:"no-store"});
        if(fresh && fresh.ok) cache.put(req, fresh.clone()).catch(()=>{});
        return fresh;
      }catch(e){
        const cached = await cache.match(req);
        if(cached) return cached;
        throw e;
      }
    })());
    return;
  }

  // Audio/images/fonts are cache-first for offline use and speed.
  const shouldCache = AUDIO_RE.test(pathname) || ASSET_RE.test(pathname);
  if(!shouldCache) return;

  event.respondWith((async () => {
    const cache = await caches.open(PRISM_CACHE);
    const cached = await cache.match(req);
    if(cached) return cached;
    try{
      const res = await fetch(req, {cache:"force-cache"});
      if(res && res.ok) cache.put(req, res.clone()).catch(()=>{});
      return res;
    }catch(e){
      if(cached) return cached;
      throw e;
    }
  })());
});
