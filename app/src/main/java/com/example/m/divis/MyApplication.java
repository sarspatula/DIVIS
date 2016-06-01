package com.example.m.divis;

// automatic crash reports for android
import android.app.Application;
import android.content.Context;

import com.example.m.divis.R;

import org.acra.*;
import org.acra.annotation.*;

@ReportsCrashes(
		mailTo = "mfranco@gmx.com",
		mode = ReportingInteractionMode.TOAST,
		resToastText = R.string.crash_toast_text)

public class MyApplication extends Application {
	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);

		// The following line triggers the initialization of ACRA
		ACRA.init(this);
	}
}
