package com.theriotjoker.beatbot;

import java.util.ArrayList;

public class TimerUtil {
    private static ArrayList<Long> arrayList = new ArrayList<>();
    private static long startTime;
    private static long endTime;
    public static void setStartTime(long startTime) {
        TimerUtil.startTime = startTime;
    }
    public static void setEndTime(long endTime) {
        TimerUtil.endTime = endTime;
        arrayList.add(endTime-startTime);

    }
    public static double getAverageTime() {
        double times = 0L;
        for(Long l : arrayList) {
            times = times + l;
        }
        times = times / arrayList.size();
        return times/1000;
    }
}
