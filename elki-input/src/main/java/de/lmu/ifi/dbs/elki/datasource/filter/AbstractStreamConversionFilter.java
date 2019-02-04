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

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;

/**
 * Abstract base class for simple conversion filters such as normalizations and
 * projections.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @param <I> Input object type
 * @param <O> Input object type
 */
public abstract class AbstractStreamConversionFilter<I, O> extends AbstractStreamFilter {
  /**
   * The filtered meta.
   */
  BundleMeta meta;

  /**
   * The column to filter.
   */
  int column = -1;
  
  @Override
  public BundleMeta getMeta() {
    return meta;
  }

  @Override
  public Object data(int rnum) {
    if(rnum != column) {
      return source.data(rnum);
    }
    // Convert:
    @SuppressWarnings("unchecked")
    final I obj = (I) source.data(rnum);
    return filterSingleObject(obj);
  }

  @Override
  public Event nextEvent() {
    Event ev = source.nextEvent();
    if(ev == Event.META_CHANGED) {
      if(meta == null) {
        meta = new BundleMeta();
      }
      BundleMeta origmeta = source.getMeta();
      for(int i = meta.size(); i < origmeta.size(); i++) {
        if(column < 0) {
          @SuppressWarnings("unchecked")
          SimpleTypeInformation<Object> type = (SimpleTypeInformation<Object>) origmeta.get(i);
          // Test whether this type matches
          if(getInputTypeRestriction().isAssignableFromType(type)) {
            @SuppressWarnings("unchecked")
            final SimpleTypeInformation<I> castType = (SimpleTypeInformation<I>) type;
            meta.add(convertedType(castType));
            column = i;
            continue;
          }
        }
        meta.add(origmeta.get(i));
      }
    }
    return ev;
  }

  /**
   * Normalize a single instance.
   * 
   * You can implement this as UnsupportedOperationException if you override
   * both public "normalize" functions!
   * 
   * @param obj Database object to normalize
   * @return Normalized database object
   */
  protected abstract O filterSingleObject(I obj);

  /**
   * Get the input type restriction used for negotiating the data query.
   * 
   * @return Type restriction
   */
  protected abstract TypeInformation getInputTypeRestriction();

  /**
   * Get the output type from the input type after conversion.
   * 
   * @param in input type restriction
   * @return output type restriction
   */
  protected abstract SimpleTypeInformation<? super O> convertedType(SimpleTypeInformation<I> in);
}