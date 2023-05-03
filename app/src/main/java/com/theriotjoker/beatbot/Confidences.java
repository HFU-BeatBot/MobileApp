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

    public void setBlues(Double blues) {
        this.blues = blues;
    }


    public void setClassical(Double classical) {
        this.classical = classical;
    }


    public void setCountry(Double country) {
        this.country = country;
    }


    public void setDisco(Double disco) {
        this.disco = disco;
    }


    public void setHipHop(Double hipHop) {
        this.hipHop = hipHop;
    }


    public void setJazz(Double jazz) {
        this.jazz = jazz;
    }


    public void setMetal(Double metal) {
        this.metal = metal;
    }


    public void setPop(Double pop) {
        this.pop = pop;
    }


    public void setReggae(Double reggae) {
        this.reggae = reggae;
    }


    public void setRock(Double rock) {
        this.rock = rock;
    }

}
