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
 * Wrapper to hold a pair of training and test data sets. The labels of both
 * training and test set are provided in labels.
 * 
 * @author Arthur Zimek
 * @since 0.7.0
 */
public class TrainingAndTestSet {
  /**
   * The overall labels.
   */
  private ArrayList<ClassLabel> labels;

  /**
   * The training data.
   */
  private MultipleObjectsBundle training;

  /**
   * The test data.
   */
  private MultipleObjectsBundle test;

  /**
   * Provides a pair of training and test data sets out of the given two
   * databases.
   */
  public TrainingAndTestSet(MultipleObjectsBundle training, MultipleObjectsBundle test, ArrayList<ClassLabel> labels) {
    this.training = training;
    this.test = test;
    this.labels = labels;
  }

  /**
   * Returns the test data set.
   * 
   * @return the test data set
   */
  public MultipleObjectsBundle getTest() {
    return test;
  }

  /**
   * Returns the training data set.
   * 
   * @return the training data set
   */
  public MultipleObjectsBundle getTraining() {
    return training;
  }

  /**
   * Returns all labels present in the data set.
   * 
   * @return all labels
   */
  public ArrayList<ClassLabel> getLabels() {
    return labels;
  }
}
