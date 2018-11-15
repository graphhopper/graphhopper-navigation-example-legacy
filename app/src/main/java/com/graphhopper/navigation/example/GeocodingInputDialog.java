package com.graphhopper.navigation.example;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class GeocodingInputDialog extends DialogFragment {

    private String geocodingInput = "";

    public void setGeocodingInput(String geocodingInput) {
        this.geocodingInput = geocodingInput;
    }

    public interface NoticeDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
    }

    NoticeDialogListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View modifiedView = inflater.inflate(R.layout.geocoding_input, null);
        if (!geocodingInput.isEmpty()) {
            EditText jobIdEditText = (EditText) modifiedView.findViewById(R.id.geocoding_input_id);
            jobIdEditText.setText(geocodingInput);
        }

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(modifiedView)
                // Add action buttons
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogPositiveClick(GeocodingInputDialog.this);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        GeocodingInputDialog.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }

}


