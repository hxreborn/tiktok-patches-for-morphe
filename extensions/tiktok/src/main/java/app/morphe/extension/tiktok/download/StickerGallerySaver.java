package app.morphe.extension.tiktok.download;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.MimeTypeMap;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.tiktok.settings.Settings;

import com.ss.android.ugc.aweme.base.model.UrlModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("unused")
public final class StickerGallerySaver {
    private static final String ACTION_LABEL = "Save image";
    private static final String STICKER_DIRECTORY = "Stickers";
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 20_000;

    private static final ExecutorService SAVE_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final WeakHashMap<View, Boolean> ATTACHED_SHEETS = new WeakHashMap<>();

    private StickerGallerySaver() {
    }

    public static void attachSaveImageButton(View sheetView, Object sheetModel) {
        try {
            if (sheetView == null || sheetModel == null) return;

            synchronized (ATTACHED_SHEETS) {
                if (ATTACHED_SHEETS.containsKey(sheetView)) {
                    return;
                }
            }

            String imageUrl = firstUsableUrl(findUrlModel(sheetModel));
            if (imageUrl == null) {
                debugLog("[Morphe Stickers] no usable sticker URL");
                return;
            }

            List<View> actionButtons = findViewsByClassName(sheetView, "X.0Daq", "LX.0Daq");
            ViewGroup actionParent = findCommonParent(actionButtons);
            if (actionParent == null || actionButtons.size() < 2 || hasSaveImageButton(actionParent)) {
                debugLog("[Morphe Stickers] action parent unavailable buttons=" + actionButtons.size());
                return;
            }

            View template = actionButtons.get(actionButtons.size() - 1);
            int insertIndex = actionParent.indexOfChild(template) + 1;
            if (insertIndex <= 0) {
                debugLog("[Morphe Stickers] action buttons are not direct children");
                return;
            }

            TextView saveImageButton = createActionButton(template, imageUrl);
            ViewGroup.LayoutParams layoutParams = cloneLayoutParams(template.getLayoutParams());
            actionParent.addView(saveImageButton, insertIndex, layoutParams);

            synchronized (ATTACHED_SHEETS) {
                ATTACHED_SHEETS.put(sheetView, Boolean.TRUE);
            }

            debugLog("[Morphe Stickers] attached Save image button url=" + summarizeUrl(imageUrl)
                    + " parent=" + actionParent.getClass().getName());
        } catch (Throwable ex) {
            if (BaseSettings.DEBUG.get()) {
                Logger.printException(() -> "[Morphe Stickers] attachSaveImageButton failure", ex);
            }
        }
    }

    private static TextView createActionButton(View template, String imageUrl) {
        Context context = template.getContext();
        TextView button = new TextView(context);
        button.setText(ACTION_LABEL);
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(true);
        button.setEllipsize(TextUtils.TruncateAt.END);
        button.setOnClickListener(view -> saveStickerFromButton(view, imageUrl));

        if (template instanceof TextView) {
            TextView textTemplate = (TextView) template;
            button.setTextColor(textTemplate.getTextColors());
            button.setTextSize(0, textTemplate.getTextSize());
            button.setTypeface(textTemplate.getTypeface(), textTemplate.getTypeface() == null ? Typeface.NORMAL : textTemplate.getTypeface().getStyle());
            button.setIncludeFontPadding(textTemplate.getIncludeFontPadding());
            button.setMinHeight(textTemplate.getMinHeight());
            button.setMinWidth(textTemplate.getMinWidth());
            button.setPadding(
                    textTemplate.getPaddingLeft(),
                    textTemplate.getPaddingTop(),
                    textTemplate.getPaddingRight(),
                    textTemplate.getPaddingBottom()
            );
        } else {
            button.setTextColor(Color.WHITE);
            button.setTextSize(16);
            int paddingHorizontal = dp(context, 16);
            int paddingVertical = dp(context, 10);
            button.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
        }

        Drawable background = template.getBackground();
        if (background != null && background.getConstantState() != null) {
            button.setBackground(background.getConstantState().newDrawable().mutate());
        } else {
            button.setBackground(template.getBackground());
        }

        button.setEnabled(template.isEnabled());
        button.setClickable(true);
        button.setFocusable(true);
        button.setAlpha(template.getAlpha());
        return button;
    }

    private static void saveStickerFromButton(View button, String imageUrl) {
        Context context = button.getContext().getApplicationContext();
        button.setEnabled(false);
        toast(context, "Saving sticker...");

        SAVE_EXECUTOR.execute(() -> {
            SaveResult result = saveSticker(context, imageUrl);
            MAIN_HANDLER.post(() -> {
                button.setEnabled(true);
                toast(context, result.message);
                if (result.success) {
                    debugLog("[Morphe Stickers] saved sticker path=" + result.path);
                } else if (BaseSettings.DEBUG.get()) {
                    Logger.printInfo(() -> "[Morphe Stickers] save failed reason=" + result.message
                            + " url=" + summarizeUrl(imageUrl));
                }
            });
        });
    }

    private static SaveResult saveSticker(Context context, String imageUrl) {
        HttpURLConnection connection = null;
        Uri pendingUri = null;

        try {
            connection = (HttpURLConnection) new URL(imageUrl).openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "TikTok 43.8.3 Morphe");

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                return SaveResult.failure("Sticker download failed");
            }

            String mimeType = normalizeMimeType(connection.getContentType(), imageUrl);
            String extension = extensionForMimeType(mimeType, imageUrl);
            String displayName = "morphe-sticker-" + System.currentTimeMillis()
                    + "-" + Integer.toHexString(imageUrl.hashCode()) + "." + extension;

            try (InputStream inputStream = connection.getInputStream()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    pendingUri = saveWithMediaStore(context, inputStream, displayName, mimeType);
                    return SaveResult.success(displayPath(displayName), pendingUri.toString());
                }

                File outputFile = saveWithLegacyStorage(context, inputStream, displayName);
                return SaveResult.success(outputFile.getAbsolutePath(), outputFile.getAbsolutePath());
            } catch (Throwable ex) {
                if (pendingUri != null) {
                    try {
                        context.getContentResolver().delete(pendingUri, null, null);
                    } catch (Throwable ignored) {
                        // Best effort cleanup.
                    }
                }
                throw ex;
            }
        } catch (Throwable ex) {
            if (BaseSettings.DEBUG.get()) {
                Logger.printException(() -> "[Morphe Stickers] saveSticker failure", ex);
            }
            return SaveResult.failure("Sticker save failed");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static Uri saveWithMediaStore(Context context, InputStream inputStream, String displayName, String mimeType) throws Exception {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName);
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
        values.put(MediaStore.Images.Media.RELATIVE_PATH, stickerRelativePath());
        values.put(MediaStore.Images.Media.IS_PENDING, 1);

        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new IllegalStateException("MediaStore insert returned null");
        }

        try (OutputStream outputStream = resolver.openOutputStream(uri)) {
            if (outputStream == null) {
                throw new IllegalStateException("MediaStore output stream returned null");
            }
            copy(inputStream, outputStream);
        }

        ContentValues complete = new ContentValues();
        complete.put(MediaStore.Images.Media.IS_PENDING, 0);
        resolver.update(uri, complete, null, null);
        return uri;
    }

    private static File saveWithLegacyStorage(Context context, InputStream inputStream, String displayName) throws Exception {
        File directory = new File(Environment.getExternalStorageDirectory(), stickerRelativePath());
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("Could not create " + directory);
        }

        File outputFile = new File(directory, displayName);
        try (OutputStream outputStream = new FileOutputStream(outputFile)) {
            copy(inputStream, outputStream);
        }
        MediaScannerConnection.scanFile(context, new String[]{outputFile.getAbsolutePath()}, null, null);
        return outputFile;
    }

    private static String stickerRelativePath() {
        String path = Settings.IMAGE_DOWNLOAD_PATH.get();
        if (path == null || path.trim().isEmpty()) {
            path = Settings.IMAGE_DOWNLOAD_PATH.defaultValue;
        }

        path = path.replace('\\', '/');
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return path + "/" + STICKER_DIRECTORY;
    }

    private static String displayPath(String displayName) {
        return stickerRelativePath() + "/" + displayName;
    }

    private static void copy(InputStream inputStream, OutputStream outputStream) throws Exception {
        byte[] buffer = new byte[16 * 1024];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
    }

    private static UrlModel findUrlModel(Object model) {
        if (model == null) return null;

        Class<?> current = model.getClass();
        while (current != null) {
            java.lang.reflect.Field[] fields = current.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(model);
                    if (value instanceof UrlModel) {
                        return (UrlModel) value;
                    }
                } catch (Throwable ignored) {
                    // Keep probing other fields.
                }
            }
            current = current.getSuperclass();
        }

        return null;
    }

    private static String firstUsableUrl(UrlModel model) {
        if (model == null) return null;

        try {
            List<String> urls = model.getUrlList();
            if (urls == null || urls.isEmpty()) return null;

            for (String url : urls) {
                if (url != null && !url.trim().isEmpty() && !"null".equalsIgnoreCase(url.trim())) {
                    return url;
                }
            }
        } catch (Throwable ignored) {
            return null;
        }

        return null;
    }

    private static List<View> findViewsByClassName(View root, String... classNames) {
        List<View> matches = new ArrayList<>();
        collectViewsByClassName(root, matches, classNames);
        return matches;
    }

    private static void collectViewsByClassName(View view, List<View> matches, String... classNames) {
        if (view == null) return;

        String viewClassName = view.getClass().getName();
        for (String className : classNames) {
            if (viewClassName.equals(className)) {
                matches.add(view);
                break;
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectViewsByClassName(group.getChildAt(i), matches, classNames);
            }
        }
    }

    private static ViewGroup findCommonParent(List<View> views) {
        if (views.size() < 2) return null;

        View first = views.get(0);
        View second = views.get(1);
        ViewParent parent = first.getParent();
        while (parent != null) {
            if (isAncestor(parent, second)) {
                return parent instanceof ViewGroup ? (ViewGroup) parent : null;
            }
            parent = parent.getParent();
        }

        return null;
    }

    private static boolean isAncestor(ViewParent candidate, View view) {
        ViewParent parent = view.getParent();
        while (parent != null) {
            if (parent == candidate) return true;
            parent = parent.getParent();
        }
        return false;
    }

    private static boolean hasSaveImageButton(ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof TextView && ACTION_LABEL.contentEquals(((TextView) child).getText())) {
                return true;
            }
        }
        return false;
    }

    private static ViewGroup.LayoutParams cloneLayoutParams(ViewGroup.LayoutParams params) {
        if (params instanceof LinearLayout.LayoutParams) {
            return new LinearLayout.LayoutParams((LinearLayout.LayoutParams) params);
        }
        if (params instanceof ViewGroup.MarginLayoutParams) {
            return new ViewGroup.MarginLayoutParams((ViewGroup.MarginLayoutParams) params);
        }
        if (params != null) {
            return new ViewGroup.LayoutParams(params);
        }
        return new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static String normalizeMimeType(String contentType, String url) {
        if (contentType != null) {
            int separatorIndex = contentType.indexOf(';');
            String mimeType = separatorIndex >= 0 ? contentType.substring(0, separatorIndex) : contentType;
            mimeType = mimeType.trim().toLowerCase();
            if (mimeType.startsWith("image/")) {
                return mimeType;
            }
        }

        String extension = extensionFromUrl(url);
        if (extension != null) {
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mimeType != null && mimeType.startsWith("image/")) {
                return mimeType;
            }
        }

        return "image/jpeg";
    }

    private static String extensionForMimeType(String mimeType, String url) {
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        if (extension != null && !extension.trim().isEmpty()) {
            return extension;
        }

        extension = extensionFromUrl(url);
        if (extension != null) {
            return extension;
        }

        if ("image/jpeg".equals(mimeType)) {
            return "jpg";
        }
        return "img";
    }

    private static String extensionFromUrl(String url) {
        try {
            String path = URI.create(url).getPath();
            if (path == null) return null;

            int dotIndex = path.lastIndexOf('.');
            if (dotIndex < 0 || dotIndex == path.length() - 1) return null;

            String extension = path.substring(dotIndex + 1).toLowerCase();
            if (extension.length() > 5) return null;
            return extension;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String summarizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) return "null";

        try {
            URI uri = URI.create(url);
            String path = uri.getPath();
            if (path != null && path.length() > 96) {
                path = path.substring(0, 96) + "...";
            }
            return uri.getHost() + path;
        } catch (Throwable ignored) {
            int queryIndex = url.indexOf('?');
            String withoutQuery = queryIndex >= 0 ? url.substring(0, queryIndex) : url;
            return withoutQuery.length() <= 96 ? withoutQuery : withoutQuery.substring(0, 96) + "...";
        }
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static void toast(Context context, String message) {
        MAIN_HANDLER.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    private static void debugLog(String message) {
        if (BaseSettings.DEBUG.get()) {
            Logger.printInfo(() -> message);
        }
    }

    private static final class SaveResult {
        final boolean success;
        final String message;
        final String path;

        private SaveResult(boolean success, String message, String path) {
            this.success = success;
            this.message = message;
            this.path = path;
        }

        static SaveResult success(String path, String uri) {
            return new SaveResult(true, "Sticker saved", path + " (" + uri + ")");
        }

        static SaveResult failure(String message) {
            return new SaveResult(false, message, null);
        }
    }
}
