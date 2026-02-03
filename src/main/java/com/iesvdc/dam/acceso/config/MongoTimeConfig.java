package com.iesvdc.dam.acceso.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import com.mongodb.lang.NonNull;

import java.time.LocalTime;
import java.util.List;

@Configuration
public class MongoTimeConfig {

  @Bean
  public MongoCustomConversions mongoCustomConversions() {
    @SuppressWarnings({"rawtypes", "unchecked"})
    List converters = List.of(new LocalTimeToString(), new StringToLocalTime());
    
    return new MongoCustomConversions(converters);    
  }

  @WritingConverter
  static class LocalTimeToString implements Converter<LocalTime, String> {
    @Override public String convert(@NonNull LocalTime source) {
      return source.toString(); // "10:00"
    }
  }

  @ReadingConverter
  static class StringToLocalTime implements Converter<String, LocalTime> {
    @Override public LocalTime convert(@NonNull String source) {
      return LocalTime.parse(source);
    }
  }
}
