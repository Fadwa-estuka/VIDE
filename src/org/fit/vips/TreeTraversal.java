package org.fit.vips;

import java.util.ArrayList;

import org.w3c.dom.Element;

public class TreeTraversal {
	
	private VisualStructure rootVisualStructure=null;
	private ArrayList<VisualStructure> visualStructureQueue = new ArrayList();
	private int order = 1;
	
	public TreeTraversal(VisualStructure visualStructure) {
		this.rootVisualStructure=visualStructure;
	}

	public ArrayList<VisualStructure> getVisualStructureQueue() {
		return visualStructureQueue;
	}
	
	public void traverse(VisualStructure visualStructure){
		visualStructureQueue.add(visualStructure);
		for(VisualStructure child : visualStructure.getChildrenVisualStructures())
			traverse(child);
	}

}
