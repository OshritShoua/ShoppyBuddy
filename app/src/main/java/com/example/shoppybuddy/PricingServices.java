package com.example.shoppybuddy;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.common.primitives.Chars;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static android.provider.ContactsContract.CommonDataKinds.Website.URL;

public class PricingServices
{
    private static String _appDataPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/ShoppyBuddy/"; //// getExternalStorageDirectory()
    private static String _imageFilePath = _appDataPath + "captured_image.jpg";
    private static String TAG;
    private String _language = "eng";
    public HashMap<Character, String> _currencySymbolsToCodes;//todo - this might change to a bimap
    private double _originalPrice;
    private double _convertedPrice;
    private String _baseCurrencyCode = "USD";
    private String _targetCurrencyCode = "ILS";
    private double _euroToBaseCurrencyRate;
    private double _euroToTargetCurrencyRate;
    private Context _context;
    private boolean _parsingComplete;

    PricingServices(Context context)
    {
        init(context);
    }

    public static String GetImageFilePath(){return _imageFilePath;}
    public boolean IsPriceParsingComplete(){return _parsingComplete;}

    private void init(Context context)
    {
        _context = context;
        TAG = "ShoppyBuddy.java";
        _currencySymbolsToCodes = new HashMap<Character, String>();
        _currencySymbolsToCodes.put('€', "EUR");
        _currencySymbolsToCodes.put('₪', "ILS");
        _currencySymbolsToCodes.put('¥', "JPY");
        _currencySymbolsToCodes.put('£', "GBP");
        _currencySymbolsToCodes.put('$', "USD");

        deleteExistingFilesAndDirs();
        createAppDirsOnPublicStorage();
        copyTesseractTrainingFileToPublicStorage();
    }

    private void deleteExistingFilesAndDirs()
    {
        String[] paths = {_appDataPath + "tessdata/" + _language + ".traineddata", _appDataPath + "tessdata/", _appDataPath, _imageFilePath};
        for (String path : paths) {
            boolean b;
            File node = new File(path);
            if (node.exists()) {
                b = node.delete();
                Log.v(TAG, "deleted " + path);
            }
        }
    }

    //todo - This can be more readable if using the Fils.copy() which requires a higher api level. Read about that error and see what it means.
    private void copyTesseractTrainingFileToPublicStorage()
    {
        if (!(new File(_appDataPath + "tessdata/" + _language + ".traineddata")).exists()) {
            try {
                AssetManager assetManager = _context.getAssets();
                InputStream in = assetManager.open("tessdata/" + _language + ".traineddata");
                OutputStream out = new FileOutputStream(new File(_appDataPath + "tessdata/" + _language + ".traineddata"));

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
                Log.v(TAG, "Copied " + _language + " traineddata");
                File f = new File(_appDataPath + "tessdata/" + _language + ".traineddata");
                boolean b = f.exists();
                long size = f.length();
            } catch (IOException e) {
                Log.e(TAG, "Was unable to copy " + _language + " traineddata " + e.toString());
            }
        }
    }

    private void createAppDirsOnPublicStorage()
    {
        String[] paths = new String[]{_appDataPath, _appDataPath + "tessdata/"};
        String state = Environment.getExternalStorageState();
        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!Environment.MEDIA_MOUNTED.equals(state)) {
                    Log.v(TAG, "ERROR: Creation of directories failed because external storage is not available for read/write");
                    return;
                }

                if (!dir.mkdirs()) {
                    Log.v(TAG, "ERROR: Creation of directory " + path + " failed");
                    return;
                } else {
                    Log.v(TAG, "Created directory " + path + " successfully");
                }
            }
        }
    }

//    public void onConversionApiResponseReceived(String response)
//    {
//        try
//        {
//            parseRatesFromConversionApiResponse(response);
//        }
//        catch (JSONException e)
//        {
//            e.printStackTrace();
//        }
//
//        calculateConvertedPrice();
//    }

    private String requestConversionRatesFromApi() throws InterruptedException
    {
        final JSONObject[] localResponse = {null};
        final Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {
                String url = "http://data.fixer.io/api/latest?access_key=28b1f943a2bc43b31e27eda845458bb8" + "&symbols=USD,ILS,AUD,CAD,PLN,MXN";
                RequestQueue queue = Volley.newRequestQueue(_context);
                RequestFuture<JSONObject> future = RequestFuture.newFuture();
                JsonObjectRequest request = new JsonObjectRequest(url, new JSONObject(), future, future);
                queue.add(request);

                try {
                    JSONObject jsonObject = future.get();
                    localResponse[0] = jsonObject;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        };
        Thread t = new Thread(runnable);
        t.start();
        t.join();
        return "foo";

//        String url = "http://data.fixer.io/api/latest?access_key=28b1f943a2bc43b31e27eda845458bb8" + "&symbols=USD,ILS,AUD,CAD,PLN,MXN";
//        // Instantiate the RequestQueue.
//        RequestQueue queue = Volley.newRequestQueue(_context);
        // Request a string response from the provided URL.
//        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
//                new Response.Listener<String>()
//                {
//                    @Override
//                    public void onResponse(String response)
//                    {
//                        //localResponse[0] = response;
//                        //onConversionApiResponseReceived(response);
//                    }
//                },
//                new Response.ErrorListener()
//                {
//                    @Override
//                    public void onErrorResponse(VolleyError error)
//                    {
//                        int errorCode = error.networkResponse.statusCode;
//                    }
//                }
//        );
//
//        //Add the request to the RequestQueue.
//        queue.add(stringRequest);
//        while (localResponse[0] == null)
//            Thread.sleep(20000);

        //return localResponse[0];


//
//          final JsonValueHolder holder = new JsonValueHolder();
//          Thread thread = new Thread(holder)
//          {
//              public void run()
//              {
//                  HttpSynchronizedRequest syncHttpRequest = new HttpSynchronizedRequest();
//                  JSONObject jsonObject = null;
//                  try {
//                      jsonObject = syncHttpRequest.execute().get(30, TimeUnit.SECONDS);
//                  } catch (InterruptedException e) {
//                      e.printStackTrace();
//                  } catch (ExecutionException e) {
//                      e.printStackTrace();
//                  } catch (TimeoutException e) {
//                      e.printStackTrace();
//                  }
//
//                  holder.setValue(new JSONObject());
//              }
//          };
//
//          thread.start();
//          thread.join();
//          return holder.getValue();
    }

    public class JsonValueHolder implements Runnable
    {
        private volatile JSONObject jsonObject;

        @Override
        public void run(){}
        public JSONObject getValue(){return jsonObject;}
        public void setValue(JSONObject obj){jsonObject = obj;}
    }

    private class HttpSynchronizedRequest extends AsyncTask<Void, Void, JSONObject>
    {
        @Override
        protected JSONObject doInBackground(Void... params) {
            String url = "http://data.fixer.io/api/latest?access_key=28b1f943a2bc43b31e27eda845458bb8" + "&symbols=USD,ILS,AUD,CAD,PLN,MXN";
            // Instantiate the RequestQueue.
            RequestQueue queue = Volley.newRequestQueue(_context);
            RequestFuture<JSONObject> future = RequestFuture.newFuture();
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, new JSONObject(), future, future);
            queue.add(request);

            try {
                JSONObject obj = future.get(30, TimeUnit.SECONDS);
                return obj;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public void parseRatesFromConversionApiResponse(String response) throws JSONException
    {
        JSONObject json = new JSONObject(response);

        if (!json.has("success") || json.getBoolean("success") != true || !json.has("rates"))
        {
            Log.v(TAG, "bad conversion url response");
            throw new JSONException("bad conversion url response");
        }

        double euroToBaseCurrencyRate = -1;
        double euroToTargetCurrencyRate = -1;
        JSONObject currencyCodesToRates = json.getJSONObject("rates");
        Iterator<String> keysIterator = currencyCodesToRates.keys();
        while(keysIterator.hasNext())
        {
            String currencyCode = (String)keysIterator.next();
            if(currencyCode.equals(_baseCurrencyCode))
                euroToBaseCurrencyRate = currencyCodesToRates.getDouble(currencyCode);
            if(currencyCode.equals(_targetCurrencyCode))
                euroToTargetCurrencyRate = currencyCodesToRates.getDouble(currencyCode);
        }

        if(euroToBaseCurrencyRate == -1 || euroToTargetCurrencyRate == -1)
        {
            Log.v(TAG, "response did not contain required rates");
            throw new JSONException("response did not contain required rates");
        }

        _euroToBaseCurrencyRate = euroToBaseCurrencyRate;
        _euroToTargetCurrencyRate = euroToTargetCurrencyRate;
    }

    public double ProvideConvertedPrice()
    {
        parsePriceFromPhoto();
        try
        {
            String ratesResponse = requestConversionRatesFromApi();
            parseRatesFromConversionApiResponse(ratesResponse);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        calculateConvertedPrice();
        return _convertedPrice;
    }

    private void calculateConvertedPrice()
    {
        double priceInEuros = _originalPrice / _euroToBaseCurrencyRate;
        double priceInTargetCurrency = priceInEuros * _euroToTargetCurrencyRate;
        _convertedPrice = priceInTargetCurrency;
        _parsingComplete = true;
    }


    public void parsePriceFromPhoto()
    {
        _parsingComplete = false;
        Bitmap bitmap = getAdjustedBitmapFromPhoto();
        String rawRecognizedText = getOCRedRawText(bitmap);
        parsePriceFromText(rawRecognizedText);
    }

    private void parsePriceFromText(String rawRecognizedText)
    {
        String filteredText = getFilteredText(rawRecognizedText);
        if (!foundPriceInText(filteredText)) {
            filteredText = ApplyHeuristicsOnText(filteredText);
            if (!foundPriceInText(filteredText)) {
                //todo - send message to the user to try and take a picture again, and send him to the camera again
            }
        }

        _originalPrice = Double.parseDouble("526"); //filteredText
    }

    private String ApplyHeuristicsOnText(String filteredText)
    {
        return filteredText;
    }

    private boolean foundPriceInText(String filteredText)
    {
        return true;
    }

    @NonNull
    private String getFilteredText(String rawRecognizedText)
    {
        Log.v(TAG, "OCRED TEXT: " + rawRecognizedText);

        rawRecognizedText = rawRecognizedText.trim();
        ArrayList<Character> whitelist = new ArrayList<>(Chars.asList(Chars.concat(" .,1234567890".toCharArray(), Chars.toArray(_currencySymbolsToCodes.keySet()))));
        StringBuilder builder = new StringBuilder();
        boolean foundMatch;
        for (char recognizedChar : rawRecognizedText.toCharArray()) {
            foundMatch = false;
            for (char approvedChar : whitelist) {
                if (recognizedChar == approvedChar) {
                    builder.append(recognizedChar);
                    foundMatch = true;
                    break;
                }
            }

            if (!foundMatch)
                builder.append('X');
        }

        return builder.toString();
    }

    private String getOCRedRawText(Bitmap bitmap)
    {
        Log.v(TAG, "Before baseApi");
        //todo - add logic that improves the ocr in this func
        TessBaseAPI baseApi = new TessBaseAPI();
        baseApi.setDebug(true);
        baseApi.init(_appDataPath, _language);
        baseApi.setImage(bitmap);
        String rawRecognizedText = baseApi.getUTF8Text();
        baseApi.end();
        return rawRecognizedText;
    }

    private Bitmap getAdjustedBitmapFromPhoto()
    {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;

        Bitmap bitmap = BitmapFactory.decodeFile(_imageFilePath, options);

        try {
            ExifInterface exif = new ExifInterface(_imageFilePath);
            int orientationMode = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            Log.v(TAG, "Orient: " + orientationMode);

            int rotate = 0;

            switch (orientationMode) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
            }

            Log.v(TAG, "Rotation: " + rotate);

            if (rotate != 0) {

                // Getting width & height of the given image.
                int w = bitmap.getWidth();
                int h = bitmap.getHeight();

                // Setting pre rotate
                Matrix mtx = new Matrix();
                mtx.preRotate(rotate);

                // Rotating Bitmap
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
            }

            // Convert to ARGB_8888, required by tess
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        } catch (IOException e) {
            Log.e(TAG, "Couldn't correct orientation: " + e.toString());
        }

        return bitmap;
    }

    public double GetConvertedPrice()
    {
        return _convertedPrice;
    }
}
