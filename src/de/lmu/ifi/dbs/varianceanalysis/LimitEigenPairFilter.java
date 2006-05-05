package de.lmu.ifi.dbs.varianceanalysis;

import de.lmu.ifi.dbs.math.linearalgebra.EigenPair;

import java.util.logging.Logger;

/**
 * The LimitEigenPairFilter filters all eigenpairs,
 * which are lower than a given value.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

public class LimitEigenPairFilter extends AbstractEigenPairFilter {
  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"UNUSED_SYMBOL"})
//  private static final boolean DEBUG = LoggingConfiguration.DEBUG;
  private static final boolean DEBUG = true;

  /**
   * The logger of this class.
   */
  @SuppressWarnings({"UNUSED_SYMBOL"})
  private Logger logger = Logger.getLogger(this.getClass().getName());

  private double limit;

  public LimitEigenPairFilter() {
  }

  /**
   * @see EigenPairFilter#filter(de.lmu.ifi.dbs.math.linearalgebra.EigenPair[])
   */
  public void filter(EigenPair[] eigenPairs) {
    // todo
    /*
    double limit;
    if (relative) {
      double max = -1.0;
      for (int i = 0; i < eigenValues.length; ++ i)
        if (max < Math.abs(eigenValues[i]))
          max = Math.abs(eigenValues[i]);
      limit = max * this.limit;
    }
    else
      limit = this.limit;
    ArrayList<Integer> list = new ArrayList<Integer>();
    for (int i = 0; i < eigenValues.length; ++ i)
      if (Math.abs(eigenValues[i]) >= limit)
        list.add(i);
    int m = Matrix.getNumberOfRows(eigenVectors);
    int n = list.size();
    this.eigenValues = Vector.newVector(n);
    this.eigenVectors = Matrix.newMatrix(m, n);
    for (int j = 0; j < n; ++j) {
      int k = list.get(j);
      this.eigenValues[j] = eigenValues[k];
      for (int i = 0; i < m; ++i)
        this.eigenVectors[i][j] = eigenVectors[i][k];
    }
    */
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(PercentageEigenPairFilter.class.getName());
    description.append(" filters all eigenpairs, " +
                       " which are lower than a given value.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }


}
