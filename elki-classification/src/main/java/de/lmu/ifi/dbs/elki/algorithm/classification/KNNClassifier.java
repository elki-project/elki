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
import java.util.Collections;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

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
public class KNNClassifier<O> extends AbstractAlgorithm<Result> implements DistanceBasedAlgorithm<O>, Classifier<O> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KNNClassifier.class);

  /**
   * Holds the value of @link #K_PARAM}.
   */
  protected int k;

  /**
   * kNN query class.
   */
  protected KNNQuery<O> knnq;

  /**
   * Class label representation.
   */
  protected Relation<? extends ClassLabel> labelrep;

  /**
   * Distance function
   */
  protected DistanceFunction<? super O> distanceFunction;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param k Number of nearest neighbors to access.
   */
  public KNNClassifier(DistanceFunction<? super O> distanceFunction, int k) {
    super();
    this.distanceFunction = distanceFunction;
    this.k = k;
  }

  @Override
  public void buildClassifier(Database database, Relation<? extends ClassLabel> labels) {
    Relation<O> relation = database.getRelation(getDistanceFunction().getInputTypeRestriction());
    DistanceQuery<O> distanceQuery = database.getDistanceQuery(relation, getDistanceFunction());
    this.knnq = database.getKNNQuery(distanceQuery, k);
    this.labelrep = labels;
  }

  @Override
  public ClassLabel classify(O instance) {
    Object2IntOpenHashMap<ClassLabel> count = new Object2IntOpenHashMap<>();
    KNNList query = knnq.getKNNForObject(instance, k);
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

    KNNList query = knnq.getKNNForObject(instance, k);
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

  @Override
  @Deprecated
  public Result run(Database database) throws IllegalStateException {
    throw new AbortException("Classifiers cannot auto-run on a database, but need to be trained and can then predict.");
  }

  @Override
  public DistanceFunction<? super O> getDistanceFunction() {
    return distanceFunction;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
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
  public static class Parameterizer<O> extends AbstractParameterizer {
    /**
     * Parameter to specify the number of neighbors to take into account for
     * classification, must be an integer greater than 0.
     */
    public static final OptionID K_ID = new OptionID("knnclassifier.k", "The number of neighbors to take into account for classification.");

    /**
     * Distance function
     */
    protected DistanceFunction<? super O> distanceFunction;

    /**
     * Holds the value of @link #K_PARAM}.
     */
    protected int k;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<DistanceFunction<? super O>> distP = new ObjectParameter<>(DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);
      if(config.grab(distP)) {
        distanceFunction = distP.instantiateClass(config);
      }
      IntParameter kP = new IntParameter(K_ID, 1)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.intValue();
      }
    }

    @Override
    protected KNNClassifier<O> makeInstance() {
      return new KNNClassifier<>(distanceFunction, k);
    }
  }
}
