package com.mikhailkazhamiaka.viatickets;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by mikhailkazhamiaka on 2016-02-21.
 */
public class TicketsListAdapter extends BaseAdapter implements ListAdapter {

    private ArrayList<Ticket> list;
    private Context context;

    public TicketsListAdapter(ArrayList<Ticket> list, Context context) {
        this.list = list;
        this.context = context;
    }


    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        final int pos = position;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.ticket_item, null);

        TextView textView = (TextView) view.findViewById(R.id.ticket_info);
        textView.setText(list.get(position).getFormattedText());

        Button pdfButton = (Button) view.findViewById(R.id.pdfButton);
        Button qrButton = (Button) view.findViewById(R.id.qrButton);

        pdfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: BRING UP PDF
                Ticket ticket = list.get(pos);
                String fileStr = ticket.getPdfFile();
                File file = new File(fileStr);
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                Uri uri = Uri.parse("content://com.mikhailkazhamiaka.viatickets/" + fileStr);
                intent.setDataAndType(uri, "application/pdf");
                v.getContext().startActivity(intent);
            }
        });

        qrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: BRING UP
                Ticket ticket = list.get(pos);
                String fileStr = ticket.getQrFile();
                File file = new File(fileStr);
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                Uri uri = Uri.parse("content://com.mikhailkazhamiaka.viatickets/" + fileStr);
                intent.setDataAndType(uri, "image/*");
                v.getContext().startActivity(intent);
            }
        });

        return view;
    }
}
