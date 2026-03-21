package org.archangel.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewsItem {
    private String title;
    private String link;
    private String description;
    private String pubDate;
}