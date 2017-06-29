package genericEntity.database.adapter;

import java.net.UnknownHostException;

import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;

import genericEntity.database.util.DBInfo;

public class MongoDatabase extends Database{
	

	public MongoDatabase(DBInfo dbInfo) {
		super(dbInfo);
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws UnknownHostException {
		MongoClient mongo = new MongoClient("localhost", 27017);
		
		DB db = mongo.getDB("dbtest");

		DBCursor cursor = db.getCollection("pessoa").find();
		
		while (cursor.hasNext()) {
			System.out.println(cursor.next());
		}
	}
	
	
	@Override
	public String getJDBCString() {
		return "mongodb://" + this.getHost() + ":" + this.getPort() + "/" + this.getDatabaseName();
		
	}

	@Override
	public String getDatabaseDriverName() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
