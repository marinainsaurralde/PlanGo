package com.marinainsaurralde.plango.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class FiltroPlanesTest {
    private Plan park(PriceStatus price) {
        return new Lugar(Plan.builder(PlanIdentity.PARQUE_MADRID,"p1","Parque")
                .planType(PlanType.NATURALEZA).temporalKind(TemporalKind.LUGAR_PERMANENTE)
                .priceStatus(price).distrito("RETIRO"));
    }

    @Test public void permanentPlaceRemainsVisibleForEveryDatePeriod() {
        Plan park=park(PriceStatus.DESCONOCIDO);
        for(FiltroEventos.Periodo period:FiltroEventos.Periodo.values()) {
            List<Plan> result=FiltroEventos.aplicar(Arrays.asList(park),
                    new FiltroEventos.Criterios(period,false,null),LocalDate.of(2026,11,6));
            assertEquals(1,result.size());
        }
    }

    @Test public void unknownParkIsNotTreatedAsFree() {
        List<Plan> result=FiltroEventos.aplicar(Arrays.asList(park(PriceStatus.DESCONOCIDO)),
                new FiltroEventos.Criterios(FiltroEventos.Periodo.HOY,true,null),LocalDate.of(2026,11,6));
        assertTrue(result.isEmpty());
    }

    @Test public void culturalCategoryDoesNotArtificiallyClassifyPark() {
        List<Plan> result=FiltroEventos.aplicar(Arrays.asList(park(PriceStatus.DESCONOCIDO)),
                new FiltroEventos.Criterios(FiltroEventos.Periodo.TODOS,false,null,CategoriaCultural.OTRAS),
                LocalDate.of(2026,11,6));
        assertTrue(result.isEmpty());
    }
}
