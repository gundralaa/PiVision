import FeedCamera.CameraFeed;
import FeedCamera.Vision;
import Pipelines.GripPipelineReflectiveTape;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import java.util.ArrayList;
import edu.wpi.cscore.VideoSource;


public final class Main {

  private static String configFile = "/boot/frc.json";
  private static CameraFeed cameraThread;
  private static int team = 4450;
  private static String tableName = "PiVision";
  private static GripPipelineReflectiveTape pipeline = new GripPipelineReflectiveTape();
  private static ArrayList			<VideoSource>cameras = new ArrayList<VideoSource>();
  public static Main main = new Main();

 ///main = new main();

  /**
   * Main.
   */
  public static void main(String... args) {
    // Set config file path
    if (args.length > 0) {
      configFile = args[0];
      System.out.println("Config File");
     main.ChangeCamera();
    }
    
    // Start NetworkTables
    NetworkTableInstance ntinst = NetworkTableInstance.getDefault();
    NetworkTable table = ntinst.getTable(tableName);
    
    // Intialize Every Table Entry
    NetworkTableEntry inside_dist = table.getEntry("inner_dist");
    NetworkTableEntry contoursPresent = table.getEntry("contours_present");
    NetworkTableEntry contourSize = table.getEntry("contour_size");
    NetworkTableEntry centerX = table.getEntry("contour_center_x");

    //NetworkTableEntry cameraVision = table.getEntry("camera_vision");
	  //NetworkTableEntry cameraDriver = table.getEntry("camera_driver");

    System.out.println("Setting up NetworkTables client for team " + team);
    ntinst.startClientTeam(team);

    // Create camera thread instance
    cameraThread = CameraFeed.getInstance();
    System.out.println("Ready Instance");

    // Create Vision Object Instance
    Vision vision = Vision.getInstance(cameraThread);
    
    // Set GRIP Pipeline and Start Thread
    cameraThread.setPipeline(pipeline);
    cameraThread.setShowContours(false);
    cameraThread.start();

    //Boolean Target Visible
    //X and Y of Rectangles
    //X and Y Contour Center Coordinates


    // loop forever
    for (;;) {
      try {
        Thread.sleep(500);
      } catch (InterruptedException ex) {
        return;
      }
      // Run Vision Method
      double offset = vision.getContourDistanceBox();
      // Update each vision entry
      
      /**
       * contourSize: the number of closed contours found
       * 
       * centerX: X Value of coordinate that is the center of the
       * two contours in the image coordinate system
       * 
       * inside dist: the distance be3tween the centers
       * of the contours
       * 
       * contoursPresent: determines if both contours
       * are visible by the camera
       */

      contourSize.setDouble(vision.contourSize());
      centerX.setDouble(vision.centerX()); 
      inside_dist.setDouble(offset);
      contoursPresent.setBoolean(vision.contoursPresent()); 
    }

  }


    public void setCameraType(String cameraType) {
      NetworkTableInstance ntinst = NetworkTableInstance.getDefault();
      NetworkTable table = ntinst.getTable(tableName);
      NetworkTableEntry cameraVision = table.getEntry("camera_vision");
      NetworkTableEntry cameraDriver = table.getEntry("camera_driver");

      if (cameraType.equals("Vision")) {
        cameraVision.setString(cameraType);
      }

      if (cameraType.equals("Driver")) {
        cameraVision.setString(cameraType);
      }
      
	    
      
    }
    public void ChangeCamera()
    {
		String cameraType;
		
		if (!cameraThread.initialized) return;
		
		if (cameras.isEmpty()) return;
		
		cameraThread.changingCamera = true;
		
		if (cameraThread.currentCamera == null) {
			cameraThread.currentCamera = cameras.get(cameraThread.currentCameraIndex);
    }
    
		if (cameraThread.currentCameraIndex == 0) {
			cameraType = "Vision";
			setCameraType(cameraType);
		}

		if (cameraThread.currentCameraIndex == 1) {
			cameraType = "Driver";
			setCameraType(cameraType);
		}
			
			
		
	
		//Util.consoleLog("current=(%d) %s", currentCameraIndex, currentCamera.getName());
		
	    synchronized (this) 
	    {
			cameraThread.imageSource.setSource(cameraThread.currentCamera);
	    }
	    
	    cameraThread.changingCamera = false;
	    
	    //Util.consoleLog("end");
    }
}