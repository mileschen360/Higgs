package RestAPI;

import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeSet;

public class Logger {
	private class Pair {
		String mname;
		Double time;

		Pair(String s, Double t) {
			mname = s;
			time = t;
		}
	}

	private int flag;
	private int accessTimeFlag;
	private int cacheHitFlag;
	HashMap<String, Double> timeMap;
	HashMap<String, Integer> callCount;

	public Logger() {
		flag = 1;
		accessTimeFlag = 1;
		cacheHitFlag = 1;
		timeMap = new HashMap<String, Double>();
		callCount = new HashMap<String, Integer>();
	}

	public void disableAll() {
		flag = 0;
	}

	public void disableAccessTimes() {
		accessTimeFlag = 0;
	}

	public void disableCacheHit() {
		cacheHitFlag = 0;
	}

	public void enable() {
		flag = 1;
		accessTimeFlag = 1;
	}

	public void printAccessTime(String methodName, String meta, long end, long start) {
		if (flag == 1 && accessTimeFlag == 1) {
			double time = (double) (end - start) / (1000000000);
			System.out.println(methodName + " - " + meta + " : " + String.valueOf(time));
			if (timeMap.containsKey(methodName)) {
				timeMap.put(methodName, timeMap.get(methodName) + time);
				callCount.put(methodName, callCount.get(methodName) + 1);
			} else {
				timeMap.put(methodName, time);
				callCount.put(methodName, 1);
			}
		}

	}

	public void printIfCacheHit(String s) {
		if (flag == 1 && cacheHitFlag == 1)
			System.out.println(s);
	}

	public void printMap() {
		Comparator<Pair> comparator = new Comparator<Pair>() {
			@Override
			public int compare(Pair o1, Pair o2) {
				if (o1.time > o2.time)
					return 1;
				else
					return -1;
			}
		};
		TreeSet<Pair> set = new TreeSet<Pair>(comparator);
		for (String key : timeMap.keySet()) {
			Pair p = new Pair(key, timeMap.get(key));
			set.add(p);
		}
		for (Pair p : set) {
			System.out.println(p.mname + " : " + p.time + " - " + callCount.get(p.mname));
		}
	}

}