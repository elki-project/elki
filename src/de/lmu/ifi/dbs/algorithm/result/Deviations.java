package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.linearalgebra.Matrix;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.Util;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Determines the derivations of an linear equation system.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class Deviations {
  private double[] lowerDeviations;
  private double[] upperDeviations;
  private double[] sqr;
  private double[] sqt;
  private double[] sqe;
  private double[] r21;
  private double[] r22;

  /**
   * Creates a new deviation object.
   *
   * @param db            the database holding the objects
   * @param normalization the normalization, can be null
   * @param ids           the ids of the objects for which the equations hold
   * @param gauss         the matrix holding the equations
   * @throws NonNumericFeaturesException
   */
  public Deviations(Database<DoubleVector> db, Normalization<DoubleVector> normalization,
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
    this.lowerDeviations = new double[noEquations];
    this.upperDeviations = new double[noEquations];
    this.sqr = new double[noEquations];
    this.sqt = new double[noEquations];
    this.sqe = new double[noEquations];
    this.r21 = new double[noEquations];
    this.r22 = new double[noEquations];
    Arrays.fill(lowerDeviations, Double.MAX_VALUE);
    Arrays.fill(upperDeviations, -Double.MAX_VALUE);

    // get centroid
    DoubleVector centroid = centroid(db);

    // get the indices of the 1's
    int dim = gauss.getColumnDimension() - 1;
    Integer[] indices = new Integer[gauss.getRowDimension()];
    for (int i = 0; i < sqr.length; i++) {
      Matrix row = gauss.getMatrix(i, i, 0, dim);
      for (int d = 0; d < dim; d++) {
        if (row.get(0, d) == 1.0) {
          indices[i] = d;
          break;
        }
      }
    }

    // determine deviations
    for (Integer id : ids) {
      DoubleVector v = normalization != null ?
                       normalization.restore(db.get(id)) :
                       db.get(id);

      for (int i = 0; i < noEquations; i++) {
        int index = indices[i];
        Matrix row = gauss.getMatrix(i, i, 0, dim);
        double soll = 0;
        double dev = 0;

        for (int d = 0; d < dim; d++) {
          dev += row.get(0, d) * v.getValue(d + 1);

          if (d == index) continue;
          soll -= row.get(0, d) * v.getValue(d + 1);
        }
        dev += (-1) * row.get(0, dim);
        soll += row.get(0, dim);
        double ist = v.getValue(index + 1);

        if (dev < 0 && lowerDeviations[i] > dev)
          lowerDeviations[i] = dev;
        else if (dev > 0 && upperDeviations[i] < dev)
          upperDeviations[i] = dev;

        sqr[i] += Math.pow((ist - soll), 2);
        sqt[i] += Math.pow(ist - centroid.getValue(index + 1), 2);
        sqe[i] += Math.pow(soll - centroid.getValue(index + 1), 2);
      }
    }

    for (int i = 0; i < noEquations; i++) {
      r21[i] = 1 - (sqr[i] / sqt[i]);
      r22[i] = sqe[i] / sqt[i];
    }
  }

  /**
   * Returns the centroid as a DoubleVector object of the specified objects
   * stored in the database.
   *
   * @param db the database storing the objects
   * @return the centroid of the specified objects stored in the database
   */
  private DoubleVector centroid(Database<DoubleVector> db) {
    int dim = db.dimensionality();
    double[] centroid = new double[dim];
    Iterator<Integer> it = db.iterator();
    while (it.hasNext()) {
      DoubleVector o = db.get(it.next());
      for (int j = 1; j <= dim; j++) {
        centroid[j - 1] += o.getValue(j);
      }
    }

    for (int i = 0; i < dim; i++) {
      centroid[i] /= db.size();
    }
    return new DoubleVector(centroid);
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString(String prefix, NumberFormat nf) {
    if (nf != null)
      return prefix + " upper deviations  : " + Util.format(upperDeviations, nf) + "\n" +
             prefix + " lower deviations  : " + Util.format(lowerDeviations, nf) + "\n" +
             prefix + " sqr               : " + Util.format(sqr, nf) + "\n" +
             prefix + " sqt               : " + Util.format(sqt, nf) + "\n" +
             prefix + " sqe               : " + Util.format(sqe, nf) + "\n" +
             prefix + " r^2 = 1 - sqr/sqt : " + Util.format(r21, nf) + "\n" +
             prefix + " r^2 = sqe/sqt     : " + Util.format(r22, nf);

    return prefix + " upper deviations  : " + Util.format(upperDeviations) + "\n" +
           prefix + " lower deviations  : " + Util.format(lowerDeviations) + "\n" +
           prefix + " sqr               : " + Util.format(sqr) + "\n" +
           prefix + " sqt               : " + Util.format(sqt) + "\n" +
           prefix + " sqe               : " + Util.format(sqe) + "\n" +
           prefix + " r^2 = 1 - sqr/sqt : " + Util.format(r21) + "\n" +
           prefix + " r^2 = sqe/sqt     : " + Util.format(r22);
  }
}
