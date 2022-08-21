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

import java.util.Random;

import org.junit.Test;

import elki.algorithm.AbstractSimpleAlgorithmTest;
import elki.clustering.trivial.ByLabelClustering;
import elki.data.Clustering;
import elki.data.model.Model;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.ids.ArrayDBIDs;
import elki.database.ids.DBIDUtil;
import elki.evaluation.clustering.EvaluateClustering.ScoreResult;
import elki.result.EvaluationResult.Measurement;
import elki.result.Metadata;
import elki.utilities.datastructures.iterator.It;

/**
 * Test the class that automatically evaluates a clustering.
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class EvaluateClusteringTest {
  // the following values depend on the data set used!
  String dataset = "elki/testdata/unittests/hierarchical-3d2d1d.csv";

  // size of the data set
  int shoulds = 600;

  @Test
  public void testAutomatic() {
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(dataset, shoulds);
    ArrayDBIDs ids = DBIDUtil.assertRange(db.getRelation(TypeUtil.GUESSED_LABEL).getDBIDs());

    Random rnd = new Random(0L);
    int[] labels = new int[ids.size()];
    for(int i = 0; i < labels.length; i++) {
      labels[i] = rnd.nextInt(2);
    }
    Clustering<Model> a = AbstractClusterEvaluationTest.makeClustering(ids.iter(), labels);
    Metadata.hierarchyOf(db).addChild(a);

    new EvaluateClustering(new ByLabelClustering(), true, false).processNewResult(a);

    It<ScoreResult> it = Metadata.hierarchyOf(a).iterChildren().filter(ScoreResult.class);
    assertTrue("No evaluation result", it.valid());
    assertTrue("Not a score result", it.get() instanceof ScoreResult);
    ScoreResult r = it.get();
    it.advance();
    assertFalse("More than one evaluation result?", it.valid());

    Measurement m = r.findOrCreateGroup("Pair counting").getMeasure("F1-Measure");
    assertNotNull("No pair counting F1", m);
    assertEquals("F1 does not match", 0.3982704265, m.getVal(), 1e-8);
    m = r.findOrCreateGroup("Pair counting").getMeasure("ARI");
    assertNotNull("No pair counting ARI", m);
    assertEquals("ARI does not match", 0, m.getVal(), 1e-2);
  }
}
