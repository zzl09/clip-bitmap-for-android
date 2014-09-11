/*
 * Copyright (C) 2014 zzl09
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.example.clipview.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * 
 * @author huyx
 * @desc 阴影部分view
 */
public class ClipView extends View {
	private final int SHADOW_COLOR = 0x7f000000;
	private int mClipWidth = 0;
	private int mClipHeight = 0;
	private Bitmap mRectBitmap; // 用于背景缓存
	private Paint mEmptyPaint = new Paint();

	public ClipView(Context context) {
		super(context);
	}

	public ClipView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ClipView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		refreshRectBitmap();
	}

	/**
	 * 刷新全局背景
	 */
	private void refreshRectBitmap() {
		Bitmap recBitmap = mRectBitmap;
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		Paint transparentPaint;
		mRectBitmap = Bitmap.createBitmap(getWidth(), getHeight(),
		        Bitmap.Config.ARGB_8888);
		Canvas temp = new Canvas(mRectBitmap);
		RectF clipRect = getClipRect();
		paint.setColor(SHADOW_COLOR);
		temp.drawRect(0, 0, temp.getWidth(), temp.getHeight(), paint);
		transparentPaint = new Paint();
		transparentPaint.setAntiAlias(true);
		transparentPaint.setColor(getResources().getColor(
		        android.R.color.transparent));
		transparentPaint.setXfermode(new PorterDuffXfermode(
		        PorterDuff.Mode.CLEAR));
		temp.drawCircle((clipRect.left + clipRect.right) / 2,
		        (clipRect.top + clipRect.bottom) / 2,
		        (clipRect.right - clipRect.left) / 2, transparentPaint);
		if (recBitmap != null) {
			recBitmap.recycle();
			recBitmap = null;
		}
	}

	/**
	 * 获取截取区域位置信息
	 * 
	 * @return
	 */
	public RectF getClipRect() {
		RectF result = new RectF();
		int width = this.getWidth();
		int height = this.getHeight();
		if (mClipWidth != 0 && mClipHeight != 0) {
			int x = (int) ((width - mClipWidth) / 2);
			int y = (int) ((height - mClipHeight) / 2);
			// int y = 1;
			if (x > 0 && y > 0) {
				result.set(x, y, x + mClipWidth, y + mClipHeight);
			} else {
				Log.e("ClipView", "Clip cal err");
			}
		}
		return result;

	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (mRectBitmap !=null) {
		    canvas.drawBitmap(mRectBitmap, 0, 0, mEmptyPaint);
		}
	}
	
	

	@Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mRectBitmap != null) {
            mRectBitmap.recycle();
        }
    }

	/**
	 * 设置宽高
	 * 
	 * @param clipViewWidth
	 * @param clipViewHeight
	 */
	public void setSize(int clipViewWidth, int clipViewHeight) {
		mClipWidth = clipViewWidth;
		mClipHeight = clipViewHeight;
	}

}
