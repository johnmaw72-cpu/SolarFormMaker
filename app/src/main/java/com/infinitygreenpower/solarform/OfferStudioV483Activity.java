package com.infinitygreenpower.solarform;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.lang.reflect.Method;

/**
 * v4.8.3 UI stability pass on the preferred v4.8 branch.
 * - Language applies immediately inside Settings.
 * - Hides empty logo preview and lets logo buttons fill the row.
 * - Makes Settings and workflow buttons compact and stable.
 * - Makes Home quick-action cards use the full two-column grid width.
 */
public class OfferStudioV483Activity extends OfferStudioV482Activity {
    private final Handler h483 = new Handler(Looper.getMainLooper());
    private boolean running483;

    private static final int NAVY = Color.rgb(18,50,79);
    private static final int EMERALD = Color.rgb(20,153,112);
    private static final int LINE = Color.rgb(207,220,232);

    private final Runnable polish483 = new Runnable() {
        @Override public void run() {
            if (!running483) return;
            try {
                View root = findViewById(android.R.id.content);
                if (root != null) {
                    boolean settings = containsAny(root, new String[]{"Settings","الإعدادات","ڕێکخستنەکان"});
                    boolean home = containsAny(root, new String[]{"Quick actions","إجراءات سريعة","کردارە خێراکان"});
                    if (settings) {
                        bindLanguageImmediate(root);
                        fixEmptyLogoPreview(root);
                        styleSettingsButtons(root);
                    }
                    if (home) fixHomeGrid(root);
                    stabilizeWorkflowButtons(root);
                }
            } catch (Exception ignored) { }
            h483.postDelayed(this, 320);
        }
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        running483 = true;
        h483.postDelayed(polish483, 260);
    }

    @Override protected void onResume() {
        super.onResume();
        if (!running483) {
            running483 = true;
            h483.postDelayed(polish483, 180);
        }
    }

    @Override protected void onPause() {
        running483 = false;
        h483.removeCallbacks(polish483);
        super.onPause();
    }

    @Override protected void onDestroy() {
        running483 = false;
        h483.removeCallbacks(polish483);
        super.onDestroy();
    }

    private int dp483(int n) {
        return (int)(n * getResources().getDisplayMetrics().density + .5f);
    }

    private SharedPreferences prefs483() {
        return getSharedPreferences("offer_studio_settings", MODE_PRIVATE);
    }

    private GradientDrawable bg483(int fill, int stroke, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp483(radius));
        if (stroke != 0) g.setStroke(dp483(1), stroke);
        return g;
    }

    private boolean containsAny(View v, String[] values) {
        if (v == null) return false;
        if (v instanceof TextView) {
            String s = String.valueOf(((TextView)v).getText()).trim();
            for (String x : values) if (x.equals(s)) return true;
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup)v;
            for (int i=0;i<g.getChildCount();i++) if (containsAny(g.getChildAt(i), values)) return true;
        }
        return false;
    }

    /* -------- immediate language -------- */
    private void bindLanguageImmediate(View v) {
        if (v instanceof Spinner) {
            Spinner s = (Spinner)v;
            if (!"v483_language".equals(String.valueOf(s.getTag())) && isLanguageSpinner(s)) {
                s.setTag("v483_language");
                s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override public void onNothingSelected(AdapterView<?> parent) { }
                    @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Object item = parent.getItemAtPosition(position);
                        String code = languageCode(item == null ? "" : String.valueOf(item));
                        if (code == null) return;
                        String old = prefs483().getString("lang", "en");
                        if (code.equals(old)) return;
                        prefs483().edit().putString("lang", code).apply();
                        h483.postDelayed(() -> reopenSettings(), 80);
                    }
                });
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++) bindLanguageImmediate(g.getChildAt(i));
        }
    }

    private boolean isLanguageSpinner(Spinner s) {
        SpinnerAdapter a=s.getAdapter();
        if(a==null) return false;
        boolean en=false, ar=false, ku=false;
        for(int i=0;i<a.getCount();i++) {
            String x=String.valueOf(a.getItem(i)).trim().toLowerCase();
            if(x.equals("english")) en=true;
            if(x.contains("العربية") || x.equals("arabic")) ar=true;
            if(x.contains("کورد") || x.contains("كورد") || x.equals("kurdish")) ku=true;
        }
        return en && ar && ku;
    }

    private String languageCode(String value) {
        String x=value.trim().toLowerCase();
        if(x.equals("english") || x.equals("en")) return "en";
        if(x.contains("العربية") || x.equals("arabic") || x.equals("ar")) return "ar";
        if(x.contains("کورد") || x.contains("كورد") || x.equals("kurdish") || x.equals("ku")) return "ku";
        return null;
    }

    private void reopenSettings() {
        try {
            Class<?> c=Class.forName("com.infinitygreenpower.solarform.OfferStudioV470Activity");
            Method m=c.getDeclaredMethod("showSettings470");
            m.setAccessible(true);
            m.invoke(this);
        } catch(Exception e) {
            recreate();
        }
    }

    /* -------- Settings logo row -------- */
    private void fixEmptyLogoPreview(View v) {
        if (v instanceof ImageView) {
            ImageView image=(ImageView)v;
            if (image.getDrawable()==null && image.getVisibility()==View.VISIBLE) {
                ViewGroup.LayoutParams raw=image.getLayoutParams();
                if(raw!=null && (raw.width>=dp483(60) || raw.height>=dp483(60))) {
                    ViewParentHelper.hideAndRedistribute(image);
                }
            }
        }
        if(v instanceof ViewGroup) {
            ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++) fixEmptyLogoPreview(g.getChildAt(i));
        }
    }

    private static class ViewParentHelper {
        static void hideAndRedistribute(ImageView image) {
            android.view.ViewParent p=image.getParent();
            image.setVisibility(View.GONE);
            if(!(p instanceof LinearLayout)) return;
            LinearLayout row=(LinearLayout)p;
            for(int i=0;i<row.getChildCount();i++) {
                View child=row.getChildAt(i);
                if(child.getVisibility()!=View.VISIBLE || !(child instanceof Button)) continue;
                ViewGroup.LayoutParams raw=child.getLayoutParams();
                if(raw instanceof LinearLayout.LayoutParams) {
                    LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)raw;
                    lp.width=0;
                    lp.weight=1f;
                    child.setLayoutParams(lp);
                }
            }
        }
    }

    /* -------- compact Settings buttons -------- */
    private void styleSettingsButtons(View v) {
        if(v instanceof Button) {
            Button b=(Button)v;
            String s=String.valueOf(b.getText()).trim();
            if(isSettingsAction(s)) styleButton(b, 40, 12, false);
        }
        if(v instanceof ViewGroup) {
            ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++) styleSettingsButtons(g.getChildAt(i));
        }
    }

    private boolean isSettingsAction(String s) {
        return eq(s,"Choose logo","اختيار الشعار","لۆگۆ هەڵبژێرە")
                || eq(s,"Remove","إزالة","لابردن")
                || eq(s,"Manage organizer profiles","إدارة منظمي الكشف","بەڕێوەبردنی ڕێکخەران")
                || eq(s,"Save settings","حفظ الإعدادات","ڕێکخستنەکان پاشەکەوت بکە")
                || eq(s,"Clear recent","مسح الأخيرة","دواییەکان بسڕەوە")
                || eq(s,"Clear favorites","مسح المفضلة","دڵخوازەکان بسڕەوە");
    }

    /* -------- Home full-width two-column cards -------- */
    private void fixHomeGrid(View v) {
        if(!(v instanceof ViewGroup)) return;
        ViewGroup g=(ViewGroup)v;
        if(g instanceof LinearLayout && g.getChildCount()==2) {
            View a=g.getChildAt(0), b=g.getChildAt(1);
            boolean left=isQuickCard(a), right=isQuickCard(b);
            if(left && right) {
                makeGridChild(a,true);
                makeGridChild(b,false);
            }
        }
        for(int i=0;i<g.getChildCount();i++) fixHomeGrid(g.getChildAt(i));
    }

    private boolean isQuickCard(View v) {
        return containsAny(v,new String[]{
                "Create offer","Saved offers","Product catalog","Quick preview",
                "إنشاء عرض","العروض المحفوظة","كتالوج المنتجات","معاينة سريعة",
                "پێشنیار دروست بکە","پێشنیارە پاشەکەوتکراوەکان","کاتەلۆگی بەرهەمەکان","پێشبینینی خێرا"
        });
    }

    private void makeGridChild(View child, boolean left) {
        ViewGroup.LayoutParams raw=child.getLayoutParams();
        if(raw instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)raw;
            lp.width=0;
            lp.weight=1f;
            lp.height=dp483(150);
            if(left) { lp.leftMargin=0; lp.rightMargin=dp483(6); }
            else { lp.leftMargin=dp483(6); lp.rightMargin=0; }
            child.setLayoutParams(lp);
        }
        child.setMinimumHeight(dp483(150));
    }

    /* -------- stable workflow/button sizing -------- */
    private void stabilizeWorkflowButtons(View v) {
        if(v instanceof Button) {
            Button b=(Button)v;
            String s=String.valueOf(b.getText()).trim();
            if(isStableAction(s)) {
                int h=isSmallAction(s)?42:48;
                styleButton(b,h,isSmallAction(s)?12:13,isPrimary(s));
            }
        }
        if(v instanceof ViewGroup) {
            ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++) stabilizeWorkflowButtons(g.getChildAt(i));
        }
    }

    private boolean isStableAction(String s) {
        return isSettingsAction(s)
                || eq(s,"Save draft","حفظ كمسودة","وەک ڕەشنووس پاشەکەوت بکە")
                || eq(s,"Continue","متابعة","بەردەوام بە")
                || eq(s,"Cancel","إلغاء","هەڵوەشاندنەوە")
                || eq(s,"Back","رجوع","گەڕانەوە")
                || eq(s,"Preview offer","معاينة العرض","پێشبینینی پێشنیار")
                || eq(s,"+ Add photos","+ إضافة صور","+ وێنە زیاد بکە")
                || eq(s,"Manage photos","إدارة الصور","بەڕێوەبردنی وێنەکان")
                || eq(s,"Choose organizer","اختيار منظم الكشف","ڕێکخەر هەڵبژێرە")
                || eq(s,"Browse catalog","تصفح الكتالوج","کاتەلۆگ ببینە")
                || eq(s,"Custom item","مادة مخصصة","ماددەی تایبەت")
                || eq(s,"Create new offer","إنشاء عرض جديد","پێشنیاری نوێ دروست بکە");
    }

    private boolean isSmallAction(String s) {
        return isSettingsAction(s)
                || eq(s,"Choose organizer","اختيار منظم الكشف","ڕێکخەر هەڵبژێرە")
                || eq(s,"Browse catalog","تصفح الكتالوج","کاتەلۆگ ببینە")
                || eq(s,"Custom item","مادة مخصصة","ماددەی تایبەت");
    }

    private boolean isPrimary(String s) {
        return eq(s,"Continue","متابعة","بەردەوام بە")
                || eq(s,"Preview offer","معاينة العرض","پێشبینینی پێشنیار")
                || eq(s,"Create new offer","إنشاء عرض جديد","پێشنیاری نوێ دروست بکە");
    }

    private boolean eq(String s,String en,String ar,String ku) {
        return s.equals(en)||s.equals(ar)||s.equals(ku);
    }

    private void styleButton(Button b,int height,int textSize,boolean primary) {
        String key="v483_"+height+"_"+textSize+"_"+primary;
        if(key.equals(String.valueOf(b.getTag()))) return;
        b.setTag(key);
        b.setAllCaps(false);
        b.setTextSize(textSize);
        b.setTypeface(Typeface.DEFAULT,Typeface.NORMAL);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(dp483(12),0,dp483(12),0);
        b.setTextColor(primary?Color.WHITE:NAVY);
        b.setBackground(bg483(primary?EMERALD:Color.rgb(240,246,250),primary?EMERALD:LINE,14));
        if(android.os.Build.VERSION.SDK_INT>=21){b.setElevation(0);b.setStateListAnimator(null);}
        ViewGroup.LayoutParams raw=b.getLayoutParams();
        if(raw!=null){raw.height=dp483(height);b.setLayoutParams(raw);}
    }
}
