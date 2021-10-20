
package com.kamranullah.facespotter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;

import com.kamranullah.facespotter.ui.camera.GraphicOverlay;

class FaceGraphic extends GraphicOverlay.Graphic {

  private static final String TAG = "FaceGraphic";

  private static final float DOT_RADIUS = 3.0f;
  private static final float TEXT_OFFSET_Y = -30.0f;

  private boolean mIsFrontFacing;

  private volatile FaceData mFaceData;

  private Paint mHintTextPaint;
  private Paint mHintOutlinePaint;
  private Paint mEyeWhitePaint;
  private Paint mIrisPaint;
  private Paint mEyeOutlinePaint;
  private Paint mEyelidPaint;

  public static boolean isPigNoseAllowed;

  public static Drawable mPigNoseGraphic;
  public static Drawable mMustacheGraphic;
  public static Drawable mHappyStarGraphic;
  public static Drawable mHatGraphic;
  public static Drawable mHairGraphic;
  public static Drawable mGlassesGraphic;
  public static Drawable mPigMaskGraphic;

  // We want each iris to move independently,
  // so each one gets its own physics engine.
  private EyePhysics mLeftPhysics = new EyePhysics();
  private EyePhysics mRightPhysics = new EyePhysics();


  FaceGraphic(GraphicOverlay overlay, Context context, boolean isFrontFacing) {
    super(overlay);
    mIsFrontFacing = isFrontFacing;
    Resources resources = context.getResources();
    initializePaints(resources);
  }

  private void initializePaints(Resources resources) {
    mHintTextPaint = new Paint();
    mHintTextPaint.setColor(resources.getColor(R.color.overlayHint));
    mHintTextPaint.setTextSize(resources.getDimension(R.dimen.textSize));

    mHintOutlinePaint = new Paint();
    mHintOutlinePaint.setColor(resources.getColor(R.color.overlayHint));
    mHintOutlinePaint.setStyle(Paint.Style.STROKE);
    mHintOutlinePaint.setStrokeWidth(resources.getDimension(R.dimen.hintStroke));

    mEyeWhitePaint = new Paint();
    mEyeWhitePaint.setColor(resources.getColor(R.color.eyeWhite));
    mEyeWhitePaint.setStyle(Paint.Style.FILL);

    mIrisPaint = new Paint();
    mIrisPaint.setColor(resources.getColor(R.color.iris));
    mIrisPaint.setStyle(Paint.Style.FILL);

    mEyeOutlinePaint = new Paint();
    mEyeOutlinePaint.setColor(resources.getColor(R.color.eyeOutline));
    mEyeOutlinePaint.setStyle(Paint.Style.STROKE);
    mEyeOutlinePaint.setStrokeWidth(resources.getDimension(R.dimen.eyeOutlineStroke));

    mEyelidPaint = new Paint();
    mEyelidPaint.setColor(resources.getColor(R.color.eyelid));
    mEyelidPaint.setStyle(Paint.Style.FILL);
  }

  /**
   *  Update the face instance based on detection from the most recent frame.
   */
  void update(FaceData faceData) {
    mFaceData = faceData;
    postInvalidate(); // Trigger a redraw of the graphic (i.e. cause draw() to be called).
  }

  @Override
  public void draw(Canvas canvas) {
    // Confirm that the face data is still available
    // before using it.
    FaceData faceData = mFaceData;
    if (faceData == null) {
      return;
    }
    PointF detectPosition = faceData.getPosition();
    PointF detectLeftEyePosition = faceData.getLeftEyePosition();
    PointF detectRightEyePosition = faceData.getRightEyePosition();
    PointF detectNoseBasePosition = faceData.getNoseBasePosition();
    PointF detectMouthLeftPosition = faceData.getMouthLeftPosition();
    PointF detectMouthBottomPosition = faceData.getMouthBottomPosition();
    PointF detectMouthRightPosition = faceData.getMouthRightPosition();
    {
      if ((detectPosition == null) ||
        (detectLeftEyePosition == null) ||
        (detectRightEyePosition == null) ||
        (detectNoseBasePosition == null) ||
        (detectMouthLeftPosition == null) ||
        (detectMouthBottomPosition == null) ||
        (detectMouthRightPosition == null)) {
        return;
      }
    }

    // If we've made it this far, it means that the face data *is* available.
    // It's time to translate camera coordinates to view coordinates.

    // Face position, dimensions, and angle
    PointF position = new PointF(translateX(detectPosition.x),
                                 translateY(detectPosition.y));
    float width = scaleX(faceData.getWidth());
    float height = scaleY(faceData.getHeight());

    // Eye coordinates
    PointF leftEyePosition = new PointF(translateX(detectLeftEyePosition.x),
                                        translateY(detectLeftEyePosition.y));
    PointF rightEyePosition = new PointF(translateX(detectRightEyePosition.x),
                                         translateY(detectRightEyePosition.y));

    // Eye state
    boolean leftEyeOpen = faceData.isLeftEyeOpen();
    boolean rightEyeOpen = faceData.isRightEyeOpen();

    // Nose coordinates
    PointF noseBasePosition = new PointF(translateX(detectNoseBasePosition.x),
      translateY(detectNoseBasePosition.y));

    // Mouth coordinates
    PointF mouthLeftPosition = new PointF(translateX(detectMouthLeftPosition.x),
      translateY(detectMouthLeftPosition.y));
    PointF mouthRightPosition = new PointF(translateX(detectMouthRightPosition.x),
      translateY(detectMouthRightPosition.y));
    PointF mouthBottomPosition = new PointF(translateX(detectMouthBottomPosition.x),
      translateY(detectMouthBottomPosition.y));

    // Smile state
    boolean smiling = faceData.isSmiling();

    // Head tilt
    float eulerY = faceData.getEulerY();
    float eulerZ = faceData.getEulerZ();

    // Calculate the distance between the eyes using Pythagoras' formula,
    // and we'll use that distance to set the size of the eyes and irises.
    final float EYE_RADIUS_PROPORTION = 0.45f;
    final float IRIS_RADIUS_PROPORTION = EYE_RADIUS_PROPORTION / 2.0f;
    float distance = (float) Math.sqrt(
            (rightEyePosition.x - leftEyePosition.x) * (rightEyePosition.x - leftEyePosition.x) +
            (rightEyePosition.y - leftEyePosition.y) * (rightEyePosition.y - leftEyePosition.y));
    float eyeRadius = EYE_RADIUS_PROPORTION * distance;
    float irisRadius = IRIS_RADIUS_PROPORTION * distance;


    PointF leftIrisPosition = mLeftPhysics.nextIrisPosition(leftEyePosition, eyeRadius, irisRadius);

    if (isPigNoseAllowed)
    {

      // Draw the eyes.
      //PointF leftIrisPosition = mLeftPhysics.nextIrisPosition(leftEyePosition, eyeRadius, irisRadius);
      drawEye(canvas, leftEyePosition, eyeRadius, leftIrisPosition, irisRadius, leftEyeOpen, smiling);
      PointF rightIrisPosition = mRightPhysics.nextIrisPosition(rightEyePosition, eyeRadius, irisRadius);
      drawEye(canvas, rightEyePosition, eyeRadius, rightIrisPosition, irisRadius, rightEyeOpen, smiling);

      try {
        // Draw the nose.
        drawPigNose(canvas, noseBasePosition, leftEyePosition, rightEyePosition, width);

      }catch (NullPointerException e){e.printStackTrace();}

    }else
      {
        try {
          // Draw the nose.
          drawNose(canvas, noseBasePosition, leftEyePosition, rightEyePosition, width);
        }catch (NullPointerException e){e.printStackTrace();}
      }





    try {

      // Draw the glasses.
      Drawglasses(canvas, noseBasePosition, leftEyePosition, rightEyePosition, width);

    }catch (NullPointerException e){e.printStackTrace();}

    try {
      drawMask(canvas, noseBasePosition, leftEyePosition, rightEyePosition, width);
    }catch (NullPointerException e){e.printStackTrace();}

    // Draw the mustache.
    //drawMustache(canvas, noseBasePosition, mouthLeftPosition, mouthRightPosition);

    try {
      drawHair(canvas, position, width, height, noseBasePosition);
    }catch (NullPointerException e){e.printStackTrace();}
    // Draw the hat only if the subject's head is titled at a
    // sufficiently jaunty angle.
try {
  drawHat(canvas, position, width, height, noseBasePosition);
}catch (NullPointerException e){e.printStackTrace();}

    /*final float HEAD_TILT_HAT_THRESHOLD = 20.0f;
    if (Math.abs(eulerZ) > HEAD_TILT_HAT_THRESHOLD) {
      drawHat(canvas, position, width, height, noseBasePosition);
    }*/

  }

  // Cartoon feature draw routines
  // =============================

  private void drawEye(Canvas canvas,
                       PointF eyePosition, float eyeRadius,
                       PointF irisPosition, float irisRadius,
                       boolean eyeOpen, boolean smiling) {
    if (eyeOpen) {
      canvas.drawCircle(eyePosition.x, eyePosition.y, eyeRadius, mEyeWhitePaint);
      if (smiling) {
        mHappyStarGraphic.setBounds(
                (int)(irisPosition.x - irisRadius),
                (int)(irisPosition.y - irisRadius),
                (int)(irisPosition.x + irisRadius),
                (int)(irisPosition.y + irisRadius));
        mHappyStarGraphic.draw(canvas);
      } else {
        canvas.drawCircle(irisPosition.x, irisPosition.y, irisRadius, mIrisPaint);
      }
    } else {
      canvas.drawCircle(eyePosition.x, eyePosition.y, eyeRadius, mEyelidPaint);
      float y = eyePosition.y;
      float start = eyePosition.x - eyeRadius;
      float end = eyePosition.x + eyeRadius;
      canvas.drawLine(start, y, end, y, mEyeOutlinePaint);
    }
    canvas.drawCircle(eyePosition.x, eyePosition.y, eyeRadius, mEyeOutlinePaint);
  }

  private void Drawglasses(Canvas canvas,
                        PointF noseBasePosition,
                        PointF leftEyePosition, PointF rightEyePosition,
                        float faceWidth) {

    final float NOSE_FACE_WIDTH_RATIO = (float)(4 / 4.5);
    float noseWidth = faceWidth * NOSE_FACE_WIDTH_RATIO;
    int left = (int)(noseBasePosition.x - (noseWidth / 2));
    int right = (int)(noseBasePosition.x + (noseWidth / 2));
    int top = (int)(leftEyePosition.y + rightEyePosition.y) / 4;
    int bottom = (int) ((int)noseBasePosition.y * 1.2f);

    mGlassesGraphic.setBounds(left, top, right, bottom);
    mGlassesGraphic.draw(canvas);
  }

  private void drawNose(Canvas canvas,
                        PointF noseBasePosition,
                        PointF leftEyePosition, PointF rightEyePosition,
                        float faceWidth) {

    final float NOSE_FACE_WIDTH_RATIO = (float)(4 / 4.5);
    float noseWidth = faceWidth * NOSE_FACE_WIDTH_RATIO;
    int left = (int)(noseBasePosition.x - (noseWidth / 4));
    int right = (int)(noseBasePosition.x + (noseWidth / 4));
    int top = (int)(leftEyePosition.y + rightEyePosition.y) / 4;
    int bottom = (int) ((int)noseBasePosition.y * 1.2f);

    mPigNoseGraphic.setBounds(left, top, right, bottom);
    mPigNoseGraphic.draw(canvas);
  }

  private void drawMask(Canvas canvas,
                        PointF noseBasePosition,
                        PointF leftEyePosition, PointF rightEyePosition,
                        float faceWidth) {
    final float NOSE_FACE_WIDTH_RATIO = (float)(1 / 2.2);
    float noseWidth = faceWidth * NOSE_FACE_WIDTH_RATIO;
    int left = (int)(noseBasePosition.x - (noseWidth)) / 2;
    int right = (int)(noseBasePosition.x + (noseWidth)) / 2;
    int top = (int)(leftEyePosition.y + rightEyePosition.y) * 2;
    int bottom = (int)noseBasePosition.y * 2;

    mPigMaskGraphic.setBounds(left, top, right, bottom);
    mPigMaskGraphic.draw(canvas);
  }


  private void drawPigNose(Canvas canvas,
                        PointF noseBasePosition,
                        PointF leftEyePosition, PointF rightEyePosition,
                        float faceWidth) {
    final float NOSE_FACE_WIDTH_RATIO = (float)(1 / 1.5);
    float noseWidth = faceWidth * NOSE_FACE_WIDTH_RATIO;
    int left = (int)(noseBasePosition.x - (noseWidth / 4));
    int right = (int)(noseBasePosition.x + (noseWidth / 4));
    int top = (int) (leftEyePosition.y + rightEyePosition.y) / 2;
    int bottom = (int)((int)noseBasePosition.y * 1.1f) + 150;

    mPigNoseGraphic.setBounds(left, top, right, bottom);
    mPigNoseGraphic.draw(canvas);
  }




  private void drawMustache(Canvas canvas,
                            PointF noseBasePosition,
                            PointF mouthLeftPosition, PointF mouthRightPosition) {
    int left = (int)mouthLeftPosition.x;
    int top = (int)noseBasePosition.y;
    int right = (int)mouthRightPosition.x;
    int bottom = (int)Math.min(mouthLeftPosition.y, mouthRightPosition.y);


    if (mIsFrontFacing) {
      mMustacheGraphic.setBounds(left, top, right, bottom);
    } else {
      mMustacheGraphic.setBounds(right, top, left, bottom);
    }
    mMustacheGraphic.draw(canvas);
  }

  private void drawHair(Canvas canvas, PointF facePosition, float faceWidth, float faceHeight, PointF noseBasePosition) {

    final float HAT_FACE_WIDTH_RATIO = (float)(1.0 / 1.5);
    final float HAT_FACE_HEIGHT_RATIO = (float)(1.0 / 1.5);
    final float HAT_CENTER_Y_OFFSET_FACTOR = (float)(1.0 / 1.5);

    float hatCenterY = facePosition.y + (faceHeight * HAT_CENTER_Y_OFFSET_FACTOR);
    float hatWidth = faceWidth * HAT_FACE_WIDTH_RATIO;
    float hatHeight = faceHeight * HAT_FACE_HEIGHT_RATIO;

    int left = (int)(noseBasePosition.x - (hatWidth / 1.5));
    int right = (int)(noseBasePosition.x + (hatWidth / 1.5));
    int top = (int)(hatCenterY - (hatHeight)) -250;
    int bottom = (int)(hatCenterY + (hatHeight / 1.2));
   // Log.d(TAG, "drawHair: "+ bottom);
    mHairGraphic.setBounds(left, top, right, bottom);
    mHairGraphic.draw(canvas);
  }

  private void drawHat(Canvas canvas, PointF facePosition, float faceWidth, float faceHeight, PointF noseBasePosition) {

    final float HAT_FACE_WIDTH_RATIO = (float)(1.0 / 1.2);
    final float HAT_FACE_HEIGHT_RATIO = (float)(1.0 / 1.2);
    final float HAT_CENTER_Y_OFFSET_FACTOR = (float)(1.0 / 1.2);

    float hatCenterY = facePosition.y + (faceHeight * HAT_CENTER_Y_OFFSET_FACTOR);
    float hatWidth = faceWidth * HAT_FACE_WIDTH_RATIO;
    float hatHeight = faceHeight * HAT_FACE_HEIGHT_RATIO;

    int left = (int)(noseBasePosition.x - (hatWidth / 1.5));
    int right = (int)(noseBasePosition.x + (hatWidth / 1.5));
    int top = (int) ((int)(hatCenterY - (hatHeight )) /2.8f);
    int bottom = (int) ((int)(hatCenterY + (hatHeight)) / 2.8f);
    //Log.d(TAG, "drawHat: "+ bottom);
    mHatGraphic.setBounds(left, top, right, bottom);
    mHatGraphic.draw(canvas);
  }

}