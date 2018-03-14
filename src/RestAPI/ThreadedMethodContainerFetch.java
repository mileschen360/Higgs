package RestAPI;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import Node.NodeJSON;

public class ThreadedMethodContainerFetch implements Runnable {
	private GraphServerAccess model;
	private NodeJSON methodNode;
	private HashMap<NodeJSON, NodeJSON> methodContainerCache;
	private List<NodeJSON> replacementClassNodesList;
	private String exactClassName;
	private String classId;

	public ThreadedMethodContainerFetch(NodeJSON candidateMethodNode, HashMap<NodeJSON, NodeJSON> methodContainerCache2,
			List<NodeJSON> methodContainerList, GraphServerAccess graphModel) {
		this.methodNode = candidateMethodNode;
		this.methodContainerCache = methodContainerCache2;
		this.replacementClassNodesList = methodContainerList;
		this.model = graphModel;
	}

	public void extractClassId(String methodId) // to store class name and exact
												// method name as ivars
	{
		if (methodId.endsWith("<clinit>")) {
			String array[] = methodId.split(".<clinit>");
			classId = array[0];
		} else {
			String[] array = methodId.split("\\(");
			array = array[0].split("\\.");
			String className = array[0];
			for (int i = 1; i < array.length - 1; i++)
				className += "." + array[i];
			classId = className;
		}
	}

	public void extractExactClassName(String methodId) // to store class name
														// and exact method name
														// as ivars
	{
		if (methodId.endsWith("<clinit>")) {
			exactClassName = "<clinit>";
		} else {
			String[] array = methodId.split("\\(");
			array = array[0].split("\\.");
			exactClassName = array[array.length - 1];
		}
	}

	@Override
	/*
	 * Real version
	 */
	/*
	 * public void run() { NodeJSON fcname =
	 * model.getMethodContainer(methodNode, methodContainerCache);
	 * if(fcname!=null) replacementClassNodesList.add(fcname); }
	 */

	public void run() {
		if (methodContainerCache.containsKey(methodNode)) {
			NodeJSON containerNode = methodContainerCache.get(methodNode);
			replacementClassNodesList.add(containerNode);
		} else {
			extractClassId(methodNode.getProperty("id"));
			extractExactClassName(methodNode.getProperty("id"));
			JSONObject data = new JSONObject();
			data.put("id", classId);
			data.put("exactName", exactClassName);
			JSONObject obj = new JSONObject();
			obj.put("data", data);

			NodeJSON containerNode = new NodeJSON(obj);
			if (containerNode != null && replacementClassNodesList != null) {
				replacementClassNodesList.add(containerNode);
				methodContainerCache.put(methodNode, containerNode);
			}
		}
	}
}
