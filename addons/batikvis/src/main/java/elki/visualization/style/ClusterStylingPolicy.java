/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.visualization.style;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import elki.clustering.em.EM;
import elki.clustering.kmeans.FuzzyCMeans;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.datastore.memory.MemoryDataStoreFactory;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.relation.MaterializedRelation;
import elki.logging.LoggingUtil;
import elki.result.Metadata;
import elki.utilities.datastructures.iterator.It;
import elki.visualization.colors.ColorLibrary;
import elki.visualization.svg.SVGUtil;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Styling policy based on cluster membership.
 * <p>
 * TODO: allow cycling though the different intensity transformations.
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
public class ClusterStylingPolicy implements ClassStylingPolicy {
  /**
   * Intensity transformation functions
   * 
   * @author Erich Schubert
   */
  public static enum IntensityTransform {
    MAXLINEAR {
      @Override
      public double transform(double[] probs) {
        double max = probs[0];
        for(int i = 1; i < probs.length; i++) {
          double d = probs[i];
          max = d > max ? d : max;
        }
        return max;
      }
    },
    SCALEDLINEAR {
      @Override
      public double transform(double[] probs) {
        double max = probs[0];
        for(int i = 1; i < probs.length; i++) {
          double d = probs[i];
          max = d > max ? d : max;
        }
        return max;
      }

      @Override
      public double scale(double v, double min, double max) {
        return min < max ? (v - min) / (max - min) : 1;
      }
    },
    MAXQUADRATIC {
      @Override
      public double transform(double[] probs) {
        double max = probs[0];
        for(int i = 1; i < probs.length; i++) {
          double d = probs[i];
          max = d > max ? d : max;
        }
        return max * max;
      }
    },
    MAX2QUOTIENT {
      @Override
      public double transform(double[] probs) {
        double max = probs[0], max2 = Double.NEGATIVE_INFINITY;
        for(int i = 1; i < probs.length; i++) {
          double d = probs[i];
          if(d > max) {
            max2 = max;
            max = d;
          }
          else if(d > max2) {
            max2 = d;
          }
        }
        return 1 - (max2 / max);
      }
    },
    MAX2QUOTIENTQUAD {
      @Override
      public double transform(double[] probs) {
        double max = probs[0], max2 = Double.NEGATIVE_INFINITY;
        for(int i = 1; i < probs.length; i++) {
          double d = probs[i];
          if(d > max) {
            max2 = max;
            max = d;
          }
          else if(d > max2) {
            max2 = d;
          }
        }
        final double v = 1 - max2 / max;
        return v * v;
      }
    },
    MAX2SUBTRACT {
      @Override
      public double transform(double[] probs) {
        double max = probs[0], max2 = Double.NEGATIVE_INFINITY;
        for(int i = 1; i < probs.length; i++) {
          double d = probs[i];
          if(d > max) {
            max2 = max;
            max = d;
          }
          else if(d > max2) {
            max2 = d;
          }
        }
        return max - max2;
      }
    };

    /**
     * Transform the intensity values.
     *
     * @param probs Probabilities / weights
     * @return transformed intensity value
     */
    public abstract double transform(double[] probs);

    /**
     * Additional scaling with minimum and maximum (default: none).
     *
     * @param v Value
     * @param min Minimum input
     * @param max Maximum input
     * @return
     */
    public double scale(double v, double min, double max) {
      return v;
    }
  }

  /**
   * Intensity transformation
   */
  IntensityTransform inttrans = IntensityTransform.MAX2QUOTIENTQUAD;

  /**
   * Object IDs.
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
   * Maps an ID to its best assignment value.
   */
  WritableDoubleDataStore intensities = null;

  /**
   * Intensity scaling
   */
  double minint = Double.POSITIVE_INFINITY, maxint = Double.NEGATIVE_INFINITY;

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
      if(col == null) {
        LoggingUtil.warning("Unrecognized color name: " + colorset.getColor(i));
        continue;
      }
      colors.add(col.getRGB());
      if(!ci.hasNext()) {
        break;
      }
    }
    // Try to find a soft clustering, currently EM only.
    @SuppressWarnings("rawtypes")
    It<MaterializedRelation> iter = Metadata.hierarchyOf(clustering).iterChildren()//
        .filter(MaterializedRelation.class)//
        .filter(mr -> mr.getDataTypeInformation() == EM.SOFT_TYPE //
            || mr.getDataTypeInformation() == FuzzyCMeans.SOFT_TYPE);
    if(iter.valid()) {
      @SuppressWarnings("unchecked")
      MaterializedRelation<double[]> softAssignments = iter.get();
      DBIDs data = softAssignments.getDBIDs();
      intensities = new MemoryDataStoreFactory().makeDoubleStorage(data, data.size());
      for(DBIDIter it = softAssignments.iterDBIDs(); it.valid(); it.advance()) {
        double[] probs = softAssignments.get(it);
        double v = probs.length > 1 ? inttrans.transform(probs) : probs.length == 1 ? probs[0] : 0.;
        intensities.put(it, v);
        maxint = v > maxint ? v : maxint;
        minint = v < minint ? v : minint;
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
  public double getIntensityForDBID(DBIDRef id) {
    return intensities == null ? 1 : inttrans.scale(intensities.doubleValue(id), minint, maxint);
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
    return Metadata.of(clustering).getLongName();
  }
}
