package de.lmu.ifi.dbs.elki.evaluation.scores;

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

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.evaluation.scores.adapter.DBIDsTest;
import de.lmu.ifi.dbs.elki.evaluation.scores.adapter.DistanceResultAdapter;
import de.lmu.ifi.dbs.elki.evaluation.scores.adapter.OutlierScoreAdapter;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;

/**
 * Abstract base class for evaluating a scoring result.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public abstract class AbstractScoreEvaluation implements ScoreEvaluation {
  @Override
  public double evaluate(Cluster<?> clus, DoubleDBIDList nei) {
    return evaluate(new DBIDsTest(DBIDUtil.ensureSet(clus.getIDs())), new DistanceResultAdapter(nei.iter()));
  }

  @Override
  public double evaluate(DBIDs ids, DoubleDBIDList nei) {
    return evaluate(new DBIDsTest(DBIDUtil.ensureSet(ids)), new DistanceResultAdapter(nei.iter()));
  }

  @Override
  public double evaluate(DBIDs ids, OutlierResult outlier) {
    return evaluate(new DBIDsTest(DBIDUtil.ensureSet(ids)), new OutlierScoreAdapter(outlier));
  }
}
