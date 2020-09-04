package ks32.crosswalk_native;

import android.util.Log;
import android.view.KeyEvent;

import com.pakdata.xwalk.refactor.XWalkUIClient;
import com.pakdata.xwalk.refactor.XWalkView;

public class uiClient extends XWalkUIClient {

    public uiClient(XWalkView view) {
        super(view);
    }

    @Override
    public void onScaleChanged(XWalkView view, float oldScale, float newScale) {
        // Log.d("qqq", "Scale changed. old:"+oldScale+" New: "+newScale);
        //QuranViewFragment.qmView.evaluateJavascript("recalculateWidth(100);", null);
        super.onScaleChanged(view, oldScale, newScale);


    }


    /*
    @Override
    public boolean shouldOverrideKeyEvent(XWalkView view, KeyEvent event) {
        // TODO Auto-generated method stub
        view.clearCache(true);
        view.getNavigationHistory().clear();

        //getActivity().finish();
        view.stopLoading();
        return true;
    } */
    @Override
    public boolean shouldOverrideKeyEvent(XWalkView view, android.view.KeyEvent event){
        int keycode=event.getKeyCode();
        return keycode == KeyEvent.KEYCODE_BACK;
    }
    @Override
    public boolean onConsoleMessage(XWalkView view, String message, int lineNumber, String sourceId, ConsoleMessageType messageType) {
            Log.i("qqq-xwalk", sourceId+" "+lineNumber+": "+message);
            return super.onConsoleMessage(view, message, lineNumber, sourceId, messageType);
    }

}
