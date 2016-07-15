package com.example.myxposedmod;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;


import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class MyIXposedHook implements IXposedHookZygoteInit{
	
	 public static final String PACKAGE_NAME = MyIXposedHook.class.getPackage().getName();
	 public static final int HWKEY_ACTION_TORCH = 10;


	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		// TODO Auto-generated method stub
  
        ModHwKeys.initZygote();
	}

}
