package ru.planwithme.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;
import android.graphics.Color;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.*;

public class PlanWidget extends AppWidgetProvider {
 public static final String REFRESH="ru.planwithme.app.REFRESH_WIDGET";
 @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){for(int id:ids)update(context,manager,id);}
 @Override public void onReceive(Context c,Intent intent){super.onReceive(c,intent);if(REFRESH.equals(intent.getAction())||Intent.ACTION_DATE_CHANGED.equals(intent.getAction())||Intent.ACTION_TIME_CHANGED.equals(intent.getAction())||Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction()))updateAll(c);}
 public static void updateAll(Context c){AppWidgetManager m=AppWidgetManager.getInstance(c);for(int id:m.getAppWidgetIds(new ComponentName(c,PlanWidget.class)))update(c,m,id);}
 private static PendingIntent open(Context c,String task,int request){Intent i=new Intent(c,MainActivity.class);i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);i.putExtra("widgetTask",task==null?"":task);i.setData(Uri.parse("planwithme://widget/"+(task==null?"today":Uri.encode(task))));return PendingIntent.getActivity(c,request,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 private static void update(Context c,AppWidgetManager manager,int widgetId){
  RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.plan_widget);
  int foreground=Color.parseColor("#343f36"),background=Color.parseColor("#f7f4ed");
  List<JSONObject> tasks=new ArrayList<>();int done=0;String today=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());
  try {JSONObject s=new JSONObject(c.getSharedPreferences("plans",Context.MODE_PRIVATE).getString("state","{}"));String theme=s.optString("theme","sage");
   if(theme.equals("night")){foreground=Color.parseColor("#efeee2");background=Color.parseColor("#222c27");}
   if(theme.equals("forest")){foreground=Color.parseColor("#e4eee6");background=Color.parseColor("#1c302a");}
   if(theme.equals("lavender")){foreground=Color.parseColor("#493d57");background=Color.parseColor("#f4f0f8");}
   if(theme.equals("rose")){foreground=Color.parseColor("#593d44");background=Color.parseColor("#faf0f1");}
   if(theme.equals("ocean")){foreground=Color.parseColor("#2f4c57");background=Color.parseColor("#eff6f7");}
   if(theme.equals("sand")){foreground=Color.parseColor("#564936");background=Color.parseColor("#f8f2e6");}
   if(theme.equals("peach")){foreground=Color.parseColor("#614634");background=Color.parseColor("#fcf1e8");}
   JSONArray array=s.optJSONArray("tasks");if(array!=null)for(int i=0;i<array.length();i++){JSONObject t=array.optJSONObject(i);if(t!=null&&today.equals(t.optString("date"))){tasks.add(t);if(t.optInt("progress")==100)done++;}}
  }catch(Exception ignored){}
  Collections.sort(tasks,(a,b)->Integer.compare(a.optInt("progress"),b.optInt("progress")));
  v.setInt(R.id.widget_root,"setBackgroundColor",background);
  int[] texts={R.id.widget_title,R.id.widget_date,R.id.widget_row1,R.id.widget_row2,R.id.widget_row3,R.id.widget_empty,R.id.widget_open,R.id.widget_refresh};for(int id:texts)v.setTextColor(id,foreground);
  v.setTextViewText(R.id.widget_date,new SimpleDateFormat("d MMMM",new Locale("ru")).format(new Date())+" · "+done+" / "+tasks.size()+" готово");
  v.setViewVisibility(R.id.widget_empty,tasks.isEmpty()?View.VISIBLE:View.GONE);
  int[] rows={R.id.widget_row1,R.id.widget_row2,R.id.widget_row3};
  for(int i=0;i<rows.length;i++){boolean visible=i<tasks.size();v.setViewVisibility(rows[i],visible?View.VISIBLE:View.GONE);if(visible){JSONObject t=tasks.get(i);v.setTextViewText(rows[i],(t.optInt("progress")==100?"✓  ":"○  ")+t.optString("title"));v.setOnClickPendingIntent(rows[i],open(c,t.optString("id"),widgetId*10+i+1));}}
  v.setOnClickPendingIntent(R.id.widget_open,open(c,null,widgetId*10));v.setOnClickPendingIntent(R.id.widget_title,open(c,null,widgetId*10));
  Intent refresh=new Intent(c,PlanWidget.class).setAction(REFRESH);v.setOnClickPendingIntent(R.id.widget_refresh,PendingIntent.getBroadcast(c,widgetId,refresh,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));manager.updateAppWidget(widgetId,v);
 }
}
