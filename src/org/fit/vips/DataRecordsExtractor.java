package org.fit.vips;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.zip.Inflater;

import org.w3c.dom.Element;

public class DataRecordsExtractor {
	
	/*
	 * Class variables
	 */
	private Element vipsElement=null;
	private VisualStructure visualStructure=null;
	private VisualStructure dataRegion=null;
	private String consoleOutput="";
	
	/*
	 * Constructor
	 */
	public DataRecordsExtractor(Element vipsElement, VisualStructure visualStructure) {
		this.vipsElement=vipsElement;
		this.visualStructure=visualStructure;
	}
	
	/*
	 * Method to set the data region variable
	 */
	public void setDataRegion(VisualStructure dataRegion){
		this.dataRegion=dataRegion;
	}
	
	/*
	 * Method to get the data region variable
	 */
	public VisualStructure getDataRegion(){
		return this.dataRegion;
	}
	
	/*
	 * Extracting the data records
	 */
	public ArrayList extractDataRecords(String regionType) {
		//String printedTable="";
		ArrayList results = new ArrayList();
		if(regionType=="dataRegion"){
			VisualStructure dataRegion=findDataRegion();
			this.setDataRegion(dataRegion);
			if(dataRegion!=null){
				removeNoiseBlocks(dataRegion);
				ArrayList<VipsBlock> blocksList=getBlocksOfDataRegion(dataRegion);
				ArrayList<ArrayList<VipsBlock>> clusters=clusterBlocks(blocksList);
				ArrayList<ArrayList<VipsBlock>> dataRecords=blockRegrouping(clusters);
				DataItemsExtractor dataItemsExtractor=new DataItemsExtractor(dataRecords,dataRegion,consoleOutput);
				results=dataItemsExtractor.extractDataItems();
			}else{
				consoleOutput+="There is no data region selected!\n";
				System.out.println("There is no data region selected!");
				System.out.println("0 Data records extracted!");
				try{
					PrintWriter writer = new PrintWriter("consoleOutput.txt", "UTF-8");
					writer.print(consoleOutput);
					writer.close();
				}catch (IOException e) {
					e.printStackTrace();
				}
			}
		}else{
			ArrayList<VipsBlock> blocksList=getBlocksOfDataRegion(visualStructure);
			ArrayList<ArrayList<VipsBlock>> clusters=clusterBlocks(blocksList);
			ArrayList<ArrayList<VipsBlock>> dataRecords=blockRegrouping(clusters);
			DataItemsExtractor dataItemsExtractor=new DataItemsExtractor(dataRecords,visualStructure,consoleOutput);
			results=dataItemsExtractor.extractDataItems();
		}
		return results;
	}
	
	/*
	 * Locating the data region of the webpage
	 */
	public VisualStructure findDataRegion(){
		int docWidth=Integer.parseInt(vipsElement.getAttribute("PageRectWidth")),
			docHeight=Integer.parseInt(vipsElement.getAttribute("PageRectHeight")),
		    docSize=docWidth*docHeight;
		double docCenterX = docWidth / 2.0;
		consoleOutput+="docWidth: "+docWidth+"  docHeight: "+docHeight+"  docSize: "+docSize+"  docCenterX:"+docCenterX+"\n\n";
		TreeTraversal treeTraversal=new TreeTraversal(visualStructure);
		treeTraversal.traverse(visualStructure);
		ArrayList<VisualStructure> visualStructureList=treeTraversal.getVisualStructureQueue(), dataRegionCandidates=new ArrayList<VisualStructure>();
		ArrayList<Integer> visualStructureSizes=new ArrayList<Integer>();
		consoleOutput+="-------------- candidates -----------\n";
		for(VisualStructure visualStructure : visualStructureList){ 
			int visualStructureWidth=visualStructure.getWidth(), visualStructureHeight=visualStructure.getHeight(),
			    visualStructureLeft=visualStructure.getX(), visualStructureSize=visualStructureWidth*visualStructureHeight;
			double visualStructureCenterX=visualStructureLeft + visualStructureWidth/2.0;
			if(visualStructureCenterX==docCenterX && visualStructureSize!=docSize && visualStructureSize>=(docSize*(20.0/100))){
				consoleOutput+="visualStructure   Order: "+visualStructure.getOrder()+"   Width: "+visualStructureWidth+"   Height:"+visualStructureHeight+"   Left: "+visualStructureLeft+"   Size: "+visualStructureSize+"   CenterX: "+visualStructureCenterX+"\n";
				dataRegionCandidates.add(visualStructure);
				visualStructureSizes.add(visualStructureSize);
			}
		}
		consoleOutput+="--------------------------------------\n";
		if(dataRegionCandidates.size()>0){
			int minSize=getMin(visualStructureSizes), minSizeIndex=visualStructureSizes.indexOf(minSize);
			VisualStructure dataRegion=dataRegionCandidates.get(minSizeIndex);
			consoleOutput+="data region ===> index: "+minSizeIndex+"     size: "+minSize+"     order: "+dataRegion.getOrder()+"\n";
			consoleOutput+="data region content:  "+dataRegion.getContent()+"\n";
			consoleOutput+="data region Children length: "+dataRegion.getChildrenVisualStructures().size()+" visual structures"+"\n";
			return dataRegion;
		}else{
			return null;
		}
	}

	/*
	 * Collecting the blocks of the data region
	 */
	public ArrayList<VipsBlock> getBlocksOfDataRegion(VisualStructure dataRegion){
		ArrayList<VipsBlock> blocksList=new ArrayList<VipsBlock>();
		TreeTraversal treeTraversal=new TreeTraversal(dataRegion);
		treeTraversal.traverse(dataRegion);
		ArrayList<VisualStructure> visualStructureList=treeTraversal.getVisualStructureQueue();
		for(int i=0; i<visualStructureList.size(); i++){
			VisualStructure visualStructure=visualStructureList.get(i);
			if(visualStructure.getChildrenVisualStructures().size()==0){
				consoleOutput+="\n------- Order: "+visualStructure.getOrder()+"    NestedBlocks: "+visualStructure.getNestedBlocks().size()+" --------"+"\n";
				for(int j=0; j<visualStructure.getNestedBlocks().size(); j++){
					VipsBlock vipsBlock=visualStructure.getNestedBlocks().get(j);
					consoleOutput+="vipsBlock "+(j+1)+"   isImg: "+vipsBlock.isImg()+"   txt: "+vipsBlock.getElementBox().getText()+"    children size: "+vipsBlock.getChildren().size()+"      nodeName: "+vipsBlock.getBox().getNode().getNodeName()+"\n";
					vipsBlock.setVisualStructureNo(visualStructure.getOrder());
					blocksList.add(vipsBlock); 
				}
			}
		}
		for(int i=0; i<blocksList.size(); i++)
			blocksList.get(i).setId(i+1);
		return blocksList;
	}
	
	/*
	 * Removing noise blocks from the data region
	 */
	public void removeNoiseBlocks(VisualStructure dataRegion) {
		if(dataRegion.getChildrenVisualStructures().size()==0){
			consoleOutput+="\nNo noise blocks to be removed !\n";
			System.out.println("\nNo noise blocks to be removed !\n");
		}else{
			ArrayList<VisualStructure> childBlocks=new ArrayList<VisualStructure>();
			consoleOutput+="Data region children visual structures :\n";
			for(int i=0; i<dataRegion.getChildrenVisualStructures().size(); i++){
				VisualStructure child=dataRegion.getChildrenVisualStructures().get(i);
				consoleOutput+=(i+1)+":   "+"order: "+child.getOrder()+"    Left: "+child.getX()+"\n";
				childBlocks.add(child);
			}
			int sizeBeforeRemove=childBlocks.size(), recordsLeft=childBlocks.get(1).getX();
			boolean recordsAligned=true;
			for(int i=1; i<childBlocks.size()-1; i++){
				if(childBlocks.get(i).getX()!=recordsLeft){
					recordsAligned=false;
					break;
				}
			}
			if(recordsAligned && childBlocks.get(0).getX()!=recordsLeft){
				consoleOutput+="Removing first noise block ..     Content: "+childBlocks.get(0).getContent()+"\n";
				dataRegion.removeChild(childBlocks.get(0));
				childBlocks.remove(0);
			}
			if(recordsAligned && childBlocks.size()>1 && childBlocks.get(childBlocks.size()-1).getX()!=recordsLeft){
				consoleOutput+="Removing last noise block ..     Content: "+childBlocks.get(childBlocks.size()-1).getContent()+"\n";
				dataRegion.removeChild(childBlocks.get(childBlocks.size()-1));
				childBlocks.remove(childBlocks.size()-1);
			}
			if(childBlocks.size()==sizeBeforeRemove){
				consoleOutput+="\nNo noise blocks found !\n";
				System.out.println("\nNo noise blocks found !");
			}
		}
	}
	
	/*
	 * Clustering the vips blocks of the data region
	 */
	public ArrayList<ArrayList<VipsBlock>> clusterBlocks(ArrayList<VipsBlock> blockList){
		ArrayList<ArrayList<VipsBlock>> clusters=new ArrayList<ArrayList<VipsBlock>>();
	    ArrayList<VipsBlock> firstCluster=new ArrayList<VipsBlock>(), clusteredNodes=new ArrayList<VipsBlock>();
	    firstCluster.add(blockList.get(0));
	    clusteredNodes.add(blockList.get(0));
		clusters.add(firstCluster);
		for(int i=1; i<blockList.size(); i++){
			VipsBlock block=blockList.get(i);
			for(int j=0; j<clusters.size(); j++){
				ArrayList<VipsBlock> cluster=clusters.get(j);
				double sim=calculateSim(block, cluster);
				if(differentVisualStructures(block, cluster) && sim>0.8 && !clusteredNodes.contains(block)){
					clusters.get(j).add(block);
					clusteredNodes.add(block);
					break;
				}
			}
			if(!clusteredNodes.contains(block)){
				ArrayList<VipsBlock> newCluster=new ArrayList<VipsBlock>();
				newCluster.add(block);
				clusters.add(newCluster);
				clusteredNodes.add(block);
			}
		}
		consoleOutput+="\nclusters size: "+clusters.size()+" : \n\n";
		for(int i=0; i<clusters.size(); i++){
			ArrayList<VipsBlock> cluster=clusters.get(i);
			Collections.sort(cluster, VipsBlock.VipsBlockComparator);
			consoleOutput+="-------- cluster "+(i+1)+":   size="+cluster.size()+" --------\n";
			for(int j=0; j<cluster.size(); j++){
				VipsBlock block=cluster.get(j);
				consoleOutput+=(j+1)+":  id: "+block.getId()+"   txt: "+block.getElementBox().getText()+"       x:"+block.getBox().getAbsoluteContentX()+"   y:"+block.getBox().getAbsoluteContentY()+"   width:"+block.getBox().getWidth()+"   height:"+block.getBox().getHeight()+"\n";
			}
		}
		return clusters;
	}
	
	/*
	 * Check if the visual structures of a vips block and a cluster's vips blocks are different
	 */
	public boolean differentVisualStructures(VipsBlock block, ArrayList<VipsBlock> cluster){
		for(int i=0; i<cluster.size(); i++){
			VipsBlock clusteredBlock=cluster.get(i);
			if(clusteredBlock.getVisualStructureNo()==block.getVisualStructureNo())
				return false;
		}
		return true;
	}

	/*
	 * Calculate the similarity between a vips block and a cluster's vips blocks 
	 */
	public double calculateSim(VipsBlock block, ArrayList<VipsBlock> cluster){
		double total=0;
		for(int i=0; i<cluster.size(); i++){
			VipsBlock clusteredBlock=cluster.get(i);
			total+=sim(clusteredBlock, block);
		}
		return total/cluster.size();
	}

	/*
	 * Calculate the similarity between two vips blocks
	 */
	public double sim(VipsBlock blockA, VipsBlock blockB){
		double similarity=wIMG(blockA,blockB) * simIMG(blockA,blockB) + wPT(blockA,blockB) * simPT(blockA,blockB) + wLT(blockA,blockB) * simLT(blockA,blockB);
		//if(blockA.getId()==3 && blockB.getId()==9)
		  //System.out.println(blockA.getId()+" **** "+blockB.getId()+"  ==> "+similarity+"       wIMG(blockA,blockB):"+wIMG(blockA,blockB)+"  simIMG(blockA,blockB):"+simIMG(blockA,blockB)+"  wPT(blockA,blockB):"+wPT(blockA,blockB)+"  simPT(blockA,blockB):"+simPT(blockA,blockB)+"   wLT(blockA,blockB):"+ wLT(blockA,blockB)+"  simLT(blockA,blockB):"+simLT(blockA,blockB));
		  //System.out.println("====>  "+blockA.getId()+"  "+blockA.getElementBox().getText()+" **** "+blockB.getId()+"  "+blockB.getElementBox().getText()+"  ==> "+similarity+"       wIMG(blockA,blockB):"+wIMG(blockA,blockB)+"  simIMG(blockA,blockB):"+simIMG(blockA,blockB)+"  wPT(blockA,blockB):"+wPT(blockA,blockB)+"  simPT(blockA,blockB):"+simPT(blockA,blockB)+"   wLT(blockA,blockB):"+ wLT(blockA,blockB)+"  simLT(blockA,blockB):"+simLT(blockA,blockB));
		return similarity;
	}

	/*
	 * Weight of image similarity
	 */
	public double wIMG(VipsBlock blockA, VipsBlock blockB){
		return (double)(sa_i(blockA)+sa_i(blockB))/(sa_b(blockA)+sa_b(blockB));
	}
	
	/*
	 * Similarity based on image size
	 */
	public double simIMG(VipsBlock blockA, VipsBlock blockB){
		int sa_i_blockA=sa_i(blockA), sa_i_blockB=sa_i(blockB);
		if(sa_i_blockA==0 && sa_i_blockB==0) return 1;
		return (double)Math.min(sa_i_blockA,sa_i_blockB)/Math.max(sa_i_blockA,sa_i_blockB);
	}
	
	/*
	 * Total area of images in a block
	 */
	public int sa_i(VipsBlock block){
		List<VipsBlock> blockChildren=block.getChildren();
		int block_imageArea=0;
		for(int i=0; i<blockChildren.size(); i++){
			VipsBlock childBlock=blockChildren.get(i);
			if(childBlock.isImg()){
				block_imageArea+=getSizeOfBlock(childBlock);
			}
		}
		if(block_imageArea==0 && block.isImg())
			block_imageArea+=getSizeOfBlock(block);
		return block_imageArea;
	}

	/*
	 * Weight of plain text similarity
	 */
	public double wPT(VipsBlock blockA, VipsBlock blockB){
		return (double)(sa_pt(blockA)+sa_pt(blockB))/(sa_b(blockA)+sa_b(blockB));
	}
	
	/*
	 * Similarity based on plain text font
	 */
	public double simPT(VipsBlock blockA, VipsBlock blockB){
		ArrayList<String> fonts_blockA=fonts_pt(blockA), fonts_blockB=fonts_pt(blockB);
		int sharedFonts=0;
		if(fonts_blockA.size()==0 || fonts_blockB.size()==0) return 0;	
		for(int i=0; i<fonts_blockA.size(); i++){
			String fontA=fonts_blockA.get(i);
			for(int j=0; j<fonts_blockB.size(); j++){
				String fontB=fonts_blockB.get(j);
				//if(fontA==fontB) sharedFonts++;
				if(fontA.compareToIgnoreCase(fontB)==0) sharedFonts++;
			}
		}
		/*if(blockA.getId()==2 && blockB.getId()==4){
			System.out.println("sharedFonts: "+sharedFonts);
			for(int i=0; i<fonts_blockA.size(); i++){
				System.out.println("fonts_blockA "+i+": "+fonts_blockA.get(i));
			}
			for(int i=0; i<fonts_blockB.size(); i++){
				System.out.println("fonts_blockB "+i+": "+fonts_blockB.get(i));
			}
		}*/
		double fn_pt_blockA=(double)sharedFonts/fonts_blockA.size(), fn_pt_blockB=(double)sharedFonts/fonts_blockB.size();
		if(fn_pt_blockA==0.0 && fn_pt_blockB==0.0) return 0;
		return (double)Math.min(fn_pt_blockA,fn_pt_blockB)/Math.max(fn_pt_blockA,fn_pt_blockB);
	}
	
	/*
	 * Total area of plain text in a block
	 */
	public int sa_pt(VipsBlock block){
		List<VipsBlock> blockChildren=block.getChildren();
		int block_plainTxtArea=0;
		/*for(int i=0; i<blockChildren.size(); i++){
			VipsBlock childBlock=blockChildren.get(i);
			if(!childBlock.isImg() && !childBlock.getBox().getNode().getNodeName().equals("a")){
				block_plainTxtArea+=getSizeOfBlock(childBlock);
			}
		}*/
		if(block_plainTxtArea==0 && !block.isImg() && !block.getBox().getNode().getNodeName().equals("a"))
			block_plainTxtArea+=getSizeOfBlock(block);
		return block_plainTxtArea;
	}
	
	/*
	 * Get the fonts of plain text in a block
	 */
	public ArrayList<String> fonts_pt(VipsBlock block){
		List<VipsBlock> blockChildren=block.getChildren();
		ArrayList<String> distinctFonts=new ArrayList<String>();
		if(distinctFonts.size()==0 && !block.isImg() && !block.getBox().getNode().getNodeName().equals("a")){
			String fontAtt=getFontAttributes(block);
			distinctFonts.add(fontAtt);
		}
		return distinctFonts;
	}

	/*
	 * Weight of link text similarity
	 */
	public double wLT(VipsBlock blockA, VipsBlock blockB){
		return (double)(sa_lt(blockA)+sa_lt(blockB))/(sa_b(blockA)+sa_b(blockB));
	}
	
	/*
	 * Similarity based on link text font
	 */
	public double simLT(VipsBlock blockA, VipsBlock blockB){
		ArrayList<String> fonts_blockA=fonts_lt(blockA), fonts_blockB=fonts_lt(blockB);
		if(fonts_blockA.size()==0 || fonts_blockB.size()==0) return 0;	
		int sharedFonts=0;
		for(int i=0; i<fonts_blockA.size(); i++){
			String fontA=fonts_blockA.get(i);
			for(int j=0; j<fonts_blockB.size(); j++){
				String fontB=fonts_blockB.get(j);
				//if(fontA==fontB) sharedFonts++;
				if(fontA.compareToIgnoreCase(fontB)==0) sharedFonts++;
			}
		}
		double fn_lt_blockA=(double)sharedFonts/fonts_blockA.size(), fn_lt_blockB=(double)sharedFonts/fonts_blockB.size();
		if(fn_lt_blockA==0.0 && fn_lt_blockB==0.0) return 0 ;
		return (double)Math.min(fn_lt_blockA,fn_lt_blockB)/Math.max(fn_lt_blockA,fn_lt_blockB);
	}
	
	/*
	 * Total area of link text in a block
	 */
	public int sa_lt(VipsBlock block){
		List<VipsBlock> blockChildren=block.getChildren();
		int block_linkTxtArea=0;
		/*for(int i=0; i<blockChildren.size(); i++){
			VipsBlock childBlock=blockChildren.get(i);
			if(!childBlock.isImg() && childBlock.getBox().getNode().getNodeName().equals("a")){
				block_linkTxtArea+=getSizeOfBlock(childBlock);
			}
		}*/
		if(block_linkTxtArea==0 && !block.isImg() && block.getBox().getNode().getNodeName().equals("a"))
			block_linkTxtArea+=getSizeOfBlock(block);
		return block_linkTxtArea;
	}
	
	/*
	 * Get the fonts of link text in a block
	 */
	public ArrayList<String> fonts_lt(VipsBlock block){
		List<VipsBlock> blockChildren=block.getChildren();
		ArrayList<String> distinctFonts=new ArrayList<String>();
		if(distinctFonts.size()==0 && !block.isImg() && block.getBox().getNode().getNodeName().equals("a")){
			String fontAtt=getFontAttributes(block);
			distinctFonts.add(fontAtt);
		}
		return distinctFonts;
	}

	/*
	 * Get the total area of a block
	 */
	public int sa_b(VipsBlock block){
		return getSizeOfBlock(block);
	}
	
	/*
	 * Get the size of a block
	 */
	public int getSizeOfBlock(VipsBlock block){
		return block.getBox().getWidth()*block.getBox().getHeight();
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
	 * Blocks regrouping to form data records
	 */
	public ArrayList<ArrayList<VipsBlock>> blockRegrouping(ArrayList<ArrayList<VipsBlock>> clusters){
		ArrayList<Integer> sizes=new ArrayList<Integer>();
		ArrayList<Rectangle> rectangles=new ArrayList<Rectangle>();
		Rectangle rectangleMax=null;
		ArrayList<VipsBlock> Cmax = null;
		ArrayList<ArrayList<VipsBlock>> groups=new ArrayList<ArrayList<VipsBlock>>();
		
		for(int i=0; i<clusters.size(); i++){
			sizes.add(clusters.get(i).size());
		}
		int n=Collections.max(sizes);
		for(int i=0; i<clusters.size(); i++){
			ArrayList<VipsBlock> cluster=clusters.get(i);
			if(cluster.size()==n){
				Cmax=cluster;
				break;
			}
		}
		consoleOutput+="\n\n";
		for(int i=0; i<clusters.size(); i++){
			ArrayList<VipsBlock> cluster=clusters.get(i);
			ArrayList<Integer> lefts=new ArrayList<Integer>(), rights=new ArrayList<Integer>(), tops=new ArrayList<Integer>(), bottoms=new ArrayList<Integer>();
			for(int j=0; j<cluster.size(); j++){
				VipsBlock vipsBlock=cluster.get(j);
				lefts.add(vipsBlock.getBox().getAbsoluteContentX());
				rights.add(vipsBlock.getBox().getAbsoluteContentX()+vipsBlock.getBox().getWidth());
				tops.add(vipsBlock.getBox().getAbsoluteContentY());
				bottoms.add(vipsBlock.getBox().getAbsoluteContentY()+vipsBlock.getBox().getHeight());
			}
			int minLeft=Collections.min(lefts), maxRight=Collections.max(rights), minTop=Collections.min(tops), maxBottom=Collections.max(bottoms);
			consoleOutput+=(i+1)+":   clusterSize:"+cluster.size()+"   minLeft:"+minLeft+"   maxRight:"+maxRight+"   minTop:"+minTop+"   maxBottom:"+maxBottom;
			if(cluster==Cmax){
				consoleOutput+=" ===> Cmax";
			}
			consoleOutput+="\n";
			Rectangle rectangle=new Rectangle(minLeft,maxRight,minTop,maxBottom);
			rectangle.setCluster(cluster);
			rectangle.setId(i+1);
			rectangles.add(rectangle);
			if(cluster==Cmax)
				rectangleMax=rectangle;
		}
		
		for(int i=0; i<n; i++){
			VipsBlock seedVipsBlock=Cmax.get(i);
			ArrayList<VipsBlock> group=new ArrayList<VipsBlock>();
			group.add(seedVipsBlock);
			groups.add(group);
		}
		
		consoleOutput+="\nOverlaping ..\n";
		for(int i=0; i<rectangles.size(); i++){
			Rectangle rectangle=rectangles.get(i);
			if(rectangle!=rectangleMax){
				consoleOutput+=rectangle.getId()+"   isOverlap: "+isOverlap(rectangle,rectangleMax)+"   isAhead:"+isAhead(rectangle,rectangleMax)+"   isBehind:"+isBehind(rectangle,rectangleMax)+"\n";
			}
		}
		
		consoleOutput+="\n";
		for(int i=0; i<rectangles.size(); i++){
			Rectangle rectangle=rectangles.get(i);
			boolean ahead=false, behind=false;
			if(rectangle!=rectangleMax && isOverlap(rectangle,rectangleMax)){
				if(isAhead(rectangle,rectangleMax))
					ahead=true;
				if(!ahead)
					behind=true;
				ArrayList<VipsBlock> cluster=rectangle.getCluster();
				for(int j=0; j<cluster.size(); j++){
					VipsBlock vipsBlock=cluster.get(j);
					if(ahead){
						if(isTopAhead(rectangle,rectangleMax)){
							ArrayList<VipsBlock> blockList=getBehindBlocks_top(vipsBlock,Cmax);
							Collections.sort(blockList, VipsBlock.VipsBlockComparator);
							if(blockList.size()>0){
								VipsBlock matchedSeedBlock=blockList.get(0);
								addBlockToGroup(vipsBlock,groups,matchedSeedBlock);
								/*System.out.println(vipsBlock.getElementBox().getText()+" (behind blocks) ===> "+matchedSeedBlock.getElementBox().getText());
								for(int k=0; k<blockList.size(); k++)
									System.out.print(blockList.get(k).getElementBox().getText()+" __ ");
								System.out.println("\n");*/
							}
						}else{
							ArrayList<VipsBlock> blockList=getBehindBlocks_left(vipsBlock,Cmax);
							Collections.sort(blockList, VipsBlock.VipsBlockComparator);
							if(blockList.size()>0){
								VipsBlock matchedSeedBlock=blockList.get(0);
								addBlockToGroup(vipsBlock,groups,matchedSeedBlock);
								/*System.out.println(vipsBlock.getElementBox().getText()+" (behind blocks) ===> "+matchedSeedBlock.getElementBox().getText());
								for(int k=0; k<blockList.size(); k++)
									System.out.print(blockList.get(k).getElementBox().getText()+" __ ");
								System.out.println("\n");*/
							}
						}
					}else{
						if(isTopBehind(rectangle,rectangleMax)){
							ArrayList<VipsBlock> blockList=getAheadBlocks_top(vipsBlock,Cmax);
							Collections.sort(blockList, VipsBlock.VipsBlockComparator);
							if(blockList.size()>0){
								VipsBlock matchedSeedBlock=blockList.get(blockList.size()-1);
								addBlockToGroup(vipsBlock,groups,matchedSeedBlock);
								/*System.out.println(vipsBlock.getElementBox().getText()+" (behind blocks) ===> "+matchedSeedBlock.getElementBox().getText());
								for(int k=0; k<blockList.size(); k++)
									System.out.print(blockList.get(k).getElementBox().getText()+" __ ");
								System.out.println("\n");*/
							}
						}else{
							ArrayList<VipsBlock> blockList=getAheadBlocks_left(vipsBlock,Cmax);
							Collections.sort(blockList, VipsBlock.VipsBlockComparator);
							if(blockList.size()>0){
								VipsBlock matchedSeedBlock=blockList.get(blockList.size()-1);
								addBlockToGroup(vipsBlock,groups,matchedSeedBlock);
								/*System.out.println(vipsBlock.getElementBox().getText()+" (behind blocks) ===> "+matchedSeedBlock.getElementBox().getText());
								for(int k=0; k<blockList.size(); k++)
									System.out.print(blockList.get(k).getElementBox().getText()+" __ ");
								System.out.println("\n");*/
							}
						}
					}
						
				}
			}
		}
		return groups;
	}
	
	/*
	 * Adding a block to a group
	 */
	public void addBlockToGroup(VipsBlock vipsBlock,ArrayList<ArrayList<VipsBlock>> groups, VipsBlock seedVipsBlock){
		for(int i=0; i<groups.size(); i++){
			ArrayList<VipsBlock> group=groups.get(i);
			if(group.contains(seedVipsBlock)){
				group.add(vipsBlock);
				return;
			}
		}
	}
	
	/*
	 * Get the blocks that are behind (below) a specific block
	 */
	public ArrayList<VipsBlock> getBehindBlocks_top(VipsBlock vipsBlock, ArrayList<VipsBlock> cluster){
		ArrayList<VipsBlock> blockList=new ArrayList<VipsBlock>();
		for(int i=0; i<cluster.size(); i++){
			VipsBlock ClusterVipsBlock=cluster.get(i);
			if(vipsBlock.getBox().getAbsoluteContentY()<ClusterVipsBlock.getBox().getAbsoluteContentY()){
				blockList.add(ClusterVipsBlock);
			}
		}
		return blockList;
	}
	
	/*
	 * Get the blocks that are behind (on the right of) a specific block
	 */
	public ArrayList<VipsBlock> getBehindBlocks_left(VipsBlock vipsBlock, ArrayList<VipsBlock> cluster){
		ArrayList<VipsBlock> blockList=new ArrayList<VipsBlock>();
		for(int i=0; i<cluster.size(); i++){
			VipsBlock ClusterVipsBlock=cluster.get(i);
			if(vipsBlock.getBox().getAbsoluteContentX()<ClusterVipsBlock.getBox().getAbsoluteContentX()){
				blockList.add(ClusterVipsBlock);
			}
		}
		return blockList;
	}
	
	/*
	 * Get the blocks that are ahead (above) a specific block
	 */
	public ArrayList<VipsBlock> getAheadBlocks_top(VipsBlock vipsBlock, ArrayList<VipsBlock> cluster){
		ArrayList<VipsBlock> blockList=new ArrayList<VipsBlock>();
		for(int i=0; i<cluster.size(); i++){
			VipsBlock ClusterVipsBlock=cluster.get(i);
			if(vipsBlock.getBox().getAbsoluteContentY()>ClusterVipsBlock.getBox().getAbsoluteContentY()){
				blockList.add(ClusterVipsBlock);
			}
		}
		return blockList;
	}
	
	/*
	 * Get the blocks that are ahead (on the left of) a specific block
	 */
	public ArrayList<VipsBlock> getAheadBlocks_left(VipsBlock vipsBlock, ArrayList<VipsBlock> cluster){
		ArrayList<VipsBlock> blockList=new ArrayList<VipsBlock>();
		for(int i=0; i<cluster.size(); i++){
			VipsBlock ClusterVipsBlock=cluster.get(i);
			if(vipsBlock.getBox().getAbsoluteContentX()>ClusterVipsBlock.getBox().getAbsoluteContentX()){
				blockList.add(ClusterVipsBlock);
			}
		}
		return blockList;
	}
	
	/*
	 * Check if two rectangles are overlapping
	 */
	public boolean isOverlap(Rectangle rectangle1,Rectangle rectangle2){
		int rectangle1_Left=rectangle1.left, rectangle1_Right=rectangle1.right, rectangle1_Top=rectangle1.top, rectangle1_Bottom=rectangle1.bottom,
			rectangle2_Left=rectangle2.left, rectangle2_Right=rectangle2.right, rectangle2_Top=rectangle2.top, rectangle2_Bottom=rectangle2.bottom;	
		if(rectangle1_Left>rectangle2_Left && rectangle1_Left<rectangle2_Right || 
		   rectangle1_Right>rectangle2_Left && rectangle1_Right<rectangle2_Right || 
		   rectangle1_Top>rectangle2_Top && rectangle1_Top<rectangle2_Bottom ||
		   rectangle1_Bottom>rectangle2_Top && rectangle1_Bottom<rectangle2_Bottom)
			return true;
		return false;
	}

	/*
	 * Check if a rectangle is ahead of another rectangle
	 */
	public boolean isAhead(Rectangle rectangle1,Rectangle rectangle2){
		int rectangle1_Left=rectangle1.left, rectangle1_Top=rectangle1.top,
			rectangle2_Left=rectangle2.left, rectangle2_Top=rectangle2.top;	
		if(rectangle1_Top<rectangle2_Top || rectangle1_Left<rectangle2_Left)
			return true;
		return false;
	}
	
	/*
	 * Check if a rectangle is ahead of (above) another rectangle
	 */
	public boolean isTopAhead(Rectangle rectangle1,Rectangle rectangle2){
		if(rectangle1.top<rectangle2.top)
			return true;
		return false;
	}
	
	/*
	 * Check if a rectangle is ahead (on the left) of another rectangle
	 */
	public boolean isLeftAhead(Rectangle rectangle1,Rectangle rectangle2){
		if(rectangle1.left<rectangle2.left)
			return true;
		return false;
	}

	/*
	 * Check if a rectangle is behind another rectangle
	 */
	public boolean isBehind(Rectangle rectangle1,Rectangle rectangle2){
		int rectangle1_Left=rectangle1.left, rectangle1_Top=rectangle1.top,
			rectangle2_Left=rectangle2.left, rectangle2_Top=rectangle2.top;	
		if(rectangle1_Top>rectangle2_Top || rectangle1_Left>rectangle2_Left)
			return true;
		return false;
	}
	
	/*
	 * Check if a rectangle is behind (below) another rectangle
	 */
	public boolean isTopBehind(Rectangle rectangle1,Rectangle rectangle2){
		if(rectangle1.top>rectangle2.top)
			return true;
		return false;
	}
	
	/*
	 * Check if a rectangle is behind (on the right) of another rectangle
	 */
	public boolean isLeftBehind(Rectangle rectangle1,Rectangle rectangle2){
		if(rectangle1.left>rectangle2.left)
			return true;
		return false;
	}

	/*
	 * Get the minimum of array list of integers
	 */
	public int getMin(ArrayList<Integer> list){
		int min=list.get(0);
		for(int i: list) {
		    if(i < min) min = i;
		}
		return min;
	}
	
}

/*
 * Rectangle class
 */
class Rectangle {
	int left=0, right=0, top=0, bottom=0, id=0;
	ArrayList<VipsBlock> cluster=null;
	public Rectangle(int left, int right, int top, int bottom){
		this.left=left;
		this.right=right;
		this.top=top;
		this.bottom=bottom;
	}
	public void setCluster(ArrayList<VipsBlock> cluster){
		this.cluster=cluster;
	}
	public ArrayList<VipsBlock> getCluster(){
		return this.cluster;
	}
	public void setId(int id){
		this.id=id;
	}
	public int getId(){
		return this.id;
	}
}