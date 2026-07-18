/*
 * Forked from:
 * https://github.com/ReVanced/revanced-patches/blob/377d4e15016296b45d809697f7f69bce74badd3a/extensions/tiktok/src/main/java/app/revanced/extension/tiktok/settings/preference/DownloadPathPreference.java
 */

package app.morphe.extension.tiktok.settings.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.preference.DialogPreference;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.morphe.extension.shared.settings.StringSetting;
import app.morphe.extension.tiktok.Utils;

@SuppressWarnings("deprecation")
public class DownloadPathPreference extends DialogPreference {
    private String mValue;

    private boolean mValueSet;
    private String downloadPathValue;
    private final String mDefaultPath;

    public DownloadPathPreference(Context context, String title, StringSetting setting) {
        super(context);
        mDefaultPath = setting.defaultValue;
        setTitle(title);
        setSummary(Environment.getExternalStorageDirectory().getPath() + "/" + setting.get());
        setKey(setting.key);
        setValue(setting.get());
    }

    public String getValue() {
        return this.mValue;
    }

    public void setValue(String value) {
        String normalizedValue = normalizePath(value);
        setSummary(Environment.getExternalStorageDirectory().getPath() + "/" + normalizedValue);
        final boolean changed = !TextUtils.equals(mValue, normalizedValue);
        if (changed || !mValueSet) {
            mValue = normalizedValue;
            mValueSet = true;
            persistString(normalizedValue);
            if (changed) {
                notifyDependencyChange(shouldDisableDependents());
                notifyChanged();
            }
        }
    }

    public void applyPickedPath(String path) {
        String newValue = normalizePath(path);
        setValue(newValue);
        app.morphe.extension.shared.Utils.showToastShort("Download path updated");
    }

    @Override
    protected View onCreateDialogView() {
        downloadPathValue = normalizePath(getValue());

        Context context = getContext();
        LinearLayout dialogView = new LinearLayout(context);
        dialogView.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        dialogView.setOrientation(LinearLayout.VERTICAL);
        int padding = SettingsUi.dp(context, 22);
        dialogView.setPadding(padding, padding, padding, SettingsUi.dp(context, 8));

        TextView title = SettingsUi.text(context, "Download path", 20, SettingsUi.textPrimary(), Typeface.BOLD);
        dialogView.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView helper = SettingsUi.text(
                context,
                "Enter a folder path relative to internal storage.",
                14,
                SettingsUi.textSecondary(),
                Typeface.NORMAL
        );
        LinearLayout.LayoutParams helperParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        helperParams.setMargins(0, SettingsUi.dp(context, 14), 0, SettingsUi.dp(context, 10));
        dialogView.addView(helper, helperParams);

        EditText downloadPath = new EditText(context);
        downloadPath.setInputType(InputType.TYPE_CLASS_TEXT);
        downloadPath.setSingleLine(true);
        downloadPath.setHint(mDefaultPath);
        downloadPath.setText(downloadPathValue);
        SettingsUi.styleEditText(downloadPath);
        downloadPath.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                downloadPathValue = editable.toString();
            }
        });
        LinearLayout.LayoutParams pathParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        pathParams.setMargins(0, SettingsUi.dp(context, 10), 0, 0);
        dialogView.addView(downloadPath, pathParams);
        return dialogView;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        Utils.setTitleAndSummaryColor(view);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder.setPositiveButton("Save", (dialog, which) -> this.onClick(dialog, DialogInterface.BUTTON_POSITIVE));
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setNeutralButton("Browse", null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            String newValue = normalizePath(downloadPathValue);
            setValue(newValue);
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        SettingsUi.styleFramedDialog(getDialog());
        AlertDialog dialog = (AlertDialog) getDialog();
        TextView browseButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        if (browseButton != null) {
            browseButton.setOnClickListener(view -> {
                TikTokPreferenceFragment.openDownloadPathFolderPicker(this);
                dialog.dismiss();
            });
        }
    }

    private String normalizePath(String path) {
        if (path == null) {
            return mDefaultPath;
        }

        String normalized = path.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }
        if (normalized.length() == 0) {
            return mDefaultPath;
        }
        return normalized;
    }
}

