package BULMADependences;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class JSON2CSV {

	public static void main(String[] args) throws IOException {
		
		
		if (args.length < 2) {
			System.err.println("Usage: <directory of inputs> <directory of outputs>");
			System.exit(1);
		}
		
		String inputPath = args[0];
		String outputPath = args[1];
		
		File dir = new File(inputPath);
		  File[] directoryListing = dir.listFiles();
		  if (directoryListing != null) {
		    for (File child : directoryListing) {
		    	String nameCSV = child.getName().substring(0, 20)+"csv";
		    	readJsonFile(child.getAbsolutePath(),outputPath+nameCSV);
		    }
		  } else {
			  System.err.println("ERROR!!");
		  }
		System.out.println("DONE!");
		
	}
	
	public static void readJsonFile(String inputPath, String outputPath) {

        BufferedReader br = null;
        JSONParser parser = new JSONParser();

        try {

            String sCurrentLine;

            br = new BufferedReader(new FileReader(inputPath));

            FileWriter output = new FileWriter(outputPath);
			PrintWriter gravarArq = new PrintWriter(output);
			gravarArq.println("bus.code,latitude,longitude,timestamp,line.code,gps.id");
			
			while ((sCurrentLine = br.readLine()) != null) {
            
                Object obj;
                try {
                    obj = parser.parse(sCurrentLine);
                    JSONObject jsonObject = (JSONObject) obj;

                    String veic = (String) jsonObject.get("VEIC");   
                    gravarArq.print(veic+",");
                    
                    String lat = (String) jsonObject.get("LAT");
                    gravarArq.print(lat+",");
                    
                    String lon = (String) jsonObject.get("LON");
                    gravarArq.print(lon+",");
                    
                    String dthr = (String) jsonObject.get("DTHR");
                    String time = dthr.split(" ")[1];
                    gravarArq.print(time+",");
                    
                    String cod_linha = (String) jsonObject.get("COD_LINHA");
                    gravarArq.print(cod_linha+",");
                                      
                    gravarArq.println("-");

                } catch (ParseException e) {
                    
                    e.printStackTrace();
                }
            }
			output.close();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null)br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
	}
}
