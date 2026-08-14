package com.infinitygreenpower.solarform;

import android.os.Bundle;
import android.widget.Toast;

/** Explicit launcher for v6.3.1 so Android update state is unambiguous. */
public class OrganizerRebuildV631Activity extends OrganizerRebuildV63Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        String key = "shown_version_6_3_1";
        if (!getSharedPreferences("offer_studio_settings", MODE_PRIVATE).getBoolean(key, false)) {
            Toast.makeText(this, "IGP Organizer Form v6.3.1 installed", Toast.LENGTH_LONG).show();
            getSharedPreferences("offer_studio_settings", MODE_PRIVATE).edit().putBoolean(key, true).apply();
        }
    }
}
