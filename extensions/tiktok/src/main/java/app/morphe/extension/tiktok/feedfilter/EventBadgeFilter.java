/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.extension.tiktok.feedfilter;

import app.morphe.extension.tiktok.settings.Settings;

public final class EventBadgeFilter {
    private EventBadgeFilter() {}

    public static boolean shouldHide() {
        return Settings.HIDE_EVENT_BADGE.get();
    }
}
