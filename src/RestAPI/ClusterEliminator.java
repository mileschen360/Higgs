package RestAPI;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import Node.NodeJSON;

public class ClusterEliminator {

	String collisionsDetectedFile;
	String originalClassesFile;
	HashSet<HashSet<String>> clustersSet; // A hashset of hashsets of clusters.
	HashMap<String, HashSet<String>> clustersMap; // A mapping from shortname to
													// clusters
	HashMap<String, String> originalClassMap; // A mapping from shortname to the
												// right package (if a cluster)
	HashMap<String, String> finalMap;

	public ClusterEliminator(String arg1, String arg2) throws IOException {
		collisionsDetectedFile = arg1;
		originalClassesFile = arg2;
		getClustersMap();
		getOriginalClassesMap();
	}

	// Check if it is really a cluster before calling this.
	public NodeJSON findRightClass(Set<NodeJSON> set) {
		String shortName = "";
		if (set != null) {
			for (NodeJSON node : set) {
				shortName = (String) node.getProperty("exactName");
				break;
			}
		}
		if (shortName != null) {
			String rightPackage = findRightPackage(shortName);
			if (rightPackage != null) {
				for (NodeJSON node : set) {
					String clusterCandidate = (String) node.getProperty("id");
					if (clusterCandidate.contains(".")) {
						String clusterPackage = clusterCandidate.substring(0, clusterCandidate.lastIndexOf("."));
						if (rightPackage.indexOf(clusterPackage) != -1)
							return node;
					}
				}
			}
		}
		return null;
	}

	public String findRightPackage(String shortName) {
		if (originalClassMap.containsKey(shortName))
			return originalClassMap.get(shortName);
		else
			return null;
	}

	/*
	 * public static void main(String[] args) throws IOException {
	 * ClusterEliminator celim = new
	 * ClusterEliminator("class-collisions_update.txt", "forReid.txt");
	 * 
	 * 
	 * //celim.test(); //HashSet<String> cluster =
	 * celim.clustersMap.get("Iterable");
	 * 
	 * }
	 */

	public boolean checkIfCluster(Set<NodeJSON> set) {
		String shortName = "";
		for (NodeJSON node : set) {
			String s = (String) node.getProperty("id");
			if (s.contains(".")) {
				String arr[] = s.split("\\.");
				shortName = arr[arr.length - 1];
				break;
			}
		}

		if (clustersMap.containsKey(shortName)) {
			HashSet<String> clusterSet = clustersMap.get(shortName);
			int yc = 0, nc = 0;
			for (NodeJSON node : set) {
				String s = (String) node.getProperty("id");
				if (clusterSet.contains(s) == false) {
					nc++;
				} else
					yc++;
			}
			if (yc >= set.size() - 3)
				return true;
			else
				return false;
		} else
			return false;

		/*
		 * if(clustersSet.contains(set)) return true; else return false;
		 */
	}

	/*
	 * public void test() { int yc = 0, nc = 0; for(String shortName :
	 * clustersMap.keySet()) { if(originalClassMap.containsKey(shortName)) {
	 * System.out.println(shortName); String x = this.findRightClass(shortName);
	 * if(x!= null) yc++; else nc++; System.out.println("-- "+ x); } else nc++;
	 * 
	 * } //System.out.println(clustersMap.values().size()); System.out.println(
	 * "yes: " + yc + " nc : " + nc);
	 * System.out.println(originalClassMap.keySet().size()); }
	 */

	public void getOriginalClassesMap() throws IOException {
		HashSet<String> insertedKeys = new HashSet<String>();
		originalClassMap = new HashMap<String, String>();
		BufferedReader br = new BufferedReader(new FileReader(originalClassesFile));
		String s = "";
		while ((s = br.readLine()) != null) {
			String[] arr = s.split(";");
			if (arr.length == 3) {
				String className = arr[1].replace(" ", "");
				String packageName = new String();
				if (!arr[0].isEmpty()) {
					packageName = arr[0].replace(" ", "").replace('/', '.').substring(1);
				}
				if (!insertedKeys.contains(className)) {
					originalClassMap.put(className, packageName);
					insertedKeys.add(className);
				} else {
					if (originalClassMap.containsKey(className))
						originalClassMap.remove(className);
				}
			}
		}
		br.close();
	}

	public void getClustersMap() throws IOException {
		clustersMap = new HashMap<String, HashSet<String>>();
		clustersSet = new HashSet<HashSet<String>>();
		BufferedReader br = new BufferedReader(new FileReader(collisionsDetectedFile));
		String s = "";
		String classShortName = new String();
		HashSet<String> set = new HashSet<String>();
		while ((s = br.readLine()) != null) {
			String line = s;
			if (s.startsWith("  -")) {

				line = s.substring(5);
				// line =
				// line.substring(0,(line.lastIndexOf(':')-1)).replace('$',
				// '.');
				line = line.substring(0, (line.lastIndexOf(':') - 1));
				set.add(line);
			} else {
				if (!classShortName.isEmpty()) {
					clustersSet.add(set);
					clustersMap.put(classShortName, set);
					set = new HashSet<String>();
				}
				// line = line.substring(0,
				// line.lastIndexOf(':')-1).replace('$', '.');
				line = line.substring(0, line.lastIndexOf(':') - 1);
				classShortName = line;

			}
		}
		clustersMap.put(classShortName, set);
		clustersSet.add(set);
		br.close();
	}

}