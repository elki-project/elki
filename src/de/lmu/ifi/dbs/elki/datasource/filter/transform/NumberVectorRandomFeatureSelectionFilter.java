package de.lmu.ifi.dbs.elki.datasource.filter.transform;

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

import java.util.BitSet;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.VectorUtil;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractVectorStreamConversionFilter;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;

/**
 * Parser to project the ParsingResult obtained by a suitable base parser onto a
 * randomly selected subset of attributes.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.uses NumberVector
 * 
 * @param <V> vector type
 */
public class NumberVectorRandomFeatureSelectionFilter<V extends NumberVector<?>> extends AbstractVectorStreamConversionFilter<V, V> {
  /**
   * The selected attributes.
   */
  protected BitSet selectedAttributes = null;

  /**
   * Holds the desired cardinality of the subset of attributes selected for
   * projection.
   */
  protected int k;

  /**
   * Holds a random object.
   */
  protected final Random random;

  /**
   * Constructor.
   * 
   * @param dim Dimensionality
   * @param seed Random seed
   */
  public NumberVectorRandomFeatureSelectionFilter(int dim, Long seed) {
    super();
    this.k = dim;
    this.random = (seed == null) ? new Random() : new Random(seed);
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
    return new VectorFieldTypeInformation<V>(factory, k);
  }

  /**
   * Initialize random attributes.
   * 
   * Invoke this from {@link #convertedType}!
   * 
   * @param in Type information.
   */
  void initializeRandomAttributes(SimpleTypeInformation<V> in) {
    int d = ((VectorFieldTypeInformation<V>) in).dimensionality();
    selectedAttributes = Util.randomBitSet(k, d, random);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter for the desired cardinality of the subset of attributes
     * selected for projection.
     * 
     * <p>
     * Key: <code>-randomprojection.numberselected</code>
     * </p>
     * <p>
     * Default: <code>1</code>
     * </p>
     * <p>
     * Constraint: &ge;1
     * </p>
     */
    public static final OptionID NUMBER_SELECTED_ATTRIBUTES_ID = OptionID.getOrCreateOptionID("randomprojection.numberselected", "number of selected attributes");

    /**
     * Optional parameter to specify a seed for random projection. If unused,
     * system time is used as seed.
     * <p>
     * Key: {@code -randomprojection.seed}
     * </p>
     */
    public static final OptionID SEED_ID = OptionID.getOrCreateOptionID("randomprojection.seed", "Seed for random selection of projection attributes.");

    /**
     * Number of attributes to select.
     */
    protected int k = 0;

    /**
     * Random seed.
     */
    protected Long seed;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(NUMBER_SELECTED_ATTRIBUTES_ID, new GreaterEqualConstraint(1), 1);
      if (config.grab(kP)) {
        k = kP.getValue();
      }
      LongParameter seedP = new LongParameter(SEED_ID, true);
      if (config.grab(seedP)) {
        seed = seedP.getValue();
      }
    }

    @Override
    protected NumberVectorRandomFeatureSelectionFilter<DoubleVector> makeInstance() {
      return new NumberVectorRandomFeatureSelectionFilter<DoubleVector>(k, seed);
    }
  }
}
