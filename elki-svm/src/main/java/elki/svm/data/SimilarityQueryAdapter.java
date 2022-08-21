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
package elki.svm.data;

import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.query.similarity.SimilarityQuery;

/**
 * Use a relation as data set
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class SimilarityQueryAdapter implements DataSet {
  /**
   * Similarity query
   */
  SimilarityQuery<?> sim;

  /**
   * Object ids to process.
   */
  ArrayModifiableDBIDs ids;

  /**
   * Iterators to offset into the ids.
   */
  DBIDArrayIter i1, i2;

  public SimilarityQueryAdapter(SimilarityQuery<?> sim, DBIDs ids) {
    this.sim = sim;
    this.ids = DBIDUtil.newArray(ids);
    this.i1 = this.ids.iter();
    this.i2 = this.ids.iter();
  }

  @Override
  public int size() {
    return ids.size();
  }

  @Override
  public double similarity(int i, int j) {
    return sim.similarity(i1.seek(i), i2.seek(j));
  }

  @Override
  public double value(int i) {
    return 1;
  }

  @Override
  public int classnum(int i) {
    return 0;
  }

  @Override
  public void swap(int i, int j) {
    ids.swap(i, j);
  }

  public DBIDArrayIter iter() {
    return ids.iter();
  }
}
