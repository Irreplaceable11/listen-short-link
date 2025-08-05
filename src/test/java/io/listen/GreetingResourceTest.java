package io.listen;

import com.deevvi.device.detector.engine.api.DeviceDetectorParser;
import com.deevvi.device.detector.engine.api.DeviceDetectorResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import io.listen.generator.ShortCodeGenerator;
import io.listen.generator.SnowflakeIdGenerator;
import io.listen.model.UrlMapping;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;

@QuarkusTest
class GreetingResourceTest {

    @Inject
    SnowflakeIdGenerator snowflakeIdGenerator;

    @Inject
    ShortCodeGenerator shortCodeGenerator;

    @Inject
    ObjectMapper objectMapper;

    @Test
    void testId() {
        System.out.println(snowflakeIdGenerator.nextId());
    }

    @Test
    void testShortCode() {
        System.out.println(shortCodeGenerator.generateShortCode());
    }

    @Test
    void testQueryOriginalUrl() {
        System.out.println(UrlMapping.findOriginalUrlByShortCode("qqq"));
    }

//    @Test
//    void testUserAgent() {
//        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36 Edg/138.0.0.0";
//
//        DeviceDetectorParser parser = DeviceDetectorParser.getClient();
//        DeviceDetectorResult parse = parser.parse(userAgent);
//        try {
//            System.out.println(objectMapper.writeValueAsString(parse));
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    @Test
//    void testIp() {
//        try {
//            URI uri = this.getClass().getClassLoader().getResource("GeoLite2-City.mmdb").toURI();
//            DatabaseReader dbReader = new DatabaseReader.Builder(new File(uri)).build();
//            InetAddress inetAddress = InetAddress.getByName("123.139.37.245");
//            CityResponse city = dbReader.city(inetAddress);
//            System.out.println(city.getCountry().getNames().get("zh-CN"));
//            System.out.println(city.getCity().getNames().get("zh-CN"));
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }

}