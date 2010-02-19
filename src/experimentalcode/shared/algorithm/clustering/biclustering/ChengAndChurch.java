package experimentalcode.shared.algorithm.clustering.biclustering;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.clustering.biclustering.AbstractBiclustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.BiclusterWithInverted;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.IntDoublePair;
import de.lmu.ifi.dbs.elki.utilities.pairs.IntIntPair;

/**
 * <b>TODO: Check if implementation for inverted rows is valid</b><br/>
 * Provides a biclustering algorithm which deletes or inserts
 * currentRows/columns dependent on their score and finds biclusters with
 * correlated values.</p>
 * <p>
 * Parameters:
 * <ul>
 * <li>{@link #DELTA_PARAM}</li>
 * <li>{@link #ALPHA_PARAM}</li>
 * <li>{@link #N_PARAM}</li>
 * <li>{@link #BEGIN_PARAM}</li>
 * <li>{@link #END_PARAM}</li>
 * <li>{@link #MISSING_PARAM}</li>
 * <li>{@link #MULTIPLE_ADDITION_PARAM} ???</li>
 * <li>{@link #SEED_PARAM}</li>
 * </ul>
 * </p>
 * <p>
 * Implementation details: In each iteration a single bicluster is found. The
 * properties of the current bicluster are saved in the fields:
 * {@link #currentRows}, {@link #currentCols}, {@link #invertedRows}
 * {@link #currentResidue}, {@link #currentMean}, {@link #rowMeans} and
 * {@link #columnMeans}.
 * </p>
 * 
 * <p>
 * Reference: <br>
 * Y. Cheng and G. M. Church. Biclustering of expression data. In Proceedings of
 * the 8th International Conference on Intelligent Systems for Molecular Biology
 * (ISMB), San Diego, CA, 2000.
 * </p>
 * 
 * @author Noemi Andor
 * @param <V> a certain subtype of NumberVector - the data matrix is supposed to
 *        consist of currentRows where each row relates to an object of type V
 *        and the columns relate to the attribute values of these objects
 */
public class ChengAndChurch<V extends NumberVector<V, Double>> extends AbstractBiclustering<V, BiclusterWithInverted<V>> {

  /**
   * The minimum number of columns that the database must have so that a removal
   * of columns is performed in {@link #multipleNodeDeletion()}.</p>
   * <p>
   * Just start deleting multiple columns when more than 100 columns are in the
   * datamatrix.
   * </p>
   */
  private static final int MIN_COLUMN_REMOE_THRESHOLD = 100;

  /**
   * The minimum number of rows that the database must have so that a removal of
   * rowss is performed in {@link #multipleNodeDeletion()}.
   * <p>
   * Just start deleting multiple rows when more than 100 rows are in the
   * datamatrix.
   * </p>
   * <!--
   * <p>
   * The value is set to 100 as this is not really described in the paper.
   * </p>
   * -->
   */
  private static final int MIN_ROW_REMOE_THRESHOLD = 100;

  /**
   * Default value for {@link #MULTIPLE_ADDITION_PARAM}
   */
  private static final int DEFAULT_MULTIPLE_ADDITION = 1;

  /**
   * OptionID for the parameter {@link #SEED_PARAM}.
   */
  public static final OptionID SEED_ID = OptionID.getOrCreateOptionID("chengandchurch.random", "Seed for the random that creates the missing values.");

  /**
   * Parameter to specify the seed for the random that creates the missing
   * values.
   * <p>
   * Default value: 0
   * </p>
   * <p>
   * Key: {@code -chengandchurch.random}
   * </p>
   */
  private final LongParameter SEED_PARAM = new LongParameter(SEED_ID, 0L);

  /**
   * OptionID for the parameter {@link #DELTA_PARAM}.
   */
  public static final OptionID DELTA_ID = OptionID.getOrCreateOptionID("chengandchurch.delta", "Threshold value to determine the maximal acceptable score (mean squared residue) of a bicluster.");

  /**
   * Threshold value to determine the maximal acceptable score (mean squared
   * residue) of a bicluster.
   * <p/>
   * Key: {@code -chengandchurch.delta}
   * </p>
   */
  public final DoubleParameter DELTA_PARAM = new DoubleParameter(DELTA_ID, new GreaterEqualConstraint(0.0));

  /**
   * OptionID for the parameter {@link #ALPHA_PARAM}.
   */
  public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("chengandchurch.alpha", "Parameter for multiple node deletion to accelerate the algorithm.");

  /**
   * Parameter for multiple node deletion to accelerate the algorithm. (&gt;= 1)
   * <p/>
   * Key: {@code -chengandchurch.alpha}
   * </p>
   */
  public final DoubleParameter ALPHA_PARAM = new DoubleParameter(ALPHA_ID, new GreaterEqualConstraint(1.0));

  /**
   * OptionID for the parameter {@link #N_PARAM}.
   */
  public static final OptionID N_ID = OptionID.getOrCreateOptionID("chengandchurch.n", "The number of biclusters to be found.");

  /**
   * Number of biclusters to be found.
   * <p/>
   * Default value: 1
   * </p>
   * <p/>
   * Key: {@code -chengandchurch.n}
   * </p>
   */
  public final IntParameter N_PARAM = new IntParameter(N_ID, new GreaterEqualConstraint(1));

  /**
   * OptionID for the parameter {@link #BEGIN_PARAM}.
   */
  public static final OptionID BEGIN_ID = OptionID.getOrCreateOptionID("chengandchurch.begin", "The lower limit for the masking and missing values. Must be smaller than the upper limit.");

  /**
   * Lower limit for the masking and missing values.
   * <p/>
   * Key: {@code -chengandchurch.begin}
   * </p>
   */
  public final DoubleParameter BEGIN_PARAM = new DoubleParameter(BEGIN_ID);

  /**
   * OptionID for the parameter {@link #END_PARAM}.
   */
  public static final OptionID END_ID = OptionID.getOrCreateOptionID("chengandchurch.end", "The upper limit for the masking and missing values.");

  /**
   * Upper limit for the masking and missing values.
   * <p/>
   * Key: {@code -chengandchurch.end}
   * </p>
   */
  public final DoubleParameter END_PARAM = new DoubleParameter(END_ID);

  /**
   * OptionID for the parameter {@link #MISSING_PARAM}.
   */
  public static final OptionID MISSING_ID = OptionID.getOrCreateOptionID("chengandchurch.missing", "This value in database will be replaced with random values in the range from begin to end.");

  /**
   * A parameter to indicate what value the missing values in the database
   * have.</p>
   * <p>
   * If a value is missing in the database, you have to give them a value (e.g.
   * -10000) and specify -10000 as this parameter.
   * </p>
   * <p>
   * The missing values in database will be replaced by a random generated value
   * in the range between {@link #BEGIN_PARAM} and {@link #END_PARAM}. The
   * missing values will be uniform distributed. Note that the random values are
   * <code>double</code> and not <code>int</code>.
   * </p>
   * <p>
   * Default: No replacement of missing parameters occurs.
   * <p/>
   * Key: {@code -chengandchurch.missing}
   * </p>
   */
  public final IntParameter MISSING_PARAM = new IntParameter(MISSING_ID);

  /**
   * OptionID for the parameter {@link #MULTIPLE_ADDITION_PARAM}.
   */
  public static final OptionID MULTIPLE_ADDITION_ID = OptionID.getOrCreateOptionID("chengandchurch.multipleAddition", "Indicates how many times the algorithm to add Nodes should be performed.");

  /**
   * Parameter to indicate how many times an addition should be performed in
   * each of the <code>n</code> iterations.</p>
   * <p>
   * A greater value will result in a more accurate result but will require more
   * time.
   * </p>
   * <p/>
   * Default value: 1 ({@value #DEFAULT_MULTIPLE_ADDITION})
   * </p>
   * <p/>
   * Key: {@code -chengandchurch.multipleAddition}
   * </p>
   */
  public final IntParameter MULTIPLE_ADDITION_PARAM = new IntParameter(MULTIPLE_ADDITION_ID, new GreaterEqualConstraint(1));

  /**
   * Threshold for the score ({@link #DELTA_PARAM}).
   */
  private double delta;

  /**
   * The parameter for multiple node deletion.</p>
   * <p>
   * It is used to magnify the {@link #delta} value in the
   * {@link #multipleNodeDeletion()} method.
   * </p>
   */
  private double alpha;

  /**
   * Number of biclusters to be found.
   */
  private int n;

  /**
   * Mean of the current bicluster.
   */
  private double currentMean;

  /**
   * The current bicluster score (mean squared residue).
   */
  private double currentResidue;

  /**
   * Indicates the columns that belong to the current bicluster.
   */
  private BitSet currentCols;

  /**
   * Indicates the current rows that belong to the current bicluster.
   */
  private BitSet currentRows;

  /**
   * All row means of the current bicluster.</p>
   * <p>
   * A row mean is the (arithmetic) mean of the values defined on the current
   * columns in a specific row (the key of the map). The value of the map is the
   * row mean.
   * </p>
   */
  private Map<Integer, Double> rowMeans;

  /**
   * All column means of the current bicluster.</p>
   * <p>
   * A column mean is the (arithmetic) mean of the values defined on the current
   * rows in a specific column (the key of the map). The value of the map is the
   * column mean.
   * </p>
   */
  private Map<Integer, Double> columnMeans;

  /**
   * Keeps the position of the inverted rows belonging to the current bicluster.
   */
  // TODO: Check if implementation of invertedRows is valid... especially in
  // mean computation, ...
  private BitSet invertedRows = new BitSet();

  /**
   * Lower threshold for random maskedValues.
   */
  private double minMissingValue;

  /**
   * Upper threshold for random maskedValues.
   */
  private double maxMissingValue;

  /**
   * <p>
   * Keeps track of all masked values. A masked value is occupied by a
   * previously found bicluster and thus populated with random noise so that a
   * new iteration will not detect the same cluster again.
   * </p>
   * <p>
   * The key of the map is an {@link IntIntPair} that contains the row and
   * column. The value of the map represents the new value that is randomly
   * distributed. The original value is still in the database but we bypass the
   * {@link AbstractBiclustering#valueAt(int, int)} by overriding it so the new
   * {@link #valueAt(int, int)} will return the masked value instead of the
   * original. The masked values will be between {@link #minMissingValue} and
   * {@link #maxMissingValue}.
   * </p>
   */
  private Map<IntIntPair, Double> maskedVals;

  /**
   * <p>
   * Keeps track of all values that are missing in the database (that are
   * <code>null</code>).
   * </p>
   * <p>
   * Depends on the {@link Database} implementation used. For every key that
   * represents a row and a column the value is returned when calling
   * {@link #valueAt(int, int)}.
   * </p>
   * 
   */
  private Map<IntIntPair, Double> missingValues;

  /**
   * A random for generating values for the missing or masking
   * currentRows/columns.
   */
  private Random random;

  /**
   * Sets the options for the parameter values {@link #random}, {@link #delta},
   * {@link #alpha}, {@link #n}, {@link #MISSING_PARAM},
   * {@link #minMissingValue} and {@link #maxMissingValue}.
   */
  public ChengAndChurch(Parameterization config) {
    super(config);
    
//    random = new Random(SEED_PARAM.getValue());
//    delta = DELTA_PARAM.getValue();
//    alpha = ALPHA_PARAM.getValue();
//    n = N_PARAM.getValue();
//    minMissingValue = BEGIN_PARAM.getValue();
//    maxMissingValue = END_PARAM.getValue();
//    if(minMissingValue > maxMissingValue) {
//      throw new WrongParameterValueException(BEGIN_PARAM, "The minimum value for missing values is larger than the maximum value", "Minimum value: " + minMissingValue + "  maximum value: " + maxMissingValue);
//    }
    // SEED_PARAM.setOptional(true);
    DELTA_PARAM.setOptional(false);
    ALPHA_PARAM.setOptional(false);
    N_PARAM.setDefaultValue(1);
    MULTIPLE_ADDITION_PARAM.setOptional(true);
    MULTIPLE_ADDITION_PARAM.setDefaultValue(DEFAULT_MULTIPLE_ADDITION);
    MISSING_PARAM.setOptional(true);
    BEGIN_PARAM.setOptional(false);
    END_PARAM.setOptional(false);

    if(config.grab(this, SEED_PARAM)) {
      random = new Random(SEED_PARAM.getValue());
    }
    if(config.grab(this, DELTA_PARAM)) {
      delta = DELTA_PARAM.getValue();
    }
    if(config.grab(this, N_PARAM)) {
      n = N_PARAM.getValue();
    }
    if(config.grab(this, ALPHA_PARAM)) {
      alpha = ALPHA_PARAM.getValue();
    }
    if(config.grab(this, MULTIPLE_ADDITION_PARAM)) {
      // TODO: make instance variable?
    }
    if(config.grab(this, MISSING_PARAM)) {
      // TODO: make instance variable?
    }
    if(config.grab(this, BEGIN_PARAM)) {
      minMissingValue = BEGIN_PARAM.getValue();
    }
    if(config.grab(this, END_PARAM)) {
      maxMissingValue = END_PARAM.getValue();
    }

    if(minMissingValue > maxMissingValue) {
      config.reportError(new WrongParameterValueException(BEGIN_PARAM, "The minimum value for missing values is larger than the maximum value", "Minimum value: " + minMissingValue + "  maximum value: " + maxMissingValue));
    }

    this.maskedVals = new HashMap<IntIntPair, Double>();
    this.missingValues = new HashMap<IntIntPair, Double>();
    this.rowMeans = new HashMap<Integer, Double>();
    this.columnMeans = new HashMap<Integer, Double>();
  }


  /*
   * (non-Javadoc)
   * 
   * @see
   * de.lmu.ifi.dbs.elki.algorithm.clustering.biclustering.AbstractBiclustering
   * #biclustering()
   */
  @Override
  public void biclustering() {
    long t = System.currentTimeMillis();

    chengAndChurch();
    if(logger.isVerbose()) {
      logger.verbose("Runtime: " + (System.currentTimeMillis() - t));
    }
  }

  /**
   * Initiates this algorithm and runs {@link #n} iterations to find the
   * {@link #n} biclusters.</p>
   * <p>
   * It first checks if some missing (<code>NULL</code>) values are in the
   * database and masks them ({@link #fillMissingValues()}).<br />
   * For each iteration it starts with the complete database as subspace cluster
   * and performs multiple row and column deletions (
   * {@link #multipleNodeDeletion()}) if more than 100 rows or columns are in
   * the datamatrix, otherwise {@link #singleNodeDeletion()} is performed until
   * the {@link #delta} value is reached. Then it calls the
   * {@link #nodeAddition()} and finally masks the found bicluster (
   * {@link #maskMatrix()}). A reset operation is called after each iteration to
   * start with the complete database as starting subspace cluster again (
   * {@link #reset()}).
   * </p>
   */
  private void chengAndChurch() {
    this.reset();
    // long t = System.currentTimeMillis();
    this.fillMissingValues();
    // logger.verbose("fillMissingValues() finished. (" +
    // (System.currentTimeMillis() - t) + ")");
    for(int i = 0; i < n; i++) {
      // t = System.currentTimeMillis();
      this.multipleNodeDeletion();
      // logger.verbose("\tmultipleNodeDeletion() finished. (" +
      // (System.currentTimeMillis() - t) + ")");
      // t = System.currentTimeMillis();
      this.nodeAddition();
      // logger.verbose("\tnodeAddition() finished. (" +
      // (System.currentTimeMillis() - t) + ")");
      // t = System.currentTimeMillis();
      this.maskMatrix();
      // logger.verbose("\tmaskMatrix() finished. (" +
      // (System.currentTimeMillis() - t) + ")");
      // t = System.currentTimeMillis();
      BiclusterWithInverted<V> bicluster = new BiclusterWithInverted<V>(rowsBitsetToIDs(currentRows), colsBitsetToIDs(currentCols), getDatabase());
      bicluster.setInvertedRows(rowsBitsetToIDs(invertedRows));
      addBiclusterToResult(bicluster);

      if(logger.isVerbose()) {
        logger.verbose("Score of bicluster" + (i + 1) + ": " + this.currentResidue);
        logger.verbose("Number of rows: " + currentRows.cardinality());
        logger.verbose("Number of columns: " + currentCols.cardinality());
        logger.verbose("Total number of masked values: " + maskedVals.size() + "\n");
      }
      this.reset();
    }
  }

  //
  /**
   * <p>
   * Removes alternating rows and columns until the residue of the current
   * bicluster is smaller than {@link #delta}.
   * </p>
   * <p>
   * If the dimension of the database is small ({@link #getColDim()} >
   * {@link #MIN_COLUMN_REMOE_THRESHOLD}), no column deletion is performed. Same
   * for rows ({@link #MIN_ROW_REMOE_THRESHOLD}).<br />
   * If a single iteration does not remove anything a single node deletion
   * algorithm ({@link #singleNodeDeletion()}) is performed.
   * </p>
   */
  private void multipleNodeDeletion() {

    // If something has been removed:
    boolean removed = false;

    int c = 0;
    while(this.currentResidue > this.delta) {
      removed = false;
      // TODO: Proposal in paper: use an adaptive alpha based on the new scores
      // of the current bicluster.
      double alphaResidue = alpha * currentResidue;
      c++;
      // TODO: Maybe use the row dim of the current cluster instead of the dim
      // of the database?

      if(getRowDim() > MIN_ROW_REMOE_THRESHOLD) {
        // Compute row mean for each row i
        BitSet unionRows = new BitSet();
        unionRows.or(currentRows);
        unionRows.or(invertedRows);
        List<Integer> rowsToRemove = new ArrayList<Integer>();
        // List<Integer> invertedRowsToRemove = new ArrayList<Integer>();
        for(int i = unionRows.nextSetBit(0); i >= 0; i = unionRows.nextSetBit(i + 1)) {
          if(computeRowResidue(i, false) > alphaResidue) {
            rowsToRemove.add(i);
          }
        }
        // Inverted:
        // for(int i = invertedRows.nextSetBit(0); i >= 0; i =
        // invertedRows.nextSetBit(i + 1)) {
        // if(computeRowResidue(i, false) > alphaResidue) {
        // invertedRowsToRemove.add(i);
        // }
        // }
        // remove the found ones
        for(Integer row : rowsToRemove) {
          currentRows.clear(row);
          invertedRows.clear(row);
          removed = true;
        }
        // for(Integer row : invertedRowsToRemove) {
        // invertedRows.clear(row);
        // currentRows.clear(row);
        // removed = true;
        // }
        if(removed) {
          updateValues();
        }
      }

      if(getColDim() > MIN_COLUMN_REMOE_THRESHOLD) {
        // Compute row mean for each column j
        List<Integer> colsToRemove = new ArrayList<Integer>();
        for(int j = currentCols.nextSetBit(0); j >= 0; j = currentCols.nextSetBit(j + 1)) {
          if(computeColResidue(j) > alphaResidue) {
            colsToRemove.add(j);
          }
        }
        boolean colRemoved = false;
        // remove them
        for(Integer col : colsToRemove) {
          currentCols.clear(col);
          removed = true;
          colRemoved = true;
        }
        if(colRemoved) {
          updateValues();
        }
      }

      if(!removed) {
        // TODO: Paper is not clear when to call singleNodeDeletion()
        singleNodeDeletion();
      }
    }
  }

  /**
   * <p>
   * Performs a single node deletion per iteration until the score of the
   * resulting bicluster is lower or equal to delta.
   * </p>
   */
  private void singleNodeDeletion() {
    while(this.currentResidue > delta) {
      IntDoublePair maxRowResidue = getLargestRowResidue();
      IntDoublePair maxColResidue = getLargestColResidue();
      if(maxRowResidue.getSecond() > maxColResidue.getSecond()) {
        currentRows.clear(maxRowResidue.getFirst());
        invertedRows.clear(maxRowResidue.getFirst());
      }
      else {
        currentCols.clear(maxColResidue.getFirst());
      }
      updateValues();
    }
  }

  /**
   * <p>
   * Adds alternating rows and columns so that the {@link #currentResidue} will
   * decrease. This is done {@link #MULTIPLE_ADDITION_PARAM} times if the
   * parameter is set. Otherwise, {@link #DEFAULT_MULTIPLE_ADDITION} times.
   * </p>
   * <p>
   * Also addes the <b>inverse</b> of a row.
   * </p>
   */
  private void nodeAddition() {
    boolean added = true;
    int numberOfAddition = DEFAULT_MULTIPLE_ADDITION;
    if(MULTIPLE_ADDITION_PARAM.isDefined()) {
      numberOfAddition = MULTIPLE_ADDITION_PARAM.getValue();
    }

    while(added && numberOfAddition > 0) {
      added = false;
      numberOfAddition--;
      // Compute row mean for each col j
      List<Integer> colToAdd = new ArrayList<Integer>();
      for(int j = currentCols.nextClearBit(0); j < getColDim(); j = currentCols.nextClearBit(j + 1)) {
        if(computeColResidue(j) <= currentResidue) {
          colToAdd.add(j);
        }
      }
      // Add the found ones
      for(Integer col : colToAdd) {
        currentCols.set(col);
        added = true;
      }
      if(added) {
        updateValues();
      }

      // Compute row mean for each row i
      // BitSet unionRows = new BitSet();
      // unionRows.or(currentRows);
      // unionRows.or(invertedRows);

      List<Integer> rowsToAdd = new ArrayList<Integer>();
      List<Integer> invertedRowsToAdd = new ArrayList<Integer>();
      for(int i = currentRows.nextClearBit(0); i < getRowDim(); i = currentRows.nextClearBit(i + 1)) {
        if(computeRowResidue(i, false) <= currentResidue) {
          rowsToAdd.add(i);
        }
      }
      for(int i = invertedRows.nextClearBit(0); i < getRowDim(); i = invertedRows.nextClearBit(i + 1)) {
        if(computeRowResidue(i, true) <= currentResidue) {
          invertedRowsToAdd.add(i);
        }
      }
      // Add the found ones
      for(Integer row : rowsToAdd) {
        currentRows.set(row);
        invertedRows.clear(row); // Make sure that just one of them has the row.
        added = true;
      }
      for(Integer row : invertedRowsToAdd) {
        invertedRows.set(row);
        currentRows.clear(row); // Make sure that just one of them has the row.
        added = true;
      }
      if(added) {
        updateValues();
      }
    }
  }

  /**
   * <p>
   * Resets the values for the next iteration to start.
   * </p>
   * <p>
   * These are {@link #rowMeans}, {@link #columnMeans}, {@link #currentMean},
   * {@link #currentResidue}, {@link #invertedRows}. It sets the
   * {@link #currentRows} and {@link #currentCols} from 0 to
   * {@link #getRowDim()} ({@link #getColDim()} resp.).<br/>
   * It calls {@link #updateValues()} at the end.
   * </p>
   */
  private void reset() {

    currentResidue = 0.0;
    currentMean = 0.0;

    rowMeans = new HashMap<Integer, Double>();
    columnMeans = new HashMap<Integer, Double>();

    invertedRows.clear();

    currentCols = new BitSet(getColDim());
    currentCols.set(0, getColDim());

    currentRows = new BitSet(getRowDim());
    currentRows.set(0, getRowDim());
    updateValues();

  }

  /**
   * Fills the missing values with random values ranging from
   * {@link #minMissingValue} to {@link #maxMissingValue} in a uniform
   * manner.</p>
   * <p>
   * It does nothing if {@link #MISSING_PARAM} is not set as it is not clear
   * what values to treat as missing values.
   * </p>
   * 
   * @see #BEGIN_PARAM
   * @see #END_PARAM
   */
  private void fillMissingValues() {
    if(this.missingValues == null) {
      this.missingValues = new HashMap<IntIntPair, Double>();
    }
    if(!MISSING_PARAM.isDefined()) {
      return;
    }
    for(int i = 0; i < getRowDim(); i++) {
      for(int j = 0; j < getColDim(); j++) {
        if(super.valueAt(i, j) == MISSING_PARAM.getValue()) {
          missingValues.put(new IntIntPair(i, j), minMissingValue + random.nextDouble() * (maxMissingValue - minMissingValue));
        }
      }
    }
  }

  /**
   * Masks all values in {@link #currentRows} UNION {@link #invertedRows} and
   * {@link #currentCols} belonging to the current bicluster.</p>
   * <p>
   * The masking values range from {@link #minMissingValue} to
   * {@link #maxMissingValue} in a uniform manner.
   * </p>
   * 
   * @see #BEGIN_PARAM
   * @see #END_PARAM
   */
  private void maskMatrix() {
    BitSet unionRows = new BitSet();
    unionRows.or(currentRows);
    unionRows.or(invertedRows);
    for(int i = unionRows.nextSetBit(0); i >= 0; i = unionRows.nextSetBit(i + 1)) {
      for(int j = currentCols.nextSetBit(0); j >= 0; j = currentCols.nextSetBit(j + 1)) {
        IntIntPair key = new IntIntPair(i, j);
        maskedVals.put(key, minMissingValue + random.nextDouble() * (maxMissingValue - minMissingValue));
      }
    }
  }

  /**
   * Updates the {@link #currentResidue}, {@link #currentMean} and calls
   * {@link #updateAllColMeans()} and {@link #updateAllRowMeans()}. </p>
   * <p>
   * It uses the {@link #currentRows} and {@link #currentCols} for that purpose.
   * </p>
   */
  private void updateValues() {
    BitSet unionRows = new BitSet();
    unionRows.or(currentRows);
    unionRows.or(invertedRows);

    this.currentMean = meanOfSubmatrix(unionRows, currentCols);
    updateAllRowMeans();
    updateAllColMeans();
    this.currentResidue = computeMeanSquaredResidue(unionRows, currentCols);
  }

  /**
   * Computes all row means of the current bicluster ({@link #currentRowMeans}
   * UNION {@link #invertedRows} and {@link #currentColMeans}).
   * 
   * <!-- @param currentRows The currentRows of the sub matrix.
   * 
   * @param currentCols The columns of the sub matrix.
   * @return Returns a map that contains a row mean for every specified row in
   *         <code>currentRows</code>. -->
   */
  private void updateAllRowMeans() {
    BitSet unionRows = new BitSet();
    unionRows.or(currentRows);
    unionRows.or(invertedRows);

    this.rowMeans = new HashMap<Integer, Double>();
    for(int i = unionRows.nextSetBit(0); i >= 0; i = unionRows.nextSetBit(i + 1)) {
      rowMeans.put(i, meanOfRow(i, currentCols));
    }
    // return rowMeans;
  }

  /**
   * Computes all column means of the current bicluster (
   * {@link #currentRowMeans} and {@link #currentColMeans}). <!--
   * 
   * @param currentRows The currentRows of the sub matrix.
   * @param currentCols The columns of the sub matrix.
   * @return Returns a map that contains a column mean for every specified
   *         column in <code>currentCols</code>. -->
   */
  private void updateAllColMeans() {
    this.columnMeans = new HashMap<Integer, Double>();

    BitSet unionRows = new BitSet();
    unionRows.or(currentRows);
    unionRows.or(invertedRows);
    for(int j = currentCols.nextSetBit(0); j >= 0; j = currentCols.nextSetBit(j + 1)) {
      columnMeans.put(j, meanOfCol(unionRows, j));
    }
  }

  /**
   * Calculates the score (= mean squared residue) of the bicluster.</p> It uses
   * {@link #rowMeans}, {@link #columnMeans} and the {@link #currentMean}.
   * 
   * @param rows A BitSet that specifies the current rows of the bicluster
   *        (including the {@link #invertedRows}).
   * @param cols A BitSet that specifies the columns of the bicluster.
   * 
   * @return Returns the score (mean squared residue) of the given bicluster.
   */
  private double computeMeanSquaredResidue(BitSet rows, BitSet cols) {
    double msr = 0.0;
    for(int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
      for(int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
        // TODO: inverted Rows computed with different equation?
        // double val = valueAt(i, j) - meanOfRow(i, cols, addition) +
        // meanOfCols(j, rows, addition) - currentMean;
        double val = valueAt(i, j) - rowMeans.get(i) + columnMeans.get(j) - currentMean;
        msr += (val * val);
      }
    }
    msr /= (rows.cardinality() * cols.cardinality());
    return msr;
  }

  /**
   * Computes the mean of the bicluster that spans over the given rows and
   * columns. Uses the custom {@link #valueAt(int, int)} method.</p>
   * 
   * @param cols A {@link BitSet} that indicates which columns should be used to
   *        compute the mean.
   * @param rows A {@link BitSet} that indicates which rows should be used to
   *        compute the mean. (Including {@link #invertedRows}) <!-- @param
   *        addition A flag that indicates if the mean should be computed in the
   *        addition mode ( <code>true</code>) or not (<code>false</code>). -->
   * @return Returns the mean of the bicluster at the given current rows and
   *         columns.
   * 
   */
  private double meanOfSubmatrix(BitSet rows, BitSet cols) {
    double sum = 0.0;
    for(int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
      for(int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
        sum += valueAt(i, j);
      }
    }
    return sum / (rows.cardinality() * cols.cardinality());
  }

  /**
   * Finds the <b>largest row residue</b> in the current biclsuter by examine
   * the its current row means. The row means and current rows of the current
   * cluster are used. </p>
   * 
   * @return An {@link IntDoublePair} that holds the row as FIRST and the value
   *         of that row residue as SECOND.
   */
  private IntDoublePair getLargestRowResidue() {
    Double max = 0.0;
    BitSet unionRows = new BitSet();
    unionRows.or(currentRows);
    unionRows.or(invertedRows);

    int row = unionRows.nextSetBit(0);
    for(int i = unionRows.nextSetBit(0); i >= 0; i = unionRows.nextSetBit(i + 1)) {
      double rowResidue = computeRowResidue(i, false);
      if(max < rowResidue) {
        max = rowResidue;
        row = i;
      }
    }
    return new IntDoublePair(row, max);
  }

  /**
   * Computes the <b>mean row residue</b> of the given <code>row</code>.
   * 
   * @param row The row who's residue should be computed.
   * @param inverted Indicates if the residue should be computed as the inverted
   *        (<code>true</code>).
   * @return The row residue of the given <code>row</code>.
   */
  private double computeRowResidue(int row, boolean inverted) {
    double rowResidue = 0.0;
    for(int j = currentCols.nextSetBit(0); j >= 0; j = currentCols.nextSetBit(j + 1)) {
      // if rowMeans does not contains the row: recompute it...
      Double rowMean = rowMeans.get(row);
      if(rowMean == null) {
        rowMean = this.meanOfRow(row, currentCols);
        rowMeans.put(row, rowMean);
      }
      Double colMean = columnMeans.get(j);
      if(colMean == null) {
        BitSet unionRows = new BitSet();
        unionRows.or(currentRows);
        unionRows.or(invertedRows);
        colMean = this.meanOfCol(unionRows, j);
        columnMeans.put(j, colMean);
      }
      double val = 0.0;
      if(inverted) {
        val = currentMean + rowMean - colMean - valueAt(row, j);
      }
      else {
        val = valueAt(row, j) - colMean - rowMean + currentMean;
      }
      rowResidue += (val * val);
    }
    return (rowResidue / currentCols.cardinality());
  }

  /**
   * Finds the <b>largest column residue</b> in the current biclsuter by examine
   * the its current column means. The column means and current columns of the
   * current cluster are used. </p>
   * 
   * @return An {@link IntDoublePair} that holds the column as FIRST and the
   *         value of that column residue as SECOND.
   */
  private IntDoublePair getLargestColResidue() {
    Double max = 0.0;
    int col = currentCols.nextSetBit(0);
    for(int j = currentCols.nextSetBit(0); j >= 0; j = currentCols.nextSetBit(j + 1)) {
      double colResidue = computeColResidue(j);
      if(max < colResidue) {
        max = colResidue;
        col = j;
      }
    }
    return new IntDoublePair(col, max);
  }

  /**
   * 
   * Computes the <b>mean column residue</b> of the given <code>col</code>.
   * 
   * @param col The column who's residue should be computed.
   * @return The row residue of the given <code>col</code>um.
   */
  private double computeColResidue(int col) {
    double colResidue = 0.0;
    BitSet unionRows = new BitSet();
    unionRows.or(currentRows);
    unionRows.or(invertedRows);

    for(int i = unionRows.nextSetBit(0); i >= 0; i = unionRows.nextSetBit(i + 1)) {
      Double colMean = columnMeans.get(col);
      // if columnMeans does not contains the column: recompute it...
      if(colMean == null) {
        colMean = this.meanOfCol(unionRows, col);
        columnMeans.put(col, colMean);
      }
      Double rowMean = rowMeans.get(i);
      if(rowMean == null) {
        rowMean = this.meanOfRow(i, currentCols);
        rowMeans.put(i, rowMean);
      }
      double val = valueAt(i, col) - rowMean - colMean + currentMean;
      colResidue += (val * val);
    }
    return (colResidue / unionRows.cardinality());
  }

  /**
   * Returns the newly generated value at the specified position if this
   * position has been masked (see {@link #maskedVals}) or that this position is
   * missing ({@link #missingValues}). Otherwise the value in the database is
   * returned. </p>
   * <p>
   * Overrides {@link AbstractBiclustering#valueAt(int, int)}
   * </p>
   * 
   * @param row The row.
   * @param col The column.
   * @return Returns the fake value if the position (given by row and col) has
   *         been masked or is missing. Otherwise the original value in the
   *         database is returned.
   */
  @Override
  protected double valueAt(int row, int col) {
    IntIntPair key = new IntIntPair(row, col);
    if(maskedVals.containsKey(key)) {
      return maskedVals.get(key);
    }
    if(missingValues.containsKey(key)) {
      return missingValues.get(key);
    }
    return super.valueAt(row, col);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.lmu.ifi.dbs.elki.algorithm.Algorithm#getDescription()
   */
  @Override
  public Description getDescription() {
    Description abs = new Description("ChengAndChurch", "A biclustering method on row- and column score base", "Finding correlated values in a subset of currentRows and a subset of columns", "Y. Cheng and G. M. Church. Biclustering of expression data. In Proceedings of the 8th International Conference on Intelligent Systems for Molecular Biology (ISMB), San Diego, CA, 2000.");
    return abs;
  }

}
