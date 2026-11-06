package com.marinainsaurralde.plango.data;

import static org.junit.Assert.*;
import com.marinainsaurralde.plango.model.*;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlanesMergerTest {
    @Test public void verifiedEquivalentMergesIntoCanonicalParkIdentity(){Plan park=new Lugar(Plan.builder(PlanIdentity.PARQUE_MADRID,"5982119","Parque del Oeste").planType(PlanType.NATURALEZA).temporalKind(TemporalKind.LUGAR_PERMANENTE).description("Descripción municipal").distrito("MONCLOA-ARAVACA").coordinates(40.43,-3.72));Plan tourism=new Lugar(Plan.builder(PlanIdentity.TURISMO_MADRID,"69423","Parque del Oeste").planType(PlanType.TURISMO).temporalKind(TemporalKind.LUGAR_PERMANENTE).categoryKey("turismo:7174").schedule("De 6 a 24 h").priceInfo("Acceso libre").priceStatus(PriceStatus.GRATIS).lastSourceUpdate("2026-11-04").coordinates(40.431,-3.721));List<Plan> result=PlanesMerger.mergeTourism(Collections.singletonList(park),Collections.singletonList(tourism));Plan merged=result.get(0);assertEquals(1,result.size());assertEquals("parque-madrid:5982119",merged.getCompositeId());assertEquals("parque-madrid:5982119",merged.getFavoriteDocumentId());assertEquals("Descripción municipal",merged.getDescription());assertEquals("MONCLOA-ARAVACA",merged.getDistrito());assertEquals("De 6 a 24 h",merged.getSchedule());assertTrue(merged.getPlanTypes().containsAll(Arrays.asList(PlanType.NATURALEZA,PlanType.TURISMO)));assertEquals("69423",merged.getSourceRefs().get(PlanIdentity.TURISMO_MADRID));assertEquals("5982119",merged.getSourceRefs().get(PlanIdentity.PARQUE_MADRID));}
    @Test public void nonEquivalentTouristPlaceRemainsIndependent(){Plan tourism=new Lugar(Plan.builder(PlanIdentity.TURISMO_MADRID,"other","Monumento").planType(PlanType.TURISMO));List<Plan> result=PlanesMerger.mergeTourism(Collections.emptyList(),Collections.singletonList(tourism));assertEquals(1,result.size());assertEquals("turismo-madrid:other",result.get(0).getCompositeId());}
    @Test public void equivalenceTableContainsOnlyFifteenReviewedPairs(){assertEquals(15,TurismoNaturalezaEquivalencias.all().size());assertEquals("5982119",TurismoNaturalezaEquivalencias.parkIdForTourism("69423"));assertNull(TurismoNaturalezaEquivalencias.parkIdForTourism("unknown"));}
}
