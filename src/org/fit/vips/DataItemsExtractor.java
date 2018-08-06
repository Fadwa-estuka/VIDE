package org.fit.vips;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;

public class DataItemsExtractor {
	
	/*
	 * Class variables
	 */
	private ArrayList<ArrayList<VipsBlock>> dataRecords=null;
	private VisualStructure dataRegion=null;
	private String consoleOutput="";
	
	/*
	 * Constructor
	 */
	public DataItemsExtractor(ArrayList<ArrayList<VipsBlock>> dataRecords, VisualStructure dataRegion, String consoleOutput){
		this.dataRecords=dataRecords;
		this.dataRegion=dataRegion;
		this.consoleOutput=consoleOutput;
	}
	
	/*
	 * Method to get the data region variable
	 */
	public VisualStructure getDataRegion(){
		return this.dataRegion;
	}
	
	/*
	 * Extracting the data items
	 */
	public ArrayList extractDataItems(){
		int rows=0, cols=0;
		String printedTable="";
		ArrayList results = new ArrayList();
		ArrayList<ArrayList<VipsBlock>> dataRecords=segmentDataRecords(this.dataRecords);
		ArrayList<ArrayList<String>> alignedGroups=alignDataItems(dataRecords);
		if(alignedGroups.size()>0)
			rows=alignedGroups.get(0).size(); 
		cols=alignedGroups.size();
		String[][] resultTable = new String[rows][cols+1], resultTableCSV = new String[rows][cols];
		ArrayList<Integer> maxSizes=new ArrayList<Integer>();
		fillResultTable(resultTable,maxSizes,alignedGroups, rows, cols+1);
		fillResultTableCSV(resultTableCSV,alignedGroups, rows, cols);
		printedTable=printTable(resultTable,maxSizes, rows, cols+1);
		results.add(printedTable);
		results.add(resultTableCSV);
		return results;
	}
	
	/*
	 * Data record segmentation
	 */
	public ArrayList<ArrayList<VipsBlock>> segmentDataRecords(ArrayList<ArrayList<VipsBlock>> groups){
		ArrayList<ArrayList<VipsBlock>> newGroups=new ArrayList<ArrayList<VipsBlock>>();
		for(int i=0; i<groups.size(); i++){
			ArrayList<VipsBlock> group=groups.get(i), newGroup=new ArrayList<VipsBlock>();
			for(int j=0; j<group.size(); j++){
				VipsBlock vipsBlock=group.get(j);
				if(vipsBlock.getChildren().size()>0){
					newGroup.addAll(vipsBlock.getChildren());
				}else
					newGroup.add(vipsBlock);
			}
			removeEmptyItems(newGroup);
			newGroups.add(newGroup);
		}
		consoleOutput+="\n Data Records : \n";
		for(int i=0; i<newGroups.size(); i++){
			consoleOutput+="------------- Data Record "+(i+1)+" -----------\n";
			ArrayList<VipsBlock> group=newGroups.get(i);
			Collections.sort(group, VipsBlock.VipsBlockComparator);
			for(int j=0; j<group.size(); j++){
				VipsBlock vipsBlock=group.get(j);
				if(j>0)
					vipsBlock.setAheadBlock(group.get(j-1));
				consoleOutput+=(j+1)+"  "+vipsBlock.getBox().getText()+"\n";
			}
		}
		return newGroups;
	}
	
	/*
	 * Removing empty items from a vipsBlock list
	 */
	public void removeEmptyItems(ArrayList<VipsBlock> itemList){
		ArrayList<VipsBlock> removedItems=new ArrayList<VipsBlock>();
		for(int i=0; i<itemList.size(); i++)
			if(itemList.get(i).getBox().getText().trim().length()==0)
				removedItems.add(itemList.get(i));
		itemList.removeAll(removedItems);
	}
	
	/*
	 * Data items alignment
	 */
	public ArrayList<ArrayList<String>> alignDataItems(ArrayList<ArrayList<VipsBlock>> dataRecords){
		ArrayList<ArrayList<String>> columns=new ArrayList<ArrayList<String>>();
		ArrayList<VipsBlock> currentItemSet=new ArrayList<VipsBlock>();
		for(int i=0; i<dataRecords.size(); i++){
			if(dataRecords.get(i).size()>0)
				currentItemSet.add(dataRecords.get(i).get(0));
		}
		while(currentItemSet.size()>0){
			ArrayList<VipsBlock> currentCluster=null;
			currentItemSet=removeDuplicates(currentItemSet);
			Collections.sort(currentItemSet, VipsBlock.VipsBlockComparator);
			consoleOutput+="-------------------------------------------------------------------------------------------------------------\n";
			consoleOutput+="currentItemSet: ";
			for(int i=0; i<currentItemSet.size(); i++){
				consoleOutput+=currentItemSet.get(i).getBox().getText()+"  __  ";
			}
			consoleOutput+="\n";
			ArrayList<ArrayList<VipsBlock>> clusters=clusterDataItems(currentItemSet);
			ArrayList<Integer> maxList=new ArrayList<Integer>();
			for(int i=0; i<clusters.size(); i++){
				ArrayList<VipsBlock> cluster=clusters.get(i);
				ArrayList<ArrayList<VipsBlock>> notClusteredDataRecords=getNotClusteredDataRecords(dataRecords,cluster);
				ArrayList<Integer> maxPositionsList=new ArrayList<Integer>();
				consoleOutput+="Cluster "+(i+1)+" :\n";
				for(int j=0; j<notClusteredDataRecords.size(); j++){
					ArrayList<VipsBlock> notClusteredDataRecord=notClusteredDataRecords.get(j);
					ArrayList<Integer> positionsList=new ArrayList<Integer>();
					consoleOutput+="Record:"+(dataRecords.indexOf(notClusteredDataRecord)+1);
					int indexOfItemU=0;
					for(int k=0; k<currentItemSet.size(); k++){
						if(notClusteredDataRecord.contains(currentItemSet.get(k)))
							indexOfItemU=notClusteredDataRecord.indexOf(currentItemSet.get(k));
					}
					consoleOutput+="(indexOfItemU:"+indexOfItemU+") ";
					for(int k=indexOfItemU+1; k<notClusteredDataRecord.size(); k++){
						if(dataItemsMatching(notClusteredDataRecord.get(k),cluster.get(0))){
							positionsList.add(k);
							consoleOutput+=(k)+",";
							break;
						}
						else
							positionsList.add(0);
					}
					if(positionsList.size()==0)
						positionsList.add(0);
					int maxP=Collections.max(positionsList);
					maxPositionsList.add(maxP);
					consoleOutput+=" __ ";
				}
				consoleOutput+="\nmaxPositions:  ";
				for(int j=0; j<maxPositionsList.size(); j++){
					consoleOutput+=maxPositionsList.get(j)+"  ";
				}
				int tempMax=0;
				if(maxPositionsList.size()>0){
					tempMax=Collections.max(maxPositionsList);
				}
				consoleOutput+="  ====> max="+tempMax+"\n";
				maxList.add(tempMax);
			}
			for(int i=0; i<clusters.size(); i++){
				ArrayList<VipsBlock> cluster=clusters.get(i);
				if(maxList.get(i)==0){
					currentCluster=cluster;
					break;
				}
			}
			if(currentCluster==null){
				int max=Collections.max(maxList), index=maxList.indexOf(max);
				currentCluster=clusters.get(index);
			}
			consoleOutput+="Maxes: ";
			for(int i=0; i<maxList.size(); i++){
				consoleOutput+=maxList.get(i)+" ";
			}
			consoleOutput+="     CurrentCluster#:"+(clusters.indexOf(currentCluster)+1)+"\n";
			ArrayList<ArrayList<VipsBlock>> clusteredDataRecords=getClusteredDataRecords(dataRecords,currentCluster);
			ArrayList<Integer> indexesOfItems=new ArrayList<Integer>();
			consoleOutput+="DataRecords in CurrentCluster: ";
			for(int i=0; i<clusteredDataRecords.size(); i++){
				indexesOfItems.add(dataRecords.indexOf(clusteredDataRecords.get(i)));
				consoleOutput+="Record:"+(dataRecords.indexOf(clusteredDataRecords.get(i))+1)+" __ ";
			}
			consoleOutput+="\n";
			ArrayList<ArrayList<VipsBlock>> notClusteredDataRecords=getNotClusteredDataRecords(dataRecords,currentCluster);
			consoleOutput+="DataRecords not in CurrentCluster: ";
			for(int i=0; i<notClusteredDataRecords.size(); i++){
				consoleOutput+="Record:"+(dataRecords.indexOf(notClusteredDataRecords.get(i))+1)+" __ ";
			}
			consoleOutput+="\n";
			ArrayList<VipsBlock> removableItems=new ArrayList<VipsBlock>(), newItems=new ArrayList<VipsBlock>();
			for(int i=0; i<currentItemSet.size(); i++){
				VipsBlock item=currentItemSet.get(i);
				for(int j=0; j<clusteredDataRecords.size(); j++)
					if(clusteredDataRecords.get(j).contains(item)){
						removableItems.add(item);
						int index=clusteredDataRecords.get(j).indexOf(item)+1;
						if(index<clusteredDataRecords.get(j).size())
							newItems.add(clusteredDataRecords.get(j).get(index));
					}	
			}
			for(int i=0; i<removableItems.size(); i++){
				currentItemSet.remove(removableItems.get(i));
			}
			consoleOutput+="\nCurrentItemSet after removing: ";
			for(int i=0; i<currentItemSet.size(); i++){
				consoleOutput+=currentItemSet.get(i).getBox().getText()+"  __  ";
			}
			currentItemSet.addAll(newItems);
			consoleOutput+="\nCurrentItemSet after adding: ";
			for(int i=0; i<currentItemSet.size(); i++){
				consoleOutput+=currentItemSet.get(i).getBox().getText()+"  __  ";
			}
			currentItemSet.addAll(newItems);
			consoleOutput+="\nindexesOfItems: ";
			for(int i=0; i<indexesOfItems.size(); i++){
				consoleOutput+=indexesOfItems.get(i)+" ";
			}
			consoleOutput+="\n\n";
			ArrayList<String> column=new ArrayList<String>();
			int counter=0;
			for(int i=0; i<dataRecords.size(); i++){
				if(indexesOfItems.contains(i)){
					column.add(currentCluster.get(counter).getBox().getText());
					counter++;
				}else{
					column.add(" ");
				}
			}
			columns.add(column);
			consoleOutput+="Column ===> ";
			for(int i=0; i<column.size(); i++){
				consoleOutput+=(i+1)+":"+column.get(i)+"    ";
			}
			consoleOutput+="\n";
		}
		return columns;
	}
	
	/*
	 * Removing duplicates from vipsBlock list
	 */
	public ArrayList<VipsBlock> removeDuplicates(ArrayList<VipsBlock> list){
		ArrayList<VipsBlock> newList=new ArrayList<VipsBlock>();
		for(int i=0; i<list.size(); i++){
			VipsBlock item=list.get(i);
			if(!newList.contains(item))
				newList.add(item);
		}
		return newList;
	}
	
	/*
	 * Data items clustering
	 */
	public ArrayList<ArrayList<VipsBlock>> clusterDataItems(ArrayList<VipsBlock> itemSet){
		ArrayList<ArrayList<VipsBlock>> clusters=new ArrayList<ArrayList<VipsBlock>>();
		ArrayList<VipsBlock> cluster=new ArrayList<VipsBlock>(), clusteredItems=new ArrayList<VipsBlock>();
		cluster.add(itemSet.get(0));
		clusteredItems.add(itemSet.get(0));
		clusters.add(cluster);
		for(int i=1; i<itemSet.size(); i++){
			VipsBlock item=itemSet.get(i);
			for(int j=0; j<clusters.size(); j++){
				cluster=clusters.get(j);
				if(dataItemsMatching(item,cluster.get(0))){
					cluster.add(item);
					clusteredItems.add(item);
					break;
				}
			}
			if(!clusteredItems.contains(item)){
				cluster=new ArrayList<VipsBlock>();
				cluster.add(item);
				clusteredItems.add(item);
				clusters.add(cluster);
			}
		}
		for(int i=0; i<clusters.size(); i++){
			cluster=clusters.get(i);
			consoleOutput+="Cluster "+(i+1)+" size("+cluster.size()+") : ";
			for(int j=0; j<cluster.size(); j++){
				consoleOutput+=cluster.get(j).getBox().getText()+" __ ";
			}
			consoleOutput+="\n";
		}
		return clusters;
	}
	
	/*
	 * Data items matching
	 */
	public boolean dataItemsMatching(VipsBlock item1, VipsBlock item2){
		if(item1==null || item2==null)
			return false;
		String font1=getFontAttributes(item1), font2=getFontAttributes(item2); 
		if(font1.compareToIgnoreCase(font2)!=0)
			return false;
		if(getPosition(item1)==getPosition(item2))
			return true;
		return dataItemsMatching(item1.getAheadBlock(), item2.getAheadBlock()); 
	}
	
	/*
	 * Getting a font attributes
	 */
	public String getFontAttributes(VipsBlock block){
		String fontAtt=block.getBox().getVisualContext().getFont().getFontName();
		fontAtt=fontAtt.concat(String.valueOf(block.getBox().getVisualContext().getFont().getSize()));
		fontAtt=fontAtt.concat(String.valueOf(block.getBox().getVisualContext().getFont().getStyle()));
		fontAtt=fontAtt.concat(String.valueOf(block.getBox().getVisualContext().getFont().BOLD));
		fontAtt=fontAtt.concat(String.valueOf(block.getBox().getVisualContext().getFont().ITALIC));
		fontAtt=fontAtt.concat(String.valueOf(block.getBox().getVisualContext().color));
		fontAtt=fontAtt.concat(String.valueOf(block.getBox().getVisualContext().getFont().getAttributes()));
		return fontAtt;
	}
	
	/*
	 * Getting an item position
	 */
	public int getPosition(VipsBlock item){
		return (item.getBox().getAbsoluteContentX()-this.getDataRegion().getX());
	}
	
	/*
	 * Identifying the not clustered data records in a cluster
	 */
	public ArrayList<ArrayList<VipsBlock>> getNotClusteredDataRecords(ArrayList<ArrayList<VipsBlock>> dataRecords, ArrayList<VipsBlock> cluster){
		ArrayList<ArrayList<VipsBlock>> dataRecordsList=new ArrayList<ArrayList<VipsBlock>>();
		for(int i=0; i<dataRecords.size(); i++){
			ArrayList<VipsBlock> dataRecord=dataRecords.get(i);
			boolean contain=false;
			for(int j=0; j<cluster.size(); j++){
				if(dataRecord.contains(cluster.get(j))){
					contain=true;
					break;
				}
			}
			if(!contain)
				dataRecordsList.add(dataRecord);
		}
		return dataRecordsList;
	}
	
	/*
	 * Identifying the clustered data records in a cluster
	 */
	public ArrayList<ArrayList<VipsBlock>> getClusteredDataRecords(ArrayList<ArrayList<VipsBlock>> dataRecords, ArrayList<VipsBlock> cluster){
		ArrayList<ArrayList<VipsBlock>> dataRecordsList=new ArrayList<ArrayList<VipsBlock>>();
		for(int i=0; i<dataRecords.size(); i++){
			ArrayList<VipsBlock> dataRecord=dataRecords.get(i);
			boolean contain=false;
			for(int j=0; j<cluster.size(); j++){
				if(dataRecord.contains(cluster.get(j))){
					contain=true;
					break;
				}
			}
			if(contain)
				dataRecordsList.add(dataRecord);
		}
		return dataRecordsList;
	}
	
	/*
	 * Get the maximum value from list of integers
	 */
	public static int maxValue(ArrayList<Integer> values) {
		int max =0;
		if(values.size()>0)
	    	max = values.get(0);
	    for (int ktr = 0; ktr < values.size(); ktr++) {
	        if (values.get(ktr) > max) {
	            max = values.get(ktr);
	        }
	    }
	    return max;
	}
	
	/*
	 * Filling the result table 
	 */
	public void fillResultTable(String[][] resultTable, ArrayList<Integer> maxSizes, ArrayList<ArrayList<String>> alignedGroups, int rows, int cols){
		ArrayList<Integer> sizes=new ArrayList<Integer>();
		String item="";
		for(int i=0; i<cols; i++){
			sizes=new ArrayList<Integer>(); 
			for(int j=0; j<rows; j++){
				if(i==0)
					item=(j+1)+"";
				else
					item=alignedGroups.get(i-1).get(j);
				resultTable[j][i]=item;
				sizes.add(item.length());
			}
			maxSizes.add(maxValue(sizes));
		}
	}
	
	/*
	 * Filling the result CSV table 
	 */
	public void fillResultTableCSV(String[][] resultTableCSV, ArrayList<ArrayList<String>> alignedGroups, int rows, int cols){
		String item="";
		for(int i=0; i<cols; i++){
			for(int j=0; j<rows; j++){
				item=alignedGroups.get(i).get(j);
				if(item.charAt(0)=='-')
					item = item.replace("-","");
				if(item.charAt(0)=='=')
					item = item.replace("=","");
				item=item.replaceAll("\"", "\"\"");
				if(item.contains(",") || item.contains("\""))
					item="\""+item+"\"";
				resultTableCSV[j][i]=item;
				//resultTableCSV[j][i]=" "+item;
			}
		}
	}
	
	/*
	 * Printing out the result table by converting the table to string that can be wrote on file
	 */
	public String printTable(String[][] resultsTable, ArrayList<Integer> maxSizes, int rows, int cols){
		int totalRowSize=0;
		String printedTable="";
		for(int i=0; i<cols; i++){
			int size=maxSizes.get(i).intValue();
			totalRowSize+=size;
			for(int j=0; j<rows; j++){
				String item=resultsTable[j][i];
				int sizeDiff=size-item.length();
				for(int k=0; k<sizeDiff; k++)
					item+=" ";
				resultsTable[j][i]=item;
			}
		}
		totalRowSize+=cols*3;
		consoleOutput+="\n";
		for(int i=0; i<rows; i++){
			for(int j=0; j<cols; j++){
				printedTable+=resultsTable[i][j]+" | ";
			}
			printedTable+="\n";
			for(int j=0; j<totalRowSize-1; j++){
				printedTable+="-";
			}
			printedTable+="\n";
		}
		consoleOutput+=printedTable+"\n";
		System.out.println(printedTable);
		try{
		    PrintWriter writer = new PrintWriter("consoleOutput.txt", "UTF-8");
		    writer.print(consoleOutput);
		    writer.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
		return printedTable;
	}
	
}
