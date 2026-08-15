package com.infinitygreenpower.solarform;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** v4.8.4 regression-safe UI stabilization. Functional View tags are never overwritten. */
public class OfferStudioV484Activity extends OfferStudioV480Activity {
    private final Handler ui484 = new Handler(Looper.getMainLooper());
    private boolean running484;
    private final Set<View> styled = Collections.newSetFromMap(new WeakHashMap<View, Boolean>());
    private final Set<View> languageBound = Collections.newSetFromMap(new WeakHashMap<View, Boolean>());

    private static final int NAVY = Color.rgb(18, 50, 79);
    private static final int EMERALD = Color.rgb(20, 153, 112);
    private static final int LINE = Color.rgb(207, 220, 232);
    private static final int SOFT = Color.rgb(238, 244, 250);
    private static final int RED = Color.rgb(184, 61, 61);

    private final Runnable polish484 = new Runnable() {
        @Override public void run() {
            if (!running484) return;
            try {
                View root = findViewById(android.R.id.content);
                if (root != null) {
                    bindImmediateLanguage(root);
                    hideEmptyLogoPreview(root);
                    fixHomeQuickCards(root);
                    stabilizeExistingButtons(root);
                }
            } catch (Exception ignored) { }
            ui484.postDelayed(this, 500);
        }
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        running484 = true;
        ui484.postDelayed(polish484, 220);
    }
    @Override protected void onResume() {
        super.onResume();
        if (!running484) { running484 = true; ui484.postDelayed(polish484, 180); }
    }
    @Override protected void onPause() {
        running484 = false; ui484.removeCallbacks(polish484); super.onPause();
    }
    @Override protected void onDestroy() {
        running484 = false; ui484.removeCallbacks(polish484); super.onDestroy();
    }

    private int dp(int n) { return (int) (n * getResources().getDisplayMetrics().density + 0.5f); }
    private SharedPreferences settings() { return getSharedPreferences("offer_studio_settings", MODE_PRIVATE); }
    private String lang() { return settings().getString("lang", "en"); }

    private boolean isSettingsScreen(View root) {
        return hasExact(root, "Settings") || hasExact(root, "الإعدادات") || hasExact(root, "ڕێکخستنەکان");
    }
    private boolean isHomeScreen(View root) {
        return hasExact(root, "Quick actions") || hasExact(root, "إجراءات سريعة") || hasExact(root, "کردارە خێراکان");
    }
    private boolean hasExact(View v, String wanted) {
        if (v == null) return false;
        if (v instanceof TextView && wanted.equals(String.valueOf(((TextView) v).getText()).trim())) return true;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) if (hasExact(g.getChildAt(i), wanted)) return true;
        }
        return false;
    }

    /* Immediate language change, without touching any functional tags. */
    private void bindImmediateLanguage(View root) {
        if (!isSettingsScreen(root)) return;
        final Spinner spinner = findLanguageSpinner(root);
        if (spinner == null || languageBound.contains(spinner)) return;
        languageBound.add(spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) { }
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String next = languageForPosition(spinner, position);
                String current = lang();
                if (next == null || next.equals(current)) return;
                settings().edit().putString("lang", next).apply();
                getWindow().getDecorView().setLayoutDirection("en".equals(next) ? View.LAYOUT_DIRECTION_LTR : View.LAYOUT_DIRECTION_RTL);
                ui484.postDelayed(() -> reopenSettings(), 80);
            }
        });
    }

    private Spinner findLanguageSpinner(View v) {
        if (v instanceof Spinner) {
            Spinner s = (Spinner) v;
            if (s.getAdapter() != null) {
                boolean en=false, ar=false, ku=false;
                for (int i=0;i<s.getAdapter().getCount();i++) {
                    String x=String.valueOf(s.getAdapter().getItem(i));
                    if (x.equalsIgnoreCase("English")) en=true;
                    if (x.contains("العربية") || x.contains("عربي")) ar=true;
                    if (x.contains("کورد") || x.toLowerCase().contains("kurd")) ku=true;
                }
                if (en && (ar || ku)) return s;
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++){
                Spinner s=findLanguageSpinner(g.getChildAt(i));
                if(s!=null)return s;
            }
        }
        return null;
    }
    private String languageForPosition(Spinner s, int position) {
        if (s.getAdapter()==null || position<0 || position>=s.getAdapter().getCount()) return null;
        String x=String.valueOf(s.getAdapter().getItem(position));
        if (x.equalsIgnoreCase("English")) return "en";
        if (x.contains("العربية") || x.contains("عربي")) return "ar";
        if (x.contains("کورد") || x.toLowerCase().contains("kurd")) return "ku";
        return null;
    }
    private void reopenSettings() {
        try {
            Method m = OfferStudioV470Activity.class.getDeclaredMethod("showSettings470");
            m.setAccessible(true); m.invoke(this);
        } catch (Exception ignored) { }
    }

    /* Hide the blank logo preview square only when it truly has no drawable. */
    private void hideEmptyLogoPreview(View root) {
        if (!isSettingsScreen(root)) return;
        hideBlankImages(root);
    }
    private void hideBlankImages(View v) {
        if (v instanceof ImageView) {
            ImageView im=(ImageView)v;
            Drawable d=im.getDrawable();
            if (d==null && im.getVisibility()==View.VISIBLE) {
                ViewGroup.LayoutParams lp=im.getLayoutParams();
                if (lp!=null && lp.width>dp(55) && lp.height>dp(55)) im.setVisibility(View.GONE);
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++)hideBlankImages(g.getChildAt(i));
        }
    }

    /* Make all four Home quick-action cards share the full row width equally. */
    private void fixHomeQuickCards(View root) {
        if (!isHomeScreen(root)) return;
        String[][] labels = {
                {"Create offer","إنشاء عرض","دروستکردنی پێشنیار"},
                {"Saved offers","العروض المحفوظة","پێشنیارە پاشەکەوتکراوەکان"},
                {"Product catalog","كتالوج المنتجات","کاتەلۆگی بەرهەمەکان"},
                {"Quick preview","معاينة سريعة","پێشبینینی خێرا"}
        };
        for (String[] names:labels) {
            TextView label=findAnyText(root,names);
            if(label==null)continue;
            View card=findHorizontalRowChild(label);
            if(card==null)continue;
            ViewGroup.LayoutParams raw=card.getLayoutParams();
            if(raw instanceof LinearLayout.LayoutParams){
                LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)raw;
                lp.width=0; lp.weight=1f; lp.height=dp(154);
                lp.leftMargin=dp(5); lp.rightMargin=dp(5);
                card.setLayoutParams(lp);
            }
        }
    }
    private TextView findAnyText(View v,String[] names){
        if(v instanceof TextView){String s=String.valueOf(((TextView)v).getText()).trim();for(String n:names)if(n.equals(s))return(TextView)v;}
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){TextView t=findAnyText(g.getChildAt(i),names);if(t!=null)return t;}}
        return null;
    }
    private View findHorizontalRowChild(View start){
        View child=start; ViewParent p=start.getParent();
        while(p instanceof View){
            View pv=(View)p;
            if(pv instanceof LinearLayout && ((LinearLayout)pv).getOrientation()==LinearLayout.HORIZONTAL)return child;
            child=pv; p=pv.getParent();
        }
        return null;
    }

    /* Stable button dimensions. Uses WeakHashSet, NEVER setTag(). */
    private void stabilizeExistingButtons(View v) {
        if (v instanceof Button) {
            Button b=(Button)v;
            if(!styled.contains(b)){ styleButtonWithoutChangingTag(b); styled.add(b); }
        }
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)stabilizeExistingButtons(g.getChildAt(i));}
    }
    private void styleButtonWithoutChangingTag(Button b){
        String s=String.valueOf(b.getText()).trim();
        if(s.isEmpty())return;
        boolean main = matches(s,"Continue","متابعة","بەردەوام بە")
                || matches(s,"Preview offer","معاينة العرض","پێشبینینی پێشنیار")
                || matches(s,"Create new offer","إنشاء عرض جديد","پێشنیاری نوێ دروست بکە");
        boolean danger = matches(s,"Remove","إزالة","سڕینەوە") || matches(s,"Delete","حذف","سڕینەوە");
        boolean settingsSmall = matches(s,"Choose logo","اختيار الشعار","لۆگۆ هەڵبژێرە")
                || matches(s,"Manage organizer profiles","إدارة منظمي الكشف","بەڕێوەبردنی ڕێکخەران")
                || matches(s,"Save settings","حفظ الإعدادات","ڕێکخستنەکان پاشەکەوت بکە");
        boolean regular = main || danger || settingsSmall
                || s.contains("Save draft") || s.contains("حفظ كمسودة") || s.contains("ڕەشنووس")
                || matches(s,"Back","رجوع","گەڕانەوە") || matches(s,"Cancel","إلغاء","هەڵوەشاندنەوە")
                || matches(s,"Browse catalog","تصفح الكتالوج","کاتەلۆگ ببینە") || matches(s,"Custom item","مادة مخصصة","ماددەی تایبەت")
                || s.toLowerCase().contains("organizer") || s.contains("منظم") || s.contains("ڕێکخەر")
                || s.toLowerCase().contains("photos") || s.contains("صور") || s.contains("وێنە");
        if(!regular)return;

        b.setAllCaps(false); b.setTextSize(settingsSmall?11.5f:12.5f); b.setTypeface(Typeface.DEFAULT,Typeface.NORMAL);
        b.setMinHeight(0); b.setMinimumHeight(0); b.setMinWidth(0); b.setMinimumWidth(0); b.setPadding(dp(10),0,dp(10),0);
        if(android.os.Build.VERSION.SDK_INT>=21){b.setElevation(0);b.setStateListAnimator(null);}

        int fill=SOFT, stroke=LINE, fg=NAVY;
        if(main){fill=EMERALD;stroke=EMERALD;fg=Color.WHITE;}
        else if(danger){fill=Color.rgb(255,241,241);stroke=Color.rgb(242,204,204);fg=RED;}
        b.setTextColor(fg);
        GradientDrawable gd=new GradientDrawable(); gd.setColor(fill); gd.setStroke(dp(1),stroke); gd.setCornerRadius(dp(14)); b.setBackground(gd);

        ViewGroup.LayoutParams raw=b.getLayoutParams();
        if(raw instanceof LinearLayout.LayoutParams){
            LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)raw;
            if(settingsSmall)lp.height=dp(40); else if(main)lp.height=dp(50); else lp.height=dp(44);
            b.setLayoutParams(lp);
        }
    }
    private boolean matches(String s,String en,String ar,String ku){return s.equals(en)||s.equals(ar)||s.equals(ku);}
}
