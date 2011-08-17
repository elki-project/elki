package experimentalcode.shared.algorithm.classifier;
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

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * A Classifier is to hold a model that is built based on a database, and to
 * classify a new instance of the same type.
 * 
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <L> the type of the ClassLabel the Classifier is assigning
 */
public interface Classifier<O, L extends ClassLabel> extends Algorithm, Parameterizable {
  /**
   * Performs the training. Sets available labels.
   * 
   * @param database the database to build the model on
   * @param classLabels the classes to be learned
   * @throws IllegalStateException if the classifier is not properly initiated
   *         (e.g. parameters are not set)
   */
  public void buildClassifier(Database database, ArrayList<L> classLabels) throws IllegalStateException;

  /**
   * Provides a classification for a given instance. The classification is the
   * index of the class-label.
   * 
   * @param instance an instance to classify
   * @return a classification for the given instance
   * @throws IllegalStateException if the Classifier has not been initialized or
   *         properly trained
   */
  public int classify(O instance) throws IllegalStateException;

  /**
   * Returns the class label for the given class index.
   * 
   * @param index the class index
   * @return the class label for the given index
   * @throws IllegalArgumentException if the given index is not valid
   */
  public L getClassLabel(int index) throws IllegalArgumentException;

  /**
   * Returns the distribution of class probabilities for the given instance. The
   * distribution is related to the class-labels.
   * 
   * @param instance an instance to define a class-probability-distribution for
   * @return a class-probability distribution for the given instance
   * @throws IllegalStateException if the Classifier has not been initialized or
   *         properly trained
   */
  public double[] classDistribution(O instance) throws IllegalStateException;

  /**
   * Provides a String representation of the classification model.
   * 
   * @return a String representation of the classification model
   */
  public String model();
}