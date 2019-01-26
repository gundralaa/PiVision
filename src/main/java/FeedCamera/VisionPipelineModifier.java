package FeedCamera;

import java.util.ArrayList;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import FeedCamera.Grip.GripVisionPipeline;

public class VisionPipelineModifier implements GripVisionPipeline 
{
	/**
	 * A class that uses the decorator class model
	 * to implement the draw contours method on
	 * the vision pipeline for the camera output.
	 */
	
	GripVisionPipeline pipeline; //The modified vision pipeline
	Mat contourImage; //Image With The Contours
	
	// Initialization for Pipeline Modifier
	public VisionPipelineModifier(GripVisionPipeline pipeline) {
		this.pipeline = pipeline;
		this.contourImage = new Mat();	
	}

	@Override
	public void process(Mat image) {
		pipeline.process(image);
		
		// Draw Contours Block
		contourImage = image;
        Imgproc.drawContours(contourImage, pipeline.filterContoursOutput(), -1, new Scalar(0, 255, 0), 2);
	}

	@Override
	public ArrayList<MatOfPoint> findContoursOutput() {
		return pipeline.findContoursOutput();
	}

	public ArrayList<MatOfPoint> filterContoursOutput(){
		return pipeline.filterContoursOutput();
	}
	
	/**
	 * Method to retrieve the drawn image
	 * @return
	 */
	public Mat getModImage() {
		return contourImage;
	}
	
}
