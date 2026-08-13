package com.infinitygreenpower.solarform;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.*;

/**
 * v4.3.0
 * - Reflows item actions below item name so the name gets full width
 * - Compact Edit / Replace / Remove buttons
 * - Structured fixed load-capacity note builder + optional custom note
 * - Full multi-line notes in PDF (no truncation)
 * - Keeps v4.2 catalog categories/search and price totals
 */
public class OfferStudioV430Activity extends OfferStudioV420Activity {
    private boolean v430Pass=false;
    private final DecimalFormat fmt=new DecimalFormat("#,##0.##");

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        View root=findViewById(android.R.id.content);
        if(root!=null){
            root.getViewTreeObserver().addOnGlobalLayoutListener(()->{
                if(v430Pass)return;v430Pass=true;
                try{reflowItemActions(root);installStructuredNote(root);wirePdf(root);}finally{v430Pass=false;}
            });
            root.post(()->{reflowItemActions(root);installStructuredNote(root);wirePdf(root);});
        }
    }

    private String lang(){return getSharedPreferences("offer_studio_settings",MODE_PRIVATE).getString("lang","en");}
    private String currency(){return getSharedPreferences("offer_studio_settings",MODE_PRIVATE).getString("currency","USD");}
    private boolean showPrices(){return getSharedPreferences("offer_studio_settings",MODE_PRIVATE).getBoolean("showPrices",true);}
    private boolean rtl(){return !"en".equals(lang());}
    private String tx(String en,String ar,String ku){return "ar".equals(lang())?ar:("ku".equals(lang())?ku:en);}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int fill,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));if(stroke!=0)g.setStroke(dp(1),stroke);return g;}

    private OfferStudioActivity.Draft draft(){try{Field f=OfferStudioActivity.class.getDeclaredField("draft");f.setAccessible(true);return (OfferStudioActivity.Draft)f.get(this);}catch(Exception e){return null;}}
    private void saveOffer(){try{Method m=OfferStudioActivity.class.getDeclaredMethod("saveOffer");m.setAccessible(true);m.invoke(this);}catch(Exception ignored){}}

    /* ---------------- Compact item card actions ---------------- */
    private void reflowItemActions(View v){
        if(v instanceof LinearLayout){LinearLayout row=(LinearLayout)v;if(hasItemActions(row))reflowActionRow(row);}
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)reflowItemActions(g.getChildAt(i));}
    }
    private boolean actionText(String s){return s.startsWith("✎ ")||s.startsWith("⇄ ")||s.startsWith("× ")||s.startsWith("✎")||s.startsWith("⇄")||s.startsWith("×");}
    private boolean hasItemActions(LinearLayout row){boolean e=false,r=false,d=false;for(int i=0;i<row.getChildCount();i++){View c=row.getChildAt(i);if(c instanceof TextView){String s=String.valueOf(((TextView)c).getText()).trim();if(s.startsWith("✎"))e=true;if(s.startsWith("⇄"))r=true;if(s.startsWith("×"))d=true;}}return e&&r&&d;}
    private void reflowActionRow(LinearLayout row){
        ArrayList<TextView> actions=new ArrayList<>();TextView name=null;
        for(int i=0;i<row.getChildCount();i++){View c=row.getChildAt(i);if(c instanceof TextView){TextView t=(TextView)c;String s=String.valueOf(t.getText()).trim();if(actionText(s))actions.add(t);else if(name==null&&!s.isEmpty())name=t;}}
        if(name!=null){name.setSingleLine(false);name.setMaxLines(3);name.setEllipsize(null);name.setTextSize(16);name.setGravity(rtl()?Gravity.RIGHT:Gravity.LEFT);name.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));}
        if("v430_reflowed".equals(row.getTag())){for(TextView a:actions)compactAction(a);return;}
        if(actions.size()<3)return;
        row.setTag("v430_reflowed");
        for(TextView a:actions)row.removeView(a);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout actionBar=new LinearLayout(this);actionBar.setTag("v430_action_bar");actionBar.setOrientation(LinearLayout.HORIZONTAL);actionBar.setLayoutDirection(rtl()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);actionBar.setGravity(rtl()?Gravity.RIGHT:Gravity.LEFT);
        LinearLayout.LayoutParams alp=new LinearLayout.LayoutParams(-1,-2);alp.topMargin=dp(10);actionBar.setLayoutParams(alp);
        for(TextView a:actions){compactAction(a);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(38),1f);lp.leftMargin=dp(3);lp.rightMargin=dp(3);a.setLayoutParams(lp);actionBar.addView(a);}
        row.addView(actionBar);
    }
    private void compactAction(TextView a){
        String s=String.valueOf(a.getText()).trim();a.setTextSize(11);a.setTypeface(Typeface.DEFAULT_BOLD);a.setGravity(Gravity.CENTER);a.setMinWidth(0);a.setMinHeight(0);a.setPadding(dp(6),dp(4),dp(6),dp(4));
        if(s.startsWith("✎"))a.setBackground(bg(Color.rgb(238,244,250),0,12));
        else if(s.startsWith("⇄"))a.setBackground(bg(Color.rgb(238,247,244),Color.rgb(196,229,216),12));
        else a.setBackground(bg(Color.rgb(255,240,240),0,12));
    }

    /* ---------------- Structured fixed note builder ---------------- */
    private EditText taggedEdit(View v,String tag){
        if(v instanceof EditText&&tag.equals(String.valueOf(v.getTag())))return (EditText)v;
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){EditText r=taggedEdit(g.getChildAt(i),tag);if(r!=null)return r;}}
        return null;
    }
    private void installStructuredNote(View root){
        EditText original=taggedEdit(root,"notes");if(original==null||original.getParent()==null)return;
        ViewGroup parent=(ViewGroup)original.getParent();
        for(int i=0;i<parent.getChildCount();i++)if("v430_note_builder".equals(parent.getChildAt(i).getTag()))return;
        int at=parent.indexOfChild(original);original.setVisibility(View.GONE);
        if(at>0&&parent.getChildAt(at-1) instanceof TextView){String s=String.valueOf(((TextView)parent.getChildAt(at-1)).getText());if(s.equals("Notes")||s.equals("ملاحظات")||s.equals("تێبینی"))parent.getChildAt(at-1).setVisibility(View.GONE);}

        OfferStudioActivity.Draft d=draft();String existing=d==null?"":d.notes;
        NoteData data=parseNote(existing,d);
        LinearLayout card=new LinearLayout(this);card.setTag("v430_note_builder");card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(14),dp(14),dp(14),dp(14));card.setLayoutDirection(rtl()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);card.setBackground(bg(Color.WHITE,Color.rgb(220,226,232),18));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(8);cp.bottomMargin=dp(12);card.setLayoutParams(cp);
        TextView title=txt(tx("Load capacity note","ملاحظة قدرة التحمل","تێبینی توانای بار"),16,true,Color.rgb(18,32,45));card.addView(title);
        TextView sub=txt(tx("Fill only the values. The app builds the full note automatically.","املأ القيم فقط، وسيقوم التطبيق بإنشاء نص الملاحظة تلقائياً.","تەنها نرخەکان پڕبکەرەوە؛ ئەپەکە دەقەکە خۆکار دروست دەکات."),12,false,Color.rgb(104,119,132));sub.setPadding(0,dp(3),0,dp(10));card.addView(sub);

        EditText kw=noteField(card,tx("System capacity (kW)","قدرة المنظومة (كيلو واط)","توانای سیستەم (kW)"),data.kw,true);
        EditText type=noteField(card,tx("System type","نوع المنظومة","جۆری سیستەم"),data.type,false);
        LinearLayout day=row2();EditText dayFrom=noteFieldCompact(day,tx("Day from (A)","بالنهار من (A)","ڕۆژ لە (A)"),data.dayFrom,true);EditText dayTo=noteFieldCompact(day,tx("Day to (A)","إلى (A)","بۆ (A)"),data.dayTo,true);card.addView(day);
        LinearLayout night=row2();EditText nightA=noteFieldCompact(night,tx("Night amps","أمبير بالليل","ئەمپێری شەو"),data.nightA,true);EditText nightH=noteFieldCompact(night,tx("Hours","عدد الساعات","کاتژمێر"),data.nightHours,true);card.addView(night);
        LinearLayout emergency=row2();EditText emA=noteFieldCompact(emergency,tx("Emergency amps","أمبير الضرورة","ئەمپێری زۆر پێویست"),data.emergencyA,true);EditText emD=noteFieldCompact(emergency,tx("Duration","المدة","ماوە"),data.emergencyDuration,false);card.addView(emergency);
        EditText custom=noteFieldMulti(card,tx("Custom note (optional)","ملاحظة مخصصة (اختياري)","تێبینی تایبەت (ئارەزوومەندانە)"),data.custom);
        TextView preview=txt("",13,false,Color.rgb(47,65,82));preview.setPadding(dp(12),dp(10),dp(12),dp(10));preview.setBackground(bg(Color.rgb(247,249,251),0,14));LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,-2);pp.topMargin=dp(10);preview.setLayoutParams(pp);card.addView(preview);

        Runnable update=()->{String note=buildNote(kw,type,dayFrom,dayTo,nightA,nightH,emA,emD,custom);original.setText(note);preview.setText(note);if(d!=null)d.notes=note;};
        TextWatcher watcher=new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){update.run();}public void afterTextChanged(Editable e){}};
        kw.addTextChangedListener(watcher);type.addTextChangedListener(watcher);dayFrom.addTextChangedListener(watcher);dayTo.addTextChangedListener(watcher);nightA.addTextChangedListener(watcher);nightH.addTextChangedListener(watcher);emA.addTextChangedListener(watcher);emD.addTextChangedListener(watcher);custom.addTextChangedListener(watcher);update.run();
        parent.addView(card,Math.max(0,at));
    }
    private LinearLayout row2(){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.setLayoutDirection(rtl()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);return r;}
    private TextView txt(String s,int size,boolean bold,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT_BOLD);t.setGravity(rtl()?Gravity.RIGHT:Gravity.LEFT);return t;}
    private EditText noteField(LinearLayout parent,String label,String value,boolean numeric){TextView l=txt(label,12,true,Color.rgb(35,52,68));l.setPadding(0,dp(6),0,dp(4));parent.addView(l);EditText e=baseField(value,numeric,false);parent.addView(e,new LinearLayout.LayoutParams(-1,dp(48)));return e;}
    private EditText noteFieldCompact(LinearLayout parent,String label,String value,boolean numeric){LinearLayout w=new LinearLayout(this);w.setOrientation(LinearLayout.VERTICAL);TextView l=txt(label,11,true,Color.rgb(35,52,68));l.setPadding(0,dp(6),0,dp(3));w.addView(l);EditText e=baseField(value,numeric,false);w.addView(e,new LinearLayout.LayoutParams(-1,dp(46)));LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(0,-2,1);wp.leftMargin=dp(4);wp.rightMargin=dp(4);parent.addView(w,wp);return e;}
    private EditText noteFieldMulti(LinearLayout parent,String label,String value){TextView l=txt(label,12,true,Color.rgb(35,52,68));l.setPadding(0,dp(8),0,dp(4));parent.addView(l);EditText e=baseField(value,false,true);parent.addView(e,new LinearLayout.LayoutParams(-1,dp(88)));return e;}
    private EditText baseField(String value,boolean numeric,boolean multi){EditText e=new EditText(this);e.setText(value==null?"":value);e.setTextSize(14);e.setTextColor(Color.rgb(18,32,45));e.setHintTextColor(Color.rgb(145,158,170));e.setPadding(dp(11),dp(8),dp(11),dp(8));e.setGravity((multi?Gravity.TOP:Gravity.CENTER_VERTICAL)|(rtl()?Gravity.RIGHT:Gravity.LEFT));e.setTextDirection(rtl()?View.TEXT_DIRECTION_RTL:View.TEXT_DIRECTION_LTR);e.setBackground(bg(Color.rgb(249,250,252),Color.rgb(220,226,232),13));if(numeric)e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);else if(multi){e.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);e.setSingleLine(false);}else e.setSingleLine(true);return e;}

    private static class NoteData{String kw="",type="",dayFrom="",dayTo="",nightA="",nightHours="",emergencyA="",emergencyDuration="",custom="";}
    private NoteData parseNote(String note,OfferStudioActivity.Draft d){
        NoteData n=new NoteData();n.kw=d==null?"":cleanCapacity(d.capacity);n.type=d==null?"":d.system;if(note==null||note.trim().isEmpty())return n;
        try{
            Pattern ar=Pattern.compile("منظومة شمسية بقدرة (.*?) كيلو واط نوع (.*?) يتحمل كالآتي:\\n- بالنهار من (.*?) الى (.*?) امبير\\n- بالليل (.*?) امبير لمدة (.*?) ساعات\\n- وفي حالات الضرورة يتحمل (.*?) امبير لمدة (.*?)(?:\\n\\nملاحظة مخصصة: (.*))?$",Pattern.DOTALL);Matcher m=ar.matcher(note.trim());if(m.matches()){n.kw=m.group(1);n.type=m.group(2);n.dayFrom=m.group(3);n.dayTo=m.group(4);n.nightA=m.group(5);n.nightHours=m.group(6);n.emergencyA=m.group(7);n.emergencyDuration=m.group(8);n.custom=m.group(9)==null?"":m.group(9);return n;}
            Pattern en=Pattern.compile("Solar system capacity (.*?) kW, type (.*?), supports:\\n- Daytime from (.*?) to (.*?) A\\n- Night (.*?) A for (.*?) hours\\n- Emergency (.*?) A for (.*?)(?:\\n\\nCustom note: (.*))?$",Pattern.DOTALL);m=en.matcher(note.trim());if(m.matches()){n.kw=m.group(1);n.type=m.group(2);n.dayFrom=m.group(3);n.dayTo=m.group(4);n.nightA=m.group(5);n.nightHours=m.group(6);n.emergencyA=m.group(7);n.emergencyDuration=m.group(8);n.custom=m.group(9)==null?"":m.group(9);return n;}
        }catch(Exception ignored){}
        n.custom=note;return n;
    }
    private String cleanCapacity(String s){if(s==null)return"";return s.replaceAll("(?i)kw|كيلو\\s*واط|کیلۆ\\s*وات","").trim();}
    private String val(EditText e){String s=e.getText().toString().trim();return s.isEmpty()?"___":s;}
    private String buildNote(EditText kw,EditText type,EditText dayFrom,EditText dayTo,EditText nightA,EditText nightH,EditText emA,EditText emD,EditText custom){
        String base;if("ar".equals(lang()))base="منظومة شمسية بقدرة "+val(kw)+" كيلو واط نوع "+val(type)+" يتحمل كالآتي:\n- بالنهار من "+val(dayFrom)+" الى "+val(dayTo)+" امبير\n- بالليل "+val(nightA)+" امبير لمدة "+val(nightH)+" ساعات\n- وفي حالات الضرورة يتحمل "+val(emA)+" امبير لمدة "+val(emD);
        else if("ku".equals(lang()))base="سیستەمی خۆرەوی بە توانای "+val(kw)+" kW، جۆری "+val(type)+" ئەم بارانە هەڵدەگرێت:\n- لە ڕۆژدا لە "+val(dayFrom)+" بۆ "+val(dayTo)+" A\n- لە شەودا "+val(nightA)+" A بۆ "+val(nightH)+" کاتژمێر\n- لە کاتی پێویستی زۆردا "+val(emA)+" A بۆ "+val(emD);
        else base="Solar system capacity "+val(kw)+" kW, type "+val(type)+", supports:\n- Daytime from "+val(dayFrom)+" to "+val(dayTo)+" A\n- Night "+val(nightA)+" A for "+val(nightH)+" hours\n- Emergency "+val(emA)+" A for "+val(emD);
        String c=custom.getText().toString().trim();if(!c.isEmpty())base+="\n\n"+tx("Custom note: ","ملاحظة مخصصة: ","تێبینی تایبەت: ")+c;return base;
    }

    /* ---------------- Full notes PDF + totals ---------------- */
    private void wirePdf(View v){
        if(v instanceof Button){Button b=(Button)v;String s=String.valueOf(b.getText()).trim();if(isPdf(s)&&!"v430_pdf".equals(b.getTag())){b.setTag("v430_pdf");b.setOnClickListener(x->{saveOffer();createPdf430();});}}
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)wirePdf(g.getChildAt(i));}
    }
    private boolean isPdf(String s){return s.equals("Export PDF")||s.equals("تصدير PDF")||s.equals("PDF دەربکە")||s.equals("PDF هەناردە بکە")||s.equals("تصدير ملف PDF");}
    private BigDecimal number(String s,BigDecimal def){if(s==null||s.trim().isEmpty())return def;try{return new BigDecimal(s.replace(",","").replace("$","").replace("IQD","").replace("USD","").trim());}catch(Exception e){return def;}}
    private BigDecimal lineTotal(OfferStudioActivity.LineItem i){return number(i.qty,BigDecimal.ONE).multiply(number(i.price,BigDecimal.ZERO));}
    private BigDecimal grandTotal(OfferStudioActivity.Draft d){BigDecimal t=BigDecimal.ZERO;for(OfferStudioActivity.LineItem i:d.items)t=t.add(lineTotal(i));return t;}
    private String money(BigDecimal v){return fmt.format(v)+("IQD".equals(currency())?" IQD":" $");}
    private String dash(String s){return s==null||s.trim().isEmpty()?"—":s.trim();}
    private String shortText(String s,int n){if(s==null)return"";return s.length()<=n?s:s.substring(0,n-1)+"…";}

    private void createPdf430(){
        OfferStudioActivity.Draft d=draft();if(d==null)return;
        try{
            File dir=getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);if(dir==null)throw new Exception("Storage unavailable");dir.mkdirs();String safe=d.client.replaceAll("[^\\p{L}\\p{N}_-]","_");if(safe.isEmpty())safe="Offer";File file=new File(dir,"IGP_Offer_"+safe+"_"+System.currentTimeMillis()+".pdf");
            PdfDocument doc=new PdfDocument();Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);int pageNo=1;PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo).create());Canvas c=page.getCanvas();c.drawColor(Color.WHITE);float X=rtl()?555:40;
            text(c,p,"INFINITY GREEN POWER",X,42,24,true,Color.rgb(15,54,89));text(c,p,tx("Solar System Offer","عرض منظومة شمسية","پێشنیاری سیستەمی خۆرەوی"),X,66,13,true,Color.rgb(20,153,112));p.setColor(Color.rgb(20,153,112));c.drawRect(40,82,555,85,p);int y=105;
            y=section(c,p,y,tx("CLIENT","العميل","کڕیار"));y=row(c,p,y,tx("Client","العميل","کڕیار"),dash(d.client));y=row(c,p,y,tx("Phone","رقم الهاتف","ژمارەی تەلەفۆن"),dash(d.phone));y=row(c,p,y,tx("Location","الموقع","شوێن"),dash(d.location));y=row(c,p,y,tx("Date","التاريخ","بەروار"),dash(d.date));y+=7;
            y=section(c,p,y,tx("SYSTEM","المنظومة","سیستەم"));y=row(c,p,y,tx("Type","النوع","جۆر"),dash(d.system));y=row(c,p,y,tx("Capacity","السعة","توانا"),dash(d.capacity));y=row(c,p,y,tx("Phase","الطور","فاز"),dash(d.phase));y+=7;
            y=section(c,p,y,tx("ITEMS","المواد","ماددەکان"));text(c,p,tx("ITEM","المادة","ماددە"),rtl()?555:45,y+15,8,true,Color.rgb(20,153,112));text(c,p,tx("QTY","الكمية","بڕ"),rtl()?240:365,y+15,8,true,Color.rgb(20,153,112));if(showPrices()){text(c,p,tx("UNIT","الوحدة","یەکە"),rtl()?160:435,y+15,8,true,Color.rgb(20,153,112));text(c,p,tx("TOTAL","المجموع","کۆ"),rtl()?75:505,y+15,8,true,Color.rgb(20,153,112));}y+=22;
            for(OfferStudioActivity.LineItem it:d.items){text(c,p,shortText(it.name,43),rtl()?555:45,y+14,9,false,Color.rgb(18,32,45));text(c,p,it.qty==null||it.qty.isEmpty()?"1":it.qty,rtl()?240:370,y+14,9,false,Color.rgb(18,32,45));if(showPrices()){text(c,p,it.price==null||it.price.isEmpty()?"—":money(number(it.price,BigDecimal.ZERO)),rtl()?160:430,y+14,8,false,Color.rgb(18,32,45));text(c,p,money(lineTotal(it)),rtl()?75:500,y+14,8,true,Color.rgb(15,54,89));}p.setColor(Color.rgb(225,231,237));c.drawLine(40,y+22,555,y+22,p);y+=26;if(y>650)break;}
            if(showPrices()){text(c,p,tx("GRAND TOTAL","المجموع الكلي","کۆی گشتی")+": "+money(grandTotal(d)),rtl()?555:45,y+24,13,true,Color.rgb(20,153,112));y+=34;}y+=5;
            y=section(c,p,y,tx("DETAILS","التفاصيل","وردەکاری"));y=row(c,p,y,tx("Installation","التركيب","دامەزراندن"),d.installation?tx("Included","مشمول","ناوخۆکراوە"):tx("Not included","غير مشمول","ناوخۆنەکراوە"));y=row(c,p,y,tx("Transport","النقل","گواستنەوە"),d.transport?tx("Included","مشمول","ناوخۆکراوە"):tx("Not included","غير مشمول","ناوخۆنەکراوە"));y=row(c,p,y,tx("Organizer","منظم الكشف","ڕێکخەر"),dash(d.organizer));

            ArrayList<String> noteLines=wrapParagraphs(p,d.notes,485,10);int need=32+noteLines.size()*15;
            if(!d.notes.trim().isEmpty()&&y+need<790){y=section(c,p,y+5,tx("NOTES","الملاحظات","تێبینی"));y=drawNoteLines(c,p,noteLines,y);}
            footer(c,p);doc.finishPage(page);
            if(!d.notes.trim().isEmpty()&&y+need>=790){pageNo++;PdfDocument.Page np=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo).create());Canvas nc=np.getCanvas();nc.drawColor(Color.WHITE);text(nc,p,"INFINITY GREEN POWER",rtl()?555:40,42,20,true,Color.rgb(15,54,89));int ny=75;ny=section(nc,p,ny,tx("NOTES","الملاحظات","تێبینی"));drawNoteLines(nc,p,noteLines,ny);footer(nc,p);doc.finishPage(np);}
            for(String photo:d.photos){Bitmap bmp=decode(Uri.parse(photo));if(bmp==null)continue;pageNo++;PdfDocument.Page pp=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo).create());Canvas pc=pp.getCanvas();pc.drawColor(Color.WHITE);text(pc,p,tx("Site photo","صورة الموقع / المنظومة","وێنەی شوێن / سیستەم"),rtl()?555:40,45,18,true,Color.rgb(15,54,89));float scale=Math.min(515f/bmp.getWidth(),700f/bmp.getHeight());int nw=(int)(bmp.getWidth()*scale),nh=(int)(bmp.getHeight()*scale),left=(595-nw)/2,top=80;pc.drawBitmap(bmp,null,new Rect(left,top,left+nw,top+nh),null);footer(pc,p);doc.finishPage(pp);bmp.recycle();}
            FileOutputStream out=new FileOutputStream(file);doc.writeTo(out);out.close();doc.close();Uri u=new Uri.Builder().scheme("content").authority(getPackageName()+".provider").appendPath(file.getName()).build();Intent sh=new Intent(Intent.ACTION_SEND);sh.setType("application/pdf");sh.putExtra(Intent.EXTRA_STREAM,u);sh.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(sh,tx("Share offer","مشاركة العرض","هاوبەشکردنی پێشنیار")));
        }catch(Exception e){Toast.makeText(this,"PDF: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private ArrayList<String> wrapParagraphs(Paint p,String text,float maxWidth,float size){ArrayList<String> out=new ArrayList<>();if(text==null)return out;p.setTextSize(size);for(String para:text.split("\\n",-1)){if(para.trim().isEmpty()){out.add("");continue;}String[] words=para.trim().split("\\s+");String cur="";for(String w:words){String test=cur.isEmpty()?w:cur+" "+w;if(p.measureText(test)<=maxWidth)cur=test;else{if(!cur.isEmpty())out.add(cur);cur=w;}}if(!cur.isEmpty())out.add(cur);}return out;}
    private int drawNoteLines(Canvas c,Paint p,ArrayList<String> lines,int y){for(String line:lines){text(c,p,line,rtl()?545:50,y+14,10,false,Color.rgb(70,82,94));y+=15;}return y+4;}
    private Bitmap decode(Uri uri){try{InputStream is=getContentResolver().openInputStream(uri);BitmapFactory.Options b=new BitmapFactory.Options();b.inJustDecodeBounds=true;BitmapFactory.decodeStream(is,null,b);if(is!=null)is.close();int sample=1;while(b.outWidth/sample>1200||b.outHeight/sample>1400)sample*=2;BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=sample;InputStream is2=getContentResolver().openInputStream(uri);Bitmap bmp=BitmapFactory.decodeStream(is2,null,o);if(is2!=null)is2.close();return bmp;}catch(Exception e){return null;}}
    private void text(Canvas c,Paint p,String s,float x,float y,float size,boolean bold,int color){p.setColor(color);p.setTextSize(size);p.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);p.setTextAlign(rtl()?Paint.Align.RIGHT:Paint.Align.LEFT);c.drawText(s==null?"":s,x,y,p);}
    private int section(Canvas c,Paint p,int y,String s){p.setColor(Color.rgb(15,54,89));c.drawRoundRect(40,y,555,y+24,9,9,p);text(c,p,s,rtl()?545:50,y+16,11,true,Color.WHITE);return y+32;}
    private int row(Canvas c,Paint p,int y,String k,String v){if(rtl()){text(c,p,k,545,y+15,10,true,Color.rgb(104,119,132));text(c,p,v,370,y+15,10,false,Color.rgb(18,32,45));}else{text(c,p,k,45,y+15,10,true,Color.rgb(104,119,132));text(c,p,v,180,y+15,10,false,Color.rgb(18,32,45));}p.setColor(Color.rgb(225,231,237));c.drawLine(40,y+23,555,y+23,p);return y+27;}
    private void footer(Canvas c,Paint p){text(c,p,"Infinity Green Power · IGP Offer Studio",rtl()?555:40,820,9,false,Color.rgb(20,153,112));}
}
