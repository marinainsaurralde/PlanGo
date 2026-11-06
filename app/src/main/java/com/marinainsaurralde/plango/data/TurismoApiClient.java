package com.marinainsaurralde.plango.data;

import android.os.Handler;
import android.os.Looper;
import com.marinainsaurralde.plango.model.CategoriaTuristica;
import com.marinainsaurralde.plango.model.Lugar;
import com.marinainsaurralde.plango.model.Plan;
import com.marinainsaurralde.plango.model.PlanIdentity;
import com.marinainsaurralde.plango.model.PlanType;
import com.marinainsaurralde.plango.model.PriceStatus;
import com.marinainsaurralde.plango.model.TemporalKind;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

public final class TurismoApiClient implements PlanesDataSource {
    public static final String ENDPOINT="https://datos.madrid.es/dataset/300030-0-puntos-interes-turistico/"
            +"resource/300030-1-puntos-interes-turistico/download/300030-1-puntos-interes-turistico.xml";
    private static final int TIMEOUT_MILLIS=20_000;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final Handler mainHandler=new Handler(Looper.getMainLooper());

    @Override public void obtenerTodosLosPlanes(Callback callback){executor.execute(()->{try{HttpURLConnection connection=(HttpURLConnection)new URL(ENDPOINT).openConnection();connection.setRequestMethod("GET");connection.setConnectTimeout(TIMEOUT_MILLIS);connection.setReadTimeout(TIMEOUT_MILLIS);connection.setRequestProperty("Accept","application/xml,text/xml");connection.setRequestProperty("User-Agent","PlanGo-Android/1.0");try{int code=connection.getResponseCode();if(code<200||code>=300)throw new IOException("Respuesta HTTP turística no válida: "+code);List<Plan> plans=parsePlans(connection.getInputStream());mainHandler.post(()->callback.onSuccess(plans));}finally{connection.disconnect();}}catch(Exception error){mainHandler.post(()->callback.onError(error));}});}

    static List<Plan> parsePlans(InputStream input) throws ParserConfigurationException, IOException, SAXException {
        byte[] xml=readAll(input);
        if(containsAsciiIgnoreCase(xml,"<!DOCTYPE"))throw new SAXException("DOCTYPE no permitido");
        DocumentBuilderFactory factory=secureFactory();
        DocumentBuilder documentBuilder=factory.newDocumentBuilder();
        documentBuilder.setEntityResolver((publicId,systemId)->{throw new SAXException("Entidades externas no permitidas");});
        Document document=documentBuilder.parse(new ByteArrayInputStream(xml));
        NodeList services=document.getElementsByTagName("service");
        List<Plan> result=new ArrayList<>(services.getLength());
        for(int i=0;i<services.getLength();i++){
            Element service=(Element)services.item(i);String id=clean(service.getAttribute("id"));if(id==null)continue;
            Element basic=child(service,"basicData"),geo=child(service,"geoData"),extra=child(service,"extradata");
            String name=HtmlTextCleaner.clean(first(text(basic,"name"),text(basic,"title")));
            String schedule=meaningful(extraItem(extra,"Horario"));
            String priceInfo=meaningful(extraItem(extra,"Servicios de pago"));
            Plan.Builder builder=Plan.builder(PlanIdentity.TURISMO_MADRID,id,name)
                    .planType(PlanType.TURISMO).temporalKind(TemporalKind.LUGAR_PERMANENTE)
                    .description(HtmlTextCleaner.clean(text(basic,"body")))
                    .address(HtmlTextCleaner.clean(text(geo,"address")))
                    .schedule(schedule).priceInfo(priceInfo).priceStatus(classifyPrice(priceInfo))
                    .lastSourceUpdate(clean(service.getAttribute("fechaActualizacion")))
                    .coordinates(number(text(geo,"latitude")),number(text(geo,"longitude")))
                    .nombreInstalacion(name).urlOficial(clean(text(basic,"web")));
            Element categories=child(extra,"categorias");
            if(categories!=null){NodeList list=categories.getElementsByTagName("categoria");for(int c=0;c<list.getLength();c++){Element category=(Element)list.item(c);String categoryId=item(category,"idCategoria");if(categoryId!=null)builder.categoryKey(CategoriaTuristica.key(categoryId));}}
            result.add(new Lugar(builder));
        }
        return result;
    }

    static PriceStatus classifyPrice(String value){String text=clean(value);if(text==null)return PriceStatus.DESCONOCIDO;String lower=text.toLowerCase(new Locale("es","ES"));boolean free=lower.matches("(?s).*(entrada gratuita|acceso gratuito|entrada libre|acceso libre|gratuito|gratuita|gratis).*");boolean money=lower.matches("(?s).*(€|\\beuros?\\b).*");boolean accessPrice=money&&lower.matches("(?s).*(entrada|acceso|tarifa general|precio de entrada|visita general).*");if(free&&!money)return PriceStatus.GRATIS;if(accessPrice&&!free)return PriceStatus.DE_PAGO;return PriceStatus.DESCONOCIDO;}
    private static DocumentBuilderFactory secureFactory() {DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();factory.setNamespaceAware(false);try{factory.setXIncludeAware(false);}catch(UnsupportedOperationException ignored){}factory.setExpandEntityReferences(false);setFeatureIfSupported(factory,"http://apache.org/xml/features/disallow-doctype-decl",true);setFeatureIfSupported(factory,"http://xml.org/sax/features/external-general-entities",false);setFeatureIfSupported(factory,"http://xml.org/sax/features/external-parameter-entities",false);return factory;}
    private static void setFeatureIfSupported(DocumentBuilderFactory factory,String feature,boolean value){try{factory.setFeature(feature,value);}catch(ParserConfigurationException ignored){/* La comprobación previa de DOCTYPE y el EntityResolver mantienen el bloqueo. */}}
    private static byte[] readAll(InputStream input)throws IOException{ByteArrayOutputStream output=new ByteArrayOutputStream();byte[] buffer=new byte[8192];for(int count;(count=input.read(buffer))!=-1;)output.write(buffer,0,count);return output.toByteArray();}
    private static boolean containsAsciiIgnoreCase(byte[] value,String expected){byte[] token=expected.getBytes(java.nio.charset.StandardCharsets.US_ASCII);outer:for(int i=0;i<=value.length-token.length;i++){for(int j=0;j<token.length;j++){int actual=value[i+j]&0xff,target=token[j]&0xff;if(actual>='a'&&actual<='z')actual-=32;if(target>='a'&&target<='z')target-=32;if(actual!=target)continue outer;}return true;}return false;}
    private static Element child(Element parent,String name){if(parent==null)return null;for(Node node=parent.getFirstChild();node!=null;node=node.getNextSibling())if(node instanceof Element&&name.equals(node.getNodeName()))return(Element)node;return null;}
    private static String text(Element parent,String name){Element value=child(parent,name);return value==null?null:value.getTextContent();}
    private static String extraItem(Element extra,String name){if(extra==null)return null;for(Node node=extra.getFirstChild();node!=null;node=node.getNextSibling())if(node instanceof Element&&"item".equals(node.getNodeName())&&name.equals(((Element)node).getAttribute("name")))return HtmlTextCleaner.clean(node.getTextContent());return null;}
    private static String item(Element parent,String name){if(parent==null)return null;NodeList items=parent.getElementsByTagName("item");for(int i=0;i<items.getLength();i++){Element value=(Element)items.item(i);if(name.equals(value.getAttribute("name")))return clean(value.getTextContent());}return null;}
    private static String meaningful(String value){String text=clean(value);return text==null||"-".equals(text)||"--".equals(text)?null:text;}
    private static String first(String first,String second){return clean(first)==null?clean(second):clean(first);}
    private static String clean(String value){if(value==null)return null;String text=value.trim();return text.isEmpty()?null:text;}
    private static Double number(String value){try{return value==null?null:Double.valueOf(value.trim().replace(',','.'));}catch(NumberFormatException ignored){return null;}}
    @Override public void cerrar(){executor.shutdownNow();}
}
