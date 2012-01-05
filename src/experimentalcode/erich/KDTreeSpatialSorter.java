package experimentalcode.erich;

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
import java.util.Comparator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.AbstractSpatialSorter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;

/**
 * Experimental spatial sorter, by ordering the elements for a k-d-tree bulk
 * load. This does <em>not</em> produce a k-d-tree. It is just an k-d-tree
 * inspired way of sorting the elements spatially.
 * 
 * @author Erich Schubert
 */
public class KDTreeSpatialSorter extends AbstractSpatialSorter {
  /**
   * Constructor.
   */
  public KDTreeSpatialSorter() {
    super();
  }

  @Override
  public <T extends SpatialComparable> void sort(List<T> objs, int start, int end, double[] minmax) {
    final int dims = objs.get(0).getDimensionality();
    kdSort(objs, start, end, 1, dims, new DimC());
  }

  private <T extends SpatialComparable> void kdSort(List<T> objs, final int start, final int end, int curdim, final int dims, DimC comp) {
    final int mid = start + ((end - start) >>> 1);
    // Make invariant
    comp.dim = curdim;
    QuickSelect.quickSelect(objs, comp, start, end, mid);
    // Recurse
    final int nextdim = (curdim % dims) + 1;
    if(start < mid - 1) {
      kdSort(objs, start, mid, nextdim, dims, comp);
    }
    if(mid + 2 < end) {
      kdSort(objs, mid + 1, end, nextdim, dims, comp);
    }
  }

  private static class DimC implements Comparator<SpatialComparable> {
    public int dim = -1;

    @Override
    public int compare(SpatialComparable o1, SpatialComparable o2) {
      double m1 = o1.getMax(dim) + o1.getMin(dim);
      double m2 = o2.getMax(dim) + o2.getMin(dim);
      return Double.compare(m1, m2);
    }
  }
}