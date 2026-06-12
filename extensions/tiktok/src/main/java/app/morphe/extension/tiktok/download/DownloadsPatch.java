/*
 * Forked from:
 * https://github.com/ReVanced/revanced-patches/blob/377d4e15016296b45d809697f7f69bce74badd3a/extensions/tiktok/src/main/java/app/revanced/extension/tiktok/download/DownloadsPatch.java
 */

package app.morphe.extension.tiktok.download;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.tiktok.settings.Settings;
import com.ss.android.ugc.aweme.base.model.UrlModel;
import com.ss.android.ugc.aweme.feed.model.Video;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@SuppressWarnings("unused")
public class DownloadsPatch {
    private static volatile String lastLoggedPath;
    private static volatile Boolean lastLoggedRemoveWatermark;
    private static volatile String lastLoggedCleanSourceSignature;

    public static String getDownloadPath() {
        String path = Settings.DOWNLOAD_PATH.get();
        if (BaseSettings.DEBUG.get() && (lastLoggedPath == null || !lastLoggedPath.equals(path))) {
            lastLoggedPath = path;
            Logger.printInfo(() -> "[Morphe Downloads] download_path=\"" + path + "\"");
        }
        return path;
    }

    public static boolean shouldRemoveWatermark() {
        boolean removeWatermark = Settings.DOWNLOAD_WATERMARK.get();
        if (BaseSettings.DEBUG.get() && (lastLoggedRemoveWatermark == null || lastLoggedRemoveWatermark != removeWatermark)) {
            lastLoggedRemoveWatermark = removeWatermark;
            Logger.printInfo(() -> "[Morphe Downloads] remove_watermark=" + removeWatermark);
        }
        return removeWatermark;
    }

    public static void patchVideoObject(Video video) {
        if (video == null) return;

        try {
            Candidate selected = selectLargestCleanCandidate(video);
            if (selected == null || "downloadNoWatermarkAddr".equals(selected.name)) {
                return;
            }

            UrlModel original = video.downloadNoWatermarkAddr;
            video.downloadNoWatermarkAddr = selected.model;

            if (BaseSettings.DEBUG.get()) {
                String originalSummary = describeUrlModel(original);
                String selectedSummary = describeUrlModel(selected.model);
                String source = selected.name;
                String signature = source + '|' + originalSummary + '|' + selectedSummary;
                if (!signature.equals(lastLoggedCleanSourceSignature)) {
                    lastLoggedCleanSourceSignature = signature;
                    Logger.printInfo(() -> "[Morphe Downloads] selected clean download source"
                            + " original=" + originalSummary
                            + " source=" + source
                            + " replacement=" + selectedSummary);
                }
            }
        } catch (Throwable ex) {
            if (BaseSettings.DEBUG.get()) {
                Logger.printException(() -> "[Morphe Downloads] patchVideoObject failure", ex);
            }
        }
    }

    private static Candidate selectLargestCleanCandidate(Video video) {
        Candidate best = null;
        Candidate[] candidates = {
                new Candidate("downloadNoWatermarkAddr", video.downloadNoWatermarkAddr),
                new Candidate("playAddrBytevc1", getUrlModelSafe(video, "playAddrBytevc1", "getPlayAddrBytevc1")),
                new Candidate("h264PlayAddr", video.h264PlayAddr),
                new Candidate("playAddr", video.playAddr),
        };

        for (Candidate candidate : candidates) {
            if (!candidate.usable) {
                continue;
            }

            if (best == null || compareDownloadQuality(candidate, best) > 0) {
                best = candidate;
            }
        }

        return best;
    }

    private static int compareDownloadQuality(Candidate left, Candidate right) {
        int leftResolution = getResolutionMarker(left);
        int rightResolution = getResolutionMarker(right);
        if (leftResolution != rightResolution) {
            return leftResolution - rightResolution;
        }

        if (left.size != right.size) {
            return left.size > right.size ? 1 : -1;
        }

        return sourcePriority(left.name) - sourcePriority(right.name);
    }

    private static int sourcePriority(String name) {
        if ("h264PlayAddr".equals(name) || "playAddr".equals(name)) return 4;
        if ("downloadNoWatermarkAddr".equals(name)) return 3;
        if ("playAddrBytevc1".equals(name)) return 2;
        return 1;
    }

    private static boolean isUsableDownloadModel(UrlModel model) {
        if (model == null) {
            return false;
        }

        String uri = getUriSafe(model);
        if (uri == null || uri.trim().isEmpty() || "null".equalsIgnoreCase(uri.trim())) {
            return false;
        }

        return hasUsableUrl(model);
    }

    private static boolean hasUsableUrl(UrlModel model) {
        List<String> urls = getUrlListSafe(model);
        if (urls == null || urls.isEmpty()) {
            return false;
        }

        for (String url : urls) {
            if (url != null && !url.trim().isEmpty() && !"null".equalsIgnoreCase(url.trim())) {
                return true;
            }
        }

        return false;
    }

    private static String describeUrlModel(UrlModel model) {
        if (model == null) {
            return "null";
        }

        List<String> urls = getUrlListSafe(model);
        int urlCount = urls == null ? -1 : urls.size();
        return "{class=" + model.getClass().getName()
                + ",uri=" + getUriSafe(model)
                + ",urlKey=" + getUrlKeySafe(model)
                + ",size=" + getSizeSafe(model)
                + ",urlCount=" + urlCount
                + ",firstUrl=" + redactUrl(firstUrl(urls)) + "}";
    }

    private static List<String> getUrlListSafe(UrlModel model) {
        try {
            return model.getUrlList();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String getUriSafe(UrlModel model) {
        try {
            return model.getUri();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String getUrlKeySafe(UrlModel model) {
        try {
            return model.getUrlKey();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static long getSizeSafe(UrlModel model) {
        try {
            return model.getSize();
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static UrlModel getUrlModelSafe(Object instance, String fieldName, String getterName) {
        UrlModel fieldValue = getUrlModelFieldSafe(instance, fieldName);
        return fieldValue != null ? fieldValue : getUrlModelMethodSafe(instance, getterName);
    }

    private static UrlModel getUrlModelFieldSafe(Object instance, String fieldName) {
        if (instance == null) {
            return null;
        }

        try {
            Class<?> clazz = instance.getClass();
            while (clazz != null) {
                try {
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(instance);
                    return value instanceof UrlModel ? (UrlModel) value : null;
                } catch (NoSuchFieldException ignored) {
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static UrlModel getUrlModelMethodSafe(Object instance, String methodName) {
        if (instance == null) {
            return null;
        }

        try {
            Method method = instance.getClass().getMethod(methodName);
            Object value = method.invoke(instance);
            return value instanceof UrlModel ? (UrlModel) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int getResolutionMarker(Candidate candidate) {
        if (candidate == null || candidate.model == null) {
            return 0;
        }

        int resolution = getResolutionMarker(getUriSafe(candidate.model));
        if (resolution > 0) {
            return resolution;
        }

        return getResolutionMarker(getUrlKeySafe(candidate.model));
    }

    private static int getResolutionMarker(String text) {
        if (text == null) {
            return 0;
        }

        int best = 0;
        for (int index = 1; index < text.length(); index++) {
            if (text.charAt(index) != 'p') {
                continue;
            }

            int start = index - 1;
            while (start >= 0 && Character.isDigit(text.charAt(start))) {
                start--;
            }

            if (start == index - 1) {
                continue;
            }

            try {
                int resolution = Integer.parseInt(text.substring(start + 1, index));
                if (resolution > best) {
                    best = resolution;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return best;
    }

    private static String firstUrl(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return null;
        }

        return urls.get(0);
    }

    private static String redactUrl(String url) {
        if (url == null) {
            return null;
        }

        int queryIndex = url.indexOf('?');
        String withoutQuery = queryIndex >= 0 ? url.substring(0, queryIndex) : url;
        return withoutQuery.length() <= 96 ? withoutQuery : withoutQuery.substring(0, 96) + "...";
    }

    private static final class Candidate {
        final String name;
        final UrlModel model;
        final boolean usable;
        final long size;

        Candidate(String name, UrlModel model) {
            this.name = name;
            this.model = model;
            this.usable = isUsableDownloadModel(model);
            this.size = getSizeSafe(model);
        }
    }
}
