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
package de.lmu.ifi.dbs.elki.datasource.filter.cleaning;

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractStreamFilter;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * A filter to remove entries that have missing values.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
@Alias({ "de.lmu.ifi.dbs.elki.datasource.filter.FilterNoMissingValuesFilter", //
"de.lmu.ifi.dbs.elki.datasource.filter.NoMissingValuesFilter" })
public class NoMissingValuesFilter extends AbstractStreamFilter {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(NoMissingValuesFilter.class);

  /**
   * Number of columns
   */
  private int cols = 0;

  /**
   * Constructor.
   */
  public NoMissingValuesFilter() {
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
        cols = source.getMeta().size();
        return ev;
      case NEXT_OBJECT:
        boolean good = true;
        for(int j = 0; j < cols; j++) {
          if(source.data(j) == null) {
            good = false;
            break;
          }
        }
        if(good) {
          return ev;
        }
        continue;
      }
    }
  }

  @Override
  public MultipleObjectsBundle filter(final MultipleObjectsBundle objects) {
    if(LOG.isDebugging()) {
      LOG.debug("Filtering the data set");
    }

    MultipleObjectsBundle bundle = new MultipleObjectsBundle();
    for(int j = 0; j < objects.metaLength(); j++) {
      bundle.appendColumn(objects.meta(j), new ArrayList<>());
    }
    for(int i = 0; i < objects.dataLength(); i++) {
      boolean good = true;
      for(int j = 0; j < objects.metaLength(); j++) {
        if(objects.data(i, j) == null) {
          good = false;
          break;
        }
      }
      if(good) {
        bundle.appendSimple(objects.getRow(i));
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
    protected NoMissingValuesFilter makeInstance() {
      return new NoMissingValuesFilter();
    }
  }
}
