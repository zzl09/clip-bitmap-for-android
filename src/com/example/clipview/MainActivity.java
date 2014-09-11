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


package com.example.clipview;

import java.io.File;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;

public class MainActivity extends Activity implements OnClickListener {

    private static final int REQUEST_CODE_TAKE_PHOTO = 2;
    private static final int REQUEST_CODE_CLIP_IMG = 3;
    public static final String EXTRA_KEY_IMAGE_PATH = "extra_key_image_path";
    private Uri mPhotoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_take_photo).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
        case R.id.btn_take_photo:
            ContentValues contentValues = new ContentValues();
            mPhotoUri = MainActivity.this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            startTakePhotoActivity(MainActivity.this, REQUEST_CODE_TAKE_PHOTO, mPhotoUri);
            break;
        default:
            break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
        case REQUEST_CODE_TAKE_PHOTO:
            if (resultCode == RESULT_OK) {
                String photoPath = getPhotoPath(this, mPhotoUri, data);
                if (!TextUtils.isEmpty(photoPath)) {
                    startClipPictureActivity(this, photoPath);
                }
            } else if (resultCode == RESULT_CANCELED) {
                // clear inserted empty data
                MainActivity.this.getContentResolver().delete(mPhotoUri, null, null);
            }
            break;
        case REQUEST_CODE_CLIP_IMG:
            
            break;
        default:
            break;
        }
    }

    public static void startClipPictureActivity(Context context, String imgPath) {
        Intent intent = new Intent(context, ClipActivity.class);
        if (imgPath == null)
            return;
        intent.putExtra(EXTRA_KEY_IMAGE_PATH, imgPath);
        ((Activity) context).startActivityForResult(intent, REQUEST_CODE_CLIP_IMG);
    }

    private static void startTakePhotoActivity(Context context, int requestCode, final Uri photoUri) {
        File file = new File(getPhotoPath(context, photoUri, null));
        Uri fileUri = Uri.fromFile(file);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        ((Activity) context).startActivityForResult(intent, requestCode);
    }

    private static String getPhotoPath(Context context, Uri photoUri, Intent data) {
        String photoPath = "";
        ContentResolver cr = context.getContentResolver();
        if (photoUri != null) {
            Cursor cursor = cr.query(photoUri, null, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                int pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                photoPath = cursor.getString(pathIndex);
                cursor.close();
            }
        } else {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null && !TextUtils.isEmpty(uri.getPath())) {
                    photoPath = uri.getPath();
                    if (!(new File(photoPath).exists())) {
                        String[] proj = { MediaStore.Images.Media.DATA };
                        Cursor actualimagecursor = cr.query(uri, proj, null, null, null);
                        int actualImg = actualimagecursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                        actualimagecursor.moveToFirst();
                        if (actualimagecursor != null)
                            photoPath = actualimagecursor.getString(actualImg);
                    }
                }
            }
        }
        return photoPath;
    }
}
