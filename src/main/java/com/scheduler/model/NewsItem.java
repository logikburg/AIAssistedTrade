/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.scheduler.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class NewsItem {

    @Id
    private String id;
    private String title;
    private String link;
    private String guid;
    private String pubDate;
    private String description;
    private String source;
    private String sentimentValue;
    private Boolean hasSentiments;
    private String namedEntity;
    private String content;
    
    public NewsItem() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getPubDate() {
        return pubDate;
    }

    public void setPubDate(String pubDate) {
        this.pubDate = pubDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSentimentValue() {
        return sentimentValue;
    }

    public void setSentimentValue(String sentimentValue) {
        this.sentimentValue = sentimentValue;
    }

    public Boolean getDefinedSentiments() {
        return hasSentiments;
    }

    public void setHasSentiments(Boolean hasSentiments) {
        this.hasSentiments = hasSentiments;
    }

    public String getNamedEntity() {
        return namedEntity;
    }

    public void setNamedEntity(String namedEntity) {
        this.namedEntity = namedEntity;
    }

    public String getContent() {
	return content;
    }

    public void setContent(String content) {
	this.content = content;
    }

    @Override
    public String toString() {
	return String.format("NewsItem[id=%s, title='%s', link='%s']", id, title, link);
    }
}