package app.morphe.extension.tiktok.speed;

import app.morphe.extension.tiktok.settings.Settings;

/**
 * Helper methods injected by the TikTok playback speed bytecode patch.
 *
 * The patch expects these exact static signatures:
 * - rememberPlaybackSpeed(F)V
 * - getPlaybackSpeed()F
 */
@SuppressWarnings("unused")
public final class PlaybackSpeedPatch {
    private static volatile float rememberedSpeed = 1.0f;

    private PlaybackSpeedPatch() {}

    public static void rememberPlaybackSpeed(float speed) {
        rememberedSpeed = speed;
        // Persist speed across video changes (best-effort).
        try {
            Settings.REMEMBERED_SPEED.save(speed);
        } catch (Throwable ignored) {
            // Avoid crashing TikTok if settings persistence isn't available yet.
        }
    }

    public static float getPlaybackSpeed() {
        // Prefer persisted value; fall back to in-memory value.
        try {
            float persisted = Settings.REMEMBERED_SPEED.get();
            return persisted != 0.0f ? persisted : rememberedSpeed;
        } catch (Throwable ignored) {
            return rememberedSpeed;
        }
    }
}

