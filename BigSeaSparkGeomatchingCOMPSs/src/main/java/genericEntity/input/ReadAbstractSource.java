package genericEntity.input;

import java.io.IOException;

import genericEntity.datasource.DataSource;
import genericEntity.util.GlobalConfig;
import genericEntity.util.data.storage.StorageManager;

public class ReadAbstractSource {

	public StorageManager readFile(DataSource dataSource) {
		
		GlobalConfig.getInstance().setInMemoryObjectThreshold(1000);
		StorageManager storage = null;
		
		try {
			storage = new StorageManager();
			storage.enableInMemoryProcessing();
			storage.addDataSource(dataSource);

			if (!storage.isDataExtracted()) {
				storage.extractData();
			}

			dataSource.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return storage;
	}
}
