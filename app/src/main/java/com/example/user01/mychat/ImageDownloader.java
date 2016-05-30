package com.example.user01.mychat;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Johnnie on 2016/5/17/0017.
 */
public class ImageDownloader<Token> extends HandlerThread {
    private static final int MESSAGE_DOWNLOAD = 0;
    Handler mHandler;
    Map<Token, String> requestMap = Collections.synchronizedMap(new HashMap<Token, String>());
    Handler mResponseHandler;
    Listener<Token> mListener;

    public interface Listener<Token>{
        void onImageDownloaded(Token token, Bitmap bitmap);
    }

    public void setListener(Listener<Token> listener){
        mListener = listener;
    }

    public ImageDownloader(Handler responseHandler, String name) {
        super(name);
        mResponseHandler = responseHandler;
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared(){
        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                if(msg.what == MESSAGE_DOWNLOAD){
                    @SuppressWarnings("unchecked")
                    Token token = (Token)msg.obj;
                    handleRequest(token);
                }
            }
        };
    }

    public void queueImage(Token token, String url){
        Log.i("download", "got an URL: " + url);
        requestMap.put(token, url);
        mHandler.obtainMessage(MESSAGE_DOWNLOAD, token).sendToTarget();
    }

    private void handleRequest(final Token token){
        try{
            final String url = requestMap.get(token);
            if(url == null)
                return;

            URL imageUrl = new URL(url);
//            BitmapFactory.Options options = new BitmapFactory.Options();
//            options.inPreferredConfig = Bitmap.Config.RGB_565;
            final Bitmap image  = BitmapFactory.decodeStream(imageUrl.openConnection().getInputStream());
            Log.i("myChat", "Bitmap created");

            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(requestMap.get(token) != url)
                        return;

                    requestMap.remove(token);
                    mListener.onImageDownloaded(token, image);
                }
            });

        }catch(MalformedURLException e){
            e.printStackTrace();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void clearQueue(){
        mHandler.removeMessages(MESSAGE_DOWNLOAD);
        requestMap.clear();
    }
}