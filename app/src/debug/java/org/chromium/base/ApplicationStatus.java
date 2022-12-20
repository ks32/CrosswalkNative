//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.chromium.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.Bundle;
import android.view.Window.Callback;
import androidx.annotation.AnyThread;
import androidx.annotation.GuardedBy;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;

@JNINamespace("base::android")
public class ApplicationStatus {
    private static final String TOOLBAR_CALLBACK_INTERNAL_WRAPPER_CLASS = "androidx.appcompat.internal.app.ToolbarActionBar$ToolbarCallbackWrapper";
    private static final String TOOLBAR_CALLBACK_WRAPPER_CLASS = "androidx.appcompat.app.ToolbarActionBar$ToolbarCallbackWrapper";
    private static final String WINDOW_PROFILER_CALLBACK = "com.android.tools.profiler.support.event.WindowProfilerCallback";
    private static final Map<Activity, ApplicationStatus.ActivityInfo> sActivityInfo = Collections.synchronizedMap(new HashMap());
    @SuppressLint({"SupportAnnotationUsage"})
    @GuardedBy("sActivityInfo")
    private static int sCurrentApplicationState = 0;
    @SuppressLint({"StaticFieldLeak"})
    private static Activity sActivity;
    private static ApplicationStatus.ApplicationStateListener sNativeApplicationStateListener;
    private static final ObserverList<ApplicationStatus.ActivityStateListener> sGeneralActivityStateListeners = new ObserverList();
    private static final ObserverList<ApplicationStatus.ApplicationStateListener> sApplicationStateListeners = new ObserverList();
    private static final ObserverList<ApplicationStatus.WindowFocusChangedListener> sWindowFocusListeners = new ObserverList();

    private ApplicationStatus() {
    }

    @MainThread
    public static void registerWindowFocusChangedListener(ApplicationStatus.WindowFocusChangedListener listener) {
        //assert isInitialized();

        sWindowFocusListeners.addObserver(listener);
    }

    @MainThread
    public static void unregisterWindowFocusChangedListener(ApplicationStatus.WindowFocusChangedListener listener) {
        sWindowFocusListeners.removeObserver(listener);
    }

    public static boolean isInitialized() {
        synchronized(sActivityInfo) {
            return sCurrentApplicationState != 0;
        }
    }

    @MainThread
    public static void initialize(Application application) {
        //assert !isInitialized();

        synchronized(sActivityInfo) {
            sCurrentApplicationState = 4;
        }

        registerWindowFocusChangedListener(new ApplicationStatus.WindowFocusChangedListener() {
            public void onWindowFocusChanged(Activity activity, boolean hasFocus) {
                if (hasFocus && activity != ApplicationStatus.sActivity) {
                    int state = ApplicationStatus.getStateForActivity(activity);
                    if (state != 6 && state != 5) {
                        ApplicationStatus.sActivity = activity;
                    }

                }
            }
        });
        application.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                ApplicationStatus.onStateChange(activity, 1);
                Callback callback = activity.getWindow().getCallback();
                activity.getWindow().setCallback((Callback)Proxy.newProxyInstance(Callback.class.getClassLoader(), new Class[]{Callback.class}, new ApplicationStatus.WindowCallbackProxy(activity, callback)));
            }

            public void onActivityDestroyed(Activity activity) {
                ApplicationStatus.onStateChange(activity, 6);
                this.checkCallback(activity);
            }

            public void onActivityPaused(Activity activity) {
                ApplicationStatus.onStateChange(activity, 4);
                this.checkCallback(activity);
            }

            public void onActivityResumed(Activity activity) {
                ApplicationStatus.onStateChange(activity, 3);
                this.checkCallback(activity);
            }

            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                this.checkCallback(activity);
            }

            public void onActivityStarted(Activity activity) {
                ApplicationStatus.onStateChange(activity, 2);
                this.checkCallback(activity);
            }

            public void onActivityStopped(Activity activity) {
                ApplicationStatus.onStateChange(activity, 5);
                this.checkCallback(activity);
            }

            private void checkCallback(Activity activity) {
                if (BuildConfig.DCHECK_IS_ON) {
                    Class<? extends Callback> callback = activity.getWindow().getCallback().getClass();

                    //assert Proxy.isProxyClass(callback) || callback.getName().equals("androidx.appcompat.app.ToolbarActionBar$ToolbarCallbackWrapper") || callback.getName().equals("androidx.appcompat.internal.app.ToolbarActionBar$ToolbarCallbackWrapper") || callback.getName().equals("com.android.tools.profiler.support.event.WindowProfilerCallback");
                }

            }
        });
    }

    private static void onStateChange(Activity activity, int newState) {
        if (activity == null) {
            throw new IllegalArgumentException("null activity is not supported");
        } else {
            if (sActivity == null || newState == 1 || newState == 3 || newState == 2) {
                sActivity = activity;
            }

            int oldApplicationState = getStateForApplication();
            ApplicationStatus.ActivityInfo info;
            synchronized(sActivityInfo) {
                if (newState == 1) {
                   // assert !sActivityInfo.containsKey(activity);

                    sActivityInfo.put(activity, new ApplicationStatus.ActivityInfo());
                }

                info = (ApplicationStatus.ActivityInfo)sActivityInfo.get(activity);
                if (info != null) {
                    info.setStatus(newState);
                }

                if (newState == 6) {
                    sActivityInfo.remove(activity);
                    if (activity == sActivity) {
                        sActivity = null;
                    }
                }

                sCurrentApplicationState = determineApplicationStateLocked();
            }

            if (info != null) {
                Iterator var4 = info.getListeners().iterator();

                ApplicationStatus.ActivityStateListener listener;
                while(var4.hasNext()) {
                    listener = (ApplicationStatus.ActivityStateListener)var4.next();
                    listener.onActivityStateChange(activity, newState);
                }

                var4 = sGeneralActivityStateListeners.iterator();

                while(var4.hasNext()) {
                    listener = (ApplicationStatus.ActivityStateListener)var4.next();
                    listener.onActivityStateChange(activity, newState);
                }

                int applicationState = getStateForApplication();
                if (applicationState != oldApplicationState) {
                    Iterator var9 = sApplicationStateListeners.iterator();

                    while(var9.hasNext()) {
                        ApplicationStatus.ApplicationStateListener listener1 = (ApplicationStatus.ApplicationStateListener)var9.next();
                        listener1.onApplicationStateChange(applicationState);
                    }
                }

            }
        }
    }

    @VisibleForTesting
    @MainThread
    public static void onStateChangeForTesting(Activity activity, int newState) {
        onStateChange(activity, newState);
    }

    @MainThread
    public static Activity getLastTrackedFocusedActivity() {
        return sActivity;
    }

    @AnyThread
    public static List<Activity> getRunningActivities() {
        //assert isInitialized();

        synchronized(sActivityInfo) {
            return new ArrayList(sActivityInfo.keySet());
        }
    }

    @AnyThread
    public static int getStateForActivity(@Nullable Activity activity) {
        //assert isInitialized();

        if (activity == null) {
            return 6;
        } else {
            ApplicationStatus.ActivityInfo info = (ApplicationStatus.ActivityInfo)sActivityInfo.get(activity);
            return info != null ? info.getStatus() : 6;
        }
    }

    @AnyThread
    @CalledByNative
    public static int getStateForApplication() {
        synchronized(sActivityInfo) {
            return sCurrentApplicationState;
        }
    }

    @AnyThread
    public static boolean hasVisibleActivities() {
        //assert isInitialized();

        int state = getStateForApplication();
        return state == 1 || state == 2;
    }

    @AnyThread
    public static boolean isEveryActivityDestroyed() {
        //assert isInitialized();

        return sActivityInfo.isEmpty();
    }

    @MainThread
    public static void registerStateListenerForAllActivities(ApplicationStatus.ActivityStateListener listener) {
        //assert isInitialized();

        sGeneralActivityStateListeners.addObserver(listener);
    }

    @MainThread
    @SuppressLint({"NewApi"})
    public static void registerStateListenerForActivity(ApplicationStatus.ActivityStateListener listener, Activity activity) {
        //assert isInitialized();

        //assert activity != null;

        ApplicationStatus.ActivityInfo info = (ApplicationStatus.ActivityInfo)sActivityInfo.get(activity);

        //assert info.getStatus() != 6;

        if (info != null) {
            info.getListeners().addObserver(listener);
        }

    }

    @MainThread
    public static void unregisterActivityStateListener(ApplicationStatus.ActivityStateListener listener) {
        sGeneralActivityStateListeners.removeObserver(listener);
        synchronized(sActivityInfo) {
            Iterator var2 = sActivityInfo.values().iterator();

            while(var2.hasNext()) {
                ApplicationStatus.ActivityInfo info = (ApplicationStatus.ActivityInfo)var2.next();
                info.getListeners().removeObserver(listener);
            }

        }
    }

    @MainThread
    public static void registerApplicationStateListener(ApplicationStatus.ApplicationStateListener listener) {
        sApplicationStateListeners.addObserver(listener);
    }

    @MainThread
    public static void unregisterApplicationStateListener(ApplicationStatus.ApplicationStateListener listener) {
        sApplicationStateListeners.removeObserver(listener);
    }

    @MainThread
    public static void destroyForJUnitTests() {
        synchronized(sActivityInfo) {
            sApplicationStateListeners.clear();
            sGeneralActivityStateListeners.clear();
            sActivityInfo.clear();
            sWindowFocusListeners.clear();
            sCurrentApplicationState = 0;
            sActivity = null;
            sNativeApplicationStateListener = null;
        }
    }

    @CalledByNative
    private static void registerThreadSafeNativeApplicationStateListener() {
        ThreadUtils.runOnUiThread(new Runnable() {
            public void run() {
                if (ApplicationStatus.sNativeApplicationStateListener == null) {
                    ApplicationStatus.sNativeApplicationStateListener = new ApplicationStatus.ApplicationStateListener() {
                        public void onApplicationStateChange(int newState) {
                            ApplicationStatus.nativeOnApplicationStateChange(newState);
                        }
                    };
                    ApplicationStatus.registerApplicationStateListener(ApplicationStatus.sNativeApplicationStateListener);
                }
            }
        });
    }

    @GuardedBy("sActivityInfo")
    private static int determineApplicationStateLocked() {
        boolean hasPausedActivity = false;
        boolean hasStoppedActivity = false;
        Iterator var2 = sActivityInfo.values().iterator();

        while(var2.hasNext()) {
            ApplicationStatus.ActivityInfo info = (ApplicationStatus.ActivityInfo)var2.next();
            int state = info.getStatus();
            if (state != 4 && state != 5 && state != 6) {
                return 1;
            }

            if (state == 4) {
                hasPausedActivity = true;
            } else if (state == 5) {
                hasStoppedActivity = true;
            }
        }

        if (hasPausedActivity) {
            return 2;
        } else if (hasStoppedActivity) {
            return 3;
        } else {
            return 4;
        }
    }

    private static native void nativeOnApplicationStateChange(int var0);

    private static class WindowCallbackProxy implements InvocationHandler {
        private final Callback mCallback;
        private final Activity mActivity;

        public WindowCallbackProxy(Activity activity, Callback callback) {
            this.mCallback = callback;
            this.mActivity = activity;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("onWindowFocusChanged") && args.length == 1 && args[0] instanceof Boolean) {
                this.onWindowFocusChanged((Boolean)args[0]);
                return null;
            } else {
                try {
                    return method.invoke(this.mCallback, args);
                } catch (InvocationTargetException var5) {
                    if (var5.getCause() instanceof AbstractMethodError) {
                        throw var5.getCause();
                    } else {
                        throw var5;
                    }
                }
            }
        }

        public void onWindowFocusChanged(boolean hasFocus) {
            this.mCallback.onWindowFocusChanged(hasFocus);
            Iterator var2 = ApplicationStatus.sWindowFocusListeners.iterator();

            while(var2.hasNext()) {
                ApplicationStatus.WindowFocusChangedListener listener = (ApplicationStatus.WindowFocusChangedListener)var2.next();
                listener.onWindowFocusChanged(this.mActivity, hasFocus);
            }

        }
    }

    public interface WindowFocusChangedListener {
        void onWindowFocusChanged(Activity var1, boolean var2);
    }

    public interface ActivityStateListener {
        void onActivityStateChange(Activity var1, int var2);
    }

    public interface ApplicationStateListener {
        void onApplicationStateChange(int var1);
    }

    private static class ActivityInfo {
        private int mStatus;
        private ObserverList<ApplicationStatus.ActivityStateListener> mListeners;

        private ActivityInfo() {
            this.mStatus = 6;
            this.mListeners = new ObserverList();
        }

        public int getStatus() {
            return this.mStatus;
        }

        public void setStatus(int status) {
            this.mStatus = status;
        }

        public ObserverList<ApplicationStatus.ActivityStateListener> getListeners() {
            return this.mListeners;
        }
    }
}
