package com.infinitygreenpower.solarform;

import android.app.*;
import android.os.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;

/** v4.1.1 UI polish: large Edit / Replace / Remove actions on item cards. */
public class OfferStudioV412Activity extends OfferStudioV411Activity {
    private boolean actionPass=false;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        View root=findViewById(android.R.id.content);
        if(root!=null){
            root.getViewTreeObserver().addOnGlobalLayoutListener(()->{
                if(actionPass)return;actionPass=true;
                try{enhanceItemActions(root);}finally{actionPass=false;}
            });
            root.post(()->enhanceItemActions(root));
        }
    }

    private String lang(){return getSharedPreferences("offer_studio_settings",MODE_PRIVATE).getString("lang","en");}
    private String tx(String en,String ar,String ku){return "ar".equals(lang())?ar:("ku".equals(lang())?ku:en);}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int fill,int stroke){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(14));if(stroke!=0)g.setStroke(dp(1),stroke);return g;}

    private void enhanceItemActions(View root){
        ArrayList<ViewGroup> rows=new ArrayList<>();
        collectActionRows(root,rows);
        for(int i=0;i<rows.size();i++)styleActionRow(rows.get(i),i);
    }

    private void collectActionRows(View v,ArrayList<ViewGroup> out){
        if(v instanceof ViewGroup){
            ViewGroup g=(ViewGroup)v;boolean edit=false,remove=false;
            for(int i=0;i<g.getChildCount();i++){
                View c=g.getChildAt(i);if(c instanceof TextView){String s=String.valueOf(((TextView)c).getText()).trim();if("✎".equals(s))edit=true;if("×".equals(s))remove=true;}
            }
            if(edit&&remove)out.add(g);
            for(int i=0;i<g.getChildCount();i++)collectActionRows(g.getChildAt(i),out);
        }
    }

    private void styleActionRow(ViewGroup row,int index){
        TextView edit=null,remove=null,replace=null;
        for(int i=0;i<row.getChildCount();i++){
            View c=row.getChildAt(i);
            if(c instanceof TextView){String s=String.valueOf(((TextView)c).getText()).trim();if("✎".equals(s)||s.startsWith("✎ "))edit=(TextView)c;else if("×".equals(s)||s.startsWith("× "))remove=(TextView)c;else if("v412_replace".equals(c.getTag()))replace=(TextView)c;}
        }
        if(edit!=null){
            edit.setText("✎ "+tx("Edit","تعديل","دەستکاری"));styleButton(edit,Color.rgb(18,50,79),Color.rgb(238,244,250));
        }
        if(remove!=null){
            remove.setText("× "+tx("Remove","حذف","سڕینەوە"));styleButton(remove,Color.rgb(184,61,61),Color.rgb(255,240,240));
        }
        if(replace==null){
            TextView r=new TextView(this);r.setTag("v412_replace");r.setText("⇄ "+tx("Replace","استبدال","گۆڕین"));r.setTextColor(Color.rgb(20,153,112));r.setTextSize(13);r.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);r.setGravity(Gravity.CENTER);r.setBackground(bg(Color.rgb(238,247,244),Color.rgb(196,229,216)));r.setPadding(dp(12),dp(9),dp(12),dp(9));r.setMinHeight(dp(44));r.setMinWidth(dp(86));
            final int itemIndex=index;r.setOnClickListener(v->showReplaceDialog(itemIndex));
            row.addView(r);
        }
    }

    private void styleButton(TextView v,int fg,int fill){
        v.setTextColor(fg);v.setTextSize(13);v.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);v.setGravity(Gravity.CENTER);v.setBackground(bg(fill,0));v.setPadding(dp(12),dp(9),dp(12),dp(9));v.setMinHeight(dp(44));v.setMinWidth(dp(78));
        ViewGroup.LayoutParams old=v.getLayoutParams();if(old instanceof ViewGroup.MarginLayoutParams){ViewGroup.MarginLayoutParams lp=(ViewGroup.MarginLayoutParams)old;lp.leftMargin=dp(4);lp.rightMargin=dp(4);v.setLayoutParams(lp);}
    }

    @SuppressWarnings("unchecked") private ArrayList<OfferStudioActivity.Product> catalog(){
        try{Field f=OfferStudioActivity.class.getDeclaredField("catalog");f.setAccessible(true);return (ArrayList<OfferStudioActivity.Product>)f.get(this);}catch(Exception e){return new ArrayList<>();}
    }
    private OfferStudioActivity.Draft draft(){try{Field f=OfferStudioActivity.class.getDeclaredField("draft");f.setAccessible(true);return (OfferStudioActivity.Draft)f.get(this);}catch(Exception e){return null;}}
    private void setWizardStep(int n){try{Field f=OfferStudioActivity.class.getDeclaredField("wizardStep");f.setAccessible(true);f.setInt(this,n);}catch(Exception ignored){}}
    private void showWizard(){try{Method m=OfferStudioActivity.class.getDeclaredMethod("showWizard");m.setAccessible(true);m.invoke(this);}catch(Exception ignored){}}

    private void showReplaceDialog(final int itemIndex){
        final OfferStudioActivity.Draft d=draft();if(d==null||itemIndex<0||itemIndex>=d.items.size())return;
        final ArrayList<OfferStudioActivity.Product> all=catalog();
        final ArrayList<OfferStudioActivity.Product> filtered=new ArrayList<>(all);
        final ArrayList<String> labels=new ArrayList<>();for(OfferStudioActivity.Product p:filtered)labels.add(p.name);

        final Dialog dialog=new Dialog(this);dialog.setTitle(tx("Replace item","استبدال المادة","گۆڕینی ماددە"));
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(14),dp(10),dp(14),dp(14));
        EditText search=new EditText(this);search.setHint(tx("Search item name","ابحث باسم المادة","بە ناوی ماددە بگەڕێ"));search.setSingleLine(true);search.setPadding(dp(14),0,dp(14),0);box.addView(search,new LinearLayout.LayoutParams(-1,dp(50)));
        ListView list=new ListView(this);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,labels);list.setAdapter(adapter);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(500));lp.topMargin=dp(8);box.addView(list,lp);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int b,int c){String q=s.toString().trim().toLowerCase(Locale.ROOT);filtered.clear();labels.clear();for(OfferStudioActivity.Product p:all){String hay=(p.name+" "+p.category).toLowerCase(Locale.ROOT);if(q.isEmpty()||hay.contains(q)){filtered.add(p);labels.add(p.name);}}adapter.notifyDataSetChanged();}});
        list.setOnItemClickListener((parent,view,pos,id)->{
            OfferStudioActivity.Product p=filtered.get(pos);OfferStudioActivity.LineItem old=d.items.get(itemIndex);String qty=old.qty,price=old.price;d.items.set(itemIndex,new OfferStudioActivity.LineItem(p.category,p.name,"",qty,price));dialog.dismiss();setWizardStep(2);showWizard();
        });
        dialog.setContentView(box);dialog.show();Window w=dialog.getWindow();if(w!=null)w.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.94),WindowManager.LayoutParams.WRAP_CONTENT);
    }
}
