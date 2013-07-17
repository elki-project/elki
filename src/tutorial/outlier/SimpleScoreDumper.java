package tutorial.outlier;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;

/**
 * Simple example output handler for processing outlier scores.
 * 
 * @author Erich Schubert
 */
public class SimpleScoreDumper implements ResultHandler {
  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    // Get all new outlier results
    ArrayList<OutlierResult> ors = ResultUtil.filterResults(newResult, OutlierResult.class);
    for (OutlierResult o : ors) {
      Relation<Double> scores = o.getScores();
      for (DBIDIter iter = scores.iterDBIDs(); iter.valid(); iter.advance()) {
        System.out.println(DBIDUtil.toString(iter) + " " + scores.get(iter));
      }
    }
  }
}
