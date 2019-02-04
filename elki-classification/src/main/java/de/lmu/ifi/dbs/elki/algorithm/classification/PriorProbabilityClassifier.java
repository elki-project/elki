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

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * Classifier to classify instances based on the prior probability of classes in
 * the database, without using the actual data values.
 *
 * @author Arthur Zimek
 * @since 0.7.0
 */
@Title("Prior Probability Classifier")
@Description("Classifier to predict simply prior probabilities for all classes as defined by their relative abundance in a given database.")
public class PriorProbabilityClassifier extends AbstractClassifier<Object, Result> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(PriorProbabilityClassifier.class);

  /**
   * Holds the prior probabilities.
   */
  protected double[] distribution;

  /**
   * Index of the most abundant class.
   */
  protected ClassLabel prediction;

  /**
   * Class labels seen.
   */
  protected ArrayList<ClassLabel> labels;

  /**
   * Constructor.
   */
  public PriorProbabilityClassifier() {
    super();
  }

  /**
   * Learns the prior probability for all classes.
   */
  @Override
  public void buildClassifier(Database database, Relation<? extends ClassLabel> labelrep) {
    Object2IntOpenHashMap<ClassLabel> count = new Object2IntOpenHashMap<>();
    for(DBIDIter iter = labelrep.iterDBIDs(); iter.valid(); iter.advance()) {
      count.addTo(labelrep.get(iter), 1);
    }
    int max = Integer.MIN_VALUE;
    double size = labelrep.size();

    distribution = new double[count.size()];
    labels = new ArrayList<>(count.size());
    ObjectIterator<Entry<ClassLabel>> iter = count.object2IntEntrySet().fastIterator();
    for(int i = 0; iter.hasNext(); ++i) {
      Entry<ClassLabel> entry = iter.next();
      distribution[i] = entry.getIntValue() / size;
      labels.add(entry.getKey());
      if(entry.getIntValue() > max) {
        max = entry.getIntValue();
        prediction = entry.getKey();
      }
    }
  }

  public double[] classProbabilities(Object instance, ArrayList<ClassLabel> labels) {
    return alignLabels(this.labels, distribution, labels);
  }

  @Override
  public ClassLabel classify(Object instance) {
    return prediction;
  }

  @Override
  public String model() {
    StringBuilder output = new StringBuilder();
    for(int i = 0; i < distribution.length; i++) {
      output.append(labels.get(i));
      output.append(" : ");
      output.append(distribution[i]);
      output.append('\n');
    }
    return output.toString();
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.ANY);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}