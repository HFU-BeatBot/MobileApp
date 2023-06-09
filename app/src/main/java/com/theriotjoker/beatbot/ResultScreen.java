package com.theriotjoker.beatbot;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.theriotjoker.beatbot.databinding.FragmentSecondBinding;
import java.util.ArrayList;
import java.util.HashMap;
public class ResultScreen extends Fragment {

    private FragmentSecondBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentSecondBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        GradientDrawable drawable;
        assert getArguments() != null;
        Genre genre = (Genre) getArguments().getSerializable("GENRE");
        TextView genreTextView = view.findViewById(R.id.genre);
        TextView titleTextView = view.findViewById(R.id.titleText);
        genreTextView.setText(genre.getMainGenreName());
        genreTextView.setTextColor(genre.getTextColor());
        titleTextView.setTextColor(genre.getTextColor());
        System.out.println(genre.getTopFiveGenres());
        HashMap<String, Double> hashMap = genre.getTopFiveGenres(); //The HashMap contains information in the following format {(REGGAE, 0.49), (METAL, 0.11), (POP, 0.01), ...}

        BarChart genreBarChart = binding.genreBarChart; //Get the bar chart from the screen (see xml)

        ArrayList<BarEntry> barEntryArrayList = new ArrayList<BarEntry>(); //Every entry of the chart is saved here, as an entry object
        //an entry object contains the index, or position, of every entry, and then the double value of the entry
        int i = 0;
        for(Double d : hashMap.values()) {
            barEntryArrayList.add(new BarEntry(i, d.floatValue())); //add each entry to the bar chart
            i++;
        }
        BarDataSet barDataSet = new BarDataSet(barEntryArrayList, ""); //create a BarDataSet object from the entries that will be in the graph
        barDataSet.setColors(R.color.hfugreen); //set the color of the bars, since the bars are somewhat transparent, this color changes with the background
        barDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return ""; //make it so that there are no labels on the top of each bar in the bar chart, otherwise there is too much information
            }
        });
        ArrayList<String> graphLabels = new ArrayList<>(hashMap.keySet()); //Labels of each graph, these are also saved in the hashmap
        BarData genreChartData = new BarData(barDataSet); //the data in the graph, saved as an object, extracted from the BarDataSet

        XAxis graphXAxis = genreBarChart.getXAxis(); //get the x axis of the graph
        graphXAxis.setValueFormatter(new IndexAxisValueFormatter(graphLabels)); //set each value of the bars (eg. REGGAE, METAL, ROCK, etc.)
        graphXAxis.setPosition(XAxis.XAxisPosition.BOTTOM); //make the Xaxis description be at the bottom
        graphXAxis.setDrawGridLines(false); //no lines should be drawn, otherwise too much info is shown
        graphXAxis.setTextSize(13);
        graphXAxis.setLabelRotationAngle(45f);
        //Setting the bar chart design
        genreBarChart.getLegend().setEnabled(false); //no legend, since it is self explanatory
        genreBarChart.getAxisLeft().setDrawGridLines(true); //add y lines, so the graph looks more readable
        genreBarChart.setBorderColor(Color.BLACK); //border should be black
        genreBarChart.setData(genreChartData); //setting the data created above
        genreBarChart.setFitBars(true);  //left and right side have a bit of padding, so that every bar is evenly spaced
        genreBarChart.getDescription().setEnabled(false); //no description for the graph, not necessary
        genreBarChart.animateY(750); //animation, 750 miliseconds, looks nice
        genreBarChart.setExtraBottomOffset(15f); //shift everything in the y direction, to make place for the labels for each bar
        //disabling the zoom function
        genreBarChart.setDoubleTapToZoomEnabled(false);
        genreBarChart.setScaleEnabled(false);
        genreBarChart.setDragEnabled(false);
        ConstraintLayout layout = view.findViewById(R.id.resultPageLayout);
        drawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, genre.getColors());
        layout.setBackground(drawable);
        requireActivity().getWindow().setBackgroundDrawable(drawable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}