package RestAPI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.jdt.core.dom.ASTNode;
import org.json.JSONObject;

import com.google.common.collect.HashMultimap;

import Node.NodeJSON;

public class ThreadedMethodReturnFetch implements Runnable {
	private ASTNode treeNode;
	private GraphServerAccess model;
	private NodeJSON methodNode;
	private HashMap<NodeJSON, NodeJSON> methodReturnCache;
	private HashMultimap<ArrayList<Integer>, NodeJSON> candidateAccumulator;
	private ArrayList<Integer> scopeArray;
	private HashSet<Integer> interestingNodes;

	public ThreadedMethodReturnFetch(NodeJSON candidateMethodNode, HashMap<NodeJSON, NodeJSON> methodReturnCache,
			HashMultimap<ArrayList<Integer>, NodeJSON> candidateAccumulator, ArrayList<Integer> scopeArray,
			GraphServerAccess graphModel, ASTNode treeNode) {
		this.methodNode = candidateMethodNode;
		this.methodReturnCache = methodReturnCache;
		this.candidateAccumulator = candidateAccumulator;
		this.scopeArray = scopeArray;
		this.model = graphModel;
		this.treeNode = treeNode;
		interestingNodes = new HashSet<Integer>();
		interestingNodes.add(7); // assignment
		interestingNodes.add(32); // methodinvocation
		interestingNodes.add(59); // VARIABLE_DECLARATION_FRAGMENT
		interestingNodes.add(42); // SIMPLE_NAME
		interestingNodes.add(23); // FIELD_DECLARATION
		// interestingNodes.add(21); //method parameter(expression_statement)
	}

	@Override
	/*
	 * original run method
	 */

	public void run() {
		Integer parentType = treeNode.getParent().getNodeType();
		if (!interestingNodes.contains(parentType)) {
			JSONObject data = new JSONObject();
			data.put("id", "MyRTPlaceHolderP.MyRTPlaceHolderEN");
			data.put("exactName", "MyRTPlaceHolderEN");
			JSONObject obj = new JSONObject();
			obj.put("data", data);

			NodeJSON retElement = new NodeJSON(obj);
			candidateAccumulator.put(scopeArray, retElement);
			methodReturnCache.put(methodNode, retElement);
		} else {
			NodeJSON retElement = model.getMethodReturn(methodNode, methodReturnCache);
			if (retElement != null) {
				candidateAccumulator.put(scopeArray, retElement);
			}
		}

	}

	/*
	 * public void run() { extractClassId(methodNode.getProperty("id"));
	 * extractExactClassName(methodNode.getProperty("id")); JSONObject data =
	 * new JSONObject(); data.put("id", classId); data.put("exactName",
	 * exactClassName); JSONObject obj = new JSONObject(); obj.put("data",
	 * data);
	 * 
	 * NodeJSON fcname = new NodeJSON(obj); if(fcname!=null &&
	 * replacementClassNodesList!=null) replacementClassNodesList.add(fcname); }
	 */
}
