package RestAPI;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.Traversal;

import com.google.common.collect.HashMultimap;

public class ClusterGenerator {
	private static GraphDatabaseService graphDb;
	private static final String DB_PATH = "maven-graph-database";
	public static Index<Node> classIndex;
	public static Index<Node> methodIndex;
	public static Index<Node> fieldIndex;

	public static Index<Node> shortClassIndex;
	public static Index<Node> shortMethodIndex;
	public static Index<Node> shortFieldIndex;
	public static Index<Node> parentIndex;

	/*
	 * public static String getIdWithoutArgs(String name) //to store class name
	 * and exact method name as ivars { String[] array = name.split("\\(");
	 * return array[0]; }
	 */

	public static String getPackage(String id, String cname) // to store class
																// name and
																// exact method
																// name as ivars
	{
		String packageName = null;
		if (id.contains(cname)) {
			cname = Pattern.quote(cname);
			String array[] = id.split(cname);
			packageName = array[0].substring(0, array[0].length() - 1);
		}
		return packageName;
	}

	public static String getClassId(String id) // to store class name and exact
												// method name as ivars
	{
		String _className = null;
		if (id.endsWith("<clinit>")) {
			String array[] = id.split(".<clinit>");
			_className = array[0];
		} else {
			String[] array = id.split("\\(");
			array = array[0].split("\\.");
			String className = array[0];
			for (int i = 1; i < array.length - 1; i++)
				className += "." + array[i];
			_className = className;
		}
		return _className;

	}

	public static String getExactNameClass(String id) {
		String name = id;
		int i;
		for (i = 0; i < name.length(); i++) {
			if (Character.isUpperCase(name.charAt(i))) {
				return id.substring(i);
			}
		}
		return id;
	}

	private static enum RelTypes implements RelationshipType {
		PARENT, CHILD, IS_METHOD, HAS_METHOD, IS_FIELD, HAS_FIELD, RETURN_TYPE, IS_RETURN_TYPE, PARAMETER, IS_PARAMETER, IS_FIELD_TYPE, HAS_FIELD_TYPE
	}

	public static TreeSet<String> findClassClusters(Set<Node> exactClassNameNodeList) {
		TreeSet<String> answerList = new TreeSet<String>();
		int size = exactClassNameNodeList.size();
		HashMap<String, TreeSet<String>> counter = new HashMap<String, TreeSet<String>>();
		HashMultimap<String, String> cache = HashMultimap.create();
		for (Node exactClassNameNode : exactClassNameNodeList) {
			// String exactClassName = (String)
			// exactClassNameNode.getProperty("exactName");
			HashSet<Node> methodNodeList = getMethodNodes(exactClassNameNode);
			for (Node methodNode : methodNodeList) {
				String idWithArgs = (String) methodNode.getProperty("id");
				String exactMethodName = (String) methodNode.getProperty("exactName");
				if (exactMethodName.equals("<init>") == false) {
					cache.put((String) exactClassNameNode.getProperty("id"), exactMethodName); // cache
																								// the
																								// values
																								// to
																								// avoid
																								// reading
																								// from
																								// the
																								// graph
																								// again
					String containerClass = getClassId(idWithArgs);
					if (counter.containsKey(exactMethodName)) {
						TreeSet<String> temp = counter.get(exactMethodName);
						temp.add(containerClass);
						counter.put(exactMethodName, temp);
					} else {
						TreeSet<String> temp = new TreeSet<String>();
						temp.add(containerClass);
						counter.put(exactMethodName, temp);
					}
				}
			}
		}
		Set<String> classKeys = cache.keySet();
		for (String key : classKeys) {
			Set<String> values = cache.get(key);
			int count = 0;
			for (String value : values) {
				TreeSet<String> candidates = counter.get(value);
				if (candidates.size() > size / 2) {
					count++;
					// System.out.println("------ "+value + " : " +candidates);
				}
			}
			if (count > values.size() / 2) {
				// answerList.add(key+" : ("+count+"/"+values.size()+")");
				answerList.add(key);
			}
		}

		return answerList;

	}

	public static void addToCluster(String test, HashMap<String, HashSet<String>> packageCluster) {

	}

	public static void main(String[] args) throws IOException {
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(DB_PATH);
		classIndex = graphDb.index().forNodes("classes");
		methodIndex = graphDb.index().forNodes("methods");
		fieldIndex = graphDb.index().forNodes("fields");

		shortClassIndex = graphDb.index().forNodes("short_classes");
		shortMethodIndex = graphDb.index().forNodes("short_methods");
		shortFieldIndex = graphDb.index().forNodes("short_fields");
		parentIndex = graphDb.index().forNodes("parents");

		registerShutdownHook();
		// BufferedWriter br = new BufferedWriter(new
		// FileWriter("class-collisions_update.txt"));

		Transaction tx2 = graphDb.beginTx();
		try {

			/*
			 * classIndex : 1646650 shortClassIndex : 1121887 methodIndex :
			 * 14206944 shortMethodIndex : 1600053 fieldIndex : 3149206
			 * shortFieldIndex : 1115099
			 */
			HashMultimap<String, Node> nodesWithSameExactName = HashMultimap.create();
			HashMap<String, HashSet<String>> packageCluster = new HashMap<String, HashSet<String>>();
			IndexHits<Node> indices = classIndex.query("id", "*");
			System.out.println("Number of distinct class names: " + indices.size());

			for (Node node : indices) {
				String shortName = (String) node.getProperty("exactName");
				nodesWithSameExactName.put(shortName, node);
			}
			Set<String> distinctExactNames = nodesWithSameExactName.keySet();
			TreeSet<String> distinctExactNamesTree = new TreeSet<String>();
			for (String s : distinctExactNames)
				distinctExactNamesTree.add(s);
			System.out.println("Number of distinct short class names: " + distinctExactNamesTree.size());

			int counter1 = 0;
			int counter2 = 0;
			for (String name : distinctExactNamesTree) {
				Set<Node> list = nodesWithSameExactName.get(name);
				if (list.size() >= 3) {
					TreeSet<String> methodnames = findClassClusters(list);
					counter2++;
					if (methodnames.size() > 1) {
						System.out.println(name);
						for (String methodname : methodnames) {
							String packageName = getPackage(methodname, name);
							System.out.println("--- " + packageName);
						}
						counter1++;
						// br.write(toWrite);
					}
				}
			}
			System.out.println("Number of short class name clusters with more than 3 candidate classes: " + counter2);
			System.out.println(
					"Number of short class names that have at least one method that occurs in more than 50% of its candidate classes: "
							+ counter1 + "\n(Only considering short class names that have at least 3 candidates)");
			tx2.success();
			// br.close();
			/*
			 * Number of distinct class names: 1646650 Number of distinct short
			 * class names: 1121887 Number of short class names with more than 3
			 * candidate classes: 71461 Number of short class names that have at
			 * least one method that occurs in more than 50% of its candidate
			 * classes: 44239 (Only considering short class names that have at
			 * least 3 candidates)
			 */
		} finally {
			tx2.finish();
		}
		shutdown();
	}

	private static HashSet<Node> getMethodNodes(Node node) {
		TraversalDescription td = Traversal.description().breadthFirst()
				.relationships(RelTypes.HAS_METHOD, Direction.OUTGOING).evaluator(Evaluators.excludeStartPosition());
		Traverser methodTraverser = td.traverse(node);
		HashSet<Node> methodsCollection = new HashSet<Node>();
		;
		for (Path methods : methodTraverser) {
			if (methods.length() == 1) {
				methodsCollection.add(methods.endNode());
			} else if (methods.length() >= 1) {
				break;
			}
		}
		return methodsCollection;
	}

	private static void shutdown() {
		graphDb.shutdown();
	}

	private static void registerShutdownHook() {
		// Registers a shutdown hook for the Neo4j and index service instances
		// so that it shuts down nicely when the VM exits (even if you
		// "Ctrl-C" the running example before it's completed)
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				shutdown();
			}
		});
	}
}