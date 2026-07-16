/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from hxreborn/revanced-patches and upstreamed to ReVanced:
 * https://gitlab.com/ReVanced/revanced-patches/-/merge_requests/6941
 * Upstream commit 26562d59d2ad95543a3609cfed84c6accf739ab5 (2026-04-27),
 * extensions/tiktok/stub/src/main/java/com/ss/android/ugc/aweme/feed/AIGCInfo.java
 */
package com.ss.android.ugc.aweme.feed;

// Dummy class
public class AIGCInfo {
    public boolean getCreateByAI() {
        throw new UnsupportedOperationException("Stub");
    }

    /**
     * @return AI label type: 0 none, 1 creator declared, 2 TikTok detected
     */
    public int getAIGCLabelType() {
        throw new UnsupportedOperationException("Stub");
    }
}
