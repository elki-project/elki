package de.lmu.ifi.dbs.elki.data.model;

/**
 * Base interface for Model classes.
 * 
 * @author Erich Schubert
 *
 */
public interface Model {
  /**
   * Get a suggested label for this model (e.g. "Correlation Cluster") used when
   * automatically generating group names.
   */
  // TODO: remove, collect into a NamingScheme object?
  public String getSuggestedLabel();
}
