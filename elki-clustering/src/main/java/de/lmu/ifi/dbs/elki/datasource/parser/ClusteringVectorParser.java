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
package de.lmu.ifi.dbs.elki.datasource.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.ClusteringVectorDumper;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * Parser for simple clustering results in vector form, as written by
 * {@link ClusteringVectorDumper}.
 *
 * This allows reading the output of <em>multiple</em> clustering runs, and
 * analyze the results using ELKI algorithm.
 *
 * The input format is very simple, each line containing a sequence of cluster
 * assignments in integer form, and an optional label:
 *
 * <pre>
 * 0 0 1 1 0 First
 * 0 0 0 1 2 Second
 * </pre>
 *
 * represents two clusterings for 5 objects. The first clustering has two
 * clusters, the second contains three clusters.
 *
 * TODO: this parser currently is quite hacky, and could use a cleanup.
 *
 * TODO: support noise, via negative cluster numbers?
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - Clustering
 */
public class ClusteringVectorParser extends AbstractStreamingParser {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(ClusteringVectorParser.class);

  /**
   * Number of different terms observed.
   */
  int numterms;

  /**
   * Metadata.
   */
  protected BundleMeta meta;

  /**
   * Event to report next.
   */
  Event nextevent;

  /**
   * Current clustering.
   */
  Clustering<Model> curclu;

  /**
   * Current labels.
   */
  LabelList curlbl;

  /**
   * Buffers, will be reused.
   */
  IntArrayList buf1 = new IntArrayList();

  /**
   * Range of the DBID values.
   */
  DBIDRange range = null;

  /**
   * Buffer for labels.
   */
  ArrayList<String> lbl = new ArrayList<>();

  /**
   * Flag if labels are present.
   */
  boolean haslbl;

  /**
   * Constructor.
   *
   * @param format Input format
   */
  public ClusteringVectorParser(CSVReaderFormat format) {
    super(format);
  }

  @Override
  public void initStream(InputStream in) {
    super.initStream(in);
    range = null; // New range
    haslbl = false;
  }

  @Override
  public Event nextEvent() {
    if(nextevent != null) {
      Event ret = nextevent;
      nextevent = null;
      return ret;
    }
    try {
      while(reader.nextLineExceptComments()) {
        buf1.clear();
        lbl.clear();
        Int2IntOpenHashMap csize = new Int2IntOpenHashMap();
        String name = null;
        for(/* initialized by nextLineExceptComments() */; tokenizer.valid(); tokenizer.advance()) {
          try {
            int cnum = tokenizer.getIntBase10();
            buf1.add(cnum);
            // Update cluster sizes:
            csize.addTo(cnum, 1);
          }
          catch(NumberFormatException e) {
            final String label = tokenizer.getSubstring();
            lbl.add(label);
            if(name == null) {
              name = label;
            }
          }
        }
        if(name == null) {
          name = "Cluster";
        }
        // Update meta on first record:
        boolean metaupdate = (range == null);
        if(range == null) {
          range = DBIDUtil.generateStaticDBIDRange(buf1.size());
        }
        if(buf1.size() != range.size()) {
          throw new AbortException("Clusterings do not contain the same number of elements!");
        }
        // Build clustering to store in the relation.
        Int2ObjectOpenHashMap<ModifiableDBIDs> clusters = new Int2ObjectOpenHashMap<>(csize.size());
        curclu = new Clustering<>(name, name);
        for(ObjectIterator<Int2IntMap.Entry> iter = csize.int2IntEntrySet().fastIterator(); iter.hasNext();) {
          Int2IntMap.Entry entry = iter.next();
          if(entry.getIntValue() > 0) {
            clusters.put(entry.getIntKey(), DBIDUtil.newArray(entry.getIntValue()));
          }
        }
        DBIDArrayIter iter = range.iter();
        for(int i = 0; i < buf1.size(); i++) {
          clusters.get(buf1.getInt(i)).add(iter.seek(i));
        }
        for(ModifiableDBIDs cids : clusters.values()) {
          curclu.addToplevelCluster(new Cluster<Model>(cids, ClusterModel.CLUSTER));
        }
        // Label handling.
        if(!haslbl && !lbl.isEmpty()) {
          haslbl = true;
          metaupdate = true;
        }
        curlbl = LabelList.make(lbl);
        if(metaupdate) {
          nextevent = Event.NEXT_OBJECT; // Force a meta update.
          return Event.META_CHANGED;
        }
        return Event.NEXT_OBJECT;
      }
      return Event.END_OF_STREAM;
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + reader.getLineNumber() + ".");
    }
  }

  @Override
  public Object data(int rnum) {
    if(rnum == 0) {
      return curclu;
    }
    if(rnum == 1) {
      return curlbl;
    }
    throw new ArrayIndexOutOfBoundsException();
  }

  @Override
  public BundleMeta getMeta() {
    if(meta == null) {
      meta = new BundleMeta(haslbl ? 2 : 1);
      meta.add(Clustering.TYPE);
      if(haslbl) {
        meta.add(TypeUtil.LABELLIST);
      }
    }
    return meta;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractStreamingParser.Parameterizer {
    @Override
    protected ClusteringVectorParser makeInstance() {
      return new ClusteringVectorParser(format);
    }
  }
}
