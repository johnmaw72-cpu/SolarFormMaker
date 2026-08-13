package com.infinitygreenpower.solarform;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * IGP Organizer Form v4.8.0
 * - Bundles the supplied Infinity Green Power logo as the automatic default PDF logo.
 * - Keeps user-selected custom logos as an override.
 * - Restyles Saved Forms actions to match the compact item-card actions.
 */
public class OfferStudioV480Activity extends OfferStudioV470Activity {
    private final Handler ui480 = new Handler(Looper.getMainLooper());
    private boolean running480 = false;

    private final Runnable savedStyler = new Runnable() {
        @Override public void run() {
            if (!running480) return;
            try {
                View root = findViewById(android.R.id.content);
                if (isSavedFormsScreen(root)) styleSavedActions(root);
            } catch (Exception ignored) { }
            ui480.postDelayed(this, 320);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        installBundledLogoIfNeeded();
        running480 = true;
        ui480.postDelayed(savedStyler, 350);
    }

    @Override protected void onResume() {
        super.onResume();
        installBundledLogoIfNeeded();
        if (!running480) {
            running480 = true;
            ui480.postDelayed(savedStyler, 250);
        }
    }

    @Override protected void onPause() {
        running480 = false;
        ui480.removeCallbacks(savedStyler);
        super.onPause();
    }

    @Override protected void onDestroy() {
        running480 = false;
        ui480.removeCallbacks(savedStyler);
        super.onDestroy();
    }

    private void installBundledLogoIfNeeded() {
        SharedPreferences p = getSharedPreferences("offer_studio_settings", MODE_PRIVATE);
        if (p.getBoolean("igp_default_logo_initialized_v480", false)) return;

        String existing = p.getString("logo_uri", "");
        if (existing != null && !existing.trim().isEmpty()) {
            p.edit().putBoolean("igp_default_logo_initialized_v480", true).apply();
            return;
        }

        try {
            InputStream in = getAssets().open("igp_default_logo.b64");
            ByteArrayOutputStream text = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) > 0) text.write(buffer, 0, n);
            in.close();

            byte[] image = Base64.decode(text.toByteArray(), Base64.DEFAULT);
            File logo = new File(getFilesDir(), "igp_default_company_logo.jpg");
            FileOutputStream out = new FileOutputStream(logo);
            out.write(image);
            out.flush();
            out.close();

            p.edit()
                    .putString("logo_uri", Uri.fromFile(logo).toString())
                    .putBoolean("pdf_show_logo", true)
                    .putBoolean("igp_default_logo_initialized_v480", true)
                    .apply();
        } catch (Exception ignored) {
            // Do not block app startup if the bundled logo cannot be restored.
        }
    }

    private boolean isSavedFormsScreen(View root) {
        return containsExactText(root, "Saved Forms")
                || containsExactText(root, "الاستمارات المحفوظة")
                || containsExactText(root, "فۆرمە پاشەکەوتکراوەکان");
    }

    private boolean containsExactText(View v, String wanted) {
        if (v == null) return false;
        if (v instanceof TextView && wanted.equals(String.valueOf(((TextView) v).getText()).trim())) return true;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                if (containsExactText(g.getChildAt(i), wanted)) return true;
            }
        }
        return false;
    }

    private void styleSavedActions(View v) {
        if (v == null) return;
        if (v instanceof Button) {
            Button b = (Button) v;
            String s = String.valueOf(b.getText()).trim();
            if (isPreview(s)) styleAction(b, "preview");
            else if (isEdit(s)) styleAction(b, "edit");
            else if (isDuplicate(s)) styleAction(b, "duplicate");
            else if (isDelete(s)) styleAction(b, "delete");
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) styleSavedActions(g.getChildAt(i));
        }
    }

    private boolean isPreview(String s) { return s.equals("Preview") || s.equals("معاينة") || s.equals("پێشبینین"); }
    private boolean isEdit(String s) { return s.equals("Edit") || s.equals("تعديل") || s.equals("دەستکاری"); }
    private boolean isDuplicate(String s) { return s.equals("Duplicate") || s.equals("نسخ") || s.equals("دووبارە"); }
    private boolean isDelete(String s) { return s.equals("Delete") || s.equals("حذف") || s.equals("سڕینەوە"); }

    private void styleAction(Button b, String kind) {
        String tag = "v480_saved_" + kind;
        if (tag.equals(String.valueOf(b.getTag()))) return;
        b.setTag(tag);
        b.setAllCaps(false);
        b.setTextSize(12);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinimumHeight(0);
        b.setPadding(dp480(5), 0, dp480(5), 0);
        b.setElevation(0);
        if (android.os.Build.VERSION.SDK_INT >= 21) b.setStateListAnimator(null);

        int fg;
        int fill;
        int stroke;
        if ("duplicate".equals(kind)) {
            fg = Color.rgb(20, 153, 112);
            fill = Color.rgb(238, 248, 244);
            stroke = Color.rgb(197, 229, 216);
        } else if ("delete".equals(kind)) {
            fg = Color.rgb(184, 61, 61);
            fill = Color.rgb(255, 240, 240);
            stroke = Color.rgb(245, 208, 208);
        } else if ("preview".equals(kind)) {
            fg = Color.rgb(18, 50, 79);
            fill = Color.rgb(235, 243, 250);
            stroke = Color.rgb(196, 216, 232);
        } else {
            fg = Color.rgb(18, 50, 79);
            fill = Color.rgb(241, 246, 250);
            stroke = Color.rgb(209, 222, 233);
        }
        b.setTextColor(fg);
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp480(13));
        g.setStroke(dp480(1), stroke);
        b.setBackground(g);

        ViewGroup.LayoutParams raw = b.getLayoutParams();
        if (raw instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) raw;
            lp.height = dp480(40);
            lp.leftMargin = dp480(3);
            lp.rightMargin = dp480(3);
            lp.topMargin = 0;
            lp.bottomMargin = 0;
            b.setLayoutParams(lp);
        }
    }

    private int dp480(int n) {
        return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
    }
}
