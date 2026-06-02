package app.morphe.extension.tiktok.settings.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.preference.Preference;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.settings.preference.AbstractPreferenceFragment;
import app.morphe.extension.tiktok.settings.Settings;
import app.morphe.extension.tiktok.spoof.sim.SimPreset;
import app.morphe.extension.tiktok.spoof.sim.SimPresets;

@SuppressWarnings("deprecation")
public class SimPresetPreference extends Preference {
    private static final int TEXT_DARK_MODE_TITLE = Color.WHITE;
    private static final int TEXT_DARK_MODE_SUMMARY = Color.argb(255, 170, 170, 170);
    private static final int TEXT_LIGHT_MODE_TITLE = Color.BLACK;
    private static final int TEXT_LIGHT_MODE_SUMMARY = Color.argb(255, 80, 80, 80);

    private final List<SimPreset> visiblePresets = new ArrayList<>();
    private final InputTextPreference countryIsoPreference;
    private final InputTextPreference mccMncPreference;
    private final InputTextPreference operatorNamePreference;

    public SimPresetPreference(Context context,
                               InputTextPreference countryIsoPreference,
                               InputTextPreference mccMncPreference,
                               InputTextPreference operatorNamePreference) {
        super(context);
        this.countryIsoPreference = countryIsoPreference;
        this.mccMncPreference = mccMncPreference;
        this.operatorNamePreference = operatorNamePreference;
        setTitle("SIM country preset");
        refreshSummary();
    }

    public void refreshSummary() {
        refreshSummary(
                Settings.SIM_SPOOF_ISO.get(),
                Settings.SIMSPOOF_MCCMNC.get(),
                Settings.SIMSPOOF_OP_NAME.get()
        );
    }

    public void refreshSummary(String iso, String mccMnc, String operatorName) {
        SimPreset selectedPreset = SimPresets.findSelected(iso, mccMnc, operatorName);
        if (selectedPreset != null) {
            setSummary(selectedPreset.country + " - " + selectedPreset.operatorName + " - "
                    + selectedPreset.mccMnc);
        } else if (SimPresets.hasEmptyCurrentValues(iso, mccMnc, operatorName)) {
            setSummary("No preset selected");
        } else {
            setSummary("Custom SIM details");
        }
    }

    @Override
    protected void onClick() {
        showPresetDialog();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        app.morphe.extension.tiktok.Utils.setTitleAndSummaryColor(view);
    }

    private void showPresetDialog() {
        Context context = getContext();
        LinearLayout dialogView = new LinearLayout(context);
        dialogView.setOrientation(LinearLayout.VERTICAL);
        int padding = dpToPx(16);
        dialogView.setPadding(padding, padding, padding, 0);

        TextView helper = new TextView(context);
        helper.setText("Choose a country preset to fill ISO, MCC/MNC, and operator name.");
        helper.setTextColor(getSummaryTextColor());
        dialogView.addView(helper, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        EditText search = new EditText(context);
        search.setSingleLine(true);
        search.setHint("Search country, operator, ISO, or MCC/MNC");
        search.setTextColor(getTitleTextColor());
        search.setHintTextColor(getSummaryTextColor());
        dialogView.addView(search, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        ListView listView = new ListView(context);
        PresetAdapter adapter = new PresetAdapter(context, visiblePresets);
        listView.setAdapter(adapter);
        dialogView.addView(listView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(360)
        ));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("SIM country preset")
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            SimPreset preset = visiblePresets.get(position);
            if (preset != null && savePreset(preset)) {
                dialog.dismiss();
            }
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPresets(s.toString(), adapter);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        filterPresets("", adapter);
        dialog.show();
    }

    private boolean savePreset(SimPreset preset) {
        if (!preset.isValid()) {
            Logger.printException(() -> "Invalid SIM preset refused: "
                    + preset.country + " / " + preset.operatorName + " / "
                    + preset.mccMnc + " / " + preset.iso);
            app.morphe.extension.shared.Utils.showToastLong("Invalid SIM preset");
            return false;
        }

        countryIsoPreference.setText(preset.iso);
        mccMncPreference.setText(preset.mccMnc);
        operatorNamePreference.setText(preset.operatorName);
        refreshSummary(preset.iso, preset.mccMnc, preset.operatorName);

        Logger.printDebug(() -> "SIM preset selected: " + preset.country + " / "
                + preset.operatorName + " / " + preset.mccMnc + " / " + preset.iso);

        if (Settings.SIM_SPOOF.get()) {
            AbstractPreferenceFragment.showRestartDialog(getContext());
        } else {
            app.morphe.extension.shared.Utils.showToastShort("SIM preset saved");
        }

        return true;
    }

    private void filterPresets(String query, PresetAdapter adapter) {
        visiblePresets.clear();
        for (SimPreset preset : SimPresets.PRESETS) {
            if (preset.matches(query)) {
                visiblePresets.add(preset);
            }
        }

        if (visiblePresets.isEmpty()) {
            visiblePresets.add(null);
        }

        adapter.notifyDataSetChanged();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getContext().getResources().getDisplayMetrics().density);
    }

    private static int getTitleTextColor() {
        return app.morphe.extension.shared.Utils.isDarkModeEnabled()
                ? TEXT_DARK_MODE_TITLE
                : TEXT_LIGHT_MODE_TITLE;
    }

    private static int getSummaryTextColor() {
        return app.morphe.extension.shared.Utils.isDarkModeEnabled()
                ? TEXT_DARK_MODE_SUMMARY
                : TEXT_LIGHT_MODE_SUMMARY;
    }

    private static class PresetAdapter extends ArrayAdapter<SimPreset> {
        PresetAdapter(Context context, List<SimPreset> presets) {
            super(context, android.R.layout.simple_list_item_2, android.R.id.text1, presets);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            TextView title = view.findViewById(android.R.id.text1);
            TextView summary = view.findViewById(android.R.id.text2);
            SimPreset preset = getItem(position);

            if (preset == null) {
                title.setText("No matching countries");
                summary.setText("");
                view.setEnabled(false);
            } else {
                title.setText(preset.country);
                summary.setText(preset.getSummary());
                view.setEnabled(true);
            }

            title.setTextColor(getTitleTextColor());
            summary.setTextColor(getSummaryTextColor());
            return view;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItem(position) != null;
        }
    }
}
