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

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Styling policy based on cluster membership.
 * 
 * @author Erich Schubert
 * 
 */
// TODO: fast enough? Some other kind of mapping we can use?
public class ClusterStylingPolicy implements StylingPolicy {
  /**
   * Object IDs
   */
  ArrayList<DBIDs> ids;

  /**
   * Constructor.
   * 
   * @param clustering Clustering to use.
   */
  public ClusterStylingPolicy(Clustering<?> clustering) {
    super();
    List<? extends Cluster<?>> clusters = clustering.getAllClusters();
    ids = new ArrayList<DBIDs>();
    for(Cluster<?> c : clusters) {
      ids.add(DBIDUtil.ensureSet(c.getIDs()));
    }
  }

  @Override
  public int getStyleForDBID(DBID id) {
    for(int i = 0; i < ids.size(); i++) {
      if(ids.get(i).contains(id)) {
        return i + 1;
      }
    }
    return 0;
  }

  @Override
  public int getMaxStyle() {
    return ids.size() + 1;
  }
}