package com.example.shoppybuddy;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;

public class CartReviewActivity extends AppCompatActivity
{
    Cart _cart;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private PricingServices _pricingServices;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart_review);
        init();
    }

    //todo - importnt. The switch to this CartReviewActivity will be from 2 places: 1. When starting a new cart.
    //todo 2. When entering an existing cart - in which case the items should be fetched from the db.
    //todo - This implementation is now for a new EMPTY cart only.
    private void init()
    {
        //todo - as explained, somewhere along these lines there need not be a creation of an empty cart but from a db
        _cart = new Cart(); //instead of "createCartFromDbEntry()
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, _cart.items);
        ListView itemListView = findViewById(R.id._dynamic_item_list);
        itemListView.setAdapter(adapter);
    }

    public void OnCameraButtonClick(View view)
    {
        _pricingServices = new PricingServices(this);
        File imageFile = new File(PricingServices.GetImageFilePath());
        Uri outputFileUri = Uri.fromFile(imageFile);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); //create an intent to start an image capturing activity
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        if (intent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK)
        {
//            _pricingServices.parsePriceFromPhoto();
//            while (!_pricingServices.IsPriceParsingComplete()) {
//                try {
//                    Thread.sleep(200);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
            double convertedPrice = _pricingServices.ProvideConvertedPrice();
        }
        else
        {
            return;
        }
    }
}
