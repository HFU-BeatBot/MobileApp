package com.theriotjoker.beatbot;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.theriotjoker.beatbot.databinding.FragmentSecondBinding;

public class UseFileScreen extends Fragment {

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
        genreTextView.setText(genre.getGenreName());
        genreTextView.setTextColor(genre.getTextColor());
        titleTextView.setTextColor(genre.getTextColor());
        ConstraintLayout layout = view.findViewById(R.id.resultPageLayout);
        layout.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, genre.getColors()));
        binding.buttonSecond.setOnClickListener(view1 -> NavHostFragment.findNavController(UseFileScreen.this)
                .navigate(R.id.fileScreenToMainScreen));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}