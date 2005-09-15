package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.DoubleVector;
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

/**
 * A solution of correlation analysis is a matrix of equations describing the
 * dependencies.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class CorrelationAnalysisSolution implements Result<DoubleVector> {
  /**
   * Matrix to store the solution equations.
   */
  private Matrix solution;

  /**
   * The maximum lower deviations for each equation.
   */
  private double[] lowerDeviations;

  /**
   * The maximum upper deviations for each equation.
   */
  private double[] upperDeviations;

  /**
   * Number format for output accuracy.
   */
  private NumberFormat nf;

  /**
   * Provides a new CorrelationAnalysisSolution holding the specified matrix.
   * <p/>
   * Same as {@link #CorrelationAnalysisSolution(de.lmu.ifi.dbs.linearalgebra.Matrix, null, double[], double[])}
   *
   * @param solution        the matrix describing the solution equations
   * @param lowerDeviations the maximum lower deviations for each equation
   * @param upperDeviations the maximum upper deviations for each equation
   */
  public CorrelationAnalysisSolution(Matrix solution, double[] lowerDeviations, double[] upperDeviations) {
    this(solution, null, lowerDeviations, upperDeviations);
  }

  /**
   * Provides a new CorrelationAnalysisSolution holding the specified matrix and number format.
   *
   * @param solution        the matrix describing the solution equations
   * @param nf              the number format for output accuracy
   * @param lowerDeviations the maximum lower deviations for each equation
   * @param upperDeviations the maximum upper deviations for each equation
   */
  public CorrelationAnalysisSolution(Matrix solution, NumberFormat nf,
                                     double[] lowerDeviations, double[] upperDeviations) {
    this.solution = solution;
    this.nf = nf;
    this.lowerDeviations = lowerDeviations;
    this.upperDeviations = upperDeviations;
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

    if (this.nf == null) {
      outStream.println("lower deviations: " + Util.format(lowerDeviations));
      outStream.println("upper deviations: " + Util.format(upperDeviations));
    }
    else {
      outStream.println("lower deviations: " + Util.format(lowerDeviations, nf));
      outStream.println("upper deviations: " + Util.format(upperDeviations, nf));
    }

    Matrix printSolution;
    if (normalization != null) {
      try {
//        printSolution = normalization.transform(solution);
        printSolution = normalization.transform(solution).gaussJordanElimination();
      }
      catch (NonNumericFeaturesException e) {
        throw new UnableToComplyException(e);
      }
    }
    else {
      printSolution = solution.copy();
    }
    if (this.nf == null) {
      outStream.println(printSolution.toString());
    }
    else {
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
   * Returns the lower deviations for each equation.
   * @return the lower deviations for each equation
   */
  public double[] getLowerDeviations() {
    return lowerDeviations;
  }

   /**
   * Returns the upper deviations for each equation.
   * @return the upper deviations for each equation
   */
   public double[] getUpperDeviations() {
    return upperDeviations;
  }

}
