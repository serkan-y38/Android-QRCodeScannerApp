package com.example.qrcodescanner;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BottomDialog extends BottomSheetDialogFragment {
    private TextView urlTv, visitTv;
    private ImageButton closeImageButton;
    private String URL;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstancesState) {
        View view = inflater.inflate(R.layout.bottom_dialog, container, false);

        urlTv = view.findViewById(R.id.urlTv);
        visitTv = view.findViewById(R.id.visitTv);
        closeImageButton = view.findViewById(R.id.close);

        urlTv.setText(URL);

        closeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();

            }
        });

        visitTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent("android.intent.action.VIEW");
                    intent.setData(Uri.parse(URL));
                    startActivity(intent);

                } catch (Exception e) {
                    Toast.makeText(getContext(), "Error occurred while parsing url, search manually using url.", Toast.LENGTH_LONG).show();
                    e.getMessage();

                }finally {
                    dismiss();

                }

            }
        });

        return view;
    }

    public void fetchurl(String url) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                URL = url;

            }

        });

    }

}
