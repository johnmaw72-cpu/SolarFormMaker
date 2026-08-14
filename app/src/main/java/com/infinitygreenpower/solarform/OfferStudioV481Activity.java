package com.infinitygreenpower.solarform;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.text.*;
import android.view.*;
import android.widget.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * v4.8.1 stability pass on the preferred v4.8 UI.
 * - Keeps v4.8 layout/PDF behavior.
 * - Fixes generated load-note self-duplication on English/Kurdish Step 4.
 * - Replaces rough default item and organizer dialogs with compact app-styled dialogs.
 * - Applies language choice immediately from Settings.
 */
public class OfferStudioV481Activity extends OfferStudioV480Activity {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private boolean running;
    private final Set<View> bound = Collections.newSetFromMap(new WeakHashMap<View,Boolean>());

    private static final int NAVY = Color.rgb(18,50,79);
    private static final int INK = Color.rgb(18,32,45);
    private static final int EMERALD = Color.rgb(20,153,112);
    private static final int MUTED = Color.rgb(104,119,132);
    private static final int LINE = Color.rgb(220,226,232);
    private static final int BG = Color.rgb(247,249,251);

    private final Runnable polish = new Runnable(){
        @Override public void run(){
            if(!running) return;
            try{
                View root=findViewById(android.R.id.content);
                if(root!=null){
                    fixStructuredNote(root);
                    bindButtons(root);
                    bindLanguage(root);
                }
            }catch(Exception ignored){}
            ui.postDelayed(this,260);
        }
    };

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        running=true;
        ui.postDelayed(polish,260);
    }
    @Override protected void onResume(){super.onResume();if(!running){running=true;ui.postDelayed(polish,180);}}
    @Override protected void onPause(){running=false;ui.removeCallbacks(polish);super.onPause();}
    @Override protected void onDestroy(){running=false;ui.removeCallbacks(polish);super.onDestroy();}

    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private SharedPreferences settings(){return getSharedPreferences("offer_studio_settings",MODE_PRIVATE);}
    private String lang(){return settings().getString("lang","en");}
    private boolean rtl(){return !"en".equals(lang());}
    private String tx(String en,String ar,String ku){return "ar".equals(lang())?ar:("ku".equals(lang())?ku:en);}

    private GradientDrawable bg(int fill,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));if(stroke!=0)g.setStroke(dp(1),stroke);return g;}
    private TextView text(String s,int sp,boolean bold,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);t.setGravity(rtl()?Gravity.RIGHT:Gravity.LEFT);return t;}
    private EditText field(String hint,String val,boolean numeric){EditText e=new EditText(this);e.setHint(hint);e.setText(val==null?"":val);e.setTextSize(14);e.setSingleLine(true);e.setPadding(dp(12),0,dp(12),0);e.setTextColor(INK);e.setHintTextColor(Color.rgb(145,157,168));e.setBackground(bg(Color.WHITE,LINE,13));e.setGravity((rtl()?Gravity.RIGHT:Gravity.LEFT)|Gravity.CENTER_VERTICAL);if(numeric)e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);return e;}
    private Button btn(String s,boolean primary){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(12);b.setTypeface(Typeface.DEFAULT_BOLD);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(dp(10),0,dp(10),0);b.setTextColor(primary?Color.WHITE:NAVY);b.setBackground(bg(primary?EMERALD:Color.WHITE,primary?EMERALD:LINE,13));if(Build.VERSION.SDK_INT>=21){b.setElevation(0);b.setStateListAnimator(null);}return b;}

    /* ---------- Fix Step-4 note duplication ---------- */
    private void fixStructuredNote(View root){
        View builder=findTag(root,"v440_note_builder");
        if(!(builder instanceof ViewGroup) || bound.contains(builder)) return;
        ArrayList<EditText> edits=new ArrayList<>();collectEdits(builder,edits);
        if(edits.size()<9) return;
        EditText custom=edits.get(edits.size()-1);
        String c=custom.getText().toString().trim();
        OfferStudioActivity.Draft d=draft();
        String full=d==null?"":(d.notes==null?"":d.notes.trim());
        if(isGeneratedNote(c) && (c.equals(full)||isGeneratedNote(full))){
            String extracted=extractCustom(full);
            if(!extracted.equals(c)) custom.setText(extracted);
        }
        bound.add(builder);
    }
    private boolean isGeneratedNote(String s){if(s==null)return false;String x=s.trim();return x.startsWith("Solar system capacity ")||x.startsWith("منظومة شمسية بقدرة ")||x.startsWith("سیستەمێکی خۆرەوی بە توانای ");}
    private String extractCustom(String s){
        if(s==null)return"";
        String[] markers={"\n\nCustom note: ","\n\nملاحظة مخصصة: ","\n\nتێبینی تایبەت: "};
        for(String m:markers){int i=s.indexOf(m);if(i>=0)return s.substring(i+m.length()).trim();}
        return "";
    }
    private void collectEdits(View v,ArrayList<EditText> out){if(v instanceof EditText)out.add((EditText)v);if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)collectEdits(g.getChildAt(i),out);}}

    /* ---------- Rebind rough dialogs ---------- */
    private void bindButtons(View v){
        if(v instanceof Button && !bound.contains(v)){
            Button b=(Button)v;String s=String.valueOf(b.getText()).trim();
            if(eq(s,"Browse catalog","تصفح الكتالوج","کاتەلۆگ ببینە")){
                bound.add(v);b.setOnClickListener(x->showCatalogPicker());
            }else if(eq(s,"Custom item","مادة مخصصة","ماددەی تایبەت")){
                bound.add(v);b.setOnClickListener(x->showItemDialog(null,-1));
            }else if(eq(s,"Manage organizer profiles","إدارة منظمي الكشف","بەڕێوەبردنی ڕێکخەران")){
                bound.add(v);b.setOnClickListener(x->showOrganizerManager());
            }else if(eq(s,"Choose organizer","اختيار منظم الكشف","ڕێکخەر هەڵبژێرە")){
                bound.add(v);b.setOnClickListener(x->showOrganizerChooser());
            }
        }
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)bindButtons(g.getChildAt(i));}
    }
    private boolean eq(String s,String en,String ar,String ku){return s.equals(en)||s.equals(ar)||s.equals(ku);}

    private void showCatalogPicker(){
        final Dialog d=new Dialog(this);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(16),dp(15),dp(16),dp(14));box.setBackground(bg(Color.WHITE,0,20));
        box.addView(text(tx("Choose material","اختيار المادة","ماددە هەڵبژێرە"),20,true,INK));
        EditText search=field(tx("Search item name…","ابحث باسم المادة…","بە ناوی ماددە بگەڕێ…"),"",false);LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,dp(46));slp.topMargin=dp(10);box.addView(search,slp);
        ListView list=new ListView(this);list.setDividerHeight(0);box.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        ArrayList<OfferStudioActivity.Product> all=catalog();ArrayList<OfferStudioActivity.Product> filtered=new ArrayList<>();ArrayList<String> labels=new ArrayList<>();ArrayAdapter<String> a=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,labels){@Override public View getView(int pos,View cv,ViewGroup p){TextView t=(TextView)super.getView(pos,cv,p);t.setTextSize(13);t.setTextColor(INK);t.setPadding(dp(10),dp(9),dp(10),dp(9));return t;}};list.setAdapter(a);
        Runnable refresh=()->{String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);filtered.clear();labels.clear();for(OfferStudioActivity.Product p:all){String name=p.name==null?"":p.name;if(q.isEmpty()||name.toLowerCase(Locale.ROOT).contains(q)){filtered.add(p);labels.add(name);}}a.notifyDataSetChanged();};
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int af){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int b,int c){refresh.run();}});
        list.setOnItemClickListener((p,v,pos,id)->{OfferStudioActivity.Product x=filtered.get(pos);d.dismiss();showItemDialog(new OfferStudioActivity.LineItem(x.category,x.name,"","1",""),-1);});
        Button close=btn(tx("Close","إغلاق","داخستن"),false);LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(-1,dp(40));clp.topMargin=dp(8);box.addView(close,clp);close.setOnClickListener(x->d.dismiss());refresh.run();
        d.setContentView(box);d.show();Window w=d.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.92f),(int)(getResources().getDisplayMetrics().heightPixels*.78f));}
    }

    private void showItemDialog(OfferStudioActivity.LineItem item,int editIndex){
        OfferStudioActivity.LineItem src=item==null?new OfferStudioActivity.LineItem("Custom","","","1",""):item;
        final Dialog d=new Dialog(this);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(17),dp(16),dp(17),dp(15));box.setBackground(bg(Color.WHITE,0,20));
        box.addView(text(editIndex>=0?tx("Edit material","تعديل المادة","دەستکاری ماددە"):tx("Add material","إضافة المادة","ماددە زیاد بکە"),20,true,INK));
        if(item!=null&&!src.name.isEmpty()){TextView n=text(src.name,14,true,NAVY);n.setPadding(dp(12),dp(10),dp(12),dp(10));n.setBackground(bg(BG,LINE,13));LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.topMargin=dp(10);box.addView(n,np);}
        EditText name=field(tx("Item name","اسم المادة","ناوی ماددە"),src.name,false);if(item!=null&&!src.name.isEmpty())name.setVisibility(View.GONE);EditText qty=field(tx("Quantity","الكمية","بڕ"),empty(src.qty)?"1":src.qty,true);EditText price=field(tx("Unit price (optional)","سعر الوحدة (اختياري)","نرخی یەکە (ئارەزوومەندانە)"),src.price,true);
        if(name.getVisibility()!=View.GONE){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(46));lp.topMargin=dp(10);box.addView(name,lp);}LinearLayout quantity=new LinearLayout(this);quantity.setOrientation(LinearLayout.HORIZONTAL);quantity.setGravity(Gravity.CENTER_VERTICAL);Button minus=btn("−",false),plus=btn("+",false);qty.setGravity(Gravity.CENTER);quantity.addView(minus,new LinearLayout.LayoutParams(dp(46),dp(42)));LinearLayout.LayoutParams qp=new LinearLayout.LayoutParams(0,dp(42),1);qp.leftMargin=dp(7);qp.rightMargin=dp(7);quantity.addView(qty,qp);quantity.addView(plus,new LinearLayout.LayoutParams(dp(46),dp(42)));LinearLayout.LayoutParams qrow=new LinearLayout.LayoutParams(-1,-2);qrow.topMargin=dp(10);box.addView(quantity,qrow);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(46));pp.topMargin=dp(10);box.addView(price,pp);
        minus.setOnClickListener(v->{int q=parseQty(qty.getText().toString());qty.setText(String.valueOf(Math.max(1,q-1)));});plus.setOnClickListener(v->{int q=parseQty(qty.getText().toString());qty.setText(String.valueOf(q+1));});
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);Button cancel=btn(tx("Cancel","إلغاء","هەڵوەشاندنەوە"),false);Button save=btn(editIndex>=0?tx("Save","حفظ","پاشەکەوت"):tx("Add material","إضافة المادة","ماددە زیاد بکە"),true);actions.addView(cancel,new LinearLayout.LayoutParams(0,dp(42),1));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(42),1);sp.leftMargin=dp(8);actions.addView(save,sp);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.topMargin=dp(14);box.addView(actions,ap);cancel.setOnClickListener(v->d.dismiss());save.setOnClickListener(v->{String nm=name.getVisibility()==View.GONE?src.name:name.getText().toString().trim();if(nm.isEmpty())nm=tx("Custom item","مادة مخصصة","ماددەی تایبەت");OfferStudioActivity.LineItem out=new OfferStudioActivity.LineItem(src.category,nm,"",empty(qty.getText().toString())?"1":qty.getText().toString().trim(),price.getText().toString().trim());OfferStudioActivity.Draft dr=draft();if(dr!=null){if(editIndex>=0&&editIndex<dr.items.size())dr.items.set(editIndex,out);else dr.items.add(out);}d.dismiss();setStep(2);showWizard();});
        d.setContentView(box);d.show();Window w=d.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.90f),WindowManager.LayoutParams.WRAP_CONTENT);}
    }

    private int parseQty(String s){try{return Math.max(1,(int)Math.round(Double.parseDouble(s)));}catch(Exception e){return 1;}}
    private boolean empty(String s){return s==null||s.trim().isEmpty();}

    /* ---------- Organizer dialogs ---------- */
    private ArrayList<String> organizers(){ArrayList<String> out=new ArrayList<>();try{org.json.JSONArray a=new org.json.JSONArray(settings().getString("organizers_json","[]"));for(int i=0;i<a.length();i++){String x=a.optString(i).trim();if(!x.isEmpty()&&!out.contains(x))out.add(x);}}catch(Exception ignored){}String def=settings().getString("organizer","").trim();if(!def.isEmpty()&&!out.contains(def))out.add(0,def);return out;}
    private void saveOrganizers(ArrayList<String> a){org.json.JSONArray j=new org.json.JSONArray();for(String x:a)j.put(x);settings().edit().putString("organizers_json",j.toString()).apply();}
    private void showOrganizerChooser(){
        ArrayList<String> list=organizers();if(list.isEmpty()){showOrganizerManager();return;}
        String[] names=list.toArray(new String[0]);new AlertDialog.Builder(this).setTitle(tx("Inspection organizer","منظم الكشف","ڕێکخەری پشکنین")).setItems(names,(di,w)->{OfferStudioActivity.Draft d=draft();if(d!=null)d.organizer=names[w];EditText e=findTaggedEdit(findViewById(android.R.id.content),"organizer");if(e!=null)e.setText(names[w]);}).setNeutralButton(tx("Manage","إدارة","بەڕێوەبردن"),(di,w)->showOrganizerManager()).show();
    }
    private void showOrganizerManager(){
        final Dialog d=new Dialog(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(16),dp(15),dp(16),dp(15));box.setBackground(bg(Color.WHITE,0,20));box.addView(text(tx("Organizer profiles","منظمو الكشف","پڕۆفایلی ڕێکخەران"),20,true,INK));LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);ScrollView sc=new ScrollView(this);sc.addView(list);box.addView(sc,new LinearLayout.LayoutParams(-1,0,1));Button add=btn(tx("+ Add organizer","+ إضافة منظم","+ ڕێکخەر زیاد بکە"),true);LinearLayout.LayoutParams alp=new LinearLayout.LayoutParams(-1,dp(42));alp.topMargin=dp(8);box.addView(add,alp);Button close=btn(tx("Close","إغلاق","داخستن"),false);LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(-1,dp(40));clp.topMargin=dp(7);box.addView(close,clp);Runnable[] refresh=new Runnable[1];refresh[0]=()->{list.removeAllViews();for(String name:organizers()){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(dp(8),dp(5),dp(8),dp(5));TextView n=text(name,13,true,INK);r.addView(n,new LinearLayout.LayoutParams(0,-2,1));Button def=btn(tx("Default","افتراضي","بنەڕەت"),false);Button del=btn(tx("Delete","حذف","سڕینەوە"),false);r.addView(def,new LinearLayout.LayoutParams(dp(82),dp(36)));r.addView(del,new LinearLayout.LayoutParams(dp(70),dp(36)));def.setOnClickListener(v->settings().edit().putString("organizer",name).apply());del.setOnClickListener(v->{ArrayList<String>x=organizers();x.remove(name);saveOrganizers(x);refresh[0].run();});list.addView(r,new LinearLayout.LayoutParams(-1,dp(46)));}};add.setOnClickListener(v->showAddOrganizer(refresh[0]));close.setOnClickListener(v->d.dismiss());refresh[0].run();d.setContentView(box);d.show();Window w=d.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.91f),(int)(getResources().getDisplayMetrics().heightPixels*.62f));}
    }
    private void showAddOrganizer(Runnable refresh){final Dialog d=new Dialog(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(16),dp(16),dp(16),dp(14));box.setBackground(bg(Color.WHITE,0,18));box.addView(text(tx("Add organizer","إضافة منظم","ڕێکخەر زیاد بکە"),19,true,INK));EditText e=field(tx("Organizer name","اسم المنظم","ناوی ڕێکخەر"),"",false);LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,dp(46));ep.topMargin=dp(10);box.addView(e,ep);LinearLayout a=new LinearLayout(this);a.setOrientation(LinearLayout.HORIZONTAL);Button c=btn(tx("Cancel","إلغاء","هەڵوەشاندنەوە"),false),s=btn(tx("Add","إضافة","زیادکردن"),true);a.addView(c,new LinearLayout.LayoutParams(0,dp(40),1));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(40),1);sp.leftMargin=dp(8);a.addView(s,sp);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.topMargin=dp(12);box.addView(a,ap);c.setOnClickListener(v->d.dismiss());s.setOnClickListener(v->{String n=e.getText().toString().trim();if(!n.isEmpty()){ArrayList<String>x=organizers();if(!x.contains(n))x.add(n);saveOrganizers(x);refresh.run();d.dismiss();}});d.setContentView(box);d.show();Window w=d.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.88f),WindowManager.LayoutParams.WRAP_CONTENT);}}

    /* ---------- Immediate language switching ---------- */
    private void bindLanguage(View v){
        if(v instanceof Spinner && !bound.contains(v)){
            Spinner s=(Spinner)v;if(s.getAdapter()!=null&&s.getAdapter().getCount()==3){String a=String.valueOf(s.getAdapter().getItem(0)),b=String.valueOf(s.getAdapter().getItem(1)),c=String.valueOf(s.getAdapter().getItem(2));if("English".equals(a)&&"العربية".equals(b)&&"کوردی".equals(c)){bound.add(v);s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){boolean first=true;public void onItemSelected(AdapterView<?>p,View view,int pos,long id){if(first){first=false;return;}String next=pos==1?"ar":pos==2?"ku":"en";if(!next.equals(lang())){settings().edit().putString("lang",next).apply();recreate();}}public void onNothingSelected(AdapterView<?>p){}});}}
        }
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)bindLanguage(g.getChildAt(i));}
    }

    /* ---------- reflection helpers ---------- */
    private OfferStudioActivity.Draft draft(){try{Field f=OfferStudioActivity.class.getDeclaredField("draft");f.setAccessible(true);return(OfferStudioActivity.Draft)f.get(this);}catch(Exception e){return null;}}
    @SuppressWarnings("unchecked") private ArrayList<OfferStudioActivity.Product> catalog(){try{Field f=OfferStudioActivity.class.getDeclaredField("catalog");f.setAccessible(true);return(ArrayList<OfferStudioActivity.Product>)f.get(this);}catch(Exception e){return new ArrayList<>();}}
    private void setStep(int n){try{Field f=OfferStudioActivity.class.getDeclaredField("wizardStep");f.setAccessible(true);f.setInt(this,n);}catch(Exception ignored){}}
    private void showWizard(){try{Method m=OfferStudioActivity.class.getDeclaredMethod("showWizard");m.setAccessible(true);m.invoke(this);}catch(Exception ignored){}}
    private View findTag(View v,String tag){if(v==null)return null;if(tag.equals(String.valueOf(v.getTag())))return v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View r=findTag(g.getChildAt(i),tag);if(r!=null)return r;}}return null;}
    private EditText findTaggedEdit(View v,String tag){if(v instanceof EditText&&tag.equals(String.valueOf(v.getTag())))return(EditText)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){EditText r=findTaggedEdit(g.getChildAt(i),tag);if(r!=null)return r;}}return null;}
}
