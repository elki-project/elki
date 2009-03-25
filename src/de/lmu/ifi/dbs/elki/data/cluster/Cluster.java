package de.lmu.ifi.dbs.elki.data.cluster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.dbs.elki.data.AbstractDatabaseObject;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroup;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Generic cluster class, that may or not have hierarchical information.
 * Note that every cluster MUST have a DatabaseObjectGroup, since it implements
 * the interface, too. Calls to the interface are proxied to the inner group object.
 * 
 * A hierarchy object of class SimpleHierarchy will be created automatically when
 * a list of parents and children is provided. Alternatively, a pre-existing
 * hierarchy object can be provided, e.g. when there is a single hierarchy object used
 * for keeping all the hierarchy information in one object.
 * 
 * @param <M> Model object type
 * 
 * @author Erich Schubert
 */
// TODO: disallow clusters without a DatabaseObjectGroup?
// TODO: remove the DatabaseObjectGroup interface to avoid confusion?
// TODO: add Model interface and delegations consequently since we have the group delegators?
public class Cluster<M extends Model> extends AbstractDatabaseObject implements HierarchyInterface<Cluster<M>>, DatabaseObjectGroup, TextWriteable {
  /**
   * Object that the hierarchy management is delegated to.
   */
  private HierarchyImplementation<Cluster<M>> hierarchy = null;

  /**
   * Cluster name.
   */
  protected String name = null;

  /**
   * Cluster data.
   */
  private DatabaseObjectGroup group = null;

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
   * @param group Group data
   * @param noise Noise flag
   * @param model Model. May be null.
   * @param hierarchy Hierarchy object. May be null.
   */
  public Cluster(String name, DatabaseObjectGroup group, boolean noise, M model, HierarchyImplementation<Cluster<M>> hierarchy) {
    super();
    // TODO: any way to check that this is a C? (see asC() method)
    this.name = name;
    this.group = group;
    this.noise = noise;
    this.model = model;
    this.hierarchy = hierarchy;
  }

  /**
   * Constructor with hierarchy information.
   * A new FullHierarchy object will be created to store the hierarchy information.
   * 
   * @param name Cluster name. May be null.
   * @param group Group data
   * @param noise Noise flag
   * @param model Model. May be null.
   * @param children Children. Will NOT be copied.
   * @param parents Parents. Will NOT be copied.
   */
  public Cluster(String name, DatabaseObjectGroup group, boolean noise, M model, List<Cluster<M>> children, List<Cluster<M>> parents) {
    this(name, group, noise, model, null);
    this.setHierarchy(new SimpleHierarchy<Cluster<M>>(this, children, parents));
  }

  /**
   * Constructor without hierarchy information. 
   * 
   * @param name
   * @param group
   * @param noise Noise flag
   */
  public Cluster(String name, DatabaseObjectGroup group, boolean noise, M model) {
    this(name, group, noise, model, null);
  }

  /**
   * Constructor without hierarchy information. 
   * 
   * @param name
   * @param group
   */
  public Cluster(String name, DatabaseObjectGroup group, M model) {
    this(name, group, false, model, null);
  }

  /**
   * Constructor without hierarchy information and name 
   * 
   * @param group
   * @param noise Noise flag
   * @param model Model
   */
  public Cluster(DatabaseObjectGroup group, boolean noise, M model) {
    this(null, group, noise, model, null);
  }

  /**
   * Constructor without hierarchy information and name 
   * 
   * @param group
   */
  public Cluster(DatabaseObjectGroup group, M model) {
    this(null, group, false, model, null);
  }

  /**
   * Constructor without hierarchy information and model 
   * 
   * @param name name
   * @param group
   * @param noise Noise flag
   */
  public Cluster(String name, DatabaseObjectGroup group, boolean noise) {
    this(name, group, noise, null, null);
  }

  /**
   * Constructor without hierarchy information and model 
   * 
   * @param name
   * @param group
   */
  public Cluster(String name, DatabaseObjectGroup group) {
    this(name, group, false, null, null);
  }

  /**
   * Constructor without hierarchy information and name and model 
   * 
   * @param group
   * @param noise Noise flag
   */
  public Cluster(DatabaseObjectGroup group, boolean noise) {
    this(null, group, noise, null, null);
  }
  
  /**
   * Constructor without hierarchy information and name and model 
   * 
   * @param group
   */
  public Cluster(DatabaseObjectGroup group) {
    this(null, group, false, null, null);
  }

  /**
   * Constructor with hierarchy but noise flag defaulting to false.
   * 
   * @param name Cluster name. May be null.
   * @param group Group data
   * @param model Model. May be null.
   * @param hierarchy Hierarchy object. May be null.
   */
  public Cluster(String name, DatabaseObjectGroup group, M model, HierarchyImplementation<Cluster<M>> hierarchy) {
    this(name, group, false, model, hierarchy);
  }

  /**
   * Constructor with hierarchy information, but no noise flag.
   * A new FullHierarchy object will be created to store the hierarchy information.
   * 
   * @param name Cluster name. May be null.
   * @param group Group data
   * @param model Model. May be null.
   * @param children Children. Will NOT be copied.
   * @param parents Parents. Will NOT be copied.
   */
  public Cluster(String name, DatabaseObjectGroup group, M model, List<Cluster<M>> children, List<Cluster<M>> parents) {
    this(name, group, false, model, null);
    this.setHierarchy(new SimpleHierarchy<Cluster<M>>(this, children, parents));
  }


  /**
   * Test hierarchy
   */
  @Override
  public final boolean isHierarchical() {
    if (hierarchy == null) {
      return false;
    }
    return hierarchy.isHierarchical();
  }
  
  /**
   * Delegate to hierarchy object
   */
  @Override
  public int numChildren() {
    if (hierarchy == null) {
      return 0;
    }
    return hierarchy.numChildren(this);
  }

  /**
   * Delegate to hierarchy object
   */
  @Override
  public List<Cluster<M>> getChildren() {
    if (hierarchy == null) {
      return new ArrayList<Cluster<M>>(0);
    }
    return hierarchy.getChildren(this);
  }

  /**
   * Delegate to hierarchy object
   */
  @Override
  public <T extends Collection<Cluster<M>>> T getDescendants(T collection) {
    if (hierarchy == null) {
      return collection;
    }
    return hierarchy.getDescendants(this, collection);
  }
  
  /**
   * Collect descendants
   */
  public Set<Cluster<M>> getDescendants() {
    return getDescendants(new HashSet<Cluster<M>>());
  }

  /**
   * Delegate to hierarchy object
   */
  @Override
  public int numParents() {
    if (hierarchy == null) {
      return 0;
    }
    return hierarchy.numParents(this);
  }
  
  /**
   * Delegate to hierarchy object
   */
  @Override
  public List<Cluster<M>> getParents() {
    if (hierarchy == null) {
      return new ArrayList<Cluster<M>>(0);
    }
    return hierarchy.getParents(this);
  }

  /**
   * Delegate to hierarchy object
   */
  @Override
  public <T extends Collection<Cluster<M>>> T getAncestors(T collection) {
    if (hierarchy == null) {
      return collection;
    }
    return hierarchy.getAncestors(this, collection);
  }
  
  /**
   * Delegate to database object group.
   * 
   * @return Cluster size retrieved from object group.
   */
  public int size() {
    return group.size();
  }

  /**
   * Delegate to group.
   */
  @Override
  public Collection<Integer> getIDs() {
    return group.getIDs();
  }

  /**
   * Delegate to group.
   */
  @Override
  public Iterator<Integer> iterator() {
    return group.iterator();
  }
  
  /**
   * Get hierarchy object
   * 
   * @return hierarchy object
   */
  public HierarchyImplementation<Cluster<M>> getHierarchy() {
    return hierarchy;
  }

  /**
   * Set hierarchy object
   * 
   * @param hierarchy new hierarchy object
   */
  public void setHierarchy(HierarchyImplementation<Cluster<M>> hierarchy) {
    this.hierarchy = hierarchy;
  }
  
  /**
   * Return either the assigned name or the suggested label
   * 
   * @return a name for the cluster
   */
  public String getNameAutomatic() {
    if (name != null) return name;
    if (isNoise()) {
      return "Noise";
    } else {
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
  public DatabaseObjectGroup getGroup() {
    return group;
  }
  
  /**
   * Access group object
   * 
   * @param g set database object group
   */
  public void setGroup(DatabaseObjectGroup g) {
    group = g;
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
   * Write to a textual representation.
   * Writing the actual group data will be handled by the caller, this
   * is only meant to write the meta information.
   *  
   * @param out output writer stream
   * @param label
   */
  public void writeToText(TextWriterStream out, String label) {
    String name = getNameAutomatic();
    out.commentPrintLn(TextWriterStream.SER_MARKER+" "+ Cluster.class.getName());
    if (name != null) {
      out.commentPrintLn("Name: "+name);
    }
    out.commentPrintLn("Noise flag: "+isNoise());
    out.commentPrintLn("Size: "+group.size());
    // print hierarchy information.
    if (isHierarchical()) {
      out.commentPrint("Parents: ");
      for (int i = 0; i < numParents(); i++) {
        if (i > 0) {
          out.commentPrint(", ");
        }
        out.commentPrint(getParents().get(i).getNameAutomatic());
      }
      out.commentPrintLn();
      out.commentPrint("Children: ");
      for (int i = 0; i < numChildren(); i++) {
        if (i > 0) {
          out.commentPrint(", ");
        }
        out.commentPrint(getChildren().get(i).getNameAutomatic());
      }
      out.commentPrintLn();
    }
    // also print model, if any and printable
    if (getModel() != null) {
      out.commentPrintLn("Model class: "+ getModel().getClass().getName());
      if (getModel() instanceof TextWriteable) {
        ((TextWriteable)getModel()).writeToText(out, label);
      }
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
}
