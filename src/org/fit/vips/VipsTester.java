/*
 * Tomas Popela, 2012
 * VIPS - Visual Internet Page Segmentation
 * Module - VipsTester.java
 */

package org.fit.vips;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import com.opencsv.CSVReader;

public class VipsTester {

	/**
	 * Main function
	 * @param args Internet address of web page.
	 * @throws IOException 
	 */
	public static void main(String args[]) throws IOException{
		/*// we've just one argument - web address of page
		if (args.length != 1)
		{
			System.err.println("We've just only one argument - web address of page!");
			System.exit(0);
		}*/
		/*String url = args[0];
		try{
			Vips vips = new Vips();
			// disable graphics output
			vips.enableGraphicsOutput(false);
			// disable output to separate folder (no necessary, it's default value is false)
			vips.enableOutputToFolder(true);
			// set permitted degree of coherence
			vips.setPredefinedDoC(8);
			System.out.println(url);
			// start segmentation on page
			vips.startSegmentation(url);
		}catch (Exception e){
			e.printStackTrace();
		}*/
		//String CSVFilesPaths_dataset="CSV files paths-dataset2017.csv";
		String CSVFilesPaths_dataset="CSV files paths-dataset2005.csv";
		List<ArrayList<String>> webPagesPaths_dataset2017=readFileToList(CSVFilesPaths_dataset);
		ArrayList<String> webPagesPaths=new ArrayList<String>();
		for(int i=0; i<webPagesPaths_dataset2017.size(); i++)
			webPagesPaths.add("file:///"+webPagesPaths_dataset2017.get(i).get(0).trim());

		//for(int i=1; i<=webPagesPaths.size(); i++){
			long startTime = System.currentTimeMillis();
			
			int i = 233 ;
			
			String url = webPagesPaths.get(i-1); 
			try{
				Vips vips = new Vips();
				vips.enableGraphicsOutput(false);
				vips.enableOutputToFolder(true);
				vips.setPredefinedDoC(8);
				System.out.println("\n"+i+". URL:  "+url);
				vips.startSegmentation(url);
			}catch (Exception e){
				e.printStackTrace();
			}
			long endTime   = System.currentTimeMillis();
			long totalTime = endTime - startTime;
			System.out.println("\nExecution time of ViDE: "+totalTime + " ms;  "+totalTime/1000.0+" sec");
			System.out.println("\n"+totalTime + " ms");
			//JOptionPane.showMessageDialog(null, "Done !");
		//}
	}
	
	public static List<ArrayList<String>> readFileToList(String csvFile) throws IOException{
		List<ArrayList<String>> dataRecords = new ArrayList<ArrayList<String>>();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(csvFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
        InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
        CSVReader reader = new CSVReader(isr);
        for (String[] row; (row = reader.readNext()) != null;) {
        	ArrayList<String> dataRecord = new ArrayList<String>();
        	for(int i=0; i<row.length; i++)
        		dataRecord.add(row[i]);
        	dataRecords.add(dataRecord);
        }
        reader.close();
        isr.close();
        fis.close();
		return dataRecords;
	}
	
}
