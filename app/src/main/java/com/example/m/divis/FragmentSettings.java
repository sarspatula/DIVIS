package com.example.m.divis;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

public class FragmentSettings extends Fragment {
	private static final String TAG = "DIVISFragmentSettings";
	private SharedPreferences sharedPrefs;

	private EditText device_id;
	private EditText divis_id;
	private EditText app_version;
	private EditText location_id;
	private EditText location_name;
	private EditText location_detail;
	private EditText upper_sensor_depth;
	private EditText lower_sensor_depth;
	private EditText data_refresh_rate;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_settings, container, false);
		sharedPrefs = getActivity().getPreferences(Context.MODE_PRIVATE);

		device_id = (EditText)v.findViewById(R.id.device_id);
		divis_id = (EditText)v.findViewById(R.id.divis_id);
		app_version = (EditText)v.findViewById(R.id.app_version);
		location_id = (EditText)v.findViewById(R.id.location_id);
		location_name = (EditText)v.findViewById(R.id.location_name);
		location_detail = (EditText)v.findViewById(R.id.location_detail);
		upper_sensor_depth = (EditText)v.findViewById(R.id.upper_sensor_depth);
		lower_sensor_depth = (EditText)v.findViewById(R.id.lower_sensor_depth);
		data_refresh_rate = (EditText)v.findViewById(R.id.data_refresh_rate);

		loadSettings();
		setupListeners();

		return v;
	}

	private void loadSettings()
	{
		device_id.setText(sharedPrefs.getString(
					getString(R.string.saved_device_id),
					getString(R.string.saved_device_id_default)));
		divis_id.setText(sharedPrefs.getString(
					getString(R.string.saved_divis_id),
					getString(R.string.saved_divis_id_default)));
		app_version.setText(sharedPrefs.getString(
					getString(R.string.saved_app_version),
					getString(R.string.saved_app_version_default)));
		location_id.setText(sharedPrefs.getString(
					getString(R.string.saved_location_id),
					getString(R.string.saved_location_id_default)));
		location_name.setText(sharedPrefs.getString(
					getString(R.string.saved_location_name),
					getString(R.string.saved_location_name_default)));
		location_detail.setText(sharedPrefs.getString(
					getString(R.string.saved_location_detail),
					getString(R.string.saved_location_detail_default)));
		upper_sensor_depth.setText(String.valueOf(sharedPrefs.getInt(
					getString(R.string.saved_upper_sensor_depth),
					Integer.parseInt(getString(R.string.saved_upper_sensor_depth_default)))));
		lower_sensor_depth.setText(String.valueOf(sharedPrefs.getInt(
					getString(R.string.saved_lower_sensor_depth),
					Integer.parseInt(getString(R.string.saved_lower_sensor_depth_default)))));
		data_refresh_rate.setText(String.valueOf(sharedPrefs.getInt(
					getString(R.string.saved_data_refresh_rate),
					Integer.parseInt(getString(R.string.saved_data_refresh_rate_default)))));
	}

	private void updatePrefs()
	{
		Log.d(TAG, "updatePrefs");
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putString(getString(R.string.saved_device_id),
				device_id.getText().toString());
		editor.putString(getString(R.string.saved_divis_id),
				divis_id.getText().toString());
		editor.putString(getString(R.string.saved_app_version),
				app_version.getText().toString());
		editor.putString(getString(R.string.saved_location_id),
				location_id.getText().toString());
		editor.putString(getString(R.string.saved_location_name),
				location_name.getText().toString());
		editor.putString(getString(R.string.saved_location_detail),
				location_detail.getText().toString());
		editor.putInt(getString(R.string.saved_upper_sensor_depth),
				Integer.parseInt(upper_sensor_depth.getText().toString()));
		editor.putInt(getString(R.string.saved_lower_sensor_depth),
				Integer.parseInt(lower_sensor_depth.getText().toString()));
		editor.putInt(getString(R.string.saved_data_refresh_rate),
				Integer.parseInt(data_refresh_rate.getText().toString()));
		editor.commit();
	}

	private void setupEditTextListener(EditText et)
	{
		et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_PREVIOUS) {
					updatePrefs();

					// hide keyboard on done event
					if(actionId == EditorInfo.IME_ACTION_DONE) {
						InputMethodManager imm= (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(device_id.getWindowToken(), 0);
					} else {
						// let handler move focus
						return false;
					}
					return true;
				}
				return false;
			}
		});
		et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					updatePrefs();
				}
			}
		});
	}

	private void setupListeners()
	{
		setupEditTextListener(device_id);
		setupEditTextListener(divis_id);
		setupEditTextListener(app_version);
		setupEditTextListener(location_id);
		setupEditTextListener(location_name);
		setupEditTextListener(location_detail);
		setupEditTextListener(upper_sensor_depth);
		setupEditTextListener(lower_sensor_depth);
		setupEditTextListener(data_refresh_rate);
	}
}
