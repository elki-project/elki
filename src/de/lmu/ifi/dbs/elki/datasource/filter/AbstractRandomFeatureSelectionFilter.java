package de.lmu.ifi.dbs.elki.datasource.filter;

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

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * <p>
 * A RandomProjectionParser selects a subset of attributes randomly for
 * projection of a ParsingResult.
 * </p>
 * 
 * The cardinality of the subset of attributes is specified as a parameter.
 * 
 * @author Arthur Zimek
 * @author Erich Schubert
 * 
 * @param <V> the type of FeatureVector contained in both the original data of
 *        the base parser and the projected data of this ProjectionParser
 */
public abstract class AbstractRandomFeatureSelectionFilter<V extends FeatureVector<?, ?>> extends AbstractStreamConversionFilter<V, V> {
  /**
   * The selected attributes
   */
  protected BitSet selectedAttributes = null;

  /**
   * Parameter for the desired cardinality of the subset of attributes selected
   * for projection.
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
   * Holds the desired cardinality of the subset of attributes selected for
   * projection.
   */
  protected int k;

  /**
   * Holds a random object.
   */
  protected final Random random = new Random();

  /**
   * Constructor.
   * 
   * @param dim dimensionality
   */
  public AbstractRandomFeatureSelectionFilter(int dim) {
    super();
    this.k = dim;
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
  public static abstract class Parameterizer<V extends NumberVector<V, ?>> extends AbstractParameterizer {
    protected int k = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(NUMBER_SELECTED_ATTRIBUTES_ID, new GreaterEqualConstraint(1), 1);
      if(config.grab(kP)) {
        k = kP.getValue();
      }
    }
  }
}