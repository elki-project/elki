/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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

import elki.algorithm.AbstractSimpleAlgorithmTest;
import elki.clustering.trivial.ByLabelClustering;
import elki.clustering.trivial.TrivialAllInOne;
import elki.clustering.trivial.TrivialAllNoise;
import elki.data.Clustering;
import elki.data.model.Model;
import elki.database.Database;
import elki.database.ids.DBIDRange;
import elki.database.ids.DBIDUtil;

/**
 * Test {@link PairCounting} with sklearn example and f1-measure with database
 *
 * @author Erich Schubert
 * @author Robert Gehde
 * @since 0.8.0
 */
public class PairCountingTest extends AbstractClusterEvaluationTest {
  // the following values depend on the data set used!
  String dataset = "elki/testdata/unittests/hierarchical-3d2d1d.csv";

  // size of the data set
  int shoulds = 600;

  /**
   * Test all calculated values with the SKLearn example
   */
  @Test
  public void testPairCountingSKlearn() {
    DBIDRange ids = DBIDUtil.generateStaticDBIDRange(SKLEARNA.length);
    PairCounting pc = new ClusterContingencyTable(false, false, makeClustering(ids.iter(), SKLEARNA), makeClustering(ids.iter(), SKLEARNB)).getPaircount();

    assertEquals("Precision not as expected", 0.476190476190476, pc.precision(), 1e-15);
    assertEquals("Recall not as expected", 0.5, pc.recall(), 0);
    assertEquals("Rand-Index not as expected", 0.691176470588235, pc.randIndex(), 1e-15);
    assertEquals("Fowlkes-Mallows not as expected", 0.487950036474267, pc.fowlkesMallows(), 1e-15);
    assertEquals("Jaccard not as expected", 0.32258064516129, pc.jaccard(), 1e-15);
    assertEquals("F1-Measure not as expected", 0.487804878048781, pc.f1Measure(), 1e-15);
    assertEquals("F_5-Measure not as expected", 0.499040307101727, pc.fMeasure(5), 1e-15);
    assertEquals("Adjusted Rand-Index not as expected", 0.26694045174538, pc.adjustedRandIndex(), 1e-15);
    assertEquals("Mirkin-Index not as expected", 84, pc.mirkin());
  }

  /**
   * Test F1-measure with the database example
   */
  @Test
  public void compareDatabases() {
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(dataset, shoulds);

    Clustering<Model> rai = new TrivialAllInOne().autorun(db);
    Clustering<Model> ran = new TrivialAllNoise().autorun(db);
    Clustering<?> rbl = new ByLabelClustering().autorun(db);

    assertEquals(1.0, computeFMeasure(rai, rai, false), 0);
    assertEquals(1.0, computeFMeasure(ran, ran, false), 0);
    assertEquals(1.0, computeFMeasure(rbl, rbl, false), 0);

    assertEquals(0.009950248756218905, computeFMeasure(ran, rbl, true), 0);
    assertEquals(0.0033277870216306157, computeFMeasure(rai, ran, true), 0);

    assertEquals(0.5 /* 0.3834296724470135 */, computeFMeasure(rai, rbl, false), 0);
  }

  /**
   * Calculate contingency table and get f1-measure
   *
   * @param c1 first clustering
   * @param c2 second clustering
   * @param noise true, if clustering should have noise
   * @return f1-measure between the clusterings
   */
  private double computeFMeasure(Clustering<?> c1, Clustering<?> c2, boolean noise) {
    return new ClusterContingencyTable(true, noise, c1, c2).getPaircount().f1Measure();
  }
}
