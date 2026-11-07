package com.marinainsaurralde.plango.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Estado editable del panel; no afecta a los resultados hasta que se aplica. */
public final class PlanFilterDraft {
    private final EnumSet<PlanType> types = EnumSet.noneOf(PlanType.class);
    private final EnumMap<PlanType, Set<String>> restrictions = new EnumMap<>(PlanType.class);

    public PlanFilterDraft(PlanFilterSelection selection) {
        replaceWith(selection);
    }

    public void replaceWith(PlanFilterSelection selection) {
        types.clear();
        restrictions.clear();
        if (selection == null) selection = PlanFilterSelection.culturaPorDefecto();
        types.addAll(selection.getSelectedTypes());
        for (Map.Entry<PlanType, Set<String>> entry
                : selection.getCategoryRestrictions().entrySet()) {
            restrictions.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
        }
    }

    public boolean isTypeSelected(PlanType type) { return types.contains(type); }
    public void setTypeSelected(PlanType type, boolean selected) {
        if (selected) {
            types.add(type);
            restrictions.remove(type); // Un tipo recién seleccionado incluye todas sus categorías.
        } else {
            types.remove(type);
            restrictions.remove(type);
        }
    }

    public boolean includesAllCategories(PlanType type) {
        return !restrictions.containsKey(type);
    }

    public boolean isSpecificCategorySelected(PlanType type, String key) {
        return restrictions.containsKey(type) && restrictions.get(type).contains(key);
    }

    public void setAllCategories(PlanType type, boolean selected) {
        if (selected) restrictions.remove(type);
        else restrictions.put(type, new LinkedHashSet<>());
    }

    public void setSpecificCategorySelected(PlanType type, String key, boolean selected) {
        Set<String> values = restrictions.get(type);
        if (values == null) values = new LinkedHashSet<>();
        if (selected) {
            values.add(key);
            restrictions.put(type, values);
        } else {
            values.remove(key);
            if (values.isEmpty()) restrictions.remove(type);
            else restrictions.put(type, values);
        }
    }

    public void clearAll() { replaceWith(PlanFilterSelection.vacia()); }

    public PlanFilterSelection toSelection() {
        return PlanFilterSelection.of(types, restrictions);
    }
}
