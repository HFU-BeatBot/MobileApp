package com.theriotjoker.beatbot;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.navigation.fragment.NavHostFragment;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FileUploadController {

    private static final String BEATBOT_API_URL = "http://141.28.73.92:8000/process";
    private final MainScreen mainScreen;

    public FileUploadController(MainScreen mainScreen) {
        this.mainScreen = mainScreen;
    }

    public String getFileNameFromUri(Uri uri) {
        String returnValue = null;
        String[] displayNameColumn = {MediaStore.Images.Media.DISPLAY_NAME};
        try (Cursor cursor  = mainScreen.requireActivity().getContentResolver().query(uri, displayNameColumn, null, null)) {

            if(cursor != null && cursor.moveToFirst()) {
                int columnIndexOfName = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                returnValue = cursor.getString(columnIndexOfName);
            }
        } catch(Exception e) {
            writeErrorToScreen("The file could not be read...");
        }
        return returnValue;
    }
    private File getFileObjectFromUri(Uri uri) {
        File selectedFile = new File(mainScreen.requireContext().getCacheDir(), getFileNameFromUri(uri));
        try (InputStream inputStream = mainScreen.requireActivity().getContentResolver().openInputStream(uri);
             OutputStream outputStream = Files.newOutputStream(selectedFile.toPath())) {
            byte[] buffer = new byte[1024];
            int length;
            while((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer,0,length);
            }
            outputStream.flush();
        } catch (IOException e) {
            writeErrorToScreen("The file could not be read...");
            return null;
        }
        selectedFile.deleteOnExit();
        return selectedFile;
    }
    public void getMusicGenreFromUri(Uri uri) {
        Executor executor = Executors.newSingleThreadExecutor();
        Handler uiHandler = new Handler(Looper.getMainLooper());
        executor.execute(new Runnable() {
            @Override
            public void run() {
                File f = getFileObjectFromUri(uri);
                AudioArithmeticController audioArithmeticController = new AudioArithmeticController();
                String musicValuesString = audioArithmeticController.getStringMusicFeaturesFromFile(f);
                String apiCallString = "{\"music_array\":"+musicValuesString+"}";
                System.out.println(apiCallString);
                String apiAnswerJson = null;
                ApiHandler apiHandler = new ApiHandler();
                try {
                    apiAnswerJson = apiHandler.sendPostToApi(BEATBOT_API_URL, apiCallString);
                } catch (IOException e) {
                    writeErrorToScreen("Api connection failed... Check your internet connection!");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                    return;
                }
                Genre detectedGenres = generateGenreFromJson(apiAnswerJson);
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("GENRE", detectedGenres);
                        ResultScreen resultScreen = new ResultScreen();
                        resultScreen.setArguments(bundle);
                        NavHostFragment.findNavController(mainScreen).navigate(R.id.mainScreenToFileScreen, bundle);
                    }
                });
            }
        });
    }
    private Genre generateGenreFromJson(String json) {
        Gson gson = new Gson();
        Genre genre = gson.fromJson(json, Genre.class);
        genre.initializeThemeValues();
        return genre;

    }
    private void writeErrorToScreen(String error) {
        System.out.println("ERROR ERROR + " +error );
        mainScreen.requireActivity().runOnUiThread(() -> {
            Toast t = Toast.makeText(mainScreen.getContext(), error, Toast.LENGTH_SHORT);
            t.show();
        });
    }
}
/*
* String[] fileColumn = {MediaStore.Images.Media.DATA};
        String filePath = null;
        try (Cursor cursor = mainScreen.requireActivity().getContentResolver().query(uri, fileColumn, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndexOfFile = cursor.getColumnIndexOrThrow(fileColumn[0]);
                filePath = cursor.getString(columnIndexOfFile);
                System.out.println("File Path = "+filePath);
                return new File(filePath);
            }
        } catch (Exception e) {
            Toast t = Toast.makeText(mainScreen.getContext(), "Could not load file...", Toast.LENGTH_SHORT);
            t.show();
        }
        System.out.println("File Path = "+filePath);
        return null; */
