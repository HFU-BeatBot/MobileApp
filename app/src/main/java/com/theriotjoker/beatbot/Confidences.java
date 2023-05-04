package com.theriotjoker.beatbot;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Confidences {

    @SerializedName("Blues")
    @Expose
    private Double blues;
    @SerializedName("Classical")
    @Expose
    private Double classical;
    @SerializedName("Country")
    @Expose
    private Double country;
    @SerializedName("Disco")
    @Expose
    private Double disco;
    @SerializedName("HipHop")
    @Expose
    private Double hipHop;
    @SerializedName("Jazz")
    @Expose
    private Double jazz;
    @SerializedName("Metal")
    @Expose
    private Double metal;
    @SerializedName("Pop")
    @Expose
    private Double pop;
    @SerializedName("Reggae")
    @Expose
    private Double reggae;
    @SerializedName("Rock")
    @Expose
    private Double rock;

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
    }
    public double[] getConfidenceValues() {
        return new double[]{blues, classical, country, disco, hipHop, jazz, metal, pop, reggae, rock};
    }
    public String getStringNameOfGenre(int i) {
        switch(i) {
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
            default:
                throw new IllegalArgumentException("Invalid genre index: " + i);
        }
    }

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
                '}';
    }
}
