package de.lmu.ifi.dbs.elki.algorithm;

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
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNResult;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.KNNDistanceOrderResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Provides an order of the kNN-distances for all objects within the database.
 * 
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 */
// TODO: redundant to kNN outlier detection?
@Title("KNN-Distance-Order")
@Description("Assesses the knn distances for a specified k and orders them.")
public class KNNDistanceOrder<O, D extends Distance<D>> extends AbstractDistanceBasedAlgorithm<O, D, KNNDistanceOrderResult<D>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KNNDistanceOrder.class);

  /**
   * Parameter to specify the distance of the k-distant object to be assessed,
   * must be an integer greater than 0.
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("knndistanceorder.k", "Specifies the distance of the k-distant object to be assessed.");

  /**
   * Holds the value of {@link #K_ID}.
   */
  private int k;

  /**
   * Parameter to specify the average percentage of distances randomly chosen to
   * be provided in the result, must be a double greater than 0 and less than or
   * equal to 1.
   */
  public static final OptionID PERCENTAGE_ID = OptionID.getOrCreateOptionID("knndistanceorder.percentage", "The average percentage of distances randomly choosen to be provided in the result.");

  /**
   * Holds the value of {@link #PERCENTAGE_ID}.
   */
  private double percentage;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param k k Parameter
   * @param percentage percentage parameter
   */
  public KNNDistanceOrder(DistanceFunction<O, D> distanceFunction, int k, double percentage) {
    super(distanceFunction);
    this.k = k;
    this.percentage = percentage;
  }

  /**
   * Provides an order of the kNN-distances for all objects within the specified
   * database.
   * 
   * @param database Database
   * @param relation Relation
   * @return Result
   */
  public KNNDistanceOrderResult<D> run(Database database, Relation<O> relation) {
    final DistanceQuery<O, D> distanceQuery = database.getDistanceQuery(relation, getDistanceFunction());
    final KNNQuery<O, D> knnQuery = database.getKNNQuery(distanceQuery, k);

    final Random random = new Random();
    List<D> knnDistances = new ArrayList<D>(relation.size());
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      if(random.nextDouble() < percentage) {
        final KNNResult<D> neighbors = knnQuery.getKNNForDBID(iditer, k);
        knnDistances.add(neighbors.getKNNDistance());
      }
    }
    Collections.sort(knnDistances, Collections.reverseOrder());
    return new KNNDistanceOrderResult<D>("kNN distance order", "knn-order", knnDistances);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends Distance<D>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {
    protected int k;

    protected double percentage;

    public Parameterizer() {
      super();
    }

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(K_ID, 1);
      kP.addConstraint(new GreaterConstraint(0));
      if(config.grab(kP)) {
        k = kP.getValue();
      }

      DoubleParameter percentageP = new DoubleParameter(PERCENTAGE_ID, 1.0);
      percentageP.addConstraint(new IntervalConstraint(0, IntervalConstraint.IntervalBoundary.OPEN, 1, IntervalConstraint.IntervalBoundary.CLOSE));
      if(config.grab(percentageP)) {
        percentage = percentageP.getValue();
      }
    }

    @Override
    protected KNNDistanceOrder<O, D> makeInstance() {
      return new KNNDistanceOrder<O, D>(distanceFunction, k, percentage);
    }
  }
}