package com.marinainsaurralde.plango.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.marinainsaurralde.plango.model.FiltroEventos;
import com.marinainsaurralde.plango.model.PlanFilterSelection;
import com.marinainsaurralde.plango.model.PlanType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Preferencias locales aisladas por usuario autenticado. */
public final class AppPreferences {
    public static final String KEY_DISPLAY_NAME = "display_name";
    public static final String KEY_PLAN_TYPES = "preferred_plan_types";
    public static final String KEY_DATE_PERIOD = "default_date_period";
    public static final String KEY_INITIAL_VIEW = "initial_view";
    public static final String KEY_RESET = "reset_settings";
    public static final String KEY_SIGN_OUT = "sign_out";
    public static final String VIEW_MAP = "MAP";
    public static final String VIEW_LIST = "LIST";

    private static final Set<String> DEFAULT_TYPES =
            Collections.singleton(PlanType.CULTURA.name());

    private final SharedPreferences preferences;

    public AppPreferences(Context context, String uid) {
        preferences = context.getSharedPreferences(fileName(uid), Context.MODE_PRIVATE);
    }

    public static String fileName(String uid) {
        if (uid == null || uid.trim().isEmpty()) throw new IllegalArgumentException("UID requerido");
        return "plango_settings_" + uid;
    }

    public String getDisplayName() {
        String value = preferences.getString(KEY_DISPLAY_NAME, "");
        return value == null ? "" : value.trim();
    }

    public EnumSet<PlanType> getPreferredTypes() {
        return decodeTypes(preferences.getStringSet(KEY_PLAN_TYPES, DEFAULT_TYPES));
    }

    public FiltroEventos.Periodo getDefaultPeriod() {
        return decodePeriod(preferences.getString(KEY_DATE_PERIOD,
                FiltroEventos.Periodo.HOY.name()));
    }

    public boolean startsInList() {
        return VIEW_LIST.equals(preferences.getString(KEY_INITIAL_VIEW, VIEW_MAP));
    }

    public FiltroEventos.Criterios initialCriteria() {
        return new FiltroEventos.Criterios(getDefaultPeriod(), false, null,
                PlanFilterSelection.of(getPreferredTypes(), Collections.emptyMap()));
    }

    public void resetDefaultsKeepingName() {
        preferences.edit()
                .putStringSet(KEY_PLAN_TYPES, new HashSet<>(DEFAULT_TYPES))
                .putString(KEY_DATE_PERIOD, FiltroEventos.Periodo.HOY.name())
                .putString(KEY_INITIAL_VIEW, VIEW_MAP)
                .apply();
    }

    static EnumSet<PlanType> decodeTypes(Set<String> values) {
        EnumSet<PlanType> result = EnumSet.noneOf(PlanType.class);
        if (values != null) for (String value : values) {
            try {
                result.add(PlanType.valueOf(value));
            } catch (IllegalArgumentException | NullPointerException ignored) {
                // Una preferencia antigua o corrupta no debe impedir abrir la aplicación.
            }
        }
        return result;
    }

    static FiltroEventos.Periodo decodePeriod(String value) {
        try {
            return FiltroEventos.Periodo.valueOf(
                    value == null ? "" : value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return FiltroEventos.Periodo.HOY;
        }
    }
}
