package FeedCamera;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.imgproc.Imgproc;

import Pipelines.GripPipelineReflectiveTape;

public class Vision 
{
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
	
	// This variable and method make sure this class is a singleton.
	
	public static Vision vision = null;
	
	public static Vision getInstance(CameraFeed cameraThread) 
	{
		if (vision == null){
			vision = new Vision(cameraThread);
			vision.contourSize = 0;
			vision.contoursPresent = false;
			vision.centerX = 0.0;
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
	}

	public void findCargo(){

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
