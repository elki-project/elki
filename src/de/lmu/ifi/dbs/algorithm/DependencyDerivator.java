package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.varianceanalysis.LinearLocalPCA;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Dependency derivator computes quantitativly linear dependencies among
 * attributes of a given dataset based on a linear correlation PCA.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class DependencyDerivator<D extends Distance<D>> extends
    DistanceBasedAlgorithm<RealVector, D> {

  /**
   * Parameter for output accuracy (number of fraction digits).
   */
  public static final String OUTPUT_ACCURACY_P = "accuracy";

  /**
   * Default value for output accuracy (number of fraction digits).
   */
  public static final int OUTPUT_ACCURACY_DEFAULT = 4;

  /**
   * Description for parameter output accuracy (number of fraction digits).
   */
  public static final String OUTPUT_ACCURACY_D = "output accuracy fraction digits (>0) (default: "
                                                 + OUTPUT_ACCURACY_DEFAULT + ").";

  /**
   * Parameter for size of random sample.
   */
  public static final String SAMPLE_SIZE_P = "sampleSize";

  /**
   * Description for parameter for size of random sample.
   */
  public static final String SAMPLE_SIZE_D = "size (> 0) of random sample to use (default: use of complete dataset).";

  /**
   * Flag for use of random sample.
   */
  public static final String RANDOM_SAMPLE_F = "randomSample";

  /**
   * Description for flag for use of random sample.
   */
  public static final String RANDOM_SAMPLE_D = "flag to use random sample (use knn query around centroid, if flag is not set). Flag is ignored if no sample size is specified.";

  /**
   * Holds size of sample.
   */
  protected int sampleSize;

  /**
   * Holds the object performing the pca.
   */
  private LinearLocalPCA pca;

  /**
   * Holds the solution.
   */
  protected CorrelationAnalysisSolution solution;

  /**
   * Number format for output of solution.
   */
  public final NumberFormat NF = NumberFormat.getInstance(Locale.US);

  /**
   * Provides a dependency derivator, setting parameters alpha and output
   * accuracy additionally to parameters of super class.
   */
  public DependencyDerivator() {
    super();

    // parameter output accuracy
    IntParameter outputACC = new IntParameter(OUTPUT_ACCURACY_P, OUTPUT_ACCURACY_D, new GreaterEqual(0));
    outputACC.setDefaultValue(Integer.valueOf(OUTPUT_ACCURACY_DEFAULT));
    optionHandler.put(OUTPUT_ACCURACY_P, outputACC);
    
    //parameter sample size
    IntParameter sampleSize = new IntParameter(SAMPLE_SIZE_P, SAMPLE_SIZE_D, new GreaterEqual(0));
    sampleSize.setDefaultValue(Integer.valueOf(-1));
    optionHandler.put(SAMPLE_SIZE_P, sampleSize);

    optionHandler.put(RANDOM_SAMPLE_F, new Flag(RANDOM_SAMPLE_F, RANDOM_SAMPLE_D));
  }

  /**
   * @see Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description(
        "DependencyDerivator",
        "Deriving numerical inter-dependencies on data",
        "Derives an equality-system describing dependencies between attributes in a correlation-cluster",
        "unpublished");
  }

  /**
   * Runs the pca.
   *
   * @param db the database
   * @see AbstractAlgorithm#runInTime(Database)
   */
  public void runInTime(Database<RealVector> db) throws IllegalStateException {
    if (isVerbose()) {
      verbose("retrieving database objects...");
    }
    List<Integer> dbIDs = new ArrayList<Integer>();
    for (Iterator<Integer> idIter = db.iterator(); idIter.hasNext();) {
      dbIDs.add(idIter.next());
    }
    RealVector centroidDV = Util.centroid(db, dbIDs);
    List<Integer> ids;
    if (this.sampleSize >= 0) {
      if (optionHandler.isSet(RANDOM_SAMPLE_F)) {
        ids = db.randomSample(this.sampleSize, 1);
      }
      else {
        List<QueryResult<D>> queryResults = db
            .kNNQueryForObject(centroidDV, this.sampleSize, this
                .getDistanceFunction());
        ids = new ArrayList<Integer>(this.sampleSize);
        for (QueryResult<D> qr : queryResults) {
          ids.add(qr.getID());
        }
      }
    }
    else {
      ids = dbIDs;
    }
    if (isVerbose()) {
      verbose("PCA...");
    }

    pca.run(ids, db);
    Matrix weakEigenvectors = pca.getEigenvectors().times(pca.selectionMatrixOfWeakEigenvectors());

    Matrix transposedWeakEigenvectors = weakEigenvectors.transpose();
    if (this.debug) {
      StringBuilder log = new StringBuilder();
      log.append("strong Eigenvectors:\n");
      log.append(pca.getEigenvectors().times(pca.selectionMatrixOfStrongEigenvectors()).toString(NF));
      log.append('\n');
      log.append("transposed weak Eigenvectors:\n");
      log.append(transposedWeakEigenvectors.toString(NF));
      log.append('\n');
      log.append("Eigenvalues:\n");
      log.append(Util.format(pca.getEigenvalues(), " , ", 2));
      log.append('\n');
      debugFine(log.toString());
    }
    Matrix centroid = centroidDV.getColumnVector();
    Matrix B = transposedWeakEigenvectors.times(centroid);
    if (this.debug) {
      StringBuilder log = new StringBuilder();
      log.append("Centroid:\n");
      log.append(centroid);
      log.append('\n');
      log.append("tEV * Centroid\n");
      log.append(B);
      log.append('\n');
      debugFine(log.toString());
    }

    Matrix gaussJordan = new Matrix(transposedWeakEigenvectors.getRowDimensionality(),
                                    transposedWeakEigenvectors.getColumnDimensionality() + B.getColumnDimensionality());
    gaussJordan.setMatrix(0,
                          transposedWeakEigenvectors.getRowDimensionality() - 1, 0,
                          transposedWeakEigenvectors.getColumnDimensionality() - 1,
                          transposedWeakEigenvectors);
    gaussJordan.setMatrix(0, gaussJordan.getRowDimensionality() - 1,
                          transposedWeakEigenvectors.getColumnDimensionality(), gaussJordan
        .getColumnDimensionality() - 1, B);

    if (isVerbose()) {
      verbose("Gauss-Jordan-Elimination of "
              + gaussJordan.toString(NF));
    }

    double[][] a = new double[transposedWeakEigenvectors.getRowDimensionality()][transposedWeakEigenvectors
        .getColumnDimensionality()];
    double[][] we = transposedWeakEigenvectors.getArray();
    double[] b = B.getColumn(0).getRowPackedCopy();
    System.arraycopy(we, 0, a, 0, transposedWeakEigenvectors
        .getRowDimensionality());

    LinearEquationSystem lq = new LinearEquationSystem(a, b);
    lq.solveByTotalPivotSearch();

    // System.out.println("gaussJordanElimination ");
    // System.out.println(gaussJordan.gaussJordanElimination().toString(NF));
    // System.out.println("exact gaussJordanElimination");
    // System.out.println(gaussJordan.exactGaussJordanElimination().toString(NF));
    // Matrix solution =gaussJordan.gaussJordanElimination();
    // Matrix solution = gaussJordan.exactGaussJordanElimination();

    Matrix strongEigenvectors = pca.getEigenvectors().times(
        pca.selectionMatrixOfStrongEigenvectors());
    this.solution = new CorrelationAnalysisSolution(lq, db,
                                                    strongEigenvectors, centroid, NF);

    if (isVerbose()) {
      StringBuilder log = new StringBuilder();
      log.append("Solution:");
      log.append('\n');
      log.append("Standard deviation "
                 + this.solution.getStandardDeviation());
      log.append('\n');
      log.append(lq.equationsToString(NF.getMaximumFractionDigits()));
      log.append('\n');
      verbose(log.toString());
    }
  }

  /**
   * @see Algorithm#getResult()
   */
  public CorrelationAnalysisSolution getResult() {
    return solution;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // accuracy
    int accuracy;
    if (optionHandler.isSet(OUTPUT_ACCURACY_P)) {
      String accuracyString = optionHandler
          .getOptionValue(OUTPUT_ACCURACY_P);
      try {
        accuracy = Integer.parseInt(accuracyString);
        if (accuracy < 0) {
          throw new WrongParameterValueException(OUTPUT_ACCURACY_P,
                                                 accuracyString, OUTPUT_ACCURACY_D);
        }
      }
      catch (NumberFormatException e) {
        throw new WrongParameterValueException(OUTPUT_ACCURACY_P,
                                               accuracyString, OUTPUT_ACCURACY_D, e);
      }
    }
    else {
      accuracy = OUTPUT_ACCURACY_DEFAULT;
    }
    NF.setMaximumFractionDigits(accuracy);
    NF.setMinimumFractionDigits(accuracy);

    // sample size
    if (optionHandler.isSet(SAMPLE_SIZE_P)) {
      String sampleSizeString = optionHandler
          .getOptionValue(SAMPLE_SIZE_P);
      try {
        int sampleSize = Integer.parseInt(sampleSizeString);
        if (sampleSize < 0) {
          throw new WrongParameterValueException(SAMPLE_SIZE_P,
                                                 sampleSizeString, SAMPLE_SIZE_D);
        }
      }
      catch (NumberFormatException e) {
        throw new WrongParameterValueException(SAMPLE_SIZE_P,
                                               sampleSizeString, SAMPLE_SIZE_D, e);
      }
    }
    else {
      sampleSize = -1;
    }

    // pca
    pca = new LinearLocalPCA();
    remainingParameters = pca.setParameters(remainingParameters);
    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Returns the parameter setting of this algorithm.
   *
   * @return the parameter setting of this algorithm
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super
        .getAttributeSettings();
    AttributeSettings mySettings = attributeSettings.get(0);

    if (optionHandler.isSet(SAMPLE_SIZE_P)) {
      mySettings.addSetting(SAMPLE_SIZE_P, Integer.toString(sampleSize));
    }
    attributeSettings.addAll(pca.getAttributeSettings());
    return attributeSettings;
  }
}