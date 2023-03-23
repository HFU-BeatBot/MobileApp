package com.theriotjoker.beatbot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.theriotjoker.beatbot.databinding.FragmentFirstBinding;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;

public class MainScreen extends Fragment {

    private FragmentFirstBinding binding;
    private ActivityResultLauncher<Intent> startActivityIntent;
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        startActivityIntent = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if(result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            System.out.println(data.toUri(Intent.URI_ALLOW_UNSAFE));
                            try {
                                FileInputStream inputStream = (FileInputStream) getActivity().getApplicationContext().getContentResolver().openInputStream(data.getData());
                            } catch (FileNotFoundException e) {
                                System.out.println(e.getMessage());
                            }
                        }
                    }
                });
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //NavHostFragment.findNavController(MainScreen.this).navigate(R.id.mainScreenToFileScreen)
        binding.useFileButton.setOnClickListener(view1 -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT); //Create an intent to get a file from the filesystem
            intent.setType("audio/mpeg"); //the file should be of the type .mp3 (actual name mpeg)
            startActivityIntent.launch(intent);

        });


    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}