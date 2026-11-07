package com.marinainsaurralde.plango;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.firebase.ui.auth.AuthUI;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.marinainsaurralde.plango.data.AppPreferences;
import com.marinainsaurralde.plango.data.EventosStore;

import java.util.Set;

public final class SettingsFragment extends PreferenceFragmentCompat {
    private AppPreferences appPreferences;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            requireActivity().finish();
            return;
        }
        getPreferenceManager().setSharedPreferencesName(AppPreferences.fileName(user.getUid()));
        setPreferencesFromResource(R.xml.preferences, rootKey);
        appPreferences = new AppPreferences(requireContext(), user.getUid());
        configureSummaries();
        Preference reset = findPreference(AppPreferences.KEY_RESET);
        if (reset != null) reset.setOnPreferenceClickListener(preference -> {
            confirmReset();
            return true;
        });
        Preference signOut = findPreference(AppPreferences.KEY_SIGN_OUT);
        if (signOut != null) signOut.setOnPreferenceClickListener(preference -> {
            confirmSignOut();
            return true;
        });
    }

    private void configureSummaries() {
        EditTextPreference name = findPreference(AppPreferences.KEY_DISPLAY_NAME);
        if (name != null) name.setSummaryProvider(preference -> {
            String value = ((EditTextPreference) preference).getText();
            return value == null || value.trim().isEmpty()
                    ? getString(R.string.settings_name_empty) : value.trim();
        });
        MultiSelectListPreference types = findPreference(AppPreferences.KEY_PLAN_TYPES);
        if (types != null) types.setSummaryProvider(preference -> typeSummary(
                ((MultiSelectListPreference) preference).getValues()));
    }

    private String typeSummary(Set<String> values) {
        if (values == null || values.isEmpty()) return getString(R.string.settings_no_plan_types);
        StringBuilder summary = new StringBuilder();
        String[] entryValues = getResources().getStringArray(R.array.settings_plan_type_values);
        String[] entries = getResources().getStringArray(R.array.settings_plan_type_entries);
        for (int index = 0; index < entryValues.length; index++) {
            if (!values.contains(entryValues[index])) continue;
            if (summary.length() > 0) summary.append(", ");
            summary.append(entries[index]);
        }
        return summary.toString();
    }

    private void confirmReset() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_reset_title)
                .setMessage(R.string.settings_reset_confirmation)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.settings_reset_action, (dialog, which) -> {
                    appPreferences.resetDefaultsKeepingName();
                    requireActivity().recreate();
                })
                .show();
    }

    private void confirmSignOut() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_sign_out_title)
                .setMessage(R.string.settings_sign_out_confirmation)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.sign_out, (dialog, which) -> signOut())
                .show();
    }

    private void signOut() {
        AuthUI.getInstance().signOut(requireContext()).addOnCompleteListener(task -> {
            if (!isAdded()) return;
            if (!task.isSuccessful()) {
                Snackbar.make(requireView(), R.string.sign_out_error, Snackbar.LENGTH_LONG).show();
                return;
            }
            EventosStore.getInstance().endSession();
            Intent intent = new Intent(requireContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
