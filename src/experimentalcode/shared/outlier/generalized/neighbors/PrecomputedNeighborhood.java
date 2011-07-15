package experimentalcode.shared.outlier.generalized.neighbors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.utilities.FileUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * A precomputed neighborhood.
 * 
 * @author Erich Schubert
 */
public class PrecomputedNeighborhood implements NeighborSetPredicate, Result {
  /**
   * Logger
   */
  private static final Logging logger = Logging.getLogger(PrecomputedNeighborhood.class);

  /**
   * Parameter to specify the neighborhood file
   */
  public static final OptionID NEIGHBORHOOD_FILE_ID = OptionID.getOrCreateOptionID("externalneighbors.file", "The file listing the neighbors.");

  /**
   * Parameter to specify the number of steps allowed
   */
  public static final OptionID STEPS_ID = OptionID.getOrCreateOptionID("externalneighbors.steps", "The number of steps allowed in the neighborhood graph.");

  /**
   * Data store for the loaded result.
   */
  private DataStore<DBIDs> store;

  /**
   * Constructor.
   * 
   * @param store Store to access
   */
  public PrecomputedNeighborhood(DataStore<DBIDs> store) {
    this.store = store;
  }

  @Override
  public DBIDs getNeighborDBIDs(DBID reference) {
    DBIDs neighbors = store.get(reference);
    if(neighbors != null) {
      return neighbors;
    }
    else {
      // Use just the object itself.
      if(logger.isDebugging()) {
        logger.warning("No neighbors for object " + reference);
      }
      return reference;
    }
  }
  
  @Override
  public String getLongName() {
    return "Precomputed Neighborhood";
  }

  @Override
  public String getShortName() {
    return "precomputed-neighborhood";
  }

  /**
   * Factory class.
   * 
   * @author Erich Schubert
   */
  public static class Factory implements NeighborSetPredicate.Factory<Object> {
    /**
     * Logger
     */
    private static final Logging logger = Logging.getLogger(PrecomputedNeighborhood.class);
    
    /**
     * The input file.
     */
    private File file;

    /**
     * Number of steps to do
     */
    private int steps;

    /**
     * Constructor.
     * 
     * @param file File to load
     * @param steps Number of steps to do
     */
    public Factory(File file, int steps) {
      super();
      this.file = file;
      this.steps = steps;
    }

    @Override
    public NeighborSetPredicate instantiate(Relation<?> database) {
      DataStore<DBIDs> store = loadNeighbors(database);
      PrecomputedNeighborhood neighborhood = new PrecomputedNeighborhood(store);
      ResultHierarchy hier = database.getHierarchy();
      if(hier != null) {
        hier.add(database, neighborhood);
      }
      return neighborhood;
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return TypeUtil.ANY;
    }

    /**
     * Method to load the external neighbors.
     */
    private DataStore<DBIDs> loadNeighbors(Relation<?> database) {
      final WritableDataStore<DBIDs> store = DataStoreUtil.makeStorage(database.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC | DataStoreFactory.HINT_TEMP, DBIDs.class);
      final WritableDataStore<DBIDs> tstore;
      // TStore is temporary when we need to do multi-step
      if(steps <= 1) {
        tstore = store;
      }
      else {
        tstore = DataStoreUtil.makeStorage(database.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC | DataStoreFactory.HINT_TEMP, DBIDs.class);
      }

      if(logger.isVerbose()) {
        logger.verbose("Loading external neighborhoods.");
      }

      if(logger.isDebugging()) {
        logger.verbose("Building reverse label index...");
      }
      // Build a map label/ExternalId -> DBID
      // (i.e. a reverse index!)
      // TODO: move this into the database layer to share?
      Map<String, DBID> lblmap = new HashMap<String, DBID>(database.size() * 2);
      {
        Relation<LabelList> olq = database.getDatabase().getRelation(SimpleTypeInformation.get(LabelList.class));
        Relation<String> eidq = null; // database.getExternalIdQuery();
        for(DBID id : database.iterDBIDs()) {
          if(eidq != null) {
            String eid = eidq.get(id);
            if(eid != null) {
              lblmap.put(eid, id);
            }
          }
          if(olq != null) {
            LabelList label = olq.get(id);
            if(label != null) {
              for (String lbl: label) {
                lblmap.put(lbl, id);
              }
            }
          }
        }
      }

      try {
        if(logger.isDebugging()) {
          logger.verbose("Loading neighborhood file.");
        }
        InputStream in = new FileInputStream(file);
        in = FileUtil.tryGzipInput(in);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        int lineNumber = 0;
        for(String line; (line = br.readLine()) != null; lineNumber++) {
          ArrayModifiableDBIDs neighbours = DBIDUtil.newArray();
          String[] entries = line.split(" ");
          DBID id = lblmap.get(entries[0]);
          if(id != null) {
            for(int i = 0; i < entries.length; i++) {
              final DBID neigh = lblmap.get(entries[i]);
              if(neigh != null) {
                neighbours.add(neigh);
              }
              else {
                logger.warning("No object found for label " + entries[i]);
              }
            }
            tstore.put(id, neighbours);
          }
          else {
            logger.warning("No object found for label " + entries[0]);
          }
        }
        br.close();
        in.close();

        // Expand multiple steps
        if(steps > 0) {
          FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Expanding neighborhoods", database.size(), logger) : null;
          for(final DBID id : database.iterDBIDs()) {
            DBIDs cur = id;
            for(int i = 0; i < steps; i++) {
              ModifiableDBIDs upd = DBIDUtil.newHashSet(cur);
              for(final DBID oid : cur) {
                DBIDs add = tstore.get(oid);
                if(add != null) {
                  upd.addDBIDs(add);
                }
              }
              cur = upd;
            }
            store.put(id, cur);
            if(progress != null) {
              progress.incrementProcessed(logger);
            }
          }
          if(progress != null) {
            progress.ensureCompleted(logger);
          }
        }

        return store;
      }
      catch(IOException e) {
        throw new AbortException("Loading of external neighborhood failed.", e);
      }
    }

    /**
     * Factory method for {@link Parameterizable}
     * 
     * @param config Parameterization
     * @return instance
     */
    public static PrecomputedNeighborhood.Factory parameterize(Parameterization config) {
      File file = getParameterNeighborhoodFile(config);
      int steps = getParameterSteps(config);
      if(config.hasErrors()) {
        return null;
      }
      return new PrecomputedNeighborhood.Factory(file, steps);
    }

    /**
     * Get the neighborhood parameter.
     * 
     * @param config Parameterization
     * @return Instance or null
     */
    public static File getParameterNeighborhoodFile(Parameterization config) {
      final FileParameter param = new FileParameter(NEIGHBORHOOD_FILE_ID, FileParameter.FileType.INPUT_FILE);
      if(config.grab(param)) {
        return param.getValue();
      }
      return null;
    }

    /**
     * Get the number of steps to do in the neighborhood graph.
     * 
     * @param config Parameterization
     * @return number of steps, default 1
     */
    public static int getParameterSteps(Parameterization config) {
      final IntParameter param = new IntParameter(STEPS_ID, 1);
      if(config.grab(param)) {
        return param.getValue();
      }
      return 1;
    }
  }
}