package com.theriotjoker.beatbot;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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


public class MainScreen extends Fragment {

    private FragmentFirstBinding binding;
    private ActivityResultLauncher<Intent> startActivityIntent;
    private MediaRecorder mediaRecorder;
    private boolean isRecording;
    private FileUploadController fileUploadController;
    private final Handler handler = new Handler();
    private ProgressBar progressBar;
    private WaveRecorder waveRecorder;
    private AnimationDrawable animationDrawable;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fileUploadController = new FileUploadController(this);
        waveRecorder = new WaveRecorder(requireContext().getCacheDir().getPath());
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        requireActivity().getWindow().setBackgroundDrawable(container.getBackground());
        progressBar = binding.progressBar;
        startActivityIntent = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        buildDialogForFile(result.getData());
                    } else {
                        Toast.makeText(getContext(),"File could not be selected.", Toast.LENGTH_SHORT).show();
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
                        fileUploadController.getGenreFromUri(result.getData());
                        setButtonsEnabled(true);
                    }
                }).setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());
        AlertDialog messageDialog = alertDialogBuilder.create();
        messageDialog.show();
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
        binding.useFileButton.setOnClickListener(view1 -> {
            animateUseFileButton();
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT); //Create an intent to get a file from the filesystem
            intent.setType("audio/*"); //the file type should be wave
            startActivityIntent.launch(intent);

        });

        binding.bbButton.setOnClickListener(view2 -> {
            if(isRecording) {
                stopRecording();
                String pathToFile = waveRecorder.filePath+"/final_record.wav";
                fileUploadController.getGenreFromFile(new File(pathToFile));
                //TODO DEBUG MEASURE
                /*MediaPlayer mediaPlayer1 = new MediaPlayer();
                try {
                    mediaPlayer1.setDataSource(pathToFile);
                    mediaPlayer1.prepare();
                    mediaPlayer1.start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }*/
            } else {
                setUseFileButtonEnabled(false);
                startTimerUpdate();
                waveRecorder.startRecording();
                startBackgroundAnimation();
                startPulsing();
                isRecording = true;
            }
        });
    }
    public void stopRecording() {
        waveRecorder.stopRecording();
        stopPulsing();
        stopBackgroundAnimation();
        binding.infoTextView.setVisibility(View.INVISIBLE);
        binding.infoTextView.setText("0s");
        isRecording = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    private void startTimerUpdate() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                final String timeInfo = "TIME ELAPSED: ";
                if(isRecording) {
                    if(binding.infoTextView.getVisibility() == View.INVISIBLE) {
                        binding.infoTextView.setVisibility(View.VISIBLE);
                        binding.infoTextView.setTextColor(ResourcesCompat.getColor(getResources(),R.color.red,null));
                        String info = timeInfo + "0s";
                        binding.infoTextView.setText(info);
                    }
                    String s = binding.infoTextView.getText().toString();
                    s = s.substring(timeInfo.length(),s.length()-1);
                    int secondsElapsed = Integer.parseInt(s);
                    secondsElapsed++;
                    if(secondsElapsed > 6) {
                        binding.infoTextView.setTextColor(ResourcesCompat.getColor(getResources(),R.color.textColor,null));
                    }
                    String toShow = timeInfo+secondsElapsed+"s";
                    binding.infoTextView.setText(toShow);
                    handler.postDelayed(this, 1000);
                }
            }
        });
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
            pulsate(binding.pulseImage1,1000,3.0f);
            pulsate(binding.pulseImage2,700,3.0f);
            handler.postDelayed(this, 1500);
        }
    };

    private void pulsate(ImageView pulsatingImage, long duration, float scale){
        //duration describes how fast the circle expand, scale describes its maximum size.
        pulsatingImage.animate().alpha(0.0f).scaleX(scale).scaleY(scale).setDuration(duration).withEndAction(() ->
                pulsatingImage.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(0)
        );
    }
    private void animateUseFileButton() {
        //ImageView useFileButtonImage = binding.useFileButtonImage;
        //useFileButtonImage.animate().alpha(0.0f).scaleX(1.5f).scaleY(3.0f).setDuration(3000).withEndAction(() -> binding.useFileButtonImage.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(0));
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
            binding.bbButton.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bb_button_design_disabled));
        } else {
            binding.bbButton.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bb_button_design));
        }
        binding.bbButton.setEnabled(isEnabled);
    }
    private void setUseFileButtonEnabled(boolean isEnabled) {
        if(!isEnabled) {
            binding.useFileButton.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.select_file_button_design_disabled));
        } else {
            binding.useFileButton.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.select_file_button_design));
        }
        binding.useFileButton.setEnabled(isEnabled);
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
                TextView onlineStatusTextView = binding.onlineStauts;
                if(isOnline) {
                    setButtonsEnabled(true);
                    onlineStatusTextView.setTextColor(getResources().getColor(R.color.green, requireContext().getTheme()));
                    onlineStatusTextView.setText(R.string.online);
                } else {
                    setButtonsEnabled(false);
                    onlineStatusTextView.setTextColor(getResources().getColor(R.color.red, requireContext().getTheme()));
                    onlineStatusTextView.setText(R.string.offline);
                }
            }
        });
    }
    public void setInfoText(String text) {
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(binding.infoTextView.getVisibility() == View.INVISIBLE) {
                    binding.infoTextView.setVisibility(View.VISIBLE);
                }
                binding.infoTextView.setTextColor(ResourcesCompat.getColor(getResources(),R.color.textColor,null));
                binding.infoTextView.setText(text);
            }
        });
    }
    public void removeInfoText() {
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.infoTextView.setVisibility(View.INVISIBLE);
            }
        });
    }
    public boolean isRecording() {
        return isRecording;
    }
}