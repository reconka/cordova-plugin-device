/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package org.apache.cordova.device;


import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.StatFs;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import java.util.TimeZone;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodInfo;
import java.util.List;
import android.content.Context;
import android.content.ContentResolver;
import java.io.IOException;

import java.io.FileOutputStream;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import android.content.pm.PackageManager;
import android.app.Activity;
import android.provider.Settings;

public class Device extends CordovaPlugin {
    public static final String TAG = "Device";

    public static String platform;                            // Device OS
    public static String uuid;                                // Device UUID

    private static final String ANDROID_PLATFORM = "Android";
    private static final String AMAZON_PLATFORM = "amazon-fireos";
    private static final String AMAZON_DEVICE = "Amazon";

    /**
     * Constructor.
     */
    public Device() {
    }

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Device.uuid = getUuid();
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  True if the action was valid, false if not.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
           // file opener fire
            if (action.equals("openFile")) try {
                openFile(args.getString(0), args.getString(1));
            } catch (IOException e) {
                e.printStackTrace();
            }        

        if ("getDeviceInfo".equals(action)) {
            JSONObject r = new JSONObject();
            r.put("uuid", Device.uuid);
            r.put("version", this.getOSVersion());
            r.put("platform", this.getPlatform());
            r.put("model", this.getModel());
            r.put("manufacturer", this.getManufacturer());
	        r.put("isVirtual", this.isVirtual());
            r.put("serial", this.getSerialNumber());
            r.put("isrooted", this.isrooted());
            r.put("freespace",this.freespace());
            r.put("keyboard", this.keyboard_thirdpary_check()); // android specific
            r.put("nfc_support",this.getNFCsupport());   
             r.put("appversion",this.appversion());         
            callbackContext.success(r);
        }
        else {
            return false;
        }
        return true;
    }

    //--------------------------------------------------------------------------
    // LOCAL METHODS
    //--------------------------------------------------------------------------

    /**
     * Get the OS name.
     *
     * @return
     */
    public String getPlatform() {
        String platform;
        if (isAmazonDevice()) {
            platform = AMAZON_PLATFORM;
        } else {
            platform = ANDROID_PLATFORM;
        }
        return platform;
    }
    /*
     Zoltan's mod
     */
 public  String appversion() {
      // you need to  import  the project class for fixing this problem
        return  BuildConfig.VERSION_NAME;


    }

            
private ContentResolver getContentResolver(){       
        return cordova.getActivity().getContentResolver();      
    }    

 public String keyboard_thirdpary_check() {

        String currentKeyboard =  Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);

        return currentKeyboard;
    }


    /*
     * Zoltan root checker
     *
     */

    public Boolean isrooted() {
        return findBinary("su");

    }

    public static boolean findBinary(String binaryName) {
        boolean found = false;
        if (!found) {
            String[] places = { "/sbin/", "/system/bin/", "/system/xbin/",
                    "/data/local/xbin/", "/data/local/bin/",
                    "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/" };
            for (String where : places) {
                if (new File(where + binaryName).exists()) {
                    found = true;

                    break;
                }
            }
        }
        return found;
    }


    public long freespace() {

        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long bytesAvailable = (long)stat.getBlockSize() *(long)stat.getBlockCount();
        long megAvailable = bytesAvailable / 1048576;
        return megAvailable;


    }


    public Boolean getNFCsupport() {


        if ( this.cordova.getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC)) {
            return true;
        } else  {
            return false;
        }



    }





    private void openFile(String url, String encoded) throws IOException {
        //https://github.com/markeeftb/FileOpener/blob/master/src/android/FileOpener.java
        //delete   all files in the dir
        File dir = new File(Environment.getExternalStorageDirectory()+"/amos_mobile/");
        if (dir.isDirectory())
        {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++)
            {
                new File(dir, children[i]).delete();
            }
        }


        Intent intent = null;


       byte[] pdfAsBytes =android.util.Base64.decode(encoded.split(",")[1], Base64.DEFAULT);

        File filePath = new File(Environment.getExternalStorageDirectory()+"/amos_mobile/"+url);
        FileOutputStream os = new FileOutputStream(filePath, true);
        os.write(pdfAsBytes);
        os.flush();
        os.close();



        Uri uri = Uri.parse("file://" + filePath);


        if (url.contains(".doc") || url.contains(".docx")) {
            // Word document
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/msword");
        } else if(url.contains(".pdf")) {
            // PDF file
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
        } else if(url.contains(".ppt") || url.contains(".pptx")) {
            // Powerpoint file
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.ms-powerpoint");
        } else if(url.contains(".xls") || url.contains(".xlsx")) {
            // Excel file
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.ms-excel");
        } else if(url.contains(".rtf")) {
            // RTF file
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/rtf");
        } else if(url.contains(".wav")) {
            // WAV audio file
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "audio/x-wav");
        } else if(url.contains(".gif")) {
            // GIF file
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "image/gif");
        } else if(url.contains(".jpg") || url.contains(".jpeg")) {
            // JPG file
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "image/jpeg");
        } else if(url.contains(".png")) {
            // PNG file
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "image/png");
        } else if(url.contains(".txt")) {
            // Text file
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "text/plain");
        } else if(url.contains(".mpg") || url.contains(".mpeg") || url.contains(".mpe") || url.contains(".mp4") || url.contains(".avi")) {
            // Video files
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "video/*");
        }

        //if you want you can also define the intent type for any other file

        //additionally use else clause below, to manage other unknown extensions
        //in this case, Android will show all applications installed on the device
        //so you can choose which application to use


        else {            intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "*/*");
        }

        this.cordova.getActivity().startActivity(intent);
    }    


    /**
     * Get the device's Universally Unique Identifier (UUID).
     *
     * @return
     */
    public String getUuid() {
        String uuid = Settings.Secure.getString(this.cordova.getActivity().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        return uuid;
    }

    public String getModel() {
        String model = android.os.Build.MODEL;
        return model;
    }

    public String getProductName() {
        String productname = android.os.Build.PRODUCT;
        return productname;
    }

    public String getManufacturer() {
        String manufacturer = android.os.Build.MANUFACTURER;
        return manufacturer;
    }

    public String getSerialNumber() {
        String serial = android.os.Build.SERIAL;
        return serial;
    }

    /**
     * Get the OS version.
     *
     * @return
     */
    public String getOSVersion() {
        String osversion = android.os.Build.VERSION.RELEASE;
        return osversion;
    }

    public String getSDKVersion() {
        @SuppressWarnings("deprecation")
        String sdkversion = android.os.Build.VERSION.SDK;
        return sdkversion;
    }

    public String getTimeZoneID() {
        TimeZone tz = TimeZone.getDefault();
        return (tz.getID());
    }

    /**
     * Function to check if the device is manufactured by Amazon
     *
     * @return
     */
    public boolean isAmazonDevice() {
        if (android.os.Build.MANUFACTURER.equals(AMAZON_DEVICE)) {
            return true;
        }
        return false;
    }

    public boolean isVirtual() {
	return android.os.Build.FINGERPRINT.contains("generic") ||
	    android.os.Build.PRODUCT.contains("sdk");
    }

}
