package experimentalcode.erich.newdblayer.ids;

/**
 * DBID Utility functions.
 * 
 * @author Erich Schubert
 */
public final class DBIDUtil {
  /**
   * Static - no public constructor.
   */
  private DBIDUtil() {
    // Never called.
  }

  /**
   * Import an Integer DBID.
   * 
   * @param id Integer ID
   * @return DBID
   */
  public static DBID importInteger(int id) {
    return DBIDFactory.FACTORY.importInteger(id);
  }

  /**
   * Generate a single DBID
   * 
   * @return A single DBID
   */
  public static DBID generateSingleDBID() {
    return DBIDFactory.FACTORY.generateSingleDBID();
  }

  /**
   * Return a single DBID for reuse.
   * 
   * @param id DBID to deallocate
   */
  public static void deallocateSingleDBID(DBID id) {
    DBIDFactory.FACTORY.deallocateSingleDBID(id);
  }

  /**
   * Generate a static DBID range.
   * 
   * @param size Requested size
   * @return DBID range
   */
  public static StaticArrayDBIDs generateStaticDBIDRange(int size) {
    return DBIDFactory.FACTORY.generateStaticDBIDRange(size);
  }

  /**
   * Deallocate a static DBID range.
   * 
   * @param range Range to deallocate
   */
  public static void deallocateDBIDRange(StaticArrayDBIDs range) {
    DBIDFactory.FACTORY.deallocateDBIDRange(range);
  }

  /**
   * Make a new (modifiable) array of DBIDs.
   * 
   * @return New array
   */
  public static ArrayModifiableDBIDs newArray() {
    return DBIDFactory.FACTORY.newArray();
  }

  /**
   * Make a new (modifiable) hash set of DBIDs.
   * 
   * @return New hash set
   */
  public static HashSetModifiableDBIDs newHashSet() {
    return DBIDFactory.FACTORY.newHashSet();
  }

  /**
   * Make a new (modifiable) tree set of DBIDs.
   * 
   * @return New tree set
   */
  public static TreeSetModifiableDBIDs newTreeSet() {
    return DBIDFactory.FACTORY.newTreeSet();
  }

  /**
   * Make a new (modifiable) array of DBIDs.
   * 
   * @param size Size hint
   * @return New array
   */
  public static ArrayModifiableDBIDs newArray(int size) {
    return DBIDFactory.FACTORY.newArray(size);
  }

  /**
   * Make a new (modifiable) hash set of DBIDs.
   * 
   * @param size Size hint
   * @return New hash set
   */
  public static HashSetModifiableDBIDs newHashSet(int size) {
    return DBIDFactory.FACTORY.newHashSet(size);
  }

  /**
   * Make a new (modifiable) tree set of DBIDs.
   * 
   * @param size Size hint
   * @return New tree set
   */
  public static TreeSetModifiableDBIDs newTreeSet(int size) {
    return DBIDFactory.FACTORY.newTreeSet(size);
  }

  /**
   * Make a new (modifiable) array of DBIDs.
   * 
   * @param existing Existing DBIDs
   * @return New array
   */
  public static ArrayModifiableDBIDs newArray(DBIDs existing) {
    return DBIDFactory.FACTORY.newArray(existing);
  }

  /**
   * Make a new (modifiable) hash set of DBIDs.
   * 
   * @param existing Existing DBIDs
   * @return New hash set
   */
  public static HashSetModifiableDBIDs newHashSet(DBIDs existing) {
    return DBIDFactory.FACTORY.newHashSet(existing);
  }

  /**
   * Make a new (modifiable) tree set of DBIDs.
   * 
   * @param existing Existing DBIDs
   * @return New tree set
   */
  public static TreeSetModifiableDBIDs newTreeSet(DBIDs existing) {
    return DBIDFactory.FACTORY.newTreeSet(existing);
  }
}