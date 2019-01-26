package FeedCamera;

import java.util.ArrayList;

import org.opencv.core.Mat;

import FeedCamera.Grip.GripVisionPipeline;
import FeedCamera.VisionPipelineModifier;
import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.UsbCameraInfo;
import edu.wpi.cscore.VideoMode;
import edu.wpi.cscore.VideoProperty;
import edu.wpi.cscore.VideoSource;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.vision.VisionPipeline;
import edu.wpi.first.vision.VisionThread;

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


/**
 * USB camera feed task. Runs as a thread separate from Robot class.
 * Manages one or more usb cameras feeding their images to the 
 * WpiLib CameraServer class to send to the DS. Creates camera object 
 * for each detected camera and starts them capturing images. We then 
 * loop on a thread getting the current image from the currently selected 
 * camera and pass the image to the camera  * server which passes the 
 * image to the driver station.
 */

public class CameraFeed extends Thread
{
	private VideoSource           currentCamera;
	private int					currentCameraIndex;
	private ArrayList			<VideoSource>cameras = new ArrayList<VideoSource>();
	private Mat 				image = new Mat();
	private static CameraFeed	cameraFeed;
	private boolean				initialized;
	private MjpegServer			mjpegServer;
	private CvSink				imageSource;
	private CvSource			imageOutputStream;
	private boolean				changingCamera;
	private boolean             showContours = true;
	
	private GripVisionPipeline pipeline;
	private VisionPipelineModifier modPipe;
	
	
	// Default Camera settings
	private final int 		imageWidth = 320; 		//640;
	private final int 		imageHeight = 240;		//480;
	//public final double 	fovH = 48.0;
	//public final double 	fovV = 32.0;
	private final double	frameRate = 30;			// frames per second
	//private final int		whitebalance = 4700;	// Color temperature in K
	private final int		brightness = 0;		// 0 - 100
	private int		        exposure = 10;			// 0 - 100

	// Create single instance of this class and return that single instance to any callers.
	
	private static String configFile = "/boot/frc.json";

	@SuppressWarnings("MemberName")
	public static class CameraConfig {
		public String name;
		public String path;
		public JsonObject config;
	}

	public static int team;
	public static boolean server;
	public static List<CameraConfig> cameraConfigs = new ArrayList<>();
	

	/**
	 * Report parse error.
	 */
	public static void parseError(String str) {
		System.err.println("config error in '" + configFile + "': " + str);
	}

	/**
	 * Read single camera configuration.
	 */
	public static boolean readCameraConfig(JsonObject config) {
		CameraConfig cam = new CameraConfig();

		// name
		JsonElement nameElement = config.get("name");
		if (nameElement == null) {
			parseError("could not read camera name");
			return false;
		}
		cam.name = nameElement.getAsString();

		// path
		JsonElement pathElement = config.get("path");
		if (pathElement == null) {
			parseError("camera '" + cam.name + "': could not read path");
			return false;
		}
		cam.path = pathElement.getAsString();

		cam.config = config;

		cameraConfigs.add(cam);
		return true;
	}

	/**
	 * Read configuration file.
	 */
	@SuppressWarnings("PMD.CyclomaticComplexity")
	public static boolean readConfig() {
		// parse file
		JsonElement top;
		try {
			top = new JsonParser().parse(Files.newBufferedReader(Paths.get(configFile)));
		} catch (IOException ex) {
			System.err.println("could not open '" + configFile + "': " + ex);
			return false;
		}

		// top level must be an object
		if (!top.isJsonObject()) {
			parseError("must be JSON object");
			return false;
		}
		JsonObject obj = top.getAsJsonObject();

		// team number
		JsonElement teamElement = obj.get("team");
		if (teamElement == null) {
			parseError("could not read team number");
			return false;
		}
		team = teamElement.getAsInt();

		// ntmode (optional)
		if (obj.has("ntmode")) {
			String str = obj.get("ntmode").getAsString();
			if ("client".equalsIgnoreCase(str)) {
				server = false;
			} else if ("server".equalsIgnoreCase(str)) {
				server = true;
			} else {
				parseError("could not understand ntmode value '" + str + "'");
			}
		}

		// cameras
		JsonElement camerasElement = obj.get("cameras");
		if (camerasElement == null) {
			parseError("could not read cameras");
			return false;
		}
		JsonArray cameras = camerasElement.getAsJsonArray();
		for (JsonElement camera : cameras) {
			if (!readCameraConfig(camera.getAsJsonObject())) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Start running the camera.
	 */
	public static VideoSource startCamera(CameraConfig config) {
		System.out.println("Starting camera '" + config.name + "' on " + config.path);
		VideoSource camera = new UsbCamera(config.name, config.path);

		Gson gson = new GsonBuilder().create();

		camera.setConfigJson(gson.toJson(config.config));

		return camera;
	}


	/**
	 * Get a reference to global CameraFeed object.
	 * @return Reference to global CameraFeed object.
	 */
	  
	public static CameraFeed getInstance() 
	{
		//Util.consoleLog();
		
		if (cameraFeed == null) cameraFeed = new CameraFeed();
	    
	    return cameraFeed;
	}
	
	/**
	 * Set the Vision Pipeline for Image Processing Contour Overlay
	 * For the Global Object
	 * @param The Vision Pipeline Used For Image Processing
	 */
	
	public void setPipeline(GripVisionPipeline pipeline) 
	{
		//Util.consoleLog();
		
		if (cameraFeed != null) 
		{
			cameraFeed.setPipe(pipeline);
			cameraFeed.modPipe = new VisionPipelineModifier(pipeline);
		}
	}
	
	// Private Setter for Pipeline
	
	private void setPipe(GripVisionPipeline pipeline)
	{
		this.pipeline = pipeline;
	}

	public void setShowContours(boolean showContours){
		this.showContours = showContours;
	}

	// Private constructor means callers must use getInstance.
	// This is the singleton class model.
	
	private CameraFeed()
	{

		try
		{
			//Util.consoleLog();
			
			//exposure = (int)(SmartDashboard.getNumber("Exposure", exposure));
    
    		this.setName("CameraFeed");

            // Create Mjpeg stream server.
            
			mjpegServer = CameraServer.getInstance().addServer("4450-RaspiServer", 1181);

            // Create image source.
            
            imageSource = new CvSink("4450-Raspi-CvSink");
            
            // Create output image stream.
            
            imageOutputStream = new CvSource("4450-Raspi-CvSource", VideoMode.PixelFormat.kMJPEG, imageWidth, imageHeight, (int) frameRate);
			//imageOutputStream = new CvSource("4450-CvSource");

			
			mjpegServer.setSource(imageOutputStream);            
            // Create cameras by getting the list of cameras detected by the RoboRio and
            // creating camera objects and storing them in an arraylist so we can switch
			// between them.
			
			// read configuration
			if (!readConfig()) {
				return;
			}

			// start cameras
			for (CameraConfig cameraConfig : cameraConfigs) {
				cameras.add(startCamera(cameraConfig));
			}

            initialized = true;
            
            // Set starting camera.

            ChangeCamera();
		}
		catch (Throwable e) {//Util.logException(e);
		}
	}
	
	/**
	 * Return current camera. May be used to configure camera settings.
	 * @return UsbCamera Current camera, may be null. 
	 */
	public VideoSource getCamera()
	{
		//Util.consoleLog();
		
		return currentCamera;
	}
	
	/**
	 * Return the number of cameras in the internal camera list.
	 * @return The camera count.
	 */
	public int getCameraCount()
	{
		//Util.consoleLog();
		
		if (!initialized) return 0;
		
		if (cameras.isEmpty()) return 0;
		
		return cameras.size();
	}
	/**
	 * Return camera from internal camera list. May be used to configure camera settings.
	 * @param index Camera index in internal camera list (0 based).
	 * @return Requested camera or null.
	 */
	public VideoSource getCamera(int index)
	{
		//Util.consoleLog("%d", index);
		
		if (!initialized) return null;
		
		if (cameras.isEmpty()) return null;
		
		if (index < 0 || index >= cameras.size()) return null;
		
		return cameras.get(index);
	}
	
	/**
	 * Return named camera from internal camera list. May be used to configure camera settings.
	 * @param name Camera name, will be "camN" where N is the device number.
	 * @return Requested camera or null.
	 */
	public VideoSource getCamera(String name)
	{
		VideoSource	camera;
		
		//Util.consoleLog("%s", name);
		
		if (!initialized) return null;
		
		if (cameras.isEmpty()) return null;

		for(int i = 0; i < cameras.size(); ++i) 
		{
			camera = cameras.get(i);

			if (camera.getName().equals(name)) return camera;
		}
		
		return null;
	}

	// Run thread to read and feed camera images. Called by Thread.start().
	
	public void run()
	{
		//Util.consoleLog();
		
		if (!initialized) return;
		
		if (cameras.isEmpty()) return;
		
		try
		{
			while (!isInterrupted())
			{
				if (!changingCamera)
				{
					//int dashExposure = (int)(SmartDashboard.getNumber("Exposure", exposure));
					UpdateCameraImage();
				}
		
				sleep((long)((1 / frameRate) * 1000));
			}
		}
		catch (Throwable e) {
			//Util.logException(e);
		}
	}
	
	/**
	 * Get last image read from camera.
	 * @return Last image from camera.
	 */
	public Mat getCurrentImage()
	{
		//Util.consoleLog();
		
	    synchronized (this) 
	    {
	    	if (image == null)
	    		return null;
	    	else
	    		return image.clone();
	    }
	}
	
	/**
	 * Stop image feed, ie close cameras stop feed thread, release the
	 * singleton cameraFeed object.
	 */
	public void EndFeed()
	{
		if (!initialized) return;

		try
		{
    		//Util.consoleLog();

    		//Thread.currentThread().interrupt();
    		cameraFeed.interrupt();
    		
    		for(int i = 0; i < cameras.size(); ++i) 
    		{
    			currentCamera = cameras.get(i);
    			currentCamera.free();
    		}
    		
    		currentCamera = null;

    		mjpegServer = null;
	
    		cameraFeed = null;
		}
		catch (Throwable e)	{
			//Util.logException(e);
		}

	}
	
	/**
	 * Change the camera to get images from the next camera in the list of cameras.
	 * At end of list loops around to the first. 
	 */
	public void ChangeCamera()
    {
		//Util.consoleLog();
		
		if (!initialized) return;
		
		if (cameras.isEmpty()) return;
		
		changingCamera = true;
		
		if (currentCamera == null)
			currentCamera = cameras.get(currentCameraIndex);
		else
		{
			currentCameraIndex++;
			
			if (currentCameraIndex == cameras.size()) currentCameraIndex = 0;
			
			currentCamera = cameras.get(currentCameraIndex);
		}
		
		//Util.consoleLog("current=(%d) %s", currentCameraIndex, currentCamera.getName());
		
	    synchronized (this) 
	    {
	    	imageSource.setSource(currentCamera);
	    }
	    
	    changingCamera = false;
	    
	    //Util.consoleLog("end");
    }
    
	/**
	 * Change current camera to specific camera.
	 * @param camera Usb camera object.
	 */
	public void changeCamera(VideoSource camera)
	{
		//Util.consoleLog("%s", camera.getName());
		
		if (!initialized) return;
		
		if (cameras.isEmpty()) return;
		
		changingCamera = true;
		
		currentCamera = camera;
		
	    synchronized (this) 
	    {
	    	imageSource.setSource(camera);
	    }
	    
	    changingCamera = false;
	    
	    //Util.consoleLog("end");
	}
	
	// Get an image from current camera and give it to the server.
    
	private void UpdateCameraImage()
    {
		long	result;
		
		try
		{
			if (currentCamera != null)
			{	
			    synchronized (this) 
			    {
					result = imageSource.grabFrame(image);
					if(showContours){
						modPipe.process(image);
						//Util.consoleLog("Output: %d", modPipe.filterContoursOutput().size());
					}
			    }
			    
			    if (result != 0){
					if(showContours){
						imageOutputStream.putFrame(modPipe.getModImage());
					}
					else {
						imageOutputStream.putFrame(image);
					}
				} 
			}
		}
		catch (Throwable e)	{
			//Util.logException(e);
		}
    }
	

	
}
