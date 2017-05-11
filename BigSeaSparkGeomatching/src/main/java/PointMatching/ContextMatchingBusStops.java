package PointMatching;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.regex.Pattern;

import org.apache.commons.collections.IteratorUtils;
import org.apache.spark.Accumulator;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.broadcast.Broadcast;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import LineDependencies.GeoLine;
import LineDependencies.GeoObject;
import PointDependencies.GeoPoint2;
import PointDependencies.PointPair;
import PolygonDependencies.InputTypes;
import PolygonDependencies.PolygonClassification;
import genericEntity.datasource.DataSource;
import genericEntity.exec.AbstractExec;
import genericEntity.util.data.GenericObject;
import genericEntity.util.data.storage.StorageManager;
import scala.Tuple2;

/**
 * Context-based matching of bus stops (streets and bus stops)
 * 
 * @author Brasileiro
 *
 */
public final class ContextMatchingBusStops {

	private static final Pattern SPACE = Pattern.compile(" ");
	private static final int rangeBlockingKey = 7;

	public static void main(String[] args) throws Exception {
		SparkConf sparkConf = new SparkConf().setAppName("ContextMatchingBusStops").setMaster("local");
		JavaSparkContext ctx = new JavaSparkContext(sparkConf);

		String dataSource1 = args[0];
		String dataSource2 = args[1];
		String dataSourceContext = args[2];
		double thresholdPointDistance = Double.parseDouble(args[3]);
		String outputPath = args[4];
		Integer amountPartition = Integer.parseInt(args[5]);
		String sourceType = args[6];

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

		// DataSource dataSourcePref =
		// AbstractExec.getDataPostGres("queries/bustops_pref_curitiba2.txt");
		// //busStops Pref
		// DataSource dataSourcePref =
		// AbstractExec.getDataPostGres("queries/bustops_osm_curitiba.txt");
		// //busStops OSM

		// DataSource dataSourceOSM =
		// AbstractExec.getDataPostGres("queries/bustops_pref_curitiba2.txt");
		// //busStops OSM
		// DataSource dataSourceOSM =
		// AbstractExec.getDataPostGres("queries/bustops_osm_curitiba.txt");
		// //busStops OSM

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
			nome = dude.getData().get("name").toString();
			id = Integer.parseInt(dude.getData().get("id").toString());// for
																		// curitiba
																		// use
																		// atribute
																		// "gid"
																		// for
																		// new
																		// york
																		// "id"

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
			nome = dude.getData().get("name").toString();
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

		JavaRDD<GeoObject> pointsDS1 = ctx.parallelize(geoPointsDS1);
		JavaRDD<GeoObject> pointsDS2 = ctx.parallelize(geoPointsDS2);
		JavaRDD<GeoObject> contextDS = ctx.parallelize(geoLinesDSContext);

		JavaRDD<GeoObject> points = pointsDS1.union(pointsDS2).union(contextDS);

		Broadcast<Integer> numReplication = ctx.broadcast(amountPartition);
		JavaRDD<Tuple2<Integer, GeoObject>> pointLabed = points
				.flatMap(new FlatMapFunction<GeoObject, Tuple2<Integer, GeoObject>>() {

					@Override
					public List<Tuple2<Integer, GeoObject>> call(GeoObject s) throws Exception {
						List<Tuple2<Integer, GeoObject>> listOfPointTuple = new ArrayList<Tuple2<Integer, GeoObject>>();
						if (s.getType().equals(InputTypes.OSM_POLYGON)) {
							listOfPointTuple.add(
									new Tuple2<Integer, GeoObject>(s.getIdGeometry() % numReplication.getValue(), s));
							return listOfPointTuple;
						} else { // equals to InputTypes.GOV_POLYGON
							for (int i = 0; i < numReplication.value(); i++) {
								listOfPointTuple.add(new Tuple2<Integer, GeoObject>(i, s));
							}
							return listOfPointTuple;
						}
					}

				});

		JavaPairRDD<Integer, GeoObject> pointsPaired = pointLabed
				.mapToPair(new PairFunction<Tuple2<Integer, GeoObject>, Integer, GeoObject>() {

					@Override
					public Tuple2<Integer, GeoObject> call(Tuple2<Integer, GeoObject> tuple) throws Exception {
						return new Tuple2<Integer, GeoObject>(tuple._1(), tuple._2());
					}
				});

		JavaPairRDD<Integer, Iterable<GeoObject>> pointsGrouped = pointsPaired.groupByKey(amountPartition);// number
																											// of
																											// partitions

		Accumulator<Double> accum = ctx.accumulator(0.0);

		JavaPairRDD<Integer, PointPair> matches = pointsGrouped
				.flatMapToPair(new PairFlatMapFunction<Tuple2<Integer, Iterable<GeoObject>>, Integer, PointPair>() {

					@Override
					public List<Tuple2<Integer, PointPair>> call(Tuple2<Integer, Iterable<GeoObject>> tuple)
							throws Exception {
						List<GeoPoint2> pointsPerKey = IteratorUtils.toList(tuple._2().iterator());
						List<GeoPoint2> pointsSource = new ArrayList<GeoPoint2>();
						List<GeoPoint2> pointsTarget = new ArrayList<GeoPoint2>();
						List<GeoLine> context = new ArrayList<GeoLine>();
						for (GeoObject entity : pointsPerKey) {
							if (entity.getType() == InputTypes.OSM_POLYGON) {
								pointsSource.add((GeoPoint2) entity);
							} else {
								if (entity instanceof GeoPoint2) {
									pointsTarget.add((GeoPoint2) entity);
								} else {
									context.add((GeoLine) entity);
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

						return entityMatches;
					}
				});

		matches.flatMap(new FlatMapFunction<Tuple2<Integer, PointPair>, String>() {

			@Override
			public ArrayList<String> call(Tuple2<Integer, PointPair> t) throws Exception {
				ArrayList<String> listOutput = new ArrayList<String>();
				listOutput.add(t._2().toStringCSV());
				return listOutput;
			}

		}).saveAsTextFile(outputPath);

		// System.out.println(accum.value());

		ctx.stop();
		ctx.close();
	}
}
