package org.archangel.rss;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.archangel.model.NewsItem;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * FIXED:
 * 1. XXE vulnerability: DocumentBuilderFactory now has XXE protections applied.
 *    An attacker controlling the RSS feed (MITM or DNS spoof) could previously
 *    use XML External Entity injection to read local files (e.g., /etc/passwd).
 *
 * 2. HttpClient is now a singleton initialized in @PostConstruct instead of
 *    being recreated on every fetchRSSNews() call (every 10 minutes).
 *    HttpClient holds a thread pool and connection pool internally — recreating
 *    it repeatedly leaks threads and connections over time.
 *
 * 3. Added connection and request timeouts to HttpClient to prevent the scheduler
 *    thread from hanging indefinitely if archlinux.org is slow or unreachable.
 */
@ApplicationScoped
public class ArchNewsFetcher {

    @ConfigProperty(name = "archangel.rss.url")
    String archRssUrl;

    private HttpClient httpClient;
    private DocumentBuilderFactory xmlFactory;

    @PostConstruct
    void init() {
        // Singleton HttpClient — reuse connection pool across scheduler invocations
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        // FIXED: XXE hardening — must be done before any XML parsing
        this.xmlFactory = DocumentBuilderFactory.newInstance();
        try {
            xmlFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            xmlFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            xmlFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            xmlFactory.setXIncludeAware(false);
            xmlFactory.setExpandEntityReferences(false);
        } catch (Exception e) {
            // If the XML parser doesn't support these features, fail fast at startup.
            // Running without XXE protection is not acceptable.
            throw new RuntimeException("Failed to configure secure XML parser", e);
        }
    }

    public List<NewsItem> fetchRSSNews() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(archRssUrl))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("RSS feed returned HTTP " + response.statusCode());
        }

        String xml = response.body();
        DocumentBuilder builder = xmlFactory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        doc.getDocumentElement().normalize();

        NodeList items = doc.getElementsByTagName("item");
        List<NewsItem> newsList = new ArrayList<>();

        for (int i = 0; i < items.getLength(); i++) {
            Node node = items.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                newsList.add(new NewsItem(
                        getTagValue("title", element),
                        getTagValue("link", element),
                        getTagValue("description", element),
                        getTagValue("pubDate", element)
                ));
            }
        }
        return newsList;
    }

    private String getTagValue(String tagName, Element element) {
        NodeList list = element.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            return list.item(0).getTextContent().trim();
        }
        return "";
    }
}