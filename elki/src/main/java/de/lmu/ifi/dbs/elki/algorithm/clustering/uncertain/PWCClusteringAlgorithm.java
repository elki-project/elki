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
import java.util.ArrayList;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
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
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.workflow.EvaluationStep;

/**
 * This classes purpose is to wrap and use a "normal" clustering algorithm in a
 * way for dealing with uncertain data of some kind.
 *
 * The approach is to construct a data set of samples drawn from the uncertain
 * objects. That means that one or maybe no sample, but definitely not more than
 * one, is drawn from each uncertain object.
 *
 * Afterwards the chosen clustering algorithm is run on this data set consisting
 * of discrete objects and the result is stored.
 *
 * The user has to specify how often this has to be repeated (for each round a
 * new sample data set is randomly drawn).
 *
 * In the end the stored clustering result are compared to each other by some
 * metric (TODO: refer elegantly to the paper) and the best one is forwarded to
 * the {@link EvaluationStep}.
 *
 * @author Alexander Koos
 */
public class PWCClusteringAlgorithm extends AbstractAlgorithm<Clustering<Model>> {
  /**
   * Initialize a Logger.
   */
  private static final Logging LOG = Logging.getLogger(PWCClusteringAlgorithm.class);

  /**
   * The algorithm to be wrapped and run.
   */
  private final ClusteringAlgorithm<?> algorithm;

  /**
   * The algorithm for meta-clustering.
   */
  private final ClusteringAlgorithm<?> metaAlgorithm;

  /**
   * How many clusterings shall be made for comparison.
   */
  private final int depth;

  /**
   * Constructor, quite trivial.
   *
   * @param algorithm
   * @param depth
   */
  public PWCClusteringAlgorithm(ClusteringAlgorithm<?> algorithm, int depth, ClusteringAlgorithm<?> metaAlgorithm) {
    this.algorithm = algorithm;
    this.depth = depth;
    this.metaAlgorithm = metaAlgorithm;
  }

  /**
   * This run method will do the wrapping.
   *
   * Its called from {@link AbstractAlgorithm#run(Database)} and performs the
   * call to the algorithms particular run method as well as the storing and
   * comparison of the resulting Clusterings.
   *
   * @param database Database
   * @param relation Data relation of uncertain objects
   * @return Clustering result
   */
  public Clustering<?> run(final Database database, final Relation<? extends UncertainObject> relation) {
    final ArrayList<Clustering<?>> clusterings = new ArrayList<>();
    {
      final int dim = RelationUtil.dimensionality(relation);
      final DBIDs ids = relation.getDBIDs();
      // Add the uncertain model:
      {
        final WritableDataStore<NumberVector> store1 = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_DB, NumberVector.class);
        for(final DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
          store1.put(iter, relation.get(iter).getMean());
        }
        runClusteringAlgorithm(relation, ids, store1, dim, "Uncertain Model: Center of Mass");
      }
      // Add samples:
      for(int i = 0; i < this.depth; i++) {
        final WritableDataStore<NumberVector> store = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_DB, NumberVector.class);
        for(final DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
          store.put(iter, relation.get(iter).drawSample());
        }
        clusterings.add(runClusteringAlgorithm(relation, ids, store, dim, "Sample " + i));
      }
    }

    // Step 2: perform the meta clustering (on samples only).
    DBIDRange rids = DBIDFactory.FACTORY.generateStaticDBIDRange(clusterings.size());
    final WritableDataStore<Clustering<?>> datastore = DataStoreUtil.makeStorage(rids, DataStoreFactory.HINT_DB, Clustering.class);
    Iterator<Clustering<?>> it2 = clusterings.iterator();
    for(final DBIDIter iter = rids.iter(); iter.valid(); iter.advance()) {
      datastore.put(iter, it2.next());
    }
    assert(rids.size() == clusterings.size());

    final Relation<Clustering<?>> simRelation = new MaterializedRelation<Clustering<?>>(TypeUtil.CLUSTERING, rids, "Clusterings", datastore);
    ProxyDatabase d = new ProxyDatabase(rids, simRelation);
    Clustering<?> c = this.metaAlgorithm.run(d);
    d.getHierarchy().remove(d, c);
    return c;
  }

  /**
   * Run a clustering algorithm on a single instance.
   *
   * @param database Database
   * @param ids
   * @param store
   * @param dim
   * @param title
   * @return
   */
  protected Clustering<?> runClusteringAlgorithm(final HierarchicalResult database, final DBIDs ids, final WritableDataStore<NumberVector> store, final int dim, final String title) {
    final SimpleTypeInformation<NumberVector> t = VectorFieldTypeInformation.typeRequest(NumberVector.class, dim, dim);
    final Relation<NumberVector> sample = new MaterializedRelation<>(t, ids, title, store);
    ProxyDatabase d = new ProxyDatabase(ids, sample);
    Clustering<?> clusterResult = this.algorithm.run(d);
    d.getHierarchy().remove(sample);
    d.getHierarchy().remove(clusterResult);
    database.getHierarchy().add(database, sample);
    database.getHierarchy().add(sample, clusterResult);
    return clusterResult;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(new SimpleTypeInformation<>(UncertainObject.class));
  }

  @Override
  protected Logging getLogger() {
    return PWCClusteringAlgorithm.LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Alexander Koos
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Field to store parameter value for depth.
     */
    protected int tryDepth;

    /**
     * Field to store the algorithm.
     */
    protected ClusteringAlgorithm<?> algorithm;

    /**
     * Field to store the inner algorithm for meta-clustering
     */
    protected ClusteringAlgorithm<?> metaAlgorithm;

    /**
     * Parameter to hand an algorithm for creating the meta-clustering to our
     * instance of {@link PWCClusteringAlgorithm}.
     *
     * It has to use a metric distancefunction to work on the
     * sample-clusterings.
     */
    public final static OptionID META_ALGORITHM_ID = new OptionID("algorithm.metaclustering", "Used Algorithm for Meta-Clustering.");

    /**
     * Parameter to hand an algorithm to be wrapped and run to our instance of
     * {@link PWCClusteringAlgorithm}.
     */
    public final static OptionID ALGORITHM_ID = new OptionID("algorithm.clustering", "Used Clustering Algorithm.");

    /**
     * Parameter to specify the amount of clusterings that shall be created and
     * compared.
     *
     * Has a default value.
     */
    public final static OptionID DEPTH_ID = new OptionID("uncert.depth", "Amount of sample-clusterings to be made.");

    @Override
    protected PWCClusteringAlgorithm makeInstance() {
      return new PWCClusteringAlgorithm(this.algorithm, this.tryDepth, this.metaAlgorithm);
    }

    @Override
    protected void makeOptions(final Parameterization config) {
      super.makeOptions(config);
      final ClassParameter<ClusteringAlgorithm<?>> malgorithm = new ClassParameter<>(Parameterizer.META_ALGORITHM_ID, ClusteringAlgorithm.class);
      if(config.grab(malgorithm)) {
        this.metaAlgorithm = malgorithm.instantiateClass(config);
        if(this.metaAlgorithm != null && this.metaAlgorithm.getInputTypeRestriction().length > 0 && //
        !this.metaAlgorithm.getInputTypeRestriction()[0].isAssignableFromType(TypeUtil.CLUSTERING)) {
          config.reportError(new WrongParameterValueException(malgorithm, malgorithm.getValueAsString(), "The meta clustering algorithm (as configured) does not accept clustering results."));
        }
      }
      final ClassParameter<ClusteringAlgorithm<?>> palgorithm = new ClassParameter<>(Parameterizer.ALGORITHM_ID, ClusteringAlgorithm.class);
      if(config.grab(palgorithm)) {
        this.algorithm = palgorithm.instantiateClass(config);
        if(this.algorithm != null && this.algorithm.getInputTypeRestriction().length > 0 && //
        !this.algorithm.getInputTypeRestriction()[0].isAssignableFromType(TypeUtil.NUMBER_VECTOR_FIELD)) {
          config.reportError(new WrongParameterValueException(palgorithm, palgorithm.getValueAsString(), "The inner clustering algorithm (as configured) does not accept numerical vectors: " + this.algorithm.getInputTypeRestriction()[0]));
        }
      }
      final IntParameter pdepth = new IntParameter(Parameterizer.DEPTH_ID, UncertainObject.DEFAULT_ENSEMBLE_DEPTH);
      if(config.grab(pdepth)) {
        this.tryDepth = pdepth.getValue();
      }
    }
  }
}
