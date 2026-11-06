package ru.planwithme.app;
import android.app.*;
import android.appwidget.AppWidgetManager;
import android.os.*;
import android.content.*;
import android.webkit.*;
import android.view.*;
import android.graphics.Color;
import android.net.Uri;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
 private WebView web; private boolean ready=false; private String pendingExport; private static final int EXPORT=21,IMPORT=22;
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(247,244,237));getWindow().setNavigationBarColor(Color.rgb(255,254,251));getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  web=new WebView(this);web.setBackgroundColor(Color.rgb(247,244,237));web.setFitsSystemWindows(true);web.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i.consumeSystemWindowInsets();});setContentView(web);
  WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);web.setWebChromeClient(new WebChromeClient());
  web.setWebViewClient(new WebViewClient(){@Override public void onPageFinished(WebView v,String url){ready=true;openWidgetIntent();}@Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){Uri u=r.getUrl();if("https".equals(u.getScheme())&&"app.planwithme.local".equals(u.getHost())){String p=u.getPath();if(p==null||p.equals("/"))p="/index.html";if(!p.matches("/(index\\.html|style\\.css|features\\.css|app\\.js|model\\.js|features\\.js|pwa\\.js|icon\\.svg|icon-(192|512)\\.png|manifest\\.webmanifest|fonts/(book|mono|soft)\\.ttf)"))return new WebResourceResponse("text/plain","UTF-8",404,"Not found",null,new ByteArrayInputStream(new byte[0]));try{return new WebResourceResponse(p.endsWith(".css")?"text/css":p.endsWith(".js")?"application/javascript":p.endsWith(".ttf")?"font/ttf":p.endsWith(".png")?"image/png":p.endsWith(".svg")?"image/svg+xml":p.endsWith(".webmanifest")?"application/manifest+json":"text/html","UTF-8",getAssets().open(p.substring(1)));}catch(Exception e){return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));}}return new WebResourceResponse("text/plain","UTF-8",403,"Forbidden",null,new ByteArrayInputStream(new byte[0]));}@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return true;}});
  web.addJavascriptInterface(new Bridge(),"Android");web.loadUrl("https://app.planwithme.local/index.html");
 }
 public class Bridge {
  @JavascriptInterface public String load(){return getSharedPreferences("plans",MODE_PRIVATE).getString("state","");}
  @JavascriptInterface public boolean save(String raw){if(raw.length()>20000000)return false;boolean saved=getSharedPreferences("plans",MODE_PRIVATE).edit().putString("state",raw).commit();if(saved)runOnUiThread(()->PlanWidget.updateAll(MainActivity.this));return saved;}
  @JavascriptInterface public void requestWidget(){runOnUiThread(()->{AppWidgetManager m=AppWidgetManager.getInstance(MainActivity.this);if(m.isRequestPinAppWidgetSupported()){boolean accepted=m.requestPinAppWidget(new ComponentName(MainActivity.this,PlanWidget.class),null,null);if(!accepted)notice("Добавьте PlanWithMe через меню виджетов главного экрана.");}else notice("Удерживайте главный экран → Виджеты → PlanWithMe.");});}
  @JavascriptInterface public void exportData(String raw){pendingExport=raw;runOnUiThread(()->{try{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"PlanWithMe-backup.json");startActivityForResult(i,EXPORT);}catch(Exception e){notice("Не удалось открыть сохранение файла");}});}
  @JavascriptInterface public void importData(){runOnUiThread(()->{try{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,IMPORT);}catch(Exception e){notice("Не удалось открыть выбор файла");}});}
 }
 private void notice(String text){runOnUiThread(()->web.evaluateJavascript("window.nativeNotice("+JSONObject.quote(text)+")",null));}
 @Override public void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null||data.getData()==null){pendingExport=null;return;}Uri uri=data.getData();if(req==EXPORT){final String raw=pendingExport;pendingExport=null;if(raw==null){notice("Повторите экспорт резервной копии");return;}new Thread(()->{try(OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new IOException();out.write(raw.getBytes(StandardCharsets.UTF_8));notice("Резервная копия сохранена");}catch(Exception e){notice("Не удалось сохранить копию");}}).start();}else if(req==IMPORT){new Thread(()->{try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream bytes=new ByteArrayOutputStream()){if(in==null)throw new IOException();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){bytes.write(buf,0,n);if(bytes.size()>20000000)throw new IOException();}String raw=bytes.toString("UTF-8");runOnUiThread(()->web.evaluateJavascript("window.receiveImport("+JSONObject.quote(raw)+")",null));}catch(Exception e){notice("Не удалось прочитать копию (максимум 20 МБ)");}}).start();}}
 @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);if(ready)openWidgetIntent();}
 private void openWidgetIntent(){Intent intent=getIntent();if(intent!=null&&intent.hasExtra("widgetTask")){String id=intent.getStringExtra("widgetTask");intent.removeExtra("widgetTask");web.evaluateJavascript("window.openTaskFromWidget("+JSONObject.quote(id==null?"":id)+")",null);}}
 @Override public void onBackPressed(){web.evaluateJavascript("window.handleBack()",result->{if(!"true".equals(result))new AlertDialog.Builder(this).setMessage("Закрыть PlanWithMe?").setPositiveButton("Закрыть",(d,w)->finish()).setNegativeButton("Остаться",null).show();});}
 @Override public void onDestroy(){web.removeJavascriptInterface("Android");web.destroy();super.onDestroy();}
}
