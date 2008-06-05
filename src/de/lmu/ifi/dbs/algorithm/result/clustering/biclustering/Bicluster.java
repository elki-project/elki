package de.lmu.ifi.dbs.algorithm.result.clustering.biclustering;

import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Arthur Zimek
 *         todo arthur comment
 * @param <V> the type of RealVector handled by this Result
 */
public class Bicluster<V extends RealVector<V, Double>> extends AbstractLoggable {
    private int[] rowIDs;

    private int[] colIDs;

    private int[] invertedRows;

    private Database<V> database;

    private Result<V> model;

    public Bicluster(int[] rowIDs, int[] colIDs, Database<V> database) {
        super(LoggingConfiguration.DEBUG);
        this.rowIDs = rowIDs;
        this.colIDs = colIDs;
        this.database = database;
    }

    public void setInvertedRows(int[] invertedRows) {
        this.invertedRows = new int[invertedRows.length];
        System.arraycopy(invertedRows, 0, this.invertedRows, 0, invertedRows.length);
    }

    public void sortIDs() {
        Arrays.sort(this.rowIDs);
        Arrays.sort(this.colIDs);
        if (this.invertedRows != null) {
            Arrays.sort(this.invertedRows);
        }
    }

    public int size() {
        return rowIDs.length;
    }

    public Result<V> model() {
        return model;
    }

    public void appendModel(Result<V> model) {
        this.model = model;
    }

    public Iterator<V> rowIterator() {
        return new Iterator<V>() {
            private int index = -1;

            public boolean hasNext() {
                return index + 1 < size();
            }

            public V next() {
                return database.get(rowIDs[++index]);
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }

    public List<String> headerInformation() {
        List<String> header = new LinkedList<String>();
        header.add("cluster size = " + size());
        header.add("cluster dimensions = " + colIDs.length);
        header.add("included row IDs = " + Util.format(rowIDs));
        header.add("included column IDs = " + Util.format(colIDs));
        if (this.invertedRows != null) {
            header.add("inverted rows (row IDs) = " + Util.format(this.invertedRows));
        }
        return header;
    }

}
