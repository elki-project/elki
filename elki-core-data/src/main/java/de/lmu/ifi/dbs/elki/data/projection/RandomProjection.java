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
package de.lmu.ifi.dbs.elki.data.projection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.projection.random.AchlioptasRandomProjectionFamily;
import de.lmu.ifi.dbs.elki.data.projection.random.RandomProjectionFamily;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Randomized projections of the data.
 * 
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
  public static class Parameterizer extends AbstractParameterizer {
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
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<RandomProjectionFamily> familyP = new ObjectParameter<>(FAMILY_ID, RandomProjectionFamily.class);
      familyP.setDefaultValue(AchlioptasRandomProjectionFamily.class);
      if(config.grab(familyP)) {
        family = familyP.instantiateClass(config);
      }

      IntParameter dimP = new IntParameter(DIMENSIONALITY_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(dimP)) {
        dimensionality = dimP.intValue();
      }
    }

    @Override
    protected RandomProjection<NumberVector> makeInstance() {
      return new RandomProjection<>(dimensionality, family);
    }
  }
}
