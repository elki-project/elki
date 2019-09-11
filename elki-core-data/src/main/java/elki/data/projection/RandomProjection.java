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
package elki.data.projection;

import elki.data.NumberVector;
import elki.data.projection.random.AchlioptasRandomProjectionFamily;
import elki.data.projection.random.RandomProjectionFamily;
import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.data.type.VectorFieldTypeInformation;
import elki.logging.Logging;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Randomized projections of the data.
 * <p>
 * This class allows projecting the data with different types of random
 * projections, in particular database friendly projections (as suggested by
 * Achlioptas, see {@link AchlioptasRandomProjectionFamily}), but also as
 * suggested for locality sensitive hashing (LSH).
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @assoc - - - NumberVector
 * 
 * @param <V> Vector type
 */
public class RandomProjection<V extends NumberVector> implements Projection<V, V> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(RandomProjection.class);

  /**
   * Vector factory.
   */
  private NumberVector.Factory<V> factory = null;

  /**
   * Output dimensionality.
   */
  private int dimensionality;

  /**
   * Projection matrix.
   */
  private RandomProjectionFamily.Projection projection = null;

  /**
   * Random projection family
   */
  private RandomProjectionFamily family;

  /**
   * Constructor.
   * 
   * @param dimensionality Desired dimensionality
   * @param family Random projection family
   */
  public RandomProjection(int dimensionality, RandomProjectionFamily family) {
    super();
    this.dimensionality = dimensionality;
    this.family = family;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void initialize(SimpleTypeInformation<? extends V> in) {
    final VectorFieldTypeInformation<V> vin = (VectorFieldTypeInformation<V>) in;
    factory = (NumberVector.Factory<V>) vin.getFactory();
    int inputdim = vin.getDimensionality();

    projection = family.generateProjection(inputdim, dimensionality);
    if(LOG.isDebugging()) {
      LOG.debug(projection.toString());
    }
  }

  @Override
  public V project(V data) {
    return factory.newNumberVector(projection.project(data));
  }

  @Override
  public TypeInformation getInputDataTypeInformation() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  public SimpleTypeInformation<V> getOutputDataTypeInformation() {
    return new VectorFieldTypeInformation<>(factory, dimensionality);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter for the projection family.
     */
    public static final OptionID FAMILY_ID = new OptionID("randomproj.family", "Projection family to use.");

    /**
     * Parameter for the desired output dimensionality.
     */
    public static final OptionID DIMENSIONALITY_ID = new OptionID("randomproj.dimensionality", "Amount of dimensions to project to.");

    /**
     * Output dimensionality.
     */
    private int dimensionality;

    /**
     * Random generator.
     */
    private RandomProjectionFamily family;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<RandomProjectionFamily>(FAMILY_ID, RandomProjectionFamily.class) //
          .setDefaultValue(AchlioptasRandomProjectionFamily.class) //
          .grab(config, x -> family = x);
      new IntParameter(DIMENSIONALITY_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> dimensionality = x);
    }

    @Override
    public RandomProjection<NumberVector> make() {
      return new RandomProjection<>(dimensionality, family);
    }
  }
}
