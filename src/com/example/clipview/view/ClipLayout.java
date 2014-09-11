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

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.example.clipview.R;

public class ClipLayout extends RelativeLayout implements OnTouchListener,
        OnClickListener {
    private static final  float MAX_SCALE = 10.0f;
    private static final int DEFAULT_SIZE = 400;

    private ClipView mClipView;
    private ImageView mSouceImageView;

    private int mClipViewHeight = DEFAULT_SIZE;
    private int mClipViewWidth = DEFAULT_SIZE;

    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();
    private Matrix mLastCurrentMatrix = new Matrix();
    private Matrix mBeforeTrackMatrix = new Matrix();

    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    private PointF start = new PointF();
    private PointF mid = new PointF();
    private float oldDist = 1f;

    private DisplayMetrics dm;
    private Bitmap mBitmap;
    private boolean mIsBeginTracking = false;
    private Window mWindow;

    public ClipLayout(Context context) {
        super(context);
        init(context);
    }

    public ClipLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ClipLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        dm = context.getResources().getDisplayMetrics();
        LayoutInflater.from(getContext()).inflate(R.layout.clip_picture_layout,
                this);
        mClipView = (ClipView) findViewById(R.id.clipview);
        mSouceImageView = (ImageView) findViewById(R.id.src_pic);
        mSouceImageView.setOnTouchListener(this);
        findViewById(R.id.bt_roate_left).setOnClickListener(this);
        findViewById(R.id.bt_roate_right).setOnClickListener(this);
        mClipViewWidth = dm.widthPixels * 2 / 3;
        mClipViewHeight = dm.widthPixels * 2 / 3;
        mClipView.setSize(mClipViewWidth, mClipViewHeight);

    }

    /**
     * 以可视区域为中心进行旋转
     * 
     * @param degree
     */
    public void rotate(float degree) {
        RectF rect = mClipView.getClipRect();
        matrix.postRotate(degree, (rect.left + rect.right) / 2,
                (rect.top + rect.bottom) / 2);
        mSouceImageView.setImageMatrix(matrix);
        mBeforeTrackMatrix.set(matrix);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // 用于修正某些手机响起全屏拍完照title会延迟显示
        super.onSizeChanged(w, h, oldw, oldh);
        if (h != 0) {
            centerClip(mBitmap);
            mSouceImageView.setImageMatrix(matrix);
        }
    }

    public void setSourceImage(Bitmap bitmap, Window window) {
        if (bitmap == null)
            return;
        int srcWidth = bitmap.getWidth();
        int srcHeight = bitmap.getHeight();
        float scaleWidth = (float) (mClipViewWidth) / (float) srcWidth;
        float scaleHeight = (float) (mClipViewHeight) / (float) srcHeight;
        float scale = Math.max(scaleWidth, scaleHeight);
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        mBitmap = Bitmap.createBitmap(bitmap, 0, 0, srcWidth, srcHeight,
                matrix, true);
        mSouceImageView.setImageBitmap(mBitmap);

        mWindow = window;
        centerClip(mBitmap);
        mSouceImageView.setImageMatrix(matrix);
    }

    public void onDestory() {
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
            mBitmap = null;
        }
    }

    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
            savedMatrix.set(matrix);
            mLastCurrentMatrix.set(matrix);
            start.set(event.getX(), event.getY());
            mode = DRAG;
            break;
        case MotionEvent.ACTION_POINTER_DOWN:
            oldDist = spacing(event);
            if (oldDist > 10f) {
                savedMatrix.set(matrix);
                midPoint(mid, event);
                mode = ZOOM;
            }
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_POINTER_UP:
            mode = NONE;
            break;
        case MotionEvent.ACTION_MOVE:
            if (mode == DRAG) {
                matrix.set(savedMatrix);
                matrix.postTranslate(event.getX() - start.x, event.getY()
                        - start.y);
            } else if (mode == ZOOM) {
                float newDist = spacing(event);
                if (newDist > 10f) {
                    matrix.set(savedMatrix);
                    float scale = newDist / oldDist;
                    matrix.postScale(scale, scale, mid.x, mid.y);
                    if (!isMatrixOutFrame(matrix)) {
                        mLastCurrentMatrix.set(matrix);
                    }
                }
            }
            break;
        }

        if (isMatrixOutFrame(matrix)) {
            mIsBeginTracking = true;
            Matrix m = new Matrix();
            m.set(matrix);
        } else {
            mIsBeginTracking = false;
            mBeforeTrackMatrix.set(savedMatrix);
        }

        if (event.getAction() == MotionEvent.ACTION_UP
                || event.getAction() == MotionEvent.ACTION_POINTER_UP) {
            if (mIsBeginTracking) {
                mIsBeginTracking = false;
                animate(matrix, mLastCurrentMatrix);
            }else if(isOutScale(matrix)) {
                animate(matrix, savedMatrix);
            }
        } else {
            mSouceImageView.setImageMatrix(matrix);
        }

        return true;
    }

    /** Determine the space between the first two fingers */
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }

    /** Calculate the mid point of the first two fingers */
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        Log.e(event.getX(0) + "", event.getY(0) + "");
        point.set(x / 2, y / 2);
    }

    /**
     * 改变化矩阵是否超出框架
     * 
     * @param matrix
     * @return
     */
    private boolean isMatrixOutFrame(final Matrix matrix) {
        RectF visibleRect = getVisibleRect(matrix);
        RectF snapRect = mClipView.getClipRect();
        return !visibleRect.contains(snapRect);
    }

    /* 获取矩形区域内的图片 */
    public Bitmap getBitmap() {
        Rect rect = getClipRect();
        if (rect == null) {
            return null;
        }
        int avatarDefaultSize = DEFAULT_SIZE;
        float scale = 1;
        int maxEdge = (int) Math.max(rect.width(), rect.height());
        if (maxEdge > avatarDefaultSize) {
            scale = (float) avatarDefaultSize / (float) maxEdge;
        }
        Matrix matrix = new Matrix();
        matrix.setRotate(getDegrees());
        matrix.postScale(scale, scale);
        Bitmap bitmap = Bitmap.createBitmap(mBitmap, (int) (rect.left),
                (int) (rect.top), (int) (rect.right - rect.left),
                (int) (rect.bottom - rect.top), matrix, false);
        return bitmap;
    }

    /**
     * 获取图片可视区域，原圖左上角为top，右下脚为bottom
     * 
     * @return
     */
    private Rect getScaleRect() {
        Rect rect = mSouceImageView.getDrawable().getBounds();
        int width = rect.width();
        int height = rect.height();
        final Matrix matrix = mSouceImageView.getImageMatrix();
        float[] values = new float[9];
        matrix.getValues(values);

        Rect visibleRect = new Rect();
        width = (int) (width * values[8]);
        height = (int) (height * values[8]);
        visibleRect.left = (int) values[2];
        visibleRect.top = (int) values[5];
        visibleRect.right = (int) (visibleRect.left + width * values[0] + height
                * values[1]);
        visibleRect.bottom = (int) (visibleRect.top + height * values[0] - width
                * values[1]);

        return visibleRect;
    }

    /**
     * 获取截图后，针对于原始图片的区域
     * 
     * @return
     */
    private Rect getClipRect() {
        if (mBitmap == null) {
            return null;
        }
        Rect scale = getScaleRect();
        RectF insert = getInsertRect(mSouceImageView.getImageMatrix());

        int originalWidth = mBitmap.getWidth();
        int originalHeight = mBitmap.getHeight();

        Point top = getCloest(new Point(scale.left, scale.top), new Point(
                (int) insert.left, (int) insert.top), new Point(
                (int) insert.left, (int) insert.bottom), new Point(
                (int) insert.right, (int) insert.top), new Point(
                (int) insert.right, (int) insert.bottom));
        Point bottom = getFarthest(new Point(scale.left, scale.top), new Point(
                (int) insert.left, (int) insert.top), new Point(
                (int) insert.left, (int) insert.bottom), new Point(
                (int) insert.right, (int) insert.top), new Point(
                (int) insert.right, (int) insert.bottom));

        // TODO：可否考虑矩阵求逆（以后再讨论）
        int scaleWidth = Math.abs(scale.right - scale.left);
        int scaleHeight = Math.abs(scale.bottom - scale.top);
        float startXPercent = (float) Math.abs((top.x - scale.left))
                / (float) scaleWidth;
        float endXPercent = (float) Math.abs((bottom.x - scale.left))
                / (float) scaleWidth;
        float startYPercent = (float) Math.abs((top.y - scale.top))
                / (float) scaleHeight;
        float endYPercent = (float) Math.abs((bottom.y - scale.top))
                / (float) scaleHeight;
        int degrees = getDegrees();
        if (degrees == 90 || degrees == 270) {
            scaleWidth = Math.abs(scale.bottom - scale.top);
            scaleHeight = Math.abs(scale.right - scale.left);
            startXPercent = (float) Math.abs((top.y - scale.top))
                    / (float) scaleWidth;
            endXPercent = (float) Math.abs((bottom.y - scale.top))
                    / (float) scaleWidth;
            startYPercent = (float) Math.abs((top.x - scale.left))
                    / (float) scaleHeight;
            endYPercent = (float) Math.abs((bottom.x - scale.left))
                    / (float) scaleHeight;
        }

        Rect resultRec = new Rect((int) (originalWidth * startXPercent),
                (int) (originalHeight * startYPercent),
                (int) (originalWidth * endXPercent),
                (int) (originalHeight * endYPercent));
        return resultRec;
    }

    private Point getFarthest(final Point target, Point... points) {

        Comparator<Point> comparator = new Comparator<Point>() {

            @Override
            public int compare(Point o1, Point o2) {
                double p1_Top = Math.pow(o1.x - target.x, 2)
                        + Math.pow(o1.y - target.y, 2);
                double p2_Top = Math.pow(o2.x - target.x, 2)
                        + Math.pow(o2.y - target.y, 2);

                if (p1_Top < p2_Top)
                    return 1;
                else if (p1_Top == p2_Top)
                    return 0;
                else
                    return -1;
            }
        };

        List<Point> list = Arrays.asList(points);
        Collections.sort(list, comparator);
        return list.get(0);
    }

    private Point getCloest(final Point target, Point... points) {

        Comparator<Point> comparator = new Comparator<Point>() {

            @Override
            public int compare(Point o1, Point o2) {
                double p1_Top = Math.pow(o1.x - target.x, 2)
                        + Math.pow(o1.y - target.y, 2);
                double p2_Top = Math.pow(o2.x - target.x, 2)
                        + Math.pow(o2.y - target.y, 2);

                if (p1_Top < p2_Top)
                    return -1;
                else if (p1_Top == p2_Top)
                    return 0;
                else
                    return 1;
            }
        };

        List<Point> list = Arrays.asList(points);
        Collections.sort(list, comparator);
        return list.get(0);
    }

    private int getDegrees() {
        Rect scale = getScaleRect();
        int degrees = 0;
        if (scale.left > scale.right && scale.top < scale.bottom)
            degrees = 90;
        else if (scale.left > scale.right && scale.top > scale.bottom)
            degrees = 180;
        else if (scale.left < scale.right && scale.top > scale.bottom)
            degrees = 270;
        else
            degrees = 0;
        return degrees;
    }

    int statusBarHeight = 0;
    int titleBarHeight = 0;

    private void getBarHeight(Window window) {
        if (window == null) {
            return;
        }
        // 获取状态栏高度
        Rect frame = new Rect();
        View decorView = window.getDecorView();
        if (decorView == null) {
            return;
        }
        decorView.getWindowVisibleDisplayFrame(frame);
        statusBarHeight = frame.top;

        int contenttop = window.findViewById(Window.ID_ANDROID_CONTENT)
                .getTop();
        // statusBarHeight是上面所求的状态栏的高度
        titleBarHeight = contenttop - statusBarHeight;

    }

    private boolean isOutScale(Matrix matrix) {
    boolean isOutMaxScale=false;
    float p[] = new float[9];
    matrix.getValues(p);
    float minScaleSize=Math.min(getVisibleRect(matrix).width(), getVisibleRect(matrix).height());
    if (minScaleSize > (getWidth() * MAX_SCALE)) {
        isOutMaxScale = true;
    }
    return isOutMaxScale;
}
    
    private void scaleFit(Bitmap bitmap, float width, float height) {
        if (bitmap == null) {
            return;
        }
        Matrix m = new Matrix();
        m.set(matrix);
        RectF rect = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
        m.mapRect(rect);

        float p[] = new float[9];
        matrix.getValues(p);
        float sourceHeight = bitmap.getWidth();
        float sourceWidth = bitmap.getHeight();
        float disHeight = sourceHeight - height;
        float disWidth = sourceWidth - width;
        if (sourceHeight > height && sourceWidth > sourceWidth) {
            if (disHeight > disWidth) {
                matrix.setScale(height / sourceHeight, height / sourceHeight);
            } else {
                matrix.setScale(width / sourceWidth, width / sourceWidth);
            }
        }
        if (sourceHeight < height && sourceWidth < width) {
            if (disHeight < disWidth) {
                matrix.setScale(height / sourceHeight, height / sourceHeight,
                        getWidth() / 2, 0);

            } else {
                matrix.setScale(width / sourceWidth, width / sourceWidth,
                        getWidth() / 2, 0);
            }
        }

    }

    private boolean isChange = true;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (isChange) {
            isChange = false;
            scaleFit(mBitmap, mClipViewWidth, mClipViewHeight);
            mSouceImageView.setImageMatrix(matrix);
            centerClip(mBitmap);
            mSouceImageView.setImageMatrix(matrix);
        }
    }

    /**
     * 横向、纵向居中
     */
    private void centerClip(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        getBarHeight(mWindow);
        Matrix m = new Matrix();
        m.set(matrix);
        RectF rect = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
        m.mapRect(rect);

        float height = rect.height();
        float width = rect.width();

        float deltaX = 0;
        float deltaY = 0;
        int screenHeight = this.getHeight();
        if (height < screenHeight) {
            deltaY = (screenHeight - height) / 2 - rect.top;
        } else if (rect.top > 0) {
            deltaY = -rect.top;
        } else if (rect.bottom < screenHeight) {
            deltaY = this.getHeight() - rect.bottom;
        }
        int screenWidth = dm.widthPixels;
        if (width < screenWidth) {
            deltaX = (screenWidth - width) / 2 - rect.left;
        } else if (rect.left > 0) {
            deltaX = -rect.left;
        } else if (rect.right < screenWidth) {
            deltaX = screenWidth - rect.right;
        }
        matrix.postTranslate(deltaX, deltaY);
    }

    /**
     * @param fromMatrix
     * @param toMatrix
     */
    private void animate(Matrix fromMatrix, Matrix toMatrix) {
        RectF fromRect = getVisibleRect(fromMatrix);
        RectF toRect = getVisibleRect(toMatrix);
        boolean isTranslate = fromRect.width() == toRect.width();
        if (isTranslate) {
            fixMatrix(fromMatrix, toMatrix);
        }
        float[] fromValues = new float[9];
        fromMatrix.getValues(fromValues);
        int fromLeft = (int) fromValues[2];
        int fromTop = (int) fromValues[5];

        float[] toValues = new float[9];
        toMatrix.getValues(toValues);
        int toLeft = (int) toValues[2];
        int toTop = (int) toValues[5];

        int gap = 15;

        if (isTranslate) {
            // 只是坐标平移变换
            int maxTimes = 10;
            // int minDis=10;
            for (int i = 0; i < maxTimes; i++) {
                int dx = (toLeft - fromLeft) * (i + 1) / maxTimes;
                int dy = (toTop - fromTop) * (i + 1) / maxTimes;
                Matrix matrix = new Matrix(fromMatrix);
                matrix.postTranslate(dx, dy);
                Message msg = mRollbackHandler.obtainMessage();
                msg.what = 0;
                msg.obj = matrix;
                msg.arg1 = MSG_ANIM_ON;
                mRollbackHandler.sendMessageDelayed(msg, gap * i);
            }
            Message lastMsg = new Message();
            lastMsg.what = 0;
            lastMsg.obj = new Matrix(toMatrix);
            lastMsg.arg1 = MSG_ANIM_END;
            mRollbackHandler.sendMessageDelayed(lastMsg, gap * maxTimes);

        } else {
            // 只是拉伸变換
            int maxTimes = 10;
            // int minDis=10;
            float base = (float) toRect.width() / (float) fromRect.width();
            float delta = (float) Math.pow(base, (float) 1 / (float) maxTimes);
            Matrix matrix = new Matrix(fromMatrix);
            for (int i = 0; i < maxTimes; i++) {
                /*
                 * matrix.postScale(delta, delta, fromRect.left + fromRect.right
                 * / 2, toRect.top + toRect.bottom / 2);
                 */
                matrix.postScale(delta, delta, mid.x, mid.y);
                Matrix sendMatrix = new Matrix(matrix);
                Message msg = mRollbackHandler.obtainMessage();
                msg.what = 0;
                msg.obj = sendMatrix;
                msg.arg1 = MSG_ANIM_ON;
                mRollbackHandler.sendMessageDelayed(msg, gap * i);
            }
            Message lastMsg = mRollbackHandler.obtainMessage();
            lastMsg.what = 0;
            lastMsg.obj = new Matrix(toMatrix);
            lastMsg.arg1 = MSG_ANIM_END;
            mRollbackHandler.sendMessageDelayed(lastMsg, gap * maxTimes);

        }

    }

    /**
     * 返回交叉区域
     * 
     * @return
     */
    private RectF getInsertRect(final Matrix matrix) {
        RectF visibleRect = getVisibleRect(matrix);
        RectF snapRect = mClipView.getClipRect();
        visibleRect.intersect(snapRect);
        return visibleRect;
    }

    // 平移回弹修正
    private void fixMatrix(Matrix fromMatrix, Matrix toMatrix) {
        RectF insert = getInsertRect(fromMatrix);
        RectF snapRect = mClipView.getClipRect();

        boolean hasInsert = insert.width() * insert.height() > 0;

        float[] fromValues = new float[9];
        fromMatrix.getValues(fromValues);
        float[] toValues = new float[9];
        toMatrix.getValues(toValues);

        int fromLeft = (int) fromValues[Matrix.MTRANS_X];
        int fromTop = (int) fromValues[Matrix.MTRANS_Y];
        int toLeft = fromLeft;
        int toTop = fromTop;

        if (isInside(snapRect, new PointF(insert.left, insert.top))) {
            toLeft += snapRect.left - insert.left;
            toTop += snapRect.top - insert.top;
        } else if (isInside(snapRect, new PointF(insert.right, insert.top))) {
            toLeft += snapRect.right - insert.right;
            toTop += snapRect.top - insert.top;

        } else if (isInside(snapRect, new PointF(insert.left, insert.bottom))) {
            toLeft += snapRect.left - insert.left;
            toTop += snapRect.bottom - insert.bottom;

        } else if (isInside(snapRect, new PointF(insert.right, insert.bottom))) {
            toLeft += snapRect.right - insert.right;
            toTop += snapRect.bottom - insert.bottom;
        } else if (insert.left > snapRect.left && insert.left <= snapRect.right
                && insert.top <= snapRect.top
                && insert.bottom >= snapRect.bottom) {
            toLeft += snapRect.left - insert.left;
        } else if (hasInsert && insert.right < snapRect.right
                && insert.right >= snapRect.left && insert.top <= snapRect.top
                && insert.bottom >= snapRect.bottom) {
            toLeft += snapRect.right - insert.right;
        } else if (hasInsert && insert.top > snapRect.top
                && insert.top <= snapRect.bottom
                && insert.left <= snapRect.left
                && insert.right >= snapRect.right) {
            toTop += snapRect.top - insert.top;
        } else if (hasInsert && insert.bottom < snapRect.bottom
                && insert.bottom >= snapRect.top
                && insert.left <= snapRect.left
                && insert.right >= snapRect.right) {
            toTop += snapRect.bottom - insert.bottom;
        } else {
            toLeft = (int) toValues[Matrix.MTRANS_X];
            toTop = (int) toValues[Matrix.MTRANS_Y];

        }
        toValues[Matrix.MTRANS_X] = toLeft;
        toValues[Matrix.MTRANS_Y] = toTop;
        toMatrix.setValues(toValues);
    }

    private boolean isInside(RectF rect, PointF point) {
        return point.x < rect.right && point.x > rect.left
                && point.y < rect.bottom && point.y > rect.top;
    }

    /**
     * 获取图片可视区域，屏幕左上角为top，右下脚为bottom
     * 
     * @return
     */
    private RectF getVisibleRect(final Matrix matrix) {
        Drawable drawable = mSouceImageView.getDrawable();
        if (drawable == null) { // 可能为空，故加上null判断
            return new RectF(0, 0, 0, 0);
        }
        Rect rect = drawable.getBounds();
        int width = rect.width();
        int height = rect.height();
        // final Matrix matrix = mImageView.getImageMatrix();
        float[] values = new float[9];
        matrix.getValues(values);
        Rect visibleRect = new Rect();
        width = (int) (width * values[8]);
        height = (int) (height * values[8]);
        visibleRect.left = (int) values[2];
        visibleRect.top = (int) values[5];
        visibleRect.right = (int) (visibleRect.left + width * values[0] + height
                * values[1]);
        visibleRect.bottom = (int) (visibleRect.top + height * values[0] - width
                * values[1]);

        RectF newRect = new RectF();
        newRect.left = Math.min(visibleRect.left, visibleRect.right);
        newRect.top = Math.min(visibleRect.top, visibleRect.bottom);
        newRect.right = Math.max(visibleRect.left, visibleRect.right);
        newRect.bottom = Math.max(visibleRect.top, visibleRect.bottom);

        return newRect;
    }

    private static final int MSG_ANIM_ON = 0;
    private static final int MSG_ANIM_END = 1;

    private Handler mRollbackHandler = new Handler(new Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Matrix mmatrix = (Matrix) msg.obj;
            mSouceImageView.setImageMatrix(mmatrix);
            if (msg.arg1 == MSG_ANIM_END) {
                matrix.set(mmatrix);
                savedMatrix.set(mmatrix);
                mode = NONE;
            }
            return true;
        }
    });

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
        case R.id.bt_roate_left:
            rotate(-90.0f);
            break;
        case R.id.bt_roate_right:
            rotate(90.0f);
            break;
        default:
            break;
        }
    }
    
}
