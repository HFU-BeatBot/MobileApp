package com.theriotjoker.beatbot;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import com.theriotjoker.beatbot.databinding.ActivityMainBinding;


import android.view.WindowManager;
import android.widget.Toast;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private boolean pressedBackRecently = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).hide();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
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
    public void onBackPressed() {
        //This is a quick, but dirty way of making the back button transition back to the main screen
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment navHostFragment = fragmentManager.findFragmentById(R.id.nav_host_fragment_content_main);
        Fragment currentFragment = navHostFragment.getChildFragmentManager().getFragments().get(0);
        if(currentFragment instanceof ResultScreen) {
            Navigation.findNavController(navHostFragment.getView()).navigate(R.id.fileScreenToMainScreen);
        } else {
            MainScreen mainScreen =(MainScreen)currentFragment;
            if(mainScreen.isRecording() || mainScreen.isProcessStarted()) {
                if(mainScreen.isRecording()) {
                    mainScreen.stopRecording();
                } else {
                    mainScreen.stopProcess();
                }
            } else {
                if(!pressedBackRecently) {
                    Toast.makeText(currentFragment.requireContext(), "Please press back again to exit the app", Toast.LENGTH_SHORT).show();
                    pressedBackRecently = true;
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            pressedBackRecently = false;
                        }
                    }, 1500);
                } else {
                    pressedBackRecently = false;
                    mainScreen.stopConnectionChecker();
                    finish();
                }
            }
        }

    }
}