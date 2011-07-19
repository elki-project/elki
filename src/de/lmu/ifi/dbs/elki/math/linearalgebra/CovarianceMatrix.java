package de.lmu.ifi.dbs.elki.math.linearalgebra;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;

/**
 * Class for computing covariance matrixes using stable mean and variance
 * computations.
 * 
 * This class encapsulates the mathematical aspects of computing this matrix.
 * 
 * See {@link de.lmu.ifi.dbs.elki.utilities.DatabaseUtil DatabaseUtil} for
 * easier to use APIs.
 * 
 * For use in algorithms, it is more appropriate to use
 * {@link de.lmu.ifi.dbs.elki.math.linearalgebra.pca.StandardCovarianceMatrixBuilder StandardCovarianceMatrixBuilder}
 * since this class can be overriden with a stabilized covariance matrix builder!
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses Vector oneway
 * @apiviz.uses NumberVector oneway
 * @apiviz.has Matrix oneway - - «produces»
 */
public class CovarianceMatrix {
  /**
   * The means
   */
  double[] mean;

  /**
   * The covariance matrix
   */
  double[][] elements;

  /**
   * Temporary storage, to avoid reallocations
   */
  double[] nmea;

  /**
   * The current weight
   */
  protected double wsum;

  /**
   * Constructor.
   * 
   * @param dim Dimensionality
   */
  public CovarianceMatrix(int dim) {
    super();
    this.mean = new double[dim];
    this.nmea = new double[dim];
    this.elements = new double[dim][dim];
    this.wsum = 0.0;
  }

  /**
   * Add a single value with weight 1.0
   * 
   * @param val Value
   */
  public void put(double[] val) {
    assert (val.length == mean.length);
    final double nwsum = wsum + 1.0;
    // Compute new means
    for(int i = 0; i < mean.length; i++) {
      final double delta = val[i] - mean[i];
      nmea[i] = mean[i] + delta / nwsum;
    }
    // Update covariance matrix
    for(int i = 0; i < mean.length; i++) {
      for(int j = i; j < mean.length; j++) {
        // We DO want to use the new mean once and the old mean once!
        // It does not matter which one is which.
        double delta = (val[i] - nmea[i]) * (val[j] - mean[j]);
        elements[i][j] = elements[i][j] + delta;
        // Optimize via symmetry
        if(i != j) {
          elements[j][i] = elements[j][i] + delta;
        }
      }
    }

    // Use new values.
    wsum = nwsum;
    System.arraycopy(nmea, 0, mean, 0, nmea.length);
  }

  /**
   * Add data with a given weight.
   * 
   * @param val data
   * @param weight weight
   */
  public void put(double val[], double weight) {
    assert (val.length == mean.length);
    final double nwsum = wsum + weight;
    // Compute new means
    for(int i = 0; i < mean.length; i++) {
      final double delta = val[i] - mean[i];
      final double rval = delta * weight / nwsum;
      nmea[i] = mean[i] + rval;
    }
    // Update covariance matrix
    for(int i = 0; i < mean.length; i++) {
      for(int j = i; j < mean.length; j++) {
        // We DO want to use the new mean once and the old mean once!
        // It does not matter which one is which.
        double delta = (val[i] - nmea[i]) * (val[j] - mean[j]) * weight;
        elements[i][j] = elements[i][j] + delta;
        // Optimize via symmetry
        if(i != j) {
          elements[j][i] = elements[j][i] + delta;
        }
      }
    }

    // Use new values.
    wsum = nwsum;
    System.arraycopy(nmea, 0, mean, 0, nmea.length);
  }

  /**
   * Add a single value with weight 1.0
   * 
   * @param val Value
   */
  public final void put(Vector val) {
    put(val.getArrayRef());
  }

  /**
   * Add data with a given weight.
   * 
   * @param val data
   * @param weight weight
   */
  public final void put(Vector val, double weight) {
    put(val.getArrayRef(), weight);
  }

  /**
   * Add a single value with weight 1.0
   * 
   * @param val Value
   */
  public void put(NumberVector<?, ?> val) {
    assert (val.getDimensionality() == mean.length);
    final double nwsum = wsum + 1.0;
    // Compute new means
    for(int i = 0; i < mean.length; i++) {
      final double delta = val.doubleValue(i + 1) - mean[i];
      nmea[i] = mean[i] + delta / nwsum;
    }
    // Update covariance matrix
    for(int i = 0; i < mean.length; i++) {
      for(int j = i; j < mean.length; j++) {
        // We DO want to use the new mean once and the old mean once!
        // It does not matter which one is which.
        double delta = (val.doubleValue(i + 1) - nmea[i]) * (val.doubleValue(j + 1) - mean[j]);
        elements[i][j] = elements[i][j] + delta;
        // Optimize via symmetry
        if(i != j) {
          elements[j][i] = elements[j][i] + delta;
        }
      }
    }
    // Use new values.
    wsum = nwsum;
    System.arraycopy(nmea, 0, mean, 0, nmea.length);
  }

  /**
   * Add data with a given weight.
   * 
   * @param val data
   * @param weight weight
   */
  public void put(NumberVector<?, ?> val, double weight) {
    assert (val.getDimensionality() == mean.length);
    final double nwsum = wsum + weight;
    // Compute new means
    for(int i = 0; i < mean.length; i++) {
      final double delta = val.doubleValue(i + 1) - mean[i];
      final double rval = delta * weight / nwsum;
      nmea[i] = mean[i] + rval;
    }
    // Update covariance matrix
    for(int i = 0; i < mean.length; i++) {
      for(int j = i; j < mean.length; j++) {
        // We DO want to use the new mean once and the old mean once!
        // It does not matter which one is which.
        double delta = (val.doubleValue(i + 1) - nmea[i]) * (val.doubleValue(j + 1) - mean[j]) * weight;
        elements[i][j] = elements[i][j] + delta;
        // Optimize via symmetry
        if(i != j) {
          elements[j][i] = elements[j][i] + delta;
        }
      }
    }
    // Use new values.
    wsum = nwsum;
    System.arraycopy(nmea, 0, mean, 0, nmea.length);
  }

  /**
   * Get the mean as vector.
   * 
   * @return Mean vector
   */
  public Vector getMeanVector() {
    return new Vector(mean);
  }

  /**
   * Get the mean as vector.
   * 
   * @return Mean vector
   */
  public <F extends NumberVector<? extends F, ?>> F getMeanVector(Relation<? extends F> relation) {
    return DatabaseUtil.assumeVectorField(relation).getFactory().newInstance(mean);
  }

  /**
   * Obtain the covariance matrix according to the sample statistics: (n-1)
   * degrees of freedom.
   * 
   * This method duplicates the matrix contents, so it does allow further
   * updates. Use {@link #destroyToSampleMatrix()} if you do not need further
   * updates.
   * 
   * @return New matrix
   */
  public Matrix makeSampleMatrix() {
    if(wsum <= 1.0) {
      throw new IllegalStateException("Too few elements used to obtain a valid covariance matrix.");
    }
    Matrix mat = new Matrix(elements);
    return mat.times(1.0 / (wsum - 1));
  }

  /**
   * Obtain the covariance matrix according to the population statistics: n
   * degrees of freedom.
   * 
   * This method duplicates the matrix contents, so it does allow further
   * updates. Use {@link #destroyToNaiveMatrix()} if you do not need further
   * updates.
   * 
   * @return New matrix
   */
  public Matrix makeNaiveMatrix() {
    if(wsum <= 0.0) {
      throw new IllegalStateException("Too few elements used to obtain a valid covariance matrix.");
    }
    Matrix mat = new Matrix(elements);
    return mat.times(1.0 / wsum);
  }

  /**
   * Obtain the covariance matrix according to the sample statistics: (n-1)
   * degrees of freedom.
   * 
   * This method doesn't require matrix duplication, but will not allow further
   * updates, the object should be discarded. Use {@link #makeSampleMatrix()} if
   * you want to perform further updates.
   * 
   * @return New matrix
   */
  public Matrix destroyToSampleMatrix() {
    if(wsum <= 1.0) {
      throw new IllegalStateException("Too few elements used to obtain a valid covariance matrix.");
    }
    Matrix mat = new Matrix(elements).timesEquals(1.0 / (wsum - 1));
    this.elements = null;
    return mat;
  }

  /**
   * Obtain the covariance matrix according to the population statistics: n
   * degrees of freedom.
   * 
   * This method doesn't require matrix duplication, but will not allow further
   * updates, the object should be discarded. Use {@link #makeNaiveMatrix()} if
   * you want to perform further updates.
   * 
   * @return New matrix
   */
  public Matrix destroyToNaiveMatrix() {
    if(wsum <= 0.0) {
      throw new IllegalStateException("Too few elements used to obtain a valid covariance matrix.");
    }
    Matrix mat = new Matrix(elements).timesEquals(1.0 / wsum);
    this.elements = null;
    return mat;
  }

  /**
   * Static Constructor.
   * 
   * @param mat Matrix to use the columns of
   */
  public static CovarianceMatrix make(Matrix mat) {
    CovarianceMatrix c = new CovarianceMatrix(mat.getRowDimensionality());
    int n = mat.getColumnDimensionality();
    for(int i = 0; i < n; i++) {
      // TODO: avoid constructing the vector objects?
      c.put(mat.getColumnVector(i));
    }
    return c;
  }

  /**
   * Static Constructor from a full relation.
   * 
   * @param relation Relation to use.
   */
  public static CovarianceMatrix make(Relation<? extends NumberVector<?, ?>> relation) {
    CovarianceMatrix c = new CovarianceMatrix(DatabaseUtil.dimensionality(relation));
    for(DBID id : relation.iterDBIDs()) {
      c.put(relation.get(id));
    }
    return c;
  }

  /**
   * Static Constructor from a full relation.
   * 
   * @param relation Relation to use.
   * @param ids IDs to add
   */
  public static CovarianceMatrix make(Relation<? extends NumberVector<?, ?>> relation, Iterable<DBID> ids) {
    CovarianceMatrix c = new CovarianceMatrix(DatabaseUtil.dimensionality(relation));
    for(DBID id : ids) {
      c.put(relation.get(id));
    }
    return c;
  }
}