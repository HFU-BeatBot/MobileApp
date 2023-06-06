package com.theriotjoker.beatbot;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public class ApiHandler {
    public String sendPostToApi(String url, String json) throws IOException {
        HttpURLConnection connection = prepareConnection(url);
        OutputStream os = connection.getOutputStream();
        os.write(json.getBytes(Charset.defaultCharset()));
        os.flush();
        BufferedReader bf = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder fullAnswerBuffer = new StringBuilder();
        String incoming = null;
        while((incoming = bf.readLine()) != null) {
            fullAnswerBuffer.append(incoming);
        }
        connection.disconnect();
        return fullAnswerBuffer.toString();
    }
    private HttpURLConnection prepareConnection(String url) throws IOException {
        URL apiUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection)apiUrl.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        return connection;
    }
    public static boolean testConnection(String url) {
        try {
            URL connectionTestURl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) connectionTestURl.openConnection();
            connection.connect();
            connection.disconnect();
            return true;
        } catch (IOException e) {
            return false;
        }

    }
}
