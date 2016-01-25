package de.lmu.ifi.dbs.elki.datasource.filter.normalization;

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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractVectorConversionFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;

/**
 * Abstract super class for all normalizations.
 * 
 * @author Elke Achtert
 * @since 0.4.0
 * 
 * @param <V> Object type processed
 */
public abstract class AbstractNormalization<V extends NumberVector> extends AbstractVectorConversionFilter<V, V> implements Normalization<V> {
  /**
   * Initializes the option handler and the parameter map.
   */
  protected AbstractNormalization() {
    super();
  }

  @Override
  protected SimpleTypeInformation<? super V> convertedType(SimpleTypeInformation<V> in) {
    initializeOutputType(in);
    return in;
  }

  @Override
  public V restore(V featureVector) throws NonNumericFeaturesException {
    // FIXME: implement everywhere.
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public LinearEquationSystem transform(LinearEquationSystem linearEquationSystem) {
    // FIXME: implement.
    throw new UnsupportedOperationException("Not yet implemented!");
  }

  @Override
  public String toString() {
    return getClass().getName();
  }
}
