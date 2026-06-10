/*
 * Forked from:
 * https://github.com/ReVanced/revanced-patches/blob/377d4e15016296b45d809697f7f69bce74badd3a/extensions/tiktok/src/main/java/app/revanced/extension/tiktok/settings/SettingsStatus.java
 */

package app.morphe.extension.tiktok.settings;

public class SettingsStatus {
    public static boolean feedFilterEnabled = false;
    public static boolean feedNavigationEnabled = false;
    public static boolean commentTranslationEnabled = false;
    public static boolean downloadEnabled = false;
    public static boolean simSpoofEnabled = false;

    public static void enableFeedFilter() {
        feedFilterEnabled = true;
    }

    public static void enableFeedNavigation() {
        feedNavigationEnabled = true;
    }

    public static void enableCommentTranslation() {
        commentTranslationEnabled = true;
    }

    public static void enableDownload() {
        downloadEnabled = true;
    }

    public static void enableSimSpoof() {
        simSpoofEnabled = true;
    }

    public static void load() {
    }
}

