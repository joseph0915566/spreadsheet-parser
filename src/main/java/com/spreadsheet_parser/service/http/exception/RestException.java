package com.spreadsheet_parser.service.http.exception;

import java.util.Map;

public class RestException extends RuntimeException {

    protected final int statusCode;
    protected final Map<String, String> headers;

    public RestException(int statusCode, String message, Map<String, String> headers) {
        super(message + " status code : " + statusCode + ", headers : " + headers.toString());
        this.statusCode = statusCode;
        this.headers = headers;
    }

}
