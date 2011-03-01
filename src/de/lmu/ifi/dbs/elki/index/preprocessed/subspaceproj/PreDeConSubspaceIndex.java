package de.lmu.ifi.dbs.elki.index.preprocessed.subspaceproj;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SubspaceProjectionResult;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint.IntervalBoundary;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Preprocessor for PreDeCon local dimensionality and locally weighted matrix
 * assignment to objects of a certain database.
 * 
 * @author Peer Kröger
 * 
 * @apiviz.has SubspaceProjectionResult
 * 
 * @param <D> Distance type
 * @param <V> Vector type
 */
@Title("PreDeCon Preprocessor")
@Description("Computes the projected dimension of objects of a certain database according to the PreDeCon algorithm.\n" + "The variance analysis is based on epsilon range queries.")
public class PreDeConSubspaceIndex<V extends NumberVector<? extends V, ?>, D extends Distance<D>> extends AbstractSubspaceProjectionIndex<V, D, SubspaceProjectionResult> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(PreDeConSubspaceIndex.class);

  /**
   * The threshold for small eigenvalues.
   */
  protected double delta;

  /**
   * The kappa value for generating the variance vector.
   */
  private final int kappa = 50;

  /**
   * Constructor.
   * 
   * @param database
   * @param epsilon
   * @param rangeQueryDistanceFunction
   * @param minpts
   * @param delta
   */
  public PreDeConSubspaceIndex(Database<V> database, D epsilon, DistanceFunction<V, D> rangeQueryDistanceFunction, int minpts, double delta) {
    super(database, epsilon, rangeQueryDistanceFunction, minpts);
    this.delta = delta;
  }

  @Override
  protected SubspaceProjectionResult computeProjection(DBID id, List<DistanceResultPair<D>> neighbors, Database<V> database) {
    StringBuffer msg = null;

    int referenceSetSize = neighbors.size();
    V obj = database.get(id);

    if(getLogger().isDebugging()) {
      msg = new StringBuffer();
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
        sum[d] += Math.pow(obj.doubleValue(d + 1) - o.doubleValue(d + 1), 2.0);
      }
    }

    for(int d = 0; d < dim; d++) {
      if(Math.sqrt(sum[d]) / referenceSetSize <= delta) {
        if(msg != null) {
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
      if(msg != null) {
        // msg.append("\nprojDim == 0!");
      }
      projDim = dim;
    }

    if(msg != null) {
      msg.append("\nprojDim " + database.getObjectLabelQuery().get(id) + ": " + projDim);
      msg.append("\nsimMatrix " + database.getObjectLabelQuery().get(id) + ": " + FormatUtil.format(simMatrix, FormatUtil.NF4));
      getLogger().debugFine(msg.toString());
    }

    return new SubspaceProjectionResult(projDim, simMatrix);
  }

  @Override
  public String getLongName() {
    return "PreDeCon Subspaces";
  }

  @Override
  public String getShortName() {
    return "PreDeCon-subsp";
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Factory
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses PreDeConSubspaceIndex oneway - - «creates»
   * 
   * @param <V> Vector type
   * @param <D> Distance type
   */
  public static class Factory<V extends NumberVector<? extends V, ?>, D extends Distance<D>> extends AbstractSubspaceProjectionIndex.Factory<V, D, PreDeConSubspaceIndex<V, D>> {
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
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     * 
     * @param config Parameterization
     */
    public Factory(Parameterization config) {
      super(config);
      config = config.descend(this);

      if(config.grab(DELTA_PARAM)) {
        delta = DELTA_PARAM.getValue();
      }
    }

    @Override
    public PreDeConSubspaceIndex<V, D> instantiate(Database<V> database) {
      return new PreDeConSubspaceIndex<V, D>(database, epsilon, rangeQueryDistanceFunction, minpts, delta);
    }
  }
}