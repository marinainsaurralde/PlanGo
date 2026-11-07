package com.marinainsaurralde.plango.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.marinainsaurralde.plango.R;
import com.marinainsaurralde.plango.data.EventosStore;
import com.marinainsaurralde.plango.model.CategoriaCultural;
import com.marinainsaurralde.plango.model.CategoriaDeportiva;
import com.marinainsaurralde.plango.model.CategoriaTuristica;
import com.marinainsaurralde.plango.model.FiltroEventos;
import com.marinainsaurralde.plango.model.Plan;
import com.marinainsaurralde.plango.model.PlanFilterDraft;
import com.marinainsaurralde.plango.model.PlanType;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ExplorarPlanesBottomSheet {
    private final AppCompatActivity activity;
    private final EventosStore store;
    private final Runnable onApplied;
    private BottomSheetDialog dialog;
    private View content;
    private PlanFilterDraft draft;
    private Map<PlanType, LinkedHashMap<String, String>> options;
    private PlanType openType;

    public ExplorarPlanesBottomSheet(AppCompatActivity activity, EventosStore store,
                                     Runnable onApplied) {
        this.activity = activity;
        this.store = store;
        this.onApplied = onApplied;
    }

    public void show() {
        dialog = new BottomSheetDialog(activity);
        content = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_explore_plans, null);
        dialog.setContentView(content);
        draft = new PlanFilterDraft(store.getCriterios().getPlanSelection());
        options = buildOptions(store.getEventos());
        openType = firstSelectedType();
        render();
        content.findViewById(R.id.buttonApplyPlanFilters).setOnClickListener(view -> apply());
        content.findViewById(R.id.buttonClearPlanFilters)
                .setOnClickListener(view -> toggleOpenTypeCategories());
        dialog.show();
        configureLandscapeHeight();
    }

    private void configureLandscapeHeight() {
        if (activity.getResources().getConfiguration().orientation
                != Configuration.ORIENTATION_LANDSCAPE) return;
        FrameLayout sheet = dialog.findViewById(
                com.google.android.material.R.id.design_bottom_sheet);
        if (sheet == null) return;
        Rect visibleFrame = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(visibleFrame);
        int availableHeight = visibleFrame.height() - dp(12);
        ViewGroup.LayoutParams params = sheet.getLayoutParams();
        params.height = Math.max(dp(240), availableHeight);
        sheet.setLayoutParams(params);
        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(sheet);
        behavior.setMaxHeight(availableHeight);
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void render() {
        GridLayout typeGrid = content.findViewById(R.id.gridPlanTypes);
        typeGrid.removeAllViews();
        addTypeCard(typeGrid, PlanType.CULTURA, R.id.buttonPlanTypeCulture,
                R.id.bodyPlanTypeCulture, "🎭", R.string.plan_type_culture);
        addTypeCard(typeGrid, PlanType.DEPORTE, R.id.buttonPlanTypeSport,
                R.id.bodyPlanTypeSport, "⚽", R.string.plan_type_sport);
        addTypeCard(typeGrid, PlanType.NATURALEZA, R.id.buttonPlanTypeNature,
                R.id.bodyPlanTypeNature, "🌿", R.string.plan_type_nature_short);
        addTypeCard(typeGrid, PlanType.TURISMO, R.id.buttonPlanTypeTourism,
                R.id.bodyPlanTypeTourism, "★", R.string.plan_type_tourism_short);

        LinearLayout sections = content.findViewById(R.id.layoutPlanCategories);
        sections.removeAllViews();
        MaterialButton toggleAll = content.findViewById(R.id.buttonClearPlanFilters);
        LinkedHashMap<String, String> openOptions = openType == null ? null : options.get(openType);
        boolean hasCategories = openOptions != null && !openOptions.isEmpty();
        toggleAll.setVisibility(hasCategories ? View.VISIBLE : View.GONE);
        if (hasCategories) {
            toggleAll.setText(areAllOpenCategoriesSelected(openOptions)
                    ? R.string.clear_plan_filters : R.string.select_all_plan_filters);
        }
        if (openType == PlanType.NATURALEZA) {
            addTypeSectionHeader(sections, openType);
            addNatureMessage(sections);
        }
        else if (openType != null) {
            LinkedHashMap<String, String> typeOptions = options.get(openType);
            if (typeOptions != null && !typeOptions.isEmpty()) {
                addCategorySection(sections, openType, typeOptions);
            }
        }
    }

    private void addTypeCard(GridLayout grid, PlanType type, int cardId,
                             int bodyId, String icon, int labelId) {
        boolean selected = draft.isTypeSelected(type);
        boolean open = openType == type;
        MaterialCardView card = new MaterialCardView(activity);
        card.setId(cardId);
        card.setSelected(selected);
        card.setActivated(open);
        card.setRadius(dp(14));
        card.setCardElevation(open ? dp(4) : dp(1));
        card.setStrokeWidth(dp(open ? 3 : selected ? 2 : 1));
        card.setStrokeColor(ContextCompat.getColor(activity,
                open ? R.color.plango_coral
                        : selected ? R.color.plango_primary : R.color.plango_card_stroke));
        card.setCardBackgroundColor(ContextCompat.getColor(activity,
                open ? R.color.plango_turquoise : R.color.white));

        LinearLayout row = new LinearLayout(activity);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout body = new LinearLayout(activity);
        body.setId(bodyId);
        body.setClickable(true);
        body.setFocusable(true);
        body.setGravity(android.view.Gravity.CENTER_VERTICAL);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14), dp(6), dp(8), dp(6));
        TextView title = new TextView(activity);
        title.setText(icon + "  " + activity.getString(labelId));
        title.setTextColor(ContextCompat.getColor(activity, R.color.plango_text));
        title.setTextSize(14);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        body.addView(title);
        if (open) {
            TextView status = new TextView(activity);
            status.setText(R.string.categories_open);
            status.setTextColor(ContextCompat.getColor(activity, R.color.plango_coral));
            status.setTextSize(11);
            body.addView(status);
        }
        body.setOnClickListener(view -> {
            openType = type;
            render();
        });
        row.addView(body, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        card.addView(row);
        int position = grid.getChildCount();
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                GridLayout.spec(position / 2, 1f),
                GridLayout.spec(position % 2, 1f));
        params.width = 0;
        params.height = dp(76);
        params.setMargins(dp(4), dp(4), dp(4), dp(4));
        card.setLayoutParams(params);
        grid.addView(card);
    }

    private void addCategorySection(LinearLayout parent, PlanType type,
                                    LinkedHashMap<String, String> typeOptions) {
        addTypeSectionHeader(parent, type);

        GridLayout grid = new GridLayout(activity);
        grid.setColumnCount(2);
        for (Map.Entry<String, String> option : typeOptions.entrySet()) {
            MaterialCheckBox checkBox = categoryCheckBox(option.getValue(),
                    draft.isSpecificCategorySelected(type, option.getKey()));
            checkBox.setOnCheckedChangeListener((button, checked) -> {
                if (checked && !draft.isTypeSelected(type)) {
                    draft.setTypeSelected(type, true);
                }
                draft.setSpecificCategorySelected(type, option.getKey(), checked);
                render();
            });
            grid.addView(checkBox);
        }
        parent.addView(grid, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    private void apply() {
        FiltroEventos.Criterios current = store.getCriterios();
        store.setCriterios(new FiltroEventos.Criterios(current.getPeriodo(),
                current.isSoloGratuitos(), current.getDistrito(), draft.toSelection()));
        onApplied.run();
        dialog.dismiss();
    }

    private void toggleOpenTypeCategories() {
        if (openType == null) return;
        LinkedHashMap<String, String> typeOptions = options.get(openType);
        if (typeOptions == null || typeOptions.isEmpty()) return;
        boolean selectAll = !areAllOpenCategoriesSelected(typeOptions);
        if (selectAll && !draft.isTypeSelected(openType)) {
            draft.setTypeSelected(openType, true);
        }
        if (selectAll) {
            draft.setAllCategories(openType, false);
            for (String key : typeOptions.keySet()) {
                draft.setSpecificCategorySelected(openType, key, true);
            }
        } else {
            draft.setAllCategories(openType, true);
        }
        render();
    }

    private boolean areAllOpenCategoriesSelected(Map<String, String> typeOptions) {
        if (openType == null || typeOptions.isEmpty()) return false;
        for (String key : typeOptions.keySet()) {
            if (!draft.isSpecificCategorySelected(openType, key)) return false;
        }
        return true;
    }

    private void addNatureMessage(LinearLayout parent) {
        TextView message = new TextView(activity);
        message.setText(R.string.nature_all_included);
        message.setTextColor(ContextCompat.getColor(activity, R.color.plango_text_secondary));
        message.setTextSize(14);
        message.setPadding(dp(4), dp(14), dp(4), dp(8));
        parent.addView(message);
    }

    private void addTypeSectionHeader(LinearLayout parent, PlanType type) {
        LinearLayout header = new LinearLayout(activity);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(dp(4), dp(10), dp(4), dp(4));
        TextView title = new TextView(activity);
        title.setText(typeLabel(type));
        title.setTextColor(ContextCompat.getColor(activity, R.color.plango_primary));
        title.setTextSize(16);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        MaterialButton toggle = new MaterialButton(activity, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        toggle.setId(R.id.buttonToggleOpenPlanType);
        toggle.setText(draft.isTypeSelected(type)
                ? R.string.remove_plan_type_filter : R.string.add_plan_type_filter);
        toggle.setTextSize(12);
        toggle.setOnClickListener(view -> {
            draft.setTypeSelected(type, !draft.isTypeSelected(type));
            render();
        });
        header.addView(toggle, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(44)));
        parent.addView(header);
    }

    private PlanType firstSelectedType() {
        for (PlanType type : PlanType.values()) if (draft.isTypeSelected(type)) return type;
        return null;
    }

    private MaterialCheckBox categoryCheckBox(String label, boolean checked) {
        MaterialCheckBox checkBox = new MaterialCheckBox(activity);
        checkBox.setText(label);
        checkBox.setTextColor(ContextCompat.getColor(activity, R.color.plango_text));
        checkBox.setChecked(checked);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED),
                GridLayout.spec(GridLayout.UNDEFINED, 1f));
        params.width = 0;
        params.setMargins(dp(2), 0, dp(2), 0);
        checkBox.setLayoutParams(params);
        return checkBox;
    }

    private Map<PlanType, LinkedHashMap<String, String>> buildOptions(List<Plan> plans) {
        EnumMap<PlanType, LinkedHashMap<String, String>> result = new EnumMap<>(PlanType.class);
        LinkedHashMap<String, String> culture = new LinkedHashMap<>();
        for (CategoriaCultural category : CategoriaCultural.values()) {
            culture.put(FiltroEventos.culturalCategoryKey(category), culturalLabel(category));
        }
        result.put(PlanType.CULTURA, culture);

        LinkedHashMap<String, String> sports = new LinkedHashMap<>();
        for (CategoriaDeportiva category : CategoriaDeportiva.values()) {
            sports.put(category.getKey(), category.getLabel());
        }
        result.put(PlanType.DEPORTE, sports);

        LinkedHashMap<String, String> tourism = new LinkedHashMap<>();
        if (plans != null) for (Plan plan : plans) for (String key : plan.getCategoryKeys()) {
            if (!key.startsWith("turismo:")) continue;
            String label = CategoriaTuristica.labelFor(key);
            tourism.putIfAbsent(key, label == null ? key.substring("turismo:".length()) : label);
        }
        result.put(PlanType.TURISMO, tourism);
        return result;
    }

    private String culturalLabel(CategoriaCultural category) {
        switch (category) {
            case TEATRO_ESPECTACULOS: return activity.getString(R.string.category_theater);
            case MUSICA: return activity.getString(R.string.category_music);
            case CINE: return activity.getString(R.string.category_cinema);
            case EXPOSICIONES: return activity.getString(R.string.category_exhibitions);
            case TALLERES_ACTIVIDADES: return activity.getString(R.string.category_workshops);
            case CONFERENCIAS: return activity.getString(R.string.category_conferences);
            default: return activity.getString(R.string.category_other);
        }
    }

    private String typeLabel(PlanType type) {
        switch (type) {
            case CULTURA: return activity.getString(R.string.plan_type_culture);
            case DEPORTE: return activity.getString(R.string.plan_type_sport);
            case NATURALEZA: return activity.getString(R.string.plan_type_nature_short);
            default: return activity.getString(R.string.plan_type_tourism_short);
        }
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
