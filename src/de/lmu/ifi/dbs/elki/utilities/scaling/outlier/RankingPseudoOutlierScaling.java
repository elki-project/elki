package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;
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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.result.outlier.InvertedOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * This is a pseudo outlier scoring obtained by only considering the ranks of
 * the objects. However, the ranks are not mapped linearly to scores, but using
 * a normal distribution.
 * 
 * @author Erich Schubert
 */
public class RankingPseudoOutlierScaling implements OutlierScalingFunction {
  /**
   * The actual scores
   */
  private double[] scores;

  /**
   * Use inverted ranking
   */
  private boolean inverted = false;

  @Override
  public void prepare(OutlierResult or) {
    // collect all outlier scores
    scores = new double[or.getScores().size()];
    int pos = 0;
    if(or.getOutlierMeta() instanceof InvertedOutlierScoreMeta) {
      inverted = true;
    }
    for(DBID id : or.getScores().iterDBIDs()) {
      scores[pos] = or.getScores().get(id);
      pos++;
    }
    if(pos != or.getScores().size()) {
      throw new AbortException("Database size is incorrect!");
    }
    // sort them
    // TODO: Inverted scores!
    Arrays.sort(scores);
  }

  @Override
  public double getMax() {
    return 1.0;
  }

  @Override
  public double getMin() {
    return 0.0;
  }

  @Override
  public double getScaled(double value) {
    assert (scores != null) : "prepare() was not run prior to using the scaling function.";
    int pos = Arrays.binarySearch(scores, value);
    if(inverted) {
      return 1.0 - ((double) pos) / scores.length;
    }
    else {
      return ((double) pos) / scores.length;
    }
  }
}