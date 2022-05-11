package ks32.crosswalk_native;

import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.pakdata.xwalk.refactor.XWalkClient;
import com.pakdata.xwalk.refactor.XWalkPreferences;
import com.pakdata.xwalk.refactor.XWalkSettings;
import com.pakdata.xwalk.refactor.XWalkUIClient;
import com.pakdata.xwalk.refactor.XWalkView;

import org.chromium.base.ApplicationStatus;
import org.xwalk.core.XWalkInitializer;


public class MainActivity extends AppCompatActivity implements XWalkInitializer.XWalkInitListener {
    private XWalkView mXWalkView;
    private XWalkInitializer mXWalkInitializer;
    private XWalkSettings mSettings;
    FrameLayout parent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mXWalkInitializer = new XWalkInitializer(this, this);
        mXWalkInitializer.initAsync();
        Log.i("qqq","ANIMATABLE_XWALK_VIEW: "+ XWalkPreferences.getBooleanValue(XWalkPreferences.ANIMATABLE_XWALK_VIEW));
    }

    private void init(){

        mXWalkView.setUIClient(new uiClient(mXWalkView));
        mXWalkView.setXWalkClient(new XWalkClientClass(mXWalkView));
        mXWalkView.getSettings().setAppCacheEnabled(true);
    }



    @Override
    public void onBackPressed() {
        parent = (FrameLayout) findViewById(R.id.parentMain);
        parent.removeAllViews();
        super.onBackPressed();
    }

    @Override
    public void onXWalkInitStarted() {
        Log.i("qqq","onXWalkInitStarted");

    }

    @Override
    public void onXWalkInitCancelled() {
        Log.i("qqq","onXWalkInitCancelled");
    }

    @Override
    public void onXWalkInitFailed() {
        Log.i("qqq","onXWalkInitFailed");

    }

    @Override
    public void onXWalkInitCompleted() {
        Log.i("qqq","onXWalkInitCompleted");
        mXWalkView = new XWalkView(this);
            init();

        mXWalkView.clearCache(true);
        parent = (FrameLayout) findViewById(R.id.parentMain);
        parent.addView(mXWalkView);
        mXWalkView.load("https://whatismybrowser.com/","");
        Log.i("qqq","version:"+mXWalkView.getSettings().getUserAgentString());
       // mXWalkView.clearCache(true);
    }

}