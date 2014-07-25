package com.dicejobsearch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.dicejobsearch.entity.RecommendedJobItem;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class RecommendationActivity extends Activity implements AdapterView.OnItemClickListener{

    private String accessToken = null;
    private String id = null;
    private ArrayList<RecommendedJobItem> mJobsList;
    private ListView jobsListView;
    private ProgressBar progressBar;

    private static final String SEARCH_FIELDS = "recommendationRating,job.id,job.company.name,job.position.location";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendation);

        jobsListView = (ListView) findViewById(R.id.jobsListView);
        jobsListView.setOnItemClickListener(this);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        Bundle bundle = getIntent().getExtras();
        if (savedInstanceState != null) {
            accessToken = savedInstanceState.getString(KeyHelper.ACCESS_TOKEN);
            mJobsList = savedInstanceState.getParcelableArrayList(KeyHelper.JOB_LIST);
            jobsListView.setAdapter(new RecommendedJobAdapter(RecommendationActivity.this, mJobsList));
        }
        else if (bundle != null) {
            if (bundle.containsKey(KeyHelper.ID) && bundle.containsKey(KeyHelper.ACCESS_TOKEN)) {
                accessToken = bundle.getString(KeyHelper.ACCESS_TOKEN);
                id = bundle.getString(KeyHelper.ID);

                List<NameValuePair> paramsList = new LinkedList<NameValuePair>();
                paramsList.add(new BasicNameValuePair("fields", SEARCH_FIELDS));
                String urlParams = URLEncodedUtils.format(paramsList, "UTF-8");
                progressBar.setVisibility(View.VISIBLE);
                jobsListView.setVisibility(View.INVISIBLE);
                new FindRecommndedJobs().execute(Credentials.BASE_URL + "/" + id + "/recommendedJobs" + "?" + urlParams, accessToken);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(KeyHelper.JOB_LIST, mJobsList);
        outState.putString(KeyHelper.ACCESS_TOKEN, accessToken);
        super.onSaveInstanceState(outState);
    }

    private void authorize() {
        List<NameValuePair> authParamsList = new LinkedList<NameValuePair>();
        authParamsList.add(new BasicNameValuePair("grant_type", "client_credentials"));
        String authParams = URLEncodedUtils.format(authParamsList, "UTF-8");
        new AuthJobSearchTask().execute(Credentials.AUTH_URL + "?" + authParams, Credentials.USER_ID);

        progressBar.setVisibility(View.VISIBLE);
        jobsListView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(this, JobActivity.class);
        intent.putExtra(KeyHelper.ACCESS_TOKEN, accessToken);
        intent.putExtra(KeyHelper.ID, mJobsList.get(position).getId());
        startActivity(intent);
    }

    class AuthJobSearchTask extends AuthorizeTask {

        public AuthJobSearchTask() {}

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

    class FindRecommndedJobs extends AsyncTask<String, Void, JSONObject> {
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

            try {
                mJobsList = new ArrayList<RecommendedJobItem>();
                JSONArray searchResult = jsonObject.getJSONArray(JSONHelper.ITEMS);
                for (int i = 0; i < searchResult.length(); i++) {
                    mJobsList.add(new RecommendedJobItem(searchResult.getJSONObject(i)));
                }
                jobsListView.setAdapter(new RecommendedJobAdapter(RecommendationActivity.this, mJobsList));
                jobsListView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.INVISIBLE);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    class RecommendedJobAdapter extends BaseAdapter {
        private LayoutInflater inflater;
        private ArrayList<RecommendedJobItem> data;

        public RecommendedJobAdapter(Context context, ArrayList<RecommendedJobItem> data) {
            inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            this.data = data;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public RecommendedJobItem getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = inflater.inflate(R.layout.row_job_search, parent, false);
                holder.titleTv = (TextView) convertView.findViewById(R.id.titleTv);
                holder.locationTv = (TextView) convertView.findViewById(R.id.locationTv);
                holder.companyTv = (TextView) convertView.findViewById(R.id.companyTv);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            RecommendedJobItem job = data.get(position);
            holder.titleTv.setText(job.getRecommendationRating() + "%");
            holder.companyTv.setText(job.getCompanyName());
            String county = job.getCountry();
            String city = job.getCity();
            String postalCode = job.getPostalCode();
            if (county != null && city != null && postalCode != null)
                holder.locationTv.setText(job.getCountry() + ", " + job.getCity() + ", " + job.getPostalCode());
            return convertView;
        }

        class ViewHolder {
            TextView titleTv;
            TextView locationTv;
            TextView companyTv;
        }
    }

}
