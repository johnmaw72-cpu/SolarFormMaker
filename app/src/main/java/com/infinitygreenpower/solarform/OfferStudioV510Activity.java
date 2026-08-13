package com.infinitygreenpower.solarform;

import android.content.res.ColorStateList;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Field;

/**
 * IGP Organizer Form v5.1.0
 * - Fixes the repeated Save Draft buttons caused by older styling layers replacing functional tags.
 * - Uses one stable UI styling pass only.
 * - Applies the clean white-card / aqua-green UI direction across the organizer workflow.
 */
public class OfferStudioV510Activity extends OfferStudioV500Activity {
    private final Handler ui510 = new Handler(Looper.getMainLooper());
    private boolean running510;

    private static final int BG = Color.rgb(246, 249, 252);
    private static final int WHITE = Color.WHITE;
    private static final int INK = Color.rgb(20, 35, 48);
    private static final int MUTED = Color.rgb(112, 127, 140);
    private static final int TEAL = Color.rgb(29, 199, 165);
    private static final int TEAL_DARK = Color.rgb(20, 163, 137);
    private static final int NAVY = Color.rgb(24, 58, 86);
    private static final int LINE = Color.rgb(222, 230, 237);

    private final Runnable polish510 = new Runnable() {
        @Override public void run() {
            if (!running510) return;
            try {
                View root = findViewById(android.R.id.content);
                removeDuplicateDraftButtons(root);
                styleScreen(root);
            } catch (Exception ignored) { }
            ui510.postDelayed(this, 450);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        // v4.9/v5.0 changed functional view tags while styling. Stop those visual loops;
        // v4.7 remains active because it owns functional navigation/forms logic.
        stopLegacyStyler(OfferStudioV490Activity.class, "running490", "ui490", "styler490");
        stopLegacyStyler(OfferStudioV500Activity.class, "running500", "ui500", "styler500");
        stopLegacyStyler(OfferStudioV480Activity.class, "running480", "ui480", "savedStyler");
        running510 = true;
        ui510.postDelayed(polish510, 250);
    }

    @Override protected void onResume() {
        super.onResume();
        stopLegacyStyler(OfferStudioV490Activity.class, "running490", "ui490", "styler490");
        stopLegacyStyler(OfferStudioV500Activity.class, "running500", "ui500", "styler500");
        stopLegacyStyler(OfferStudioV480Activity.class, "running480", "ui480", "savedStyler");
        if (!running510) {
            running510 = true;
            ui510.postDelayed(polish510, 180);
        }
    }

    @Override protected void onPause() {
        running510 = false;
        ui510.removeCallbacks(polish510);
        super.onPause();
    }

    @Override protected void onDestroy() {
        running510 = false;
        ui510.removeCallbacks(polish510);
        super.onDestroy();
    }

    private void stopLegacyStyler(Class<?> owner, String runningField, String handlerField, String runnableField) {
        try {
            Field rf = owner.getDeclaredField(runningField); rf.setAccessible(true); rf.setBoolean(this, false);
            Field hf = owner.getDeclaredField(handlerField); hf.setAccessible(true);
            Field task = owner.getDeclaredField(runnableField); task.setAccessible(true);
            Object h = hf.get(this), r = task.get(this);
            if (h instanceof Handler && r instanceof Runnable) ((Handler) h).removeCallbacks((Runnable) r);
        } catch (Exception ignored) { }
    }

    /* ---------------- bug fix ---------------- */
    private boolean isDraftText(String s) {
        return "Save draft".equals(s) || "حفظ كمسودة".equals(s) || "وەک ڕەشنووس پاشەکەوت بکە".equals(s);
    }

    private void removeDuplicateDraftButtons(View root) {
        final Button[] keeper = {null};
        cleanDraftTree(root, keeper);
        if (keeper[0] != null) keeper[0].setTag("v470_draft");
    }

    private void cleanDraftTree(View v, Button[] keeper) {
        if (!(v instanceof ViewGroup)) return;
        ViewGroup g = (ViewGroup) v;
        for (int i = g.getChildCount() - 1; i >= 0; i--) {
            View child = g.getChildAt(i);
            if (child instanceof Button && isDraftText(String.valueOf(((Button) child).getText()).trim())) {
                if (keeper[0] == null) keeper[0] = (Button) child;
                else g.removeViewAt(i);
            } else cleanDraftTree(child, keeper);
        }
    }

    /* ---------------- UI system ---------------- */
    private void styleScreen(View root) {
        if (root == null) return;
        root.setBackgroundColor(BG);
        styleTree(root);
        styleBottomNavigation(root);
        styleKnownCards(root);
    }

    private void styleTree(View v) {
        if (v instanceof EditText) styleField((EditText) v);
        else if (v instanceof Button) styleButton((Button) v);
        else if (v instanceof CheckBox) styleCheck((CheckBox) v);
        else if (v instanceof TextView) styleText((TextView) v);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) styleTree(g.getChildAt(i));
        }
    }

    private void styleText(TextView t) {
        String s = String.valueOf(t.getText()).trim();
        if (isLargeTitle(s)) {
            t.setTextColor(INK); t.setTypeface(Typeface.DEFAULT_BOLD); t.setTextSize(25);
        } else if (isSectionTitle(s)) {
            t.setTextColor(INK); t.setTypeface(Typeface.DEFAULT_BOLD);
        }
    }

    private boolean isLargeTitle(String s) {
        return eq(s,"Home","الرئيسية","سەرەکی") || eq(s,"New Offer","عرض جديد","پێشنیاری نوێ") ||
               eq(s,"Saved Forms","الاستمارات المحفوظة","فۆرمە پاشەکەوتکراوەکان") ||
               eq(s,"Product Catalog","كتالوج المنتجات","کاتەلۆگی بەرهەمەکان") ||
               eq(s,"Settings","الإعدادات","ڕێکخستنەکان");
    }

    private boolean isSectionTitle(String s) {
        return eq(s,"Client Information","معلومات العميل","زانیاری کڕیار") ||
               eq(s,"System Information","معلومات المنظومة","زانیاری سیستەم") ||
               eq(s,"Offer Items","مواد العرض","ماددەکانی پێشنیار") ||
               eq(s,"Additional Information","معلومات إضافية","زانیاری زیاتر") ||
               eq(s,"Company identity","هوية الشركة","ناسنامەی کۆمپانیا") ||
               eq(s,"App defaults","الإعدادات الافتراضية","ڕێکخستنی بنەڕەت") ||
               eq(s,"PDF content","محتوى PDF","ناوەڕۆکی PDF") ||
               eq(s,"Organizer & catalog tools","أدوات المنظم والكتالوج","ئامرازەکانی ڕێکخەر و کاتەلۆگ");
    }

    private void styleField(EditText e) {
        e.setTextColor(INK);
        e.setHintTextColor(Color.rgb(154, 165, 176));
        e.setTextSize(15);
        e.setPadding(dp(16), e.getPaddingTop(), dp(16), e.getPaddingBottom());
        e.setBackground(round(WHITE, LINE, 17));
        if (android.os.Build.VERSION.SDK_INT >= 21) e.setElevation(0);
    }

    private void styleCheck(CheckBox c) {
        c.setTextColor(INK); c.setTextSize(13);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            int[][] states = new int[][] { new int[]{android.R.attr.state_checked}, new int[]{} };
            int[] colors = new int[] { TEAL_DARK, Color.rgb(135, 148, 158) };
            c.setButtonTintList(new ColorStateList(states, colors));
        }
    }

    private void styleButton(Button b) {
        String s = String.valueOf(b.getText()).trim();
        if (s.isEmpty()) return;
        b.setAllCaps(false); b.setTypeface(Typeface.DEFAULT_BOLD); b.setGravity(Gravity.CENTER);
        b.setMinHeight(0); b.setMinimumHeight(0); b.setMinWidth(0); b.setMinimumWidth(0);
        if (android.os.Build.VERSION.SDK_INT >= 21) { b.setStateListAnimator(null); b.setElevation(0); }

        if (isPrimary(s)) apply(b, TEAL, Color.rgb(22,175,145), TEAL, Color.WHITE, 18, 54);
        else if (isNavy(s)) apply(b, NAVY, Color.rgb(16,45,68), NAVY, Color.WHITE, 17, 50);
        else if (isDanger(s)) apply(b, Color.rgb(255,247,247), Color.rgb(255,235,235), Color.rgb(237,184,184), Color.rgb(183,55,55), 16, 44);
        else if (isTealOutline(s)) apply(b, WHITE, Color.rgb(235,252,248), TEAL, TEAL_DARK, 16, 46);
        else apply(b, Color.rgb(243,247,250), Color.rgb(232,240,246), Color.rgb(211,222,230), NAVY, 16, Math.max(44, heightDp(b)));
    }

    private boolean isPrimary(String s) {
        return eq(s,"Continue","متابعة","بەردەوام بە") || eq(s,"Preview offer","معاينة العرض","پێشبینینی پێشنیار") ||
               eq(s,"Save settings","حفظ الإعدادات","ڕێکخستنەکان پاشەکەوت بکە") || eq(s,"Preview","معاينة","پێشبینین") ||
               eq(s,"Share PDF","مشاركة PDF","PDF هاوبەش بکە") || eq(s,"Save PDF","حفظ PDF","PDF پاشەکەوت بکە") ||
               eq(s,"WhatsApp PDF","PDF عبر واتساب","PDF بە واتسئاپ");
    }
    private boolean isNavy(String s) { return eq(s,"Browse catalog","اختيار من الكتالوج","کاتەلۆگ ببینە"); }
    private boolean isDanger(String s) { return eq(s,"Delete","حذف","سڕینەوە") || eq(s,"Remove","إزالة","سڕینەوە") || eq(s,"Clear favorites","مسح المفضلة","دڵخوازەکان پاک بکەرەوە"); }
    private boolean isTealOutline(String s) {
        return eq(s,"Custom item","مادة مخصصة","ماددەی تایبەت") || eq(s,"Replace","استبدال","گۆڕین") ||
               eq(s,"Duplicate","نسخ","دووبارە") || eq(s,"Choose organizer","اختيار منظم الكشف","ڕێکخەر هەڵبژێرە") ||
               eq(s,"Manage organizer profiles","إدارة منظمي الكشف","بەڕێوەبردنی ڕێکخەران") || eq(s,"Choose logo","اختيار الشعار","لۆگۆ هەڵبژێرە") ||
               eq(s,"Share PDF + Data","مشاركة PDF + البيانات","PDF + داتا هاوبەش بکە");
    }

    private int heightDp(Button b) {
        ViewGroup.LayoutParams p = b.getLayoutParams();
        if (p != null && p.height > 0) return Math.max(40, (int)(p.height / getResources().getDisplayMetrics().density));
        return 44;
    }

    private void apply(Button b, int normal, int pressed, int stroke, int fg, int radius, int height) {
        b.setTextColor(fg); b.setTextSize(13); b.setPadding(dp(14),0,dp(14),0);
        b.setBackground(state(normal,pressed,stroke,radius));
        ViewGroup.LayoutParams raw = b.getLayoutParams();
        if (raw instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)raw;
            lp.height=dp(height); lp.leftMargin=Math.max(lp.leftMargin,dp(4)); lp.rightMargin=Math.max(lp.rightMargin,dp(4));
            lp.topMargin=Math.max(lp.topMargin,dp(4)); lp.bottomMargin=Math.max(lp.bottomMargin,dp(4));
            b.setLayoutParams(lp);
        }
    }

    private void styleBottomNavigation(View root) {
        View nav = findBottomNavCandidate(root);
        if (nav != null) {
            nav.setBackgroundColor(WHITE);
            if (android.os.Build.VERSION.SDK_INT >= 21) nav.setElevation(dp(10));
        }
    }

    private View findBottomNavCandidate(View v) {
        if (!(v instanceof ViewGroup)) return null;
        ViewGroup g=(ViewGroup)v;
        int labels=0;
        for(int i=0;i<g.getChildCount();i++){
            View c=g.getChildAt(i);
            if(hasAnyNavText(c)) labels++;
        }
        if(labels>=4) return g;
        for(int i=0;i<g.getChildCount();i++){View r=findBottomNavCandidate(g.getChildAt(i));if(r!=null)return r;}
        return null;
    }

    private boolean hasAnyNavText(View v) {
        if(v instanceof TextView){String s=String.valueOf(((TextView)v).getText()).trim();return eq(s,"Home","الرئيسية","سەرەکی")||eq(s,"Catalog","الكتالوج","کاتەلۆگ")||eq(s,"Saved","محفوظ","پاشەکەوتکراو")||eq(s,"New Offer","عرض جديد","پێشنیاری نوێ")||eq(s,"Settings","الإعدادات","ڕێکخستنەکان");}
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(hasAnyNavText(g.getChildAt(i)))return true;}
        return false;
    }

    private void styleKnownCards(View root) {
        String[][] headings = new String[][] {
                {"Company identity","هوية الشركة","ناسنامەی کۆمپانیا"},
                {"App defaults","الإعدادات الافتراضية","ڕێکخستنی بنەڕەت"},
                {"PDF content","محتوى PDF","ناوەڕۆکی PDF"},
                {"Organizer & catalog tools","أدوات المنظم والكتالوج","ئامرازەکانی ڕێکخەر و کاتەلۆگ"},
                {"Load capacity note","ملاحظة قدرة التحمل","تێبینی توانای بار"}
        };
        for(String[] h:headings){TextView t=findExact(root,h);if(t!=null&&t.getParent() instanceof View){View p=(View)t.getParent();p.setBackground(round(WHITE,LINE,22));if(android.os.Build.VERSION.SDK_INT>=21)p.setElevation(dp(1));}}
    }

    private TextView findExact(View v,String[] vals){
        if(v instanceof TextView){String s=String.valueOf(((TextView)v).getText()).trim();for(String x:vals)if(x.equals(s))return(TextView)v;}
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){TextView r=findExact(g.getChildAt(i),vals);if(r!=null)return r;}}
        return null;
    }

    private StateListDrawable state(int normal,int pressed,int stroke,int radius){
        StateListDrawable s=new StateListDrawable();s.addState(new int[]{android.R.attr.state_pressed},round(pressed,stroke,radius));s.addState(new int[]{},round(normal,stroke,radius));return s;
    }
    private GradientDrawable round(int fill,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));g.setStroke(dp(1),stroke);return g;}
    private boolean eq(String actual,String en,String ar,String ku){return actual.equals(en)||actual.equals(ar)||actual.equals(ku);}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
