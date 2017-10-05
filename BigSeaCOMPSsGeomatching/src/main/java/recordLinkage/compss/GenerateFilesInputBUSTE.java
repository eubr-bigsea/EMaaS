package recordLinkage.compss;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import recordLinkage.dependencies.BulmaOutput;
import recordLinkage.dependencies.TicketInformation;

public class GenerateFilesInputBUSTE {

	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {

		if (args.length < 5) {
			System.err.println(
					"Usage: <BULMA output file> <tickets file> <output directory BULMA output> <tickets files directory output> <number of partitions>");
			System.exit(1);
		}

		Long initialTime = System.currentTimeMillis();

		String bulmaOutputFile = args[0];
		String ticketsFile = args[1];
		String outputDirectoryBO = args[2];
		String outputDirectoryTickets = args[3];
		int numPartitions = Integer.parseInt(args[4]);

		createFiles(numPartitions, bulmaOutputFile, ticketsFile, outputDirectoryBO, outputDirectoryTickets);

		System.out.println("[LOG] Execution time: " + (System.currentTimeMillis() - initialTime));
	}

	public static void createFiles(int numPartitions, String bulmaOutputFile, String ticketsFile,
			String outputDirectoryBO, String outputDirectoryTickets)
			throws FileNotFoundException, UnsupportedEncodingException {

		HashMap<String, LinkedList<BulmaOutput>> mapBulmaOut = mapBulmaOutput(bulmaOutputFile);
		HashMap<String, LinkedList<TicketInformation>> mapTickets = mapTickets(ticketsFile);

		for (int i = 0; i < numPartitions; i++) {
			PrintWriter writerBO = new PrintWriter(outputDirectoryBO + "/_bo" + String.format("%02d", i) + ".csv",
					"UTF-8");
			writerBO.println(
					"TRIP_NUM,ROUTE,SHAPE_ID,SHAPE_SEQ,LAT_SHAPE,LON_SHAPE,GPS_POINT_ID,BUS_CODE,TIMESTAMP,LAT_GPS,LON_GPS,DISTANCE,THRESHOLD_PROBLEM,TRIP_PROBLEM");
			PrintWriter writerTickets = new PrintWriter(
					outputDirectoryTickets + "/_ticket" + String.format("%02d", i) + ".csv", "UTF-8");
			writerTickets.println(
					"CODLINHA,NOMELINHA,CODVEICULO,NUMEROCARTAO,HORAUTILIZACAO,DATAUTILIZACAO,DATANASCIMENTO,SEXO");
			
			int nextIndex = i;
			int j = 0;
			
			
			for (Entry<String, LinkedList<TicketInformation>> entrySet : mapTickets.entrySet()) {

				if (j == nextIndex) {
					nextIndex += numPartitions;
					
					LinkedList<BulmaOutput> bulmaOut = mapBulmaOut.get(entrySet.getKey());
					if (bulmaOut != null) {
						for (BulmaOutput bulmaOutput : bulmaOut) {
							writerBO.println(bulmaOutput.toString());
						}
						
						for (TicketInformation ticketInfo : entrySet.getValue()) {						
							writerTickets.println(ticketInfo.toString());
						}
					}
					
				}
				j++;
			}
			writerBO.close();
			writerTickets.close();

			System.out.println("[LOG] Created: " + outputDirectoryBO + "/_bo" + String.format("%02d", i) + ".csv");
			System.out.println(
					"[LOG] Created: " + outputDirectoryTickets + "/_ticket" + String.format("%02d", i) + ".csv");
		}
		// TODO insert Bulma Output info into files even if there is no ticket
		// for the bus

	}

	public static HashMap<String, LinkedList<BulmaOutput>> mapBulmaOutput(String filePath) {

		HashMap<String, LinkedList<BulmaOutput>> mapBulmaOut = new HashMap<String, LinkedList<BulmaOutput>>();

		BufferedReader br = null;
		FileReader fr = null;

		try {
			fr = new FileReader(filePath);
			br = new BufferedReader(fr);

			String sCurrentLine = br.readLine();
			String[] currentLineSplitted;
			while ((sCurrentLine = br.readLine()) != null) {

				if (!sCurrentLine.isEmpty()) {
					currentLineSplitted = sCurrentLine.split(",");
					BulmaOutput bulmaOutput = new BulmaOutput(currentLineSplitted[0], currentLineSplitted[1],
							currentLineSplitted[2], currentLineSplitted[3], currentLineSplitted[4],
							currentLineSplitted[5], currentLineSplitted[6], currentLineSplitted[7],
							currentLineSplitted[8], currentLineSplitted[9], currentLineSplitted[10],
							currentLineSplitted[11], currentLineSplitted[12], currentLineSplitted[13]);

					if (!mapBulmaOut.containsKey(bulmaOutput.getBusCode())) {
						mapBulmaOut.put(bulmaOutput.getBusCode(), new LinkedList<BulmaOutput>());
					}

					mapBulmaOut.get(bulmaOutput.getBusCode()).add(bulmaOutput);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();

		} finally {
			try {

				if (br != null)
					br.close();

				if (fr != null)
					fr.close();

			} catch (IOException ex) {

				ex.printStackTrace();

			}
		}
		return mapBulmaOut;
	}

	public static HashMap<String, LinkedList<TicketInformation>> mapTickets(String filePath) {

		HashMap<String, LinkedList<TicketInformation>> mapTickets = new HashMap<String, LinkedList<TicketInformation>>();
		BufferedReader br = null;
		FileReader fr = null;

		try {
			fr = new FileReader(filePath);
			br = new BufferedReader(fr);

			String sCurrentLine = br.readLine();
			String[] currentLineSplitted;
			while ((sCurrentLine = br.readLine()) != null) {
				if (!sCurrentLine.isEmpty()) {
					sCurrentLine += " ";
					currentLineSplitted = sCurrentLine.split("(?<=" + "," + ")");
					TicketInformation ticket = new TicketInformation(currentLineSplitted[0].replace(",", ""),
							currentLineSplitted[1].replace(",", ""), currentLineSplitted[2].replace(",", ""),
							currentLineSplitted[3].replace(",", ""), currentLineSplitted[4].replace(",", ""),
							currentLineSplitted[5].replace(",", ""), currentLineSplitted[6].replace(",", ""),
							currentLineSplitted[7].replace(",", ""));

					if (!mapTickets.containsKey(ticket.getBusCode())) {
						mapTickets.put(ticket.getBusCode(), new LinkedList<TicketInformation>());
					}
					mapTickets.get(ticket.getBusCode()).add(ticket);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();

		} finally {
			try {

				if (br != null)
					br.close();

				if (fr != null)
					fr.close();

			} catch (IOException ex) {

				ex.printStackTrace();

			}
		}

		return mapTickets;
	}
}
