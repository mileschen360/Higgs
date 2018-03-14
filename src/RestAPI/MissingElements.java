package RestAPI;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

class MissingElements {
	public static void main(String[] args) throws IOException {
		String fname = "/home/s23subra/Downloads/all.sorted.unique";

		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line;
		HashSet<String> setmethods = new HashSet<String>();
		HashSet<String> setclasses = new HashSet<String>();
		while ((line = br.readLine()) != null) {
			String[] arr = line.split(";");
			if (arr[0].equals("method")) {
				setmethods.add(arr[2] + "." + arr[1]);
				// setmethods.add(arr[2]);
			} else if (arr[0].equals("class") || arr[0].equals("interface")) {
				if (!setclasses.contains(arr[1]))
					setclasses.add(arr[1]);
				else
					System.out.println(arr[1]);
			}
		}

		System.out.println(setclasses.size());
		System.out.println(setmethods.size());
	}
}