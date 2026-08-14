package com.infinitygreenpower.solarform;

import android.app.*;
import android.os.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.content.*;
import android.text.InputType;
import android.widget.*;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

/** v6.3.2: professional custom dialogs for catalog/material editing and organizer profiles. */
public class OrganizerRebuildV632Activity extends OrganizerRebuildV631Activity {
    private final int NAVY=Color.rgb(19,50,76), TEAL=Color.rgb(28,196,163), TEAL_DARK=Color.rgb(18,158,132), MUTED=Color.rgb(118,132,146), BORDER=Color.rgb(224,230,236), BG=Color.rgb(247,250,252);
    private final HashMap<String,String> catalog=new HashMap<>();
    private final Set<View> bound=java.util.Collections.newSetFromMap(new WeakHashMap<View,Boolean>());
    private final Handler handler=new Handler(Looper.getMainLooper());
    private ViewTreeObserver.OnGlobalLayoutListener listener;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        loadCatalog();
        View root=findViewById(android.R.id.content);
        if(root!=null){
            listener=()->{handler.removeCallbacks(bind);handler.postDelayed(bind,120);};
            root.getViewTreeObserver().addOnGlobalLayoutListener(listener);
        }
        handler.postDelayed(bind,220);
    }

    @Override protected void onDestroy(){
        View root=findViewById(android.R.id.content);
        if(root!=null&&listener!=null&&root.getViewTreeObserver().isAlive())root.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
        handler.removeCallbacks(bind);
        super.onDestroy();
    }

    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
    private String lang(){return getSharedPreferences("offer_studio_settings",MODE_PRIVATE).getString("lang","ar");}
    private boolean rtl(){return!"en".equals(lang());}
    private String tx(String en,String ar,String ku){return"ar".equals(lang())?ar:("ku".equals(lang())?ku:en);}

    private GradientDrawable rounded(int fill,int stroke,int radius){
        GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));if(stroke!=0)g.setStroke(dp(1),stroke);return g;
    }
    private TextView label(String s,int size,boolean bold,int color){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);t.setGravity(rtl()?Gravity.RIGHT:Gravity.LEFT);return t;
    }
    private EditText field(String hint,String value,boolean numeric){
        EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setTextSize(14);e.setSingleLine(true);e.setTextColor(NAVY);e.setHintTextColor(Color.rgb(153,164,174));e.setPadding(dp(13),0,dp(13),0);e.setBackground(rounded(Color.WHITE,BORDER,14));e.setGravity((rtl()?Gravity.RIGHT:Gravity.LEFT)|Gravity.CENTER_VERTICAL);if(numeric)e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);return e;
    }
    private Button button(String s,boolean primary){
        Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(12);b.setTypeface(Typeface.DEFAULT_BOLD);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(dp(12),0,dp(12),0);b.setTextColor(primary?Color.WHITE:TEAL_DARK);b.setBackground(rounded(primary?TEAL:Color.WHITE,TEAL,15));if(Build.VERSION.SDK_INT>=21){b.setElevation(0);b.setStateListAnimator(null);}return b;
    }

    private final Runnable bind=()->{
        View root=findViewById(android.R.id.content);
        if(root!=null)walk(root);
    };
    private void walk(View v){
        if(v instanceof LinearLayout){String name=findProduct((ViewGroup)v);if(name!=null&&!bound.contains(v)){bound.add(v);String category=catalog.get(name);v.setOnClickListener(x->showMaterialDialog(name,category));}}
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)walk(g.getChildAt(i));}
    }
    private String findProduct(ViewGroup g){
        for(int i=0;i<g.getChildCount();i++){
            View v=g.getChildAt(i);
            if(v instanceof TextView){String s=String.valueOf(((TextView)v).getText()).trim();if(catalog.containsKey(s))return s;}
            if(v instanceof ViewGroup){String s=findProduct((ViewGroup)v);if(s!=null)return s;}
        }
        return null;
    }
    private void loadCatalog(){
        catalog.clear();
        for(int part=1;part<=6;part++)try{
            BufferedReader br=new BufferedReader(new InputStreamReader(getAssets().open("catalog_v4_"+part+".tsv")));
            String line;while((line=br.readLine())!=null){String[] x=line.split("\t",-1);if(x.length>=2&&!x[1].trim().isEmpty())catalog.put(x[1].trim(),x[0].trim());}br.close();
        }catch(Exception ignored){}
    }

    private void showMaterialDialog(String name,String category){
        final Dialog d=new Dialog(this);
        LinearLayout outer=new LinearLayout(this);outer.setOrientation(LinearLayout.VERTICAL);outer.setPadding(dp(18),dp(18),dp(18),dp(16));outer.setBackground(rounded(Color.WHITE,0,24));

        LinearLayout header=new LinearLayout(this);header.setOrientation(LinearLayout.HORIZONTAL);header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(label(tx("Add material","إضافة مادة","ماددە زیاد بکە"),20,true,NAVY));
        TextView sub=label(tx("Set quantity and optional unit price","حدد الكمية وسعر الوحدة الاختياري","بڕ و نرخی یەکە دیاری بکە"),11,false,MUTED);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=dp(2);titles.addView(sub,sp);
        header.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        TextView close=label("×",24,false,MUTED);close.setGravity(Gravity.CENTER);close.setOnClickListener(v->d.dismiss());header.addView(close,new LinearLayout.LayoutParams(dp(38),dp(38)));
        outer.addView(header);

        LinearLayout itemCard=new LinearLayout(this);itemCard.setOrientation(LinearLayout.VERTICAL);itemCard.setPadding(dp(13),dp(11),dp(13),dp(11));itemCard.setBackground(rounded(BG,BORDER,16));
        itemCard.addView(label(name,14,true,NAVY));TextView cat=label(category==null?"":category,10,false,MUTED);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(3);itemCard.addView(cat,cp);
        LinearLayout.LayoutParams icp=new LinearLayout.LayoutParams(-1,-2);icp.topMargin=dp(14);outer.addView(itemCard,icp);

        TextView ql=label(tx("Quantity","الكمية","بڕ"),11,true,MUTED);LinearLayout.LayoutParams qlp=new LinearLayout.LayoutParams(-1,-2);qlp.topMargin=dp(14);outer.addView(ql,qlp);
        LinearLayout quantityRow=new LinearLayout(this);quantityRow.setOrientation(LinearLayout.HORIZONTAL);quantityRow.setGravity(Gravity.CENTER_VERTICAL);Button minus=button("−",false);EditText qty=field("1","1",true);qty.setGravity(Gravity.CENTER);Button plus=button("+",false);quantityRow.addView(minus,new LinearLayout.LayoutParams(dp(48),dp(42)));LinearLayout.LayoutParams qep=new LinearLayout.LayoutParams(0,dp(42),1);qep.leftMargin=dp(7);qep.rightMargin=dp(7);quantityRow.addView(qty,qep);quantityRow.addView(plus,new LinearLayout.LayoutParams(dp(48),dp(42)));LinearLayout.LayoutParams qr=new LinearLayout.LayoutParams(-1,-2);qr.topMargin=dp(6);outer.addView(quantityRow,qr);
        minus.setOnClickListener(v->{int q=parseQty(qty.getText().toString());qty.setText(String.valueOf(Math.max(1,q-1)));});plus.setOnClickListener(v->{int q=parseQty(qty.getText().toString());qty.setText(String.valueOf(q+1));});

        TextView pl=label(tx("Unit price","سعر الوحدة","نرخی یەکە"),11,true,MUTED);LinearLayout.LayoutParams plp=new LinearLayout.LayoutParams(-1,-2);plp.topMargin=dp(13);outer.addView(pl,plp);
        String remembered=lastPrice(name);EditText price=field(tx("Optional","اختياري","ئارەزوومەندانە"),remembered,true);LinearLayout.LayoutParams pep=new LinearLayout.LayoutParams(-1,dp(46));pep.topMargin=dp(6);outer.addView(price,pep);
        if(!remembered.isEmpty()){TextView mem=label(tx("Last used price filled automatically","تم تعبئة آخر سعر مستخدم تلقائياً","دوایین نرخ خۆکار پڕکراوەتەوە"),10,false,TEAL_DARK);LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,-2);mp.topMargin=dp(5);outer.addView(mem,mp);}

        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);Button cancel=button(tx("Cancel","إلغاء","هەڵوەشاندنەوە"),false);Button add=button(tx("Add material","إضافة المادة","ماددە زیاد بکە"),true);actions.addView(cancel,new LinearLayout.LayoutParams(0,dp(44),1));LinearLayout.LayoutParams alp=new LinearLayout.LayoutParams(0,dp(44),1);alp.leftMargin=dp(8);actions.addView(add,alp);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.topMargin=dp(18);outer.addView(actions,ap);
        cancel.setOnClickListener(v->d.dismiss());
        add.setOnClickListener(v->{String q=qty.getText().toString().trim();if(q.isEmpty())q="1";String p=price.getText().toString().trim();addItem(name,category,q,p);if(!p.isEmpty())rememberPrice(name,p);d.dismiss();showWizardReflect();});

        d.setContentView(outer);Window w=d.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams lp=w.getAttributes();lp.dimAmount=.45f;w.setAttributes(lp);}d.show();if(w!=null){int width=(int)(getResources().getDisplayMetrics().widthPixels*.90f);w.setLayout(width,WindowManager.LayoutParams.WRAP_CONTENT);w.setGravity(Gravity.CENTER);}
    }

    private int parseQty(String s){try{return Math.max(1,(int)Math.round(Double.parseDouble(s)));}catch(Exception e){return 1;}}
    private String key(String n){return"last_price_"+Integer.toHexString(n.toLowerCase(Locale.ROOT).hashCode());}
    private String lastPrice(String n){return getSharedPreferences("offer_studio_prices",MODE_PRIVATE).getString(key(n),"");}
    private void rememberPrice(String n,String p){getSharedPreferences("offer_studio_prices",MODE_PRIVATE).edit().putString(key(n),p).apply();}

    @SuppressWarnings("unchecked") private void addItem(String name,String category,String qty,String price){
        try{
            Field f=OrganizerRebuildActivity.class.getDeclaredField("items");f.setAccessible(true);ArrayList<OrganizerRebuildActivity.LineItem> list=(ArrayList<OrganizerRebuildActivity.LineItem>)f.get(this);
            OrganizerRebuildActivity.LineItem it=new OrganizerRebuildActivity.LineItem();it.name=name;it.category=category==null?"":category;it.qty=qty;it.price=price;list.add(it);
        }catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private void showWizardReflect(){
        try{java.lang.reflect.Method m=OrganizerRebuildActivity.class.getDeclaredMethod("showWizard");m.setAccessible(true);m.invoke(this);}catch(Exception ignored){}
    }
}
