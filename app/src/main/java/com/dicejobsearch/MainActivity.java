package com.dicejobsearch;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import com.dicejobsearch.entity.JobListItem;
import com.dicejobsearch.utils.Credentials;
import com.dicejobsearch.utils.JSONHelper;
import com.dicejobsearch.utils.KeyHelper;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
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

public class MainActivity extends Activity implements AdapterView.OnItemClickListener {

    private String accessToken = null;
    private boolean isSearching = false;
    private String mQuery = null;

    private ListView jobsListView;
    private ProgressBar progressBar;
    private TextView infoTv;

    private ArrayList<JobListItem> mJobsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        jobsListView = (ListView) findViewById(R.id.jobsListView);
        jobsListView.setOnItemClickListener(this);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        infoTv = (TextView) findViewById(R.id.infoTv);

        mJobsList = new ArrayList<JobListItem>();

        if (savedInstanceState == null) {
            SharedPreferences prefs = getSharedPreferences("credentials", MODE_MULTI_PROCESS);
            if (prefs.contains(KeyHelper.ACCESS_TOKEN))
                accessToken = prefs.getString(KeyHelper.ACCESS_TOKEN, null);
            else
                authorize();
        } else {
            accessToken = savedInstanceState.getString(KeyHelper.ACCESS_TOKEN);
            mJobsList = savedInstanceState.getParcelableArrayList("job_list");
            isSearching = savedInstanceState.getBoolean("is_searching");
            mQuery = savedInstanceState.getString("query");
            jobsListView.setAdapter(new JobAdapter(MainActivity.this, mJobsList));
            infoTv.setVisibility(View.INVISIBLE);
            if (mJobsList.size() == 0) {
                infoTv.setText(getString(R.string.no_jobs_found));
                infoTv.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("job_list", mJobsList);
        outState.putString(KeyHelper.ACCESS_TOKEN, accessToken);
        outState.putBoolean("is_searching", isSearching);
        outState.putString("query", mQuery);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final MenuItem searchItem = menu.findItem(R.id.search);
        final SearchView searchView =
                (SearchView) searchItem.getActionView();

        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        searchView.setQueryHint(getString(R.string.search_hint));
        if (isSearching) {
            searchItem.expandActionView();
        }
        if (mQuery != null) {
            searchView.setQuery(mQuery, false);
        }
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSearching = true;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mQuery = query;
                if (accessToken == null) {
                    authorize(query);
                }

                findJobs(query);

                infoTv.setVisibility(View.INVISIBLE);
                jobsListView.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);

                searchItem.collapseActionView();
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mQuery = newText;
                return false;
            }
        });
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem searchItem = menu.findItem(R.id.search);
        searchItem.setVisible(accessToken != null);
        return true;
    }

    private void authorize() {
        List<NameValuePair> authParamsList = new LinkedList<NameValuePair>();
        authParamsList.add(new BasicNameValuePair("grant_type", "client_credentials"));
        String authParams = URLEncodedUtils.format(authParamsList, "UTF-8");
        new AuthorizeTask().execute(Credentials.AUTH_URL + "?" + authParams, Credentials.USER_ID);

        progressBar.setVisibility(View.VISIBLE);
        jobsListView.setVisibility(View.INVISIBLE);
        infoTv.setVisibility(View.INVISIBLE);
    }

    private void authorize(String query) {
        List<NameValuePair> authParamsList = new LinkedList<NameValuePair>();
        authParamsList.add(new BasicNameValuePair("grant_type", "client_credentials"));
        String authParams = URLEncodedUtils.format(authParamsList, "UTF-8");
        new AuthorizeTask(query).execute(Credentials.AUTH_URL + "?" + authParams, Credentials.USER_ID);

        progressBar.setVisibility(View.VISIBLE);
        jobsListView.setVisibility(View.INVISIBLE);
        infoTv.setVisibility(View.INVISIBLE);
    }

    private void findJobs(String query) {
        List<NameValuePair> paramsList = new LinkedList<NameValuePair>();
        paramsList.add(new BasicNameValuePair("fields", "id,company,position"));
        paramsList.add(new BasicNameValuePair("q", query));
        String urlParams = URLEncodedUtils.format(paramsList, "UTF-8");

        new JobSearchTask().execute(Credentials.BASE_URL + "?" + urlParams, accessToken);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(this, JobActivity.class);
        intent.putExtra(KeyHelper.ACCESS_TOKEN, accessToken);
        intent.putExtra(KeyHelper.ID, mJobsList.get(position).getId());
        startActivity(intent);
    }

    class AuthorizeTask extends AsyncTask<String, Void, JSONObject> {

        String query;

        public AuthorizeTask() {
        }

        public AuthorizeTask(String query) {
            this.query = query;
        }

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

                if (query != null)
                    findJobs(query);
                progressBar.setVisibility(View.INVISIBLE);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    class JobSearchTask extends AsyncTask<String, Void, JSONObject> {

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
            progressBar.setVisibility(View.INVISIBLE);
            try {
                String total = jsonObject.getString(JSONHelper.TOTAL);
                if (Integer.valueOf(total) == 0) {
                    infoTv.setText(getString(R.string.no_jobs_found));
                    infoTv.setVisibility(View.VISIBLE);

                } else {
                    mJobsList = new ArrayList<JobListItem>();
                    JSONArray searchResult = jsonObject.getJSONArray(JSONHelper.SEARCH_RESULTS);
                    for (int i = 0; i < searchResult.length(); i++) {
                        mJobsList.add(new JobListItem(searchResult.getJSONObject(i)));
                    }
                    jobsListView.setAdapter(new JobAdapter(MainActivity.this, mJobsList));
                    jobsListView.setVisibility(View.VISIBLE);

                    isSearching = false;
                    mQuery = null;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    class JobAdapter extends BaseAdapter {

        private LayoutInflater inflater;
        private ArrayList<JobListItem> data;

        public JobAdapter(Context context, ArrayList<JobListItem> data) {
            inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            this.data = data;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public JobListItem getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = new ViewHolder();
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

            JobListItem job = data.get(position);
            holder.titleTv.setText(job.getTitle());
            holder.companyTv.setText(job.getCompany());
            holder.locationTv.setText(job.getLocation());
            return convertView;
        }

        class ViewHolder {
            TextView titleTv;
            TextView locationTv;
            TextView companyTv;
        }
    }


}
