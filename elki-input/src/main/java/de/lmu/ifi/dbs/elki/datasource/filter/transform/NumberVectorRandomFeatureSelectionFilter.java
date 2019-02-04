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
package de.lmu.ifi.dbs.elki.datasource.filter.transform;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.VectorUtil;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractVectorStreamConversionFilter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Parser to project the ParsingResult obtained by a suitable base parser onto a
 * randomly selected subset of attributes.
 * 
 * @author Arthur Zimek
 * @since 0.4.0
 * 
 * @assoc - - - NumberVector
 * 
 * @param <V> vector type
 */
public class NumberVectorRandomFeatureSelectionFilter<V extends NumberVector> extends AbstractVectorStreamConversionFilter<V, V> {
  /**
   * The selected attributes.
   */
  protected long[] selectedAttributes = null;

  /**
   * Holds the desired cardinality of the subset of attributes selected for
   * projection.
   */
  protected int k;

  /**
   * Holds a random generator.
   */
  protected RandomFactory rnd;

  /**
   * Constructor.
   * 
   * @param dim Dimensionality
   * @param rnd Random generator
   */
  public NumberVectorRandomFeatureSelectionFilter(int dim, RandomFactory rnd) {
    super();
    this.k = dim;
    this.rnd = rnd;
  }

  @Override
  protected V filterSingleObject(V obj) {
    return VectorUtil.project(obj, selectedAttributes, factory);
  }

  @Override
  protected SimpleTypeInformation<? super V> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  protected SimpleTypeInformation<? super V> convertedType(SimpleTypeInformation<V> in) {
    initializeRandomAttributes(in);
    initializeOutputType(in);
    return new VectorFieldTypeInformation<>(factory, k);
  }

  /**
   * Initialize random attributes.
   * 
   * Invoke this from {@link #convertedType}!
   * 
   * @param in Type information.
   */
  void initializeRandomAttributes(SimpleTypeInformation<V> in) {
    int d = ((VectorFieldTypeInformation<V>) in).getDimensionality();
    selectedAttributes = BitsUtil.random(k, d, rnd.getSingleThreadedRandom());
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter for the desired cardinality of the subset of attributes
     * selected for projection.
     */
    public static final OptionID NUMBER_SELECTED_ATTRIBUTES_ID = new OptionID("randomprojection.numberselected", "number of selected attributes");

    /**
     * Optional parameter to specify a seed for random projection. If unused,
     * system time is used as seed.
     */
    public static final OptionID SEED_ID = new OptionID("randomprojection.seed", "Seed for random selection of projection attributes.");

    /**
     * Number of attributes to select.
     */
    protected int k = 0;

    /**
     * Random generator.
     */
    protected RandomFactory rnd;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(NUMBER_SELECTED_ATTRIBUTES_ID, 1) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.intValue();
      }
      RandomParameter rndP = new RandomParameter(SEED_ID);
      if(config.grab(rndP)) {
        rnd = rndP.getValue();
      }
    }

    @Override
    protected NumberVectorRandomFeatureSelectionFilter<DoubleVector> makeInstance() {
      return new NumberVectorRandomFeatureSelectionFilter<>(k, rnd);
    }
  }
}
