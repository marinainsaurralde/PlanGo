package com.marinainsaurralde.plango.model;

import java.io.Serializable;

public final class MuestraEvento implements Serializable {
    private static final long serialVersionUID = 1L;
    public final String id, titulo, fechaInicio, fechaFin, instalacion, distrito;
    public MuestraEvento(String id, String titulo, String inicio, String fin, String instalacion, String distrito) {
        this.id=id; this.titulo=titulo; this.fechaInicio=inicio; this.fechaFin=fin;
        this.instalacion=instalacion; this.distrito=distrito;
    }
}
