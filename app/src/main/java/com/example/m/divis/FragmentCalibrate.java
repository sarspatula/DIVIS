package com.example.m.divis;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;

public class FragmentCalibrate extends Fragment {
	private static final String TAG = "DIVISFragmentCalibrate";
	private SharedPreferences sharedPrefs;

	// class encapsulates drawable, draw style, center, and radius
	private Shape mUpperShape = null;
	private Shape mLowerShape = null;

	// View to display the camera output.
	public CameraPreview mPreview;

	// Reference to the containing view.
	private View mCameraView;

	// control shapes will be drawn on this view
	private ImageView mDrawingImageView;
	private Bitmap mDrawingBitmap;

	// controls overlay
	private EditText mEditUpperX;
	private EditText mEditUpperY;
	private EditText mEditUpperSize;
	private EditText mEditLowerX;
	private EditText mEditLowerY;
	private EditText mEditLowerSize;
	private Spinner mCameraExposure;
	private Spinner mCameraResolution;
//	private TextView mCameraPreviewResolution;
	private EditText mMinRGBLevel;

	private Camera mCamera;
	private int mCaptureWidth;
	private int mCaptureHeight;
	public static boolean isVisibleToUser=false;

	class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

		// SurfaceHolder
		private SurfaceHolder mHolder;

		// Our Camera.
		private Camera mCamera;

		// Parent Context.
		private Context mContext;

		// Camera Sizing (For rotation, orientation changes)
		public Camera.Size mPreviewSize;

		// List of supported preview sizes
		private List<Camera.Size> mSupportedPreviewSizes;

		// View holding this camera.
		private View mCameraView;

		public CameraPreview(Context context, Camera camera, View cameraView) {
			super(context);

			// Capture the context
			mCameraView = cameraView;
			mContext = context;
			setCamera(camera);

			// Install a SurfaceHolder.Callback so we get notified when the
			// underlying surface is created and destroyed.
			mHolder = getHolder();
			mHolder.addCallback(this);
			mHolder.setKeepScreenOn(true);
			// deprecated setting, but required on Android versions prior to 3.0
			mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		/**
		 * Begin the preview of the camera input.
		 */

		public void startCameraPreview()
		{
			try{
				mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
				mCamera.setPreviewDisplay(mHolder);
				mCamera.startPreview();
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}

		/**
		 * Extract supported preview and flash modes from the camera.
		 * @param camera
		 */
		private void setCamera(Camera camera)
		{
			// Source: http://stackoverflow.com/questions/7942378/android-camera-will-not-work-startpreview-fails
			mCamera = camera;
			mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
			requestLayout();
		}

		/**
		 * The Surface has been created, now tell the camera where to draw the preview.
		 * @param holder
		 */
		public void surfaceCreated(SurfaceHolder holder) {
			try {
				mCamera.setPreviewDisplay(holder);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Dispose of the camera preview.
		 * @param holder
		 */
		public void surfaceDestroyed(SurfaceHolder holder) {
			if (mCamera != null){

				try {
					mCamera.stopPreview();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}

		/**
		 * React to surface changed events
		 * @param holder
		 * @param format
		 * @param w
		 * @param h
		 */
		public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
			Log.d(TAG, "surfaceChanged");
			// If your preview can change or rotate, take care of those events here.
			// Make sure to stop the preview before resizing or reformatting it.

			if (mHolder.getSurface() == null){
				// preview surface does not exist
				return;
			}

			// stop preview before making changes
			/*try {
				mCamera.stopPreview();
			} catch (Exception e){
				// ignore: tried to stop a non-existent preview
			}*/

			try {
				Camera.Parameters parameters = mCamera.getParameters();

				// Preview size must exist.
				if(mPreviewSize != null) {
					Camera.Size previewSize = mPreviewSize;
					parameters.setPreviewSize(previewSize.width, previewSize.height);
//					mCameraPreviewResolution.setText("" + previewSize.width + "x" + previewSize.height);
				}

				mCamera.setParameters(parameters);
				mCamera.startPreview();
			} catch (Exception e){
				e.printStackTrace();
			}
		}

		/**
		 * Calculate the measurements of the layout
		 * @param widthMeasureSpec
		 * @param heightMeasureSpec
		 */
		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
		{
			// Source: http://stackoverflow.com/questions/7942378/android-camera-will-not-work-startpreview-fails
			final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
			final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
			Log.d(TAG, "Preview View's Size: " + width + "x" + height);
			setMeasuredDimension(width, height);

			if (mSupportedPreviewSizes != null){
				mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
				if(mPreviewSize != null) {
					Log.d(TAG, "Optimal Preview Size: " + mPreviewSize.width + "x" + mPreviewSize.height);
//					mCameraPreviewResolution.setText("" + mPreviewSize.width + "x" + mPreviewSize.height);
				}
			}
		}

		/**
		 * Update the layout based on rotation and orientation changes.
		 * @param changed
		 * @param left
		 * @param top
		 * @param right
		 * @param bottom
		 */
		@Override
		protected void onLayout(boolean changed, int left, int top, int right, int bottom)
		{
			// Source: http://stackoverflow.com/questions/7942378/android-camera-will-not-work-startpreview-fails
			if (changed) {
				final int width = right - left;
				final int height = bottom - top;

				int previewWidth = width;
				int previewHeight = height;

				if (mPreviewSize != null){
					Display display = ((WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

					switch (display.getRotation())
					{
						case Surface.ROTATION_0:
							previewWidth = mPreviewSize.height;
							previewHeight = mPreviewSize.width;
							mCamera.setDisplayOrientation(90);
							break;
						case Surface.ROTATION_90:
							previewWidth = mPreviewSize.width;
							previewHeight = mPreviewSize.height;
							break;
						case Surface.ROTATION_180:
							previewWidth = mPreviewSize.height;
							previewHeight = mPreviewSize.width;
							break;
						case Surface.ROTATION_270:
							previewWidth = mPreviewSize.width;
							previewHeight = mPreviewSize.height;
							mCamera.setDisplayOrientation(180);
							break;
					}
					Log.d(TAG, "Preview Size (rot): " + previewWidth + "x" + previewHeight);
				}

				final int scaledChildHeight = previewHeight * width / previewWidth;
				mCameraView.layout(0, height - scaledChildHeight, width, height);
			}
		}

		/**
		 *
		 * @param sizes
		 * @param width
		 * @param height
		 * @return
		 */
		private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int width, int height)
		{
			// Source: http://stackoverflow.com/questions/7942378/android-camera-will-not-work-startpreview-fails
			Camera.Size optimalSize = null;

			final double ASPECT_TOLERANCE = 0.1;
			double targetRatio = (double) height / width;

			// Try to find a size match which suits the whole screen minus the menu on the left.
			for (Camera.Size size : sizes){

				if (size.height != width) continue;
				double ratio = (double) size.width / size.height;
				if (ratio <= targetRatio + ASPECT_TOLERANCE && ratio >= targetRatio - ASPECT_TOLERANCE){
					optimalSize = size;
				}
			}

			// If we cannot find the one that matches the aspect ratio, ignore the requirement.
			if (optimalSize == null) {
				// TODO : Backup in case we don't get a size.
			}

			return optimalSize;
		}
	}
//	public void forceCrash(View view) {
//		throw new RuntimeException("This is a crash");
//	}

	class MyOnTouchListener implements View.OnTouchListener {
		Shape shape;
		PointF origin;
		Point shape_origin;

		Shape touchedShape(PointF pt)
		{
			if(mUpperShape == null || mLowerShape == null)
				return null;
			PointF ptfBmp = transformCoordTouchToBitmap(pt.x, pt.y);
			Point ptBmp = new Point((int)Math.floor(ptfBmp.x), (int)Math.floor(ptfBmp.y));
			Log.d(TAG, "touchedShape: " + pt.toString());
			if(pixelWithinArea(mUpperShape, ptBmp))
				return mUpperShape;
			if(pixelWithinArea(mLowerShape, ptBmp))
				return mLowerShape;
			return null;
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			/*
			// FIXME
			if(mPreview.mPreviewSize != null) {
				mCameraPreviewResolution.setText("" + mPreview.mPreviewSize.width + "x" + mPreview.mPreviewSize.height);
			}
			*/

			boolean handledEvent = false;
			PointF curr = new PointF(event.getX(), event.getY());

			switch(event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				shape = touchedShape(curr);
				if(shape != null) {
					Log.d(TAG, "Begin dragging shape");
					((MainActivity)getActivity()).mViewPager.enabled = false;
					shape_origin = shape.center;
					origin = curr;
					handledEvent = true;
				}
				break;
			case MotionEvent.ACTION_MOVE:
				if(shape != null) {
					handledEvent = true;
					Point p = new Point();
					p.x = (int)(curr.x - origin.x);
					p.y = (int)(curr.y - origin.y);

					PointF pf2 = transformCoordTouchToBitmap((float)p.x, (float)p.y);
					Point p2 = new Point((int)Math.floor(pf2.x), (int)Math.floor(pf2.y));

					// centered on touch
					PointF currf2 = transformCoordTouchToBitmap((float)curr.x, (float)curr.y);
					Point curr2 = new Point((int)Math.floor(currf2.x), (int)Math.floor(currf2.y));
					shape.center = validateShapePosition(curr2, shape.radius);

					updateDrawables();
					updateEditFields();
					updatePrefs();

					// NOTE: stops redrawing for some reason?  Fixes it?
					((MainActivity)getActivity()).mViewPager.invalidate();

				}
				break;
			case MotionEvent.ACTION_UP:
				if(shape != null) {
					Log.d(TAG, "End dragging shape");
					handledEvent = true;
				}
				shape = null;
				((MainActivity)getActivity()).mViewPager.enabled = true;
				break;
			}
			return handledEvent;
		}
	}

	private boolean pixelWithinArea(Point center, int radius, Point px)
	{
		// fast check
		if(center.x - radius > px.x || center.x + radius < px.x ||
				center.y - radius > px.y || center.y + radius < px.y)
			return false;

		// slow check
		int dx = center.x - px.x;
		int dy = center.y - px.y;
		int dist = (int)Math.floor(Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2)));
		return dist <= radius;
	}

	private boolean pixelWithinArea(Shape s, Point px)
	{
		if(s == null)
			return false;
		return pixelWithinArea(s.center, s.radius, px);
	}

	// Transform coordinates from screen to that of the imageview's drawable
	private PointF transformCoordTouchToBitmap(float x, float y) {
		Drawable d = mDrawingImageView.getDrawable();

		float percentX = x / mDrawingImageView.getWidth();
		float percentY = y / mDrawingImageView.getHeight();

		float coordX = percentX * mCaptureWidth;
		float coordY = percentY * mCaptureHeight;

		return new PointF(coordX , coordY);
	}
	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);

		if (isVisibleToUser&&mCamera!=null) {
			try {
				this.isVisibleToUser=isVisibleToUser;
				mCamera.reconnect();
				mCamera.startPreview();
			} catch (Exception e) {
				e.printStackTrace();
			}

				Log.e("MyFragment", "Fragment is visible.");
		}
		else
			Log.e("MyFragment", "Fragment is not visible.");
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.e(TAG,"LifeCycle onCreateView");
		sharedPrefs = getActivity().getPreferences(Context.MODE_PRIVATE);
		View v = inflater.inflate(R.layout.fragment_calibrate, container, false);
		mCameraView = v;

		mEditUpperX = (EditText)v.findViewById(R.id.upper_x);
		mEditUpperY = (EditText)v.findViewById(R.id.upper_y);
		mEditUpperSize = (EditText)v.findViewById(R.id.upper_size);
		mEditLowerX = (EditText)v.findViewById(R.id.lower_x);
		mEditLowerY = (EditText)v.findViewById(R.id.lower_y);
		mEditLowerSize = (EditText)v.findViewById(R.id.lower_size);
		mCameraExposure = (Spinner)v.findViewById(R.id.camera_exposure);
		mCameraResolution = (Spinner)v.findViewById(R.id.camera_resolution);
//		mCameraPreviewResolution = (TextView)v.findViewById(R.id.camera_preview_resolution);
		mMinRGBLevel = (EditText)v.findViewById(R.id.min_rgb);

		mDrawingImageView = (ImageView)v.findViewById(R.id.canvas);
		mDrawingImageView.setOnTouchListener(new MyOnTouchListener());

		// GridLayout's weight support requires android 5.0+
		// workaround that for older devices
		// BUG: rendered wrong on test device
		GridLayout.LayoutParams lparams = (GridLayout.LayoutParams)mCameraExposure.getLayoutParams();
		GridLayout.LayoutParams lparams2 = (GridLayout.LayoutParams)mCameraResolution.getLayoutParams();
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		int width = displaymetrics.widthPixels;
		// size it to narrowest dimension
		if(displaymetrics.heightPixels < width)
			width = displaymetrics.heightPixels;
		lparams.width = width/2;
		lparams2.width = width/2;
		mCameraExposure.setLayoutParams(lparams);
		mCameraResolution.setLayoutParams(lparams2);

		GridLayout grid = (GridLayout)v.findViewById(R.id.control_grid);
		for (int i = grid.getChildCount() - 3; i >= 0; i--) {
			final View child = grid.getChildAt(i);
			lparams = (GridLayout.LayoutParams)child.getLayoutParams();
			lparams.width = width/4;
			child.setLayoutParams(lparams);
		}

		mMinRGBLevel.setText(String.valueOf(sharedPrefs.getInt(
						getString(R.string.saved_min_rgb_for_live_pixel),
						Integer.parseInt(getString(R.string.saved_min_rgb_for_live_pixel_default)))));

		mCamera = ((MainActivity)getActivity()).mCamera;
		if(mCamera != null) {
			mCaptureWidth = ((MainActivity)getActivity()).mCaptureWidth;
			mCaptureHeight = ((MainActivity)getActivity()).mCaptureHeight;
			setupCameraPreview(getActivity().getBaseContext());
			setupCameraExposureSpinner();
			setupCameraResolutionSpinner();
			setupControlShapes();
			setupControlOverlay();
		}

		// takePicture stops preview, restart when calibrate screen shown
		// DISABLED: startPreview must be started before takePicture is called,
		// so Data screen will restart preview after each capture.
		/*
		((MainActivity)getActivity()).mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { } 

			@Override
			public void onPageSelected(int position) {
				MainActivity act = (MainActivity)getActivity();
				if(position == 1 && act.mCamera != null) {
					act.mCamera.startPreview();
				} else {
				}
			}

			@Override
			public void onPageScrollStateChanged(int state) { }
		});
		*/

		// hide keyboard on start
		getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

		return v;
	}

	private void setupCameraPreview(Context context)
	{
		if(mCamera == null)
			return;
		mPreview = new CameraPreview(context, mCamera, mCameraView);
		FrameLayout preview = (FrameLayout) mCameraView.findViewById(R.id.camera_view);
		preview.addView(mPreview);
		mPreview.startCameraPreview();



	}

	public void onCameraReady()
	{

		setupCameraPreview(getActivity().getBaseContext());

	}

	private void setupCameraExposureSpinner()
	{
		ArrayAdapter<String> adapter;
		adapter = new ArrayAdapter(getActivity(), R.layout.spinner_item);
		Camera.Parameters params = mCamera.getParameters();
		int min = params.getMinExposureCompensation();
		int max = params.getMaxExposureCompensation();
		float step = params.getExposureCompensationStep();
		int current = params.getExposureCompensation();

		for(int i=min; i<max; i++) {
			adapter.add(Float.toString(i * step));
		}

		adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		mCameraExposure.setAdapter(adapter);
		mCameraExposure.setSelection(current - min);

		mCameraExposure.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				try {
					Camera.Parameters params = null;
					if (mCamera != null) {
                        if (mCamera.getParameters()!= null) {
                            params = mCamera.getParameters();
                        }else{
                            params= ((MainActivity)getActivity()).params;
                        }
                    }else{
                        params= ((MainActivity)getActivity()).params;
                    }
					int min = params.getMinExposureCompensation();
					int idx = position + min;
					params.setExposureCompensation(idx);
					mCamera.setParameters(params);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			   // sometimes you need nothing here
			}
		});
	}

	private void setupCameraResolutionSpinner()
	{
		ArrayAdapter adapter;
		adapter = new ArrayAdapter(getActivity(), R.layout.spinner_item);
		Camera.Parameters params = mCamera.getParameters();
		List<Camera.Size> capture_sizes = params.getSupportedPictureSizes();
		for(int i=0; i<capture_sizes.size(); i++) {
			adapter.add(capture_sizes.get(i).width + "x" +
					capture_sizes.get(i).height);
		}

		// default highest
		int savedResolutionIndex = sharedPrefs.getInt(
				getString(R.string.saved_camera_resolution_index),
				Integer.parseInt(getString(R.string.saved_camera_resolution_index_default)));
		if(savedResolutionIndex > capture_sizes.size()-1) {
//			savedResolutionIndex = capture_sizes.size()-1;
			savedResolutionIndex = 1;
			SharedPreferences.Editor editor = sharedPrefs.edit();
			editor.putInt(
					getString(R.string.saved_camera_resolution_index),
					savedResolutionIndex);
			editor.commit();
		}

		adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		mCameraResolution.setAdapter(adapter);
		mCameraResolution.setSelection(savedResolutionIndex);

		mCameraResolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				int savedResolutionIndex = sharedPrefs.getInt(
						getString(R.string.saved_camera_resolution_index),
						Integer.parseInt(getString(R.string.saved_camera_resolution_index_default)));
				if(savedResolutionIndex != position) {
					SharedPreferences.Editor editor = sharedPrefs.edit();
					editor.putInt(
							getString(R.string.saved_camera_resolution_index),
							position);
					editor.commit();
					getActivity().recreate();
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			   // sometimes you need nothing here
			}
		});
	}

	private Point validateShapePosition(Point center, int radius)
	{
		if(center.x - radius < 0) {
			int dx = radius - center.x;
			center.x += dx;
		}
		if(center.x + radius > mCaptureWidth) {
			int dx = center.x + radius - mCaptureWidth;
			center.x -= dx;
		}
		if(center.y - radius < 0) {
			int dy = radius - center.y;
			center.y += dy;
		}
		if(center.y + radius > mCaptureHeight) {
			int dy = center.y + radius - mCaptureHeight;
			center.y -= dy;
		}
		return center;
	}

	private void updateDrawables()
	{
		mDrawingBitmap.eraseColor(Color.argb(0,0,0,0));

		Canvas c = new Canvas(mDrawingBitmap);

		mUpperShape.drawable.setBounds(
				mUpperShape.center.x - mUpperShape.radius, // left
				mUpperShape.center.y - mUpperShape.radius, // top
				mUpperShape.center.x + mUpperShape.radius, // right
				mUpperShape.center.y + mUpperShape.radius); // bottom
		mUpperShape.drawable.draw(c);

		mLowerShape.drawable.setBounds(
				mLowerShape.center.x - mLowerShape.radius, // left
				mLowerShape.center.y - mLowerShape.radius, // top
				mLowerShape.center.x + mLowerShape.radius, // right
				mLowerShape.center.y + mLowerShape.radius); // bottom
		mLowerShape.drawable.draw(c);

		// captions
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		int screenHeight = displaymetrics.heightPixels;
//		int textHeight = (int)Math.ceil(14 * screenHeight / mCaptureHeight);
		int textHeight = mCaptureHeight / 20;

		Paint paint = new Paint();
		paint.setColor(Color.WHITE);
		paint.setTypeface(MainActivity.typeface);
		paint.setTextSize(textHeight);
		int textWidth = (int)Math.ceil(paint.measureText("Upper"));

		c.drawText("Upper",
				mUpperShape.center.x - textWidth/2,
				mUpperShape.center.y - mUpperShape.radius - textHeight/2,
				paint);
		textWidth = (int)Math.ceil(paint.measureText("Lower"));
		c.drawText("Lower",
				mLowerShape.center.x - textWidth/2,
				mLowerShape.center.y - mLowerShape.radius - textHeight/2,
				paint);

		MainActivity act = (MainActivity)getActivity();
		Camera.Parameters params = null;
		try {
			params = act.mCamera.getParameters();
		} catch (Exception e) {
			e.printStackTrace();
			params = act.params;
		}
		Camera.Size sz = params.getPreviewSize();
		c.drawText(sz.width + "x" + sz.height,
				0,
				textHeight,
				paint);

		mDrawingImageView.setImageDrawable(new BitmapDrawable(getResources(), mDrawingBitmap));
		mDrawingImageView.setScaleType(ImageView.ScaleType.FIT_XY);
	}

	private void updateEditFields()
	{
		mEditUpperX.setText(String.valueOf(mUpperShape.center.x));
		mEditUpperY.setText(String.valueOf(mUpperShape.center.y));
		mEditUpperSize.setText(String.valueOf(mUpperShape.radius));
		mEditLowerX.setText(String.valueOf(mLowerShape.center.x));
		mEditLowerY.setText(String.valueOf(mLowerShape.center.y));
		mEditLowerSize.setText(String.valueOf(mLowerShape.radius));
	}

	private void updatePrefs()
	{
		Log.d(TAG, "updatePrefs");
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putInt(getString(R.string.saved_upper_x), mUpperShape.center.x);
		editor.putInt(getString(R.string.saved_upper_y), mUpperShape.center.y);
		editor.putInt(getString(R.string.saved_upper_radius), mUpperShape.radius);

		editor.putInt(getString(R.string.saved_lower_x), mLowerShape.center.x);
		editor.putInt(getString(R.string.saved_lower_y), mLowerShape.center.y);
		editor.putInt(getString(R.string.saved_lower_radius), mLowerShape.radius);

		editor.putString(getString(R.string.saved_camera_exposure),
				mCameraExposure.getSelectedItem().toString());

		int i = 0;
		String s = mMinRGBLevel.getText().toString();
		if(s != null)
			i = Integer.parseInt(s);
		editor.putInt(getString(R.string.saved_min_rgb_for_live_pixel), i);

		editor.commit();
	}

	private void updateEditFieldsChanged()
	{
		// validate input
		if(mEditUpperX.getText().toString().isEmpty())
			mEditUpperX.setText(String.valueOf(mUpperShape.center.x));
		if(mEditUpperY.getText().toString().isEmpty())
			mEditUpperY.setText(String.valueOf(mUpperShape.center.y));
		if(mEditUpperSize.getText().toString().isEmpty())
			mEditUpperSize.setText(String.valueOf(mUpperShape.radius));
		if(mEditLowerX.getText().toString().isEmpty())
			mEditLowerX.setText(String.valueOf(mLowerShape.center.x));
		if(mEditLowerY.getText().toString().isEmpty())
			mEditLowerY.setText(String.valueOf(mLowerShape.center.y));
		if(mEditLowerSize.getText().toString().isEmpty())
			mEditLowerSize.setText(String.valueOf(mLowerShape.radius));

		Point p = new Point();
		p.x = Integer.parseInt(mEditUpperX.getText().toString());
		p.y = Integer.parseInt(mEditUpperY.getText().toString());
		mUpperShape.radius = Math.max(10, Integer.parseInt(mEditUpperSize.getText().toString()));
		mUpperShape.center = validateShapePosition(p, mUpperShape.radius);

		p = new Point();
		p.x = Integer.parseInt(mEditLowerX.getText().toString());
		p.y = Integer.parseInt(mEditLowerY.getText().toString());
		mLowerShape.radius = Math.max(10, Integer.parseInt(mEditLowerSize.getText().toString()));
		mLowerShape.center = validateShapePosition(p, mLowerShape.radius);
	}

	private void setupControlShapes()
	{
		mUpperShape = new Shape();
		mLowerShape = new Shape();

		mDrawingBitmap = Bitmap.createBitmap(mCaptureWidth, mCaptureHeight, Bitmap.Config.ARGB_8888);

		// restore last settings
		mUpperShape.radius = sharedPrefs.getInt(getString(R.string.saved_upper_radius), 100);
		mUpperShape.center = validateShapePosition(new Point(
					sharedPrefs.getInt(getString(R.string.saved_upper_x), 100),
					sharedPrefs.getInt(getString(R.string.saved_upper_y), 100)),
				mUpperShape.radius);

		mLowerShape.radius = sharedPrefs.getInt(getString(R.string.saved_lower_radius), 100);
		mLowerShape.center = validateShapePosition(new Point(
					sharedPrefs.getInt(getString(R.string.saved_lower_x), 300),
					sharedPrefs.getInt(getString(R.string.saved_lower_y), 100)),
				mLowerShape.radius);

		updateDrawables();
		updateEditFields();

		// BUGFIX: viewpager showing wrong screen upon startup or permissions granted
		((MainActivity)getActivity()).mViewPager.invalidate();
	}

	private void setupEditTextListener(final EditText et)
	{
		et.setTag(""+et.getId());
		et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_PREVIOUS) {
					updateEditFieldsChanged();
//					Crashlytics.setString("UpdateDrawable","EditText Id="+et.getTag());
					updateDrawables();
					updateEditFields();
					updatePrefs();

					// hide keyboard on done event
					if(actionId == EditorInfo.IME_ACTION_DONE) {
						InputMethodManager imm= (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(mEditUpperX.getWindowToken(), 0);
					}
					return true;
				}
				return false;
			}
		});
		et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				try {
					if (!hasFocus) {
                        updateEditFieldsChanged();
                        updateDrawables();
                        updateEditFields();
                        updatePrefs();
                    }
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private void setupControlOverlay()
	{
		updateEditFields();
		setupEditTextListener(mEditUpperX);
		setupEditTextListener(mEditUpperX);
		setupEditTextListener(mEditUpperY);
		setupEditTextListener(mEditUpperSize);
		setupEditTextListener(mEditLowerX);
		setupEditTextListener(mEditLowerY);
		setupEditTextListener(mEditLowerSize);
		setupEditTextListener(mMinRGBLevel);
	}


	@Override
	public void onDestroy() {
		Log.e(TAG,TAG+"LifeCycle onDestroy");
		if(mPreview != null){
			mPreview.destroyDrawingCache();
			mPreview.mCamera = null;
		}
		mDrawingBitmap=null;
		super.onDestroy();

	}
}
