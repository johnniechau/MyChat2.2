package com.example.user01.mychat;

import com.example.user01.mychat.GridViewImageAdapter;
import com.example.user01.mychat.Utils;
import com.example.user01.mychat.ImageDownloader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class ShowMembersActivity extends Activity {
    private Utils utils;
    private GridView mGridView;
    private int columnWidth;
//    private ArrayList<String> imagePaths = new ArrayList<String>();
    private ArrayList<ImageItem> mItems = new ArrayList<ImageItem>();
//    private GridViewImageAdapter adapter;
    private ImageDownloader<ImageView> mImageDownLoader;

    JSONParser mJSONParser = new JSONParser();
    private SharedPreferences pref;
    private String username;

    private static String url_show_members = "http://192.168.1.100/mychat/show_members_x.php";
    private static final String TAG_COUNT = "count";
    private static final String TAG_USERS = "users";

    private static final String TAG_APP = "myChat";

    public static final int NUM_OF_COLUMNS = 3;
    public static final int GRID_PADDING = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_members);

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        username = pref.getString("username", "");

        //get the members' images from the server
        new ShowMembersAsnyc().execute(username);

//        mGridView = (GridView)findViewById(R.id.grid_view);
        utils = new Utils(this);
//        InitilizeGridLayout();
//
//        imagePaths = utils.getFilePaths();
//
//        adapter = new GridViewImageAdapter(ShowMembersActivity.this, imagePaths, columnWidth);
//
//        mGridView.setAdapter(adapter);

        mGridView = (GridView)findViewById(R.id.grid_view);
        setupAdapter();

        mImageDownLoader = new ImageDownloader<ImageView>(new Handler(), TAG_APP);
        mImageDownLoader.setListener(new ImageDownloader.Listener<ImageView>(){
            @Override
            public void onImageDownloaded(ImageView imageView, Bitmap bitmap) {
               imageView.setImageBitmap(bitmap);
            }

        });
        mImageDownLoader.start();
        mImageDownLoader.getLooper();
    }


    void setupAdapter(){
        if(mGridView == null)
            return;
        if(mItems != null){
        //    mGridView.setAdapter(new ArrayAdapter<ImageItem>(this, android.R.layout.simple_gallery_item, mItems));
            InitilizeGridLayout();
            mGridView.setAdapter(new ImageItemAdapter(this, mItems));

        } else {
            mGridView.setAdapter(null);
        }

    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mImageDownLoader.clearQueue();
        mImageDownLoader.quit();
        Log.i("download", "background thread destroyed");
    }

    private void InitilizeGridLayout(){
        Resources r = getResources();
        float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                GRID_PADDING, r.getDisplayMetrics());
        columnWidth = (int) ((utils.getScreenWidth() - ((NUM_OF_COLUMNS + 1) * padding)) / NUM_OF_COLUMNS);

        mGridView.setNumColumns(NUM_OF_COLUMNS);
        mGridView.setColumnWidth(columnWidth);
        mGridView.setStretchMode(GridView.NO_STRETCH);
        mGridView.setPadding((int) padding, (int) padding, (int) padding,
                (int) padding);
        mGridView.setHorizontalSpacing((int) padding);
        mGridView.setVerticalSpacing((int) padding);
    }

    class ShowMembersAsnyc extends AsyncTask<String, Void, JSONObject>{

        @Override
        protected JSONObject doInBackground(String... args) {
            try{
                HashMap<String, String> params = new HashMap<>();
                params.put("username", args[0]);
                JSONObject json = mJSONParser.makeHttpRequest(url_show_members, "POST", params);

                if(json != null){
                    Log.d("JSON result : ", json.toString());

                    return json;
                }
            }catch(Exception e){
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(JSONObject json){
            int count = 0;
            String message = "";

            if(json != null){

                try{
                    count = json.getInt(TAG_COUNT);


                    if(count > 0){
                     //   users = json.getJSONArray(TAG_USERS).toString();
                        // users = json.getJSONArray(TAG_USERS).getJSONObject(0).getString("photo");
                        JSONArray jsonArray = json.getJSONArray(TAG_USERS);
                        for(int i = 0; i < jsonArray.length(); i++){
                            ImageItem item;
                            String strUsername = jsonArray.getJSONObject(i).getString("username");
                            String strPhoto = jsonArray.getJSONObject(i).getString("photo");
                            item = new ImageItem(strUsername, strPhoto);
                            mItems.add(item);
                        }
                        message = "服务器上还有 " + mItems.size() + " 名用户";
                        showAlert(message);
                        setupAdapter();
                    }

                }catch(JSONException e){
                    e.printStackTrace();
                }
            }
        }
    }

    private void showAlert(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message).setTitle("Response from Servers")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // do nothing
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private class ImageItemAdapter extends ArrayAdapter<ImageItem>{
        private Activity mActivity;
        public ImageItemAdapter(Activity activity, ArrayList<ImageItem> items) {
            super(activity, 0, items);
            mActivity = activity;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            if(convertView == null){
                convertView = mActivity.getLayoutInflater().inflate(R.layout.image_item, parent, false);
            }
            ImageView imageView = (ImageView)convertView.findViewById(R.id.image_item_imageView);
//            imageView.setImageResource(R.drawable.default_profile);

            ImageItem item = getItem(position);
            mImageDownLoader.queueImage(imageView, item.getUrl());

            imageView.setOnClickListener(new OnImageClickListener(position, item));

            return convertView;
        }
    }

    class OnImageClickListener implements View.OnClickListener {

        int _postion;
        ImageItem _item;

        // constructor
        public OnImageClickListener(int position, ImageItem item) {
            this._postion = position;
            this._item = item;
        }

        @Override
        public void onClick(View v) {
            // on selecting grid view image
            // launch full screen activity
        //    showAlert("您好!" + " 我叫 " + _item.getUsername());
            Intent i = new Intent(ShowMembersActivity.this, ChatActivity.class);
            i.putExtra("name", _item.getUsername());
            startActivity(i);
        }

    }

}
