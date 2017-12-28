package com.kshah21.readreceipts.Fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kshah21.readreceipts.R;


public class CaptureFragment extends Fragment {

    public static final String OPTION_NUMBER = "option_number";

    public CaptureFragment(){

    }

    public static Fragment newInstance(int position){
        Fragment fragment = new CaptureFragment();
        Bundle args = new Bundle();
        args.putInt(OPTION_NUMBER,position);
        fragment.setArguments(args);
        return fragment;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        View rootView = inflater.inflate(R.layout.capture_fragment, container, false);

        int pos = getArguments().getInt(OPTION_NUMBER);
        String option = getResources().getStringArray(R.array.drawer_options)[pos];

        TextView textView = rootView.findViewById(R.id.capture_fragment_text);
        textView.setText(option);

        getActivity().setTitle(option);
        return rootView;
    }
}
