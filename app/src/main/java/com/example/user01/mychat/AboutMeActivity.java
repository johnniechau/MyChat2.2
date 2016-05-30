package com.example.user01.mychat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AboutMeActivity extends Activity {
    private TextView mUsernameTextView;
    private ImageView mProfileImageView;
    private Button mChangeProfileButton;
    private Button mUploadProfileButton;
    private Button mCapturePictureButton;
    private Button mShowMembersButton;
 //   private TextView mUploadPercentage;
 //   private ProgressBar mUploadProgressBar;
    private ImageView mPreviewImageView;
    private Button mLogoutButton;

    private Bitmap mBitmap;

    private String mUsername;
    private String profileURL;
    private Uri profileUri;
    private String filePath;
    private String fileName;
    private String fileExt;
    private Uri captureUri;
    long totalSize = 0;

    JSONParser mJSONParser = new JSONParser();

    private static String url_about_me = "http://192.168.1.100/mychat/upload_profile_x.php";
    private static final String TAG_SUCCESS = "success";
    private static final String TAG_MESSAGE = "message";
    private static final String TAG_PROFILE = "profile";
    private static final String TAG_USERNAME = "username";
    private static final int PICK_IMAGE_REQUEST_CODE = 1;
    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 2;

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_me);

        mUsernameTextView = (TextView)findViewById(R.id.usernameTextView);
        Intent i = getIntent();
        mUsername = i.getStringExtra(TAG_USERNAME);
        String showName = "Welcome " + mUsername;
        mUsernameTextView.setText(showName);

        mProfileImageView = (ImageView)findViewById(R.id.profileImageView);
        profileURL = i.getStringExtra("profileURL");

        new GetImage().execute(profileURL);

        mChangeProfileButton = (Button)findViewById(R.id.change_profile_button);
        mChangeProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageChooser();
            }
        });

        mUploadProfileButton = (Button)findViewById(R.id.upload_profile_button);
        mUploadProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new UploadImage().execute();
            }
        });

//        mUploadPercentage = (TextView)findViewById(R.id.upload_Percentage);
//        mUploadProgressBar = (ProgressBar)findViewById(R.id.upload_progressBar);

        mPreviewImageView = (ImageView)findViewById(R.id.previewImageView);

        mShowMembersButton = (Button)findViewById(R.id.show_members_button);
        mShowMembersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(AboutMeActivity.this, ShowMembersActivity.class);
                i.putExtra(TAG_USERNAME, mUsername);
                startActivity(i);
            }
        });

        mLogoutButton = (Button)findViewById(R.id.logout_button);
        mLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //// TODO: 2016/5/16/0016   reset the pref 
                Intent i = new Intent(AboutMeActivity.this, LoginActivity.class);
                startActivity(i);
                finish();
            }
        });

        mCapturePictureButton = (Button)findViewById(R.id.capture_picture_button);
        mCapturePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureImage();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);
        if(requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == RESULT_OK
                && intent != null && intent.getData() != null){
            profileUri = intent.getData();
            Log.e("uri", profileUri.toString());
            try{
                mBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), profileUri);
                mPreviewImageView.setImageBitmap(mBitmap);

                if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT){//4.4及以上

                    String wholeID = DocumentsContract.getDocumentId(profileUri);
                    String id = wholeID.split(":")[1];
                    String[] column = { MediaStore.Images.Media.DATA };
                    String sel = MediaStore.Images.Media._ID + "= ?";
                    Cursor cursor = getApplication().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, column,
                            sel, new String[] { id }, null);
                    int columnIndex = cursor.getColumnIndex(column[0]);
                    if (cursor.moveToFirst()) {
                        filePath = cursor.getString(columnIndex);
                    }
                    cursor.close();
                }else{//4.4以下，即4.4以上获取路径的方法

                    String[] projection = { MediaStore.Images.Media.DATA };
                    Cursor cursor = getApplication().getContentResolver().query(profileUri, projection, null, null, null);
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    cursor.moveToFirst();
                    filePath = cursor.getString(column_index);
                }


                Log.i("postStr", filePath);
                fileName = filePath.substring(filePath.lastIndexOf("/")+1);
                Log.i("postStr", fileName);
                fileExt = filePath.substring(filePath.lastIndexOf(".")+1);
                Log.i("postStr", fileExt);

            }catch(IOException e){
                e.printStackTrace();
            }
        }else if(requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE ){
            if(resultCode == RESULT_OK) {
                Log.i("postStr", captureUri.toString());
                try{
                    mBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), captureUri);
                    profileUri = captureUri;
                    filePath = captureUri.getPath();
                    Log.i("postStr", filePath);
                    mPreviewImageView.setImageBitmap(mBitmap);
                }catch(IOException e){
                    e.printStackTrace();
                }

            }else if(resultCode == RESULT_CANCELED){
                Toast.makeText(getApplicationContext(),
                        "User cancelled image capture", Toast.LENGTH_SHORT)
                        .show();
            }else {
                Toast.makeText(getApplicationContext(),
                        "Sorry! Failed to capture image", Toast.LENGTH_SHORT)
                        .show();
            }

        }
    }

    private void captureImage(){
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        captureUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
        i.putExtra(MediaStore.EXTRA_OUTPUT, captureUri);
        startActivityForResult(i, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
    }

    private void showImageChooser(){
        Intent i = new Intent();
        i.setType("image/*");
//        i.setAction(Intent.ACTION_GET_CONTENT);
//        i.setAction(Intent.ACTION_PICK);
//           i.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            i.setAction(Intent.ACTION_OPEN_DOCUMENT);
        }else{
            i.setAction(Intent.ACTION_GET_CONTENT);
        }
        startActivityForResult(i, PICK_IMAGE_REQUEST_CODE);
    }

    class UploadImage extends AsyncTask<Void, Integer, String>{
        @Override
        protected void onPreExecute(){
//            mUploadProgressBar.setProgress(0);
  //          mUploadProgressBar.setVisibility(View.VISIBLE);
            super.onPreExecute();
        }
//
//        @Override
//        protected void onProgressUpdate(Integer... progress) {
//            // Making progress bar visible
//            mUploadProgressBar.setVisibility(View.VISIBLE);
//
//            // updating progress bar value
//            mUploadProgressBar.setProgress(progress[0]);
//
//            // updating percentage value
//            mUploadPercentage.setText(String.valueOf(progress[0]) + "%");
//        }

        @Override
        protected void onPostExecute(String message){
     //       Log.i("message", message);
            Log.i("postStr", message);
            showAlert(message);
 //           Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
 //           mUploadProgressBar.setVisibility(View.GONE);
            new GetImage().execute(profileURL);
//            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            super.onPostExecute(message);
        }


        @Override
        protected String doInBackground(Void... params) {
            String responseString = "请先选择图片:)";
            if(mBitmap != null){
                responseString = uploadImage(filePath ,url_about_me);
            }
            return responseString;
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

    class GetImage extends AsyncTask<String, Void, Bitmap>{
        ProgressDialog loading;
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            loading = ProgressDialog.show(AboutMeActivity.this, "Loading profile...", null, true, true);
        }

        @Override
        protected void onPostExecute(Bitmap b){
            super.onPostExecute(b);
            loading.dismiss();
            if(b != null) {
                mProfileImageView.setImageBitmap(b);
            }else {
                mProfileImageView.setImageDrawable(getResources().getDrawable(R.drawable.default_profile));
            }
        }
        @Override
        protected Bitmap doInBackground(String ... params) {
            String strUrl = params[0];
            Log.i("myChat", strUrl);
            URL url = null;
            Bitmap image =null;
            try{
                url = new URL(strUrl);
                image = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            }catch(MalformedURLException e){
                e.printStackTrace();
            }catch(IOException e){
                e.printStackTrace();
            }


            return image;
        }
    }

    public String uploadImage(String filename, String upUrl)
    {
        String uploadUrl = upUrl;
        String end = "\r\n";
        String twoHyphens = "--";		// 两个连字符
        String boundary = "******";		// 分界符的字符串
        String response = "$$$";

//        String params = "param?username=" + mUsername;
        try
        {
            URL url = new URL(uploadUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setRequestMethod("POST");
            // 设置Http请求头
            httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
            httpURLConnection.setRequestProperty("Charset", "UTF-8");
            //  必须在Content-Type 请求头中指定分界符中的任意字符串
            httpURLConnection.setRequestProperty("Content-Type","multipart/form-data;boundary=" + boundary);


            //定义数据写入流，准备上传文件
            DataOutputStream dos = new DataOutputStream(httpURLConnection.getOutputStream());

            //test
            StringBuffer sb = new StringBuffer();
            sb.append(twoHyphens).append(boundary).append(end);
            sb.append("Content-Disposition: form-data; name").append("=").append("username").append(end);
            sb.append(end);
            sb.append(mUsername);
            sb.append(end);
            dos.write(sb.toString().getBytes());
            //test


            dos.writeBytes(twoHyphens + boundary + end);
            //设置与上传文件相关的信息
            dos.writeBytes("Content-Disposition: form-data; name=\"userfile\"; filename=\""
                    + filename.substring(filename.lastIndexOf("/") + 1)
                    + "\"" + end);
            dos.writeBytes(end);


            FileInputStream fis = new FileInputStream(filename);
            Log.i("postStr fileSize ", "" + fis.available());
            byte[] buffer = new byte[8192]; // 8k
            int count = 0;
            // 读取文件夹内容，并写入OutputStream对象
            while ((count = fis.read(buffer)) != -1)
            {
                dos.write(buffer, 0, count);
            }
            fis.close();
            dos.writeBytes(end);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + end);
            dos.flush();
            // 开始读取从服务器传过来的信息
            InputStream is = httpURLConnection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is, "utf-8");
            BufferedReader reader = new BufferedReader(isr);


            JSONObject jsonObj;
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }

            Log.d("postStr JSON Parser", "result: " + result.toString());
            jsonObj = new JSONObject(result.toString());
            response = jsonObj.getString(TAG_MESSAGE);
            profileURL = jsonObj.getString(TAG_PROFILE);
            dos.close();
            is.close();
        }
        catch (Exception e)
        {
            Log.i("postStr error", e.getMessage());
        }
        finally{

        }
        return response;
    }

    public Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * returning image / video
     */
    private static File getOutputMediaFile(int type) {

        // External sdcard location
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "myChat");

//        File mediaStorageDir = new File(
//                android.os.Environment.getExternalStorageDirectory()
//                        + File.separator + "myChat");

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("error", "Oops! Failed create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }
}


