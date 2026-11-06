package com.marinainsaurralde.plango.data;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@RunWith(AndroidJUnit4.class)
public final class TurismoApiClientInstrumentedTest {

    @Test
    public void parsePlans_usesAnAndroidCompatibleXmlParser() throws Exception {
        String xml = "<serviceList><service id=\"tur-1\">"
                + "<basicData><name>Mirador de prueba</name></basicData>"
                + "<geoData><latitude>40.4</latitude><longitude>-3.7</longitude></geoData>"
                + "</service></serviceList>";

        assertEquals(1, TurismoApiClient.parsePlans(new ByteArrayInputStream(
                xml.getBytes(StandardCharsets.UTF_8))).size());
    }
}
