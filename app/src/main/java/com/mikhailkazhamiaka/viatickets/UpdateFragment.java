package com.mikhailkazhamiaka.viatickets;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by mikhailkazhamiaka on 2016-02-25.
 */
public class UpdateFragment extends DialogFragment {

    private String mEmailRangeLower;
    private String mEmailRangeUpper;
    private String mTicketRangeLower;
    private String mTicketRangeUpper;

    private EditText emailRangeLowerEditText;
    private EditText emailRangeUpperEditText;
    private EditText ticketRangeLowerEditText;
    private EditText ticketRangeUpperEditText;

    private CheckBox clearDBCheck;
    private CheckBox downloadAllCheck;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder updateDialog = new AlertDialog.Builder(getActivity());
        final View view = getActivity().getLayoutInflater().inflate(R.layout.update_fragment, null);
        updateDialog.setView(view);

        clearDBCheck = (CheckBox) view.findViewById(R.id.clearDBCheckBox);
        downloadAllCheck = (CheckBox) view.findViewById(R.id.downloadAllCheck);

        emailRangeLowerEditText = (EditText) view.findViewById(R.id.updateEmailRangeLower);
        emailRangeUpperEditText = (EditText) view.findViewById(R.id.updateEmailRangeUpper);

        ticketRangeLowerEditText = (EditText) view.findViewById(R.id.updateTicketRangeLower);
        ticketRangeUpperEditText = (EditText) view.findViewById(R.id.updateTicketRangeUpper);

        emailRangeLowerEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mEmailRangeLower = s.toString();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        emailRangeUpperEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mEmailRangeUpper = s.toString();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        ticketRangeLowerEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mTicketRangeLower = s.toString();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        ticketRangeUpperEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mTicketRangeUpper = s.toString();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });


        updateDialog.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO: send bundle back
                if (!areDatesValid()) {
                    Toast.makeText(getActivity().getBaseContext(), "Invalid date specified", Toast.LENGTH_SHORT).show();
                    return;
                }
                Bundle bundle = new Bundle();
                if ((mEmailRangeLower == null || mEmailRangeUpper == null) ||
                        (mEmailRangeLower.length() != mEmailRangeUpper.length())) {
                    mEmailRangeLower = null;
                    mEmailRangeUpper = null;
                }
                if ((mTicketRangeLower == null || mTicketRangeUpper == null) ||
                        (mTicketRangeLower.length() != mTicketRangeUpper.length())) {
                    mTicketRangeLower = null;
                    mTicketRangeUpper = null;
                }
                bundle.putString("email_lower", mEmailRangeLower);
                bundle.putString("email_upper", mEmailRangeUpper);
                bundle.putString("ticket_lower", mTicketRangeLower);
                bundle.putString("ticket_upper", mTicketRangeUpper);
                bundle.putBoolean("clear_db", clearDBCheck.isChecked());
                bundle.putBoolean("download_all", downloadAllCheck.isChecked());
                MainActivity callingActivity = (MainActivity) getActivity();
                callingActivity.onUpdateFragmentReturn(bundle);
                dialog.dismiss();
            }
        });
        updateDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });


        return updateDialog.create();
    }

    public boolean areDatesValid() {
        String strArr[] = {mEmailRangeLower, mEmailRangeUpper, mTicketRangeLower, mTicketRangeUpper};
        for (String s : strArr) {
            if (s == null || s.equals("")) continue;
            String split[] = s.split("/");
            if (split.length != 3) return false;
            int day = Integer.parseInt(split[2]);
            int month = Integer.parseInt(split[1]);
            int year = Integer.parseInt(split[0]);
            if (day < 1 || day > 31) return false; // not as rigorous since it depends on month but whatever for now
            if (month < 1 || month > 12) return false;
            if (year < 1000 || year > 9999) return false; // not really needed
        }
        return true;
    }



}
