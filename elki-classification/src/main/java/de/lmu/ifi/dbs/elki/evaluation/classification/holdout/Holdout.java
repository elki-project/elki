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

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;

/**
 * A holdout procedure is to provide a range of partitions of a database to
 * pairs of training and test data sets.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @assoc - - - MultipleObjectsBundle
 * @has - - - TrainingAndTestSet
 */
public interface Holdout {
  /**
   * Initialize the holdout procedure for a data set.
   *
   * @param bundle Data set bundle
   */
  void initialize(MultipleObjectsBundle bundle);

  /**
   * Get the next partitioning of the given holdout.
   *
   * @return Next partitioning of the data set
   */
  TrainingAndTestSet nextPartitioning();

  /**
   * Get the <i>sorted</i> class labels present in this data set.
   *
   * For indexing into assignment arrays.
   *
   * @return Class labels
   */
  ArrayList<ClassLabel> getLabels();

  /**
   * How many partitions to test.
   *
   * @return Number of partitions.
   */
  int numberOfPartitions();
}
