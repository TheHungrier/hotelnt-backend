package com.hotelnt.domain;

/** Gia tri cot CoSoLuuTru.trangThai (khop CHECK trong Liquibase changelog). */
public final class ListingStatus {
    private ListingStatus() {
    }

    public static final String DRAFT = "DRAFT";
    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String HIDDEN = "HIDDEN";
    public static final String REMOVED = "REMOVED";
}
