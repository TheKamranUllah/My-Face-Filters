
package com.kamranullah.facespotter;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.kamranullah.facespotter.ui.camera.CameraSourcePreview;
import com.kamranullah.facespotter.ui.camera.GraphicOverlay;

import java.io.IOException;

import static android.view.View.GONE;


public final class FaceActivity extends AppCompatActivity {

  private static final String TAG = "FaceActivity";

  private static final int RC_HANDLE_GMS = 9001;
  // permission request codes need to be < 256
  private static final int RC_HANDLE_CAMERA_PERM = 255;
  private int typeFace = 0;

  private CameraSource mCameraSource = null;
  private CameraSourcePreview mPreview;
  private GraphicOverlay mGraphicOverlay;
  private boolean mIsFrontFacing = true;

  private static final int MASK[] = {
          R.id.no_filter,
          R.id.red_hat,
          R.id.hair,
          R.id.op,
          R.id.snap,
          R.id.pig_nose,
          R.id.glasses5,
          R.id.mask2,
          R.id.dog,
          R.id.cat2
  };


  // Activity event handlers
  // =======================

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate called.");

    setContentView(R.layout.activity_face);

    mPreview = (CameraSourcePreview) findViewById(R.id.preview);
    mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);

    if (savedInstanceState != null) {
      mIsFrontFacing = savedInstanceState.getBoolean("IsFrontFacing");
    }

    // Start using the camera if permission has been granted to this app,
    // otherwise ask for permission to use it.
    int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
    if (rc == PackageManager.PERMISSION_GRANTED) {
      createCameraSource();
    } else {
      requestCameraPermission();
    }


    ImageButton face = (ImageButton) findViewById(R.id.face);
    face.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        if(findViewById(R.id.scrollView).getVisibility() == GONE){
          findViewById(R.id.scrollView).setVisibility(View.VISIBLE);
          ((ImageButton) findViewById(R.id.face)).setImageResource(R.drawable.face_select);
        }else{
          findViewById(R.id.scrollView).setVisibility(GONE);
          ((ImageButton) findViewById(R.id.face)).setImageResource(R.drawable.face);
        }
      }
    });

    ImageButton no_filter = (ImageButton) findViewById(R.id.no_filter);
    no_filter.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        findViewById(MASK[typeFace]).setBackgroundResource(R.drawable.round_background);
        typeFace = 0;
        findViewById(MASK[typeFace]).setBackgroundResource(R.drawable.round_background_select);
        FaceGraphic.mHatGraphic = null;
        FaceGraphic.mHairGraphic = null;
        FaceGraphic.mPigNoseGraphic = null;
        FaceGraphic.isPigNoseAllowed = false;

      }
    });

    ImageButton Redhat = (ImageButton) findViewById(R.id.red_hat);
    Redhat.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        findViewById(MASK[typeFace]).setBackgroundResource(R.drawable.round_background);
        typeFace = 1;
        findViewById(MASK[typeFace]).setBackgroundResource(R.drawable.round_background_select);

        FaceGraphic.mHatGraphic = getResources().getDrawable(R.drawable.red_hat);
        FaceGraphic.mHairGraphic = null;
        FaceGraphic.mPigNoseGraphic = null;
        FaceGraphic.isPigNoseAllowed = false;
        FaceGraphic.mMustacheGraphic = null;
        FaceGraphic. mHappyStarGraphic = null;
        FaceGraphic. mGlassesGraphic = null;
        FaceGraphic.mPigMaskGraphic = null;

      }
    });


    ImageButton hair = (ImageButton) findViewById(R.id.hair);
    hair.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        findViewById(MASK[typeFace]).setBackgroundResource(R.drawable.round_background);
        typeFace = 2;
        findViewById(MASK[typeFace]).setBackgroundResource(R.drawable.round_background_select);
        FaceGraphic.mHairGraphic = getResources().getDrawable(R.drawable.hair);
        FaceGraphic.mHatGraphic = null;
        FaceGraphic.mPigNoseGraphic = null;
        FaceGraphic.isPigNoseAllowed = false;
        FaceGraphic.mMustacheGraphic = null;
        FaceGraphic. mHappyStarGraphic = null;
        FaceGraphic. mGlassesGraphic = null;
        FaceGraphic.mPigMaskGraphic = null;

      }
    });

    ImageButton op = (ImageButton) findViewById(R.id.op);
    op.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        findViewById(MASK[typeFace]).setBackgroundResource(R.drawable.round_background);
        typeFace = 3;
        findViewById(MASK[typeFace]).setBackgroundResource(R.drawable.round_background_select);

        FaceGraphic.isPigNoseAllowed = false;
        FaceGraphic.mHairGraphic = null;
        FaceGraphic.mHatGraphic = null;
        FaceGraphic.mHappyStarGraphic = null;
        FaceGraphic.mPigNoseGraphic = null;
        FaceGraphic.mGlassesGraphic = getResources().getDrawable(R.drawable.op);
        FaceGraphic.mMustacheGraphic = null;
        //FaceGraphic.mPigMaskGraphic =  getResources().getDrawable(R.drawable.op);

      }
    });

    ImageButton snap = (ImageButton) findViewById(R.id.snap);
    snap.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        findViewById(MASK[typeFace]).setBackgroundResource(R.drawable.round_background);
        typeFace = 4;
        findViewById(MASK[typeFace]).setBackgroundResource(R.drawable.round_background_select);
       // FaceGraphic.mPigNoseGraphic = getResources().getDrawable(R.drawable.snap);
        FaceGraphic.mPigMaskGraphic = null;
        FaceGraphic.mHatGraphic = null;
        FaceGraphic.mHairGraphic = null;
        FaceGraphic.isPigNoseAllowed = false;
        FaceGraphic.mPigNoseGraphic = getResources().getDrawable(R.drawable.snap1);
        FaceGraphic.mHappyStarGraphic = null;
        FaceGraphic.mGlassesGraphic = null;
        FaceGraphic.mMustacheGraphic = null;

      }
    });


    ImageButton glasses4 = (ImageButton) findViewById(R.id.pig_nose);
    glasses4.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        findViewById(MASK[typeFace]).setBackgroundResource(R.drawable.round_background);
        typeFace = 5;
        findViewById(MASK[typeFace]).setBackgroundResource(R.drawable.round_background_select);
        FaceGraphic.isPigNoseAllowed = true;
        FaceGraphic.mHairGraphic = null;
        FaceGraphic.mHatGraphic = null;
        FaceGraphic.mPigNoseGraphic = getResources().getDrawable(R.drawable.pig_nose_emoji);
        FaceGraphic.mHappyStarGraphic = getResources().getDrawable(R.drawable.happy_star);
        FaceGraphic.mPigMaskGraphic = null;
        FaceGraphic.mGlassesGraphic = null;
        FaceGraphic.mMustacheGraphic = null;

      }
    });

    ImageButton glasses5 = (ImageButton) findViewById(R.id.glasses5);
    glasses5.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        findViewById(MASK[typeFace]).setBackgroundResource(R.drawable.round_background);
        typeFace = 6;
        findViewById(MASK[typeFace]).setBackgroundResource(R.drawable.round_background_select);
        FaceGraphic.isPigNoseAllowed = false;
        FaceGraphic.mHairGraphic = null;
        FaceGraphic.mHatGraphic = null;
        FaceGraphic.mPigNoseGraphic = null;
        FaceGraphic.mHappyStarGraphic = null;
        FaceGraphic.mGlassesGraphic = getResources().getDrawable(R.drawable.glasses5);
        FaceGraphic.mPigMaskGraphic = null;
        FaceGraphic.mMustacheGraphic = null;
      }
    });


    ImageButton mask2 = (ImageButton) findViewById(R.id.mask2);
    mask2.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        findViewById(MASK[typeFace]).setBackgroundResource(R.drawable.round_background);
        typeFace = 7;
        findViewById(MASK[typeFace]).setBackgroundResource(R.drawable.round_background_select);
        FaceGraphic.isPigNoseAllowed = false;
        FaceGraphic.mHairGraphic = null;
        FaceGraphic.mHatGraphic = null;
        FaceGraphic.mPigNoseGraphic = null;
        FaceGraphic.mHappyStarGraphic = null;
        //FaceGraphic.mPigMaskGraphic = getResources().getDrawable(R.drawable.mask2);
        FaceGraphic.mMustacheGraphic = null;
        FaceGraphic.mGlassesGraphic = getResources().getDrawable(R.drawable.mask2);



      }
    });

    ImageButton dog = (ImageButton) findViewById(R.id.dog);
    dog.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        findViewById(MASK[typeFace]).setBackgroundResource(R.drawable.round_background);
        typeFace = 8;
        findViewById(MASK[typeFace]).setBackgroundResource(R.drawable.round_background_select);

        FaceGraphic.isPigNoseAllowed = false;
        FaceGraphic.mHairGraphic = null;
        FaceGraphic.mHatGraphic = null;
        FaceGraphic.mHappyStarGraphic = null;
        FaceGraphic.mPigNoseGraphic = getResources().getDrawable(R.drawable.dog2);
        FaceGraphic.mGlassesGraphic = null;
        FaceGraphic.mPigMaskGraphic = null;
        FaceGraphic.mMustacheGraphic = null;
      }
    });

    ImageButton cat2 = (ImageButton) findViewById(R.id.cat2);
    cat2.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        findViewById(MASK[typeFace]).setBackgroundResource(R.drawable.round_background);
        typeFace = 9;
        findViewById(MASK[typeFace]).setBackgroundResource(R.drawable.round_background_select);

        FaceGraphic.isPigNoseAllowed = false;
        FaceGraphic.mHairGraphic = null;
        FaceGraphic.mHatGraphic = null;
        FaceGraphic.mHappyStarGraphic = null;
        FaceGraphic.mPigNoseGraphic = getResources().getDrawable(R.drawable.cat1);
        FaceGraphic.mGlassesGraphic = null;
        FaceGraphic.mPigMaskGraphic = null;
        FaceGraphic.mMustacheGraphic = null;

      }
    });

  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.d(TAG, "onResume called.");

    startCameraSource();
  }

  @Override
  protected void onPause() {
    super.onPause();

    mPreview.stop();
  }

  @Override
  public void onSaveInstanceState(Bundle savedInstanceState) {
    super.onSaveInstanceState(savedInstanceState);
    savedInstanceState.putBoolean("IsFrontFacing", mIsFrontFacing);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
try{
  if (mCameraSource != null) {
    mCameraSource.release();
  }
}catch (NullPointerException e)
{e.printStackTrace();}

  }

  // Handle camera permission requests
  // =================================

  private void requestCameraPermission() {
    Log.w(TAG, "Camera permission not acquired. Requesting permission.");

    final String[] permissions = new String[]{Manifest.permission.CAMERA};
    if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
      Manifest.permission.CAMERA)) {
      ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
      return;
    }

    final Activity thisActivity = this;
    View.OnClickListener listener = new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        ActivityCompat.requestPermissions(thisActivity, permissions, RC_HANDLE_CAMERA_PERM);
      }
    };
    Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
      Snackbar.LENGTH_INDEFINITE)
      .setAction(R.string.ok, listener)
      .show();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    if (requestCode != RC_HANDLE_CAMERA_PERM) {
      Log.d(TAG, "Got unexpected permission result: " + requestCode);
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
      return;
    }

    if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      // We have permission to access the camera, so create the camera source.
      Log.d(TAG, "Camera permission granted - initializing camera source.");
      createCameraSource();
      return;
    }

    // If we've reached this part of the method, it means that the user hasn't granted the app
    // access to the camera. Notify the user and exit.
    Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
      " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));
    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        finish();
      }
    };
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(R.string.app_name)
      .setMessage(R.string.no_camera_permission)
      .setPositiveButton(R.string.disappointed_ok, listener)
      .show();
  }

  // Camera source
  // =============

  private void createCameraSource() {
    Log.d(TAG, "createCameraSource called.");

    Context context = getApplicationContext();
    FaceDetector detector = createFaceDetector(context);

    int facing = CameraSource.CAMERA_FACING_FRONT;
    if (!mIsFrontFacing) {
      facing = CameraSource.CAMERA_FACING_BACK;
    }

    // The camera source is initialized to use either the front or rear facing camera.  We use a
    // relatively low resolution for the camera preview, since this is sufficient for this app
    // and the face detector will run faster at lower camera resolutions.
    //
    // However, note that there is a speed/accuracy trade-off with respect to choosing the
    // camera resolution.  The face detector will run faster with lower camera resolutions,
    // but may miss smaller faces, landmarks, or may not correctly detect eyes open/closed in
    // comparison to using higher camera resolutions.  If you have any of these issues, you may
    // want to increase the resolution.
    mCameraSource = new CameraSource.Builder(context, detector)
      .setFacing(facing)
      .setRequestedPreviewSize(320, 240)
      .setRequestedFps(60.0f)
      .setAutoFocusEnabled(true)
      .build();
  }

  private void startCameraSource() {
    Log.d(TAG, "startCameraSource called.");

    // Make sure that the device has Google Play services available.
    int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
            getApplicationContext());
    if (code != ConnectionResult.SUCCESS) {
      Dialog dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
      dlg.show();
    }

    if (mCameraSource != null) {
      try {
        mPreview.start(mCameraSource, mGraphicOverlay);
      } catch (IOException e) {
        Log.e(TAG, "Unable to start camera source.", e);
        mCameraSource.release();
        mCameraSource = null;
      }
    }
  }

  // Face detector
  // =============

  /**
   *  Create the face detector, and check if it's ready for use.
   */
  @NonNull
  private FaceDetector createFaceDetector(final Context context) {
    Log.d(TAG, "createFaceDetector called.");

    FaceDetector detector = new FaceDetector.Builder(context)
      .setLandmarkType(FaceDetector.ALL_LANDMARKS)
      .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
      .setTrackingEnabled(true)
      .setMode(FaceDetector.FAST_MODE)
      .setProminentFaceOnly(mIsFrontFacing)
      .setMinFaceSize(mIsFrontFacing ? 0.35f : 0.15f)
      .build();

    MultiProcessor.Factory<Face> factory = new MultiProcessor.Factory<Face>() {
      @Override
      public Tracker<Face> create(Face face) {
        return new FaceTracker(mGraphicOverlay, context, mIsFrontFacing);
      }
    };

    Detector.Processor<Face> processor = new MultiProcessor.Builder<>(factory).build();
    detector.setProcessor(processor);

    if (!detector.isOperational()) {
      Log.w(TAG, "Face detector dependencies are not yet available.");

      // Check the device's storage.  If there's little available storage, the native
      // face detection library will not be downloaded, and the app won't work,
      // so notify the user.
      IntentFilter lowStorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
      boolean hasLowStorage = registerReceiver(null, lowStorageFilter) != null;

      if (hasLowStorage) {
        Log.w(TAG, getString(R.string.low_storage_error));
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            finish();
          }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.app_name)
          .setMessage(R.string.low_storage_error)
          .setPositiveButton(R.string.disappointed_ok, listener)
          .show();
      }
    }
    return detector;
  }

}