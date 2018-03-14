package RestAPI;

import java.util.HashMap;

import Node.IndexHits;
import Node.NodeJSON;

public class ThreadedSuperClassHelper implements Runnable {
	private GraphServerAccess model;
	private String candidateSuperClass;
	private HashMap<String, IndexHits<NodeJSON>> candidateClassNodesCache;

	public ThreadedSuperClassHelper(String classNameString,
			HashMap<String, IndexHits<NodeJSON>> candidateClassNodesCache, GraphServerAccess graphModel) {
		this.candidateClassNodesCache = candidateClassNodesCache;
		model = graphModel;
	}

	@Override
	public void run() {

	}
}
