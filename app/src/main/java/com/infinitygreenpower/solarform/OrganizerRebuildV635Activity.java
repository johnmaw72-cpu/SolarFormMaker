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
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;

/** v6.3.5: restore the proven professional PDF visual template, retain modern pagination. */
public class OrganizerRebuildV635Activity extends OrganizerRebuildV634Activity {
    private static final int SAVE_PDF_635=6353;
    private static final int NAVY=Color.rgb(19,50,76), TEAL=Color.rgb(28,196,163), TEAL_DARK=Color.rgb(18,158,132), MUTED=Color.rgb(112,126,140), BORDER=Color.rgb(224,230,236);
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Set<View> bound=java.util.Collections.newSetFromMap(new WeakHashMap<View,Boolean>());
    private ViewTreeObserver.OnGlobalLayoutListener listener;
    private File pending;
    private final DecimalFormat money=new DecimalFormat("#,##0.##");

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        View root=findViewById(android.R.id.content);
        if(root!=null){listener=()->{handler.removeCallbacks(bindButtons);handler.postDelayed(bindButtons,140);};root.getViewTreeObserver().addOnGlobalLayoutListener(listener);}handler.postDelayed(bindButtons,240);
    }
    @Override protected void onDestroy(){View root=findViewById(android.R.id.content);if(root!=null&&listener!=null&&root.getViewTreeObserver().isAlive())root.getViewTreeObserver().removeOnGlobalLayoutListener(listener);handler.removeCallbacks(bindButtons);super.onDestroy();}

    private String lang(){return settings().getString("lang","ar");}
    private boolean rtl(){return !"en".equals(lang());}
    private String tx(String e,String a,String k){return "ar".equals(lang())?a:("ku".equals(lang())?k:e);}
    private boolean eq(String s,String e,String a,String k){return s.equals(e)||s.equals(a)||s.equals(k);}

    private final Runnable bindButtons=()->{View root=findViewById(android.R.id.content);if(root!=null)walk(root);};
    private void walk(View v){
        if(v instanceof Button&&!bound.contains(v)){
            Button b=(Button)v;String s=String.valueOf(b.getText()).trim();
            if(eq(s,"Share PDF","مشاركة PDF","PDF هاوبەش بکە")){
                bound.add(v);b.setOnClickListener(x->{if(!validate())return;File f=makeLegacyPdf();if(f!=null)share(f);});
            }else if(eq(s,"Save PDF","حفظ PDF","PDF پاشەکەوت بکە")){
                bound.add(v);b.setOnClickListener(x->{if(!validate())return;pending=makeLegacyPdf();if(pending!=null){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/pdf");i.putExtra(Intent.EXTRA_TITLE,pending.getName());startActivityForResult(i,SAVE_PDF_635);}});
            }
        }
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)walk(g.getChildAt(i));}
    }

    private boolean validate(){
        Object d=draft();ArrayList<String> miss=new ArrayList<>();if(d==null)return false;
        if(get(d,"client").trim().isEmpty())miss.add(tx("Client name","اسم العميل","ناوی کڕیار"));
        if(get(d,"phone").trim().isEmpty())miss.add(tx("Phone","رقم الهاتف","تەلەفۆن"));
        if(get(d,"location").trim().isEmpty())miss.add(tx("Location","الموقع","شوێن"));
        if(get(d,"capacity").trim().isEmpty())miss.add(tx("System capacity","سعة المنظومة","توانای سیستەم"));
        if(get(d,"organizer").trim().isEmpty())miss.add(tx("Inspection organizer","منظم الكشف","ڕێکخەری پشکنین"));
        if(items().isEmpty())miss.add(tx("At least one material","مادة واحدة على الأقل","لانیکەم یەک ماددە"));
        if(miss.isEmpty())return true;
        StringBuilder m=new StringBuilder(tx("Please complete these fields before exporting:","أكمل هذه البيانات قبل التصدير:","پێش هەناردەکردن ئەم زانیارییانە پڕ بکەرەوە:")).append("\n\n");
        for(String x:miss)m.append("• ").append(x).append("\n");
        new AlertDialog.Builder(this).setTitle(tx("Missing information","بيانات ناقصة","زانیاری کەمە")).setMessage(m.toString()).setPositiveButton(tx("OK","حسناً","باشە"),null).show();return false;
    }

    private File makeLegacyPdf(){
        Object d=draft();if(d==null)return null;
        try{
            File dir=getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS);if(dir==null)return null;
            File out=new File(dir,baseName(d)+".pdf");
            PdfDocument doc=new PdfDocument();LegacyWriter w=new LegacyWriter(doc,d);w.start();w.client();w.systemAndOrganizer();w.materials();w.notes();w.finish();appendPhotos(doc);
            FileOutputStream os=new FileOutputStream(out);doc.writeTo(os);os.close();doc.close();return out;
        }catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();return null;}
    }

    private class LegacyWriter {
        final PdfDocument doc;final Object d;PdfDocument.Page page;Canvas c;Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);int y=0,pageNo=0;
        LegacyWriter(PdfDocument doc,Object d){this.doc=doc;this.d=d;}
        void start(){newPage(false);}
        void newPage(boolean continuation){if(page!=null){footer();doc.finishPage(page);}page=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,++pageNo).create());c=page.getCanvas();c.drawColor(Color.WHITE);if(continuation)continuationHeader();else mainHeader();}
        void mainHeader(){
            Bitmap logo=logo();
            if(logo!=null){float s=Math.min(90f/logo.getWidth(),54f/logo.getHeight());int ww=(int)(logo.getWidth()*s),hh=(int)(logo.getHeight()*s);int left=rtl()?555-ww:40;c.drawBitmap(logo,null,new Rect(left,24,left+ww,24+hh),null);}
            float textX=rtl()?430:145;
            draw(settings().getString("company_name","Infinity Green Power"),textX,39,20,true,NAVY);
            draw(tx("Organizer Preliminary Solar Form","استمارة أولية للمنظومة الشمسية","فۆرمی سەرەتایی سیستەمی خۆرەوی"),textX,63,11,true,TEAL_DARK);
            p.setColor(TEAL);c.drawRoundRect(40,92,555,96,2,2,p);y=113;
        }
        void continuationHeader(){
            draw(settings().getString("company_name","Infinity Green Power"),rtl()?555:40,40,14,true,NAVY);
            draw(tx("Form continued","تكملة الاستمارة","بەردەوامی فۆرم")+" - "+tx("Page","صفحة","لاپەڕە")+" "+pageNo,rtl()?555:40,60,9,false,MUTED);
            p.setColor(TEAL);c.drawRect(40,72,555,75,p);y=92;
        }
        void footer(){draw(settings().getString("company_name","Infinity Green Power")+" · "+tx("Organizer Form","استمارة المنظم","فۆرمی ڕێکخەر"),rtl()?555:40,820,8,false,TEAL_DARK);}
        void require(int need){if(y+need>770)newPage(true);}
        void section(String title,int bodyNeed){require(31+bodyNeed);p.setColor(NAVY);c.drawRoundRect(40,y,555,y+23,7,7,p);draw(title,rtl()?545:50,y+16,10,true,Color.WHITE);y+=31;}
        void row(String key,String value){require(27);draw(key,rtl()?545:45,y+15,8,true,NAVY);draw(value,rtl()?365:180,y+15,10,false,NAVY);p.setColor(BORDER);c.drawLine(40,y+23,555,y+23,p);y+=27;}

        void client(){
            section(tx("Client","العميل","کڕیار"),108);
            row(tx("Client","العميل","کڕیار"),get(d,"client"));
            row(tx("Phone","الهاتف","تەلەفۆن"),get(d,"phone"));
            row(tx("Location","الموقع","شوێن"),get(d,"location"));
            row(tx("Date","التاريخ","بەروار"),get(d,"date"));
        }
        void systemAndOrganizer(){
            section(tx("System","المنظومة","سیستەم"),108);
            row(tx("Type","النوع","جۆر"),get(d,"system"));
            row(tx("Capacity","السعة","توانا"),get(d,"capacity"));
            row(tx("Phase","الطور","فاز"),get(d,"phase"));
            row(tx("Inspection organizer","منظم الكشف","ڕێکخەری پشکنین"),get(d,"organizer"));
        }
        void materials(){
            section(tx("Materials","المواد","ماددە"),52);tableHeader();
            for(Object it:items()){
                if(y+27>770){newPage(true);section(tx("Materials continued","تكملة المواد","بەردەوامی ماددەکان"),52);tableHeader();}
                itemRow(it);
            }
            if(settings().getBoolean("pdf_show_total",true)){
                if(y+32>770){newPage(true);section(tx("Materials summary","ملخص المواد","پوختەی ماددەکان"),32);}
                draw(tx("Estimated total","المجموع التقديري","کۆی خەمڵاندراو")+": "+money.format(grand())+" "+settings().getString("currency","USD"),rtl()?545:45,y+19,11,true,NAVY);y+=32;
            }
        }
        void tableHeader(){
            require(22);draw(tx("Item","المادة","ماددە"),rtl()?545:45,y+13,8,true,NAVY);draw(tx("Qty","الكمية","بڕ"),rtl()?205:355,y+13,8,true,NAVY);
            if(settings().getBoolean("showPrices",true)){draw(tx("Price","السعر","نرخ"),rtl()?145:425,y+13,8,true,NAVY);draw(tx("Total","المجموع","کۆ"),rtl()?75:500,y+13,8,true,NAVY);}p.setColor(BORDER);c.drawLine(40,y+20,555,y+20,p);y+=22;
        }
        void itemRow(Object it){
            draw(trim(get(it,"name"),43),rtl()?545:45,y+15,9,false,NAVY);draw(get(it,"qty"),rtl()?205:355,y+15,9,false,NAVY);
            if(settings().getBoolean("showPrices",true)){draw(get(it,"price"),rtl()?145:425,y+15,9,false,NAVY);draw(money.format(lineTotal(it)),rtl()?75:500,y+15,9,false,NAVY);}p.setColor(BORDER);c.drawLine(40,y+23,555,y+23,p);y+=27;
        }
        void notes(){
            ArrayList<String> lines=wrap(get(d,"notes"),72);int need=Math.min(175,Math.max(42,lines.size()*18));section(tx("Load / Notes","التحميل / الملاحظات","بار / تێبینی"),need);
            for(String line:lines){if(y+19>770){newPage(true);section(tx("Load / Notes continued","تكملة التحميل / الملاحظات","بەردەوامی بار / تێبینی"),36);}draw(line,rtl()?545:45,y+14,9,false,NAVY);y+=18;}
        }
        void finish(){if(page!=null){footer();doc.finishPage(page);page=null;}}
        void draw(String s,float x,float yy,float size,boolean bold,int color){p.setColor(color);p.setTextSize(size);p.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);p.setTextAlign(rtl()?Paint.Align.RIGHT:Paint.Align.LEFT);c.drawText(s==null?"":s,x,yy,p);}
        String trim(String s,int n){return s==null?"":(s.length()>n?s.substring(0,n-1)+"…":s);}
    }

    private void appendPhotos(PdfDocument doc){
        if(!settings().getBoolean("include_photos_pdf",true))return;
        ArrayList<String> ph=photos();Set<String> include=includedPhotos();
        for(String s:ph){if(!include.isEmpty()&&!include.contains(s))continue;try{Uri u=Uri.parse(s);InputStream in=getContentResolver().openInputStream(u);Bitmap b=BitmapFactory.decodeStream(in);if(in!=null)in.close();if(b==null)continue;PdfDocument.Page pg=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,doc.getPages().size()+1).create());Canvas c=pg.getCanvas();c.drawColor(Color.WHITE);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(NAVY);p.setTextSize(20);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(rtl()?Paint.Align.RIGHT:Paint.Align.LEFT);c.drawText(tx("Site photo","صورة الموقع","وێنەی شوێن"),rtl()?555:40,52,p);p.setColor(TEAL);c.drawRect(40,68,555,71,p);float scale=Math.min(515f/b.getWidth(),690f/b.getHeight());int ww=(int)(b.getWidth()*scale),hh=(int)(b.getHeight()*scale);int left=(595-ww)/2,top=92;c.drawBitmap(b,null,new Rect(left,top,left+ww,top+hh),null);p.setColor(TEAL_DARK);p.setTextSize(8);p.setTypeface(Typeface.DEFAULT);p.setTextAlign(rtl()?Paint.Align.RIGHT:Paint.Align.LEFT);c.drawText(settings().getString("company_name","Infinity Green Power")+" · "+tx("Site photo","صورة الموقع","وێنەی شوێن"),rtl()?555:40,820,p);doc.finishPage(pg);b.recycle();}catch(Exception ignored){}}
    }

    private SharedPreferences settings(){return getSharedPreferences("offer_studio_settings",MODE_PRIVATE);}
    @SuppressWarnings("unchecked") private ArrayList<Object> items(){try{Field f=OrganizerRebuildActivity.class.getDeclaredField("items");f.setAccessible(true);return(ArrayList<Object>)f.get(this);}catch(Exception e){return new ArrayList<>();}}
    @SuppressWarnings("unchecked") private ArrayList<String> photos(){try{Field f=OrganizerRebuildActivity.class.getDeclaredField("photos");f.setAccessible(true);return(ArrayList<String>)f.get(this);}catch(Exception e){return new ArrayList<>();}}
    @SuppressWarnings("unchecked") private Set<String> includedPhotos(){try{Field f=OrganizerRebuildActivity.class.getDeclaredField("includedPhotos");f.setAccessible(true);return(Set<String>)f.get(this);}catch(Exception e){return new HashSet<>();}}
    private Object draft(){try{Field f=OrganizerRebuildActivity.class.getDeclaredField("draft");f.setAccessible(true);return f.get(this);}catch(Exception e){return null;}}
    private String get(Object o,String n){try{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);Object v=f.get(o);return v==null?"":String.valueOf(v);}catch(Exception e){return"";}}
    private BigDecimal num(String s){try{return new BigDecimal(s==null||s.trim().isEmpty()?"0":s.replace(",","").replace("$","").trim());}catch(Exception e){return BigDecimal.ZERO;}}
    private BigDecimal lineTotal(Object it){BigDecimal q=num(get(it,"qty"));if(q.signum()==0)q=BigDecimal.ONE;return q.multiply(num(get(it,"price")));}
    private BigDecimal grand(){BigDecimal x=BigDecimal.ZERO;for(Object it:items())x=x.add(lineTotal(it));return x;}
    private ArrayList<String> wrap(String s,int max){ArrayList<String> out=new ArrayList<>();if(s==null||s.trim().isEmpty()){out.add("—");return out;}for(String raw:s.split("\n")){String line="";for(String word:raw.trim().split("\\s+")){if((line+" "+word).trim().length()>max){if(!line.isEmpty())out.add(line);line=word;}else line=(line+" "+word).trim();}if(!line.isEmpty())out.add(line);}return out;}
    private Bitmap logo(){String u=settings().getString("logo_uri","");if(u.isEmpty())return null;try{InputStream in=getContentResolver().openInputStream(Uri.parse(u));Bitmap b=BitmapFactory.decodeStream(in);if(in!=null)in.close();return b;}catch(Exception e){return null;}}
    private String baseName(Object d){String c=get(d,"client").replaceAll("[^A-Za-z0-9\\u0600-\\u06FF_-]+","_");if(c.isEmpty())c="Client";String cap=get(d,"capacity").replaceAll("[^A-Za-z0-9]+","");return c+"_"+(cap.isEmpty()?"Solar":cap)+"_"+get(d,"date").replace("/","-");}
    private void share(File f){Uri u=new Uri.Builder().scheme("content").authority(getPackageName()+".provider").appendPath(f.getName()).build();Intent i=new Intent(Intent.ACTION_SEND);i.setType("application/pdf");i.putExtra(Intent.EXTRA_STREAM,u);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(i,tx("Share PDF","مشاركة PDF","PDF هاوبەش بکە")));}

    @Override protected void onActivityResult(int req,int res,Intent data){
        if(req==SAVE_PDF_635){if(res==RESULT_OK&&data!=null&&data.getData()!=null&&pending!=null)try{InputStream in=new FileInputStream(pending);OutputStream out=getContentResolver().openOutputStream(data.getData());byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);in.close();if(out!=null)out.close();Toast.makeText(this,tx("PDF saved","تم حفظ PDF","PDF پاشەکەوت کرا"),Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}return;}
        super.onActivityResult(req,res,data);
    }
}
