package com.spreadsheet_parser.service.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

@Service
public class CredentialProvider {

    private final StaticCredentialsProvider credentialProvider;

    public CredentialProvider(
            @Value("${aws.access.key}") String awsAccessKey,
            @Value("${aws.secret.key}") String awsSecretKey
    ) {
        this.credentialProvider = StaticCredentialsProvider
                .create(
                        AwsBasicCredentials.create(awsAccessKey, awsSecretKey)
                );
    }

    public StaticCredentialsProvider get(){
        return this.credentialProvider;
    }

}
