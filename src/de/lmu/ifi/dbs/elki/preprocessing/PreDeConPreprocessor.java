package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint.IntervalBoundary;
import de.lmu.ifi.dbs.elki.utilities.output.FormatUtil;

/**
 * Preprocessor for PreDeCon local dimensionality and locally weighted matrix
 * assignment to objects of a certain database.
 * 
 * @author Peer Kr&ouml;ger
 * @param <D> Distance type
 * @param <V> Vector type
 */
public class PreDeConPreprocessor<D extends Distance<D>, V extends NumberVector<V,?>> extends ProjectedDBSCANPreprocessor<D, V> {
  /**
   * The default value for delta.
   */
  public static final double DEFAULT_DELTA = 0.01;

  /**
   * OptionID for {@link #DELTA_PARAM}
   */
  public static final OptionID DELTA_ID = OptionID.getOrCreateOptionID("predecon.delta", "a double between 0 and 1 specifying the threshold for small Eigenvalues (default is delta = " + DEFAULT_DELTA + ").");

  /**
   * Parameter for Delta
   */
  private final DoubleParameter DELTA_PARAM = new DoubleParameter(DELTA_ID, new IntervalConstraint(0.0, IntervalBoundary.OPEN, 1.0, IntervalBoundary.OPEN), DEFAULT_DELTA);

  /**
   * The threshold for small eigenvalues.
   */
  protected double delta;

  /**
   * The kappa value for generating the variance vector.
   */
  private final int kappa = 50;

  /**
   * Provides a new Preprocessor that computes the local dimensionality and
   * locally weighted matrix of objects of a certain database.
   */
  public PreDeConPreprocessor() {
    super();
    // this.debug = true;

    addOption(DELTA_PARAM);
  }

  /**
   * TODO provide correct commentary
   * 
   * This method implements the type of variance analysis to be computed for a
   * given point.
   * <p/>
   * Example1: for 4C, this method should implement a PCA for the given point.
   * Example2: for PreDeCon, this method should implement a simple axis-parallel
   * variance analysis.
   * 
   * @param id the given point
   * @param neighbors the neighbors as query results of the given point
   * @param database the database for which the preprocessing is performed
   */
  @Override
  protected void runVarianceAnalysis(Integer id, List<DistanceResultPair<D>> neighbors, Database<V> database) {
    StringBuffer msg = new StringBuffer();

    int referenceSetSize = neighbors.size();
    V obj = database.get(id);

    if(logger.isDebugging()) {
      msg.append("referenceSetSize = " + referenceSetSize);
      msg.append("\ndelta = " + delta);
    }

    if(referenceSetSize == 0) {
      throw new RuntimeException("Reference Set Size = 0. This should never happen!");
    }

    // prepare similarity matrix
    int dim = obj.getDimensionality();
    Matrix simMatrix = new Matrix(dim, dim, 0);
    for(int i = 0; i < dim; i++) {
      simMatrix.set(i, i, 1);
    }

    // prepare projected dimensionality
    int projDim = 0;

    // start variance analysis
    double[] sum = new double[dim];
    for(DistanceResultPair<D> neighbor : neighbors) {
      V o = database.get(neighbor.getID());
      for(int d = 0; d < dim; d++) {
        sum[d] = +Math.pow(obj.getValue(d + 1).doubleValue() - o.getValue(d + 1).doubleValue(), 2.0);
      }
    }

    for(int d = 0; d < dim; d++) {
      if(Math.sqrt(sum[d]) / referenceSetSize <= delta) {
        if(logger.isDebugging()) {
          msg.append("\nsum[" + d + "]= " + sum[d]);
          msg.append("\n  Math.sqrt(sum[d]) / referenceSetSize)= " + Math.sqrt(sum[d]) / referenceSetSize);
        }
        // projDim++;
        simMatrix.set(d, d, kappa);
      }
      else {
        // bug in paper?
        projDim++;
      }
    }

    if(projDim == 0) {
      if(logger.isDebugging()) {
        // msg.append("\nprojDim == 0!");
      }
      projDim = dim;
    }

    if(logger.isDebugging()) {
      msg.append("\nprojDim " + database.getAssociation(AssociationID.LABEL, id) + ": " + projDim);
      msg.append("\nsimMatrix " + database.getAssociation(AssociationID.LABEL, id) + ": " + simMatrix.toString(FormatUtil.NF4));
      logger.debugFine(msg.toString());
    }

    // set the associations
    database.associate(AssociationID.LOCAL_DIMENSIONALITY, id, projDim);
    database.associate(AssociationID.LOCALLY_WEIGHTED_MATRIX, id, simMatrix);
  }

  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);
    delta = DELTA_PARAM.getValue();

    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }

  @Override
  public String shortDescription() {
    StringBuffer description = new StringBuffer();
    description.append(PreDeConPreprocessor.class.getName());
    description.append(" computes the projected dimension of objects of a certain database according to the PreDeCon algorithm.\n");
    description.append("The variance analysis is based on epsilon range queries.\n");
    return description.toString();
  }
}
