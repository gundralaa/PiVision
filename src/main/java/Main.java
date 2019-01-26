/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

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

/*
   JSON format:
   {
       "team": <team number>,
       "ntmode": <"client" or "server", "client" if unspecified>
       "cameras": [
           {
               "name": <camera name>
               "path": <path, e.g. "/dev/video0">
               "pixel format": <"MJPEG", "YUYV", etc>   // optional
               "width": <video mode width>              // optional
               "height": <video mode height>            // optional
               "fps": <video mode fps>                  // optional
               "brightness": <percentage brightness>    // optional
               "white balance": <"auto", "hold", value> // optional
               "exposure": <"auto", "hold", value>      // optional
               "properties": [                          // optional
                   {
                       "name": <property name>
                       "value": <property value>
                   }
               ]
           }
       ]
   }
 */

public final class Main {

  private static String configFile = "/boot/frc.json";
  private static CameraFeed cameraThread;
  private static int team = 4450;
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
    NetworkTable table = ntinst.getTable("vision_data");
    NetworkTableEntry inside_dist = table.getEntry("inner_dist");

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
      inside_dist.setDouble(offset);    
    }
  }
}
