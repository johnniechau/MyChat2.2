package com.example.user01.mychat;

/**
 * Created by Johnnie on 2016/5/17/0017.
 */
public class ImageItem {
    private String mUsername;
    private String mUrl;

    public String toString(){
        return getUsername();
    }

    public ImageItem(String username, String url){
        setUsername(username);
        setUrl(url);
    }

    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String username) {
        mUsername = username;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }
}
