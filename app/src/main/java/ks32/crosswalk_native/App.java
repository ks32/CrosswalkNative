package ks32.crosswalk_native;

import android.content.Context;

import org.chromium.base.ApplicationStatus;
import org.xwalk.core.XWalkApplication;

public class App extends XWalkApplication {
    private static Context mContext;

    @Override
    public void onCreate() {

        super.onCreate();
        try {
            mContext = getApplicationContext();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        ApplicationStatus.initialize(this);


    }

    public static Context getContext(){
        return mContext;
    }
}