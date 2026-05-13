package com.example.WonkaoTalk.domain.shipping.dto;

public record ShippingAddressUpdateRequest(
    String recipientName,
    String recipientPhone,
    String zipCode,
    String address1,
    String address2,
    String memo,
    Boolean isDefault
) {

}
