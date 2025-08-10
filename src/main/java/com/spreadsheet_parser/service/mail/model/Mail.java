package com.spreadsheet_parser.service.mail.model;

import java.util.List;

public class Mail {

    private final String sender;
    private final String commaSeparatedRecipients;
    private final String subject;
    private final String text;
    private final String html;
    private final List<Attachment> attachments;

    public Mail(String sender, String commaSeparatedRecipients, String subject, String text, String html, List<Attachment> attachments) {
        this.sender = sender;
        this.commaSeparatedRecipients = commaSeparatedRecipients;
        this.subject = subject;
        this.text = text;
        this.html = html;
        this.attachments = attachments;
    }

    public String getSender() {
        return sender;
    }

    public String getCommaSeparatedRecipients() {
        return commaSeparatedRecipients;
    }

    public String getSubject() {
        return subject;
    }

    public String getText() {
        return text;
    }

    public String getHtml() {
        return html;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

}
