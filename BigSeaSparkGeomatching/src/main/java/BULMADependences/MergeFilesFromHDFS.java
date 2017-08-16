package BULMADependences;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class MergeFilesFromHDFS {

	private static String header = "route,tripNum,shapeId,shapeSequence,shapeLat,shapeLon,distanceTraveledShape,busCode,gpsPointId,gpsLat,gpsLon,distanceToShapePoint,timestamp,busStopId,problem";
	
	
	public static void main(String[] args) {
		
		if (args.length < 2) {
			System.err.println("Usage: <input directory> <output directory>");
			System.exit(1);
		}
		
		String input = args[0];
		String output = args[1];
				
		File inputDir = new File(input);		

		for (File dir : inputDir.listFiles()) {
			
			FileWriter outputFile;
			PrintWriter printWriter = null;
			try {
				outputFile = new FileWriter(output + dir.getName());
				printWriter = new PrintWriter(outputFile);
				printWriter.println(header);
				
				BufferedReader br = null;
				String line = "";
				for (File file : dir.listFiles()) {
					if (!file.getName().equals("_SUCCESS")) {
						br = new BufferedReader(new FileReader(file.getAbsolutePath()));
						
						while ((line = br.readLine()) != null) {
							printWriter.println(line);
						}
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
				
			} finally {
				printWriter.close();
			}
			
			
			
		}
		
	}
}
