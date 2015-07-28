package de.lmu.ifi.dbs.elki.data.uncertain;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.algorithm.clustering.uncertain.PWCClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.uncertain.probabilitydensityfunction.ProbabilityDensityFunction;
import de.lmu.ifi.dbs.elki.datasource.filter.transform.UncertainifyFilter;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

public class ContinuousUncertainObject<F extends ProbabilityDensityFunction> extends UOModel {
  /**
   * Field holding the probabilityDensityFunction this object will use for one
   * of the following tasks: - Uncertainification in
   * {@link UncertainifyFilter#filter(de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle)}
   * - drawing samples in
   * {@link PWCClusteringAlgorithm#run(de.lmu.ifi.dbs.elki.database.Database, de.lmu.ifi.dbs.elki.database.relation.Relation)}
   */
  private F probabilityDensityFunction;

  /**
   * Constructor for uncertainification.
   *
   * @param probabilityDensityFunction
   */
  public ContinuousUncertainObject(final F probabilityDensityFunction) {
    this.probabilityDensityFunction = probabilityDensityFunction;
  }

  /**
   * Constructor.
   */
  public ContinuousUncertainObject() {
    this.rand = (new RandomFactory(null)).getRandom();
  }

  /**
   * Constructor.
   *
   * @param probabilityDensityFunction
   * @param dimensions
   */
  public ContinuousUncertainObject(final F probabilityDensityFunction, final int dimensions) {
    this(probabilityDensityFunction.getDefaultBounds(dimensions), probabilityDensityFunction);
  }

  /**
   * Constructor.
   *
   * @param bounds
   * @param probabilityDensityFunction
   */
  public ContinuousUncertainObject(final SpatialComparable bounds, final F probabilityDensityFunction) {
    this(bounds, probabilityDensityFunction, new RandomFactory(null));
  }

  /**
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
  public UncertainObject<UOModel> uncertainify(final NumberVector vec, final boolean blur, final boolean uncertainify, final int dims) {
    return this.probabilityDensityFunction.uncertainify(vec, blur, uncertainify, dims);
  }

  /**
   * Parameterizer class.
   *
   * @author Alexander Koos
   */
  public static class Parameterizer<F extends ProbabilityDensityFunction> extends AbstractParameterizer {
    /**
     * Parameter to specify the {@link ProbabilityDensityFunction} to be used
     * for uncertainification.
     */
    public static final OptionID PROBABILITY_DENSITY_FUNCTION_ID = new OptionID("cuo.pdf", "This parameter is used to choose what kind of continuous probability-density-model is to be used.");

    /**
     * Field to hold parameter value.
     */
    protected F pdf;

    @Override
    protected void makeOptions(final Parameterization config) {
      super.makeOptions(config);
      final ObjectParameter<F> ppdf = new ObjectParameter<>(Parameterizer.PROBABILITY_DENSITY_FUNCTION_ID, ProbabilityDensityFunction.class);
      if(config.grab(ppdf)) {
        this.pdf = ppdf.instantiateClass(config);
      }
    }

    @Override
    protected ContinuousUncertainObject<F> makeInstance() {
      return new ContinuousUncertainObject<F>(this.pdf);
    }
  }
}
