package minegame159.meteorclient.utils;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtils {
    private static final Gson GSON = new Gson();

    public static <T> T get(String url, Type type) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2500);
            conn.setReadTimeout(2500);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            T response = GSON.fromJson(reader, type);
            reader.close();

            return response;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
