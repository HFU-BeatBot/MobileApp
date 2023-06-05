package com.theriotjoker.beatbot;


import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import java.util.concurrent.TimeUnit;

import javazoom.jl.converter.Converter;
import javazoom.jl.decoder.JavaLayerException;

public class FileUploadController {
    private final ArrayList<Genre> genres;
    private static final String BEATBOT_API_URL = "http://gamers-galaxy.ddns.net:8000/process";
    private static final long MAX_FILE_SIZE_WAV = 100*1024*1024;
    private static final long MAX_FILE_SIZE_MP3 = 15*1024*1024;
    private static final int AUDIO_SNIPPET_LENGTH = 5;
    private final MainScreen mainScreen;

    public FileUploadController(MainScreen mainScreen) {
        genres = new ArrayList<>();
        startConnectionCheckDaemon();
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
            writeErrorToScreen("The file could not be read.");
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
    public void getGenreFromUri(Uri uri) {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            mainScreen.setButtonsEnabled(false);
            File f = getFileObjectFromUri(uri);
            if(f == null) {
                cleanUp();
                return;
            }

            if(f.getName().endsWith(".mp3")) {
                if(f.length() > MAX_FILE_SIZE_MP3) {
                    writeErrorToScreen("The selected file is too large...");
                    cleanUp();
                    return;
                }
                try {
                    mainScreen.setInfoText("CONVERTING TO .WAV");
                    f = convert(f.getPath());
                } catch (JavaLayerException e) {
                    writeErrorToScreen("The selected file could not be converted...");
                    cleanUp();
                    return;
                }
            } else {
                if(f.getName().endsWith(".wav")) {
                    if(f.length() > MAX_FILE_SIZE_WAV) {
                        writeErrorToScreen("The selected file is too large...");
                        cleanUp();
                        return;
                    }
                } else {
                    writeErrorToScreen("The selected file format is unsupported...");
                    cleanUp();
                    return;
                }
            }
            getGenreFromFile(f);
        });
    }
    public void getGenreFromFile(@NonNull File inputFile) {
        genres.clear();
        Executor executor = Executors.newSingleThreadExecutor();
        Handler uiHandler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            if(!ApiHandler.testConnection(BEATBOT_API_URL)) {
                writeErrorToScreen("Connection failed...");
                return;
            }
            boolean terminatedSuccessfully;
            mainScreen.setInfoText("EXTRACTING MFCCs");
            AudioArithmeticController audioArithmeticController;
            try {
                audioArithmeticController = new AudioArithmeticController(inputFile);
            } catch (FileFormatNotSupportedException | IOException | WavFileException e) {
                writeErrorToScreen("File could not be read."+e.getMessage());
                cleanUp();
                return;
            }
            long audioLength;
            try {
                audioLength = audioArithmeticController.getLengthOfAudio();
                if(audioLength <= AUDIO_SNIPPET_LENGTH) {
                    writeErrorToScreen("The length of the recording is too short...");
                    cleanUp();
                    return;
                }
            } catch (IOException | WavFileException e) {
                writeErrorToScreen("The file could not be read."+e.getMessage());
                cleanUp();
                return;
            }

            mainScreen.setButtonsEnabled(false);
            long time = System.currentTimeMillis();
            mainScreen.initializeProgressBar((int)audioLength/AUDIO_SNIPPET_LENGTH);
            ArrayList<Runnable> runnables = new ArrayList<>();
            for(int i = 0; i+AUDIO_SNIPPET_LENGTH < audioLength; i = i+AUDIO_SNIPPET_LENGTH) {
                runnables.add(createMFCCTask(i, audioArithmeticController));
            }
            final int MAX_THREADS = 5;
            ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREADS);
            for(Runnable r : runnables) {
                executorService.execute(r);
            }
            executorService.shutdown();
            try {
                terminatedSuccessfully = executorService.awaitTermination(300, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Total time needed for everything: "+(System.currentTimeMillis()-time)/1000+" seconds.");
            boolean finalTerminatedSuccessfully = terminatedSuccessfully;
            mainScreen.setInfoText("Done!");
            uiHandler.post(() -> {
                if(finalTerminatedSuccessfully) {
                    mainScreen.resetProgressBar();
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("GENRE", calculateAverageGenre());
                    NavHostFragment.findNavController(mainScreen).navigate(R.id.mainScreenToFileScreen, bundle);
                    mainScreen.stopBackgroundAnimation();
                }
            });
        });
    }
    private void cleanUp() {
        mainScreen.setButtonsEnabled(true);
        mainScreen.stopBackgroundAnimation();
        mainScreen.removeInfoText();
    }
    private Genre calculateAverageGenre() {
        double[] array = new double[10];
        for(Genre g : genres) {
            if(g.getConfidences().getConfidenceValues() != null) { //in case the internet communication for some reason gets wrong packages, this becomes null and crashes the app
                double[] confidenceValues = g.getConfidences().getConfidenceValues();
                for(int i = 0; i < confidenceValues.length; i++) {
                    array[i] = array[i]+confidenceValues[i];
                }
            }
        }
        double sum = Arrays.stream(array).sum();
        for(int i = 0; i < array.length; i++) {
            array[i] = array[i] / sum;
        }

        return new Genre(new Confidences(array));
    }
    private Runnable createMFCCTask(int offset, AudioArithmeticController audioArithmeticController) {
        return () -> {
            TimerUtil.setStartTime(System.currentTimeMillis());
            String musicValuesString = audioArithmeticController.getStringMusicFeaturesFromFile(offset, AUDIO_SNIPPET_LENGTH);
            String apiCallString = "{\"music_array\":"+musicValuesString+"}";
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
        };
    }
    public File convert(String filePath) throws JavaLayerException {
        Converter c = new Converter();
        String pathToSave = filePath.replace(".mp3", "");
        pathToSave = pathToSave + "-Converted.wav";
        c.convert(filePath, pathToSave);
        return new File(pathToSave);
    }
    private Genre generateGenreFromJson(String json) {
        Gson gson = new Gson();
        Genre genre = gson.fromJson(json, Genre.class);
        genre.initializeThemeValues();
        return genre;

    }
    private void writeErrorToScreen(String error) {
        Handler uiHandler = new Handler(Looper.getMainLooper());
        uiHandler.post(() -> {
            Toast.makeText(mainScreen.getContext(), error, Toast.LENGTH_SHORT).show();
        });
    }
    private void startConnectionCheckDaemon() {
        //If there is no connection, write it to the screen, disable buttons and check every 5 seconds for the connection
        Thread connectionCheckThread = new Thread(() -> {
            while (true) {
                boolean success = ApiHandler.testConnection(BEATBOT_API_URL);
                while (!success) { //If there is no connection, write it to the screen, disable buttons and check every 5 seconds for the connection
                    mainScreen.setButtonsEnabled(false);
                    mainScreen.setConnectionAvailable(false);
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    success = ApiHandler.testConnection(BEATBOT_API_URL);
                }
                mainScreen.setConnectionAvailable(true);
                while (success) {
                    success = ApiHandler.testConnection(BEATBOT_API_URL);
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        connectionCheckThread.setDaemon(true);
        connectionCheckThread.start();
    }
}
