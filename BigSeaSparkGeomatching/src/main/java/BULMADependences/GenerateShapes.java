package BULMADependences;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class GenerateShapes {

	private static final String FILE_SEPARATOR = ",";
	
	public static void main(String[] args) {
		String inputPath = args[0];
		String outputPath = args[1];
		
		BufferedReader brShapes = null;
		String lineShapes = "";
		try {
			brShapes = new BufferedReader(new FileReader(inputPath));
			FileWriter output = null;
			PrintWriter printWriter = null;

			String shapeId = "";
			brShapes.readLine();
			
			while ((lineShapes = brShapes.readLine()) != null) {
				String[] data = lineShapes.split(FILE_SEPARATOR);
				String routeId = data[0];
				String currentShapeId = data[1];
				Double lat = Double.valueOf(data[2]);
				Double lng = Double.valueOf(data[3]);
				
				if (!shapeId.equals(currentShapeId)) { //new shape
					shapeId = currentShapeId;
					String newOutputPath = outputPath +  "shape" + routeId + "-" + currentShapeId + ".csv";
					
					if(output != null && printWriter != null) {
						output.close();
						printWriter.close();
					}
					
					output = new FileWriter(newOutputPath);
					printWriter = new PrintWriter(output);
					printWriter.println("latitude,longitude");
				}
				
				printWriter.println(lat + FILE_SEPARATOR + lng);
				
				}
			
			System.out.println("Done!");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}