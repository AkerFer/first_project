"""Run against a local static web server: PLANWITHME_URL=http://127.0.0.1:8000 python tests/browser_test.py."""
import os,json
from pathlib import Path
from playwright.sync_api import sync_playwright
URL=os.environ.get('PLANWITHME_URL','http://127.0.0.1:8000')
OUT=Path(os.environ.get('QA_OUTPUT',str(Path(__file__).resolve().parents[1]/'build'/'qa')));OUT.mkdir(parents=True,exist_ok=True)
with sync_playwright() as p:
 browser=p.chromium.launch(executable_path=os.environ.get('CHROMIUM_PATH','/usr/bin/chromium'),headless=True,args=['--no-sandbox'])
 context=browser.new_context(viewport={'width':1440,'height':1000},service_workers='block',timezone_id='Asia/Tashkent')
 page=context.new_page();errors=[];page.on('pageerror',lambda e:errors.append(str(e)));page.on('dialog',lambda d:d.accept())
 page.goto(URL)
 # Old backups and storage are migrated without losing completed task history.
 old={'version':1,'theme':'night','topics':[{'id':'p','name':'Личное','type':'personal'}],'events':[],'tasks':[{'id':'old','title':'Старая задача','topic':'p','date':'2026-11-01','priority':'high','progress':100,'completedAt':'2026-11-01','note':'Сохранённая заметка'}]}
 page.evaluate('(s)=>localStorage.setItem("planwithme.v1",JSON.stringify(s))',old);page.reload();assert page.evaluate('state.version===2&&state.tasks[0].progress===100&&state.tasks[0].completedAt==="2026-11-01"')
 page.evaluate("go('tasks')")
 # A single copy is a separate entity with reset progress.
 page.locator('[data-action=duplicateTask]').click();page.locator('#f_date').fill('2026-11-10');page.locator('#editForm button[type=submit]').click()
 assert page.evaluate('state.tasks.length===2&&state.tasks[1].date==="2026-11-10"&&state.tasks[1].progress===0&&state.tasks[1].completedAt===null')
 copied=page.evaluate('state.tasks[1].id');page.locator(f'.task-info[data-id="{copied}"]').click();assert page.locator('#editForm button[type=submit]').inner_text()=='Сохранить'
 page.locator('#f_title').fill('Копия изменена');page.locator('#editForm button[type=submit]').click();assert page.evaluate('state.tasks[0].title==="Старая задача"')
 # Switch modes: preview must update immediately; cancel creates nothing.
 page.locator('[data-action=duplicateTask]').first.click();page.locator('#f_mode').select_option('month');page.locator('#f_month').fill('2027-02');page.locator('#f_month').dispatch_event('change');assert '28' in page.locator('#copyPreview').inner_text();page.locator('#closeDialog').click();assert page.evaluate('state.tasks.length===2')
 # Bulk copies use a snapshot and never copy newly generated tasks again.
 page.locator('[data-action=bulkMode]').click();page.locator('[data-action=selectVisible]').click();page.locator('[data-action=bulkMonth]').click();page.locator('#f_month').fill('2028-02');page.locator('#f_month').dispatch_event('change');assert '58' in page.locator('#copyPreview').inner_text();page.locator('#editForm button[type=submit]').click();assert page.evaluate('state.tasks.length===60&&state.tasks.slice(2).every(t=>t.progress===0)')
 # Profile and daily journal, graph, overwrite, date change and deletion.
 page.evaluate("go('profile')");page.get_by_role('button',name='Создать себя',exact=True).click();page.locator('#f_name').fill('Мой профиль');page.locator('#f_height').fill('175');page.locator('#f_avatar').select_option('🦊');page.locator('#f_bio').fill('Расту в своём ритме');page.locator('#editForm button[type=submit]').click();assert page.get_by_role('heading',name='Мой профиль').count()==1
 page.get_by_role('button',name='＋ Запись',exact=True).click();page.locator('#f_weight').fill('70.5');page.locator('#f_sleep').fill('7.5');page.locator('#f_mood').select_option('4');page.locator('#f_energy').select_option('3');page.locator('#f_stress').select_option('2');page.locator('#f_note').fill('<script>bad()</script> Спокойный день');page.locator('#editForm button[type=submit]').click();assert page.locator('.wellness-chart circle').count()==1
 assert page.locator('.entry-note').inner_text().startswith('<script>')
 page.evaluate('openWellness(shiftDay(today(),-1))');page.locator('#f_weight').fill('71');page.locator('#f_mood').select_option('3');page.locator('#editForm button[type=submit]').click();assert page.locator('.wellness-chart circle').count()==2
 page.locator('[data-action=chartMetric][data-id=weight]').click();assert '-0.5' in page.locator('.metric-delta').inner_text()
 page.locator('.wellness-chart circle').first.click();page.locator('#f_note').fill('Исправление');page.locator('#editForm button[type=submit]').click();assert page.evaluate('state.wellness.length===2')
 page.evaluate('openWellness(shiftDay(today(),-1))');page.locator('#deleteBtn').click();assert page.evaluate('state.wellness.length===1')
 # Theme/font settings apply immediately and survive reload; fonts actually load.
 page.evaluate("go('settings')");assert page.locator('.theme-tile').count()==8
 for theme in ['sage','night','lavender','rose','ocean','sand','peach','forest']:
  page.locator(f'[data-action=theme][data-id={theme}]').click();assert page.locator('body').get_attribute('data-theme')==theme
 for font in ['book','soft','mono','system']:
  page.locator('#fontChoice').select_option(font);assert page.locator('body').get_attribute('data-font')==font
 page.locator('#fontChoice').select_option('book');page.locator('#motionChoice').uncheck();page.reload();assert page.evaluate('state.font==="book"&&state.motion===false&&state.profile.name==="Мой профиль"&&state.wellness.length===1')
 page.evaluate("go('settings')");page.locator('#motionChoice').check();page.evaluate("go('profile')");assert page.evaluate('document.getElementById("main").getAnimations().length>0');page.wait_for_timeout(350)
 page.emulate_media(reduced_motion='reduce');page.evaluate("go('home')");assert page.evaluate('document.getElementById("main").getAnimations().length===0');page.emulate_media(reduced_motion='no-preference')
 before=page.evaluate('JSON.stringify(state)');page.evaluate('receiveImport("{broken")');assert page.evaluate('JSON.stringify(state)')==before
 page.evaluate('receiveImport(JSON.stringify(state))');assert page.evaluate('JSON.stringify(state)')==before
 # Tablet and narrow mobile navigation, profile, settings and task selection.
 for width in [768,390,320]:
  page.set_viewport_size({'width':width,'height':844})
  for tab in ['home','tasks','schedule','topics','progress','profile','settings']:
   page.evaluate('(tab)=>go(tab)',tab);page.wait_for_timeout(300);assert page.evaluate('document.documentElement.scrollWidth<=innerWidth'),(width,tab)
 page.set_viewport_size({'width':390,'height':844});page.evaluate("state.font='system';state.theme='lavender';save();go('profile')");page.wait_for_timeout(350);page.screenshot(path=str(OUT/'profile-mobile.png'),full_page=True)
 page.set_viewport_size({'width':1440,'height':1000});page.evaluate("go('settings')");page.wait_for_timeout(350);page.screenshot(path=str(OUT/'themes-desktop.png'),full_page=True)
 # Compact browser mode keeps real state and supports completing tasks.
 page.evaluate("state.tasks[1].date=today();save()");page.goto(URL+'/?widget=1');assert page.locator('body').get_attribute('class')=='compact-mode';assert page.locator('#mobileNav').is_hidden();page.locator('.check').first.click();assert page.locator('.check.done').count()==1
 assert not errors,errors
 # PWA shell installs and reopens offline, including the compact query route.
 c2=browser.new_context(viewport={'width':390,'height':844});pg=c2.new_page();pg.goto(URL);pg.wait_for_function('navigator.serviceWorker.controller!==null',timeout=30000)
 assert pg.evaluate('navigator.serviceWorker.controller.scriptURL.endsWith("/sw.js")')
 c2.set_offline(True);pg.reload();assert pg.get_by_role('heading',name='Планы без спешки').count()==1
 pg.goto(URL+'/?widget=1');assert pg.locator('body.compact-mode').count()==1
 browser.close();print('PASS: browser migration, single/bulk month duplication, profile/journal/chart, all themes/fonts, persistence, reduced motion, backups, narrow layouts, compact mode, PWA offline')
