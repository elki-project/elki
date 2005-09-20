package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.linearalgebra.Matrix;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Iterator;

/**
 * A solution of correlation analysis is a matrix of equations describing the
 * dependencies.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class CorrelationAnalysisSolution extends AbstractResult<DoubleVector> {
  /**
   * Matrix to store the solution equations.
   */
  private Matrix solution;

  /**
   * Number format for output accuracy.
   */
  private NumberFormat nf;

  /**
   * The dimensionality of the correlation.
   */
  private int correlationDimensionality;

  /**
   * Provides a new CorrelationAnalysisSolution holding the specified matrix.
   * <p/>
   *
   * @param solution                  the matrix describing the solution equations
   * @param db                        the database containing the objects
   * @param correlationDimensionality the dimensionality of the correlation
   * @param parameters the parameter setting of the algorithm to which this result belongs to
   */
  public CorrelationAnalysisSolution(Matrix solution, Database<DoubleVector> db,
                                     int correlationDimensionality, String[] parameters) {

    this(solution, db, correlationDimensionality, null, parameters);
  }

  /**
   * Provides a new CorrelationAnalysisSolution holding the specified matrix and number format.
   *
   * @param solution the matrix describing the solution equations
   *                 * @param correlationDimensionality the dimensionality of the correlation
   * @param nf       the number format for output accuracy
   * @param parameters the parameter setting of the algorithm to which this result belongs to
   */
  public CorrelationAnalysisSolution(Matrix solution, Database<DoubleVector> db,
                                     int correlationDimensionality, NumberFormat nf, String[] parameters) {
    super(db, parameters);

    this.solution = solution;
    this.correlationDimensionality = correlationDimensionality;
    this.nf = nf;
  }


  /**
   * @see Result#output(File, Normalization)
   */
  public void output(File out, Normalization<DoubleVector> normalization) throws UnableToComplyException {
    PrintStream outStream;
    try {
      outStream = new PrintStream(new FileOutputStream(out));
    }
    catch (Exception e) {
      outStream = new PrintStream(new FileOutputStream(FileDescriptor.out));
    }

    try {
      writeHeader(outStream, normalization);
    }
    catch (NonNumericFeaturesException e) {
      throw new UnableToComplyException(e);
    }

    // print solution
    Matrix printSolution;
    try {
      printSolution = getPrintSolution(normalization);
    }
    catch (NonNumericFeaturesException e) {
      throw new UnableToComplyException(e);
    }

    // determine lower and upper deviations
    double[][] deviations;
    try {
      deviations = getPrintDeviations(normalization, printSolution);
    }
    catch (NonNumericFeaturesException e) {
      throw new UnableToComplyException(e);
    }
    double[] lowerDeviations = deviations[0];
    double[] upperDeviations = deviations[1];

    // output
    if (this.nf == null) {
      outStream.println("lower deviations: " + Util.format(lowerDeviations));
      outStream.println("upper deviations: " + Util.format(upperDeviations));
      outStream.println(printSolution.toString());
    }
    else {
      outStream.println("lower deviations: " + Util.format(lowerDeviations, nf));
      outStream.println("upper deviations: " + Util.format(upperDeviations, nf));
      outStream.println(printSolution.toString(nf));
    }

    outStream.flush();
  }

  /**
   * Returns the matrix that stores the solution equations.
   *
   * @return the matrix that stores the solution equations
   */
  public Matrix getSolutionMatrix() {
    return solution;
  }

  /**
   * Returns the deviations in each equation for printing purposes.
   *
   * @param normalization the normalization, can be null
   * @param printSolution the denormalized solution matrix
   * @return the deviations in each equation, the first argument are the lower deviations, the second argument
   *         are the upper deviations.
   * @throws NonNumericFeaturesException
   */
  public double[][] getPrintDeviations(Normalization<DoubleVector> normalization,
                                       Matrix printSolution) throws NonNumericFeaturesException {
    int dim = db.dimensionality();
    double[] lowerDeviations = new double[dim - correlationDimensionality];
    double[] upperDeviations = new double[dim - correlationDimensionality];

    Arrays.fill(lowerDeviations, Double.MAX_VALUE);
    Arrays.fill(upperDeviations, -Double.MAX_VALUE);

    Iterator<Integer> it = db.iterator();
    while (it.hasNext()) {
      Integer id = it.next();
      DoubleVector v;
      if (normalization != null) {
        v = normalization.restore(db.get(id));
      }
      else {
        v = db.get(id);
      }

      for (int e = 0; e < lowerDeviations.length; e++) {
        Matrix gauss = printSolution.getMatrix(e, e, 0, printSolution.getColumnDimension() - 1);
        double b_soll = gauss.get(0, dim);
        double b_ist = 0;
        for (int d = 1; d <= dim; d++) {
          b_ist += gauss.get(0, d - 1) * v.getValue(d);
        }

        double dev = b_soll - b_ist;
        if (dev < 0 && lowerDeviations[e] > dev)
          lowerDeviations[e] = dev;
        else if (dev > 0 && upperDeviations[e] < dev)
          upperDeviations[e] = dev;
      }
    }
    return new double[][]{lowerDeviations, upperDeviations};

  }

  /**
   * Returns the solution for printing purposes. If normalization is null, a copy of the solution
   * matrix is returned, otherwise the solution matrix will be transformed according to the normalization.
   *
   * @param normalization the normalization, can be null
   * @return the solution for printing purposes
   * @throws NonNumericFeaturesException
   */
  public Matrix getPrintSolution
  (Normalization<DoubleVector> normalization) throws NonNumericFeaturesException {
    if (normalization != null) {
      return normalization.transform(solution).gaussJordanElimination();
    }
    else {
      return solution.copy();
    }
  }
}
