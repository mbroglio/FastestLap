package com.the_coffe_coders.fastestlap.source.news;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.the_coffe_coders.fastestlap.domain.news.JuniorNews;
import com.the_coffe_coders.fastestlap.domain.news.News;

import org.junit.Test;

import java.util.List;

public class NewsFetcherTest {

    @Test
    public void testFetchJuniorNewsF2() {
        List<News> news = NewsFetcher.fetchJuniorNews("f2", 1);
        assertNotNull(news);
        assertFalse(news.isEmpty());

        for (News item : news) {
            assertTrue(item instanceof JuniorNews);
            JuniorNews j = (JuniorNews) item;
            org.junit.Assert.assertEquals("f2", j.getSeriesId());
            assertNotNull(item.getTitle());
            assertFalse(item.getTitle().trim().isEmpty());
            assertNotNull(item.getLink());
            assertTrue(item.getLink().startsWith("http"));
        }
    }

    @Test
    public void testFetchJuniorNewsF3() {
        List<News> news = NewsFetcher.fetchJuniorNews("f3", 1);
        assertNotNull(news);
        assertFalse(news.isEmpty());

        for (News item : news) {
            assertTrue(item instanceof JuniorNews);
            JuniorNews j = (JuniorNews) item;
            org.junit.Assert.assertEquals("f3", j.getSeriesId());
            assertNotNull(item.getTitle());
            assertFalse(item.getTitle().trim().isEmpty());
            assertNotNull(item.getLink());
            assertTrue(item.getLink().startsWith("http"));
        }
    }

    @Test
    public void testFetchJuniorNewsPage2F2() {
        List<News> page2News = NewsFetcher.fetchJuniorNewsPage("f2", 2);
        assertNotNull(page2News);
        assertFalse(page2News.isEmpty());

        for (News item : page2News) {
            assertTrue(item instanceof JuniorNews);
            JuniorNews j = (JuniorNews) item;
            org.junit.Assert.assertEquals("f2", j.getSeriesId());
            assertNotNull(item.getTitle());
            assertFalse(item.getTitle().trim().isEmpty());
            assertNotNull(item.getLink());
            assertTrue(item.getLink().startsWith("http"));
        }
    }

    @Test
    public void testJuniorNewsSeriesColor() {
        JuniorNews f2News = new JuniorNews("F2 Title", "https://example.com/f2", "2 hours ago", null, "f2");
        JuniorNews f3News = new JuniorNews("F3 Title", "https://example.com/f3", "3 hours ago", null, "f3");

        org.junit.Assert.assertEquals(com.the_coffe_coders.fastestlap.R.color.formula_2, f2News.getSeriesColorRes());
        org.junit.Assert.assertEquals(com.the_coffe_coders.fastestlap.R.color.formula_3, f3News.getSeriesColorRes());
    }
}
