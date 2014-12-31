package de.lmu.ifi.dbs.elki.algorithm.clustering;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.uncertain.UOModel;
import de.lmu.ifi.dbs.elki.data.uncertain.UncertainObject;
import de.lmu.ifi.dbs.elki.database.AdaptedHashmapDatabase;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.memory.MemoryDataStoreFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.integer.SimpleDBIDFactory;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.ClusteringAdapterDatabaseConnection;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.exceptions.APIViolationException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;
import de.lmu.ifi.dbs.elki.workflow.EvaluationStep;

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
/**
 *
 * This classes purpose is to wrap and use a "normal" clustering
 * algorithm in a way for dealing with uncertain data of some kind.
 *
 * The approach is to construct a data set of samples drawn from
 * the uncertain objects.
 * That means that one or maybe no sample, but definitely not more
 * than one, is drawn from each uncertain object.
 *
 * Afterwards the chosen clustering algorithm is run on this
 * data set consisting of discrete objects and the result is
 * stored.
 *
 * The user has to specify how often this has to be repeated
 * (for each round a new sample data set is randomly drawn).
 *
 * In the end the stored clustering result are compared to
 * each other by some metric (TODO: refer elegantly to the paper)
 * and the best one is forwarded to the {@link EvaluationStep}.
 *
 * @author Alexander Koos
 *
 */
public class PWCClusteringAlgorithm extends AbstractAlgorithm<Clustering<Model>>{

  /**
   *
   * Parameterization class.
   *
   * @author Alexander Koos
   *
   */
  public static class Parameterizer extends AbstractParameterizer {

    /**
     * Field to store parameter value for depth.
     */
    protected long tryDepth;

    /**
     * Field to store the algorithm.
     */
    protected Algorithm algorithm;

    /**
     * Field to store the inner algorithm for meta-clustering
     */
    protected Algorithm metaAlgorithm;

    /**
     * Parameter to hand an algorithm for creating the meta-clustering
     * to our instance of {@link PWCClusteringAlgorithm}.
     *
     * It has to use a metric distancefunction to work on the
     * sample-clusterings.
     */
    public final static OptionID META_ALGORITHM_ID = new OptionID("algorithm.metaclustering","Used Algorithm for Meta-Clustering.");

    /**
     * Parameter to hand an algorithm to be wrapped and run
     * to our instance of {@link PWCClusteringAlgorithm}.
     */
    public final static OptionID ALGORITHM_ID = new OptionID("algorithm.clustering","Used Clustering Algorithm.");

    /**
     * Parameter to specify the amount of clusterings that
     * shall be created and compared.
     *
     * Has a default value.
     */
    public final static OptionID DEPTH_ID = new OptionID("uncert.depth","Amount of sample-clusterings to be made.");

    @Override
    protected Object makeInstance() {
      return new PWCClusteringAlgorithm(this.algorithm, this.tryDepth, this.metaAlgorithm);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected void makeOptions(final Parameterization config) {
      super.makeOptions(config);
      final ClassParameter malgorithm = new ClassParameter(Parameterizer.META_ALGORITHM_ID, Algorithm.class);
      if(config.grab(malgorithm)) {
        this.metaAlgorithm = (Algorithm) malgorithm.instantiateClass(config);
      }
      final ClassParameter palgorithm = new ClassParameter(Parameterizer.ALGORITHM_ID, Algorithm.class);
      if(config.grab(palgorithm)) {
        this.algorithm = (Algorithm) palgorithm.instantiateClass(config);
      }
      final LongParameter pdepth = new LongParameter(Parameterizer.DEPTH_ID, UOModel.DEFAULT_ENSAMBLE_DEPTH);
      if(config.grab(pdepth)) {
        this.tryDepth = pdepth.getValue();
      }
    }
  }

  /**
   * The algorithm to be wrapped and run.
   */
  private final Algorithm algorithm;

  /**
   * The algorithm for meta-clustering.
   */
  private final Algorithm metaAlgorithm;

  /**
   * Initialize a Logger.
   */
  private static final Logging LOG = Logging.getLogger(PWCClusteringAlgorithm.class);

  /**
   * How many clusterings shall be made for
   * comparison.
   */
  private final long depth;

  /**
   * Field to store arguments for invocation.
   */
  private Object[] arguments;

  /**
   * Field to store the run method for invocation.
   */
  private Method runAlgorithm;

  /**
   *
   * Constructor, quite trivial.
   *
   * @param algorithm
   * @param depth
   */
  public PWCClusteringAlgorithm(final Algorithm algorithm, final long depth, final Algorithm metaAlgorithm) {
    this.algorithm = algorithm;
    this.depth = depth;
    this.metaAlgorithm = metaAlgorithm;
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
   *
   * This Method tries if there is a run method
   * capable for some of the asked for signatures
   * and returns it if some exists, throws an Exception
   * otherwise.
   *
   * @param a
   * @param signatures
   * @param relations
   * @return
   * @throws NoSuchMethodException
   * @throws InvocationTargetException
   * @throws IllegalAccessException
   */
  private Method getRunMethod(final Algorithm a, final Class<?>[][] signatures, final Object[]... relations) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method m = null;
    for(int i = 0; i < signatures.length; i++) {
      try {
        m = a.getClass().getMethod("run", signatures[i]);
        this.arguments = relations[i];
      } catch(final NoSuchMethodException e) {
        if(i == signatures.length - 1)
        {
          throw e;
        }
        continue;
      }
      return m;
    }
    // to satisfy signature constraints
    return null;
  }

  /**
   *
   * This Method is used to prepare and hand over the signatures and arguments
   * for an unknown run method.
   */
  @SuppressWarnings("rawtypes")
  private Method prepareAlgorithm(final Database database, final Relation relation, final Algorithm algorithm) {
    final Class<?>[] signature1 = new Class<?>[2];
    final Object[] relations1 = new Object[2];
    final Class<?>[] signature2 = new Class<?>[1];
    final Object[] relations2 = new Object[1];
    for(int j = 0; j < 2; j++) {
      if(j == 0) {
        signature1[j] = Database.class;
        relations1[j] = database;
        continue;
      }
      signature1[j] = signature2[j - 1] = Relation.class;
      relations1[j] = relations2[j - 1] = relation;
    }
    Method runAlgorithm;
    try {
      runAlgorithm = this.getRunMethod(algorithm, new Class<?>[][] {signature1, signature2}, relations1, relations2);
      return runAlgorithm;
    } catch(final NoSuchMethodException e) {
      throw new APIViolationException("No appropriate 'run' method found.");
    }catch(IllegalArgumentException | IllegalAccessException | SecurityException e) {
      throw new APIViolationException("Invoking the real 'run' method failed.", e);
    }
    catch(final InvocationTargetException e) {
      final Throwable cause = e.getTargetException();
      if(cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      if(cause instanceof Error) {
        throw (Error) cause;
      }
      throw new APIViolationException("Invoking the real 'run' method failed: " + cause.toString(), cause);
    }
  }

  /**
   *
   * This run method will do the wrapping.
   *
   * Its called from {@link AbstractAlgorithm#run(Database)} and
   * performs the call to the algorithms particular run method
   * as well as the storing and comparison of the resulting Clusterings.
   *
   * @param database
   * @param relation
   * @return
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Clustering<Model> run(final Database database, final Relation<UncertainObject<UOModel<SpatialComparable>>> relation) {
    final MemoryDataStoreFactory factory = new MemoryDataStoreFactory();
    final ArrayList<Clustering<Model>> clusterings = new ArrayList<Clustering<Model>>();
    final ArrayList<Clustering<Model>> rclusterings = new ArrayList<Clustering<Model>>();
    Relation<?> labelRel = null;
    for(final Relation<?> r : database.getRelations()) {
      if(r.getDataTypeInformation().equals(TypeUtil.CLASSLABEL)) {
        labelRel = r;
      }
    }
    final List<Relation<NumberVector>> rlist = new ArrayList<Relation<NumberVector>>();
    for(int i = 0; i < this.depth; i++) {
      final double[][] sampleVecs = new double[relation.getDBIDs().size()][relation.get(relation.getDBIDs().iter()).getDimensionality()];
      final double[][] groundVecs = new double[relation.getDBIDs().size()][relation.get(relation.getDBIDs().iter()).getDimensionality()];
      final double[][] obsVecs = new double[relation.getDBIDs().size()][relation.get(relation.getDBIDs().iter()).getDimensionality()];
      int j = 0;
      final String[] labels = new String[relation.getDBIDs().size()];
      for(final DBIDIter iter = relation.getDBIDs().iter(); iter.valid(); iter.advance()) {
        final DoubleVector vec = relation.get(iter).drawSample();
        if(i == 0) {
          groundVecs[j] = relation.get(iter).getValues();
          obsVecs[j] = relation.get(iter).getObservation();
        }
        sampleVecs[j] = vec.getValues();
        labels[j] = labelRel.get(iter).toString();
        j++;
      }
      if(i == 0) {
        final SimpleTypeInformation<NumberVector> t = VectorFieldTypeInformation.typeRequest(NumberVector.class,relation.get(relation.getDBIDs().iter()).getDimensionality(),relation.get(relation.getDBIDs().iter()).getDimensionality());
        final WritableDataStore<NumberVector> store0 = factory.makeStorage(database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD).getDBIDs(), 0, NumberVector.class);
        final WritableDataStore<NumberVector> store1 = factory.makeStorage(database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD).getDBIDs(), 0, NumberVector.class);
        int k = 0;
        for(final DBIDIter iter = database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD).getDBIDs().iter(); iter.valid(); iter.advance()) {
          store0.put(iter, new DoubleVector(groundVecs[k]));
          store1.put(iter, new DoubleVector(obsVecs[k]));
          k++;
        }
        final Relation<NumberVector> ground = new MaterializedRelation<>(database, t, database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD).getDBIDs(), "Groundtruth", store0);
        final Relation<NumberVector> obs = new MaterializedRelation<>(database, t, database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD).getDBIDs(), "Centers of Mass", store1);
        rlist.add(ground);
        rlist.add(obs);
      }
      j = 0;
      final SimpleTypeInformation<NumberVector> t = VectorFieldTypeInformation.typeRequest(NumberVector.class,relation.get(relation.getDBIDs().iter()).getDimensionality(),relation.get(relation.getDBIDs().iter()).getDimensionality());
      final WritableDataStore<NumberVector> store = factory.makeStorage(database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD).getDBIDs(), 0, NumberVector.class);
      for(final DBIDIter iter = database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD).getDBIDs().iter(); iter.valid(); iter.advance()) {
        store.put(iter, new DoubleVector(sampleVecs[j]));
        j++;
      }
      final Relation<NumberVector> sample = new MaterializedRelation<>(database, t, database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD).getDBIDs(), "Sample" + i, store);
      rlist.add(sample);
    }

    for(final Relation<NumberVector> r : rlist) {
      if(this.arguments == null) {
        this.runAlgorithm = this.prepareAlgorithm(database, r, this.algorithm);
      } else {
        this.arguments[this.arguments.length - 1] =  r;
      }

      Clustering<Model> clusterResult;
      try {
        clusterResult = (Clustering<Model>) this.runAlgorithm.invoke(this.algorithm, this.arguments);
      } catch(final Exception e) {
        throw new APIViolationException("Invoking the run method failed at sample clustering.", e);
      }

      database.getHierarchy().add(database, r);
      database.getHierarchy().add(r, clusterResult);

      if(r.getLongName().substring(0,5).equals("Sample")) {
        clusterings.add(clusterResult);
      } else {
        rclusterings.add(clusterResult);
      }
    }

    rclusterings.addAll(clusterings);

    final ClusteringAdapterDatabaseConnection dbc = new ClusteringAdapterDatabaseConnection(rclusterings);
    final HashSetModifiableDBIDs iids = DBIDUtil.newHashSet();
    ((AdaptedHashmapDatabase) database).insert(dbc.loadData(), iids, "Sample-Clusterings");

    final SimpleTypeInformation<Clustering<Model>> t = new SimpleTypeInformation<>(Clustering.class);
    final WritableDataStore<Clustering<Model>> datastore = factory.makeStorage(database.getRelation(t).getDBIDs(), 0, Clustering.class);
    for(final DBIDIter iter = database.getRelation(t).getDBIDs().iter(); iter.valid(); iter.advance()) {
      datastore.put(iter, (Clustering<Model>) database.getRelation(t).get(iter));
    }

    final ModifiableDBIDs ids = (new SimpleDBIDFactory()).newArray();
    for(final DBIDIter iter = database.getRelation(t).getDBIDs().iter(); iter.valid(); iter.advance()) {
      for(int i = 0; i < database.getBundle(iter).metaLength(); i++) {
        final Object b = database.getBundle(iter).data(i);
        if(b != null) {
          if(b.getClass().equals(Clustering.class)) {
            if(((Relation)database.getHierarchy().iterParents((Result)b).get()).getLongName().substring(0,6).equals("Sample")) {
              ids.add(iter);
            }
          }
        }
      }
    }

    final Relation<Clustering<Model>> simRelation = new MaterializedRelation<Clustering<Model>>(database, t, ids, "Clusterings", datastore);

    this.runAlgorithm = null;
    this.arguments = null;

    this.runAlgorithm = this.prepareAlgorithm(database, simRelation, this.metaAlgorithm);

    Clustering<Model> metaClustering;

    try {
      metaClustering = (Clustering<Model>) this.runAlgorithm.invoke(this.metaAlgorithm, this.arguments);
    } catch(final Exception e) {
      throw new APIViolationException("Invoking the run method failed during meta clustering.", e);
    }

    return metaClustering;
  }

}
