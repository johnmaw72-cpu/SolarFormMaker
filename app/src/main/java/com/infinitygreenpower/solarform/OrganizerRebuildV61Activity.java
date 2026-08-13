package com.infinitygreenpower.solarform;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.*;
import android.view.*;
import android.widget.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * v6.1 refinement over the clean v6 rebuild.
 * No legacy OfferStudio styling layers.
 */
public class OrganizerRebuildV61Activity extends OrganizerRebuildActivity {
    private final WeakHashMap<View,Boolean> styled = new WeakHashMap<>();
    private final HashMap<String,String> catalogCategories = new HashMap<>();
    private ViewTreeObserver.OnGlobalLayoutListener layoutListener;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable restyle = () -> applyUiFixes();
    private int baseContentTop = 0;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        loadCatalogNames();
        installSafeArea();
        installDebouncedStyler();
        ui.postDelayed(restyle, 120);
    }

    @Override protected void onDestroy() {
        View content=findViewById(android.R.id.content);
        if(content!=null && layoutListener!=null && content.getViewTreeObserver().isAlive())
            content.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
        ui.removeCallbacks(restyle);
        super.onDestroy();
    }

    private int dp(int n){ return (int)(n*getResources().getDisplayMetrics().density+.5f); }
    private String lang(){ return getSharedPreferences("offer_studio_settings",MODE_PRIVATE).getString("lang","ar"); }
    private String tx(String en,String ar,String ku){ return "ar".equals(lang())?ar:("ku".equals(lang())?ku:en); }
    private boolean rtl(){ return !"en".equals(lang()); }

    private void installSafeArea(){
        View content=findViewById(android.R.id.content);
        if(content==null)return;
        baseContentTop=content.getPaddingTop();
        if(Build.VERSION.SDK_INT>=20){
            content.setOnApplyWindowInsetsListener((v,insets)->{
                int top=insets.getSystemWindowInsetTop();
                v.setPadding(v.getPaddingLeft(),baseContentTop+top,v.getPaddingRight(),v.getPaddingBottom());
                return insets;
            });
            content.requestApplyInsets();
        }
    }

    private void installDebouncedStyler(){
        View content=findViewById(android.R.id.content);
        if(content==null)return;
        layoutListener=()->{
            ui.removeCallbacks(restyle);
            ui.postDelayed(restyle,55);
        };
        content.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
    }

    private void applyUiFixes(){
        View root=findViewById(android.R.id.content);
        if(root==null)return;
        boolean catalogScreen=containsExact(root,new String[]{"Material catalog","كتالوج المواد","کاتەلۆگی ماددە"});
        boolean materialsStep=containsExact(root,new String[]{"Materials","مواد العرض","ماددەکان"});
        boolean settingsScreen=containsExact(root,new String[]{"Settings","الإعدادات","ڕێکخستنەکان"});
        walk(root,catalogScreen,materialsStep,settingsScreen);
    }

    private void walk(View v,boolean catalogScreen,boolean materialsStep,boolean settingsScreen){
        if(v==null)return;
        if(!styled.containsKey(v)){
            if(v instanceof Button) compactButton((Button)v);
            if(v instanceof TextView && !(v instanceof EditText)) tuneText((TextView)v);
            styled.put(v,Boolean.TRUE);
        }

        if(catalogScreen && v instanceof LinearLayout){
            String product=findCatalogName((ViewGroup)v);
            if(product!=null){
                String category=catalogCategories.get(product);
                v.setOnClickListener(x->showAddEditor(product,category,false));
            }
        }

        if(materialsStep && v instanceof Button){
            Button b=(Button)v;
            String s=String.valueOf(b.getText()).trim();
            if(matches(s,"Browse catalog","اختيار من الكتالوج","کاتەلۆگ ببینە") ||
               matches(s,"Browse catalog","اختيار من الكتالوج","لە کاتەلۆگ")){
                b.setOnClickListener(x->showCompactPicker());
            }
        }

        if(settingsScreen && v instanceof Button){
            Button b=(Button)v;
            String s=String.valueOf(b.getText()).trim();
            if(matches(s,"Manage organizer profiles","إدارة منظمي الكشف","بەڕێوەبردنی ڕێکخەران"))
                b.setOnClickListener(x->showOrganizerManager());
        }

        if(v instanceof ViewGroup){
            ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++)walk(g.getChildAt(i),catalogScreen,materialsStep,settingsScreen);
        }
    }

    private void compactButton(Button b){
        b.setTextSize(12);
        b.setPadding(dp(9),0,dp(9),0);
        b.setMinHeight(0); b.setMinimumHeight(0);
        b.setMinWidth(0); b.setMinimumWidth(0);
        ViewGroup.LayoutParams raw=b.getLayoutParams();
        if(raw instanceof LinearLayout.LayoutParams){
            LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)raw;
            if(lp.height>0){
                if(lp.height>dp(42)) lp.height=dp(42);
                else if(lp.height>dp(37)) lp.height=dp(37);
            }
            lp.topMargin=Math.min(lp.topMargin,dp(4));
            lp.bottomMargin=Math.min(lp.bottomMargin,dp(4));
            lp.leftMargin=Math.min(lp.leftMargin,dp(3));
            lp.rightMargin=Math.min(lp.rightMargin,dp(3));
            b.setLayoutParams(lp);
        }
    }

    private void tuneText(TextView t){
        String s=String.valueOf(t.getText()).trim();
        float sp=t.getTextSize()/getResources().getDisplayMetrics().scaledDensity;
        if(sp>24 && !"+".equals(s)) t.setTextSize(24);
        if(s.matches("^(5|8|10|12|16|20|30)\\s*kW$")){
            t.setTextSize(9);
            t.setSingleLine(true);
            t.setEllipsize(null);
            t.setPadding(dp(2),0,dp(2),0);
            t.setGravity(Gravity.CENTER);
        }
        if("+".equals(s) && t.getLayoutParams()!=null && t.getLayoutParams().width>=dp(48)){
            ViewGroup.LayoutParams lp=t.getLayoutParams();
            lp.width=dp(46); lp.height=dp(46); t.setLayoutParams(lp); t.setTextSize(25);
        }
    }

    private boolean containsExact(View v,String[] values){
        if(v instanceof TextView){String s=String.valueOf(((TextView)v).getText()).trim();for(String x:values)if(x.equals(s))return true;}
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(containsExact(g.getChildAt(i),values))return true;}
        return false;
    }
    private boolean matches(String actual,String en,String ar,String ku){return actual.equals(en)||actual.equals(ar)||actual.equals(ku);}

    private void loadCatalogNames(){
        catalogCategories.clear();
        for(int part=1;part<=6;part++){
            try{
                BufferedReader br=new BufferedReader(new InputStreamReader(getAssets().open("catalog_v4_"+part+".tsv")));
                String line;
                while((line=br.readLine())!=null){
                    String[] x=line.split("\\t",-1);
                    if(x.length>=2 && !x[1].trim().isEmpty())catalogCategories.put(x[1].trim(),x[0].trim());
                }
                br.close();
            }catch(Exception ignored){}
        }
    }

    private String findCatalogName(ViewGroup g){
        for(int i=0;i<g.getChildCount();i++){
            View c=g.getChildAt(i);
            if(c instanceof TextView){
                String s=String.valueOf(((TextView)c).getText()).trim();
                if(catalogCategories.containsKey(s))return s;
            }
            if(c instanceof ViewGroup){String s=findCatalogName((ViewGroup)c);if(s!=null)return s;}
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private ArrayList<Object> itemsList(){
        try{Field f=OrganizerRebuildActivity.class.getDeclaredField("items");f.setAccessible(true);return (ArrayList<Object>)f.get(this);}catch(Exception e){return null;}
    }

    private void invokePrivate(String name){
        try{Method m=OrganizerRebuildActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(this);}catch(Exception ignored){}
    }

    private Object newLineItem(String name,String category,String qty,String price){
        try{
            Class<?> c=Class.forName("com.infinitygreenpower.solarform.OrganizerRebuildActivity$LineItem");
            Constructor<?> ct=c.getDeclaredConstructor();ct.setAccessible(true);Object it=ct.newInstance();
            setField(c,it,"name",name);setField(c,it,"category",category==null?"":category);setField(c,it,"qty",qty);setField(c,it,"price",price);return it;
        }catch(Exception e){return null;}
    }
    private void setField(Class<?> c,Object o,String field,String value)throws Exception{Field f=c.getDeclaredField(field);f.setAccessible(true);f.set(o,value);}

    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));if(stroke!=0)g.setStroke(dp(1),stroke);return g;}
    private EditText dialogField(String hint,String value,boolean numeric){
        EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setTextSize(14);e.setSingleLine(true);e.setPadding(dp(12),0,dp(12),0);e.setBackground(rounded(Color.rgb(249,251,253),Color.rgb(220,228,235),14));e.setGravity((rtl()?Gravity.RIGHT:Gravity.LEFT)|Gravity.CENTER_VERTICAL);if(numeric)e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);return e;
    }

    private void showAddEditor(String name,String category,boolean returnToWizard){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(8),dp(18),0);
        TextView product=new TextView(this);product.setText(name);product.setTextSize(16);product.setTypeface(Typeface.DEFAULT_BOLD);product.setTextColor(Color.rgb(19,50,76));product.setGravity(rtl()?Gravity.RIGHT:Gravity.LEFT);box.addView(product);
        Space s1=new Space(this);box.addView(s1,new LinearLayout.LayoutParams(1,dp(12)));
        EditText qty=dialogField(tx("Quantity","الكمية","بڕ"),"1",true);box.addView(qty,new LinearLayout.LayoutParams(-1,dp(46)));
        Space s2=new Space(this);box.addView(s2,new LinearLayout.LayoutParams(1,dp(8)));
        EditText price=dialogField(tx("Unit price (optional)","سعر الوحدة (اختياري)","نرخی یەکە (ئارەزوومەندانە)"),"",true);box.addView(price,new LinearLayout.LayoutParams(-1,dp(46)));
        AlertDialog dlg=new AlertDialog.Builder(this).setTitle(tx("Add material","إضافة المادة","ماددە زیاد بکە")).setView(box)
                .setPositiveButton(tx("Add","إضافة","زیادکردن"),null)
                .setNegativeButton(tx("Cancel","إلغاء","هەڵوەشاندنەوە"),null).create();
        dlg.setOnShowListener(x->{
            Button ok=dlg.getButton(AlertDialog.BUTTON_POSITIVE);compactButton(ok);
            Button cancel=dlg.getButton(AlertDialog.BUTTON_NEGATIVE);compactButton(cancel);
            ok.setOnClickListener(v->{
                String q=qty.getText().toString().trim();if(q.isEmpty())q="1";
                Object it=newLineItem(name,category,q,price.getText().toString().trim());
                ArrayList<Object> list=itemsList();if(it!=null&&list!=null)list.add(it);
                dlg.dismiss();
                if(returnToWizard)invokePrivate("showWizard");
                else Toast.makeText(this,tx("Material added","تمت إضافة المادة","ماددە زیادکرا"),Toast.LENGTH_SHORT).show();
            });
        });
        dlg.show();
    }

    private void showCompactPicker(){
        final Dialog d=new Dialog(this);
        LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);shell.setPadding(dp(14),dp(14),dp(14),dp(12));shell.setBackgroundColor(Color.WHITE);
        EditText search=dialogField(tx("Search item name…","ابحث باسم المادة…","بە ناوی ماددە بگەڕێ…"),"",false);shell.addView(search,new LinearLayout.LayoutParams(-1,dp(46)));
        ListView list=new ListView(this);shell.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        final ArrayList<String> names=new ArrayList<>(catalogCategories.keySet());Collections.sort(names,String.CASE_INSENSITIVE_ORDER);
        final ArrayList<String> filtered=new ArrayList<>(names);
        final ArrayAdapter<String> ad=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,filtered);list.setAdapter(ad);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int a,int b,int c){String q=s.toString().trim().toLowerCase(Locale.ROOT);filtered.clear();for(String n:names)if(q.isEmpty()||n.toLowerCase(Locale.ROOT).contains(q))filtered.add(n);ad.notifyDataSetChanged();}});
        list.setOnItemClickListener((p,v,pos,id)->{String n=filtered.get(pos);d.dismiss();showAddEditor(n,catalogCategories.get(n),true);});
        d.setContentView(shell);d.show();Window w=d.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.92f),(int)(getResources().getDisplayMetrics().heightPixels*.72f));}}

    private void showOrganizerManager(){
        SharedPreferences sp=getSharedPreferences("offer_studio_settings",MODE_PRIVATE);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(8),dp(18),0);
        TextView hint=new TextView(this);hint.setText(tx("One organizer per line","منظم واحد في كل سطر","هەر ڕێکخەر لە هێڵێک"));hint.setTextSize(12);hint.setTextColor(Color.rgb(118,132,146));hint.setGravity(rtl()?Gravity.RIGHT:Gravity.LEFT);box.addView(hint);
        EditText e=new EditText(this);e.setText(sp.getString("organizers",""));e.setTextSize(14);e.setMinLines(3);e.setMaxLines(5);e.setGravity((rtl()?Gravity.RIGHT:Gravity.LEFT)|Gravity.TOP);e.setPadding(dp(12),dp(10),dp(12),dp(10));e.setBackground(rounded(Color.rgb(249,251,253),Color.rgb(220,228,235),14));LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,dp(128));ep.topMargin=dp(7);box.addView(e,ep);
        AlertDialog dlg=new AlertDialog.Builder(this).setTitle(tx("Organizer profiles","منظمو الكشف","ڕێکخەران")).setView(box)
                .setPositiveButton(tx("Save","حفظ","پاشەکەوت"),(d,w)->sp.edit().putString("organizers",e.getText().toString()).apply())
                .setNegativeButton(tx("Cancel","إلغاء","هەڵوەشاندنەوە"),null).create();
        dlg.setOnShowListener(x->{compactButton(dlg.getButton(AlertDialog.BUTTON_POSITIVE));compactButton(dlg.getButton(AlertDialog.BUTTON_NEGATIVE));});dlg.show();
    }
}
