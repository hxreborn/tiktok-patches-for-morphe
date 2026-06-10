package app.morphe.extension.tiktok.translation;

import android.view.View;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.tiktok.settings.Settings;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CommentBatchTranslator {
    private static final long STALE_ENTRY_MS = 15_000L;
    private static final long LOADED_BATCH_STALE_MS = 60_000L;
    private static final int MAX_LOADED_BATCHES = 4;
    private static final int MAX_REQUESTED_BATCH_KEYS = 12;

    private static final Object LOCK = new Object();
    private static final LinkedHashMap<String, VisibleComment> visibleComments = new LinkedHashMap<>();
    private static final LinkedHashMap<String, LoadedBatch> loadedBatches = new LinkedHashMap<>();
    private static final LinkedHashSet<String> requestedLoadedBatchKeys = new LinkedHashSet<>();
    private static LoadedBatch latestLoadedBatch;
    private static WeakReference<Object> lastManager = new WeakReference<>(null);

    private CommentBatchTranslator() {
    }

    public static void registerCommentCell(View itemView, Object manager) {
        if (!Settings.COMMENT_BATCH_TRANSLATION.get()) return;
        if (itemView == null || manager == null) return;

        try {
            Object comment = readField(manager, "LLILLIZIL");
            Object context = readField(manager, "LLILZ");
            if (comment == null || context == null) return;

            String cid = invokeString(comment, "getCid");
            if (isBlank(cid)) return;

            long now = System.currentTimeMillis();
            synchronized (LOCK) {
                pruneLocked(now);
                visibleComments.put(cid, new VisibleComment(manager, comment, context, now));
                lastManager = new WeakReference<>(manager);
            }

            translateLoadedBatchIfReady(manager, false);
            itemView.postDelayed(() -> translateLoadedBatchIfReady(manager, true), 350);
        } catch (Throwable ex) {
            Logger.printDebug(() -> "[Morphe CommentBatchTranslator] register failed", asException(ex));
        }
    }

    public static void onCommentListLoaded(Object commentItemList) {
        if (!Settings.COMMENT_BATCH_TRANSLATION.get()) return;
        if (commentItemList == null) return;

        try {
            Object itemsObject = readField(commentItemList, "items");
            if (!(itemsObject instanceof List)) {
                Logger.printDebug(() -> "[Morphe CommentBatchTranslator] loaded.batch ignored items="
                        + className(itemsObject));
                return;
            }

            List<?> items = (List<?>) itemsObject;
            ArrayList<Object> comments = new ArrayList<>();
            LinkedHashSet<String> cids = new LinkedHashSet<>();
            String aid = null;

            for (Object item : items) {
                if (item == null) continue;

                String cid = invokeStringQuiet(item, "getCid");
                if (isBlank(cid) || cids.contains(cid)) continue;

                String itemAid = invokeStringQuiet(item, "getAwemeId");
                if (isBlank(aid) && !isBlank(itemAid)) {
                    aid = itemAid;
                }

                comments.add(item);
                cids.add(cid);
            }

            if (comments.isEmpty()) {
                Logger.printDebug(() -> "[Morphe CommentBatchTranslator] loaded.batch empty"
                        + " rawSize=" + items.size());
                return;
            }

            LoadedBatch batch = new LoadedBatch(aid, comments, cids, System.currentTimeMillis());
            synchronized (LOCK) {
                pruneLocked(batch.loadedAtMs);
                latestLoadedBatch = batch;
                loadedBatches.put(batch.key(), batch);
                while (loadedBatches.size() > MAX_LOADED_BATCHES) {
                    Iterator<String> iterator = loadedBatches.keySet().iterator();
                    if (!iterator.hasNext()) break;
                    iterator.next();
                    iterator.remove();
                }
            }

            if (BaseSettings.DEBUG.get()) {
                String loadedAid = aid;
                String firstCid = comments.isEmpty() ? null : invokeStringQuiet(comments.get(0), "getCid");
                Logger.printInfo(() -> "[Morphe CommentBatchTranslator] loaded.batch"
                        + " rawSize=" + items.size()
                        + " eligibleSize=" + comments.size()
                        + " aid=" + value(loadedAid)
                        + " firstCid=" + value(firstCid));
            }

            translateLoadedBatchIfReady(lastManager.get(), false);
        } catch (Throwable ex) {
            Logger.printDebug(() -> "[Morphe CommentBatchTranslator] loaded.batch failed", asException(ex));
        }
    }

    public static void onNativeBatchStart(Object comments, Object context, boolean forceWithoutAweme) {
        if (!BaseSettings.DEBUG.get()) return;

        Logger.printInfo(() -> "[Morphe CommentBatchTranslator] native.start"
                + " size=" + collectionSize(comments)
                + " contextAid=" + value(readFieldQuiet(context, "LIZIZ"))
                + " forceWithoutAweme=" + forceWithoutAweme);
    }

    public static void onNativeBatchComplete(Object runner) {
        if (!BaseSettings.DEBUG.get()) return;

        Object results = readFieldQuiet(runner, "l0");
        Object task = readFieldQuiet(runner, "l1");
        Object requested = readFieldQuiet(task, "LIZ");
        Logger.printInfo(() -> "[Morphe CommentBatchTranslator] native.complete"
                + " requestedSize=" + collectionSize(requested)
                + " resultSize=" + collectionSize(results));
    }

    private static void translateLoadedBatchIfReady(Object anchor, boolean allowVisibleFallback) {
        Batch batch = buildLoadedBatch(anchor, allowVisibleFallback);
        if (batch.comments.isEmpty()) {
            return;
        }

        try {
            synchronized (LOCK) {
                if (requestedLoadedBatchKeys.contains(batch.requestKey)) {
                    return;
                }
            }

            Method method = batch.nativeManagerClass.getDeclaredMethod(
                    "LJFF",
                    List.class,
                    batch.context.getClass(),
                    boolean.class
            );
            method.setAccessible(true);
            method.invoke(null, batch.comments, batch.context, false);

            synchronized (LOCK) {
                requestedLoadedBatchKeys.add(batch.requestKey);
                while (requestedLoadedBatchKeys.size() > MAX_REQUESTED_BATCH_KEYS) {
                    Iterator<String> iterator = requestedLoadedBatchKeys.iterator();
                    if (!iterator.hasNext()) break;
                    iterator.next();
                    iterator.remove();
                }
            }

            Logger.printInfo(() -> "[Morphe CommentBatchTranslator] requested"
                    + " size=" + batch.comments.size()
                    + " requestKey=" + batch.requestKey
                    + " aid=" + value(readFieldQuiet(batch.context, "LIZIZ")));
        } catch (Throwable ex) {
            Logger.printException(() -> "[Morphe CommentBatchTranslator] native request failed", ex);
        }
    }

    private static Batch buildLoadedBatch(Object anchor, boolean allowVisibleFallback) {
        long now = System.currentTimeMillis();
        Object anchorContext = readFieldQuiet(anchor, "LLILZ");
        Object nativeManager = readFieldQuiet(anchor, "LLILLL");
        String anchorAid = valueOrNull(value(readFieldQuiet(anchorContext, "LIZIZ")));
        String anchorCid = invokeStringQuiet(readFieldQuiet(anchor, "LLILLIZIL"), "getCid");
        Set<?> pending = readPendingSet(nativeManager);

        ArrayList<Object> comments = new ArrayList<>();
        Object context = anchorContext;
        Class<?> nativeManagerClass = nativeManager == null ? null : nativeManager.getClass();
        String requestKey = null;

        synchronized (LOCK) {
            pruneLocked(now);
            LoadedBatch loadedBatch = findLoadedBatchLocked(anchorAid, anchorCid);
            if (loadedBatch != null) {
                requestKey = loadedBatch.requestKey();
                for (Object comment : loadedBatch.comments) {
                    if (comment == null) continue;
                    if (!matchesAid(anchorAid, invokeStringQuiet(comment, "getAwemeId"))) continue;
                    if (isTranslated(comment)) continue;
                    if (isDefaultLanguage(comment)) continue;

                    String cid = invokeStringQuiet(comment, "getCid");
                    if (pending != null && cid != null && pending.contains(cid)) continue;

                    comments.add(comment);
                }
            } else if (allowVisibleFallback) {
                requestKey = buildVisibleFallbackLocked(anchorAid, anchorContext, pending, comments);
            }
        }

        if (context == null || nativeManagerClass == null || requestKey == null) {
            comments.clear();
        }
        return new Batch(comments, context, nativeManagerClass, requestKey);
    }

    private static String buildVisibleFallbackLocked(
            String anchorAid,
            Object anchorContext,
            Set<?> pending,
            ArrayList<Object> comments
    ) {
        if (anchorContext == null) return null;

        LinkedHashSet<String> cids = new LinkedHashSet<>();
        for (VisibleComment entry : visibleComments.values()) {
            Object context = entry.context.get();
            if (context != anchorContext) continue;

            Object comment = entry.comment.get();
            if (comment == null || isTranslated(comment)) continue;
            if (isDefaultLanguage(comment)) continue;
            if (!matchesAid(anchorAid, invokeStringQuiet(comment, "getAwemeId"))) continue;

            String cid = invokeStringQuiet(comment, "getCid");
            if (isBlank(cid) || cids.contains(cid)) continue;
            if (pending != null && pending.contains(cid)) continue;

            comments.add(comment);
            cids.add(cid);
        }

        if (cids.isEmpty()) return null;

        String firstCid = cids.iterator().next();
        String lastCid = firstCid;
        for (String cid : cids) {
            lastCid = cid;
        }
        String scope = isBlank(anchorAid) ? "context:" + System.identityHashCode(anchorContext) : anchorAid;
        return "visible:" + scope + ":" + comments.size() + ":" + firstCid + ":" + lastCid;
    }

    private static LoadedBatch findLoadedBatchLocked(String anchorAid, String anchorCid) {
        LoadedBatch latest = latestLoadedBatch;
        if (!isBlank(anchorCid)) {
            if (latest != null && latest.cids.contains(anchorCid)) return latest;

            LoadedBatch cidMatchedBatch = null;
            for (LoadedBatch batch : loadedBatches.values()) {
                if (batch.cids.contains(anchorCid)) {
                    cidMatchedBatch = batch;
                }
            }
            if (cidMatchedBatch != null) return cidMatchedBatch;

            return null;
        }

        LoadedBatch loadedBatch = isBlank(anchorAid) ? null : loadedBatches.get(anchorAid);
        if (loadedBatch != null) return loadedBatch;

        if (latest == null) return null;
        if (!isBlank(anchorAid) && sameValue(anchorAid, latest.aid)) return latest;
        return null;
    }

    private static void pruneLocked(long now) {
        Iterator<Map.Entry<String, VisibleComment>> iterator = visibleComments.entrySet().iterator();
        while (iterator.hasNext()) {
            VisibleComment entry = iterator.next().getValue();
            if (now - entry.lastSeenMs > STALE_ENTRY_MS
                    || entry.manager.get() == null
                    || entry.comment.get() == null
                    || entry.context.get() == null) {
                iterator.remove();
            }
        }

        Iterator<Map.Entry<String, LoadedBatch>> batchIterator = loadedBatches.entrySet().iterator();
        while (batchIterator.hasNext()) {
            LoadedBatch entry = batchIterator.next().getValue();
            if (now - entry.loadedAtMs > LOADED_BATCH_STALE_MS) {
                batchIterator.remove();
                if (entry == latestLoadedBatch) latestLoadedBatch = null;
            }
        }
    }

    private static boolean isTranslated(Object comment) {
        String translated = invokeStringQuiet(comment, "isTranslated");
        return "true".equalsIgnoreCase(translated);
    }

    private static boolean isDefaultLanguage(Object comment) {
        String commentLanguage = primaryLanguageTag(invokeStringQuiet(comment, "getCommentLanguage"));
        if (isBlank(commentLanguage)) return false;

        String defaultLanguage = primaryLanguageTag(Locale.getDefault().getLanguage());
        return !isBlank(defaultLanguage) && commentLanguage.equals(defaultLanguage);
    }

    private static String primaryLanguageTag(String language) {
        if (isBlank(language)) return null;

        String normalized = language.trim().replace('_', '-').toLowerCase(Locale.ROOT);
        int separatorIndex = normalized.indexOf('-');
        return separatorIndex > 0 ? normalized.substring(0, separatorIndex) : normalized;
    }

    private static Set<?> readPendingSet(Object nativeManager) {
        if (nativeManager == null) return null;
        try {
            Field field = nativeManager.getClass().getDeclaredField("LIZIZ");
            field.setAccessible(true);
            Object value = field.get(null);
            return value instanceof Set ? (Set<?>) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object readField(Object instance, String name) throws NoSuchFieldException, IllegalAccessException {
        Field field = findField(instance.getClass(), name);
        if (field == null) throw new NoSuchFieldException(name);
        field.setAccessible(true);
        return field.get(instance);
    }

    private static Object readFieldQuiet(Object instance, String name) {
        if (instance == null) return null;
        try {
            return readField(instance, name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static String invokeString(Object instance, String methodName) throws Exception {
        Method method = instance.getClass().getMethod(methodName);
        Object value = method.invoke(instance);
        return value == null ? null : String.valueOf(value);
    }

    private static String invokeStringQuiet(Object instance, String methodName) {
        if (instance == null) return null;
        try {
            return invokeString(instance, methodName);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int collectionSize(Object value) {
        return value instanceof java.util.Collection ? ((java.util.Collection<?>) value).size() : -1;
    }

    private static boolean sameValue(String left, String right) {
        if (left == null) return right == null;
        return left.equals(right);
    }

    private static boolean matchesAid(String anchorAid, String itemAid) {
        return isBlank(anchorAid) || isBlank(itemAid) || sameValue(anchorAid, valueOrNull(itemAid));
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String valueOrNull(String value) {
        return isBlank(value) ? null : value;
    }

    private static String value(Object value) {
        return value == null ? "null" : String.valueOf(value);
    }

    private static String className(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }

    private static Exception asException(Throwable throwable) {
        return throwable instanceof Exception ? (Exception) throwable : new Exception(throwable);
    }

    private static final class VisibleComment {
        final WeakReference<Object> manager;
        final WeakReference<Object> comment;
        final WeakReference<Object> context;
        final long lastSeenMs;

        VisibleComment(Object manager, Object comment, Object context, long lastSeenMs) {
            this.manager = new WeakReference<>(manager);
            this.comment = new WeakReference<>(comment);
            this.context = new WeakReference<>(context);
            this.lastSeenMs = lastSeenMs;
        }
    }

    private static final class Batch {
        final ArrayList<Object> comments;
        final Object context;
        final Class<?> nativeManagerClass;
        final String requestKey;

        Batch(ArrayList<Object> comments, Object context, Class<?> nativeManagerClass, String requestKey) {
            this.comments = comments;
            this.context = context;
            this.nativeManagerClass = nativeManagerClass;
            this.requestKey = requestKey;
        }
    }

    private static final class LoadedBatch {
        final String aid;
        final ArrayList<Object> comments;
        final LinkedHashSet<String> cids;
        final long loadedAtMs;

        LoadedBatch(String aid, ArrayList<Object> comments, LinkedHashSet<String> cids, long loadedAtMs) {
            this.aid = aid;
            this.comments = comments;
            this.cids = cids;
            this.loadedAtMs = loadedAtMs;
        }

        String key() {
            if (!isBlank(aid)) return aid;
            return cids.isEmpty() ? "unknown" : "cid:" + cids.iterator().next();
        }

        String requestKey() {
            String firstCid = cids.isEmpty() ? "unknown" : cids.iterator().next();
            String lastCid = firstCid;
            for (String cid : cids) {
                lastCid = cid;
            }
            return key() + ":" + comments.size() + ":" + firstCid + ":" + lastCid;
        }
    }
}
