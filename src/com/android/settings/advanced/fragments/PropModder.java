/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (C) 2012 LiquidSmooth
 * Copyright (C) 2012 Lounge Katt Entertainment
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.advanced.fragments;

import com.android.settings.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.Spannable;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import com.android.settings.advanced.SettingsPreferenceFragment;
import com.android.settings.util.CMDProcessor;
import com.android.settings.util.CMDProcessor.CommandResult;
import com.android.settings.util.Helpers;
import com.android.settings.Utils;

public class PropModder extends PreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "Settings :PropModder";
    private static final String APPEND_CMD = "echo \"%s=%s\" >> /system/build.prop";
    private static final String KILL_PROP_CMD = "busybox sed -i \"/%s/D\" /system/build.prop";
    private static final String REPLACE_CMD = "busybox sed -i \"/%s/ c %<s=%s\" /system/build.prop";
    private static final String LOGCAT_CMD = "busybox sed -i \"/log/ c %s\" /system/etc/init.d/72propmodder_script";
    private static final String SDCARD_BUFFER_CMD = "echo %s > /sys/devices/virtual/bdi/179:0/read_ahead_kb";
    private static final String REBOOT_PREF = "reboot";
    private static final String FIND_CMD = "grep -q \"%s\" /system/build.prop";
    private static final String REMOUNT_CMD = "busybox mount -o %s,remount -t yaffs2 /dev/block/mtdblock1 /system";
    private static final String PROP_EXISTS_CMD = "grep -q %s /system/build.prop";
    private static final String DISABLE = "disable";
    private static final String SHOWBUILD_PATH = "/system/tmp/showbuild";
    private static final String INIT_SCRIPT_LOGCAT = "/system/etc/init.d/72logcat";
    private static final String INIT_SCRIPT_SDCARD = "/system/etc/init.d/72sdcard";
    private static final String INIT_SCRIPT_TEMP_PATH = "/system/tmp/init_script";
    private static final String WIFI_SCAN_PREF = "pref_wifi_scan_interval";
    private static final String WIFI_SCAN_PROP = "wifi.supplicant_scan_interval";
    private static final String WIFI_SCAN_PERSIST_PROP = "persist.wifi_scan_interval";
    private static final String WIFI_SCAN_DEFAULT = System.getProperty(WIFI_SCAN_PROP);
    private static final String LCD_DENSITY_PREF = "pref_lcd_density";
    private static final String LCD_DENSITY_PROP = "ro.sf.lcd_density";
    private static final String LCD_DENSITY_PERSIST_PROP = "persist.lcd_density";
    private static final String LCD_DENSITY_DEFAULT = System.getProperty(LCD_DENSITY_PROP);
    private static final String MAX_EVENTS_PREF = "pref_max_events";
    private static final String MAX_EVENTS_PROP = "windowsmgr.max_events_per_sec";
    private static final String MAX_EVENTS_PERSIST_PROP = "persist.max_events";
    private static final String MAX_EVENTS_DEFAULT = System.getProperty(MAX_EVENTS_PROP);
    private static final String USB_MODE_PREF = "pref_usb_mode";
    private static final String USB_MODE_PROP = "ro.default_usb_mode";
    private static final String USB_MODE_PERSIST_PROP = "persist.usb_mode";
    private static final String USB_MODE_DEFAULT = System.getProperty(USB_MODE_PROP);
    private static final String RING_DELAY_PREF = "pref_ring_delay";
    private static final String RING_DELAY_PROP = "ro.telephony.call_ring.delay";
    private static final String RING_DELAY_PERSIST_PROP = "persist.call_ring.delay";
    private static final String RING_DELAY_DEFAULT = System.getProperty(RING_DELAY_PROP);
    private static final String VM_HEAPSIZE_PREF = "pref_vm_heapsize";
    private static final String VM_HEAPSIZE_PROP = "dalvik.vm.heapsize";
    private static final String VM_HEAPSIZE_PERSIST_PROP = "persist.vm_heapsize";
    private static final String VM_HEAPSIZE_DEFAULT = System.getProperty(VM_HEAPSIZE_PROP);
    private static final String FAST_UP_PREF = "pref_fast_up";
    private static final String FAST_UP_PROP = "ro.ril.hsxpa";
    private static final String FAST_UP_PERSIST_PROP = "persist.fast_up";
    private static final String FAST_UP_DEFAULT = System.getProperty(FAST_UP_PROP);
    private static final String PROX_DELAY_PREF = "pref_prox_delay";
    private static final String PROX_DELAY_PROP = "mot.proximity.delay";
    private static final String PROX_DELAY_PERSIST_PROP = "persist.prox.delay";
    private static final String PROX_DELAY_DEFAULT = System.getProperty(PROX_DELAY_PROP);
    private static final String LOGCAT_PREF = "pref_logcat";
    private static final String LOGCAT_PERSIST_PROP = "persist.logcat";
    private static final String LOGCAT_ALIVE_PATH = "/system/etc/init.d/72propmodder_script";
    private static final String LOGCAT_ENABLE = "rm /dev/log/main";
    private static final String MOD_VERSION_PREF = "pref_mod_version";
    private static final String MOD_VERSION_PROP = "ro.build.display.id";
    private static final String MOD_VERSION_PERSIST_PROP = "persist.build.display.id";
    private static final String MOD_VERSION_DEFAULT = System.getProperty(MOD_VERSION_PROP);
    private static final String MOD_BUTTON_TEXT = "doMod";
    private static final String MOD_VERSION_TEXT = "Mods by PropModder";
    private static final String SLEEP_PREF = "pref_sleep";
    private static final String SLEEP_PROP = "pm.sleep_mode";
    private static final String SLEEP_PERSIST_PROP = "persist.sleep";
    private static final String SLEEP_DEFAULT = System.getProperty(SLEEP_PROP);
    private static final String TCP_STACK_PREF = "pref_tcp_stack";
    private static final String TCP_STACK_PERSIST_PROP = "persist_tcp_stack";
    private static final String TCP_STACK_PROP_0 = "net.tcp.buffersize.default";
    private static final String TCP_STACK_PROP_1 = "net.tcp.buffersize.wifi";
    private static final String TCP_STACK_PROP_2 = "net.tcp.buffersize.umts";
    private static final String TCP_STACK_PROP_3 = "net.tcp.buffersize.gprs";
    private static final String TCP_STACK_PROP_4 = "net.tcp.buffersize.edge";
    private static final String TCP_STACK_BUFFER = "4096,87380,256960,4096,16384,256960";
    private static final String CHECK_IN_PREF = "pref_check_in";
    private static final String CHECK_IN_PERSIST_PROP = "persist_check_in";
    private static final String CHECK_IN_PROP = "ro.config.nocheckin";
    private static final String CHECK_IN_PROP_HTC = "ro.config.htc.nocheckin";
    private static final String SDCARD_BUFFER_PREF = "pref_sdcard_buffer";
    private static final String SDCARD_BUFFER_PRESIST_PROP = "persist_sdcard_buffer";
    private static final String GPU_PREF = "pref_gpu";
    private static final String GPU_PERSIST_PROP = "persist_gpu";
    private static final String GPU_PROP = "debug.sf.hw";
    
    private static final String TILED_RENDERING_PREF = "tiled_rendering";
    private static final String TILED_RENDERING_PROP = "debug.enabletr";
    private static final String TILED_RENDERING_DEFAULT = "false";
    
    private static final String BOOT_SOUND_PREF = "boot_sound";
    private static final String BOOT_SOUND_PROP = "ro.config.play.bootsound";
    private static final String BOOT_SOUND_DEFAULT = "1";
    
    private static final String KEY_COMPATIBILITY_MODE = "compatibility_mode";
    
    private static final String INSTALL_LOCATION = "install_location";
    
    private static final String COMP_TYPE_PREF = "composition_type";
    private static final String COMP_TYPE_PROP = "debug.composition.type";
    private static final String COMP_TYPE_DEFAULT = "mdp";
    
    private static final String COMP_BYPASS_PREF = "composition_bypass";
    private static final String COMP_BYPASS_PROP = "ro.sf.compbypass.enable";
    private static final String COMP_BYPASS_DEFAULT = "1";
    
    private static final String KEY_EXTERNAL_CACHE = "external_cache";
    
    private boolean NEEDS_SETUP = false;
    private boolean success = false;

    private String placeholder;
    private String tcpstack0;

    private String ModPrefHolder = SystemProperties.get(MOD_VERSION_PERSIST_PROP,
                SystemProperties.get(MOD_VERSION_PROP, MOD_VERSION_DEFAULT));

    //handles for our menu hard key press
    private final int MENU_MARKET = 1;
    private final int MENU_REBOOT = 2;
    private int NOTE_ID;

    private PreferenceScreen mRebootMsg;
    private ListPreference mWifiScanPref;
    private ListPreference mLcdDensityPref;
    private ListPreference mMaxEventsPref;
    private ListPreference mRingDelayPref;
    private ListPreference mVmHeapsizePref;
    private ListPreference mFastUpPref;
    private ListPreference mProxDelayPref;
    private CheckBoxPreference mLogcatPref;
    private EditTextPreference mModVersionPref;
    private ListPreference mSleepPref;
    private CheckBoxPreference mTcpStackPref;
    private CheckBoxPreference mCheckInPref;
    private ListPreference mSdcardBufferPref;
    private CheckBoxPreference mGpuPref;
    private AlertDialog mAlertDialog;
    private NotificationManager mNotificationManager;
            
    private CheckBoxPreference mTiledRenderingPref;
    private CheckBoxPreference mBootSoundPref;
    private CheckBoxPreference mCompatibilityMode;
    private CheckBoxPreference mCompositionBypass;
    private ListPreference mCompositionType;
    private ListPreference mInstallLocation;
    private Preference mExternalCache;
    private Preference mCarrier;
    
    String mCarrierText = null;
    
    File cacheDir = new File(Environment.getExternalStorageDirectory() + "/cache/");

    private File tmpDir = new File("/system/tmp");
    private File init_d = new File("/system/etc/init.d");
    private File initScriptLogcat = new File(INIT_SCRIPT_LOGCAT);
    private File initScriptSdcard = new File(INIT_SCRIPT_SDCARD);

    //handler for command processor
    private final CMDProcessor cmd = new CMDProcessor();
    private PreferenceScreen prefSet;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "asking for SU premission " + cmd.su.runWaitFor("echo PROPMODDER").success());

        addPreferencesFromResource(R.xml.propmodder);
        prefSet = getPreferenceScreen();

        mRebootMsg = (PreferenceScreen) prefSet.findPreference(REBOOT_PREF);
        prefSet.removePreference(mRebootMsg);

        mWifiScanPref = (ListPreference) prefSet.findPreference(WIFI_SCAN_PREF);
        mWifiScanPref.setOnPreferenceChangeListener(this);

        mLcdDensityPref = (ListPreference) prefSet.findPreference(LCD_DENSITY_PREF);
        mLcdDensityPref.setOnPreferenceChangeListener(this);

        mMaxEventsPref = (ListPreference) prefSet.findPreference(MAX_EVENTS_PREF);
        mMaxEventsPref.setOnPreferenceChangeListener(this);

        mRingDelayPref = (ListPreference) prefSet.findPreference(RING_DELAY_PREF);
        mRingDelayPref.setOnPreferenceChangeListener(this);

        mVmHeapsizePref = (ListPreference) prefSet.findPreference(VM_HEAPSIZE_PREF);
        mVmHeapsizePref.setOnPreferenceChangeListener(this);

        mFastUpPref = (ListPreference) prefSet.findPreference(FAST_UP_PREF);
        mFastUpPref.setOnPreferenceChangeListener(this);

        mProxDelayPref = (ListPreference) prefSet.findPreference(PROX_DELAY_PREF);
        mProxDelayPref.setOnPreferenceChangeListener(this);

        mLogcatPref = (CheckBoxPreference) prefSet.findPreference(LOGCAT_PREF);

        mSleepPref = (ListPreference) prefSet.findPreference(SLEEP_PREF);
        mSleepPref.setOnPreferenceChangeListener(this);

        mTcpStackPref = (CheckBoxPreference) prefSet.findPreference(TCP_STACK_PREF);

        mModVersionPref = (EditTextPreference) prefSet.findPreference(MOD_VERSION_PREF);
        String mod = Helpers.findBuildPropValueOf(MOD_VERSION_PROP);
        if (mModVersionPref != null) {
            EditText modET = mModVersionPref.getEditText();
            ModPrefHolder = mModVersionPref.getEditText().toString();
            if (modET != null){
                InputFilter lengthFilter = new InputFilter.LengthFilter(32);
                modET.setFilters(new InputFilter[]{lengthFilter});
                modET.setSingleLine(true);
            }
            mModVersionPref.setSummary(String.format(getString(R.string.pref_mod_version_alt_summary), mod));
        }
        Log.d(TAG, String.format("ModPrefHoler = '%s' found build number = '%s'", ModPrefHolder, mod));
        mModVersionPref.setOnPreferenceChangeListener(this);

        mCheckInPref = (CheckBoxPreference) prefSet.findPreference(CHECK_IN_PREF);

        //TODO check all init.d scripts for buffer values to display in summary
        //     for now we will just let it go with a generic summary displayed
        mSdcardBufferPref = (ListPreference) prefSet.findPreference(SDCARD_BUFFER_PREF);
        mSdcardBufferPref.setOnPreferenceChangeListener(this);

        mGpuPref = (CheckBoxPreference) prefSet.findPreference(GPU_PREF);
        
        mTiledRenderingPref = (CheckBoxPreference) prefSet.findPreference(TILED_RENDERING_PREF);
        String tiledRendering = SystemProperties.get(TILED_RENDERING_PROP, TILED_RENDERING_DEFAULT);
        if (tiledRendering != null) {
            mTiledRenderingPref.setChecked("true".equals(tiledRendering));
        } else {
            prefSet.removePreference(mTiledRenderingPref);
        }
        
        mCompositionType = (ListPreference) findPreference(COMP_TYPE_PREF);
        mCompositionType.setOnPreferenceChangeListener(this);
        mCompositionType.setValue(SystemProperties.get(COMP_TYPE_PROP, COMP_TYPE_DEFAULT));
        mCompositionType.setOnPreferenceChangeListener(this);
        
        String currentInstall = "0";
        CommandResult installCheck = new CMDProcessor().su.runWaitFor("pm get-install-location");
        if (installCheck.success()) {
            currentInstall =  (installCheck.stdout).substring(0,1);
        }
        
        mInstallLocation = (ListPreference) findPreference(INSTALL_LOCATION);
        mInstallLocation.setOnPreferenceChangeListener(this);
        mInstallLocation.setValue(currentInstall);
        mInstallLocation.setOnPreferenceChangeListener(this);
        
        mCompositionBypass = (CheckBoxPreference) findPreference(COMP_BYPASS_PREF);
        String compositionBypass = SystemProperties.get(COMP_BYPASS_PROP, COMP_BYPASS_DEFAULT);
        mCompositionBypass.setChecked("1".equals(compositionBypass));
        
        mBootSoundPref = (CheckBoxPreference) findPreference(BOOT_SOUND_PREF);
        String bootSound = SystemProperties.get(BOOT_SOUND_PROP, BOOT_SOUND_DEFAULT);
        mBootSoundPref.setChecked("1".equals(bootSound));
        
        mCompatibilityMode = (CheckBoxPreference) findPreference(KEY_COMPATIBILITY_MODE);
        mCompatibilityMode.setPersistent(false);
        mCompatibilityMode.setChecked(Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(), Settings.System.COMPATIBILITY_MODE, 0) != 0);
        
        mExternalCache = findPreference(KEY_EXTERNAL_CACHE);
        File cacheDir = new File(Environment.getExternalStorageDirectory() + "/cache/");
        if (cacheDir.exists()) {
            mExternalCache.setTitle("Disable External Cache");
        } else {
            mExternalCache.setTitle("Enable External Cache");
        }

        new UpdateScreen().execute();
    }
            
    private void exportCache(Preference preference, boolean export) {
        Helpers.getMount("rw");
        if (!isSdPresent()) {
            preference.setSummary("Sdcard Unavailable");
        } else {
            String extCache = "/cache/files/maps/";
            File extDir = new File(Environment.getExternalStorageDirectory()
                                   + extCache);
            if (!extDir.mkdirs()) {
                extDir.mkdirs();
            }
            String webCache = "/cache/webviewCache/";
            File extWeb = new File(Environment.getExternalStorageDirectory()
                                   + webCache);
            if (!extWeb.mkdirs()) {
                extWeb.mkdirs();
            }
            String streetCache = "/cache/streetCache/";
            File extStreet = new File(Environment.getExternalStorageDirectory()
                                      + streetCache);
            if (!extStreet.mkdirs()) {
                extStreet.mkdirs();
            }
            String marketCache = "/cache/marketCache/";
            File extMarket = new File(Environment.getExternalStorageDirectory()
                                      + marketCache);
            if (!extMarket.mkdirs()) {
                extMarket.mkdirs();
            }
            if (export) {
                cacheDir.mkdirs();
                List<String> rmCache = new ArrayList<String>();
                List<String> lnCache = new ArrayList<String>();
                rmCache.add("busybox rm -rf /data/data/com.android.browser/cache/webviewCache");
                lnCache.add("busybox ln -s /sdcard/cache/webviewCache /data/data/com.android.browser/cache/webviewCache");
                rmCache.add("busybox rm -rf /data/data/com.google.android.gm/cache/webviewCache");
                lnCache.add("busybox ln -s /sdcard/cache/webviewCache /data/data/com.google.android.gm/cache/webviewCache");
                rmCache.add("busybox rm -rf /data/data/com.google.android.voicesearch/cache/webviewCache");
                lnCache.add("busybox ln -s /sdcard/cache/webviewCache /data/data/com.google.android.voicesearch/cache/webviewCache");
                rmCache.add("busybox rm -rf /data/data/com.google.android.apps.maps/files");
                lnCache.add("busybox ln -s /sdcard/cache/files/maps /data/data/com.google.android.apps.maps/files");
                rmCache.add("busybox rm -rf /data/data/com.google.android.street/cache");
                lnCache.add("busybox ln -s /sdcard/cache/streetCache /data/data/com.google.android.street/cache");
                rmCache.add("busybox rm -rf /data/data/com.android.vending/cache");
                lnCache.add("busybox ln -s /sdcard/cache/marketCache /data/data/com.android.vending/cache");
                for (int i = 0; i < rmCache.size(); i++) {
                    new CMDProcessor().su.runWaitFor(rmCache.get(i));
                    new CMDProcessor().su.runWaitFor(lnCache.get(i));
                }
                preference.setTitle("Disable External Cache");
                preference.setSummary("Google Apps now store their related cache in external sdcard");
            } else {
                List<String> rmCache = new ArrayList<String>();
                rmCache.add("busybox rm -rf /data/data/com.android.browser/cache/webviewCache");
                rmCache.add("busybox rm -rf /sdcard/cache/webviewCache");
                rmCache.add("busybox rm -rf /data/data/com.google.android.gm/cache/webviewCache");
                rmCache.add("busybox rm -rf /data/data/com.google.android.voicesearch/cache/webviewCache");
                rmCache.add("busybox rm -rf /data/data/com.google.android.apps.maps/files");
                rmCache.add("busybox rm -rf /sdcard/cache/files/maps");
                rmCache.add("busybox rm -rf /data/data/com.google.android.street/cache");
                rmCache.add("busybox rm -rf /sdcard/cache/streetCache");
                rmCache.add("busybox rm -rf /data/data/com.android.vending/cache");
                rmCache.add("busybox rm -rf /sdcard/cache/marketCache");
                rmCache.add("busybox rm -rf /sdcard/cache");
                for (int i = 0; i < rmCache.size(); i++) {
                    new CMDProcessor().su.runWaitFor(rmCache.get(i));
                }
                preference.setTitle("Enable External Cache");
                preference.setSummary("Google Apps now store their related cache in internal data");
            }
            Helpers.getMount("ro");
        }
    }
    
    public static boolean isSdPresent() {
        return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
    }

    public void writeScript() {
        Handler mHandler = new Handler();
        final Runnable mCheckForDirectories = new Runnable() {
            public void run() {
                //Mounting takes the most time so lets avoid doing it if possible
                if (!tmpDir.isDirectory() || !init_d.isDirectory()) NEEDS_SETUP = true;

                if (NEEDS_SETUP) {
                    try {
                        if (!mount("rw")) throw new RuntimeException("Could not remount /system rw");
                        if (!tmpDir.isDirectory()) {
                            Log.d(TAG, "We need to make /system/tmp dir");
                            cmd.su.runWaitFor("mkdir /system/tmp");
                        }
                        if (!init_d.isDirectory()) {
                            Log.d(TAG, "We need to make /system/etc/init.d/ dir");
                            enableInit();
                        }
                    } finally {
                        mount("ro");
                        NEEDS_SETUP = false;
                    }
                }
            }
        };
        mHandler.post(mCheckForDirectories);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "com.android.settings.advanced.fragments.PropModder has been paused");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "com.android.settings.advanced.fragments.PropModder is being resumed");
    }

    /* handle CheckBoxPreference clicks */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;
        if (preference == mLogcatPref) {
            value = mLogcatPref.isChecked();
            boolean returnValue = false;
            mount("rw");
            if (value) {
                returnValue = cmd.su.runWaitFor(String.format("echo %s > %s", LOGCAT_ENABLE, INIT_SCRIPT_LOGCAT)).success();
                if (returnValue) {
                    cmd.su.runWaitFor(String.format("chmod 555 %s", INIT_SCRIPT_LOGCAT)).success();
                }        
            } else {
                returnValue = cmd.su.runWaitFor(String.format("rm %s", INIT_SCRIPT_LOGCAT)).success();
            }
            mount("ro");
            rebootRequired();
            return returnValue;
        } else if (preference == mTcpStackPref) {
            Log.d(TAG, "mTcpStackPref.onPreferenceTreeClick()");
            value = mTcpStackPref.isChecked();
            return doMod(null, TCP_STACK_PROP_0, String.valueOf(value ? TCP_STACK_BUFFER : DISABLE))
                    && doMod(null, TCP_STACK_PROP_1, String.valueOf(value ? TCP_STACK_BUFFER : DISABLE))
                    && doMod(null, TCP_STACK_PROP_2, String.valueOf(value ? TCP_STACK_BUFFER : DISABLE))
                    && doMod(null, TCP_STACK_PROP_3, String.valueOf(value ? TCP_STACK_BUFFER : DISABLE))
                    && doMod(TCP_STACK_PERSIST_PROP, TCP_STACK_PROP_4, String.valueOf(value ? TCP_STACK_BUFFER : DISABLE));
        } else if (preference == mCheckInPref) {
            value = mCheckInPref.isChecked();
            return doMod(null, CHECK_IN_PROP_HTC, String.valueOf(value ? 1 : DISABLE))
            && doMod(CHECK_IN_PERSIST_PROP, CHECK_IN_PROP, String.valueOf(value ? 1 : DISABLE));
        } else if (preference == mGpuPref) {
            value = mGpuPref.isChecked();
            return doMod(GPU_PERSIST_PROP, GPU_PROP, String.valueOf(value ? 1 : DISABLE));
        } else if (preference == mRebootMsg) {
            return cmd.su.runWaitFor("reboot").success();
        } else if (preference == mTiledRenderingPref) {
            Helpers.getMount("rw");
            new CMDProcessor().su.runWaitFor("busybox sed -i 's|"+ TILED_RENDERING_PROP +"=.*|" + TILED_RENDERING_PROP + "=" + (String)(mTiledRenderingPref.isChecked() ? "true" : "false") + "|' " + "/system/build.prop");
            Helpers.getMount("ro");
        } else if (preference == mCompositionBypass) {
            Helpers.getMount("rw");
            new CMDProcessor().su.runWaitFor("busybox sed -i 's|"+ COMP_BYPASS_PROP +"=.*|" + COMP_BYPASS_PROP + "=" + (String)(mCompositionBypass.isChecked() ? "1" : "0") + "|' " + "/system/build.prop");
            Helpers.getMount("ro");
        } else if (preference == mBootSoundPref) {
            Helpers.getMount("rw");
            new CMDProcessor().su.runWaitFor("busybox sed -i 's|"+ BOOT_SOUND_PROP +"=.*|" + BOOT_SOUND_PROP + "=" + (String)(mBootSoundPref.isChecked() ? "1" : "0") + "|' " + "/system/build.prop");
            Helpers.getMount("ro");
        } else if (preference == mCompatibilityMode) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(), Settings.System.COMPATIBILITY_MODE, mCompatibilityMode.isChecked() ? 1 : 0);
        } else if (preference == mExternalCache) {
            if (cacheDir.exists()) {
                exportCache(preference, false);
            } else {
                exportCache(preference, true);
            }
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return false;
    }

    /* handle ListPreferences and EditTextPreferences */
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue != null) {
            Log.e(TAG, "New preference selected: " + newValue);
            if (preference == mWifiScanPref) {
                return doMod(WIFI_SCAN_PERSIST_PROP, WIFI_SCAN_PROP,
                        newValue.toString());
            } else if (preference == mLcdDensityPref) {
                return doMod(LCD_DENSITY_PERSIST_PROP, LCD_DENSITY_PROP,
                        newValue.toString());
            } else if (preference == mMaxEventsPref) {
                return doMod(MAX_EVENTS_PERSIST_PROP, MAX_EVENTS_PROP,
                        newValue.toString());
            } else if (preference == mRingDelayPref) {
                return doMod(RING_DELAY_PERSIST_PROP, RING_DELAY_PROP,
                        newValue.toString());
            } else if (preference == mVmHeapsizePref) {
                return doMod(VM_HEAPSIZE_PERSIST_PROP, VM_HEAPSIZE_PROP,
                        newValue.toString());
            } else if (preference == mFastUpPref) {
                return doMod(FAST_UP_PERSIST_PROP, FAST_UP_PROP,
                        newValue.toString());
            } else if (preference == mProxDelayPref) {
                 return doMod(PROX_DELAY_PERSIST_PROP, PROX_DELAY_PROP,
                        newValue.toString());
            } else if (preference == mModVersionPref) {
                 return doMod(MOD_VERSION_PERSIST_PROP, MOD_VERSION_PROP,
                        newValue.toString());
            } else if (preference == mSleepPref) {
                 return doMod(SLEEP_PERSIST_PROP, SLEEP_PROP,
                        newValue.toString());
            } else if (preference == mSdcardBufferPref) {
                boolean returnValue = false;
                mount("rw");
                if (newValue.toString() == DISABLE) {
                    returnValue = cmd.su.runWaitFor(String.format("rm %s", INIT_SCRIPT_SDCARD)).success();
                } else {
                    String newFormat = String.format(SDCARD_BUFFER_CMD, newValue.toString());
                    cmd.su.runWaitFor(String.format(SDCARD_BUFFER_CMD, newValue.toString()));
                    cmd.su.runWaitFor(String.format("echo '%s' > %s", newFormat, INIT_SCRIPT_SDCARD));
                    cmd.su.runWaitFor(String.format("chmod 555 %s", INIT_SCRIPT_SDCARD));
                    returnValue = true;
                }
                mount("ro");
                rebootRequired();
                return returnValue;
            } else if (preference == mCompositionType) {
                if (newValue != null) {
                    SystemProperties.set(COMP_TYPE_PROP, (String)newValue);
                    Helpers.getMount("rw");
                    new CMDProcessor().su.runWaitFor("busybox sed -i 's|"+COMP_TYPE_PROP+"=.*|"+COMP_TYPE_PROP+"="+newValue+"|' "+"/system/build.prop");
                    Helpers.getMount("ro");
                    mCompositionType.setSummary("Queued "+newValue+" composition");
                    return true;
                }
            } else if (preference == mInstallLocation) {
                if (newValue != null) {
                    new CMDProcessor().su.runWaitFor("pm set-install-location "+newValue);
                    String summary = "default location";
                    if (newValue.equals("0")) {
                        summary = "automatic location";
                    } else if (newValue.equals("1")) {
                        summary = "internal only";
                    } else if (newValue.equals("2")) {
                        summary = "external only";
                    }
                    mInstallLocation.setSummary("Install to "+summary);
                    return true;
                }
            }
        }

        return false;
    }

    public boolean doMod(String persist, String key, String value) {
        return doMod(persist, key, value, false);
    }

    /* method to handle mods */
    public boolean doMod(final String persist, final String key, final String value, final boolean externalThread) {
        Handler mModHandler = new Handler();
        Runnable mMakeChanges = new Runnable() {
            public void run() {
                success = false;
                if (persist != null) SystemProperties.set(persist, value);
                Log.d(TAG, String.format("Calling script with args '%s' and '%s'", key, value));
                backupBuildProp();
                if (!mount("rw")) {
                    throw new RuntimeException("Could not remount /system rw");
                }
                try {
                    if (!propExists(key) && value.equals(DISABLE)) {
                        Log.d(TAG, String.format("We want {%s} DISABLED however it doesn't exist so we do nothing and move on", key));
                    } else if (propExists(key)) {
                        if (value.equals(DISABLE)) {
                            Log.d(TAG, String.format("value == %s", DISABLE));
                            success = cmd.su.runWaitFor(String.format(KILL_PROP_CMD, key)).success();
                        } else {
                            Log.d(TAG, String.format("value != %s", DISABLE));
                            success = cmd.su.runWaitFor(String.format(REPLACE_CMD, key, value)).success();
                        }
                    } else {
                        Log.d(TAG, "append command starting");
                        success = cmd.su.runWaitFor(String.format(APPEND_CMD, key, value)).success();
                    }
                    if (!success) {
                        restoreBuildProp();
                    } else {
                        if (!externalThread)
                            new UpdateScreen().execute();
                    }
                } finally {
                    mount("ro");
                }
                if (!externalThread)
                    rebootRequired();
            }
        };
        mModHandler.post(mMakeChanges);
        return success;
    }

    private void rebootRequired() {
        prefSet.addPreference(mRebootMsg);
        mRebootMsg.setTitle("Reboot required");
        mRebootMsg.setSummary("values will take effect on next boot");
    }

    public boolean mount(String read_value) {
        return Helpers.mountSystem(read_value);
    }

    public boolean propExists(String prop) {
        Log.d(TAG, "Checking if prop " + prop + " exists in /system/build.prop");
        return cmd.su.runWaitFor(String.format(PROP_EXISTS_CMD, prop)).success();
    }

    public void updateShowBuild() {
        Log.d(TAG, "Setting up /system/tmp/showbuild");
        try {
            mount("rw");
            cmd.su.runWaitFor("cp -f /system/build.prop " + SHOWBUILD_PATH).success();
            cmd.su.runWaitFor("chmod 777 " + SHOWBUILD_PATH).success();
        } finally {
            mount("ro");
        }
    }

    public boolean initLogcat(boolean swap0) {
        if (swap0) {
            cmd.su.runWaitFor(String.format("echo 'rm /dev/log/main' >  %s", INIT_SCRIPT_LOGCAT)).success();
            return cmd.su.runWaitFor(String.format("chmod 555 %s", INIT_SCRIPT_LOGCAT)).success();
        } else {
            return cmd.su.runWaitFor(String.format("rm -f %s", INIT_SCRIPT_LOGCAT)).success();
        }
    }

    public boolean initSdcard(boolean swap1) {
        if (swap1) {
            cmd.su.runWaitFor(String.format("echo 'rm -f /dev/log/main' >  %s", INIT_SCRIPT_LOGCAT)).success();
            return cmd.su.runWaitFor(String.format("chmod 755 %s", INIT_SCRIPT_LOGCAT)).success();
        } else {
            return cmd.su.runWaitFor(String.format("rm -f %s", INIT_SCRIPT_LOGCAT)).success();
        }
    }

    public void enableInit() {
        Handler mEnableInit = new Handler();
        Runnable mWriteInitFiles = new Runnable() {
            public void run() {
                FileWriter wAlive;
                try {
                    wAlive = new FileWriter("/system/tmp/initscript");
                    //forgive me but without all the \n's the script is one line long O:-)
                    wAlive.write("#\n#enable init.d script by PropModder\n#\n\n");
                    wAlive.write("log -p I -t boot \"Starting init.d ...\"\n");
                    wAlive.write("busybox run-parts /system/etc/init.d");
                    wAlive.flush();
                    wAlive.close();
                    cmd.su.runWaitFor("cp -f /system/tmp/initscript /system/usr/bin/init.sh");
                    cmd.su.runWaitFor("chmod 755 /system/usr/bin/pm_init.sh").success();
                } catch(Exception e) {
                    Log.e(TAG, "enableInit install failed: " + e);
                    e.printStackTrace();
                }
            }
        };
        mEnableInit.post(mWriteInitFiles);
    }

    public boolean backupBuildProp() {
        Log.d(TAG, "Backing up build.prop to /system/tmp/pm_build.prop");
        return cmd.su.runWaitFor("cp /system/build.prop /system/tmp/pm_build.prop").success();
    }
    
    public boolean restoreBuildProp() {
        Log.d(TAG, "Restoring build.prop from /system/tmp/pm_build.prop");
        return cmd.su.runWaitFor("cp /system/tmp/pm_build.prop /system/build.prop").success();
    }

    private class UpdateScreen extends AsyncTask<Void, Void, Void> {
        String wifi;
        String lcd;
        String maxE;
        String ring;
        String vm;
        String fast;
        String prox;
        String sleep;
        String tcp;
        String mod;
        String chk;
        String gpu;

        public UpdateScreen() {
        }

        protected void onPreExecute() {
        }

        protected Void doInBackground(Void... noNeed) {
            // accessing storage is slow so don't block the main
            // thread while updating values from build.prop
            wifi = Helpers.findBuildPropValueOf(WIFI_SCAN_PROP);
            lcd = Helpers.findBuildPropValueOf(LCD_DENSITY_PROP);
            maxE = Helpers.findBuildPropValueOf(MAX_EVENTS_PROP);
            ring = Helpers.findBuildPropValueOf(RING_DELAY_PROP);
            vm = Helpers.findBuildPropValueOf(VM_HEAPSIZE_PROP);
            fast = Helpers.findBuildPropValueOf(FAST_UP_PROP);
            prox = Helpers.findBuildPropValueOf(PROX_DELAY_PROP);
            sleep = Helpers.findBuildPropValueOf(SLEEP_PROP);
            tcp = Helpers.findBuildPropValueOf(TCP_STACK_PROP_0);
            mod = Helpers.findBuildPropValueOf(MOD_VERSION_PROP);
            chk = Helpers.findBuildPropValueOf(CHECK_IN_PROP);
            gpu = Helpers.findBuildPropValueOf(GPU_PROP);
            return null;
        }

        protected void onPostExecute(Void no) {
            //update all the summaries
            if (!wifi.equals(DISABLE)) {
                mWifiScanPref.setValue(wifi);
                mWifiScanPref.setSummary(String.format(getString(R.string.pref_wifi_scan_alt_summary), wifi));
            } else {
                mWifiScanPref.setValue(WIFI_SCAN_DEFAULT);
            }
            if (!lcd.equals(DISABLE)) {
                mLcdDensityPref.setValue(lcd);
                mLcdDensityPref.setSummary(String.format(getString(R.string.pref_lcd_density_alt_summary), lcd));
            } else {
                mLcdDensityPref.setValue(LCD_DENSITY_DEFAULT);
            }
            if (!maxE.equals(DISABLE)) {
                mMaxEventsPref.setValue(maxE);
                mMaxEventsPref.setSummary(String.format(getString(R.string.pref_max_events_alt_summary), maxE));
            } else {
                mMaxEventsPref.setValue(MAX_EVENTS_DEFAULT);
            }
            if (!ring.equals(DISABLE)) {
                mRingDelayPref.setValue(ring);
                mRingDelayPref.setSummary(String.format(getString(R.string.pref_ring_delay_alt_summary), ring));
            } else {
                mRingDelayPref.setValue(RING_DELAY_DEFAULT);
            }
            if (!vm.equals(DISABLE)) {
                mVmHeapsizePref.setValue(vm);
                mVmHeapsizePref.setSummary(String.format(getString(R.string.pref_vm_heapsize_alt_summary), vm));
            } else {
                mVmHeapsizePref.setValue(VM_HEAPSIZE_DEFAULT);
            }
            if (!fast.equals(DISABLE)) {
                mFastUpPref.setValue(fast);
                mFastUpPref.setSummary(String.format(getString(R.string.pref_fast_up_alt_summary), fast));
            } else {
                mFastUpPref.setValue(FAST_UP_DEFAULT);
            }
            if (!prox.equals(DISABLE)) {
                mProxDelayPref.setValue(prox);
                mProxDelayPref.setSummary(String.format(getString(R.string.pref_prox_delay_alt_summary), prox));
            } else {
                mProxDelayPref.setValue(PROX_DELAY_DEFAULT);
            }
            if (!sleep.equals(DISABLE)) {
                mSleepPref.setValue(sleep);
                mSleepPref.setSummary(String.format(getString(R.string.pref_sleep_alt_summary), sleep));
            } else {
                mSleepPref.setValue(SLEEP_DEFAULT);
            }
            if (tcp.equals(TCP_STACK_BUFFER)) {
                mTcpStackPref.setChecked(true);
            } else {
                mTcpStackPref.setChecked(false);
            }
            mModVersionPref.setSummary(String.format(getString(R.string.pref_mod_version_alt_summary), mod));
            if (!chk.equals(DISABLE)) {
                mCheckInPref.setChecked(true);
            } else {
                mCheckInPref.setChecked(false);
            }
            if (!gpu.equals(DISABLE)) {
                mGpuPref.setChecked(true);
            } else {
                mGpuPref.setChecked(false);
            }
            if (initScriptLogcat.isFile()) {
                mLogcatPref.setChecked(true);
            } else {
                mLogcatPref.setChecked(false);
            }
            writeScript();

        }
    }
}
