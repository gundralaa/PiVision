
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.security.auth.NTSid;

import edu.wpi.cscore.VideoSource;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.vision.VisionPipeline;
import edu.wpi.first.vision.VisionThread;

import org.opencv.core.Mat;

import FeedCamera.CameraFeed;
import FeedCamera.Vision;
import Pipelines.GripPipelineReflectiveTape;


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
    
    if (args.length > 0) {
      configFile = args[0];
      System.out.println("Config File");
    }
    
    System.out.println("No Network Tables");

    // start NetworkTables
    NetworkTableInstance ntinst = NetworkTableInstance.getDefault();
    NetworkTable table = ntinst.getTable(tableName);
    
    NetworkTableEntry inside_dist = table.getEntry("inner_dist");
    NetworkTableEntry contoursPresent = table.getEntry("contours_present");
    NetworkTableEntry contourSize = table.getEntry("contour_size");
    NetworkTableEntry centerX = table.getEntry("contour_center_x");

    System.out.println("Setting up NetworkTables client for team " + team);
    ntinst.startClientTeam(team);

    cameraThread = CameraFeed.getInstance();
    System.out.println("Ready Instance");
    Vision vision = Vision.getInstance(cameraThread);
    
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
      double offset = vision.getContourDistanceBox();
      
      contourSize.setDouble(vision.contourSize());
      centerX.setDouble(vision.centerX()); 
      inside_dist.setDouble(offset);
      contoursPresent.setBoolean(vision.contoursPresent()); 
    }
  }
}
