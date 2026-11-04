package com.marinainsaurralde.plango.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public final class ConsultaHistorial implements Serializable {
    private static final long serialVersionUID = 1L;
    public final String id;
    public final Date fechaConsulta;
    public final int totalEventos, totalGratuitos, totalPago, totalSinInformacion;
    public final Map<String, Integer> eventosPorDistrito, eventosPorTipo;
    public final List<MuestraEvento> muestraEventos;
    public final String origen;

    public ConsultaHistorial(String id, Date fechaConsulta, int totalEventos, int totalGratuitos,
            int totalPago, int totalSinInformacion, Map<String, Integer> distritos,
            Map<String, Integer> tipos, List<MuestraEvento> muestra, String origen) {
        this.id=id; this.fechaConsulta=fechaConsulta; this.totalEventos=totalEventos;
        this.totalGratuitos=totalGratuitos; this.totalPago=totalPago;
        this.totalSinInformacion=totalSinInformacion;
        this.eventosPorDistrito=Collections.unmodifiableMap(distritos);
        this.eventosPorTipo=Collections.unmodifiableMap(tipos);
        this.muestraEventos=Collections.unmodifiableList(muestra); this.origen=origen;
    }
}
