package RestAPI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jdt.core.dom.MethodInvocation;

import Node.NodeJSON;

public class ThreadedParentFetch implements Runnable {

	private NodeJSON candidateClassNode;
	private MethodInvocation treeNode;
	private GraphServerAccess model;
	private HashMap<String, ArrayList<NodeJSON>> parentNodeCache;
	private List<NodeJSON> candidateParentNodes;

	public ThreadedParentFetch(NodeJSON candidateClassNode, MethodInvocation treeNode,
			List<NodeJSON> candidateParentNodes, HashMap<String, ArrayList<NodeJSON>> parentNodeCache,
			GraphServerAccess graphModel) {
		this.candidateClassNode = candidateClassNode;
		this.treeNode = treeNode;
		this.parentNodeCache = parentNodeCache;
		this.candidateParentNodes = candidateParentNodes;
		this.model = graphModel;
	}

	@Override
	public void run() {
		// ArrayList<NodeJSON> parentNodeList = new ArrayList<NodeJSON>();
		ArrayList<NodeJSON> parentNodeList = model.getParents(candidateClassNode, parentNodeCache);
		candidateParentNodes.addAll(parentNodeList);
	}
}
