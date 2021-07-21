# 스프링 데이터 JPA

## 1. 프로젝트 환경설정

### 1-1. 프로젝트 생성

#### build.gradle

```groovy
plugins {
    id 'org.springframework.boot' version '2.5.2'
    id 'io.spring.dependency-management' version '1.0.11.RELEASE'
    id 'java'
}

group = 'study'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '11'

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    compileOnly 'org.projectlombok:lombok'
    runtimeOnly 'com.h2database:h2'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

test {
    useJUnitPlatform()
}

```

* 아래는 JUnit4를 사용하기 위한 라이브러리 추가 코드

```groovy
dependencies {
    testImplementation('org.springframework.boot:spring-boot-starter-test') {
        exclude group: 'org.junit.vintage', module: 'junit-vintage-engine'
    }
}
```

### 1-2. 라이브러리 살펴보기

#### gradle 의존관계 보기

```
./gradlew dependencies --configuration compileClasspath
```

#### 스프링 부트 라이브러리 살펴보기

* spring-boot-starter-web
    * spring-boot-starter-tomcat: 톰캣 (웹서버)
    * spring-webmvc: 스프링 웹 MVC
* spring-boot-starter-data-jpa
    * spring-boot-starter-aop
    * spring-boot-starter-jdbc
        * HikariCP 커넥션 풀 (부트 2.0 기본)
    * hibernate + JPA: 하이버네이트 + JPA
    * spring-data-jpa: 스프링 데이터 JPA
* spring-boot-starter(공통): 스프링 부트 + 스프링 코어 + 로깅
    * spring-boot
        * spring-core
    * spring-boot-starter-logging
    * logback, slf4j

#### 테스트 라이브러리

* spring-boot-starter-test
    * junit: 테스트 프레임워크, 스프링 부트 2.2부터 junit5( jupiter ) 사용
        * 과거 버전은 vintage
    * mockito: 목 라이브러리
    * assertj: 테스트 코드를 좀 더 편하게 작성하게 도와주는 라이브러리
        * https://joel-costigliola.github.io/assertj/index.html
    * spring-test: 스프링 통합 테스트 지원
* 핵심 라이브러리
    * 스프링 MVC
    * 스프링 ORM
    * JPA, 하이버네이트
    * 스프링 데이터 JPA
* 기타 라이브러리
    * H2 데이터베이스 클라이언트
    * 커넥션 풀: 부트 기본은 HikariCP
    * 로깅 SLF4J & LogBack
    * 테스트

## Note