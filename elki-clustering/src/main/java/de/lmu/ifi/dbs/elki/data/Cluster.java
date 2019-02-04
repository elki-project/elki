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
package de.lmu.ifi.dbs.elki.data;

import java.util.Comparator;

import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Generic cluster class, that may or not have hierarchical information. Note
 * that every cluster MUST have a DBIDs, since it implements the interface, too.
 * Calls to the interface are proxied to the inner group object.
 * 
 * A hierarchy object of class SimpleHierarchy will be created automatically
 * when a list of parents and children is provided. Alternatively, a
 * pre-existing hierarchy object can be provided, e.g. when there is a single
 * hierarchy object used for keeping all the hierarchy information in one
 * object.
 * 
 * @param <M> Model object type
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @opt nodefillcolor LemonChiffon
 * 
 * @composed - - - DBIDs
 * @composed - - - Model
 */
public class Cluster<M extends Model> implements TextWriteable {
  /**
   * Cluster name.
   */
  protected String name = null;

  /**
   * Cluster data.
   */
  private DBIDs ids = null;

  /**
   * Cluster model.
   */
  private M model = null;

  /**
   * Noise?
   */
  private boolean noise = false;

  /**
   * Full constructor
   * 
   * @param name Cluster name. May be null.
   * @param ids Object Group
   * @param noise Noise flag
   * @param model Model. May be null.
   */
  public Cluster(String name, DBIDs ids, boolean noise, M model) {
    super();
    this.name = name;
    this.ids = ids;
    this.noise = noise;
    this.model = model;
  }

  /**
   * Constructor without hierarchy information.
   * 
   * @param name Cluster name. May be null.
   * @param ids Object group
   * @param model Model
   */
  public Cluster(String name, DBIDs ids, M model) {
    this(name, ids, false, model);
  }

  /**
   * Constructor without hierarchy information and name
   * 
   * @param ids Object group
   * @param noise Noise flag
   * @param model Model
   */
  public Cluster(DBIDs ids, boolean noise, M model) {
    this(null, ids, noise, model);
  }

  /**
   * Constructor without hierarchy information and name
   * 
   * @param ids Object group
   * @param model Model
   */
  public Cluster(DBIDs ids, M model) {
    this(null, ids, false, model);
  }

  /**
   * Constructor without hierarchy information and model
   * 
   * @param name Cluster name. May be null.
   * @param ids Object group
   * @param noise Noise flag
   */
  public Cluster(String name, DBIDs ids, boolean noise) {
    this(name, ids, noise, null);
  }

  /**
   * Constructor without hierarchy information and model
   * 
   * @param name Cluster name. May be null.
   * @param ids Object group
   */
  public Cluster(String name, DBIDs ids) {
    this(name, ids, false, null);
  }

  /**
   * Constructor without hierarchy information and name and model
   * 
   * @param ids Cluster name. May be null.
   * @param noise Noise flag
   */
  public Cluster(DBIDs ids, boolean noise) {
    this(null, ids, noise, null);
  }

  /**
   * Constructor without hierarchy information and name and model
   * 
   * @param ids Object group
   */
  public Cluster(DBIDs ids) {
    this(null, ids, false, null);
  }

  /**
   * Delegate to database object group.
   * 
   * @return Cluster size retrieved from object group.
   */
  public int size() {
    return ids.size();
  }

  /**
   * Return either the assigned name or the suggested label
   * 
   * @return a name for the cluster
   */
  public String getNameAutomatic() {
    if(name != null) {
      return name;
    }
    if(isNoise()) {
      return "Noise";
    }
    else {
      return "Cluster";
    }
  }

  /**
   * Get Cluster name. May be null.
   * 
   * @return cluster name, or null
   */
  public String getName() {
    return name;
  }

  /**
   * Set Cluster name
   * 
   * @param name new cluster name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Access group object
   * 
   * @return database object group
   */
  public DBIDs getIDs() {
    return ids;
  }

  /**
   * Access group object
   * 
   * @param g set database object group
   */
  public void setIDs(DBIDs g) {
    ids = g;
  }

  /**
   * Access model object
   * 
   * @return Cluster model
   */
  public M getModel() {
    return model;
  }

  /**
   * Access model object
   * 
   * @param model New cluster model
   */
  public void setModel(M model) {
    this.model = model;
  }

  /**
   * Write to a textual representation. Writing the actual group data will be
   * handled by the caller, this is only meant to write the meta information.
   * 
   * @param out output writer stream
   * @param label Label to prefix
   */
  @Override
  public void writeToText(TextWriterStream out, String label) {
    String name = getNameAutomatic();
    if(name != null) {
      out.commentPrintLn("Cluster name: " + name);
    }
    out.commentPrintLn("Cluster noise flag: " + isNoise());
    out.commentPrintLn("Cluster size: " + ids.size());
    // also print model, if any and printable
    if(getModel() != null && (getModel() instanceof TextWriteable)) {
      ((TextWriteable) getModel()).writeToText(out, label);
    }
  }

  /**
   * Getter for noise flag.
   * 
   * @return noise flag
   */
  public boolean isNoise() {
    return noise;
  }

  /**
   * Setter for noise flag.
   * 
   * @param noise new noise flag value
   */
  public void setNoise(boolean noise) {
    this.noise = noise;
  }

  /**
   * A partial comparator for Clusters, based on their name. Useful for sorting
   * clusters. Do NOT use in e.g. a TreeSet since it is
   * <em>inconsistent with equals</em>.
   */
  public static Comparator<Cluster<?>> BY_NAME_SORTER = new Comparator<Cluster<?>>() {
    @Override
    public int compare(Cluster<?> o1, Cluster<?> o2) {
      if(o1 == o2) {
        return 0;
      }
      if (o1 == null) {
        return +1;
      }
      if (o2 == null) {
        return -1;
      }
      // sort by label if possible
      if(o1.name != o2.name) {
        if (o1.name == null) {
          return +1;
        }
        if (o2.name == null) {
          return -1;
        }
        int lblresult = o1.name.compareTo(o2.name);
        if(lblresult != 0) {
          return lblresult;
        }
      }
      return 0;
    }
  };

  /** {@inheritDoc} */
  @Override
  public String toString() {
    String mstr = (model == null) ? "null" : model.toString();
    String nstr = noise ? ",noise" : "";
    return "Cluster(size=" + size() + ",model=" + mstr + nstr + ")";
  }
}
