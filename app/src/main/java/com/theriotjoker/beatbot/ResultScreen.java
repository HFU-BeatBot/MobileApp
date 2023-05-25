package com.theriotjoker.beatbot;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.theriotjoker.beatbot.databinding.FragmentSecondBinding;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
        List<RelativeLayout> layoutList = Arrays.asList(binding.genre1, binding.genre2, binding.genre3, binding.genre4, binding.genre5);
        //layoutList.forEach(relativeLayout -> relativeLayout.animate().scaleY(0.5f));
        HashMap<String, Double> hashMap = genre.getTopFiveGenres();
        Iterator<RelativeLayout> iterator = layoutList.iterator();
        for(String s : hashMap.keySet()) {
            RelativeLayout layout = iterator.next();
            float translationFactor = layout.getHeight()*(0.5f-1f);
            System.out.println(layout.getHeight());
            layout.animate().translationY(translationFactor);
        }
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