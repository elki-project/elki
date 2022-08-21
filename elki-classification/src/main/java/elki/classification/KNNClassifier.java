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
package elki.classification;

import java.util.ArrayList;
import java.util.Collections;

import elki.Algorithm;
import elki.data.ClassLabel;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.ids.DoubleDBIDListIter;
import elki.database.ids.KNNList;
import elki.database.query.QueryBuilder;
import elki.database.query.knn.KNNSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.utilities.Priority;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * KNNClassifier classifies instances based on the class distribution among the
 * k nearest neighbors in a database.
 *
 * @author Arthur Zimek
 * @since 0.7.0
 * @param <O> the type of objects handled by this algorithm
 */
@Title("kNN-classifier")
@Description("Lazy classifier classifies a given instance to the majority class of the k-nearest neighbors.")
@Priority(Priority.IMPORTANT)
public class KNNClassifier<O> implements Classifier<O> {
  /**
   * Holds the value of @link #K_PARAM}.
   */
  protected int k;

  /**
   * kNN query class.
   */
  protected KNNSearcher<O> knnq;

  /**
   * Class label representation.
   */
  protected Relation<? extends ClassLabel> labelrep;

  /**
   * Distance function
   */
  protected Distance<? super O> distance;

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param k Number of nearest neighbors to access.
   */
  public KNNClassifier(Distance<? super O> distance, int k) {
    super();
    this.distance = distance;
    this.k = k;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  public void buildClassifier(Database database, Relation<? extends ClassLabel> labels) {
    Relation<O> relation = database.getRelation(distance.getInputTypeRestriction());
    this.knnq = new QueryBuilder<>(relation, distance).kNNByObject(k);
    this.labelrep = labels;
  }

  @Override
  public ClassLabel classify(O instance) {
    Object2IntOpenHashMap<ClassLabel> count = new Object2IntOpenHashMap<>();
    KNNList query = knnq.getKNN(instance, k);
    for(DoubleDBIDListIter neighbor = query.iter(); neighbor.valid(); neighbor.advance()) {
      count.addTo(labelrep.get(neighbor), 1);
    }

    int bestoccur = Integer.MIN_VALUE;
    ClassLabel bestl = null;
    for(ObjectIterator<Entry<ClassLabel>> iter = count.object2IntEntrySet().fastIterator(); iter.hasNext();) {
      Entry<ClassLabel> entry = iter.next();
      if(entry.getIntValue() > bestoccur) {
        bestoccur = entry.getIntValue();
        bestl = entry.getKey();
      }
    }
    return bestl;
  }

  public double[] classProbabilities(O instance, ArrayList<ClassLabel> labels) {
    int[] occurences = new int[labels.size()];

    KNNList query = knnq.getKNN(instance, k);
    for(DoubleDBIDListIter neighbor = query.iter(); neighbor.valid(); neighbor.advance()) {
      int index = Collections.binarySearch(labels, labelrep.get(neighbor));
      if(index >= 0) {
        occurences[index]++;
      }
    }
    double[] distribution = new double[labels.size()];
    for(int i = 0; i < distribution.length; i++) {
      distribution[i] = ((double) occurences[i]) / (double) query.size();
    }
    return distribution;
  }

  @Override
  public String model() {
    return "lazy learner - provides no model";
  }

  /**
   * Returns the distance.
   *
   * @return the distance
   */
  public Distance<? super O> getDistance() {
    return distance;
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Par<O> implements Parameterizer {
    /**
     * Parameter to specify the number of neighbors to take into account for
     * classification, must be an integer greater than 0.
     */
    public static final OptionID K_ID = new OptionID("knnclassifier.k", "The number of neighbors to take into account for classification.");

    /**
     * Distance function
     */
    protected Distance<? super O> distanceFunction;

    /**
     * Holds the value of @link #K_PARAM}.
     */
    protected int k;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distanceFunction = x);
      new IntParameter(K_ID, 1)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
    }

    @Override
    public KNNClassifier<O> make() {
      return new KNNClassifier<>(distanceFunction, k);
    }
  }
}
