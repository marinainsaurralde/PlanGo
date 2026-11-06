package com.marinainsaurralde.plango.data;

import android.os.Handler;
import android.os.Looper;
import com.marinainsaurralde.plango.model.Lugar;
import com.marinainsaurralde.plango.model.Plan;
import com.marinainsaurralde.plango.model.PlanIdentity;
import com.marinainsaurralde.plango.model.PlanType;
import com.marinainsaurralde.plango.model.PriceStatus;
import com.marinainsaurralde.plango.model.TemporalKind;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ParquesApiClient implements PlanesDataSource {
    public static final String ENDPOINT = "https://datos.madrid.es/dataset/200761-0-parques-jardines/"
            + "resource/200761-5-parques-jardines-json/download/200761-5-parques-jardines-json.json";
    private static final int TIMEOUT_MILLIS = 15_000;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override public void obtenerTodosLosPlanes(Callback callback) {
        executor.execute(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(TIMEOUT_MILLIS);
                connection.setReadTimeout(TIMEOUT_MILLIS);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("User-Agent", "PlanGo-Android/1.0");
                try {
                    int code = connection.getResponseCode();
                    if (code < 200 || code >= 300) throw new IOException("Respuesta HTTP de parques no válida: " + code);
                    List<Plan> plans = parsePlans(readBody(connection.getInputStream()));
                    mainHandler.post(() -> callback.onSuccess(plans));
                } finally { connection.disconnect(); }
            } catch (IOException | JSONException error) {
                mainHandler.post(() -> callback.onError(error));
            }
        });
    }

    static List<Plan> parsePlans(String json) throws JSONException {
        JSONArray records = new JSONObject(json).optJSONArray("@graph");
        if (records == null) throw new JSONException("La fuente de parques no contiene @graph");
        List<Plan> result = new ArrayList<>(records.length());
        for (int i = 0; i < records.length(); i++) {
            JSONObject record = records.optJSONObject(i);
            if (record == null) continue;
            String id = string(record, "id");
            if (id == null) continue;
            JSONObject address = record.optJSONObject("address");
            JSONObject organization = record.optJSONObject("organization");
            JSONObject location = record.optJSONObject("location");
            Plan.Builder builder = Plan.builder(PlanIdentity.PARQUE_MADRID, id,
                            first(string(record, "title"), string(organization, "organization-name")))
                    .planType(PlanType.NATURALEZA).temporalKind(TemporalKind.LUGAR_PERMANENTE)
                    .categoryKey("naturaleza:parques-jardines")
                    .description(string(organization, "organization-desc"))
                    .address(string(address, "street-address"))
                    .schedule(string(organization, "schedule"))
                    .priceStatus(PriceStatus.DESCONOCIDO).distrito(district(address))
                    .coordinates(number(location, "latitude"), number(location, "longitude"))
                    .nombreInstalacion(string(organization, "organization-name"))
                    .urlOficial(string(record, "relation"));
            result.add(new Lugar(builder));
        }
        return result;
    }

    private static String district(JSONObject address) {
        JSONObject district = address == null ? null : address.optJSONObject("district");
        String uri = string(district, "@id");
        if (uri == null) return null;
        String value = uri.substring(uri.lastIndexOf('/') + 1);
        if ("Distrito".equalsIgnoreCase(value)) return null;
        if ("Chamartin".equalsIgnoreCase(value)) return "CHAMARTÍN";
        if ("Chamberi".equalsIgnoreCase(value)) return "CHAMBERÍ";
        if ("Tetuan".equalsIgnoreCase(value)) return "TETUÁN";
        if ("Vicalvaro".equalsIgnoreCase(value)) return "VICÁLVARO";
        return value.replaceAll("([a-záéíóúñ])([A-ZÁÉÍÓÚÑ])", "$1 $2").toUpperCase(new Locale("es", "ES"));
    }

    private static String string(JSONObject object, String key) {
        if (object == null) return null;
        Object value = object.opt(key);
        if (value == null || value == JSONObject.NULL) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
    private static Double number(JSONObject object, String key) {
        String value = string(object, key);
        if (value == null) return null;
        try { return Double.valueOf(value.replace(',', '.')); } catch (NumberFormatException ignored) { return null; }
    }
    private static String first(String first, String second) { return first == null ? second : first; }
    private static String readBody(InputStream stream) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line; while ((line = reader.readLine()) != null) body.append(line);
        }
        return body.toString();
    }
    @Override public void cerrar() { executor.shutdownNow(); }
}
