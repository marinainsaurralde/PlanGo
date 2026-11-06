package com.marinainsaurralde.plango.data;

import static org.junit.Assert.*;
import com.marinainsaurralde.plango.model.*;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TurismoApiClientTest {
    @Test public void parsesXmlWithMultipleCategoriesAndCleanText() throws Exception {
        String xml="<?xml version=\"1.0\"?><serviceList><service id=\"99\" fechaActualizacion=\"2026-11-04\">"
                +"<basicData><name><![CDATA[Jard&iacute;n Tur&iacute;stico]]></name><body><![CDATA[<p>Zona <strong>verde</strong>&nbsp;accesible.</p>]]></body><web>https://example.test</web></basicData>"
                +"<geoData><address><![CDATA[de Alcal&aacute;, 1]]></address><latitude>40.4</latitude><longitude>-3.7</longitude></geoData>"
                +"<extradata><categorias><categoria><item name=\"idCategoria\">7174</item><item name=\"Categoria\">Parques y jardines</item></categoria><categoria><item name=\"idCategoria\">7173</item><item name=\"Categoria\">Edificios y monumentos</item></categoria></categorias>"
                +"<item name=\"Servicios de pago\"><![CDATA[Entrada gratuita]]></item><item name=\"Horario\"><![CDATA[<p>Lun - Dom: 10:00 - 20:00 h</p>]]></item></extradata></service></serviceList>";
        List<Plan> plans=TurismoApiClient.parsePlans(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        Plan plan=plans.get(0);
        assertEquals(1,plans.size());assertEquals("Jardín Turístico",plan.getTitulo());
        assertEquals(PlanIdentity.TURISMO_MADRID,plan.getSource());assertEquals("turismo-madrid:99",plan.getFavoriteDocumentId());
        assertTrue(plan.getPlanTypes().contains(PlanType.TURISMO));assertEquals(TemporalKind.LUGAR_PERMANENTE,plan.getTemporalKind());
        assertTrue(plan.getCategoryKeys().contains("turismo:7174"));assertTrue(plan.getCategoryKeys().contains("turismo:7173"));
        assertEquals("Zona verde accesible.",plan.getDescription());assertEquals("Lun - Dom: 10:00 - 20:00 h",plan.getSchedule());
        assertEquals(PriceStatus.GRATIS,plan.getPriceStatus());assertEquals("Entrada gratuita",plan.getPriceInfo());
        assertEquals("2026-11-04",plan.getLastSourceUpdate());assertNull(plan.getDistrito());assertTrue(plan.hasValidCoordinates());
    }

    @Test public void pricePolicyIsConservative(){assertEquals(PriceStatus.GRATIS,TurismoApiClient.classifyPrice("Acceso gratuito"));assertEquals(PriceStatus.DE_PAGO,TurismoApiClient.classifyPrice("Entrada general: 12 €"));assertEquals(PriceStatus.DESCONOCIDO,TurismoApiClient.classifyPrice("Consultar web oficial"));assertEquals(PriceStatus.DESCONOCIDO,TurismoApiClient.classifyPrice("Visita gratuita a los jardines. Audioguía: 5 €"));assertEquals(PriceStatus.DESCONOCIDO,TurismoApiClient.classifyPrice("Según espectáculo"));}

    @Test public void htmlCleanerDecodesEntitiesAndKeepsParagraphs(){assertEquals("Línea á\nSegunda & última",HtmlTextCleaner.clean("<p>L&iacute;nea &aacute;</p><p>Segunda &amp; &uacute;ltima</p>"));}
    @Test public void rejectsDoctypeAndExternalEntities() throws Exception {String xml="<?xml version=\"1.0\"?><!DOCTYPE serviceList [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><serviceList/>";try{TurismoApiClient.parsePlans(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));fail("DOCTYPE should be rejected");}catch(org.xml.sax.SAXException expected){assertTrue(true);}}
}
