package com.the_coffe_coders.fastestlap.domain.news;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor

public class News {
    private String title;
    private String link;
    private String description;
    private String date;
    private String category;
    private String imageUrl;
}