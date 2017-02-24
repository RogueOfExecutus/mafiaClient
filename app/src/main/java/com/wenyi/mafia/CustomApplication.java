package com.wenyi.mafia;

import android.app.Application;
import com.tencent.bugly.crashreport.CrashReport;

/**
 * Created by Administrator on 2016/8/24.
 */
public class CustomApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CrashReport.initCrashReport(getApplicationContext(), "f9e2a96081", false);
    }
}
