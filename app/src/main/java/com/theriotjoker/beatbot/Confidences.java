package com.theriotjoker.beatbot;

import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.HashMap;

/*
* This class is a kind of record class, the JSON answers get read, and objects of confidence values get created
* this class saves the confidence values of each genre and has the ability to return the genres in a hashmap
* where the key is the double value
* The key is a Double object because when we sort the keys, we can get the corresponding values, so that we can get the top 5 genres
*
*  */


public class Confidences implements Serializable {

    private final double[] confidenceArray;
    @SerializedName("Blues")
    @Expose
    private final double blues;
    @SerializedName("Classical")
    @Expose
    private final double classical;
    @SerializedName("Country")
    @Expose
    private final double country;
    @SerializedName("Disco")
    @Expose
    private final double disco;
    @SerializedName("HipHop")
    @Expose
    private final double hipHop;
    @SerializedName("Jazz")
    @Expose
    private final double jazz;
    @SerializedName("Metal")
    @Expose
    private final double metal;
    @SerializedName("Pop")
    @Expose
    private final double pop;
    @SerializedName("Reggae")
    @Expose
    private final double reggae;
    @SerializedName("Rock")
    @Expose
    private final double rock;
    @SerializedName("Electronic")
    @Expose
    private final double electronic;
    @SerializedName("Experimental")
    @Expose
    private final double experimental;
    @SerializedName("Folk")
    @Expose
    private final double folk;
    @SerializedName("Instrumental")
    @Expose
    private final double instrumental;
    @SerializedName("International")
    @Expose
    private final double international;

    public Confidences(double[] confidences) {
        this.blues = confidences[0];
        this.classical = confidences[1];
        this.country = confidences[2];
        this.disco = confidences[3];
        this.hipHop = confidences[4];
        this.jazz = confidences[5];
        this.metal = confidences[6];
        this.pop = confidences[7];
        this.reggae = confidences[8];
        this.rock = confidences[9];
        this.electronic = confidences[10];
        this.experimental = confidences[11];
        this.folk = confidences[12];
        this.instrumental = confidences[13];
        this.international = confidences[14];
        confidenceArray = new double[]{blues, classical, country, disco, hipHop, jazz, metal, pop, reggae, rock, electronic, experimental, folk, instrumental, international};
    }
    public double[] getConfidenceValues() {
        return new double[]{blues, classical, country, disco, hipHop, jazz, metal, pop, reggae, rock, electronic, experimental, folk, instrumental, international};
    }
    public HashMap<Double, String> getConfidenceHashMap() {
        HashMap<Double, String> retVal = new HashMap<>();
        for(int i = 0; i < confidenceArray.length; i++) {
            retVal.put(confidenceArray[i], getStringNameOfGenre(i));
        }
        return retVal;
    }
    public String getStringNameOfGenre(int i) {
        switch (i) {
            case 0:
                return "Blues".toUpperCase();
            case 1:
                return "Classical".toUpperCase();
            case 2:
                return "Country".toUpperCase();
            case 3:
                return "Disco".toUpperCase();
            case 4:
                return "HipHop".toUpperCase();
            case 5:
                return "Jazz".toUpperCase();
            case 6:
                return "Metal".toUpperCase();
            case 7:
                return "Pop".toUpperCase();
            case 8:
                return "Reggae".toUpperCase();
            case 9:
                return "Rock".toUpperCase();
            case 10:
                return "Electronic".toUpperCase();
            case 11:
                return "Experimental".toUpperCase();
            case 12:
                return "Folk".toUpperCase();
            case 13:
                return "Instrumental".toUpperCase();
            case 14:
                return "International".toUpperCase();
            default:
                throw new IllegalArgumentException("Invalid genre index: " + i);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Confidences{" +
                "blues=" + blues +
                ", classical=" + classical +
                ", country=" + country +
                ", disco=" + disco +
                ", hipHop=" + hipHop +
                ", jazz=" + jazz +
                ", metal=" + metal +
                ", pop=" + pop +
                ", reggae=" + reggae +
                ", rock=" + rock +
                ", electronic=" + electronic +
                ", experimental=" + experimental +
                ", folk=" + folk +
                ", instrumental=" + instrumental +
                ", international=" + international +
                '}';
    }
}
