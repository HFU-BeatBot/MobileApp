package com.theriotjoker.beatbot;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public class ApiHandler {
    private int failedConnectionCounter = 0;
    private static String BEATBOT_API_URL = "http://gamers-galaxy.ddns.net:8000";
    private static final String BEATBOT_SERVICE = "/process";
    public String sendPostToApi(String json) throws IOException {
        HttpURLConnection connection = prepareConnection();
        OutputStream os = connection.getOutputStream();
        os.write(json.getBytes(Charset.defaultCharset()));
        os.flush();
        BufferedReader bf = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder fullAnswerBuffer = new StringBuilder();
        String incoming;
        while((incoming = bf.readLine()) != null) {
            fullAnswerBuffer.append(incoming);
        }
        connection.disconnect();
        return fullAnswerBuffer.toString();
    }
    private HttpURLConnection prepareConnection() throws IOException {
        String url = BEATBOT_API_URL+BEATBOT_SERVICE;
        URL apiUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection)apiUrl.openConnection();
        connection.setConnectTimeout(1500);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        return connection;
    }
    public boolean testConnection() {
        if(failedConnectionCounter > 3) {
            if(BEATBOT_API_URL.contentEquals("http://gamers-galaxy.ddns.net:8000")) {
                BEATBOT_API_URL = "http://beatbot.informatik.hs-furtwangen.de:8000";
            } else {
                BEATBOT_API_URL = "http://gamers-galaxy.ddns.net:8000";
            }
            failedConnectionCounter = 0;
        }
        try {
            URL connectionTestURl = new URL(BEATBOT_API_URL);
            HttpURLConnection connection = (HttpURLConnection) connectionTestURl.openConnection();
            connection.setConnectTimeout(1500);
            connection.connect();
            failedConnectionCounter = 0;
            return true;
        } catch (IOException e) {
            failedConnectionCounter++;
            return false;
        }
    }
    public String connectedTo() {
        if(BEATBOT_API_URL.contentEquals("http://gamers-galaxy.ddns.net:8000")) {
            return "Connected to the main server";
        } else {
            return "Connected to the fallback server";
        }
    }
}
