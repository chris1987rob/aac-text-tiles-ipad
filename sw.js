/* ==========================================================================
   AAC Text Tiles iPad — Service Worker (Offline CacheStorage)
   ========================================================================== */

const CACHE_NAME = 'aac-text-tiles-ipad-v9'; // v9: pictures recompressed WITH alpha (v8 shipped them on black squares); v8: voice picker (Bella/Jake/Maya), type-to-say pipeline, AAC_VOICES in symbols_data.js; v7: clips trimmed of leading silence, decoded ahead of the tap, phrase clips for the built-in boards; v6: voice clips re-rendered with the bella voice; v5: pre-rendered voice clips (symbols/audio); v2: in-house symbol pictures replace the Mulberry SVGs; v3: 29 regenerated; v4: catalogue expanded 558 -> 2210

const CORE_ASSETS = [
  './',
  './index.html',
  './manifest.json',
  './symbols_data.js',
  './icons/apple-touch-icon.png',
  './icons/icon-192.png',
  './icons/icon-512.png'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(CORE_ASSETS);
    }).then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.map((key) => {
          if (key !== CACHE_NAME) {
            return caches.delete(key);
          }
        })
      );
    }).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const request = event.request;
  if (request.method !== 'GET') return;

  event.respondWith(
    caches.match(request).then((cachedResponse) => {
      if (cachedResponse) {
        return cachedResponse;
      }
      return fetch(request).then((networkResponse) => {
        if (!networkResponse || networkResponse.status !== 200 || networkResponse.type !== 'basic') {
          return networkResponse;
        }
        const responseToCache = networkResponse.clone();
        caches.open(CACHE_NAME).then((cache) => {
          cache.put(request, responseToCache);
        });
        return networkResponse;
      }).catch(() => {
        if (request.destination === 'document') {
          return caches.match('./index.html');
        }
      });
    })
  );
});
