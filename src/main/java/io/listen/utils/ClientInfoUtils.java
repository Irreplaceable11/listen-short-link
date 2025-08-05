package io.listen.utils;

import com.deevvi.device.detector.engine.api.DeviceDetectorParser;
import com.deevvi.device.detector.engine.api.DeviceDetectorResult;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import io.vertx.core.http.HttpServerRequest;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
@Startup
public class ClientInfoUtils {

    DeviceDetectorParser parser = null;

    DatabaseReader dbReader = null;

    @PostConstruct
    public void init() {
        try {
            String dbFileName = "GeoLite2-City.mmdb";
            parser = DeviceDetectorParser.getClient();
            dbReader = new DatabaseReader.Builder(new File(getClass().getClassLoader().getResource(dbFileName).toURI())).build();
        } catch (IOException | URISyntaxException e) {
            Log.error(e.getMessage(), e);
        }

    }

    @PreDestroy
    public void destroy() {
        try {
            dbReader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    public String getClientIpAddress(HttpServerRequest request) {
        // 优先检查 X-Forwarded-For 头（常用代理头）
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            // X-Forwarded-For 可能包含多个 IP，取第一个（客户端 IP）
            return ip.split(",")[0].trim();
        }

        // 再检查 X-Real-IP 头（某些代理使用）
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty()) {
            return ip.trim();
        }

        // 最后从远程地址获取（直接连接情况）
        return Optional.ofNullable(request.authority().host())
                .orElse("unknown");
    }

    public Map<String, String> getIpInfo(String ip) {
        InetAddress inetAddress = null;
        Map<String, String> map = new HashMap<>();
        try {
            inetAddress = InetAddress.getByName(ip);
            CityResponse response = dbReader.city(inetAddress);
            map.put("country", response.getCountry().getName());
            map.put("city", response.getCity().getName());
        } catch (IOException | GeoIp2Exception e) {
            Log.error(e.getMessage(), e);
        }
        return map;
    }

   public Map<String, String> getClientInfo(String userAgent) {
       Map<String, String>  map = new HashMap<>();
       DeviceDetectorResult result = parser.parse(userAgent);
       Map<String, String> resultMap = result.toMap();
       //设备类型
       map.put("device", resultMap.get("device.deviceType"));
       //操作系统
       map.put("os", resultMap.get("os.name") + " " + resultMap.getOrDefault("os.version", ""));
       //浏览器类型
       map.put("client", resultMap.get("client.name"));
       return map;
   }
}
