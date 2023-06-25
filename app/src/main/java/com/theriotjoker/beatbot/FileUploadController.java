package com.theriotjoker.beatbot;


import android.annotation.SuppressLint;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;

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
    //genres saves all results of all api calls
    private final ArrayList<Genre> genres;
    private static final long MAX_FILE_SIZE_WAV = 100*1024*1024;
    private static final long MAX_FILE_SIZE_MP3 = 15*1024*1024;
    private static final int AUDIO_SNIPPET_DURATION = 5;
    public static final int MAX_RECORDING_DURATION = 200;
    public static final int MIN_RECORDING_DURATION = AUDIO_SNIPPET_DURATION +1;
    //this variable saves if the app is currently able to establish a connection
    private boolean connectionAvailable = false;
    //this variable is used to determine if a process (song conversion, mfcc extraction, etc.) is currently running
    private boolean processStarted = false;
    //this variable is used to shut down the process, it is set to true if the back button is pressed and a process is running
    private boolean shutdownForcefully = false;
    private final MainScreen mainScreen;
    private final ApiHandler apiHandler;
    //this list is used to save all api calls that failed due to the internet connection getting cut off
    private final ArrayList<String> apiCallStrings;
    //this executor service is used to start and stop the connection checking thread
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
    //This function takes an Uri (a path variable inside the Android System) and extracts the file name from it
    //This is used for the user to know which song they selected
    public String getFileNameFromUri(Uri uri) {
        String returnValue = null;
        //We need to be looking for content
        if (uri.getScheme().equals("content")) {
            //Then we create a cursor (which is a pointer but for databases)
            try (Cursor cursor = mainScreen.requireActivity().getContentResolver().query(uri, null, null, null, null)) {
                //Then we check if we got permission to use the cursor and we move it to the first entry
                if (cursor != null && cursor.moveToFirst()) {
                    //We get the first entry (Which is the name of the file, since that is the only column we are looking for)
                    @SuppressLint("Range") String displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    //If the name is not null, and we got access, then we can display it
                    if (displayName != null) {
                        returnValue = displayName;
                    }
                }
            } catch (Exception e) {
                mainScreen.writeErrorToScreen("The file could not be read.");
            }
        }
        //if the name is null, we can at least display a part of the uri (which sometimes may be the name)
        //this is a good fallback that guarantees that even if the filesystem fails in some way, we have a fallback that is not a NullPointerException
        if (returnValue == null) {
            returnValue = uri.getLastPathSegment();
        }

        return returnValue;
    }
    //This function gets a java.util.File object from an uri
    //This is achieved by taking the file from the filesystem, and copying it to the cache of the app
    //then, and only then, are we permitted to work on the file
    private File getFileObjectFromUri(Uri uri) {
        //A new file is created in the cache with the same name as the original file
        File selectedFile = new File(mainScreen.requireContext().getCacheDir(), getFileNameFromUri(uri));
        //we open an input stream with the selected file, and an output stream to the newly created cache file
        try (InputStream inputStream = mainScreen.requireActivity().getContentResolver().openInputStream(uri);
             OutputStream outputStream = Files.newOutputStream(selectedFile.toPath())) {
            byte[] buffer = new byte[1024];
            int length;
            //we copy the file in increments of 1024 bytes
            while((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer,0,length);
            }
            outputStream.flush();
        } catch (IOException e) {
            mainScreen.writeErrorToScreen("The file could not be read...");
            return null;
        }
        //the copied file needs to self delete when the app is closed
        selectedFile.deleteOnExit();
        return selectedFile;
    }


    //this function is part 1 of 2 functions:
    //in this one an uri gets taken and transformed into a java.util.File
    //if the file is of .mp3 format, then it gets transformed to .wav (otherwise it cannot be processed)
    //all of this needs to happen on a separate thread, as to not block the UI thread
    public void getGenreFromUri(Uri uri) {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            processStarted = true;
            mainScreen.setButtonsEnabled(false);
            mainScreen.setBackgroundImageVisible(false);
            File f = getFileObjectFromUri(uri);
            //If the file is corrupted, we stop
            if(f == null) {
                mainScreen.writeErrorToScreen("The chosen file is corrupted...");
                cleanUp();
                return;
            }
            //if the file is of an .mp3 format, it needs to be converted
            if(f.getName().endsWith(".mp3")) {
                if(f.length() > MAX_FILE_SIZE_MP3) {
                    mainScreen.writeErrorToScreen("The selected file is too large...");
                    cleanUp();
                    return;
                }
                try {
                    mainScreen.setConversionText();
                    f = convert(f.getPath());
                } catch (JavaLayerException e) {
                    mainScreen.writeErrorToScreen("The selected file could not be converted...");
                    cleanUp();
                    return;
                }
            } else {
                //if the file is .wav and it is larger than the max size, it gets thrown away
                if(f.getName().endsWith(".wav")) {
                    if(f.length() > MAX_FILE_SIZE_WAV) {
                        mainScreen.writeErrorToScreen("The selected file is too large...");
                        cleanUp();
                        return;
                    }
                } else {
                    //if the file extension isn't .wav or .mp3, then it is an unsupported format
                    mainScreen.writeErrorToScreen("The selected file format is unsupported...");
                    cleanUp();
                    return;
                }
            }
            //if the user pressed the back button, then we need to stop the process
            if(shutdownForcefully) {
                cleanUp();
                return;
            }
            getGenreFromFile(f);
        });
    }
    //Here is where the most important part of the application happens:
    //The function first creates an audioArithmeticController, which is an object that extracts the important
    //features from the audio file
    //Then we check if the file is readable and if it is too short, we cannot process it
    //afterwards we extract the audio features
    //check if some api calls failed and repeat them
    //get the average and let the UI go to the next screen with the genre data
    public void getGenreFromFile(@NonNull File inputFile) {
        processStarted = true;
        genres.clear();
        mainScreen.setButtonsEnabled(false);
        final int TERMINATION_TIMEOUT_SECONDS = 300;
        final int MAX_CONCURRENT_THREADS = 5; //this seems to be a good balance between having a fast application and not consuming too much ram
        Executor executor = Executors.newSingleThreadExecutor();
        Handler uiHandler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            boolean terminatedSuccessfully;
            if(shutdownForcefully) {
                cleanUp();
                return;
            }
            AudioArithmeticController audioArithmeticController = initializeAudioController(inputFile);
            if(audioArithmeticController == null) {
                cleanUp();
                return;
            }
            final long audioLength = audioArithmeticController.getAudioLength();
            if(audioLength <= AUDIO_SNIPPET_DURATION) {
                mainScreen.writeErrorToScreen("The audio duration is too short!");
                cleanUp();
                return;
            }
            mainScreen.startTextChanger();
            mainScreen.initializeProgressBar((int)audioLength/ AUDIO_SNIPPET_DURATION); //creating the progress bar
            ArrayList<Runnable> conversionTasks = new ArrayList<>(); //the list of tasks which will need to be executed, 1 task for every 5 seconds of a song
            for(int i = 0; i+ AUDIO_SNIPPET_DURATION < audioLength; i = i+ AUDIO_SNIPPET_DURATION) {
                conversionTasks.add(createMFCCTask(i, audioArithmeticController)); //we create a task for every 5 seconds of a song, with an offset, e.g. 0s -> 5s, 5s -> 10s, etc.
            }
            executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_THREADS);
            for(Runnable r : conversionTasks) {
                if(!shutdownForcefully) {
                    executorService.execute(r);
                }
            }
            executorService.shutdown(); //we will not accept any further tasks
            try {
               terminatedSuccessfully = executorService.awaitTermination(TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            } catch (InterruptedException ignored) {
                cleanUp();
                return;
            }
            if(terminatedSuccessfully) {
                terminatedSuccessfully = checkForFailedPackets();
            }
            if(shutdownForcefully || !terminatedSuccessfully || !connectionAvailable) {
                if(!terminatedSuccessfully) {
                    mainScreen.writeErrorToScreen("Conversion Failed: Timed out");
                }
                if(!connectionAvailable) {
                    mainScreen.writeErrorToScreen("Connection failed...");
                }
                cleanUp();
                return;
            }
            //handler.post is used to update the UI when the process is done
            while(MainActivity.isAppPaused()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            uiHandler.post(() -> {
                Bundle bundle = new Bundle();
                bundle.putSerializable("GENRE", calculateAverageGenre());
                NavHostFragment.findNavController(mainScreen).navigate(R.id.mainScreenToFileScreen, bundle);

            });
        });
    }
    private AudioArithmeticController initializeAudioController(File inputFile) {
        AudioArithmeticController audioArithmeticController;
        try {
            audioArithmeticController = new AudioArithmeticController(inputFile);
        } catch (FileFormatNotSupportedException | IOException | WavFileException e) {
            mainScreen.writeErrorToScreen("File could not be read."+e.getMessage());
            return null;
        }
        return audioArithmeticController;
    }
    private boolean checkForFailedPackets() {
        boolean returnValue = true;
        final int MAX_WAIT_TIME_SECS = 20;
        while(!apiCallStrings.isEmpty()) { //if the call strings are not empty, that means that our connection got cut off somewhere along the way, so we shall retry
            //We will wait for the connection for 20 seconds, if no connection, we quit

            int counter = 0;
            while(!connectionAvailable && counter < MAX_WAIT_TIME_SECS) { //if the connection is cut, and we haven't waited for 20 seconds, we need to wait
                mainScreen.setInfoText("RETRYING CONNECTION..."+counter+"/"+MAX_WAIT_TIME_SECS);
                try {
                    Thread.sleep(1000);
                    counter++;
                } catch(InterruptedException ignored) {

                }
            }
            if(counter >= MAX_WAIT_TIME_SECS && !connectionAvailable) { //if we waited for long enough and the connection is still off, we go back without a result
                apiCallStrings.clear();
                returnValue = false;
                break;
            }
            //otherwise if the connection is re-established, we can work with the rest of the api call strings
            Iterator<String> i = apiCallStrings.iterator();
            while(i.hasNext()) {
                String s = i.next();
                boolean success = callApiForGenre(s);
                if(success) {
                    i.remove();
                }
            }
        }
        return returnValue;
    }



    public void stopConnectionChecker() {
        scheduledExecutorService.shutdownNow();
    }
    //this function is called if a process is running and the user presses the back button
    //this makes sure that everything stops with the execution
    public void stopProcess() {
        if(!shutdownForcefully) {
            if(executorService != null) {
                executorService.shutdownNow();
            }
            mainScreen.setCancellingMessage();
            shutdownForcefully = true;
        }
    }
    public boolean isShutdownForcefully() {
        return shutdownForcefully;
    }
    //cleanUp is called every time the process finishes, regularly or irregularly
    //it cleans up the screen, resets the loading button, enables the buttons, etc.
    private void cleanUp() {
        shutdownForcefully = false;
        processStarted = false;
        mainScreen.stopProcessUI();
        apiCallStrings.clear();
        if(connectionAvailable) {
            mainScreen.setButtonsEnabled(true);
        }
    }
    //calculateAverageGenre() takes the answers of the api calls and calculates an average for each genre
    //by iterating over every api answer, getting the genre confidence values array and adding them on top of the result array
    //in the end each value in the result array is divided by the sum of the probabilities (this is how you get the average of a percentage)
    private Genre calculateAverageGenre() {
        double[] avgGenreProbabilities = new double[Genre.NUMBER_OF_POSSIBLE_GENRES];
        for(Genre g : genres) {
            if(g.getConfidences() != null) { //in case the internet communication for some reason gets wrong packages, this becomes null and crashes the app
                double[] confidenceValues = g.getConfidences().getConfidenceValues();
                for(int i = 0; i < confidenceValues.length; i++) {
                    avgGenreProbabilities[i] = avgGenreProbabilities[i]+confidenceValues[i];
                }
            }
        }
        double sum = Arrays.stream(avgGenreProbabilities).sum();
        for(int i = 0; i < avgGenreProbabilities.length; i++) {
            avgGenreProbabilities[i] = avgGenreProbabilities[i] / sum;
        }
        return new Genre(new Confidences(avgGenreProbabilities));
    }
    //this function returns a task for audio conversion and api calling
    //In essence, we get the MFCC features, turn that into a JSON String, ask the AI at the API end for a result, and we save the result in a variable
    private Runnable createMFCCTask(int offset, AudioArithmeticController audioArithmeticController) {
        return () -> {
            if(Thread.currentThread().isInterrupted()) {
                return;
            }
            String musicValuesString = audioArithmeticController.getStringMusicFeaturesFromFile(offset, AUDIO_SNIPPET_DURATION);
            String apiCallString = "{\"model_to_use\":3,\"music_array\":"+musicValuesString+"}";
            boolean success = callApiForGenre(apiCallString);
            if(!success && !shutdownForcefully) { //if the thread is interrupted at just the right time, callApiForGenre will throw an IOException named "thread interrupted" so we need to check if
                //the thread was interrupted to stop the process and save the string
                apiCallStrings.add(apiCallString);
            }
        };
    }
    //Returns false if api call was unsuccessful, true if the api call was successful
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
    //this function converts an .mp3 file into a .wav file
    public File convert(String filePath) throws JavaLayerException {
        Converter c = new Converter();
        String pathToSave = filePath.replace(".mp3", "");
        pathToSave = pathToSave + "-Converted.wav";
        c.convert(filePath, pathToSave);
        return new File(pathToSave);
    }
    //This function uses the GSON (google json) library to turn a json answer from the server
    //into a genre object with the correct values
    private Genre generateGenreFromJson(String json) {
        Gson gson = new Gson();
        Genre genre = gson.fromJson(json, Genre.class);
        System.out.println(genre.getConfidences());
        return genre;

    }

    //in this function we check if the connection works, the task is executed every X seconds to see if there is a connection
    //if there is no connection, the buttons get disabled and the user gets notified that there is no connection
    private void startConnectionChecker() {
        int CONNECTION_CHECK_INTERVAL = 4;
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            if(!Thread.currentThread().isInterrupted()) {
                boolean connectionExists = ApiHandler.testConnection();
                if(!connectionExists && connectionAvailable) {
                    mainScreen.setConnectionAvailable(false);
                    if(!processStarted) {
                        mainScreen.setButtonsEnabled(false);
                        mainScreen.setBackgroundImageVisible(false);
                    }
                    connectionAvailable = false;
                } else {
                    if(connectionExists && !connectionAvailable) {
                        mainScreen.setConnectionAvailable(true);
                        if(!processStarted) {
                            mainScreen.setBackgroundImageVisible(true);
                            mainScreen.setButtonsEnabled(true);
                        }
                        connectionAvailable = true;
                    }
                }
            } else {
                scheduledExecutorService.shutdownNow();
            }

        }, 0, CONNECTION_CHECK_INTERVAL, TimeUnit.SECONDS);
    }
}
