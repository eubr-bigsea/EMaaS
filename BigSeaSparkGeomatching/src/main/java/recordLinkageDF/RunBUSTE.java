package recordLinkageDF;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FilterFunction;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import com.clearspring.analytics.util.Lists;

import BULMADependences.BULMAFieldsInputShape;
import recordLinkage.dependencies.TicketInformation;
import recordLinkageDF.dependencies.BUSTEFieldsInputBusStop;
import recordLinkageDF.dependencies.BUSTEFieldsInputTicket;
import scala.Tuple2;

public class RunBUSTE {

	private static int NUM_ATTRIBUTES_SHAPE = 6;
	private static int NUM_ATTRIBUTES_TICKET = 8;
	private static int NUM_ATTRIBUTES_STOPS = 9;
	private static final String FILE_SEPARATOR = ",";
	private static final String SLASH = "/";

	@SuppressWarnings("serial")
	public static void main(String[] args) throws Exception {

		if (args.length < 6) {
			System.err.println(
					"Usage: <Output Bulma directory> <shape file> <Bus stops file> <Bus tickets directory> <outputPath> <number of partitions>");
			System.exit(1);
		}

		Long initialTime = System.currentTimeMillis();

		String pathBulmaOutput = args[0];
		String pathFileShapes = args[1];
		String busStopsFile = args[2];
		String busTicketPath = args[3];
		String outputPath = args[4];
		final Integer minPartitions = Integer.valueOf(args[5]);

		SparkSession spark = SparkSession.builder()
//				.master("local")
				.config("spark.some.config.option", "DataFrameBUSTE")
				.config("spark.sql.warehouse.dir", "file:///tmp/spark-warehouse")
				.getOrCreate();

		BULMAFieldsInputShape headerShapes = new BULMAFieldsInputShape("route_id", "shape_id", "shape_pt_lat",
				"shape_pt_lon", "shape_pt_sequence", "shape_dist_traveled");
		BUSTEFieldsInputTicket headerTickets = new BUSTEFieldsInputTicket(
				"CODLINHA,NOMELINHA,CODVEICULO,NUMEROCARTAO,HORAUTILIZACAO,DATAUTILIZACAO,DATANASCIMENTO,SEXO");
		BUSTEFieldsInputBusStop headerStops = new BUSTEFieldsInputBusStop(
				"arrival_time,departure_time,stop_id,stop_sequence,lat_stop,lng_stop,route_id,shape_id,closest_shape_point");

		Configuration conf = new Configuration();
		FileSystem fs = FileSystem.get(new URI(pathBulmaOutput), conf);
		FileStatus[] fileStatus = fs.listStatus(new Path(pathBulmaOutput));

		for (FileStatus file : fileStatus) {
			FileStatus[] fileStatus2 = fs.listStatus(new Path(pathBulmaOutput + SLASH + file.getPath().getName()));
			String pathDir = pathBulmaOutput + SLASH + file.getPath().getName();

			Dataset<Row> bulmaOutputString = spark.read().text(pathDir + SLASH + "part-00000");

			for (FileStatus filePart : fileStatus2) {
				if (!filePart.getPath().getName().equals("_SUCCESS")
						&& !filePart.getPath().getName().equals("part-00000")) {
					bulmaOutputString.union(spark.read().text(pathDir + SLASH + filePart.getPath().getName()));
				}
			}

			//filter to remove empty rows
			JavaRDD<Row> bulmaOutputRow = bulmaOutputString.filter(new FilterFunction<Row>() {

				private static final long serialVersionUID = 1L;

				public boolean call(Row value) throws Exception {
					
					return !value.getString(0).isEmpty();
				}
			}).toJavaRDD();

			String ticketPathFile = busTicketPath + SLASH + "doc1-"
					+ file.getPath().getName().substring(0, file.getPath().getName().lastIndexOf("_veiculos")) + ".csv";
			Dataset<Row> busTickets = spark.read().text(ticketPathFile);
			JavaRDD<Row> busTicketsRow = removeHeaderTicket(busTickets, headerTickets)._1().toJavaRDD();

			Map<String, List<TicketInformation>> mapTickets = new HashMap<String, List<TicketInformation>>();

			JavaPairRDD<String, Iterable<TicketInformation>> rddTicketsMapped = busTicketsRow
					.mapToPair(new PairFunction<Row, String, TicketInformation>() {

						public Tuple2<String, TicketInformation> call(Row t) throws Exception {
							String entry = t.get(0) + " ";
							String[] splittedEntry = entry.split("(?<=" + FILE_SEPARATOR + ")");

							TicketInformation ticket = new TicketInformation(
									splittedEntry[0].replace(FILE_SEPARATOR, ""),
									splittedEntry[1].replace(FILE_SEPARATOR, ""),
									splittedEntry[2].replace(FILE_SEPARATOR, ""),
									splittedEntry[3].replace(FILE_SEPARATOR, ""),
									splittedEntry[4].replace(FILE_SEPARATOR, ""),
									splittedEntry[5].replace(FILE_SEPARATOR, ""),
									splittedEntry[6].replace(FILE_SEPARATOR, ""),
									splittedEntry[7].replace(FILE_SEPARATOR, ""));

							return new Tuple2<String, TicketInformation>(ticket.getBusCode(), ticket);
						}
					}).groupByKey();

			for (Tuple2<String, Iterable<TicketInformation>> entry : rddTicketsMapped.collect()) {
				mapTickets.put(entry._1, Lists.newArrayList(entry._2));
			}

			Dataset<Row> shapeString = spark.read().text(pathFileShapes);
			JavaRDD<Row> shapeRow = removeHeaderShape(shapeString, headerShapes)._1().toJavaRDD();

			Dataset<Row> busStopsString = spark.read().text(busStopsFile);
			JavaRDD<Row> busStopsRow = removeHeaderBusStop(busStopsString, headerStops)._1().toJavaRDD();

			Dataset<Tuple2<String, Object>> lines = GenerateDataFrameBUSTE.generateDataFrames(shapeRow, busStopsRow,
					bulmaOutputRow, minPartitions, spark);

			Dataset<String> output = GenerateDataFrameBUSTE.run(lines, minPartitions, spark, mapTickets);
			output.toJavaRDD().saveAsTextFile(outputPath + file.getPath().getName());
		}
		
		System.out.println("Execution time with Dataset: " + (System.currentTimeMillis() - initialTime));
	}

	private static Tuple2<Dataset<Row>, Integer[]> removeHeaderBusStop(Dataset<Row> dataset,
			final BUSTEFieldsInputBusStop fieldsInputBusStop) throws Exception {
		final Integer[] arrayIndexFieldsInputStops = new Integer[NUM_ATTRIBUTES_STOPS];
		final Row header = dataset.first();

		String[] fields = header.getString(0).split(FILE_SEPARATOR);
		for (int i = 0; i < NUM_ATTRIBUTES_STOPS; i++) {
			if (fields[i].equals(fieldsInputBusStop.getArrivalTime())) {
				arrayIndexFieldsInputStops[0] = i;
			} else if (fields[i].equals(fieldsInputBusStop.getDepartureTime())) {
				arrayIndexFieldsInputStops[1] = i;
			} else if (fields[i].equals(fieldsInputBusStop.getStopId())) {
				arrayIndexFieldsInputStops[2] = i;
			} else if (fields[i].equals(fieldsInputBusStop.getStopSequence())) {
				arrayIndexFieldsInputStops[3] = i;
			} else if (fields[i].equals(fieldsInputBusStop.getLatStop())) {
				arrayIndexFieldsInputStops[4] = i;
			} else if (fields[i].equals(fieldsInputBusStop.getLngStop())) {
				arrayIndexFieldsInputStops[5] = i;
			} else if (fields[i].equals(fieldsInputBusStop.getRouteId())) {
				arrayIndexFieldsInputStops[6] = i;
			} else if (fields[i].equals(fieldsInputBusStop.getShapeId())) {
				arrayIndexFieldsInputStops[7] = i;
			} else if (fields[i].equals(fieldsInputBusStop.getClosestShapePoint())) {
				arrayIndexFieldsInputStops[8] = i;
			} else {
				throw new Exception("Input fields do not match stops file fields.");
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

		return new Tuple2<Dataset<Row>, Integer[]>(dataset, arrayIndexFieldsInputStops);
	}

	private static Tuple2<Dataset<Row>, Integer[]> removeHeaderTicket(Dataset<Row> dataset,
			final BUSTEFieldsInputTicket fieldsInputTicket) throws Exception {
		final Integer[] arrayIndexFieldsInputTicket = new Integer[NUM_ATTRIBUTES_TICKET];
		final Row header = dataset.first();

		String[] fields = header.getString(0).split(FILE_SEPARATOR);
		for (int i = 0; i < NUM_ATTRIBUTES_TICKET; i++) {
			if (fields[i].equals(fieldsInputTicket.getCodLinha())) {
				arrayIndexFieldsInputTicket[0] = i;
			} else if (fields[i].equals(fieldsInputTicket.getNomeLinha())) {
				arrayIndexFieldsInputTicket[1] = i;
			} else if (fields[i].equals(fieldsInputTicket.getCodVeiculo())) {
				arrayIndexFieldsInputTicket[2] = i;
			} else if (fields[i].equals(fieldsInputTicket.getNumCartao())) {
				arrayIndexFieldsInputTicket[3] = i;
			} else if (fields[i].equals(fieldsInputTicket.getHoraUtilizacao())) {
				arrayIndexFieldsInputTicket[4] = i;
			} else if (fields[i].equals(fieldsInputTicket.getDataUtilizacao())) {
				arrayIndexFieldsInputTicket[5] = i;
			} else if (fields[i].equals(fieldsInputTicket.getDataNasc())) {
				arrayIndexFieldsInputTicket[6] = i;
			} else if (fields[i].equals(fieldsInputTicket.getSexo())) {
				arrayIndexFieldsInputTicket[7] = i;
			} else {
				throw new Exception("Input fields do not match ticket file fields.");
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

		return new Tuple2<Dataset<Row>, Integer[]>(dataset, arrayIndexFieldsInputTicket);
	}

	private static Tuple2<Dataset<Row>, Integer[]> removeHeaderShape(Dataset<Row> dataset,
			final BULMAFieldsInputShape fieldsInputShape) throws Exception {

		final Integer[] arrayIndexFieldsInputShape = new Integer[NUM_ATTRIBUTES_SHAPE];
		final Row header = dataset.first();

		String[] fields = header.getString(0).split(FILE_SEPARATOR);

		for (int i = 0; i < NUM_ATTRIBUTES_SHAPE; i++) {
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