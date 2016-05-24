package com.example.m.divis;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
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
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
  
import android.widget.TextView;

import java.io.InputStream;

public class FragmentData extends Fragment {
	private static final String TAG = "DIVISFragmentData";
	SharedPreferences sharedPrefs;

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
	class JpegCallback implements Camera.PictureCallback {

		@Override
		public void onPictureTaken(byte[] jpeg, Camera camera) {
			Log.d(TAG, "onPictureTaken (Jpeg)");
			Bitmap bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);

			int w = bmp.getWidth();
			int h = bmp.getHeight();

			Point upperCenter = new Point(
					sharedPrefs.getInt(getString(R.string.saved_upper_x), 100),
					sharedPrefs.getInt(getString(R.string.saved_upper_y), 100));
			int upperRadius = sharedPrefs.getInt(getString(R.string.saved_upper_radius), 100);

			Point lowerCenter = new Point(
					sharedPrefs.getInt(getString(R.string.saved_lower_x), 300),
					sharedPrefs.getInt(getString(R.string.saved_lower_y), 100));
			int lowerRadius = sharedPrefs.getInt(getString(R.string.saved_lower_radius), 100);

			int upperLive = 0;
			int upperWashed = 0;
			int upperData = 0;
			int upperRTotal = 0;
			int upperGTotal = 0;
			int upperBTotal = 0;
			int upperRAvg = 0;
			int upperGAvg = 0;
			int upperBAvg = 0;

			int lowerLive = 0;
			int lowerWashed = 0;
			int lowerData = 0;
			int lowerRTotal = 0;
			int lowerGTotal = 0;
			int lowerBTotal = 0;
			int lowerRAvg = 0;
			int lowerGAvg = 0;
			int lowerBAvg = 0;

			for(int i=0; i<h; i++) {
				for(int j=0; j<w; j++) {
					if(pixelWithinArea(upperCenter, upperRadius, new Point(j, i))) {
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

					if(pixelWithinArea(lowerCenter, lowerRadius, new Point(j, i))) {
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

			upperRAvg = upperRTotal / upperData;
			upperGAvg = upperGTotal / upperData;
			upperBAvg = upperBTotal / upperData;

			lowerRAvg = lowerRTotal / lowerData;
			lowerGAvg = lowerGTotal / lowerData;
			lowerBAvg = lowerBTotal / lowerData;

			Time now = new Time();
			now.setToNow();
			String sTime = now.format("%Y_%m_%d_%H_%M_%S");

			mTimestamp.setText(sTime);
//			mSecciDepth.setText();
			mLivePixelsUpper.setText(Integer.toString(upperLive));
			mWashedPixelsUpper.setText(Integer.toString(upperWashed));
			mDataPixelsUpper.setText(Integer.toString(upperData));
			mAvgRUpper.setText(Integer.toString(upperRAvg));
			mAvgGUpper.setText(Integer.toString(upperGAvg));
			mAvgBUpper.setText(Integer.toString(upperBAvg));

			mLivePixelsLower.setText(Integer.toString(lowerLive));
			mWashedPixelsLower.setText(Integer.toString(lowerWashed));
			mDataPixelsLower.setText(Integer.toString(lowerData));
			mAvgRLower.setText(Integer.toString(lowerRAvg));
			mAvgGLower.setText(Integer.toString(lowerGAvg));
			mAvgBLower.setText(Integer.toString(lowerBAvg));

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
			if(act.mCamera != null && act.mViewPager.getCurrentItem() == 2) {
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
		sharedPrefs = getActivity().getPreferences(Context.MODE_PRIVATE);
		View v = inflater.inflate(R.layout.fragment_data, container, false);

		mTimestamp = (TextView)v.findViewById(R.id.timestamp);          
		mSecciDepth = (TextView)v.findViewById(R.id.secci_depth);        
		mLivePixelsUpper = (TextView)v.findViewById(R.id.live_pixels_upper);  
		mLivePixelsLower = (TextView)v.findViewById(R.id.live_pixels_lower);  
		mWashedPixelsUpper = (TextView)v.findViewById(R.id.washed_pixels_upper);
		mWashedPixelsLower = (TextView)v.findViewById(R.id.washed_pixels_lower);
		mDataPixelsUpper = (TextView)v.findViewById(R.id.data_pixels_upper);  
		mDataPixelsLower = (TextView)v.findViewById(R.id.data_pixels_lower);  
		mAvgRUpper = (TextView)v.findViewById(R.id.avg_r_upper);        
		mAvgRLower = (TextView)v.findViewById(R.id.avg_r_lower);        
		mAvgGUpper = (TextView)v.findViewById(R.id.avg_g_upper);        
		mAvgGLower = (TextView)v.findViewById(R.id.avg_g_lower);        
		mAvgBUpper = (TextView)v.findViewById(R.id.avg_b_upper);        
		mAvgBLower = (TextView)v.findViewById(R.id.avg_b_lower);        
		
		return v;
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

	boolean pixelIsLive(int c)
	{
		return (Color.red(c) > 1 && Color.green(c) > 1 && Color.blue(c) > 1);
	}

	boolean pixelIsWashed(int c)
	{
		return (Color.red(c) > 254 && Color.green(c) > 254 && Color.blue(c) > 254);
	}

	boolean pixelIsData(int c)
	{
		return pixelIsLive(c) && !pixelIsWashed(c);
	}

	boolean pixelWithinArea(Point center, int radius, Point px)
	{
		int dx = center.x - px.x;
		int dy = center.y - px.y;
		int dist = (int)Math.floor(Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2)));
		if(dist > radius)
			return false;
		return true;
	}
}
