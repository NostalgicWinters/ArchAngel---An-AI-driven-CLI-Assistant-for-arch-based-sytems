package org.archangel.rss;

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
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ArchNewsFetcher {

    @ConfigProperty(name = "archangel.rss.url")
    String ARCH_RSS_URL;

    public List<NewsItem> fetchRSSNews() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(ARCH_RSS_URL)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String xml = response.body();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        doc.getDocumentElement().normalize();

        NodeList items = doc.getElementsByTagName("item");
        List<NewsItem> newsList = new ArrayList<>();

        for (int i = 0; i < items.getLength(); i++) {
            Node node = items.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String title = getTagValue("title", element);
                String link = getTagValue("link", element);
                String description = getTagValue("description", element);
                String pubDate = getTagValue("pubDate", element);

                newsList.add(new NewsItem(title, link, description, pubDate));
            }
        }
        return  newsList;
    }

    private String getTagValue(String tagName, Element elementNeeded){
        NodeList List = elementNeeded.getElementsByTagName(tagName);
        if(List.getLength() > 0){
            return List.item(0).getTextContent();
        }
        return "";
    }
}
