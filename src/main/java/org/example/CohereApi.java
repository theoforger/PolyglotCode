package org.example;
import okhttp3.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

// CohereApi class to do and manage api
public class CohereApi {

    // call api method receives 4 strings
    // content - content from the files
    // language - language to translate file in
    // api - api-key
    // baseURL - baseURL default or specified by user
    public static JSONObject callApi(String content, String language, String api, String baseURL, boolean stream) throws Exception {

        // Transform content into the JSON object
        String contentJson = JSONObject.quote(content);

        // Create a client that sends requests to the api
        OkHttpClient client = new OkHttpClient();

        // Complete user's request
        String requestText = getMsg(language, contentJson);
        String json = requestText + ", \"model\": \"command-r\"";


        if (stream) {
            json += ", \"stream\": true";
        }

        json += " }";

//        System.out.println("JSON request is: " + json);

        // Request's body
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        // Define a request sent to the server
        Request request = new Request.Builder()
                .url(baseURL)
                .post(body)
                .addHeader("Authorization", "Bearer " + api)
                .addHeader("Content-Type", "application/json")
                .build();

        // Retrieve a response from the server
        Response response = client.newCall(request).execute();
        // if -else statement
        // if response is successful transform from JSON
        // into String object
        // otherwise throw an exception
        if (response.isSuccessful()) {
            if (stream) {
                // Handle streaming response
                InputStream is = response.body().byteStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String line;
                StringBuilder resultBuilder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    // Each line is a JSON message
                    if (!line.trim().isEmpty()) { // Skip empty lines
                        JSONObject jsonLine = new JSONObject(line);
                        String eventType = jsonLine.getString("event_type");
                        if ("text-generation".equals(eventType)) {
                            String text = jsonLine.getString("text");
                            System.out.print(text);
                            System.out.flush();
                            resultBuilder.append(text);
                        }
                    }
                }
                // After streaming is complete, construct the JSONObject
                String resultText = resultBuilder.toString();
                JSONObject responseJson = new JSONObject();
                responseJson.put("text", resultText);
                // Token usage may not be available when streaming
                return responseJson;
            }
            else {
                // Handle non-streaming response
                // Handle non-streaming response
                String responseString = response.body().string();

                // Debugging output
                // System.out.println("API Response: " + responseString);

                // Parse the response string as a JSONObject
                JSONObject responseJson = new JSONObject(responseString);

                // Extract the translated text from the response
                if (responseJson.has("text")) {
                    String translatedText = responseJson.getString("text");

                    // Create a new JSONObject to return
                    JSONObject resultJson = new JSONObject();
                    resultJson.put("text", translatedText);

                    // Include token usage if available
                    if (responseJson.has("meta")) {
                        resultJson.put("meta", responseJson.getJSONObject("meta"));
                    }

                    return resultJson;
                } else {
                    throw new Exception("No 'text' field found in the response.");
                }
            }
        } else {
            // Read the error message from the response body if available
            String errorBody = "";
            if (response.body() != null) {
                errorBody = response.body().string();
            }

            // Construct a detailed error message based on the status code
            String errorMessage = "API request failed with status code " + response.code() + ": ";

            switch (response.code()) {
                case 400:
                    errorMessage += "Bad Request. The request was invalid or cannot be otherwise served.";
                    break;
                case 401:
                    errorMessage += "Unauthorized. Authentication failed or API key is invalid.";
                    break;
                case 403:
                    errorMessage += "Forbidden. The request is understood, but it has been refused.";
                    break;
                case 404:
                    errorMessage += "Not Found. The requested resource could not be found.";
                    break;
                case 429:
                    errorMessage += "Too Many Requests. Rate limit exceeded.";
                    break;
                case 500:
                    errorMessage += "Internal Server Error. An error occurred on the server.";
                    break;
                case 503:
                    errorMessage += "Service Unavailable. The service is temporarily unavailable.";
                    break;
                default:
                    errorMessage += "An unexpected error occurred.";
                    break;
            }

            if (!errorBody.isEmpty()) {
                errorMessage += " Response body: " + errorBody;
            }

            throw new Exception(errorMessage);
        }
    }

    // getMsg method, completes and returns json request
    static String getMsg(String language, String fileContent) {
        String comments = "";
        return "{ \"message\": \"" + comments + "Translate this code in " + language + "\\" + fileContent;

    }
}

