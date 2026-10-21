package com.fromsoftware.darksouls2;

import org.libsdl.app.SDLActivity;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;

public class DarkSouls2Activity extends SDLActivity {
    private static final String TAG = "DarkSouls2Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "=== Starting DarkSouls2Activity native SDL runtime v1.0.10 (VTable Adjuster Thunks Fix) ===");
        super.onCreate(savedInstanceState);
    }

    @Override
    protected String[] getLibraries() {
        return new String[] {
            "TracyClientrd",
            "rexruntimerd",
            "rexgpu-xenosrd",
            "ds2"
        };
    }

    @Override
    protected String getMainFunction() {
        return "SDL_main";
    }

    @Override
    protected String[] getArguments() {
        Intent intent = getIntent();
        String gamePath = intent.getStringExtra("game_path");
        if (gamePath == null || gamePath.isEmpty()) {
            gamePath = "/sdcard/DarkSouls2";
        }
        return new String[] {
            gamePath
        };
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "DarkSouls2Activity destroyed.");
        super.onDestroy();
    }
}
