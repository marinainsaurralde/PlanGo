package com.marinainsaurralde.plango.model;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import com.marinainsaurralde.plango.data.AgrupadorEventos;

public class CategoriaCulturalTest {
    private Evento event(String id, String type, String district, boolean free, double lat) {
        return new Evento(id,id,"2026-11-05 00:00:00.0","2026-11-05 23:59:00.0",
                district,type,free,lat,-3.7,"Lugar",null,null);
    }

    @Test public void classifiesReliableApiTypes() {
        assertEquals(CategoriaCultural.TEATRO_ESPECTACULOS, CategoriaCultural.clasificar(event("1","/TeatroPerformance","CENTRO",true,40.1)));
        assertEquals(CategoriaCultural.MUSICA, CategoriaCultural.clasificar(event("2","/Musica/JazzSoulFunkySwingReagge","CENTRO",true,40.2)));
        assertEquals(CategoriaCultural.CINE, CategoriaCultural.clasificar(event("3","/CineActividadesAudiovisuales","CENTRO",true,40.3)));
        assertEquals(CategoriaCultural.EXPOSICIONES, CategoriaCultural.clasificar(event("4","/Exposiciones","CENTRO",true,40.4)));
        assertEquals(CategoriaCultural.TALLERES_ACTIVIDADES, CategoriaCultural.clasificar(event("5","/CursosTalleres","CENTRO",true,40.5)));
        assertEquals(CategoriaCultural.CONFERENCIAS, CategoriaCultural.clasificar(event("6","/ConferenciasColoquios","CENTRO",true,40.6)));
        assertEquals(CategoriaCultural.OTRAS, CategoriaCultural.clasificar(event("7","/TipoDesconocido","CENTRO",true,40.7)));
    }

    @Test public void categoryCombinesWithDateFreeAndDistrictAndUpdatesGroups() {
        List<Evento> events=Arrays.asList(
                event("match","/Musica","CENTRO",true,40.1),
                event("paid","/Musica","CENTRO",false,40.1),
                event("otherDistrict","/Musica","RETIRO",true,40.1),
                event("cinema","/CineActividadesAudiovisuales","CENTRO",true,40.1));
        FiltroEventos.Criterios criteria=new FiltroEventos.Criterios(
                FiltroEventos.Periodo.HOY,true,"CENTRO",CategoriaCultural.MUSICA);
        List<GrupoUbicacion> unfilteredGroups=AgrupadorEventos.agrupar(events);
        List<Evento> filtered=FiltroEventos.aplicar(events,criteria,LocalDate.of(2026,11,5));
        assertEquals(4,unfilteredGroups.get(0).size());
        assertEquals(1,filtered.size());
        assertEquals(1,AgrupadorEventos.agrupar(filtered).get(0).size());
    }

    @Test public void nullCategoryMeansAll() {
        List<Evento> events=Arrays.asList(event("music","/Musica","CENTRO",true,40.1),event("cinema","/CineActividadesAudiovisuales","CENTRO",true,40.2));
        FiltroEventos.Criterios all=new FiltroEventos.Criterios(FiltroEventos.Periodo.TODOS,false,null,null);
        assertEquals(2,FiltroEventos.aplicar(events,all,LocalDate.of(2026,11,5)).size());
    }
}
