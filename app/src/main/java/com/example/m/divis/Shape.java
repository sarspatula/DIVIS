package com.example.m.divis;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;

public class Shape {
	public Point center;
	public int radius;
	public int stroke_width;
	public int color;
	public ShapeDrawable drawable;

	Shape()
	{
		center = new Point();
		radius = 100;
		stroke_width = 5;
		color = Color.rgb(255, 255, 255);
		update();
	}

	Shape(int center_x, int center_y, int radius)
	{
		this();
		center.x = center_x;
		center.y = center_y;
		this.radius = radius;
		update();
	}

	public Drawable update()
	{
		drawable = new ShapeDrawable(new OvalShape());
		drawable.getPaint().setStrokeWidth(stroke_width);
		drawable.getPaint().setAntiAlias(true);
		drawable.getPaint().setColor(color);
		drawable.getPaint().setStyle(Paint.Style.STROKE);

		drawable.setIntrinsicWidth(radius*2);
		drawable.setIntrinsicHeight(radius*2);

        return drawable;
	}
}
