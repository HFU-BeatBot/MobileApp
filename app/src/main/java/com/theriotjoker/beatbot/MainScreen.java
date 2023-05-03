package com.theriotjoker.beatbot;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.theriotjoker.beatbot.databinding.FragmentFirstBinding;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Random;

public class MainScreen extends Fragment {

    private FragmentFirstBinding binding;
    private ActivityResultLauncher<Intent> startActivityIntent;
    private MediaRecorder mediaRecorder;
    private boolean isRecording;
    private Intent chosenFileIntent;
    private FileUploadController fileUploadController;
    private final Handler handler = new Handler();
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fileUploadController = new FileUploadController(this);
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        requireActivity().getWindow().setBackgroundDrawable(container.getBackground());
        startActivityIntent = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    chosenFileIntent = result.getData();
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        buildDialogForFile(result.getData());
                    } else {
                        Toast.makeText(getContext(),"File not supported...", Toast.LENGTH_SHORT).show();
                    }
                });

        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, 30000);
        }
        isRecording = false;
        return binding.getRoot();
    }
    private void buildDialogForFile(Intent result) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
        alertDialogBuilder.setMessage("You have chosen the following file: "+fileUploadController.getFileNameFromUri(result.getData())+". Do you want to upload it?")
                .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        fileUploadController.getMusicGenreFromUri(result.getData());

                    }
                }).setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());
        AlertDialog messageDialog = alertDialogBuilder.create();
        messageDialog.show();
    }


    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        binding.testButton.setVisibility(View.INVISIBLE);
        //TODO setVisibility of Debug Button
        super.onViewCreated(view, savedInstanceState);
        AnimationDrawable animationDrawable = (AnimationDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.gradient_animation, null);
        if(animationDrawable != null) {
            animationDrawable.setEnterFadeDuration(10);
            animationDrawable.setExitFadeDuration(1750);
            requireView().setBackground(animationDrawable);
        }
        //NavHostFragment.findNavController(MainScreen.this).navigate(R.id.mainScreenToFileScreen)
        binding.useFileButton.setOnClickListener(view1 -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT); //Create an intent to get a file from the filesystem
            intent.setType("audio/*"); //the file type should be wave
            startActivityIntent.launch(intent);

        });

        binding.bbButton.setOnClickListener(view2 -> {
            File f = new File(requireActivity().getCacheDir(), "temp.mp4");
            if(isRecording) {

                mediaRecorder.stop();
                mediaRecorder.reset();
                mediaRecorder.release();
                System.out.println("DOES THE RECORDED FILE EXIST? "+f.exists()+ " info = ");
                stopPulsing();
                if(animationDrawable != null) {
                    animationDrawable.stop();
                }
                binding.useFileButton.setEnabled(true);
            } else {
                binding.useFileButton.setEnabled(false);
                if(animationDrawable != null) {
                    animationDrawable.start();
                }
                startPulsing();
                mediaRecorder = new MediaRecorder();
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mediaRecorder.setOutputFile(f);
                try {
                    mediaRecorder.prepare();
                    mediaRecorder.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            isRecording = !isRecording;
        });
        //TODO: remove debug
        binding.testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaPlayer player = new MediaPlayer();
                try {
                    File file = new File(requireContext().getCacheDir(), "temp.mp4");
                    player.setDataSource(file.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                /*player.prepareAsync();
                player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        mediaPlayer.start();
                    }
                });*/
            }
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void startPulsing() {
        animationRunnable.run();
    }

    private void stopPulsing() {
        handler.removeCallbacks(animationRunnable);
    }

    private final Runnable animationRunnable = new Runnable() {

        //makes the pulsing animation for the "BB" Button when recording.
        @Override
        public void run() {

            pulse(binding.pulseImage1,1000,3.0f);
            pulse(binding.pulseImage2,700,3.0f);
            handler.postDelayed(this, 1500);
        }
    };

    private void pulse(ImageView pulsatingImage, long duration, float scale){
        //duration describes how fast the circle expand, scale describes its maximum size.
        pulsatingImage.animate().alpha(0.0f).scaleX(scale).scaleY(scale).setDuration(duration).withEndAction(() ->
                pulsatingImage.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(0)
        );
    }
}