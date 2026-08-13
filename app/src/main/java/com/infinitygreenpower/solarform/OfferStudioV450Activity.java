package com.infinitygreenpower.solarform;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * IGP Offer Studio v4.5.0
 * Reliable Step-4 fixed-note activation on top of the stable v4.4 layer.
 *
 * v4.4 already contains the structured note builder, but its one-time
 * decorator can run before the Notes field is fully attached on some phones.
 * This class watches only for screen-root changes and invokes that existing
 * builder once when Step 4 is actually present. No global-layout scanning.
 */
public class OfferStudioV450Activity extends OfferStudioV440Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int lastRootIdentity = 0;
    private boolean fixedNoteReady = false;
    private boolean running = false;

    private final Runnable noteWatcher = new Runnable() {
        @Override public void run() {
            if (!running) return;
            try {
                LinearLayout content = content();
                int identity = 0;
                if (content != null && content.getChildCount() > 0) {
                    identity = System.identityHashCode(content.getChildAt(0));
                }
                if (identity != lastRootIdentity) {
                    lastRootIdentity = identity;
                    fixedNoteReady = false;
                }

                if (wizardStep() == 3 && !fixedNoteReady) {
                    View root = findViewById(android.R.id.content);
                    if (root != null) {
                        if (!hasTag(root, "v440_note_builder")) {
                            invokeStructuredNote(root);
                        }
                        View builder = findTagged(root, "v440_note_builder");
                        if (builder != null) {
                            fixedNoteReady = true;
                            localizeSystemType(builder);
                        }
                    }
                }
            } catch (Exception ignored) { }
            handler.postDelayed(this, 300);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        running = true;
        handler.postDelayed(noteWatcher, 250);
    }

    @Override protected void onResume() {
        super.onResume();
        if (!running) {
            running = true;
            handler.postDelayed(noteWatcher, 200);
        }
    }

    @Override protected void onPause() {
        running = false;
        handler.removeCallbacks(noteWatcher);
        super.onPause();
    }

    @Override protected void onDestroy() {
        running = false;
        handler.removeCallbacks(noteWatcher);
        super.onDestroy();
    }

    private LinearLayout content() {
        try {
            Field f = OfferStudioActivity.class.getDeclaredField("content");
            f.setAccessible(true);
            Object o = f.get(this);
            return o instanceof LinearLayout ? (LinearLayout)o : null;
        } catch (Exception e) {
            return null;
        }
    }

    private int wizardStep() {
        try {
            Field f = OfferStudioActivity.class.getDeclaredField("wizardStep");
            f.setAccessible(true);
            return f.getInt(this);
        } catch (Exception e) {
            return -1;
        }
    }

    private void invokeStructuredNote(View root) {
        try {
            Method m = OfferStudioV440Activity.class.getDeclaredMethod("installStructuredNote", View.class);
            m.setAccessible(true);
            m.invoke(this, root);
        } catch (Exception ignored) { }
    }

    private boolean hasTag(View v, String tag) {
        if (v == null) return false;
        if (tag.equals(String.valueOf(v.getTag()))) return true;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup)v;
            for (int i = 0; i < g.getChildCount(); i++) {
                if (hasTag(g.getChildAt(i), tag)) return true;
            }
        }
        return false;
    }

    private View findTagged(View v, String tag) {
        if (v == null) return null;
        if (tag.equals(String.valueOf(v.getTag()))) return v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup)v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View r = findTagged(g.getChildAt(i), tag);
                if (r != null) return r;
            }
        }
        return null;
    }

    private void localizeSystemType(View builder) {
        String lang = getSharedPreferences("offer_studio_settings", MODE_PRIVATE).getString("lang", "en");
        if ("en".equals(lang)) return;
        localizeEditTexts(builder, lang);
    }

    private void localizeEditTexts(View v, String lang) {
        if (v instanceof EditText) {
            EditText e = (EditText)v;
            String s = e.getText().toString().trim();
            String replacement = null;
            if ("ar".equals(lang)) {
                if (s.equalsIgnoreCase("Hybrid")) replacement = "هجين";
                else if (s.equalsIgnoreCase("On-Grid") || s.equalsIgnoreCase("On Grid")) replacement = "أون كريد";
                else if (s.equalsIgnoreCase("Off-Grid") || s.equalsIgnoreCase("Off Grid")) replacement = "أوف كريد";
            } else if ("ku".equals(lang)) {
                if (s.equalsIgnoreCase("Hybrid")) replacement = "هایبرید";
                else if (s.equalsIgnoreCase("On-Grid") || s.equalsIgnoreCase("On Grid")) replacement = "ئۆن‌گرید";
                else if (s.equalsIgnoreCase("Off-Grid") || s.equalsIgnoreCase("Off Grid")) replacement = "ئۆف‌گرید";
            }
            if (replacement != null && !replacement.equals(s)) e.setText(replacement);
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup)v;
            for (int i = 0; i < g.getChildCount(); i++) localizeEditTexts(g.getChildAt(i), lang);
        }
    }
}
