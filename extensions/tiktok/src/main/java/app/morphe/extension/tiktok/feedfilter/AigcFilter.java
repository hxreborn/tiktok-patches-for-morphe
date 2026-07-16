/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Additional terms under GPLv3 section 7:
 * - You must preserve reasonable legal notices and author attributions in this file.
 * - Modified versions must not misrepresent the origin of this file.
 *
 * Ported from hxreborn/revanced-patches and upstreamed to ReVanced:
 * https://gitlab.com/ReVanced/revanced-patches/-/merge_requests/6941
 * Upstream commit 26562d59d2ad95543a3609cfed84c6accf739ab5 (2026-04-27),
 * extensions/tiktok/src/main/java/app/revanced/extension/tiktok/feedfilter/AigcFilter.java
 *
 * Builds on the TikTok feed filter framework from ReVanced.
 */
package app.morphe.extension.tiktok.feedfilter;

import app.morphe.extension.tiktok.settings.Settings;
import com.ss.android.ugc.aweme.feed.AIGCInfo;
import com.ss.android.ugc.aweme.feed.model.Aweme;

public class AigcFilter implements IFilter {
    @Override
    public boolean getEnabled() {
        return Settings.HIDE_AI_GENERATED.get();
    }

    @Override
    public boolean getFiltered(Aweme item) {
        final AIGCInfo aigcInfo = item.getAigcInfo();
        if (aigcInfo == null) {
            return false;
        }

        return aigcInfo.getCreateByAI() || aigcInfo.getAIGCLabelType() != 0;
    }
}
