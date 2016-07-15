/*
 * Copyright (C) 2013 Peter Gregus for GravityBox Project (C3C076@xda)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.myxposedmod;

import android.content.ActivityNotFoundException;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.widget.Toast;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XCallback;

public class ModHwKeys {
	
	
    private static final String TAG = "GB:ModHwKeys";
    private static final String CLASS_PHONE_WINDOW_MANAGER = "com.android.internal.policy.impl.PhoneWindowManager";
    private static final String CLASS_ACTIVITY_MANAGER_NATIVE = "android.app.ActivityManagerNative";
    private static final String CLASS_WINDOW_STATE = "android.view.WindowManagerPolicy$WindowState";
    private static final String CLASS_WINDOW_MANAGER_FUNCS = "android.view.WindowManagerPolicy.WindowManagerFuncs";
    private static final String CLASS_IWINDOW_MANAGER = "android.view.IWindowManager";
    private static final String CLASS_LOCAL_POWER_MANAGER = "android.os.LocalPowerManager";
    
    
    private static final int FLAG_WAKE = 0x00000001;
    private static final int FLAG_WAKE_DROPPED = 0x00000002;
    public static final String ACTION_SCREENSHOT = "gravitybox.intent.action.SCREENSHOT";
    public static final String ACTION_SHOW_POWER_MENU = "gravitybox.intent.action.SHOW_POWER_MENU";

    private static final String SEPARATOR = "#C3C0#";

    private static Class<?> classActivityManagerNative;
    private static Object mPhoneWindowManager;
    private static Context mContext;
    private static Context mGbContext;
    private static String mStrAppKilled;
    private static String mStrNothingToKill;
    private static String mStrNoPrevApp;
    private static String mStrCustomAppNone;
    private static String mStrCustomAppMissing;
    private static String mStrExpandedDesktopDisabled;
    private static boolean mIsMenuLongPressed = false;
    private static boolean mIsMenuDoubleTap = false;
    private static boolean mIsBackLongPressed = false;
    private static boolean mIsBackDoubleTap = false;
    private static boolean mWasBackDoubleTap = false;
    private static boolean mIsRecentsLongPressed = false;
    private static boolean mIsHomeLongPressed = false;
    private static int mMenuLongpressAction = 0;
    private static int mMenuDoubletapAction = 0;
    private static int mHomeLongpressAction = 0;
    private static int mHomeLongpressActionKeyguard = 0;
    private static boolean mHomeDoubletapDisabled;
    private static int mHomeDoubletapDefaultAction;
    private static int mBackLongpressAction = 0;
    private static int mBackDoubletapAction = 0;
    private static int mRecentsSingletapAction = 0;
    private static int mRecentsLongpressAction = 0;
    private static boolean mVolumeRockerWakeDisabled = false;
    private static boolean mHwKeysEnabled = true;
    private static XSharedPreferences mPrefs;
    private static int mPieMode;
    private static int mExpandedDesktopMode;
    private static boolean mMenuKeyPressed;
    private static boolean mBackKeyPressed;


    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static enum HwKey {
        MENU,
        HOME,
        BACK,
        RECENTS
    }

    private static enum HwKeyTrigger {
        MENU_LONGPRESS,
        MENU_DOUBLETAP,
        HOME_LONGPRESS,
        HOME_LONGPRESS_KEYGUARD,
        BACK_LONGPRESS,
        BACK_DOUBLETAP,
        RECENTS_SINGLETAP,
        RECENTS_LONGPRESS
    }

  
    
    public static void initZygote() {
    	
    	
           //始化环境
            final Class<?> classPhoneWindowManager = XposedHelpers.findClass(CLASS_PHONE_WINDOW_MANAGER, null);
            classActivityManagerNative = XposedHelpers.findClass(CLASS_ACTIVITY_MANAGER_NATIVE, null);

            if (Build.VERSION.SDK_INT > 16) {
                XposedHelpers.findAndHookMethod(classPhoneWindowManager, "init",
                    Context.class, CLASS_IWINDOW_MANAGER, CLASS_WINDOW_MANAGER_FUNCS, phoneWindowManagerInitHook);
            } else {
                XposedHelpers.findAndHookMethod(classPhoneWindowManager, "init",
                        Context.class, CLASS_IWINDOW_MANAGER, CLASS_WINDOW_MANAGER_FUNCS, 
                        CLASS_LOCAL_POWER_MANAGER, phoneWindowManagerInitHook);
            }

            //捕获按键
            XposedHelpers.findAndHookMethod(classPhoneWindowManager, "interceptKeyBeforeQueueing", 
                    KeyEvent.class, int.class, boolean.class, new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    KeyEvent event = (KeyEvent) param.args[0];
                    int keyCode = event.getKeyCode();
                    boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
                    boolean keyguardOn = (Boolean) XposedHelpers.callMethod(mPhoneWindowManager, "keyguardOn");
                    Handler handler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");
                 

                    //捕获音量上键
                    if(keyCode == 24)
                    {
                        if (!down) {
                            handler.removeCallbacks(mHomeLongPressKeyguard);
                            if (mIsHomeLongPressed) {
                                mIsHomeLongPressed = false;
                                param.setResult(0);
                                return;
                            }
                           
                        } else if (keyguardOn) {
                            if (event.getRepeatCount() == 0) {
                                mIsHomeLongPressed = false;
                             handler.postDelayed(mHomeLongPressKeyguard, getLongpressTimeoutForAction(mHomeLongpressActionKeyguard));
                                
                            }
                        }
                    }
                    	
                    	
                    
                      }
            });
        
}
    
    
    private static XC_MethodHook phoneWindowManagerInitHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            mPhoneWindowManager = param.thisObject;
            mContext = (Context) XposedHelpers.getObjectField(mPhoneWindowManager, "mContext");
            mGbContext = mContext.createPackageContext(MyIXposedHook.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
            XposedHelpers.setIntField(mPhoneWindowManager, "mAllowAllRotations", 1);
        }
    };


    
    //运行长按音量上键内容
    private static Runnable mHomeLongPressKeyguard = new Runnable() {

        @Override
        public void run() {
            mIsHomeLongPressed = true;
            toggleLockScreenVoiceRecorder();
            //performAction(HwKeyTrigger.HOME_LONGPRESS_KEYGUARD);
        }
    };


    
    private static int getActionForHwKeyTrigger(HwKeyTrigger keyTrigger) {
        
    	int action = 0;
        if (keyTrigger == HwKeyTrigger.MENU_LONGPRESS) {
            action = mMenuLongpressAction;
        } else if (keyTrigger == HwKeyTrigger.MENU_DOUBLETAP) {
            action = mMenuDoubletapAction;
        } else if (keyTrigger == HwKeyTrigger.HOME_LONGPRESS) {
            action = mHomeLongpressAction;
        } else if (keyTrigger == HwKeyTrigger.HOME_LONGPRESS_KEYGUARD) {
            action = 10;
        } else if (keyTrigger == HwKeyTrigger.BACK_LONGPRESS) {
            action = mBackLongpressAction;
        } else if (keyTrigger == HwKeyTrigger.BACK_DOUBLETAP) {
            action = mBackDoubletapAction;
        } else if (keyTrigger == HwKeyTrigger.RECENTS_SINGLETAP) {
            action = mRecentsSingletapAction;
        } else if (keyTrigger == HwKeyTrigger.RECENTS_LONGPRESS) {
            action = mRecentsLongpressAction;
        }
        
        return action;
    }

    
    
    private static int getLongpressTimeoutForAction(int action) {
        return ViewConfiguration.getLongPressTimeout();
    }

    
    
    private static void performAction(HwKeyTrigger keyTrigger) {
    	
        int action = getActionForHwKeyTrigger(keyTrigger);
        
        if (action == 0) return;

       if (action == MyIXposedHook.HWKEY_ACTION_TORCH) {
            toggleLockScreenVoiceRecorder();
        } 
    }


   
   

    private static void launchCustomApp(final int action) {
        Handler handler = (Handler) XposedHelpers.getObjectField(mPhoneWindowManager, "mHandler");
        if (handler == null) return;
        mPrefs.reload();

        handler.post(
            new Runnable() {
                @Override
                public void run() {
                    try {
                    	
                    	//自己启动APP
                        String appInfo = "";

                        String[] splitValue = appInfo.split(SEPARATOR);
                        ComponentName cn = new ComponentName(splitValue[0], splitValue[1]);
                        Intent i = new Intent();
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        i.setComponent(cn);
                        mContext.startActivity(i);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(mContext, mStrCustomAppMissing, Toast.LENGTH_SHORT).show();
                    } catch (Throwable t) {
                        XposedBridge.log(t);
                    }
                }
            }
        );
    }

    
    private static void toggleLockScreenVoiceRecorder(){

        try {
            Intent intent = new Intent(mGbContext, VoiceRecordService.class);
            intent.setAction(VoiceRecordService.ACTION_TOGGLE_VOICERECORD);
            mGbContext.startService(intent);
        } catch (Throwable t) {
            log("Error toggling Torch: " + t.getMessage());
        }
    }
}