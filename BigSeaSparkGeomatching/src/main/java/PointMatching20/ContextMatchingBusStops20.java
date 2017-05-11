package PointMatching20;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.regex.Pattern;

import org.apache.commons.collections.IteratorUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.Accumulator;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.FlatMapGroupsFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.KeyValueGroupedDataset;
import org.apache.spark.sql.SparkSession;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import LineDependencies.GeoLine;
import LineDependencies.GeoObject;
import PointDependencies.GeoPoint2;
import PointDependencies.PointPair;
import PolygonDependencies.GeoPolygon;
import PolygonDependencies.InputTypes;
import PolygonDependencies.PolygonClassification;
import PolygonDependencies.PolygonPair;
import genericEntity.datasource.DataSource;
import genericEntity.exec.AbstractExec;
import genericEntity.util.data.GenericObject;
import genericEntity.util.data.storage.StorageManager;
import scala.Tuple2;
import uk.ac.shef.wit.simmetrics.similaritymetrics.JaccardSimilarity;

/**
 * Context-based matching of bus stops (streets and bus stops)
 * 
 * @author Brasileiro
 *
 */
public final class ContextMatchingBusStops20 {

	private static final Pattern SPACE = Pattern.compile(" ");
	private static final int rangeBlockingKey = 7;
	
	public static Dataset<GeoObject> generateDataFrames(String dataSource1, String dataSource2, String dataSourceContext, String sourceType, SparkSession spark) throws Exception {
		DataSource source1 = null;
		DataSource source2 = null;
		DataSource sourceContext = null;
		if (sourceType.equals("CSV")) {
			source1 = AbstractExec.getDataCSV(dataSource1, ';');
			source2 = AbstractExec.getDataCSV(dataSource2, ';');
			sourceContext = AbstractExec.getDataCSV(dataSourceContext, ';');
		} else { // is postgis
			source1 = AbstractExec.getDataPostGres(dataSource1);
			source2 = AbstractExec.getDataPostGres(dataSource2);
			sourceContext = AbstractExec.getDataPostGres(dataSourceContext);
		}
		
		
		StorageManager storageDS1 = new StorageManager();
		StorageManager storageDS2 = new StorageManager();
		StorageManager storageDSContext = new StorageManager();

		// enables in-memory execution for faster processing
		// this can be done since the whole data fits into memory
		storageDS1.enableInMemoryProcessing();
		storageDS2.enableInMemoryProcessing();
		storageDSContext.enableInMemoryProcessing();

		// adds the "data" to the algorithm
		storageDS1.addDataSource(source1);
		storageDS2.addDataSource(source2);
		storageDSContext.addDataSource(sourceContext);

		if (!storageDS1.isDataExtracted()) {
			storageDS1.extractData();
		}
		if (!storageDS2.isDataExtracted()) {
			storageDS2.extractData();
		}
		if (!storageDSContext.isDataExtracted()) {
			storageDSContext.extractData();
		}

		List<GeoObject> geoPointsDS1 = new ArrayList<GeoObject>();
		List<GeoObject> geoPointsDS2 = new ArrayList<GeoObject>();
		List<GeoObject> geoLinesDSContext = new ArrayList<GeoObject>();

		// the algorithm returns each generated pair step-by-step
		int indexOfPref = 0;
		for (GenericObject dude : storageDS1.getExtractedData()) {
			String nome = "";
			Integer id;
			// if (!dude.getData().get("name").toString().equals("null")) {//for
			// curitiba use atribute "nome" for new york "signname"
			nome = new String(dude.getData().get("name").toString().getBytes("UTF8"), "UTF8");
			id = Integer.parseInt(dude.getData().get("id").toString());// for curitiba use atribute "gid", for new york "id"

			if (!dude.getData().get("geometry").toString().contains("INF")) {
				geoPointsDS1.add(new GeoPoint2(dude.getData().get("geometry").toString(), nome, InputTypes.GOV_POLYGON,
						indexOfPref, id));
				indexOfPref++;
			}
			// }

		}

		int indexOfOSM = 0;
		for (GenericObject dude : storageDS2.getExtractedData()) {
			// System.out.println(dude.getData().get("geometry"));
			String nome = "";
			Integer id;
			// if (!dude.getData().get("name").toString().equals("null")) {
			nome = new String(dude.getData().get("name").toString().getBytes(), "UTF-8");
			id = Integer.parseInt(dude.getData().get("id").toString());
			geoPointsDS2.add(new GeoPoint2(dude.getData().get("geometry").toString(), nome, InputTypes.OSM_POLYGON,
					indexOfOSM, id));
			indexOfOSM++;
			// }

		}

		int indexOfContext = 0;
		for (GenericObject dude : storageDSContext.getExtractedData()) {
			// System.out.println(dude.getData().get("geometry"));
			String nome = "";
			Integer id;
			// if (!dude.getData().get("name").toString().equals("null")) {
			nome = dude.getData().get("name").toString();
			id = Integer.parseInt(dude.getData().get("id").toString());
			geoLinesDSContext.add(new GeoLine(dude.getData().get("geometry").toString(), nome, InputTypes.GOV_POLYGON,
					indexOfContext, id));
			indexOfContext++;
			// }

		}
		
		Encoder<GeoObject> geoObjEncoder = Encoders.javaSerialization(GeoObject.class);
		
		Dataset<GeoObject> pointsDS1 = spark.createDataset(geoPointsDS1, geoObjEncoder);
		Dataset<GeoObject> pointsDS2 = spark.createDataset(geoPointsDS2, geoObjEncoder);
		Dataset<GeoObject> contextDS = spark.createDataset(geoLinesDSContext, geoObjEncoder);
		
		Dataset<GeoObject> points = pointsDS1.union(pointsDS2).union(contextDS);
		
		return points;
	}

	public static Dataset<String> run(Dataset<GeoObject> points, double thresholdLinguistic, double thresholdPointDistance, Integer amountPartition, SparkSession spark) throws Exception {
		JavaSparkContext ctx = new JavaSparkContext(spark.sparkContext());
		Broadcast<Integer> numReplication = ctx.broadcast(amountPartition);
		Encoder<GeoObject> geoObjEncoder = Encoders.javaSerialization(GeoObject.class);
		
		Encoder<Tuple2<Integer, GeoObject>> tupleEncoder = Encoders.tuple(Encoders.INT(), geoObjEncoder);
		
		Dataset<Tuple2<Integer, GeoObject>> pointsPaired = points.flatMap(new FlatMapFunction<GeoObject, Tuple2<Integer, GeoObject>>() {

			@Override
			public Iterator<Tuple2<Integer, GeoObject>> call(GeoObject s) throws Exception {
				List<Tuple2<Integer, GeoObject>> listOfPointTuple = new ArrayList<Tuple2<Integer, GeoObject>>();
				if (s.getType().equals(InputTypes.OSM_POLYGON)) {
					listOfPointTuple.add(new Tuple2<Integer, GeoObject>(s.getIdGeometry()%numReplication.getValue(), s));
					return listOfPointTuple.iterator();
				} else { //equals to InputTypes.GOV_POLYGON
					for (int i = 0; i < numReplication.value(); i++) {
						listOfPointTuple.add(new Tuple2<Integer, GeoObject>(i, s));
					}
					return listOfPointTuple.iterator();
				}
			}
			
		}, tupleEncoder);
		
		
		KeyValueGroupedDataset<Integer, Tuple2<Integer, GeoObject>> pointsGrouped = pointsPaired.groupByKey(new MapFunction<Tuple2<Integer, GeoObject>, Integer>() {

			@Override
			public Integer call(Tuple2<Integer, GeoObject> value) throws Exception {
				return value._1();
			}
		}, Encoders.INT());

		Accumulator<Double> accum = ctx.accumulator(0.0);
		
		Encoder<PointPair> pairEncoder = Encoders.javaSerialization(PointPair.class);
		Encoder<Tuple2<Integer, PointPair>> tuplePairEncoder = Encoders.tuple(Encoders.INT(), pairEncoder);

		Dataset<Tuple2<Integer, PointPair>> matches = pointsGrouped.flatMapGroups(new FlatMapGroupsFunction<Integer, Tuple2<Integer, GeoObject>, Tuple2<Integer, PointPair>>() {
			@Override
			public Iterator<Tuple2<Integer, PointPair>> call(Integer key, Iterator<Tuple2<Integer, GeoObject>> values)
					throws Exception {
						List<Tuple2<Integer, GeoObject>> pointsPerKey = IteratorUtils.toList(values);
						List<GeoPoint2> pointsSource = new ArrayList<GeoPoint2>();
						List<GeoPoint2> pointsTarget = new ArrayList<GeoPoint2>();
						List<GeoLine> context = new ArrayList<GeoLine>();
						for (Tuple2<Integer, GeoObject> entityPair : pointsPerKey) {
							if (entityPair._2().getType() == InputTypes.OSM_POLYGON) {
								pointsSource.add((GeoPoint2) entityPair._2());
							} else {
								if (entityPair._2() instanceof GeoPoint2) {
									pointsTarget.add((GeoPoint2) entityPair._2());
								} else {
									context.add((GeoLine) entityPair._2());
								}

							}
						}

						List<Tuple2<Integer, PointPair>> entityMatches = new ArrayList<Tuple2<Integer, PointPair>>();

						for (GeoPoint2 entSource : pointsSource) {
							List<Tuple2<Integer, PointPair>> partialResults = new ArrayList<Tuple2<Integer, PointPair>>();
							PriorityQueue<GeoPoint2> neighborhoodBustops = new PriorityQueue<GeoPoint2>();
							List<GeoLine> neighborhoodContext = new ArrayList<GeoLine>();
							GeometryFactory gf = new GeometryFactory();

							for (int i = 0; i < Math.max(pointsTarget.size(), context.size()); i++) {
								if (i < pointsTarget.size()
										&& entSource.getDistance(pointsTarget.get(i)) <= thresholdPointDistance) {
									pointsTarget.get(i).setDistanceToPOI(entSource.getDistance(pointsTarget.get(i)));
									neighborhoodBustops.add(pointsTarget.get(i));
								}

								if (i < context.size() && context.get(i).getGeometry()
										.distance(entSource.getGeometry()) <= thresholdPointDistance) {
									neighborhoodContext.add(context.get(i));
								}
							}

							for (GeoPoint2 point : neighborhoodBustops) {
								Geometry line = gf.createLineString(new Coordinate[] {
										point.getGeometry().getCoordinate(), entSource.getGeometry().getCoordinate() });
								PointPair pair;
								int intersections = 0;
								for (GeoLine street : neighborhoodContext) {
									if (line.intersects(street.getGeometry())) {
										intersections++;
									}
								}

								if (intersections == 0) { // first case (points
															// are near and in
															// same side of the
															// street)
									pair = new PointPair(entSource, point, 0, point.getDistanceToPOI(),
											PolygonClassification.MATCH);
									int index = partialResults.size();
									partialResults.add(new Tuple2<Integer, PointPair>(index, pair));
								} else {
									pair = new PointPair(entSource, point, 0, point.getDistanceToPOI(),
											PolygonClassification.POSSIBLE_PROBLEM);
									int index = partialResults.size();
									partialResults.add(new Tuple2<Integer, PointPair>(index, pair));
								}

							}

							Tuple2<Integer, PointPair> previous = null;
							for (Tuple2<Integer, PointPair> pointPair : partialResults) {
								if (previous != null) {
									if (previous._2().getPolygonClassification().equals(PolygonClassification.MATCH)
											&& pointPair._2().getPolygonClassification()
													.equals(PolygonClassification.POSSIBLE_PROBLEM)) { // case
																										// 1
										entityMatches.add(previous);
										break;
									} else if (previous._2().getPolygonClassification()
											.equals(PolygonClassification.POSSIBLE_PROBLEM)
											&& pointPair._2().getPolygonClassification()
													.equals(PolygonClassification.MATCH)) { // case
																							// 2
										pointPair._2().setPolygonClassification(PolygonClassification.POSSIBLE_PROBLEM);

										entityMatches.add(previous);
										entityMatches.add(pointPair);
									} else if (previous._2().getPolygonClassification()
											.equals(PolygonClassification.MATCH)
											&& pointPair._2().getPolygonClassification()
													.equals(PolygonClassification.MATCH)) { // case
																							// 3
										double maxDistance = Math.max(previous._2().getTarget().getDistanceToPOI(),
												pointPair._2().getTarget().getDistanceToPOI());

										if (pointPair._2().getTarget().getDistanceToPOI() > (2
												* previous._2().getTarget().getDistanceToPOI())) {
											pointPair._2().setPolygonClassification(PolygonClassification.NON_MATCH);
											entityMatches.add(previous);
										} /*
											 * else if
											 * (previous._2().getTarget().
											 * getDistance(pointPair._2().
											 * getTarget()) < (maxDistance)) {
											 * previous._2().
											 * setPolygonClassification(
											 * PolygonClassification.
											 * POSSIBLE_PROBLEM);
											 * pointPair._2().
											 * setPolygonClassification(
											 * PolygonClassification.
											 * POSSIBLE_PROBLEM);
											 * entityMatches.add(previous);
											 * entityMatches.add(pointPair); }
											 */ else {
											// previous._2().setPolygonClassification(PolygonClassification.NON_MATCH);
											// pointPair._2().setPolygonClassification(PolygonClassification.NON_MATCH);
											previous._2()
													.setPolygonClassification(PolygonClassification.POSSIBLE_PROBLEM);
											pointPair._2()
													.setPolygonClassification(PolygonClassification.POSSIBLE_PROBLEM);
											entityMatches.add(previous);
											entityMatches.add(pointPair);
										}
									} else { // POSSIBLE_PROBLEM &&
												// POSSIBLE_PROBLEM
										if (pointPair._2().getTarget().getDistanceToPOI() > (2
												* previous._2().getTarget().getDistanceToPOI())) {
											pointPair._2().setPolygonClassification(PolygonClassification.NON_MATCH);
											entityMatches.add(previous);
										} else {
											entityMatches.add(previous);
											entityMatches.add(pointPair);
										}
									}

									break;

								} else {
									if (partialResults.size() == 1) {
										entityMatches.add(pointPair);
									} else {
										previous = pointPair;
									}
								}
							}
						}

						return entityMatches.iterator();
					}
				}, tuplePairEncoder).repartition(amountPartition);

		Dataset<String> output = matches.flatMap(new FlatMapFunction<Tuple2<Integer, PointPair>, String>() {

			@Override
			public Iterator<String> call(Tuple2<Integer, PointPair> t) throws Exception {
				ArrayList<String> listOutput = new ArrayList<String>();
				listOutput.add(t._2().toStringCSV());
				return listOutput.iterator();
			}
		
		}, Encoders.STRING()).repartition(amountPartition);
		
		return output;
	}
}
