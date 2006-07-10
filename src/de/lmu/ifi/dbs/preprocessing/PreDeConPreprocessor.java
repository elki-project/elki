package de.lmu.ifi.dbs.preprocessing;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.List;

/**
 * Preprocessor for PreDeCon local dimensionality and locally weighted matrix
 * assignment to objects of a certain database.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class PreDeConPreprocessor extends ProjectedDBSCANPreprocessor {
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
  public static final String DELTA_D = "<double>a double between 0 and 1 specifying the threshold for small Eigenvalues (default is delta = "
                                       + DEFAULT_DELTA + ").";

  /**
   * The threshold for small eigenvalues.
   */
  protected double delta;

  /*
  * The kappa value for generating the variance vector.
  */
  private final int kappa = 50;

  /**
   * Provides a new Preprocessor that computes the clocal dimensionality and locally weighted matrix
   * of objects of a certain database.
   */
  public PreDeConPreprocessor() {
    super();
    parameterToDescription.put(DELTA_P + OptionHandler.EXPECTS_VALUE, DELTA_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
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
  protected <D extends Distance<D>> void runVarianceAnalysis(Integer id, List<QueryResult<D>> neighbors, Database<RealVector> database) {
    int referenceSetSize = neighbors.size();
    RealVector obj = database.get(id);

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
      RealVector o = database.get(neighbor.getID());
      for (int d = 0; d < dim; d++) {
        sum[d] = + Math.pow(obj.getValue(d + 1).doubleValue() - o.getValue(d + 1).doubleValue(), 2.0);
      }
    }

    for (int d = 0; d < dim; d++) {
      if (Math.sqrt(sum[d]) / referenceSetSize <= delta) {
        projDim++;
        simMatrix.set(d, d, kappa);
      }
    }

    if (projDim == 0) {
      projDim = dim;
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

    // delta
    if (optionHandler.isSet(DELTA_P)) {
      String deltaString = optionHandler.getOptionValue(DELTA_P);
      try {
        delta = Double.parseDouble(deltaString);
        if (delta < 0 || delta > 1) {
          throw new WrongParameterValueException(DELTA_P, deltaString, DELTA_D);
        }
      }
      catch (NumberFormatException e) {
        throw new WrongParameterValueException(DELTA_P, deltaString, DELTA_D, e);
      }
    }
    else {
      delta = DEFAULT_DELTA;
    }

    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();

    AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting(DELTA_P, Double.toString(delta));

    return attributeSettings;
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
