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
package de.lmu.ifi.dbs.elki.evaluation.scores.adapter;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.evaluation.scores.ScoreEvaluation.ScoreIter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.ArrayIter;

/**
 * Class to iterate over a number vector in decreasing order.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @composed - - - NumberVector
 */
public abstract class AbstractVectorIter implements ScoreIter, ArrayIter {
  /**
   * Order of dimensions.
   */
  protected int[] sort;

  /**
   * Data vector.
   */
  protected NumberVector vec;

  /**
   * Current position.
   */
  int pos = 0;

  /**
   * Constructor.
   * 
   * @param vec Vector to iterate over.
   */
  public AbstractVectorIter(NumberVector vec) {
    this.vec = vec;
  }

  /**
   * Get the dimension in the <i>original</i> vector.
   * 
   * @return Vector position.
   */
  public int dim() {
    return sort[pos];
  }

  @Override
  public boolean valid() {
    return pos < vec.getDimensionality() && pos >= 0;
  }

  @Override
  public AbstractVectorIter advance() {
    ++pos;
    return this;
  }

  @Override
  public boolean tiedToPrevious() {
    return pos > 0 && Double.compare(vec.doubleValue(sort[pos]), vec.doubleValue(sort[pos - 1])) == 0;
  }

  @Override
  public int getOffset() {
    return pos;
  }

  @Override
  public AbstractVectorIter advance(int count) {
    pos += count;
    return this;
  }

  @Override
  public AbstractVectorIter retract() {
    pos--;
    return this;
  }

  @Override
  public AbstractVectorIter seek(int off) {
    pos = off;
    return this;
  }
}