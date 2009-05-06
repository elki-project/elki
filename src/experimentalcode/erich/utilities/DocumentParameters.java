package experimentalcode.erich.utilities;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionHandler;
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
    // title
    Element title = htmldoc.createElement(HTML_TITLE_TAG);
    title.setTextContent("Command line parameter overview.");
    head.appendChild(title);
    // body
    Element body = htmldoc.createElement(HTML_BODY_TAG);
    htmldoc.getDocumentElement().appendChild(body);

    // Heading
    Element h1 = htmldoc.createElement(HTML_H1_TAG);
    h1.setTextContent("ELKI command line parameter overview:");
    body.appendChild(h1);
    
    // Main definition list
    Element maindl = htmldoc.createElement(HTML_DL_TAG);
    body.appendChild(maindl);
    
    for (Entry<Class<?>, List<Option<?>>> e : byclass.entrySet()) {
      Element classdt = htmldoc.createElement(HTML_DT_TAG);
      classdt.setTextContent("{@link " + e.getKey().getName() + "}");
      maindl.appendChild(classdt);
      Element classdd = htmldoc.createElement(HTML_DD_TAG);
      maindl.appendChild(classdd);
      Element classdl = htmldoc.createElement(HTML_DL_TAG);
      classdd.appendChild(classdl);
      for (Option<?> opt : e.getValue()) {
        Element elemdt = htmldoc.createElement(HTML_DT_TAG);
        Element elemtt = htmldoc.createElement(HTML_TT_TAG);
        elemtt.setTextContent(OptionHandler.OPTION_PREFIX + opt.getName());
        elemdt.appendChild(elemtt);
        classdl.appendChild(elemdt);
        Element elemdd = htmldoc.createElement(HTML_DD_TAG);
        //elemdd.setTextContent(opt.getDescription());
        int state = 0;
        for (String line : opt.getDescription().split("\n")) {
          if (state == 1) {
            elemdd.appendChild(htmldoc.createElement(HTML_BR_TAG));
          }
          Text le = htmldoc.createTextNode(line);
          elemdd.appendChild(le);
          state = 1;
        }
        classdl.appendChild(elemdd);
      }
    }
    
    OutputStream out = System.out;
    // TODO embed linked images.
    javax.xml.transform.Result result = new StreamResult(out);
    // Use a transformer for pretty printing
    Transformer xformer;
    try {
      xformer = TransformerFactory.newInstance().newTransformer();
      xformer.setOutputProperty(OutputKeys.INDENT, "yes");
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
}
