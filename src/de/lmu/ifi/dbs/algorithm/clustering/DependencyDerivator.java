package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.linearalgebra.Matrix;
import de.lmu.ifi.dbs.pca.LinearLocalPCA;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

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
public class DependencyDerivator<D extends Distance<D>> extends DistanceBasedAlgorithm<DoubleVector, D> {

  /**
   * Parameter name for alpha - threshold to discern strong from weak
   * Eigenvectors.
   */
  public static final String ALPHA_P = "alpha";

  /**
   * Default value for alpha.
   */
  public static final double ALPHA_DEFAULT = 0.85;

  /**
   * Description for parameter alpha - threshold to discern strong from weak
   * Eigenvectors.
   */
  public static final String ALPHA_D = "<double>threshold to discern strong from weak Eigenvectors ([0:1)) - default: " + ALPHA_DEFAULT + ". Corresponds to the percentage of variance, that is to be explained by a set of strongest Eigenvectors.";

  /**
   * Parameter for correlation dimensionality.
   */
  public static final String DIMENSIONALITY_P = "corrdim";

  /**
   * Description for parameter correlation dimensionality.
   */
  public static final String DIMENSIONALITY_D = "<int>desired correlation dimensionality (> 0 - number of strong Eigenvectors). This parameter is ignored, if parameter " + ALPHA_P + " is set.";

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
  public static final String OUTPUT_ACCURACY_D = "<integer>output accuracy fraction digits (default: " + OUTPUT_ACCURACY_DEFAULT + ").";

  /**
   * Parameter for size of random sample.
   */
  public static final String SAMPLE_SIZE_P = "sampleSize";

  /**
   * Description for parameter for size of random sample.
   */
  public static final String SAMPLE_SIZE_D = "<int>size (> 0) of random sample to use (default: use of complete dataset).";

  /**
   * Flag for use of random sample.
   */
  public static final String RANDOM_SAMPLE_F = "randomSample";

  /**
   * Description for flag for use of random sample.
   */
  public static final String RANDOM_SAMPLE_D = "flag to use random sample (use knn query around centroid, if flag is not set). Flag is ignored if no sample size is specified.";

  /**
   * Holds alpha.
   */
  protected double alpha;

  /**
   * Holds the correlation dimensionality.
   */
  protected int corrdim;

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
  protected final NumberFormat NF = NumberFormat.getInstance(Locale.US);

  /**
   * Provides a dependency derivator, setting parameters alpha and output
   * accuracy additionally to parameters of super class.
   */
  public DependencyDerivator() {
    super();
    parameterToDescription.put(ALPHA_P + OptionHandler.EXPECTS_VALUE, ALPHA_D);
    parameterToDescription.put(OUTPUT_ACCURACY_P + OptionHandler.EXPECTS_VALUE, OUTPUT_ACCURACY_D);
    parameterToDescription.put(SAMPLE_SIZE_P + OptionHandler.EXPECTS_VALUE, SAMPLE_SIZE_D);
    parameterToDescription.put(RANDOM_SAMPLE_F, RANDOM_SAMPLE_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * @see Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description("DependencyDerivator", "Deriving numerical inter-dependencies on data", "Derives an equality-system describing dependencies between attributes in a correlation-cluster", "unpublished");
  }

  /**
   * Calls {@link #runInTime(Database, Integer) run(db,null)}.
   *
   * @see AbstractAlgorithm#runInTime(Database)
   */
  protected void runInTime(Database<DoubleVector> db) throws IllegalStateException {
    runInTime(db, null);
  }

  /**
   * Runs the pca with the specified correlation dimensionality.
   *
   * @param db                        the database
   * @param correlationDimensionality the desired correlation dimensionality - may remain null, then
   *                                  the parameter setting is used as usual
   * @throws IllegalStateException
   */
  public void runInTime(Database<DoubleVector> db, Integer correlationDimensionality) throws IllegalStateException {
    if (isVerbose()) {
      System.out.println("retrieving database objects...");
    }
    List<Integer> dbIDs = new ArrayList<Integer>();
    for (Iterator<Integer> idIter = db.iterator(); idIter.hasNext();) {
      dbIDs.add(idIter.next());
    }
    DoubleVector centroidDV = Util.centroid(db, dbIDs);
    List<Integer> ids;
    if (this.sampleSize >= 0) {
      if (optionHandler.isSet(RANDOM_SAMPLE_F)) {
        ids = db.randomSample(this.sampleSize, 1);
      }
      else {
        List<QueryResult<D>> queryResults = db.kNNQueryForObject(centroidDV, this.sampleSize, this.getDistanceFunction());
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
      System.out.println("PCA...");
    }
    if (correlationDimensionality != null) {
      pca.run(ids, db, correlationDimensionality);
    }
    else if (!optionHandler.isSet(ALPHA_P) && optionHandler.isSet(DIMENSIONALITY_P)) {
      pca.run(ids, db, corrdim);
    }
    else {
      pca.run(ids, db, alpha);
    }

    Matrix weakEigenvectors = pca.getEigenvectors().times(pca.getSelectionMatrixOfWeakEigenvectors());

    Matrix transposedWeakEigenvectors = weakEigenvectors.transpose();
    if (isVerbose()) {
      System.out.println("transposed weak Eigenvectors:");
      System.out.println(transposedWeakEigenvectors);
      System.out.println("Eigenvalues:");
      System.out.println(Util.format(pca.getEigenvalues(), " , ", 2));
    }
    Matrix centroid = centroidDV.getColumnVector();
    Matrix B = transposedWeakEigenvectors.times(centroid);
    if (isVerbose()) {
      System.out.println("Centroid:");
      System.out.println(centroid);
      System.out.println("tEV * Centroid");
      System.out.println(B);
    }

    Matrix gaussJordan = new Matrix(transposedWeakEigenvectors.getRowDimension(), transposedWeakEigenvectors.getColumnDimension() + B.getColumnDimension());
    gaussJordan.setMatrix(0, transposedWeakEigenvectors.getRowDimension() - 1, 0, transposedWeakEigenvectors.getColumnDimension() - 1, transposedWeakEigenvectors);
    gaussJordan.setMatrix(0, gaussJordan.getRowDimension() - 1, transposedWeakEigenvectors.getColumnDimension(), gaussJordan.getColumnDimension() - 1, B);

    if (isVerbose()) {
      System.out.println("Gauss-Jordan-Elimination of " + gaussJordan);

      Iterator<Integer> it = db.iterator();
      while (it.hasNext()) {
        Integer id = it.next();
        DoubleVector dv = db.get(id);

        double[][] values = new double[dv.getDimensionality()][1];
        for (int i = 1; i <= dv.getDimensionality(); i++) {
          values[i - 1][0] = dv.getValue(i);
        }
      }
    }

    Matrix solution = gaussJordan.exactGaussJordanElimination();
    if (isVerbose()) {
      System.out.println("Solution:");
      System.out.println(solution.toString(NF));
    }

    Matrix strongEigenvectors = pca.getEigenvectors().times(pca.getSelectionMatrixOfStrongEigenvectors());
    this.solution = new CorrelationAnalysisSolution(solution, db, strongEigenvectors, centroid, NF);
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
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = super.setParameters(args);
    try {
      if (optionHandler.isSet(ALPHA_P)) {
        alpha = Double.parseDouble(optionHandler.getOptionValue(ALPHA_P));
      }
      else {
        if (optionHandler.isSet(DIMENSIONALITY_P)) {
          try {
            corrdim = Integer.parseInt(optionHandler.getOptionValue(DIMENSIONALITY_P));
            if (corrdim < 0) {
              throw new NumberFormatException("negative integer");
            }
          }
          catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter " + DIMENSIONALITY_P + " is of wrong format: " + optionHandler.getOptionValue(DIMENSIONALITY_P) + " must be parseable as non-negative integer.");
          }
        }
        alpha = ALPHA_DEFAULT;
      }
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException("Parameter " + ALPHA_P + " is of invalid format: " + optionHandler.getOptionValue(ALPHA_P) + ". Must be parseable as double-value.");
    }
    try {
      int accuracy = OUTPUT_ACCURACY_DEFAULT;
      if (optionHandler.isSet(OUTPUT_ACCURACY_P)) {
        accuracy = Integer.parseInt(optionHandler.getOptionValue(OUTPUT_ACCURACY_P));
        if (accuracy < 0) {
          throw new NumberFormatException("Accuracy negative: " + optionHandler.getOptionValue(OUTPUT_ACCURACY_P));
        }
      }
      NF.setMaximumFractionDigits(accuracy);
      NF.setMinimumFractionDigits(accuracy);
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException("Parameter " + OUTPUT_ACCURACY_P + " is of invalid format: " + optionHandler.getOptionValue(OUTPUT_ACCURACY_P) + ". Must be parseable as non-negative integer.");
    }
    if (optionHandler.isSet(SAMPLE_SIZE_P)) {
      try {
        int sampleSize = Integer.parseInt(optionHandler.getOptionValue(SAMPLE_SIZE_P));
        if (sampleSize < 0) {
          throw new IllegalArgumentException("Parameter " + SAMPLE_SIZE_P + " is of invalid format: " + optionHandler.getOptionValue(SAMPLE_SIZE_P) + ". Must be a non-negative integer.");
        }
        else {
          this.sampleSize = sampleSize;
        }
      }
      catch (NumberFormatException e) {
        throw new IllegalArgumentException("Parameter " + SAMPLE_SIZE_P + " is of invalid format: " + optionHandler.getOptionValue(SAMPLE_SIZE_P) + ". Must be parseable as non-negative integer.");
      }
    }
    else {
      sampleSize = -1;
    }

    pca = new LinearLocalPCA();
    return pca.setParameters(remainingParameters);
  }

  /**
   * Returns the parameter setting of this algorithm.
   *
   * @return the parameter setting of this algorithm
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = super.getAttributeSettings();
    AttributeSettings attributeSettings = result.get(0);

    if (optionHandler.isSet(ALPHA_P) || !optionHandler.isSet(DIMENSIONALITY_P))
      attributeSettings.addSetting(ALPHA_P, Double.toString(alpha));

    if (optionHandler.isSet(DIMENSIONALITY_P))
      attributeSettings.addSetting(DIMENSIONALITY_P, Integer.toString(corrdim));

    if (optionHandler.isSet(SAMPLE_SIZE_P))
      attributeSettings.addSetting(SAMPLE_SIZE_P, Integer.toString(sampleSize));

    return result;
  }
}
