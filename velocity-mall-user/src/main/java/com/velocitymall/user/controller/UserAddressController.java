package com.velocitymall.user.controller;

import com.velocitymall.common.result.Result;
import com.velocitymall.user.model.dto.UserAddressSaveDTO;
import com.velocitymall.user.model.vo.UserAddressVO;
import com.velocitymall.user.service.UserAddressService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService userAddressService;

    @PostMapping("/api/v1/users/addresses")
    public Result<UserAddressVO> create(@Valid @RequestBody UserAddressSaveDTO dto) {
        return userAddressService.create(dto);
    }

    @PutMapping("/api/v1/users/addresses/{id}")
    public Result<UserAddressVO> update(@PathVariable Long id, @Valid @RequestBody UserAddressSaveDTO dto) {
        return userAddressService.update(id, dto);
    }

    @DeleteMapping("/api/v1/users/addresses/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return userAddressService.delete(id);
    }

    @GetMapping("/api/v1/users/addresses")
    public Result<List<UserAddressVO>> listByUser() {
        return userAddressService.listByUser();
    }

    @GetMapping("/api/v1/users/inner/addresses/{id}")
    public Result<UserAddressVO> getById(@PathVariable Long id, @RequestParam("userId") Long userId) {
        return userAddressService.getById(id, userId);
    }
}
