package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.linearalgebra.Matrix;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.normalization.Normalization;

import java.text.NumberFormat;

/**
 * Determines the mean square error in each equation for each attribute in an linear equation system.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MeanSquareErrors {
  /**
   * The mean square error.
   */
  private double[][] mse;

  /**
   * Creates a new deviation object.
   *
   * @param db            the database holding the objects
   * @param normalization the normalization, can be null
   * @param ids           the ids of the objects for which the equations hold
   * @param gauss         the matrix holding the equations
   * @throws NonNumericFeaturesException
   */
  public MeanSquareErrors(Database<DoubleVector> db, Normalization<DoubleVector> normalization,
                          Integer[] ids, Matrix gauss) throws NonNumericFeaturesException {
    init(db, normalization, ids, gauss);
  }

  /**
   * Computes the deviations.
   *
   * @param db            the database holding the objects
   * @param normalization the normalization, can be null
   * @param ids           the ids of the objects for which the equations hold
   * @param gauss         the matrix holding the equations
   * @throws NonNumericFeaturesException
   */
  private void init(Database<DoubleVector> db, Normalization<DoubleVector> normalization,
                    Integer[] ids, Matrix gauss) throws NonNumericFeaturesException {

    // init arrays
    int noEquations = gauss.getRowDimension();
    int dim = gauss.getColumnDimension() - 1;
    this.mse = new double[noEquations][dim];

    // determine mse
    for (Integer id : ids) {
      DoubleVector v = normalization != null ?
                       normalization.restore(db.get(id)) :
                       db.get(id);

      for (int i = 0; i < noEquations; i++) {
        Matrix row = gauss.getMatrix(i, i, 0, dim);

        for (int j = 0; j < dim; j++) {
          double ist = v.getValue(j + 1);
          double factor = row.get(0, j);
          double soll = 0;

          for (int d = 0; d < dim; d++) {
            if (d == j) continue;
            soll -= row.get(0, d) * v.getValue(d + 1);
          }
          soll += row.get(0, dim);
          soll /= factor;

          mse[i][j] += Math.pow((ist - soll), 2);
        }
      }
    }

    int size = db.size();
    for (int i = 0; i < noEquations; i++) {
      for (int j = 0; j < dim; j++) {
        mse[i][j] /= size;
      }
    }
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString(String prefix, NumberFormat nf) {
    if (mse.length == 0) return prefix;
    return prefix + " MSE: \n" + new Matrix(mse).toString(prefix, nf);
  }
}
