package experimentalcode.shared.outlier.generalized.neighbors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DataQuery;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.utilities.FileUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;

/**
 * A precomputed neighborhood.
 * 
 * @author Erich Schubert
 */
public class PrecomputedNeighborhood implements NeighborSetPredicate, Result {
  /**
   * Logger
   */
  protected static final Logging logger = Logging.getLogger(PrecomputedNeighborhood.class);

  /**
   * Parameter to specify the neighborhood file
   */
  public static final OptionID NEIGHBORHOOD_FILE_ID = OptionID.getOrCreateOptionID("externalneighbors.file", "The file listing the neighbors.");

  /**
   * Data store for the loaded result.
   */
  DataStore<DBIDs> store;

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
    return store.get(reference);
  }

  /**
   * Factory class.
   * 
   * @author Erich Schubert
   */
  public static class Factory implements NeighborSetPredicate.Factory<DatabaseObject> {
    /**
     * The input file.
     */
    private File file;

    /**
     * Constructor.
     * 
     * @param file File to load
     */
    public Factory(File file) {
      super();
      this.file = file;
    }

    @Override
    public NeighborSetPredicate instantiate(Database<? extends DatabaseObject> database) {
      DataStore<DBIDs> store = loadNeighbors(database);
      PrecomputedNeighborhood neighborhood = new PrecomputedNeighborhood(store);
      ResultHierarchy hier = database.getHierarchy();
      if(hier != null) {
        hier.add(database, neighborhood);
      }
      return neighborhood;
    }

    /**
     * Method to load the external neighbors.
     */
    private DataStore<DBIDs> loadNeighbors(Database<? extends DatabaseObject> database) {
      WritableDataStore<DBIDs> store = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC | DataStoreFactory.HINT_TEMP, DBIDs.class);

      // Build a map label/ExternalId -> DBID
      // (i.e. a reverse index!)
      // TODO: move this into the database layer to share?
      Map<String, DBID> lblmap = new HashMap<String, DBID>(database.size() * 2);
      {
        DataQuery<String> olq = database.getObjectLabelQuery();
        DataQuery<String> eidq = database.getExternalIdQuery();
        for(DBID id : database) {
          if(eidq != null) {
            String eid = eidq.get(id);
            if(eid != null) {
              lblmap.put(eid, id);
            }
          }
          if(olq != null) {
            String label = olq.get(id);
            if(label != null) {
              lblmap.put(label, id);
            }
          }
        }
      }

      try {
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
            store.put(id, neighbours);
          }
          else {
            logger.warning("No object found for label " + entries[0]);
          }
        }
        br.close();
        in.close();
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
      File file = getNeighborhoodFile(config);
      if(config.hasErrors()) {
        return null;
      }
      return new PrecomputedNeighborhood.Factory(file);
    }

    /**
     * Get the neighborhood parameter.
     * 
     * @param config Parameterization
     * @return Instance or null
     */
    public static File getNeighborhoodFile(Parameterization config) {
      final FileParameter param = new FileParameter(NEIGHBORHOOD_FILE_ID, FileParameter.FileType.INPUT_FILE);
      if(config.grab(param)) {
        return param.getValue();
      }
      return null;
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
}