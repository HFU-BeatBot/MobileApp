package com.theriotjoker.beatbot;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Genre implements Serializable {

    @SerializedName("genre")
    @Expose
    private String genre;
    @SerializedName("confidences")
    @Expose
    private Confidences confidences;
    private int textColor;
    private int topColor;
    private int bottomColor;
    private int middleColor;
    public Genre(Confidences confidences) {
        this.confidences = confidences;
        genre = confidences.getStringNameOfGenre(getIndexOfMaxGenreConfidence());
        initializeThemeValues();
    }
    public void initializeThemeValues() {
        middleColor=0x00000000;
        this.genre = genre.toUpperCase();
        switch(genre.toUpperCase()) {
            case "ROCK":
                topColor = 0xFFFF2222;
                bottomColor = 0xFF7E0000;
                textColor = 0xFF0F0F0F;
                break;
            case "BLUES":
                topColor = 0xFF1B4FCC;
                bottomColor = 0xFF0099E7;
                textColor = 0xFF0F0F0F;
                break;
            case "CLASSICAL":
                topColor = 0xFFC69E69;
                bottomColor = 0xFFFFECCB;
                textColor = 0xFF0F0F0F;
                break;
            case "POP":
                topColor = 0xFFEC2BBF;
                bottomColor = 0xFFFF0087;
                textColor = 0xFFF0F0F0;
                break;
            case "HIPHOP":
                topColor = 0xFFFFFFFF;
                bottomColor = 0xFFA5A5A5;
                textColor = 0xFF0F0F0F;
                break;
            case "JAZZ":
                topColor = 0xFFF9C51A;
                bottomColor = 0xFFF00919;
                textColor = 0xFF0F0F0F;
                break;
            case "COUNTRY":
                topColor = 0xFFD2A65A;
                bottomColor = 0xFF5C3E0A;
                textColor = 0xFFF0F0F0;
                break;
            case "REGGAE":
                topColor = 0xFFD20000;
                bottomColor = 0xFF36F21A;
                middleColor = 0xFFFFFF00;
                textColor = 0xFF0F0F0F;
                break;
            case "DISCO":
                topColor = 0xFF490D4D; //0xFF490D4D
                bottomColor = 0xFF36F21A;
                middleColor = 0xFFFF00FF;
                textColor = 0xFF0F0F0F;
                break;
            case "METAL":
                topColor = 0xFF000000;
                bottomColor = 0xFFFFFFFF;
                textColor = 0xFFFFFFFF;
                break;
            default:
                throw new IllegalArgumentException("Unsupported Argument: "+genre);
        }
    }
    public String getMainGenreName() {
        return genre;
    }
    public int[] getColors() {
        if(middleColor != 0x000000) {
            return new int[] {topColor,middleColor,bottomColor};
        }
        return new int[]{topColor, bottomColor};
    }
    private int getIndexOfMaxGenreConfidence() {
        double[] conf = confidences.getConfidenceValues();
        int max = 0;
        for (int i = 1; i < conf.length; i++) {
            if(conf[max] < conf[i]) {
                max = i;
            }
        }
        return max;
    }
    public int getTextColor() {
        return textColor;
    }
    public Confidences getConfidences() {
        return confidences;
    }

}
