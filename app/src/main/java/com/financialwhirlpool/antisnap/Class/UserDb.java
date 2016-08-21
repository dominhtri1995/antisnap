package com.financialwhirlpool.antisnap.Class;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by an vo on 8/20/2016.
 */
public class UserDb extends RealmObject {
    String profile;
    String name;
    String email;
    @PrimaryKey
    String uid;
    String gender;

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getProfile() {

        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public UserDb() {
    }

    public UserDb(String name, String email, String uid) {
        this.name = name;
        this.email = email;
        this.uid = uid;
    }
}
