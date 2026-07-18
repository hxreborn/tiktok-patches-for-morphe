/*
 * Forked from:
 * https://github.com/ReVanced/revanced-patches/blob/377d4e15016296b45d809697f7f69bce74badd3a/extensions/tiktok/src/main/java/app/revanced/extension/tiktok/settings/preference/categories/DownloadsPreferenceCategory.java
 */

package app.morphe.extension.tiktok.settings.preference.categories;

import android.content.Context;
import android.preference.PreferenceScreen;

import app.morphe.extension.tiktok.settings.Settings;
import app.morphe.extension.tiktok.settings.SettingsStatus;
import app.morphe.extension.tiktok.settings.preference.DownloadPathPreference;
import app.morphe.extension.tiktok.settings.preference.NumberInputPreference;
import app.morphe.extension.tiktok.settings.preference.TogglePreference;

@SuppressWarnings("deprecation")
public class DownloadsPreferenceCategory extends ConditionalPreferenceCategory {
    public DownloadsPreferenceCategory(Context context, PreferenceScreen screen) {
        super(context, screen);
        setTitle("Downloads");
    }

    @Override
    public boolean getSettingsStatus() {
        return SettingsStatus.downloadEnabled;
    }

    @Override
    public void addPreferences(Context context) {
        addPreference(new DownloadPathPreference(
                context,
                "Video download path",
                Settings.DOWNLOAD_PATH
        ));
        addPreference(new DownloadPathPreference(
                context,
                "Image download path",
                Settings.IMAGE_DOWNLOAD_PATH
        ));
        addPreference(new TogglePreference(
                context,
                "Remove watermark",
                "Apply to video downloads and image downloads.",
                Settings.DOWNLOAD_WATERMARK
        ));
        addPreference(new TogglePreference(
                context,
                "Custom offline videos",
                "Adds a custom option to TikTok's offline videos menu after restart.",
                Settings.CUSTOM_OFFLINE_VIDEOS
        ));
        addPreference(new NumberInputPreference(
                context,
                "Offline videos limit",
                "Choose 201-500 videos. Restart TikTok after saving.",
                Settings.CUSTOM_OFFLINE_VIDEO_LIMIT,
                201,
                500
        ));

    }
}

