package FeedCamera;

import java.util.ArrayList;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import edu.wpi.cscore.VideoSource;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.vision.VisionPipeline;
import edu.wpi.first.vision.VisionThread;

public class Grip {
	
	/**
	 * Interface must be implemented by the
	 * Grip Vision Pipeline that is Auto Generated
	 * By the GRIP Program.
	 */
	public interface GripVisionPipeline extends VisionPipeline 
	{
		public ArrayList<MatOfPoint> findContoursOutput();
		public ArrayList<MatOfPoint> filterContoursOutput();
		
	}

}
