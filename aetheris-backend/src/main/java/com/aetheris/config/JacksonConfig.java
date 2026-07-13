package com.aetheris.config;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers Hibernate6Module so Jackson can serialize JPA entities that
 * still carry uninitialized lazy proxies (spring.jpa.open-in-view=false
 * means those proxies are never fetched unless the query explicitly does a
 * JOIN FETCH). Without this module, serializing any such entity throws:
 * "Type definition error ... ByteBuddyInterceptor".
 *
 * With the module registered, an uninitialized lazy association is
 * serialized as null instead of crashing, and one that was explicitly
 * fetched (e.g. via JOIN FETCH) is serialized normally.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Hibernate6Module hibernate6Module() {
        Hibernate6Module module = new Hibernate6Module();
        module.disable(Hibernate6Module.Feature.USE_TRANSIENT_ANNOTATION);
        return module;
    }
}
