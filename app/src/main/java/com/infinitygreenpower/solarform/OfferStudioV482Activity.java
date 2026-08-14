package com.infinitygreenpower.solarform;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Field;

/**
 * v4.8.2 UI consistency pass.
 * Keeps the preferred v4.8 branch and v4.8.1 stability fixes.
 * Makes Saved Forms and item action button styling permanent at layout time,
 * rather than relying on v4.8.0's delayed styling loop.
 */
public class OfferStudioV482Activity extends OfferStudioV481Activity {
    private View root482;
    private ViewTreeObserver.OnGlobalLayoutListener layout482;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        stopOldSavedStyler();
        root482 = findViewById(android.R.id.content);
        layout482 = () -> {
            try { styleStableActions(root482); } catch (Exception ignored) { }
        };
        if (root482 != null) {
            root482.getViewTreeObserver().addOnGlobalLayoutListener(layout482);
            root482.post(() -> styleStableActions(root482));
        }
    }

    @Override protected void onResume() {
        super.onResume();
        stopOldSavedStyler();
        if (root482 != null) root482.post(() -> styleStableActions(root482));
    }

    @Override protected void onDestroy() {
        if (root482 != null && layout482 != null) {
            try {
                ViewTreeObserver o = root482.getViewTreeObserver();
                if (o.isAlive()) o.removeOnGlobalLayoutListener(layout482);
            } catch (Exception ignored) { }
        }
        super.onDestroy();
    }

    /** Stop v4.8.0's delayed 320ms saved-button restyler so two systems never fight. */
    private void stopOldSavedStyler() {
        try {
            Field running = OfferStudioV480Activity.class.getDeclaredField("running480");
            running.setAccessible(true);
            running.setBoolean(this, false);

            Field hf = OfferStudioV480Activity.class.getDeclaredField("ui480");
            hf.setAccessible(true);
            Handler h = (Handler) hf.get(this);

            Field rf = OfferStudioV480Activity.class.getDeclaredField("savedStyler");
            rf.setAccessible(true);
            Runnable r = (Runnable) rf.get(this);
            if (h != null && r != null) h.removeCallbacks(r);
        } catch (Exception ignored) { }
    }

    private void styleStableActions(View v) {
        if (v == null) return;
        boolean savedScreen = containsExact(v, "Saved Forms")
                || containsExact(v, "الاستمارات المحفوظة")
                || containsExact(v, "فۆرمە پاشەکەوتکراوەکان");
        walkAndStyle(v, savedScreen);
    }

    private void walkAndStyle(View v, boolean savedScreen) {
        if (v instanceof Button) {
            Button b = (Button) v;
            String s = String.valueOf(b.getText()).trim();
            if (savedScreen) {
                if (eq(s,"Preview","معاينة","پێشبینین")) savedStyle(b,"preview");
                else if (eq(s,"Edit","تعديل","دەستکاری")) savedStyle(b,"edit");
                else if (eq(s,"Duplicate","نسخ","دووبارە")) savedStyle(b,"duplicate");
                else if (eq(s,"Delete","حذف","سڕینەوە")) savedStyle(b,"delete");
            }
            if (eq(s,"Replace","استبدال","گۆڕین")) itemStyle(b,"replace");
            else if (eq(s,"Remove","إزالة","لابردن")) itemStyle(b,"remove");
            else if (eq(s,"Edit","تعديل","دەستکاری") && isItemActionRow(b)) itemStyle(b,"edit");
        }
        if (v instanceof ViewGroup) {
            ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++) walkAndStyle(g.getChildAt(i),savedScreen);
        }
    }

    private boolean isItemActionRow(Button b) {
        if (!(b.getParent() instanceof ViewGroup)) return false;
        ViewGroup g=(ViewGroup)b.getParent();
        for(int i=0;i<g.getChildCount();i++) {
            View x=g.getChildAt(i);
            if (!(x instanceof Button)) continue;
            String s=String.valueOf(((Button)x).getText()).trim();
            if (eq(s,"Replace","استبدال","گۆڕین") || eq(s,"Remove","إزالة","لابردن")) return true;
        }
        return false;
    }

    private void savedStyle(Button b,String kind) {
        int fg,fill,stroke;
        if ("duplicate".equals(kind)) {
            fg=Color.rgb(20,153,112); fill=Color.rgb(238,248,244); stroke=Color.rgb(197,229,216);
        } else if ("delete".equals(kind)) {
            fg=Color.rgb(184,61,61); fill=Color.rgb(255,240,240); stroke=Color.rgb(245,208,208);
        } else if ("preview".equals(kind)) {
            fg=Color.rgb(18,50,79); fill=Color.rgb(235,243,250); stroke=Color.rgb(196,216,232);
        } else {
            fg=Color.rgb(18,50,79); fill=Color.rgb(241,246,250); stroke=Color.rgb(209,222,233);
        }
        applyButton(b,fg,fill,stroke,13,40,12);
    }

    private void itemStyle(Button b,String kind) {
        if ("remove".equals(kind)) {
            applyButton(b,Color.rgb(184,61,61),Color.rgb(255,241,241),Color.rgb(245,211,211),12,38,11);
        } else if ("replace".equals(kind)) {
            applyButton(b,Color.rgb(20,153,112),Color.rgb(238,248,244),Color.rgb(201,230,218),12,38,11);
        } else {
            applyButton(b,Color.rgb(18,50,79),Color.rgb(239,245,250),Color.rgb(207,221,232),12,38,11);
        }
    }

    private void applyButton(Button b,int fg,int fill,int stroke,int radius,int height,int textSize) {
        b.setAllCaps(false);
        b.setTextSize(textSize);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(fg);
        b.setMinWidth(0); b.setMinimumWidth(0); b.setMinimumHeight(0);
        b.setPadding(dp482(5),0,dp482(5),0);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            b.setElevation(0);
            b.setStateListAnimator(null);
        }
        GradientDrawable d=new GradientDrawable();
        d.setColor(fill); d.setCornerRadius(dp482(radius)); d.setStroke(dp482(1),stroke);
        b.setBackground(d);
        ViewGroup.LayoutParams raw=b.getLayoutParams();
        if(raw instanceof LinearLayout.LayoutParams){
            LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)raw;
            lp.height=dp482(height); lp.leftMargin=dp482(3); lp.rightMargin=dp482(3);
            lp.topMargin=0; lp.bottomMargin=0; b.setLayoutParams(lp);
        }
    }

    private boolean containsExact(View v,String wanted){
        if(v instanceof TextView && wanted.equals(String.valueOf(((TextView)v).getText()).trim())) return true;
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(containsExact(g.getChildAt(i),wanted))return true;}
        return false;
    }
    private boolean eq(String s,String en,String ar,String ku){return s.equals(en)||s.equals(ar)||s.equals(ku);}
    private int dp482(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
