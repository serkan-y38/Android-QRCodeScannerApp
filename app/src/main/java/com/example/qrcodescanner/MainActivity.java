package com.example.qrcodescanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ListenableFuture listenableFuture;
    private ExecutorService executorService;
    private PreviewView previewView;
    private ImageAnalyzer imageAnalyzer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        previewView = findViewById(R.id.previewView);
        executorService = Executors.newSingleThreadExecutor();

        imageAnalyzer = new ImageAnalyzer(getSupportFragmentManager());
        listenableFuture = ProcessCameraProvider.getInstance(this);

        listenableFuture.addListener(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                try {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != (PackageManager.PERMISSION_GRANTED)) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 100);

                    } else {
                        ProcessCameraProvider processCameraProvider = (ProcessCameraProvider) listenableFuture.get();
                        bindpreview(processCameraProvider);

                    }
                } catch (ExecutionException | InterruptedException e) {
                    Log.e("error", e.getMessage());

                }

            }

        }, ContextCompat.getMainExecutor(this));

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0) {
            ProcessCameraProvider processCameraProvider = null;

            try {
                processCameraProvider = (ProcessCameraProvider) listenableFuture.get();

            } catch (InterruptedException | ExecutionException e) {
                Log.e("error", e.getMessage());

            }
            bindpreview(processCameraProvider);
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void bindpreview(ProcessCameraProvider processCameraProvider) {

        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector
                .Builder()
                .requireLensFacing(
                        CameraSelector.LENS_FACING_BACK
                )
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        ImageCapture imageCapture = new ImageCapture.Builder().build();

        ImageAnalysis imageAnalysis = new ImageAnalysis
                .Builder()
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(executorService, imageAnalyzer);
        processCameraProvider.unbindAll();
        processCameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);

    }

    public class ImageAnalyzer implements ImageAnalysis.Analyzer {
        private FragmentManager fragmentManager;
        private BottomDialog dialog;

        public ImageAnalyzer(FragmentManager fragmentManager) {
            this.fragmentManager = fragmentManager;
            dialog = new BottomDialog();
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void analyze(@NonNull ImageProxy image) {
            scan(image);
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        private void scan(ImageProxy image) {

            @SuppressLint("UnsafeOptInUsageError")
            Image image1 = image.getImage();
            assert image1 != null;

            InputImage inputImage = InputImage.fromMediaImage(image1, image.getImageInfo().getRotationDegrees());
            BarcodeScannerOptions options = new BarcodeScannerOptions
                    .Builder()
                    .setBarcodeFormats(
                            Barcode.FORMAT_QR_CODE,
                            Barcode.FORMAT_AZTEC
                    )
                    .build();

            BarcodeScanner barcodeScanner = BarcodeScanning.getClient(options);
            Task<List<Barcode>> listTask = barcodeScanner.process(inputImage)
                    .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                        @Override
                        public void onSuccess(List<Barcode> barcodes) {
                            readBarcode(barcodes);

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MainActivity.this, "Oops, something went wrong. Try again.", Toast.LENGTH_SHORT).show();

                        }
                    })
                    .addOnCompleteListener(new OnCompleteListener<List<Barcode>>() {
                        @Override
                        public void onComplete(@NonNull @NotNull Task<List<Barcode>> task) {
                            image.close();

                        }
                    });

        }

        private void readBarcode(List<Barcode> barcodes) {

            for (Barcode barcode : barcodes) {

                if (barcode.getValueType() == Barcode.TYPE_URL) {

                    if (!dialog.isAdded())
                        dialog.show(fragmentManager, "dialog");

                    dialog.fetchurl(barcode.getUrl().getUrl());
                    //String title = barcode.getUrl().getTitle();
                    // String url = barcode.getUrl().getUrl();

                }

            }

        }

    }

}
