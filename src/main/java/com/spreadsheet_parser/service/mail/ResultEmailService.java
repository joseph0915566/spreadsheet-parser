package com.spreadsheet_parser.service.mail;

import com.spreadsheet_parser.service.aws.s3.S3PresignerService;
import com.spreadsheet_parser.service.template.MustacheService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
public class ResultEmailService {

    private final MailService mailService;
    private final MustacheService mustacheService;
    private final S3PresignerService s3PresignerService;
    private final int urlExpiryDay;
    private final String templateLocation;

    @Inject
    public ResultEmailService(
            MailService mailService,
            MustacheService mustacheService,
            S3PresignerService s3PresignerService,
            @Value("${aws.s3.download.url-expiry-day}") int urlExpiryDay,
            @Value("${mail.template-location}") String templateLocation
    ){
        this.mailService = mailService;
        this.mustacheService = mustacheService;
        this.s3PresignerService = s3PresignerService;
        this.urlExpiryDay = urlExpiryDay;
        this.templateLocation = templateLocation;
    }

    public void sendResultEmail(String recipient, String key){
        Map<String, String> data = new HashMap<>();
        data.put("day", String.valueOf(urlExpiryDay));
        data.put("url", s3PresignerService.generateDownloadUrl(key));
        mailService.send(null, Arrays.asList(recipient), null, mustacheService.getTemplate(templateLocation, data), null, null);
    }

}
