package RestAPI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import Node.IndexHits;
import Node.NodeJSON;

public class ThreadedMethodsInClassFetch implements Runnable {

	private NodeJSON candidateClassNode;
	private String treeNodeName;
	private GraphServerAccess model;
	private HashMap<String, IndexHits<NodeJSON>> candidateMethodNodesCache;
	private List<NodeJSON> candidateMethodNodes;
	private HashMap<NodeJSON, NodeJSON> methodContainerCache;

	public ThreadedMethodsInClassFetch(NodeJSON candidateClassNode, String treeNodeName,
			List<NodeJSON> candidateMethodNodes, HashMap<String, IndexHits<NodeJSON>> candidateMethodNodesCache,
			HashMap<NodeJSON, NodeJSON> methodContainerCache, GraphServerAccess graphModel) {
		this.candidateClassNode = candidateClassNode;
		this.treeNodeName = treeNodeName;
		this.candidateMethodNodesCache = candidateMethodNodesCache;
		this.model = graphModel;
		this.candidateMethodNodes = candidateMethodNodes;
		this.methodContainerCache = methodContainerCache;
	}

	@Override
	public void run() {
		ArrayList<NodeJSON> candidateMethods = model.getMethodNodesInClassNode(candidateClassNode, treeNodeName,
				candidateMethodNodesCache);
		for (NodeJSON candidateMethod : candidateMethods) {
			synchronized (candidateMethodNodes) {
				candidateMethodNodes.add(candidateMethod);
			}
			methodContainerCache.put(candidateMethod, candidateClassNode);
		}
	}
}
