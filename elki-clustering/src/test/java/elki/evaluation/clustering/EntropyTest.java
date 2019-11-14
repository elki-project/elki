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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.junit.Test;

import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.Model;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDRange;
import elki.database.ids.DBIDUtil;
import elki.database.ids.ModifiableDBIDs;

/**
 * Test entropy-based measures.
 * 
 * @author Erich Schubert
 */
public class EntropyTest {
  @Test
  public void testIdentical() {
    int[] a = { 0, 0, 1, 1, 2, 2 };
    int[] b = { 2, 2, 1, 1, 0, 0 };
    DBIDRange ids = DBIDUtil.generateStaticDBIDRange(a.length);
    Entropy e = new ClusterContingencyTable(false, false, makeClustering(ids.iter(), a), makeClustering(ids.iter(), b)).getEntropy();
    assertEquals("MI not as expected", e.upperBoundMI(), e.mutualInformation(), 1e-15);
    assertEquals("Joint NMI not as expected", 1, e.jointNMI(), 1e-15);
    assertEquals("minNMI not as expected", 1, e.minNMI(), 1e-15);
    assertEquals("maxNMI not as expected", 1, e.maxNMI(), 1e-15);
    assertEquals("Arithmetic NMI not as expected", 1, e.arithmeticNMI(), 1e-15);
    assertEquals("Geometric NMI not as expected", 1, e.geometricNMI(), 1e-15);
    assertEquals("EMI not as expected", 0.5441, e.expectedMutualInformation(), 1e-5);
    assertEquals("AMI not as expected", 1, e.adjustedMaxMI(), 1e-15);
  }

  @Test
  public void testSklearn() {
    // From sklearn unit test
    int[] a = { 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3 };
    int[] b = { 1, 1, 1, 1, 2, 1, 2, 2, 2, 2, 3, 1, 3, 3, 3, 2, 2 };
    DBIDRange ids = DBIDUtil.generateStaticDBIDRange(a.length);
    Entropy e = new ClusterContingencyTable(false, false, makeClustering(ids.iter(), a), makeClustering(ids.iter(), b)).getEntropy();
    assertEquals("MI not as expected", 0.41022, e.mutualInformation(), 1e-5);
    assertEquals("EMI not as expected", 0.15042, e.expectedMutualInformation(), 1e-5);
    assertEquals("AMI not as expected", 0.27821, e.adjustedArithmeticMI(), 1e-5);
  }

  /**
   * Helper, to generate a clustering from int[]
   *
   * @param iter DBID Iterator
   * @param a cluster numbers
   * @return Clustering
   */
  public static Clustering<Model> makeClustering(DBIDArrayIter iter, int[] a) {
    ArrayList<ModifiableDBIDs> l = new ArrayList<>();
    for(int i = 0; i < a.length; i++) {
      int j = a[i];
      while(j >= l.size()) {
        l.add(null);
      }
      ModifiableDBIDs cids = l.get(j);
      if(cids == null) {
        l.set(j, cids = DBIDUtil.newArray());
      }
      cids.add(iter.seek(i));
    }
    return new Clustering<>(l.stream() //
        .filter(x -> x != null) //
        .map(x -> new Cluster<>(x)) //
        .collect(Collectors.toList()));
  }
}
