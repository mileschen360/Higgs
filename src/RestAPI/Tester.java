package RestAPI;

import java.io.IOException;
import java.util.HashMap;

import org.neo4j.kernel.StoreLockException;

import Node.IndexHits;
import Node.NodeJSON;

public class Tester {
	private static GraphServerAccess graphDb;
	private static final String DB_URI = "http://localhost:7474/db/data";
//	private static final String DB_URI = "http://neo4j:neo4j@localhost:7474/user/neo4j";

	public static void main(String[] args) throws StoreLockException, IOException {
		graphDb = new GraphServerAccess(DB_URI);
		long startTime = System.nanoTime();
		IndexHits<NodeJSON> classes = graphDb.getCandidateClassNodes("*", new HashMap<String, IndexHits<NodeJSON>>());
		for (final NodeJSON classNode : classes) {
			System.out.println(classNode.getProperty("id"));
		}

		long endTime = System.nanoTime();
		double time = (double) (endTime - startTime) / (1000000000);
		System.out.println(time);

	}

	public void run() {

	}
}