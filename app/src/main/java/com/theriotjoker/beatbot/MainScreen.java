package com.theriotjoker.beatbot;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.theriotjoker.beatbot.databinding.FragmentFirstBinding;

import java.io.File;
import java.util.Random;


public class MainScreen extends Fragment {

    private FragmentFirstBinding binding;
    private ActivityResultLauncher<Intent> startActivityIntent;
    private boolean isRecording;
    private FileUploadController fileUploadController;
    private final Handler handler = new Handler();
    private ProgressBar progressBar;
    private WaveRecorder waveRecorder;
    private AnimationDrawable animationDrawable;
    private boolean recordingCooldown;
    private final static int[] drawableIds = {R.drawable.blues_1, R.drawable.classical_1, R.drawable.country_1,R.drawable.disco_1, R.drawable.hiphop_1, R.drawable.jazz_1, R.drawable.metal_1, R.drawable.pop_1, R.drawable.reggae_1, R.drawable.rock_1};
    private int animationImageChooser = 0;
    private ImageButton recordButton;
    private Button useFileButton;
    private TextView infoTextView;
    private TextView onlineStatusTextView;
    private ImageView backgroundImage;
    private final Runnable animationRunnable = new Runnable() {

        //makes the pulsing animation for the "BB" Button when recording.
        @Override
        public void run() {
            pulsate(binding.pulseImage1,1000,3.0f);
            pulsate(binding.pulseImage2,700,3.0f);
            handler.postDelayed(this, 1500);
        }
    };
    private final Runnable backgroundAnimationRunnable = new Runnable() {
        final int ANIMATION_LENGTH_MILLISECONDS = 2000;
        @Override
        public void run() {
            if(getContext() != null) {
                backgroundImage.animate().alpha(0.0f).setDuration(ANIMATION_LENGTH_MILLISECONDS).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        backgroundImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), drawableIds[animationImageChooser], null));
                        backgroundImage.animate().alpha(1.0f).setDuration(ANIMATION_LENGTH_MILLISECONDS);
                    }
                });
                handler.postDelayed(this, (int) (ANIMATION_LENGTH_MILLISECONDS * 2.5));
                animationImageChooser = (animationImageChooser + 1) % drawableIds.length;
            }
        }
    };

    public boolean isProcessStarted() {
        return fileUploadController.isProcessStarted();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fileUploadController = new FileUploadController(this);
        waveRecorder = new WaveRecorder(requireContext().getCacheDir().getPath());
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        requireActivity().getWindow().setBackgroundDrawable(container.getBackground());
        useFileButton = binding.useFileButton;
        recordButton = binding.bbButton;
        progressBar = binding.progressBar;
        infoTextView = binding.infoTextView;
        onlineStatusTextView = binding.onlineStatus;
        backgroundImage = binding.backgroundImage;
        recordingCooldown = false;
        startActivityIntent = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        buildDialogForFile(result.getData());
                    } else {
                        Toast.makeText(getContext(),"No file selected.", Toast.LENGTH_SHORT).show();
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
                        startBackgroundAnimation();
                        backgroundImage.setVisibility(View.INVISIBLE);
                        fileUploadController.getGenreFromUri(result.getData());
                    }
                }).setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());
        AlertDialog messageDialog = alertDialogBuilder.create();
        messageDialog.show();
    }

    public void exit() {
        handler.removeCallbacks(backgroundAnimationRunnable);
        fileUploadController.stopConnectionChecker();
    }


    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        setButtonsEnabled(false);
        setConnectionAvailable(false);

        super.onViewCreated(view, savedInstanceState);
        animationDrawable = (AnimationDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.gradient_animation, null);
        if(animationDrawable != null) {
            animationDrawable.setEnterFadeDuration(10);
            animationDrawable.setExitFadeDuration(1750);
            requireView().setBackground(animationDrawable);
        }
        useFileButton.setOnClickListener(view1 -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT); //Create an intent to get a file from the filesystem
            intent.setType("audio/*"); //the file type should be wave
            startActivityIntent.launch(intent);

        });

        recordButton.setOnClickListener(view2 -> {
            if(isRecording) {

                stopRecording();
                String pathToFile = waveRecorder.filePath+"/final_record.wav";
                fileUploadController.getGenreFromFile(new File(pathToFile));
            } else {
                if(!recordingCooldown) {
                    startRecording();
                }
            }
        });
        animateBackgroundImage();

    }
    private void startRecording() {
        backgroundImage.setVisibility(View.INVISIBLE);
        setUseFileButtonEnabled(false);
        startTimerUpdate();
        waveRecorder.startRecording();
        startBackgroundAnimation();
        startPulsing();
        isRecording = true;
    }
    public void stopProcessUI() {
        removeInfoText();
        resetProgressBar();
        setBackgroundImageVisible(true);
        stopBackgroundAnimation();
    }
    public void stopRecording() {
        waveRecorder.stopRecording();
        stopPulsing();
        stopBackgroundAnimation();
        setUseFileButtonEnabled(true);
        backgroundImage.setVisibility(View.VISIBLE);
        infoTextView.setVisibility(View.INVISIBLE);
        infoTextView.setText(R.string.zero_seconds);
        isRecording = false;
        recordingCooldown = true;
    }
    public void setBackgroundImageVisible(boolean visible) {
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(visible) {
                    backgroundImage.setVisibility(View.VISIBLE);
                } else {
                    backgroundImage.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    private Runnable getTimerUpdateRunnable() {
        final int WARNING_INTERVAL_TIME = 15;
        return new Runnable() {
            @Override
            public void run() {
                final String timeInfo = "TIME ELAPSED: ";
                if(isRecording) {
                    if(infoTextView.getVisibility() == View.INVISIBLE) {
                        infoTextView.setVisibility(View.VISIBLE);
                        infoTextView.setTextColor(ResourcesCompat.getColor(getResources(),R.color.red,null));
                        String info = timeInfo + "0s";
                       infoTextView.setText(info);
                    }
                    String s = infoTextView.getText().toString();
                    s = s.substring(timeInfo.length(),s.length()-1);
                    int secondsElapsed = Integer.parseInt(s);
                    secondsElapsed++;
                    if(secondsElapsed > FileUploadController.MIN_RECORDING_DURATION) {
                        infoTextView.setTextColor(ResourcesCompat.getColor(getResources(),R.color.textColor,null));
                    }
                    if(secondsElapsed > FileUploadController.MAX_RECORDING_DURATION - WARNING_INTERVAL_TIME) {
                        infoTextView.setTextColor(ResourcesCompat.getColor(getResources(), R.color.yellow, null));
                        int secondsLeft = FileUploadController.MAX_RECORDING_DURATION -secondsElapsed;
                        if(secondsLeft % 5 == 0) {
                            String warningText = secondsLeft+ " seconds left";
                            Toast.makeText(requireContext(), warningText, Toast.LENGTH_SHORT).show();
                        }
                    }
                    if(secondsElapsed > FileUploadController.MAX_RECORDING_DURATION) {
                        Toast.makeText(requireContext(),"The recording is too long...", Toast.LENGTH_SHORT).show();
                        stopRecording();
                    }
                    String toShow = timeInfo+secondsElapsed+"s";
                    infoTextView.setText(toShow);
                    handler.postDelayed(this, 1000);
                } else {
                    recordingCooldown=false;
                }
            }
        };

    }
    private void startTimerUpdate() {
        handler.post(getTimerUpdateRunnable());
    }
    private void startPulsing() {
        animationRunnable.run();
    }

    private void stopPulsing() {
        handler.removeCallbacks(animationRunnable);
    }

    public String getCurrentInfoText() {
        return infoTextView.getText().toString();
    }
    private void pulsate(ImageView pulsatingImage, long duration, float scale){
        //duration describes how fast the circle expand, scale describes its maximum size.
        pulsatingImage.animate().alpha(0.0f).scaleX(scale).scaleY(scale).setDuration(duration).withEndAction(() ->
                pulsatingImage.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(0)
        );
    }
    private void animateBackgroundImage() {

        final Random random = new Random();
        final int bounds = drawableIds.length;
        int randomNumber;
        do {
            randomNumber = random.nextInt(bounds);
        } while(randomNumber == animationImageChooser);
        backgroundImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(),drawableIds[randomNumber],null));
        handler.postDelayed(backgroundAnimationRunnable, 2000);

    }
    public void initializeProgressBar(int length) {
        handler.post(() -> {
            progressBar.setVisibility(ProgressBar.VISIBLE);
            progressBar.setMax(length);
        });
    }
    public void incrementProgressBar() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                progressBar.incrementProgressBy(1);
            }
        });
    }
    public void resetProgressBar() {
        progressBar.setVisibility(ProgressBar.INVISIBLE);
        progressBar.setProgress(0);
    }

    public void setButtonsEnabled(boolean isEnabled) {
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setBBButtonEnabled(isEnabled);
                setUseFileButtonEnabled(isEnabled);
            }
        });
    }
    private void setBBButtonEnabled(boolean isEnabled) {
        if(!isEnabled) {
            recordButton.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bb_button_design_disabled));
        } else {
            recordButton.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bb_button_design));
        }
        recordButton.setEnabled(isEnabled);
    }
    private void setUseFileButtonEnabled(boolean isEnabled) {
        if(!isEnabled) {
            useFileButton.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.select_file_button_design_disabled));
        } else {
            useFileButton.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.select_file_button_design));
        }
        useFileButton.setEnabled(isEnabled);
    }
    private void startBackgroundAnimation() {
        animationDrawable.start();
    }
    public void stopBackgroundAnimation() {
        animationDrawable.stop();
    }

    public void setConnectionAvailable(boolean isOnline) {
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(isOnline) {
                    onlineStatusTextView.setTextColor(getResources().getColor(R.color.green, requireContext().getTheme()));
                    onlineStatusTextView.setText(R.string.online);
                } else {

                    onlineStatusTextView.setTextColor(getResources().getColor(R.color.red, requireContext().getTheme()));
                    onlineStatusTextView.setText(R.string.offline);
                    if(isRecording) {
                        stopRecording();
                    }
                }
            }
        });
    }
    public void setInfoText(String text) {
        requireActivity().runOnUiThread(() -> {
            if(infoTextView.getVisibility() == View.INVISIBLE) {
                infoTextView.setVisibility(View.VISIBLE);
            }
            infoTextView.setTextColor(ResourcesCompat.getColor(getResources(),R.color.textColor,null));
            infoTextView.setText(text);
        });
    }
    public void removeInfoText() {
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                infoTextView.setVisibility(View.INVISIBLE);
            }
        });
    }
    public boolean isRecording() {
        return isRecording;
    }

    public void stopProcess() {
        fileUploadController.stopProcess();
    }
}