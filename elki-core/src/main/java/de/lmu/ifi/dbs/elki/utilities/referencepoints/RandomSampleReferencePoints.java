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
package de.lmu.ifi.dbs.elki.utilities.referencepoints;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

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
  public static class Parameterizer extends AbstractParameterizer {
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
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter samplesizeP = new IntParameter(N_ID)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(samplesizeP)) {
        samplesize = samplesizeP.intValue();
      }
      RandomParameter randomP = new RandomParameter(RANDOM_ID);
      if(config.grab(randomP)) {
        rnd = randomP.getValue();
      }
    }

    @Override
    protected RandomSampleReferencePoints makeInstance() {
      return new RandomSampleReferencePoints(samplesize, rnd);
    }
  }
}
