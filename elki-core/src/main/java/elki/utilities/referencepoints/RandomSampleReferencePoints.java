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
package elki.utilities.referencepoints;

import java.util.ArrayList;
import java.util.Collection;

import elki.data.NumberVector;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.logging.LoggingUtil;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

/**
 * Random-Sampling strategy for picking reference points.
 * 
 * @author Erich Schubert
 * @since 0.3
 */
public class RandomSampleReferencePoints implements ReferencePointsHeuristic {
  /**
   * Sample size.
   */
  protected int samplesize;

  /**
   * Random generator.
   */
  protected RandomFactory rnd;

  /**
   * Constructor.
   * 
   * @param samplesize Sampling size
   */
  public RandomSampleReferencePoints(int samplesize, RandomFactory rnd) {
    super();
    this.samplesize = samplesize;
    this.rnd = rnd;
  }

  @Override
  public Collection<? extends NumberVector> getReferencePoints(Relation<? extends NumberVector> db) {
    if(samplesize >= db.size()) {
      LoggingUtil.warning("Requested sample size is larger than database size!");
      return new RelationUtil.CollectionFromRelation<>(db);
    }

    DBIDs sample = DBIDUtil.randomSample(db.getDBIDs(), samplesize, rnd);

    ArrayList<NumberVector> result = new ArrayList<>(sample.size());
    for(DBIDIter it = sample.iter(); it.valid(); it.advance()) {
      result.add(db.get(it));
    }
    return result;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter to specify the sample size.
     */
    public static final OptionID N_ID = new OptionID("sample.n", "The number of samples to draw.");

    /**
     * Parameter to specify the sample size.
     */
    public static final OptionID RANDOM_ID = new OptionID("sample.random", "Random generator seed.");

    /**
     * Sample size.
     */
    protected int samplesize;

    /**
     * Random generator.
     */
    protected RandomFactory rnd;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(N_ID)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> samplesize = x);
      new RandomParameter(RANDOM_ID).grab(config, x -> rnd = x);
    }

    @Override
    public RandomSampleReferencePoints make() {
      return new RandomSampleReferencePoints(samplesize, rnd);
    }
  }
}
