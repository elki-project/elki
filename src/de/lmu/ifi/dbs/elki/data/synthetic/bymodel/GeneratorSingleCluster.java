package de.lmu.ifi.dbs.elki.data.synthetic.bymodel;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.math.linearalgebra.AffineTransformation;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.DistributionWithRandom;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;

/**
 * Class to generate a single cluster according to a model as well as getting
 * the density of a given model at that point (to evaluate generated points
 * according to the same model)
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf DistributionWithRandom
 * @apiviz.composedOf AffineTransformation
 */
public class GeneratorSingleCluster implements GeneratorInterfaceDynamic, Model {
  /**
   * The distribution generators for each axis
   */
  private List<DistributionWithRandom> axes = new ArrayList<DistributionWithRandom>();

  /**
   * The transformation matrix
   */
  private AffineTransformation trans;

  /**
   * The dimensionality
   */
  private int dim;

  /**
   * Clipping vectors. Note: currently, either both or none have to be set!
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
   * Random generator (used for initializing random generators)
   */
  private Random random;

  /**
   * Generator (without axes)
   * 
   * @param name Cluster name
   * @param size Cluster size
   * @param densitycorrection Density correction factor
   * @param random Random number generator
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
   * @param gen Distribution generator
   * @throws UnableToComplyException thrown when no new generators may be added
   *         anymore
   */
  public void addGenerator(DistributionWithRandom gen) throws UnableToComplyException {
    if(trans != null) {
      throw new UnableToComplyException("Generators may no longer be added when transformations have been applied.");
    }
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
    if(trans == null) {
      trans = new AffineTransformation(dim);
    }
    trans.addRotation(axis1, axis2, angle);
  }

  /**
   * Add a translation to the generator
   * 
   * @param v translation vector
   */
  public void addTranslation(Vector v) {
    if(trans == null) {
      trans = new AffineTransformation(dim);
    }
    trans.addTranslation(v);
  }

  /**
   * Set a clipping box. min needs to be smaller than max in each component.
   * Note: Clippings are not 'modified' by translation / rotation /
   * transformation operations.
   * 
   * @param min Minimum values for clipping
   * @param max Maximum values for clipping
   * @throws UnableToComplyException thrown when invalid vectors were given.
   */
  public void setClipping(Vector min, Vector max) throws UnableToComplyException {
    // if only one dimension was given, expand to all dimensions.
    if(min.getDimensionality() == 1 && max.getDimensionality() == 1) {
      if(min.get(0) >= max.get(0)) {
        throw new UnableToComplyException("Clipping range empty.");
      }
      clipmin = new Vector(dim);
      clipmax = new Vector(dim);
      for(int i = 0; i < dim; i++) {
        clipmin.set(i, min.get(0));
        clipmax.set(i, max.get(0));
      }
      return;
    }
    if(dim != min.getDimensionality()) {
      throw new UnableToComplyException("Clipping vector dimensionalities do not match: " + dim + " vs. " + min.getDimensionality());
    }
    if(dim != max.getDimensionality()) {
      throw new UnableToComplyException("Clipping vector dimensionalities do not match: " + dim + " vs. " + max.getDimensionality());
    }
    for(int i = 0; i < dim; i++) {
      if(min.get(i) >= max.get(i)) {
        throw new UnableToComplyException("Clipping range empty in dimension " + (i + 1));
      }
    }
    clipmin = min;
    clipmax = max;
  }

  /**
   * Get the cluster dimensionality
   * 
   * @return dimensionality
   */
  @Override
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
    if(clipmin == null || clipmax == null) {
      return false;
    }
    for(int i = 0; i < p.getDimensionality(); i++) {
      if(p.get(i) < clipmin.get(i)) {
        return true;
      }
      if(p.get(i) > clipmax.get(i)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Generate the given number of additional points.
   * 
   * @see de.lmu.ifi.dbs.elki.data.synthetic.bymodel.GeneratorInterface#generate(int)
   */
  @Override
  public List<Vector> generate(int count) throws UnableToComplyException {
    ArrayList<Vector> result = new ArrayList<Vector>(count);
    while(result.size() < count) {
      double[] d = new double[dim];
      int i = 0;
      for(DistributionWithRandom axis : axes) {
        d[i] = axis.nextRandom();
        i++;
      }
      Vector p = new Vector(d);
      if(trans != null) {
        p = trans.apply(p);
      }
      if(testClipping(p)) {
        retries--;
        if(retries < 0) {
          throw new UnableToComplyException("Maximum retry count in generator exceeded.");
        }
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
  @Override
  public double getDensity(Vector p) {
    Vector o = p;
    if(trans != null) {
      o = trans.applyInverse(p);
    }

    double density = 1.0;
    int i = 0;
    for(DistributionWithRandom axis : axes) {
      density = density * axis.pdf(o.get(i));
      i++;
    }
    return density * densitycorrection;
  }

  /**
   * Get transformation
   * 
   * @return transformation matrix, may be null.
   */
  public AffineTransformation getTransformation() {
    return trans;
  }

  /**
   * Return a copy of the 'clipping minimum' vector.
   * 
   * @return vector with lower clipping bounds. May be null.
   */
  public Vector getClipmin() {
    if(clipmin == null) {
      return null;
    }
    return clipmin.copy();
  }

  /**
   * Return a copy of the 'clipping maximum' vector
   * 
   * @return vector with upper clipping bounds. May be null.
   */
  public Vector getClipmax() {
    if(clipmax == null) {
      return null;
    }
    return clipmax.copy();
  }

  /**
   * Return the size
   * 
   * @return size of this cluster.
   */
  @Override
  public int getSize() {
    return size;
  }

  /**
   * Get cluster name.
   * 
   * @return name of this cluster.
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Get number of discarded points
   * 
   * @return number of discarded points
   */
  @Override
  public int getDiscarded() {
    return discarded;
  }

  /**
   * Increase number of discarded points
   */
  @Override
  public void incrementDiscarded() {
    ++this.discarded;
  }

  /**
   * Return number of remaining retries.
   * 
   * @return Number of retries left in this cluster.
   */
  @Override
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

  /**
   * Make a cluster model for this cluster.
   * 
   * @return Model
   */
  @Override
  public Model makeModel() {
    return this;
  }

  /**
   * Get distribution along (generator) axis i.
   * 
   * @param i Generator axis i
   * @return Distribution
   */
  public DistributionWithRandom getDistribution(int i) {
    return axes.get(i);
  }
}