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
		    	String nameCSV = child.getName();
		    	nameCSV = nameCSV.substring(0, nameCSV.lastIndexOf("."))+".csv";
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

			gravarArq.println("CODLINHA,NOMELINHA,CODVEICULO,NUMEROCARTAO,HORAUTILIZACAO,DATAUTILIZACAO,DATANASCIMENTO,SEXO");
			
			while ((sCurrentLine = br.readLine()) != null) {
            
                Object obj;
                try {
                    obj = parser.parse(sCurrentLine);
                    JSONObject jsonObject = (JSONObject) obj;

                    String veic = (String) jsonObject.get("CODLINHA");   
                    gravarArq.print(veic+",");
                    
                    String lat = (String) jsonObject.get("NOMELINHA");
                    gravarArq.print(lat+",");
                    
                    String lon = (String) jsonObject.get("CODVEICULO");
                    gravarArq.print(lon+",");
                    
                    String lon2 = (String) jsonObject.get("NUMEROCARTAO");
                    gravarArq.print(lon2+",");
                    
                    String dthr = (String) jsonObject.get("DATAUTILIZACAO");
                    String time = dthr.split(" ")[1];
                    time = time.substring(0, time.lastIndexOf(","));
                    String date = dthr.split(" ")[0];
                    gravarArq.print(time+","+date +",");
                    
                    String cod_linha = (String) jsonObject.get("DATANASCIMENTO");
                    gravarArq.print(cod_linha+",");
                    
                    String cod_linha2 = (String) jsonObject.get("SEXO");
                    gravarArq.println(cod_linha2);

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
