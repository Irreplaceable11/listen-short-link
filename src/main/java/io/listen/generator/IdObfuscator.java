package io.listen.generator;

public class IdObfuscator {

    private final long prime;       // 与模数互质的素数
    private final long mod;         // 必须大于最大ID
    private final long inverse;     // prime在mod下的乘法逆元

    public IdObfuscator(long prime, long mod) {
        this.prime = prime;
        this.mod = mod;
        this.inverse = modInverse(prime, mod);
    }

    public long obfuscate(long id) {
        return (id * prime) % mod;
    }

    public long deobfuscate(long obfuscated) {
        return (obfuscated * inverse) % mod;
    }

    // 扩展欧几里得算法计算乘法逆元
    private long modInverse(long a, long m) {
        long m0 = m, t, q;
        long x0 = 0, x1 = 1;
        if (m == 1) return 0;
        while (a > 1) {
            q = a / m;
            t = m;
            m = a % m;
            a = t;
            t = x0;
            x0 = x1 - q * x0;
            x1 = t;
        }
        if (x1 < 0) x1 += m0;
        return x1;
    }
}
