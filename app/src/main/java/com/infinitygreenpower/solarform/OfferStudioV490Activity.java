package com.infinitygreenpower.solarform;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * IGP Organizer Form v4.9.0
 * Professional action-button system for Settings and Step 4.
 */
public class OfferStudioV490Activity extends OfferStudioV480Activity {
    private final Handler ui490 = new Handler(Looper.getMainLooper());
    private boolean running490;

    private final Runnable styler490 = new Runnable() {
        @Override public void run() {
            if (!running490) return;
            try {
                View root = findViewById(android.R.id.content);
                if (isSettings(root)) styleSettingsButtons(root);
                if (isFinalStep(root)) styleFinalStepButtons(root);
            } catch (Exception ignored) { }
            ui490.postDelayed(this, 300);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        running490 = true;
        ui490.postDelayed(styler490, 350);
    }

    @Override protected void onResume() {
        super.onResume();
        if (!running490) {
            running490 = true;
            ui490.postDelayed(styler490, 220);
        }
    }

    @Override protected void onPause() {
        running490 = false;
        ui490.removeCallbacks(styler490);
        super.onPause();
    }

    @Override protected void onDestroy() {
        running490 = false;
        ui490.removeCallbacks(styler490);
        super.onDestroy();
    }

    private boolean isSettings(View root) {
        return hasExact(root, "Settings") || hasExact(root, "الإعدادات") || hasExact(root, "ڕێکخستنەکان");
    }

    private boolean isFinalStep(View root) {
        return hasExact(root, "Additional Information") || hasExact(root, "معلومات إضافية") || hasExact(root, "زانیاری زیاتر");
    }

    private boolean hasExact(View v, String text) {
        if (v == null) return false;
        if (v instanceof TextView && text.equals(String.valueOf(((TextView) v).getText()).trim())) return true;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) if (hasExact(g.getChildAt(i), text)) return true;
        }
        return false;
    }

    private void styleSettingsButtons(View v) {
        if (v == null) return;
        if (v instanceof Button) {
            Button b = (Button) v;
            String s = String.valueOf(b.getText()).trim();
            if (matches(s, "Choose logo", "اختيار الشعار", "لۆگۆ هەڵبژێرە")) {
                apply(b, "choose_logo", Style.NAVY, 46);
            } else if (matches(s, "Remove", "إزالة", "سڕینەوە")) {
                apply(b, "remove_logo", Style.DANGER_OUTLINE, 46);
            } else if (matches(s, "Manage organizer profiles", "إدارة منظمي الكشف", "بەڕێوەبردنی ڕێکخەران")) {
                apply(b, "manage_org", Style.SOFT_BLUE, 50);
            } else if (matches(s, "Clear recent items", "مسح المواد الأخيرة", "دوایین ماددەکان پاک بکەرەوە")) {
                apply(b, "clear_recent", Style.SOFT_AMBER, 50);
            } else if (matches(s, "Clear favorites", "مسح المفضلة", "دڵخوازەکان پاک بکەرەوە")) {
                apply(b, "clear_fav", Style.DANGER_OUTLINE, 50);
            } else if (matches(s, "Save settings", "حفظ الإعدادات", "ڕێکخستنەکان پاشەکەوت بکە")) {
                apply(b, "save_settings", Style.PRIMARY, 54);
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) styleSettingsButtons(g.getChildAt(i));
        }
    }

    private void styleFinalStepButtons(View v) {
        if (v == null) return;
        if (v instanceof Button) {
            Button b = (Button) v;
            String s = String.valueOf(b.getText()).trim();
            if (matches(s, "Choose organizer", "اختيار منظم الكشف", "ڕێکخەر هەڵبژێرە")) {
                apply(b, "choose_organizer", Style.NAVY, 50);
            } else if (matches(s, "Save draft", "حفظ كمسودة", "وەک ڕەشنووس پاشەکەوت بکە")) {
                apply(b, "save_draft", Style.SOFT_BLUE, 48);
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) styleFinalStepButtons(g.getChildAt(i));
        }
    }

    private boolean matches(String actual, String a, String b, String c) {
        return actual.equals(a) || actual.equals(b) || actual.equals(c);
    }

    private enum Style { PRIMARY, NAVY, SOFT_BLUE, SOFT_AMBER, DANGER_OUTLINE }

    private void apply(Button b, String id, Style style, int heightDp) {
        String tag = "v490_" + id;
        if (tag.equals(String.valueOf(b.getTag()))) return;
        b.setTag(tag);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setGravity(android.view.Gravity.CENTER);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(14), 0, dp(14), 0);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            b.setStateListAnimator(null);
            b.setElevation(dp(style == Style.PRIMARY || style == Style.NAVY ? 2 : 0));
        }

        int normal, pressed, stroke, fg;
        switch (style) {
            case PRIMARY:
                normal = Color.rgb(20, 163, 119);
                pressed = Color.rgb(15, 137, 99);
                stroke = Color.rgb(20, 163, 119);
                fg = Color.WHITE;
                break;
            case NAVY:
                normal = Color.rgb(20, 57, 88);
                pressed = Color.rgb(14, 45, 70);
                stroke = Color.rgb(20, 57, 88);
                fg = Color.WHITE;
                break;
            case SOFT_AMBER:
                normal = Color.rgb(255, 249, 236);
                pressed = Color.rgb(250, 239, 212);
                stroke = Color.rgb(239, 216, 169);
                fg = Color.rgb(151, 98, 20);
                break;
            case DANGER_OUTLINE:
                normal = Color.rgb(255, 245, 245);
                pressed = Color.rgb(250, 228, 228);
                stroke = Color.rgb(239, 196, 196);
                fg = Color.rgb(181, 58, 58);
                break;
            case SOFT_BLUE:
            default:
                normal = Color.rgb(239, 246, 251);
                pressed = Color.rgb(222, 237, 247);
                stroke = Color.rgb(199, 219, 233);
                fg = Color.rgb(18, 50, 79);
                break;
        }
        b.setTextColor(fg);
        b.setBackground(stateBackground(normal, pressed, stroke, 16));

        ViewGroup.LayoutParams raw = b.getLayoutParams();
        if (raw instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) raw;
            lp.height = dp(heightDp);
            lp.topMargin = dp(5);
            lp.bottomMargin = dp(5);
            lp.leftMargin = dp(4);
            lp.rightMargin = dp(4);
            b.setLayoutParams(lp);
        }
    }

    private StateListDrawable stateBackground(int normal, int pressed, int stroke, int radiusDp) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, rounded(pressed, stroke, radiusDp));
        s.addState(new int[]{}, rounded(normal, stroke, radiusDp));
        return s;
    }

    private GradientDrawable rounded(int fill, int stroke, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        g.setStroke(dp(1), stroke);
        return g;
    }

    private int dp(int n) {
        return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
    }
}
