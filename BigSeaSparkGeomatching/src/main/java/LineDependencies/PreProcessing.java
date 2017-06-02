package LineDependencies;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PreProcessing {

	public static void main(String[] args) throws Exception {

		if (args.length < 2) {
			System.err.println("Usage: <directory of inputs> <directory of outputs>");
			System.exit(1);
		}
		
		String inputDir = args[0];
		String outputDir = args[1];
		
		File dir = new File(inputDir);
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			for (File child : directoryListing) {
				String nameCSV = child.getName().substring(0, 20) + "csv";
				generateNewOrderedFile(child.getAbsolutePath(), outputDir + nameCSV);
			}
		} else {
			System.err.println("ERROR!!");
		}
		
		System.out.println("DONE!");

	}

	private static String getField(String line) {
		return line.split(",")[3];// extract value you want to sort on
	}

	private static void generateNewOrderedFile(String inputFile, String outputFile) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		Map<String, List<String>> map = new TreeMap<String, List<String>>();
		String line = reader.readLine();// read header
		while ((line = reader.readLine()) != null) {
			String key = getField(line);
			List<String> l = map.get(key);
			if (l == null) {
				l = new LinkedList<String>();
				map.put(key, l);
			}
			l.add(line);

		}
		reader.close();
		FileWriter writer = new FileWriter(outputFile);
		writer.write("bus.code,latitude,longitude,timestamp,line.code,gps.id\n");
		for (List<String> list : map.values()) {
			for (String val : list) {
				writer.write(val);
				writer.write("\n");
			}
		}
		writer.close();
	}

}
