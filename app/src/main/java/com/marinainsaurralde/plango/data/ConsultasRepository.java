package com.marinainsaurralde.plango.data;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.marinainsaurralde.plango.model.ConsultaHistorial;
import com.marinainsaurralde.plango.model.MuestraEvento;
import java.util.ArrayList; import java.util.Date; import java.util.HashMap; import java.util.LinkedHashMap; import java.util.List; import java.util.Map;

public final class ConsultasRepository {
    public interface SaveCallback { void onSuccess(); void onError(Exception error); }
    public interface LoadCallback { void onSuccess(List<ConsultaHistorial> consultas); void onError(Exception error); }
    private static final ConsultasRepository INSTANCE=new ConsultasRepository();
    private final FirebaseFirestore db=FirebaseFirestore.getInstance();
    private ConsultasRepository(){}
    public static ConsultasRepository getInstance(){return INSTANCE;}

    public void guardar(String uid,String descargaId,ConsultaHistorial consulta,SaveCallback callback){
        String currentUid=FirebaseAuth.getInstance().getCurrentUser()==null?null:FirebaseAuth.getInstance().getCurrentUser().getUid();
        if(uid==null||!uid.equals(currentUid)){callback.onError(new IllegalStateException("La sesión cambió durante la descarga"));return;}
        Map<String,Object> data=toMap(consulta); data.put("fechaConsulta",FieldValue.serverTimestamp());
        db.collection("usuarios").document(uid).collection("consultas").document(descargaId)
                .set(data).addOnSuccessListener(v->callback.onSuccess()).addOnFailureListener(callback::onError);
    }

    public void cargar(LoadCallback callback){
        if(FirebaseAuth.getInstance().getCurrentUser()==null){callback.onError(new IllegalStateException("Sin sesión"));return;}
        String uid=FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("usuarios").document(uid).collection("consultas")
                .orderBy("fechaConsulta", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(snapshot->{
                    String active=FirebaseAuth.getInstance().getCurrentUser()==null?null:FirebaseAuth.getInstance().getCurrentUser().getUid();
                    if(!uid.equals(active)){callback.onError(new IllegalStateException("La sesión cambió"));return;}
                    List<ConsultaHistorial> result=new ArrayList<>(); snapshot.getDocuments().forEach(d->result.add(fromMap(d.getId(),d.getData()))); callback.onSuccess(result);
                }).addOnFailureListener(callback::onError);
    }

    private static Map<String,Object> toMap(ConsultaHistorial c){
        Map<String,Object> m=new HashMap<>(); m.put("totalEventos",c.totalEventos);m.put("totalGratuitos",c.totalGratuitos);m.put("totalPago",c.totalPago);m.put("totalSinInformacion",c.totalSinInformacion);m.put("eventosPorDistrito",c.eventosPorDistrito);m.put("eventosPorTipo",c.eventosPorTipo);m.put("origen",c.origen);
        List<Map<String,Object>> sample=new ArrayList<>();for(MuestraEvento e:c.muestraEventos){Map<String,Object>x=new HashMap<>();put(x,"id",e.id);put(x,"titulo",e.titulo);put(x,"fechaInicio",e.fechaInicio);put(x,"fechaFin",e.fechaFin);put(x,"instalacion",e.instalacion);put(x,"distrito",e.distrito);sample.add(x);}m.put("muestraEventos",sample);return m;
    }
    @SuppressWarnings("unchecked") private static ConsultaHistorial fromMap(String id,Map<String,Object>m){
        Object ts=m.get("fechaConsulta");Date date=ts instanceof Timestamp?((Timestamp)ts).toDate():null;
        Map<String,Integer>d=integerMap(m.get("eventosPorDistrito")),t=integerMap(m.get("eventosPorTipo"));List<MuestraEvento>s=new ArrayList<>();Object raw=m.get("muestraEventos");if(raw instanceof List)for(Object o:(List<?>)raw)if(o instanceof Map){Map<String,Object>x=(Map<String,Object>)o;s.add(new MuestraEvento(str(x.get("id")),str(x.get("titulo")),str(x.get("fechaInicio")),str(x.get("fechaFin")),str(x.get("instalacion")),str(x.get("distrito"))));}
        return new ConsultaHistorial(id,date,num(m.get("totalEventos")),num(m.get("totalGratuitos")),num(m.get("totalPago")),num(m.get("totalSinInformacion")),d,t,s,str(m.get("origen")));
    }
    @SuppressWarnings("unchecked") private static Map<String,Integer> integerMap(Object o){Map<String,Integer>r=new LinkedHashMap<>();if(o instanceof Map)((Map<?,?>)o).forEach((k,v)->{if(k!=null&&v instanceof Number)r.put(String.valueOf(k),((Number)v).intValue());});return r;}
    private static int num(Object o){return o instanceof Number?((Number)o).intValue():0;} private static String str(Object o){return o==null?null:String.valueOf(o);} private static void put(Map<String,Object>m,String k,Object v){if(v!=null)m.put(k,v);}
}
