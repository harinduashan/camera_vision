package com.example.cameravision;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.service.voice.VoiceInteractionSessionService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;

public class MainActivity extends AppCompatActivity {
    private zoomImageView imageView;
    private ScaleGestureDetector scaleGestureDetector;
    private float mScaleFactor = 1.0f;
    private Uri image_uri;
    private double refObjDist;
    private double [] calibrateOne, calibrateTwo, crackOne, crackTwo;

    private EditText editLength;
    private TextView firstRefCoordinateX, firstRefCoordinateY;
    private TextView secondRefCoordinateX, secondRefCoordinateY;
    private TextView firstCrackPointX, firstCrackPointY;
    private TextView secondCrackPointX, secondCrackPointY;
    private TextView finalDistanceTextView;
    private Button segmentationBtn;
    private SeekBar segSeek;
    private static final int IMAGE_PICK_CODE = 1000;
    private static final int IMAGE_CAPTURE_CODE = 1002;
    private static final int CAMERA_PERMISSION_CODE = 2001;
    private static final int GALLERY_PERMISSION_CODE = 2002;


    // Testing OpenCV
    private static String TAG = "MainActivity";
    static {
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "Configured");
        }
        else {
            Log.d(TAG, "Not Success");
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        editLength = findViewById(R.id.editLength);
        firstRefCoordinateX = findViewById(R.id.firstRefCoordinateX);
        firstRefCoordinateY = findViewById(R.id.firstRefCoordinateY);
        secondRefCoordinateX = findViewById(R.id.secondRefCoordinateX);
        secondRefCoordinateY = findViewById(R.id.secondRefCoordinateY);
        firstCrackPointX = findViewById(R.id.firstCrackPointX);
        firstCrackPointY = findViewById(R.id.firstCrackPointY);
        secondCrackPointX = findViewById(R.id.secondCrackPointX);
        secondCrackPointY = findViewById(R.id.secondCrackPointY);
        finalDistanceTextView = findViewById(R.id.finalDistanceTextView);
        segmentationBtn = findViewById(R.id.seg_btn);
        segSeek = findViewById(R.id.seekBar);


//        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
        final TextView coordinates =(TextView) findViewById(R.id.textView);
        //bitmap = ((BitmapDrawable) Map.getDrawable()).getBitmap();
        //coordinates.setText("Touch coordinates:"+String.valueOf(event.getX())+"x"+String.valueOf(event.getY()));
        //int pixel = bitmap.getPixel((int)event.getX(), (int)event.getY());
       /* imageView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                int[] values = new int[2];
                view.getLocationOnScreen(values);
                Log.d("X & Y",values[0]+" "+values[1]);
            }
        });

        */

       imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View view, MotionEvent motionEvent) {
                int[] locations = new int[2];
                view.getLocationOnScreen(locations);
                coordinates.setText(String.valueOf(motionEvent.getX())+"  "+String.valueOf(motionEvent.getY()));
                //view.getLocationInWindow(locations);
                return false;
            }
        });
    }

    public void onSetLengthClick(View view){
//        imageView.initPosition();
        setDistanceCalculationData("referenceObject");
    }

    public void onResetLengthClick(View view) {
        refObjDist = 0.0f;
        editLength.getText().clear();
    }

    public void onSetFirstRefClick(View view) {
        setDistanceCalculationData("calibrationFirstPoint");
    }

    public void onResetFirstRefClick(View view) {
        calibrateOne = new double[2];
        calibrateOne[0] = 0.0f;
        calibrateOne[1] = 0.0f;
        firstRefCoordinateX.setText("");
        firstRefCoordinateY.setText("");
    }

    public void onResetSecondRefClick(View view) {
        calibrateTwo = new double[2];
        calibrateTwo[0] = 0.0f;
        calibrateTwo[1] = 0.0f;
        secondRefCoordinateX.setText("");
        secondRefCoordinateY.setText("");
    }

    public void onSetSecondRefClick(View view) {
        setDistanceCalculationData("calibrationSecondPoint");
    }

    public void onResetCrackFirstClick(View view) {
        crackOne = new double[2];
        crackOne[0] = 0.0f;
        crackOne[1] = 0.0f;
        firstCrackPointX.setText("");
        firstCrackPointY.setText("");
    }

    public void onSetCrackFirstClick(View view) {
        setDistanceCalculationData("crackFirstPoint");
    }

    public void onResetCrackSecondClick(View view) {
        crackTwo = new double[2];
        crackTwo[0] = 0.0f;
        crackTwo[1] = 0.0f;
        secondCrackPointX.setText("");
        secondCrackPointY.setText("");
    }

    public void onSetCrackSecondClick(View view) {
        setDistanceCalculationData("crackSecondPoint");
    }

    private void setDistanceCalculationData(String caseId) {
        switch (caseId) {
            case "referenceObject":
                refObjDist = 0.0f;
                refObjDist = Double.parseDouble(editLength.getText().toString());
                Log.d(TAG, "LENGTH +++++++++ " + String.valueOf(refObjDist));
                break;

            case "calibrationFirstPoint":
                calibrateOne = new double[2];
//                calibrateOne[0] = Double.parseDouble(firstRefCoordinateX.getText().toString());
//                calibrateOne[1] = Double.parseDouble(firstRefCoordinateY.getText().toString());
                calibrateOne[0] = imageView.getPosX();
                calibrateOne[1] = imageView.getPosY();
                firstRefCoordinateX.setText(String.valueOf(calibrateOne[0]));
                firstRefCoordinateY.setText(String.valueOf(calibrateOne[1]));

                Log.d(TAG, "LENGTH +++++++++ " + String.valueOf(calibrateOne[0]) + " " + calibrateOne[1]);
                break;

            case "calibrationSecondPoint":
                calibrateTwo = new double[2];

                calibrateTwo[0] = imageView.getPosX();
                calibrateTwo[1] = imageView.getPosY();
                secondRefCoordinateX.setText(String.valueOf(calibrateTwo[0]));
                secondRefCoordinateY.setText(String.valueOf(calibrateTwo[1]));

                Log.d(TAG, "LENGTH +++++++++ " + String.valueOf(calibrateTwo[0]) + " " + calibrateTwo[1]);
                break;

            case "crackFirstPoint":
                crackOne = new double[2];

                crackOne[0] = imageView.getPosX();
                crackOne[1] = imageView.getPosY();
                firstCrackPointX.setText(String.valueOf(crackOne[0]));
                firstCrackPointY.setText(String.valueOf(crackOne[1]));

                Log.d(TAG, "LENGTH +++++++++ " + String.valueOf(crackOne[0]) + " " + crackOne[1]);
                break;

            case "crackSecondPoint":
                crackTwo = new double[2];

                crackTwo[0] = imageView.getPosX();
                crackTwo[1] = imageView.getPosY();
                secondCrackPointX.setText(String.valueOf(crackTwo[0]));
                secondCrackPointY.setText(String.valueOf(crackTwo[1]));

                Log.d(TAG, "LENGTH +++++++++ " + String.valueOf(crackTwo[0]) + " " + crackTwo[1]);
                break;

            default:
                break;
        }
    }

    public void onCalculateDistanceClick(View view) {
        finalDistanceTextView.setText(String.valueOf(getDistanceOfObject()));
    }

    public void onSegmentation(View view){
        if(segSeek.getVisibility() == View.VISIBLE){
            segSeek.setVisibility(View.GONE);
        }else{
            segSeek.setVisibility(View.VISIBLE);
        }
    }


    private double getDistanceOfObject() {
        double referenceObj = Math.sqrt(Math.pow((calibrateOne[0] - calibrateTwo[0]), 2) +
                Math.pow((calibrateOne[1] - calibrateTwo[1]), 2));
        double crackObj = Math.sqrt(Math.pow((crackOne[0] - crackTwo[0]), 2) +
                Math.pow((crackOne[1] - crackTwo[1]), 2));
        return refObjDist * (crackObj/referenceObj);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            scaleGestureDetector.onTouchEvent(event);
        }catch (Exception e){
            return false;
        }
        return true;
    }


    public void camEvent(View view) {
        imageView.invalidate();
        imageView.setImageBitmap(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                            PackageManager.PERMISSION_DENIED) {
                // permission not granted, request it.
                String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                // show popup for run time permission
                requestPermissions(permissions, CAMERA_PERMISSION_CODE);
            } else {
                //permission is already granted
                openCamera();
            }
        } else {
            //system is less than marshmallow
            openCamera();
        }
    }
    private void openCamera() {
        imageView.setColorFilter(null);
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Pictures");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        // Camera intent
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE);
    }
    private void pickImageFromGallery() {
        // intent to pick image
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_CODE);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_PERMISSION_CODE: {
                if (grantResults.length > 0 && grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    openCamera();
                } else {
                    // permission was denied
                    Toast.makeText(this, "Permission denied for camera...!", Toast.LENGTH_SHORT).show();
                }
            }
            break;
            case GALLERY_PERMISSION_CODE: {
                if (grantResults.length > 0 && grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    pickImageFromGallery();
                } else {
                    // permission was denied
                    Toast.makeText(this, "Permission denied for gallery...!", Toast.LENGTH_SHORT).show();
                }
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            // set image to view - Load Button
            assert data != null;
            imageView.setImageURI(data.getData());
        }
        else if (resultCode == RESULT_OK && requestCode == IMAGE_CAPTURE_CODE) {
            // Set the image captured to our imageView
            imageView.setImageURI(image_uri);
        }
    }

    public void loadEvent(View view) {
        imageView.setColorFilter(null);
        imageView.invalidate();
        imageView.setImageBitmap(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED) {
                // permission not granted, request it.
                String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
                // show popup for run time permission
                requestPermissions(permissions, GALLERY_PERMISSION_CODE);
            } else {
                //permission is already granted
                pickImageFromGallery();
            }
        } else {
            //system is less than marshmallow
            pickImageFromGallery();
        }
    }

    public void grayEvent(View view) {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
       // matrix.setYUV2RGB();
        //matrix.setRGB2YUV();
        imageView.setColorFilter(new ColorMatrixColorFilter(matrix));
    }

    public void filltEvent(View view) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
        Bitmap bitmap = drawable.getBitmap();
       // bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
     //   byte[] bytes = outputStream.toByteArray();
     //   String encodedImage = Base64.encodeToString(bytes, Base64.DEFAULT);
       // byte[] imageBytes = Base64.decode(encodedImage, Base64.DEFAULT);
     //   Bitmap decodedImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        Bitmap decodedImage = removeNoise(bitmap);
        imageView.setImageBitmap(decodedImage);
    }

    public Bitmap removeNoise(Bitmap bitmap) {
      //  bitmap = Bitmap.createBitmap(LABEL_SIDE_LENGTH, LABEL_SIDE_LENGTH, Bitmap.Config.ARGB_8888)
        return bitmap;
    }

    public void edgeDetection(View view) {
        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
        Bitmap bitmap = drawable.getBitmap();
        Bitmap decodedImage = detectEdges(bitmap);
        imageView.setImageBitmap(decodedImage);
    }

    private Bitmap detectEdges(Bitmap bitmap) {
        // Read the bitmap
        Mat rgba = new Mat();
        Utils.bitmapToMat(bitmap, rgba);

        // Convert to Grey scale
        Mat edges = new Mat(rgba.size(), CvType.CV_8UC1);
        Imgproc.cvtColor(rgba, edges, Imgproc.COLOR_RGB2GRAY, 4);

        // Apply Canny's Algorithm
        Imgproc.Canny(edges, edges, 80, 100);

        // Visualization
        Utils.matToBitmap(edges, bitmap);
        return bitmap;
    }
}