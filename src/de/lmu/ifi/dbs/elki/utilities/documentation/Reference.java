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
   * @return
   */
  String title();

  /**
   * Publication Authors
   * 
   * @return
   */
  String authors();

  /**
   * Book title or Journal title etc.
   * 
   * @return
   */
  String booktitle();

  /**
   * Prefix to the reference, e.g. "Generalization of a method proposed in"
   * 
   * @return
   */
  String prefix() default "";
  
  /**
   * Reference URL, e.g. DOI
   * 
   * @return
   */
  String url() default "";
}