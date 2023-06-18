package com.theriotjoker.beatbot;


import android.annotation.SuppressLint;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
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
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javazoom.jl.converter.Converter;
import javazoom.jl.decoder.JavaLayerException;

public class FileUploadController {
    private final ArrayList<Genre> genres;
    private static final long MAX_FILE_SIZE_WAV = 100*1024*1024;
    private static final long MAX_FILE_SIZE_MP3 = 15*1024*1024;
    private static final int AUDIO_SNIPPET_DURATION = 5;
    public static final int MAX_RECORDING_DURATION = 200;
    public static final int MIN_RECORDING_DURATION = AUDIO_SNIPPET_DURATION +1;
    private boolean connectionAvailable = false;
    private boolean processStarted = false;
    private boolean shutdownForcefully = false;
    private final MainScreen mainScreen;
    private final ApiHandler apiHandler;
    private ArrayList<String> apiCallStrings;
    private ScheduledExecutorService scheduledExecutorService;
    private ExecutorService executorService;

    public FileUploadController(MainScreen mainScreen) {
        apiCallStrings = new ArrayList<>();
        genres = new ArrayList<>();
        apiHandler = new ApiHandler();
        startConnectionChecker();
        this.mainScreen = mainScreen;
    }

    public boolean isProcessStarted() {
        return processStarted;
    }

    public String getFileNameFromUri(Uri uri) {
        String returnValue = null;

        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = mainScreen.requireActivity().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    @SuppressLint("Range") String displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    if (displayName != null) {
                        returnValue = displayName;
                    }
                }
            } catch (Exception e) {
                writeErrorToScreen("The file could not be read.");
            }
        }

        if (returnValue == null) {
            returnValue = uri.getLastPathSegment();
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
            processStarted = true;
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
            if(shutdownForcefully) {
                cleanUp();
                return;
            }
            getGenreFromFile(f);
        });
    }
    public void getGenreFromFile(@NonNull File inputFile) {
        genres.clear();
        Executor executor = Executors.newSingleThreadExecutor();
        Handler uiHandler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {

            processStarted = true;
            boolean terminatedSuccessfully;

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
                if(audioLength <= AUDIO_SNIPPET_DURATION) {
                    writeErrorToScreen("The length of the recording is too short...");
                    cleanUp();
                    return;
                }
            } catch (IOException | WavFileException e) {
                writeErrorToScreen("The file could not be read."+e.getMessage());
                cleanUp();
                return;
            }
            if(shutdownForcefully) {
                return;
            }
            mainScreen.setInfoText("EXTRACTING MFCCs");
            mainScreen.setButtonsEnabled(false);
            long time = System.currentTimeMillis();
            mainScreen.initializeProgressBar((int)audioLength/ AUDIO_SNIPPET_DURATION);
            ArrayList<Runnable> conversionTasks = new ArrayList<>();
            for(int i = 0; i+ AUDIO_SNIPPET_DURATION < audioLength; i = i+ AUDIO_SNIPPET_DURATION) {
                conversionTasks.add(createMFCCTask(i, audioArithmeticController));
            }
            final int MAX_THREADS = 5;
            executorService = Executors.newFixedThreadPool(MAX_THREADS);
            for(Runnable r : conversionTasks) {
                executorService.execute(r);
            }
            executorService.shutdown();
            try {
                terminatedSuccessfully = executorService.awaitTermination(300, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            final int MAX_WAIT_TIME_SECS = 20;
            while(!apiCallStrings.isEmpty()) { //if the call strings are not empty, that means that our connection got cut off somewhere along the way, so we shall retry
                //We will wait for the connection for 20 seconds, if no connection, we quit

                int counter = 0;
                while(!connectionAvailable && counter < MAX_WAIT_TIME_SECS) {
                    mainScreen.setInfoText("RETRYING CONNECTION..."+counter+"/"+MAX_WAIT_TIME_SECS);
                    try {
                        Thread.sleep(1000);
                        counter++;
                    } catch(InterruptedException ignored) {

                    }
                }
                if(counter >= MAX_WAIT_TIME_SECS) {
                    apiCallStrings.clear();
                    terminatedSuccessfully = false;
                    break;
                }
                mainScreen.setInfoText("EXTRACTING MFCCs");
                Iterator<String> i = apiCallStrings.iterator();
                while(i.hasNext()) {
                    String s = i.next();
                    boolean success = callApiForGenre(s);
                    if(success) {
                        i.remove();
                    }
                }
            }
            if(shutdownForcefully) {
                cleanUp();
                return;
            }
            System.out.println("Total time needed for everything: "+(System.currentTimeMillis()-time)/1000+" seconds.");
            boolean finalTerminatedSuccessfully = terminatedSuccessfully;
            mainScreen.setInfoText("Done!");
            uiHandler.post(() -> {

                cleanUp();
                if(finalTerminatedSuccessfully) {
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("GENRE", calculateAverageGenre());
                    NavHostFragment.findNavController(mainScreen).navigate(R.id.mainScreenToFileScreen, bundle);
                } else {
                    if(!connectionAvailable) {
                        Toast.makeText(mainScreen.requireContext(), "Connection failed...", Toast.LENGTH_SHORT).show();
                    }
                }

            });
        });
    }
    public void stopConnectionChecker() {
        scheduledExecutorService.shutdownNow();
    }
    public void stopProcess() {
        if(executorService != null) {
            executorService.shutdownNow();
        }
        mainScreen.setInfoText("CANCELLING");
        shutdownForcefully = true;
    }
    private void cleanUp() {
        shutdownForcefully = false;
        processStarted = false;
        mainScreen.removeInfoText();
        mainScreen.resetProgressBar();
        apiCallStrings.clear();
        if(connectionAvailable) {
            mainScreen.setButtonsEnabled(true);
        }
        mainScreen.stopBackgroundAnimation();
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
            if(Thread.currentThread().isInterrupted()) {
                return;
            }
            TimerUtil.setStartTime(System.currentTimeMillis());
            String musicValuesString = audioArithmeticController.getStringMusicFeaturesFromFile(offset, AUDIO_SNIPPET_DURATION);
            String apiCallString = "{\"model_to_use\":2,\"music_array\":"+musicValuesString+"}";
            TimerUtil.setEndTime(System.currentTimeMillis());
            boolean success = callApiForGenre(apiCallString);
            if(!success && !shutdownForcefully) { //if the thread is interrupted at just the right time, callApiForGenre will throw an IOException named "thread interrupted" so we need to check if
                //the thread was interrupted to stop the process
                apiCallStrings.add(apiCallString);
            }
        };
    }
    //Returns false if api call was unsuccessful
    private boolean callApiForGenre(String apiCallString) {
        String answer;
        try {
            answer = apiHandler.sendPostToApi(apiCallString);
        } catch (IOException e) { //e is also a "thread interrupted" exception
            return false;
        }
        Genre genre = generateGenreFromJson(answer);
        genres.add(genre);
        mainScreen.incrementProgressBar();
        return true;
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
        uiHandler.post(() -> Toast.makeText(mainScreen.getContext(), error, Toast.LENGTH_SHORT).show());
    }


    private void startConnectionChecker() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            if(!Thread.currentThread().isInterrupted()) {
                boolean connectionExists = ApiHandler.testConnection();
                if(!connectionExists && connectionAvailable) {
                    mainScreen.setConnectionAvailable(false);
                    if(!processStarted) {
                        mainScreen.setButtonsEnabled(false);
                    }
                    connectionAvailable = false;
                } else {
                    if(connectionExists && !connectionAvailable) {
                        mainScreen.setConnectionAvailable(true);
                        if(!processStarted) {
                            mainScreen.setButtonsEnabled(true);
                        }
                        connectionAvailable = true;
                    }
                }
            }

        }, 0, 4, TimeUnit.SECONDS);
    }
}
