package com.marinainsaurralde.plango.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.marinainsaurralde.plango.model.Plan;
import com.marinainsaurralde.plango.model.PlanIdentity;
import com.marinainsaurralde.plango.model.PlanType;
import com.marinainsaurralde.plango.model.PriceStatus;
import com.marinainsaurralde.plango.model.TemporalKind;
import org.junit.Test;
import java.util.List;

public class ParquesApiClientTest {
    @Test public void parsesPermanentParksWithoutInventingPriceOrDates() throws Exception {
        String json="{\"@graph\":[{\"id\":\"123\",\"title\":\"Parque Uno\","
                + "\"relation\":\"https://madrid.es/parque\","
                + "\"address\":{\"street-address\":\"CALLE VERDE 1\",\"district\":{\"@id\":\"https://datos.madrid.es/Distrito/Retiro\"}},"
                + "\"location\":{\"latitude\":40.4,\"longitude\":-3.7},"
                + "\"organization\":{\"organization-name\":\"Parque Uno\",\"organization-desc\":\"Zona verde\",\"schedule\":\"De 8 a 22 h\"}},"
                + "{\"id\":\"124\",\"title\":\"Coordenadas inválidas\",\"address\":{},"
                + "\"location\":{\"latitude\":100.0,\"longitude\":-3.7},\"organization\":{}}]}";
        List<Plan> plans=ParquesApiClient.parsePlans(json);
        Plan park=plans.get(0);
        assertEquals(2,plans.size());
        assertEquals(PlanIdentity.PARQUE_MADRID,park.getSource());
        assertEquals("parque-madrid:123",park.getCompositeId());
        assertEquals("parque-madrid:123",park.getFavoriteDocumentId());
        assertEquals(TemporalKind.LUGAR_PERMANENTE,park.getTemporalKind());
        assertEquals(PriceStatus.DESCONOCIDO,park.getPriceStatus());
        assertTrue(park.getPlanTypes().contains(PlanType.NATURALEZA));
        assertTrue(park.hasValidCoordinates());
        assertFalse(plans.get(1).hasValidCoordinates());
        assertEquals("De 8 a 22 h",park.getSchedule());
        assertEquals("CALLE VERDE 1",park.getAddress());
    }
}
