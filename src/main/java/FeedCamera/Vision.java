package FeedCamera;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import org.opencv.calib3d.*;

import Pipelines.GripPipelineReflectiveTape;

public class Vision 
{
	/**
	 * 
	 * 
	 */

	private CameraFeed cameraThread;
	public Rect   targetRectangleRight = null, targetRectangeLeft = null;
	private GripPipelineReflectiveTape pipeline = new GripPipelineReflectiveTape();
	private double centerX, centerY;
	
	private final double LEFT_ANGLE_THRESHOLD = -70;
	private final double RIGHT_ANGLE_THRESHOLD = -10;
	private boolean contoursPresent;
	private int contourSize;

	private double offset = 0.0;
	private double centerXLeft = 0.0, centerXRight = 0.0;
	private Mat image = null;
	

	// Values in CM Coordinates of Both Targets
	private static final MatOfPoint3f worldCoordinatesTarget = new MatOfPoint3f();

	private Mat rvec = new Mat();
	private Mat tvec = new Mat();
	private Mat rMat = new Mat();

	private double directAngle; // Angle Between Ray between Robot and Target
	private double perpAngle; // Angle Between Target and Perpendicular

	private Mat cameraMatrix = new Mat(3, 3, CvType.CV_32FC1);


	private MatOfPoint2f imageCoordinatesTarget = new MatOfPoint2f();
	
	// This variable and method make sure this class is a singleton.
	
	public static Vision vision = null;
	
	public static Vision getInstance(CameraFeed cameraThread) 
	{
		if (vision == null){
			vision = new Vision(cameraThread);
			vision.contourSize = 0;
			vision.contoursPresent = false;
			vision.centerX = 0.0;

			worldCoordinatesTarget.alloc(4);
			
			new Point3(0,0,0); // Center
			new Point3(10,0,0); //Right Target Center
			new Point3(-10, 0, 0); // Left Target Center
			
			worldCoordinatesTarget.put(0, 0, 0, 0, 0); //Center
			worldCoordinatesTarget.put(1, 0, 0, 0, 10); //Right
			worldCoordinatesTarget.put(2, 0, 0, 0, -10); //Left
			worldCoordinatesTarget.put(3, 0, 0, 5, -10);

		}
		
		return vision;
	}
	
	// This is the rest of the class.
	
	private Vision(CameraFeed cameraThread) 
	{
		this.cameraThread = cameraThread;
	}

	public boolean contoursPresent(){
		return contoursPresent;		
	}

	public double contourSize(){
		return contourSize;
	}

	public double centerX(){
		return centerX;
	}

	public double distBetweenContours(){
		return offset;
	}

	public Rect getRightRect(){
		return targetRectangleRight;
	}

	public Rect getLeftRect(){
		return targetRectangeLeft;
	}

	public String targetsPresent(){
		if(contoursPresent){
			return "Both";
		}
		else if(targetRectangeLeft != null){
			return "Left";
		}
		else if(targetRectangleRight != null){
			return "Right";
		}
		else{
			return "None";
		}
	}

	public void findContours(){
		image = cameraThread.getCurrentImage();
		pipeline.process(image);
	}

	public void findVisionTargets(){
		findContours();
		getContourTargetAngled();
		if(targetRectangeLeft != null){
			cameraThread.addRect(targetRectangeLeft);
		}
		if(targetRectangleRight != null){
			cameraThread.addRect(targetRectangleRight);
		}
		contourDistanceBox();
		if(contoursPresent){
			calculateAngles();
		}
	}

	public void findCargo(){

	}

	public void calculateAngles(){
		int leftYCenter = (targetRectangeLeft.y + targetRectangeLeft.height) / 2;
		int rightYCenter = (targetRectangleRight.y + targetRectangleRight.height) / 2;
		
		Point left = new Point(centerXLeft, leftYCenter);
		Point right = new Point(centerXRight, rightYCenter);
		Point center = new Point(centerX, (leftYCenter + rightYCenter)/2);

		imageCoordinatesTarget.alloc(4);

		imageCoordinatesTarget.put(0, 0, center.x, center.y);
		imageCoordinatesTarget.put(1, 0, right.x, right.y);
		imageCoordinatesTarget.put(2, 0, left.x, left.y);
		imageCoordinatesTarget.put(3, 0, targetRectangeLeft.x, targetRectangeLeft.y);

		// Get Camera Matrix (TODO Camera Calibration)
		cameraMatrix.put(0, 0, 1);
		cameraMatrix.put(1, 1, 1);

		Mat intrinsics = Mat.eye(3, 3, CvType.CV_64F);
        intrinsics.put(0, 0, 1);
		intrinsics.put(1, 1, 1);
		
		MatOfDouble distCoff = new MatOfDouble();


		// Get rotation and translation matrix
		Calib3d.solvePnP(worldCoordinatesTarget, imageCoordinatesTarget , intrinsics , distCoff, rvec, tvec);
		Calib3d.Rodrigues(rvec, rMat);

		double x = tvec.get(0, 0)[0];
		double z = tvec.get(2, 0)[0];

		directAngle = Math.atan2(x, z);
		System.out.println("Angle" + directAngle);


	}


	public void getContourTarget(){
		contourSize = pipeline.filterContoursOutput().size();
		
		if(contourSize > 1){
			targetRectangeLeft = Imgproc.boundingRect(pipeline.filterContoursOutput().get(0));
			targetRectangleRight = Imgproc.boundingRect(pipeline.filterContoursOutput().get(1));

			if(targetRectangeLeft.x > targetRectangleRight.x){
				Rect temp = targetRectangeLeft;
				targetRectangeLeft = targetRectangleRight;
				targetRectangleRight = temp;
			}

		}

	}

	public void contourDistanceBox(){

		contoursPresent = (targetRectangeLeft != null && targetRectangleRight != null);

		if(contoursPresent){
			centerXLeft = targetRectangeLeft.x + targetRectangeLeft.width / 2;
			centerXRight = targetRectangleRight.x + targetRectangleRight.width / 2;

			offset = Math.abs((centerXLeft - centerXRight));
			centerX = centerXLeft + (offset/2);
		}
		else {
			offset = 0.0;
			centerX = 0.0;
		}

		if(offset > 10.0){
			System.out.println("Contour Distance: " + offset);
		}
		else {
			System.out.println("No Targets");
		}

	}

	public void getContourTargetAngled(){
		
		Mat image = null;
		RotatedRect rect1 = null;

		int size = pipeline.filterContoursOutput().size();

		if( size > 1){
			getContourTarget();
		}
		else if( size > 0){
			MatOfPoint2f rect1Points = new MatOfPoint2f(pipeline.filterContoursOutput().get(0).toArray());
			rect1 = Imgproc.minAreaRect(rect1Points);
			System.out.println(rect1.angle);
						
			if(rect1.angle < RIGHT_ANGLE_THRESHOLD && rect1.angle > LEFT_ANGLE_THRESHOLD){
				targetRectangleRight = Imgproc.boundingRect(pipeline.filterContoursOutput().get(0));
				targetRectangeLeft = null;
				return;
			}
			else if(rect1.angle < LEFT_ANGLE_THRESHOLD) {
				targetRectangeLeft = Imgproc.boundingRect(pipeline.filterContoursOutput().get(0));
				targetRectangleRight = null;
				return;
			}

			
		}

	}




}
