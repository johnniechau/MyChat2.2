package com.example.user01.mychat;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

//import org.apache.http.NameValuePair;
//import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RegisterActivity extends Activity {
    private EditText mUsername;
    private EditText mPassword;
    private EditText mPassword2;

    private Button mRegMessage;

    private Button mRegisterButton;

    private String msgUsername;
    private String msgPassword;
    private String msgPassword2;
    private String msgMessage;

    private ProgressDialog mProgressDialog;
    JSONParser mJSONParser = new JSONParser();

    private static String url_register = "http://192.168.1.100/mychat/register_x.php";
    private static final String TAG_SUCCESS = "success";
    private static final String TAG_MESSAGE = "message";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mRegisterButton = (Button)findViewById(R.id.register_button);
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                msgUsername = mUsername.getText().toString();
                msgPassword = mPassword.getText().toString();
                msgPassword2 = mPassword2.getText().toString();

//                if(msgPassword.equals(msgPassword2) ) {
//                    msgMessage = "Trying to register with:" + msgUsername + " [ " + msgPassword + " | " + msgPassword2 + " ] ";
//                }else{
//                    msgMessage = "The passwords you entered do not match! You entered " + msgPassword + " and " + msgPassword2;
//                }
               // Toast.makeText(RegisterActivity.this, msgMessage, Toast.LENGTH_SHORT).show();
                new RegisterAsync().execute(msgUsername, msgPassword, msgPassword2);
            }
        });


        mUsername = (EditText)findViewById(R.id.register_username);
        mPassword = (EditText)findViewById(R.id.register_password);
        mPassword2 = (EditText)findViewById(R.id.register_password2);
        mRegMessage = (Button)findViewById(R.id.register_message);
        mRegMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(i);
            }
        });


    }

    class RegisterAsync extends AsyncTask<String, String, JSONObject>{

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(RegisterActivity.this);
            mProgressDialog.setMessage(msgMessage);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setCancelable(true);
            mProgressDialog.show();

        }


        protected JSONObject doInBackground(String... args) {
            try{
                HashMap<String, String> params = new HashMap<>();
                params.put("username", args[0]);
                params.put("passwd", args[1]);
                params.put("passwd2", args[2]);
                Log.d("Register", args[0] + ":" + args[1] + "__" + args[2]);
                JSONObject json = mJSONParser.makeHttpRequest(url_register, "POST", params);

                if(json != null){
                    Log.d("JSON result : ", json.toString());

                    return json;
                }
            }catch(Exception e){
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(JSONObject json){
        //    super.onPostExecute(json);
            int success = 0;
            String message = "";
            if(mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }

            if(json != null){

             //   Toast.makeText(RegisterActivity.this, json.toString(), Toast.LENGTH_LONG);
                try{
                    success = json.getInt(TAG_SUCCESS);
                    message = json.getString(TAG_MESSAGE);
                    mRegMessage.setText(message);

                }catch(JSONException e){
                    e.printStackTrace();
                }
            }

        }
    }

}
