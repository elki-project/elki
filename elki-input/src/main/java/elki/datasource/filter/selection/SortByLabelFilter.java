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
package elki.datasource.filter.selection;

import java.util.ArrayList;
import java.util.List;

import elki.datasource.bundle.MultipleObjectsBundle;
import elki.datasource.filter.FilterUtil;
import elki.datasource.filter.ObjectFilter;
import elki.logging.Logging;
import elki.utilities.datastructures.arrays.IntegerArrayQuickSort;
import elki.utilities.exceptions.AbortException;

/**
 * A filter to sort the data set by some label. The filter sorts the
 * labels in alphabetical order.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @navassoc - reads -elki.data.LabelList
 */
public class SortByLabelFilter implements ObjectFilter {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(SortByLabelFilter.class);

  /**
   * Constructor.
   */
  public SortByLabelFilter() {
    super();
  }

  @Override
  public MultipleObjectsBundle filter(final MultipleObjectsBundle objects) {
    if(LOG.isDebugging()) {
      LOG.debug("Sorting the data set");
    }

    // Prepare a reposition array for cheap resorting
    final int size = objects.dataLength();
    final int[] offsets = new int[size];
    for(int i = 0; i < size; i++) {
      offsets[i] = i;
    }
    // Sort by labels - identify a label column
    final int lblcol = FilterUtil.findLabelColumn(objects);
    if(lblcol == -1) {
      throw new AbortException("No label column found - cannot sort by label.");
    }
    IntegerArrayQuickSort.sort(offsets, (o1, o2) -> objects.data(o1, lblcol).toString().compareToIgnoreCase(objects.data(o2, lblcol).toString()));

    MultipleObjectsBundle bundle = new MultipleObjectsBundle();
    for(int j = 0; j < objects.metaLength(); j++) {
      // Reorder column accordingly
      List<?> in = objects.getColumn(j);
      List<Object> data = new ArrayList<>(size);
      for(int i = 0; i < size; i++) {
        data.add(in.get(offsets[i]));
      }
      bundle.appendColumn(objects.meta(j), data);
    }
    return bundle;
  }
}
