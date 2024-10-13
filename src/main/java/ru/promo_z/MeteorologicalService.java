package ru.promo_z;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.IntStream;

public class MeteorologicalService {

    private static final String YANDEX_WEATHER_API_URI = "https://api.weather.yandex.ru/v2/forecast";
    private static final String YANDEX_WEATHER_API_KEY = "4eb52d7f-79a9-4f21-a37e-2a660670c257";

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Введите значение широты:");
        double lat  = castCoordinateToDouble(scanner.nextLine());

        System.out.println("Введите значение долготы:");
        double lon = castCoordinateToDouble(scanner.nextLine());

        System.out.println("Введите значение лимита:");
        int limit = castLimitToInt(scanner.nextLine());
        scanner.close();

        HttpResponse<String> response = getDataFromYandexService(lat, lon, limit);
        JSONObject jsonObjectResponseBody = new JSONObject(response.body());
        String currentTemperature = jsonObjectResponseBody.getJSONObject("fact").optString("temp");
        long averageTemperature = calculateAverageTemperature(jsonObjectResponseBody);

        System.out.println("От сервиса получен ответ: " + jsonObjectResponseBody);
        System.out.println("Текущая фактическая температура: " + currentTemperature + " °C");
        System.out.println("Средняя температура за период " + limit + " дн. составляет: " + averageTemperature + " °C");
    }

    private static long calculateAverageTemperature(JSONObject jsonObjectResponseBody) {
        JSONArray forecasts = jsonObjectResponseBody.optJSONArray("forecasts");
        List<JSONObject> elements = IntStream.range(0, forecasts.length())
                .mapToObj(index -> ((JSONObject) forecasts.get(index)).optJSONObject("parts"))
                .filter(Objects::nonNull)
                .toList();

        double avgTemperature = 0.0;

        for (JSONObject jsonObject : elements) {
            double avgTemperatureNight = jsonObject.getJSONObject("night").optDouble("temp_avg");
            double avgTemperatureMorning = jsonObject.getJSONObject("morning").optDouble("temp_avg");
            double avgTemperatureDay = jsonObject.getJSONObject("day").optDouble("temp_avg");
            double avgTemperatureEvening = jsonObject.getJSONObject("evening").optDouble("temp_avg");

            avgTemperature = avgTemperature + (avgTemperatureNight + avgTemperatureMorning
                    + avgTemperatureDay + avgTemperatureEvening) / 4;
        }

        //Вернем округленное значение средней температуры до ближайшего целого числа (как в прогнозе погоды)
        return Math.round(avgTemperature / elements.size());
    }

    private static HttpResponse<String> getDataFromYandexService(double lat, double lon, int limit) throws Exception {
        StringBuilder uri = new StringBuilder(YANDEX_WEATHER_API_URI)
                .append("?lat=")
                .append(lat)
                .append("&lon=")
                .append(lon)
                .append("&limit=")
                .append(limit);

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri.toString()))
                .header("X-Yandex-Weather-Key", YANDEX_WEATHER_API_KEY)
                .GET()
                .build();

        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ex) {
            throw new Exception("При выполнении HTTP-запроса возникла ошибка: " + ex.getMessage());
        }
    }

    private static int castLimitToInt(String input) throws Exception {
        try {
            int limit = Integer.parseInt(input);
            if (limit < 1 || limit > 11) {
                throw new IllegalArgumentException("Значение лимита должно находиться в интервале от 1 до 11.");
            }

            return limit;
        } catch (Exception ex) {
            throw new Exception("При попытке получить допустимое значение лимита возникла ошибка: " + ex.getMessage());
        }
    }

    private static double castCoordinateToDouble(String input) throws Exception {
        try {
            return Double.parseDouble(input);
        } catch (Exception exception) {
            throw new Exception("При попытке получить допустимое значение координаты возникла ошибка: "
                    + exception.getMessage());
        }
    }
}