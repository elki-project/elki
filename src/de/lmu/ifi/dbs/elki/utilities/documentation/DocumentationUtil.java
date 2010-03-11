package de.lmu.ifi.dbs.elki.utilities.documentation;

/**
 * Utilities for extracting documentation from class annotations.
 * 
 * @author Erich Schubert
 */
public final class DocumentationUtil {
  /**
   * Get a useful title from a class, either by reading the
   * "title" annotation, or by using the class name.
   * 
   * @param c Class
   * @return title
   */
  public static String getTitle(Class<?> c) {
    Title title = c.getAnnotation(Title.class);
    if(title != null && title.value() != "") {
      return title.value();
    }
    return c.getSimpleName();
  }
  
  /**
   * Get a class description if defined, an empty string otherwise.
   * 
   * @param c Class
   * @return description or the emtpy string
   */
  public static String getDescription(Class<?> c) {
    Description desc = c.getAnnotation(Description.class);
    if (desc != null) {
      return desc.value();
    }
    return "";
  }
  
  /**
   * Get the reference annotation of a class, or {@code null}.
   * 
   * @param c Class
   * @return Reference or {@code null}
   */
  public static Reference getReference(Class<?> c) {
    Reference ref = c.getAnnotation(Reference.class);
    return ref;
  }
}