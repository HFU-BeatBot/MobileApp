package com.theriotjoker.beatbot;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class AudioArithmeticController {
    private final File musicFile;
    private float[][] mfccValues;
    private final float[] magValues;
    private final JLibrosa librosa;
    private final long audioLength;
    public AudioArithmeticController(File f) throws FileFormatNotSupportedException, IOException, WavFileException {
        this.musicFile = f;
        librosa = new JLibrosa();
        magValues = librosa.loadAndRead(f.getPath(),-1,-1);
        audioLength = WavFile.openWavFile(musicFile).getDuration();
    }
    public long getAudioLength() {
        return audioLength;
    }
    public String getStringMusicFeaturesFromFile(int offset, int length) {
        populateMFCCValues(offset, length);
        float[] meanMFCCs = generateMeanMFCCValues();
        double[] standardDeviations = generateStandardDeviations();
        double[] apiCallArray = createArrayForApi(meanMFCCs,standardDeviations);
        System.out.println(Arrays.toString(apiCallArray));
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

        mfccValues = librosa.generateMFCCFeatures(Arrays.copyOfRange(magValues, offset * librosa.getSampleRate(), (offset + length) * librosa.getSampleRate()),librosa.getSampleRate(),20);
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
