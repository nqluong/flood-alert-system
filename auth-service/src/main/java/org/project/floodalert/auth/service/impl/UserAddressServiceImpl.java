package org.project.floodalert.auth.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.auth.dto.request.UserAddressRequest;
import org.project.floodalert.auth.dto.response.UserAddressResponse;
import org.project.floodalert.auth.exception.AuthErrorCode;
import org.project.floodalert.auth.model.UserAddress;
import org.project.floodalert.auth.repository.UserAddressRepository;
import org.project.floodalert.auth.repository.UserRepository;
import org.project.floodalert.auth.service.UserAddressService;
import org.project.floodalert.common.exception.AppException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserAddressServiceImpl implements UserAddressService {

    UserAddressRepository userAddressRepository;
    UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserAddressResponse> getUserAddresses(UUID userId) {
        validateUserExists(userId);

        return userAddressRepository.findByUserId(userId)
                .stream()
                .map(this::mapToAddressResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserAddressResponse getUserAddressById(UUID userId, UUID addressId) {
        UserAddress address = findAddressByIdAndUserId(addressId, userId);
        return mapToAddressResponse(address);
    }

    @Override
    public UserAddressResponse createAddress(UUID userId, UserAddressRequest addressRequest) {
        validateUserExists(userId);

        UserAddress address = UserAddress.builder()
                .userId(userId)
                .addressText(addressRequest.getAddressText())
                .lat(BigDecimal.valueOf(addressRequest.getLat()))
                .lon(BigDecimal.valueOf(addressRequest.getLon()))
                .isPrimary(addressRequest.getIsPrimary() != null ? addressRequest.getIsPrimary() : false)
                .addressType(addressRequest.getAddressType())
                .build();

        if(Boolean.TRUE.equals(addressRequest.getIsPrimary())) {
            userAddressRepository.unsetAllPrimaryForUser(userId);
        }
        UserAddress savedAddress = userAddressRepository.save(address);
        return mapToAddressResponse(savedAddress);
    }

    @Override
    public UserAddressResponse updateAddress(UUID userId, UUID addressId, UserAddressRequest addressRequest) {
        UserAddress address = findAddressByIdAndUserId(addressId, userId);

        address.setAddressText(addressRequest.getAddressText());
        address.setLat(BigDecimal.valueOf(addressRequest.getLat()));
        address.setLon(BigDecimal.valueOf(addressRequest.getLon()));
        address.setAddressType(addressRequest.getAddressType());

        if(addressRequest.getIsPrimary() != null && addressRequest.getIsPrimary()) {
            userAddressRepository.unsetPrimaryForUser(userId, addressId);
            address.setIsPrimary(true);

        }
        UserAddress updatedAddress = userAddressRepository.save(address);
        return mapToAddressResponse(updatedAddress);
    }

    @Override
    public void deleteAddress(UUID userId, UUID addressId) {
        UserAddress address = findAddressByIdAndUserId(addressId, userId);
        userAddressRepository.delete(address);
    }

    @Override
    public UserAddressResponse setPrimaryAddress(UUID userId, UUID addressId) {
        UserAddress address = findAddressByIdAndUserId(addressId, userId);
        userAddressRepository.unsetPrimaryForUser(userId, addressId);

        address.setIsPrimary(true);
        UserAddress updatedAddress = userAddressRepository.save(address);
        return mapToAddressResponse(updatedAddress);
    }

    @Override
    public void adminDeleteAddress(UUID addressId) {
        UserAddress userAddress = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new AppException(
                        AuthErrorCode.ADDRESS_NOT_FOUND,
                        "Địa chỉ với ID " + addressId + " không tồn tại"
                ));
        userAddressRepository.delete(userAddress);
    }

    private void validateUserExists(UUID userId) {
        if(!userRepository.existsById(userId)) {
            throw new AppException(AuthErrorCode.USER_NOT_FOUND, "Người dùng với ID " + userId + " không tồn tại");
        }
    }

    private UserAddress findAddressByIdAndUserId(UUID addressId, UUID userId) {
        return userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AppException(
                        AuthErrorCode.ADDRESS_NOT_FOUND,
                        "Địa chỉ với ID " + addressId + " không tồn tại cho người dùng với ID " + userId
                ));
    }

    private UserAddressResponse mapToAddressResponse(UserAddress address) {
        return UserAddressResponse.builder()
                .id(address.getId().toString())
                .addressText(address.getAddressText())
                .lat(address.getLat().doubleValue())
                .lon(address.getLon().doubleValue())
                .isPrimary(address.getIsPrimary())
                .addressType(address.getAddressType())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }


}
