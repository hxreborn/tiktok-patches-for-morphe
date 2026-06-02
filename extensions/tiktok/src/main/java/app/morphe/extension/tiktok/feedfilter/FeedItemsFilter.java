package app.morphe.extension.tiktok.feedfilter;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.tiktok.settings.Settings;
import com.ss.android.ugc.aweme.feed.model.Aweme;
import com.ss.android.ugc.aweme.feed.model.AwemeStatistics;
import com.ss.android.ugc.aweme.feed.model.FeedItemList;
import com.ss.android.ugc.aweme.follow.presenter.FollowFeed;
import com.ss.android.ugc.aweme.follow.presenter.FollowFeedList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class FeedItemsFilter {
    private static final List<IFilter> FILTERS = List.of(
        new AdsFilter(),
        new LiveFilter(),
        new StoryFilter(),
        new ImageVideoFilter(),
        new ViewCountFilter(),
        new LikeCountFilter(),
        new ShopFilter()
    );

    private static final int MAX_NULL_ITEMS_LOGS = 3;
    private static final int MAX_BATCH_LOGS = 10;
    private static final int MAX_ITEM_LOGS = 50;
    private static final AtomicInteger feedItemListNullItemsLogCount = new AtomicInteger();
    private static final AtomicInteger followFeedListNullItemsLogCount = new AtomicInteger();
    private static final AtomicInteger batchLogCount = new AtomicInteger();
    private static final AtomicInteger itemLogCount = new AtomicInteger();

    private FeedItemsFilter() {}

    public static void filter(FeedItemList feedItemList) {
        boolean verbose = BaseSettings.DEBUG.get();

        if (feedItemList == null || feedItemList.items == null) {
            if (verbose) {
                logNullItems("FeedItemList", feedItemListNullItemsLogCount);
            }
            return;
        }

        if (verbose && shouldLogBatch()) {
            debugLogBatch("FeedItemList", feedItemList.items);
        }

        filterFeedList(
            "FeedItemList",
            feedItemList.items,
            container -> (container instanceof Aweme) ? (Aweme) container : null,
            verbose
        );
    }

    public static void filter(FollowFeedList followFeedList) {
        boolean verbose = BaseSettings.DEBUG.get();

        if (followFeedList == null || followFeedList.mItems == null) {
            if (verbose) {
                logNullItems("FollowFeedList", followFeedListNullItemsLogCount);
            }
            return;
        }

        if (verbose && shouldLogBatch()) {
            debugLogBatch("FollowFeedList", followFeedList.mItems);
        }

        filterFeedList(
            "FollowFeedList",
            followFeedList.mItems,
            container -> (container instanceof FollowFeed) ? ((FollowFeed) container).aweme : null,
            verbose
        );
    }

    private static void filterFeedList(
        String source,
        List list,
        AwemeExtractor extractor,
        boolean verbose
    ) {
        if (list == null) return;

        List<IFilter> activeFilters = getActiveFilters();
        if (activeFilters.isEmpty()) return;

        int initialSize = list.size();
        int removed = 0;

        // Could be simplified with removeIf() but requires Android 7.0+ while TikTok supports 4.0+.
        Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
            Object container = iterator.next();
            Aweme item = extractor.extract(container);
            if (item == null) {
                continue;
            }

            String reason = getFilterReason(activeFilters, item);
            logItem(item, reason, verbose);

            if (reason != null) {
                removed++;
                iterator.remove();
            }
        }

        if (verbose && removed > 0 && shouldLogBatch()) {
            int removedFinal = removed;
            Logger.printInfo(() -> "[Morphe TikTok FeedFilter] filter(" + source + "): size "
                + initialSize + " -> " + list.size() + " (removed=" + removedFinal + ")");
        }
    }

    private static boolean shouldFilter(Aweme item) {
        return getFilterReason(getActiveFilters(), item) != null;
    }

    private static List<IFilter> getActiveFilters() {
        List<IFilter> activeFilters = new ArrayList<>(FILTERS.size());
        for (IFilter filter : FILTERS) {
            if (filter.getEnabled()) {
                activeFilters.add(filter);
            }
        }
        return activeFilters;
    }

    private static String getFilterReason(List<IFilter> activeFilters, Aweme item) {
        for (IFilter filter : activeFilters) {
            if (filter.getFiltered(item)) {
                return filter.getClass().getSimpleName();
            }
        }
        return null;
    }

    private static void logNullItems(String source, AtomicInteger counter) {
        int count = counter.getAndIncrement();
        if (count < MAX_NULL_ITEMS_LOGS) {
            Logger.printInfo(() -> "[Morphe TikTok FeedFilter] filter(" + source + "): items=null");
        } else if (count == MAX_NULL_ITEMS_LOGS) {
            Logger.printInfo(() -> "[Morphe TikTok FeedFilter] filter(" + source + "): items=null (further logs suppressed)");
        }
    }

    private static void debugLogBatch(String source, List list) {
        int size = list == null ? -1 : list.size();
        Logger.printInfo(() ->
            "[Morphe TikTok FeedFilter] filter(" + source + "): size=" + size
                + " remove_ads=" + Settings.REMOVE_ADS.get()
                + " hide_shop=" + Settings.HIDE_SHOP.get()
                + " hide_live=" + Settings.HIDE_LIVE.get()
                + " hide_story=" + Settings.HIDE_STORY.get()
                + " hide_image=" + Settings.HIDE_IMAGE.get()
                + " min_max_views=\"" + Settings.MIN_MAX_VIEWS.get() + "\""
                + " min_max_likes=\"" + Settings.MIN_MAX_LIKES.get() + "\""
        );
    }

    private static void logItem(Aweme item, String reason, boolean verbose) {
        if (!verbose || reason == null || !shouldLogItem()) return;

        String shareUrl = item.getShareUrl();
        if (shareUrl != null && shareUrl.length() > 140) {
            shareUrl = shareUrl.substring(0, 140) + "...";
        }

        String finalShareUrl = shareUrl;
        Logger.printInfo(() -> {
            long playCount = -1;
            long likeCount = -1;

            AwemeStatistics statistics = item.getStatistics();
            if (statistics != null) {
                playCount = statistics.getPlayCount();
                likeCount = statistics.getDiggCount();
            }

            var imageInfos = item.getImageInfos();
            boolean isImage = imageInfos != null && !imageInfos.isEmpty();
            boolean isPhotoMode = item.getPhotoModeImageInfo() != null || item.getPhotoModeTextInfo() != null;

            return "[Morphe TikTok FeedFilter] item"
                + " aid=" + item.getAid()
                + " ad=" + item.isAd()
                + " promo=" + item.isWithPromotionalMusic()
                + " liveEvidence=" + LiveFilter.getLiveEvidence(item)
                + " story=" + item.getIsTikTokStory()
                + " image=" + isImage
                + " photoMode=" + isPhotoMode
                + " playCount=" + playCount
                + " likeCount=" + likeCount
                + " shareUrl=" + (finalShareUrl == null ? "null" : "\"" + finalShareUrl + "\"")
                + " => " + (reason == null ? "KEEP" : "FILTER(" + reason + ")");
        });
    }

    private static boolean shouldLogBatch() {
        return batchLogCount.getAndIncrement() < MAX_BATCH_LOGS;
    }

    private static boolean shouldLogItem() {
        return itemLogCount.getAndIncrement() < MAX_ITEM_LOGS;
    }

    @FunctionalInterface
    interface AwemeExtractor {
        Aweme extract(Object source);
    }
}

