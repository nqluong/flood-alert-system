package org.project.floodalert.auth.service;

import org.project.floodalert.auth.dto.request.UserAddressRequest;
import org.project.floodalert.auth.dto.response.UserAddressResponse;

import java.util.List;
import java.util.UUID;

public interface UserAddressService {
    List<UserAddressResponse> getUserAddresses(UUID userId);

    UserAddressResponse getUserAddressById(UUID userId, UUID addressId);

    UserAddressResponse createAddress(UUID userId, UserAddressRequest addressRequest);

    UserAddressResponse updateAddress(UUID userId, UUID addressId, UserAddressRequest addressRequest);

    void deleteAddress(UUID userId, UUID addressId);

    UserAddressResponse setPrimaryAddress(UUID userId, UUID addressId);

    void adminDeleteAddress(UUID addressId);
}
