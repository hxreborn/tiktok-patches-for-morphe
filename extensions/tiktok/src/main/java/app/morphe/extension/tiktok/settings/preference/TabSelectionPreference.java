package app.morphe.extension.tiktok.settings.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.preference.Preference;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import app.morphe.extension.shared.settings.StringSetting;
import app.morphe.extension.shared.settings.preference.AbstractPreferenceFragment;
import app.morphe.extension.tiktok.navigation.NavigationTabOptions;
import app.morphe.extension.tiktok.settings.Settings;

@SuppressWarnings("deprecation")
public class TabSelectionPreference extends Preference {
    private final StringSetting setting;
    private String value;
    private boolean valueSet;

    public TabSelectionPreference(Context context, StringSetting setting) {
        super(context);
        this.setting = setting;
        setTitle("Allowed loaded tabs");
        setKey(setting.key);
        setValue(setting.get());
    }

    public String getValue() {
        return value;
    }

    public boolean setValue(String value) {
        String sanitizedValue = NavigationTabOptions.serializeEnabledKeys(
                NavigationTabOptions.parseEnabledKeys(value)
        );
        boolean changed = !TextUtils.equals(this.value, sanitizedValue);
        if (changed || !valueSet) {
            this.value = sanitizedValue;
            valueSet = true;
            setting.save(sanitizedValue);
            refreshSummary();
            if (changed) {
                notifyDependencyChange(shouldDisableDependents());
                notifyChanged();
            }
        }
        return changed;
    }

    @Override
    protected void onClick() {
        showSelectionDialog();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        app.morphe.extension.tiktok.Utils.setTitleAndSummaryColor(view);
    }

    private void refreshSummary() {
        Set<String> selected = NavigationTabOptions.parseEnabledKeys(value);
        List<NavigationTabOptions.Option> observedOptions = getObservedOptions();
        if (observedOptions.size() <= 1) {
            setSummary("Open TikTok home feed to detect loaded tabs.");
            return;
        }

        int selectedObserved = 0;
        StringBuilder builder = new StringBuilder();
        for (NavigationTabOptions.Option option : observedOptions) {
            if (!selected.contains(option.key)) {
                continue;
            }

            selectedObserved++;
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(option.label);
        }

        if (selectedObserved == observedOptions.size()) {
            setSummary("All loaded tabs");
            return;
        }

        if (builder.length() == 0) {
            setSummary("For You");
            return;
        }
        setSummary(builder.toString());
    }

    private void showSelectionDialog() {
        Context context = getContext();
        Set<String> selected = new LinkedHashSet<>(NavigationTabOptions.parseEnabledKeys(value));
        List<NavigationTabOptions.Option> observedOptions = getObservedOptions();

        LinearLayout dialogView = new LinearLayout(context);
        dialogView.setOrientation(LinearLayout.VERTICAL);
        dialogView.setBackground(createDialogBackground());
        int padding = dpToPx(22);
        dialogView.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(context);
        title.setText("Allowed loaded tabs");
        title.setTextColor(getTitleTextColor());
        title.setTextSize(20);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        dialogView.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView helper = new TextView(context);
        helper.setText("Only tabs TikTok has loaded on this device are shown here. This does not force unavailable tabs to appear.");
        helper.setTextColor(getSummaryTextColor());
        LinearLayout.LayoutParams helperParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        helperParams.setMargins(0, dpToPx(16), 0, dpToPx(12));
        dialogView.addView(helper, helperParams);

        LinearLayout optionsContainer = new LinearLayout(context);
        optionsContainer.setOrientation(LinearLayout.VERTICAL);
        optionsContainer.setBackground(createListBackground());
        int optionInset = Math.max(1, dpToPx(1));
        optionsContainer.setPadding(optionInset, optionInset, optionInset, optionInset);

        for (NavigationTabOptions.Option option : observedOptions) {
            optionsContainer.addView(createOptionRow(context, selected, option));
        }

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(false);
        scrollView.addView(optionsContainer, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(380)
        );
        scrollParams.setMargins(0, 0, 0, dpToPx(16));
        dialogView.addView(scrollView, scrollParams);

        LinearLayout actions = new LinearLayout(context);
        actions.setGravity(Gravity.CENTER_VERTICAL);

        TextView showAllButton = createActionButton(context, "Reset to loaded", false);
        TextView cancelButton = createActionButton(context, "Cancel", false);
        TextView saveButton = createActionButton(context, "Save", true);

        actions.addView(showAllButton, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        ));
        actions.addView(cancelButton);
        actions.addView(saveButton);
        dialogView.addView(actions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        showAllButton.setOnClickListener(view -> {
            selected.clear();
            for (NavigationTabOptions.Option option : observedOptions) {
                selected.add(option.key);
            }
            boolean changed = setValue(NavigationTabOptions.serializeEnabledKeys(selected));
            dialog.dismiss();
            if (changed && setting.rebootApp) {
                AbstractPreferenceFragment.showRestartDialog(context);
            }
        });
        cancelButton.setOnClickListener(view -> dialog.dismiss());
        saveButton.setOnClickListener(view -> {
            boolean changed = setValue(NavigationTabOptions.serializeEnabledKeys(selected));
            dialog.dismiss();
            if (changed && setting.rebootApp) {
                AbstractPreferenceFragment.showRestartDialog(context);
            }
        });

        dialog.show();
        SettingsUi.styleDialog(dialog);
    }

    private List<NavigationTabOptions.Option> getObservedOptions() {
        Set<String> observed = NavigationTabOptions.parseObservedKeys(Settings.FEED_NAVIGATION_OBSERVED_TABS.get());
        observed.add(NavigationTabOptions.HOT);
        return NavigationTabOptions.optionsForKeys(observed);
    }

    private View createOptionRow(Context context, Set<String> selected, NavigationTabOptions.Option option) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(getDialogBackgroundColor());
        row.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));

        CheckBox checkBox = new CheckBox(context);
        checkBox.setChecked(selected.contains(option.key));
        checkBox.setEnabled(!NavigationTabOptions.HOT.equals(option.key));
        checkBox.setClickable(false);
        SettingsUi.styleCheckBox(checkBox);
        row.addView(checkBox, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout textContainer = new LinearLayout(context);
        textContainer.setOrientation(LinearLayout.VERTICAL);

        TextView label = new TextView(context);
        label.setText(option.label);
        label.setTextColor(getTitleTextColor());
        label.setTextSize(16);
        textContainer.addView(label);

        if (NavigationTabOptions.HOT.equals(option.key)) {
            TextView summary = new TextView(context);
            summary.setText("Required");
            summary.setTextColor(getSummaryTextColor());
            summary.setTextSize(13);
            textContainer.addView(summary);
        }

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        );
        row.addView(textContainer, textParams);

        row.setOnClickListener(view -> {
            if (NavigationTabOptions.HOT.equals(option.key)) {
                return;
            }

            if (selected.contains(option.key)) {
                selected.remove(option.key);
                checkBox.setChecked(false);
            } else {
                selected.add(option.key);
                checkBox.setChecked(true);
            }
        });

        View divider = new View(context);
        divider.setBackgroundColor(getDialogDividerColor());

        LinearLayout wrapper = new LinearLayout(context);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        wrapper.addView(divider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Math.max(1, dpToPx(1))
        ));
        return wrapper;
    }

    private TextView createActionButton(Context context, String text, boolean primary) {
        TextView button = new TextView(context);
        button.setText(text);
        button.setTextSize(16);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(6));
        SettingsUi.styleTextAction(button, primary);
        return button;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getContext().getResources().getDisplayMetrics().density);
    }

    private GradientDrawable createDialogBackground() {
        return SettingsUi.borderedSurface(getContext(), 6, true);
    }

    private GradientDrawable createListBackground() {
        return SettingsUi.borderedSurface(getContext(), 4, false);
    }

    private static int getDialogBackgroundColor() {
        return SettingsUi.surface();
    }

    private static int getDialogDividerColor() {
        return SettingsUi.divider();
    }

    private static int getTitleTextColor() {
        return SettingsUi.textPrimary();
    }

    private static int getSummaryTextColor() {
        return SettingsUi.textSecondary();
    }
}
