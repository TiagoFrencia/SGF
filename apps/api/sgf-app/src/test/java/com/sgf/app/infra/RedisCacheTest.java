package com.sgf.app.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.sgf.app.support.PostgresRedisIntegrationTestSupport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
@Tag("integration")
class RedisCacheTest extends PostgresRedisIntegrationTestSupport {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void redisIsReachable() {
        assertThat(redisTemplate).isNotNull();
        redisTemplate.opsForValue().set("test-key", "test-value");
        String value = redisTemplate.opsForValue().get("test-key");
        assertThat(value).isEqualTo("test-value");
    }
}
