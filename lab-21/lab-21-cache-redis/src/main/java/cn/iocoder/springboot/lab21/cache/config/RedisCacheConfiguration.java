package cn.iocoder.springboot.lab21.cache.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * @author SZFHH
 * @date 2020/6/23
 */
@Configuration
@EnableConfigurationProperties(CacheProperties.class)
@AutoConfigureAfter({RedisAutoConfiguration.class})
class RedisCacheConfiguration {

    private final CacheProperties cacheProperties;

    RedisCacheConfiguration(CacheProperties cacheProperties) {

        this.cacheProperties = cacheProperties;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory,
                                          ResourceLoader resourceLoader) {
        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager
            .builder(redisConnectionFactory)
            // 设置默认的配置信息，用的是JDK序列化器（SpringBoot默认），也可以改成其他的。
            .cacheDefaults(defaultConfiguration(new JdkSerializationRedisSerializer(resourceLoader.getClassLoader())));
        List<String> cacheNames = this.cacheProperties.getCacheNames();
        if (!cacheNames.isEmpty()) {
            builder.initialCacheNames(new LinkedHashSet<>(cacheNames));
        }

        // 上面基本没变，这里加了需要预先创建好的Cache信息。
        Map<String, org.springframework.data.redis.cache.RedisCacheConfiguration> cacheConfigurations = new LinkedHashMap<>();

        // ------------在下面加自己的Cache名，ttl，值序列化器-------------
        cacheConfigurations.put("MyCache1", customizedConfiguration(Duration.ofSeconds(10),
            RedisSerializer.json()));
        cacheConfigurations.put("MyCache2", customizedConfiguration(Duration.ofSeconds(20),
            RedisSerializer.json()));
        // -------------------------------------------------------------

        // builder加入需要预先创建好的Cache信息。
        builder.withInitialCacheConfigurations(cacheConfigurations);
        return builder.build();
    }

    //跟SpringBoot中的defaultConfiguration基本一样，就是改了个输入，可以自定义序列化器了。
    //原始的是传入ClassLoader，去创建JDK序列化器
    private org.springframework.data.redis.cache.RedisCacheConfiguration defaultConfiguration(
        RedisSerializer valueSerializer) {
        CacheProperties.Redis redisProperties = this.cacheProperties.getRedis();
        org.springframework.data.redis.cache.RedisCacheConfiguration config = org.springframework.data.redis.cache.RedisCacheConfiguration
            .defaultCacheConfig();
        config = config.serializeValuesWith(RedisSerializationContext.SerializationPair
            .fromSerializer(valueSerializer));
        if (redisProperties.getTimeToLive() != null) {
            config = config.entryTtl(redisProperties.getTimeToLive());
        }
        if (redisProperties.getKeyPrefix() != null) {
            config = config.prefixKeysWith(redisProperties.getKeyPrefix());
        }
        if (!redisProperties.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }
        if (!redisProperties.isUseKeyPrefix()) {
            config = config.disableKeyPrefix();
        }
        return config;
    }

    // 自定义的配置，可以设置ttl和序列化器
    private org.springframework.data.redis.cache.RedisCacheConfiguration customizedConfiguration(
        Duration ttl, RedisSerializer valueSerializer) {

        // 获得默认的配置
        org.springframework.data.redis.cache.RedisCacheConfiguration config = defaultConfiguration(valueSerializer);

        //设置ttl
        config = config.entryTtl(ttl);
        return config;
    }


}

