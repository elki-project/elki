package experimentalcode.erich.lsh;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.AbstractIndex;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.math.Mean;

/**
 * Locality Sensitive Hashing.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Object type to index
 */
public class InMemoryLSHIndex<V> implements IndexFactory<V, InMemoryLSHIndex.Instance<V>> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(InMemoryLSHIndex.class);

  /**
   * LSH hash function family to use.
   */
  LocalitySensitiveHashFunctionFamily<? super V> family;

  /**
   * Number of hash functions for each table.
   */
  int k;

  @Override
  public Instance<V> instantiate(Relation<V> relation) {
    return new Instance<>(relation, family.generateHashFunctions(relation, k));
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return family.getInputTypeRestriction();
  }

  /**
   * Instance of a LSH index for a single relation.
   * 
   * @author Erich Schubert
   */
  public static class Instance<V> extends AbstractIndex<V> {
    /**
     * Hash functions to use.
     */
    ArrayList<? extends LocalitySensitiveHashFunction<? super V>> hashfunctions;

    /**
     * Number of buckets for each hash function.
     */
    private int bucketsPerHash;

    /**
     * The actual table
     */
    ArrayList<TIntObjectMap<DBIDs>> hashtables;

    /**
     * Number of buckets to use.
     */
    private int numberOfBuckets = 100;

    /**
     * Constructor.
     * 
     * @param relation Relation to index.
     * @param hashfunctions Hash functions.
     */
    public Instance(Relation<V> relation, ArrayList<? extends LocalitySensitiveHashFunction<? super V>> hashfunctions) {
      super(relation);
      this.hashfunctions = hashfunctions;
    }

    @Override
    public String getLongName() {
      return "LSH index";
    }

    @Override
    public String getShortName() {
      return "lsh-index";
    }

    @Override
    public void initialize() {
      final int totalbuckets = bucketsPerHash * hashfunctions.size();
      hashtables = new ArrayList<>(hashfunctions.size());
      for (int i = 0; i < totalbuckets; i++) {
        hashtables.add(new TIntObjectHashMap<DBIDs>(numberOfBuckets));
      }

      for (DBIDIter iter = relation.getDBIDs().iter(); iter.valid(); iter.advance()) {
        V obj = relation.get(iter);
        for (int i = 0; i < hashfunctions.size(); i++) {
          // Get the initial (unbounded) hash code:
          int hash = hashfunctions.get(i).hashObject(obj);
          // Reduce to hash table size
          int bucket = hash % numberOfBuckets;
          final TIntObjectMap<DBIDs> table = hashtables.get(i);
          DBIDs cur = table.get(bucket);
          if (cur == null) {
            table.put(bucket, DBIDUtil.deref(iter));
          } else if (cur.size() > 1) {
            ((ModifiableDBIDs) cur).add(iter);
          } else {
            ModifiableDBIDs newbuck = DBIDUtil.newArray();
            newbuck.addDBIDs(cur);
            newbuck.add(iter);
            table.put(bucket, newbuck);
          }
        }
      }
      if (LOG.isStatistics()) {
        Mean bmean = new Mean();
        for (int i = 0; i < hashfunctions.size(); i++) {
          final TIntObjectMap<DBIDs> table = hashtables.get(i);
          for (TIntObjectIterator<DBIDs> iter = table.iterator(); iter.hasNext(); iter.advance()) {
            bmean.put(iter.value().size());
          }
        }
        LOG.statistics(new DoubleStatistic("lsh.mean-fill", bmean.getMean()));
      }
    }

    @Override
    public void logStatistics() {
      LOG.statistics(new LongStatistic("lsh.hashfunctions", hashfunctions.size()));
    }
  }
}
