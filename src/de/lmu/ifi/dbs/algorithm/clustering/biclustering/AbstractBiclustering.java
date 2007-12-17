package de.lmu.ifi.dbs.algorithm.clustering.biclustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.IDPropertyPair;
import de.lmu.ifi.dbs.utilities.PropertyPermutationComparator;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Abstract class as a convenience for different biclustering approaches.
 * 
 * The typically required values describing submatrices
 * are computed using the corresponding values within a database
 * of RealVectors.
 * 
 * The database is supposed to present a data matrix with
 * a row representing an entry ({@link RealVector}),
 * a column representing a dimension (attribute)
 * of the {@link RealVector}s.
 *  
 * @author Arthur Zimek
 */
public abstract class AbstractBiclustering<V extends RealVector<V, Double>> extends AbstractAlgorithm<V>
{
    /**
     * Keeps the currently set database.
     */
    private Database<V> database;
    
    /**
     * The row ids corresponding to the currently set {@link #database}.
     */
    private int[] rowIDs;
    
    /**
     * The column ids corresponding to the currently set {@link #database}.
     */
    private int[] colIDs;
    
    /**
     * Assigns the database, the row ids, and the col ids, then calls
     * {@link #biclustering()}.
     * 
     * Any concrete algorithm should be implemented within method
     * {@link #biclustering()} by an inheriting biclustering approach.
     * 
     * @see de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#runInTime(de.lmu.ifi.dbs.database.Database)
     */
    @Override
    protected final void runInTime(Database<V> database) throws IllegalStateException
    {
        if(database.size() == 0)
        {
            throw new IllegalArgumentException("database empty: must contain elements");
        }
        this.database = database;
        colIDs = new int[this.database.get(this.database.iterator().next()).getDimensionality()];
        for(int i = 0; i < colIDs.length; i++)
        {
            colIDs[i] = i+1;
        }
        rowIDs = new int[this.database.size()];
        {
            int i = 0;
            for(Iterator<Integer> iter = this.database.iterator(); iter.hasNext();)
            {
                rowIDs[i] = iter.next();
                i++;
            }
        }
        biclustering();
    }
    
    /**
     * Any concrete biclustering algorithm should be implemented within this method.
     * The database of double-valued <code>RealVector</code>s
     * is encapsulated, methods
     * {@link #sortRows(int, int, List, Comparator)},
     * {@link #sortCols(int, int, List, Comparator)},
     * {@link #meanOfBicluster(BitSet, BitSet)},
     * {@link #meanOfRow(int, BitSet)},
     * {@link #meanOfCol(BitSet, int)},
     * {@link #valueAt(int, int)},
     * allow typical operations like on a data matrix.
     * 
     * This method is supposed to be called only from the method
     * {@link #runInTime(Database)}.
     * 
     * @throws IllegalStateException if the properties are not set properly (e.g. method is not called from method
     * {@link #runInTime(Database)}, but directly)
     */
    protected abstract void biclustering() throws IllegalStateException;
    
    /**
     * Sorts the rows of the data matrix within the range
     * from row <code>from</code> (inclusively)
     * to row <code>to</code> (exclusively),
     * according to the specified <code>properties</code>
     * and Comparator.
     * 
     * The List of properties must be of size <code>to - from</code>
     * and reflect the properties corresponding to the row ids <code>{@link #rowIDs rowIDs[from]}</code> to <code>{@link #rowIDs rowIDs[to-1]}</code>.
     * 
     * @param <P> the type of <code>properties</code> suitable to the comparator
     * @param from begin of range to be sorted (inclusively)
     * @param to end of range to be sorted (exclusively)
     * @param properties the properties to sort the rows of the data matrix according to
     * @param comp a Comparator suitable to the type of <code>properties</code>
     */
    protected <P> void sortRows(int from, int to, List<P> properties, Comparator<P> comp)
    {
        sort(rowIDs, from, to, properties, comp);
    }
    
    /**
     * Sorts the columns of the data matrix within the range
     * from column <code>from</code> (inclusively)
     * to column <code>to</code> (exclusively),
     * according to the specified <code>properties</code>
     * and Comparator.
     * 
     * The List of properties must be of size <code>to - from</code>
     * and reflect the properties corresponding to the column ids <code>{@link #colIDs colIDs[from]}</code> to <code>{@link #colIDs colIDs[to-1]}</code>.
     * 
     * @param <P> the type of <code>properties</code> suitable to the comparator
     * @param from begin of range to be sorted (inclusively)
     * @param to end of range to be sorted (exclusively)
     * @param properties the properties to sort the columns of the data matrix according to
     * @param comp a Comparator suitable to the type of <code>properties</code>
     */
    protected <P> void sortCols(int from, int to, List<P> properties, Comparator<P> comp)
    {
        sort(colIDs, from, to, properties, comp);
    }
    
    /**
     * Sorts the array of ids within the range
     * from index <code>from</code> (inclusively)
     * to index <code>to</code> (exclusively),
     * according to the specified <code>properties</code>
     * and Comparator.
     * The List of properties must be of size <code>to - from</code>
     * and reflect the properties corresponding to ids <code>ids[from]</code> to <code>ids[to-1]</code>.
     * 
     * @param <P> the type of <code>properties</code> suitable to the comparator
     * @param ids the ids to sort
     * @param from begin of range to be sorted (inclusively)
     * @param to end of range to be sorted (exclusively)
     * @param properties the properties to sort the ids according to
     * @param comp a Comparator suitable to the type of <code>properties</code>
     */
    private <P> void sort(int[] ids, int from, int to, List<P> properties, Comparator<P> comp)
    {
        if(from >= to)
        {
            throw new IllegalArgumentException("Parameter from (="+from+") >= parameter to (="+to+")");
        }
        if(from < 0)
        {
            throw new IllegalArgumentException("Parameter from (="+from+") < 0");
        }
        if(to > ids.length)
        {
            throw new IllegalArgumentException("Parameter to (="+to+") > array length (="+ids.length+")");
        }
        if(properties.size() != to - from)
        {
            throw new IllegalArgumentException("Length of properties (="+properties.size()+") does not conform specified length (="+(to-from)+")");
        }
        List<IDPropertyPair<P>> pairs = new ArrayList<IDPropertyPair<P>>(to-from);
        for(int i = 0; i < properties.size(); i++)
        {
            pairs.add(new IDPropertyPair<P>(ids[i+from], properties.get(i)));
        }
        Collections.sort(pairs, new PropertyPermutationComparator<P>(comp));

        for(int i = from; i < to; i++)
        {
            ids[i] = pairs.get(i-from).getId();
        }
    }

    /**
     * Returns the value of the data matrix at row <code>row</code> and column <code>col</code>.
     * 
     * @param row the row in the data matrix according to the current order of rows (refers to database entry <code>database.get(rowIDs[row])</code>)
     * @param col the column in the data matrix according to the current order of rows (refers to the attribute value of an database entry <code>getValue(colIDs[col])</code>)
     * @return the attribute value of the database entry as retrieved by <code>database.get(rowIDs[row]).getValue(colIDs[col])</code>
     */
    protected double valueAt(int row, int col)
    {
        return database.get(rowIDs[row]).getValue(colIDs[col]);
    }

    /**
     * Provides the mean value for a row on a set of columns.
     * The columns are specified by a BitSet where the indices of a set bit
     * relate to the indices in {@link #colIDs}.
     * 
     * @param row the row to compute the mean value w.r.t. the given set of columns
     *  (relates to database entry id <code>{@link #rowIDs[row]}</code>)
     * @param cols the set of columns to include in the computation of the mean of the given row
     * @return the mean value of the specified row over the specified columns
     */
    protected double meanOfRow(int row, BitSet cols)
    {
        double sum = 0;
        for(int i = cols.nextSetBit(0); i >= 0; i = cols.nextSetBit(i+1))
        {
            sum += valueAt(row, i);
        }
        return sum / cols.cardinality();
    }
    
    /**
     * Provides the mean value for a column on a set of rows.
     * The rows are specified by a BitSet where the indices of a set bit
     * relate to the indices in {@link #rowIDs}.
     * 
     * @param rows the set of rows to include in the computation of the mean of the given column
     * @param col the column index to compute the mean value w.r.t. the given set of rows (relates to attribute <code>{@link #colIDs[col]}</code> of the corresponding database entries)
     * @return the mean value of the specified column over the specified rows
     */
    protected double meanOfCol(BitSet rows, int col)
    {
        double sum = 0;
        for(int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i+1))
        {
            sum += valueAt(i, col);
        }
        return sum / rows.cardinality();
    }
    
    /**
     * Provides the mean of all entries in the submatrix
     * as specified by a set of columns and a set of rows.
     * 
     * @param rows the set of rows to include in the computation of the mean of the submatrix
     * @param cols the set of columns to include in the computation of the mean of the submatrix
     * @return the mean of all entries in the submatrix
     */
    protected double meanOfBicluster(BitSet rows, BitSet cols)
    {
        double sum = 0;
        for(int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i+1))
        {
            sum += meanOfRow(i, cols);
        }
        return sum / rows.cardinality();
    }
    
    /*
    public static void testSort(String[] args)
    {
        int[] indices = new int[args.length+4];
        for(int i = 0; i < indices.length; i++)
        {
            indices[i] = i;
        }
        
        AbstractBiclustering b = new AbstractBiclustering(){
            protected void biclustering() throws IllegalStateException {}
            public Description getDescription(){
                return null;
            }
            public Result getResult(){
                return null;
            }            
        };
        
        Comparator<String> comp = new Comparator<String>(){
            public int compare(String o1, String o2)
            {
                return o1.compareTo(o2);
            }
        };
        
        b.sort(indices, 4, args.length+4, Arrays.asList(args), comp);
        System.out.println(Util.format(indices));
    }
    */
}
