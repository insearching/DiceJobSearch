package com.dicejobsearch.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.dicejobsearch.utils.JSONHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by insearching on 20.07.2014.
 */
public class JobDetails implements Parcelable {

    private String description;
    private String[] skills;
    private String webUrl;

    public JobDetails(JSONObject jsonObject) {
        try {
            description = jsonObject.getString(JSONHelper.DESCRIPTION);
            skills = exportToArray(jsonObject.getJSONArray(JSONHelper.SKILLS));
            webUrl = jsonObject.getString(JSONHelper.WEB_URL);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String[] exportToArray(JSONArray array) {
        String[] newArray = new String[array.length()];
        try {
            for (int i = 0; i < array.length(); i++) {
                newArray[i] = array.getString(i);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return newArray;
    }

    public String getDescription() {
        return description;
    }

    public String[] getSkills() {
        return skills;
    }

    public String getWebUrl() {
        return webUrl;
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
        dest.writeString(description);
        dest.writeArray(skills);
        dest.writeString(webUrl);
    }
}
