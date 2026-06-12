package app.morphe.extension.shared.settings.preference;

import java.util.Deque;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.shared.settings.BaseSettings;

/**
 * Manages a buffer for storing debug logs from {@link Logger}.
 * Stores just under 1MB of the most recent log data.
 * <p>
 * All methods are thread-safe.
 */
public final class LogBufferManager {
    /** Maximum character size of all buffer entries. Keep well under Android's 1 MB Binder clipboard limit. */
    private static final int BUFFER_MAX_CHARS = 250_000;
    /** Limit number of log lines. */
    private static final int BUFFER_MAX_SIZE = 10_000;

    private static final String DEBUG_LOGS_DISABLED = "Debug logs are disabled.";
    private static final String DEBUG_LOGS_NONE_FOUND = "No Morphe debug logs found.";
    private static final String DEBUG_LOGS_NONE_MATCHING = "No matching Morphe debug logs found.";
    private static final String DEBUG_LOGS_COPIED_TO_CLIPBOARD = "Morphe debug logs copied to clipboard.";
    private static final String DEBUG_LOGS_CLEAR_TOAST = "Morphe debug logs cleared.";
    private static final String DEBUG_LOGS_FAILED_TO_EXPORT = "Failed to export Morphe debug logs: %s";

    private static final Deque<String> logBuffer = new ConcurrentLinkedDeque<>();
    private static final AtomicInteger logBufferCharSize = new AtomicInteger();

    /**
     * Appends a log message to the internal buffer if debugging is enabled.
     * The buffer is limited to approximately {@link #BUFFER_MAX_CHARS} or {@link #BUFFER_MAX_SIZE}
     * to prevent excessive memory usage.
     *
     * @param message The log message to append.
     */
    public static void appendToLogBuffer(String message) {
        Objects.requireNonNull(message);

        // It's very important that no Settings are used in this method,
        // as this code is used when a context is not set and thus referencing
        // a setting will crash the app.
        logBuffer.addLast(message);
        int newSize = logBufferCharSize.addAndGet(message.length());

        // Remove the oldest entries if over the log size limits.
        while (newSize > BUFFER_MAX_CHARS || logBuffer.size() > BUFFER_MAX_SIZE) {
            String removed = logBuffer.pollFirst();
            if (removed == null) {
                // Thread race of two different calls to this method, and the other thread won.
                return;
            }

            newSize = logBufferCharSize.addAndGet(-removed.length());
        }
    }

    /**
     * Exports all logs from the internal buffer to the clipboard.
     * Displays a toast with the result.
     */
    public static void exportToClipboard() {
        try {
            if (!BaseSettings.DEBUG.get()) {
                Utils.showToastShort(DEBUG_LOGS_DISABLED);
                return;
            }

            if (logBuffer.isEmpty()) {
                Utils.showToastShort(DEBUG_LOGS_NONE_FOUND);
                clearLogBufferData(); // Clear toast log entry that was just created.
                return;
            }

            String exportText = buildExportText();
            if (exportText.isEmpty()) {
                Utils.showToastShort(DEBUG_LOGS_NONE_MATCHING);
                return;
            }

            Utils.setClipboard(exportText);

            // Most Android 13+ devices also show their own clipboard toast, but not all of them do.
            Utils.showToastShort(DEBUG_LOGS_COPIED_TO_CLIPBOARD);
        } catch (Exception ex) {
            // Handle security exception if clipboard access is denied.
            String errorMessage = String.format(DEBUG_LOGS_FAILED_TO_EXPORT, ex.getMessage());
            Utils.showToastLong(errorMessage);
            Logger.printDebug(() -> errorMessage, ex);
        }
    }

    private static void clearLogBufferData() {
        // Cannot simply clear the log buffer because there is no
        // write lock for both the deque and the atomic int.
        // Instead, pop off log entries and decrement the size one by one.
        while (!logBuffer.isEmpty()) {
            String removed = logBuffer.pollFirst();
            if (removed != null) {
                logBufferCharSize.addAndGet(-removed.length());
            }
        }
    }

    private static String buildExportText() {
        Set<String> selected = LogExportFilterPreference.parse(BaseSettings.DEBUG_LOG_FILTERS.get());
        boolean includeAll = selected.isEmpty() || selected.contains("all");

        StringBuilder builder = new StringBuilder();
        for (String entry : logBuffer) {
            if (!includeAll && !matchesSelectedFilter(entry, selected)) continue;

            if (builder.length() > 0) builder.append('\n');
            builder.append(entry);
        }

        return builder.toString();
    }

    private static boolean matchesSelectedFilter(String entry, Set<String> selected) {
        String normalized = entry.toLowerCase(Locale.US);
        if (selected.contains("errors") && isErrorLog(normalized)) return true;

        return selected.contains(logFamily(normalized));
    }

    private static String logFamily(String normalized) {
        if (normalized.contains("followprobe") || normalized.contains("followdiagnostics")) {
            return "follow";
        }
        if (normalized.contains("morphe downloads") || normalized.contains("downloadspatch")) {
            return "downloads";
        }
        if (normalized.contains("feed")
                || normalized.contains("navigation")
                || normalized.contains("tako")
                || normalized.contains("tab")) {
            return "feed";
        }
        if (normalized.contains("setting") || normalized.contains("preference")) {
            return "settings";
        }
        if (isErrorLog(normalized)) {
            return "errors";
        }
        return "other";
    }

    private static boolean isErrorLog(String normalized) {
        return normalized.contains("error")
                || normalized.contains("failed")
                || normalized.contains("failure")
                || normalized.contains("exception")
                || normalized.contains("crash");
    }

    /**
     * Clears the internal log buffer and displays a toast with the result.
     */
    public static void clearLogBuffer() {
        if (!BaseSettings.DEBUG.get()) {
            Utils.showToastShort(DEBUG_LOGS_DISABLED);
            return;
        }

        // Show toast before clearing, otherwise toast log will still remain.
        Utils.showToastShort(DEBUG_LOGS_CLEAR_TOAST);
        clearLogBufferData();
    }
}
