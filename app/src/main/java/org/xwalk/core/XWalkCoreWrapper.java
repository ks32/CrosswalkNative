//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.xwalk.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build.VERSION;
import android.util.Log;
import com.pakdata.xwalk.refactor.ReflectMethod;
import com.pakdata.xwalk.refactor.XWalkCoreBridge;
import com.pakdata.xwalk.refactor.XWalkViewDelegate;
import dalvik.system.DexClassLoader;
import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

@SuppressLint({"StaticFieldLeak"})
class XWalkCoreWrapper {
    private static final String WRAPPER_PACKAGE = "org.xwalk.core";
    private static final String BRIDGE_PACKAGE = "org.xwalk.core.internal";
    private static final String TAG = "XWalkCoreWrapper";
    private static final String XWALK_CORE_CLASSES_DEX = "classes.dex";
    private static XWalkCoreWrapper sProvisionalInstance;
    private static XWalkCoreWrapper sInstance;
    private static LinkedList<String> sReservedActivities = new LinkedList();
    private static HashMap<String, LinkedList<XWalkCoreWrapper.ReservedAction>> sReservedActions = new HashMap();
    private int mApiVersion = 8;
    private int mMinApiVersion;
    private int mCoreStatus;
    private Context mWrapperContext;
    private Context mBridgeContext;
    private ClassLoader mBridgeLoader;

    public static XWalkCoreWrapper getInstance() {
        return sInstance;
    }

    public static int getCoreStatus() {
        if (sInstance != null) {
            return sInstance.mCoreStatus;
        } else {
            return sProvisionalInstance == null ? 0 : sProvisionalInstance.mCoreStatus;
        }
    }

    public static void handlePreInit(String tag) {
        if (sInstance == null) {
            Log.d("XWalkCoreWrapper", "Pre init xwalk core in " + tag);
            if (sReservedActions.containsKey(tag)) {
                sReservedActions.remove(tag);
            } else {
                sReservedActivities.add(tag);
            }

            sReservedActions.put(tag, new LinkedList());
        }
    }

    public static void reserveReflectObject(Object object) {
        String tag = (String)sReservedActivities.getLast();
        Log.d("XWalkCoreWrapper", "Reserve object " + object.getClass() + " to " + tag);
        ((LinkedList)sReservedActions.get(tag)).add(new XWalkCoreWrapper.ReservedAction(object));
    }

    public static void reserveReflectClass(Class<?> clazz) {
        String tag = (String)sReservedActivities.getLast();
        Log.d("XWalkCoreWrapper", "Reserve class " + clazz.toString() + " to " + tag);
        ((LinkedList)sReservedActions.get(tag)).add(new XWalkCoreWrapper.ReservedAction(clazz));
    }

    public static void reserveReflectMethod(ReflectMethod method) {
        String tag = (String)sReservedActivities.getLast();
        Log.d("XWalkCoreWrapper", "Reserve method " + method.toString() + " to " + tag);
        ((LinkedList)sReservedActions.get(tag)).add(new XWalkCoreWrapper.ReservedAction(method));
    }

    public static void handlePostInit(String tag) {
        Log.d("XWalkCoreWrapper", "Post init xwalk core in " + tag);
        if (sReservedActions.containsKey(tag)) {
            LinkedList<XWalkCoreWrapper.ReservedAction> reservedActions = (LinkedList)sReservedActions.get(tag);
            Iterator var2 = reservedActions.iterator();

            while(true) {
                while(var2.hasNext()) {
                    XWalkCoreWrapper.ReservedAction action = (XWalkCoreWrapper.ReservedAction)var2.next();
                    if (action.mObject != null) {
                        Log.d("XWalkCoreWrapper", "Init reserved object: " + action.mObject.getClass().getCanonicalName());
                        (new ReflectMethod(action.mObject, "reflectionInit", new Class[0])).invoke(new Object[0]);
                    } else if (action.mClass != null) {
                        Log.d("XWalkCoreWrapper", "Init reserved class: " + action.mClass.toString());
                        (new ReflectMethod(action.mClass, "reflectionInit", new Class[0])).invoke(new Object[0]);
                    } else {
                        Log.d("XWalkCoreWrapper", "Call reserved method: " + action.mMethod.toString());
                        Object[] args = action.mArguments;
                        if (args != null) {
                            for(int i = 0; i < args.length; ++i) {
                                if (args[i] instanceof ReflectMethod) {
                                    args[i] = ((ReflectMethod)args[i]).invokeWithArguments();
                                }
                            }
                        }

                        action.mMethod.invoke(args);
                    }
                }

                sReservedActions.remove(tag);
                sReservedActivities.remove(tag);
                return;
            }
        }
    }

    public static void handleRuntimeError(RuntimeException e) {
        Log.e("XWalkCoreWrapper", "This API is incompatible with the Crosswalk runtime library");
        e.printStackTrace();
    }

    public static int attachXWalkCore() {
        //assert sReservedActivities.isEmpty();

        //assert sInstance != null;

        Log.d("XWalkCoreWrapper", "Attach xwalk core");
        sProvisionalInstance = new XWalkCoreWrapper(XWalkEnvironment.getApplicationContext(), 1);
        if (sProvisionalInstance.findEmbeddedCore()) {
            return sProvisionalInstance.mCoreStatus;
        } else if (XWalkEnvironment.isDownloadMode()) {
            sProvisionalInstance.findDownloadedCore();
            return sProvisionalInstance.mCoreStatus;
        } else {
            Log.d("XWalkCoreWrapper", "Not verifying the package integrity of Crosswalk runtime library");
            if (XWalkEnvironment.is64bitDevice()) {
                if (!sProvisionalInstance.findSharedCore("org.xwalk.core") && !sProvisionalInstance.findSharedCore("org.xwalk.core64") && XWalkEnvironment.isIaDevice()) {
                    sProvisionalInstance.findSharedCore("org.xwalk.core64.ia");
                }
            } else if (!sProvisionalInstance.findSharedCore("org.xwalk.core") && XWalkEnvironment.isIaDevice()) {
                sProvisionalInstance.findSharedCore("org.xwalk.core.ia");
            }

            return sProvisionalInstance.mCoreStatus;
        }
    }

    public static void dockXWalkCore() {
        //assert sProvisionalInstance == null;

        //assert sInstance != null;

        Log.d("XWalkCoreWrapper", "Dock xwalk core");
        sInstance = sProvisionalInstance;
        sProvisionalInstance = null;
        sInstance.initCoreBridge();
        sInstance.initXWalkView();
    }

    public static void initEmbeddedMode() {
        if (sInstance == null && sReservedActivities.isEmpty()) {
            Log.d("XWalkCoreWrapper", "Init embedded mode");
            XWalkCoreWrapper provisionalInstance = new XWalkCoreWrapper((Context)null, -1);
            if (!provisionalInstance.findEmbeddedCore()) {
                throw new RuntimeException("Please have your activity extend XWalkActivity for shared mode");
            } else {
                sInstance = provisionalInstance;
                sInstance.initCoreBridge();
            }
        }
    }

    private XWalkCoreWrapper(Context context, int minApiVersion) {
        this.mMinApiVersion = minApiVersion > 0 && minApiVersion <= this.mApiVersion ? minApiVersion : this.mApiVersion;
        this.mCoreStatus = 0;
        this.mWrapperContext = context;
    }

    private void initCoreBridge() {
        Log.d("XWalkCoreWrapper", "Init core bridge TODO");
        XWalkCoreBridge.init(this.mBridgeContext, this);
    }

    private void initXWalkView() {
        Log.d("XWalkCoreWrapper", "Init xwalk view TODO");
        XWalkViewDelegate.init(this.mBridgeContext, this.mWrapperContext);
    }

    private boolean findEmbeddedCore() {
        this.mBridgeContext = null;
        this.mBridgeLoader = XWalkCoreWrapper.class.getClassLoader();
        if (this.checkCoreVersion() && this.checkCoreArchitecture()) {
            Log.d("XWalkCoreWrapper", "Running in embedded mode");
            this.mCoreStatus = 1;
            return true;
        } else {
            this.mBridgeLoader = null;
            return false;
        }
    }

    private boolean findSharedCore(String packageName) {
        if (!this.checkCorePackage(packageName)) {
            return false;
        } else {
            this.mBridgeLoader = this.mBridgeContext.getClassLoader();
            if (this.checkCoreVersion() && this.checkCoreArchitecture()) {
                Log.d("XWalkCoreWrapper", "Running in shared mode");
                this.mCoreStatus = 1;
                return true;
            } else {
                this.mBridgeContext = null;
                this.mBridgeLoader = null;
                return false;
            }
        }
    }

    private boolean findDownloadedCore() {
        String libDir = XWalkEnvironment.getExtractedCoreDir();
        String dexPath = libDir + File.separator + "classes.dex";
        String dexOutputPath = XWalkEnvironment.getOptimizedDexDir();
        ClassLoader localClassLoader = ClassLoader.getSystemClassLoader();
        this.mBridgeLoader = new DexClassLoader(dexPath, dexOutputPath, libDir, localClassLoader);
        if (this.checkCoreVersion() && this.checkCoreArchitecture()) {
            Log.d("XWalkCoreWrapper", "Running in downloaded mode");
            this.mCoreStatus = 1;
            return true;
        } else {
            this.mBridgeLoader = null;
            return false;
        }
    }

    private boolean checkCoreVersion() {
        Log.d("XWalkCoreWrapper", "[Environment] SDK:" + VERSION.SDK_INT);
        Log.d("XWalkCoreWrapper", "[App Version] build:47.2.1.1, api:" + this.mApiVersion + ", min_api:" + this.mMinApiVersion);
        Log.d("XWalkCoreWrapper", "XWalk core version matched");
        return true;
    }

    private boolean checkCoreArchitecture() {
        try {
            boolean architectureMatched = false;
            String libDir = null;
            if (this.mBridgeContext != null) {
                if (VERSION.SDK_INT < 17) {
                    libDir = this.mBridgeContext.getApplicationInfo().dataDir + "/lib";
                }

                architectureMatched = XWalkViewDelegate.loadXWalkLibrary(this.mBridgeContext, libDir);
            } else {
                try {
                    architectureMatched = XWalkViewDelegate.loadXWalkLibrary(this.mBridgeContext, libDir);
                } catch (RuntimeException var4) {
                    Log.d("XWalkCoreWrapper", var4.getLocalizedMessage());
                }

                if (!architectureMatched && this.mWrapperContext != null) {
                    libDir = XWalkEnvironment.getPrivateDataDir();
                    architectureMatched = XWalkViewDelegate.loadXWalkLibrary(this.mBridgeContext, libDir);
                }
            }

            if (!architectureMatched) {
                Log.d("XWalkCoreWrapper", "Mismatch of CPU architecture");
                this.mCoreStatus = 6;
                return false;
            }
        } catch (RuntimeException var5) {
            Log.d("XWalkCoreWrapper", var5.getLocalizedMessage());
            if (var5.getCause() instanceof UnsatisfiedLinkError) {
                this.mCoreStatus = 6;
                return false;
            }

            this.mCoreStatus = 5;
            return false;
        }

        Log.d("XWalkCoreWrapper", "XWalk core architecture matched");
        return true;
    }

    @SuppressLint({"PackageManagerGetSignatures"})
    private boolean checkCorePackage(String packageName) {
        try {
            this.mBridgeContext = this.mWrapperContext.createPackageContext(packageName, 3);
        } catch (NameNotFoundException var3) {
            Log.d("XWalkCoreWrapper", packageName + " not found");
            return false;
        }

        Log.d("XWalkCoreWrapper", "Created package context for " + packageName);
        return true;
    }

    private boolean verifyPackageInfo(PackageInfo packageInfo, String hashAlgorithm, String hashCode) {
        if (packageInfo.signatures == null) {
            Log.e("XWalkCoreWrapper", "No signature in package info");
            return false;
        } else {
            MessageDigest md = null;

            try {
                md = MessageDigest.getInstance(hashAlgorithm);
            } catch (NullPointerException | NoSuchAlgorithmException var9) {
                throw new IllegalArgumentException("Invalid hash algorithm");
            }

            byte[] hashArray = this.hexStringToByteArray(hashCode);
            if (hashArray == null) {
                throw new IllegalArgumentException("Invalid hash code");
            } else {
                for(int i = 0; i < packageInfo.signatures.length; ++i) {
                    Log.d("XWalkCoreWrapper", "Checking signature " + i);
                    byte[] binaryCert = packageInfo.signatures[i].toByteArray();
                    byte[] digest = md.digest(binaryCert);
                    if (MessageDigest.isEqual(digest, hashArray)) {
                        Log.d("XWalkCoreWrapper", "Signature passed verification");
                        return true;
                    }

                    Log.e("XWalkCoreWrapper", "Hash code does not match");
                }

                return false;
            }
        }
    }

    private byte[] hexStringToByteArray(String str) {
        if (str != null && !str.isEmpty() && str.length() % 2 == 0) {
            byte[] result = new byte[str.length() / 2];

            for(int i = 0; i < str.length(); i += 2) {
                int digit = Character.digit(str.charAt(i), 16);
                digit <<= 4;
                digit += Character.digit(str.charAt(i + 1), 16);
                result[i / 2] = (byte)digit;
            }

            return result;
        } else {
            return null;
        }
    }

    public boolean isSharedMode() {
        return this.mBridgeContext != null;
    }

    public Object getBridgeObject(Object object) {
        try {
            return (new ReflectMethod(object, "getBridge", new Class[0])).invoke(new Object[0]);
        } catch (RuntimeException var3) {
            return null;
        }
    }

    public Object getWrapperObject(Object object) {
        try {
            return (new ReflectMethod(object, "getWrapper", new Class[0])).invoke(new Object[0]);
        } catch (RuntimeException var3) {
            return null;
        }
    }

    public Class<?> getBridgeClass(String name) {
        try {
            return this.mBridgeLoader.loadClass("org.xwalk.core.internal." + name);
        } catch (ClassNotFoundException var3) {
            return null;
        }
    }

    private static class ReservedAction {
        Object mObject;
        Class<?> mClass;
        ReflectMethod mMethod;
        Object[] mArguments;

        ReservedAction(Object object) {
            this.mObject = object;
        }

        ReservedAction(Class<?> clazz) {
            this.mClass = clazz;
        }

        ReservedAction(ReflectMethod method) {
            this.mMethod = method;
            if (method.getArguments() != null) {
                this.mArguments = Arrays.copyOf(method.getArguments(), method.getArguments().length);
            }

        }
    }
}
