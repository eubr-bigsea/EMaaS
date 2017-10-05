package bulma;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import bulma.dependencies.GPSPoint;
import bulma.dependencies.GeoPoint;

public class GenerateFilesInputBULMA {

	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {

		if (args.length < 3) {
			System.err.println("Usage: <GPS file> <output directory> <number of partitions>");
			System.exit(1);
		}

		Long initialTime = System.currentTimeMillis();

		String gpsFile = args[0];
		String outputDirectory = args[1];
		int numPartitions = Integer.parseInt(args[2]);

		System.out.println("[LOG] Starting...");
		splitFile(numPartitions, gpsFile, outputDirectory);
		System.out.println("[LOG] Execution time: " + (System.currentTimeMillis() - initialTime));
	}

	public static boolean splitFile(int numPartitions, String gpsFile, String outputDirectory)
			throws FileNotFoundException, UnsupportedEncodingException {
		System.out.println("[LOG] Reading GPS file to separate it in " + numPartitions + " files");

		HashMap<String, LinkedList<GeoPoint>> map = mapGPSFile(gpsFile);

		System.out.println("[LOG] File mapped...");
		for (int i = 0; i < numPartitions; i++) {
			PrintWriter writer = new PrintWriter(outputDirectory + "/_" + String.format("%02d", i) + ".csv", "UTF-8");
			writer.println("bus.code,latitude,longitude,timestamp,line.code,gps.id");

			int nextIndex = i;
			int j = 0;
			for (Entry<String, LinkedList<GeoPoint>> entrySet : map.entrySet()) {

				if (j == nextIndex) {
					nextIndex += numPartitions;

					for (GeoPoint geoPoint : entrySet.getValue()) {
						writer.println(geoPoint.toString());
					}
				}
				j++;
			}
			writer.close();

			System.out.println("[LOG] Created: " + outputDirectory + "/_" + String.format("%02d", i) + ".csv");

		}
		return true;

	}

	public static HashMap<String, LinkedList<GeoPoint>> mapGPSFile(String filePath) {

		HashMap<String, LinkedList<GeoPoint>> output = new HashMap<String, LinkedList<GeoPoint>>();
		BufferedReader br = null;
		FileReader fr = null;

		try {
			fr = new FileReader(filePath);
			br = new BufferedReader(fr);

			String sCurrentLine = br.readLine();
			while ((sCurrentLine = br.readLine()) != null) {
				if (!sCurrentLine.isEmpty()) {
					GPSPoint gpsPoint = GPSPoint.createGPSPointWithId(sCurrentLine);
					if (!output.containsKey(gpsPoint.getBusCode())) {
						output.put(gpsPoint.getBusCode(), new LinkedList<GeoPoint>());
					}
					output.get(gpsPoint.getBusCode()).add(gpsPoint);
				}
			}

		} catch (IOException e) {

			e.printStackTrace();

		} finally {

			try {

				if (br != null)
					br.close();

				if (fr != null)
					fr.close();

			} catch (IOException ex) {

				ex.printStackTrace();

			}
		}

		return output;
	}

}
