package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.linearalgebra.Matrix;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
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
   * @param parameters                the parameter setting of the algorithm to which this result belongs to
   */
  public CorrelationAnalysisSolution(Matrix solution, Database<DoubleVector> db,
                                     int correlationDimensionality, String[] parameters) {

    this(solution, db, correlationDimensionality, null, parameters);
  }

  /**
   * Provides a new CorrelationAnalysisSolution holding the specified matrix and number format.
   *
   * @param solution   the matrix describing the solution equations
   *                   * @param correlationDimensionality the dimensionality of the correlation
   * @param nf         the number format for output accuracy
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

    // determine deviations
    try {
      Matrix printSolution = getPrintSolutionMatrix(normalization);
      int noEquations = db.dimensionality() - correlationDimensionality;
      Matrix gauss = printSolution.getMatrix(0, noEquations - 1, 0, printSolution.getColumnDimension() - 1);

      // get the ids
      Integer[] ids = new Integer[db.size()];
      Iterator<Integer> it = db.iterator();
      int i = 0;
      while (it.hasNext()) ids[i++] = it.next();
      MeanSquareErrors mse = new MeanSquareErrors(db, normalization, ids, gauss);
      outStream.println(mse.toString("", nf));

      outStream.flush();
    }
    catch (NonNumericFeaturesException e) {
      throw new UnableToComplyException(e);
    }
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
   * Returns the solution matrix for printing purposes. If normalization is null, a copy of the solution
   * matrix is returned, otherwise the solution matrix will be transformed according to the normalization.
   *
   * @param normalization the normalization, can be null
   * @return the solution for printing purposes
   * @throws NonNumericFeaturesException
   */
  public Matrix getPrintSolutionMatrix
  (Normalization<DoubleVector> normalization) throws NonNumericFeaturesException {
    if (normalization != null) {
      return normalization.transform(solution).gaussJordanElimination();
    }
    else {
      return solution.copy();
    }
  }

  /**
   * Return the correlation dimensionality.
   * @return the correlation dimensionality
   */
  public int getCorrelationDimensionality() {
    return correlationDimensionality;
  }
}
