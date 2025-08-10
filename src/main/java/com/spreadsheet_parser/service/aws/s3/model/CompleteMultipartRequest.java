package com.spreadsheet_parser.service.aws.s3.model;

public class CompleteMultipartRequest {

    private String uploadId;
    private String filename;
    private UploadPart[] parts;

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public UploadPart[] getParts() {
        return parts;
    }

    public void setParts(UploadPart[] parts) {
        this.parts = parts;
    }

    public static class UploadPart{

        private int partNumber;
        private String eTag;

        public int getPartNumber() {
            return partNumber;
        }

        public void setPartNumber(int partNumber) {
            this.partNumber = partNumber;
        }

        public String geteTag() {
            return eTag;
        }

        public void seteTag(String eTag) {
            this.eTag = eTag;
        }

    }

}
