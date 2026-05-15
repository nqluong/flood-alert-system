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
import org.project.floodalert.auth.service.StaticLocationRedisService;
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
    StaticLocationRedisService staticLocationRedisService;

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
    @Transactional(readOnly = true)
    public UserAddressResponse getPrimaryAddress(UUID userId) {
        UserAddress address = userAddressRepository.findPrimaryByUserId(userId)
                .orElseThrow(() -> new AppException(
                        AuthErrorCode.ADDRESS_NOT_FOUND,
                        "Người dùng với ID " + userId + " chưa có địa chỉ chính"
                ));
        return mapToAddressResponse(address);
    }

    @Override
    @Transactional
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
        
        // Cập nhật Redis Geo
        String zoneName = determineZoneName(savedAddress);
        staticLocationRedisService.addOrUpdateLocation(
                userId, 
                savedAddress.getId(), 
                zoneName,
                savedAddress.getLat().doubleValue(),
                savedAddress.getLon().doubleValue()
        );
        log.info("Đã tạo địa chỉ và cập nhật Redis: userId={}, addressId={}, zone={}", 
                userId, savedAddress.getId(), zoneName);
        
        return mapToAddressResponse(savedAddress);
    }

    @Override
    @Transactional
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
        
        // Cập nhật Redis Geo
        String zoneName = determineZoneName(updatedAddress);
        staticLocationRedisService.addOrUpdateLocation(
                userId,
                updatedAddress.getId(),
                zoneName,
                updatedAddress.getLat().doubleValue(),
                updatedAddress.getLon().doubleValue()
        );
        log.info("Đã cập nhật địa chỉ và Redis: userId={}, addressId={}, zone={}", 
                userId, updatedAddress.getId(), zoneName);
        
        return mapToAddressResponse(updatedAddress);
    }

    @Override
    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        UserAddress address = findAddressByIdAndUserId(addressId, userId);
        userAddressRepository.delete(address);
        
        // Xóa khỏi Redis Geo
        staticLocationRedisService.removeLocation(userId, addressId);
        log.info("Đã xóa địa chỉ và cập nhật Redis: userId={}, addressId={}", userId, addressId);
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
    @Transactional
    public void adminDeleteAddress(UUID addressId) {
        UserAddress userAddress = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new AppException(
                        AuthErrorCode.ADDRESS_NOT_FOUND,
                        "Địa chỉ với ID " + addressId + " không tồn tại"
                ));
        userAddressRepository.delete(userAddress);
        
        staticLocationRedisService.removeLocation(userAddress.getUserId(), addressId);
        log.info("Admin đã xóa địa chỉ và cập nhật Redis: userId={}, addressId={}", 
                userAddress.getUserId(), addressId);
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


    private String determineZoneName(UserAddress address) {
        if (address.getAddressText() != null && !address.getAddressText().isBlank()) {
            String text = address.getAddressText().trim();
            return text.length() > 50 ? text.substring(0, 50) : text;
        }
        
        if (address.getAddressType() != null && !address.getAddressType().isBlank()) {
            return mapAddressTypeToVietnamese(address.getAddressType());
        }
        
        return "Địa điểm";
    }
    
    /**
     * Map address type sang tiếng Việt
     */
    private String mapAddressTypeToVietnamese(String addressType) {
        return switch (addressType.toUpperCase()) {
            case "HOME" -> "Nhà riêng";
            case "WORK" -> "Công ty";
            case "SCHOOL" -> "Trường học";
            case "OTHER" -> "Khác";
            default -> addressType;
        };
    }
}
