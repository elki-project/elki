package de.lmu.ifi.dbs.elki.visualization.style;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import gnu.trove.list.array.TIntArrayList;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;

/**
 * Styling policy based on cluster membership.
 * 
 * @author Erich Schubert
 * 
 */
// TODO: fast enough? Some other kind of mapping we can use?
public class ClusterStylingPolicy implements ClassStylingPolicy {
  /**
   * Object IDs
   */
  ArrayList<DBIDs> ids;

  /**
   * Colors
   */
  TIntArrayList colors;

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
    ids = new ArrayList<DBIDs>(clusters.size());
    colors = new TIntArrayList(clusters.size());

    Iterator<? extends Cluster<?>> ci = clusters.iterator();
    for(int i = 0;; i++) {
      Cluster<?> c = ci.next();
      ids.add(DBIDUtil.ensureSet(c.getIDs()));
      Color col = SVGUtil.stringToColor(colorset.getColor(i));
      if (col != null) {
        colors.add(col.getRGB());
      } else {
        LoggingUtil.warning("Unrecognized color name: "+colorset.getColor(i));
      }
      if(!ci.hasNext()) {
        break;
      }
    }
  }

  @Override
  public int getStyleForDBID(DBID id) {
    for(int i = 0; i < ids.size(); i++) {
      if(ids.get(i).contains(id)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public int getColorForDBID(DBID id) {
    for(int i = 0; i < ids.size(); i++) {
      if(ids.get(i).contains(id)) {
        return colors.get(i);
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
  public Iterator<DBID> iterateClass(int cnum) {
    return ids.get(cnum).iterator();
  }

  /**
   * Get the clustering used by this styling policy
   * 
   * @return Clustering in use
   */
  public Clustering<?> getClustering() {
    return clustering;
  }
}