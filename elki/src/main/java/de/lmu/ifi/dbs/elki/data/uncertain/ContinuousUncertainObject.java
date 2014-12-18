package de.lmu.ifi.dbs.elki.data.uncertain;

import de.lmu.ifi.dbs.elki.algorithm.clustering.uncertain.PWCClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.uncertain.probabilitydensityfunction.ProbabilityDensityFunction;
import de.lmu.ifi.dbs.elki.data.uncertain.probabilitydensityfunction.UniformDistributionFunction;
import de.lmu.ifi.dbs.elki.datasource.filter.transform.UncertainifyFilter;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

public class ContinuousUncertainObject<F extends ProbabilityDensityFunction> extends AbstractContinuousUncertainObject {

  /**
   * Field holding the probabilityDensityFunction this object
   * will use for one of the following tasks:
   * - Uncertainification in {@link UncertainifyFilter#filter(de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle)}
   * - drawing samples in {@link PWCClusteringAlgorithm#run(de.lmu.ifi.dbs.elki.database.Database, de.lmu.ifi.dbs.elki.database.relation.Relation)}
   */
  private F probabilityDensityFunction;

  /**
   *
   * Constructor for uncertainification.
   *
   * @param probabilityDensityFunction
   */
  public ContinuousUncertainObject(final F probabilityDensityFunction) {
    this.probabilityDensityFunction = probabilityDensityFunction;
  }

  /**
   *
   * Constructor.
   *
   */
  public ContinuousUncertainObject() {
    this.rand = (new RandomFactory(null)).getRandom();
  }

  /**
   *
   * Constructor.
   *
   * @param probabilityDensityFunction
   * @param dimensions
   */
  public ContinuousUncertainObject(final F probabilityDensityFunction, final int dimensions) {
    this(probabilityDensityFunction.getDefaultBounds(dimensions), probabilityDensityFunction);
  }

  /**
   *
   * Constructor.
   *
   * @param bounds
   * @param probabilityDensityFunction
   */
  public ContinuousUncertainObject(final SpatialComparable bounds, final F probabilityDensityFunction) {
    this(bounds, probabilityDensityFunction, new RandomFactory(null));
  }

  /**
   *
   * Constructor.
   *
   * @param bounds
   * @param probabilityDensityFunction
   * @param randomFactory
   */
  public ContinuousUncertainObject(final SpatialComparable bounds, final F probabilityDensityFunction, final RandomFactory randomFactory) {
    this.bounds = bounds;
    this.dimensions = bounds.getDimensionality();
    this.probabilityDensityFunction = probabilityDensityFunction;
    this.rand = randomFactory.getRandom();
  }

  /**
   *
   * Constructor.
   *
   * @param min
   * @param max
   * @param probabilityDensityFunction
   */
  public ContinuousUncertainObject(final double[] min, final double[] max, final F probabilityDensityFunction) {
    this(min, max, probabilityDensityFunction, new RandomFactory(null));
  }

  /**
   *
   * Constructor.
   *
   * @param min
   * @param max
   * @param probabilityDensityFunction
   * @param randomFactory
   */
  public ContinuousUncertainObject(final double[] min, final double[] max, final F probabilityDensityFunction, final RandomFactory randomFactory) {
    this.bounds = new HyperBoundingBox(min, max);
    this.dimensions = min.length;
    this.probabilityDensityFunction = probabilityDensityFunction;
    this.rand = randomFactory.getRandom();
  }

  @Override
  public DoubleVector drawSample() {
    return this.probabilityDensityFunction.drawValue(this.bounds, this.rand);
  }

  @Override
  public SpatialComparable getBounds() {
    return this.bounds;
  }

  @Override
  public void setBounds(final SpatialComparable bounds) {
    this.bounds = bounds;
  }

  @Override
  public int getDimensionality() {
    return this.dimensions;
  }

  /**
   *
   * Set a new dimensionality for this object.
   *
   * This will most likely not be used, since dimensionality
   * should always be clear upon the construction of an
   * uncertain database.
   *
   * TODO: maybe drop every constructors and methods with
   * the solely purpose to build uncertain objects manually -
   * I assume they won't be to useful at all.
   *
   * @param dimensions
   */
  public void setDimensionality(final int dimensions) {
    this.dimensions = dimensions;
    if(this.bounds == null || this.bounds.getDimensionality() != dimensions) {
      this.bounds = ContinuousUncertainObject.getDefaultBounds(dimensions);
    }
  }

  @Override
  public double getMin(final int dimension) {
    return this.bounds.getMin(dimension);
  }

  @Override
  public double getMax(final int dimension) {
    return this.bounds.getMax(dimension);
  }

  /**
   *
   * Get the used probability density function.
   *
   * TODO: Not sure how useful this may be.
   *
   * @return
   */
  public F getProbabilityDensityFunction() {
    return this.probabilityDensityFunction;
  }

  /**
   *
   * Set a new probability density function.
   *
   * Take note of {@link ContinuousUncertainObject#setDimensionality(int)}
   *
   * @param probabilityDensityFunction
   */
  public void setProbabilityDensityFunction(final F probabilityDensityFunction) {
    this.probabilityDensityFunction = probabilityDensityFunction;
    this.setBounds();
  }

  //TODO: assert this one can be erased
  private static HyperBoundingBox getDefaultBounds(final int dimensions) {
    final double[] min = new double[dimensions];
    final double[] max = new double[dimensions];
    for(int i = 0; i < dimensions; i++) {
      min[i] = UOModel.DEFAULT_MIN;
      max[i] = UOModel.DEFAULT_MAX;
    }
    return new HyperBoundingBox(min, max);
  }

  /**
   * set a new boundary for this object.
   *
   * TODO: not sure how useful this may be.
   */
  private void setBounds() {
    this.bounds = this.probabilityDensityFunction.getDefaultBounds(this.getDimensionality());
  }

  @Override
  public UncertainObject<UOModel<SpatialComparable>> uncertainify(final NumberVector vec, final boolean blur, final boolean uncertainify, final int dims) {
    return this.probabilityDensityFunction.uncertainify(vec, blur, uncertainify, dims);
  }

  /**
   *
   * Parameterizer class.
   *
   * @author Alexander Koos
   *
   */
  public static class Parameterizer extends AbstractParameterizer {

    /**
     * Parameter to specify the {@link ProbabilityDensityFunction} to be used
     * for uncertainification.
     */
    public static final OptionID PROBABILITY_DENSITY_FUNCTION_ID = new OptionID("cuo.pdf","This parameter is used to choose what kind of continuous probability-density-model is to be used.");

    /**
     * Field to hold parameter value.
     */
    protected ProbabilityDensityFunction pdf;

    @Override
    protected void makeOptions(final Parameterization config) {
      super.makeOptions(config);
      @SuppressWarnings({ "unchecked", "rawtypes" })
      final ObjectParameter ppdf = new ObjectParameter(Parameterizer.PROBABILITY_DENSITY_FUNCTION_ID, ProbabilityDensityFunction.class);
      if(config.grab(ppdf)) {
        this.pdf = (ProbabilityDensityFunction) ppdf.instantiateClass(config);
      }
    }

    @Override
    protected Object makeInstance() {
      return new ContinuousUncertainObject<ProbabilityDensityFunction>(this.pdf);
    }

  }

  @Override
  public void writeToText(final TextWriterStream out, final String label) {
    if(this.probabilityDensityFunction.getClass().equals(UniformDistributionFunction.class)) {
      String res = "";
      if(label != null) {
        for(int i = 0; i < this.bounds.getDimensionality(); i++) {
          res += label + "= " + "dimMin(" + i + "): " + this.bounds.getMin(i)
              + " dimMax(" + i +"): " + this.bounds.getMax(i) + "\n";
        }
      }
      out.inlinePrintNoQuotes(res);
    } else {
      this.probabilityDensityFunction.writeToText(out, label);
    }
  }

  @Override
  public DoubleVector getAnker() {
    return this.probabilityDensityFunction.getAnker(this.bounds);
  }
}
