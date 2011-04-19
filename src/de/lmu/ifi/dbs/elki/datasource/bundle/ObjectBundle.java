package de.lmu.ifi.dbs.elki.datasource.bundle;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;

/**
 * Abstract interface for object packages.
 * 
 * Shared API for both single-object and multi-object packages.
 * 
 * @author Erich Schubert
 */
public interface ObjectBundle {
  /**
   * Access the meta data.
   * 
   * @return metadata
   */
  public BundleMeta meta();

  /**
   * Access the meta data.
   * 
   * @param i component
   * @return metadata of component i
   */
  public SimpleTypeInformation<?> meta(int i);

  /**
   * Get the metadata length.
   * 
   * @return length of metadata
   */
  public int metaLength();

  /**
   * Get the number of objects contained.
   * 
   * @return Number of objects
   */
  public int dataLength();

  /**
   * Access a particular object and representation.
   * 
   * @param onum Object number
   * @param rnum Representation number
   * @return Contained data
   */
  public Object data(int onum, int rnum);
}