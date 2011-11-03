package de.lmu.ifi.dbs.elki.datasource.filter.normalization;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractConversionFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;

/**
 * Abstract super class for all normalizations.
 * 
 * @author Elke Achtert
 * 
 * @param <O> Object type processed
 */
public abstract class AbstractNormalization<O> extends AbstractConversionFilter<O, O> implements Normalization<O> {
  /**
   * Initializes the option handler and the parameter map.
   */
  protected AbstractNormalization() {
    super();
  }

  @Override
  protected SimpleTypeInformation<? super O> convertedType(SimpleTypeInformation<O> in) {
    return in;
  }

  @Override
  public final MultipleObjectsBundle normalizeObjects(MultipleObjectsBundle objects) {
    return normalizeObjects(objects);
  }

  @Override
  public LinearEquationSystem transform(LinearEquationSystem linearEquationSystem) {
    // FIXME: implement.
    throw new UnsupportedOperationException("Not yet implemented!");
  }

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer();
    result.append("normalization class: ").append(getClass().getName());
    return result.toString();
  }
}