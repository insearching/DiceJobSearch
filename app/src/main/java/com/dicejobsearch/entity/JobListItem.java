package com.dicejobsearch.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.dicejobsearch.utils.JSONHelper;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by insearching on 19.07.2014.
 */
public class JobListItem implements Parcelable{
    private String id;
    private String title;
    private String company;
    private String location;
    private String url;

    public JobListItem(JSONObject json){
        try {
            id = json.getString(JSONHelper.ID);
            url = json.getString(JSONHelper.WEB_URL);
            JSONObject positionObject = json.getJSONObject(JSONHelper.POSITION);
            title = positionObject.getString(JSONHelper.TITLE);
            JSONObject locationObject = positionObject.getJSONObject(JSONHelper.LOCATION);
            location = locationObject.getString(JSONHelper.CITY) +", "+ locationObject.getString(JSONHelper.COUNTRY);
            JSONObject companyObject = json.getJSONObject(JSONHelper.COMPANY);
            company = companyObject.getString(JSONHelper.NAME);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JobListItem(Parcel in) {

    }

    public String getId(){
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCompany() {
        return company;
    }

    public String getUrl() {
        return url;
    }

    public String getLocation() {
        return location;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator CREATOR =
            new Parcelable.Creator() {
                public JobListItem createFromParcel(Parcel in) {
                    return new JobListItem(in);
                }

                public JobListItem[] newArray(int size) {
                    return new JobListItem[size];
                }
            };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(company);
        dest.writeString(location);
        dest.writeString(url);
    }
}