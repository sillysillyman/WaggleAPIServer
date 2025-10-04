package io.waggle.waggleapiserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class WaggleApiServerApplication

fun main(args: Array<String>) {
    runApplication<WaggleApiServerApplication>(*args)
}
