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
package elki.outlier.spatial.neighborhood;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import elki.data.ExternalID;
import elki.data.LabelList;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStore;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.ids.*;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.utilities.exceptions.AbortException;
import elki.utilities.io.FileUtil;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.FileParameter;

/**
 * A precomputed neighborhood, loaded from an external file.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public class ExternalNeighborhood extends AbstractPrecomputedNeighborhood {
  /**
   * Logger
   */
  private static final Logging LOG = Logging.getLogger(ExternalNeighborhood.class);

  /**
   * Constructor.
   * 
   * @param store Store to access
   */
  public ExternalNeighborhood(DataStore<DBIDs> store) {
    super(store);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Factory class.
   * 
   * @author Erich Schubert
   * 
   * @stereotype factory
   * @navhas - produces - ExternalNeighborhood
   */
  public static class Factory extends AbstractPrecomputedNeighborhood.Factory<Object> {
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
    public NeighborSetPredicate instantiate(Database database, Relation<?> relation) {
      DataStore<DBIDs> store = loadNeighbors(database, relation);
      ExternalNeighborhood neighborhood = new ExternalNeighborhood(store);
      return neighborhood;
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return TypeUtil.ANY;
    }

    /**
     * Method to load the external neighbors.
     */
    private DataStore<DBIDs> loadNeighbors(Database database, Relation<?> relation) {
      final WritableDataStore<DBIDs> store = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC | DataStoreFactory.HINT_TEMP, DBIDs.class);

      if(LOG.isVerbose()) {
        LOG.verbose("Loading external neighborhoods.");
      }

      if(LOG.isDebugging()) {
        LOG.verbose("Building reverse label index...");
      }
      // Build a map label/ExternalId -> DBID
      // (i.e. a reverse index!)
      // TODO: move this into the database layer to share?
      Map<String, DBID> lblmap = new HashMap<>(relation.size() << 1);
      {
        Relation<LabelList> olq = database.getRelation(TypeUtil.LABELLIST);
        Relation<ExternalID> eidq = database.getRelation(TypeUtil.EXTERNALID);
        for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
          if(eidq != null) {
            ExternalID eid = eidq.get(iditer);
            if(eid != null) {
              lblmap.put(eid.toString(), DBIDUtil.deref(iditer));
            }
          }
          if(olq != null) {
            LabelList label = olq.get(iditer);
            if(label != null) {
              for(int i = 0; i < label.size(); i++) {
                lblmap.put(label.get(i), DBIDUtil.deref(iditer));
              }
            }
          }
        }
      }

      if(LOG.isDebugging()) {
        LOG.verbose("Loading neighborhood file.");
      }
      try (FileInputStream fis = new FileInputStream(file);
          InputStream in = FileUtil.tryGzipInput(fis);
          BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
        for(String line; (line = br.readLine()) != null;) {
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
                if(LOG.isDebugging()) {
                  LOG.debug("No object found for label " + entries[i]);
                }
              }
            }
            store.put(id, neighbours);
          }
          else {
            if(LOG.isDebugging()) {
              LOG.warning("No object found for label " + entries[0]);
            }
          }
        }
        return store;
      }
      catch(IOException e) {
        throw new AbortException("Loading of external neighborhood failed.", e);
      }
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     */
    public static class Par implements Parameterizer {
      /**
       * Parameter to specify the neighborhood file
       */
      public static final OptionID NEIGHBORHOOD_FILE_ID = new OptionID("externalneighbors.file", "The file listing the neighbors.");

      /**
       * The input file.
       */
      File file;

      @Override
      public void configure(Parameterization config) {
        new FileParameter(NEIGHBORHOOD_FILE_ID, FileParameter.FileType.INPUT_FILE) //
            .grab(config, x -> file = x);
      }

      @Override
      public ExternalNeighborhood.Factory make() {
        return new ExternalNeighborhood.Factory(file);
      }
    }
  }
}
