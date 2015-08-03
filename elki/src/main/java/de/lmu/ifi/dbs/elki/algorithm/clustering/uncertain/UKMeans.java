package de.lmu.ifi.dbs.elki.algorithm.clustering.uncertain;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeansLloyd;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMeansInitialization;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.RandomlyChosenInitialMeans;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.uncertain.UncertainObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ProxyDatabase;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

// TODO: JavaDoc
// FIXME: add @Reference
public class UKMeans extends AbstractAlgorithm<Clustering<Model>> {
  /**
   * CLass logger.
   */
  private static final Logging LOG = Logging.getLogger(UKMeans.class);

  /**
   * Number of cluster centers to initialize.
   */
  protected int k;

  /**
   * Maximum number of iterations
   */
  protected int maxiter;

  /**
   * Method to choose initial means.
   */
  protected KMeansInitialization<? super NumberVector> initializer;

  // TODO: JavaDoc
  public UKMeans(int k, int maxiter, KMeansInitialization<? super NumberVector> initializer) {
    this.k = k;
    this.maxiter = maxiter;
    this.initializer = initializer;
  }

  // TODO: JavaDoc
  public Clustering<?> run(final Database database, final Relation<UncertainObject> relation) {
    final int dim = RelationUtil.dimensionality(relation);
    final DBIDs ids = relation.getDBIDs();
    final WritableDataStore<NumberVector> store = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_DB, NumberVector.class);
    for(final DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      store.put(iter, relation.get(iter).getMean());
    }

    final SimpleTypeInformation<NumberVector> t = VectorFieldTypeInformation.typeRequest(NumberVector.class, dim, dim);
    final Relation<NumberVector> sample = new MaterializedRelation<>(t, ids, "means", store);
    ProxyDatabase d = new ProxyDatabase(ids, sample);
    KMeansLloyd<NumberVector> kmeans = new KMeansLloyd<NumberVector>(EuclideanDistanceFunction.STATIC, this.k, this.maxiter, this.initializer);
    return kmeans.run(d);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(new SimpleTypeInformation<>(UncertainObject.class));
  }

  @Override
  protected Logging getLogger() {
    return UKMeans.LOG;
  }

  // FIXME: add Parameterizer, to allow using this in the GUI.
  public static class Parameterizer extends AbstractParameterizer {
    private KMeansInitialization<? super NumberVector> initializer;
    
    private int k;
    
    private int maxiter;
    
    public static final OptionID INIT_ID = new OptionID("ukmeans.initialization", "Method to choose the initial means.");
    
    public final static OptionID K_ID = new OptionID("ukmeans.k","The number of clusters to find.");
    
    public final static OptionID MAXITER_ID = new OptionID("ukmeans.maxiter","The maximum number of iterations to do. 0 means no limit.");
    
    @Override
    public void makeOptions(Parameterization config) {
      super.makeOptions(config);ObjectParameter<KMeansInitialization<? super NumberVector>> initialP = new ObjectParameter<>(INIT_ID, KMeansInitialization.class, RandomlyChosenInitialMeans.class);
      if(config.grab(initialP)) {
        initializer = initialP.instantiateClass(config);
      }
      IntParameter maxiterP = new IntParameter(MAXITER_ID, 0);
      maxiterP.addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT);
      if(config.grab(maxiterP)) {
        maxiter = maxiterP.getValue();
      }
      IntParameter kP = new IntParameter(K_ID);
      kP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.getValue();
      }
    }
    
    @Override
    protected UKMeans makeInstance() {
      return new UKMeans(k, maxiter, initializer);
    }
    
  }
}
