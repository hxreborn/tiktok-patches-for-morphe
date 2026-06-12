package app.morphe.extension.tiktok.settings.preference.categories;

import android.content.Context;
import android.preference.PreferenceScreen;

import app.morphe.extension.tiktok.settings.Settings;
import app.morphe.extension.tiktok.settings.SettingsStatus;
import app.morphe.extension.tiktok.settings.preference.TogglePreference;

@SuppressWarnings("deprecation")
public class CommentsPreferenceCategory extends ConditionalPreferenceCategory {
    public CommentsPreferenceCategory(Context context, PreferenceScreen screen) {
        super(context, screen);
        setTitle("Comments");
    }

    @Override
    public boolean getSettingsStatus() {
        return SettingsStatus.commentTranslationEnabled;
    }

    @Override
    public void addPreferences(Context context) {
        addPreference(new TogglePreference(
                context,
                "Auto translate comments",
                "Automatically translates loaded comment batches using TikTok's translation system.",
                Settings.COMMENT_BATCH_TRANSLATION
        ));
        addPreference(new TogglePreference(
                context,
                "Copy comments without username",
                "Copy only the comment text when using TikTok's copy comment action.",
                Settings.COPY_COMMENTS_WITHOUT_USERNAME
        ));
    }
}
