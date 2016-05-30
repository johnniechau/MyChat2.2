package com.example.user01.mychat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;


import com.cheng.chat.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


public class ChatActivity extends Activity {
    private Button btnSend;
    private EditText inputMsg;

    private MessagesListAdapter adapter;
    private List<Message> listMessages;
    private ListView listViewMessages;

    private String toUserName = null;
    private String fromUserName = null;
    private SharedPreferences pref;

    private static final String USERNAME = "username";
    private static final String TAG_SELF = "self", TAG_NEW = "new",
            TAG_MESSAGE = "message";


    private static final String TAG_ERRCODE = "errcode";
    private static final String TAG_ERRMSG = "errmsg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        btnSend = (Button)findViewById(R.id.btnSend);
        inputMsg = (EditText)findViewById(R.id.inputMsg);
        listViewMessages = (ListView)findViewById(R.id.list_view_messages);

        Intent i = getIntent();
        toUserName = i.getStringExtra("name");

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        fromUserName = pref.getString(USERNAME, "");

        Log.i("ChatActivity", fromUserName + toUserName);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String textMessageContent = inputMsg.getText().toString();
                Message msg = new Message("你", textMessageContent, true);
                appendMessage(msg);

                MyChatTextMessage m = new MyChatTextMessage(fromUserName, toUserName, textMessageContent, new Date());

                new SendMessageTask().execute(m);

                inputMsg.setText("");
            }
        });

        listMessages = new ArrayList<Message>();
        adapter = new MessagesListAdapter(this, listMessages);
        listViewMessages.setAdapter(adapter);


        new LoadChatHistoryTask(getApplicationContext()).execute( );
    }


    private class LoadChatHistoryTask extends AsyncTask<Void, Void, ArrayList<MyChatMessage>> {

        private Context ctx;
        private ChatDatabaseHelper helper;

        LoadChatHistoryTask(Context c) { ctx = c; }

        @Override
        protected ArrayList<MyChatMessage> doInBackground(Void ... param) {

            // must init the helper first :)
            helper = new ChatDatabaseHelper(ctx);
            Log.i("DBHELPER", "do in background--->ReceiveMessage from Server");

            try {
                ReceiveMessageFromServer();
            } catch (Exception ex)
            {
                Log.e("DBHELPER", "Failed to receive from Server");
            }

            Log.i("DBHELPER", "do in background--->RetrieveMessage");

            return helper.retrieveChatMessage(fromUserName, toUserName);
        }


        protected void ReceiveMessageFromServer() throws Exception
        {
            URL urlrecv = new URL("http://192.168.1.100/mychat/get_messages.php");
            HttpURLConnection con = (HttpURLConnection) urlrecv.openConnection();

            con.setRequestMethod("POST");
            con.setDoOutput(true);
            DataOutputStream payload = new DataOutputStream(con.getOutputStream());

            String postvar = "username=" + fromUserName;

            payload.write(postvar.getBytes("UTF-8"));
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "UTF-8"));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            System.out.println(response.toString());

            JSONObject json = new JSONObject(response.toString());  // this is a must
            System.out.println(json.getInt("errcode"));
            System.out.println(json.getString("errmsg"));

            if(json.getInt("errcode") == 0)
            {
                System.out.println(json.getString("xml"));
            }

            Log.i("DBHELPER", "errcode " + json.getInt("errcode") + "error msg " + json.getString("errmsg"));

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            ByteArrayInputStream bytein = new ByteArrayInputStream(json.getString("xml").getBytes("UTF-8"));
            InputSource is = new InputSource(bytein);
            Document doc = builder.parse(is);
            Element root = doc.getDocumentElement();
            NodeList children = root.getChildNodes();

            for( int i = 0; i < children.getLength(); i++)
            {
                Node child = children.item(i);
                if(child instanceof Element)
                {
                    Element msgElem = (Element)child;
                    MyChatTextMessage m = new MyChatTextMessage(msgElem);
                    if (m != null )
                        helper.insertChatMessage(m);
                }
            }
            in.close();
        }


        @Override
        protected void onPostExecute(ArrayList<MyChatMessage> msgArray) {
            for(MyChatMessage m: msgArray)
            {
                Message msg;
                if (m.getFromUser().equals(fromUserName))
                {
                    msg = new Message("你", m.getContent(), true);
                }
                else {
                    msg = new Message(m.getFromUser(), m.getContent(), false);
                }
                appendMessage(msg);
            }
        }

    }


    private class SendMessageTask extends AsyncTask<MyChatMessage, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(MyChatMessage ... message)
        {
            Log.i("DBHELPER" , "SendMessageTask");
            ChatDatabaseHelper helper = new ChatDatabaseHelper(getApplicationContext());
            if (message[0] != null )
                helper.insertChatMessage(message[0]);


            JSONObject jObj = null;
            try {
                URL obj = new URL("http://192.168.1.100/mychat/xml_post2.php");
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                con.setRequestMethod("POST");
                con.setDoInput(true);
                con.setDoOutput(true);
                con.setUseCaches(false);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());

                MyChatMessage m = message[0];


                wr.write(m.getXML().getBytes("UTF8"));
                wr.flush();
                wr.close();



                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), "UTF-8"));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                Log.d("JSON", response.toString());
                // try parse the string to a JSON object
                try {
                    jObj = new JSONObject(response.toString());
                } catch (JSONException e) {
                    Log.d("ChatActivity", "??????????????!!!!!?");
                    Log.e("JSON Parser", "Error parsing data " + e.toString());
                }
                con.disconnect();
            } catch ( IOException ioe)
            {
                Log.d("MyChat", "AsyncTask: IO Exception");
            }


            return jObj;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);

            int errcode = -1;
            String errmsg = null;

            try {
                errcode = jsonObject.getInt(TAG_ERRCODE);
                errmsg = jsonObject.getString(TAG_ERRMSG);

                } catch (JSONException e) {


                Log.d("ChatActivity", "???????????????");

                e.printStackTrace();
            }

            Log.d("ChatActivity", "Err code:" + errcode + "  " + errmsg);

        }


    }


    private String getRandomString(){
        String[] strArray = {"哈哈", "我是测试机器人", "还能好好玩耍吗？"};
        Random ran = new Random();
        return strArray[ran.nextInt(3)];

    }

    private void appendMessage(final Message m){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listMessages.add(m);
                adapter.notifyDataSetChanged();
                playBeep();
            }
        });
    }

    /**
     * Plays device's default notification sound
     * */
    public void playBeep() {

        try {
            Uri notification = RingtoneManager
                    .getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(),
                    notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
