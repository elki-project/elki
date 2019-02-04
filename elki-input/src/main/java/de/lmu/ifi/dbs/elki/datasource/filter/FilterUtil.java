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
package de.lmu.ifi.dbs.elki.datasource.filter;

import java.lang.reflect.Field;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;

/**
 * Utilities for implementing filters.
 * 
 * @author Erich Schubert
 * @since 0.5.0
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
  public static <V extends NumberVector> NumberVector.Factory<V> guessFactory(SimpleTypeInformation<V> in) {
    NumberVector.Factory<V> factory = null;
    if(in instanceof VectorTypeInformation) {
      factory = (NumberVector.Factory<V>) ((VectorTypeInformation<V>) in).getFactory();
    }
    if(factory == null) {
      // FIXME: hack. Add factories to simple type information, too?
      try {
        Field f = in.getRestrictionClass().getField("FACTORY");
        factory = (NumberVector.Factory<V>) f.get(null);
      }
      catch(Exception e) {
        LoggingUtil.warning("Cannot determine factory for type " + in.getRestrictionClass(), e);
      }
    }
    return factory;
  }

  /**
   * Find the first "label-like" column (matching {@link TypeUtil#GUESSED_LABEL}
   * ) in a bundle.
   * 
   * @param bundle Bundle
   * @return Column number, or {@code -1}.
   */
  public static int findLabelColumn(MultipleObjectsBundle bundle) {
    for(int i = 0; i < bundle.metaLength(); i++) {
      if(TypeUtil.GUESSED_LABEL.isAssignableFromType(bundle.meta(i))) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Find the first "label-like" column (matching {@link TypeUtil#GUESSED_LABEL}
   * ) in a type information.
   * 
   * @param meta Meta data
   * @return Column number, or {@code -1}.
   */
  public static int findLabelColumn(BundleMeta meta) {
    for(int i = 0; i < meta.size(); i++) {
      if(TypeUtil.GUESSED_LABEL.isAssignableFromType(meta.get(i))) {
        return i;
      }
    }
    return -1;
  }
}