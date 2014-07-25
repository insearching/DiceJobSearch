package com.dicejobsearch;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.dicejobsearch.entity.JobDetails;
import com.dicejobsearch.utils.Credentials;
import com.dicejobsearch.utils.JSONHelper;
import com.dicejobsearch.utils.KeyHelper;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class JobActivity extends Activity {

    private ProgressBar progressBar;
    private WebView descrWebView;
    private TextView skillsTv;

    private String accessToken = null;
    private String id = null;
    private JobDetails jobDetails = null;
    private String skillsStr = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_details);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        skillsTv = (TextView) findViewById(R.id.skillsTv);
        descrWebView = (WebView) findViewById(R.id.descriptionWeb);
        descrWebView.getSettings().setJavaScriptEnabled(true);
        descrWebView.setBackgroundColor(Color.TRANSPARENT);

        Bundle extras = getIntent().getExtras();
        if (savedInstanceState != null) {
            accessToken = savedInstanceState.getString(KeyHelper.ACCESS_TOKEN);
            skillsStr = savedInstanceState.getString(KeyHelper.SKILLS);
            jobDetails = savedInstanceState.getParcelable(KeyHelper.JOB_DETAILS);

            skillsTv.setText(skillsStr);
            descrWebView.loadDataWithBaseURL(null, jobDetails.getDescription(), "text/html", "utf-8", null);
        } else if (extras != null) {
            accessToken = extras.getString(KeyHelper.ACCESS_TOKEN);
            id = extras.getString(KeyHelper.ID);


            new GetJobInfo().execute(Credentials.BASE_URL + "/" + id, accessToken);

            progressBar.setVisibility(View.VISIBLE);
            descrWebView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(KeyHelper.ACCESS_TOKEN, accessToken);
        outState.putParcelable(KeyHelper.JOB_DETAILS, jobDetails);
        outState.putString(KeyHelper.SKILLS, skillsStr);
        super.onSaveInstanceState(outState);
    }

    class GetJobInfo extends AsyncTask<String, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                HttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet(params[0]);
                request.setHeader("Authorization", "Bearer " + params[1]);
                HttpResponse response = client.execute(request);
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED)
                    authorize();
                else
                    return new JSONObject(EntityUtils.toString(response.getEntity()));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);
            if (jsonObject == null)
                return;
            jobDetails = new JobDetails(jsonObject);
            descrWebView.loadDataWithBaseURL(null, jobDetails.getDescription(), "text/html", "utf-8", null);
            String[] skills = jobDetails.getSkills();
            skillsStr = getString(R.string.skills);
            for (int i = 0; i < skills.length; i++) {
                skillsStr += skills[i];
                if (i < skills.length - 1)
                    skillsStr += ", ";
            }
            skillsTv.setText(skillsStr);

            progressBar.setVisibility(View.INVISIBLE);
            descrWebView.setVisibility(View.VISIBLE);
        }
    }

    private void authorize() {
        List<NameValuePair> authParamsList = new LinkedList<NameValuePair>();
        authParamsList.add(new BasicNameValuePair("grant_type", "client_credentials"));
        String authParams = URLEncodedUtils.format(authParamsList, "UTF-8");
        new AuthJobTask().execute(Credentials.AUTH_URL + "?" + authParams, Credentials.USER_ID);

        progressBar.setVisibility(View.VISIBLE);
    }

    class AuthJobTask extends AuthorizeTask {
        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);
            if (jsonObject == null)
                return;
            try {
                accessToken = jsonObject.getString(JSONHelper.ACCESS_TOKEN);
                SharedPreferences prefs = getSharedPreferences("credentials", MODE_MULTI_PROCESS);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(KeyHelper.ACCESS_TOKEN, accessToken);
                editor.apply();

                progressBar.setVisibility(View.INVISIBLE);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
