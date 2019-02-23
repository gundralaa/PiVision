
import java.util.function.Consumer;

import FeedCamera.CameraFeed;
import FeedCamera.Vision;
import Pipelines.GripPipelineReflectiveTape;
import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.EntryNotification;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;


public final class Main {

  private static String configFile = "/boot/frc.json";
  private static CameraFeed cameraThread;
  private static int team = 4450;
  private static String tableName = "PiVision";
  private static GripPipelineReflectiveTape pipeline = new GripPipelineReflectiveTape();


  /**
   * Main.
   */
  public static void main(String... args) {
    
    // Set config file path
    if (args.length > 0) {
      configFile = args[0];
      System.out.println("Config File");
    }
    
    // Start NetworkTables
    NetworkTableInstance ntinst = NetworkTableInstance.getDefault();
    NetworkTable table = ntinst.getTable(tableName);
    
    // Intialize Every Table Entry
    NetworkTableEntry inside_dist = table.getEntry("inner_dist");
    NetworkTableEntry contoursPresent = table.getEntry("contours_present");
    NetworkTableEntry contourSize = table.getEntry("contour_size");
    NetworkTableEntry centerX = table.getEntry("contour_center_x");
    NetworkTableEntry targetsPresent = table.getEntry("targets_present");

    NetworkTableEntry directAngle = table.getEntry("turn_angle");
    NetworkTableEntry perpAngle = table.getEntry("turn_angle_complex");

    NetworkTableEntry angleOffset = table.getEntry("angle_offset");

    // Camera Switch
    NetworkTableEntry cameraVision = table.getEntry("camera_vision");


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

      vision.findVisionTargets();
      // Run Vision Method
      // Update each vision entry
      
      /**
       * contourSize: the number of closed contours found
       * 
       * centerX: X Value of coordinate that is the center of the
       * two contours in the image coordinate system
       * 
       * inside dist: the distance between the centers
       * of the contours
       * 
       * contoursPresent: determines if both contours
       * are visible by the camera
       */

      contourSize.setDouble(vision.contourSize());
      centerX.setDouble(vision.centerX()); 
      
      inside_dist.setDouble(vision.distBetweenContours());
      contoursPresent.setBoolean(vision.contoursPresent());
      targetsPresent.setString(vision.targetsPresent()); 

      directAngle.setDouble(vision.getDirectAngle());
      perpAngle.setDouble(vision.getPerpAngle());
      angleOffset.setDouble(vision.getAngleOffset());

      if((int)cameraVision.getDouble(0.0) != cameraThread.getCameraType()){
        System.out.println("Changing Camera");
        cameraThread.ChangeCamera((int)cameraVision.getDouble(0.0));
      }
      
      cameraVision.setDouble(cameraThread.getCameraType());
      

    }
  }
}
