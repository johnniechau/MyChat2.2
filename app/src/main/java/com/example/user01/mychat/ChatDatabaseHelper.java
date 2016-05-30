package com.example.user01.mychat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.cheng.chat.MyChatMessage;
import com.cheng.chat.MyChatTextMessage;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by anna on 2016/5/26.
 */
public class ChatDatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "chat_messages.sqlite";
    private static final int VERSION = 1;
    private static final String TABLE_NAME = "message";
    private static final String TAG = "DBHELPER";

    private static final String column_timestamp = "timestamp";
    private static final String column_message_from = "message_from";
    private static final String column_message_to = "message_to";
    private static final String column_message_type = "message_type";
    private static final String column_message_content = "message_content";

    private static final String sql_statement_create_table = "CREATE TABLE " + TABLE_NAME
            + " ( _id integer primary key autoincrement , timestamp integer, message_from text, " +
            " message_to text, message_type text, message_content text )";

    public ChatDatabaseHelper(Context context)
    {
        super(context, DB_NAME, null, VERSION );
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(sql_statement_create_table);
        Log.i(TAG, "helper creates the table");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public long insertChatMessage(MyChatMessage m)
    {
        ContentValues cv = new ContentValues();
        cv.put(column_timestamp, m.getCreateDate());
        cv.put(column_message_from, m.getFromUser());
        cv.put(column_message_to, m.getToUser());
        cv.put(column_message_type, m.getType());
        cv.put(column_message_content, m.getContent());

        Log.i(TAG, "insert a message to sqlite");
        return getWritableDatabase().insert(TABLE_NAME, null, cv);
    }

    public ArrayList<MyChatMessage> retrieveChatMessage(String fromUser, String toUser)
    {


        ArrayList<MyChatMessage> msglist = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();
        Cursor resultSet = db.rawQuery("select * from message where " +
                " (message_from = '" + fromUser + "' and message_to = '" + toUser + "') or " +
                " (message_from = '" + toUser + "' and message_to = '" + fromUser + "') " +
                " order by timestamp asc", null);


        Log.i("DBHELPER", "cursor: db has " + resultSet.getCount() + "messages!!");
        Log.i("DBHELPER", "cursor: db has " + resultSet.getColumnName(5) + "name column");

        resultSet.moveToFirst();

        while(resultSet.isAfterLast() == false){
            Log.i("DBHELPER", "cursor: db has message" + resultSet.getString(resultSet.getColumnIndex("message_content")));


            String from = resultSet.getString(resultSet.getColumnIndex("message_from"));
            String to = resultSet.getString(resultSet.getColumnIndex("message_to"));
            String content = resultSet.getString(resultSet.getColumnIndex("message_content"));
            long timestamp = resultSet.getLong(resultSet.getColumnIndex("timestamp"));

            String msgtype = resultSet.getString(resultSet.getColumnIndex("message_type"));

            if (msgtype.equals("text")) {
                Log.i("DBHELPER", "cursor: db has message got a text message" + content);
                msglist.add(new MyChatTextMessage(from, to, content, new Date(timestamp*1000)));
            }
            resultSet.moveToNext();
        }

        return msglist;
    }
}
