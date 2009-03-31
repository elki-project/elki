package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.logging.LogLevel;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint.IntervalBoundary;
import de.lmu.ifi.dbs.elki.utilities.output.Format;

/**
 * Preprocessor for PreDeCon local dimensionality and locally weighted matrix
 * assignment to objects of a certain database.
 *
 * @author Peer Kr&ouml;ger
 */
public class PreDeConPreprocessor<D extends Distance<D>, V extends RealVector<V,?>> extends ProjectedDBSCANPreprocessor<D,V> {
  /**
   * The default value for delta.
   */
  public static final double DEFAULT_DELTA = 0.01;

  /**
   * OptionID for {@link #DELTA_PARAM}
   */
  public static final OptionID DELTA_ID = OptionID.getOrCreateOptionID("predecon.delta",
      "a double between 0 and 1 specifying the threshold for small Eigenvalues (default is delta = "
      + DEFAULT_DELTA + ").");
  
  /**
   * Parameter for Delta
   */
  private final DoubleParameter DELTA_PARAM = new DoubleParameter(DELTA_ID,
      new IntervalConstraint(0.0,IntervalBoundary.OPEN,1.0,IntervalBoundary.OPEN),
      DEFAULT_DELTA);

  /**
   * The threshold for small eigenvalues.
   */
  protected double delta;

  /**
   * The kappa value for generating the variance vector.
   */
  private final int kappa = 50;

  /**
   * Provides a new Preprocessor that computes the local dimensionality and locally weighted matrix
   * of objects of a certain database.
   */
  public PreDeConPreprocessor() {
    super();
//    this.debug = true;

    addOption(DELTA_PARAM);
  }

  /**
   * TODO provide correct commentary
   * 
   * This method implements the type of variance analysis to be computed for a given point.
   * <p/>
   * Example1: for 4C, this method should implement a PCA for the given point.
   * Example2: for PreDeCon, this method should implement a simple axis-parallel variance analysis.
   *
   * @param id        the given point
   * @param neighbors the neighbors as query results of the given point
   * @param database  the database for which the preprocessing is performed
   */
  @Override
  protected void runVarianceAnalysis(Integer id, List<DistanceResultPair<D>> neighbors, Database<V> database) {
    StringBuffer msg = new StringBuffer();

    int referenceSetSize = neighbors.size();
    V obj = database.get(id);

    if (logger.isLoggable(LogLevel.FINE)) {
      msg.append("\n\nreferenceSetSize = " + referenceSetSize);
      msg.append("\ndelta = " + delta);
    }

    if (referenceSetSize == 0) {
      throw new RuntimeException("Reference Set Size = 0. This should never happen!");
    }

    // prepare similarity matrix
    int dim = obj.getDimensionality();
    Matrix simMatrix = new Matrix(dim, dim, 0);
    for (int i = 0; i < dim; i++) {
      simMatrix.set(i, i, 1);
    }

    // prepare projected dimensionality
    int projDim = 0;

    // start variance analysis
    double[] sum = new double[dim];
    for (DistanceResultPair<D> neighbor : neighbors) {
      RealVector<?,?> o = database.get(neighbor.getID());
      for (int d = 0; d < dim; d++) {
        sum[d] = + Math.pow(obj.getValue(d + 1).doubleValue() - o.getValue(d + 1).doubleValue(), 2.0);
      }
    }

    for (int d = 0; d < dim; d++) {
      if (Math.sqrt(sum[d]) / referenceSetSize <= delta) {
        if (logger.isLoggable(LogLevel.FINE)) {
          msg.append("\nsum[" + d + "]= " + sum[d]);
          msg.append("\n  Math.sqrt(sum[d]) / referenceSetSize)= " + Math.sqrt(sum[d]) / referenceSetSize);
        }
//        projDim++;
        simMatrix.set(d, d, kappa);
      }
      else {
        // bug in paper?
        projDim++;
      }
    }

    if (projDim == 0) {
      if (logger.isLoggable(LogLevel.FINE)) {
//        msg.append("\nprojDim == 0!");
      }
      projDim = dim;
    }

    if (logger.isLoggable(LogLevel.FINE)) {
      msg.append("\nprojDim " + database.getAssociation(AssociationID.LABEL, id) + ": " + projDim);
      msg.append("\nsimMatrix " + database.getAssociation(AssociationID.LABEL, id) + ": " + simMatrix.toString(Format.NF4));
      debugFine(msg.toString());
    }

    // set the associations
    database.associate(AssociationID.LOCAL_DIMENSIONALITY, id, projDim);
    database.associate(AssociationID.LOCALLY_WEIGHTED_MATRIX, id, simMatrix);
  }

  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    delta = DELTA_PARAM.getValue();
    return remainingParameters;
  }

  @Override
  public String parameterDescription() {
    StringBuffer description = new StringBuffer();
    description.append(PreDeConPreprocessor.class.getName());
    description.append(" computes the projected dimension of objects of a certain database according to the PreDeCon algorithm.\n");
    description.append("The variance analysis is based on epsilon range queries.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }
}
