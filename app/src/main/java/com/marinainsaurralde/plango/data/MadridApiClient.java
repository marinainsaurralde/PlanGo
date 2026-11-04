package com.marinainsaurralde.plango.data;

import android.os.Handler;
import android.os.Looper;

import com.marinainsaurralde.plango.model.Evento;

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

public class MadridApiClient {

    private static final String ENDPOINT =
            "https://datos.madrid.es/id/api/3/action/datastore_search";
    private static final String RESOURCE_ID =
            "300107-2-agenda-actividades-eventos-csv";
    private static final int TIMEOUT_MILLIS = 15_000;
    private static final int DEFAULT_PAGE_SIZE = 500;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onSuccess(List<Evento> eventos);

        void onError(Exception error);
    }

    public void obtenerEventos(int limit, int offset, Callback callback) {
        if (limit <= 0 || offset < 0) {
            callback.onError(new IllegalArgumentException("Parámetros de paginación no válidos"));
            return;
        }

        executor.execute(() -> {
            try {
                List<Evento> eventos = requestEvents(limit, offset);
                mainHandler.post(() -> callback.onSuccess(eventos));
            } catch (IOException | JSONException error) {
                mainHandler.post(() -> callback.onError(error));
            }
        });
    }

    public void obtenerTodosLosEventos(Callback callback) {
        executor.execute(() -> {
            try {
                List<Evento> allEvents = new ArrayList<>();
                int offset = 0;
                int total = Integer.MAX_VALUE;
                while (!Thread.currentThread().isInterrupted()) {
                    ApiPage page = requestPage(DEFAULT_PAGE_SIZE, offset);
                    if (page.events.isEmpty() && offset < page.total) {
                        throw new IOException("La API devolvió una página incompleta");
                    }
                    allEvents.addAll(page.events);
                    total = page.total;
                    offset += page.events.size();
                    if (offset >= total) {
                        mainHandler.post(() -> callback.onSuccess(allEvents));
                        return;
                    }
                }
            } catch (IOException | JSONException error) {
                mainHandler.post(() -> callback.onError(error));
            }
        });
    }

    public void cerrar() {
        executor.shutdownNow();
    }

    private List<Evento> requestEvents(int limit, int offset)
            throws IOException, JSONException {
        return requestPage(limit, offset).events;
    }

    private ApiPage requestPage(int limit, int offset)
            throws IOException, JSONException {
        String requestUrl = String.format(
                Locale.ROOT,
                "%s?resource_id=%s&limit=%d&offset=%d",
                ENDPOINT,
                RESOURCE_ID,
                limit,
                offset
        );

        HttpURLConnection connection = (HttpURLConnection) new URL(requestUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "PlanGo-Android/1.0");

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("Respuesta HTTP no válida: " + responseCode);
            }

            String responseBody = readBody(connection.getInputStream());
            JSONObject root = new JSONObject(responseBody);
            List<Evento> events = parseEvents(root);
            JSONObject result = root.getJSONObject("result");
            int total = result.optInt("total", offset + events.size());
            return new ApiPage(events, Math.max(total, offset + events.size()));
        } finally {
            connection.disconnect();
        }
    }

    private static String readBody(InputStream inputStream) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
    }

    static List<Evento> parseEvents(String json) throws JSONException {
        return parseEvents(new JSONObject(json));
    }

    private static List<Evento> parseEvents(JSONObject root) throws JSONException {
        if (!root.optBoolean("success", false)) {
            throw new JSONException("La API indicó que la consulta no fue correcta");
        }

        JSONObject result = root.optJSONObject("result");
        if (result == null) {
            throw new JSONException("La respuesta no contiene el objeto result");
        }

        JSONArray records = result.optJSONArray("records");
        if (records == null) {
            throw new JSONException("La respuesta no contiene el array records");
        }

        List<Evento> eventos = new ArrayList<>(records.length());
        for (int index = 0; index < records.length(); index++) {
            JSONObject record = records.optJSONObject(index);
            if (record == null) {
                continue;
            }

            eventos.add(new Evento(
                    nullableString(record, "ID-EVENTO"),
                    nullableString(record, "TITULO"),
                    nullableString(record, "FECHA"),
                    nullableString(record, "FECHA-FIN"),
                    nullableString(record, "DISTRITO-INSTALACION"),
                    nullableString(record, "TIPO"),
                    parseBoolean(record.opt("GRATUITO")),
                    nullableDouble(record, "LATITUD"),
                    nullableDouble(record, "LONGITUD"),
                    nullableString(record, "NOMBRE-INSTALACION"),
                    nullableString(record, "DIAS-SEMANA"),
                    nullableString(record, "DIAS-EXCLUIDOS")
            ));
        }
        return eventos;
    }

    private static String nullableString(JSONObject object, String property) {
        Object value = object.opt(property);
        if (value == null || value == JSONObject.NULL) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static boolean parseBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() == 1;
        }
        if (value == null || value == JSONObject.NULL) {
            return false;
        }
        String text = String.valueOf(value).trim();
        return "1".equals(text)
                || "true".equalsIgnoreCase(text)
                || "si".equalsIgnoreCase(text)
                || "sí".equalsIgnoreCase(text);
    }

    private static Double nullableDouble(JSONObject object, String property) {
        Object value = object.opt(property);
        if (value == null || value == JSONObject.NULL) {
            return null;
        }
        String text = String.valueOf(value).trim().replace(',', '.');
        if (text.isEmpty()) {
            return null;
        }
        try {
            double number = Double.parseDouble(text);
            return Double.isFinite(number) ? number : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static final class ApiPage {
        private final List<Evento> events;
        private final int total;

        private ApiPage(List<Evento> events, int total) {
            this.events = events;
            this.total = total;
        }
    }
}
