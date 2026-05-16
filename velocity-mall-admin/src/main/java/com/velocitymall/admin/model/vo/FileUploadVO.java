package com.velocitymall.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileUploadVO {

    private String bucketName;

    private String objectKey;

    private String url;

    private String originalFilename;

    private String contentType;

    private Long size;
}
