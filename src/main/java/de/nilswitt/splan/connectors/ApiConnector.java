/*
 * Copyright (c) 2021. Nils Witt
 */

package de.nilswitt.splan.connectors;

import com.google.gson.Gson;
import de.nilswitt.splan.dataModels.Config;
import de.nilswitt.splan.dataModels.Klausur;
import de.nilswitt.splan.dataModels.Lesson;
import de.nilswitt.splan.dataModels.VertretungsLesson;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class ApiConnector {
    private static final Logger logger = LogManager.getLogger(ApiConnector.class);
    private final OkHttpClient client;
    private final Config config;
    private final Gson gson = new Gson();
    private final MediaType mediaType = MediaType.parse("application/json");


    public ApiConnector(Config config) {
        this.config = config;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Überprüfen der Gültigkeit des Zugriffstoken auf die Api
     *
     * @param bearer token for api access
     * @param url    base api url
     * @return validity of bearer to given url
     */
    public static boolean verifyBearer(@NotNull String bearer, @NotNull String url) {
        OkHttpClient client = new OkHttpClient();
        boolean isValid = false;
        Request request = new Request.Builder()
                .url(url.concat("/user"))
                .addHeader("Authorization", "Bearer ".concat(bearer))
                .build();
        try {
            Response response = client.newCall(request).execute();
            // Api gibt den status 200 zurück, wenn alles  gültig ist.
            if (response.code() == 200) {
                isValid = true;
                logger.info("Bearer valid");
            } else {
                logger.warn("Bearer invalid");
            }
        } catch (java.net.UnknownHostException e) {
            e.printStackTrace();
            logger.warn("Host not reachable");
        } catch (Exception e) {
            e.printStackTrace();
            logger.warn("Exception while verifying Bearer");
        }

        return isValid;
    }

    public VertretungsLesson[] getVertretungenByDate(String date) {
        Request request = new Request.Builder()
                .url(this.config.getUrl().concat("/replacementLessons/date/").concat(date))
                .addHeader("Authorization", "Bearer ".concat(this.config.getBearer()))
                .build();

        try {
            Response response = client.newCall(request).execute();
            assert response.body() != null;
            String json = response.body().string();
            response.close();
            return gson.fromJson(json, VertretungsLesson[].class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public Lesson[] getLessons() {
        Request request = new Request.Builder()
                .url(this.config.getUrl().concat("/lessons"))
                .addHeader("Authorization", "Bearer ".concat(this.config.getBearer()))
                .build();

        try {
            Response response = client.newCall(request).execute();
            assert response.body() != null;
            String json = response.body().string();
            response.close();
            return gson.fromJson(json, Lesson[].class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public void deleteVertretung(String id) {
        Request request = new Request.Builder()
                .url(this.config.getUrl().concat("/replacementLessons/id/").concat(id))
                .delete()
                .addHeader("Authorization", "Bearer ".concat(this.config.getBearer()))
                .build();

        try {
            Response response = client.newCall(request).execute();
            response.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addVertretungen(ArrayList<VertretungsLesson> vertretungen) {

        RequestBody body = RequestBody.create(gson.toJson(vertretungen), mediaType);
        Request request = new Request.Builder()
                .url(this.config.getUrl().concat("/replacementLessons"))
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer ".concat(this.config.getBearer()))
                .build();

        try {
            Response response = client.newCall(request).execute();
            response.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addLessons(ArrayList<Lesson> lessons) {

        RequestBody body = RequestBody.create(gson.toJson(lessons), mediaType);
        Request request = new Request.Builder()
                .url(this.config.getUrl().concat("/lessons"))
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer ".concat(this.config.getBearer()))
                .build();

        try {
            Response response = client.newCall(request).execute();
            response.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public VertretungsLesson[] getReplacementLessonByFilter(String info) {

        RequestBody body = RequestBody.create("{\"info\":\"" + info + "\"}", mediaType);

        Request request = new Request.Builder()
                .url(this.config.getUrl().concat("/replacementLessons/find/"))
                .post(body)
                .addHeader("Authorization", "Bearer ".concat(this.config.getBearer()))
                .build();
        try {
            Response response = client.newCall(request).execute();
            assert response.body() != null;
            String json = response.body().string();
            response.close();
            return gson.fromJson(json, VertretungsLesson[].class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void addExams(ArrayList<Klausur> exams) {
        RequestBody body = RequestBody.create(gson.toJson(exams), mediaType);
        Request request = new Request.Builder()
                .url(this.config.getUrl().concat("/exams"))
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer ".concat(this.config.getBearer()))
                .build();

        try {
            Response response = client.newCall(request).execute();
            response.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
