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

import java.util.Random;
import de.lmu.ifi.dbs.elki.algorithm.clustering.uncertain.PWCClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.uncertain.probabilitydensityfunction.ProbabilityDensityFunction;
import de.lmu.ifi.dbs.elki.datasource.filter.typeconversions.UncertainifyFilter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Continuous uncertain objects are determined by a distribution function, and
 * can be sampled from repeatedly.
 *
 * @author Alexander Koos
 * @author Erich Schubert
 */
public class ContinuousUncertainObject extends AbstractUncertainObject {
  /**
   * Field holding the probabilityDensityFunction this object will use for
   * drawing samples in {@link PWCClusteringAlgorithm}
   */
  private ProbabilityDensityFunction probabilityDensityFunction;

  /**
   * Constructor.
   */
  public ContinuousUncertainObject() {
  }

  /**
   * Constructor.
   *
   * @param probabilityDensityFunction
   * @param dimensions
   */
  public ContinuousUncertainObject(ProbabilityDensityFunction probabilityDensityFunction, int dimensions) {
    this(probabilityDensityFunction.getDefaultBounds(dimensions), probabilityDensityFunction);
  }

  /**
   * Constructor.
   *
   * @param bounds
   * @param probabilityDensityFunction
   */
  public ContinuousUncertainObject(SpatialComparable bounds, ProbabilityDensityFunction probabilityDensityFunction) {
    this.bounds = bounds;
    this.probabilityDensityFunction = probabilityDensityFunction;
  }

  /**
   * Constructor.
   *
   * @param min
   * @param max
   * @param probabilityDensityFunction
   */
  public ContinuousUncertainObject(double[] min, double[] max, ProbabilityDensityFunction probabilityDensityFunction) {
    this.bounds = new HyperBoundingBox(min, max);
    this.probabilityDensityFunction = probabilityDensityFunction;
  }

  @Override
  public DoubleVector drawSample(Random rand) {
    return probabilityDensityFunction.drawValue(bounds, rand);
  }

  @Override
  public DoubleVector getMean() {
    return probabilityDensityFunction.getMean(bounds);
  }

  @Override
  public Double getValue(int dimension) {
    // Not particularly meaningful, but currently unused anyway.
    return (bounds.getMax(dimension) + bounds.getMin(dimension)) * .5;
  }

  public static class Factory extends AbstractUncertainObject.Factory<ContinuousUncertainObject> {
    /**
     * Field holding the probabilityDensityFunction this object will use for
     * uncertainification in {@link UncertainifyFilter#filter}
     */
    private ProbabilityDensityFunction probabilityDensityFunction;

    /**
     * Constructor for uncertainification.
     *
     * @param probabilityDensityFunction
     */
    public Factory(boolean symmetric, ProbabilityDensityFunction probabilityDensityFunction) {
      super(symmetric);
      this.probabilityDensityFunction = probabilityDensityFunction;
    }

    @Override
    public <A> ContinuousUncertainObject newFeatureVector(A array, NumberArrayAdapter<?, A> adapter) {
      return probabilityDensityFunction.uncertainify(array, adapter, !symmetric);
    }

    @Override
    public ByteBufferSerializer<ContinuousUncertainObject> getDefaultSerializer() {
      return null; // FIXME: not yet available.
    }

    @Override
    public Class<? super ContinuousUncertainObject> getRestrictionClass() {
      return ContinuousUncertainObject.class;
    }

    /**
     * Parameterizer class.
     *
     * @author Alexander Koos
     */
    public static class Parameterizer extends AbstractUncertainObject.Factory.Parameterizer {
      /**
       * Parameter to specify the {@link ProbabilityDensityFunction} to be used
       * for uncertainification.
       */
      public static final OptionID PROBABILITY_DENSITY_FUNCTION_ID = new OptionID("cuo.pdf", "This parameter is used to choose what kind of continuous probability-density-model is to be used.");

      /**
       * Field to hold parameter value.
       */
      protected ProbabilityDensityFunction pdf;

      /**
       *
       */
      protected boolean symmetric;

      @Override
      protected void makeOptions(final Parameterization config) {
        super.makeOptions(config);
        final ObjectParameter<ProbabilityDensityFunction> ppdf = new ObjectParameter<>(PROBABILITY_DENSITY_FUNCTION_ID, ProbabilityDensityFunction.class);
        if(config.grab(ppdf)) {
          this.pdf = ppdf.instantiateClass(config);
        }
        Flag symmetricF = new Flag(SYMMETRIC_ID);
        if(config.grab(symmetricF)) {
          symmetric = symmetricF.isTrue();
        }
      }

      @Override
      protected Factory makeInstance() {
        return new Factory(symmetric, pdf);
      }
    }
  }
}
