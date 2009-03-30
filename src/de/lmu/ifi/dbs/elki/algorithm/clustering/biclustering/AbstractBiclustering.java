package de.lmu.ifi.dbs.elki.algorithm.clustering.biclustering;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Bicluster;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.pairs.SimplePair;

/**
 * Abstract class as a convenience for different biclustering approaches.
 * <p/>
 * The typically required values describing submatrices are computed using the
 * corresponding values within a database of RealVectors.
 * <p/>
 * The database is supposed to present a data matrix with a row representing an
 * entry ({@link RealVector}), a column representing a dimension (attribute) of
 * the {@link RealVector}s.
 * 
 * @author Arthur Zimek
 * @param <V> a certain subtype of RealVector - the data matrix is supposed to
 *        consist of rows where each row relates to an object of type V and the
 *        columns relate to the attribute values of these objects
 */
public abstract class AbstractBiclustering<V extends RealVector<V, Double>, M extends Bicluster<V>> extends AbstractAlgorithm<V, Clustering<M>> implements ClusteringAlgorithm<Clustering<M>, V> {
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
   * Keeps the result. A new ResultObject is assigned when the method
   * {@link #runInTime(Database)} is called.
   */
  private Clustering<M> result;

  /**
   * Prepares the algorithm for running on a specific database.
   * <p/>
   * Assigns the database, the row ids, and the col ids, then calls
   * {@link #biclustering()}.
   * <p/>
   * Any concrete algorithm should be implemented within method
   * {@link #biclustering()} by an inheriting biclustering approach.
   * 
   */
  @Override
  protected final Clustering<M> runInTime(Database<V> database) throws IllegalStateException {
    if(database.size() == 0) {
      throw new IllegalArgumentException(ExceptionMessages.DATABASE_EMPTY);
    }
    this.database = database;
    this.result = new Clustering<M>();
    colIDs = new int[this.getDatabase().dimensionality()];
    for(int i = 0; i < colIDs.length; i++) {
      colIDs[i] = i + 1;
    }
    rowIDs = new int[this.getDatabase().size()];
    {
      int i = 0;
      for(Iterator<Integer> iter = this.getDatabase().iterator(); iter.hasNext();) {
        rowIDs[i] = iter.next();
        i++;
      }
    }
    biclustering();
    return result;
  }

  /**
   * Any concrete biclustering algorithm should be implemented within this
   * method. The database of double-valued <code>RealVector</code>s is
   * encapsulated, methods {@link #sortRows(int,int,List,Comparator)},
   * {@link #sortCols(int,int,List,Comparator)},
   * {@link #meanOfBicluster(BitSet,BitSet)}, {@link #meanOfRow(int,BitSet)},
   * {@link #meanOfCol(BitSet,int)}, {@link #valueAt(int,int)}, allow typical
   * operations like on a data matrix.
   * <p/>
   * This method is supposed to be called only from the method
   * {@link #runInTime(Database)}.
   * <p/>
   * If a bicluster is to be appended to the result, the methods
   * {@link #defineBicluster(BitSet,BitSet)} and
   * {@link #addBiclusterToResult(Bicluster)} should be used.
   * 
   * @throws IllegalStateException if the properties are not set properly (e.g.
   *         method is not called from method {@link #runInTime(Database)}, but
   *         directly)
   */
  protected abstract void biclustering() throws IllegalStateException;

  /**
   * Convert a bitset into integer column ids.
   * 
   * @param cols
   * @return integer column ids
   */
  protected int[] colsBitsetToIDs(BitSet cols) {
    int[] colIDs = new int[cols.cardinality()];
    int colsIndex = 0;
    for(int i = cols.nextSetBit(0); i >= 0; i = cols.nextSetBit(i + 1)) {
      colIDs[colsIndex] = this.colIDs[i];
      colsIndex++;
    }
    return colIDs;
  }

  /**
   * Convert a bitset into integer row ids.
   * 
   * @param rows
   * @return integer row ids
   */
  protected int[] rowsBitsetToIDs(BitSet rows) {
    int[] rowIDs = new int[rows.cardinality()];
    int rowsIndex = 0;
    for(int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
      rowIDs[rowsIndex] = this.rowIDs[i];
      rowsIndex++;
    }
    return rowIDs;
  }

  /**
   * Defines a bicluster as given by the included rows and columns.
   * 
   * @param rows the rows included in the bicluster
   * @param cols the columns included in the bicluster
   * @return a bicluster as given by the included rows and columns
   */
  protected Bicluster<V> defineBicluster(BitSet rows, BitSet cols) {
    int[] rowIDs = rowsBitsetToIDs(rows);
    int[] colIDs = colsBitsetToIDs(cols);
    return new Bicluster<V>(rowIDs, colIDs, getDatabase());
  }

  /**
   * Adds the given Bicluster to the result of this Biclustering.
   * 
   * @param bicluster the bicluster to add to the result
   */
  protected void addBiclusterToResult(M bicluster) {
    result.addCluster(new Cluster<M>(bicluster.getDatabaseObjectGroup(), bicluster));
  }

  /**
   * Sorts the rows. The rows of the data matrix within the range from row
   * <code>from</code> (inclusively) to row <code>to</code> (exclusively) are
   * sorted according to the specified <code>properties</code> and Comparator.
   * <p/>
   * The List of properties must be of size <code>to - from</code> and reflect
   * the properties corresponding to the row ids
   * <code>{@link #rowIDs rowIDs[from]}</code> to
   * <code>{@link #rowIDs rowIDs[to-1]}</code>.
   * 
   * @param <P> the type of <code>properties</code> suitable to the comparator
   * @param from begin of range to be sorted (inclusively)
   * @param to end of range to be sorted (exclusively)
   * @param properties the properties to sort the rows of the data matrix
   *        according to
   * @param comp a Comparator suitable to the type of <code>properties</code>
   */
  protected <P> void sortRows(int from, int to, List<P> properties, Comparator<P> comp) {
    sort(rowIDs, from, to, properties, comp);
  }

  /**
   * Sorts the columns. The columns of the data matrix within the range from
   * column <code>from</code> (inclusively) to column <code>to</code>
   * (exclusively) are sorted according to the specified <code>properties</code>
   * and Comparator.
   * <p/>
   * The List of properties must be of size <code>to - from</code> and reflect
   * the properties corresponding to the column ids
   * <code>{@link #colIDs colIDs[from]}</code> to
   * <code>{@link #colIDs colIDs[to-1]}</code>.
   * 
   * @param <P> the type of <code>properties</code> suitable to the comparator
   * @param from begin of range to be sorted (inclusively)
   * @param to end of range to be sorted (exclusively)
   * @param properties the properties to sort the columns of the data matrix
   *        according to
   * @param comp a Comparator suitable to the type of <code>properties</code>
   */
  protected <P> void sortCols(int from, int to, List<P> properties, Comparator<P> comp) {
    sort(colIDs, from, to, properties, comp);
  }

  /**
   * Sorts an array based on specified properties. The array of ids is sorted
   * within the range from index <code>from</code> (inclusively) to index
   * <code>to</code> (exclusively) according to the specified
   * <code>properties</code> and Comparator. The List of properties must be of
   * size <code>to - from</code> and reflect the properties corresponding to ids
   * <code>ids[from]</code> to <code>ids[to-1]</code>.
   * 
   * @param <P> the type of <code>properties</code> suitable to the comparator
   * @param ids the ids to sort
   * @param from begin of range to be sorted (inclusively)
   * @param to end of range to be sorted (exclusively)
   * @param properties the properties to sort the ids according to
   * @param comp a Comparator suitable to the type of <code>properties</code>
   */
  private <P> void sort(int[] ids, int from, int to, List<P> properties, Comparator<P> comp) {
    if(from >= to) {
      throw new IllegalArgumentException("Parameter from (=" + from + ") >= parameter to (=" + to + ")");
    }
    if(from < 0) {
      throw new IllegalArgumentException("Parameter from (=" + from + ") < 0");
    }
    if(to > ids.length) {
      throw new IllegalArgumentException("Parameter to (=" + to + ") > array length (=" + ids.length + ")");
    }
    if(properties.size() != to - from) {
      throw new IllegalArgumentException("Length of properties (=" + properties.size() + ") does not conform specified length (=" + (to - from) + ")");
    }
    List<SimplePair<Integer, P>> pairs = new ArrayList<SimplePair<Integer, P>>(to - from);
    for(int i = 0; i < properties.size(); i++) {
      pairs.add(new SimplePair<Integer, P>(ids[i + from], properties.get(i)));
    }
    Collections.sort(pairs, new SimplePair.CompareBySecond<Integer, P>(comp));

    for(int i = from; i < to; i++) {
      ids[i] = pairs.get(i - from).getFirst();
    }
  }

  /**
   * Returns the value of the data matrix at row <code>row</code> and column
   * <code>col</code>.
   * 
   * @param row the row in the data matrix according to the current order of
   *        rows (refers to database entry
   *        <code>database.get(rowIDs[row])</code>)
   * @param col the column in the data matrix according to the current order of
   *        rows (refers to the attribute value of an database entry
   *        <code>getValue(colIDs[col])</code>)
   * @return the attribute value of the database entry as retrieved by
   *         <code>database.get(rowIDs[row]).getValue(colIDs[col])</code>
   */
  protected double valueAt(int row, int col) {
    return getDatabase().get(rowIDs[row]).getValue(colIDs[col]);
  }

  /**
   * Provides the mean value for a row on a set of columns. The columns are
   * specified by a BitSet where the indices of a set bit relate to the indices
   * in {@link #colIDs}.
   * 
   * @param row the row to compute the mean value w.r.t. the given set of
   *        columns (relates to database entry id
   *        <code>{@link #rowIDs rowIDs[row]}</code>)
   * @param cols the set of columns to include in the computation of the mean of
   *        the given row
   * @return the mean value of the specified row over the specified columns
   */
  protected double meanOfRow(int row, BitSet cols) {
    double sum = 0;
    for(int i = cols.nextSetBit(0); i >= 0; i = cols.nextSetBit(i + 1)) {
      sum += valueAt(row, i);
    }
    return sum / cols.cardinality();
  }

  /**
   * Provides the mean value for a column on a set of rows. The rows are
   * specified by a BitSet where the indices of a set bit relate to the indices
   * in {@link #rowIDs}.
   * 
   * @param rows the set of rows to include in the computation of the mean of
   *        the given column
   * @param col the column index to compute the mean value w.r.t. the given set
   *        of rows (relates to attribute
   *        <code>{@link #colIDs colIDs[col]}</code> of the corresponding
   *        database entries)
   * @return the mean value of the specified column over the specified rows
   */
  protected double meanOfCol(BitSet rows, int col) {
    double sum = 0;
    for(int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
      sum += valueAt(i, col);
    }
    return sum / rows.cardinality();
  }

  /**
   * Provides the mean of all entries in the submatrix as specified by a set of
   * columns and a set of rows.
   * 
   * @param rows the set of rows to include in the computation of the mean of
   *        the submatrix
   * @param cols the set of columns to include in the computation of the mean of
   *        the submatrix
   * @return the mean of all entries in the submatrix
   */
  protected double meanOfBicluster(BitSet rows, BitSet cols) {
    double sum = 0;
    for(int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
      sum += meanOfRow(i, cols);
    }
    return sum / rows.cardinality();
  }

  public Clustering<M> getResult() {
    return result;
  }

  /**
   * Provides the number of rows of the data matrix.
   * 
   * @return the number of rows of the data matrix
   */
  protected int getRowDim() {
    return this.rowIDs.length;
  }

  /**
   * Provides the number of columns of the data matrix.
   * 
   * @return the number of columns of the data matrix
   */
  protected int getColDim() {
    return this.colIDs.length;
  }

  /**
   * Getter for database.
   * 
   * @return database
   */
  public Database<V> getDatabase() {
    return database;
  }
}
