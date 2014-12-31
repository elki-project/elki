package de.lmu.ifi.dbs.elki.data.uncertain;

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;

/**
 * An abstract class used to define the basic format
 * for Uncertain-Data-Objects.
 * 
 * The SpatialComparable parameter asserts, that
 * the Uncertain-Data-Objects from classes derived
 * from UOModel are fit for indexing via R-Trees.
 * 
 * The method drawSample is planned to retrieve
 * SamplePoints from a particular Uncertain-Data-Object.
 * 
 * To implement drawSample to retrieve such SamplePoints
 * random, iterative or in any other way is a matter of
 * the particular author.
 * 
 * The way one shapes his Uncertain-Data-Objects and there
 * possible values isn't of our concern, but drawSample
 * shall return a {@link DoubleVector} for its result
 * to be easy to use with else ELKI algorithms.
 * 
 * @author Alexander Koos
 *
 * @param <C>
 */
public abstract class UOModel<C extends SpatialComparable> implements Model, SpatialComparable, TextWriteable {
  protected Random rand;
  protected C bounds;
  protected int dimensions;
  public static final double DEFAULT_MIN = Double.MIN_VALUE;
  public static final double DEFAULT_MAX = Double.MAX_VALUE;
  
  public static final int PROBABILITY_SCALE = 10000;
  
  public final static long DEFAULT_MIN_MAX_DEVIATION = 5l;
  
  public final static long DEFAULT_MIN_MAX_DEVIATION_GAUSSIAN = 3l;
  
  public final static long DEFAULT_SAMPLE_SIZE = 10l;
  
  public final static long DEFAULT_STDDEV = 1l;
  
  public final static long DEFAULT_MULTIPLICITY = 1l;
  
  public final static int DEFAULT_PROBABILITY_SEED = 5;
  
  public final static double DEFAULT_MAX_TOTAL_PROBABILITY = 1.0;
  
  public final static long DEFAULT_ENSAMBLE_DEPTH = 10l;
  
  public final static int DEFAULT_TRY_LIMIT = 1000;
  
  public abstract DoubleVector drawSample();
  
  public abstract int getWeight();
  
  /**
   * Explicitly sets a {@link SpatialComparable} to bound
   * the particular Uncertain-Data-Object.
   * 
   * @param C bounds
   * 
   * @return void
   */
  public void setBounds(final C bounds) {
    this.bounds = bounds;
  }
  
  /**
   * Returns the {@link SpatialComparable} bounding the
   * particular Uncertain-Data-Object.
   * 
   * @return C
   */
  public C getBounds() {
    return this.bounds;
  }
  
  /**
   * Returns the Low-Boundary of a specific
   * dimension of the particular Uncertain-Data-Object.
   * 
   * @param int dimension
   * 
   * @return double
   */
  @Override
  public double getMin(final int dimension) {
    return this.bounds.getMin(dimension);
  }
  
  /**
   * Returns the High-Boundary of a specific
   * dimension of the particular Uncertain-Data-Object.
   * 
   * @param int dimension
   * 
   * @return double
   */
  @Override
  public double getMax(final int dimension) {
    return this.bounds.getMax(dimension);
  }
  
  /**
   * Returns the dimensionality of the
   * particular Uncertain-Data-Object.
   * 
   * @return int
   */
  @Override
  public int getDimensionality() {
    return this.dimensions;
  }
  
  /**
   * This method's purpose is to be implemented by
   * particular uncertainity models in a way, they
   * can be used as parameters to create an uncertain
   * dataset out of a given certain dataset (a
   * groundtruth by any means).
   * 
   * @param {@link NumberVector} vec
   * @param boolean blur
   * @param boolean groundtruth
   * @param int dims
   * 
   * @return {@link UncertainObject}
   */
  public abstract UncertainObject<UOModel<SpatialComparable>> uncertainify(NumberVector vec, boolean blur, boolean uncertainify, int dims);
  
  /**
   * Creates a vector that makes it possible to
   * visualize databases containing {@link UncertainObject}
   * 
   * @return {@link DoubleVector}
   */
  public abstract DoubleVector getAnker();
}
