package experimentalcode.shared.algorithm.classifier;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2012
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
import java.util.Collections;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Classifier to classify instances based on the prior probability of classes in
 * the database.
 * 
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <L> the type of the ClassLabel the Classifier is assigning
 */
@Title("Prior Probability Classifier")
@Description("Classifier to predict simply prior probabilities for all classes as defined by their relative abundance in a given database.")
public class PriorProbabilityClassifier<O, L extends ClassLabel> extends AbstractClassifier<O, L, Result> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(PriorProbabilityClassifier.class);
  
  /**
   * Holds the prior probabilities.
   */
  protected double[] distribution;

  /**
   * Index of the most abundant class.
   */
  protected int prediction;

  /**
   * Holds the database the prior probabilities are based on.
   */
  protected Database database;

  /**
   * Provides a classifier always predicting the prior probabilities.
   */
  public PriorProbabilityClassifier(Parameterization config) {
    super();
    config = config.descend(this);
  }

  /**
   * Learns the prior probability for all classes.
   */
  @Override
  public void buildClassifier(Database database, ArrayList<L> classLabels) throws IllegalStateException {
    this.setLabels(classLabels);
    this.database = database;
    distribution = new double[getLabels().size()];
    int[] occurences = new int[getLabels().size()];
    Relation<ClassLabel> crep = database.getRelation(TypeUtil.CLASSLABEL);
    for(DBIDIter iter = crep.iterDBIDs(); iter.valid(); iter.advance()) {
      ClassLabel label = crep.get(iter.getDBID());
      int index = Collections.binarySearch(getLabels(), label);
      if(index > -1) {
        occurences[index]++;
      }
      else {
        throw new IllegalStateException(ExceptionMessages.INCONSISTENT_STATE_NEW_LABEL + ": " + label);
      }
    }
    double size = crep.size();
    for(int i = 0; i < distribution.length; i++) {
      distribution[i] = occurences[i] / size;
    }
    prediction = ArrayLikeUtil.getIndexOfMaximum(distribution);
  }

  /**
   * Returns the index of the most abundant class. According to the prior class
   * probability distribution, this is the index of the class showing maximum
   * prior probability.
   * 
   * @param instance unused
   */
  @Override
  public int classify(O instance) throws IllegalStateException {
    return prediction;
  }

  /**
   * Returns the distribution of the classes' prior probabilities.
   * 
   * @param instance unused
   */
  @Override
  public double[] classDistribution(O instance) throws IllegalStateException {
    return distribution;
  }

  @Override
  public String model() {
    StringBuffer output = new StringBuffer();
    for(int i = 0; i < distribution.length; i++) {
      output.append(getLabels().get(i));
      output.append(" : ");
      output.append(distribution[i]);
      output.append('\n');
    }
    return output.toString();
  }

  @Override
  public Result run(@SuppressWarnings("unused") Database database) throws IllegalStateException {
    // TODO Implement sensible default behavior.
    return null;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }
  
  @Override
  protected Logging getLogger() {
    return logger;
  }
}