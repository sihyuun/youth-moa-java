package io.github.sihyuuun.youthmoa;

import org.springframework.boot.SpringApplication;

public class TestYouthMoaApplication {

  public static void main(String[] args) {
    SpringApplication.from(YouthMoaApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
