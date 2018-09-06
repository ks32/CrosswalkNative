package ks32.crosswalk_native;

import android.app.Activity;
import android.os.Bundle;

import org.xwalk.core.XWalkActivity;
import org.xwalk.core.XWalkView;

public class MainActivity extends XWalkActivity {
    private XWalkView mXWalkView;

    @Override
    protected void onXWalkReady() {

        mXWalkView.load("http://whatismybrowser.com/", null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mXWalkView = (XWalkView) findViewById(R.id.xwalk_main);
        //mXWalkView.load("http://whatismybrowser.com/", null);

    }
}