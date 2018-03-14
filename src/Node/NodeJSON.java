package Node;

import org.json.JSONObject;

/*
 * 
 * Represents a generic Graph Node in the Graph DB. Can represent a class, method, field, method parameter, 
 * method return type, etc.
 * 
 */

public class NodeJSON {

	private JSONObject data;

	public JSONObject getJSONObject() {
		return data;
	}

	public NodeJSON(JSONObject obj) {
		data = obj;
	}

	/*
	 * public Integer getNodeNumber() { //Identify Node Id (neo4j automatically
	 * assigns every node an Id) based on it being used in URLs in the data.
	 * 
	 * if(data.has("indexed")) { String[] temp = ((String)
	 * data.get("indexed")).split("/"); String nodeNumber = temp[temp.length-1];
	 * return Integer.valueOf(nodeNumber); } else { String[] temp = ((String)
	 * data.get("self")).split("/"); String nodeNumber = temp[temp.length-1];
	 * return Integer.valueOf(nodeNumber); } }
	 */
	public String getProperty(String prop) {
		JSONObject obj = data.getJSONObject("data");
		// String property = obj.getString(prop).replace('$', '.');
		String property = obj.getString(prop);
		return property;
	}
}
