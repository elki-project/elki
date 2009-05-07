package experimentalcode.erich.utilities;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Comment;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

public class DocumentParameters {

  private static final String HTML_HTML_TAG = "html";
  private static final String HTML_NAMESPACE = "http://www.w3.org/1999/xhtml";
  private static final String HTML_HEAD_TAG = "head";
  private static final String HTML_TITLE_TAG = "title";
  private static final String HTML_BODY_TAG = "body";
  private static final String HTML_DL_TAG = "dl";
  private static final String HTML_DT_TAG = "dt";
  private static final String HTML_DD_TAG = "dd";
  private static final String HTML_TT_TAG = "tt";
  private static final String HTML_BR_TAG = "br";
  private static final String HTML_H1_TAG = "h1";
  private static final String HTML_A_TAG = "a";
  private static final String HTML_HREF_ATTRIBUTE = "href";
  private static final String HTML_NAME_ATTRIBUTE = "name";
  private static final String HTML_P_TAG = "p";
  private static final String HTML_META_TAG = "meta";
  private static final String HTML_LINK_TAG = "link";
  private static final String HTML_DOCTYPE_PUBLIC = "-//W3C//DTD XHTML 1.0 Transitional//EN";
  private static final String HTML_DOCTYPE_SYSTEM = "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd";

  /**
   * @param args
   */
  public static void main(String[] args) {
    List<Pair<Parameterizable, Option<?>>> options = new ArrayList<Pair<Parameterizable, Option<?>>>();
    String[] emptyargs = {};
    for(Class<?> cls : InspectionUtil.findAllImplementations(Parameterizable.class)) {
      try {
        // try to parameterize the class.
        Parameterizable p = (Parameterizable) cls.newInstance();
        try {
          p.setParameters(emptyargs);
        }
        catch(ParameterException e) {
          // this is expected to happen.
        }
        catch(Exception e) {
          // this is expected to happen.
        }
        p.collectOptions(options);
      }
      catch(LinkageError e) {
        continue;
      }
      catch(InstantiationException e) {
        continue;
      }
      catch(IllegalAccessException e) {
        continue;
      }
    }
    
    HashMapList<Class<?>, Option<?>> byclass = new HashMapList<Class<?>, Option<?>>();
    HashMapList<Option<?>, Class<?>> byopt = new HashMapList<Option<?>, Class<?>>();

    for(Pair<Parameterizable, Option<?>> pair : options) {
      Class<?> c = pair.getFirst().getClass();
      Option<?> o = pair.getSecond();
      
      // just collect unique occurrences
      if (!byclass.contains(c, o)) {
        byclass.add(c, o);
      }
      if (!byopt.contains(o, c)) {
        byopt.add(o, c);
      }
    }
    
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder;
    try {
      builder = factory.newDocumentBuilder();
    }
    catch(ParserConfigurationException e1) {
      throw new RuntimeException(e1);
    }
    DOMImplementation impl = builder.getDOMImplementation();
    Document htmldoc = impl.createDocument(HTML_NAMESPACE, HTML_HTML_TAG, null);
    // head
    Element head = htmldoc.createElement(HTML_HEAD_TAG);
    htmldoc.getDocumentElement().appendChild(head);
    // meta with charset information
    Element meta = htmldoc.createElement(HTML_META_TAG);
    meta.setAttribute("http-equiv","Content-Type");
    meta.setAttribute("content","text/html; charset=UTF-8");
    head.appendChild(meta);
    // stylesheet
    Element css = htmldoc.createElement(HTML_LINK_TAG);
    css.setAttribute("rel","stylesheet");
    css.setAttribute("type","text/css");
    css.setAttribute(HTML_HREF_ATTRIBUTE, "stylesheet.css");
    head.appendChild(css);
    // title
    Element title = htmldoc.createElement(HTML_TITLE_TAG);
    title.setTextContent("Command line parameter overview.");
    head.appendChild(title);
    // body
    Element body = htmldoc.createElement(HTML_BODY_TAG);
    htmldoc.getDocumentElement().appendChild(body);
    
    // modification warning
    Comment warn = htmldoc.createComment("WARNING: THIS DOCUMENT IS AUTOMATICALLY GENERATED. MODIFICATIONS MAY GET LOST.");
    body.appendChild(warn);

    // Heading
    Element h1 = htmldoc.createElement(HTML_H1_TAG);
    h1.setTextContent("ELKI command line parameter overview:");
    body.appendChild(h1);
    
    // Main definition list
    Element maindl = htmldoc.createElement(HTML_DL_TAG);
    body.appendChild(maindl);
    
    List<Class<?>> classes = new ArrayList<Class<?>>(byclass.keySet());
    Collections.sort(classes, new SortByName());
    
    for (Class<?> cls : classes) {
      Element classdt = htmldoc.createElement(HTML_DT_TAG);
      Element classan = htmldoc.createElement(HTML_A_TAG);
      classan.setAttribute(HTML_NAME_ATTRIBUTE, cls.getName());
      classdt.appendChild(classan);
      Element classa = htmldoc.createElement(HTML_A_TAG);
      classa.setAttribute(HTML_HREF_ATTRIBUTE, linkForClassName(cls.getName()));
      classa.setTextContent(cls.getName());
      classdt.appendChild(classa);
      maindl.appendChild(classdt);
      Element classdd = htmldoc.createElement(HTML_DD_TAG);
      maindl.appendChild(classdd);
      Element classdl = htmldoc.createElement(HTML_DL_TAG);
      classdd.appendChild(classdl);
      for (Option<?> opt : byclass.get(cls)) {
        Element elemdt = htmldoc.createElement(HTML_DT_TAG);
        Element elemtt = htmldoc.createElement(HTML_TT_TAG);
        elemtt.setTextContent(OptionHandler.OPTION_PREFIX + opt.getName()+" "+opt.getSyntax());
        elemdt.appendChild(elemtt);
        classdl.appendChild(elemdt);
        Element elemdd = htmldoc.createElement(HTML_DD_TAG);
        //elemdd.setTextContent(opt.getDescription());
        int state = 0;
        Element elemp = htmldoc.createElement(HTML_P_TAG);
        for (String line : opt.getShortDescription().split("\n")) {
          if (state == 1) {
            elemp.appendChild(htmldoc.createElement(HTML_BR_TAG));
          }
          Text le = htmldoc.createTextNode(line);
          elemp.appendChild(le);
          state = 1;
        }
        elemdd.appendChild(elemp);
        // class restriction?
        if (opt instanceof ClassParameter<?>) {
          Element p = htmldoc.createElement(HTML_P_TAG);
          p.appendChild(htmldoc.createTextNode("Class Restriction: "));
          Element defa = htmldoc.createElement(HTML_A_TAG);
          defa.setAttribute(HTML_HREF_ATTRIBUTE, linkForClassName(((ClassParameter<?>) opt).getRestrictionClass().getName()));
          defa.setTextContent(((ClassParameter<?>) opt).getRestrictionClass().getName());
          p.appendChild(defa);
          elemdd.appendChild(p);
        }
        // default value? completions?
        if (opt instanceof Parameter<?,?>) {
          Parameter<?,?> par = (Parameter<?, ?>) opt;
          if (par.hasDefaultValue()) {
            Element p = htmldoc.createElement(HTML_P_TAG);
            Object def = par.getDefaultValue();
            p.appendChild(htmldoc.createTextNode("Default: "));
            if (opt instanceof ClassParameter<?>) {
              Element defa = htmldoc.createElement(HTML_A_TAG);
              defa.setAttribute(HTML_HREF_ATTRIBUTE, linkForClassName(((ClassParameter<?>) opt).getDefaultValue()));
              defa.setTextContent(((ClassParameter<?>) opt).getDefaultValue());
              p.appendChild(defa);
            } else {
              p.appendChild(htmldoc.createTextNode(def.toString()));
            }
            elemdd.appendChild(p);
          }
        }
        classdl.appendChild(elemdd);
      }
    }
    
    writeXHTML(htmldoc, System.out);
  }

  private static void writeXHTML(Document htmldoc, OutputStream out) throws Error {
    javax.xml.transform.Result result = new StreamResult(out);
    // Use a transformer for pretty printing
    Transformer xformer;
    try {
      xformer = TransformerFactory.newInstance().newTransformer();
      xformer.setOutputProperty(OutputKeys.INDENT, "yes");
      xformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      xformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, HTML_DOCTYPE_PUBLIC);
      xformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, HTML_DOCTYPE_SYSTEM);
      xformer.transform(new DOMSource(htmldoc), result);
    }
    catch(TransformerException e1) {
      throw new RuntimeException(e1);
    }
    try {
      out.flush();
    }
    catch(IOException e1) {
      throw new RuntimeException(e1);
    }
  }

  private static String linkForClassName(String name) {
    String link = name.replace(".","/") + ".html";
    return link;
  }
  
  protected static class SortByName implements Comparator<Class<?>> {
    @Override
    public int compare(Class<?> o1, Class<?> o2) {
      return o1.getName().compareTo(o2.getName());
    }
  }
}
