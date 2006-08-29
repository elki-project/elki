package de.lmu.ifi.dbs.persistent;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Abstract superclass for pages.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class AbstractPage<P extends AbstractPage<P>> extends AbstractLoggable implements Page<P> {

  /**
   * The unique id if this page.
   */
  private Integer id;

  /**
   * The dirty flag of this page.
   */
  private boolean dirty;

  /**
   * The file that stores the pages.
   */
  private PageFile<P> file;

  /**
   * Empty constructor for externalizable interface.
   */
  public AbstractPage() {
    this(null);
  }

  /**
   * Provides a new page object.
   *
   * @param file the page file that stores the pages.
   */
  public AbstractPage(PageFile<P> file) {
    super(LoggingConfiguration.DEBUG);
    this.file = file;
  }

  /**
   * Returns the unique id of this Page.
   *
   * @return the unique id of this Page
   */
  public final Integer getID() {
    return id;
  }

  /**
   * Sets the unique id of this Page.
   *
   * @param id the id to be set
   */
  public final void setID(int id) {
    this.id = id;
  }

  /**
   * Sets the page file of this page.
   *
   * @param file the page file to be set
   */
  public final void setFile(PageFile<P> file) {
    this.file = file;
  }


  /**
   * Returns true if this page is dirty, false otherwise.
   *
   * @return true if this page is dirty, false otherwise
   */
  public final boolean isDirty() {
    return dirty;
  }

  /**
   * Sets the dirty flag of this page.
   *
   * @param dirty the dirty flag to be set
   */
  public final void setDirty(boolean dirty) {
    this.dirty = dirty;
  }

  /**
   * The object implements the writeExternal method to save its contents
   * by calling the methods of DataOutput for its primitive values or
   * calling the writeObject method of ObjectOutput for objects, strings,
   * and arrays.
   *
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   * @serialData Overriding methods should use this tag to describe
   * the data layout of this Externalizable object.
   * List the sequence of element types and, if possible,
   * relate the element to a public/protected field and/or
   * method of this Externalizable class.
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(id);
  }

  /**
   * The object implements the readExternal method to restore its
   * contents by calling the methods of DataInput for primitive
   * types and readObject for objects, strings and arrays.  The
   * readExternal method must read the values in the same sequence
   * and with the same types as were written by writeExternal.
   *
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException    if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being
   *                                restored cannot be found.
   */
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    id = in.readInt();
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object
   */
  public String toString() {
    if (id != null)
      return Integer.toString(id);
    else
      return "null";
  }

  /**
   * Returns the file that stores the pages.
   *
   * @return the file that stores the pages
   */
  public final PageFile<P> getFile() {
    return file;
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   *
   * @param o the object to be tested
   * @return true, if o is an AbstractNode and has the same
   *         id and the same entries as this node.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final AbstractPage that = (AbstractPage) o;

    return id.equals(that.id);
  }

  /**
   * Returns as hash code value for this node the id of this node.
   *
   * @return the id of this node
   */
  public int hashCode() {
    return id.hashCode();
  }
}


