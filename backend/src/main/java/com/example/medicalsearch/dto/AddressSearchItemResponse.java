package com.example.medicalsearch.dto;

import com.example.medicalsearch.client.NaverMapsGeocodingClient.GeocodedAddress;
import java.math.BigDecimal;

public record AddressSearchItemResponse(
        String address,
        String roadAddress,
        String jibunAddress,
        BigDecimal latitude,
        BigDecimal longitude
) {
    public static AddressSearchItemResponse from(GeocodedAddress address) {
        return new AddressSearchItemResponse(
                address.address(),
                address.roadAddress(),
                address.jibunAddress(),
                address.latitude(),
                address.longitude()
        );
    }
}
