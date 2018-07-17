package com.example.shoppybuddy;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

public class CartReviewActivity extends AppCompatActivity {
    private Cart _cart;
    private PricingServices _pricingServices;
    private static final int REQUEST_IMAGE_CAPTURE = 10;
    private static final int REQUEST_WRITE_PERMISSION = 20;
    private static final String LOG_TAG = "Text API";
    private static final String SAVED_INSTANCE_URI = "uri";
    private static final String SAVED_INSTANCE_RESULT = "result";
    private TextView scanResults;
    private Uri capturedImageUri;
    private TextRecognizer textDetector;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart_review);
        scanResults = findViewById(R.id.scanResults);
        if (savedInstanceState != null) {
            capturedImageUri = Uri.parse(savedInstanceState.getString(SAVED_INSTANCE_URI));
            scanResults.setText(savedInstanceState.getString(SAVED_INSTANCE_RESULT));
        }

        textDetector = new TextRecognizer.Builder(getApplicationContext()).build();
        initComponents();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_WRITE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePicture();
                } else {
                    Toast.makeText(CartReviewActivity.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            launchMediaScanIntent();
            try {
                Bitmap bitmap = decodeBitmapUri(this, capturedImageUri);
                if (textDetector.isOperational() && bitmap != null) {
                    Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                    SparseArray<TextBlock> textBlocks = textDetector.detect(frame);
                    String blocks = "";
                    String lines = "";
                    String words = "";
                    for (int index = 0; index < textBlocks.size(); index++) {
                        //extract scanned text blocks here
                        TextBlock tBlock = textBlocks.valueAt(index);
                        blocks = blocks + tBlock.getValue() + "\n" + "\n";
                        for (Text line : tBlock.getComponents()) {
                            //extract scanned text lines here
                            lines = lines + line.getValue() + "\n";
                            for (Text element : line.getComponents()) {
                                //extract scanned text words here
                                words = words + element.getValue() + ", ";
                            }
                        }
                    }
                    if (textBlocks.size() == 0) {
                        scanResults.setText("Scan Failed: Found nothing to scan");
                    } else {
//                        scanResults.setText(scanResults.getText() + "Blocks: " + "\n");
//                        scanResults.setText(scanResults.getText() + blocks + "\n");
//                        scanResults.setText(scanResults.getText() + "---------" + "\n");
//                        scanResults.setText(scanResults.getText() + "Lines: " + "\n");
//                        scanResults.setText(scanResults.getText() + lines + "\n");
//                        scanResults.setText(scanResults.getText() + "---------" + "\n");
//                        scanResults.setText(scanResults.getText() + "Words: " + "\n");
//                        scanResults.setText(scanResults.getText() + words + "\n");
//                        scanResults.setText(scanResults.getText() + "---------" + "\n");
                        scanResults.setText(words + "\n");
                        System.out.println(words);
                    }
                } else {
                    scanResults.setText("Could not set up the detector!");
                }
            } catch (Exception e) {
                Toast.makeText(this, "Failed to load Image", Toast.LENGTH_SHORT)
                        .show();
                Log.e(LOG_TAG, e.toString());
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (capturedImageUri != null) {
            outState.putString(SAVED_INSTANCE_URI, capturedImageUri.toString());
            outState.putString(SAVED_INSTANCE_RESULT, scanResults.getText().toString());
        }
        super.onSaveInstanceState(outState);
    }

    //todo - importent.
    // The switch to this CartReviewActivity will be from 2 places:
    // 1. When starting a new cart.
    // 2. When entering an existing cart - in which case the items should be fetched from the db.
    // This implementation is now for a new EMPTY cart only.
    private void initComponents() {
        ImageButton captureImageButton = findViewById(R.id.cameraButton);
        captureImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(CartReviewActivity.this, new
                        String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, REQUEST_WRITE_PERMISSION);
            }
        });

        //todo - as explained, somewhere along these lines there need not be a creation of an empty cart but from a db
        _cart = new Cart(); //instead of "createCartFromDbEntry()
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, _cart.items);
        ListView itemListView = findViewById(R.id._dynamic_item_list);
        itemListView.setAdapter(adapter);
    }

    private void takePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = new File(Environment.getExternalStorageDirectory(), "captured_image.jpg");
        capturedImageUri = FileProvider.getUriForFile(CartReviewActivity.this,
                BuildConfig.APPLICATION_ID + ".provider", photo);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
    }

    private void launchMediaScanIntent() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(capturedImageUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private Bitmap decodeBitmapUri(Context ctx, Uri uri) throws FileNotFoundException {
        int targetW = 600;
        int targetH = 600;
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(ctx.getContentResolver().openInputStream(uri), null, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeStream(ctx.getContentResolver()
                .openInputStream(uri), null, bmOptions);
    }
}