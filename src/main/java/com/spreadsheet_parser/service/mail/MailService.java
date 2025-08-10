package com.spreadsheet_parser.service.mail;

import com.spreadsheet_parser.service.mail.model.Attachment;

import java.util.List;

public interface MailService {
    void send(String sender, List<String> recipientList, String subject, String textContent, String htmlContent, List<Attachment> attachments);
}
