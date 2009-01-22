package de.lmu.ifi.dbs.elki.algorithm;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.data.model.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.varianceanalysis.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.varianceanalysis.PCAFilteredRunner;

/**
 * Dependency derivator computes quantitatively linear dependencies among
 * attributes of a given dataset based on a linear correlation PCA. <p/>
 * Reference: <br>
 * E. Achtert, C. Boehm, H.-P. Kriegel, P. Kroeger, A. Zimek: Deriving
 * Quantitative Dependencies for Correlation Clusters. <br>
 * In Proc. 12th Int. Conf. on Knowledge Discovery and Data Mining (KDD '06),
 * Philadelphia, PA 2006. </p>
 * 
 * @author Arthur Zimek
 * @param <V> the type of RealVector handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 */
public class DependencyDerivator<V extends RealVector<V, ?>, D extends Distance<D>> extends DistanceBasedAlgorithm<V, D, CorrelationAnalysisSolution<V>> {
  /**
   * OptionID for {@link #RANDOM_SAMPLE_FLAG}
   */
  public static final OptionID DEPENDENCY_DERIVATOR_RANDOM_SAMPLE = OptionID.getOrCreateOptionID(
      "derivator.randomSample", 
      "Flag to use random sample (use knn query around centroid, if flag is not set).");

  /**
   * OptionID for {@link #OUTPUT_ACCURACY_PARAM}
   */
  public static final OptionID OUTPUT_ACCURACY_ID = OptionID.getOrCreateOptionID(
      "derivator.accuracy", "Threshold for output accuracy fraction digits.");

  /**
   * Parameter to specify the threshold for output accuracy fraction digits,
   * must be an integer equal to or greater than 0. <p/> Default value: {@code
   * 4} </p> <p/> Key: {@code -derivator.accuracy} </p>
   */
  private final IntParameter OUTPUT_ACCURACY_PARAM = new IntParameter(OUTPUT_ACCURACY_ID, 
      new GreaterEqualConstraint(0), 4);

  /**
   * OptionID for
   * {@link de.lmu.ifi.dbs.elki.algorithm.DependencyDerivator#SAMPLE_SIZE_PARAM}
   */
  public static final OptionID SAMPLE_SIZE_ID = OptionID.getOrCreateOptionID("derivator.sampleSize", 
      "Threshold for the size of the random sample to use. " 
      + "Default value is size of the complete dataset.");

  /**
   * Optional parameter to specify the treshold for the size of the random
   * sample to use, must be an integer greater than 0. <p/> Default value: the
   * size of the complete dataset </p> <p/> Key: {@code -derivator.sampleSize}
   * </p>
   */
  private final IntParameter SAMPLE_SIZE_PARAM = new IntParameter(SAMPLE_SIZE_ID, 
      new GreaterConstraint(0), true);

  /**
   * Holds the value of {@link #SAMPLE_SIZE_PARAM}.
   */
  private Integer sampleSize;

  /**
   * Flag to use random sample (use knn query around centroid, if flag is not
   * set). <p/> Key: {@code -derivator.randomSample} </p>
   */
  private final Flag RANDOM_SAMPLE_FLAG = new Flag(DEPENDENCY_DERIVATOR_RANDOM_SAMPLE);

  /**
   * Holds the object performing the pca.
   */
  private PCAFilteredRunner<V> pca = new PCAFilteredRunner<V>();

  /**
   * Holds the solution.
   */
  private CorrelationAnalysisSolution<V> solution;

  /**
   * Number format for output of solution.
   */
  public final NumberFormat NF = NumberFormat.getInstance(Locale.US);

  /**
   * Provides a dependency derivator, adding parameters
   * {@link DependencyDerivator#OUTPUT_ACCURACY_PARAM},
   * {@link de.lmu.ifi.dbs.elki.algorithm.DependencyDerivator#SAMPLE_SIZE_PARAM}
   * , and flag
   * {@link de.lmu.ifi.dbs.elki.algorithm.DependencyDerivator#RANDOM_SAMPLE_FLAG}
   * to the option handler additionally to parameters of super class.
   */
  public DependencyDerivator() {
    super();

    // parameter output accuracy
    addOption(OUTPUT_ACCURACY_PARAM);

    // parameter sample size
    addOption(SAMPLE_SIZE_PARAM);

    // random sample
    addOption(RANDOM_SAMPLE_FLAG);
  }

  public Description getDescription() {
    return new Description("DependencyDerivator", "Deriving numerical inter-dependencies on data", "Derives an equality-system describing dependencies between attributes in a correlation-cluster", "E. Achtert, C. Boehm, H.-P. Kriegel, P. Kroeger, A. Zimek: " + "Deriving Quantitative Dependencies for Correlation Clusters. " + "In Proc. 12th Int. Conf. on Knowledge Discovery and Data Mining (KDD '06), Philadelphia, PA 2006.");
  }

  /**
   * Computes quantitatively linear dependencies among the attributes of the
   * given database based on a linear correlation PCA.
   * 
   * @param db the database to run this DependencyDerivator on
   * @return the CorrelationAnalysisSolution computed by this DependencyDerivator
   */
  @Override
  public CorrelationAnalysisSolution<V> runInTime(Database<V> db) throws IllegalStateException {
    if(isVerbose()) {
      verbose("retrieving database objects...");
    }
    Set<Integer> dbIDs = new HashSet<Integer>();
    for(Iterator<Integer> idIter = db.iterator(); idIter.hasNext();) {
      dbIDs.add(idIter.next());
    }
    V centroidDV = Util.centroid(db, dbIDs);
    Set<Integer> ids;
    if(this.sampleSize != null) {
      if(RANDOM_SAMPLE_FLAG.isSet()) {
        ids = db.randomSample(this.sampleSize, 1);
      }
      else {
        List<QueryResult<D>> queryResults = db.kNNQueryForObject(centroidDV, this.sampleSize, this.getDistanceFunction());
        ids = new HashSet<Integer>(this.sampleSize);
        for(QueryResult<D> qr : queryResults) {
          ids.add(qr.getID());
        }
      }
    }
    else {
      ids = dbIDs;
    }

    this.solution = generateModel(db, ids, centroidDV);
    return this.solution;
  }

  /**
   * Runs the pca on the given set of IDs. The centroid is computed from the
   * given ids.
   * 
   * @param db the database
   * @param ids the set of ids
   * @return a matrix of equations describing the dependencies
   */
  public CorrelationAnalysisSolution<V> generateModel(Database<V> db, Collection<Integer> ids) {
    V centroidDV = Util.centroid(db, ids);
    return generateModel(db, ids, centroidDV);
  }

  /**
   * Runs the pca on the given set of IDs and for the given centroid.
   * 
   * @param db the database
   * @param ids the set of ids
   * @param centroidDV the centroid
   * @return a matrix of equations describing the dependencies
   */
  public CorrelationAnalysisSolution<V> generateModel(Database<V> db, Collection<Integer> ids, V centroidDV) {
    CorrelationAnalysisSolution<V> sol;
    if(isVerbose()) {
      verbose("PCA...");
    }

    PCAFilteredResult pcares = pca.processIds(ids, db);
    // Matrix weakEigenvectors =
    // pca.getEigenvectors().times(pca.selectionMatrixOfWeakEigenvectors());
    Matrix weakEigenvectors = pcares.getWeakEigenvectors();
    // Matrix strongEigenvectors =
    // pca.getEigenvectors().times(pca.selectionMatrixOfStrongEigenvectors());
    Matrix strongEigenvectors = pcares.getStrongEigenvectors();
    Vector centroid = centroidDV.getColumnVector();

    // TODO: what if we don't have any weak eigenvectors?
    if(weakEigenvectors.getColumnDimensionality() == 0) {
      sol = new CorrelationAnalysisSolution<V>(null, db, strongEigenvectors, weakEigenvectors, pcares.similarityMatrix(), centroid, NF);
    }
    else {
      Matrix transposedWeakEigenvectors = weakEigenvectors.transpose();
      if(this.debug) {
        StringBuilder log = new StringBuilder();
        log.append("strong Eigenvectors:\n");
        log.append(pcares.getEigenvectors().times(pcares.selectionMatrixOfStrongEigenvectors()).toString(NF));
        log.append('\n');
        log.append("transposed weak Eigenvectors:\n");
        log.append(transposedWeakEigenvectors.toString(NF));
        log.append('\n');
        log.append("Eigenvalues:\n");
        log.append(Util.format(pcares.getEigenvalues(), " , ", 2));
        log.append('\n');
        debugFine(log.toString());
      }
      Matrix B = transposedWeakEigenvectors.times(centroid);
      if(this.debug) {
        StringBuilder log = new StringBuilder();
        log.append("Centroid:\n");
        log.append(centroid);
        log.append('\n');
        log.append("tEV * Centroid\n");
        log.append(B);
        log.append('\n');
        debugFine(log.toString());
      }

      Matrix gaussJordan = new Matrix(transposedWeakEigenvectors.getRowDimensionality(), transposedWeakEigenvectors.getColumnDimensionality() + B.getColumnDimensionality());
      gaussJordan.setMatrix(0, transposedWeakEigenvectors.getRowDimensionality() - 1, 0, transposedWeakEigenvectors.getColumnDimensionality() - 1, transposedWeakEigenvectors);
      gaussJordan.setMatrix(0, gaussJordan.getRowDimensionality() - 1, transposedWeakEigenvectors.getColumnDimensionality(), gaussJordan.getColumnDimensionality() - 1, B);

      if(isVerbose()) {
        verbose("Gauss-Jordan-Elimination of " + gaussJordan.toString(NF));
      }

      double[][] a = new double[transposedWeakEigenvectors.getRowDimensionality()][transposedWeakEigenvectors.getColumnDimensionality()];
      double[][] we = transposedWeakEigenvectors.getArray();
      double[] b = B.getColumn(0).getRowPackedCopy();
      System.arraycopy(we, 0, a, 0, transposedWeakEigenvectors.getRowDimensionality());

      LinearEquationSystem lq = new LinearEquationSystem(a, b);
      lq.solveByTotalPivotSearch();

      sol = new CorrelationAnalysisSolution<V>(lq, db, strongEigenvectors, pcares.getWeakEigenvectors(), pcares.similarityMatrix(), centroid, NF);

      if(isVerbose()) {
        StringBuilder log = new StringBuilder();
        log.append("Solution:");
        log.append('\n');
        log.append("Standard deviation ").append(sol.getStandardDeviation());
        log.append('\n');
        log.append(lq.equationsToString(NF.getMaximumFractionDigits()));
        log.append('\n');
        verbose(log.toString());
      }
    }
    return sol;
  }

  public CorrelationAnalysisSolution<V> getResult() {
    return solution;
  }
  
  /**
   * Calls the super method and sets additionally the values of the parameters
   * {@link #OUTPUT_ACCURACY_PARAM} and {@link #SAMPLE_SIZE_PARAM}. The
   * remaining parameters are passed to the {@link #pca}.
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // accuracy
    int accuracy = OUTPUT_ACCURACY_PARAM.getValue();
    NF.setMaximumFractionDigits(accuracy);
    NF.setMinimumFractionDigits(accuracy);

    // sample size
    if(SAMPLE_SIZE_PARAM.isSet()) {
      sampleSize = SAMPLE_SIZE_PARAM.getValue();
    }

    remainingParameters = pca.setParameters(remainingParameters);

    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Calls the super method and adds to the returned attribute settings the
   * attribute settings of the {@link #pca}.
   */
  @Override
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();
    attributeSettings.addAll(pca.getAttributeSettings());
    return attributeSettings;
  }

  /**
   * Calls the super method and appends the parameter description of
   * {@link #pca}.
   */
  @Override
  public String parameterDescription() {
    StringBuilder description = new StringBuilder();
    description.append(super.parameterDescription());
    description.append(pca.parameterDescription());
    return description.toString();
  }
}
