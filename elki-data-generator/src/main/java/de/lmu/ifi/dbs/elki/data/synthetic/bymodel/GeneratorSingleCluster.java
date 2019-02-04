/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.data.synthetic.bymodel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.model.GeneratorModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.math.linearalgebra.AffineTransformation;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Class to generate a single cluster according to a model as well as getting
 * the density of a given model at that point (to evaluate generated points
 * according to the same model)
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @composed - - - Distribution
 * @composed - - - AffineTransformation
 */
public class GeneratorSingleCluster implements GeneratorInterfaceDynamic {
  /**
   * The distribution generators for each axis
   */
  private List<Distribution> axes = new ArrayList<>();

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
  private double[] clipmin, clipmax;

  /**
   * Correction factor for probability computation
   */
  private double densitycorrection = 1.0;

  /**
   * Number of points in the cluster (~density)
   */
  private int size;

  /**
   * Cluster name
   */
  private String name;

  /**
   * Retry count
   */
  // TODO: make configurable?
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
   */
  public void addGenerator(Distribution gen) {
    if(trans != null) {
      throw new AbortException("Generators may no longer be added when transformations have been applied.");
    }
    axes.add(gen);
    dim++;
  }

  /**
   * Apply a rotation to the generator
   *
   * @param axis1 First axis (0 &lt;= axis1 &lt; dim)
   * @param axis2 Second axis (0 &lt;= axis2 &lt; dim)
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
   * @param v translation double[]
   */
  public void addTranslation(double[] v) {
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
   */
  public void setClipping(double[] min, double[] max) {
    // if only one dimension was given, expand to all dimensions.
    if(min.length == 1 && max.length == 1) {
      if(min[0] >= max[0]) {
        throw new AbortException("Clipping range empty.");
      }
      clipmin = new double[dim];
      clipmax = new double[dim];
      Arrays.fill(clipmin, min[0]);
      Arrays.fill(clipmax, max[0]);
      return;
    }
    if(dim != min.length) {
      throw new AbortException("Clipping double[] dimensionalities do not match: " + dim + " vs. " + min.length);
    }
    if(dim != max.length) {
      throw new AbortException("Clipping double[] dimensionalities do not match: " + dim + " vs. " + max.length);
    }
    for(int i = 0; i < dim; i++) {
      if(min[i] >= max[i]) {
        throw new AbortException("Clipping range empty in dimension " + (i + 1));
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
  private boolean testClipping(double[] p) {
    if(clipmin == null || clipmax == null) {
      return false;
    }
    for(int i = 0; i < p.length; i++) {
      if(p[i] < clipmin[i] || p[i] > clipmax[i]) {
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
  public List<double[]> generate(int count) {
    ArrayList<double[]> result = new ArrayList<>(count);
    while(result.size() < count) {
      double[] d = new double[dim];
      for(int i = 0; i < dim; i++) {
        d[i] = axes.get(i).nextRandom();
      }
      if(trans != null) {
        d = trans.apply(d);
      }
      if(testClipping(d)) {
        if(--retries < 0) {
          throw new AbortException("Maximum retry count in generator exceeded.");
        }
        continue;
      }
      result.add(d);
    }
    return result;
  }

  /**
   * Compute density for cluster model at given double[] p-
   *
   * @see GeneratorInterface#getDensity(double[])
   */
  @Override
  public double getDensity(double[] p) {
    if(trans != null) {
      p = trans.applyInverse(p);
    }

    double density = densitycorrection;
    for(int i = 0; i < dim; i++) {
      density *= axes.get(i).pdf(p[i]);
    }
    return density;
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
   * Return a copy of the 'clipping minimum' double[].
   *
   * @return double[] with lower clipping bounds. May be null.
   */
  public double[] getClipmin() {
    if(clipmin == null) {
      return null;
    }
    return clipmin;
  }

  /**
   * Return a copy of the 'clipping maximum' double[]
   *
   * @return double[] with upper clipping bounds. May be null.
   */
  public double[] getClipmax() {
    if(clipmax == null) {
      return null;
    }
    return clipmax;
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
    return new GeneratorModel(this, computeMean());
  }

  /**
   * Get distribution along (generator) axis i.
   *
   * @param i Generator axis i
   * @return Distribution
   */
  public Distribution getDistribution(int i) {
    return axes.get(i);
  }

  @Override
  public double[] computeMean() {
    double[] v = new double[dim];
    for(int i = 0; i < dim; i++) {
      v[i] = axes.get(i).quantile(0.5);
    }
    if(trans != null) {
      v = trans.apply(v);
    }
    return v;
  }
}
