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


package com.example.clipview.utils;

import java.io.FileDescriptor;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

public class BitmapUtils {
    public static Bitmap decodeBitmapFromPathUri(final Uri pathUri, final Activity activity) {
        return decodeBitmapFromPathUri(pathUri, activity, true);
    }

    public static Bitmap decodeBitmapFromPathUri(final Uri pathUri, final Activity activity, boolean isLow) {
        int size = 800;
        return compressImage(size, 0, true, isLow, new IBitMapReaderCallBack() {
            @Override
            public Bitmap getBitMap(Options options) {
                try {
                    ParcelFileDescriptor parcelFileDescriptor = activity.getContentResolver().openFileDescriptor(pathUri, "r");
                    FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                    return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
                } catch (Throwable t) {
                    if (t != null)
                        Log.e("exception", "BitmapUtil.decodeBitmapFromPathUri(Uri pathUri, Activity activity) Exception " + t.getMessage());
                    return null;
                }
            }
        });
    }

    /**
     * 按比例生成图片缩略图
     * 
     * @param filePath
     * @param size
     *            区域大小
     * @return
     */
    public static Bitmap createImageThumbnailScale(final String filePath, int size) {
        return createImageThumbnailScale(filePath, size, true);
    }

    /**
     * 按比例生成图片缩略图
     * 
     * @param filePath
     * @param size
     *            区域大小
     * @param isLow
     *            是低品质图片
     * @return
     */
    public static Bitmap createImageThumbnailScale(final String filePath, int size, boolean isLow) {
        return compressImage(size, 0, true, isLow, new IBitMapReaderCallBack() {
            @Override
            public Bitmap getBitMap(Options options) {
                return BitmapFactory.decodeFile(filePath, options);
            }
        });
    }

    /**
     * 不按比例生成图片缩略图，按假定显示最大分辨率{@code MAX_NUM_PIXELS_THUMBNAIL}来计算inSampleSize；
     * 再按指定宽高生成缩略图
     * 
     * @param filePath
     * @param size
     *            区域大小
     * @return
     */
    public static Bitmap createImageThumbnail(final String filePath, int size) {
        return createImageThumbnail(filePath, size, true);
    }

    /**
     * 不按比例生成图片缩略图，按假定显示最大分辨率{@code MAX_NUM_PIXELS_THUMBNAIL}来计算inSampleSize；
     * 再按指定宽高生成缩略图
     * 
     * @param filePath
     * @param size
     *            区域大小
     * @param isLow
     *            是低品质图片
     * @return
     */
    public static Bitmap createImageThumbnail(final String filePath, int size, boolean isLow) {
        return compressImage(size, 0, false, isLow, new IBitMapReaderCallBack() {
            @Override
            public Bitmap getBitMap(Options options) {
                return BitmapFactory.decodeFile(filePath, options);
            }
        });
    }


    private static int calculateInSampleSize(float outWidth, float outHeight, float reqWidth, float reqHeight) {
        int inSampleSize = 1;
        if (reqHeight == 0 || reqWidth == 0) {
            return inSampleSize;
        }
        if (outHeight > reqHeight || outWidth > reqWidth) {
            final int heightRatio = Math.round(outHeight / reqHeight);
            final int widthRatio = Math.round(outWidth / reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        final float totalPixels = outWidth * outHeight;
        final float totalReqPixelsCap = reqWidth * reqHeight * 2;

        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }

        return inSampleSize;
    }

    private static Matrix getMatrix(int orientation) {
        Matrix matrix = new Matrix();
        if (orientation == 90 || orientation == 180 || orientation == 270) {
            matrix.postRotate(orientation);
        }
        return matrix;
    }

    private static Bitmap compressImage(float size, int orientation, boolean scale, boolean isLow, IBitMapReaderCallBack bitMapReaderCallBack) {
        Bitmap bmp = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            bmp = bitMapReaderCallBack.getBitMap(options);

            float actualHeight = options.outHeight;
            float actualWidth = options.outWidth;

            float destHeight = size;
            float destWidth = size;
            // 解析过程出错，options.outHeight = -1
            if (actualHeight <= 0 || actualWidth <= 0 || size <= 0) {
                return null;
            }
            if (scale) {
                if (actualHeight > actualWidth) {
                    destWidth = (actualWidth * size) / actualHeight;
                    destHeight = size;
                } else if (actualWidth > actualHeight) {
                    destHeight = (actualHeight * size) / actualWidth;
                    destWidth = size;
                }
            }

            options.inSampleSize = calculateInSampleSize((float) options.outWidth, (float) options.outHeight, destWidth, destHeight);
            options.inJustDecodeBounds = false;
            options.inPurgeable = true;
            options.inInputShareable = true;
            options.inTempStorage = new byte[16 * 1024];

            Bitmap scaledBitmap = null;

            bmp = bitMapReaderCallBack.getBitMap(options);
            if (bmp == null)
                return bmp;
            scaledBitmap = Bitmap.createBitmap((int) destWidth, (int) destHeight, isLow ? Bitmap.Config.RGB_565 : Bitmap.Config.ARGB_8888);

            float ratioX = destWidth / (float) options.outWidth;
            float ratioY = destHeight / (float) options.outHeight;
            float middleX = destWidth / 2.0f;
            float middleY = destHeight / 2.0f;
            if (!scale) {
                // 计算非缩放下的放大倍数及放大后中心点
                ratioX = ratioX > ratioY ? ratioX : ratioY;
                ratioY = ratioX;
                middleX = (((float) options.outWidth) * ratioX) / 2.0f;
                middleY = (((float) options.outHeight) * ratioY) / 2.0f;
            }

            Matrix scaleMatrix = new Matrix();
            scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

            ColorMatrix mSaturationMatrix = new ColorMatrix();
            mSaturationMatrix.reset(); // 设为默认值
            mSaturationMatrix.setSaturation(1.3f);

            Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
            paint.setColorFilter(new ColorMatrixColorFilter(mSaturationMatrix));

            Canvas canvas = new Canvas(scaledBitmap);
            canvas.setMatrix(scaleMatrix);

            float x = middleX - ((float) bmp.getWidth()) / 2.0f;
            float y = middleY - ((float) bmp.getHeight()) / 2.0f;
            if (!scale) {
                // 计算截取图片中心缩放中心坐标
                if (bmp.getWidth() > bmp.getHeight()) {
                    x = x - ((bmp.getWidth() - bmp.getHeight()) / 2.0f);
                } else {
                    y = y - ((bmp.getHeight() - bmp.getWidth()) / 2.0f);
                }
            }
            canvas.drawBitmap(bmp, x, y, paint);
            Matrix matrix = getMatrix(orientation);
            scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);

            return scaledBitmap;
        } catch (Throwable t) {
            if (t != null)
                Log.e("exception", "BitmapUtil.decodeBitmapFromPath(String path, Activity activity) Exception " + t.getMessage());
            return null;
        } finally {
            if (bmp != null && !bmp.isRecycled()) {
                bmp.recycle();
                bmp = null;
                System.gc();
            }
        }
    }

    private interface IBitMapReaderCallBack {
        public Bitmap getBitMap(BitmapFactory.Options options);
    }

}
