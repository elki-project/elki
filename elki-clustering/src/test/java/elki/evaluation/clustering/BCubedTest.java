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
package elki.evaluation.clustering;

import static org.junit.Assert.*;

import org.junit.Test;

import elki.database.ids.DBIDRange;
import elki.database.ids.DBIDUtil;

/**
 * Validate {@link BCubed} based measures with the SkLearn example
 * 
 * @author Robert Gehde
 *
 */
public class BCubedTest {

  @Test
  public void testBCubed() {
    int[] a = { 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3 };
    int[] b = { 1, 1, 1, 1, 2, 1, 2, 2, 2, 2, 3, 1, 3, 3, 3, 2, 2 };
    DBIDRange ids = DBIDUtil.generateStaticDBIDRange(a.length);
    BCubed bc = new BCubed(new ClusterContingencyTable(true, false, EntropyTest.makeClustering(ids.iter(), a), EntropyTest.makeClustering(ids.iter(), b)));

    assertEquals("BCubed precision not as expected", 0.57843137254902, bc.precision(), 1e-15);
    assertEquals("BCubed Recall not as expected", 0.584313725490196, bc.recall(), 1e-15);
  }
}
