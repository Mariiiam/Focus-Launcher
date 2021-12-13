package com.android.launcher3.util;

import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;

@RequiresApi(api = Build.VERSION_CODES.N)
public class ProfileTileService extends TileService {

    @Override
    public void onClick() {
        Intent intent = new Intent(this, ManualProfileSelectionActivity.class);
        startActivityAndCollapse(intent);
    }

    /**
     * callback any time the tile becomes visible */
    @Override
    public void onStartListening(){
        String current_profile = getString(R.string.profile_display_default);
        Icon icon = Icon.createWithResource(this, R.drawable.ic_focus_launcher_24dp);
        String label;
        if (Launcher.mSharedPrefs != null){
            current_profile = Launcher.mSharedPrefs.getString("current_profile", "");
        }
        if(current_profile.equals("work")){
            icon = Icon.createWithResource(this, R.drawable.ic_work);
            label = "Work";
        }
        else if(current_profile.equals("home")){
            icon = Icon.createWithResource(this, R.drawable.ic_home);
            label = "Home";
        }
        else if(current_profile.equals("disconnected")){
            icon = Icon.createWithResource(this, R.drawable.ic_offline);
            label = "Disconnected";
        }
        else if(current_profile.equals("default")){
            label = "Default";
        }
        else {
            label = current_profile;
        }

        Tile tile = getQsTile();
        tile.setLabel(label);
        tile.setIcon(icon);
        tile.setContentDescription(getString(R.string.app_name));
        tile.updateTile();
    }
}
