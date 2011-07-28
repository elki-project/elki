package de.lmu.ifi.dbs.elki.data;

/**
 * External ID objects.
 * 
 * @author Erich Schubert
 */
public final class ExternalID {
  /**
   * Object name
   */
  private final String name;

  /**
   * Constructor.
   * 
   * @param name
   */
  public ExternalID(String name) {
    super();
    assert (name != null);
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null) {
      return false;
    }
    if(getClass() != obj.getClass()) {
      return false;
    }
    ExternalID other = (ExternalID) obj;
    return name.equals(other.name);
  }
}