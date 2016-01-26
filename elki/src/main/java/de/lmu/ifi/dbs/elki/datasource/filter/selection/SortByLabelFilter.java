package de.lmu.ifi.dbs.elki.datasource.filter.selection;

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

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.FilterUtil;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.IntegerArrayQuickSort;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.IntegerComparator;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * A filter to sort the data set by some label.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.data.LabelList oneway - - «reads»
 */
@Alias("de.lmu.ifi.dbs.elki.datasource.filter.SortByLabelFilter")
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
      LOG.debug("Shuffling the data set");
    }

    // Prepare a reposition array for cheap resorting
    final int size = objects.dataLength();
    final int[] offsets = new int[size];
    for(int i = 0; i < size; i++) {
      offsets[i] = i;
    }
    // Sort by labels - identify a label column
    final int lblcol = FilterUtil.findLabelColumn(objects);
    if (lblcol == -1) {
      throw new AbortException("No label column found - cannot sort by label.");
    }
    IntegerArrayQuickSort.sort(offsets, new IntegerComparator() {
      @Override
      public int compare(int o1, int o2) {
        String l1 = objects.data(o1, lblcol).toString();
        String l2 = objects.data(o2, lblcol).toString();
        return l1.compareToIgnoreCase(l2);
      }
    });

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
