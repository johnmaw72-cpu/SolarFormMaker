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

/**
 * v4.2.0
 * - Catalog and item cards are name-only (inherited from v4.1.1)
 * - Large Edit / Replace / Remove actions (inherited from v4.1.1)
 * - Fast name search + category filters
 * - Quantity x unit price line totals + grand total
 * - Name-only PDF with Unit Price, Line Total and Grand Total
 */
public class OfferStudioV420Activity extends OfferStudioV412Activity {
    private boolean pass=false;
    private final DecimalFormat numFmt=new DecimalFormat("#,##0.##");
    private final String[] groupKeys={"ALL","PANELS","HYBRID","ONGRID","PUMP","BATTERY","STRUCTURE","CABLE","PROTECTION","INSTALL","OTHER"};

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        View root=findViewById(android.R.id.content);
        if(root!=null){
            root.getViewTreeObserver().addOnGlobalLayoutListener(()->{
                if(pass)return;pass=true;
                try{stripSpecs();wireActions(root);decorateTotals(root);wirePdf(root);}finally{pass=false;}
            });
            root.post(()->{stripSpecs();wireActions(root);decorateTotals(root);wirePdf(root);});
        }
    }

    private String lang(){return getSharedPreferences("offer_studio_settings",MODE_PRIVATE).getString("lang","en");}
    private String currency(){return getSharedPreferences("offer_studio_settings",MODE_PRIVATE).getString("currency","USD");}
    private boolean showPrices(){return getSharedPreferences("offer_studio_settings",MODE_PRIVATE).getBoolean("showPrices",true);}
    private boolean rtl(){return !"en".equals(lang());}
    private String tx(String en,String ar,String ku){return "ar".equals(lang())?ar:("ku".equals(lang())?ku:en);}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}

    @SuppressWarnings("unchecked") private ArrayList<OfferStudioActivity.Product> catalog(){
        try{Field f=OfferStudioActivity.class.getDeclaredField("catalog");f.setAccessible(true);return (ArrayList<OfferStudioActivity.Product>)f.get(this);}catch(Exception e){return new ArrayList<>();}
    }
    private OfferStudioActivity.Draft draft(){try{Field f=OfferStudioActivity.class.getDeclaredField("draft");f.setAccessible(true);return (OfferStudioActivity.Draft)f.get(this);}catch(Exception e){return null;}}
    private void setStep(int n){try{Field f=OfferStudioActivity.class.getDeclaredField("wizardStep");f.setAccessible(true);f.setInt(this,n);}catch(Exception ignored){}}
    private void showWizard(){try{Method m=OfferStudioActivity.class.getDeclaredMethod("showWizard");m.setAccessible(true);m.invoke(this);}catch(Exception ignored){}}
    private void saveOffer(){try{Method m=OfferStudioActivity.class.getDeclaredMethod("saveOffer");m.setAccessible(true);m.invoke(this);}catch(Exception ignored){}}

    private void stripSpecs(){
        OfferStudioActivity.Draft d=draft();if(d!=null)for(OfferStudioActivity.LineItem i:d.items)i.spec="";
        for(OfferStudioActivity.Product p:catalog())p.spec="";
    }

    private GradientDrawable bg(int fill,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));if(stroke!=0)g.setStroke(dp(1),stroke);return g;}

    /* ---------- Search / category wiring ---------- */
    private void wireActions(View root){
        ArrayList<TextView> edits=new ArrayList<>(),replaces=new ArrayList<>();
        collectActionLabels(root,edits,replaces);
        for(int i=0;i<edits.size();i++){final int idx=i;TextView v=edits.get(i);if(!"v420_edit".equals(v.getTag())){v.setTag("v420_edit");v.setOnClickListener(x->showNameOnlyEditor(idx));}}
        for(int i=0;i<replaces.size();i++){final int idx=i;TextView v=replaces.get(i);if(!"v420_replace".equals(v.getTag())){v.setTag("v420_replace");v.setOnClickListener(x->showFastCatalog(idx));}}
        wireNamedButtons(root);
    }

    private void collectActionLabels(View v,ArrayList<TextView> edits,ArrayList<TextView> replaces){
        if(v instanceof TextView){String s=String.valueOf(((TextView)v).getText()).trim();if(isEdit(s))edits.add((TextView)v);else if(isReplace(s))replaces.add((TextView)v);}
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)collectActionLabels(g.getChildAt(i),edits,replaces);}
    }
    private boolean isEdit(String s){return s.equals("✎ Edit")||s.equals("✎ تعديل")||s.equals("✎ دەستکاری");}
    private boolean isReplace(String s){return s.equals("⇄ Replace")||s.equals("⇄ استبدال")||s.equals("⇄ گۆڕین");}

    private void wireNamedButtons(View v){
        if(v instanceof Button){Button b=(Button)v;String s=String.valueOf(b.getText()).trim();
            if(isBrowse(s)&&!"v420_browse".equals(b.getTag())){b.setTag("v420_browse");b.setOnClickListener(x->showFastCatalog(-1));}
            else if(isCustom(s)&&!"v420_custom".equals(b.getTag())){b.setTag("v420_custom");b.setOnClickListener(x->showCustomEditor());}
        }
        if(v instanceof TextView && !(v instanceof Button)){
            TextView t=(TextView)v;String s=String.valueOf(t.getText()).trim();
            if(isCatalogNav(s)){ViewParent p=t.getParent();if(p instanceof LinearLayout){LinearLayout parent=(LinearLayout)p;if(parent.getChildCount()<=3&&!"v420_catalog_nav".equals(parent.getTag())){parent.setTag("v420_catalog_nav");parent.setOnClickListener(x->showFastCatalog(-1));}}}
        }
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)wireNamedButtons(g.getChildAt(i));}
    }
    private boolean isBrowse(String s){return s.equals("Browse catalog")||s.equals("اختيار من الكتالوج")||s.equals("کاتەلۆگ ببینە");}
    private boolean isCustom(String s){return s.equals("Custom item")||s.equals("مادة مخصصة")||s.equals("ماددەی تایبەت");}
    private boolean isCatalogNav(String s){return s.equals("Catalog")||s.equals("الكتالوج")||s.equals("کاتەلۆگ");}

    private String groupLabel(String k){
        switch(k){
            case "ALL":return tx("All","الكل","هەموو");
            case "PANELS":return tx("Panels","الألواح","پانێڵ");
            case "HYBRID":return tx("Hybrid Inverters","إنفرترات هجينة","ئینڤێرتەری هایبرید");
            case "ONGRID":return tx("On-Grid Inverters","إنفرترات أون كريد","ئینڤێرتەری ئۆن‌گرید");
            case "PUMP":return tx("Pump Inverters","إنفرترات مضخات","ئینڤێرتەری پەمپ");
            case "BATTERY":return tx("Batteries","البطاريات","باتری");
            case "STRUCTURE":return tx("Structures","الهياكل","هەیکەل");
            case "CABLE":return tx("Cables","الكيبلات","کەیبڵ");
            case "PROTECTION":return tx("Protection & Accessories","الحماية والملحقات","پاراستن و پێداویستی");
            case "INSTALL":return tx("Installation & Delivery","التركيب والنقل","دامەزراندن و گواستنەوە");
            default:return tx("Other","أخرى","هیتر");
        }
    }

    private String groupOf(OfferStudioActivity.Product p){
        String c=(p.category==null?"":p.category).toLowerCase(Locale.ROOT),n=(p.name==null?"":p.name).toLowerCase(Locale.ROOT);
        if(c.contains("panel"))return "PANELS";
        if(n.contains("pump")||n.contains("lar100"))return "PUMP";
        if(n.contains("on-grid")||n.contains("on grid")||n.contains("ongrid"))return "ONGRID";
        if(c.contains("inverter")||n.contains("inverter"))return "HYBRID";
        if(c.contains("battery")||n.contains("battery")||n.contains("bms"))return "BATTERY";
        if(c.contains("structure")||n.contains("structure")||n.contains("rack"))return "STRUCTURE";
        if(n.contains("cable")||n.contains("connector"))return "CABLE";
        if(c.contains("installation")||c.contains("delivery")||n.contains("installation")||n.contains("transport")||n.contains("delivery")||n.contains("civil")||n.contains("maintenance"))return "INSTALL";
        if(c.contains("accessor")||c.contains("earthing")||c.contains("lightning")||n.contains("breaker")||n.contains("spd")||n.contains("combiner")||n.contains("ats")||n.contains("meter")||n.contains("zero export")||n.contains("earthing"))return "PROTECTION";
        return "OTHER";
    }

    private void showFastCatalog(final int replaceIndex){
        final ArrayList<OfferStudioActivity.Product> all=new ArrayList<>(catalog());
        final ArrayList<OfferStudioActivity.Product> filtered=new ArrayList<>();
        final ArrayList<String> labels=new ArrayList<>();
        final Dialog dialog=new Dialog(this);dialog.setTitle(replaceIndex>=0?tx("Replace item","استبدال المادة","گۆڕینی ماددە"):tx("Product catalog","كتالوج المنتجات","کاتەلۆگی بەرهەمەکان"));
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(12),dp(8),dp(12),dp(12));box.setLayoutDirection(rtl()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);
        EditText search=new EditText(this);search.setHint(tx("Search item name…","ابحث باسم المادة…","بە ناوی ماددە بگەڕێ…"));search.setSingleLine(true);search.setTextSize(15);search.setPadding(dp(14),0,dp(14),0);search.setBackground(bg(Color.rgb(247,249,251),Color.rgb(220,226,232),15));box.addView(search,new LinearLayout.LayoutParams(-1,dp(52)));

        HorizontalScrollView hsv=new HorizontalScrollView(this);hsv.setHorizontalScrollBarEnabled(false);LinearLayout chips=new LinearLayout(this);chips.setOrientation(LinearLayout.HORIZONTAL);chips.setPadding(0,dp(8),0,dp(6));hsv.addView(chips);box.addView(hsv,new LinearLayout.LayoutParams(-1,dp(58)));
        ListView list=new ListView(this);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,labels);list.setAdapter(adapter);box.addView(list,new LinearLayout.LayoutParams(-1,dp(510)));
        final String[] selected={"ALL"};final ArrayList<TextView> chipViews=new ArrayList<>();
        Runnable refresh=()->{
            String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);filtered.clear();labels.clear();
            for(OfferStudioActivity.Product p:all){String grp=groupOf(p);String name=(p.name==null?"":p.name);if(("ALL".equals(selected[0])||selected[0].equals(grp))&&(q.isEmpty()||name.toLowerCase(Locale.ROOT).contains(q))){filtered.add(p);labels.add(name);}}
            adapter.notifyDataSetChanged();
            for(int i=0;i<chipViews.size();i++){TextView cv=chipViews.get(i);boolean on=groupKeys[i].equals(selected[0]);cv.setTextColor(on?Color.WHITE:Color.rgb(38,59,77));cv.setBackground(bg(on?Color.rgb(20,153,112):Color.rgb(241,245,248),on?0:Color.rgb(218,225,231),14));}
        };
        for(String k:groupKeys){TextView chip=new TextView(this);chip.setText(groupLabel(k));chip.setTextSize(12);chip.setTypeface(Typeface.DEFAULT_BOLD);chip.setGravity(Gravity.CENTER);chip.setPadding(dp(12),dp(7),dp(12),dp(7));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,dp(38));cp.leftMargin=dp(4);cp.rightMargin=dp(4);chip.setLayoutParams(cp);chip.setOnClickListener(v->{selected[0]=k;refresh.run();});chips.addView(chip);chipViews.add(chip);}
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int b,int c){refresh.run();}});
        list.setOnItemClickListener((parent,view,pos,id)->{OfferStudioActivity.Product p=filtered.get(pos);dialog.dismiss();if(replaceIndex>=0)replaceItem(replaceIndex,p);else showAddProductEditor(p);});
        refresh.run();dialog.setContentView(box);dialog.show();Window w=dialog.getWindow();if(w!=null)w.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.96),WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private void replaceItem(int idx,OfferStudioActivity.Product p){OfferStudioActivity.Draft d=draft();if(d==null||idx<0||idx>=d.items.size())return;OfferStudioActivity.LineItem old=d.items.get(idx);d.items.set(idx,new OfferStudioActivity.LineItem(p.category,p.name,"",old.qty,old.price));setStep(2);showWizard();}
    private void showAddProductEditor(OfferStudioActivity.Product p){showSimpleEditor(-1,p.name,p.category,"1","");}
    private void showCustomEditor(){showSimpleEditor(-1,"","Custom","1","");}
    private void showNameOnlyEditor(int idx){OfferStudioActivity.Draft d=draft();if(d==null||idx<0||idx>=d.items.size())return;OfferStudioActivity.LineItem i=d.items.get(idx);showSimpleEditor(idx,i.name,i.category,i.qty,i.price);}

    private EditText field(LinearLayout box,String label,String value,boolean numeric){TextView l=new TextView(this);l.setText(label);l.setTextSize(13);l.setTypeface(Typeface.DEFAULT_BOLD);l.setTextColor(Color.rgb(35,52,68));l.setPadding(0,dp(7),0,dp(4));box.addView(l);EditText e=new EditText(this);e.setText(value==null?"":value);e.setSingleLine(true);e.setPadding(dp(12),0,dp(12),0);e.setBackground(bg(Color.rgb(249,250,252),Color.rgb(220,226,232),13));if(numeric)e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);box.addView(e,new LinearLayout.LayoutParams(-1,dp(48)));return e;}
    private void showSimpleEditor(final int idx,String name,String category,String qty,String price){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(16),dp(6),dp(16),dp(10));box.setLayoutDirection(rtl()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);EditText n=field(box,tx("Item name","اسم المادة","ناوی ماددە"),name,false);EditText q=field(box,tx("Quantity","الكمية","بڕ"),qty,true);EditText pr=field(box,tx("Unit price","سعر الوحدة","نرخی یەکە"),price,true);new AlertDialog.Builder(this).setTitle(idx>=0?tx("Edit item","تعديل المادة","دەستکاری ماددە"):tx("Add item","إضافة المادة","زیادکردنی ماددە")).setView(box).setPositiveButton(idx>=0?tx("Save","حفظ","پاشەکەوت"):tx("Add","إضافة","زیادکردن"),(d,w)->{String nm=n.getText().toString().trim();if(nm.isEmpty())nm=tx("Custom item","مادة مخصصة","ماددەی تایبەت");OfferStudioActivity.LineItem x=new OfferStudioActivity.LineItem(category,nm,"",q.getText().toString().trim(),pr.getText().toString().trim());OfferStudioActivity.Draft dr=draft();if(dr!=null){if(idx>=0)dr.items.set(idx,x);else dr.items.add(x);}setStep(2);showWizard();}).setNegativeButton(tx("Cancel","إلغاء","هەڵوەشاندنەوە"),null).show();}

    /* ---------- Totals ---------- */
    private BigDecimal number(String s,BigDecimal def){if(s==null||s.trim().isEmpty())return def;try{return new BigDecimal(s.replace(",","").replace("$","").replace("IQD","").replace("USD","").trim());}catch(Exception e){return def;}}
    private BigDecimal lineTotal(OfferStudioActivity.LineItem i){return number(i.qty,BigDecimal.ONE).multiply(number(i.price,BigDecimal.ZERO));}
    private BigDecimal grandTotal(){BigDecimal t=BigDecimal.ZERO;OfferStudioActivity.Draft d=draft();if(d!=null)for(OfferStudioActivity.LineItem i:d.items)t=t.add(lineTotal(i));return t;}
    private String money(BigDecimal v){return numFmt.format(v)+("IQD".equals(currency())?" IQD":" $");}

    private void decorateTotals(View root){
        OfferStudioActivity.Draft d=draft();if(d==null)return;
        ArrayList<ViewGroup> chipRows=new ArrayList<>();collectPriceChipRows(root,chipRows);
        ViewGroup listParent=null;
        for(int i=0;i<chipRows.size()&&i<d.items.size();i++){ViewGroup row=chipRows.get(i);addOrUpdateLineTotal(row,d.items.get(i));ViewParent card=row.getParent();if(card instanceof ViewGroup){ViewParent lp=((ViewGroup)card).getParent();if(lp instanceof ViewGroup)listParent=(ViewGroup)lp;}}
        if(listParent!=null)addOrUpdateGrandTotal(listParent);
        addPreviewTotal(root);
    }
    private void collectPriceChipRows(View v,ArrayList<ViewGroup> out){if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;boolean qty=false,price=false;for(int i=0;i<g.getChildCount();i++){View c=g.getChildAt(i);if(c instanceof TextView){String s=String.valueOf(((TextView)c).getText()).trim();if(s.startsWith("Qty ")||s.startsWith("الكمية ")||s.startsWith("بڕ "))qty=true;if(s.startsWith("Price ")||s.startsWith("السعر ")||s.startsWith("نرخ "))price=true;}}if(qty&&price)out.add(g);for(int i=0;i<g.getChildCount();i++)collectPriceChipRows(g.getChildAt(i),out);}}
    private TextView totalLabel(String text){TextView t=new TextView(this);t.setText(text);t.setTextSize(12);t.setTypeface(Typeface.DEFAULT_BOLD);t.setTextColor(Color.rgb(18,50,79));t.setGravity(Gravity.CENTER);t.setPadding(dp(10),dp(7),dp(10),dp(7));t.setBackground(bg(Color.rgb(235,242,250),0,14));return t;}
    private void addOrUpdateLineTotal(ViewGroup row,OfferStudioActivity.LineItem item){TextView t=null;for(int i=0;i<row.getChildCount();i++)if("v420_line_total".equals(row.getChildAt(i).getTag()))t=(TextView)row.getChildAt(i);String s=tx("Total ","المجموع ","کۆ ")+money(lineTotal(item));if(t==null){t=totalLabel(s);t.setTag("v420_line_total");LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.leftMargin=dp(5);lp.rightMargin=dp(5);row.addView(t,lp);}else t.setText(s);}
    private void addOrUpdateGrandTotal(ViewGroup list){TextView t=null;for(int i=0;i<list.getChildCount();i++)if("v420_grand_total".equals(list.getChildAt(i).getTag()))t=(TextView)list.getChildAt(i);String s=tx("Grand Total: ","المجموع الكلي: ","کۆی گشتی: ")+money(grandTotal());if(t==null){t=new TextView(this);t.setTag("v420_grand_total");t.setTextSize(18);t.setTypeface(Typeface.DEFAULT_BOLD);t.setTextColor(Color.rgb(15,54,89));t.setGravity(rtl()?Gravity.RIGHT:Gravity.LEFT);t.setPadding(dp(16),dp(14),dp(16),dp(14));t.setBackground(bg(Color.rgb(236,248,244),Color.rgb(189,226,212),17));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(8);list.addView(t,lp);}t.setText(s);}
    private void addPreviewTotal(View root){TextView details=findText(root,new String[]{"DETAILS","التفاصيل","وردەکاری"});if(details==null)return;ViewParent sec=details.getParent();if(!(sec instanceof View))return;ViewParent paper=((View)sec).getParent();if(!(paper instanceof ViewGroup))return;ViewGroup p=(ViewGroup)paper;for(int i=0;i<p.getChildCount();i++)if("v420_preview_total".equals(p.getChildAt(i).getTag())){((TextView)p.getChildAt(i)).setText(tx("Grand Total: ","المجموع الكلي: ","کۆی گشتی: ")+money(grandTotal()));return;}int idx=p.indexOfChild((View)sec);TextView t=totalLabel(tx("Grand Total: ","المجموع الكلي: ","کۆی گشتی: ")+money(grandTotal()));t.setTag("v420_preview_total");t.setTextSize(17);t.setTextColor(Color.rgb(20,153,112));t.setGravity(rtl()?Gravity.RIGHT:Gravity.LEFT);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(12);lp.bottomMargin=dp(5);t.setLayoutParams(lp);p.addView(t,Math.max(0,idx));}
    private TextView findText(View v,String[] vals){if(v instanceof TextView){String s=String.valueOf(((TextView)v).getText()).trim();for(String x:vals)if(x.equals(s))return (TextView)v;}if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){TextView r=findText(g.getChildAt(i),vals);if(r!=null)return r;}}return null;}

    /* ---------- PDF with totals ---------- */
    private void wirePdf(View v){if(v instanceof Button){Button b=(Button)v;String s=String.valueOf(b.getText());if(isPdf(s)&&!"v420_pdf".equals(b.getTag())){b.setTag("v420_pdf");b.setOnClickListener(x->{saveOffer();createTotalPdf();});}}if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)wirePdf(g.getChildAt(i));}}
    private boolean isPdf(String s){return s.equals("Export PDF")||s.equals("تصدير PDF")||s.equals("PDF دەربکە")||s.equals("PDF هەناردە بکە")||s.equals("تصدير ملف PDF");}
    private String dash(String s){return s==null||s.trim().isEmpty()?"—":s.trim();}
    private String trim(String s,int n){if(s==null)return"";return s.length()<=n?s:s.substring(0,n-1)+"…";}
    private void createTotalPdf(){OfferStudioActivity.Draft d=draft();if(d==null)return;stripSpecs();try{File dir=getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);if(dir==null)throw new Exception("Storage unavailable");dir.mkdirs();String safe=d.client.replaceAll("[^\\p{L}\\p{N}_-]","_");if(safe.isEmpty())safe="Offer";File f=new File(dir,"IGP_Offer_"+safe+"_"+System.currentTimeMillis()+".pdf");PdfDocument doc=new PdfDocument();Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);PdfDocument.Page pg=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,1).create());Canvas c=pg.getCanvas();c.drawColor(Color.WHITE);float X=rtl()?555:40;text(c,p,"INFINITY GREEN POWER",X,42,24,true,Color.rgb(15,54,89));text(c,p,tx("Solar System Offer","عرض منظومة شمسية","پێشنیاری سیستەمی خۆرەوی"),X,66,13,true,Color.rgb(20,153,112));p.setColor(Color.rgb(20,153,112));c.drawRect(40,82,555,85,p);int y=105;y=section(c,p,y,tx("CLIENT","العميل","کڕیار"));y=row(c,p,y,tx("Client","العميل","کڕیار"),dash(d.client));y=row(c,p,y,tx("Phone","رقم الهاتف","ژمارەی تەلەفۆن"),dash(d.phone));y=row(c,p,y,tx("Location","الموقع","شوێن"),dash(d.location));y=row(c,p,y,tx("Date","التاريخ","بەروار"),dash(d.date));y+=7;y=section(c,p,y,tx("SYSTEM","المنظومة","سیستەم"));y=row(c,p,y,tx("Type","النوع","جۆر"),dash(d.system));y=row(c,p,y,tx("Capacity","السعة","توانا"),dash(d.capacity));y=row(c,p,y,tx("Phase","الطور","فاز"),dash(d.phase));y+=7;y=section(c,p,y,tx("ITEMS","المواد","ماددەکان"));
        text(c,p,tx("ITEM","المادة","ماددە"),rtl()?555:45,y+15,8,true,Color.rgb(20,153,112));text(c,p,tx("QTY","الكمية","بڕ"),rtl()?240:365,y+15,8,true,Color.rgb(20,153,112));if(showPrices()){text(c,p,tx("UNIT","الوحدة","یەکە"),rtl()?160:435,y+15,8,true,Color.rgb(20,153,112));text(c,p,tx("TOTAL","المجموع","کۆ"),rtl()?75:505,y+15,8,true,Color.rgb(20,153,112));}y+=22;
        for(OfferStudioActivity.LineItem it:d.items){text(c,p,trim(it.name,42),rtl()?555:45,y+14,9,false,Color.rgb(18,32,45));text(c,p,it.qty==null||it.qty.isEmpty()?"1":it.qty,rtl()?240:370,y+14,9,false,Color.rgb(18,32,45));if(showPrices()){text(c,p,it.price==null||it.price.isEmpty()?"—":money(number(it.price,BigDecimal.ZERO)),rtl()?160:430,y+14,8,false,Color.rgb(18,32,45));text(c,p,money(lineTotal(it)),rtl()?75:500,y+14,8,true,Color.rgb(15,54,89));}p.setColor(Color.rgb(225,231,237));c.drawLine(40,y+22,555,y+22,p);y+=26;if(y>675)break;}
        if(showPrices()){text(c,p,tx("GRAND TOTAL","المجموع الكلي","کۆی گشتی")+": "+money(grandTotal()),rtl()?555:45,y+24,13,true,Color.rgb(20,153,112));y+=34;}y+=5;y=section(c,p,y,tx("DETAILS","التفاصيل","وردەکاری"));y=row(c,p,y,tx("Installation","التركيب","دامەزراندن"),d.installation?tx("Included","مشمول","ناوخۆکراوە"):tx("Not included","غير مشمول","ناوخۆنەکراوە"));y=row(c,p,y,tx("Transport","النقل","گواستنەوە"),d.transport?tx("Included","مشمول","ناوخۆکراوە"):tx("Not included","غير مشمول","ناوخۆنەکراوە"));y=row(c,p,y,tx("Organizer","منظم الكشف","ڕێکخەر"),dash(d.organizer));text(c,p,"Infinity Green Power · IGP Offer Studio",X,820,9,false,Color.rgb(20,153,112));doc.finishPage(pg);FileOutputStream out=new FileOutputStream(f);doc.writeTo(out);out.close();doc.close();Uri u=new Uri.Builder().scheme("content").authority(getPackageName()+".provider").appendPath(f.getName()).build();Intent sh=new Intent(Intent.ACTION_SEND);sh.setType("application/pdf");sh.putExtra(Intent.EXTRA_STREAM,u);sh.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(sh,tx("Share offer","مشاركة العرض","هاوبەشکردنی پێشنیار")));}catch(Exception e){Toast.makeText(this,"PDF: "+e.getMessage(),Toast.LENGTH_LONG).show();}}
    private void text(Canvas c,Paint p,String s,float x,float y,float z,boolean bold,int color){p.setColor(color);p.setTextSize(z);p.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);p.setTextAlign(rtl()?Paint.Align.RIGHT:Paint.Align.LEFT);c.drawText(s==null?"":s,x,y,p);}
    private int section(Canvas c,Paint p,int y,String s){p.setColor(Color.rgb(15,54,89));c.drawRoundRect(40,y,555,y+24,9,9,p);text(c,p,s,rtl()?545:50,y+16,11,true,Color.WHITE);return y+32;}
    private int row(Canvas c,Paint p,int y,String k,String v){if(rtl()){text(c,p,k,545,y+15,10,true,Color.rgb(104,119,132));text(c,p,v,370,y+15,10,false,Color.rgb(18,32,45));}else{text(c,p,k,45,y+15,10,true,Color.rgb(104,119,132));text(c,p,v,180,y+15,10,false,Color.rgb(18,32,45));}p.setColor(Color.rgb(225,231,237));c.drawLine(40,y+23,555,y+23,p);return y+27;}
}
