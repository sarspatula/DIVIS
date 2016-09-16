package com.example.m.divis;

import android.Manifest;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "DIVISMainActivity";
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 42;

    private SharedPreferences sharedPrefs;

    // custom font Calibri.ttf
    public static Typeface typeface;// = Typeface.createFromAsset(getContext().getAssets(), "fonts/Calibri.ttf");

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    public SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    public MyViewPager mViewPager;

    // Native camera.
    public Camera mCamera;
    Camera.Parameters params;
    // size of image captured by camera, set to largest available
    private Camera.Size mCaptureSize;
    // adjusted by device orientation
    public int mCaptureWidth;
    public int mCaptureHeight;
    private int mCameraRotationDegrees;
    public int mCameraRotation;

    private static int index_of_back_camera = 0;
    private boolean isActivityActive;
    private long DATA_SHIFT_DURATION=3000;
    private long ACTIVITY_RESET_DURATION=10000;
//    private long DATA_SHIFT_DURATION=10000;
//    private long ACTIVITY_RESET_DURATION=3600000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e(TAG,"LifeCycle onCreate");
        sharedPrefs = getPreferences(Context.MODE_PRIVATE);
        isActivityActive=true;
        // for hiding titlebar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        // Remember that you should never show the action bar if the
        // status bar is hidden, so hide that too if necessary.
        ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.hide();

        super.onCreate(savedInstanceState);
//        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        // for hiding titlebar
        View root = findViewById(R.id.main_content);
        root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        typeface = Typeface.createFromAsset(getAssets(), "fonts/Calibri.ttf");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (MyViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setCurrentItem(1, false);

        // hide keyboard
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                final InputMethodManager imm = (InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mViewPager.getWindowToken(), 0);
            }

            @Override
            public void onPageScrolled(int position, float offset, int offsetPixels) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    if (mViewPager.getCurrentItem() == 0) {
                        // Hide the keyboard.
                        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                                .hideSoftInputFromWindow(mViewPager.getWindowToken(), 0);
                    }
                }
            }
        });

        // prevent destruction of other fragments
        mViewPager.setOffscreenPageLimit(2);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        setupCamera();
//        setTimerRamCleaner();

       /* if(((MainActivity)getActivity()).getLogToCSV()){
            if (((MainActivity)getActivity()).mViewPager != null) {
                ((MainActivity)getActivity()).mViewPager.setCurrentItem(2);
            }
        }*/

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 100ms
                if (isActivityActive) {
                    freeMemory();
//                    mCamera.stopPreview();
//                                MainActivity.this.onRestart();// Such as "sendEmail()"
                    MainActivity.this.recreate();
                  /*  Intent newIntent = new Intent(MainActivity.this, MainActivity.class);

                    startActivity(newIntent);
                    finish();*/
                    Log.e("TimerTask","Activity reseted");
                }
            }
        }, ACTIVITY_RESET_DURATION);


        final Handler handlerData = new Handler();
        handlerData.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 100ms
                if (isActivityActive) {
                    if(getLogToCSV()){
                        if (mViewPager != null) {
                           mViewPager.setCurrentItem(2,true);
                        }
                    }
                }
            }
        }, DATA_SHIFT_DURATION);
    }

    private void setTimerRamCleaner() {




        ScheduledExecutorService scheduler = Executors
                .newScheduledThreadPool(1);

        scheduler.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                if (getLogToCSV()) {
                    try {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                              /*  if (mCamera != null) {
                                    mCamera.stopPreview();
                                    mCamera.release();
                                    mCamera = null;
                                }*/

                                freeMemory();

//                                MainActivity.this.onRestart();// Such as "sendEmail()"
                                Intent newIntent = new Intent(MainActivity.this, MainActivity.class);

                                startActivity(newIntent);

                                Log.e("TimerTask","Activity reseted");
                            }
                        });
                        // call service
                        } catch (Exception e) {
                        e.printStackTrace(); // Or better, use next line if you have configured a logger:
                        Log.e("TimerTask","Exception in scheduled task");
                    }
                }
            }
        }, 30, 30, TimeUnit.SECONDS);


       /* ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate
                (new Runnable() {
                    public void run() {
                        // call service
                        freeMemory();
                        MainActivity.this.recreate();
                    }
                }, 5, 20, TimeUnit.SECONDS);*/
    }

    public boolean getLogToCSV(){
        return sharedPrefs.getBoolean(getString(R.string.mLoggingToCSV),false);
    }

    private void freeMemory() {
      /*  System.runFinalization();
        Runtime.getRuntime().gc();
        System.gc();*/

        Log.e("Memory", "freeMemory executed...");
    }

    private boolean setupCamera() {
        boolean opened = false;
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            opened = safeCameraOpen();
            if (!opened) {
                Log.d(TAG, "Error, Camera failed to open");
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        }
        return opened;
    }

    @Override
    protected void onPause() {
        isActivityActive=false;

        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                Log.d(TAG, "MY_PERMISSIONS_REQUEST_CAMERA");
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.d(TAG, "MY_PERMISSIONS_REQUEST_CAMERA 2");
                    if (safeCameraOpen()) {
                        Log.d(TAG, "=== PERMISSION GRANTED, SETTING UP UI!");
                        recreate();
                    } else {
                        Log.d(TAG, "Error, failed to open Camera");
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }
        }
    }

    private boolean safeCameraOpen() {
        boolean qOpened = false;
//		releaseCameraAndPreview();
        mCamera = getCameraInstance();
        qOpened = (mCamera != null);

        Log.d(TAG, "INFO, qOpened==" + qOpened);

        if (qOpened) {
            // disable shutter sound
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(index_of_back_camera, info);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                if (info.canDisableShutterSound) {
                    mCamera.enableShutterSound(false);
                }
            }
            // determine capture size
            if (mCamera != null) {
                if (mCamera.getParameters() != null) {
                    params = mCamera.getParameters();
                }
            }
            List<Camera.Size> capture_sizes = params.getSupportedPictureSizes();
            int savedResolutionIndex = sharedPrefs.getInt(
                    getString(R.string.saved_camera_resolution_index),
                    Integer.parseInt(getString(R.string.saved_camera_resolution_index_default)));
            if (savedResolutionIndex > capture_sizes.size() - 1) {
//                savedResolutionIndex = capture_sizes.size() - 1;
                savedResolutionIndex = 1;
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putInt(
                        getString(R.string.saved_camera_resolution_index),
                        savedResolutionIndex);
                editor.commit();
            }
            mCaptureSize = capture_sizes.get(savedResolutionIndex);

            // adjust by orientation
            Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            mCameraRotationDegrees = 0;
            switch (display.getRotation()) {
                case Surface.ROTATION_0:
                    mCaptureWidth = mCaptureSize.height;
                    mCaptureHeight = mCaptureSize.width;
                    break;
                case Surface.ROTATION_180:
                    mCameraRotationDegrees = 180;
                    mCaptureWidth = mCaptureSize.height;
                    mCaptureHeight = mCaptureSize.width;
                    break;
                case Surface.ROTATION_90:
                    mCameraRotationDegrees = 90;
                    mCaptureWidth = mCaptureSize.width;
                    mCaptureHeight = mCaptureSize.height;
                    break;
                case Surface.ROTATION_270:
                    mCameraRotationDegrees = 270;
                    mCaptureWidth = mCaptureSize.width;
                    mCaptureHeight = mCaptureSize.height;
                    break;
            }
            //mCameraRotation = info.orientation;
            mCameraRotation = (info.orientation - mCameraRotationDegrees + 360) % 360;
//			params.setRotation(mCameraRotation); 
            Log.d(TAG, "Camera Rotation: orientation=" + info.orientation + ", Rot degrees=" + mCameraRotationDegrees);

            params.setPictureSize(mCaptureSize.width, mCaptureSize.height);

            Log.d(TAG, "Capture Size Set To " + mCaptureSize.width + "x" + mCaptureSize.height);
            Log.d(TAG, "Capture Size (rot): " + mCaptureWidth + "x" + mCaptureHeight);

            // capture settings
            // Configure image format. RGB_565 is the most common format.
            List<Integer> formats = params.getSupportedPictureFormats();
            // TODO: optimization: use raw rather than decompress jpeg, if available
//			if (formats.contains(PixelFormat.RGB_565)) {
//				Log.d(TAG, "Picture format: RGB_565");
//				params.setPictureFormat(PixelFormat.RGB_565);
//			} else {
//				Log.d(TAG, "Picture format: JPEG");
//				params.setPictureFormat(PixelFormat.JPEG);
//			}
            if (formats.contains(PixelFormat.JPEG))
                params.setPictureFormat(PixelFormat.JPEG);
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            else if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_FIXED))
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
            mCamera.setParameters(params);
        }
        return qOpened;
    }

    /**
     * Safe method for getting a camera instance.
     *
     * @return
     */
    private Camera getCameraInstance() {
        Camera c = null;
        Log.d(TAG, "INFO, <getCameraInstance>");
        try {
            int n = Camera.getNumberOfCameras();
            // choose back facing camera
            if (n > 0) {
                index_of_back_camera = 0;
                Camera.CameraInfo info = new Camera.CameraInfo();
                for (int i = 0; i < n - 1; i++) {
                    Camera.getCameraInfo(i, info);
                    if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                        index_of_back_camera = i;
                        break;
                    }
                }
                releaseCameraAndPreview();
                c = Camera.open(index_of_back_camera);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "INFO, </getCameraInstance> " + c);
        return c;
    }

    private void releaseCameraAndPreview() {

        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onDestroy() {
        Log.e(TAG,"LifeCycle MainActivity onDestroy");
       /* if (mCamera != null) {

            mCamera.stopPreview();
            mCamera = null;
        }*/
        releaseCameraAndPreview();
        super.onDestroy();

    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new FragmentSettings();
                case 1:
                    return new FragmentCalibrate();
                case 2:
                    return new FragmentData();
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_settings).toUpperCase(l);
                case 1:
                    return getString(R.string.title_calibrate).toUpperCase(l);
                case 2:
                    return getString(R.string.title_data).toUpperCase(l);
            }
            return null;
        }
    }
}
