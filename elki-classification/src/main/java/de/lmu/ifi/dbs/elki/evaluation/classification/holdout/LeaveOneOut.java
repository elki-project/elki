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
package de.lmu.ifi.dbs.elki.evaluation.classification.holdout;

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;

/**
 * A leave-one-out-holdout is to provide a set of partitions of a database where
 * each instances once hold out as a test instance while the respectively
 * remaining instances are training instances.
 * 
 * @author Arthur Zimek
 * @since 0.7.0
 */
public class LeaveOneOut extends AbstractHoldout {
  /**
   * Size of the data set.
   */
  private int len, pos;

  /**
   * Constructor.
   */
  public LeaveOneOut() {
    super();
  }

  @Override
  public void initialize(MultipleObjectsBundle bundle) {
    super.initialize(bundle);
    len = bundle.dataLength();
    pos = 0;
  }

  @Override
  public int numberOfPartitions() {
    return len;
  }

  @Override
  public TrainingAndTestSet nextPartitioning() {
    if(pos >= len) {
      return null;
    }
    MultipleObjectsBundle training = new MultipleObjectsBundle();
    MultipleObjectsBundle test = new MultipleObjectsBundle();
    // Process column-wise.
    for(int c = 0, cs = bundle.metaLength(); c < cs; ++c) {
      ArrayList<Object> tr = new ArrayList<>(len - 1), te = new ArrayList<>(1);
      for(int i = 0; i < bundle.dataLength(); ++i) {
        ((i != pos) ? tr : te).add(bundle.data(i, c));
      }
      training.appendColumn(bundle.meta(c), tr);
      test.appendColumn(bundle.meta(c), te);
    }

    ++pos;
    return new TrainingAndTestSet(training, test, labels);
  }
}
