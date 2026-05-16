package com.velocitymall.admin.service.impl;

import com.velocitymall.admin.config.AdminMinioProperties;
import com.velocitymall.admin.model.vo.FileUploadVO;
import com.velocitymall.admin.service.ProductImageStorageService;
import com.velocitymall.common.exception.BusinessException;
import com.velocitymall.common.result.ResultCode;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioProductImageStorageService implements ProductImageStorageService {

    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif"
    );

    private final MinioClient minioClient;

    private final AdminMinioProperties minioProperties;

    @Override
    public FileUploadVO uploadSkuCover(Long skuId, MultipartFile file) {
        validate(skuId, file);
        String contentType = file.getContentType();
        String extension = IMAGE_EXTENSIONS.get(contentType);
        String objectKey = "products/skus/%d/%s.%s".formatted(skuId, UUID.randomUUID(), extension);

        try {
            ensureBucketReadable();
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .object(objectKey)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(contentType)
                                .build()
                );
            }
            String url = buildPublicUrl(objectKey);
            log.info("Product image uploaded. skuId={}, objectKey={}", skuId, objectKey);
            return FileUploadVO.builder()
                    .bucketName(minioProperties.getBucketName())
                    .objectKey(objectKey)
                    .url(url)
                    .originalFilename(file.getOriginalFilename())
                    .contentType(contentType)
                    .size(file.getSize())
                    .build();
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Product image upload failed. skuId={}, objectKey={}", skuId, objectKey, exception);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "商品图片上传失败");
        }
    }

    private void validate(Long skuId, MultipartFile file) {
        if (skuId == null || skuId < 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "SKU ID不能为空");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "上传图片不能为空");
        }
        if (file.getSize() > minioProperties.getMaxFileSize()) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "上传图片不能超过" + (minioProperties.getMaxFileSize() / 1024 / 1024) + "MB");
        }
        if (!IMAGE_EXTENSIONS.containsKey(file.getContentType())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅支持 JPG、PNG、WEBP、GIF 图片");
        }
    }

    private void ensureBucketReadable() throws Exception {
        String bucketName = minioProperties.getBucketName();
        if (!StringUtils.hasText(bucketName)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "MinIO bucket-name未配置");
        }
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build()
        );
        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build()
            );
        }
        minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config(publicReadPolicy(bucketName))
                        .build()
        );
    }

    private String publicReadPolicy(String bucketName) {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": { "AWS": ["*"] },
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucketName);
    }

    private String buildPublicUrl(String objectKey) {
        String endpoint = StringUtils.hasText(minioProperties.getPublicEndpoint())
                ? minioProperties.getPublicEndpoint()
                : minioProperties.getEndpoint();
        String normalizedEndpoint = endpoint.endsWith("/")
                ? endpoint.substring(0, endpoint.length() - 1)
                : endpoint;
        return "%s/%s/%s".formatted(normalizedEndpoint, minioProperties.getBucketName(), objectKey);
    }
}
