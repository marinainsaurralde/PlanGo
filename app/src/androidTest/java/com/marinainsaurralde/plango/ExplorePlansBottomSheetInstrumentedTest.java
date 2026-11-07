package com.marinainsaurralde.plango;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.matcher.ViewMatchers.isActivated;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isSelected;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Intent;
import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.marinainsaurralde.plango.data.EventosStore;
import com.marinainsaurralde.plango.model.Evento;
import com.marinainsaurralde.plango.model.CategoriaDeportiva;
import com.marinainsaurralde.plango.model.FiltroEventos;
import com.marinainsaurralde.plango.model.PlanFilterSelection;
import com.marinainsaurralde.plango.model.PlanType;
import com.marinainsaurralde.plango.ui.ExplorarPlanesBottomSheet;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(AndroidJUnit4.class)
public final class ExplorePlansBottomSheetInstrumentedTest {

    @Test public void applyAndClearUpdateSharedSelectionWithoutReloadingData() {
        EventosStore store = EventosStore.getInstance();
        Evento event = sampleEvent();
        store.setEventos(Collections.singletonList(event));
        store.setCriterios(FiltroEventos.Criterios.porDefecto());
        AtomicInteger applied = new AtomicInteger();
        Activity activity = startHost();

        showSheet(activity, store, applied);
        onView(withId(R.id.buttonPlanTypeCulture)).check(matches(isSelected()));
        onView(withId(R.id.buttonPlanTypeSport)).check(matches(not(isSelected())));
        onView(withId(R.id.buttonPlanTypeNature)).check(matches(not(isSelected())));
        onView(withId(R.id.buttonPlanTypeTourism)).check(matches(not(isSelected())));
        onView(withId(R.id.buttonPlanTypeCulture)).check(matches(isActivated()));
        onView(withId(R.id.bodyPlanTypeSport)).perform(click());
        onView(withId(R.id.buttonPlanTypeSport)).check(matches(isActivated()));
        onView(withId(R.id.buttonPlanTypeSport)).check(matches(not(isSelected())));
        onView(withId(R.id.buttonPlanTypeCulture)).check(matches(not(isActivated())));
        onView(withId(R.id.buttonToggleOpenPlanType)).perform(click());
        onView(withId(R.id.buttonPlanTypeSport)).check(matches(isSelected()));
        onView(withText(CategoriaDeportiva.CICLISMO.getLabel())).perform(click());
        onView(withId(R.id.bodyPlanTypeCulture)).perform(click());
        onView(withText(R.string.category_music)).perform(click());
        onView(withId(R.id.bodyPlanTypeSport)).perform(click());
        onView(withText(CategoriaDeportiva.CICLISMO.getLabel())).check(matches(isChecked()));
        onView(withId(R.id.bodyPlanTypeCulture)).perform(click());
        onView(withText(R.string.category_music)).check(matches(isChecked()));
        onView(withId(R.id.buttonApplyPlanFilters)).perform(click());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertEquals(1, applied.get());
        assertTrue(store.getCriterios().getPlanSelection().isTypeSelected(PlanType.CULTURA));
        assertTrue(store.getCriterios().getPlanSelection().isTypeSelected(PlanType.DEPORTE));
        assertTrue(store.getCriterios().getPlanSelection()
                .hasCategoryRestriction(PlanType.CULTURA));
        assertSame(event, store.getEventos().get(0));

        showSheet(activity, store, applied);
        onView(withId(R.id.buttonClearPlanFilters)).perform(click());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertEquals(1, applied.get());
        onView(withId(R.id.buttonPlanTypeCulture)).check(matches(not(isSelected())));
        onView(withId(R.id.buttonPlanTypeSport)).check(matches(not(isSelected())));
        onView(withId(R.id.buttonPlanTypeNature)).check(matches(not(isSelected())));
        onView(withId(R.id.buttonPlanTypeTourism)).check(matches(not(isSelected())));
        onView(withId(R.id.buttonApplyPlanFilters)).check(matches(isDisplayed()));
        onView(withText(R.string.category_music)).check(doesNotExist());
        assertTrue(store.getCriterios().getPlanSelection().isTypeSelected(PlanType.CULTURA));
        assertTrue(store.getCriterios().getPlanSelection().isTypeSelected(PlanType.DEPORTE));

        onView(withId(R.id.bodyPlanTypeTourism)).perform(click());
        onView(withId(R.id.buttonPlanTypeTourism)).check(matches(not(isSelected())));
        onView(withId(R.id.buttonToggleOpenPlanType)).perform(click());
        onView(withId(R.id.buttonPlanTypeTourism)).check(matches(isSelected()));
        onView(withId(R.id.buttonApplyPlanFilters)).perform(click());
        assertEquals(2, applied.get());
        assertEquals(Collections.singleton(PlanType.TURISMO),
                store.getCriterios().getPlanSelection().getSelectedTypes());

        showSheet(activity, store, applied);
        onView(withId(R.id.buttonPlanTypeTourism)).check(matches(isSelected()));
        onView(withId(R.id.bodyPlanTypeCulture)).perform(click());
        pressBack();
        assertEquals(Collections.singleton(PlanType.TURISMO),
                store.getCriterios().getPlanSelection().getSelectedTypes());

        showSheet(activity, store, applied);
        onView(withId(R.id.buttonClearPlanFilters)).perform(click());
        onView(withId(R.id.buttonApplyPlanFilters)).perform(click());
        assertEquals(3, applied.get());
        assertTrue(store.getCriterios().getPlanSelection().getSelectedTypes().isEmpty());
        assertSame(event, store.getEventos().get(0));
        activity.finish();
    }

    private Activity startHost() {
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                ExplorePlansHostActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activity = InstrumentationRegistry.getInstrumentation().startActivitySync(intent);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        return activity;
    }

    private void showSheet(Activity activity, EventosStore store, AtomicInteger applied) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                new ExplorarPlanesBottomSheet((ExplorePlansHostActivity) activity, store,
                        applied::incrementAndGet).show());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        SystemClock.sleep(500);
    }

    private Evento sampleEvent() {
        return new Evento("sheet-test", "Concierto", "2026-11-06 18:00:00.0",
                "2026-11-06 20:00:00.0", "CENTRO", "/Musica", true,
                40.4168, -3.7038, "Centro cultural", null, null);
    }
}
