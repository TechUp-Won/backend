package com.example.WonkaoTalk.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

/**
 * ES 통합 테스트용 컨테이너 설정. ES + analysis-nori 플러그인 이미지를 인라인으로 빌드해
 * {@code @ServiceConnection} 으로 spring.elasticsearch.uris 를 자동 주입한다.
 *
 * 운영용 elasticsearch.Dockerfile 과 동일한 구성(ES 9.2.8 + analysis-nori)을 유지한다.
 * Dockerfile 을 직접 build context 로 쓰지 않고 인라인 빌더를 쓰는 이유는, 프로젝트 디렉터리 전체가
 * Docker build context 로 전송되는 비용/이슈를 피하기 위함이다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class SearchTestContainerConfig {

  private static final String ES_IMAGE = "docker.elastic.co/elasticsearch/elasticsearch:9.2.8";

  @Bean
  @ServiceConnection
  public ElasticsearchContainer elasticsearchContainer() throws Exception {
    String image = new ImageFromDockerfile("wonkao-talk-es-nori", false)
        .withDockerfileFromBuilder(builder -> builder
            .from(ES_IMAGE)
            .run("elasticsearch-plugin install --batch analysis-nori")
            .build())
        .get();
    return new ElasticsearchContainer(
        DockerImageName.parse(image)
            .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch"))
        .withEnv("xpack.security.enabled", "false");
  }
}
