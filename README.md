# Red_Green_Tethering
CenpA tethering Plugin for ImageJ

INSTALL

    Ensure that the ImageJ version is at least 1.5 and the installation has Java 1.8.0_60 (64bit) installed. If not download the latest version of ImageJ bundled with Java and install it.

    The versions can be checked by opening ImageJ and clicking Help then About ImageJ.

    Download the latest copy of Bio-Formats into the ImageJ plugin directory

    Create a directory in the C: drive called Temp (case sensitive)

    Using notepad save a blank .txt file called Results.txt into the Temp directory you previously created (also case sensitive).

    Place Red_Green_Tether.jar into the plugins directory of your ImageJ installation.

    If everything has worked Red green tethering should be in the Plugins menu.

    Red_green_tethering.java is the editable code for the plugin should improvements or changes be required.
    
USAGE

    You will be prompted to Open Images. The plugin was written for 3 channel 3 dimensional images.

    When the Bio-Formats dialogue opens make sure that the only tick is in Split Channels, nothing else should be ticked.

    Once the images have opened you will be prompted to select each colour channel yourself.
    
    The reference channel (usually the DAPI nucleus channel) is used to select relevant cells. Selection is achieved by clicking on the number in the ROI  Manager

    Once all the cells have been picked the measurements of the spots on the red and green images will be made automatically.

    Results are saved to the text file you should have created in C:\Temp
