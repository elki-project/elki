package de.lmu.ifi.dbs.elki.evaluation.scores.adapter;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.evaluation.scores.ScoreEvaluation.ScoreIter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.IntegerArrayQuickSort;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.IntegerComparator;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.ArrayIter;

/**
 * Class to iterate over a number vector in decreasing order.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf NumberVector
 */
public class IncreasingVectorIter implements ScoreIter, IntegerComparator, ArrayIter {
  /**
   * Order of dimensions.
   */
  private int[] sort;

  /**
   * Data vector.
   */
  private NumberVector vec;

  /**
   * Current position.
   */
  int pos = 0;

  /**
   * Constructor.
   * 
   * @param vec Vector to iterate over.
   */
  public IncreasingVectorIter(NumberVector vec) {
    this.vec = vec;
    final int dim = vec.getDimensionality();
    this.sort = new int[dim];
    for(int d = 0; d < dim; d++) {
      sort[d] = d;
    }
    IntegerArrayQuickSort.sort(sort, this);
  }

  @Override
  public int compare(int x, int y) {
    return Double.compare(vec.doubleValue(x), vec.doubleValue(y));
  }

  @Override
  public double score() {
    return vec.doubleValue(sort[pos]);
  }

  public int dim() {
    return sort[pos];
  }

  @Override
  public boolean valid() {
    return pos < vec.getDimensionality();
  }

  @Override
  public IncreasingVectorIter advance() {
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
  public IncreasingVectorIter advance(int count) {
    pos += count;
    return this;
  }

  @Override
  public IncreasingVectorIter retract() {
    pos--;
    return this;
  }

  @Override
  public IncreasingVectorIter seek(int off) {
    pos = off;
    return this;
  }
}