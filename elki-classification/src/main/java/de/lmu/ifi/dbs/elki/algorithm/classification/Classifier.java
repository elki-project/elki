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
package de.lmu.ifi.dbs.elki.algorithm.classification;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.relation.Relation;

/**
 * A Classifier is to hold a model that is built based on a database, and to
 * classify a new instance of the same type.
 * 
 * @author Arthur Zimek
 * @since 0.7.0
 *
 * @param <O> the type of objects handled by this algorithm
 */
public interface Classifier<O> extends Algorithm {
  /**
   * Performs the training. Sets available labels.
   * 
   * @param database the database to build the model on
   * @param classLabels the classes to be learned
   */
  void buildClassifier(Database database, Relation<? extends ClassLabel> classLabels);

  /**
   * Classify a single instance.
   * 
   * @param instance an instance to classify
   * @return predicted class label of the given instance
   */
  ClassLabel classify(O instance);

  /**
   * Produce a String representation of the classification model.
   * 
   * @return a String representation of the classification model
   */
  String model();
}