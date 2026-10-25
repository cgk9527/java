package com.ecommerce.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ecommerce.common.ResultCode;
import com.ecommerce.common.exception.BizException;
import com.ecommerce.dto.AddressRequest;
import com.ecommerce.entity.UserAddress;
import com.ecommerce.mapper.UserAddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 收货地址服务
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private final UserAddressMapper addressMapper;

    public List<UserAddress> list(Long userId) {
        return addressMapper.selectList(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, AddressRequest request) {
        UserAddress address = new UserAddress();
        apply(address, userId, request);
        address.setIsDefault(0);
        LocalDateTime now = LocalDateTime.now();
        address.setCreatedAt(now);
        address.setUpdatedAt(now);
        addressMapper.insert(address);

        Long count = addressMapper.selectCount(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getUserId, userId));
        if ((request.getIsDefault() != null && request.getIsDefault() == 1) || (count != null && count == 1)) {
            setDefault(userId, address.getId());
        }
        return address.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Long id, AddressRequest request) {
        UserAddress address = getAndCheck(userId, id);
        apply(address, userId, request);
        address.setUpdatedAt(LocalDateTime.now());
        addressMapper.updateById(address);
        if (request.getIsDefault() != null && request.getIsDefault() == 1) {
            setDefault(userId, id);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        getAndCheck(userId, id);
        addressMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long userId, Long id) {
        getAndCheck(userId, id);
        addressMapper.update(null, Wrappers.<UserAddress>lambdaUpdate()
                .eq(UserAddress::getUserId, userId)
                .set(UserAddress::getIsDefault, 0));
        UserAddress address = addressMapper.selectById(id);
        address.setIsDefault(1);
        address.setUpdatedAt(LocalDateTime.now());
        addressMapper.updateById(address);
    }

    private UserAddress getAndCheck(Long userId, Long id) {
        UserAddress address = addressMapper.selectById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return address;
    }

    private void apply(UserAddress address, Long userId, AddressRequest request) {
        address.setUserId(userId);
        address.setReceiverName(request.getReceiverName());
        address.setReceiverPhone(request.getReceiverPhone());
        address.setProvince(request.getProvince());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setDetail(request.getDetail());
    }
}