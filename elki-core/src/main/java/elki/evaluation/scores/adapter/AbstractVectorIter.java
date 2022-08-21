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
package elki.evaluation.scores.adapter;

import elki.data.NumberVector;
import elki.evaluation.scores.ScoreEvaluation;
import elki.utilities.datastructures.iterator.ArrayIter;

/**
 * Class to iterate over a number vector in decreasing order.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @composed - - - NumberVector
 */
public abstract class AbstractVectorIter implements ScoreEvaluation.Adapter, ArrayIter {
  /**
   * Order of dimensions.
   */
  protected int[] sort;

  /**
   * Vector of positive examples.
   */
  protected NumberVector positive;

  /**
   * Data vector.
   */
  protected NumberVector vec;

  /**
   * Current position.
   */
  protected int pos = 0;

  /**
   * Number of positive examples in the vector.
   */
  protected int numPositive;

  /**
   * Constructor.
   * 
   * @param positive Positive examples
   * @param vec Vector to iterate over.
   */
  public AbstractVectorIter(NumberVector positive, NumberVector vec) {
    this.positive = positive;
    this.vec = vec;
    for(int i = 0, l = positive.getDimensionality(); i < l; i++) {
      if(positive.doubleValue(i) > 0) {
        ++numPositive;
      }
    }
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

  @Override
  public boolean test() {
    return positive.doubleValue(sort[pos]) > 0;
  }

  @Override
  public int numPositive() {
    return numPositive;
  }

  @Override
  public int numTotal() {
    return positive.getDimensionality();
  }
}
