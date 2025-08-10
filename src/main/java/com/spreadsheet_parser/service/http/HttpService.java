package com.spreadsheet_parser.service.http;

import java.util.Map;

public interface HttpService {
    String doPost(String hostAddress, String resourcePath, String json, Map<String, String> headers, Map<String, String> parameters, int requestTimeout);
}
