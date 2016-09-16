package com.example.m.divis;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.crashlytics.android.Crashlytics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.graphics.Bitmap.createBitmap;

public class FragmentData extends Fragment {
    private static final String TAG = "DIVISFragmentData";
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private SharedPreferences sharedPrefs;
    private MainActivity mActivity;
//test comment added
    // text views
    private TextView mTimestamp;
    private TextView mSecciDepth;
    private TextView mLivePixelsUpper;
    private TextView mLivePixelsLower;
    private TextView mWashedPixelsUpper;
    private TextView mWashedPixelsLower;
    private TextView mDataPixelsUpper;
    private TextView mDataPixelsLower;
    private TextView mAvgRUpper;
    private TextView mAvgRLower;
    private TextView mAvgGUpper;
    private TextView mAvgGLower;
    private TextView mAvgBUpper;
    private TextView mAvgBLower;

    private ToggleButton mButtonSave;
    private TextView mCounter;

    // preview capture
    //private ImageView mImagePreview;
//    private Bitmap mLastBitmap = null;


    //remove to re-run logging after app resume from background
//    private static boolean mLoggingToCSV;
    private boolean mLoggingToCSV;
    private int mCount;

    // data
    private int upperLive = 0;
    private int upperWashed = 0;
    private int upperData = 0;
    private int upperRTotal = 0;
    private int upperGTotal = 0;
    private int upperBTotal = 0;
    private int upperRAvg = 0;
    private int upperGAvg = 0;
    private int upperBAvg = 0;

    private int lowerLive = 0;
    private int lowerWashed = 0;
    private int lowerData = 0;
    private int lowerRTotal = 0;
    private int lowerGTotal = 0;
    private int lowerBTotal = 0;
    private int lowerRAvg = 0;
    private int lowerGAvg = 0;
    private int lowerBAvg = 0;

    String sTime;
    Bitmap imageCaptured;
    // TODO: optimization: use raw instead of jpeg when available


    File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
    private int upperCenterX;
    private int upperCenterY;
    private int upperRadius;
    private int lowerCenterX;
    private int lowerCenterY;
    private int lowerRadius;
    // CSV file delimeter
    String seperator = ",";

    private class RawCallback implements Camera.ShutterCallback, Camera.PictureCallback {

        @Override
        public void onShutter() {
            Log.d(TAG, "onShutter");
            // notify the user, normally with a sound, that the picture has
            // been taken
        }

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            /*
            Log.d(TAG, "onPictureTaken (Raw)");
			if(data == null) {
				Log.d(TAG, "ERROR, Raw picture data is not available!");
			}
			// manipulate uncompressed image data
			*/
        }
    }

    private JpegCallback mJpegCallback;

    private class JpegCallback implements Camera.PictureCallback {

        @Override
        public void onPictureTaken(byte[] jpeg, Camera camera) {
            try {
                Log.d(TAG, "onPictureTaken (Jpeg)");

                Thread t = new Thread(new AnalyzerTask(jpeg));
                t.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class AnalyzerTask implements Runnable {
        byte[] jpeg;

        AnalyzerTask(byte[] jpeg) {
            this.jpeg = jpeg;
        }

        public void run() {
            try {
                if (!isAdded())
                    return;
                try {
                    analyzeImage(jpeg);
                } catch (OutOfMemoryError ex) {
                    ex.printStackTrace();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                getMemoryStatastics();
                getBatteryInfo();

                if (getLogToCSV()) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!isAdded())
                                return;
                            writeToCsv();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void getBatteryInfo() {
        if (getActivity() != null) {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = getActivity().registerReceiver(null, ifilter);
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            float batteryPct = ((float) level / (float) scale) * 100.0f;
            Log.e("Memory", "Battery percentage=" + (int) batteryPct);

            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString(getString(R.string.battery_level),
                    formatBattery((int) batteryPct));
            editor.commit();
        }


    }

    private String formatBattery(int batteryPct) {
        return batteryPct + "%";
    }

    private long getMemoryStatastics() {

        long freeHeapSize = 0L;
        long currentHeapSizeAvailable = 0L;
        long usedHeapSize = -1L;
        try {
            Runtime info = Runtime.getRuntime();
            freeHeapSize = info.freeMemory();
            currentHeapSizeAvailable = info.totalMemory();
            usedHeapSize = currentHeapSizeAvailable - freeHeapSize;
            long maxHeapExpandable = info.maxMemory();
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString(getString(R.string.total_memory),
                    formatMemory(maxHeapExpandable));

            editor.putString(getString(R.string.used_memory),
                    formatMemory(usedHeapSize));
            editor.commit();
            Log.e("Memory", "\ntotalSize=" + formatMemory(currentHeapSizeAvailable) + ",\nmaxHeapExpandable=" + formatMemory(maxHeapExpandable) + ",\nfreeSize=" + formatMemory(freeHeapSize) + ",\nusedSize=" + formatMemory(usedHeapSize));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return usedHeapSize;

    }


    private String formatMemory(long totalSize) {
        long memMB = totalSize / 1048576L;
        long memKB = (totalSize - (memMB * 1048576L)) / 1024L;
        return "" + memMB + "MB " + memKB + "KB";
    }

    private void updateUi() {
        if (isAdded()) {
            /*if(mLastBitmap != null) {
                mImagePreview.setImageBitmap(mLastBitmap);
			}*/
            mTimestamp.setText(sTime);
            //			mSecciDepth.setText();
            mLivePixelsUpper.setText(String.valueOf(upperLive));
            mWashedPixelsUpper.setText(String.valueOf(upperWashed));
            mDataPixelsUpper.setText(String.valueOf(upperData));
            mAvgRUpper.setText(String.valueOf(upperRAvg));
            mAvgGUpper.setText(String.valueOf(upperGAvg));
            mAvgBUpper.setText(String.valueOf(upperBAvg));

            mLivePixelsLower.setText(String.valueOf(lowerLive));
            mWashedPixelsLower.setText(String.valueOf(lowerWashed));
            mDataPixelsLower.setText(String.valueOf(lowerData));
            mAvgRLower.setText(String.valueOf(lowerRAvg));
            mAvgGLower.setText(String.valueOf(lowerGAvg));
            mAvgBLower.setText(String.valueOf(lowerBAvg));
        }
    }

    private Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.postRotate(degree);

        return createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

    // run in separate thread
    private void analyzeImage(byte[] jpeg) {

        setDefaultValues();
       /* Point upperCenter = new Point(
                sharedPrefs.getInt(getString(R.string.saved_upper_x), 100),
                sharedPrefs.getInt(getString(R.string.saved_upper_y), 100));*/
        upperCenterX = sharedPrefs.getInt(getString(R.string.saved_upper_x), 100);
        upperCenterY = sharedPrefs.getInt(getString(R.string.saved_upper_y), 100);
        upperRadius = sharedPrefs.getInt(getString(R.string.saved_upper_radius), 100);

       /* Point lowerCenter = new Point(
                sharedPrefs.getInt(getString(R.string.saved_lower_x), 300),
                sharedPrefs.getInt(getString(R.string.saved_lower_y), 100));*/
        lowerCenterX = sharedPrefs.getInt(getString(R.string.saved_lower_x), 300);
        lowerCenterY = sharedPrefs.getInt(getString(R.string.saved_lower_y), 100);
        lowerRadius = sharedPrefs.getInt(getString(R.string.saved_lower_radius), 100);

        // Bugfix: not rotated to match preview
        imageCaptured = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
//        int angleToRotate = mActivity.mCameraRotation;
        //Bitmap bmp = rotate(bmpPreRotate, angleToRotate);
        imageCaptured = rotate(imageCaptured, mActivity.mCameraRotation);

        saveUpperData(upperRadius, upperCenterX, upperCenterY);
        saveLowerData(lowerRadius, lowerCenterX, lowerCenterY);
//        int upper_radius_squared = upperRadius * upperRadius;
//        int lower_radius_squared = lowerRadius * lowerRadius;
        freeBitmapResource(imageCaptured);

        Time now = new Time();
        now.setToNow();
        Log.e(TAG, now.format("%Y_%m_%d_%H_%M_%S"));
        Log.d("Image Length", "Image Length=====>" + jpeg.length);

	/*	int w = bmp.getWidth();
        int h = bmp.getHeight();*/





       /* for (int i = minX; i < maxX; i++) {
            for (int j = minY; j < maxY; j++) {

                int c = imageCaptured.getPixel(i, j);
                if (pixelIsLive(c)) {
                    upperLive++;
                    if (!pixelIsWashed(c)) {
                        upperData++;
                        upperRTotal += Color.red(c);
                        upperGTotal += Color.green(c);
                        upperBTotal += Color.blue(c);
                    } else {
                        upperWashed++;
                    }

                }
				*//*if(pixelIsWashed(c))
					upperWashed++;
				if(pixelIsData(c)) {
					upperData++;
					upperRTotal += Color.red(c);
					upperGTotal += Color.green(c);
					upperBTotal += Color.blue(c);
				}*//*
            }
        }
*/

      /*  for (int i = minX1; i < maxX1; i++) {
            for (int j = minY1; j < maxY1; j++) {
                int c = imageCaptured.getPixel(i, j);
                if (pixelIsLive(c)) {
                    lowerLive++;

                    if (!pixelIsWashed(c)) {
                        lowerData++;
                        lowerRTotal += Color.red(c);
                        lowerGTotal += Color.green(c);
                        lowerBTotal += Color.blue(c);
                    } else
                        lowerWashed++;
                }
            }
        }*/


		/*for(int i=0; i<h; i++) {
			for(int j=0; j<w; j++) {

				if(pixelWithinArea(upperCenter, upperRadius, upper_radius_squared, new Point(j, i))) {
					Log.d(TAG, "pixelWithinArea: " + j + "x" + i);
					int c = bmp.getPixel(j, i);
					if(pixelIsLive(c))
						upperLive++;
					if(pixelIsWashed(c))
						upperWashed++;
					if(pixelIsData(c)) {
						upperData++;
						upperRTotal += Color.red(c);
						upperGTotal += Color.green(c);
						upperBTotal += Color.blue(c);
					}
				}

				if(pixelWithinArea(lowerCenter, lowerRadius, lower_radius_squared, new Point(j, i))) {
					int c = bmp.getPixel(j, i);
					if(pixelIsLive(c))
						lowerLive++;
					if(pixelIsWashed(c))
						lowerWashed++;
					if(pixelIsData(c)) {
						lowerData++;
						lowerRTotal += Color.red(c);
						lowerGTotal += Color.green(c);
						lowerBTotal += Color.blue(c);
					}
				}
			}
		}
*/
        if (upperData > 0) {
            upperRAvg = upperRTotal / upperData;
            upperGAvg = upperGTotal / upperData;
            upperBAvg = upperBTotal / upperData;
        }

        if (lowerData > 0) {
            lowerRAvg = lowerRTotal / lowerData;
            lowerGAvg = lowerGTotal / lowerData;
            lowerBAvg = lowerBTotal / lowerData;
        }


        now.setToNow();
        Log.e("updateUi====>", "updateUi===>" + now.format("%Y_%m_%d_%H_%M_%S"));
        sTime = now.format("%Y_%m_%d_%H_%M_%S");

//        mLastBitmap = bmp;

        Log.e("PixelsRange", "upperRAvg=" + upperRAvg+" ,lowerRAvg="+lowerRAvg+"\n upperGAvg"+upperGAvg+" ,lowerGAvg="+lowerGAvg+"\n ,upperBAvg="+upperBAvg+" ,lowerBAvg"+lowerBAvg);
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
				/*if(!isAdded())
					return;*/
                updateUi();
            }
        });

        // takePicture has finished, now safe to resume the preview
        // NOTE: preview must be started before takePicture
        // NOTE: must be called after this function returns
//		mActivity.mCamera.startPreview();
        previewHandler.postDelayed(previewRunnable, 1);

        // schedule next timer
        timerHandler.postDelayed(timerRunnable, 500);

    }

    private void saveLowerData(int lowerRadius, int lowerCenterX, int lowerCenterY) {
        int minX1 = lowerCenterX - lowerRadius;
        int maxX1 = lowerCenterX + lowerRadius;
        int minY1 = lowerCenterY - lowerRadius;
        int maxY1 = lowerCenterY + lowerRadius;
        Log.e("PixelsRange", "Lower W*H===>" + (maxX1 - minX1) * (maxY1 - minY1) + "\n maxX1::" + maxX1 + " ,minX1::" + minX1 + " ,maxY1::" + maxY1 + " ,minY1::" + minY1);

//        Bitmap bitmapLower = Bitmap.createBitmap(imageCaptured, minX1, minY1, (maxX1 - minX1), (maxY1 - minY1));
//        Bitmap bitmapLower = imageCaptured;
//        Log.e("PixelsRange", "bitmapLower width::" + bitmapLower.getWidth() + " ,height::" + bitmapLower.getHeight());
        getLowerImageData(minX1, maxX1, minY1, maxY1,imageCaptured);
//        getLowerImageData(0, bitmapLower.getWidth(), 0, bitmapLower.getHeight(),bitmapLower);
//        freeBitmapResource(bitmapLower);
//        freeBitmapResource(imageCaptured);
    }

    private void saveUpperData(int upperRadius, int upperCenterX, int upperCenterY) {
        int minX = upperCenterX - upperRadius;
        int maxX = upperCenterX + upperRadius;
        int minY = upperCenterY - upperRadius;
        int maxY = upperCenterY + upperRadius;
        Log.e("PixelsRange", "Upper W*H===>" + (maxX - minX) * (maxY - minY) + "\n maxX::" + maxX + " ,minX::" + minX + " ,maxY::" + maxY + " ,minY::" + minY);

//        Bitmap bitmapUpper = imageCaptured;
//        Bitmap bitmapUpper = Bitmap.createBitmap(imageCaptured, minX, minY, (maxX - minX), (maxY - minY));
//        Log.e("PixelsRange", "bitmapUpper width::" + bitmapUpper.getWidth() + " ,height::" + bitmapUpper.getHeight());
        getUpperImageData(minX, maxX, minY, maxY,imageCaptured);
//        getUpperImageData(0, bitmapUpper.getWidth(), 0, bitmapUpper.getHeight(),bitmapUpper);
//        freeBitmapResource(bitmapUpper);
    }

    private void setDefaultValues() {
        upperLive = 0;
        upperWashed = 0;
        upperData = 0;
        upperRTotal = 0;
        upperGTotal = 0;
        upperBTotal = 0;
        upperRAvg = 0;
        upperGAvg = 0;
        upperBAvg = 0;

        lowerLive = 0;
        lowerWashed = 0;
        lowerData = 0;
        lowerRTotal = 0;
        lowerGTotal = 0;
        lowerBTotal = 0;
        lowerRAvg = 0;
        lowerGAvg = 0;
        lowerBAvg = 0;
    }

    private void getLowerImageData(int minX1, int maxX1, int minY1, int maxY1, Bitmap bitmapLower) {
        for (int i = minX1; i < maxX1; i++) {
            for (int j = minY1; j < maxY1; j++) {
                int c = bitmapLower.getPixel(i, j);
                if (pixelIsLive(c)) {
                    lowerLive++;

                    if (!pixelIsWashed(c)) {
                        lowerData++;
                        lowerRTotal += Color.red(c);
                        lowerGTotal += Color.green(c);
                        lowerBTotal += Color.blue(c);
                    } else
                        lowerWashed++;
                }
            }
        }
    }

    private void getUpperImageData(int minX, int maxX, int minY, int maxY, Bitmap bitmapUpper) {
        for (int i = minX; i < maxX; i++) {
            for (int j = minY; j < maxY; j++) {

                int c = bitmapUpper.getPixel(i, j);
                if (pixelIsLive(c)) {
                    upperLive++;
                    if (!pixelIsWashed(c)) {
                        upperData++;
                        upperRTotal += Color.red(c);
                        upperGTotal += Color.green(c);
                        upperBTotal += Color.blue(c);
                    } else {
                        upperWashed++;
                    }
                }
				/*if(pixelIsWashed(c))
					upperWashed++;
				if(pixelIsData(c)) {
					upperData++;
					upperRTotal += Color.red(c);
					upperGTotal += Color.green(c);
					upperBTotal += Color.blue(c);
				}*/
            }
        }

    }

    private void freeBitmapResource(Bitmap bitmapNotUsed) {
        if (bitmapNotUsed != null) {
            bitmapNotUsed.recycle();
        }
    }

    private boolean accessExternalStorage() {
        int permissionCheck = ContextCompat.checkSelfPermission(mActivity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission already granted!");
            return true;
        } else {
            Log.d(TAG, "Requesting");
            requestPermissions(
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    writeToCsv();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }

        }
    }

    private String getS(int id) {
        return sharedPrefs.getString(getString(id), "");
    }

    private int getI(int id) {
        return sharedPrefs.getInt(getString(id), 0);
    }

    private void doWriteToCsv() {
        Log.d(TAG, "Writing to csv");


        if (!dir.exists()) {
            if (!dir.mkdir()) {
                Log.d(TAG, "Target directory cannot be created : " + dir.toString());
                return;
            }
        }
//      new code to make file hourly bases
        makeNewFileName();

//        Old code to write in old file
//        AppendDataToFile("DIVIS.csv");
    }

    private void AppendDataToFile(String filename) {
        File file = new File(dir, filename);
        boolean write_header = !file.exists();
//        if(write_header){file.mkdir();}
        //Write to file
        try {
            FileWriter fileWriter = new FileWriter(file, true);
            String file_header = "# device_ID, divis_ID, app_version, location_ID, location_Name, location_Detail, upper_Sensor_Depth, lower_Sensor_Depth, date_time, secci_depth, live_pixels_upper, washed_pixels_upper, data_pixels_upper, avg_r_upper, avg_g_upper, avg_b_upper, live_pixels_lower, washed_pixels_lower, data_pixels_lower, avg_r_lower, avg_g_lower, avg_b_lower, camera_exposure, min_rgb_for_live_pixel, upper_x, upper_y, upper_size, lower_x, lower_y, lower_size, total_memory, used_memory, battery_level\n";
            if (write_header) {
                fileWriter.append(file_header);
                mCount = 0;
                updateCountSP(mCount);
            } else {
                changeHeader(file_header, filename);
            }

            appendToWriter(fileWriter);
//            logToFabric();
            fileWriter.flush();
            fileWriter.close();
//			Toast.makeText(mActivity, getString(R.string.msg_csv_written),
//					Toast.LENGTH_SHORT).show();
        } catch (java.io.IOException e) {
            Log.e(TAG, e.toString());
            Toast.makeText(mActivity, e.toString(), Toast.LENGTH_LONG).show();
            //Handle exception
        }
    }

    private void appendToWriter(FileWriter fileWriter) {
        try {
            fileWriter.append(
                    getS(R.string.saved_device_id) + seperator +
                            getS(R.string.saved_divis_id) + seperator +
                            getS(R.string.saved_app_version) + seperator +
                            getS(R.string.saved_location_id) + seperator +
                            getS(R.string.saved_location_name) + seperator +
                            getS(R.string.saved_location_detail) + seperator +
                            getI(R.string.saved_upper_sensor_depth) + seperator +
                            getI(R.string.saved_lower_sensor_depth) + seperator +
                            sTime + seperator +
                            getI(R.string.saved_secci_depth) + seperator +
                            String.valueOf(upperLive) + seperator +
                            String.valueOf(upperWashed) + seperator +
                            String.valueOf(upperData) + seperator +
                            String.valueOf(upperRAvg) + seperator +
                            String.valueOf(upperGAvg) + seperator +
                            String.valueOf(upperBAvg) + seperator +
                            String.valueOf(lowerLive) + seperator +
                            String.valueOf(lowerWashed) + seperator +
                            String.valueOf(lowerData) + seperator +
                            String.valueOf(lowerRAvg) + seperator +
                            String.valueOf(lowerGAvg) + seperator +
                            String.valueOf(lowerBAvg) + seperator +
                            getS(R.string.saved_camera_exposure) + seperator +
                            getI(R.string.saved_min_rgb_for_live_pixel) + seperator +
                            getI(R.string.saved_upper_x) + seperator +
                            getI(R.string.saved_upper_y) + seperator +
                            getI(R.string.saved_upper_radius) + seperator +
                            getI(R.string.saved_lower_x) + seperator +
                            getI(R.string.saved_lower_y) + seperator +
                            getI(R.string.saved_lower_radius) + seperator +
                            getS(R.string.total_memory) + seperator +
                            getS(R.string.used_memory) + seperator +
                            getS(R.string.battery_level) +
                            "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void makeNewFileName() {
//        new file on minutes bases
//        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMMdd_HH:mm");

        //new file on hourly bases
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMMdd_HH");
        Date now = new Date();
        String fileName = "DIVIS_" + formatter.format(now) + ".CSV";
        AppendDataToFile(fileName);
    }

    private void logToFabric() {

        Crashlytics.setString("saved_device_id", getS(R.string.saved_device_id));
        Crashlytics.setString("saved_app_version", getS(R.string.saved_app_version));
        Crashlytics.setString("saved_location_id", getS(R.string.saved_location_id));
        Crashlytics.setString("saved_location_name", getS(R.string.saved_location_name));
        Crashlytics.setString("saved_location_detail", getS(R.string.saved_location_detail));
        Crashlytics.setString("saved_upper_sensor_depth", "" + getI(R.string.saved_upper_sensor_depth));
        Crashlytics.setString("saved_lower_sensor_depth", "" + getI(R.string.saved_lower_sensor_depth));

        Crashlytics.setString("sTime", sTime);
        Crashlytics.setString("saved_secci_depth", "" + getI(R.string.saved_secci_depth));
        Crashlytics.setString("upperLive", String.valueOf(upperLive));
        Crashlytics.setString("upperWashed", String.valueOf(upperWashed));
        Crashlytics.setString("upperData", String.valueOf(upperData));
        Crashlytics.setString("upperRAvg", String.valueOf(upperRAvg));
        Crashlytics.setString("upperGAvg", String.valueOf(upperGAvg));
        Crashlytics.setString("upperBAvg", String.valueOf(upperBAvg));

        Crashlytics.setString("lowerLive", String.valueOf(lowerLive));
        Crashlytics.setString("lowerWashed", String.valueOf(lowerWashed));
        Crashlytics.setString("lowerData", String.valueOf(lowerData));
        Crashlytics.setString("lowerRAvg", String.valueOf(lowerRAvg));
        Crashlytics.setString("lowerGAvg", String.valueOf(lowerGAvg));
        Crashlytics.setString("lowerBAvg", String.valueOf(lowerBAvg));

        Crashlytics.setString("saved_camera_exposure", getS(R.string.saved_camera_exposure));
        Crashlytics.setString("saved_min_rgb_for_live_pixel", "" + getI(R.string.saved_min_rgb_for_live_pixel));
        Crashlytics.setString("saved_upper_x", "" + getI(R.string.saved_upper_x));
        Crashlytics.setString("saved_upper_y", "" + getI(R.string.saved_upper_y));
        Crashlytics.setString("saved_upper_radius", "" + getI(R.string.saved_upper_radius));
        Crashlytics.setString("saved_lower_x", "" + getI(R.string.saved_lower_x));
        Crashlytics.setString("saved_lower_y", "" + getI(R.string.saved_lower_y));
        Crashlytics.setString("saved_lower_radius", "" + getI(R.string.saved_lower_radius));
        Crashlytics.setString("total_memory", getS(R.string.total_memory));
        Crashlytics.setString("used_memory", getS(R.string.used_memory));
        Crashlytics.setString("battery_level", getS(R.string.battery_level));
    }

    private void changeHeader(String file_header, String filename) {

        File inputFile = new File(dir, filename);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(inputFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        String currentLine;

        try {
            currentLine = reader.readLine() + "\n";
            if (currentLine != null) {
                if (!currentLine.equals(file_header)) {
                    Log.e("CSV", "\nHeader line changed \n Previous=" + currentLine + "\n Current=" + file_header);
                    File tempFile = new File(dir, "myTempFile.csv");
                    BufferedWriter writer = null;
                    try {
                        writer = new BufferedWriter(new FileWriter(tempFile));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                    int lineIndex = 1;
                    while ((currentLine = reader.readLine()) != null) {
                        // trim newline when comparing with lineToRemove
                        if (lineIndex == 1) {
                            if (!currentLine.equals(file_header)) {
                                writer.write(file_header);
                            }
                        } else {
                            writer.write(currentLine + System.getProperty("line.separator"));
                        }
                        ++lineIndex;
                    }
                    writer.flush();
                    writer.close();
                    reader.close();

                    inputFile.delete(); // remove the old file
                    boolean successful = tempFile.renameTo(inputFile);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeToCsv() {
        // no data captured yet
        if (mTimestamp.getText().toString().isEmpty()) {
            Toast.makeText(mActivity, getString(R.string.msg_no_data_yet), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!accessExternalStorage())
            return;

        doWriteToCsv();
    }

    // capture timer
    long timerInterval = 1000;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isAdded())
                return;

            if (mActivity.mViewPager.getCurrentItem() != 2)
                return;
            if (mActivity.mCamera != null) {
                SurfaceView preview = ((FragmentCalibrate) mActivity.mSectionsPagerAdapter.getItem(1)).mPreview;

                // free up memory
                //mImagePreview.setImageResource(android.R.color.transparent);
//                mLastBitmap = null;

                // update counter
                if (getLogToCSV()) {
                    updateCountSP(getCountSP()+1);
                    mCounter.setText(getString(R.string.data_counter_prefix) +
                            String.valueOf(getCountSP()));
                } else {
                    updateCountSP(0);
                    mCounter.setText(getString(R.string.data_counter_zeroed));
                }

                // callbacks: shutter, raw, post view, jpeg

                try {
                        Time now = new Time();
                        now.setToNow();
                        if(mActivity.mCamera == null){
                            Log.e("Before pick taken","camera null");
                        }

                        Log.e("Before pick taken", now.format("%Y_%m_%d_%H_%M_%S"));

                        mActivity.mCamera.takePicture(null, null, null, mJpegCallback);
                        now.setToNow();
                } catch (Exception e) {
                    // "E/Camera: Error 100" and "Camera service died!"
                    // NOTE: fixed by not re-enabling preview until calibrate
                    // screen shown.
                    Log.d(TAG, e.toString());
                }
            }

		/*	else {
                timerHandler.postDelayed(this, timerInterval);
			}*/
        }
    };

    // restart preview
    private Handler previewHandler = new Handler();
    private Runnable previewRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isAdded())
                return;
            // in case surfaceChanged and preview already started?
            try {
                mActivity.mCamera.stopPreview();
            } catch (Exception e) {
                // ignore: tried to stop a non-existent preview
            }

            try {
                mActivity.mCamera.startPreview();
            } catch (Exception e) {
                Log.d(TAG, "ERROR: " + e.toString());
                Toast.makeText(mActivity, "startPreview: " + e.toString(),
                        Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.e(TAG, "LifeCycle onAttach");
        mActivity = (MainActivity) activity;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.e(TAG, "LifeCycle onCreateView");
        RawCallback mRawCallback = new RawCallback();
        mJpegCallback = new JpegCallback();
        if (mActivity == null)
            mActivity = (MainActivity) getActivity();
        sharedPrefs = mActivity.getPreferences(Context.MODE_PRIVATE);
        View v = inflater.inflate(R.layout.fragment_data, container, false);

        mTimestamp = (TextView) v.findViewById(R.id.timestamp);
        mSecciDepth = (EditText) v.findViewById(R.id.secci_depth);
        mLivePixelsUpper = (TextView) v.findViewById(R.id.live_pixels_upper);
        mLivePixelsLower = (TextView) v.findViewById(R.id.live_pixels_lower);
        mWashedPixelsUpper = (TextView) v.findViewById(R.id.washed_pixels_upper);
        mWashedPixelsLower = (TextView) v.findViewById(R.id.washed_pixels_lower);
        mDataPixelsUpper = (TextView) v.findViewById(R.id.data_pixels_upper);
        mDataPixelsLower = (TextView) v.findViewById(R.id.data_pixels_lower);
        mAvgRUpper = (TextView) v.findViewById(R.id.avg_r_upper);
        mAvgRLower = (TextView) v.findViewById(R.id.avg_r_lower);
        mAvgGUpper = (TextView) v.findViewById(R.id.avg_g_upper);
        mAvgGLower = (TextView) v.findViewById(R.id.avg_g_lower);
        mAvgBUpper = (TextView) v.findViewById(R.id.avg_b_upper);
        mAvgBLower = (TextView) v.findViewById(R.id.avg_b_lower);
        //mImagePreview = (ImageView)v.findViewById(R.id.data_preview);
        mCounter = (TextView) v.findViewById(R.id.counter);

        mButtonSave = (ToggleButton) v.findViewById(R.id.btn_write_csv);
        mButtonSave.setChecked(getLogToCSV());
        mButtonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mButtonSave.isChecked()) {
                    if (!mSecciDepth.getText().toString().isEmpty()) {
                        mLoggingToCSV = true;
                        updateLogToCSV(true);

                    } else {
                        mButtonSave.setChecked(false);
                        new AlertDialog.Builder(mActivity)
                                .setTitle("Title")
                                .setMessage(getString(R.string.msg_write_despite_blank_secci))
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        mButtonSave.setChecked(true);
                                        mLoggingToCSV = true;
                                        updateLogToCSV(true);
                                    }
                                })
                                .setNegativeButton(android.R.string.no, null).show();
                    }
                } else {
                    mLoggingToCSV = false;
                    updateLogToCSV(false);
                }
                //writeToCsv();
            }
        });

        mSecciDepth.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    updatePrefs();
                    InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mSecciDepth.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });
        mSecciDepth.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus)
                    updatePrefs();
            }
        });

        // setup timer
        mActivity.mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                timerHandler.removeCallbacks(timerRunnable);
                if (position == 2) {
                    timerHandler.postDelayed(timerRunnable, 1000);
                }
				/*else if(position==1){
					mActivity.setupCamera();
				}
*/
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        // update counter
        if (getLogToCSV()) {
            updateCountSP(getCountSP()+1);
            mCounter.setText(getString(R.string.data_counter_prefix) +
                    String.valueOf(getCountSP()));
        } else {
            updateCountSP(0);
            mCounter.setText(getString(R.string.data_counter_zeroed));
        }
        return v;
    }

    private void updateLogToCSV(Boolean mLoggingToCSV){
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean(getString(R.string.mLoggingToCSV),
                mLoggingToCSV);
        editor.commit();
    }

    private boolean getLogToCSV(){
        return sharedPrefs.getBoolean(getString(R.string.mLoggingToCSV),false);
    }

    private void updateCountSP(Integer mCounter){
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt(getString(R.string.mCounter),
                mCounter);
        editor.commit();
    }

    private int getCountSP(){
        return sharedPrefs.getInt(getString(R.string.mCounter),0);
    }
    private void updatePrefs() {
        Log.d(TAG, "updatePrefs");
        SharedPreferences.Editor editor = sharedPrefs.edit();
        int i = 0;
        String txt = mSecciDepth.getText().toString();
        if (!txt.isEmpty())
            i = Integer.parseInt(txt);
        editor.putInt(getString(R.string.saved_secci_depth), i);
        editor.commit();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e(TAG, "LifeCycle onPause");
        if (isAdded()) {
            previewHandler.removeCallbacks(previewRunnable);
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "LifeCycle onResume");
        final Handler handlerData = new Handler();
        handlerData.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 100ms
                if (isAdded()) {
                    timerHandler.removeCallbacks(timerRunnable);
                    timerHandler.postDelayed(timerRunnable, 1000);
                }
            }
        }, 3000);


    }

    private boolean pixelIsLive(int c) {
        int min = sharedPrefs.getInt("saved_min_rgb_for_live_pixel",
                Integer.parseInt(getString(R.string.saved_min_rgb_for_live_pixel_default)));
        return (Color.red(c) > min && Color.green(c) > min && Color.blue(c) > min);
    }

    private boolean pixelIsWashed(int c) {
        return (Color.red(c) > 254 || Color.green(c) > 254 || Color.blue(c) > 254);
    }

    boolean pixelIsData(int c) {
        return pixelIsLive(c) && !pixelIsWashed(c);
    }


    boolean pixelWithinArea(Point center, int radius, int radius_squared, Point px) {
        // fast check
        if (center.x - radius > px.x || center.x + radius < px.x ||
                center.y - radius > px.y || center.y + radius < px.y)
            return false;

        // slow check
		/*int dx = center.x - px.x;
		int dy = center.y - px.y;
		int dist_squared = (int)Math.floor(Math.pow(dx, 2) + Math.pow(dy, 2));
		if(dist_squared > radius_squared)
			return false;*/
        return true;
    }

    @Override
    public void onDestroy() {
        Log.e(TAG,TAG+"LifeCycle FragmentData onDestroy");
        freeBitmapResource(imageCaptured);

        super.onDestroy();

    }
}
