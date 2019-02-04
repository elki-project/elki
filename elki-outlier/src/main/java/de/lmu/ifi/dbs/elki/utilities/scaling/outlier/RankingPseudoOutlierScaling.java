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
package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.result.outlier.InvertedOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * This is a pseudo outlier scoring obtained by only considering the ranks of
 * the objects. However, the ranks are not mapped linearly to scores, but using
 * a normal distribution.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public class RankingPseudoOutlierScaling implements OutlierScaling {
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
    DoubleRelation oscores = or.getScores();
    scores = new double[oscores.size()];
    if(or.getOutlierMeta() instanceof InvertedOutlierScoreMeta) {
      inverted = true;
    }
    int pos = 0;
    for(DBIDIter iditer = oscores.iterDBIDs(); iditer.valid(); iditer.advance()) {
      scores[pos++] = oscores.doubleValue(iditer);
    }
    if(pos != oscores.size()) {
      throw new AbortException("Database size is incorrect!");
    }
    // sort them
    Arrays.sort(scores);
  }

  @Override
  public <A> void prepare(A array, NumberArrayAdapter<?, A> adapter) {
    Arrays.sort(scores = ArrayLikeUtil.toPrimitiveDoubleArray(array, adapter));
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
    final int pos = Arrays.binarySearch(scores, value);
    int first = pos, last = pos;
    // Check for ties:
    while(first > 0 && scores[first - 1] == value) {
      --first;
    }
    while(last + 1 < scores.length && scores[last + 1] == value) {
      ++last;
    }
    double v = (first + last) * .5 / (scores.length - 1.);
    return inverted ? 1.0 - v : v;
  }
}