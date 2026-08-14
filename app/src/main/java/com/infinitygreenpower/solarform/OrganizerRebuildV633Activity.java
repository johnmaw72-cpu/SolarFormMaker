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

/** v6.3.3: immediate language switching + robust multi-page PDF pagination. */
public class OrganizerRebuildV633Activity extends OrganizerRebuildV632Activity {
    private static final int SAVE_PDF_633=6333;
    private final int NAVY=Color.rgb(19,50,76), TEAL=Color.rgb(28,196,163), TEAL_DARK=Color.rgb(18,158,132), MUTED=Color.rgb(118,132,146), BORDER=Color.rgb(224,230,236);
    private final Handler h=new Handler(Looper.getMainLooper());
    private ViewTreeObserver.OnGlobalLayoutListener gl;
    private final Set<View> bound=java.util.Collections.newSetFromMap(new WeakHashMap<View,Boolean>());
    private File pendingPdf;
    private final DecimalFormat money=new DecimalFormat("#,##0.##");

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        View root=findViewById(android.R.id.content);
        if(root!=null){gl=()->{h.removeCallbacks(bindUi);h.postDelayed(bindUi,120);};root.getViewTreeObserver().addOnGlobalLayoutListener(gl);}h.postDelayed(bindUi,220);
    }
    @Override protected void onDestroy(){View root=findViewById(android.R.id.content);if(root!=null&&gl!=null&&root.getViewTreeObserver().isAlive())root.getViewTreeObserver().removeOnGlobalLayoutListener(gl);h.removeCallbacks(bindUi);super.onDestroy();}

    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
    private String lang(){return settings().getString("lang","ar");}
    private boolean rtl(){return!"en".equals(lang());}
    private String tx(String e,String a,String k){return"ar".equals(lang())?a:("ku".equals(lang())?k:e);}
    private boolean eq(String s,String e,String a,String k){return s.equals(e)||s.equals(a)||s.equals(k);}

    private final Runnable bindUi=()->{View root=findViewById(android.R.id.content);if(root!=null){bindLanguageSpinner(root);bindPdfButtons(root);}};

    private void bindLanguageSpinner(View v){
        if(v instanceof Spinner&&!bound.contains(v)){
            Spinner s=(Spinner)v;
            if(s.getAdapter()!=null&&s.getAdapter().getCount()==3){
                String a=String.valueOf(s.getAdapter().getItem(0)),b=String.valueOf(s.getAdapter().getItem(1)),c=String.valueOf(s.getAdapter().getItem(2));
                if("English".equals(a)&&"العربية".equals(b)&&"کوردی".equals(c)){
                    bound.add(v);
                    final int initial=s.getSelectedItemPosition();
                    s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
                        boolean first=true;
                        @Override public void onItemSelected(AdapterView<?> p,View view,int pos,long id){
                            if(first){first=false;return;}
                            String next=pos==1?"ar":pos==2?"ku":"en";
                            if(!next.equals(lang())){settings().edit().putString("lang",next).apply();recreate();}
                        }
                        @Override public void onNothingSelected(AdapterView<?> p){}
                    });
                }
            }
        }
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)bindLanguageSpinner(g.getChildAt(i));}
    }

    private void bindPdfButtons(View v){
        if(v instanceof Button&&!bound.contains(v)){
            Button b=(Button)v;String s=String.valueOf(b.getText()).trim();
            if(eq(s,"Share PDF","مشاركة PDF","PDF هاوبەش بکە")){
                bound.add(v);b.setOnClickListener(x->{if(!validate())return;File f=makePdf633();if(f!=null)share(f);});
            }else if(eq(s,"Save PDF","حفظ PDF","PDF پاشەکەوت بکە")){
                bound.add(v);b.setOnClickListener(x->{if(!validate())return;pendingPdf=makePdf633();if(pendingPdf!=null){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/pdf");i.putExtra(Intent.EXTRA_TITLE,pendingPdf.getName());startActivityForResult(i,SAVE_PDF_633);}});
            }
        }
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)bindPdfButtons(g.getChildAt(i));}
    }

    private boolean validate(){Object d=draft();ArrayList<String> miss=new ArrayList<>();if(d==null)return false;if(get(d,"client").trim().isEmpty())miss.add(tx("Client name","اسم العميل","ناوی کڕیار"));if(get(d,"phone").trim().isEmpty())miss.add(tx("Phone","رقم الهاتف","تەلەفۆن"));if(get(d,"location").trim().isEmpty())miss.add(tx("Location","الموقع","شوێن"));if(get(d,"capacity").trim().isEmpty())miss.add(tx("System capacity","سعة المنظومة","توانای سیستەم"));if(get(d,"organizer").trim().isEmpty())miss.add(tx("Organizer","منظم الكشف","ڕێکخەر"));if(items().isEmpty())miss.add(tx("At least one material","مادة واحدة على الأقل","لانیکەم یەک ماددە"));if(miss.isEmpty())return true;StringBuilder m=new StringBuilder(tx("Please complete these fields before exporting:","أكمل هذه البيانات قبل التصدير:","پێش هەناردەکردن ئەم زانیارییانە پڕ بکەرەوە:")).append("\n\n");for(String x:miss)m.append("• ").append(x).append("\n");new AlertDialog.Builder(this).setTitle(tx("Missing information","بيانات ناقصة","زانیاری کەمە")).setMessage(m.toString()).setPositiveButton(tx("OK","حسناً","باشە"),null).show();return false;}

    private File makePdf633(){
        Object d=draft();if(d==null)return null;
        try{
            File dir=getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS);if(dir==null)return null;
            File out=new File(dir,baseName(d)+".pdf");
            PdfDocument doc=new PdfDocument();Writer w=new Writer(doc,d);w.start();w.client();w.system();w.materials();w.organizer();w.notes();w.finish();
            FileOutputStream os=new FileOutputStream(out);doc.writeTo(os);os.close();doc.close();return out;
        }catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();return null;}
    }

    private class Writer{
        final PdfDocument doc;final Object d;PdfDocument.Page page;Canvas c;Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);int y,pageNo;
        Writer(PdfDocument doc,Object d){this.doc=doc;this.d=d;}
        void start(){newPage(false);}
        void newPage(boolean continuation){if(page!=null){footer();doc.finishPage(page);}page=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,++pageNo).create());c=page.getCanvas();c.drawColor(Color.WHITE);if(continuation)continuationHeader();else fullHeader();}
        void fullHeader(){Bitmap logo=logo();if(logo!=null){float s=Math.min(92f/logo.getWidth(),52f/logo.getHeight());int ww=(int)(logo.getWidth()*s),hh=(int)(logo.getHeight()*s);int left=rtl()?555-ww:40;c.drawBitmap(logo,null,new Rect(left,24,left+ww,24+hh),null);}draw(settings().getString("company_name","Infinity Green Power"),rtl()?440:145,39,19,true,NAVY);draw(tx("Organizer Preliminary Solar Form","استمارة أولية للمنظومة الشمسية","فۆرمی سەرەتایی سیستەمی خۆرەوی"),rtl()?440:145,61,10,true,TEAL_DARK);draw(tx("Date","التاريخ","بەروار")+": "+get(d,"date"),rtl()?440:145,78,8,false,MUTED);p.setColor(TEAL);c.drawRoundRect(40,94,555,98,2,2,p);y=116;}
        void continuationHeader(){draw(settings().getString("company_name","Infinity Green Power"),rtl()?555:40,38,14,true,NAVY);draw(tx("Form continued","تكملة الاستمارة","بەردەوامی فۆرم")+"  ·  "+tx("Page","صفحة","لاپەڕە")+" "+pageNo,rtl()?555:40,58,9,false,MUTED);p.setColor(TEAL);c.drawRect(40,70,555,73,p);y=91;}
        void footer(){draw(settings().getString("company_name","Infinity Green Power")+"  ·  "+tx("Organizer Form","استمارة المنظم","فۆرمی ڕێکخەر"),rtl()?555:40,820,8,false,TEAL_DARK);}
        void require(int h){if(y+h>770)newPage(true);}
        void section(String title,int bodyNeed){require(32+bodyNeed);p.setColor(NAVY);c.drawRoundRect(40,y,555,y+25,8,8,p);draw(title,rtl()?545:50,y+17,10,true,Color.WHITE);y+=34;}
        void row(String key,String value){require(28);draw(key,rtl()?545:45,y+15,8,true,MUTED);draw(value,rtl()?360:180,y+15,10,false,NAVY);p.setColor(BORDER);c.drawLine(40,y+23,555,y+23,p);y+=28;}
        void client(){section(tx("Client","العميل","کڕیار"),112);row(tx("Client","العميل","کڕیار"),get(d,"client"));row(tx("Phone","الهاتف","تەلەفۆن"),get(d,"phone"));row(tx("Location","الموقع","شوێن"),get(d,"location"));row(tx("Date","التاريخ","بەروار"),get(d,"date"));}
        void system(){section(tx("System","المنظومة","سیستەم"),84);row(tx("Type","النوع","جۆر"),get(d,"system"));row(tx("Capacity","السعة","توانا"),get(d,"capacity"));row(tx("Phase","الطور","فاز"),get(d,"phase"));}
        void materials(){
            section(tx("Materials","المواد","ماددە"),56);tableHeader();int count=0;
            for(Object it:items()){
                if(y+29>770){newPage(true);section(tx("Materials continued","تكملة المواد","بەردەوامی ماددەکان"),56);tableHeader();}
                itemRow(it);count++;
            }
            if(settings().getBoolean("pdf_show_total",true)){if(y+34>770){newPage(true);section(tx("Materials summary","ملخص المواد","پوختەی ماددەکان"),34);}draw(tx("Estimated total","المجموع التقديري","کۆی خەمڵاندراو")+": "+settings().getString("currency","USD")+" "+money.format(grand()),rtl()?545:45,y+20,11,true,TEAL_DARK);y+=34;}
        }
        void tableHeader(){require(24);draw(tx("Item","المادة","ماددە"),rtl()?545:45,y+14,8,true,TEAL_DARK);draw(tx("Qty","الكمية","بڕ"),rtl()?205:355,y+14,8,true,TEAL_DARK);if(settings().getBoolean("showPrices",true)){draw(tx("Price","السعر","نرخ"),rtl()?145:425,y+14,8,true,TEAL_DARK);draw(tx("Total","المجموع","کۆ"),rtl()?75:500,y+14,8,true,TEAL_DARK);}y+=22;}
        void itemRow(Object it){draw(trim(get(it,"name"),42),rtl()?545:45,y+16,9,false,NAVY);draw(get(it,"qty"),rtl()?205:355,y+16,9,false,NAVY);if(settings().getBoolean("showPrices",true)){draw(get(it,"price"),rtl()?145:425,y+16,9,false,NAVY);draw(money.format(lineTotal(it)),rtl()?75:500,y+16,9,false,NAVY);}p.setColor(BORDER);c.drawLine(40,y+24,555,y+24,p);y+=29;}
        void organizer(){section(tx("Organizer","منظم الكشف","ڕێکخەر"),34);row(tx("Organizer","منظم الكشف","ڕێکخەر"),get(d,"organizer"));}
        void notes(){ArrayList<String> lines=wrap(get(d,"notes"),72);int need=Math.min(190,Math.max(45,lines.size()*19));section(tx("Load / notes","التحميل / الملاحظات","بار / تێبینی"),need);for(String line:lines){if(y+20>770){newPage(true);section(tx("Load / notes continued","تكملة التحميل / الملاحظات","بەردەوامی بار / تێبینی"),38);}draw(line,rtl()?545:45,y+14,9,false,NAVY);y+=19;}}
        void finish(){if(page!=null){footer();doc.finishPage(page);page=null;}}
        void draw(String s,float x,float yy,float sz,boolean bold,int color){p.setColor(color);p.setTextSize(sz);p.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);p.setTextAlign(rtl()?Paint.Align.RIGHT:Paint.Align.LEFT);c.drawText(s==null?"":s,x,yy,p);}
        String trim(String s,int n){return s==null?"":(s.length()>n?s.substring(0,n-1)+"…":s);}
    }

    private Bitmap logo(){String u=settings().getString("logo_uri","");if(u.isEmpty())return null;try{InputStream in=getContentResolver().openInputStream(Uri.parse(u));Bitmap b=BitmapFactory.decodeStream(in);if(in!=null)in.close();return b;}catch(Exception e){return null;}}
    private void share(File f){Uri u=new Uri.Builder().scheme("content").authority(getPackageName()+".provider").appendPath(f.getName()).build();Intent i=new Intent(Intent.ACTION_SEND);i.setType("application/pdf");i.putExtra(Intent.EXTRA_STREAM,u);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(i,tx("Share PDF","مشاركة PDF","PDF هاوبەش بکە")));}
    @Override protected void onActivityResult(int req,int res,Intent data){if(req==SAVE_PDF_633){if(res==RESULT_OK&&data!=null&&data.getData()!=null&&pendingPdf!=null)try{InputStream in=new FileInputStream(pendingPdf);OutputStream out=getContentResolver().openOutputStream(data.getData());byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);in.close();if(out!=null)out.close();Toast.makeText(this,tx("PDF saved","تم حفظ PDF","PDF پاشەکەوت کرا"),Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}return;}super.onActivityResult(req,res,data);}

    private android.content.SharedPreferences settings(){return getSharedPreferences("offer_studio_settings",MODE_PRIVATE);}
    @SuppressWarnings("unchecked") private ArrayList<Object> items(){try{Field f=OrganizerRebuildActivity.class.getDeclaredField("items");f.setAccessible(true);return(ArrayList<Object>)f.get(this);}catch(Exception e){return new ArrayList<>();}}
    private Object draft(){try{Field f=OrganizerRebuildActivity.class.getDeclaredField("draft");f.setAccessible(true);return f.get(this);}catch(Exception e){return null;}}
    private String get(Object o,String n){try{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);Object v=f.get(o);return v==null?"":String.valueOf(v);}catch(Exception e){return"";}}
    private BigDecimal num(String s){try{return new BigDecimal(s==null||s.trim().isEmpty()?"0":s.replace(",","").replace("$","").trim());}catch(Exception e){return BigDecimal.ZERO;}}
    private BigDecimal lineTotal(Object it){BigDecimal q=num(get(it,"qty"));if(q.signum()==0)q=BigDecimal.ONE;return q.multiply(num(get(it,"price")));}
    private BigDecimal grand(){BigDecimal x=BigDecimal.ZERO;for(Object it:items())x=x.add(lineTotal(it));return x;}
    private ArrayList<String> wrap(String s,int max){ArrayList<String> out=new ArrayList<>();if(s==null||s.trim().isEmpty()){out.add("—");return out;}for(String raw:s.split("\n")){String line="";for(String word:raw.trim().split("\\s+")){if((line+" "+word).trim().length()>max){if(!line.isEmpty())out.add(line);line=word;}else line=(line+" "+word).trim();}if(!line.isEmpty())out.add(line);}return out;}
    private String baseName(Object d){String c=get(d,"client").replaceAll("[^A-Za-z0-9\\u0600-\\u06FF_-]+","_");if(c.isEmpty())c="Client";String cap=get(d,"capacity").replaceAll("[^A-Za-z0-9]+","");return c+"_"+(cap.isEmpty()?"Solar":cap)+"_"+get(d,"date").replace("/","-");}
}
