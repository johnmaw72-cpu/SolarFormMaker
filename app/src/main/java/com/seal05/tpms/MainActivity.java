package com.seal05.tpms;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Context;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final ExecutorService worker=Executors.newSingleThreadExecutor();
  private TextView headline, diagnostics, updated;
  private final TextView[] pressures=new TextView[4];
  private final TextView[] notes=new TextView[4];
  private Button unitsButton, diagnosticsButton;
  private boolean paused=false, busy=false, diagOpen=false;
  private int unit=0;
  private final String[] wheelNames={"FRONT LEFT","FRONT RIGHT","REAR LEFT","REAR RIGHT"};
  private final String[] areaNames={"TYRE_COMMAND_AREA_LEFT_FRONT","TYRE_COMMAND_AREA_RIGHT_FRONT","TYRE_COMMAND_AREA_LEFT_REAR","TYRE_COMMAND_AREA_RIGHT_REAR"};
  private final String clsName="android.hardware.bydauto.tyre.BYDAutoTyreDevice";
  private int[] values={-1,-1,-1,-1};
  private String[] wheelNotes={"No data","No data","No data","No data"};
  private String status="Checking BYD tire pressure service";
  private String debug="";
  private long timestamp=0;
  private boolean active=false;
  private final int BG=0xff081423, PANEL=0xff132339, TEXT=Color.WHITE, MUTED=0xff9cacc2, GREEN=0xff31d3ab, AMBER=0xffffc76a;
  private int dip(float v){return (int)(getResources().getDisplayMetrics().density*v+.5f);}
  private GradientDrawable background(int fill,int rad){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dip(rad));d.setStroke(dip(1),0xff2c4158);return d;}
  private TextView label(String s,int size,int color,boolean bold){
    TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
    t.setGravity(Gravity.CENTER_VERTICAL);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
  }
  private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(TEXT);b.setTextSize(12);b.setBackground(background(PANEL,12));return b;}
  private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
  private void add(LinearLayout l, View v,int h){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,h<0?h:dip(h));p.setMargins(dip(5),dip(5),dip(5),dip(5));l.addView(v,p);}
  @Override public void onCreate(Bundle b){super.onCreate(b);setRequestedOrientation(-1);render();}
  private void render(){
    ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(BG);scroll.setFillViewport(true);
    LinearLayout layout=column();layout.setPadding(dip(17),dip(12),dip(17),dip(15));scroll.addView(layout);setContentView(scroll);
    TextView kicker=label("BYD SEAL 05 DM-i  /  DiLink 2.1",12,GREEN,true);add(layout,kicker,24);
    LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
    TextView title=label("Tire pressure monitor",24,TEXT,true);top.addView(title,new LinearLayout.LayoutParams(0,dip(48),1));
    unitsButton=button("bar");top.addView(unitsButton,new LinearLayout.LayoutParams(dip(100),dip(44)));
    unitsButton.setOnClickListener(v->{unit=(unit+1)%3;refresh();});
    add(layout,top,52);
    headline=label("Checking vehicle connection…",15,AMBER,true);headline.setPadding(dip(12),0,dip(12),0);headline.setBackground(background(PANEL,12));add(layout,headline,52);
    LinearLayout first=new LinearLayout(this);LinearLayout second=new LinearLayout(this);
    first.setOrientation(LinearLayout.HORIZONTAL);second.setOrientation(LinearLayout.HORIZONTAL);
    for(int i=0;i<4;i++){
      LinearLayout panel=column();panel.setPadding(dip(14),dip(12),dip(10),dip(10));panel.setBackground(background(PANEL,14));
      panel.addView(label(wheelNames[i],12,MUTED,true));pressures[i]=label("--",31,TEXT,true);panel.addView(pressures[i]);
      notes[i]=label("NO DATA",11,AMBER,false);panel.addView(notes[i]);
      LinearLayout row=i<2?first:second;LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(0,dip(123),1f);
      wp.setMargins(dip(5),dip(5),dip(5),dip(5));row.addView(panel,wp);
    }
    add(layout,first,133);add(layout,second,133);
    updated=label("Refresh: every 2 seconds",12,MUTED,false);add(layout,updated,32);
    LinearLayout controls=new LinearLayout(this);
    Button pause=button("Pause");controls.addView(pause,new LinearLayout.LayoutParams(0,dip(45),1));
    pause.setOnClickListener(v->{paused=!paused;pause.setText(paused?"Resume":"Pause");if(!paused)poll();refresh();});
    diagnosticsButton=button("Diagnostics");controls.addView(diagnosticsButton,new LinearLayout.LayoutParams(0,dip(45),1));
    diagnosticsButton.setOnClickListener(v->{diagOpen=!diagOpen;diagnostics.setVisibility(diagOpen?View.VISIBLE:View.GONE);refresh();});
    Button copy=button("Copy report");controls.addView(copy,new LinearLayout.LayoutParams(0,dip(45),1));
    copy.setOnClickListener(v->{ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
      if(cm!=null)cm.setPrimaryClip(ClipData.newPlainText("BYD TPMS",debug));
      Toast.makeText(this,"Report copied",Toast.LENGTH_SHORT).show();});
    add(layout,controls,55);
    diagnostics=label("",12,MUTED,false);diagnostics.setPadding(dip(10),dip(12),dip(10),dip(12));
    diagnostics.setTextIsSelectable(true);diagnostics.setBackground(background(PANEL,10));
    diagnostics.setVisibility(View.GONE);add(layout,diagnostics,-2);
    TextView warning=label("Read-only test: if the car exposes only low-pressure warnings, exact PSI values cannot be calculated. Never use this app instead of the factory warning system.",12,MUTED,false);
    add(layout,warning,-2);refresh();
  }
  private void refresh(){
    if(headline==null)return;
    unitsButton.setText(unit==0?"bar":unit==1?"PSI":"kPa");
    headline.setText(status);
    int count=0;
    for(int i=0;i<4;i++){
      int k=values[i];
      boolean valid=k>=40&&k<=600;
      if(valid)count++;
      pressures[i].setText(valid?(unit==0?String.format(Locale.US,"%.2f bar",k/100.0):unit==1?String.format(Locale.US,"%.1f PSI",k*.145037738):k+" kPa"):"--");
      notes[i].setText(wheelNotes[i]);
      notes[i].setTextColor(valid?GREEN:AMBER);
    }
    headline.setTextColor(count==4?GREEN:AMBER);
    updated.setText((paused?"PAUSED  ·  ":"2 s auto-refresh  ·  ")+(timestamp==0?"No sample yet":new SimpleDateFormat("HH:mm:ss",Locale.US).format(new Date(timestamp))));
    diagnostics.setText(debug);
  }
  private void poll(){
    if(paused||busy||!active)return;
    busy=true;
    worker.execute(()->{
      int[] next={-1,-1,-1,-1};String[] states={"No data","No data","No data","No data"};
      String result="BYD API unavailable", info="";
      try {
        Class<?> c=Class.forName(clsName);
        Method get=c.getMethod("getInstance",Context.class);
        Object instance=get.invoke(null,getApplicationContext());
        if(instance==null)throw new IllegalStateException("BYD service returned null");
        StringBuilder report=new StringBuilder();report.append("Class: ").append(clsName).append("\n");
        for(int i=0;i<4;i++){
          try {
            Field field=c.getField(areaNames[i]);int area=field.getInt(null);
            Method pressure=c.getMethod("getTyrePressureValue",int.class);
            Object raw=pressure.invoke(instance,area);
            int k=raw instanceof Number?((Number)raw).intValue():-1;
            next[i]=k;
            states[i]=(k>=40&&k<=600)?"Sensor reading received":"Not a valid pressure";
            report.append(wheelNames[i]).append(": area=").append(area).append(" kPa=").append(k);
            for(String m:new String[]{"getTyrePressureState","getTyreSignalState","getTyreAirLeakState"}){
              try {Object val=c.getMethod(m,int.class).invoke(instance,area);report.append(" ").append(m).append("=").append(val);}
              catch(Throwable ex){report.append(" ").append(m).append("=unavailable");}
            }
            report.append("\n");
          }catch(Throwable ex){states[i]="API error: "+ex.getClass().getSimpleName();report.append(wheelNames[i]).append(": ").append(ex).append("\n");}
        }
        info=report.toString();
        int count=0;for(int k:next)if(k>=40&&k<=600)count++;
        result=count==4?"●  All four tires received":count>0?"●  Partial readings: "+count+"/4":"●  No numerical pressure available";
      }catch(Throwable t){result="●  BYD TPMS service unavailable";info=t.toString();}
      final int[] v=next;final String[] n=states;final String msg=result, raw=info;
      handler.post(()->{
        values=v;wheelNotes=n;status=msg;timestamp=System.currentTimeMillis();
        debug="Seal TPMS Monitor v0.1.1\nDevice: "+android.os.Build.MANUFACTURER+" / "+android.os.Build.MODEL+
            "\nAndroid: "+android.os.Build.VERSION.RELEASE+"\nSample: "+new Date(timestamp)+
            "\nStatus: "+status+"\n"+raw;
        busy=false;refresh();
      });
    });
  }
  private final Runnable loop=new Runnable(){public void run(){if(active){poll();handler.postDelayed(this,2000);}}};
  @Override protected void onResume(){super.onResume();active=true;handler.removeCallbacks(loop);handler.post(loop);}
  @Override protected void onPause(){active=false;handler.removeCallbacks(loop);super.onPause();}
  @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
