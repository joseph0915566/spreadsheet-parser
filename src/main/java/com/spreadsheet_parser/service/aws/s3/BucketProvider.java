package com.spreadsheet_parser.service.aws.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;

@Service
public class BucketProvider {

    private final Region region;
    private final String bucketName;

    public BucketProvider(
            @Value("${aws.region}") String awsRegion,
            @Value("${aws.s3.bucket.name}") String bucketName
    ){
        this.region = Region.of(awsRegion);
        this.bucketName = bucketName;
    }

    public Region getRegion(){
        return this.region;
    }

    public String getBucketName(){
        return this.bucketName;
    }

}
