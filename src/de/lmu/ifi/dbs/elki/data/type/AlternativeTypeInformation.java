package de.lmu.ifi.dbs.elki.data.type;


/**
 * Class that combines multiple type restrictions into one using the "or"
 * operator.
 * 
 * @author Erich Schubert
 */
public class AlternativeTypeInformation implements TypeInformation {
  /**
   * The wrapped type restrictions
   */
  private final TypeInformation[] restrictions;

  /**
   * Constructor.
   * 
   * @param restrictions
   */
  public AlternativeTypeInformation(TypeInformation... restrictions) {
    super();
    this.restrictions = restrictions;
  }

  @Override
  public boolean isAssignableFromType(TypeInformation type) {
    for(int i = 0; i < restrictions.length; i++) {
      if(restrictions[i].isAssignableFromType(type)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isAssignableFrom(Object other) {
    for(int i = 0; i < restrictions.length; i++) {
      if(restrictions[i].isAssignableFrom(other)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    for(int i = 0; i < restrictions.length; i++) {
      if(i > 0) {
        buf.append(" OR ");
      }
      buf.append(restrictions[i].toString());
    }
    return buf.toString();
  }
}