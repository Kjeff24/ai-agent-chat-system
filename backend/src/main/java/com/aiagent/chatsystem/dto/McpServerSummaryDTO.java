package com.aiagent.chatsystem.dto;

/**
 * Summary of an MCP server (name, url, source: static from config or dynamic from API).
 */
public class McpServerSummaryDTO {

    private String name;
    private String url;
    private String source; // "static" | "dynamic"

    public McpServerSummaryDTO() {
    }

    public McpServerSummaryDTO(String name, String url, String source) {
        this.name = name;
        this.url = url;
        this.source = source;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
