package com.velocitymall.admin.service;

import com.velocitymall.admin.model.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

public interface ProductImageStorageService {

    FileUploadVO uploadSkuCover(Long skuId, MultipartFile file);
}
