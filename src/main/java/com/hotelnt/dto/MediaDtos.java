package com.hotelnt.dto;

public final class MediaDtos {
        private MediaDtos() {
        }

        public record MediaUploadResponse(
                        int mediaId, String url, String type, int kichThuoc, int thuTu, int soAnh, int soVideo) {
        }

        public record MediaDeleteResponse(
                        String message, int soAnh, int soVideo, boolean thayDoiTrangThai) {
        }
}
