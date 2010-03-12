package de.lmu.ifi.dbs.elki.data;

/**
 * A simple class label casting a String as it is as label.
 * 
 * @author Arthur Zimek
 */
public class SimpleClassLabel extends ClassLabel {
  /**
   * Holds the String designating the label.
   */
  private String label;

  /**
   * @see ClassLabel#ClassLabel()
   */
  public SimpleClassLabel() {
    super();
  }

  /**
   * Provides a simple class label covering the given String.
   * 
   * @param label the String to be cast as label
   */
  @Override
  public void init(String label) {
    this.label = label;
  }

  /**
   * The ordering of two SimpleClassLabels is given by the ordering on the
   * Strings they represent.
   * <p/>
   * That is, the result equals <code>this.label.compareTo(o.label)</code>.
   */
  public int compareTo(ClassLabel o) {
    SimpleClassLabel other = (SimpleClassLabel) o;
    return this.label.compareTo(other.label);
  }

  /**
   * The hash code of a simple class label is the hash code of the String
   * represented by the ClassLabel.
   */
  @Override
  public int hashCode() {
    return label.hashCode();
  }

  /**
   * Any ClassLabel should ensure a natural ordering that is consistent with
   * equals. Thus, if <code>this.compareTo(o)==0</code>, then
   * <code>this.equals(o)</code> should be <code>true</code>.
   * 
   * @param o an object to test for equality w.r.t. this ClassLabel
   * @return true, if <code>this==obj || this.compareTo(o)==0</code>, false
   *         otherwise
   */
  @Override
  public boolean equals(Object o) {
    if(this == o) {
      return true;
    }
    if(o == null || getClass() != o.getClass()) {
      return false;
    }
    if(!super.equals(o)) {
      return false;
    }
    final SimpleClassLabel that = (SimpleClassLabel) o;

    return label.equals(that.label);
  }

  /**
   * Returns a new instance of the String covered by this SimpleClassLabel.
   * 
   * @return a new instance of the String covered by this SimpleClassLabel
   */
  @Override
  public String toString() {
    return label;
  }
}