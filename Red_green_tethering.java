/*
 * ImageJ plugin to measure the green labelled CenpA
 * spots and the red labelled HJURP, BP1 spots.
 * 
 * Author: David Kelly
 * 
 * Date: April 2019
 * 
 * Version 1.0
 * 
 * Usage: Opens a 3 channel image and Z projects each channel,
 * user selects which channel is which. The blue (DAPI) labelled
 * channel is used as reference to select the cells of interest.
 * User is asked to click on a cell of interest and the plugin 
 * measures the green and red spots within the area of the selected
 * cell. The mean intensity, integrated density and spot area are
 * output to a text file for use in Excel, R or Graphpad.
 * 
 */



import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.WaitForUserDialog;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.ZProjector;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JOptionPane;

public class Red_green_tethering implements PlugIn{
	String filename;
	ImagePlus BlueImage;
	int BlueImageID;
	ImagePlus GreenImage;
	int GreenImageID;
	ImagePlus RedImage;
	int RedImageID;
	int RoiIndexPos;
	String RegionName;
	
	public void run(String arg) {
		
		//Set measurements for the plugin
		IJ.run("Set Measurements...", "area mean min centroid integrated redirect=None decimal=2");
		//Open the image and split channels, requires Bio-Formats plugin to be installed.
		new WaitForUserDialog("Open Image", "Open Images. SPLIT CHANNELS!").show();
		IJ.run("Bio-Formats Importer");
		
		//Get image name for saving
		ImagePlus imp = WindowManager.getCurrentImage();
		filename = imp.getTitle();//Get file name
		
		//Ask user to select Blue image
		new WaitForUserDialog("Select" , "Select Blue Image").show();
		ImagePlus ImTemp = WindowManager.getCurrentImage();
		Zproject();//Z project the image
		BlueImage = WindowManager.getCurrentImage();
		BlueImageID = BlueImage.getID();
		IJ.run(BlueImage, "Enhance Contrast", "saturated=0.35"); //Autoscale image
		ImTemp.changes = false;
		ImTemp.close();
		
		//Ask user to select Green image
		new WaitForUserDialog("Select" , "Select Green Image").show();
		ImTemp = WindowManager.getCurrentImage();
		Zproject();//Z project the image
		GreenImage = WindowManager.getCurrentImage();
		GreenImageID = GreenImage.getID();
		IJ.run(GreenImage, "Enhance Contrast", "saturated=0.35"); //Autoscale image
		ImTemp.changes = false;
		ImTemp.close();
		
		//Ask user to select Red image
		new WaitForUserDialog("Select" , "Select Red Image").show();
		ImTemp = WindowManager.getCurrentImage();
		Zproject();//Z project the image
		RedImage = WindowManager.getCurrentImage();
		RedImageID = RedImage.getID();
		IJ.run(RedImage, "Enhance Contrast", "saturated=0.35"); //Autoscale image
		ImTemp.changes = false;
		ImTemp.close();
		
		int SelectedCells [] = new int [50];
		SelectedCells = SelectReferenceCells();  //Calls the function which allows user to select cells
		GreenApply(SelectedCells); //All the selected cells id numbers are passed to function to measure green 
		IJ.run("Close All", "");
		new WaitForUserDialog("Finished" , "Plugin Has Finished").show();
	}
	
	/*
	 * Function to z project each image stack in the order that the
	 * user selects them
	 */
	private void Zproject(){
		ImagePlus TheImage;
		ImagePlus temp = WindowManager.getCurrentImage();
	    int tempID = temp.getID();
	    int theframes = temp.getNSlices();
	   
	    IJ.selectWindow(tempID);
	    TheImage = WindowManager.getCurrentImage();
	    ZProjector ThisZproject = new ZProjector(TheImage);
	    ThisZproject.setMethod(ZProjector.MAX_METHOD);
	    ThisZproject.setStartSlice(0);
	    ThisZproject.setStopSlice(theframes);
	    ThisZproject.doProjection();
	    ImagePlus tempWindow = ThisZproject.getProjection();
	    tempWindow.show();
	       
	    
	}
	
	/*
	 * Function allows user to select each highlighted cell and
	 * adds its ID number to an array for use later
	 */
	private int [] SelectReferenceCells(){
		IJ.selectWindow(BlueImageID);
		IJ.setAutoThreshold(BlueImage, "Default dark");
		
		//Make Sure ROI Manager is cleared for first run
		RoiManager PickOne = null;
						
		IJ.run("Threshold...");
		IJ.run(BlueImage, "Analyze Particles...", "size=150-Infinity pixel exclude include add");
		PickOne = RoiManager.getInstance();
		int RoiCount = PickOne.getCount();
		
		//SAVE THE ROI LIST		
		RegionName = "C:" + "\\" + "Temp" + "\\" + "RoiSet" + ".zip"; 

		PickOne.runCommand("Save", RegionName);
		int SelectedCells [] =  new int [RoiCount];
		
		//Select the correct cells
		String response;
		response = "y";
		 
		IJ.setTool("point");
				
		int countpos = 0;
		do{
			
			IJ.selectWindow(BlueImageID);			
			new WaitForUserDialog("Click", "Select ROI then OK").show();
		
			SelectedCells [countpos] = PickOne.getSelectedIndex();
	
			response = JOptionPane.showInputDialog("Another y/n", "y");
			countpos++;
		 }while(response.equals("y"));
		
		return SelectedCells; //return an array of integers corresponding to the cells ID
	
	}

	/*
	 * Function to measure the green spots within the area of 
	 * the selected cell nucleus
	 */
	private void GreenApply(int [] SelectedCells){
		IJ.selectWindow(GreenImageID);
			
		double Mean[] = new double[100];
		double Area[] = new double[100];
		double IntegratedD[] = new double[100];
		
		for (int i=0;i<SelectedCells.length;i++){
			RoiManager SelectedOne = RoiManager.getInstance();
			SelectedOne.select(SelectedCells[i]);
			
			double threshvals = 0;			
			threshvals = FindThreshold();
			double minthreshvals = threshvals*0.6;
			IJ.setThreshold(GreenImage, minthreshvals, threshvals);
			new WaitForUserDialog("Adjust Threshold", "Adjust the threshold if required and then click OK").show();
			SelectedOne.runCommand(GreenImage,"Deselect");
			SelectedOne.runCommand(GreenImage,"Delete");
			IJ.run(GreenImage, "Analyze Particles...", "size=5-200 pixel display exclude clear include add slice");
			ResultsTable rt = new ResultsTable();	
			rt = Analyzer.getResultsTable();
			int numvals = rt.getCounter();
			String Whatis = "Green Dot"; //Set colour for the dot so that output function labels it correctly
			//Populate the arrays for the measurement data
			for (int j=0;j<numvals;j++){
				Mean[j] = rt.getValueAsDouble(1, j);
				Area[j] = rt.getValueAsDouble(0, j);
				IntegratedD[j] = rt.getValueAsDouble(25, j);				
			}
			OutputText(Mean,Area,IntegratedD,Whatis,i); //Send results to output function to add to text file.
			IJ.deleteRows(0, numvals);
			
			/*
			 * Call the function to count the red values, its
			 * called now so that the same selected cell is counted 
			 * in both green and red at the same time. This is to make 
			 * sorting them out for the output text file a bit easier 
			 */
			RedApply(SelectedCells, SelectedOne, i);	
			
			
			IJ.selectWindow(GreenImageID);
			RoiManager rm = RoiManager.getInstance();
			rm.runCommand("Open", RegionName); //Reopen the ROI manager with all the selected cells  
			RedBackground();//Goto the function to calculate the level of background in the red image.
		}
//		RedBackground();
	}
	
	private void RedApply(int [] SelectedCells, RoiManager SelectedOne, int i){
		IJ.selectWindow(RedImageID);
		
		//Measure Tethering Site in red labelled image
		
		for (int d=0;d<SelectedOne.getCount();d++){
			SelectedOne.select(d);
			SelectedOne.runCommand(RedImage,"Measure");
			ResultsTable rt = new ResultsTable();	
			rt = Analyzer.getResultsTable();
			int numvals = rt.getCounter();
			String Whatis = "Red Dot"; //Set colour for the dot so that output function labels it correctly
			//Populate the arrays for the measurement data
			double Mean[] = new double[100];
			double Area[] = new double[100];
			double IntegratedD[] = new double[100];
			for (int j=0;j<numvals;j++){
				Mean[j] = rt.getValueAsDouble(1, j);
				Area[j] = rt.getValueAsDouble(0, j);
				IntegratedD[j] = rt.getValueAsDouble(25, j);		
			}
			OutputText(Mean,Area,IntegratedD,Whatis,i); //Send results to output function to add to text file.
			IJ.deleteRows(0, numvals);
		}
		
		SelectedOne.runCommand(RedImage,"Deselect");
		SelectedOne.runCommand(RedImage,"Delete");
	}
	
	private void RedBackground(){
		ClearResults();
		IJ.selectWindow(RedImageID);
		
		double Mean[] = new double[4];
		double Area[] = new double[4];
		double IntegratedD[] = new double[4];
		
		for(int y=0;y<4;y++){
			new WaitForUserDialog("Place ROI", "Place ROI " + (y+1) + " on area of background").show();
			IJ.run("Measure","");
			ResultsTable rt = new ResultsTable();	
			rt = Analyzer.getResultsTable();
			Mean[y] = rt.getValueAsDouble(1, 0);
			Area[y] = rt.getValueAsDouble(0, 0);
			IntegratedD[y] = rt.getValueAsDouble(25, 0);	
			ClearResults();
		}
		OutputBackgroundText(Mean,Area,IntegratedD);
	}

	private double FindThreshold(){
		ImagePlus imp;
		imp = WindowManager.getCurrentImage();
		double threshvals = 0;
		IJ.setAutoThreshold(imp, "Default dark");
		IJ.run(imp, "Analyze Particles...", "size=3-Infinity pixel display clear include");
		
		ResultsTable rt = new ResultsTable();	
		rt = Analyzer.getResultsTable();
		int numvals = rt.getCounter();
		double highval = 0;
		for (int j=0;j<numvals;j++){
			double max = rt.getValueAsDouble(5, j);
			if (max>highval){
				highval = max;
			}
		}
		threshvals=highval;
		
		return threshvals;
	}
	
	
	/*
	 * Function to output the measurements from RedApply and GreenApply to a text file. The text file
	 * is kept in a temp directory in the C: drive rather than putting it back in the directory from 
	 * which the image came. This was done so that all the data is put straight into 1 file from the
	 * start to save a lot of cutting and pasting in excel.
	 */
	public void OutputText(double[] Mean, double [] Area, double[] IntegratedD, String Whatis, int i){
		
		String CreateName = "C:/Temp/Results.txt";
		String FILE_NAME = CreateName;
    	
		//Get Number of values in array
		int numcells = Mean.length;
		int Cellcount = 0;
		for (int t=0; t<numcells; t++){
			double valnum = 0;
			valnum = Mean[t];
			if (valnum>0){
				Cellcount++;
			}
		}
		
		
		try{
			FileWriter fileWriter = new FileWriter(FILE_NAME,true);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			for (int z = 0; z<Cellcount;z++){
				if (Whatis.equals("Green Dot") && i==0){
					bufferedWriter.newLine();
					bufferedWriter.newLine();
					bufferedWriter.write(" File= " + filename);
					bufferedWriter.newLine();
					bufferedWriter.newLine();
				}
			
				
				if(Whatis.equals("Green Dot")||Whatis.equals("Red Dot")){
					bufferedWriter.write(Whatis + " Cell " + (i+1) + " Mean Intensity = " + Mean[z] + " " + " Area = " + Area[z] + " Integrated Intensity = " + IntegratedD[z]);

				}
				if(Whatis.equals("Rest of Red")){
					bufferedWriter.write(Whatis + " Cell " + (i+1) + " Dot_Number " + (z+1) + " Mean Intensity = " + Mean[z] + " " + " Area = " + Area[z] + " Integrated Intensity = " + IntegratedD[z]);

				}
				
				bufferedWriter.newLine();
			}
			
			if(Whatis.equals("Background")){
				bufferedWriter.write(Whatis + " Mean Intensity = " + Mean[0] + " " + " Area = " + Area[0] + " Integrated Intensity = " + IntegratedD[0]);
			}
			bufferedWriter.close();

		}
		catch(IOException ex) {
            System.out.println(
                "Error writing to file '"
                + FILE_NAME + "'");
        }
		
	}
	
	public void OutputBackgroundText(double[] Mean, double [] Area, double[] IntegratedD){
		String CreateName = "C:/Temp/Results.txt";
		String FILE_NAME = CreateName;
		
		try{
			FileWriter fileWriter = new FileWriter(FILE_NAME,true);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			for(int y=0;y<4;y++){
				bufferedWriter.newLine();
				bufferedWriter.write("BackGround Mean Intensity = " + Mean[y] + " " + " Area = " + Area[y] + " Integrated Intensity = " + IntegratedD[y]);
			}
			bufferedWriter.close();

		}
		catch(IOException ex) {
            System.out.println(
                "Error writing to file '"
                + FILE_NAME + "'");
        
		}
	}
			
	
	//Function to remove all the values from the results table at the end of a run.
	private void ClearResults(){
		ResultsTable emptyrt = new ResultsTable();	
		emptyrt = Analyzer.getResultsTable();
		int valnums = emptyrt.getCounter();
		for(int a=0;a<valnums;a++){
			IJ.deleteRows(0, a);
		}
	}
	
}

