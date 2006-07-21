package de.lmu.ifi.dbs.algorithm.result;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

/**
 * A solution of correlation analysis is a matrix of equations describing the
 * dependencies.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class CorrelationAnalysisSolution extends AbstractResult<RealVector> {
  /**
   * Stores the solution equations.
   */
  private LinearEquationSystem linearEquationSystem;

  /**
   * Number format for output accuracy.
   */
  private NumberFormat nf;

  /**
   * The dimensionality of the correlation.
   */
  private int correlationDimensionality;

  /**
   * The standard deviation within this solution.
   */
  private final double standardDeviation;

  /**
   * The strong eigenvectors of the hyperplane induced by the correlation.
   */
  private final Matrix strongEigenvectors;

  /**
   * The centroid if the objects belonging to the hyperplane induced by the
   * correlation.
   */
  private final Matrix centroid;

  /**
   * Provides a new CorrelationAnalysisSolution holding the specified matrix.
   * <p/>
   *
   * @param solution           the linear equation system describing the solution equations
   * @param db                 the database containing the objects
   * @param strongEigenvectors the strong eigenvectors of the hyperplane induced by the
   *                           correlation
   * @param centroid           the centroid if the objects belonging to the hyperplane
   *                           induced by the correlation
   */
  public CorrelationAnalysisSolution(LinearEquationSystem solution,
                                     Database<RealVector> db,
                                     Matrix strongEigenvectors,
                                     Matrix centroid) {
    this(solution, db, strongEigenvectors, centroid, null);
  }

  /**
   * Provides a new CorrelationAnalysisSolution holding the specified matrix
   * and number format.
   *
   * @param solution           the linear equation system describing the solution equations
   * @param strongEigenvectors the strong eigenvectors of the hyperplane induced by the
   *                           correlation
   * @param centroid           the centroid if the objects belonging to the hyperplane
   *                           induced by the correlation
   * @param nf                 the number format for output accuracy
   */
  public CorrelationAnalysisSolution(LinearEquationSystem solution,
                                     Database<RealVector> db,
                                     Matrix strongEigenvectors,
                                     Matrix centroid,
                                     NumberFormat nf) {
    super(db);

    this.linearEquationSystem = solution;
    this.correlationDimensionality = strongEigenvectors.getColumnDimension();
    this.strongEigenvectors = strongEigenvectors;
    this.centroid = centroid;
    this.nf = nf;

    // determine standard deviation
    double variance = 0;
    Iterator<Integer> it = db.iterator();
    while (it.hasNext()) {
      Integer id = it.next();
      double distance = distance(db.get(id).getColumnVector());
      variance += distance * distance;
    }
    standardDeviation = Math.sqrt(variance / db.size());
  }

  /**
   * @see Result#output(File, Normalization, List)
   */
  public void output(File out, Normalization<RealVector> normalization,
                     List<AttributeSettings> settings) throws UnableToComplyException {
    PrintStream outStream;
    try {
      outStream = new PrintStream(new FileOutputStream(out));
    }
    catch (Exception e) {
      outStream = new PrintStream(
        new FileOutputStream(FileDescriptor.out));
    }
    output(outStream, normalization, settings);
  }

  /**
   * Writes the clustering result to the given stream.
   *
   * @param outStream     the stream to write to
   * @param normalization Normalization to restore original values according to, if this
   *                      action is supported - may remain null.
   * @param settings      the settings to be written into the header
   * @throws de.lmu.ifi.dbs.utilities.UnableToComplyException
   *          if any feature vector is not compatible with values
   *          initialized during normalization
   */
  public void output(PrintStream outStream,
                     Normalization<RealVector> normalization,
                     List<AttributeSettings> settings) throws UnableToComplyException {
    writeHeader(outStream, settings, null);

    try {
      LinearEquationSystem printSolution = getNormalizedLinearEquationSystem(normalization);
      outStream.println("### " + this.getClass().getSimpleName() + ":");
      outStream.println("### standardDeviation = " + standardDeviation);
      outStream.println(printSolution.equationsToString("###  ", nf.getMaximumFractionDigits()));
      outStream
        .println("################################################################################");
      outStream.flush();
    }
    catch (NonNumericFeaturesException e) {
      throw new UnableToComplyException(e);
    }
  }

  /**
   * Returns the linear equation system for printing purposes. If normalization is
   * null the linear equation system is returned, otherwise the linear equation system
   * will be transformed according to the normalization.
   *
   * @param normalization the normalization, can be null
   * @return the linear equation system for printing purposes
   * @throws NonNumericFeaturesException
   */
  public LinearEquationSystem getNormalizedLinearEquationSystem(Normalization<? extends RealVector> normalization)
    throws NonNumericFeaturesException {
    if (normalization != null) {
      LinearEquationSystem lq = normalization.transform(linearEquationSystem);
      lq.solveByTotalPivotSearch();
      return lq;
    }
    else {
      return linearEquationSystem;
    }
  }

  /**
   * Return the correlation dimensionality.
   *
   * @return the correlation dimensionality
   */
  public int getCorrelationDimensionality() {
    return correlationDimensionality;
  }

  /**
   * Returns the distance of RealVector p from the hyperplane underlying this
   * solution.
   *
   * @param p a vector in the space underlying this solution
   * @return the distance of p from the hyperplane underlying this solution
   */
  public double distance(RealVector p) {
    return distance(p.getColumnVector());
  }

  /**
   * Returns the distance of Matrix p from the hyperplane underlying this
   * solution.
   *
   * @param p a vector in the space underlying this solution
   * @return the distance of p from the hyperplane underlying this solution
   */
  private double distance(Matrix p) {
    // V_affin = V + a
    // dist(p, V_affin) = d(p-a, V) = ||p - a - proj_V(p-a) ||
    Matrix p_minus_a = p.minus(centroid);
    Matrix proj = p_minus_a.projection(strongEigenvectors);
    return p_minus_a.minus(proj).euclideanNorm(0);
  }

  /**
   * Returns the standard deviation of the distances of the objects belonging
   * to the hyperplane underlying this solution.
   *
   * @return the standard deviation of this solution
   */
  public double getStandardDeviation() {
    return standardDeviation;
  }
}
