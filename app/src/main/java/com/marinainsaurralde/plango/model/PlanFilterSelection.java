package com.marinainsaurralde.plango.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Selección inmutable de tipos y categorías del panel Explorar planes. */
public final class PlanFilterSelection {
    private final Set<PlanType> selectedTypes;
    private final Map<PlanType, Set<String>> categoryRestrictions;

    private PlanFilterSelection(Set<PlanType> types, Map<PlanType, Set<String>> restrictions) {
        EnumSet<PlanType> safeTypes = types == null || types.isEmpty()
                ? EnumSet.noneOf(PlanType.class) : EnumSet.copyOf(types);
        selectedTypes = Collections.unmodifiableSet(safeTypes);
        EnumMap<PlanType, Set<String>> safeRestrictions = new EnumMap<>(PlanType.class);
        if (restrictions != null) for (Map.Entry<PlanType, Set<String>> entry : restrictions.entrySet()) {
            if (entry.getKey() != null && safeTypes.contains(entry.getKey())) {
                safeRestrictions.put(entry.getKey(), Collections.unmodifiableSet(
                        new LinkedHashSet<>(entry.getValue() == null
                                ? Collections.emptySet() : entry.getValue())));
            }
        }
        categoryRestrictions = Collections.unmodifiableMap(safeRestrictions);
    }

    public static PlanFilterSelection culturaPorDefecto() {
        return of(EnumSet.of(PlanType.CULTURA), Collections.emptyMap());
    }

    public static PlanFilterSelection vacia() {
        return of(EnumSet.noneOf(PlanType.class), Collections.emptyMap());
    }

    public static PlanFilterSelection todosLosTipos() {
        return of(EnumSet.allOf(PlanType.class), Collections.emptyMap());
    }

    public static PlanFilterSelection soloCategoriaCultural(CategoriaCultural category) {
        EnumMap<PlanType, Set<String>> restrictions = new EnumMap<>(PlanType.class);
        restrictions.put(PlanType.CULTURA, Collections.singleton(
                FiltroEventos.culturalCategoryKey(category)));
        return of(EnumSet.of(PlanType.CULTURA), restrictions);
    }

    public static PlanFilterSelection of(Set<PlanType> types,
                                         Map<PlanType, Set<String>> restrictions) {
        return new PlanFilterSelection(types, restrictions);
    }

    public Set<PlanType> getSelectedTypes() { return selectedTypes; }
    public boolean isTypeSelected(PlanType type) { return selectedTypes.contains(type); }
    public boolean hasCategoryRestriction(PlanType type) {
        return categoryRestrictions.containsKey(type);
    }
    public Set<String> getSelectedCategoryKeys(PlanType type) {
        Set<String> values = categoryRestrictions.get(type);
        return values == null ? Collections.emptySet() : values;
    }
    public Map<PlanType, Set<String>> getCategoryRestrictions() {
        return categoryRestrictions;
    }
}
