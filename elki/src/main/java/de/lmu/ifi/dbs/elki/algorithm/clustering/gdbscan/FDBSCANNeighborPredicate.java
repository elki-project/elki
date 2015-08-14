package de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.uncertain.UncertainObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

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

public class FDBSCANNeighborPredicate<O extends NumberVector, U extends UncertainObject> extends EpsilonNeighborPredicate<O> {

  private int sampleSize;
  
  private double threshold;
  
  public FDBSCANNeighborPredicate(double epsilon, DistanceFunction<? super O> distFunc, int sampleSize, double threshold) {
    super(epsilon, distFunc);
    this.sampleSize = sampleSize;
    this.threshold = threshold;
  }
  /**
   * 
   * @author Alexander Koos
   *
   */
  /*
   * TODO: Why does it have to be DoubleDBIDs?
   * Took the implementation from EpsilonNeighborPredicate.Instance as inspiration...
   */
  public static class Instance<O,U> implements NeighborPredicate.Instance<DoubleDBIDList> {

    private DoubleDBIDList ids;
    
    private double epsilon;
    
    private int sampleSize;
    
    private double threshold;

    private Relation<U> relation;
    
    public Instance(double epsilon, DoubleDBIDList ids, int sampleSize, double threshold, Relation<U> relation) {
      super();
      this.ids = ids;
      this.epsilon = epsilon;
      this.sampleSize = sampleSize;
      this.threshold = threshold;
      this.relation = relation;
      // TODO: integrate distancefunction correctly
    }
    
    @Override
    // FIXME: You can't accelerate me by using indices as I am!
    // Particularly useful would be spatial index-structures like
    // R-Trees.
    // That would give the opportunity to use a rangequery on the
    // database that only returns objects that aren't completely excluded.
    //
    // Even more powerful would be a spatial index-structure that knows about
    // complete inclusion of objects in other objects boundaries and returns
    // with its rangequery a tupel that holds as FIRST a list of all completely
    // included and as SECOND a list of all other possible neighbors...
    //
    // But I believe this to be too special to consider it in detail right now.
    public DoubleDBIDList getNeighbors(DBIDRef reference) {
      UncertainObject referenceObject = (UncertainObject) relation.get(reference);
    
      for(DBIDIter iter = iterDBIDs(ids); iter.valid(); iter.advance()) {
        if(DBIDUtil.equal(reference,iter)) {
          // No need to test the object itself
          continue;
        }

        boolean included = true;
        UncertainObject comparisonObject = (UncertainObject) relation.get(iter);
        for(int i = 0; i < referenceObject.getDimensionality(); i++) {
          if(included) {
            if(((referenceObject.getMax(i) - referenceObject.getMin(i)) < (epsilon * 2)) && referenceObject.getMin(i) <= comparisonObject.getMin(i) && referenceObject.getMax(i) >= comparisonObject.getMax(i)) { 
              // leave as marked as completely included
              continue;
            }
            // at least in one dimension it is not completely included
            included = false;            
          } 
          if((referenceObject.getMin(i) - epsilon) > comparisonObject.getMax(i) || (referenceObject.getMax(i) + epsilon) < comparisonObject.getMin(i)) {
            // completely excluded
            continue;
          }
        }
        if(included) {
        }

        for(int i = 0; i < sampleSize; i++) {
          // comparisonObject.drawSample
          // nested loop because of cartesian product
          for(int j = 0; j < sampleSize; j++) {
            // TODO: referenceObject.drawSample and check if they're epsilon-close
            // Keep track of how many are epsilon-close
          }
        }
        // TODO: check if enough sample-pairings were epsilon-close
        // If yes, add to neighborlist to return
      }
      return null;
    }

    @Override
    public DBIDs getIDs() {
      return this.ids;
    }

    @Override
    public DBIDIter iterDBIDs(DoubleDBIDList neighbors) {
      return neighbors.iter();
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  // FIXME: An elegant way to retrieve a relation containing uncertain objects from the database...
  public <T> NeighborPredicate.Instance<T> instantiate(Database database, SimpleTypeInformation<?> type) {
    return (NeighborPredicate.Instance<T>) new Instance<>(epsilon, 
        (DoubleDBIDList) database.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD).getDBIDs(), sampleSize, threshold, 
        database.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD));
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public SimpleTypeInformation<?>[] getOutputType() {
    // TODO Auto-generated method stub
    return null;
  }
  
  public static class Parameterizer<O extends NumberVector, U extends UncertainObject> extends EpsilonNeighborPredicate.Parameterizer<O> {

    private int sampleSize;
    
    private double threshold;
    
    public final static OptionID SAMPLE_SIZE_ID = new OptionID("","");
    
    public final static OptionID THRESHOLD_ID = new OptionID("","");
    
    @Override
    public void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter sampleSizep = new IntParameter(SAMPLE_SIZE_ID);
      if(config.grab(sampleSizep)) {
        sampleSize = sampleSizep.getValue();
      }
      DoubleParameter thresholdp = new DoubleParameter(THRESHOLD_ID, 0.5);
      if(config.grab(thresholdp)) {
        threshold = thresholdp.getValue();
      }
    }
    
    @Override
    protected FDBSCANNeighborPredicate<O, U> makeInstance() {
      return new FDBSCANNeighborPredicate<>(epsilon, distfun, sampleSize, threshold);
    }
  }
}
