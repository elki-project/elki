package de.lmu.ifi.dbs.elki.data.synthetic.bymodel;

import java.util.LinkedList;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.synthetic.bymodel.distribution.Distribution;
import de.lmu.ifi.dbs.elki.math.linearalgebra.AffineTransformation;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;

/**
 * Class to generate a single cluster according to a model as well as getting the density
 * of a given model at that point (to evaluate generated points according to the same model)
 * 
 * @author Erich Schubert
 *
 */
public class GeneratorSingleCluster implements GeneratorInterfaceDynamic {
  /**
   * The distribution generators for each axis
   */
  private LinkedList<Distribution> axes = new LinkedList<Distribution>();

  /**
   * The transformation matrix
   */
  private AffineTransformation trans;

  /**
   * The dimensionality
   */
  private int dim;

  /**
   * Clipping vectors.
   * Note: currently, either both or none have to be set!
   */
  private Vector clipmin;
  private Vector clipmax;

  /**
   * Correction factor for probability computation
   */
  private double densitycorrection = 1.0;

  /**
   * Number of points in the cluster (-> density)
   */
  private int size;

  /**
   * Cluster name
   */
  private String name;

  /**
   * Retry count
   */
  // TODO: make configureable.
  private int retries = 1000;

  /**
   * Discarded count
   */
  private int discarded = 0;

  /**
   * The generated cluster points.
   */
  public LinkedList<Vector> points = new LinkedList<Vector>();

  /**
   * Random generator (used for initializing random generators)
   */
  private Random random;
  
  /**
   * Generator (without axes)
   * 
   * @param name Cluster name
   * @param size Cluster size
   */
  public GeneratorSingleCluster(String name, int size, double densitycorrection, Random random) {
    super();
    this.size = size;
    this.name = name;
    this.densitycorrection = densitycorrection;
    this.random = random;
  }

  /**
   * Add a new generator to the cluster. No transformations must have been added
   * so far!
   * 
   * @param gen
   * @throws UnableToComplyException 
   */
  public void addGenerator(Distribution gen) throws UnableToComplyException {
    if (trans != null)
      throw new UnableToComplyException("Generators may no longer be added when transformations have been applied.");
    axes.add(gen);
    dim++;
  }

  /**
   * Apply a rotation to the generator
   * 
   * @param axis1 First axis (0 <= axis1 < dim)
   * @param axis2 Second axis (0 <= axis2 < dim)
   * @param angle Angle in Radians
   */
  public void addRotation(int axis1, int axis2, double angle) {
    if(trans == null)
      trans = new AffineTransformation(dim);
    trans.addRotation(axis1, axis2, angle);
  }

  /**
   * Add a translation to the generator
   * 
   * @param v translation vector
   */
  public void addTranslation(Vector v) {
    if(trans == null)
      trans = new AffineTransformation(dim);
    trans.addTranslation(v);
  }

  /**
   * Set a clipping box. min needs to be smaller than max in each component. 
   * Note: Clippings are not 'modified' by translation / rotation / transformation operations.
   * 
   * @param min
   * @param max
   * @throws UnableToComplyException 
   */
  public void setClipping(Vector min, Vector max) throws UnableToComplyException {
    // if only one dimension was given, expand to all dimensions.
    if (min.getRowDimensionality() == 1 && max.getRowDimensionality() == 1) {
      if (min.get(0) >= max.get(0))
        throw new UnableToComplyException("Clipping range empty.");
      clipmin = new Vector(dim);
      clipmax = new Vector(dim);
      for (int i = 0; i < dim; i++) {
        clipmin.set(i,0, min.get(0));
        clipmax.set(i,0, max.get(0));
      }
      return;
    }
    if (dim != min.getRowDimensionality())
      throw new UnableToComplyException("Clipping vector dimensionalities do not match: "+dim+" vs. "+min.getRowDimensionality());
    if (dim != max.getRowDimensionality())
      throw new UnableToComplyException("Clipping vector dimensionalities do not match: "+dim+" vs. "+max.getRowDimensionality());
    for (int i=0; i < dim; i++)
      if (min.get(i) >= max.get(i))
        throw new UnableToComplyException("Clipping range empty in dimension "+(i+1));
    clipmin = min;
    clipmax = max;
  }

  /**
   * Get the cluster dimensionality
   * 
   * @return dimensionality
   */
  public int getDim() {
    return dim;
  }

  /**
   * Test if a point is to be clipped
   * 
   * @param p point
   * @return true if the point is to be clipped
   */
  private boolean testClipping(Vector p) {
    if (clipmin == null || clipmax == null)
      return false;
    for(int i = 0; i < p.getRowDimensionality(); i++) {
      if(p.get(i) < clipmin.get(i))
        return true;
      if(p.get(i) > clipmax.get(i))
        return true;
    }
    return false;
  }

  /**
   * Generate the given number of additional points.
   * 
   * @see de.lmu.ifi.dbs.elki.data.synthetic.bymodel.GeneratorInterface#generate(int)
   */
  public LinkedList<Vector> generate(int count) throws UnableToComplyException {
    LinkedList<Vector> result = new LinkedList<Vector>();
    while(result.size() < count) {
      double[] d = new double[dim];
      int i = 0;
      for(Distribution axis : axes) {
        d[i] = axis.generate();
        i++;
      }
      Vector p = new Vector(d);
      if(trans != null)
        p = trans.apply(p);
      if(testClipping(p)) {
        retries--;
        if(retries < 0)
          throw new UnableToComplyException("Maximum retry count in generator exceeded.");
        continue;
      }
      result.add(p);
    }
    return result;
  }

  /** 
   * Compute density for cluster model at given vector p-
   * 
   * @see de.lmu.ifi.dbs.elki.data.synthetic.bymodel.GeneratorInterface#getDensity(de.lmu.ifi.dbs.elki.math.linearalgebra.Vector)
   */
  public double getDensity(Vector p) {
    Vector o = p;
    if(trans != null)
      o = trans.applyInverse(p);

    double density = 1.0;
    int i = 0;
    for(Distribution axis : axes) {
      density = density * axis.explain(o.get(i));
      i++;
    }
    return density * densitycorrection;
  }

  /**
   * Get axis generators.
   * Used for printing model information
   * 
   * @return list of distributions
   */
  public LinkedList<Distribution> getAxes() {
    return axes;
  }

  /**
   * Get transformation
   * 
   * @return transformation matrix, may be null.
   */
  public AffineTransformation getTrans() {
    return trans;
  }

  /**
   * Return a copy of the 'clipping minimum' vector.
   * 
   * @return vector with lower clipping bounds. May be null.
   */
  public Vector getClipmin() {
    if (clipmin == null) return null;
    return clipmin.copy();
  }
  
  /**
   * Return a copy of the 'clipping maximum' vector
   * 
   * @return vector with upper clipping bounds. May be null.
   */
  public Vector getClipmax() {
    if (clipmax == null) return null;
    return clipmax.copy();
  }

  /**
   * Return the list of points (no copy)
   */
  public LinkedList<Vector> getPoints() {
    return points;
  }

  /**
   * Set the list of points in the cluster
   * 
   * @param points New list of points in this cluster.
   */
  public void setPoints(LinkedList<Vector> points) {
    this.points = points;
  }

  /**
   * Return the size
   * 
   * @return size of this cluster.
   */
  public int getSize() {
    return size;
  }

  /**
   * Get cluster name.
   * 
   * @return name of this cluster.
   */
  public String getName() {
    return name;
  }

  /**
   * Get number of discarded points
   * 
   * @return number of discarded points
   */
  public int getDiscarded() {
    return discarded;
  }

  /**
   * Increase number of discarded points
   * 
   * @param discarded number of points discarded.
   */
  public void addDiscarded(int discarded) {
    this.discarded += discarded;
  }

  /**
   * Return number of remaining retries.
   * 
   * @return Number of retries left in this cluster.
   */
  public int getRetries() {
    return retries;
  }
  
  /**
   * Return density correction factor
   * 
   * @return density correction factor
   */
  public double getDensityCorrection() {
    return densitycorrection;
  }

  /**
   * Set density correction factor.
   * 
   * @param densitycorrection new density correction factor.
   */
  public void setDensityCorrection(double densitycorrection) {
    this.densitycorrection = densitycorrection;
  }

  /**
   * Create a new random generator (reproducible)
   * 
   * @return new random generator derived from cluster master random.
   */
  public Random getNewRandomGenerator() {
    return new Random(random.nextLong());
  }

}