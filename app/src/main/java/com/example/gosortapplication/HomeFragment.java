package com.example.gosortapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.view.ViewTreeObserver;
import android.util.TypedValue;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        // If there's a search bar inside the layout, wire a focus listener so
        // parent containers using @drawable/card_inner_bg can show the focused outline
        EditText searchBar = root.findViewById(R.id.searchBar);
        if (searchBar != null) {
            searchBar.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    // Walk up to the immediate parent CardView inner container and activate it
                    View parent = (View) v.getParent();
                    if (parent != null) {
                        parent.setActivated(hasFocus);
                    }
                }
            });
        }

        return root;
    }
}
