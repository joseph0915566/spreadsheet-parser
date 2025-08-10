package com.spreadsheet_parser.service.json;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class JsonService {

    private final ObjectMapper mapper;

    public JsonService() {
        this.mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public ObjectMapper getJsonMapper(){
        return this.mapper;
    }

    public String toJson(Object object){
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public<T> T toPojo(String json, Class<T> clazz){
        return toPojo(json, clazz, "");
    }

    public<T> T toPojo(String json, Class<T> clazz, String rootName){

        if(json == null || json.isEmpty()) return null;
        try {
            return mapper.readValue(mapper.readTree(json).at(toJacksonNotationExpr(rootName)).traverse(), clazz);
        } catch (JsonParseException e) {
            throw new RuntimeException(e);
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public<T> List<T> toList(Class<T> clazz, String json){
        return toList(clazz, json, "");
    }

    public boolean isPropertyExist(String json, String propertyName){
        try {
            return !mapper.readTree(json).at(toJacksonNotationExpr(propertyName)).isMissingNode();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public<T> List<T> toList(Class<T> clazz, String json, String newRootPropertyName){

        if(json == null || json.isEmpty()) return null;
        try {
            JavaType type = mapper.getTypeFactory().constructCollectionType(ArrayList.class, clazz);
            return mapper.readValue(mapper.readTree(json).at(toJacksonNotationExpr(newRootPropertyName)).traverse(), type);
        } catch (JsonParseException e) {
            throw new RuntimeException(e);
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public String toJacksonNotationExpr(String propertyName){
        if(propertyName == null || propertyName.isEmpty()) return "";
        return "/" + propertyName.replaceAll("\\.", "/");
    }

}
