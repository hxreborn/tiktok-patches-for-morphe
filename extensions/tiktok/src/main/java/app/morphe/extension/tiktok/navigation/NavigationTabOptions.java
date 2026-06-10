package app.morphe.extension.tiktok.navigation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class NavigationTabOptions {
    public static final String HOT = "HOT";
    public static final String EXPLORE = "EXPLORE";
    public static final String FOLLOWING = "FOLLOWING";
    public static final String MALL = "MALL";
    public static final String NEARBY = "NEARBY";
    public static final String FRIENDS = "FRIENDS";
    public static final String LIVE = "LIVE";
    public static final String POPULAR = "POPULAR";
    public static final String STEM = "STEM";
    public static final String SERIES = "SERIES";

    private static final String RAW_PREFIX = "RAW:";

    public static final Option[] OPTIONS = {
            new Option(HOT, "For You"),
            new Option(EXPLORE, "Explore"),
            new Option(FOLLOWING, "Following"),
            new Option(MALL, "Shop"),
            new Option(NEARBY, "Nearby / Local"),
            new Option(FRIENDS, "Friends feed tab"),
            new Option(LIVE, "Live"),
            new Option(POPULAR, "Popular"),
            new Option(STEM, "STEM feed"),
            new Option(SERIES, "Drama / Series"),
    };

    private NavigationTabOptions() {
    }

    public static String defaultEnabledKeys() {
        StringBuilder builder = new StringBuilder();
        for (Option option : OPTIONS) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(option.key);
        }
        return builder.toString();
    }

    public static Set<String> parseEnabledKeys(String keys) {
        return parseKeys(keys, true);
    }

    public static Set<String> parseObservedKeys(String keys) {
        return parseKeys(keys, false);
    }

    private static Set<String> parseKeys(String keys, boolean forceHot) {
        LinkedHashSet<String> parsed = new LinkedHashSet<>();
        if (keys != null) {
            for (String key : keys.split(",")) {
                String normalized = normalizeSettingKey(key);
                if (normalized != null) {
                    parsed.add(normalized);
                }
            }
        }

        if (forceHot) {
            parsed.add(HOT);
        }
        return parsed;
    }

    public static String serializeEnabledKeys(Set<String> keys) {
        StringBuilder builder = new StringBuilder();
        for (Option option : OPTIONS) {
            appendKey(builder, keys, option.key);
        }

        for (String key : keys) {
            if (findOption(key) == null) {
                appendKey(builder, keys, key);
            }
        }
        return builder.toString();
    }

    public static List<Option> optionsForKeys(Set<String> keys) {
        ArrayList<Option> options = new ArrayList<>();
        for (Option option : OPTIONS) {
            if (keys.contains(option.key)) {
                options.add(option);
            }
        }

        for (String key : keys) {
            if (findOption(key) == null) {
                options.add(new Option(key, rawLabel(key)));
            }
        }

        return options;
    }

    public static String normalizeRuntimeTag(String tag) {
        String known = normalizeKnownRuntimeTag(tag);
        if (known != null) {
            return known;
        }

        String raw = normalizeRawRuntimeTag(tag);
        return raw == null ? null : RAW_PREFIX + raw;
    }

    public static boolean isKnownKey(String key) {
        return findOption(key) != null;
    }

    private static String normalizeKnownRuntimeTag(String tag) {
        if (tag == null) {
            return null;
        }

        String normalized = tag.trim().toLowerCase(Locale.US);
        switch (normalized) {
            case "for you":
            case "hot":
            case "homepage_hot":
                return HOT;
            case "explore":
            case "homepage_explore":
                return EXPLORE;
            case "following":
            case "homepage_follow":
            case "homepage_following":
                return FOLLOWING;
            case "shop":
            case "mall":
            case "homepage_shop":
            case "homepage_shop_mall":
                return MALL;
            case "nearby":
            case "local":
            case "homepage_nearby":
                return NEARBY;
            case "friends":
            case "friends_feed":
            case "homepage_friends":
                return FRIENDS;
            case "live":
            case "homepage_live":
                return LIVE;
            case "popular":
            case "homepage_popular":
                return POPULAR;
            case "stem":
            case "homepage_stem":
            case "homepage_topic_stem":
                return STEM;
            case "drama":
            case "series":
            case "homepage_series":
                return SERIES;
            default:
                return null;
        }
    }

    public static Option findOption(String key) {
        String normalized = normalizeSettingKey(key);
        if (normalized == null) {
            return null;
        }

        for (Option option : OPTIONS) {
            if (option.key.equals(normalized)) {
                return option;
            }
        }
        return null;
    }

    private static String normalizeSettingKey(String key) {
        if (key == null) {
            return null;
        }

        String normalized = key.trim().toUpperCase(Locale.US);
        for (Option option : OPTIONS) {
            if (option.key.equals(normalized)) {
                return option.key;
            }
        }

        if (normalized.startsWith(RAW_PREFIX)) {
            String raw = normalizeRawRuntimeTag(normalized.substring(RAW_PREFIX.length()));
            return raw == null ? null : RAW_PREFIX + raw;
        }
        return null;
    }

    private static String normalizeRawRuntimeTag(String tag) {
        if (tag == null) {
            return null;
        }

        String normalized = tag.trim().toLowerCase(Locale.US)
                .replace(',', ' ')
                .replace('\n', ' ')
                .replace('\r', ' ');
        while (normalized.contains("  ")) {
            normalized = normalized.replace("  ", " ");
        }

        return normalized.isEmpty() ? null : normalized;
    }

    private static String rawLabel(String key) {
        if (key == null || !key.startsWith(RAW_PREFIX)) {
            return key == null ? "Unknown tab" : key;
        }

        String raw = key.substring(RAW_PREFIX.length()).replace('_', ' ');
        if (raw.isEmpty()) {
            return "Unknown tab";
        }

        return raw.substring(0, 1).toUpperCase(Locale.US) + raw.substring(1);
    }

    private static void appendKey(StringBuilder builder, Set<String> keys, String key) {
        if (!HOT.equals(key) && !keys.contains(key)) {
            return;
        }

        if (builder.length() > 0) {
            builder.append(',');
        }
        builder.append(key);
    }

    public static final class Option {
        public final String key;
        public final String label;

        private Option(String key, String label) {
            this.key = key;
            this.label = label;
        }
    }
}
