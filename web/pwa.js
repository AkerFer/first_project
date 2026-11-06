'use strict';
let plannerInstallPrompt=null;
if(!window.Android){
 window.addEventListener('beforeinstallprompt',e=>{e.preventDefault();plannerInstallPrompt=e;});
 window.addEventListener('appinstalled',()=>{plannerInstallPrompt=null;toast('PlanWithMe добавлен на главный экран');});
 if('serviceWorker' in navigator)navigator.serviceWorker.register('./sw.js').catch(()=>{});
}
window.installPlanner=async()=>{
 if(plannerInstallPrompt){await plannerInstallPrompt.prompt();await plannerInstallPrompt.userChoice;plannerInstallPrompt=null;return;}
 toast(matchMedia('(display-mode: standalone)').matches?'Приложение уже установлено.':'Откройте меню браузера → «Установить приложение» или «На экран Домой». На iPhone: Поделиться → На экран Домой.');
};
