package com.kshah21.readreceipts.Fragments;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kshah21.readreceipts.Activities.MainActivity;
import com.kshah21.readreceipts.Bookkeeping.RealmWrapper;
import com.kshah21.readreceipts.OCR.OCR;
import com.kshah21.readreceipts.R;

/**
 * Created by kunal on 12/28/17.
 */

public class OverviewFragment extends Fragment {

    private TextView titleText;
    private RealmWrapper realm;
    public static final String OPTION_NUMBER = "option_number";


    public OverviewFragment(){

    }

    public static Fragment newInstance(int position){
        Fragment fragment = new OverviewFragment();
        Bundle args = new Bundle();
        args.putInt(OPTION_NUMBER,position);
        fragment.setArguments(args);
        return fragment;
    }

    public void onAttach(Context activity){
        super.onAttach(activity);
    }

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View rootView = inflater.inflate(R.layout.overview_fragment, container, false);

        int pos = getArguments().getInt(OPTION_NUMBER);
        String option = getResources().getStringArray(R.array.drawer_options)[pos];

        titleText = (TextView) rootView.findViewById(R.id.overview_title);

        getActivity().setTitle(option);
        return rootView;
    }

    public void onActivityCreated(Bundle savedState){
        super.onActivityCreated(savedState);
    }

    public void onStart() {
        super.onStart();
    }

    public void onResume() {
        super.onResume();
    }

    public void onPause() {
        super.onPause();
    }

    public void onStop() {
        super.onStop();
    }

    public void onDestroyView() {
        super.onDestroyView();
    }

    public void onDestroy() {
        super.onDestroy();
    }

    public void onDetach() {
        super.onDetach();
    }

}
