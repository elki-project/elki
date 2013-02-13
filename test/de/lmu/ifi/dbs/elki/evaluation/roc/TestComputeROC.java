package de.lmu.ifi.dbs.elki.evaluation.roc;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import junit.framework.Assert;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDPairList;
import de.lmu.ifi.dbs.elki.math.geometry.XYCurve;

/**
 * Test to validate ROC curve computation.
 * 
 * @author Erich Schubert
 */
public class TestComputeROC implements JUnit4Test {
  /**
   * Test ROC curve generation, including curve simplification
   */
  @Test
  public void testROCCurve() {
    HashSetModifiableDBIDs positive = DBIDUtil.newHashSet();
    positive.add(DBIDUtil.importInteger(1));
    positive.add(DBIDUtil.importInteger(2));
    positive.add(DBIDUtil.importInteger(3));
    positive.add(DBIDUtil.importInteger(4));
    positive.add(DBIDUtil.importInteger(5));

    final DoubleDistanceDBIDPairList distances = new DoubleDistanceDBIDPairList();
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

    XYCurve roccurve = ROC.materializeROC(new ROC.DBIDsTest(positive), new ROC.DistanceResultAdapter<>(distances.iter()));
    // System.err.println(roccurve);
    Assert.assertEquals("ROC curve too complex", 6, roccurve.size());

    double auc = XYCurve.areaUnderCurve(roccurve);
    Assert.assertEquals("ROC AUC not right.", 0.6, auc, 0.0001);
  }
}
