package com.velocitymall.admin.service;

import com.velocitymall.admin.model.vo.AdminLoginVO;

public interface AdminService {

    AdminLoginVO login(String username, String password);

    void deliverOrder(String orderSn, String deliveryCompany, String deliverySn);

    void publishSpu(Long spuId);

    void unpublishSpu(Long spuId);
}
