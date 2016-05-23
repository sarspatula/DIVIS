package com.example.m.divis;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
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

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
	private static final String TAG = "DVISFragmentCalibrate";
	private static final int MY_PERMISSIONS_REQUEST_CAMERA = 42;

	// class encapsulates drawable, draw style, center, and radius
	private Shape mUpperShape;
	private Shape mLowerShape;

	private Drawable[] mDrawList;
	private LayerDrawable mLayerDrawable;

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

	private Spinner mCameraExposure;

	// control shapes will be drawn on this view
	private ImageView mCanvas;

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
		Point origin;
		Point shape_origin;

		Shape touchedShape(PointF pt)
		{
			PointF ptfBmp = transformCoordTouchToBitmap(pt.x, pt.y, true);
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
			PointF curr = new PointF(event.getX(), event.getY());

			switch(event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				shape = touchedShape(curr);
				if(shape != null) {
					Log.d(TAG, "Begin dragging shape");
					shape_origin = shape.center;
				}
				break;
			case MotionEvent.ACTION_MOVE:
				if(shape != null) {
					Point p = new Point();
					p.x = (int)(curr.x - origin.x);
					p.y = (int)(curr.y - origin.y);

					shape.center.x = shape_origin.x + p.x;
					shape.center.y = shape_origin.y + p.y;

					mDrawList[1] = mUpperShape.update();
					mDrawList[2] = mLowerShape.update();
					mLayerDrawable = new LayerDrawable(mDrawList);
					mCanvas.setImageDrawable(mLayerDrawable);
					Log.d("DEBUG", "drag " + p.x + "x" + p.y);
				}
				break;
			case MotionEvent.ACTION_UP:
				if(shape != null)
					Log.d(TAG, "End dragging shape");
				shape = null;
				break;
			}
			return true;
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
	public PointF transformCoordTouchToBitmap(float x, float y, boolean clipToBitmap) {
		Drawable d = mCanvas.getDrawable();
		float origW = d.getIntrinsicWidth();
		float origH = d.getIntrinsicHeight();
		float finalX = origW / mCanvas.getWidth();
		float finalY = origH / mCanvas.getHeight();

		if (clipToBitmap) {
			finalX = Math.min(Math.max(finalX, 0), origW);
			finalY = Math.min(Math.max(finalY, 0), origH);
		}

		return new PointF(finalX , finalY);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_calibrate, container, false);
		mCameraView = v;
		mCameraExposure = (Spinner)v.findViewById(R.id.camera_exposure);
		mCanvas = (ImageView)v.findViewById(R.id.canvas);
		mCanvas.setOnTouchListener(new MyOnTouchListener());

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

		setupCamera();
		setupCameraExposureSpinner();
		setupControlShapes();
		return v;
	}

	void setupCamera()
	{
		int permissionCheck = ContextCompat.checkSelfPermission(getActivity(),
				Manifest.permission.CAMERA);
		if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
			boolean opened = safeCameraOpenInView();
			if(!opened) {
				Log.d(TAG, "Error, Camera failed to open");
			}
		} else {
			// Should we show an explanation?
			if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
					Manifest.permission.CAMERA)) {

				// Show an expanation to the user *asynchronously* -- don't block
				// this thread waiting for the user's response! After the user
				// sees the explanation, try again to request the permission.

			} else {

				// No explanation needed, we can request the permission.

				ActivityCompat.requestPermissions(getActivity(),
						new String[]{Manifest.permission.CAMERA},
						MY_PERMISSIONS_REQUEST_CAMERA);

				// MY_PERMISSIONS_REQUEST_CAMERA is an
				// app-defined int constant. The callback method gets the
				// result of the request.
			}
		}
	}

	void setupCameraExposureSpinner()
	{
		if(mCamera == null) {
			Log.d(TAG, "Error, setupCameraExposureSpinner: camera not opened");
			return;
		}

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
		if(center.y + radius > mCaptureWidth) {
			int dy = center.y + radius - mCaptureWidth;
			center.y -= dy;
		}
		return center;
	}

	void setupControlShapes()
	{
		// TODO: fail gracefully if camera failed to open

		mUpperShape = new Shape();
		mLowerShape = new Shape();

		// ensure drawable size matches image capture size
		ShapeDrawable blank_drawable = new ShapeDrawable();
		blank_drawable.setIntrinsicWidth(mCaptureSize.width);
		blank_drawable.setIntrinsicHeight(mCaptureSize.height);
		blank_drawable.getPaint().setStrokeWidth(0);
		blank_drawable.getPaint().setColor(Color.argb(0,0,0,0));
		blank_drawable.getPaint().setStyle(Paint.Style.STROKE);

		Drawable[] mDrawList = new Drawable[3];
		mDrawList[0] = blank_drawable;
		mDrawList[1] = mUpperShape.drawable;
		mDrawList[2] = mLowerShape.drawable;
		mLayerDrawable = new LayerDrawable(mDrawList);

		// TODO: restore last settings
		mUpperShape.center = validateShapePosition(new Point(100, 150), mUpperShape.radius);
		mLowerShape.center = validateShapePosition(new Point(350, 150), mLowerShape.radius);

		mUpperShape.update();
		mLowerShape.update();

		// blank drawable, size matched to image capture.  TODO: needed?
//		mLayerDrawable.setLayerSize(0, mCaptureSize.width, mCaptureSize.height);

		// upper
		mLayerDrawable.setLayerInset(1,
				mUpperShape.center.x - mUpperShape.radius, // left
				mUpperShape.center.y - mUpperShape.radius, // top
				mCaptureWidth - (mUpperShape.center.x + mUpperShape.radius), // right
				mCaptureHeight - (mUpperShape.center.y + mUpperShape.radius)); // bottom

		// lower
		mLayerDrawable.setLayerInset(2,
				mLowerShape.center.x - mLowerShape.radius, // left
				mLowerShape.center.y - mLowerShape.radius, // top
				mCaptureWidth - (mLowerShape.center.x + mLowerShape.radius), // right
				mCaptureHeight - (mLowerShape.center.y + mLowerShape.radius)); // bottom

		mCanvas.setImageDrawable(mLayerDrawable);
		mCanvas.setScaleType(ImageView.ScaleType.FIT_XY);
	}

	void updateDrawables()
	{
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
			String permissions[], int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_CAMERA: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {

					boolean opened = safeCameraOpenInView();
					if(!opened) {
						Log.d(TAG, "Error, failed to open Camera");
					}
				} else {

					// permission denied, boo! Disable the
					// functionality that depends on this permission.
				}
				return;
			}

			// other 'case' lines to check for other
			// permissions this app might request
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
