package genericEntity.exec;

import java.io.File;
import java.io.FileNotFoundException;

import genericEntity.database.DatabaseSource;
import genericEntity.database.adapter.MySQLDatabase;
import genericEntity.database.adapter.OracleDatabase;
import genericEntity.database.adapter.PostGreSQLDatabase;
import genericEntity.database.util.DBInfo;
import genericEntity.datasource.CSVSource;
import genericEntity.datasource.DataSource;
import genericEntity.datasource.HBaseSource;
import genericEntity.datasource.XMLSource;
import genericEntity.input.ReadAbstractSource;
import genericEntity.util.data.GenericObject;
import genericEntity.util.data.storage.StorageManager;

public class AbstractExec {

	public static DataSource getDataXML(String pathFile, String root){
		DataSource dataSource = null;
		try {
			dataSource = new XMLSource("", new File(pathFile), root);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return dataSource;
	}
	
	public static DataSource getDataCSV(String pathFile){
		CSVSource dataSource = null;
		try {
			dataSource = new CSVSource("", new File(pathFile));
			dataSource.enableHeader();
			dataSource.setSeparatorCharacter(',');
			dataSource.addIdAttributes("id");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return dataSource;
	}
	
	public static DataSource getDataOracle(String propertiesFile){
		
		DataSource dataSource = null;
		try {
			dataSource = new DatabaseSource(propertiesFile,
					new OracleDatabase(new DBInfo(propertiesFile)));
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return dataSource;
	}
	
	public static DataSource getDataPostGres(String propertiesFile){
		DataSource dataSource = null;
		try {
			dataSource = new DatabaseSource(propertiesFile,
					new PostGreSQLDatabase(new DBInfo(propertiesFile)));
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();

		}
		
		return dataSource;
	}
	
	public static DataSource getDataMySQL(String propertiesFile){
		DataSource dataSource = null;
		try {
			dataSource = new DatabaseSource(propertiesFile,
					new MySQLDatabase(new DBInfo(propertiesFile)));
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return dataSource;
	}
	
	public static DataSource getDataHBase() {
		DataSource dataSource = new HBaseSource("");
		
		return dataSource;
	}
}
