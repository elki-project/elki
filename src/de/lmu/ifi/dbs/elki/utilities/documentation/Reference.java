package de.lmu.ifi.dbs.elki.utilities.documentation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify a reference.
 * 
 * @author Erich Schubert
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.TYPE })
public @interface Reference {
  /**
   * Publication title.
   * 
   * @return publication title
   */
  String title();

  /**
   * Publication Authors
   * 
   * @return authors
   */
  String authors();

  /**
   * Book title or Journal title etc.
   * 
   * @return book title
   */
  String booktitle();

  /**
   * Prefix to the reference, e.g. "Generalization of a method proposed in"
   * 
   * @return Prefix or empty string
   */
  String prefix() default "";
  
  /**
   * Reference URL, e.g. DOI
   * 
   * @return Reference URL or empty string
   */
  String url() default "";
}