package com.infinitygreenpower.solarform;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * IGP Organizer Form v5.0.0
 * Flat web-button design inspired by the supplied turquoise UI reference.
 * Applies a consistent action system throughout the organizer workflow.
 */
public class OfferStudioV500Activity extends OfferStudioV490Activity {
    private final Handler ui500 = new Handler(Looper.getMainLooper());
    private boolean running500;

    private final Runnable styler500 = new Runnable() {
        @Override public void run() {
            if (!running500) return;
            try { styleTree(findViewById(android.R.id.content)); }
            catch (Exception ignored) { }
            ui500.postDelayed(this, 320);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        running500 = true;
        ui500.postDelayed(styler500, 420);
    }

    @Override protected void onResume() {
        super.onResume();
        if (!running500) {
            running500 = true;
            ui500.postDelayed(styler500, 220);
        }
    }

    @Override protected void onPause() {
        running500 = false;
        ui500.removeCallbacks(styler500);
        super.onPause();
    }

    @Override protected void onDestroy() {
        running500 = false;
        ui500.removeCallbacks(styler500);
        super.onDestroy();
    }

    private void styleTree(View v) {
        if (v == null) return;
        if (v instanceof Button) styleButton((Button) v);
        else if (v instanceof TextView) styleClickableText((TextView) v);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) styleTree(g.getChildAt(i));
        }
    }

    private void styleButton(Button b) {
        String s = String.valueOf(b.getText()).trim();
        if (s.isEmpty()) return;

        // Main flow actions.
        if (m(s,"Continue","متابعة","بەردەوام بە") ||
            m(s,"Preview offer","معاينة العرض","پێشبینینی پێشنیار") ||
            m(s,"Save settings","حفظ الإعدادات","ڕێکخستنەکان پاشەکەوت بکە")) {
            apply(b,"main",Kind.TEAL_SOLID,54,17);
            return;
        }
        if (m(s,"Back","رجوع","گەڕانەوە")) {
            apply(b,"back",Kind.OUTLINE_DARK,54,17);
            return;
        }

        // Item selection and editing.
        if (m(s,"Browse catalog","اختيار من الكتالوج","کاتەلۆگ ببینە")) {
            apply(b,"browse",Kind.NAVY_SOLID,50,16);
            return;
        }
        if (m(s,"Custom item","مادة مخصصة","ماددەی تایبەت")) {
            apply(b,"custom",Kind.TEAL_OUTLINE,50,16);
            return;
        }
        if (m(s,"Edit","تعديل","دەستکاری")) {
            apply(b,"edit",Kind.SOFT_BLUE,42,14);
            return;
        }
        if (m(s,"Replace","استبدال","گۆڕین") || m(s,"Duplicate","نسخ","دووبارە")) {
            apply(b,"replace",Kind.TEAL_OUTLINE,42,14);
            return;
        }
        if (m(s,"Delete","حذف","سڕینەوە") || m(s,"Remove","إزالة","سڕینەوە")) {
            apply(b,"delete",Kind.DANGER_OUTLINE,42,14);
            return;
        }

        // Organizer / draft actions.
        if (m(s,"Choose organizer","اختيار منظم الكشف","ڕێکخەر هەڵبژێرە") ||
            m(s,"Manage organizer profiles","إدارة منظمي الكشف","بەڕێوەبردنی ڕێکخەران")) {
            apply(b,"organizer",Kind.TEAL_OUTLINE,48,16);
            return;
        }
        if (m(s,"Save draft","حفظ كمسودة","وەک ڕەشنووس پاشەکەوت بکە")) {
            apply(b,"draft",Kind.SOFT_BLUE,46,16);
            return;
        }

        // PDF / share actions.
        if (m(s,"Share PDF","مشاركة PDF","PDF هاوبەش بکە") ||
            m(s,"Save PDF","حفظ PDF","PDF پاشەکەوت بکە") ||
            m(s,"WhatsApp PDF","PDF عبر واتساب","PDF بە واتسئاپ")) {
            apply(b,"pdf",Kind.TEAL_SOLID,46,16);
            return;
        }
        if (m(s,"Share PDF + Data","مشاركة PDF + البيانات","PDF + داتا هاوبەش بکە")) {
            apply(b,"pdfdata",Kind.TEAL_OUTLINE,46,16);
            return;
        }

        // Settings utility actions.
        if (m(s,"Choose logo","اختيار الشعار","لۆگۆ هەڵبژێرە")) {
            apply(b,"logo",Kind.TEAL_SOLID,44,15);
            return;
        }
        if (m(s,"Clear recent items","مسح المواد الأخيرة","دوایین ماددەکان پاک بکەرەوە")) {
            apply(b,"recent",Kind.OUTLINE_DARK,46,15);
            return;
        }
        if (m(s,"Clear favorites","مسح المفضلة","دڵخوازەکان پاک بکەرەوە")) {
            apply(b,"fav",Kind.DANGER_OUTLINE,46,15);
            return;
        }

        // Saved forms preview.
        if (m(s,"Preview","معاينة","پێشبینین")) {
            apply(b,"savedpreview",Kind.TEAL_SOLID,42,14);
        }
    }

    /** Some item-card actions in older layers are clickable TextViews rather than Button. */
    private void styleClickableText(TextView t) {
        if (t instanceof Button || !t.isClickable()) return;
        String s=String.valueOf(t.getText()).trim();
        Kind k=null; String id="";
        if (m(s,"Edit","تعديل","دەستکاری")) { k=Kind.SOFT_BLUE; id="tv_edit"; }
        else if (m(s,"Replace","استبدال","گۆڕین")) { k=Kind.TEAL_OUTLINE; id="tv_replace"; }
        else if (m(s,"Remove","حذف","سڕینەوە") || m(s,"Delete","حذف","سڕینەوە")) { k=Kind.DANGER_OUTLINE; id="tv_delete"; }
        if (k==null) return;
        String tag="v500_"+id;
        if (tag.equals(String.valueOf(t.getTag()))) return;
        t.setTag(tag);
        t.setTextSize(12);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(11),0,dp(11),0);
        Palette p=palette(k);
        t.setTextColor(p.fg);
        t.setBackground(stateBg(p.normal,p.pressed,p.stroke,15));
        ViewGroup.LayoutParams raw=t.getLayoutParams();
        if(raw instanceof LinearLayout.LayoutParams){
            LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)raw;
            lp.height=dp(40);
            lp.leftMargin=dp(4); lp.rightMargin=dp(4);
            t.setLayoutParams(lp);
        }
    }

    private enum Kind { TEAL_SOLID, TEAL_OUTLINE, NAVY_SOLID, SOFT_BLUE, OUTLINE_DARK, DANGER_OUTLINE }
    private static class Palette { int normal,pressed,stroke,fg; Palette(int n,int p,int s,int f){normal=n;pressed=p;stroke=s;fg=f;} }

    private Palette palette(Kind kind) {
        switch(kind){
            case TEAL_SOLID:
                return new Palette(Color.rgb(37,205,174),Color.rgb(27,177,150),Color.rgb(37,205,174),Color.WHITE);
            case TEAL_OUTLINE:
                return new Palette(Color.WHITE,Color.rgb(233,251,247),Color.rgb(37,205,174),Color.rgb(24,164,137));
            case NAVY_SOLID:
                return new Palette(Color.rgb(20,58,89),Color.rgb(14,45,70),Color.rgb(20,58,89),Color.WHITE);
            case SOFT_BLUE:
                return new Palette(Color.rgb(241,247,251),Color.rgb(226,239,247),Color.rgb(201,219,232),Color.rgb(20,58,89));
            case DANGER_OUTLINE:
                return new Palette(Color.WHITE,Color.rgb(255,240,240),Color.rgb(236,175,175),Color.rgb(183,55,55));
            case OUTLINE_DARK:
            default:
                return new Palette(Color.WHITE,Color.rgb(244,248,250),Color.rgb(168,187,199),Color.rgb(38,66,83));
        }
    }

    private void apply(Button b,String id,Kind kind,int height,int radius){
        String tag="v500_"+id;
        if(tag.equals(String.valueOf(b.getTag()))) return;
        b.setTag(tag);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setGravity(Gravity.CENTER);
        b.setMinWidth(0); b.setMinimumWidth(0); b.setMinHeight(0); b.setMinimumHeight(0);
        b.setPadding(dp(13),0,dp(13),0);
        if(android.os.Build.VERSION.SDK_INT>=21){b.setStateListAnimator(null);b.setElevation(0);}
        Palette p=palette(kind);
        b.setTextColor(p.fg);
        b.setBackground(stateBg(p.normal,p.pressed,p.stroke,radius));
        ViewGroup.LayoutParams raw=b.getLayoutParams();
        if(raw instanceof LinearLayout.LayoutParams){
            LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)raw;
            lp.height=dp(height);
            lp.leftMargin=dp(4); lp.rightMargin=dp(4);
            lp.topMargin=Math.max(lp.topMargin,dp(4));
            lp.bottomMargin=Math.max(lp.bottomMargin,dp(4));
            b.setLayoutParams(lp);
        }
    }

    private StateListDrawable stateBg(int normal,int pressed,int stroke,int radius){
        StateListDrawable s=new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed},rounded(pressed,stroke,radius));
        s.addState(new int[]{},rounded(normal,stroke,radius));
        return s;
    }
    private GradientDrawable rounded(int fill,int stroke,int radius){
        GradientDrawable g=new GradientDrawable();
        g.setColor(fill); g.setCornerRadius(dp(radius)); g.setStroke(dp(1),stroke); return g;
    }
    private boolean m(String actual,String en,String ar,String ku){return actual.equals(en)||actual.equals(ar)||actual.equals(ku);}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
