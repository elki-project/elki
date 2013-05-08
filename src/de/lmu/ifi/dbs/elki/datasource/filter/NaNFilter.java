package de.lmu.ifi.dbs.elki.datasource.filter;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import java.util.ArrayList;
import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * A filter to drop all records that contain NaN values.
 * 
 * Note: currently, only dense vector columns are supported.
 * 
 * TODO: add support for sparse vectors.
 * 
 * @author Erich Schubert
 */
public class NaNFilter extends AbstractStreamFilter {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(NaNFilter.class);

  /**
   * Columns to check.
   */
  private BitSet densecols = new BitSet();

  /**
   * Constructor.
   */
  public NaNFilter() {
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
    while (true) {
      Event ev = source.nextEvent();
      switch(ev) {
      case END_OF_STREAM:
        return ev;
      case META_CHANGED:
        final BundleMeta meta = source.getMeta();
        int cols = meta.size();
        densecols.clear();
        for (int i = 0; i < cols; i++) {
          if (TypeUtil.SPARSE_VECTOR_VARIABLE_LENGTH.isAssignableFromType(meta.get(i))) {
            throw new AbortException("Filtering sparse vectors is not yet supported by this filter. Please contribute.");
          }
          // TODO: only check for double and float?
          if (TypeUtil.NUMBER_VECTOR_VARIABLE_LENGTH.isAssignableFromType(meta.get(i))) {
            LOG.verbose("Filtering column " + i);
            densecols.set(i);
            continue;
          }
          if (TypeUtil.DOUBLE_VECTOR_FIELD.isAssignableFromType(meta.get(i))) {
            LOG.verbose("Filtering column " + i);
            densecols.set(i);
            continue;
          }
          LOG.verbose("Not filtering column: " + i + " " + meta.get(i));
        }
        return ev;
      case NEXT_OBJECT:
        boolean good = true;
        for (int j = densecols.nextSetBit(0); j >= 0; j = densecols.nextSetBit(j + 1)) {
          NumberVector<?> v = (NumberVector<?>) source.data(j);
          if (v == null) {
            good = false;
            break;
          }
          for (int i = 0; i < v.getDimensionality(); i++) {
            if (Double.isNaN(v.doubleValue(i))) {
              good = false;
              break;
            }
          }
        }
        if (good) {
          return ev;
        }
        continue;
      }
    }
  }

  @Override
  public MultipleObjectsBundle filter(final MultipleObjectsBundle objects) {
    if (LOG.isDebugging()) {
      LOG.debug("Filtering the data set");
    }

    MultipleObjectsBundle bundle = new MultipleObjectsBundle();
    for (int j = 0; j < objects.metaLength(); j++) {
      bundle.appendColumn(objects.meta(j), new ArrayList<>());
    }
    for (int i = 0; i < objects.dataLength(); i++) {
      boolean good = true;
      for (int j = 0; j < objects.metaLength(); j++) {
        if (objects.data(i, j) == null) {
          good = false;
          break;
        }
      }
      if (good) {
        bundle.appendSimple(objects.getRow(i));
      }
    }
    return bundle;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected Object makeInstance() {
      return new NaNFilter();
    }
  }
}
