package app.morphe.extension.tiktok.feedfilter;

import app.morphe.extension.tiktok.settings.Settings;

public final class PlaylistBarFilter {
    private PlaylistBarFilter() {}

    public static boolean shouldHide() {
        return Settings.HIDE_PLAYLIST_BAR.get();
    }
}
