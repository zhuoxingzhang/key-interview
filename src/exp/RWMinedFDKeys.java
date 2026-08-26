package exp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import entity.FD;
import entity.Key;
import util.Utils;

/**
 * Compare all eight interview strategies of the two families on real-world
 * datasets whose FDs were mined from data (FD directory of the project).
 * The mined FDs are not meaningful, but the set of all minimal keys derived
 * from them serves as the answering oracle: a question set is a key iff it
 * contains some minimal key, exactly as in the other ground-truth experiments.
 * Recorded per run: number of questions answered as key ("No" answers) and
 * total number of questions.
 *
 * Usage:
 *   prepare <dataset> <fdRoot> <cacheDir>            compute + cache minimal keys
 *   run <dataset> <strategy> <fdRoot> <cacheDir>     run one strategy, print RESULT line
 *   (no args)                                        run everything inline
 */
public class RWMinedFDKeys {

	/** The data sets in ascending schema size, mapped to their mined-FD files. */
	public static Map<String, String> datasetFiles() {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("abalone", "FD on Complete/FD/abalone.json");
		m.put("routes", "FD on Incomplete/FD on NULL UNCERTAINTY/FD/routes.json");
		m.put("breast", "FD on Incomplete/FD on NULL UNCERTAINTY/FD/breast.json");
		m.put("echo", "FD on Incomplete/FD on NULL UNCERTAINTY/FD/echo.json");
		m.put("bridges", "FD on Incomplete/FD on NULL UNCERTAINTY/FD/bridges.json");
		m.put("claims", "FD on Incomplete/FD on NULL UNCERTAINTY/FD/claims.json");
		m.put("pdbx", "FD on Incomplete/FD on NULL UNCERTAINTY/FD/pdbx.json");
		m.put("adult", "FD on Complete/FD/adult.json");
		m.put("hospital", "FD on Incomplete/FD on NULL UNCERTAINTY/FD/hospital.json");
		m.put("lineitem", "FD on Complete/FD/lineitem.json");
		m.put("weather", "FD on Incomplete/FD on NULL UNCERTAINTY/FD/china_weather.json");
		m.put("ncvoter", "FD on Incomplete/FD on NULL UNCERTAINTY/FD/ncvoter.json");
		return m;
	}

	/** minimal JSON reader for the mined-FD format {"R":n,"fds":[{"lhs":[..],"rhs":[..]},..]} */
	public static List<Object> readMinedFDs(Path file) throws IOException {
		String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
		Matcher rM = Pattern.compile("\"R\"\\s*:\\s*(\\d+)").matcher(text);
		if (!rM.find())
			throw new IOException("no R field in " + file);
		int n = Integer.parseInt(rM.group(1));
		List<String> R = new ArrayList<>();
		for (int i = 0; i < n; i++)
			R.add(String.valueOf(i));

		List<FD> fds = new ArrayList<>();
		Matcher fdM = Pattern.compile(
				"\\\"lhs\\\"\\s*:\\s*\\[([^\\]]*)\\]\\s*,\\s*\\\"rhs\\\"\\s*:\\s*\\[([^\\]]*)\\]",
				Pattern.DOTALL).matcher(text);
		while (fdM.find()) {
			fds.add(new FD(parseIntList(fdM.group(1)), parseIntList(fdM.group(2))));
		}
		List<Object> res = new ArrayList<>();
		res.add(R);
		res.add(fds);
		return res;
	}

	private static List<String> parseIntList(String s) {
		List<String> attrs = new ArrayList<>();
		for (String tok : s.split(",")) {
			tok = tok.trim();
			if (!tok.isEmpty())
				attrs.add(String.valueOf(Integer.parseInt(tok)));
		}
		return attrs;
	}

	public static List<Key> loadOrComputeMinimalKeys(String dataset, Path fdFile, Path cacheDir) throws IOException {
		Path cache = cacheDir.resolve(dataset + ".minkeys.txt");
		if (Files.exists(cache)) {
			List<Key> keys = new ArrayList<>();
			for (String line : Files.readAllLines(cache, StandardCharsets.UTF_8)) {
				line = line.trim();
				if (line.isEmpty())
					continue;
				if (line.equals("<EMPTYKEY>"))
					keys.add(new Key(new ArrayList<>()));
				else
					keys.add(new Key(Arrays.asList(line.split(","))));
			}
			return keys;
		}
		List<Object> parsed = readMinedFDs(fdFile);
		@SuppressWarnings("unchecked")
		List<String> R = (List<String>) parsed.get(0);
		@SuppressWarnings("unchecked")
		List<FD> fds = (List<FD>) parsed.get(1);
		long start = System.currentTimeMillis();
		List<Key> minKeys = Utils.getMinimalKeys(R, fds);
		long cost = System.currentTimeMillis() - start;
		Files.createDirectories(cacheDir);
		List<String> lines = new ArrayList<>();
		for (Key k : minKeys)
			lines.add(k.getAttributes().isEmpty() ? "<EMPTYKEY>" : String.join(",", k.getAttributes()));
		Files.write(cache, lines, StandardCharsets.UTF_8);
		System.out.println("PREPARED," + dataset + ",R=" + R.size() + ",fds=" + fds.size()
				+ ",minKeys=" + minKeys.size() + ",ms=" + cost);
		return minKeys;
	}

	public static String runOne(String dataset, String strategy, Path fdFile, Path cacheDir) throws IOException {
		List<Object> parsed = readMinedFDs(fdFile);
		@SuppressWarnings("unchecked")
		List<String> R = (List<String>) parsed.get(0);
		@SuppressWarnings("unchecked")
		List<FD> fds = (List<FD>) parsed.get(1);
		List<Key> minimalKeys = loadOrComputeMinimalKeys(dataset, fdFile, cacheDir);

		long start = System.currentTimeMillis();
		List<Object> res = Interview.interviewByStrategy(strategy, R, false, minimalKeys);
		long cost = System.currentTimeMillis() - start;

		@SuppressWarnings("unchecked")
		Set<Key> minedKeys = (Set<Key>) res.get(0);
		Set<Key> refined = Utils.refineToMinimalKeys(new ArrayList<>(minedKeys));
		boolean correct = Interview.isTwoKeySetsEqual(minimalKeys, new ArrayList<>(refined));
		int keyQ = (int) res.get(1);// questions answered as key ("No" answers)
		int totalQ = (int) res.get(2);

		return "RESULT," + dataset + "," + R.size() + "," + fds.size() + "," + minimalKeys.size()
				+ "," + strategy.replace(' ', '_') + "," + keyQ + "," + totalQ + "," + cost + "," + correct;
	}

	public static void main(String[] args) throws Exception {
		if (args.length >= 3 && args[0].equals("prepare")) {
			String dataset = args[1];
			Path fdRoot = Paths.get(args[2]);
			Path cacheDir = Paths.get(args[3]);
			loadOrComputeMinimalKeys(dataset, fdRoot.resolve(datasetFiles().get(dataset)), cacheDir);
			return;
		}
		if (args.length >= 4 && args[0].equals("run")) {
			String dataset = args[1];
			String strategy = args[2].replace('_', ' ');
			Path fdRoot = Paths.get(args[3]);
			Path cacheDir = Paths.get(args[4]);
			System.out.println(runOne(dataset, strategy, fdRoot.resolve(datasetFiles().get(dataset)), cacheDir));
			return;
		}
		// inline mode: everything, ascending schema size
		Path fdRoot = Paths.get("..", "FD");
		Path cacheDir = Paths.get("minkeys-cache");
		for (Map.Entry<String, String> e : datasetFiles().entrySet()) {
			for (String strategy : Arrays.asList("topdown dfs", "topdown bfs", "bottomup dfs", "bottomup bfs", "dualize")) {
				System.out.println(runOne(e.getKey(), strategy, fdRoot.resolve(e.getValue()), cacheDir));
			}
		}
	}
}
