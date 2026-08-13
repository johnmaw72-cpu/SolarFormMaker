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

/** v4.1.1: product/catalog rows are intentionally name-only. */
public class OfferStudioV411Activity extends OfferStudioV41CompatActivity {
    private boolean busy=false;
    private final HashSet<String> categoryLabels=new HashSet<>();

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        seedCategoryLabels();
        stripCatalogDetails();
        stripDraftDetails();
        View root=findViewById(android.R.id.content);
        if(root!=null){
            root.getViewTreeObserver().addOnGlobalLayoutListener(()->{
                if(busy)return;busy=true;
                try{stripCatalogDetails();stripDraftDetails();simplifyTree(root);wireNameOnlyPdf(root);}finally{busy=false;}
            });
            root.post(()->{stripCatalogDetails();stripDraftDetails();simplifyTree(root);wireNameOnlyPdf(root);});
        }
    }

    private void seedCategoryLabels(){
        String[] a={
            "Solar Inverter","Solar Panels","Inverters","Lithium Battery","Batteries","Battery Control","Accessories / Structures","Battery Rack","Installation / Delivery","Solar Array Structure","Accessories","Lightning System","Earthing System","Delivery","Installation","Other",
            "إنفرتر شمسي","ألواح شمسية","إنفرترات","بطارية ليثيوم","بطاريات","تحكم البطاريات","ملحقات / هياكل","راك بطاريات","تركيب / نقل","هياكل الألواح الشمسية","ملحقات","نظام الحماية من الصواعق","نظام التأريض","نقل","تركيب","أخرى",
            "ئینڤێرتەری خۆرەوی","پانێڵی خۆرەوی","ئینڤێرتەرەکان","باتری لیتھیۆم","باترییەکان","کۆنترۆڵی باتری","پێداویستی / هەیکەل","ڕاکی باتری","دامەزراندن / گواستنەوە","هەیکەلی پانێڵی خۆرەوی","پێداویستییەکان","سیستەمی پاراستن لە برووسکە","سیستەمی ئەرث","گواستنەوە","دامەزراندن","هیتر"
        };
        categoryLabels.addAll(Arrays.asList(a));
    }

    @SuppressWarnings("unchecked") private void stripCatalogDetails(){
        try{Field f=OfferStudioActivity.class.getDeclaredField("catalog");f.setAccessible(true);Object o=f.get(this);if(o instanceof ArrayList){for(Object x:(ArrayList<?>)o)if(x instanceof OfferStudioActivity.Product)((OfferStudioActivity.Product)x).spec="";}}catch(Exception ignored){}
    }

    private void stripDraftDetails(){
        try{Field f=OfferStudioActivity.class.getDeclaredField("draft");f.setAccessible(true);Object o=f.get(this);if(o instanceof OfferStudioActivity.Draft){OfferStudioActivity.Draft d=(OfferStudioActivity.Draft)o;for(OfferStudioActivity.LineItem x:d.items)x.spec="";}}catch(Exception ignored){}
    }

    private void simplifyTree(View v){
        if(v instanceof EditText){
            EditText e=(EditText)v;CharSequence h=e.getHint();
            if(h!=null){String s=h.toString();if(s.equals("Capacity / specification")||s.equals("السعة / المواصفة")||s.equals("توانا / تایبەتمەندی")){e.setText("");e.setVisibility(View.GONE);}}
        } else if(v instanceof TextView){
            TextView t=(TextView)v;String s=String.valueOf(t.getText()).trim();
            if(categoryLabels.contains(s)){t.setVisibility(View.GONE);}
            else {
                String simple=simplifyCatalogLabel(s);if(!simple.equals(s))t.setText(simple);
                if(s.equals("Add products from the catalog or create custom rows.")){
                    String l=getSharedPreferences("offer_studio_settings",MODE_PRIVATE).getString("lang","en");
                    if("ar".equals(l))t.setText("اختر المواد من الكتالوج أو أضف مادة مخصصة.");
                    else if("ku".equals(l))t.setText("ماددە لە کاتەلۆگ هەڵبژێرە یان ماددەی تایبەت زیاد بکە.");
                }
            }
        }
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)simplifyTree(g.getChildAt(i));}
    }

    private String simplifyCatalogLabel(String s){
        String[] seps={"  •  "," — ","\n"};
        for(String sep:seps){int p=s.indexOf(sep);if(p>0){String prefix=s.substring(0,p).trim();if(categoryLabels.contains(prefix))return s.substring(p+sep.length()).trim();}}
        return s;
    }

    private void wireNameOnlyPdf(View v){
        if(v instanceof Button){Button b=(Button)v;String s=String.valueOf(b.getText());if(isPdfText(s)&&!"v411_pdf".equals(b.getTag())){b.setTag("v411_pdf");b.setOnClickListener(x->{invokeParentSave();createNameOnlyPdf();});}}
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)wireNameOnlyPdf(g.getChildAt(i));}
    }
    private boolean isPdfText(String s){return "Export PDF".equals(s)||"تصدير PDF".equals(s)||"PDF دەربکە".equals(s)||"PDF هەناردە بکە".equals(s)||"تصدير ملف PDF".equals(s);}
    private void invokeParentSave(){try{Method m=OfferStudioActivity.class.getDeclaredMethod("saveOffer");m.setAccessible(true);m.invoke(this);}catch(Exception ignored){}}
    private OfferStudioActivity.Draft draft(){try{Field f=OfferStudioActivity.class.getDeclaredField("draft");f.setAccessible(true);return (OfferStudioActivity.Draft)f.get(this);}catch(Exception e){return null;}}

    private String lang(){return getSharedPreferences("offer_studio_settings",MODE_PRIVATE).getString("lang","en");}
    private String currency(){return getSharedPreferences("offer_studio_settings",MODE_PRIVATE).getString("currency","USD");}
    private boolean prices(){return getSharedPreferences("offer_studio_settings",MODE_PRIVATE).getBoolean("showPrices",true);}
    private boolean rtl(){return !"en".equals(lang());}
    private String tx(String en,String ar,String ku){return "ar".equals(lang())?ar:("ku".equals(lang())?ku:en);}
    private String dash(String s){return s==null||s.trim().isEmpty()?"—":s.trim();}
    private String price(String s){if(s==null||s.trim().isEmpty())return "—";return s.trim()+("IQD".equals(currency())?" IQD":" $");}
    private String trim(String s,int n){if(s==null)return"";return s.length()<=n?s:s.substring(0,n-1)+"…";}

    private void createNameOnlyPdf(){
        OfferStudioActivity.Draft d=draft();if(d==null)return;stripDraftDetails();
        try{
            File dir=getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);if(dir==null)throw new Exception("Storage unavailable");dir.mkdirs();
            String safe=d.client.replaceAll("[^\\p{L}\\p{N}_-]","_");if(safe.isEmpty())safe="Offer";File f=new File(dir,"IGP_Offer_"+safe+"_"+System.currentTimeMillis()+".pdf");
            PdfDocument doc=new PdfDocument();Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);PdfDocument.Page pg=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,1).create());Canvas c=pg.getCanvas();c.drawColor(Color.WHITE);float X=rtl()?555:40;
            text(c,p,"INFINITY GREEN POWER",X,42,24,true,Color.rgb(15,54,89));text(c,p,tx("Solar System Offer","عرض منظومة شمسية","پێشنیاری سیستەمی خۆرەوی"),X,66,13,true,Color.rgb(20,153,112));p.setColor(Color.rgb(20,153,112));c.drawRect(40,82,555,85,p);int y=105;
            y=section(c,p,y,tx("CLIENT","العميل","کڕیار"));y=row(c,p,y,tx("Client","العميل","کڕیار"),dash(d.client));y=row(c,p,y,tx("Phone","رقم الهاتف","ژمارەی تەلەفۆن"),dash(d.phone));y=row(c,p,y,tx("Location","الموقع","شوێن"),dash(d.location));y=row(c,p,y,tx("Date","التاريخ","بەروار"),dash(d.date));y+=7;
            y=section(c,p,y,tx("SYSTEM","المنظومة","سیستەم"));y=row(c,p,y,tx("Type","النوع","جۆر"),dash(d.system));y=row(c,p,y,tx("Capacity","السعة","توانا"),dash(d.capacity));y=row(c,p,y,tx("Phase","الطور","فاز"),dash(d.phase));y+=7;
            y=section(c,p,y,tx("ITEMS","المواد","ماددەکان"));
            text(c,p,tx("ITEM","المادة","ماددە"),rtl()?555:45,y+15,9,true,Color.rgb(20,153,112));text(c,p,tx("QTY","الكمية","بڕ"),rtl()?190:410,y+15,9,true,Color.rgb(20,153,112));if(prices())text(c,p,tx("PRICE","السعر","نرخ"),rtl()?85:490,y+15,9,true,Color.rgb(20,153,112));y+=22;
            for(OfferStudioActivity.LineItem it:d.items){text(c,p,trim(it.name,48),rtl()?555:45,y+14,9,false,Color.rgb(18,32,45));text(c,p,it.qty==null||it.qty.isEmpty()?"1":it.qty,rtl()?190:415,y+14,9,false,Color.rgb(18,32,45));if(prices())text(c,p,price(it.price),rtl()?85:490,y+14,9,false,Color.rgb(18,32,45));p.setColor(Color.rgb(225,231,237));c.drawLine(40,y+22,555,y+22,p);y+=26;if(y>690)break;}
            y+=7;y=section(c,p,y,tx("DETAILS","التفاصيل","وردەکاری"));y=row(c,p,y,tx("Installation","التركيب","دامەزراندن"),d.installation?tx("Included","مشمول","ناوخۆکراوە"):tx("Not included","غير مشمول","ناوخۆنەکراوە"));y=row(c,p,y,tx("Transport","النقل","گواستنەوە"),d.transport?tx("Included","مشمول","ناوخۆکراوە"):tx("Not included","غير مشمول","ناوخۆنەکراوە"));y=row(c,p,y,tx("Organizer","منظم الكشف","ڕێکخەر"),dash(d.organizer));
            text(c,p,"Infinity Green Power · IGP Offer Studio",X,820,9,false,Color.rgb(20,153,112));doc.finishPage(pg);FileOutputStream out=new FileOutputStream(f);doc.writeTo(out);out.close();doc.close();Uri u=new Uri.Builder().scheme("content").authority(getPackageName()+".provider").appendPath(f.getName()).build();Intent sh=new Intent(Intent.ACTION_SEND);sh.setType("application/pdf");sh.putExtra(Intent.EXTRA_STREAM,u);sh.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(sh,tx("Share offer","مشاركة العرض","هاوبەشکردنی پێشنیار")));
        }catch(Exception e){Toast.makeText(this,"PDF: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private void text(Canvas c,Paint p,String s,float x,float y,float z,boolean bold,int color){p.setColor(color);p.setTextSize(z);p.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);p.setTextAlign(rtl()?Paint.Align.RIGHT:Paint.Align.LEFT);c.drawText(s==null?"":s,x,y,p);}
    private int section(Canvas c,Paint p,int y,String s){p.setColor(Color.rgb(15,54,89));c.drawRoundRect(40,y,555,y+24,9,9,p);text(c,p,s,rtl()?545:50,y+16,11,true,Color.WHITE);return y+32;}
    private int row(Canvas c,Paint p,int y,String k,String v){if(rtl()){text(c,p,k,545,y+15,10,true,Color.rgb(104,119,132));text(c,p,v,370,y+15,10,false,Color.rgb(18,32,45));}else{text(c,p,k,45,y+15,10,true,Color.rgb(104,119,132));text(c,p,v,180,y+15,10,false,Color.rgb(18,32,45));}p.setColor(Color.rgb(225,231,237));c.drawLine(40,y+23,555,y+23,p);return y+27;}
}
