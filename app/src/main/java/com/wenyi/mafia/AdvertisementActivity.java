package com.wenyi.mafia;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;

import com.wenyi.mafia.myservice.InternetService;

import java.lang.ref.WeakReference;

public class AdvertisementActivity extends Activity {
    private static final int START_APP = 1;
    private MyHandler handler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advertisement);
        startService(new Intent(this,InternetService.class));
        handler = new MyHandler(this);
        handler.sendEmptyMessageDelayed(START_APP,3000);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK)
            handler.removeMessages(START_APP);
        return super.onKeyDown(keyCode, event);
    }

    static class MyHandler extends Handler {
        WeakReference<Activity> weakReference;
        public MyHandler(Activity activity){
            this.weakReference = new WeakReference<>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            Activity activity = this.weakReference.get();
            super.handleMessage(msg);
            if(activity != null){
                switch (msg.what) {
                    case START_APP:
                        activity.startActivity(new Intent(activity,LoginActivity.class));
                        activity.finish();
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
