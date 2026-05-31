/*
 * Forked from:
 * https://github.com/ReVanced/revanced-patches/blob/377d4e15016296b45d809697f7f69bce74badd3a/extensions/tiktok/src/main/java/app/revanced/extension/tiktok/settings/preference/categories/ExtensionPreferenceCategory.java
 */

package app.morphe.extension.tiktok.settings.preference.categories;

import android.content.Context;
import android.preference.PreferenceScreen;

import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.shared.settings.preference.ClearLogBufferPreference;
import app.morphe.extension.shared.settings.preference.ExportLogToClipboardPreference;
import app.morphe.extension.tiktok.settings.preference.MorpheTikTokAboutPreference;
import app.morphe.extension.tiktok.settings.preference.TogglePreference;

@SuppressWarnings("deprecation")
public class ExtensionPreferenceCategory extends ConditionalPreferenceCategory {
    public ExtensionPreferenceCategory(Context context, PreferenceScreen screen) {
        super(context, screen);
        setTitle("Miscellaneous");
    }

    @Override
    public boolean getSettingsStatus() {
        return true;
    }

    @Override
    public void addPreferences(Context context) {
        addPreference(new MorpheTikTokAboutPreference(context));

        addPreference(new TogglePreference(
                context,
                "Sanitize sharing links",
                "Remove tracking parameters from shared links.",
                BaseSettings.SANITIZE_SHARING_LINKS
        ));

        addPreference(new TogglePreference(
                context,
                "Enable debug log",
                "Only enable when recording logs to report an issue. Leaving it on for too long can make TikTok feel laggy and may lead to crashes.",
                BaseSettings.DEBUG
        ));

        var exportLogs = new ExportLogToClipboardPreference(context);
        exportLogs.setTitle("Export debug logs");
        exportLogs.setSummary("Copy Morphe debug logs to clipboard.");
        addPreference(exportLogs);

        var clearLogs = new ClearLogBufferPreference(context);
        clearLogs.setTitle("Clear debug logs");
        clearLogs.setSummary("Clear stored Morphe debug logs.");
        addPreference(clearLogs);
    }
}

