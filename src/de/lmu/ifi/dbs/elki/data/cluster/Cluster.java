package de.lmu.ifi.dbs.elki.data.cluster;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroup;
import de.lmu.ifi.dbs.elki.data.model.Model;

/**
 * Wrapper, removing the additional C template of the BaseCluster class.
 * 
 * @author Erich Schubert
 *
 * @param <M> Base model class allowed in hierarchy.
 */
public class Cluster<M extends Model> extends BaseCluster<Cluster<M>, M> {
  /**
   * Constructor with model.
   * 
   * @param group
   * @param noise
   * @param model
   */
  public Cluster(DatabaseObjectGroup group, boolean noise, M model) {
    super(group, noise, model);
  }

  /**
   * Constructor with model.
   * 
   * @param group
   * @param model
   */
  public Cluster(DatabaseObjectGroup group, M model) {
    super(group, model);
  }

  /**
   * Constructor without model.
   * 
   * @param group
   * @param noise
   */
  public Cluster(DatabaseObjectGroup group, boolean noise) {
    super(group, noise);
  }

  /**
   * Constructor without model.
   * 
   * @param group
   */
  public Cluster(DatabaseObjectGroup group) {
    super(group);
  }

  /**
   * Constructor with model and hierarchy.
   * 
   * @param name
   * @param group
   * @param model
   * @param hierarchy
   */
  public Cluster(String name, DatabaseObjectGroup group, M model, HierarchyImplementation<Cluster<M>> hierarchy) {
    super(name, group, false, model, hierarchy);
  }

  /**
   * Constructor with model and hierarchy.
   * 
   * @param name
   * @param group
   * @param noise
   * @param model
   * @param hierarchy
   */
  public Cluster(String name, DatabaseObjectGroup group, boolean noise, M model, HierarchyImplementation<Cluster<M>> hierarchy) {
    super(name, group, noise, model, hierarchy);
  }

  /**
   * Constructor with model and hierarchy (given as list of children and parents)
   * 
   * @param name
   * @param group
   * @param model
   * @param children
   * @param parents
   */
  public Cluster(String name, DatabaseObjectGroup group, M model, List<Cluster<M>> children, List<Cluster<M>> parents) {
    super(name, group, false, model, children, parents);
  }

  /**
   * Constructor with model and hierarchy (given as list of children and parents)
   * 
   * @param name
   * @param group
   * @param noise
   * @param model
   * @param children
   * @param parents
   */
  public Cluster(String name, DatabaseObjectGroup group, boolean noise, M model, List<Cluster<M>> children, List<Cluster<M>> parents) {
    super(name, group, noise, model, children, parents);
  }

  /**
   * Constructor with name, group and model.
   * 
   * @param name
   * @param group
   * @param noise
   * @param model
   */
  public Cluster(String name, DatabaseObjectGroup group, boolean noise, M model) {
    super(name, group, noise, model);
  }

  /**
   * Constructor with name, group and model.
   * 
   * @param name
   * @param group
   * @param model
   */
  public Cluster(String name, DatabaseObjectGroup group, M model) {
    super(name, group, model);
  }

  /**
   * Constructor with name and group, no model.
   * 
   * @param name
   * @param group
   * @param noise
   */
  public Cluster(String name, DatabaseObjectGroup group, boolean noise) {
    super(name, group, noise);
  }

  /**
   * Constructor with name and group, no model.
   * 
   * @param name
   * @param group
   */
  public Cluster(String name, DatabaseObjectGroup group) {
    super(name, group);
  }
}
