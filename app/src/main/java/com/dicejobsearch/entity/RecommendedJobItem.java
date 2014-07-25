package com.dicejobsearch.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.dicejobsearch.utils.JSONHelper;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by insearching on 24.07.2014.
 */
public class RecommendedJobItem implements Parcelable {

    private String href = null;
    private String id = null;
    private String companyName = null;
    private String country = null;
    private String city = null;
    private String region = null;
    private String postalCode = null;
    private int recommendationRating;

    public RecommendedJobItem(JSONObject jsonObject) {
        try {
            href = jsonObject.getString(JSONHelper.HREF);
            String rating = jsonObject.getString(JSONHelper.RECOMMENDATION_RATING);
            recommendationRating = Integer.valueOf(rating.substring(2, 4));

            if(jsonObject.has(JSONHelper.JOB)) {
                JSONObject job = jsonObject.getJSONObject(JSONHelper.JOB);
                id = job.getString(JSONHelper.ID);
                if(job.has(JSONHelper.COMPANY)) {
                    JSONObject companyObject = job.getJSONObject(JSONHelper.COMPANY);
                    companyName = companyObject.getString(JSONHelper.NAME);

                    JSONObject positionObject = job.getJSONObject(JSONHelper.POSITION);
                    JSONObject locationObject = positionObject.getJSONObject(JSONHelper.LOCATION);
                    country = locationObject.getString(JSONHelper.COUNTRY);
                    city = locationObject.getString(JSONHelper.CITY);
                    region = locationObject.getString(JSONHelper.REGION);
                    postalCode = locationObject.getString(JSONHelper.POSTAL_CODE);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public RecommendedJobItem(Parcel in) {

    }

    public String getHref() {
        return href;
    }

    public String getId() {
        return id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getCountry() {
        return country;
    }

    public String getCity() {
        return city;
    }

    public String getRegion() {
        return region;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public int getRecommendationRating() {
        return recommendationRating;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator CREATOR =
            new Parcelable.Creator() {
                public RecommendedJobItem createFromParcel(Parcel in) {
                    return new RecommendedJobItem(in);
                }

                public RecommendedJobItem[] newArray(int size) {
                    return new RecommendedJobItem[size];
                }
            };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(href);
    }
}
