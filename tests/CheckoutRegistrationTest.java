import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ByteArrayResource;
import org.w3c.dom.Element;

/** Executes the actual registration XML against Spring 6, without SAP dependencies. */
public class CheckoutRegistrationTest {
    public static class Group {
        private final Map<String, Object> steps = new LinkedHashMap<>();
        public Map<String, Object> getCheckoutStepMap() { return steps; }
    }

    public static void main(String[] args) throws Exception {
        var parser = DocumentBuilderFactory.newInstance();
        parser.setNamespaceAware(true);
        parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var document = parser.newDocumentBuilder().parse(Path.of(args[0]).toFile());
        var root = document.getDocumentElement();
        // Keep the registration bean unchanged; provide its two SAP collaborators below.
        for (var child = root.getFirstChild(); child != null;) {
            var next = child.getNextSibling();
            if (!(child instanceof Element element)
                    || !"customCheckoutGroupConfiguration".equals(element.getAttribute("id"))) {
                root.removeChild(child);
            }
            child = next;
        }
        var xml = new ByteArrayOutputStream();
        TransformerFactory.newInstance().newTransformer()
                .transform(new DOMSource(document), new StreamResult(xml));

        var group = new Group();
        var delivery = new Object();
        var payment = new Object();
        var custom = new Object();
        group.steps.put("delivery-method", delivery);
        group.steps.put("payment", payment);
        var factory = new DefaultListableBeanFactory();
        factory.registerSingleton("defaultCheckoutGroup", group);
        factory.registerSingleton("customCheckoutStep", custom);
        new XmlBeanDefinitionReader(factory).loadBeanDefinitions(new ByteArrayResource(xml.toByteArray()));
        factory.preInstantiateSingletons();
        if (group.steps.size() != 3 || group.steps.get("delivery-method") != delivery
                || group.steps.get("payment") != payment || group.steps.get("custom-step") != custom) {
            throw new AssertionError("Registration must add one step and preserve existing entries");
        }
        System.out.println("Spring checkout registration test passed");
    }
}
