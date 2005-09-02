package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.linearalgebra.Matrix;
import de.lmu.ifi.dbs.pca.CorrelationPCA;
import de.lmu.ifi.dbs.pca.LinearCorrelationPCA;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Dependency derivator computes quantitativly linear dependencies
 * among attributes of a given dataset based on a linear correlation PCA.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class DependencyDerivator extends AbstractAlgorithm<DoubleVector> {

  /**
   * Parameter name for alpha - threshold to discern strong from weak Eigenvectors.
   */
  public static final String ALPHA_P = "alpha";

  /**
   * Default value for alpha.
   */
  public static final double ALPHA_DEFAULT = 0.8;

  /**
   * Description for parameter alpha - threshold to discern strong from weak Eigenvectors.
   */
  public static final String ALPHA_D = "<double>threshold to discern strong from weak Eigenvectors ([0:1)) - default: " + ALPHA_DEFAULT + ". Corresponds to the percentage of variance, that is to be explained by a set of strongest Eigenvectors.";

  /**
   * Parameter for output accuracy (number of fraction digits).
   */
  public static final String OUTPUT_ACCURACY_P = "accuracy";

  /**
   * Default value for output accuracy (number of fraction digits).
   */
  public static final int OUTPUT_ACCURACY_DEFAULT = 2;

  /**
   * Description for parameter output accuracy (number of fraction digits).
   */
  public static final String OUTPUT_ACCURACY_D = "<integer>output accuracy fraction digits (default: " + OUTPUT_ACCURACY_DEFAULT + ").";

  /**
   * Holds alpha.
   */
  protected double alpha;

  /**
   * Holds the solution.
   */
  protected Result solution;

  /**
   * Number format for output of solution.
   */
  protected final NumberFormat NF = NumberFormat.getInstance(Locale.US);

  /**
   * Provides a dependency derivator, setting parameters
   * alpha and output accuracy
   * additionally to parameters of super class.
   */
  public DependencyDerivator() {
    super();
    parameterToDescription.put(ALPHA_P + OptionHandler.EXPECTS_VALUE, ALPHA_D);
    parameterToDescription.put(OUTPUT_ACCURACY_P + OptionHandler.EXPECTS_VALUE, OUTPUT_ACCURACY_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * @see Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description("DependencyDerivator",
                           "Deriving numerical inter-dependencies on data",
                           "Derives an equality-system describing dependencies between attributes in a correlation-cluster",
                           "unpublished");
  }

  /**
   * @see Algorithm#run(de.lmu.ifi.dbs.database.Database)
   */
  public void run(Database<DoubleVector> db) throws IllegalStateException {
    long start = System.currentTimeMillis();
    if (isVerbose()) {
      System.out.println("retrieving database objects...");
    }
    List<Integer> ids = new ArrayList<Integer>();
    for (Iterator<Integer> idIter = db.iterator(); idIter.hasNext();) {
      ids.add(idIter.next());
    }
    if (isVerbose()) {
      System.out.println("PCA...");
    }
    CorrelationPCA pca = new LinearCorrelationPCA();
    pca.run(ids, db, alpha);

    Matrix weakEigenvectors = pca.getEigenvectors().times(pca.getSelectionMatrixOfWeakEigenvectors());

    Matrix transposedWeakEigenvectors = weakEigenvectors.transpose();
    if (isVerbose()) {
      System.out.println("transposed weak Eigenvectors:");
      System.out.println(transposedWeakEigenvectors);
      System.out.println("Eigenvalues:");
      System.out.println(Util.format(pca.getEigenvalues(), " , ", 2));
    }
    Matrix centroid = Util.centroid(db, ids).getVector();
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

    // System.out.println(gaussJordan);
    if (false) // normalization
    {
      if (isVerbose()) {
        System.out.println("Resizing matrix...");
      }
      for (int row = 0; row < gaussJordan.getRowDimension(); row++) {
        double sum = 0.0;
        for (int col = 0; col < gaussJordan.getColumnDimension() - 1; col++) {
          //sum += db.summand(col) * gaussJordan.get(row, col) / db.factor(col);
          //gaussJordan.set(row, col, gaussJordan.get(row, col) / db.factor(col));
        }
        gaussJordan.set(row, gaussJordan.getColumnDimension() - 1, gaussJordan.get(row, gaussJordan.getColumnDimension() - 1) + sum);
      }
    }
    if (isVerbose()) {
      System.out.println("Gauss-Jordan-Elimination...");
    }
    Matrix solution = gaussJordan.gaussJordanElimination();
    if (isVerbose()) {
      System.out.println("Solution:");
      System.out.println(solution.toString(NF));
    }
    this.solution = new CorrelationAnalysisSolution(solution, NF);

    long end = System.currentTimeMillis();
    if (isTime()) {
      long elapsedTime = end - start;
      System.out.println(this.getClass().getName() + " runtime: " + elapsedTime + " milliseconds.");
    }
  }


  /**
   * @see Algorithm#getResult()
   */
  public Result getResult() {
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
    return remainingParameters;
  }
}
