package de.lmu.ifi.dbs.elki.evaluation;

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

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.evaluation.roc.ROC;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Test to validate ROC curve computation.
 * 
 * @author Erich Schubert
 * 
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

    ArrayList<Pair<Double, DBID>> distances = new ArrayList<Pair<Double, DBID>>();
    distances.add(new Pair<Double, DBID>(0.0, DBIDUtil.importInteger(1)));
    distances.add(new Pair<Double, DBID>(1.0, DBIDUtil.importInteger(2)));
    distances.add(new Pair<Double, DBID>(2.0, DBIDUtil.importInteger(6)));
    distances.add(new Pair<Double, DBID>(3.0, DBIDUtil.importInteger(7)));
    distances.add(new Pair<Double, DBID>(3.0, DBIDUtil.importInteger(3)));
    distances.add(new Pair<Double, DBID>(4.0, DBIDUtil.importInteger(8)));
    distances.add(new Pair<Double, DBID>(4.0, DBIDUtil.importInteger(4)));
    distances.add(new Pair<Double, DBID>(5.0, DBIDUtil.importInteger(9)));
    distances.add(new Pair<Double, DBID>(6.0, DBIDUtil.importInteger(5)));

    List<DoubleDoublePair> roccurve = ROC.materializeROC(9, positive, distances.iterator());
    // System.out.println(roccurve);
    Assert.assertEquals("ROC curve too complex", 6, roccurve.size());

    double auc = ROC.computeAUC(roccurve);
    Assert.assertEquals("ROC AUC not right.", 0.6, auc, 0.0001);
  }
}
