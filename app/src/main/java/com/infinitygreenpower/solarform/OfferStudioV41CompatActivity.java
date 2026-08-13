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
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

/**
 * v4.1 compatibility layer over OfferStudioActivity.
 * Keeps the existing offer/saved-data engine while adding:
 * - 222-item Solar App V4 catalog
 * - English / Arabic / Kurdish (Sorani) UI
 * - Settings screen
 * - localized PDF export and price visibility
 * - safe-area behavior inherited from OfferStudioFixedActivity
 */
public class OfferStudioV41CompatActivity extends OfferStudioFixedActivity {
    private final HashMap<String,String[]> words=new HashMap<>();
    private final HashMap<String,String[]> cats=new HashMap<>();
    private SharedPreferences prefs;
    private String lang="en", currency="USD", defaultOrganizer="";
    private boolean defaultInstallation=true, defaultTransport=false, showPrices=true;
    private boolean observerBusy=false;
    private int catalogCount=0;

    @Override public void onCreate(Bundle b){
        prefs=getSharedPreferences("offer_studio_settings",MODE_PRIVATE);
        lang=prefs.getString("lang","en"); currency=prefs.getString("currency","USD");
        defaultOrganizer=prefs.getString("organizer","");
        defaultInstallation=prefs.getBoolean("installation",true);
        defaultTransport=prefs.getBoolean("transport",false);
        showPrices=prefs.getBoolean("showPrices",true);
        loadLanguageAssets();
        super.onCreate(b);
        injectCatalog();
        applyDefaultsToFreshDraft();
        invokePrivate("showHome");
        installObserver();
    }

    private boolean rtl(){return !"en".equals(lang);}
    private int langCol(){return "ar".equals(lang)?1:("ku".equals(lang)?2:0);}

    private void loadLanguageAssets(){
        words.clear();cats.clear();
        try{BufferedReader br=new BufferedReader(new InputStreamReader(getAssets().open("ui_translations.tsv"),"UTF-8"));String s;boolean first=true;
            while((s=br.readLine())!=null){if(first){first=false;continue;}String[] p=s.split("\t",-1);if(p.length>=4)words.put(p[1],new String[]{p[1],p[2],p[3]});}br.close();}catch(Exception ignored){}
        try{BufferedReader br=new BufferedReader(new InputStreamReader(getAssets().open("category_translations.tsv"),"UTF-8"));String s;boolean first=true;
            while((s=br.readLine())!=null){if(first){first=false;continue;}String[] p=s.split("\t",-1);if(p.length>=3)cats.put(p[0],new String[]{p[0],p[1],p[2]});}br.close();}catch(Exception ignored){}
    }

    private String tr(String en){String[] a=words.get(en);return a==null?en:a[langCol()];}
    private String cat(String en){String[] a=cats.get(en);return a==null?en:a[langCol()];}

    @SuppressWarnings("unchecked") private void injectCatalog(){
        try{
            Field f=OfferStudioActivity.class.getDeclaredField("catalog");f.setAccessible(true);
            ArrayList<OfferStudioActivity.Product> list=(ArrayList<OfferStudioActivity.Product>)f.get(this);list.clear();
            for(int part=1;part<=6;part++){
                try{BufferedReader br=new BufferedReader(new InputStreamReader(getAssets().open("catalog_v4_"+part+".tsv"),"UTF-8"));String s;
                    while((s=br.readLine())!=null){String[] p=s.split("\t",-1);if(p.length>=2)list.add(new OfferStudioActivity.Product(p[0],p[1],p.length>2?p[2]:""));}br.close();
                }catch(Exception ignored){}
            }
            catalogCount=list.size();
        }catch(Exception e){catalogCount=0;}
    }

    private Object privateField(String name){try{Field f=OfferStudioActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}catch(Exception e){return null;}}
    private void invokePrivate(String name){try{Method m=OfferStudioActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(this);}catch(Exception ignored){}}
    private void invokePrivate(String name,Class<?>[] types,Object[] args){try{Method m=OfferStudioActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(this,args);}catch(Exception ignored){}}

    private void applyDefaultsToFreshDraft(){
        try{Object o=privateField("draft");if(!(o instanceof OfferStudioActivity.Draft))return;OfferStudioActivity.Draft d=(OfferStudioActivity.Draft)o;
            Object step=privateField("wizardStep");int w=step instanceof Integer?(Integer)step:-1;
            if(w==0 && (d.id==null||d.id.isEmpty()) && (d.client==null||d.client.isEmpty())){
                if(d.organizer==null||d.organizer.isEmpty())d.organizer=defaultOrganizer;
                d.installation=defaultInstallation;d.transport=defaultTransport;
            }
        }catch(Exception ignored){}
    }

    private void installObserver(){
        View root=findViewById(android.R.id.content);if(root==null)return;
        root.getViewTreeObserver().addOnGlobalLayoutListener(()->{
            if(observerBusy)return;observerBusy=true;
            try{applyDefaultsToFreshDraft();translateTree(root);ensureSettingsTab();wirePdfButtons(root);}finally{observerBusy=false;}
        });
        root.post(()->{translateTree(root);ensureSettingsTab();wirePdfButtons(root);});
    }

    private void translateTree(View v){
        v.setLayoutDirection(rtl()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);
        if(v instanceof TextView && !(v instanceof EditText)){
            TextView t=(TextView)v;CharSequence cs=t.getText();if(cs!=null)t.setText(translateText(cs.toString()));
            t.setTextDirection(rtl()?View.TEXT_DIRECTION_RTL:View.TEXT_DIRECTION_LTR);
        }
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)translateTree(g.getChildAt(i));}
    }

    private String translateText(String s){
        if(s==null||s.isEmpty()||"en".equals(lang))return s;
        String[] direct=words.get(s);if(direct!=null)return direct[langCol()];
        Matcher m=Pattern.compile("Step (\\d+) of 4").matcher(s);if(m.matches())return "ar".equals(lang)?"الخطوة "+m.group(1)+" من 4":"هەنگاوی "+m.group(1)+" لە 4";
        m=Pattern.compile("(\\d+) saved").matcher(s);if(m.matches())return "ar".equals(lang)?m.group(1)+" محفوظ":m.group(1)+" پاشەکەوتکراو";
        m=Pattern.compile("(\\d+) items").matcher(s);if(m.matches())return "ar".equals(lang)?m.group(1)+" مادة":m.group(1)+" ماددە";
        m=Pattern.compile("(\\d+) ready-to-use items").matcher(s);if(m.matches())return "ar".equals(lang)?m.group(1)+" مادة جاهزة للاستخدام":m.group(1)+" ماددەی ئامادە بۆ بەکارهێنان";
        m=Pattern.compile("Site Photo (\\d+)").matcher(s);if(m.matches())return "ar".equals(lang)?"صورة الموقع "+m.group(1):"وێنەی شوێن "+m.group(1);
        for(Map.Entry<String,String[]> e:cats.entrySet()){
            String k=e.getKey();if(s.startsWith(k+" —")||s.startsWith(k+"  •")||s.startsWith(k+"\n"))return e.getValue()[langCol()]+s.substring(k.length());
        }
        if(s.startsWith("Qty "))return ("ar".equals(lang)?"الكمية ":"بڕ ")+s.substring(4);
        if(s.startsWith("Price "))return ("ar".equals(lang)?"السعر ":"نرخ ")+s.substring(6);
        return s;
    }

    private void ensureSettingsTab(){
        Object o=privateField("bottomNav");if(!(o instanceof LinearLayout))return;LinearLayout nav=(LinearLayout)o;
        boolean found=false;for(int i=0;i<nav.getChildCount();i++){View c=nav.getChildAt(i);if("v41_settings".equals(c.getTag())){found=true;break;}}
        if(found)return;
        LinearLayout b=new LinearLayout(this);b.setTag("v41_settings");b.setOrientation(LinearLayout.VERTICAL);b.setGravity(Gravity.CENTER);b.setPadding(2,2,2,2);b.setOnClickListener(v->showSettings());
        TextView icon=new TextView(this);icon.setText("⚙");icon.setTextSize(20);icon.setTextColor(Color.rgb(104,119,132));icon.setGravity(Gravity.CENTER);
        TextView label=new TextView(this);label.setText(tr("Settings"));label.setTextSize(9);label.setTextColor(Color.rgb(104,119,132));label.setGravity(Gravity.CENTER);label.setSingleLine(true);
        b.addView(icon);b.addView(label);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1f);lp.leftMargin=2;lp.rightMargin=2;nav.addView(b,lp);
    }

    private void showSettings(){
        final Dialog d=new Dialog(this);d.setTitle(tr("Settings"));
        ScrollView sv=new ScrollView(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(12),dp(18),dp(18));box.setLayoutDirection(rtl()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);sv.addView(box);
        TextView head=title(tr("Settings"),24,true);box.addView(head);box.addView(sub(tr("Configure language, currency and offer defaults.")));
        box.addView(sectionLabel(tr("App language")));Spinner language=new Spinner(this);String[] langs={"English","العربية","کوردی"};language.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,langs));language.setSelection("ar".equals(lang)?1:("ku".equals(lang)?2:0));box.addView(language,new LinearLayout.LayoutParams(-1,dp(52)));
        box.addView(sectionLabel(tr("Default currency")));Spinner curr=new Spinner(this);curr.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"USD","IQD"}));curr.setSelection("IQD".equals(currency)?1:0);box.addView(curr,new LinearLayout.LayoutParams(-1,dp(52)));
        box.addView(sectionLabel(tr("Default inspection organizer")));EditText org=new EditText(this);org.setText(defaultOrganizer);org.setHint(tr("Organizer name"));org.setSingleLine(true);org.setPadding(dp(12),0,dp(12),0);box.addView(org,new LinearLayout.LayoutParams(-1,dp(52)));
        CheckBox inst=new CheckBox(this);inst.setText(tr("Include installation by default"));inst.setChecked(defaultInstallation);box.addView(inst);
        CheckBox trans=new CheckBox(this);trans.setText(tr("Include transport by default"));trans.setChecked(defaultTransport);box.addView(trans);
        CheckBox prices=new CheckBox(this);prices.setText(tr("Show prices in preview and PDF"));prices.setChecked(showPrices);box.addView(prices);
        TextView info=sub((catalogCount>0?catalogCount:222)+" "+tr("Solar App V4 catalog items included"));LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,-2);ip.topMargin=dp(12);info.setLayoutParams(ip);box.addView(info);
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);actions.setPadding(0,dp(16),0,0);Button cancel=new Button(this);cancel.setText(tr("Cancel"));Button save=new Button(this);save.setText(tr("Save settings"));actions.addView(cancel,new LinearLayout.LayoutParams(0,dp(50),1));actions.addView(save,new LinearLayout.LayoutParams(0,dp(50),1));box.addView(actions);
        cancel.setOnClickListener(v->d.dismiss());save.setOnClickListener(v->{int li=language.getSelectedItemPosition();lang=li==1?"ar":(li==2?"ku":"en");currency=String.valueOf(curr.getSelectedItem());defaultOrganizer=org.getText().toString().trim();defaultInstallation=inst.isChecked();defaultTransport=trans.isChecked();showPrices=prices.isChecked();prefs.edit().putString("lang",lang).putString("currency",currency).putString("organizer",defaultOrganizer).putBoolean("installation",defaultInstallation).putBoolean("transport",defaultTransport).putBoolean("showPrices",showPrices).apply();d.dismiss();recreate();});
        d.setContentView(sv);d.show();Window w=d.getWindow();if(w!=null)w.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.94),WindowManager.LayoutParams.MATCH_PARENT);
    }

    private TextView title(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.rgb(18,32,45));if(bold)t.setTypeface(Typeface.DEFAULT_BOLD);t.setGravity(rtl()?Gravity.RIGHT:Gravity.LEFT);return t;}
    private TextView sub(String s){TextView t=title(s,13,false);t.setTextColor(Color.rgb(104,119,132));t.setPadding(0,3,0,8);return t;}
    private TextView sectionLabel(String s){TextView t=title(s,13,true);t.setPadding(0,dp(15),0,dp(6));return t;}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}

    private void wirePdfButtons(View v){
        if(v instanceof Button){Button b=(Button)v;String s=String.valueOf(b.getText());if(isPdfLabel(s)&&!"v41_pdf".equals(b.getTag())){b.setTag("v41_pdf");b.setOnClickListener(x->{invokePrivate("saveOffer");createLocalizedPdf();});}}
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)wirePdfButtons(g.getChildAt(i));}
    }
    private boolean isPdfLabel(String s){return "Export PDF".equals(s)||"تصدير PDF".equals(s)||"PDF هەناردە بکە".equals(s)||"تصدير ملف PDF".equals(s);}

    private String p(String value){if(value==null||value.trim().isEmpty())return "—";return value.trim()+("USD".equals(currency)?" $":" IQD");}
    private void createLocalizedPdf(){
        try{
            Object x=privateField("draft");if(!(x instanceof OfferStudioActivity.Draft))return;OfferStudioActivity.Draft dr=(OfferStudioActivity.Draft)x;
            File dir=getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);if(dir==null)throw new Exception("Storage unavailable");dir.mkdirs();String safe=dr.client.replaceAll("[^\\p{L}\\p{N}_-]","_");if(safe.isEmpty())safe="Offer";File f=new File(dir,"IGP_Offer_"+safe+"_"+System.currentTimeMillis()+".pdf");
            PdfDocument doc=new PdfDocument();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);PdfDocument.Page pg=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,1).create());Canvas c=pg.getCanvas();c.drawColor(Color.WHITE);float X=rtl()?555:40;pdfText(c,paint,"INFINITY GREEN POWER",X,42,24,true,Color.rgb(15,54,89));pdfText(c,paint,tr("Solar System Offer"),X,66,13,true,Color.rgb(20,153,112));paint.setColor(Color.rgb(20,153,112));c.drawRect(40,82,555,85,paint);int y=105;
            y=pdfSection(c,paint,y,tr("Client").toUpperCase());y=pdfRow(c,paint,y,tr("Client"),dash(dr.client));y=pdfRow(c,paint,y,tr("Phone"),dash(dr.phone));y=pdfRow(c,paint,y,tr("Location"),dash(dr.location));y=pdfRow(c,paint,y,tr("Date"),dash(dr.date));y+=7;
            y=pdfSection(c,paint,y,tr("System").toUpperCase());y=pdfRow(c,paint,y,tr("System type"),translateText(dr.system));y=pdfRow(c,paint,y,tr("Required capacity"),dash(dr.capacity));y=pdfRow(c,paint,y,tr("Phase"),translateText(dr.phase));y+=7;
            y=pdfSection(c,paint,y,tr("Items").toUpperCase());pdfText(c,paint,tr("Item / model"),rtl()?555:45,y+15,9,true,Color.rgb(20,153,112));pdfText(c,paint,tr("Capacity / specification"),rtl()?310:285,y+15,9,true,Color.rgb(20,153,112));pdfText(c,paint,tr("Quantity"),rtl()?160:430,y+15,9,true,Color.rgb(20,153,112));if(showPrices)pdfText(c,paint,tr("Price"),rtl()?80:490,y+15,9,true,Color.rgb(20,153,112));y+=22;
            for(OfferStudioActivity.LineItem it:dr.items){pdfText(c,paint,trim(it.name,31),rtl()?555:45,y+14,9,false,Color.rgb(18,32,45));pdfText(c,paint,trim(it.spec,19),rtl()?310:285,y+14,9,false,Color.rgb(18,32,45));pdfText(c,paint,it.qty==null||it.qty.isEmpty()?"1":it.qty,rtl()?160:430,y+14,9,false,Color.rgb(18,32,45));if(showPrices)pdfText(c,paint,p(it.price),rtl()?80:490,y+14,9,false,Color.rgb(18,32,45));paint.setColor(Color.rgb(225,231,237));c.drawLine(40,y+22,555,y+22,paint);y+=26;if(y>685)break;}
            y+=7;y=pdfSection(c,paint,y,tr("Details").toUpperCase());y=pdfRow(c,paint,y,tr("Installation"),dr.installation?tr("Included"):tr("Not included"));y=pdfRow(c,paint,y,tr("Transport"),dr.transport?tr("Included"):tr("Not included"));y=pdfRow(c,paint,y,tr("Inspection organizer"),dash(dr.organizer));y=pdfRow(c,paint,y,tr("Site photos"),String.valueOf(dr.photos.size()));if(dr.notes!=null&&!dr.notes.isEmpty())pdfText(c,paint,tr("Notes")+": "+trim(dr.notes,72),rtl()?555:40,y+20,9,false,Color.rgb(104,119,132));
            pdfText(c,paint,tr("Generated by IGP Offer Studio"),X,820,9,false,Color.rgb(20,153,112));doc.finishPage(pg);
            FileOutputStream out=new FileOutputStream(f);doc.writeTo(out);out.close();doc.close();Uri u=new Uri.Builder().scheme("content").authority(getPackageName()+".provider").appendPath(f.getName()).build();Intent sh=new Intent(Intent.ACTION_SEND);sh.setType("application/pdf");sh.putExtra(Intent.EXTRA_STREAM,u);sh.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(sh,tr("Share offer")));
        }catch(Exception e){Toast.makeText(this,"PDF: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private String dash(String s){return s==null||s.trim().isEmpty()?"—":s.trim();}
    private String trim(String s,int n){if(s==null)return"";return s.length()<=n?s:s.substring(0,n-1)+"…";}
    private void pdfText(Canvas c,Paint p,String s,float x,float y,float z,boolean bold,int color){p.setColor(color);p.setTextSize(z);p.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);p.setTextAlign(rtl()?Paint.Align.RIGHT:Paint.Align.LEFT);c.drawText(s==null?"":s,x,y,p);}
    private int pdfSection(Canvas c,Paint p,int y,String s){p.setColor(Color.rgb(15,54,89));c.drawRoundRect(40,y,555,y+24,9,9,p);pdfText(c,p,s,rtl()?545:50,y+16,11,true,Color.WHITE);return y+32;}
    private int pdfRow(Canvas c,Paint p,int y,String k,String v){if(rtl()){pdfText(c,p,k,545,y+15,10,true,Color.rgb(104,119,132));pdfText(c,p,v,385,y+15,10,false,Color.rgb(18,32,45));}else{pdfText(c,p,k,45,y+15,10,true,Color.rgb(104,119,132));pdfText(c,p,v,180,y+15,10,false,Color.rgb(18,32,45));}p.setColor(Color.rgb(225,231,237));c.drawLine(40,y+23,555,y+23,p);return y+27;}
}
