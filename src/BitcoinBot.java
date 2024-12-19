import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import org.json.JSONObject;

public class BitcoinBot {

    // Replace with your actual bot token
    private static final String TELEGRAM_BOT_TOKEN = "8100532205:AAHQ51SF3bKkCbYFu6t_g0IGAg8EyvwPFbE";

    // List of chat IDs (add more as needed)
    private static final String[] CHAT_IDS = {
            "7461504737",   // Your first group chat ID
            "6611537396",   // Your second group chat ID
            "5736366704",   // Your third group chat ID
            "6240501542",   // Your fourth group chat ID
            "-1002449726286" // Additional group chat ID (example)
    };

    // URL for fetching Bitcoin price from Binance API
    private static final String API_URL = "https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT";

    // Thresholds for price change (e.g., $2000 increase, $1000 decrease)
    private static final double PRICE_THRESHOLD_INCREASE = 10;
    private static final double PRICE_THRESHOLD_DECREASE = 10;

    // Variable to store the last known Bitcoin price
    private static double lastPrice = -1;

    // Method to send a notification to Telegram
    public static void sendTelegramNotification(String message) throws IOException {
        try {
            // URL-encode the message to ensure special characters are handled properly
            String encodedMessage = URLEncoder.encode(message, "UTF-8");

            // Loop through each chat ID and send the message
            for (String chatId : CHAT_IDS) {
                // Construct the URL for the Telegram API
                String telegramApiUrl = "https://api.telegram.org/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage?chat_id=" + chatId + "&text=" + encodedMessage;

                // Open the connection to the Telegram API
                HttpURLConnection connection = (HttpURLConnection) new URL(telegramApiUrl).openConnection();
                connection.setRequestMethod("GET");

                // Get the response code to check if the request was successful
                int responseCode = connection.getResponseCode();

                // Check if the response was successful (HTTP 200)
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    System.out.println("Notification sent to Telegram chat ID: " + chatId);
                } else {
                    System.out.println("Failed to send notification to chat ID: " + chatId + ". Response code: " + responseCode);
                }

                // Close the connection
                connection.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to fetch the current Bitcoin price from Binance API
    public static double getBitcoinPrice() throws IOException {
        URL url = new URL(API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // Read the response from the API
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // Parse the response to extract the Bitcoin price
        JSONObject jsonResponse = new JSONObject(response.toString());
        double price = jsonResponse.getDouble("price");
        connection.disconnect();

        return price;
    }

    // Method to check if the price change exceeds the threshold
    public static void checkPriceThreshold(double currentPrice) throws IOException {
        if (lastPrice == -1) {
            // Initialize the last price if this is the first run
            lastPrice = currentPrice;
            System.out.println("Initial Bitcoin price: $" + lastPrice);
            return;
        }

        // Calculate the difference between the current and last price
        double priceDiffIncrease = currentPrice - lastPrice;
        double priceDiffDecrease = lastPrice - currentPrice;

        // Check if the price increase or decrease exceeds the threshold
        if (priceDiffIncrease >= PRICE_THRESHOLD_INCREASE) {
            String message = "Bitcoin price increased by $" + priceDiffIncrease + ". Current price: $" + currentPrice;
            sendTelegramNotification(message);
            System.out.println(message);
        } else if (priceDiffDecrease >= PRICE_THRESHOLD_DECREASE) {
            String message = "Bitcoin price decreased by $" + priceDiffDecrease + ". Current price: $" + currentPrice;
            sendTelegramNotification(message);
            System.out.println(message);
        }

        // Update the last known price to the current price
        lastPrice = currentPrice;
    }

    // Main method to get Bitcoin price and send a message to Telegram if the threshold is met
    public static void main(String[] args) {
        while (true) {
            try {
                // Fetch the current Bitcoin price
                double currentPrice = getBitcoinPrice();

                // Check if the price change exceeds the threshold
                checkPriceThreshold(currentPrice);

                // Sleep for 60 seconds before checking again
                Thread.sleep(60000); // 60000 milliseconds = 60 seconds
            } catch (IOException e) {
                System.err.println("Error fetching Bitcoin price: " + e.getMessage());
                e.printStackTrace();
            } catch (InterruptedException e) {
                System.err.println("Thread interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();  // Restore interrupt status
                break;  // Exit the loop if the thread is interrupted
            }
        }
    }
}
