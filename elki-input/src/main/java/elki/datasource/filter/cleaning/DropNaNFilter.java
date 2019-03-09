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
package elki.datasource.filter.cleaning;

import java.util.ArrayList;

import elki.data.NumberVector;
import elki.data.type.TypeUtil;
import elki.datasource.bundle.BundleMeta;
import elki.datasource.bundle.MultipleObjectsBundle;
import elki.datasource.filter.AbstractStreamFilter;
import elki.logging.Logging;
import elki.utilities.datastructures.BitsUtil;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.AbstractParameterizer;

/**
 * A filter to drop all records that contain NaN values.
 * 
 * Note: currently, only dense vector columns are supported.
 * 
 * TODO: add support for sparse vectors.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class DropNaNFilter extends AbstractStreamFilter {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(DropNaNFilter.class);

  /**
   * Columns to check.
   */
  private long[] densecols = null;

  /**
   * Constructor.
   */
  public DropNaNFilter() {
    super();
  }

  @Override
  public BundleMeta getMeta() {
    return source.getMeta();
  }

  @Override
  public Object data(int rnum) {
    return source.data(rnum);
  }

  @Override
  public Event nextEvent() {
    while(true) {
      Event ev = source.nextEvent();
      switch(ev){
      case END_OF_STREAM:
        return ev;
      case META_CHANGED:
        updateMeta(source.getMeta());
        return ev;
      case NEXT_OBJECT:
        if(densecols == null) {
          updateMeta(source.getMeta());
        }
        boolean good = true;
        for(int j = BitsUtil.nextSetBit(densecols, 0); j >= 0; j = BitsUtil.nextSetBit(densecols, j + 1)) {
          NumberVector v = (NumberVector) source.data(j);
          if(v == null) {
            good = false;
            break;
          }
          for(int i = 0; i < v.getDimensionality(); i++) {
            if(Double.isNaN(v.doubleValue(i))) {
              good = false;
              break;
            }
          }
        }
        if(good) {
          return ev;
        }
        continue;
      }
    }
  }

  /**
   * Process an updated meta record.
   * 
   * @param meta Meta record
   */
  private void updateMeta(BundleMeta meta) {
    int cols = meta.size();
    densecols = BitsUtil.zero(cols);
    for(int i = 0; i < cols; i++) {
      if(TypeUtil.SPARSE_VECTOR_VARIABLE_LENGTH.isAssignableFromType(meta.get(i))) {
        throw new AbortException("Filtering sparse vectors is not yet supported by this filter. Please contribute.");
      }
      // TODO: only check for double and float?
      if(TypeUtil.NUMBER_VECTOR_VARIABLE_LENGTH.isAssignableFromType(meta.get(i))) {
        BitsUtil.setI(densecols, i);
        continue;
      }
      if(TypeUtil.DOUBLE_VECTOR_FIELD.isAssignableFromType(meta.get(i))) {
        BitsUtil.setI(densecols, i);
        continue;
      }
    }
  }

  @Override
  public MultipleObjectsBundle filter(final MultipleObjectsBundle objects) {
    if(LOG.isDebuggingFinest()) {
      LOG.debugFinest("Removing records with NaN values.");
    }

    updateMeta(objects.meta());
    MultipleObjectsBundle bundle = new MultipleObjectsBundle();
    for(int j = 0; j < objects.metaLength(); j++) {
      bundle.appendColumn(objects.meta(j), new ArrayList<>());
    }
    for(int i = 0; i < objects.dataLength(); i++) {
      final Object[] row = objects.getRow(i);
      boolean good = true;
      for(int j = BitsUtil.nextSetBit(densecols, 0); j >= 0; j = BitsUtil.nextSetBit(densecols, j + 1)) {
        NumberVector v = (NumberVector) row[j];
        if(v == null) {
          good = false;
          break;
        }
        for(int d = 0; d < v.getDimensionality(); d++) {
          if(Double.isNaN(v.doubleValue(d))) {
            good = false;
            break;
          }
        }
      }
      if(good) {
        bundle.appendSimple(row);
      }
    }
    return bundle;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected DropNaNFilter makeInstance() {
      return new DropNaNFilter();
    }
  }
}
