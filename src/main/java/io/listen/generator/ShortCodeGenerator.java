package io.listen.generator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ShortCodeGenerator {

    private static final String BASE62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = BASE62_CHARS.length();
    private static final int MIN_LENGTH = 6;

    @Inject
    SnowflakeIdGenerator idGenerator;

    /**
     * 生成短码
     */
    public String generateShortCode() {
        long id = idGenerator.nextId(); // 分布式ID生成
        return encode(id);
    }

    /**
     * Base62编码
     */
    private String encode(long num) {
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(BASE62_CHARS.charAt((int) (num % BASE)));
            num /= BASE;
        }

        // 确保最小长度
        while (sb.length() < MIN_LENGTH) {
            sb.append(BASE62_CHARS.charAt(0));
        }

        return sb.reverse().toString();
    }

    /**
     * Base62解码
     */
    public long decode(String shortCode) {
        long result = 0;
        for (int i = 0; i < shortCode.length(); i++) {
            result = result * BASE + BASE62_CHARS.indexOf(shortCode.charAt(i));
        }
        return result;
    }
}
