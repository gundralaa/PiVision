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
	public Rect   targetRectangleRight, targetRectangeLeft;
	private GripPipelineReflectiveTape pipeline = new GripPipelineReflectiveTape();
	private double centerX, centerY;
	
	private final double LEFT_ANGLE_THRESHOLD = 0.0;
	private final double RIGHT_ANGLE_THRESHOLD = 0.0;
	private boolean contoursPresent;
	private int contourSize;
	
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

	public double getContourDistanceBox(){
		
		double offset = 0.0;
		double centerXLeft = 0.0, centerXRight = 0.0;
		Mat image = null;
		targetRectangeLeft = null;
		targetRectangleRight = null;

	    image = cameraThread.getCurrentImage();

		pipeline.process(image);
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

		return offset;
	}

	public void getContourTargetAngled(){
		
		Mat image = null;
		RotatedRect rect2 = null, rect1 = null;
		image = cameraThread.getCurrentImage();

		pipeline.process(image);

		int size = pipeline.filterContoursOutput().size();
		
		if( size > 0){
			MatOfPoint2f rect1Points = new MatOfPoint2f(pipeline.filterContoursOutput().get(0).toArray());
			rect1 = Imgproc.minAreaRect(rect1Points);
						
			if(rect1.angle > LEFT_ANGLE_THRESHOLD){
				targetRectangeLeft = Imgproc.boundingRect(pipeline.filterContoursOutput().get(0));
				return;
			}

			
		}

		if(size > 1){
			MatOfPoint2f rect2Points = new MatOfPoint2f(pipeline.filterContoursOutput().get(1).toArray());
			rect2 = Imgproc.minAreaRect(rect2Points);

			if(rect2.angle > LEFT_ANGLE_THRESHOLD){
				targetRectangeLeft = Imgproc.boundingRect(pipeline.filterContoursOutput().get(1));
				targetRectangleRight = Imgproc.boundingRect(pipeline.filterContoursOutput().get(0));
				return;
			}

			if(rect2.angle < RIGHT_ANGLE_THRESHOLD){
				targetRectangeLeft = Imgproc.boundingRect(pipeline.filterContoursOutput().get(0));
				targetRectangleRight = Imgproc.boundingRect(pipeline.filterContoursOutput().get(1));
				return;
			}
			
		}



	}
}
