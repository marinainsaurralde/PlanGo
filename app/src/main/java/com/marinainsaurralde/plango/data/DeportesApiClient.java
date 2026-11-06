package com.marinainsaurralde.plango.data;

import android.os.Handler;
import android.os.Looper;

import com.marinainsaurralde.plango.model.CategoriaDeportiva;
import com.marinainsaurralde.plango.model.Evento;
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

public final class DeportesApiClient implements EventosDataSource {

    public static final String ENDPOINT =
            "https://datos.madrid.es/dataset/212504-0-agenda-actividades-deportes/"
                    + "resource/212504-2-agenda-actividades-deportes-json/download/"
                    + "212504-2-agenda-actividades-deportes-json.json";
    private static final int TIMEOUT_MILLIS = 15_000;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void obtenerTodosLosEventos(Callback callback) {
        executor.execute(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(TIMEOUT_MILLIS);
                connection.setReadTimeout(TIMEOUT_MILLIS);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("User-Agent", "PlanGo-Android/1.0");
                try {
                    int responseCode = connection.getResponseCode();
                    if (responseCode < 200 || responseCode >= 300) {
                        throw new IOException("Respuesta HTTP deportiva no válida: " + responseCode);
                    }
                    List<Evento> events = parseEvents(readBody(connection.getInputStream()));
                    mainHandler.post(() -> callback.onSuccess(events));
                } finally {
                    connection.disconnect();
                }
            } catch (IOException | JSONException error) {
                mainHandler.post(() -> callback.onError(error));
            }
        });
    }

    static List<Evento> parseEvents(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        JSONArray records = root.optJSONArray("@graph");
        if (records == null) throw new JSONException("La agenda deportiva no contiene @graph");
        List<Evento> result = new ArrayList<>(records.length());
        for (int index = 0; index < records.length(); index++) {
            JSONObject record = records.optJSONObject(index);
            if (record == null) continue;
            String id = string(record, "id");
            if (id == null) continue;
            String apiType = string(record, "@type");
            CategoriaDeportiva category = CategoriaDeportiva.fromApiType(apiType);
            Boolean free = nullableBoolean(record.opt("free"));
            JSONObject address = record.optJSONObject("address");
            JSONObject location = record.optJSONObject("location");
            JSONObject organization = record.optJSONObject("organization");
            JSONObject recurrence = record.optJSONObject("recurrence");
            String venue = first(string(organization, "organization-name"),
                    string(record, "event-location"));
            String days = recurrence == null ? null : convertWeekdays(string(recurrence, "days"));
            Plan.Builder plan = Plan.builder(PlanIdentity.EVENTO_MADRID, id, string(record, "title"))
                    .planType(PlanType.DEPORTE)
                    .temporalKind(TemporalKind.EVENTO)
                    .categoryKey(category.getKey())
                    .description(string(record, "description"))
                    .address(string(address, "street-address"))
                    .schedule(days)
                    .priceStatus(priceStatus(free))
                    .distrito(district(address))
                    .coordinates(number(location, "latitude"), number(location, "longitude"))
                    .nombreInstalacion(venue)
                    .urlOficial(string(record, "link"));
            result.add(new Evento(
                    plan,
                    string(record, "dtstart"),
                    string(record, "dtend"),
                    category.getLabel(),
                    Boolean.TRUE.equals(free),
                    free != null,
                    days,
                    string(record, "excluded-days"),
                    string(record, "time"),
                    string(record, "price")
            ));
        }
        return result;
    }

    private static String district(JSONObject address) {
        if (address == null) return null;
        JSONObject district = address.optJSONObject("district");
        String uri = string(district, "@id");
        if (uri == null) return null;
        String value = uri.substring(uri.lastIndexOf('/') + 1);
        if ("Distrito".equalsIgnoreCase(value)) return null;
        if ("Chamartin".equalsIgnoreCase(value)) return "CHAMARTÍN";
        if ("Chamberi".equalsIgnoreCase(value)) return "CHAMBERÍ";
        return value.replaceAll("([a-záéíóúñ])([A-ZÁÉÍÓÚÑ])", "$1 $2")
                .toUpperCase(new Locale("es", "ES"));
    }

    private static String convertWeekdays(String value) {
        if (value == null) return null;
        StringBuilder result = new StringBuilder();
        for (String token : value.split(",")) {
            String converted;
            switch (token.trim()) {
                case "MO": converted = "L"; break;
                case "TU": converted = "M"; break;
                case "WE": converted = "X"; break;
                case "TH": converted = "J"; break;
                case "FR": converted = "V"; break;
                case "SA": converted = "S"; break;
                case "SU": converted = "D"; break;
                default: continue;
            }
            if (result.length() > 0) result.append(',');
            result.append(converted);
        }
        return result.length() == 0 ? null : result.toString();
    }

    private static PriceStatus priceStatus(Boolean free) {
        if (free == null) return PriceStatus.DESCONOCIDO;
        return free ? PriceStatus.GRATIS : PriceStatus.DE_PAGO;
    }

    private static Boolean nullableBoolean(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) {
            int number = ((Number) value).intValue();
            return number == 1 ? Boolean.TRUE : number == 0 ? Boolean.FALSE : null;
        }
        if (value == null || value == JSONObject.NULL) return null;
        String text = String.valueOf(value).trim();
        if ("1".equals(text) || "true".equalsIgnoreCase(text)) return true;
        if ("0".equals(text) || "false".equalsIgnoreCase(text)) return false;
        return null;
    }

    private static String string(JSONObject object, String property) {
        if (object == null) return null;
        Object value = object.opt(property);
        if (value == null || value == JSONObject.NULL) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static Double number(JSONObject object, String property) {
        String text = string(object, property);
        if (text == null) return null;
        try { return Double.valueOf(text.replace(',', '.')); }
        catch (NumberFormatException ignored) { return null; }
    }

    private static String first(String first, String second) {
        return first == null ? second : first;
    }

    private static String readBody(InputStream inputStream) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
        }
        return body.toString();
    }

    @Override
    public void cerrar() { executor.shutdownNow(); }
}
