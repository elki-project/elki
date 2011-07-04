package de.lmu.ifi.dbs.elki.persistent;

import java.io.Externalizable;

/**
 * Defines the requirements for objects that can be stored in a cache and can be
 * persistently saved.
 * 
 * @author Elke Achtert
 */
public interface Page extends Externalizable {
  /**
   * Returns the unique id of this Page.
   * 
   * @return the unique id of this Page. May be {@code null}.
   */
  Integer getPageID();

  /**
   * Sets the unique id of this Page.
   * 
   * @param id the id to be set
   */
  void setPageID(int id);

  /**
   * Returns true if this page is dirty, false otherwise.
   * 
   * @return true if this page is dirty, false otherwise
   */
  boolean isDirty();

  /**
   * Sets the dirty flag of this page.
   * 
   * @param dirty the dirty flag to be set
   */
  void setDirty(boolean dirty);
}