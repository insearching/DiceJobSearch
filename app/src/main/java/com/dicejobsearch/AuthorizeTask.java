package com.dicejobsearch;

import android.os.AsyncTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by insearching on 24.07.2014.
 */
public class AuthorizeTask extends AsyncTask<String, Void, JSONObject> {

    @Override
    protected JSONObject doInBackground(String... params) {
        try {
            HttpClient client = new DefaultHttpClient();
            HttpPost request = new HttpPost(params[0]);
            request.setHeader("Authorization", "Basic " + params[1]);
            request.setHeader("Content-Type", "application/x-www-form-urlencoded");
            HttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() == 200)
                return new JSONObject(EntityUtils.toString(response.getEntity()));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
