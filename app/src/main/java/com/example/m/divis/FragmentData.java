package com.example.m.divis;

import android.hardware.Camera;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
   
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
  
import android.widget.TextView;

public class FragmentData extends Fragment {
	private static final String TAG = "DIVISFragmentData";

	private RawCallback mRawCallback;
	class RawCallback implements Camera.ShutterCallback, Camera.PictureCallback {

		@Override
		public void onShutter() {
			Log.d(TAG, "onShutter");
			// notify the user, normally with a sound, that the picture has 
			// been taken
		}

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			Log.d(TAG, "onPictureTaken (Raw)");
			// manipulate uncompressed image data
		}
	}

	private JpegCallback mJpegCallback;
	class JpegCallback implements Camera.PictureCallback {

		@Override
		public void onPictureTaken(byte[] jpeg, Camera camera) {
			Log.d(TAG, "onPictureTaken (Jpeg)");
			// takePicture has finished, now safe to resume the preview
			MainActivity act = (MainActivity)getActivity();
			act.mCamera.startPreview();
		}
	}

	// capture timer
	long timerInterval = 1000;
	Handler timerHandler = new Handler();
	Runnable timerRunnable = new Runnable() {
		@Override
		public void run() {
			MainActivity act = (MainActivity)getActivity();
			if(act.mCamera != null) {
				SurfaceView preview = ((FragmentCalibrate)act.mSectionsPagerAdapter.getItem(1)).mPreview;
				// callbacks: shutter, raw, post view, jpeg
				Log.d(TAG, "Taking a picture!");
				act.mCamera.takePicture(mRawCallback, mRawCallback, null, mJpegCallback);
			}
			timerHandler.postDelayed(this, timerInterval);
		}
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mRawCallback = new RawCallback();
		mJpegCallback = new JpegCallback();
		return inflater.inflate(R.layout.fragment_data, container, false);
	}

	@Override
	public void onPause() {
		super.onPause();
		timerHandler.removeCallbacks(timerRunnable);
	}

	@Override
	public void onResume() {
		super.onResume();
		setupTimer();
	}

	void setupTimer()
	{
		timerHandler.postDelayed(timerRunnable, 1000);
	}
}
