package BULMAversion20;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import BULMADependences.BULMAFieldsInputGPS;
import BULMADependences.BULMAFieldsInputShape;
import BULMADependences.GeoLine;
import scala.Tuple2;

public class RunBULMAv2 {
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
                  .master("local")
                  .config("spark.some.config.option", "some-value")
                  .config("spark.sql.warehouse.dir", "file:///tmp/spark-warehouse")
                  .getOrCreate();
            
        Dataset<Row> datasetGPSFile = spark.read().text(pathGPSFile);
        Dataset<Row> datasetShapesFile = spark.read().text(pathFileShapes);
        
        BULMAFieldsInputGPS headerGPS = new BULMAFieldsInputGPS("bus.code", "latitude", "longitude", "timestamp", "line.code", "gps.id");
        BULMAFieldsInputShape headerShapes = new BULMAFieldsInputShape("route_id","shape_id","shape_pt_lat","shape_pt_lon","shape_pt_sequence","shape_dist_traveled");
                
        Dataset<Tuple2<String, GeoLine>> lines = MatchingRoutesVersion2.generateDataFrames(datasetShapesFile, datasetGPSFile, headerGPS, headerShapes, minPartitions, spark);
        Dataset<String> output = MatchingRoutesVersion2.run(lines,minPartitions, spark);
        output.toJavaRDD().saveAsTextFile(pathOutput);
        
        System.out.println("Execution time with Dataset: " + (System.currentTimeMillis() - initialTime));
        
    }
}