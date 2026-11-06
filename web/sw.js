'use strict';
const CACHE='planwithme-shell-1.1.0';
const ASSETS=['./','./index.html','./style.css','./features.css','./model.js','./features.js','./app.js','./pwa.js','./manifest.webmanifest','./icon.svg','./icon-192.png','./icon-512.png','./fonts/book.ttf','./fonts/mono.ttf','./fonts/soft.ttf'];
self.addEventListener('install',event=>event.waitUntil((async()=>{const cache=await caches.open(CACHE);for(const path of ASSETS){const response=await fetch(new Request(path,{cache:'reload'}));if(!response.ok||response.redirected||new URL(response.url).origin!==self.location.origin)throw Error('App shell unavailable');await cache.put(path,response);}await self.skipWaiting();})()));
self.addEventListener('activate',event=>event.waitUntil((async()=>{for(const key of await caches.keys())if(key.startsWith('planwithme-shell-')&&key!==CACHE)await caches.delete(key);await self.clients.claim();})()));
self.addEventListener('fetch',event=>{const url=new URL(event.request.url);if(event.request.method!=='GET'||url.origin!==self.location.origin)return;const base=new URL('./',self.location).pathname;
 if(event.request.mode==='navigate'&&(url.pathname===base||url.pathname===base+'index.html')){event.respondWith(fetch(event.request).catch(async()=>await caches.match('./index.html')||Response.error()));return;}
 if(ASSETS.some(path=>new URL(path,self.location).pathname===url.pathname))event.respondWith(caches.match(event.request,{ignoreSearch:true}).then(cached=>cached||fetch(event.request)));
});
