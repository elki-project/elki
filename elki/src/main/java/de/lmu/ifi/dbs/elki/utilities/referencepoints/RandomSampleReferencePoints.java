package de.lmu.ifi.dbs.elki.utilities.referencepoints;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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
import java.util.Collection;
import java.util.Random;

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

/**
 * Random-Sampling strategy for picking reference points.
 * 
 * @author Erich Schubert
 */
public class RandomSampleReferencePoints implements ReferencePointsHeuristic {
  /**
   * Sample size.
   */
  private int samplesize;

  /**
   * Constructor.
   * 
   * @param samplesize Sampling size
   */
  public RandomSampleReferencePoints(int samplesize) {
    super();
    this.samplesize = samplesize;
  }

  @Override
  public Collection<? extends NumberVector> getReferencePoints(Relation<? extends NumberVector> db) {
    if(samplesize >= db.size()) {
      LoggingUtil.warning("Requested sample size is larger than database size!");
      return new RelationUtil.CollectionFromRelation<>(db);
    }

    ArrayList<NumberVector> result = new ArrayList<>(samplesize);
    DBIDs sample = DBIDUtil.randomSample(db.getDBIDs(), samplesize, new Random());

    for(DBIDIter it = sample.iter(); it.valid(); it.advance()) {
      result.add(db.get(it));
    }
    return result;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    // TODO: use reproducible Random

    /**
     * Parameter to specify the sample size.
     * <p>
     * Key: {@code -sample.n}
     * </p>
     */
    public static final OptionID N_ID = new OptionID("sample.n", "The number of samples to draw.");

    /**
     * Holds the value of {@link #N_ID}.
     */
    protected int samplesize;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter samplesizeP = new IntParameter(N_ID)//
      .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(samplesizeP)) {
        samplesize = samplesizeP.intValue();
      }
    }

    @Override
    protected RandomSampleReferencePoints makeInstance() {
      return new RandomSampleReferencePoints(samplesize);
    }
  }
}