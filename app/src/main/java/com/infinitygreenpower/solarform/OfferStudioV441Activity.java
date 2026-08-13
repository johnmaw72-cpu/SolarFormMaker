package com.infinitygreenpower.solarform;

import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** v4.4.1: reliable structured-note insertion without a continuous layout observer. */
public class OfferStudioV441Activity extends OfferStudioV440Activity {
    private boolean noteInsertBusy=false;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        View root=findViewById(android.R.id.content);
        if(root!=null)root.postDelayed(()->ensureStructuredNote(root),250);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus){View root=findViewById(android.R.id.content);if(root!=null)root.postDelayed(()->ensureStructuredNote(root),120);}
    }

    private String lang(){return getSharedPreferences("offer_studio_settings",MODE_PRIVATE).getString("lang","en");}
    private boolean rtl(){return !"en".equals(lang());}
    private String tx(String en,String ar,String ku){return "ar".equals(lang())?ar:("ku".equals(lang())?ku:en);}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int fill,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));if(stroke!=0)g.setStroke(dp(1),stroke);return g;}

    private OfferStudioActivity.Draft draft(){try{Field f=OfferStudioActivity.class.getDeclaredField("draft");f.setAccessible(true);return(OfferStudioActivity.Draft)f.get(this);}catch(Exception e){return null;}}
    private int step(){try{Field f=OfferStudioActivity.class.getDeclaredField("wizardStep");f.setAccessible(true);return f.getInt(this);}catch(Exception e){return-1;}}

    private EditText findTaggedEdit(View v,String tag){
        if(v instanceof EditText&&tag.equals(String.valueOf(v.getTag())))return(EditText)v;
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){EditText r=findTaggedEdit(g.getChildAt(i),tag);if(r!=null)return r;}}
        return null;
    }
    private boolean hasTag(View v,String tag){if(tag.equals(String.valueOf(v.getTag())))return true;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(hasTag(g.getChildAt(i),tag))return true;}return false;}

    private void ensureStructuredNote(View root){
        if(noteInsertBusy||step()!=3||hasTag(root,"v441_fixed_note"))return;
        EditText original=findTaggedEdit(root,"notes");if(original==null||original.getParent()==null)return;
        noteInsertBusy=true;
        try{
            ViewGroup parent=(ViewGroup)original.getParent();int at=parent.indexOfChild(original);
            original.setVisibility(View.GONE);
            if(at>0&&parent.getChildAt(at-1)instanceof TextView){TextView prev=(TextView)parent.getChildAt(at-1);String s=String.valueOf(prev.getText()).trim();if(s.equals("Notes")||s.equals("ملاحظات")||s.equals("تێبینی"))prev.setVisibility(View.GONE);}
            OfferStudioActivity.Draft d=draft();NoteData nd=parseNote(d==null?"":d.notes,d);
            LinearLayout card=new LinearLayout(this);card.setTag("v441_fixed_note");card.setOrientation(LinearLayout.VERTICAL);card.setLayoutDirection(rtl()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);card.setPadding(dp(14),dp(13),dp(14),dp(14));card.setBackground(bg(Color.WHITE,Color.rgb(218,225,232),18));
            card.addView(text(tx("Fixed load-capacity note","ملاحظة ثابتة لقدرة التحمل","تێبینی جێگیری توانای بار"),17,true,Color.rgb(18,32,45)));
            TextView help=text(tx("Only fill or change the values below. The final note is generated automatically.","غيّر القيم المطلوبة فقط، وسيتم إنشاء نص الملاحظة تلقائياً.","تەنها نرخە پێویستەکان بگۆڕە؛ دەقی کۆتایی خۆکار دروست دەبێت."),12,false,Color.rgb(104,119,132));help.setPadding(0,dp(3),0,dp(9));card.addView(help);

            EditText kw=field(card,tx("System capacity (kW)","قدرة المنظومة (كيلو واط)","توانای سیستەم (kW)"),nd.kw,true);
            EditText type=field(card,tx("System type","نوع المنظومة","جۆری سیستەم"),nd.type,false);
            LinearLayout day=two();EditText dayFrom=compact(day,tx("Day from (A)","بالنهار من (أمبير)","ڕۆژ لە (A)"),nd.dayFrom,true);EditText dayTo=compact(day,tx("Day to (A)","إلى (أمبير)","بۆ (A)"),nd.dayTo,true);card.addView(day);
            LinearLayout night=two();EditText nightA=compact(night,tx("Night amps","أمبير بالليل","ئەمپێری شەو"),nd.nightA,true);EditText nightH=compact(night,tx("Night hours","عدد الساعات","کاتژمێری شەو"),nd.nightHours,true);card.addView(night);
            LinearLayout emergency=two();EditText emA=compact(emergency,tx("Emergency amps","أمبير الضرورة","ئەمپێری پێویست"),nd.emA,true);EditText emD=compact(emergency,tx("Emergency duration","مدة الضرورة","ماوەی پێویست"),nd.emDuration,false);card.addView(emergency);
            EditText custom=multi(card,tx("Custom note (optional)","ملاحظة مخصصة (اختياري)","تێبینی تایبەت (ئارەزوومەندانە)"),nd.custom);

            TextView generatedLabel=text(tx("Generated note","النص الناتج","دەقی دروستکراو"),12,true,Color.rgb(20,153,112));generatedLabel.setPadding(0,dp(10),0,dp(4));card.addView(generatedLabel);
            TextView preview=text("",13,false,Color.rgb(47,65,82));preview.setPadding(dp(11),dp(10),dp(11),dp(10));preview.setBackground(bg(Color.rgb(247,249,251),0,13));card.addView(preview,new LinearLayout.LayoutParams(-1,-2));

            Runnable update=()->{String note=buildNote(kw,type,dayFrom,dayTo,nightA,nightH,emA,emD,custom);original.setText(note);preview.setText(note);if(d!=null)d.notes=note;};
            TextWatcher watcher=new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int before,int count){update.run();}};
            for(EditText e:new EditText[]{kw,type,dayFrom,dayTo,nightA,nightH,emA,emD,custom})e.addTextChangedListener(watcher);
            update.run();
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(8);cp.bottomMargin=dp(14);parent.addView(card,Math.max(0,at),cp);
        }finally{noteInsertBusy=false;}
    }

    private LinearLayout two(){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.setLayoutDirection(rtl()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);return r;}
    private TextView text(String s,int size,boolean bold,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT_BOLD);t.setGravity(rtl()?Gravity.RIGHT:Gravity.LEFT);return t;}
    private EditText field(LinearLayout p,String label,String value,boolean numeric){TextView l=text(label,12,true,Color.rgb(35,52,68));l.setPadding(0,dp(6),0,dp(4));p.addView(l);EditText e=baseField(value,numeric,false);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(48));lp.bottomMargin=dp(5);p.addView(e,lp);return e;}
    private EditText compact(LinearLayout row,String label,String value,boolean numeric){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);TextView l=text(label,11,true,Color.rgb(35,52,68));l.setPadding(0,dp(6),0,dp(3));box.addView(l);EditText e=baseField(value,numeric,false);box.addView(e,new LinearLayout.LayoutParams(-1,dp(46)));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.leftMargin=dp(4);lp.rightMargin=dp(4);row.addView(box,lp);return e;}
    private EditText multi(LinearLayout p,String label,String value){TextView l=text(label,12,true,Color.rgb(35,52,68));l.setPadding(0,dp(8),0,dp(4));p.addView(l);EditText e=baseField(value,false,true);p.addView(e,new LinearLayout.LayoutParams(-1,dp(84)));return e;}
    private EditText baseField(String value,boolean numeric,boolean multi){EditText e=new EditText(this);e.setText(value==null?"":value);e.setTextSize(14);e.setTextColor(Color.rgb(18,32,45));e.setHintTextColor(Color.rgb(145,158,170));e.setPadding(dp(11),dp(8),dp(11),dp(8));e.setBackground(bg(Color.rgb(249,250,252),Color.rgb(220,226,232),13));e.setGravity((multi?Gravity.TOP:Gravity.CENTER_VERTICAL)|(rtl()?Gravity.RIGHT:Gravity.LEFT));e.setTextDirection(rtl()?View.TEXT_DIRECTION_RTL:View.TEXT_DIRECTION_LTR);if(numeric)e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);else if(multi){e.setSingleLine(false);e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);}else e.setSingleLine(true);return e;}

    private static class NoteData{String kw="",type="",dayFrom="",dayTo="",nightA="",nightHours="",emA="",emDuration="",custom="";}
    private String localizedSystem(String s){if(s==null)return"";if("ar".equals(lang())){if(s.equalsIgnoreCase("Hybrid"))return"هجين";if(s.equalsIgnoreCase("On-Grid"))return"أون كريد";if(s.equalsIgnoreCase("Off-Grid"))return"أوف كريد";}if("ku".equals(lang())){if(s.equalsIgnoreCase("Hybrid"))return"هایبرید";if(s.equalsIgnoreCase("On-Grid"))return"ئۆن‌گرید";if(s.equalsIgnoreCase("Off-Grid"))return"ئۆف‌گرید";}return s;}
    private String cleanCapacity(String s){if(s==null)return"";return s.replaceAll("(?i)\\s*kilo\\s*watt|\\s*kilowatt|\\s*kw|\\s*كيلو\\s*واط|\\s*کیلۆ\\s*وات","").trim();}
    private NoteData parseNote(String note,OfferStudioActivity.Draft d){NoteData n=new NoteData();if(d!=null){n.kw=cleanCapacity(d.capacity);n.type=localizedSystem(d.system);}if(note==null||note.trim().isEmpty())return n;try{Pattern p=Pattern.compile("منظومة شمسية بقدرة (.*?) كيلو واط نوع (.*?) يتحمل كالآتي:\\n- بالنهار من (.*?) الى (.*?) امبير\\n- بالليل (.*?) امبير لمدة (.*?) ساعات\\n- وفي حالات الضرورة يتحمل (.*?) امبير لمدة (.*?)(?:\\n\\nملاحظة مخصصة: (.*))?$",Pattern.DOTALL);Matcher m=p.matcher(note.trim());if(m.matches()){n.kw=m.group(1);n.type=m.group(2);n.dayFrom=m.group(3);n.dayTo=m.group(4);n.nightA=m.group(5);n.nightHours=m.group(6);n.emA=m.group(7);n.emDuration=m.group(8);n.custom=m.group(9)==null?"":m.group(9);return n;}}catch(Exception ignored){}if(!note.contains("منظومة شمسية بقدرة")&&!note.contains("Solar system capacity"))n.custom=note;return n;}
    private String value(EditText e){String s=e.getText().toString().trim();return s.isEmpty()?"___":s;}
    private String buildNote(EditText kw,EditText type,EditText df,EditText dt,EditText na,EditText nh,EditText ea,EditText ed,EditText custom){String extra=custom.getText().toString().trim();if("ar".equals(lang())){String s="منظومة شمسية بقدرة "+value(kw)+" كيلو واط نوع "+value(type)+" يتحمل كالآتي:\n- بالنهار من "+value(df)+" الى "+value(dt)+" امبير\n- بالليل "+value(na)+" امبير لمدة "+value(nh)+" ساعات\n- وفي حالات الضرورة يتحمل "+value(ea)+" امبير لمدة "+value(ed);if(!extra.isEmpty())s+="\n\nملاحظة مخصصة: "+extra;return s;}if("ku".equals(lang())){String s="سیستەمی خۆرەوی بە توانای "+value(kw)+" kW جۆری "+value(type)+" ئەمانە هەڵدەگرێت:\n- بە ڕۆژ لە "+value(df)+" بۆ "+value(dt)+" ئەمپێر\n- بە شەو "+value(na)+" ئەمپێر بۆ "+value(nh)+" کاتژمێر\n- لە کاتی پێویستدا "+value(ea)+" ئەمپێر بۆ "+value(ed);if(!extra.isEmpty())s+="\n\nتێبینی تایبەت: "+extra;return s;}String s="Solar system capacity "+value(kw)+" kW, type "+value(type)+", supports:\n- Daytime from "+value(df)+" to "+value(dt)+" A\n- Night "+value(na)+" A for "+value(nh)+" hours\n- Emergency "+value(ea)+" A for "+value(ed);if(!extra.isEmpty())s+="\n\nCustom note: "+extra;return s;}
}
