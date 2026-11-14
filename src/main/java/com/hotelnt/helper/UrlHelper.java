package com.hotelnt.helper;

public final class UrlHelper {

    private UrlHelper() {
    }

    public static String toPublicUrl(String baseUrl, String path) {
        if (path == null || path.isBlank()) {
            return path;
        }
        if (path.startsWith("http://") || path.startsWith("https://")
                || path.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) {
            return path;
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            return path;
        }
        String cleanBase = baseUrl.replaceAll("/+$", "");
        String cleanPath = path.replaceFirst("^/+", "");
        return cleanBase + "/" + cleanPath;
    }
}
