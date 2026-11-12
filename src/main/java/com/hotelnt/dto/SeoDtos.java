package com.hotelnt.dto;

public final class SeoDtos {
    private SeoDtos() {}

    public record SeoResponse(
            String metaTitle,
            String metaDescription,
            String slugUrl,
            String ogTitle,
            String ogImageUrl) {}
}
