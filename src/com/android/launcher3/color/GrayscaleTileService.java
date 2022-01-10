package com.android.launcher3.color;


import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.service.quicksettings.TileService;
import android.support.annotation.RequiresApi;

import com.android.launcher3.QuestionGrayscaleDialog;

@RequiresApi(api = Build.VERSION_CODES.N)
public class GrayscaleTileService extends TileService {
    @Override
    public void onClick() {
        Intent intent = new Intent(this, QuestionGrayscaleDialog.class);
        startActivityAndCollapse(intent);
    }
}