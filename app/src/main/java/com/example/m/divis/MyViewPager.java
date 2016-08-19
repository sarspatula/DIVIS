package com.example.m.divis;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class MyViewPager extends ViewPager {
	// Note: disabled, rename neverEnabled to enabled below to reenable
	public boolean enabled;

	// NOTE: Disable swipe always rather than only while handling motion events
	private boolean neverEnabled = false;

	public MyViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.enabled = true;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (neverEnabled) {
			return super.onTouchEvent(event);
		}

		return false;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if (neverEnabled) {
			return super.onInterceptTouchEvent(event);
		}

		return false;
	}

	public void setPagingEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
