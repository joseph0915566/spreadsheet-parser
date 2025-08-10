package com.spreadsheet_parser.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;

@Configuration
public class AppConfig {

    @Bean
    DispatcherServlet dispatcherServlet() {

        DispatcherServlet dispatcherServlet = new DispatcherServlet();
        dispatcherServlet.setThreadContextInheritable(true);

        return dispatcherServlet;

    }

}
