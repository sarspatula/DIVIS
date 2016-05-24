package com.example.m.divis;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.hardware.Camera;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
   
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class FragmentCalibrate extends Fragment {
	private static final String TAG = "DIVISFragmentCalibrate";
	private static final int MY_PERMISSIONS_REQUEST_CAMERA = 42;

	// class encapsulates drawable, draw style, center, and radius
	private Shape mUpperShape;
	private Shape mLowerShape;

	// Native camera.
	private Camera mCamera;

	// size of image captured by camera, set to largest available
	private Camera.Size mCaptureSize;
	// adjusted by device orientation
	private int mCaptureWidth;
	private int mCaptureHeight;

	// View to display the camera output.
	private CameraPreview mPreview;

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

	class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

		// SurfaceHolder
		private SurfaceHolder mHolder;

		// Our Camera.
		private Camera mCamera;

		// Parent Context.
		private Context mContext;

		// Camera Sizing (For rotation, orientation changes)
		private Camera.Size mPreviewSize;

		// List of supported preview sizes
		private List<Camera.Size> mSupportedPreviewSizes;

		// Flash modes supported by this camera
		private List<String> mSupportedFlashModes;

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
			mSupportedFlashModes = mCamera.getParameters().getSupportedFlashModes();

			// Set the camera to Auto Flash mode.
			if (mSupportedFlashModes != null && mSupportedFlashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)){
				Camera.Parameters parameters = mCamera.getParameters();
				parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
				mCamera.setParameters(parameters);
			}

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
				mCamera.stopPreview();
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
			// If your preview can change or rotate, take care of those events here.
			// Make sure to stop the preview before resizing or reformatting it.

			if (mHolder.getSurface() == null){
				// preview surface does not exist
				return;
			}

			// stop preview before making changes
			try {
				Camera.Parameters parameters = mCamera.getParameters();

				// Set the auto-focus mode to "continuous"
				parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

				// Preview size must exist.
				if(mPreviewSize != null) {
					Camera.Size previewSize = mPreviewSize;
					parameters.setPreviewSize(previewSize.width, previewSize.height);
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
				if(mPreviewSize != null)
					Log.d(TAG, "Optimal Preview Size: " + mPreviewSize.width + "x" + mPreviewSize.height);
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

	protected class MyOnTouchListener implements View.OnTouchListener {
		Shape shape;
		PointF origin;
		Point shape_origin;

		Shape touchedShape(PointF pt)
		{
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

					/*
					shape.center.x = shape_origin.x + p2.x;
					shape.center.y = shape_origin.y + p2.y;
					shape.center = validateShapePosition(shape.center, shape.radius);
					*/

					// centered on touch
					PointF currf2 = transformCoordTouchToBitmap((float)curr.x, (float)curr.y);
					Point curr2 = new Point((int)Math.floor(currf2.x), (int)Math.floor(currf2.y));
					shape.center = validateShapePosition(curr2, shape.radius);

					updateDrawables();

					// FIXME: stops redrawing for some reason?  Fixes it?
					((MainActivity)getActivity()).mViewPager.invalidate();

//					Log.d(TAG, "drag " + p2.x + "x" + p2.y + " : " + shape.center.toString());
//					Log.d(TAG, "drag " + curr2.x + "x" + curr2.y + " : " + shape.center.toString());
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

	boolean pixelWithinArea(Shape s, Point px)
	{
		int dx = s.center.x - px.x;
		int dy = s.center.y - px.y;
		int dist = (int)Math.floor(Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2)));
		Log.d(TAG, "pixelWithinArea: dist: " + dist);
		if(dist > s.radius)
			return false;
		return true;
	}

	// Transform coordinates from screen to that of the imageview's drawable
	public PointF transformCoordTouchToBitmap(float x, float y) {
		Drawable d = mDrawingImageView.getDrawable();

		float percentX = x / mDrawingImageView.getWidth();
		float percentY = y / mDrawingImageView.getHeight();

		float coordX = percentX * mCaptureWidth;
		float coordY = percentY * mCaptureHeight;
/*
//		float finalX = origW / mDrawingImageView.getWidth();
//		float finalY = origH / mDrawingImageView.getHeight();
		float finalX = x / mDrawingImageView.getWidth() * origW;
		float finalY = y / mDrawingImageView.getHeight() * origH;
*/
//		Log.d(TAG, "COORDS: " + origW + "x" + origH + " : " + mDrawingImageView.getWidth() + "x" + mDrawingImageView.getHeight());

//		return new PointF(finalX , finalY);
		return new PointF(coordX , coordY);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_calibrate, container, false);
		mCameraView = v;

		mEditUpperX = (EditText)v.findViewById(R.id.upper_x);
		mEditUpperY = (EditText)v.findViewById(R.id.upper_y);
		mEditUpperSize = (EditText)v.findViewById(R.id.upper_size);
		mEditLowerX = (EditText)v.findViewById(R.id.lower_x);
		mEditLowerY = (EditText)v.findViewById(R.id.lower_y);
		mEditLowerSize = (EditText)v.findViewById(R.id.lower_size);
		mCameraExposure = (Spinner)v.findViewById(R.id.camera_exposure);

		mDrawingImageView = (ImageView)v.findViewById(R.id.canvas);
		mDrawingImageView.setOnTouchListener(new MyOnTouchListener());

		// GridLayout's weight support requires android 5.0+
		// workaround that for older devices
		GridLayout.LayoutParams lparams = (GridLayout.LayoutParams)mCameraExposure.getLayoutParams();
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		int width = displaymetrics.widthPixels;
		lparams.width = width/2;
		mCameraExposure.setLayoutParams(lparams);
		Log.d(TAG, "SET WIDTH " + Float.toString(width/2));

		GridLayout grid = (GridLayout)v.findViewById(R.id.control_grid);
		for (int i = grid.getChildCount() - 3; i >= 0; i--) {
			final View child = grid.getChildAt(i);
			lparams = (GridLayout.LayoutParams)child.getLayoutParams();
			lparams.width = width/4;
			child.setLayoutParams(lparams);
		}

		if(setupCamera()) {
			setupCameraExposureSpinner();
			setupControlShapes();
			setupControlOverlay();
		}
		return v;
	}

	boolean setupCamera()
	{
		boolean opened = false;
		int permissionCheck = ContextCompat.checkSelfPermission(getActivity(),
				Manifest.permission.CAMERA);
		if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
			opened = safeCameraOpenInView();
			if(!opened) {
				Log.d(TAG, "Error, Camera failed to open");
			}
		} else {
//			// Should we show an explanation?
//			if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
//					Manifest.permission.CAMERA)) {
//
//				// Show an expanation to the user *asynchronously* -- don't block
//				// this thread waiting for the user's response! After the user
//				// sees the explanation, try again to request the permission.
//
//			} else {

				// No explanation needed, we can request the permission.

				requestPermissions(
						new String[]{Manifest.permission.CAMERA},
						MY_PERMISSIONS_REQUEST_CAMERA);

				// MY_PERMISSIONS_REQUEST_CAMERA is an
				// app-defined int constant. The callback method gets the
				// result of the request.
//			}
		}
		return opened;
	}

	void setupCameraExposureSpinner()
	{
		ArrayAdapter<String> adapter = new ArrayAdapter(getActivity(), R.layout.spinner_item);
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
				Camera.Parameters params = mCamera.getParameters();
				int min = params.getMinExposureCompensation();
				int idx = position + min;
				params.setExposureCompensation(idx);
				mCamera.setParameters(params);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			   // sometimes you need nothing here
			}
		});
	}

	Point validateShapePosition(Point center, int radius)
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

	void updateDrawables()
	{
//		Log.d(TAG, "=== updateDrawables upper center: " + mUpperShape.center.toString());
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

		mDrawingImageView.setImageDrawable(new BitmapDrawable(getResources(), mDrawingBitmap));
		mDrawingImageView.setScaleType(ImageView.ScaleType.FIT_XY);

		updateEditFields();
	}

	void updateEditFields()
	{
		mEditUpperX.setText(Integer.toString(mUpperShape.center.x));
		mEditUpperY.setText(Integer.toString(mUpperShape.center.y));
		mEditUpperSize.setText(Integer.toString(mUpperShape.radius));
		mEditLowerX.setText(Integer.toString(mLowerShape.center.x));
		mEditLowerY.setText(Integer.toString(mLowerShape.center.y));
		mEditLowerSize.setText(Integer.toString(mLowerShape.radius));
	}

	void updateEditFieldsChanged()
	{
//		Log.d(TAG, "=== updateEditFieldsChanged");
		Point p = new Point();
		p.x = Integer.parseInt(mEditUpperX.getText().toString());
		p.y = Integer.parseInt(mEditUpperY.getText().toString());
		mUpperShape.radius = Math.max(10, Integer.parseInt(mEditUpperSize.getText().toString()));
		mUpperShape.center = validateShapePosition(p, mUpperShape.radius);
//		Log.d(TAG, "=== updateEditFieldsChanged: " + p.toString() + " into " + mUpperShape.center.toString());

		p = new Point();
		p.x = Integer.parseInt(mEditLowerX.getText().toString());
		p.y = Integer.parseInt(mEditLowerY.getText().toString());
		mLowerShape.radius = Math.max(10, Integer.parseInt(mEditLowerSize.getText().toString()));
		mLowerShape.center = validateShapePosition(p, mLowerShape.radius);

		updateDrawables();
	}

	void setupControlShapes()
	{
		mUpperShape = new Shape();
		mLowerShape = new Shape();

		mDrawingBitmap = Bitmap.createBitmap(mCaptureWidth, mCaptureHeight, Bitmap.Config.ARGB_8888);

		// TODO: restore last settings
		mUpperShape.radius = 100;
		mUpperShape.center = validateShapePosition(new Point(100, 150), mUpperShape.radius);
		mLowerShape.radius = 50;
		mLowerShape.center = validateShapePosition(new Point(350, 150), mLowerShape.radius);

		updateDrawables();

		// BUGFIX: viewpager showing wrong screen upon startup or permissions granted
		((MainActivity)getActivity()).mViewPager.invalidate();
	}

	void setupControlOverlay()
	{
		updateEditFields();
		mEditUpperX.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				Log.d(TAG, "onEditorAction Action " + actionId + " compared to " + EditorInfo.IME_ACTION_DONE);
				if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_PREVIOUS) {
					updateEditFieldsChanged();
					return true;
				}
				return false;
			}
		});
		mEditUpperX.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				Log.d(TAG, "onEditorAction Action " + actionId + " compared to " + EditorInfo.IME_ACTION_DONE);
				if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_PREVIOUS) {
					updateEditFieldsChanged();
					return true;
				}
				return false;
			}
		});

		mEditUpperY.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				Log.d(TAG, "onEditorAction Action " + actionId + " compared to " + EditorInfo.IME_ACTION_DONE);
				if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_PREVIOUS) {
					updateEditFieldsChanged();
					return true;
				}
				return false;
			}
		});

		mEditUpperSize.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				Log.d(TAG, "onEditorAction Action " + actionId + " compared to " + EditorInfo.IME_ACTION_DONE);
				if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_PREVIOUS) {
					updateEditFieldsChanged();
					return true;
				}
				return false;
			}
		});

		mEditLowerX.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				Log.d(TAG, "onEditorAction Action " + actionId + " compared to " + EditorInfo.IME_ACTION_DONE);
				if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_PREVIOUS) {
					updateEditFieldsChanged();
					return true;
				}
				return false;
			}
		});

		mEditLowerY.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				Log.d(TAG, "onEditorAction Action " + actionId + " compared to " + EditorInfo.IME_ACTION_DONE);
				if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_PREVIOUS) {
					updateEditFieldsChanged();
					return true;
				}
				return false;
			}
		});

		mEditLowerSize.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_PREVIOUS) {
					updateEditFieldsChanged();
					return true;
				}
				return false;
			}
		});

		/*
		mEditUpperX.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s)
			{
				updateEditFieldsChanged();
			}
			@Override public void beforeTextChanged(CharSequence s, int start, int before, int count) {}
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
		});
		mEditUpperY.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s)
			{
				updateEditFieldsChanged();
			}
			@Override public void beforeTextChanged(CharSequence s, int start, int before, int count) {}
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
		});
		mEditUpperSize.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s)
			{
				updateEditFieldsChanged();
			}
			@Override public void beforeTextChanged(CharSequence s, int start, int before, int count) {}
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
		});
		mEditLowerX.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s)
			{
				updateEditFieldsChanged();
			}
			@Override public void beforeTextChanged(CharSequence s, int start, int before, int count) {}
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
		});
		mEditLowerY.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s)
			{
				updateEditFieldsChanged();
			}
			@Override public void beforeTextChanged(CharSequence s, int start, int before, int count) {}
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
		});
		mEditLowerSize.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s)
			{
				updateEditFieldsChanged();
			}
			@Override public void beforeTextChanged(CharSequence s, int start, int before, int count) {}
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
		});
		*/
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
				if(safeCameraOpenInView()) {
					setupCameraExposureSpinner();
					setupControlShapes();
					setupControlOverlay();
					Log.d(TAG, "=== PERMISSION GRANTED, SETTING UP UI!");
				} else {
					Log.d(TAG, "Error, failed to open Camera");
				}
			} else {
				// permission denied, boo! Disable the
				// functionality that depends on this permission.
			}
			return;
		}
		}
	}

	private boolean safeCameraOpenInView() {
		boolean qOpened = false;
		releaseCameraAndPreview();
		mCamera = getCameraInstance();
		qOpened = (mCamera != null);

		Log.d(TAG, "INFO, qOpened==" + qOpened);

		if(qOpened == true){
			// setup preview
			mPreview = new CameraPreview(getActivity().getBaseContext(), mCamera, mCameraView);
			FrameLayout preview = (FrameLayout) mCameraView.findViewById(R.id.camera_view);
			preview.addView(mPreview);
			mPreview.startCameraPreview();

			// determine largest capture size available
			Camera.Parameters params = mCamera.getParameters();
			List<Camera.Size> capture_sizes = params.getSupportedPictureSizes();
			int idx_of_largest = 0;
			for(int i=0; i<capture_sizes.size(); i++) {
				Log.d(TAG, "Capture Size: " + capture_sizes.get(i).width + "x" + capture_sizes.get(i).height);
				if(capture_sizes.get(idx_of_largest).height < capture_sizes.get(i).height)
					idx_of_largest = i;
			}
			mCaptureSize = capture_sizes.get(idx_of_largest);

			// adjust by orientation
			Display display = ((WindowManager)getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
			switch (display.getRotation()) {
			case Surface.ROTATION_0:
			case Surface.ROTATION_180:
				mCaptureWidth = mCaptureSize.height;
				mCaptureHeight = mCaptureSize.width;
				break;
			default:
				mCaptureWidth = mCaptureSize.width;
				mCaptureHeight = mCaptureSize.height;
				break;
			}

			Log.d(TAG, "Capture Size Set To " + mCaptureSize.width + "x" + mCaptureSize.height);
			Log.d(TAG, "Capture Size (rot): " + mCaptureWidth + "x" + mCaptureHeight);
		}
		return qOpened;
	}

	/**
	 * Safe method for getting a camera instance.
	 * @return
	 */
	public static Camera getCameraInstance(){
		Camera c = null;
		Log.d(TAG, "INFO, <getCameraInstance>");
		try {
			int n = Camera.getNumberOfCameras();
			if(n > 0)
				c = Camera.open(0); // attempt to get a Camera instance
		} catch (Exception e){
			e.printStackTrace();
		}
		Log.d(TAG, "INFO, </getCameraInstance> " + c);
		return c; // returns null if camera is unavailable
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		releaseCameraAndPreview();
	}

	/**
	 * Clear any existing preview / camera.
	 */
	private void releaseCameraAndPreview() {

		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
		if(mPreview != null){
			mPreview.destroyDrawingCache();
			mPreview.mCamera = null;
		}
	}
}
