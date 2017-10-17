package BULMAversion20;
import org.apache.spark.api.java.function.FilterFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import BULMADependences.BULMAFieldsInputGPS;
import BULMADependences.BULMAFieldsInputShape;
import BULMADependences.GeoLine;
import scala.Tuple2;

public class RunBULMAv2 {
	
	private static int NUMBER_ATTRIBUTES_INPUTS = 6;
	private static final String FILE_SEPARATOR = ",";
	
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: <shape file> <GPS file> <directory of output path> <number of partitions>");
            System.exit(1);
        }
        Long initialTime = System.currentTimeMillis();
        
        String pathFileShapes = args[0];
        String pathGPSFile = args[1];
        String pathOutput = args[2];
        int minPartitions = Integer.valueOf(args[3]);
        
        
        SparkSession spark = SparkSession
                  .builder()
//                  .master("local")
                  .config("spark.some.config.option", "some-value")
                  .config("spark.sql.warehouse.dir", "file:///tmp/spark-warehouse")
                  .getOrCreate();
       
        
        
        BULMAFieldsInputGPS headerGPS = new BULMAFieldsInputGPS("bus.code", "latitude", "longitude", "timestamp", "line.code", "gps.id");
        BULMAFieldsInputShape headerShapes = new BULMAFieldsInputShape("route_id","shape_id","shape_pt_lat","shape_pt_lon","shape_pt_sequence","shape_dist_traveled");
        
        Dataset<Row> datasetGPSFile = spark.read().text(pathGPSFile);
        Dataset<Row> datasetShapesFile = spark.read().text(pathFileShapes);
        Tuple2<Dataset<Row>, Integer[]> resultGPS = removeHeaderGPS(datasetGPSFile, headerGPS);
        datasetGPSFile = resultGPS._1;
        Integer[] arrayIndexFieldsInputGPS = resultGPS._2;
        Tuple2<Dataset<Row>, Integer[]> resultShapes = removeHeaderShape(datasetShapesFile, headerShapes);
         datasetShapesFile = resultShapes._1;
         Integer[] arrayIndexFieldsInputShape = resultShapes._2;
        
        Dataset<Tuple2<String, GeoLine>> lines = MatchingRoutesVersion2.generateDataFrames(datasetShapesFile, datasetGPSFile, arrayIndexFieldsInputGPS, arrayIndexFieldsInputShape, minPartitions, spark);
        Dataset<String> output = MatchingRoutesVersion2.run(lines,minPartitions, spark);
        output.toJavaRDD().saveAsTextFile(pathOutput);
        
        System.out.println("Execution time with Dataset: " + (System.currentTimeMillis() - initialTime));
        
    }


    private static Tuple2<Dataset<Row>, Integer[]> removeHeaderGPS(Dataset<Row> dataset, final BULMAFieldsInputGPS fieldsInputGPS) throws Exception {
    	final Integer[] arrayIndexFieldsInputGPS = new Integer[NUMBER_ATTRIBUTES_INPUTS];
		final Row header = dataset.first();
		
		String[] fields = header.getString(0).split(FILE_SEPARATOR);

		for (int i = 0; i < NUMBER_ATTRIBUTES_INPUTS; i++) {

			if (fields[i].equals(fieldsInputGPS.getBusCode())) {
				arrayIndexFieldsInputGPS[0] = i;
			} else if (fields[i].equals(fieldsInputGPS.getLatitude())) {
				arrayIndexFieldsInputGPS[1] = i;
			} else if (fields[i].equals(fieldsInputGPS.getLongitude())) {
				arrayIndexFieldsInputGPS[2] = i;
			} else if (fields[i].equals(fieldsInputGPS.getTimestamp())) {
				arrayIndexFieldsInputGPS[3] = i;
			} else if (fields[i].equals(fieldsInputGPS.getLineCode())) {
				arrayIndexFieldsInputGPS[4] = i;
			} else if (fields[i].equals(fieldsInputGPS.getGpsId())) {
				arrayIndexFieldsInputGPS[5] = i;
			} else {
				throw new Exception("Input fields do not match GPS file fields.");
			}
		}
		
		
		dataset = dataset.filter(new FilterFunction<Row>() {

			private static final long serialVersionUID = 1L;

			public boolean call(Row value) throws Exception {
				if (value.equals(header)) {

					return false;

				}
				return true;
			}
		});

		return new Tuple2<Dataset<Row>, Integer[]>(dataset, arrayIndexFieldsInputGPS);
	}
    
    private static Tuple2<Dataset<Row>, Integer[]> removeHeaderShape(Dataset<Row> dataset, final BULMAFieldsInputShape fieldsInputShape) throws Exception {
    	
    	final Integer[] arrayIndexFieldsInputShape = new Integer[NUMBER_ATTRIBUTES_INPUTS];
		final Row header = dataset.first();
		
		
		String[] fields = header.getString(0).split(FILE_SEPARATOR);

		for (int i = 0; i < NUMBER_ATTRIBUTES_INPUTS; i++) {
			if (fields[i].equals(fieldsInputShape.getRoute())) {
				arrayIndexFieldsInputShape[0] = i;
			} else if (fields[i].equals(fieldsInputShape.getShapeId())) {
				arrayIndexFieldsInputShape[1] = i;
			} else if (fields[i].equals(fieldsInputShape.getLatitude())) {
				arrayIndexFieldsInputShape[2] = i;
			} else if (fields[i].equals(fieldsInputShape.getLongitude())) {
				arrayIndexFieldsInputShape[3] = i;
			} else if (fields[i].equals(fieldsInputShape.getSequence())) {
				arrayIndexFieldsInputShape[4] = i;
			} else if (fields[i].equals(fieldsInputShape.getDistanceTraveled())) {
				arrayIndexFieldsInputShape[5] = i;
			} else {
				throw new Exception("Input fields do not match shape file fields.");
			}
		}

		
		
		dataset = dataset.filter(new FilterFunction<Row>() {

			private static final long serialVersionUID = 1L;

			public boolean call(Row value) throws Exception {
				if (value.equals(header)) {

					return false;
				}

				return true;
			}
		});

		return new Tuple2<Dataset<Row>, Integer[]>(dataset, arrayIndexFieldsInputShape);
	}
}