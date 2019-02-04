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
package de.lmu.ifi.dbs.elki.visualization.style;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Styling policy based on cluster membership.
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
// TODO: fast enough? Some other kind of mapping we can use?
public class ClusterStylingPolicy implements ClassStylingPolicy {
  /**
   * Object IDs
   */
  ArrayList<DBIDs> ids;

  /**
   * Map from cluster objects to color offsets.
   */
  Object2IntOpenHashMap<Cluster<?>> cmap;

  /**
   * Colors
   */
  IntArrayList colors;

  /**
   * Clustering in use.
   */
  Clustering<?> clustering;

  /**
   * Constructor.
   *
   * @param clustering Clustering to use.
   */
  public ClusterStylingPolicy(Clustering<?> clustering, StyleLibrary style) {
    super();
    this.clustering = clustering;
    ColorLibrary colorset = style.getColorSet(StyleLibrary.PLOT);
    List<? extends Cluster<?>> clusters = clustering.getAllClusters();
    ids = new ArrayList<>(clusters.size());
    colors = new IntArrayList(clusters.size());
    cmap = new Object2IntOpenHashMap<>(clusters.size());
    cmap.defaultReturnValue(-1);

    Iterator<? extends Cluster<?>> ci = clusters.iterator();
    for(int i = 0; ci.hasNext(); i++) {
      Cluster<?> c = ci.next();
      ids.add(DBIDUtil.ensureSet(c.getIDs()));
      cmap.put(c, i);
      Color col = SVGUtil.stringToColor(colorset.getColor(i));
      if(col != null) {
        colors.add(col.getRGB());
      }
      else {
        LoggingUtil.warning("Unrecognized color name: " + colorset.getColor(i));
      }
      if(!ci.hasNext()) {
        break;
      }
    }
  }

  @Override
  public int getStyleForDBID(DBIDRef id) {
    for(int i = 0; i < ids.size(); i++) {
      if(ids.get(i).contains(id)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public int getColorForDBID(DBIDRef id) {
    for(int i = 0; i < ids.size(); i++) {
      if(ids.get(i).contains(id)) {
        return colors.getInt(i);
      }
    }
    return 0;
  }

  @Override
  public int getMinStyle() {
    return 0;
  }

  @Override
  public int getMaxStyle() {
    return ids.size();
  }

  @Override
  public DBIDIter iterateClass(int cnum) {
    return ids.get(cnum).iter();
  }

  @Override
  public int classSize(int cnum) {
    return ids.get(cnum).size();
  }

  /**
   * Get the clustering used by this styling policy
   *
   * @return Clustering in use
   */
  public Clustering<?> getClustering() {
    return clustering;
  }

  /**
   * Get the style number for a cluster.
   *
   * @param c Cluster
   * @return Style number
   */
  public int getStyleForCluster(Cluster<?> c) {
    return cmap.getInt(c);
  }

  @Override
  public String getMenuName() {
    return clustering.getLongName();
  }
}
