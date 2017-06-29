package PolygonDependencies;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Vector;

import genericEntity.datasource.DataSource;
import genericEntity.exec.AbstractExec;
import genericEntity.util.data.GenericObject;
import genericEntity.util.data.storage.StorageManager;

public class DatabaseCSVConverter {

	public static void main(String[] args) throws IOException {
		String dataSource1 = args[0]; //querie to acess th data source
		String output = args[1]; // output (csv)
		
		DataSource source1 = AbstractExec.getDataPostGres(dataSource1);
		
		StorageManager storageDS1 = new StorageManager();
		
		// enables in-memory execution for faster processing
		// this can be done since the whole data fits into memory
        storageDS1.enableInMemoryProcessing();

		// adds the "data" to the algorithm
        storageDS1.addDataSource(source1);

		if(!storageDS1.isDataExtracted()) {
			storageDS1.extractData();
		}
		
		
		
		OutputStreamWriter bw = null;
		
		bw = new OutputStreamWriter(new FileOutputStream(output), "UTF-8");
		String content = "geometry;name;indexOfID;id" + "\n";
		bw.write(content);
		int indexOfID = 0;
		
		Vector<GenericObject> list = storageDS1.getExtractedData();
		
		for (GenericObject dude : list) {
			String nome = "";
			Integer id;
			nome = dude.getData().get("name").toString();
			id = Integer.parseInt(dude.getData().get("id").toString());//for curitiba use atribute "gid" for new york "id"
			
			try {

				content = dude.getData().get("geometry").toString() + ";" + nome.replace(";", ",") + ";" + indexOfID + ";" + id + "\n";
				bw.write(content);
				indexOfID++;


			} catch (IOException e) {

				e.printStackTrace();

			}
		}
		
		try {

			if (bw != null)
				bw.close();

		} catch (IOException ex) {

			ex.printStackTrace();

		}

	}

}
