package app.morphe.extension.tiktok.navigation;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.tiktok.settings.Settings;

public final class NavigationTabsFilter {
    private static String lastDebugSignature;
    private static String lastObservedSignature;

    private NavigationTabsFilter() {
    }

    @SuppressWarnings({"unused", "rawtypes", "unchecked"})
    public static List<?> filterTopTabs(List<?> tabs, boolean includeChildren) {
        try {
            if (tabs == null || includeChildren) {
                return tabs;
            }

            Set<String> previousObservedKeys = NavigationTabOptions.parseObservedKeys(
                    Settings.FEED_NAVIGATION_OBSERVED_TABS.get()
            );
            observeTabs(tabs, previousObservedKeys);

            if (!Settings.FEED_NAVIGATION.get()) {
                debugTabs("observed", tabs, tabs);
                return tabs;
            }

            Set<String> enabledKeys = NavigationTabOptions.parseEnabledKeys(Settings.FEED_NAVIGATION_TABS.get());
            Set<String> observedKeys = NavigationTabOptions.parseObservedKeys(
                    Settings.FEED_NAVIGATION_OBSERVED_TABS.get()
            );
            boolean blockNewTabs = Settings.FEED_NAVIGATION_BLOCK_NEW_TABS.get();
            CopyOnWriteArrayList filtered = new CopyOnWriteArrayList();
            Object hotTab = null;

            for (Object tab : tabs) {
                String key = NavigationTabOptions.normalizeRuntimeTag(getTag(tab));
                if (NavigationTabOptions.HOT.equals(key)) {
                    hotTab = tab;
                }

                if (shouldKeepTab(key, enabledKeys, previousObservedKeys, observedKeys, blockNewTabs)) {
                    filtered.add(tab);
                }
            }

            if (filtered.isEmpty()) {
                if (hotTab != null) {
                    filtered.add(hotTab);
                } else {
                    debugTabs("fallback-original", tabs, tabs);
                    return tabs;
                }
            }

            debugTabs("filtered", tabs, filtered);
            return filtered;
        } catch (Throwable throwable) {
            Logger.printException(() -> "Feed tab navigation failed; returning original tabs", throwable);
            return tabs;
        }
    }

    private static boolean shouldKeepTab(
            String key,
            Set<String> enabledKeys,
            Set<String> previousObservedKeys,
            Set<String> observedKeys,
            boolean blockNewTabs
    ) {
        if (key == null) {
            return !blockNewTabs;
        }

        if (NavigationTabOptions.HOT.equals(key) || enabledKeys.contains(key)) {
            return true;
        }

        boolean wasAlreadyObserved = previousObservedKeys.contains(key);
        boolean isNewlyObserved = observedKeys.contains(key) && !wasAlreadyObserved;
        return isNewlyObserved && !blockNewTabs;
    }

    private static void observeTabs(List<?> tabs, Set<String> previousObservedKeys) {
        LinkedHashSet<String> observedKeys = new LinkedHashSet<>(previousObservedKeys);
        LinkedHashSet<String> newlyObservedKeys = new LinkedHashSet<>();
        for (Object tab : tabs) {
            String key = NavigationTabOptions.normalizeRuntimeTag(getTag(tab));
            if (key != null && observedKeys.add(key)) {
                newlyObservedKeys.add(key);
            }
        }

        String signature = NavigationTabOptions.serializeEnabledKeys(observedKeys);
        if (!signature.equals(lastObservedSignature)
                && !signature.equals(Settings.FEED_NAVIGATION_OBSERVED_TABS.get())) {
            Settings.FEED_NAVIGATION_OBSERVED_TABS.save(signature);
            lastObservedSignature = signature;
        }

        if (!newlyObservedKeys.isEmpty() && !Settings.FEED_NAVIGATION_BLOCK_NEW_TABS.get()) {
            Set<String> enabledKeys = NavigationTabOptions.parseEnabledKeys(Settings.FEED_NAVIGATION_TABS.get());
            if (enabledKeys.addAll(newlyObservedKeys)) {
                Settings.FEED_NAVIGATION_TABS.save(NavigationTabOptions.serializeEnabledKeys(enabledKeys));
            }
        }
    }

    private static String getTag(Object tab) {
        if (tab == null) {
            return null;
        }

        try {
            Method method = tab.getClass().getMethod("tag");
            Object value = method.invoke(tab);
            return value instanceof String ? (String) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void debugTabs(String state, List<?> original, List<?> filtered) {
        if (!BaseSettings.DEBUG.get()) {
            return;
        }

        String signature = state + " original=" + describe(original) + " filtered=" + describe(filtered);
        if (signature.equals(lastDebugSignature)) {
            return;
        }

        lastDebugSignature = signature;
        Logger.printDebug(() -> "Feed tab navigation: " + signature);
    }

    private static String describe(List<?> tabs) {
        if (tabs == null) {
            return "null";
        }

        StringBuilder builder = new StringBuilder();
        builder.append(tabs.size()).append('[');
        for (int i = 0; i < tabs.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }

            String tag = getTag(tabs.get(i));
            String key = NavigationTabOptions.normalizeRuntimeTag(tag);
            builder.append(tag == null ? "unknown" : tag);
            if (key != null) {
                builder.append('/').append(key);
            }
        }
        return builder.append(']').toString();
    }
}
