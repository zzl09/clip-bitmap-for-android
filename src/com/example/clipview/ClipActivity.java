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
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Toast;

import com.example.clipview.utils.BitmapUtils;
import com.example.clipview.view.ClipLayout;

public class ClipActivity extends Activity implements OnClickListener {
    ClipLayout mClipLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clip_picture);
        initView();
        initBitmap();
    }

    private void initView() {
        mClipLayout = (ClipLayout) findViewById(R.id.clip_layout);
        findViewById(R.id.ok).setOnClickListener(this);
        findViewById(R.id.cancel).setOnClickListener(this);
    }

    private void initBitmap() {
        String imgPath = getIntent().getStringExtra(MainActivity.EXTRA_KEY_IMAGE_PATH);
        File file = new File(imgPath);
        if (file.exists()) {
            Window window = getWindow();
            mClipLayout.setSourceImage(BitmapUtils.createImageThumbnailScale(imgPath, 800), window);
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mClipLayout.onDestory();
    }

    private void clipBitmap() {
        Bitmap bitmap = mClipLayout.getBitmap();
        Toast.makeText(getApplicationContext(), "clipBitmap() finish just handler the bitmap", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.ok:
            clipBitmap();
            break;
        case R.id.cancel:
            Toast.makeText(getApplicationContext(), "cancel=>finish", Toast.LENGTH_SHORT).show();
            break;
        default:
            break;
        }
    }
}
