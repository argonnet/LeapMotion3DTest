package leapmotion3dtest.leapmotion.gestures;

import com.leapmotion.leap.Hand;
import com.leapmotion.leap.Vector;
import leapmotion3dtest.leapmotion.gestures.information.SwipeGestureInformation;
import org.apache.commons.math.stat.regression.SimpleRegression;

import java.io.Console;

/**
 * This class detect swipe and inform her client.
 *
 * Description
 * -----------
 *
 * The swipe detection is base on several parameter :
 * - The length of the gesture, measured in frame number,
 * - The minimum velocity on the X axis to detect frame,
 * - The maximum velocity to reach to validate the gesture,
 * - The min time between gesture,
 * - The slope of the gesture with  the X axis,
 * - The R coefficient of the the linear regression done on the gesture (we only take care of X and Y axis)
 *
 * All these parameter are checked to detect the gesture.
 *
 * /!\ Only work for on hand /!\
 * */
public class SwipeGestureDetector extends BaseGestureDetector implements IGestureDetector {

    //region Enum / Constants / Variables

    private final static int GESTURE_LENGTH = 100; //millimeters
    private final static int FRAME_MAX_GESTURE_LENGTH = 50;

    private final static int MIN_GESTURE_VELOCITY_X_FRAME_DECTECTION = 750;
    private final static int MAX_GESTURE_VELOCITY_X_VALIDATION = 1200;

    private final static long MIN_DELAY_BETWEEN_GESTURE_IN_MILLIS = 1000;

    private final static double MIN_R = 0.5;
    private final static double MIN_SLOPE  = 0.5;

    private Vector firstPointOfGesture;
    private long lastGestureDetectedInMillis;
    private int frameGestureCount;
    private double xVelocityMax;
    private double xVelocityMin;
    private Side currentGestureDirection;

    private SimpleRegression regression;


    //endregion

    /**
     * Constructor in which we need to precise which end to detect
     * @param handSide Hand to detect (right or left one)
     */
    public SwipeGestureDetector(Side handSide){
        super(handSide);

        regression = new SimpleRegression();

        xVelocityMax = 0;
        xVelocityMin = Double.MAX_VALUE;
        lastGestureDetectedInMillis = 0;
    }

    //region Constructor


    //endregion

    //region Methods

    @Override
    protected void onFrameRegistered(Hand selectedHand){


            //Gesture detection start here ...

            //We only detect frame with a minimum of velocitiy in the palm gesture.
            if(selectedHand.isValid() && Math.abs(selectedHand.palmVelocity().getX()) >= MIN_GESTURE_VELOCITY_X_FRAME_DECTECTION) {

                //We keep the departure point for the gesture
                if(frameGestureCount == 0) firstPointOfGesture = selectedHand.stabilizedPalmPosition();


                frameGestureCount++;

                //Determine the direction of the initiated gesture
                currentGestureDirection = selectedHand.palmVelocity().getX() > 0 ? Side.Right : Side.Left;

                regression.addData(selectedHand.stabilizedPalmPosition().getX(),
                                   selectedHand.stabilizedPalmPosition().getY());

                //Checking max velocity on the gesture
                //Abs use for compatibility for both gesture (right and left)
                xVelocityMax =  Math.max(Math.abs(selectedHand.palmVelocity().getX()),xVelocityMax);
                xVelocityMin =  Math.min(Math.abs(selectedHand.palmVelocity().getX()),xVelocityMin);

                //Calc the distance from the first point of the gesture
                double distance = Math.sqrt(
                        Math.pow(selectedHand.stabilizedPalmPosition().getX() - firstPointOfGesture.getX(),2) +
                        Math.pow(selectedHand.stabilizedPalmPosition().getY() - firstPointOfGesture.getY(),2));


                System.out.print("\nDistance : " + distance + " - xVelocityMax : "  + xVelocityMax  + " -  delay : " +
                        (System.currentTimeMillis() - lastGestureDetectedInMillis)  + " - R : " + regression.getR() +
                        " - Slope : " + regression.getSlope());


                if(distance >= GESTURE_LENGTH && //We reach the end of the gesture
                   xVelocityMax >= MAX_GESTURE_VELOCITY_X_VALIDATION && //The max velocity is OK
                   System.currentTimeMillis() - lastGestureDetectedInMillis > MIN_DELAY_BETWEEN_GESTURE_IN_MILLIS && //To prevent long gesture launch several Swipe event
                   Math.abs(regression.getR()) >= MIN_R &&
                   (-1 * MIN_SLOPE <= regression.getSlope() && regression.getSlope() <= MIN_SLOPE)){

                System.out.print(" !! DETECTION !!");

                    //fire the event
                    listeners.forEach(l -> l.gestureDetected(new SwipeGestureInformation(
                            currentGestureDirection, xVelocityMin, xVelocityMax, distance)));

                    //Register time the gesture was detected
                    lastGestureDetectedInMillis = System.currentTimeMillis();

                }


                //If the gesture change the direction it started with we clear it.
                if ((currentGestureDirection == Side.Right && selectedHand.palmVelocity().getX() < 0) ||
                    (currentGestureDirection == Side.Left && selectedHand.palmVelocity().getX() > 0) ||
                     frameGestureCount >= FRAME_MAX_GESTURE_LENGTH ||
                     distance >= GESTURE_LENGTH ) { //The gesture ended we have to check several things

                    regression.clear();

                    frameGestureCount = 0;
                    xVelocityMax = 0;
                    xVelocityMin = Double.MAX_VALUE;


                }

            }
        }

        //endregion


        //region Getter / Setter

        //endregion

}
