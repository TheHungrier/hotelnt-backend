package com.hotelnt.helper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UrlHelperTest {

    @Test
    @DisplayName("toPublicUrl: đường dẫn null hoặc rỗng trả về nguyên dạng")
    void toPublicUrl_nullOrBlank_returnsAsIs() {
        assertThat(UrlHelper.toPublicUrl("https://example.com/api", null)).isNull();
        assertThat(UrlHelper.toPublicUrl("https://example.com/api", "")).isEmpty();
        assertThat(UrlHelper.toPublicUrl("https://example.com/api", "   ")).isEqualTo("   ");
    }

    @Test
    @DisplayName("toPublicUrl: đường dẫn đã là URL tuyệt đối (http/https) giữ nguyên")
    void toPublicUrl_absoluteUrl_returnsAsIs() {
        assertThat(UrlHelper.toPublicUrl("https://example.com/api", "https://cdn.example.com/photo.jpg"))
                .isEqualTo("https://cdn.example.com/photo.jpg");
        assertThat(UrlHelper.toPublicUrl("https://example.com/api", "http://cdn.example.com/photo.jpg"))
                .isEqualTo("http://cdn.example.com/photo.jpg");
    }

    @Test
    @DisplayName("toPublicUrl: baseUrl null hoặc rỗng trả về đường dẫn tương đối nguyên dạng")
    void toPublicUrl_noBaseUrl_returnsRelativePathAsIs() {
        assertThat(UrlHelper.toPublicUrl(null, "/uploads/photo.jpg")).isEqualTo("/uploads/photo.jpg");
        assertThat(UrlHelper.toPublicUrl("", "/uploads/photo.jpg")).isEqualTo("/uploads/photo.jpg");
        assertThat(UrlHelper.toPublicUrl("   ", "uploads/photo.jpg")).isEqualTo("uploads/photo.jpg");
    }

    @Test
    @DisplayName("toPublicUrl: ghép chuẩn xác baseUrl và đường dẫn tương đối (xử lý dấu gạch chéo dư thừa)")
    void toPublicUrl_concatenatesCorrectly() {
        assertThat(UrlHelper.toPublicUrl("https://host.com/api", "/uploads/photo.jpg"))
                .isEqualTo("https://host.com/api/uploads/photo.jpg");
        assertThat(UrlHelper.toPublicUrl("https://host.com/api/", "/uploads/photo.jpg"))
                .isEqualTo("https://host.com/api/uploads/photo.jpg");
        assertThat(UrlHelper.toPublicUrl("https://host.com/api", "uploads/photo.jpg"))
                .isEqualTo("https://host.com/api/uploads/photo.jpg");
        assertThat(UrlHelper.toPublicUrl("https://host.com/api///", "///uploads/photo.jpg"))
                .isEqualTo("https://host.com/api/uploads/photo.jpg");
    }
}
