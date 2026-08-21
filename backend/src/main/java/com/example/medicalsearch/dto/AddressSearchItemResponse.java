package com.example.medicalsearch.dto;

import com.example.medicalsearch.client.NaverMapsGeocodingClient.GeocodedAddress;
import com.example.medicalsearch.client.OpenStreetMapStationSearchClient.StationLocation;
import java.math.BigDecimal;

public record AddressSearchItemResponse(
        String address,
        String kind,
        String roadAddress,
        String jibunAddress,
        BigDecimal latitude,
        BigDecimal longitude
) {
    public static AddressSearchItemResponse from(GeocodedAddress address) {
        return new AddressSearchItemResponse(
                address.address(),
                "ADDRESS",
                address.roadAddress(),
                address.jibunAddress(),
                address.latitude(),
                address.longitude()
        );
    }

    public static AddressSearchItemResponse from(StationLocation station) {
        return new AddressSearchItemResponse(
                station.name(),
                "STATION",
                station.name(),
                station.displayName(),
                station.latitude(),
                station.longitude()
        );
    }
}
