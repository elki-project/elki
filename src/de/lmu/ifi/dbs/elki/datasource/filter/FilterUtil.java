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

import java.lang.reflect.Field;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;

/**
 * Utilities for implementing filters.
 * 
 * @author Erich Schubert
 */
public final class FilterUtil {
  /**
   * Fake constructor: do not instantiate.
   */
  private FilterUtil() {
    // Do not instantiate.
  }

  /**
   * Try to guess the appropriate factory.
   * 
   * @param in Input type
   * @param <V> Vector type
   * @return Factory
   */
  @SuppressWarnings("unchecked")
  public static <V extends NumberVector<?>> NumberVector.Factory<V, ?> guessFactory(SimpleTypeInformation<V> in) {
    NumberVector.Factory<V, ?> factory = null;
    if(in instanceof VectorFieldTypeInformation) {
      factory = (NumberVector.Factory<V, ?>) ((VectorFieldTypeInformation<V>) in).getFactory();
    }
    if(factory == null) {
      // FIXME: hack. Add factories to simple type information, too?
      try {
        Field f = in.getRestrictionClass().getField("FACTORY");
        factory = (NumberVector.Factory<V, ?>) f.get(null);
      }
      catch(Exception e) {
        LoggingUtil.warning("Cannot determine factory for type " + in.getRestrictionClass(), e);
      }
    }
    return factory;
  }
}