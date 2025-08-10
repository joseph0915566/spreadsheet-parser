package com.spreadsheet_parser.service.aws.s3.model;

import java.util.Map;

public class MultipartUploadResponse {

    private String uploadId;
    private Map<Integer, String> presignedUrls;

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public Map<Integer, String> getPresignedUrls() {
        return presignedUrls;
    }

    public void setPresignedUrls(Map<Integer, String> presignedUrls) {
        this.presignedUrls = presignedUrls;
    }

}
