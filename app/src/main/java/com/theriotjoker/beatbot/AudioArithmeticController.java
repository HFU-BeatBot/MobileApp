package com.theriotjoker.beatbot;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class AudioArithmeticController {
    private final File musicFile;
    private float[][] mfccValues;
    public AudioArithmeticController(File f) {
        this.musicFile = f;
    }
    public long getLengthOfAudio() throws IOException, WavFileException {
        return WavFile.openWavFile(musicFile).getDuration();
    }
    public String getStringMusicFeaturesFromFile(int offset, int length) {
        populateMFCCValues(offset, length);
        float[] meanMFCCs = generateMeanMFCCValues();
        double[] standardDeviations = generateStandardDeviations();
        double[] apiCallArray = createArrayForApi(meanMFCCs,standardDeviations);
        return Arrays.toString(apiCallArray);
    }
    private double[] generateStandardDeviations() {
        double[] standardDeviations = new double[mfccValues.length];
        for(int i = 0; i < mfccValues.length; i++) {
            standardDeviations[i] = getStandardDeviation(mfccValues[i]);
        }
        return standardDeviations;
    }
    private void populateMFCCValues(int offset, int length) {
        JLibrosa librosa = new JLibrosa();
        try {
            WavFile wavFile = WavFile.openWavFile(musicFile);
            System.out.println(wavFile.getDuration());
        } catch (IOException | WavFileException e) {
            throw new RuntimeException(e);
        }
        try {
            float[] audioValues = librosa.loadAndReadWithOffset(musicFile.getPath(), -1,length, offset);
            mfccValues = librosa.generateMFCCFeatures(audioValues,librosa.getSampleRate(),20);
        } catch (FileFormatNotSupportedException | WavFileException | IOException e) {
            throw new RuntimeException(e);
        }
    }
    private long getLengthOfAudioFile(File f) throws IOException, WavFileException {
        WavFile wavFile = WavFile.openWavFile(f);
        return wavFile.getDuration();

    }
    private float[] generateMeanMFCCValues() {
        return new JLibrosa().generateMeanMFCCFeatures(mfccValues,mfccValues.length,mfccValues[0].length);
    }
    private double getStandardDeviation(float[] array) {
        // get the sum of array
        double sum = 0.0;
        for (double i : array) {
            sum += i;
        }
        //TODO: mean is not necessary
        // get the mean of array
        int length = array.length;
        double mean = sum / length;
        // calculate the standard deviation
        double standardDeviation = 0.0;
        for (double num : array) {
            standardDeviation += Math.pow(num - mean, 2);
        }
        return Math.sqrt(standardDeviation / length);
    }
    private double[] createArrayForApi(float[] meanMFCC, double[] deviations) {
        double[] apiCallArray = new double[meanMFCC.length*2];
        int apiArrayPointer = 0;
        for(int i = 0; i < meanMFCC.length; i++) {
            apiCallArray[apiArrayPointer] = meanMFCC[i];
            apiArrayPointer++;
            apiCallArray[apiArrayPointer] = deviations[i];
            apiArrayPointer++;
        }
        return  apiCallArray;
    }
}