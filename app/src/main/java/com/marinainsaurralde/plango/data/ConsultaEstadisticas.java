package com.marinainsaurralde.plango.data;

import com.marinainsaurralde.plango.model.ConsultaHistorial;
import com.marinainsaurralde.plango.model.Evento;
import com.marinainsaurralde.plango.model.MuestraEvento;
import java.util.ArrayList; import java.util.Date; import java.util.LinkedHashMap; import java.util.List; import java.util.Locale; import java.util.Map;

public final class ConsultaEstadisticas {
    public static final int MAX_MUESTRA = 15;
    private ConsultaEstadisticas() {}
    public static ConsultaHistorial calcular(List<Evento> eventos, String origen) {
        List<Evento> safe = eventos == null ? java.util.Collections.emptyList() : eventos;
        int gratis=0, pago=0, sin=0; Map<String,Integer> distritos=new LinkedHashMap<>(), tipos=new LinkedHashMap<>();
        List<MuestraEvento> muestra=new ArrayList<>();
        for (Evento e:safe) {
            if (!e.isGratuidadInformada()) sin++; else if (e.isGratuito()) gratis++; else pago++;
            increment(distritos, clean(e.getDistrito())); increment(tipos, category(e.getTipo()));
            if (muestra.size()<MAX_MUESTRA) muestra.add(new MuestraEvento(e.getIdentificador(),e.getTitulo(),e.getFechaInicio(),e.getFechaFin(),e.getNombreInstalacion(),e.getDistrito()));
        }
        return new ConsultaHistorial(null,new Date(),safe.size(),gratis,pago,sin,distritos,tipos,muestra,origen);
    }
    private static void increment(Map<String,Integer> map,String key){if(key!=null)map.put(key,map.getOrDefault(key,0)+1);}
    private static String clean(String s){if(s==null||s.trim().isEmpty())return null;return s.trim();}
    private static String category(String tipo){String value=clean(tipo);if(value==null)return null;int slash=value.lastIndexOf('/');return slash>=0&&slash<value.length()-1?value.substring(slash+1):value;}
}
