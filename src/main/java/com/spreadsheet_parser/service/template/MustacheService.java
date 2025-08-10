package com.spreadsheet_parser.service.template;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.io.Writer;

@Service
public class MustacheService {

    private static final Logger LOGGER = LogManager.getLogger(MustacheService.class);

    private final MustacheFactory factory;

    public MustacheService(){
        this.factory = new DefaultMustacheFactory();
    }

    public String getTemplate(String template, Object data){

        try(Writer writer = new StringWriter()) {
            factory.compile(template).execute(writer, data).flush();
            return writer.toString();
        } catch (Exception e) {
            LOGGER.error("Error working on template " + template, e);
            throw new RuntimeException(e);
        }

    }

}
