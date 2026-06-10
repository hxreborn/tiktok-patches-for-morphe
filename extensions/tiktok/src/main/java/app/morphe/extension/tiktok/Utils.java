/*
 * Forked from:
 * https://github.com/ReVanced/revanced-patches/blob/377d4e15016296b45d809697f7f69bce74badd3a/extensions/tiktok/src/main/java/app/revanced/extension/tiktok/Utils.java
 */

package app.morphe.extension.tiktok;

import android.view.View;

import app.morphe.extension.shared.settings.StringSetting;
import app.morphe.extension.tiktok.settings.preference.SettingsUi;

public class Utils {

    private static final long[] DEFAULT_MIN_MAX_VALUES = {0L, Long.MAX_VALUE};

    public static long[] parseMinMax(StringSetting setting) {
        final String[] minMax = setting.get().split("-");
        if (minMax.length == 2) {
            try {
                final long min = Long.parseLong(minMax[0]);
                final long max = Long.parseLong(minMax[1]);

                if (min <= max && min >= 0) return new long[]{min, max};

            } catch (NumberFormatException ignored) {
            }
        }

        setting.save("0-" + Long.MAX_VALUE);
        return DEFAULT_MIN_MAX_VALUES;
    }

    public static void setTitleAndSummaryColor(View view) {
        SettingsUi.styleTitleAndSummary(view);
    }
}

