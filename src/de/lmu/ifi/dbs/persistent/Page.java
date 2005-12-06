package de.lmu.ifi.dbs.persistent;

import java.io.Externalizable;

/**
 * Defines the requirements for objects that can be stored in a cache and can be persistently
 * saved.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface Page<P extends Page> extends Externalizable {

  /**
   * Returns the unique id of this Page.
   *
   * @return the unique id of this Page
   */
  Integer getID();

  /**
   * Sets the unique id of this Page.
   *
   * @param id the id to be set
   */
  void setID(int id);

  /**
   * Sets the page file of this page.
   *
   * @param file the page file to be set
   */
  void setFile(PageFile<P> file);

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
