package com.fusionquery.jdbc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/** Namespace-aware SOAP responses; error text is decoded without expanding external entities. */
final class SoapXml {
    private SoapXml() {}

    /**
     * The JDK's own parser, ignoring JVM-wide JAXP overrides. SQL Developer's
     * ide.conf points the javax.xml factories at Oracle XDK / Woodstox
     * classes: inside an OSGi bundle DocumentBuilderFactory.newInstance()
     * either cannot load them or gets a parser that rejects the Apache
     * security feature names below — so a perfectly valid SOAP reply was
     * reported as "not a valid SOAP response". newDefaultInstance() (Java 9+)
     * sidesteps the override; the Java 8 fallbacks keep the old behaviour.
     */
    private static DocumentBuilderFactory newFactory() {
        try {
            return (DocumentBuilderFactory) DocumentBuilderFactory.class
                    .getMethod("newDefaultInstance").invoke(null);
        } catch (Throwable java8OrBlocked) {
            // fall through
        }
        try {
            return DocumentBuilderFactory.newInstance();
        } catch (Throwable overrideNotLoadable) {
            return DocumentBuilderFactory.newInstance(
                    "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl", null);
        }
    }

    static Document parse(String xml) throws IOException {
        try {
            DocumentBuilderFactory factory = newFactory();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(
                    new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IOException("Invalid XML response from BI Publisher ("
                    + e.getClass().getSimpleName() + ": " + e.getMessage() + ")", e);
        }
    }

    static String text(Document document, String localName) {
        NodeList nodes = document.getElementsByTagNameNS("*", localName);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent().trim();
    }

    static void checkFault(Document document) throws IOException {
        if (document.getElementsByTagNameNS("*", "Fault").getLength() > 0) {
            String message = text(document, "faultstring");
            if (message == null) message = text(document, "Text");
            throw new IOException("SOAP Fault: " + (message == null ? "Unspecified BI Publisher fault" : message));
        }
    }

    static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}
