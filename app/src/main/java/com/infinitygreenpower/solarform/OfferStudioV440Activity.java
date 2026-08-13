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
 * IGP Offer Studio v4.4.0
 * Stable layer over v4.1 core (avoids v4.2/v4.3 repeated layout observers).
 * - Full-screen Catalog tab with category filters + fast name search
 * - Android Back navigates wizard/preview/sections instead of closing app
 * - Compact item actions, Replace, name-only cards, line totals + grand total
 * - Structured load-capacity note + optional custom note
 * - Full wrapped notes in PDF
 * - Separate Share PDF and Save PDF-to-phone actions
 */
public class OfferStudioV440Activity extends OfferStudioV41CompatActivity {
    private static final int SAVE_PDF_REQUEST=5404;
    private final DecimalFormat fmt=new DecimalFormat("#,##0.##");
    private final String[] groups={"ALL","PANELS","HYBRID","ONGRID","PUMP","BATTERY","STRUCTURE","CABLE","PROTECTION","INSTALL","OTHER"};
    private LinearLayout contentRef,bottomRef;
    private boolean decorating=false;
    private String currentScreen="home";
    private File pendingPdf;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        contentRef=(LinearLayout)getBaseField("content");
        bottomRef=(LinearLayout)getBaseField("bottomNav");
        if(contentRef!=null){
            contentRef.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener(){
                public void onChildViewAdded(View parent,View child){contentRef.post(()->decorateCurrentScreen());}
                public void onChildViewRemoved(View parent,View child){}
            });
            contentRef.post(()->decorateCurrentScreen());
        }
    }

    private String lang(){return getSharedPreferences("offer_studio_settings",MODE_PRIVATE).getString("lang","en");}
    private String currency(){return getSharedPreferences("offer_studio_settings",MODE_PRIVATE).getString("currency","USD");}
    private boolean showPrices(){return getSharedPreferences("offer_studio_settings",MODE_PRIVATE).getBoolean("showPrices",true);}
    private boolean rtl(){return !"en".equals(lang());}
    private String tx(String en,String ar,String ku){return "ar".equals(lang())?ar:("ku".equals(lang())?ku:en);}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int fill,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));if(stroke!=0)g.setStroke(dp(1),stroke);return g;}

    private Object getBaseField(String name){try{Field f=OfferStudioActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}catch(Exception e){return null;}}
    private OfferStudioActivity.Draft draft(){Object o=getBaseField("draft");return o instanceof OfferStudioActivity.Draft?(OfferStudioActivity.Draft)o:null;}
    @SuppressWarnings("unchecked") private ArrayList<OfferStudioActivity.Product> catalog(){Object o=getBaseField("catalog");return o instanceof ArrayList?(ArrayList<OfferStudioActivity.Product>)o:new ArrayList<>();}
    private int step(){Object o=getBaseField("wizardStep");return o instanceof Integer?(Integer)o:0;}
    private void setStep(int n){try{Field f=OfferStudioActivity.class.getDeclaredField("wizardStep");f.setAccessible(true);f.setInt(this,n);}catch(Exception ignored){}}
    private void invoke(String method){try{Method m=OfferStudioActivity.class.getDeclaredMethod(method);m.setAccessible(true);m.invoke(this);}catch(Exception ignored){}}
    private void invoke(String method,Class<?>[] types,Object[] args){try{Method m=OfferStudioActivity.class.getDeclaredMethod(method,types);m.setAccessible(true);m.invoke(this,args);}catch(Exception ignored){}}

    private void decorateCurrentScreen(){
        if(decorating||contentRef==null)return;decorating=true;
        try{
            stripSpecs();
            currentScreen=detectScreen();
            wireBottomNavigation();
            wireHomeCatalogTile(contentRef);
            if("wizard".equals(currentScreen)){
                if(step()==2)decorateItemsStep(contentRef);
                if(step()==3)installStructuredNote(contentRef);
            }
            if("preview".equals(currentScreen))decoratePreview(contentRef);
        }finally{decorating=false;}
    }

    private String detectScreen(){
        if(hasText(contentRef,new String[]{"Offer Preview","معاينة العرض","پێشبینینی پێشنیار"}))return "preview";
        if(hasText(contentRef,new String[]{"New Offer","عرض جديد","پێشنیاری نوێ"}))return "wizard";
        if(hasText(contentRef,new String[]{"Saved Offers","العروض المحفوظة","پێشنیارە پاشەکەوتکراوەکان"}))return "saved";
        if(hasText(contentRef,new String[]{"Product Catalog","كتالوج المنتجات","کاتەلۆگی بەرهەمەکان"})||"v440_catalog".equals(contentRef.getTag()))return "catalog";
        return "home";
    }
    private boolean hasText(View v,String[] values){if(v instanceof TextView){String s=String.valueOf(((TextView)v).getText()).trim();for(String x:values)if(x.equals(s))return true;}if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(hasText(g.getChildAt(i),values))return true;}return false;}

    private void stripSpecs(){for(OfferStudioActivity.Product p:catalog())p.spec="";OfferStudioActivity.Draft d=draft();if(d!=null)for(OfferStudioActivity.LineItem i:d.items)i.spec="";}

    /* ---------------- Navigation ---------------- */
    private void wireBottomNavigation(){
        if(bottomRef==null)return;
        for(int i=0;i<bottomRef.getChildCount();i++){
            View v=bottomRef.getChildAt(i);String label=findNavLabel(v);
            if(label==null)continue;
            if(isCatalogLabel(label)){
                v.setTag("v420_catalog_nav");
                v.setOnClickListener(x->showCatalogSection());
            }else if(isHomeLabel(label))v.setOnClickListener(x->{invoke("showHome");});
            else if(isNewLabel(label))v.setOnClickListener(x->{invoke("startNewOffer");});
            else if(isSavedLabel(label))v.setOnClickListener(x->{invoke("showSaved");});
        }
    }
    private String findNavLabel(View v){if(v instanceof TextView)return String.valueOf(((TextView)v).getText()).trim();if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){String s=findNavLabel(g.getChildAt(i));if(s!=null&&(isCatalogLabel(s)||isHomeLabel(s)||isNewLabel(s)||isSavedLabel(s)))return s;}}return null;}
    private boolean isCatalogLabel(String s){return s.equals("Catalog")||s.equals("الكتالوج")||s.equals("کاتەلۆگ");}
    private boolean isHomeLabel(String s){return s.equals("Home")||s.equals("الرئيسية")||s.equals("سەرەکی");}
    private boolean isNewLabel(String s){return s.equals("New Offer")||s.equals("عرض جديد")||s.equals("پێشنیاری نوێ");}
    private boolean isSavedLabel(String s){return s.equals("Saved")||s.equals("محفوظ")||s.equals("پاشەکەوتکراو");}

    private void wireHomeCatalogTile(View v){
        if(v instanceof TextView){TextView t=(TextView)v;String s=String.valueOf(t.getText()).trim();if(s.equals("Product catalog")||s.equals("كتالوج المنتجات")||s.equals("کاتەلۆگی بەرهەمەکان")){ViewParent p=t.getParent();if(p instanceof View)((View)p).setOnClickListener(x->showCatalogSection());}}
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)wireHomeCatalogTile(g.getChildAt(i));}
    }

    @Override public void onBackPressed(){
        if("wizard".equals(currentScreen)){
            captureVisible();
            int s=step();if(s>0){setStep(s-1);invoke("showWizard");}else invoke("showHome");return;
        }
        if("preview".equals(currentScreen)){setStep(3);invoke("showWizard");return;}
        if("catalog".equals(currentScreen)||"saved".equals(currentScreen)){invoke("showHome");return;}
        // On dashboard, keep the user in the app instead of unexpectedly closing it.
        invoke("showHome");
    }
    private void captureVisible(){invoke("captureVisibleFields");}

    /* ---------------- Full-screen catalog ---------------- */
    private String groupLabel(String k){switch(k){case"ALL":return tx("All","الكل","هەموو");case"PANELS":return tx("Panels","الألواح","پانێڵ");case"HYBRID":return tx("Hybrid Inverters","إنفرترات هجينة","ئینڤێرتەری هایبرید");case"ONGRID":return tx("On-Grid Inverters","إنفرترات أون كريد","ئینڤێرتەری ئۆن‌گرید");case"PUMP":return tx("Pump Inverters","إنفرترات مضخات","ئینڤێرتەری پەمپ");case"BATTERY":return tx("Batteries","البطاريات","باتری");case"STRUCTURE":return tx("Structures","الهياكل","هەیکەل");case"CABLE":return tx("Cables","الكيبلات","کەیبڵ");case"PROTECTION":return tx("Protection & Accessories","الحماية والملحقات","پاراستن و پێداویستی");case"INSTALL":return tx("Installation & Delivery","التركيب والنقل","دامەزراندن و گواستنەوە");default:return tx("Other","أخرى","هیتر");}}
    private String groupOf(OfferStudioActivity.Product p){String c=(p.category==null?"":p.category).toLowerCase(Locale.ROOT),n=(p.name==null?"":p.name).toLowerCase(Locale.ROOT);if(c.contains("panel"))return"PANELS";if(n.contains("pump")||n.contains("lar100"))return"PUMP";if(n.contains("on-grid")||n.contains("on grid")||n.contains("ongrid"))return"ONGRID";if(c.contains("inverter")||n.contains("inverter"))return"HYBRID";if(c.contains("battery")||n.contains("battery")||n.contains("bms"))return"BATTERY";if(c.contains("structure")||n.contains("structure")||n.contains("rack"))return"STRUCTURE";if(n.contains("cable")||n.contains("connector"))return"CABLE";if(c.contains("installation")||c.contains("delivery")||n.contains("installation")||n.contains("transport")||n.contains("delivery")||n.contains("civil")||n.contains("maintenance"))return"INSTALL";if(c.contains("accessor")||c.contains("earthing")||c.contains("lightning")||n.contains("breaker")||n.contains("spd")||n.contains("combiner")||n.contains("ats")||n.contains("meter")||n.contains("zero export")||n.contains("earthing"))return"PROTECTION";return"OTHER";}

    private void showCatalogSection(){
        invoke("clearContent");invoke("buildBottomNav",new Class[]{String.class},new Object[]{"catalog"});currentScreen="catalog";contentRef.setTag("v440_catalog");
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setLayoutDirection(rtl()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);contentRef.addView(page,new LinearLayout.LayoutParams(-1,-1));
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.VERTICAL);top.setPadding(dp(18),dp(15),dp(18),dp(12));top.setBackgroundColor(Color.WHITE);
        TextView title=text(tx("Product Catalog","كتالوج المنتجات","کاتەلۆگی بەرهەمەکان"),24,true,Color.rgb(18,32,45));top.addView(title);top.addView(text(catalog().size()+" "+tx("items","مادة","ماددە"),12,false,Color.rgb(104,119,132)));
        EditText search=new EditText(this);search.setHint(tx("Search item name…","ابحث باسم المادة…","بە ناوی ماددە بگەڕێ…"));search.setSingleLine(true);search.setTextSize(15);search.setPadding(dp(14),0,dp(14),0);search.setBackground(bg(Color.rgb(247,249,251),Color.rgb(220,226,232),15));LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,dp(52));slp.topMargin=dp(12);top.addView(search,slp);page.addView(top);
        HorizontalScrollView hsv=new HorizontalScrollView(this);hsv.setHorizontalScrollBarEnabled(false);LinearLayout chipRow=new LinearLayout(this);chipRow.setOrientation(LinearLayout.HORIZONTAL);chipRow.setPadding(dp(12),dp(7),dp(12),dp(7));hsv.addView(chipRow);page.addView(hsv,new LinearLayout.LayoutParams(-1,dp(54)));
        ListView list=new ListView(this);page.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        final ArrayList<OfferStudioActivity.Product> filtered=new ArrayList<>();final ArrayList<String> labels=new ArrayList<>();final ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,labels);list.setAdapter(adapter);final String[] selected={"ALL"};final ArrayList<TextView> chipViews=new ArrayList<>();
        Runnable refresh=()->{String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);filtered.clear();labels.clear();for(OfferStudioActivity.Product p:catalog()){String n=p.name==null?"":p.name;if((selected[0].equals("ALL")||selected[0].equals(groupOf(p)))&&(q.isEmpty()||n.toLowerCase(Locale.ROOT).contains(q))){filtered.add(p);labels.add(n);}}adapter.notifyDataSetChanged();for(int i=0;i<chipViews.size();i++){TextView cv=chipViews.get(i);boolean on=groups[i].equals(selected[0]);cv.setTextColor(on?Color.WHITE:Color.rgb(38,59,77));cv.setBackground(bg(on?Color.rgb(20,153,112):Color.rgb(241,245,248),on?0:Color.rgb(218,225,231),14));}};
        for(String g:groups){TextView ch=text(groupLabel(g),12,true,Color.rgb(38,59,77));ch.setGravity(Gravity.CENTER);ch.setPadding(dp(12),dp(6),dp(12),dp(6));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(38));lp.leftMargin=dp(4);lp.rightMargin=dp(4);ch.setLayoutParams(lp);ch.setOnClickListener(v->{selected[0]=g;refresh.run();});chipRow.addView(ch);chipViews.add(ch);}search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int a,int b,int c){refresh.run();}});list.setOnItemClickListener((p,v,pos,id)->showCatalogItemDialog(filtered.get(pos)));refresh.run();contentRef.post(()->wireBottomNavigation());
    }
    private void showCatalogItemDialog(OfferStudioActivity.Product p){new AlertDialog.Builder(this).setTitle(p.name).setItems(new String[]{tx("Add to current offer","إضافة إلى العرض الحالي","زیادکردن بۆ پێشنیاری ئێستا"),tx("Close","إغلاق","داخستن")},(d,w)->{if(w==0){OfferStudioActivity.Draft dr=draft();if(dr!=null){dr.items.add(new OfferStudioActivity.LineItem(p.category,p.name,"","1",""));Toast.makeText(this,tx("Added to current offer","تمت الإضافة إلى العرض","زیادکرا بۆ پێشنیار"),Toast.LENGTH_SHORT).show();}}}).show();}

    /* ---------------- Item screen ---------------- */
    private void decorateItemsStep(View root){
        ArrayList<LinearLayout> actionRows=new ArrayList<>();collectItemRows(root,actionRows);OfferStudioActivity.Draft d=draft();
        for(int i=0;i<actionRows.size()&&d!=null&&i<d.items.size();i++)rebuildItemActionRow(actionRows.get(i),i,d.items.get(i));
        wireBrowseButtons(root);
        addGrandTotal(root);
    }
    private void collectItemRows(View v,ArrayList<LinearLayout> out){if(v instanceof LinearLayout){LinearLayout g=(LinearLayout)v;boolean edit=false,remove=false;for(int i=0;i<g.getChildCount();i++){View c=g.getChildAt(i);if(c instanceof TextView){String s=String.valueOf(((TextView)c).getText()).trim();if(s.startsWith("✎"))edit=true;if(s.startsWith("×"))remove=true;}}if(edit&&remove)out.add(g);}if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)collectItemRows(g.getChildAt(i),out);}}
    private void rebuildItemActionRow(LinearLayout row,int index,OfferStudioActivity.LineItem item){
        if("v440_item_row".equals(row.getTag()))return;row.setTag("v440_item_row");TextView name=null;ArrayList<View> remove=new ArrayList<>();for(int i=0;i<row.getChildCount();i++){View c=row.getChildAt(i);if(c instanceof TextView){String s=String.valueOf(((TextView)c).getText()).trim();if(s.startsWith("✎")||s.startsWith("×"))remove.add(c);else if(name==null&&!s.isEmpty())name=(TextView)c;}}
        for(View x:remove)row.removeView(x);if(name!=null){name.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));name.setSingleLine(false);name.setMaxLines(2);name.setTextSize(16);name.setGravity(rtl()?Gravity.RIGHT:Gravity.LEFT);}row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);actions.setLayoutDirection(rtl()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);actions.setGravity(Gravity.CENTER);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.topMargin=dp(8);actions.setLayoutParams(ap);
        TextView edit=smallAction(tx("Edit","تعديل","دەستکاری"),Color.rgb(18,50,79),Color.rgb(238,244,250));TextView replace=smallAction(tx("Replace","استبدال","گۆڕین"),Color.rgb(20,153,112),Color.rgb(238,247,244));TextView del=smallAction(tx("Remove","حذف","سڕینەوە"),Color.rgb(184,61,61),Color.rgb(255,240,240));edit.setOnClickListener(v->showItemEditor(index));replace.setOnClickListener(v->showProductPicker(index));del.setOnClickListener(v->{OfferStudioActivity.Draft d=draft();if(d!=null&&index<d.items.size()){d.items.remove(index);setStep(2);invoke("showWizard");}});actions.addView(edit,new LinearLayout.LayoutParams(0,dp(36),1));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,dp(36),1);rp.leftMargin=dp(4);rp.rightMargin=dp(4);actions.addView(replace,rp);actions.addView(del,new LinearLayout.LayoutParams(0,dp(36),1));row.addView(actions);
        // Hide category/spec line to keep item cards name-only.
        ViewParent parent=row.getParent();if(parent instanceof ViewGroup){ViewGroup card=(ViewGroup)parent;for(int i=0;i<card.getChildCount();i++){View c=card.getChildAt(i);if(c instanceof TextView&&c!=name){String s=String.valueOf(((TextView)c).getText());if(s.equals(item.category)||s.startsWith(item.category+" ·"))c.setVisibility(View.GONE);}}decoratePriceChips(card,item);}
    }
    private TextView smallAction(String s,int fg,int fill){TextView t=text(s,11,true,fg);t.setGravity(Gravity.CENTER);t.setBackground(bg(fill,0,11));t.setPadding(dp(5),dp(3),dp(5),dp(3));return t;}
    private void decoratePriceChips(ViewGroup card,OfferStudioActivity.LineItem item){ArrayList<TextView> chips=new ArrayList<>();collectText(card,chips);boolean has=false;for(TextView t:chips)if("v440_line_total".equals(t.getTag()))has=true;if(!has){for(TextView t:chips){String s=String.valueOf(t.getText());if(s.startsWith("Price ")||s.startsWith("السعر ")||s.startsWith("نرخ ")){ViewParent p=t.getParent();if(p instanceof LinearLayout){TextView total=chip(tx("Total ","المجموع ","کۆ ")+money(lineTotal(item)),Color.rgb(235,242,250),Color.rgb(18,50,79));total.setTag("v440_line_total");((LinearLayout)p).addView(total);break;}}}}}
    private void collectText(View v,ArrayList<TextView> out){if(v instanceof TextView)out.add((TextView)v);if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)collectText(g.getChildAt(i),out);}}
    private TextView chip(String s,int fill,int fg){TextView t=text(s,11,true,fg);t.setGravity(Gravity.CENTER);t.setBackground(bg(fill,0,12));t.setPadding(dp(8),dp(5),dp(8),dp(5));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.leftMargin=dp(4);lp.rightMargin=dp(4);t.setLayoutParams(lp);return t;}
    private void wireBrowseButtons(View v){if(v instanceof Button){Button b=(Button)v;String s=String.valueOf(b.getText()).trim();if(s.equals("Browse catalog")||s.equals("اختيار من الكتالوج")||s.equals("کاتەلۆگ ببینە")){b.setOnClickListener(x->showProductPicker(-1));}else if(s.equals("Custom item")||s.equals("مادة مخصصة")||s.equals("ماددەی تایبەت")){b.setOnClickListener(x->showItemEditor(-1));}}if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)wireBrowseButtons(g.getChildAt(i));}}
    private void showItemEditor(final int index){OfferStudioActivity.Draft d=draft();OfferStudioActivity.LineItem old=index>=0&&d!=null&&index<d.items.size()?d.items.get(index):new OfferStudioActivity.LineItem("Custom","","","1","");LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(14),dp(8),dp(14),dp(10));EditText n=editorField(box,tx("Item name","اسم المادة","ناوی ماددە"),old.name,false);EditText q=editorField(box,tx("Quantity","الكمية","بڕ"),old.qty,true);EditText p=editorField(box,tx("Unit price","سعر الوحدة","نرخی یەکە"),old.price,true);new AlertDialog.Builder(this).setTitle(index>=0?tx("Edit item","تعديل المادة","دەستکاری ماددە"):tx("Custom item","مادة مخصصة","ماددەی تایبەت")).setView(box).setPositiveButton(tx("Save","حفظ","پاشەکەوت"),(di,w)->{String name=n.getText().toString().trim();if(name.isEmpty())name=tx("Custom item","مادة مخصصة","ماددەی تایبەت");OfferStudioActivity.LineItem x=new OfferStudioActivity.LineItem(old.category,name,"",q.getText().toString().trim(),p.getText().toString().trim());OfferStudioActivity.Draft dr=draft();if(dr!=null){if(index>=0)dr.items.set(index,x);else dr.items.add(x);}setStep(2);invoke("showWizard");}).setNegativeButton(tx("Cancel","إلغاء","هەڵوەشاندنەوە"),null).show();}
    private EditText editorField(LinearLayout box,String label,String value,boolean numeric){TextView l=text(label,12,true,Color.rgb(35,52,68));l.setPadding(0,dp(6),0,dp(4));box.addView(l);EditText e=new EditText(this);e.setText(value==null?"":value);e.setSingleLine(true);e.setPadding(dp(12),0,dp(12),0);e.setBackground(bg(Color.rgb(249,250,252),Color.rgb(220,226,232),13));if(numeric)e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);box.addView(e,new LinearLayout.LayoutParams(-1,dp(48)));return e;}

    private void showProductPicker(final int replaceIndex){
        final Dialog dialog=new Dialog(this);dialog.setTitle(replaceIndex>=0?tx("Replace item","استبدال المادة","گۆڕینی ماددە"):tx("Choose item","اختيار مادة","ماددە هەڵبژێرە"));LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(12),dp(8),dp(12),dp(12));box.setLayoutDirection(rtl()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);EditText search=new EditText(this);search.setHint(tx("Search item name…","ابحث باسم المادة…","بە ناوی ماددە بگەڕێ…"));search.setSingleLine(true);search.setBackground(bg(Color.rgb(247,249,251),Color.rgb(220,226,232),15));search.setPadding(dp(14),0,dp(14),0);box.addView(search,new LinearLayout.LayoutParams(-1,dp(50)));HorizontalScrollView hsv=new HorizontalScrollView(this);LinearLayout chips=new LinearLayout(this);chips.setOrientation(LinearLayout.HORIZONTAL);chips.setPadding(0,dp(7),0,dp(6));hsv.addView(chips);box.addView(hsv,new LinearLayout.LayoutParams(-1,dp(54)));ListView list=new ListView(this);box.addView(list,new LinearLayout.LayoutParams(-1,dp(470)));final ArrayList<OfferStudioActivity.Product> filtered=new ArrayList<>();final ArrayList<String> labels=new ArrayList<>();final ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,labels);list.setAdapter(adapter);final String[] selected={"ALL"};final ArrayList<TextView> cv=new ArrayList<>();Runnable refresh=()->{String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);filtered.clear();labels.clear();for(OfferStudioActivity.Product p:catalog()){String n=p.name==null?"":p.name;if((selected[0].equals("ALL")||selected[0].equals(groupOf(p)))&&(q.isEmpty()||n.toLowerCase(Locale.ROOT).contains(q))){filtered.add(p);labels.add(n);}}adapter.notifyDataSetChanged();for(int i=0;i<cv.size();i++){boolean on=groups[i].equals(selected[0]);cv.get(i).setTextColor(on?Color.WHITE:Color.rgb(38,59,77));cv.get(i).setBackground(bg(on?Color.rgb(20,153,112):Color.rgb(241,245,248),on?0:Color.rgb(218,225,231),13));}};for(String g:groups){TextView c=text(groupLabel(g),11,true,Color.rgb(38,59,77));c.setGravity(Gravity.CENTER);c.setPadding(dp(10),dp(5),dp(10),dp(5));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(36));lp.leftMargin=dp(3);lp.rightMargin=dp(3);c.setLayoutParams(lp);c.setOnClickListener(v->{selected[0]=g;refresh.run();});chips.addView(c);cv.add(c);}search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int a,int b,int c){refresh.run();}});list.setOnItemClickListener((p,v,pos,id)->{OfferStudioActivity.Product pr=filtered.get(pos);OfferStudioActivity.Draft d=draft();if(d!=null){if(replaceIndex>=0&&replaceIndex<d.items.size()){OfferStudioActivity.LineItem old=d.items.get(replaceIndex);d.items.set(replaceIndex,new OfferStudioActivity.LineItem(pr.category,pr.name,"",old.qty,old.price));}else d.items.add(new OfferStudioActivity.LineItem(pr.category,pr.name,"","1",""));}dialog.dismiss();setStep(2);invoke("showWizard");});refresh.run();dialog.setContentView(box);dialog.show();Window w=dialog.getWindow();if(w!=null)w.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.96),WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private BigDecimal num(String s,BigDecimal def){if(s==null||s.trim().isEmpty())return def;try{return new BigDecimal(s.replace(",","").replace("$","").replace("IQD","").replace("USD","").trim());}catch(Exception e){return def;}}
    private BigDecimal lineTotal(OfferStudioActivity.LineItem i){return num(i.qty,BigDecimal.ONE).multiply(num(i.price,BigDecimal.ZERO));}
    private BigDecimal grandTotal(){BigDecimal t=BigDecimal.ZERO;OfferStudioActivity.Draft d=draft();if(d!=null)for(OfferStudioActivity.LineItem i:d.items)t=t.add(lineTotal(i));return t;}
    private String money(BigDecimal v){return fmt.format(v)+(currency().equals("IQD")?" IQD":" $");}
    private void addGrandTotal(View root){if(hasTag(root,"v440_grand"))return;OfferStudioActivity.Draft d=draft();if(d==null||d.items.isEmpty())return;ViewGroup target=findItemsList(root);if(target==null)return;TextView t=text(tx("Grand Total: ","المجموع الكلي: ","کۆی گشتی: ")+money(grandTotal()),17,true,Color.rgb(15,54,89));t.setTag("v440_grand");t.setGravity(rtl()?Gravity.RIGHT:Gravity.LEFT);t.setPadding(dp(14),dp(12),dp(14),dp(12));t.setBackground(bg(Color.rgb(236,248,244),Color.rgb(189,226,212),16));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(8);target.addView(t,lp);}
    private boolean hasTag(View v,String tag){if(tag.equals(String.valueOf(v.getTag())))return true;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(hasTag(g.getChildAt(i),tag))return true;}return false;}
    private ViewGroup findItemsList(View v){if(v instanceof LinearLayout){LinearLayout l=(LinearLayout)v;int cards=0;for(int i=0;i<l.getChildCount();i++){View c=l.getChildAt(i);if(c instanceof LinearLayout&&"v440_item_row".equals(findTagRecursive(c,"v440_item_row")))cards++;}if(cards>0)return l;}if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){ViewGroup r=findItemsList(g.getChildAt(i));if(r!=null)return r;}}return null;}
    private String findTagRecursive(View v,String tag){if(tag.equals(String.valueOf(v.getTag())))return tag;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){String r=findTagRecursive(g.getChildAt(i),tag);if(r!=null)return r;}}return null;}

    /* ---------------- Structured note ---------------- */
    private EditText taggedEdit(View v,String tag){if(v instanceof EditText&&tag.equals(String.valueOf(v.getTag())))return(EditText)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){EditText r=taggedEdit(g.getChildAt(i),tag);if(r!=null)return r;}}return null;}
    private void installStructuredNote(View root){EditText original=taggedEdit(root,"notes");if(original==null||original.getParent()==null)return;ViewGroup parent=(ViewGroup)original.getParent();if(hasTag(parent,"v440_note_builder"))return;int at=parent.indexOfChild(original);original.setVisibility(View.GONE);if(at>0&&parent.getChildAt(at-1)instanceof TextView)parent.getChildAt(at-1).setVisibility(View.GONE);OfferStudioActivity.Draft d=draft();NoteData nd=parseNote(d==null?"":d.notes,d);LinearLayout card=new LinearLayout(this);card.setTag("v440_note_builder");card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(14),dp(12),dp(14),dp(14));card.setBackground(bg(Color.WHITE,Color.rgb(220,226,232),17));TextView h=text(tx("Load capacity note","ملاحظة قدرة التحمل","تێبینی توانای بار"),16,true,Color.rgb(18,32,45));card.addView(h);card.addView(text(tx("Fill the values only. The sentence is generated automatically.","املأ القيم فقط، وسيتم إنشاء النص تلقائياً.","تەنها نرخەکان پڕ بکەرەوە؛ دەقەکە خۆکار دروست دەبێت."),12,false,Color.rgb(104,119,132)));EditText kw=noteField(card,tx("System capacity (kW)","قدرة المنظومة (كيلو واط)","توانای سیستەم (kW)"),nd.kw,true);EditText type=noteField(card,tx("System type","نوع المنظومة","جۆری سیستەم"),nd.type,false);LinearLayout day=two();EditText df=noteCompact(day,tx("Day from (A)","بالنهار من (A)","ڕۆژ لە (A)"),nd.dayFrom,true);EditText dt=noteCompact(day,tx("Day to (A)","إلى (A)","بۆ (A)"),nd.dayTo,true);card.addView(day);LinearLayout night=two();EditText na=noteCompact(night,tx("Night amps","أمبير بالليل","ئەمپێری شەو"),nd.nightA,true);EditText nh=noteCompact(night,tx("Hours","الساعات","کاتژمێر"),nd.nightHours,true);card.addView(night);LinearLayout em=two();EditText ea=noteCompact(em,tx("Emergency amps","أمبير الضرورة","ئەمپێری پێویست"),nd.emA,true);EditText ed=noteCompact(em,tx("Duration","المدة","ماوە"),nd.emDuration,false);card.addView(em);EditText custom=noteMulti(card,tx("Custom note (optional)","ملاحظة مخصصة (اختياري)","تێبینی تایبەت (ئارەزوومەندانە)"),nd.custom);TextView preview=text("",13,false,Color.rgb(47,65,82));preview.setPadding(dp(11),dp(10),dp(11),dp(10));preview.setBackground(bg(Color.rgb(247,249,251),0,13));LinearLayout.LayoutParams pv=new LinearLayout.LayoutParams(-1,-2);pv.topMargin=dp(10);card.addView(preview,pv);Runnable update=()->{String s=buildNote(kw,type,df,dt,na,nh,ea,ed,custom);original.setText(s);preview.setText(s);if(d!=null)d.notes=s;};TextWatcher w=new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int a,int b,int c){update.run();}};for(EditText e:new EditText[]{kw,type,df,dt,na,nh,ea,ed,custom})e.addTextChangedListener(w);update.run();LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(8);cp.bottomMargin=dp(12);parent.addView(card,Math.max(0,at),cp);}
    private LinearLayout two(){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.setLayoutDirection(rtl()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);return r;}
    private EditText noteField(LinearLayout p,String label,String val,boolean numeric){TextView l=text(label,12,true,Color.rgb(35,52,68));l.setPadding(0,dp(7),0,dp(3));p.addView(l);EditText e=baseField(val,numeric,false);p.addView(e,new LinearLayout.LayoutParams(-1,dp(46)));return e;}
    private EditText noteCompact(LinearLayout p,String label,String val,boolean numeric){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);TextView l=text(label,11,true,Color.rgb(35,52,68));l.setPadding(0,dp(6),0,dp(3));box.addView(l);EditText e=baseField(val,numeric,false);box.addView(e,new LinearLayout.LayoutParams(-1,dp(44)));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.leftMargin=dp(3);lp.rightMargin=dp(3);p.addView(box,lp);return e;}
    private EditText noteMulti(LinearLayout p,String label,String val){TextView l=text(label,12,true,Color.rgb(35,52,68));l.setPadding(0,dp(8),0,dp(3));p.addView(l);EditText e=baseField(val,false,true);p.addView(e,new LinearLayout.LayoutParams(-1,dp(82)));return e;}
    private EditText baseField(String val,boolean numeric,boolean multi){EditText e=new EditText(this);e.setText(val==null?"":val);e.setTextSize(14);e.setPadding(dp(10),dp(7),dp(10),dp(7));e.setBackground(bg(Color.rgb(249,250,252),Color.rgb(220,226,232),12));e.setGravity((multi?Gravity.TOP:Gravity.CENTER_VERTICAL)|(rtl()?Gravity.RIGHT:Gravity.LEFT));if(numeric)e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);else if(multi){e.setSingleLine(false);e.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);}else e.setSingleLine(true);return e;}
    private static class NoteData{String kw="",type="",dayFrom="",dayTo="",nightA="",nightHours="",emA="",emDuration="",custom="";}
    private NoteData parseNote(String note,OfferStudioActivity.Draft d){NoteData n=new NoteData();n.kw=d==null?"":cleanCap(d.capacity);n.type=d==null?"":d.system;if(note==null||note.trim().isEmpty())return n;try{Pattern p=Pattern.compile("منظومة شمسية بقدرة (.*?) كيلو واط نوع (.*?) يتحمل كالآتي:\\n- بالنهار من (.*?) الى (.*?) امبير\\n- بالليل (.*?) امبير لمدة (.*?) ساعات\\n- وفي حالات الضرورة يتحمل (.*?) امبير لمدة (.*?)(?:\\n\\nملاحظة مخصصة: (.*))?$",Pattern.DOTALL);Matcher m=p.matcher(note.trim());if(m.matches()){n.kw=m.group(1);n.type=m.group(2);n.dayFrom=m.group(3);n.dayTo=m.group(4);n.nightA=m.group(5);n.nightHours=m.group(6);n.emA=m.group(7);n.emDuration=m.group(8);n.custom=m.group(9)==null?"":m.group(9);return n;}}catch(Exception ignored){}if(!note.isEmpty())n.custom=note;return n;}
    private String cleanCap(String s){if(s==null)return"";return s.replaceAll("(?i)\\s*(kW|كيلو\\s*واط|کیلو\\s*وات|كيلو\\s*واط)$","").trim();}
    private String v(EditText e){String s=e.getText().toString().trim();return s.isEmpty()?"___":s;}
    private String buildNote(EditText kw,EditText type,EditText df,EditText dt,EditText na,EditText nh,EditText ea,EditText ed,EditText custom){String main;if("ar".equals(lang()))main="منظومة شمسية بقدرة "+v(kw)+" كيلو واط نوع "+v(type)+" يتحمل كالآتي:\n- بالنهار من "+v(df)+" الى "+v(dt)+" امبير\n- بالليل "+v(na)+" امبير لمدة "+v(nh)+" ساعات\n- وفي حالات الضرورة يتحمل "+v(ea)+" امبير لمدة "+v(ed);else if("ku".equals(lang()))main="سیستەمێکی خۆرەوی بە توانای "+v(kw)+" کیلۆوات، جۆری "+v(type)+"، ئەمانە بەرگە دەگرێت:\n- لە ڕۆژدا لە "+v(df)+" بۆ "+v(dt)+" ئەمپێر\n- لە شەودا "+v(na)+" ئەمپێر بۆ "+v(nh)+" کاتژمێر\n- لە حاڵەتی پێویستدا "+v(ea)+" ئەمپێر بۆ "+v(ed);else main="Solar system capacity "+v(kw)+" kW, type "+v(type)+", supports:\n- Daytime from "+v(df)+" to "+v(dt)+" A\n- Night "+v(na)+" A for "+v(nh)+" hours\n- Emergency "+v(ea)+" A for "+v(ed);String c=custom.getText().toString().trim();if(!c.isEmpty())main+="\n\n"+tx("Custom note: ","ملاحظة مخصصة: ","تێبینی تایبەت: ")+c;return main;}

    /* ---------------- Preview / PDF ---------------- */
    private void decoratePreview(View root){addPreviewTotal(root);wirePreviewPdfActions(root);}
    private void addPreviewTotal(View root){if(hasTag(root,"v440_preview_total"))return;TextView details=findText(root,new String[]{"DETAILS","التفاصيل","وردەکاری"});if(details==null)return;ViewParent sec=details.getParent();if(!(sec instanceof View))return;ViewParent pp=((View)sec).getParent();if(!(pp instanceof ViewGroup))return;ViewGroup paper=(ViewGroup)pp;int at=paper.indexOfChild((View)sec);TextView t=text(tx("Grand Total: ","المجموع الكلي: ","کۆی گشتی: ")+money(grandTotal()),16,true,Color.rgb(20,153,112));t.setTag("v440_preview_total");t.setPadding(dp(10),dp(10),dp(10),dp(10));t.setBackground(bg(Color.rgb(236,248,244),Color.rgb(189,226,212),14));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(10);lp.bottomMargin=dp(6);paper.addView(t,Math.max(0,at),lp);}
    private TextView findText(View v,String[] arr){if(v instanceof TextView){String s=String.valueOf(((TextView)v).getText()).trim();for(String x:arr)if(x.equals(s))return(TextView)v;}if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){TextView t=findText(g.getChildAt(i),arr);if(t!=null)return t;}}return null;}
    private void wirePreviewPdfActions(View root){Button export=findButton(root,new String[]{"Export PDF","تصدير PDF","PDF دەربکە","PDF هەناردە بکە"});if(export==null)return;export.setText(tx("Share PDF","مشاركة PDF","PDF هاوبەش بکە"));export.setOnClickListener(v->{saveOfferQuiet();File f=buildPdfFile();if(f!=null)sharePdf(f);});ViewParent p=export.getParent();if(!(p instanceof ViewGroup))return;ViewGroup actions=(ViewGroup)p;ViewParent pp=actions.getParent();if(!(pp instanceof LinearLayout))return;LinearLayout page=(LinearLayout)pp;if(hasTag(page,"v440_save_pdf"))return;Button savePdf=new Button(this);savePdf.setTag("v440_save_pdf");savePdf.setText(tx("Save PDF to phone","حفظ PDF في الهاتف","PDF لە مۆبایل پاشەکەوت بکە"));savePdf.setTextSize(14);savePdf.setTypeface(Typeface.DEFAULT_BOLD);savePdf.setTextColor(Color.rgb(18,50,79));savePdf.setAllCaps(false);savePdf.setBackground(bg(Color.rgb(238,244,250),Color.rgb(197,214,228),15));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(50));lp.leftMargin=dp(18);lp.rightMargin=dp(18);lp.bottomMargin=dp(10);savePdf.setLayoutParams(lp);savePdf.setOnClickListener(v->{saveOfferQuiet();File f=buildPdfFile();if(f!=null)launchSavePdf(f);});page.addView(savePdf);}
    private Button findButton(View v,String[] vals){if(v instanceof Button){String s=String.valueOf(((Button)v).getText()).trim();for(String x:vals)if(x.equals(s))return(Button)v;}if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){Button b=findButton(g.getChildAt(i),vals);if(b!=null)return b;}}return null;}
    private void saveOfferQuiet(){invoke("saveOffer");}

    private File buildPdfFile(){OfferStudioActivity.Draft d=draft();if(d==null)return null;try{File dir=getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);if(dir==null)throw new Exception("Storage unavailable");dir.mkdirs();String safe=(d.client==null?"Offer":d.client).replaceAll("[^\\p{L}\\p{N}_-]","_");if(safe.isEmpty())safe="Offer";File file=new File(dir,"IGP_Offer_"+safe+"_"+System.currentTimeMillis()+".pdf");PdfDocument doc=new PdfDocument();PdfWriter w=new PdfWriter(doc);w.newPage();w.header();w.section(tx("CLIENT","العميل","کڕیار"));w.row(tx("Client","العميل","کڕیار"),dash(d.client));w.row(tx("Phone","رقم الهاتف","ژمارەی تەلەفۆن"),dash(d.phone));w.row(tx("Location","الموقع","شوێن"),dash(d.location));w.row(tx("Date","التاريخ","بەروار"),dash(d.date));w.section(tx("SYSTEM","المنظومة","سیستەم"));w.row(tx("Type","النوع","جۆر"),dash(d.system));w.row(tx("Capacity","السعة","توانا"),dash(d.capacity));w.row(tx("Phase","الطور","فاز"),dash(d.phase));w.section(tx("ITEMS","المواد","ماددەکان"));w.itemHeader();for(OfferStudioActivity.LineItem i:d.items)w.item(i);if(showPrices()){w.ensure(35);w.bigText(tx("Grand Total: ","المجموع الكلي: ","کۆی گشتی: ")+money(grandTotal()),Color.rgb(20,153,112));}w.section(tx("DETAILS","التفاصيل","وردەکاری"));w.row(tx("Installation","التركيب","دامەزراندن"),d.installation?tx("Included","مشمول","ناوخۆکراوە"):tx("Not included","غير مشمول","ناوخۆنەکراوە"));w.row(tx("Transport","النقل","گواستنەوە"),d.transport?tx("Included","مشمول","ناوخۆکراوە"):tx("Not included","غير مشمول","ناوخۆنەکراوە"));w.row(tx("Organizer","منظم الكشف","ڕێکخەر"),dash(d.organizer));w.row(tx("Photos","الصور","وێنەکان"),String.valueOf(d.photos.size()));if(d.notes!=null&&!d.notes.trim().isEmpty()){w.section(tx("NOTES","الملاحظات","تێبینی"));w.paragraph(d.notes);}w.finishPage();
        for(int i=0;i<d.photos.size();i++){Bitmap bm=decodeImage(Uri.parse(d.photos.get(i)));if(bm==null)continue;w.newPage();w.headerSmall(tx("Site Photo ","صورة الموقع ","وێنەی شوێن ")+(i+1));float scale=Math.min(500f/bm.getWidth(),680f/bm.getHeight());int nw=(int)(bm.getWidth()*scale),nh=(int)(bm.getHeight()*scale);int left=(595-nw)/2,top=90;w.canvas.drawBitmap(bm,null,new Rect(left,top,left+nw,top+nh),null);w.finishPage();bm.recycle();}
        FileOutputStream out=new FileOutputStream(file);doc.writeTo(out);out.close();doc.close();return file;}catch(Exception e){Toast.makeText(this,tx("PDF error: ","خطأ PDF: ","هەڵەی PDF: ")+e.getMessage(),Toast.LENGTH_LONG).show();return null;}}
    private String dash(String s){return s==null||s.trim().isEmpty()?"—":s.trim();}
    private void sharePdf(File f){Uri u=new Uri.Builder().scheme("content").authority(getPackageName()+".provider").appendPath(f.getName()).build();Intent sh=new Intent(Intent.ACTION_SEND);sh.setType("application/pdf");sh.putExtra(Intent.EXTRA_STREAM,u);sh.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(sh,tx("Share PDF","مشاركة PDF","PDF هاوبەش بکە")));}
    private void launchSavePdf(File f){pendingPdf=f;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/pdf");i.putExtra(Intent.EXTRA_TITLE,f.getName());startActivityForResult(i,SAVE_PDF_REQUEST);}
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(req==SAVE_PDF_REQUEST&&res==RESULT_OK&&data!=null&&data.getData()!=null&&pendingPdf!=null){try{InputStream in=new FileInputStream(pendingPdf);OutputStream out=getContentResolver().openOutputStream(data.getData(),"w");byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);in.close();if(out!=null)out.close();Toast.makeText(this,tx("PDF saved successfully","تم حفظ ملف PDF بنجاح","PDF بە سەرکەوتوویی پاشەکەوت کرا"),Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,tx("Could not save PDF","تعذر حفظ PDF","نەتوانرا PDF پاشەکەوت بکرێت"),Toast.LENGTH_LONG).show();}pendingPdf=null;}}

    private Bitmap decodeImage(Uri u){try{InputStream a=getContentResolver().openInputStream(u);BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeStream(a,null,o);if(a!=null)a.close();int s=1;while(o.outWidth/s>1200||o.outHeight/s>1200)s*=2;BitmapFactory.Options b=new BitmapFactory.Options();b.inSampleSize=s;InputStream in=getContentResolver().openInputStream(u);Bitmap bm=BitmapFactory.decodeStream(in,null,b);if(in!=null)in.close();return bm;}catch(Exception e){return null;}}

    private class PdfWriter{
        final PdfDocument doc;final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);PdfDocument.Page page;Canvas canvas;int pageNo=0,y=0;PdfWriter(PdfDocument d){doc=d;}
        void newPage(){pageNo++;page=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo).create());canvas=page.getCanvas();canvas.drawColor(Color.WHITE);y=92;}
        void finishPage(){if(page!=null){p.setColor(Color.rgb(20,153,112));p.setTextSize(9);p.setTypeface(Typeface.DEFAULT);p.setTextAlign(rtl()?Paint.Align.RIGHT:Paint.Align.LEFT);canvas.drawText("Infinity Green Power · IGP Offer Studio",rtl()?555:40,820,p);doc.finishPage(page);page=null;}}
        void ensure(int h){if(y+h>790){finishPage();newPage();headerSmall(tx("Offer continued","تكملة العرض","بەردەوامی پێشنیار"));}}
        void header(){p.setTextAlign(rtl()?Paint.Align.RIGHT:Paint.Align.LEFT);draw("INFINITY GREEN POWER",rtl()?555:40,40,24,true,Color.rgb(15,54,89));draw(tx("Solar System Offer","عرض منظومة شمسية","پێشنیاری سیستەمی خۆرەوی"),rtl()?555:40,65,13,true,Color.rgb(20,153,112));p.setColor(Color.rgb(20,153,112));canvas.drawRect(40,80,555,83,p);y=100;}
        void headerSmall(String s){draw(s,rtl()?555:40,45,18,true,Color.rgb(15,54,89));p.setColor(Color.rgb(20,153,112));canvas.drawRect(40,61,555,64,p);y=78;}
        void section(String s){ensure(34);p.setColor(Color.rgb(15,54,89));canvas.drawRoundRect(40,y,555,y+24,8,8,p);draw(s,rtl()?545:50,y+16,11,true,Color.WHITE);y+=32;}
        void row(String k,String v){ensure(27);if(rtl()){draw(k,545,y+15,10,true,Color.rgb(104,119,132));draw(v,365,y+15,10,false,Color.rgb(18,32,45));}else{draw(k,45,y+15,10,true,Color.rgb(104,119,132));draw(v,180,y+15,10,false,Color.rgb(18,32,45));}p.setColor(Color.rgb(225,231,237));canvas.drawLine(40,y+23,555,y+23,p);y+=27;}
        void itemHeader(){ensure(24);draw(tx("ITEM","المادة","ماددە"),rtl()?555:45,y+15,8,true,Color.rgb(20,153,112));draw(tx("QTY","الكمية","بڕ"),rtl()?240:365,y+15,8,true,Color.rgb(20,153,112));if(showPrices()){draw(tx("UNIT","الوحدة","یەکە"),rtl()?160:435,y+15,8,true,Color.rgb(20,153,112));draw(tx("TOTAL","المجموع","کۆ"),rtl()?75:505,y+15,8,true,Color.rgb(20,153,112));}y+=22;}
        void item(OfferStudioActivity.LineItem i){ensure(28);draw(trim(i.name,42),rtl()?555:45,y+14,9,false,Color.rgb(18,32,45));draw(i.qty==null||i.qty.isEmpty()?"1":i.qty,rtl()?240:370,y+14,9,false,Color.rgb(18,32,45));if(showPrices()){draw(i.price==null||i.price.isEmpty()?"—":money(num(i.price,BigDecimal.ZERO)),rtl()?160:430,y+14,8,false,Color.rgb(18,32,45));draw(money(lineTotal(i)),rtl()?75:500,y+14,8,true,Color.rgb(15,54,89));}p.setColor(Color.rgb(225,231,237));canvas.drawLine(40,y+22,555,y+22,p);y+=26;}
        void bigText(String s,int color){ensure(30);draw(s,rtl()?555:45,y+20,13,true,color);y+=30;}
        void paragraph(String s){List<String> lines=wrap(s,500,10);for(String line:lines){ensure(18);draw(line,rtl()?555:40,y+14,10,false,Color.rgb(55,70,84));y+=17;}y+=6;}
        List<String> wrap(String text,float max,int size){ArrayList<String> out=new ArrayList<>();if(text==null)return out;p.setTextSize(size);for(String para:text.split("\\n",-1)){if(para.isEmpty()){out.add("");continue;}String prefix=para.startsWith("-")?"- ":"";String clean=para.startsWith("-")?para.substring(1).trim():para;StringBuilder line=new StringBuilder(prefix);for(String word:clean.split("\\s+")){String test=line.length()==0?word:line+" "+word;if(p.measureText(test)>max&&line.length()>0){out.add(line.toString());line=new StringBuilder(word);}else{if(line.length()>0&&!line.toString().endsWith(" "))line.append(' ');line.append(word);}}if(line.length()>0)out.add(line.toString());}return out;}
        void draw(String s,float x,float yy,float size,boolean bold,int color){p.setColor(color);p.setTextSize(size);p.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);p.setTextAlign(rtl()?Paint.Align.RIGHT:Paint.Align.LEFT);canvas.drawText(s==null?"":s,x,yy,p);}
        String trim(String s,int n){if(s==null)return"";return s.length()<=n?s:s.substring(0,n-1)+"…";}
    }

    private TextView text(String s,int size,boolean bold,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT_BOLD);t.setGravity(rtl()?Gravity.RIGHT:Gravity.LEFT);return t;}
}
