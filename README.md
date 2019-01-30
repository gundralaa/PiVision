# PiVision
Separate camera server code. Runs entirely independent on the External Raspberry Pi.

### Web Dashboard
- The Image generate a static IP Dashboard : http:frcvision.local/
- Connect to Robot Network

Allows For:
- Edit Camera Configurations (Brightness, Exposure, Image Size etc)
- View Camera Streams
- Start and Terminate Vision Proccessing Loop

### How To Deploy
- Navigate to the root of the Project
- Run ./gradlew build
- Run ./install with password "raspberry"

### Editing
- The src/main/java contains the main code base
- Main.java contains the main method which performs all the vision proccessing
