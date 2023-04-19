package com.theriotjoker.beatbot;

import java.io.Serializable;

public class Genre implements Serializable {
   // private GradientDrawable drawable;
    private final String genreName;
    private final int textColor;
    private final int topColor;

    private final int bottomColor;
    private int middleColor;
    public Genre(String genre) {
        middleColor=0x00000000;
        this.genreName = genre.toUpperCase();
        //final GradientDrawable.Orientation orientation = GradientDrawable.Orientation.TOP_BOTTOM;
    /*    int firstColor;
        int secondColor;*/
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
                throw new IllegalArgumentException("Critical Communication Error: Wrong/Unsupported Argument: "+genre);
        }
        //drawable = new GradientDrawable(orientation, new int[]{firstColor,secondColor});
    }
    /*public GradientDrawable getDrawable() {
        return drawable;
    }*/
    public String getGenreName() {
        return genreName;
    }
    public int[] getColors() {
        if(middleColor != 0x000000) {
            return new int[] {topColor,middleColor, bottomColor};
        }
        return new int[]{topColor, bottomColor};
    }

    public int getTextColor() {
        return textColor;
    }
}
