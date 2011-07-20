package de.lmu.ifi.dbs.elki.database;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.utilities.ConstantObject;

/**
 * An AssociationID is used by databases as a unique identifier for specific
 * associations to single objects. Such as label, local similarity measure.
 * There is no association possible without a specific AssociationID defined
 * within this class.
 * <p/>
 * An AssociationID provides also information concerning the class of the
 * associated objects.
 * 
 * @author Arthur Zimek
 * @param <C> the type of the class of the associated object
 */
public class AssociationID<C> extends ConstantObject<AssociationID<C>> {
  /**
   * The standard association id to associate a label to an object.
   */
  public static final AssociationID<String> LABEL = new AssociationID<String>("label", TypeUtil.STRING);

  /**
   * The association id to associate a class (class label) to an object.
   */
  public static final AssociationID<ClassLabel> CLASS = new AssociationID<ClassLabel>("class", TypeUtil.CLASSLABEL);

  /**
   * The association id to associate an external id to an object.
   */
  public static final AssociationID<String> EXTERNAL_ID = new AssociationID<String>("externalID", TypeUtil.STRING);

  /**
   * The Class type related to this AssociationID.
   */
  private SimpleTypeInformation<C> type;

  /**
   * Provides a new AssociationID of the given name and type.
   * <p/>
   * All AssociationIDs are unique w.r.t. their name. An AssociationID provides
   * information of which class the associated objects are.
   * 
   * @param name name of the association
   * @param type class of the objects that are associated under this
   *        AssociationID
   */
  private AssociationID(final String name, final SimpleTypeInformation<C> type) {
    super(name);
    this.type = type;
  }

  /**
   * Returns the type of the AssociationID.
   * 
   * @return the type of the AssociationID
   */
  public SimpleTypeInformation<C> getType() {
    return type;
  }

  /**
   * Returns the AssociationID for the given name if it exists, null otherwise.
   * 
   * @param name the name of the desired AssociationID
   * @return the AssociationID for the given name if it exists, null otherwise
   */
  // We extensively suppress warnings because of compiler differences in what
  // warning they generate here - including "unneeded suppressWarnings". Argh.
  @SuppressWarnings({ "unchecked", "cast", "all" })
  public static AssociationID<?> getAssociationID(final String name) {
    return (AssociationID<?>) AssociationID.lookup(AssociationID.class, name);
  }

  /**
   * Gets or creates the AssociationID for the given name and given type.
   * 
   * @param <C> association class
   * @param name the name
   * @param type the type of the association
   * @return the AssociationID for the given name
   */
  @SuppressWarnings("unchecked")
  public static <C> AssociationID<C> getOrCreateAssociationID(final String name, final SimpleTypeInformation<C> type) {
    AssociationID<C> associationID = (AssociationID<C>) getAssociationID(name);
    if(associationID == null) {
      associationID = new AssociationID<C>(name, type);
    }
    return associationID;
  }

  /**
   * Gets or creates the AssociationID for the given name and given type.
   * Generics version, with relaxed typechecking.
   * 
   * @param <C> association class
   * @param name the name
   * @param type the type of the association
   * @return the AssociationID for the given name
   */
  @SuppressWarnings("unchecked")
  public static <C> AssociationID<C> getOrCreateAssociationIDGenerics(final String name, final SimpleTypeInformation<C> type) {
    AssociationID<C> associationID = (AssociationID<C>) getAssociationID(name);
    if(associationID == null) {
      associationID = new AssociationID<C>(name, type);
    }
    return associationID;
  }

  /**
   * Return the name formatted for use in text serialization
   * 
   * @return uppercased, no whitespace version of the association name.
   */
  public String getLabel() {
    return getName().replace(" ", "_").toUpperCase();
  }
}