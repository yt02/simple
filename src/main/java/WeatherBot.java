import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Scanner;

public class WeatherBot extends TelegramLongPollingBot {
    private boolean waitingForCity = false;  // To track if bot is expecting a city name

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String text = message.getText();

            if (waitingForCity) {
                // The user is expected to provide a city name now
                String cityName = text.trim();
                waitingForCity = false;  // Reset the flag
                fetchWeather(message, cityName);  // Fetch and send weather data
            } else if (text.startsWith("/start")) {
                sendReply(message, "Welcome! Use /weather to get the weather.");
            } else if (text.equalsIgnoreCase("/weather")) {
                // Ask user to enter a city name
                waitingForCity = true;
                sendReply(message, "Enter city name.");
            }
        }
    }

    // Method to send a reply to the user
    private void sendReply(Message message, String responseText) {
        SendMessage reply = new SendMessage();
        reply.setChatId(String.valueOf(message.getChatId()));
        reply.setText(responseText);
        try {
            execute(reply);  // Send the message
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Fetch weather data and send to the user
    private void fetchWeather(Message message, String cityName) {
        String apiKey = "c46ecd7b5365f44d4eb51a874df7d4c9";  // Your OpenWeatherMap API key

        // Geocoding API URL to get latitude and longitude of the city
        String geoUrl = "https://api.openweathermap.org/geo/1.0/direct?q=" + cityName + "&limit=1&appid=" + apiKey;

        try {
            HttpResponse<JsonNode> geoResponse = Unirest.get(geoUrl).asJson();

            if (geoResponse.getStatus() == 200) {
                JSONArray geoData = geoResponse.getBody().getArray();

                if (geoData.length() > 0) {
                    JSONObject location = geoData.getJSONObject(0);
                    double lat = location.getDouble("lat");
                    double lon = location.getDouble("lon");

                    // Weather API URL
                    String weatherUrl = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon + "&appid=" + apiKey + "&units=metric";
                    HttpResponse<JsonNode> weatherResponse = Unirest.get(weatherUrl).asJson();

                    if (weatherResponse.getStatus() == 200) {
                        JSONObject weatherData = weatherResponse.getBody().getObject();

                        // Extracting relevant data
                        JSONObject main = weatherData.getJSONObject("main");
                        double temp = main.getDouble("temp");
                        double feelsLike = main.getDouble("feels_like");
                        int humidity = main.getInt("humidity");

                        JSONObject weather = weatherData.getJSONArray("weather").getJSONObject(0);
                        String description = weather.getString("description");

                        JSONObject wind = weatherData.getJSONObject("wind");
                        double windSpeed = wind.getDouble("speed");

                        // Format the message
                        String replyText = String.format("Weather for %s:\n- Temperature: %.2f°C\n- Feels like: %.2f°C\n- Condition: %s\n- Humidity: %d%%\n- Wind speed: %.2f m/s",
                                cityName, temp, feelsLike, description, humidity, windSpeed);

                        sendReply(message, replyText);
                    } else {
                        sendReply(message, "Could not fetch weather data. Try again later.");
                    }
                } else {
                    sendReply(message, "City not found. Please check the name and try again.");
                }
            } else {
                sendReply(message, "Failed to fetch location data.");
            }
        } catch (Exception e) {
            sendReply(message, "An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }


    // Override method to return bot username
    @Override
    public String getBotUsername() {
        return "SYTimpleBot";  // Your bot's username
    }

    // Override method to return bot token
    @Override
    public String getBotToken() {
        return "8163083774:AAHqVX4G1DY0uldTBDDHux6OM3u4NNONXrw";  // Your Telegram bot token
    }
}