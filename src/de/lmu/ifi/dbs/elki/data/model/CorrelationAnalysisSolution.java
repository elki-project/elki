package de.lmu.ifi.dbs.elki.data.model;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStreamNormalizing;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;

/**
 * A solution of correlation analysis is a matrix of equations describing the
 * dependencies.
 * 
 * @author Arthur Zimek
 * @param <V> the type of RealVector handled by this Result
 */
public class CorrelationAnalysisSolution<V extends RealVector<V, ?>> implements TextWriteable, Result, Model {
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
   * The weak eigenvectors of the hyperplane induced by the correlation.
   */
  private final Matrix weakEigenvectors;

  /**
   * The strong eigenvectors of the hyperplane induced by the correlation.
   */
  private final Matrix strongEigenvectors;

  /**
   * The similarity matrix of the pca.
   */
  private final Matrix similarityMatrix;

  /**
   * The centroid if the objects belonging to the hyperplane induced by the
   * correlation.
   */
  private final Vector centroid;

  /**
   * Provides a new CorrelationAnalysisSolution holding the specified matrix.
   * <p/>
   * 
   * @param solution the linear equation system describing the solution
   *        equations
   * @param db the database containing the objects
   * @param strongEigenvectors the strong eigenvectors of the hyperplane induced
   *        by the correlation
   * @param weakEigenvectors the weak eigenvectors of the hyperplane induced by
   *        the correlation
   * @param similarityMatrix the similarity matrix of the underlying distance
   *        computations
   * @param centroid the centroid if the objects belonging to the hyperplane
   *        induced by the correlation
   */
  public CorrelationAnalysisSolution(LinearEquationSystem solution, Database<V> db, Matrix strongEigenvectors, Matrix weakEigenvectors, Matrix similarityMatrix, Vector centroid) {
    this(solution, db, strongEigenvectors, weakEigenvectors, similarityMatrix, centroid, NumberFormat.getInstance(Locale.US));
  }

  /**
   * Provides a new CorrelationAnalysisSolution holding the specified matrix and
   * number format.
   * 
   * @param solution the linear equation system describing the solution
   *        equations
   * @param db the database containing the objects
   * @param strongEigenvectors the strong eigenvectors of the hyperplane induced
   *        by the correlation
   * @param weakEigenvectors the weak eigenvectors of the hyperplane induced by
   *        the correlation
   * @param similarityMatrix the similarity matrix of the underlying distance
   *        computations
   * @param centroid the centroid if the objects belonging to the hyperplane
   *        induced by the correlation
   * @param nf the number format for output accuracy
   */
  public CorrelationAnalysisSolution(LinearEquationSystem solution, Database<V> db, Matrix strongEigenvectors, Matrix weakEigenvectors, Matrix similarityMatrix, Vector centroid, NumberFormat nf) {
    // TODO: re-add db or remove parameter?
    //super(db);

    this.linearEquationSystem = solution;
    this.correlationDimensionality = strongEigenvectors.getColumnDimensionality();
    this.strongEigenvectors = strongEigenvectors;
    this.weakEigenvectors = weakEigenvectors;
    this.similarityMatrix = similarityMatrix;
    this.centroid = centroid;
    this.nf = nf;

    // determine standard deviation
    double variance = 0;
    Iterator<Integer> it = db.iterator();
    while(it.hasNext()) {
      Integer id = it.next();
      double distance = distance(db.get(id).getColumnVector());
      variance += distance * distance;
    }
    standardDeviation = Math.sqrt(variance / db.size());
  }

  //@Override
  //TODO: remove?
  public void output(File out, Normalization<V> normalization, List<AttributeSettings> settings) throws UnableToComplyException, IOException {
    PrintStream outStream;
    try {
      outStream = new PrintStream(new FileOutputStream(out));
    }
    catch(Exception e) {
      outStream = new PrintStream(new FileOutputStream(FileDescriptor.out));
    }
    output(outStream, normalization, settings);
  }

  /**
   * Writes the clustering result to the given stream.
   * 
   * @param outStream the stream to write to
   * @param normalization Normalization to restore original values according to,
   *        if this action is supported - may remain null.
   * @param settings the settings to be written into the header
   * @throws de.lmu.ifi.dbs.elki.utilities.UnableToComplyException if any
   *         feature vector is not compatible with values initialized during
   *         normalization
   * @throws IOException
   */
  //TODO: remove
  public void output(PrintStream outStream, Normalization<V> normalization, List<AttributeSettings> settings) throws UnableToComplyException, IOException {
    //writeHeader(outStream, settings, null);

    try {
      LinearEquationSystem printSolution = getNormalizedLinearEquationSystem(normalization);
      outStream.println("### " + this.getClass().getSimpleName() + ":");
      outStream.println("### standardDeviation = " + standardDeviation);
      outStream.println("### mean = " + centroid);
      outStream.println(printSolution.equationsToString("###  ", nf.getMaximumFractionDigits()));
      outStream.println("################################################################################");
      // outStream.flush();
    }
    catch(NonNumericFeaturesException e) {
      throw new UnableToComplyException(e);
    }
  }

  /**
   * Returns the linear equation system for printing purposes. If normalization
   * is null the linear equation system is returned, otherwise the linear
   * equation system will be transformed according to the normalization.
   * 
   * @param normalization the normalization, can be null
   * @return the linear equation system for printing purposes
   * @throws NonNumericFeaturesException if the linear equation system is not
   *         compatible with values initialized during normalization
   */
  public LinearEquationSystem getNormalizedLinearEquationSystem(Normalization<V> normalization) throws NonNumericFeaturesException {
    if(normalization != null) {
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
  public double distance(V p) {
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
    // TODO: Is there a particular reason not to do this:
    // return p.minus(centroid).projection(weakEigenvectors).euclideanNorm(0);
    // V_affin = V + a
    // dist(p, V_affin) = d(p-a, V) = ||p - a - proj_V(p-a) ||
    Matrix p_minus_a = p.minus(centroid);
    Matrix proj = p_minus_a.projection(strongEigenvectors);
    return p_minus_a.minus(proj).euclideanNorm(0);
  }

  /**
   * Returns the error vectors after projection.
   * 
   * @param p a vector in the space underlying this solution
   * @return the error vectors
   */
  public Matrix errorVectors(V p) {
    return errorVectors(p.getColumnVector());
  }

  /**
   * Returns the error vectors after projection.
   * 
   * @param p a vector in the space underlying this solution
   * @return the error vectors
   */
  public Matrix errorVectors(Matrix p) {
    return p.minus(centroid).projection(weakEigenvectors);
  }

  /**
   * Returns the error vectors after projection.
   * 
   * @param p a vector in the space underlying this solution
   * @return the error vectors
   */
  public Vector errorVector(V p) {
    Matrix evs = errorVectors(p.getColumnVector());
    Vector result = evs.getColumnVector(0);
    // getColumnDimensionality == 1 anyway.
    for(int i = 1; i < evs.getColumnDimensionality(); i++)
      result = result.minus(evs.getColumnVector(i));
    return result;
  }

  /**
   * Returns the data vectors after projection.
   * 
   * @param p a vector in the space underlying this solution
   * @return the data projections
   */
  public Matrix dataProjections(V p) {
    Matrix centered = p.getColumnVector().minus(centroid);
    Matrix sum = new Matrix(p.getDimensionality(), strongEigenvectors.getColumnDimensionality());
    for(int i = 0; i < strongEigenvectors.getColumnDimensionality(); i++) {
      Matrix v_i = strongEigenvectors.getColumn(i);
      Matrix proj = v_i.times(centered.scalarProduct(0, v_i, 0));

      sum.setColumn(i, proj);
    }
    return sum;
  }

  /**
   * Returns the data vectors after projection.
   * 
   * @param p a vector in the space underlying this solution
   * @return the error vectors
   */
  public Matrix dataVectors(Matrix p) {
    return p.minus(centroid).projection(strongEigenvectors);
  }

  /**
   * Returns the data vectors after projection.
   * 
   * @param p a vector in the space underlying this solution
   * @return the error vectors
   */
  public Vector dataVector(V p) {
    Matrix dvs = dataVectors(p.getColumnVector());
    Vector result = dvs.getColumnVector(0);
    // getColumnDimensionality == 1 anyway.
    for(int i = 1; i < dvs.getColumnDimensionality(); i++)
      result = result.plus(dvs.getColumnVector(i));
    return result;
  }

  /**
   * Returns the standard deviation of the distances of the objects belonging to
   * the hyperplane underlying this solution.
   * 
   * @return the standard deviation of this solution
   */
  public double getStandardDeviation() {
    return standardDeviation;
  }

  /**
   * Returns a copy of the strong eigenvectors.
   * 
   * @return a copy of the strong eigenvectors
   */
  public Matrix getStrongEigenvectors() {
    return strongEigenvectors.copy();
  }

  /**
   * Returns a copy of the weak eigenvectors.
   * 
   * @return a copy of the weak eigenvectors
   */
  public Matrix getWeakEigenvectors() {
    return weakEigenvectors.copy();
  }

  /**
   * Returns the similarity matrix of the pca.
   * 
   * @return the similarity matrix of the pca
   */
  public Matrix getSimilarityMatrix() {
    return similarityMatrix;
  }

  /**
   * Returns the centroid of this model.
   * 
   * @return the centroid of this model
   */
  public Vector getCentroid() {
    return centroid;
  }

  /**
   * Text output of the equation system
   */
  @SuppressWarnings("unchecked")
  @Override
  public void writeToText(TextWriterStream out) {
    out.commentPrintLn("Model class: "+this.getClass().getName());
    try {
      if(getNormalizedLinearEquationSystem(null) != null) {
        // TODO: more elegant way of doing normalization here?
        if(out instanceof TextWriterStreamNormalizing) {
          TextWriterStreamNormalizing<V> nout = (TextWriterStreamNormalizing<V>) out;
          LinearEquationSystem lq = getNormalizedLinearEquationSystem(nout.getNormalization());
          out.commentPrint("Linear Equation System: ");
          out.commentPrintLn(lq.equationsToString(2));
        }
      }
    }
    catch(NonNumericFeaturesException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /**
   * Implementation of {@link Model} interface
   */
  @Override
  public String getSuggestedLabel() {
    return "CorrelationCluster";
  }
}
