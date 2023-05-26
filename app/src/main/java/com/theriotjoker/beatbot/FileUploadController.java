package com.theriotjoker.beatbot;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.navigation.fragment.NavHostFragment;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javazoom.jl.converter.Converter;
import javazoom.jl.decoder.JavaLayerException;

public class FileUploadController {
    private final ArrayList<Genre> genres;
    private static final String BEATBOT_API_URL = "http://141.28.73.92:8000/process";
    private final MainScreen mainScreen;

    private Semaphore semaphore;
    public FileUploadController(MainScreen mainScreen) {
        genres = new ArrayList<>();
        this.mainScreen = mainScreen;
        semaphore = new Semaphore(5);
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
            writeErrorToScreen("The file could not be read.");
        }
        return returnValue;
    }
    public File getFileObjectFromUri(Uri uri, String helloWorld) {
        File selectedFile = new File(mainScreen.requireContext().getCacheDir(), helloWorld);
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
    public File getFileObjectFromUri(Uri uri) {
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
                String filename = getFileNameFromUri(uri);
                if(f == null) return;
                if(getFileNameFromUri(uri).endsWith(".mp3")) {
                    f = convert(f.getPath());
                }
                AudioArithmeticController audioArithmeticController;
                try {
                    audioArithmeticController = new AudioArithmeticController(f);
                } catch (FileFormatNotSupportedException | IOException | WavFileException e) {
                      throw new RuntimeException(e);
                }
                String musicValuesString = "";
                long audioLength;
                try {
                    audioLength = audioArithmeticController.getLengthOfAudio();
                } catch (IOException | WavFileException e) {
                    writeErrorToScreen("The file could not be read.");
                    return;
                }
                long time = System.currentTimeMillis();
                final int AUDIO_SNIPPET_LENGTH = 5; //the audio gets divided into snippets of 3 seconds
                mainScreen.initializeProgressBar((int)audioLength/AUDIO_SNIPPET_LENGTH);
                ArrayList<Runnable> runnables = new ArrayList<>();
                for(int i = 0; i+AUDIO_SNIPPET_LENGTH < audioLength; i = i+AUDIO_SNIPPET_LENGTH) {
                    runnables.add(createMFCCTask(i,AUDIO_SNIPPET_LENGTH, audioArithmeticController));
                }
                final int MAX_THREADS = 1;
                ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREADS);
                for(Runnable r : runnables) {
                    executorService.execute(r);
                }
                executorService.shutdown();
                try {
                    boolean terminatedSuccessfully = executorService.awaitTermination(60, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("Total time needed for everything: "+(System.currentTimeMillis()-time)/1000+" seconds.");
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("GENRE", calculateAverageGenre());
                        ResultScreen resultScreen = new ResultScreen();
                        resultScreen.setArguments(bundle);
                        NavHostFragment.findNavController(mainScreen).navigate(R.id.mainScreenToFileScreen, bundle);
                    }
                });
            }
        });
    }
    private Genre calculateAverageGenre() {
        System.out.println(genres.size() + " GENRES SIZE GENRESS IZE");
        double[] array = new double[10];
        for(Genre g : genres) {
            double[] confidenceValues = g.getConfidences().getConfidenceValues();
            for(int i = 0; i < confidenceValues.length; i++) {
                array[i] = array[i]+confidenceValues[i];
            }
        }
        double sum = Arrays.stream(array).sum();
        for(int i = 0; i < array.length; i++) {
            array[i] = array[i] / sum;
        }
        Genre retVal = new Genre(new Confidences(array));

        return retVal;
    }
    private Runnable createMFCCTask(int offset, int length, AudioArithmeticController audioArithmeticController) {
        return new Runnable() {
            @Override
            public void run() {
                TimerUtil.setStartTime(System.currentTimeMillis());
                String musicValuesString = audioArithmeticController.getStringMusicFeaturesFromFile(offset,length);
                String apiCallString = "{\"music_array\":"+musicValuesString+"}";
                //System.out.println("Variance + MFCC"+apiCallString);
                TimerUtil.setEndTime(System.currentTimeMillis());

                ApiHandler apiHandler = new ApiHandler();
                String answer;
                try {
                    answer = apiHandler.sendPostToApi(BEATBOT_API_URL, apiCallString);
                } catch (IOException e) {
                    writeErrorToScreen("Connection to server failed.");
                    return;
                }
                Genre genre = generateGenreFromJson(answer);
                genres.add(genre);
                mainScreen.incrementProgressBar();
            }
        };
    }
    public File convert(String filePath) {
        // TODO Auto-generated method stub
        Converter c = new Converter();
        try {
            String pathToSave = filePath.replace(".mp3", "");
            pathToSave = pathToSave + "-Converted.wav";
            c.convert(filePath, pathToSave);
            return new File(pathToSave);
        } catch (JavaLayerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    private Genre generateGenreFromJson(String json) {
        Gson gson = new Gson();
        Genre genre = gson.fromJson(json, Genre.class);
        genre.initializeThemeValues();
        return genre;

    }
    private void writeErrorToScreen(String error) {
        mainScreen.requireActivity().runOnUiThread(() -> {
            Toast t = Toast.makeText(mainScreen.getContext(), error, Toast.LENGTH_SHORT);
            t.show();
        });
    }
}
