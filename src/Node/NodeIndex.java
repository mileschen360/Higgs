package Node;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class NodeIndex {
	public String URI;

	public NodeIndex(String URIString) {
		URI = URIString;
	}

	public JSONArray getJSONArray(String elementName) {

		WebResource resource = null;
		try {
			resource = Client.create().resource(URI + URLEncoder.encode(elementName, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		ClientResponse response = resource.accept("application/json").get(ClientResponse.class);
		String jsonString = response.getEntity(String.class);
		JSONArray jsonArray = null;
		try {
			jsonArray = new JSONArray(jsonString);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		response.close();
		return jsonArray;
	}

	public IndexHits<NodeJSON> get(String elementName) {
		IndexHits<NodeJSON> list = new IndexHits<NodeJSON>();

		JSONArray jsonArray = getJSONArray(elementName);
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject obj = jsonArray.getJSONObject(i);
			NodeJSON node = new NodeJSON(obj);
			list.add(node);
		}
		return list;
	}

	public IndexHits<NodeJSON> query(String string, String name) {

		return null;
	}

}
