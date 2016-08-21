package com.financialwhirlpool.antisnap.Class;

import io.realm.RealmObject;

/**
 * Created by an vo on 8/11/2016.
 */
public class FriendRequest extends RealmObject {
    private String uid;
    private String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public FriendRequest(String uid) {
        this.uid = uid;
    }

    public FriendRequest() {
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
