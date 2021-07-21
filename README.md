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

### 1-3. H2 데이터베이스 설치

* 권한 주기: `chmod 755 h2.sh`
* 데이터베이스 파일 생성 방법
    * `jdbc:h2:~/datajpa` (최초 한번)
    * `~/datajpa.mv.db` 파일 생성 확인
    * 이후 부터는 `jdbc:h2:tcp://localhost/~/datajpa` 이렇게 접속

> 참고: H2 데이터베이스의 MVCC 옵션은 H2 14.198 버전부터 제거되었다.

### 1-4. 스프링 데이터 JPA와 DB 설정, 동작 확인

#### application.yml

```yaml
spring:
  datasource:
    url: jdbc:h2:tcp://localhost/~/datajpa
    username: sa
    password:
    driver-class-name: org.h2.Driver

  jpa:
    hibernate:
      ddl-auto: create
    properties:
      hibernate:
        #        show_sql: true
        format_sql: true

logging.level:
  org.hibernate.SQL: debug
# org.hibernate.type: trace
```

* spring.jpa.hibernate.ddl-auto: create
    * 애플리케이션 실행 시점에 테이블을 drop 하고, 다시 생성한다.

> 참고: 모든 로그 출력은 가급적 로거를 통해 남겨야 한다.    
> `show_sql`옵션은 `System.out`에 하이버네이트 실행 SQL을 남긴다.   
> `org.hibernate.SQL`옵션은 logger를 통해 하이버네이트 실행 SQL을 남긴다.

#### Member.java - 회원 엔티티

* `src/main/java/study/datajpa/entity/Member.java`

```java
package study.datajpa.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter @Setter
@NoArgsConstructor(access = PROTECTED)
public class Member {

    @Id @GeneratedValue
    private Long id;
    private String username;

    public Member(String username) {
        this.username = username;
    }
}

```

#### MemberJpaRepository.java - 회원 JPA 리포지토리

* `src/main/java/study/datajpa/repository/MemberJpaRepository.java`

```java
package study.datajpa.repository;

import org.springframework.stereotype.Repository;
import study.datajpa.entity.Member;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Repository
public class MemberJpaRepository {

    @PersistenceContext
    private EntityManager em;

    public Member save(Member member) {
        em.persist(member);
        return member;
    }

    public Member find(Long id) {
        return em.find(Member.class, id);
    }
}

```

#### MemberJpaRepositoryTest.java - JPA 기반 테스트

* `src/test/java/study/datajpa/repository/MemberJpaRepositoryTest.java`

```java
package study.datajpa.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.datajpa.entity.Member;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Rollback(false)
class MemberJpaRepositoryTest {

    @Autowired MemberJpaRepository memberJpaRepository;

    @Test
    @DisplayName("테스트")
    public void test() {
        // given
        Member member = new Member("서정민");

        // when
        Member savedMember = memberJpaRepository.save(member);

        // then
        Member findMember = memberJpaRepository.find(savedMember.getId());

        assertThat(findMember.getId()).isEqualTo(member.getId());
        assertThat(findMember.getUsername()).isEqualTo(member.getUsername());
        assertThat(findMember).isEqualTo(member);
    }
}
```

#### MemberRepository.java - 스프링 데이터 JPA 리포지토리

* `src/main/java/study/datajpa/repository/MemberRepository.java`

```java
package study.datajpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.datajpa.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
}

```

#### MemberRepositoryTest.java - 스프링 데이터 JPA 기반 테스트

* `src/test/java/study/datajpa/repository/MemberRepositoryTest.java`

```java
package study.datajpa.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.datajpa.entity.Member;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Rollback(false)
class MemberRepositoryTest {

    @Autowired MemberRepository memberRepository;

    @Test
    public void testMember() {
        Member member = new Member("memberA");
        Member savedMember = memberRepository.save(member);

        Member findMember = memberRepository.findById(savedMember.getId()).orElseThrow();

        assertThat(findMember.getId()).isEqualTo(member.getId());
        assertThat(findMember.getUsername()).isEqualTo(member.getUsername());
        assertThat(findMember).isEqualTo(member);
    }
}
```

#### 쿼리 파라미터 로그 남기기

* 로그에 다음을 추가한다. `org.hibernate.type`: SQL - 실행 파라미터를 로그로 남긴다.
* 외부 라이브러리 사용
    * `implementation 'com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.5.7'`

> 참고: 쿼리 파리미터를 로그로 남기는 외부 라이브러리는 시스템 자원을 사용하므로, 개발 단계에서는 편하게 사용해도 된다. 하지만 운영시스템에 적용하려면꼭 성능 테스트를 하고 사용하는 것이 좋다.

## Note