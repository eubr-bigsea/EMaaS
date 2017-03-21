package LineDependencies;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MultiColumnCsvSort
{
    private static final String COLUMN_SEPARATOR = ",";

    public static void main(String[] args) throws Exception
    {
        InputStream inputStream = new FileInputStream("C:/Users/Andreza/Desktop/april/csv/2017_02_01_veiculos.csv");
        List<List<String>> lines = readCsv(inputStream);

        // Create a comparator that sorts primarily by column 0,
        // and if these values are equal, by column 2
        Comparator<List<String>> comparator = createComparator(0, 3);
        Collections.sort(lines, comparator);

        OutputStream outputStream = new FileOutputStream("C:/Users/Andreza/Desktop/april/output.csv");
        String header = "bus.code,latitude,longitude,timestamp,line.code,gps.id";
        writeCsv(header, lines, outputStream);        
    }

    private static List<List<String>> readCsv(
        InputStream inputStream) throws IOException
    {
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(inputStream));
        List<List<String>> lines = new ArrayList<List<String>>();

        // Skip header
        String line = reader.readLine();

        while (true)
        {
            line = reader.readLine();
            if (line == null)
            {
                break;
            }
            List<String> list = Arrays.asList(line.split(COLUMN_SEPARATOR));
            lines.add(list);
        }
        return lines;
    }

    private static void writeCsv(
        String header, List<List<String>> lines, OutputStream outputStream) 
        throws IOException
    {
        Writer writer = new OutputStreamWriter(outputStream);
        writer.write(header+"\n");
        for (List<String> list : lines)
        {
            for (int i = 0; i < list.size(); i++)
            {
                writer.write(list.get(i));
                if (i < list.size() - 1)
                {
                    writer.write(COLUMN_SEPARATOR);
                }
            }
            writer.write("\n");
        }
        writer.close();

    }

    private static <T extends Comparable<? super T>> Comparator<List<T>> 
        createComparator(int... indices)
    {
        return createComparator(MultiColumnCsvSort.<T>naturalOrder(), indices);
    }

    private static <T extends Comparable<? super T>> Comparator<T>
        naturalOrder()
    {
        return new Comparator<T>()
        {
            @Override
            public int compare(T t0, T t1)
            {
                return t0.compareTo(t1);
            }
        };
    }

    private static <T> Comparator<List<T>> createComparator(
        final Comparator<? super T> delegate, final int... indices)
    {
        return new Comparator<List<T>>()
        {
            @Override
            public int compare(List<T> list0, List<T> list1)
            {
                for (int i = 0; i < indices.length; i++)
                {
                    T element0 = list0.get(indices[i]);
                    T element1 = list1.get(indices[i]);
                    int n = delegate.compare(element0, element1);
                    if (n != 0)
                    {
                        return n;
                    }
                }
                return 0;
            }
        };
    }
}