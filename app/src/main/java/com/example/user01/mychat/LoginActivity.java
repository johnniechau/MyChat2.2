package com.example.user01.mychat;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;


public class LoginActivity extends Activity {
    private EditText mUsername;
    private EditText mPassword;
    private Button mRegisterButton;
    private Button mLoginButton;

    private CheckBox mRememberPasswd;

    private EditText mLoginMessage;

    private String msgUsername;
    private String msgPassword;
    private String msgMessage;

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    JSONParser mJSONParser = new JSONParser();

    private static String url_register = "http://192.168.1.100/mychat/login_x.php";
    private static final String TAG_SUCCESS = "success";
    private static final String TAG_MESSAGE = "message";
    private static final String PREF_NAME = "myChat";
    private static final String REMEMBER_PASSWD = "rememberPasswd";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String IS_LOGGEDIN = "isLoggedIn";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mRegisterButton = (Button)findViewById(R.id.mychat_register_button);
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            //    msgMessage = "Register:" + msgUsername + " [ " + msgPassword + " ] ";
                Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(i);
            }
        });

        mLoginButton = (Button)findViewById(R.id.mychat_login_button);
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                msgUsername = mUsername.getText().toString();
                msgPassword = mPassword.getText().toString();
                msgMessage = "Login:" + msgUsername + " [ " + msgPassword + " ] ";
                mLoginButton.setEnabled(false);
                new LoginAsync().execute(msgUsername, msgPassword);
            }
        });



        mUsername = (EditText)findViewById(R.id.login_username);
        mPassword = (EditText)findViewById(R.id.login_password);
        mRememberPasswd = (CheckBox)findViewById(R.id.remember_passwd);
        mLoginMessage = (EditText)findViewById(R.id.login_message);

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isRememberPasswd = pref.getBoolean(REMEMBER_PASSWD, false);
        if(isRememberPasswd){
            String username = pref.getString(USERNAME, "");
            String password = pref.getString(PASSWORD, "");
            mUsername.setText(username);
            mPassword.setText(password);
            mRememberPasswd.setChecked(true);
        }
    }


    class LoginAsync extends AsyncTask<String, String, JSONObject>{
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
        }

        @Override
        protected JSONObject doInBackground(String... args) {
            try{
                HashMap<String, String> params = new HashMap<>();
                params.put("username", args[0]);
                params.put("passwd", args[1]);
                JSONObject json = mJSONParser.makeHttpRequest(url_register, "POST", params);

                if(json != null){
                    return json;
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject json){
            int success = 0;
            String message = "";

            if(json != null){

                try{
                    success = json.getInt(TAG_SUCCESS);
                    message = json.getString(TAG_MESSAGE);
                    mLoginMessage.setText(message);

                    if(success == 0){
                        editor = pref.edit();
                        if(mRememberPasswd.isChecked()){
                            editor.putBoolean(REMEMBER_PASSWD, true);
                        //    editor.putString(USERNAME, msgUsername);
                            editor.putString(PASSWORD, msgPassword);

                        }else{
                            editor.clear();

                        }
                        editor.putString(USERNAME, msgUsername);
                        editor.commit();
                        Intent i = new Intent(LoginActivity.this, AboutMeActivity.class);
                        i.putExtra("username", msgUsername );
                        i.putExtra("profileURL", json.getString("url"));
                        startActivity(i);
                    }

                }catch(JSONException e){
                    e.printStackTrace();
                }finally {
                    mLoginButton.setEnabled(true);
                }
            }
        }
    }
}
