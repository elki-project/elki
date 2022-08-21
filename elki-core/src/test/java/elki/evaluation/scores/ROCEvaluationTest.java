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
package elki.evaluation.scores;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import elki.database.ids.DBIDUtil;
import elki.database.ids.HashSetModifiableDBIDs;
import elki.database.ids.ModifiableDoubleDBIDList;
import elki.evaluation.scores.ROCEvaluation.ROCurve;
import elki.evaluation.scores.adapter.DistanceResultAdapter;
import elki.math.geometry.XYCurve;

/**
 * Test to validate receiver operating curve computation.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class ROCEvaluationTest {
  @Test
  public void testROCurve() {
    HashSetModifiableDBIDs positive = DBIDUtil.newHashSet();
    positive.add(DBIDUtil.importInteger(1));
    positive.add(DBIDUtil.importInteger(2));
    positive.add(DBIDUtil.importInteger(3));
    positive.add(DBIDUtil.importInteger(4));
    positive.add(DBIDUtil.importInteger(5));

    final ModifiableDoubleDBIDList distances = DBIDUtil.newDistanceDBIDList();
    // Starting point: ................................ 0.0,0. ++
    distances.add(0.0, DBIDUtil.importInteger(1)); // + 0.0,.2 -- redundant
    distances.add(1.0, DBIDUtil.importInteger(2)); // + 0.0,.4 ++
    distances.add(2.0, DBIDUtil.importInteger(6)); // - .25,.4 ++
    distances.add(3.0, DBIDUtil.importInteger(7)); // -
    distances.add(3.0, DBIDUtil.importInteger(3)); // + .50,.6 -- redundant
    distances.add(4.0, DBIDUtil.importInteger(8)); // -
    distances.add(4.0, DBIDUtil.importInteger(4)); // + .75,.8 ++
    distances.add(5.0, DBIDUtil.importInteger(9)); // - 1.0,.8 ++
    distances.add(6.0, DBIDUtil.importInteger(5)); // + 1.0,1. ++

    ROCurve curve = ROCEvaluation.materializeROC(new DistanceResultAdapter(positive, distances.iter(), distances.size()));
    assertEquals("ROC curve too complex", 6, curve.size());
    assertEquals("AUROC (cached) not correct.", 0.6, curve.getAUC(), 1e-14);
    assertEquals("AUROC (direct) not correct.", 0.6, new ROCEvaluation().evaluate(positive, distances), 1e-14);
    assertEquals("AUROC (curve) not correct.", 0.6, XYCurve.areaUnderCurve(curve), 1e-14);
  }
}
