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
 */
public class Bicluster<V extends RealVector<V, Double>> extends AbstractLoggable
{
    private int[] rowIDs;
    
    private int[] colIDs;
    
    private Database<V> database;
    
    private Result<V> model;
    
    public Bicluster(int[] rowIDs, int[] colIDs, Database<V> database)
    {
        super(LoggingConfiguration.DEBUG);
        this.rowIDs = rowIDs;
        Arrays.sort(this.rowIDs);
        this.colIDs = colIDs;
        Arrays.sort(this.colIDs);
        this.database = database;
    }
    
    public int size()
    {
        return rowIDs.length;
    }
    
    public Result<V> model()
    {
        return model;
    }
    
    public void appendModel(Result<V> model)
    {
        this.model = model;
    }
    
    public Iterator<V> rowIterator()
    {
        return new Iterator<V>(){
            private int index = -1;

            public boolean hasNext()
            {
                return index + 1 < size();
            }

            public V next()
            {
                return database.get(rowIDs[index++]);
            }

            public void remove()
            {
                throw new UnsupportedOperationException();
            }
            
        };
    }
    
    public List<String> headerInformation()
    {
        List<String> header = new LinkedList<String>();
        header.add("cluster size = "+size());
        header.add("cluster dimensions = "+colIDs.length);
        header.add("included row IDs = "+Util.format(rowIDs));
        header.add("included column IDs = "+Util.format(colIDs));
        return header;
    }
    
}
