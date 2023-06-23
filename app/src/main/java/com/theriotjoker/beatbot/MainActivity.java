package com.theriotjoker.beatbot;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.Navigation;
import com.theriotjoker.beatbot.databinding.ActivityMainBinding;


import android.view.WindowManager;
import android.widget.Toast;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private boolean pressedBackRecently = false;
    private static boolean appPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.theriotjoker.beatbot.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).hide();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    123);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        appPaused = true;
    }
    @Override
    protected void onResume() {
        super.onResume();
        appPaused = false;
    }
    public static boolean isAppPaused() {
        return appPaused;
    }

    //This function controls what happens when the back button is pressed
    // Possible outcomes:
    // It will work as a return button if the current screen is resultscreen, going back to the main screen
    // Otherwise if a conversion or recording is running, it will stop that
    // Or if the user presses the back button twice quickly, it will exit the app
    @Override
    public void onBackPressed() {
        //Get the fragment manager, a class that contains all the fragments of the app
        FragmentManager fragmentManager = getSupportFragmentManager();
        //use the fragment manager to find the fragment that has all the content
        Fragment navHostFragment = fragmentManager.findFragmentById(R.id.nav_host_fragment_content_main);
        if(navHostFragment == null) {
            return;
        }
        //get the fragment currently displayed
        Fragment currentFragment = navHostFragment.getChildFragmentManager().getFragments().get(0);
        if(currentFragment instanceof ResultScreen) {
            //return if we are on the result screen
            if(navHostFragment.getView() != null) {
                Navigation.findNavController(navHostFragment.getView()).navigate(R.id.fileScreenToMainScreen);
            }
        } else {
            //otherwise we are on the main screen
            MainScreen mainScreen =(MainScreen)currentFragment;
            //if we are recording or processing a song, stop that
            if(mainScreen.isRecording() || mainScreen.isProcessStarted()) {
                if(mainScreen.isRecording()) {
                    mainScreen.stopRecording();
                } else {
                    mainScreen.stopProcess();
                }
            } else {
                //otherwise the user needs to press back twice quickly to exit the app
                if(!pressedBackRecently) {
                    Toast.makeText(currentFragment.requireContext(), "Please press back again to exit the app", Toast.LENGTH_SHORT).show();
                    pressedBackRecently = true;
                    new Handler(Looper.getMainLooper()).postDelayed(() -> pressedBackRecently = false, 1500);
                } else {
                    //The routine that closes the app
                    pressedBackRecently = false;
                    mainScreen.exit();
                    finish();
                }
            }
        }

    }
}