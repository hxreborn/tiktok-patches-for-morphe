package app.morphe.extension.tiktok.share;

import android.content.ClipData;
import android.content.ClipDescription;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.settings.BaseSettings;

/**
 * Best-effort share URL sanitizer.
 *
 * Removes common tracking/query parameters from TikTok-generated share links.
 * Uses simple parsing and is null-safe to prevent crashes.
 */
@SuppressWarnings("unused")
public final class ShareUrlSanitizer {

    private ShareUrlSanitizer() {}

    public static boolean shouldSanitize() {
        return BaseSettings.SANITIZE_SHARING_LINKS.get();
    }

    public static String sanitizeShareUrlIfEnabled(String url) {
        boolean enabled = shouldSanitize();
        if (BaseSettings.DEBUG.get()) {
            Logger.printInfo(() -> "[Morphe TikTok ShareProbe] post_shortening enabled=" + enabled +
                " value=" + describeUrl(url));
        }

        if (!enabled) return url;
        return sanitizeShareUrl(url);
    }

    public static String getSanitizedExternalShareUrlOrNull(String url) {
        boolean enabled = shouldSanitize();
        boolean externalTikTokUrl = isExternalTikTokUrl(url);
        if (BaseSettings.DEBUG.get()) {
            Logger.printInfo(() -> "[Morphe TikTok ShareProbe] pre_shortening enabled=" + enabled +
                " external=" + externalTikTokUrl + " value=" + describeUrl(url));
        }

        if (!enabled || !externalTikTokUrl) return null;

        String sanitizedUrl = sanitizeShareUrl(url);
        if (BaseSettings.DEBUG.get()) {
            Logger.printDebug(() -> "[Morphe TikTok Share] Using pre-shortening share URL");
        }
        return sanitizedUrl;
    }

    public static String getSanitizedShorteningUrlOrNull(String itemType, String source, String url) {
        boolean enabled = shouldSanitize();
        boolean externalTikTokUrl = isExternalTikTokUrl(url);
        if (BaseSettings.DEBUG.get()) {
            Logger.printInfo(() -> "[Morphe TikTok ShareProbe] shorten_request enabled=" + enabled +
                " external=" + externalTikTokUrl +
                " itemType=" + describeUrl(itemType) +
                " source=" + describeUrl(source) +
                " url=" + describeUrl(url));
        }

        if (!enabled || !externalTikTokUrl) return null;

        String sanitizedUrl = sanitizeShareUrl(url);
        if (BaseSettings.DEBUG.get()) {
            Logger.printInfo(() -> "[Morphe TikTok ShareProbe] shorten_request_result url=" + describeUrl(sanitizedUrl));
        }
        return sanitizedUrl;
    }

    public static ClipData sanitizeClipboardClipDataIfNeeded(ClipData clipData) {
        if (clipData == null) return null;

        try {
            CharSequence text = null;
            if (clipData.getItemCount() > 0 && clipData.getItemAt(0) != null) {
                text = clipData.getItemAt(0).getText();
            }

            String textString = text == null ? null : text.toString();
            boolean enabled = shouldSanitize();
            boolean externalTikTokUrl = isExternalTikTokUrl(textString);
            if (BaseSettings.DEBUG.get()) {
                Logger.printInfo(() -> "[Morphe TikTok ShareProbe] clipboard enabled=" + enabled +
                    " external=" + externalTikTokUrl + " value=" + describeUrl(textString));
            }

            if (!enabled || !externalTikTokUrl) return clipData;

            String sanitizedUrl = sanitizeShareUrl(textString);
            if (sanitizedUrl.equals(textString)) return clipData;

            ClipDescription description = clipData.getDescription();
            CharSequence label = description == null ? "" : description.getLabel();
            return ClipData.newPlainText(label, sanitizedUrl);
        } catch (Exception ex) {
            Logger.printInfo(() -> "[Morphe TikTok ShareProbe] clipboard probe failed", ex);
            return clipData;
        }
    }

    private static boolean isExternalTikTokUrl(String url) {
        if (url == null) return false;

        String lowerUrl = url.toLowerCase();
        if (!lowerUrl.startsWith("https://") && !lowerUrl.startsWith("http://")) return false;

        return lowerUrl.contains("://www.tiktok.com/") ||
            lowerUrl.contains("://m.tiktok.com/") ||
            lowerUrl.contains("://vm.tiktok.com/") ||
            lowerUrl.contains("://vt.tiktok.com/");
    }

    private static String describeUrl(String url) {
        if (url == null) return "null";
        if (url.isEmpty()) return "empty";

        String sanitized = url.replace('\n', ' ').replace('\r', ' ');
        int queryIndex = sanitized.indexOf('?');
        if (queryIndex >= 0) {
            sanitized = sanitized.substring(0, queryIndex) + "?<redacted>";
        }
        if (sanitized.length() > 160) {
            sanitized = sanitized.substring(0, 160) + "...";
        }

        return "\"" + sanitized + "\"";
    }

    private static final Set<String> TRACKING_KEYS = new HashSet<>(Arrays.asList(
        "utm_source",
        "utm_medium",
        "utm_campaign",
        "utm_term",
        "utm_content",
        "fbclid",
        "igshid",
        "share_link_id",
        "invitation_scene",
        "ref",
        "source",
        "si",
        "utm_id"
    ));

    public static String sanitizeShareUrl(String url) {
        if (url == null) return null;

        try {
            int qIndex = url.indexOf('?');
            if (qIndex < 0 || qIndex == url.length() - 1) return url;

            String base = url.substring(0, qIndex);
            if (isExternalTikTokUrl(url)) {
                if (BaseSettings.DEBUG.get() && !base.equals(url)) {
                    Logger.printDebug(() -> "[Morphe TikTok Share] Canonicalized TikTok share URL");
                }
                return base;
            }

            String query = url.substring(qIndex + 1);

            String[] parts = query.split("&");
            StringBuilder kept = new StringBuilder();

            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (part == null || part.isEmpty()) continue;

                int eq = part.indexOf('=');
                String key = (eq >= 0) ? part.substring(0, eq) : part;

                if (key != null && TRACKING_KEYS.contains(key)) {
                    continue;
                }

                if (kept.length() > 0) kept.append('&');
                kept.append(part);
            }

            String sanitizedUrl = kept.length() == 0 ? base : base + "?" + kept;
            if (BaseSettings.DEBUG.get() && !sanitizedUrl.equals(url)) {
                Logger.printDebug(() -> "[Morphe TikTok Share] Sanitized share URL");
            }
            return sanitizedUrl;
        } catch (Throwable ignored) {
            return url;
        }
    }
}

