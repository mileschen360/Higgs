package RestAPI;

import java.util.HashMap;

import Node.IndexHits;
import Node.NodeJSON;

public class ThreadedClassFetch implements Runnable {
	private GraphServerAccess model;
	private String className;
	private HashMap<String, IndexHits<NodeJSON>> candidateClassNodesCache;

	public ThreadedClassFetch(String classNameString, HashMap<String, IndexHits<NodeJSON>> candidateClassNodesCache,
			GraphServerAccess graphModel) {
		className = classNameString;
		this.candidateClassNodesCache = candidateClassNodesCache;
		model = graphModel;
	}

	@Override
	public void run() {
		model.getCandidateClassNodes(className, candidateClassNodesCache);
	}
}
