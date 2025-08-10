package com.spreadsheet_parser.service.mail.impl;

import com.spreadsheet_parser.service.http.HttpService;
import com.spreadsheet_parser.service.json.JsonService;
import com.spreadsheet_parser.service.mail.MailService;
import com.spreadsheet_parser.service.mail.model.Attachment;
import com.spreadsheet_parser.service.mail.model.Mail;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.inject.Inject;
import java.util.List;

@Service
public class MailGunService implements MailService {

    private static final Logger LOGGER = LogManager.getLogger(MailGunService.class);

    private static final String MAILGUN_URL = "https://api.mailgun.net/";

    private final HttpService httpService;
    private final JsonService jsonService;
    private final String defaultSender;
    private final String defaultSubject;
    private final String env;

    @Inject
    public MailGunService(
                            HttpService httpService,
                            JsonService jsonService,
                            @Value("${mail.default-sender}") String defaultSender,
                            @Value("${mail.default-subject}") String defaultSubject,
                            @Value("${app.env}") String env
    ){
        this.httpService = httpService;
        this.jsonService = jsonService;
        this.defaultSender = defaultSender;
        this.defaultSubject = defaultSubject;
        this.env = env;
    }

    @Override
    public void send(String sender, List<String> recipientList, String subject, String textContent, String htmlContent, List<Attachment> attachments) {

        final String recipients = String.join(",", recipientList);
        try {

            final Mail mail = new Mail(
                    ObjectUtils.isEmpty(sender) ? defaultSender : sender,
                    recipients,
                    ObjectUtils.isEmpty(subject) ? defaultSubject : subject,
                    textContent,
                    htmlContent,
                    attachments
            );

            try {
                httpService.doPost(MAILGUN_URL, "/v3/" + env + "messages", jsonService.toJson(mail), null, null, 10);
            } catch (Exception e) {
                LOGGER.error("Error sending email to " + recipients, e);
            }

        } catch (Exception e) {
            String errorMessage = "Failed to execute email sending activity to following emails <" + recipients + ">";
            LOGGER.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }

    }

}
