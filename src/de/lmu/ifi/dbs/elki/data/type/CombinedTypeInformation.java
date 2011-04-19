package de.lmu.ifi.dbs.elki.data.type;

/**
 * Class that combines multiple type restrictions into one using an "and" operator.
 * 
 * @author Erich Schubert
 */
public class CombinedTypeInformation implements TypeInformation {
  /**
   * The wrapped type restrictions
   */
  private final TypeInformation[] restrictions;
  
  /**
   * Constructor.
   *
   * @param restrictions
   */
  public CombinedTypeInformation(TypeInformation... restrictions) {
    super();
    this.restrictions = restrictions;
  }

  @Override
  public boolean isAssignableFromType(TypeInformation type) {
    for (int i = 0; i < restrictions.length; i++) {
      if (!restrictions[i].isAssignableFromType(type)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isAssignableFrom(Object other) {
    for (int i = 0; i < restrictions.length; i++) {
      if (!restrictions[i].isAssignableFrom(other)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < restrictions.length; i++) {
      if (i > 0) {
        buf.append(" AND ");
      }
      buf.append(restrictions[i].toString());
    }
    return buf.toString();
  }
}