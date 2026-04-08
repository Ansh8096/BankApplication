package net.engineerAnsh.BankApplication.Services;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Service;
import org.json.JSONObject;

@Service
public class GeoLocationService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${geo.ip.token}")
    private String TOKEN; // optional but recommended

    public String getLocationFromIp(String ip) {
        try {
            String url = "https://ipinfo.io/" + ip + "?token=" + TOKEN;

            String response = restTemplate.getForObject(url, String.class);

            JSONObject json = new JSONObject(response);

            String city = json.optString("city");
            String region = json.optString("region");
            String country = json.optString("country");

            return formatLocation(city, region, country);

        } catch (Exception e) {
            return "Unknown Location";
        }
    }

    private String formatLocation(String city, String region, String country) {
        return java.util.stream.Stream.of(city, region, country)
                .filter(s -> s != null && !s.isBlank())
                .reduce((a, b) -> a + ", " + b)
                .orElse("Unknown Location");
    }
}