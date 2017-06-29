package genericEntity.datasource;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.log4j.Logger;

import genericEntity.exception.ExtractionFailedException;
import genericEntity.util.data.GenericObject;
import genericEntity.util.data.json.JsonRecord;
import genericEntity.util.data.json.JsonString;

public class HBaseSource extends AbstractDataSource<HBaseSource> {
	
	protected class HBaseSourceIterator extends AbstractDataSourceIterator<HBaseSource> {
		
		private Configuration conf = HBaseConfiguration.create();
		private ResultScanner result;
		private Result resultLine;

		protected HBaseSourceIterator(HBaseSource source) {
			super(source);
			
			try {				
				HTable table = new HTable(conf, this.dataSource.getIdentifier());
				Scan s = new Scan();	             
	            this.result = table.getScanner(s);
	             
			} catch (IOException e) {				
				e.printStackTrace();
			}           
			
		}

		private boolean resultSetClosed() {
			return this.result == null;
		}
		
		@Override
		protected JsonRecord loadNextRecord() throws ExtractionFailedException {
			if (this.resultSetClosed()) {
				return null;
			}
			
			JsonRecord retVal = null;			
			try {
				resultLine = this.result.next();
				if (resultLine != null) {
					retVal = new JsonRecord();
					String fieldValueTime;
					String key = null;
							 
				         for (KeyValue kv : resultLine.raw()) { 
				        	 key =  new String(kv.getRow());                	 
				        	 fieldValueTime = new String(kv.getQualifier()) + ":" + new String(kv.getValue()) + ":"+ kv.getTimestamp();  
				        	 this.addAttributeValue(retVal, new String(kv.getFamily()), new JsonString(fieldValueTime));
				         } 
				         
				         if (key != null) {
				        	 this.addAttributeValue(retVal, "id", new JsonString(key));
				         }
					 
				} else {
					this.result.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return retVal;
		}

	}

	private static final Logger logger = Logger.getLogger(HBaseSource.class.getPackage().getName());
	
	public HBaseSource(String identifier) {		
		super(identifier);
	}
	
	@Override
	public Iterator<GenericObject> iterator() {
		return new HBaseSourceIterator(this);		
	}
	

}
