/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.extension.tiktok.feedfilter;

import app.morphe.extension.tiktok.settings.Settings;

@SuppressWarnings("unused")
public final class PlaylistBarFilter {
    private PlaylistBarFilter() {}

    public static boolean shouldHide() {
        return Settings.HIDE_PLAYLIST_BAR.get();
    }
}
