package com.infinitygreenpower.solarform;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private final int NAVY=Color.rgb(11,49,93), GREEN=Color.rgb(46,139,46), BG=Color.rgb(245,247,250);
    private LinearLayout root;
    private EditText clientName, phone, location, date, sizeKw, notes, organizer;
    private RadioGroup systemType, phase;
    private CheckBox installation, transport;
    private final String[] itemNames={"الإنفرتر","الألواح الشمسية","البطارية","القواعد","كيبل DC","كيبل AC","صندوق حماية / قواطع","مواد أخرى"};
    private final ArrayList<EditText[]> itemRows=new ArrayList<>();

    @Override public void onCreate(Bundle b){ super.onCreate(b); getWindow().setStatusBarColor(NAVY); buildUi(); loadDraft(); }

    private TextView tv(String text,int size,boolean bold){
        TextView v=new TextView(this); v.setText(text); v.setTextSize(size); v.setTextColor(Color.rgb(25,35,45));
        v.setGravity(Gravity.RIGHT); v.setTextDirection(View.TEXT_DIRECTION_RTL); v.setPadding(dp(6),dp(7),dp(6),dp(7));
        if(bold)v.setTypeface(android.graphics.Typeface.DEFAULT,1); return v;
    }
    private EditText field(String hint){
        EditText e=new EditText(this); e.setHint(hint); e.setTextSize(16); e.setSingleLine(true); e.setGravity(Gravity.RIGHT); e.setTextDirection(View.TEXT_DIRECTION_RTL);
        e.setBackgroundColor(Color.WHITE); e.setPadding(dp(12),dp(8),dp(12),dp(8));
        e.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(50))); return e;
    }
    private void section(String title){
        TextView h=tv(title,18,true); h.setTextColor(Color.WHITE); h.setBackgroundColor(NAVY); h.setPadding(dp(14),dp(10),dp(14),dp(10));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,dp(14),0,dp(8)); h.setLayoutParams(lp); root.addView(h);
    }
    private void addLabelField(String label, EditText e){ root.addView(tv(label,15,true)); root.addView(e); }
    private RadioButton rb(String text){ RadioButton b=new RadioButton(this); b.setText(text); b.setTextSize(15); b.setTextDirection(View.TEXT_DIRECTION_RTL); return b; }

    private void buildUi(){
        ScrollView scroll=new ScrollView(this); scroll.setBackgroundColor(BG);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(14),dp(14),dp(14),dp(28)); root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); scroll.addView(root);
        TextView title=tv("استمارة طلب عرض سعر منظومة شمسية",24,true); title.setTextColor(NAVY); title.setGravity(Gravity.CENTER); root.addView(title);
        TextView sub=tv("Infinity Green Power • نموذج مبسط",14,false); sub.setTextColor(GREEN); sub.setGravity(Gravity.CENTER); root.addView(sub);

        section("1  معلومات العميل");
        clientName=field("اسم العميل"); phone=field("رقم الهاتف"); location=field("الموقع"); date=field("التاريخ");
        date.setText(new SimpleDateFormat("yyyy/MM/dd",Locale.US).format(new Date()));
        addLabelField("اسم العميل",clientName); addLabelField("رقم الهاتف",phone); addLabelField("الموقع",location); addLabelField("التاريخ",date);

        section("2  معلومات المنظومة");
        root.addView(tv("نوع المنظومة",15,true)); systemType=new RadioGroup(this); systemType.setOrientation(RadioGroup.HORIZONTAL); systemType.setGravity(Gravity.RIGHT);
        for(String x:new String[]{"هجين","أون كريد","أوف كريد"}) systemType.addView(rb(x)); root.addView(systemType);
        sizeKw=field("مثال: 8"); addLabelField("السعة المطلوبة (kW)",sizeKw);
        root.addView(tv("الطور",15,true)); phase=new RadioGroup(this); phase.setOrientation(RadioGroup.HORIZONTAL); phase.setGravity(Gravity.RIGHT);
        phase.addView(rb("أحادي")); phase.addView(rb("ثلاثي")); root.addView(phase);

        section("3  المواد الرئيسية");
        TextView help=tv("أدخل الماركة/الموديل، السعة، الكمية والسعر إن وجد",13,false); help.setTextColor(Color.DKGRAY); root.addView(help);
        for(String item:itemNames) addItemRow(item);

        section("4  معلومات إضافية");
        LinearLayout checks=new LinearLayout(this); checks.setOrientation(LinearLayout.HORIZONTAL); checks.setGravity(Gravity.RIGHT);
        installation=new CheckBox(this); installation.setText("التركيب"); transport=new CheckBox(this); transport.setText("النقل"); checks.addView(installation); checks.addView(transport); root.addView(checks);
        notes=field("ملاحظات إضافية"); notes.setSingleLine(false); notes.setMinLines(3); notes.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(100))); addLabelField("ملاحظات",notes);
        organizer=field("اسم منظم الكشف"); addLabelField("اسم منظم الكشف",organizer);

        LinearLayout buttons=new LinearLayout(this); buttons.setOrientation(LinearLayout.HORIZONTAL); buttons.setGravity(Gravity.CENTER); buttons.setPadding(0,dp(18),0,0);
        Button save=button("حفظ المسودة",NAVY); Button pdf=button("إنشاء PDF",GREEN); Button clear=button("جديد",Color.DKGRAY);
        buttons.addView(save); buttons.addView(pdf); buttons.addView(clear); root.addView(buttons);
        save.setOnClickListener(v->{saveDraft();Toast.makeText(this,"تم حفظ المسودة",Toast.LENGTH_SHORT).show();});
        pdf.setOnClickListener(v->{saveDraft();createPdfAndShare();});
        clear.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("مسح جميع البيانات وبدء نموذج جديد؟").setPositiveButton("نعم",(d,w)->clearForm()).setNegativeButton("لا",null).show());
        setContentView(scroll);
    }

    private Button button(String t,int color){ Button b=new Button(this); b.setText(t); b.setTextColor(Color.WHITE); b.setBackgroundColor(color); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(50),1); lp.setMargins(dp(4),0,dp(4),0); b.setLayoutParams(lp); return b; }

    private void addItemRow(String item){
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(9),dp(8),dp(9),dp(8)); card.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(-1,-2); clp.setMargins(0,dp(4),0,dp(4)); card.setLayoutParams(clp);
        TextView name=tv(item,16,true); name.setTextColor(NAVY); card.addView(name);
        LinearLayout line=new LinearLayout(this); line.setOrientation(LinearLayout.HORIZONTAL);
        EditText model=smallField("الماركة / الموديل",3); EditText cap=smallField("السعة",2); EditText qty=smallField("الكمية",1); EditText price=smallField("السعر",2);
        line.addView(model); line.addView(cap); line.addView(qty); line.addView(price); card.addView(line); root.addView(card);
        itemRows.add(new EditText[]{model,cap,qty,price});
    }
    private EditText smallField(String hint,int weight){ EditText e=field(hint); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(48),weight); lp.setMargins(dp(2),0,dp(2),0); e.setLayoutParams(lp); e.setTextSize(13); return e; }
    private int dp(int n){ return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
    private String t(EditText e){return e.getText().toString().trim();}
    private String checkedText(RadioGroup g){int id=g.getCheckedRadioButtonId(); if(id==-1)return ""; return ((RadioButton)findViewById(id)).getText().toString();}

    private void saveDraft(){
        android.content.SharedPreferences.Editor e=getSharedPreferences("draft",0).edit();
        e.putString("client",t(clientName)).putString("phone",t(phone)).putString("location",t(location)).putString("date",t(date)).putString("kw",t(sizeKw)).putString("notes",t(notes)).putString("organizer",t(organizer));
        e.putInt("system",systemType.getCheckedRadioButtonId()).putInt("phase",phase.getCheckedRadioButtonId()).putBoolean("install",installation.isChecked()).putBoolean("transport",transport.isChecked());
        for(int i=0;i<itemRows.size();i++)for(int j=0;j<4;j++)e.putString("i"+i+"_"+j,t(itemRows.get(i)[j])); e.apply();
    }
    private void loadDraft(){
        android.content.SharedPreferences p=getSharedPreferences("draft",0); clientName.setText(p.getString("client","")); phone.setText(p.getString("phone","")); location.setText(p.getString("location",""));
        String d=p.getString("date",""); if(!d.isEmpty())date.setText(d); sizeKw.setText(p.getString("kw","")); notes.setText(p.getString("notes","")); organizer.setText(p.getString("organizer",""));
        int s=p.getInt("system",-1),ph=p.getInt("phase",-1); if(s!=-1)systemType.check(s); if(ph!=-1)phase.check(ph); installation.setChecked(p.getBoolean("install",false)); transport.setChecked(p.getBoolean("transport",false));
        for(int i=0;i<itemRows.size();i++)for(int j=0;j<4;j++)itemRows.get(i)[j].setText(p.getString("i"+i+"_"+j,""));
    }
    private void clearForm(){ getSharedPreferences("draft",0).edit().clear().apply(); clientName.setText("");phone.setText("");location.setText("");sizeKw.setText("");notes.setText("");organizer.setText("");systemType.clearCheck();phase.clearCheck();installation.setChecked(false);transport.setChecked(false);for(EditText[] r:itemRows)for(EditText e:r)e.setText("");date.setText(new SimpleDateFormat("yyyy/MM/dd",Locale.US).format(new Date())); }

    private void createPdfAndShare(){
        try{
            File dir=getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS); if(dir==null)throw new IOException("Storage unavailable"); if(!dir.exists())dir.mkdirs();
            String safe=t(clientName).replaceAll("[^\\p{L}\\p{N}_-]","_"); if(safe.isEmpty())safe="client";
            File file=new File(dir,"Solar_Form_"+safe+"_"+System.currentTimeMillis()+".pdf");
            PdfDocument doc=new PdfDocument(); PdfDocument.PageInfo pi=new PdfDocument.PageInfo.Builder(595,842,1).create(); PdfDocument.Page page=doc.startPage(pi); Canvas c=page.getCanvas(); Paint p=new Paint(1); p.setTypeface(Typeface.create("sans",Typeface.NORMAL));
            c.drawColor(Color.WHITE); drawHeader(c,p); int y=120; y=drawClient(c,p,y); y=drawSystem(c,p,y+8); y=drawItems(c,p,y+8); drawFooter(c,p,y+8);
            doc.finishPage(page); FileOutputStream out=new FileOutputStream(file); doc.writeTo(out); out.close(); doc.close();
            Uri uri=new Uri.Builder().scheme("content").authority(getPackageName()+".provider").appendPath(file.getName()).build();
            Intent share=new Intent(Intent.ACTION_SEND); share.setType("application/pdf"); share.putExtra(Intent.EXTRA_STREAM,uri); share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); startActivity(Intent.createChooser(share,"مشاركة النموذج"));
        }catch(Exception ex){Toast.makeText(this,"تعذر إنشاء PDF: "+ex.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private void rtl(Paint p,float size,int color,Paint.Align align,boolean bold){p.setTextSize(size);p.setColor(color);p.setTextAlign(align);p.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));}
    private void drawHeader(Canvas c,Paint p){ rtl(p,24,NAVY,Paint.Align.RIGHT,true); c.drawText("استمارة طلب عرض سعر منظومة شمسية",565,48,p); rtl(p,13,GREEN,Paint.Align.RIGHT,false); c.drawText("INFINITY GREEN POWER",565,72,p); p.setColor(GREEN);p.setStrokeWidth(2);c.drawLine(30,88,565,88,p); }
    private int drawClient(Canvas c,Paint p,int y){ y=bar(c,p,"1  معلومات العميل",y); y=line(c,p,"اسم العميل",t(clientName),y); y=line(c,p,"رقم الهاتف",t(phone),y); y=line(c,p,"الموقع",t(location),y); y=line(c,p,"التاريخ",t(date),y); return y; }
    private int drawSystem(Canvas c,Paint p,int y){ y=bar(c,p,"2  معلومات المنظومة",y); y=line(c,p,"نوع المنظومة",checkedText(systemType),y); y=line(c,p,"السعة المطلوبة",t(sizeKw)+(t(sizeKw).isEmpty()?"":" kW"),y); y=line(c,p,"الطور",checkedText(phase),y); return y; }
    private int drawItems(Canvas c,Paint p,int y){ y=bar(c,p,"3  المواد الرئيسية",y); rtl(p,11,Color.WHITE,Paint.Align.RIGHT,true); p.setColor(GREEN); c.drawRect(30,y,565,y+28,p); rtl(p,10,Color.WHITE,Paint.Align.CENTER,true); c.drawText("المادة",500,y+19,p);c.drawText("الماركة / الموديل",365,y+19,p);c.drawText("السعة",240,y+19,p);c.drawText("الكمية",155,y+19,p);c.drawText("السعر",75,y+19,p); y+=28;
        for(int i=0;i<itemNames.length;i++){ Paint line=new Paint();line.setColor(Color.LTGRAY);c.drawRect(30,y,565,y+30,line);line.setColor(Color.WHITE);c.drawRect(31,y+1,564,y+29,line); rtl(p,9,Color.DKGRAY,Paint.Align.RIGHT,false); c.drawText(itemNames[i],555,y+20,p); rtl(p,9,Color.DKGRAY,Paint.Align.CENTER,false); EditText[] r=itemRows.get(i);c.drawText(t(r[0]),365,y+20,p);c.drawText(t(r[1]),240,y+20,p);c.drawText(t(r[2]),155,y+20,p);c.drawText(t(r[3]),75,y+20,p); y+=30;} return y; }
    private void drawFooter(Canvas c,Paint p,int y){ y=bar(c,p,"4  معلومات إضافية",y); y=line(c,p,"التركيب",installation.isChecked()?"نعم":"لا",y); y=line(c,p,"النقل",transport.isChecked()?"نعم":"لا",y); y=line(c,p,"اسم منظم الكشف",t(organizer),y); rtl(p,10,Color.DKGRAY,Paint.Align.RIGHT,false); c.drawText("ملاحظات: "+t(notes),565,y+18,p); rtl(p,9,GREEN,Paint.Align.CENTER,false); c.drawText("نموذج مولد بواسطة Solar Form Maker",297,820,p); }
    private int bar(Canvas c,Paint p,String s,int y){p.setColor(NAVY);c.drawRect(30,y,565,y+28,p);rtl(p,13,Color.WHITE,Paint.Align.RIGHT,true);c.drawText(s,555,y+20,p);return y+34;}
    private int line(Canvas c,Paint p,String label,String value,int y){rtl(p,11,Color.DKGRAY,Paint.Align.RIGHT,true);c.drawText(label+" :",555,y+17,p);rtl(p,11,Color.BLACK,Paint.Align.RIGHT,false);c.drawText(value,430,y+17,p);p.setColor(Color.LTGRAY);p.setStrokeWidth(1);c.drawLine(30,y+24,565,y+24,p);return y+28;}
}
