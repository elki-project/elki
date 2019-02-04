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
package de.lmu.ifi.dbs.elki.math.spacefillingcurves;

import static de.lmu.ifi.dbs.elki.math.spacefillingcurves.ZCurveSpatialSorterTest.assertSameOrder;
import static de.lmu.ifi.dbs.elki.math.spacefillingcurves.ZCurveSpatialSorterTest.shuffle;

import java.util.ArrayList;
import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.math.spacefillingcurves.ZCurveSpatialSorterTest.V;

/**
 * Class to unit test the k-d-tree like spatial sorter.
 * 
 * Understanding this on discrete data is tricky, because of ties. It is
 * intended for continuous data.
 * 
 * In each recursion, the median object is selected as pivot. This point is now
 * fixed, and both halves are processed recursively, with the next dimension.
 * So first, the median on X is found, in the basic test below this is the
 * record with x=2. For the upper half, the next pivot is the median in y, i.e.
 * y=1, for the lower half y=2. <b>Ties are broken pseudo-randomly, because the
 * QuickSelec is based on swapping points to find the median, and may not
 * preserve order</b>. It would be easier to understand on toy data if we would
 * use a stable sort, but also more expensive.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class BinarySplitSpatialSorterTest {
  @Test
  public void testBasic() {
    ArrayList<V> vs = new ArrayList<>();
    vs.add(new V(0, 0, 0));
    vs.add(new V(0, 0, 1));
    vs.add(new V(1, 1, 1)); // Second pivot
    vs.add(new V(1, 2, 0));
    vs.add(new V(0, 2, 2));
    vs.add(new V(2, 0, 1)); // First pivot
    vs.add(new V(4, 0, 1));
    vs.add(new V(3, 1, 2));
    vs.add(new V(4, 2, 0)); // Second pivot
    vs.add(new V(4, 3, 2));
    ArrayList<V> x = shuffle(vs, new Random(0L));
    BinarySplitSpatialSorter.STATIC.sort(x);
    assertSameOrder("Binary split order incorrect", vs, x);
  }
}
