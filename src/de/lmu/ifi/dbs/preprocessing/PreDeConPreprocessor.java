package de.lmu.ifi.dbs.preprocessing;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.LessEqualConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.ParameterConstraint;
import de.lmu.ifi.dbs.utilities.output.Format;

import java.util.ArrayList;
import java.util.List;

/**
 * Preprocessor for PreDeCon local dimensionality and locally weighted matrix
 * assignment to objects of a certain database.
 *
 * @author Arthur Zimek
 */
public class PreDeConPreprocessor<D extends Distance<D>, V extends RealVector<V,? extends Number>> extends ProjectedDBSCANPreprocessor<D,V> {
  /**
   * The default value for delta.
   */
  public static final double DEFAULT_DELTA = 0.01;

  /**
   * Option string for parameter delta.
   */
  public static final String DELTA_P = "delta";

  /**
   * Description for parameter delta.
   */
  public static final String DELTA_D = "a double between 0 and 1 specifying the threshold for small Eigenvalues (default is delta = "
                                       + DEFAULT_DELTA + ").";

  /**
   * The threshold for small eigenvalues.
   */
  protected double delta;

  /**
   * The kappa value for generating the variance vector.
   */
  private final int kappa = 50;

  /**
   * Provides a new Preprocessor that computes the clocal dimensionality and locally weighted matrix
   * of objects of a certain database.
   */
  public PreDeConPreprocessor() {
    super();
//    this.debug = true;

    ArrayList<ParameterConstraint<Number>> deltaCons = new ArrayList<ParameterConstraint<Number>>();
    deltaCons.add(new GreaterEqualConstraint(0));
    deltaCons.add(new LessEqualConstraint(1));
    DoubleParameter delta = new DoubleParameter(DELTA_P, DELTA_D, deltaCons);
    delta.setDefaultValue(DEFAULT_DELTA);
    optionHandler.put(DELTA_P, delta);
  }

  /**
   * This method implements the type of variance analysis to be computed for a given point.
   * <p/>
   * Example1: for 4C, this method should implement a PCA for the given point.
   * Example2: for PreDeCon, this method should implement a simple axis-parallel variance analysis.
   *
   * @param id        the given point
   * @param neighbors the neighbors as query results of the given point
   * @param database  the database for which the preprocessing is performed
   */
  protected void runVarianceAnalysis(Integer id, List<QueryResult<D>> neighbors, Database<V> database) {
    StringBuffer msg = new StringBuffer();

    int referenceSetSize = neighbors.size();
    V obj = database.get(id);

    if (debug) {
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

    // start variance analyis
    double[] sum = new double[dim];
    for (QueryResult<D> neighbor : neighbors) {
      RealVector<?,?> o = database.get(neighbor.getID());
      for (int d = 0; d < dim; d++) {
        sum[d] = + Math.pow(obj.getValue(d + 1).doubleValue() - o.getValue(d + 1).doubleValue(), 2.0);
      }
    }

    for (int d = 0; d < dim; d++) {
      if (Math.sqrt(sum[d]) / referenceSetSize <= delta) {
        if (debug) {
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
      if (debug) {
//        msg.append("\nprojDim == 0!");
      }
      projDim = dim;
    }

    if (debug) {
      msg.append("\nprojDim " + database.getAssociation(AssociationID.LABEL, id) + ": " + projDim);
      msg.append("\nsimMatrix " + database.getAssociation(AssociationID.LABEL, id) + ": " + simMatrix.toString(Format.NF4));
      debugFine(msg.toString());
    }

    // set the associations
    database.associate(AssociationID.LOCAL_DIMENSIONALITY, id, projDim);
    database.associate(AssociationID.LOCALLY_WEIGHTED_MATRIX, id, simMatrix);
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    delta = (Double) optionHandler.getOptionValue(DELTA_P);
    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(PreDeConPreprocessor.class.getName());
    description.append(" computes the projected dimension of objects of a certain database according to the PreDeCon algorithm.\n");
    description.append("The variance analysis is based on epsilon range queries.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }
}
