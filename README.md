# 스프링 데이터 JPA

## 5. 확장 기능

### 5-1. 사용자 정의 리포지토리 구현

* 스프링 데이터 JPA 리포지토리는 인터페이스만 정의하고 구현체는 스프링이 자동 생성
* 스프링 데이터 JPA가 제공하는 인터페이스를 직접 구현하면 구현해야 하는 기능이 너무 많음
* 다양한 이유로 인터페이스의 메서드를 직접 구현하고 싶다면?
    * JPA 직접 사용(`EntityManager`)
    * 스프링 JDBC Template 사용
    * MyBatis 사용
    * 데이터베이스 커넥션 직접 사용 등등...
    * Querydsl 사용

#### MemberRepositoryCustom.java - 사용자 정의 인터페이스

* `src/main/java/study/datajpa/repository/MemberRepositoryCustom.java`

```java
package study.datajpa.repository;

import study.datajpa.entity.Member;

import java.util.List;

public interface MemberRepositoryCustom {
    List<Member> findMemberCustom();
}

```

#### MemberRepositoryImpl.java - 사용자 정의 인터페이스 구현 클래스

* `src/main/java/study/datajpa/repository/MemberRepositoryImpl.java`

```java
package study.datajpa.repository;

import lombok.RequiredArgsConstructor;
import study.datajpa.entity.Member;

import javax.persistence.EntityManager;
import java.util.List;

@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final EntityManager em;

    @Override
    public List<Member> findMemberCustom() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }
}

```

#### MemberRepository.java (추가) - 사용자 정의 인터페이스 상속

```java
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

}
```

* 사용자 정의 메서드 호출 코드

```
List<Member> result = memberRepository.findMemberCustom();
```

* 사용자 정의 구현 클래스
    * 규칙: 리포지토리 인터페이스 이름 + `Imple`
    * 스프링 데이터 JPA가 인식해서 스프링 빈으로 등록

* Impl 대신 다른 이름으로 변경하고 싶으면?
    * XML 설정

    ```xml
    
    <repositories base-package="study.datajpa.repository" repository-impl-postfix="Impl"/>
    ```

    * JavaConfig 설정

    ```
    @EnableJpaRepositories(basePackages = "study.datajpa.repository",
                           repositoryImplementationPostfix = "Impl")
    ```

> 참고: 실무에서는 주로 QueryDSL이나 SpringJdbcTemplate을 함께 사용할 때 사용자 정의 리포지토리 기능 자주 사용

> 참고: 항상 사용자 정의 리포지토리가 필요한 것은 아니다. 그냥 임의의 리포지토리를 만들어도 된다.
> 예를 들어 MemberQueryRepository를 인터페이스가 아닌 클래스로 만들고 스프링 빈으로 등록해서    
> 그냥 직접 사용해도 된다. 물론 이 경우 스프링 데이터 JPA와는 아무런 관계 없이 별도로 동작한다.

#### 사용자 정의 리포지토리 구현 최신 방식

스프링 데이터 2.x 부터는 사용자 정의 구현 클래스에 리포지토리 인터페이스 이름 + `Impl`을 적용하는 대신에    
사용자 정의 인터페이스 명 + `Impl`방식도 지원한다.    
예를 들어서 위 예제의 `MemberRepositoryImpl` 대신에 `MemberRepositoryCustomImpl`같이 구현해도 된다.

```java
@RequiredArgsConstructor
public class MemberRepositoryCustomImpl implements MemberRepsitoryCustom {

    private final EntityManager em;

    @Override
    public List<Member> findMemberCustom() {
        return em.createQuery("select m from Member m")
                .getResultList();
    }
}
```

기존 방식보다 이 방식이 사용자 정의 인터페이스 이름과 구현 클래스 이름이 비슷하므로 더 직관적이다.    
추가로 여러 인터페이스를 분리해서 구현하는 것도 가능하기 때문에 새롭게 변경된 이 방식을 사용하는 것을 더 권장한다.

### 5-2. Auditing

* 엔티티를 생성, 변경할 때 변경한 사람과 시간을 추적하고 싶으면?
    * 등록일
    * 수정일
    * 등록자
    * 수정자

#### JpaBaseEntity.java - 순수 JPA 사용

* `src/main/java/study/datajpa/entity/JpaBaseEntity.java`

```java
package study.datajpa.entity;

import lombok.Getter;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import java.time.LocalDateTime;

@MappedSuperclass
@Getter
public class JpaBaseEntity {

    @Column(updatable = false)
    private LocalDateTime createDate;
    private LocalDateTime updateDate;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createDate = now;
        updateDate = now;
    }

    @PreUpdate
    public void preUpdate() {
        updateDate = LocalDateTime.now();
    }
}

```

#### MemberTest.java (추가) - JpaBaseEntity 테스트

```java
public class MemberTest {
    @Test
    @DisplayName("BaseTimeEntity 테스트")
    public void JpaEventBaseEntity() throws Exception {
        // given
        Member member = new Member("member1");
        memberRepository.save(member); // @PrePersist

        Thread.sleep(100);
        member.setUsername("member2");

        em.flush(); // @PreUpdate
        em.clear();

        // when
        Member findMember = memberRepository.findById(member.getId()).orElseThrow();

        // then
        System.out.println("findMember.getCreateDate() = " + findMember.getCreateDate());
        System.out.println("findMember.getUpdateDate() = " + findMember.getLastModifiedDate());
    }
}
```

* JPA 주요 이벤트 어노테이션
    * `@PrePersist`, `@PostPersist`
    * `@PreUpdate`, `@PostUpdate`

#### 스프링 데이터 JPA 사용

* 설정
    * `@EnableJpaAuditing` -> 스프링 부트 설정 클래스에 적용해야 함
    * `@EntityListeners(AuditingEntityListener.class)` -> 엔티티에 적용

* 사용 어노테이션
    * `@CreatedDate`
    * `@LastModifiedDate`
    * `@CreateBy`
    * `@LastModifiedBy`

#### BaseEntity.java - 스프링 데이터 Auditing 적용: 등록일, 수정일

* `src/main/java/study/datajpa/entity/BaseEntity.java`

```java
package study.datajpa.entity;

@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@Getter
public class BaseEntity {
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdDate;
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;
}

```

#### BaseEntity.java (추가) - 등록자, 수정자

```java
package jpabook.jpashop.domain;

@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public class BaseEntity {
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdDate;
    @LastModifiedDate private LocalDateTime lastModifiedDate;
    @CreatedBy
    @Column(updatable = false)
    private String createdBy;
    @LastModifiedBy
    private String lastModifiedBy;
}
```

#### DataJpaApplication.java - 등록자, 수정자를 처리해주는 `AuditorAware`스프링 빈 등록

```java
package study.datajpa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;
import java.util.UUID;

@EnableJpaAuditing
@SpringBootApplication
public class DataJpaApplication {

    // ...

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of(UUID.randomUUID().toString());
    }
}

```

실무에서는 세션 정보나, 스프링 시큐리티 로그인 정보에서 ID를 받음

> 참고: 실무에서 대부분의 엔티티는 등록시간, 수정시간이 필요하지만, 등록자, 수정자는 없을 수도 있다.     
> 그래서 다름과 같이 Base 타입을 분리하고, 원하는 타입을 선택해서 상속한다.

```java
public class BaseTimeEntity {
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdDate;
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;
}

public class BaseEntity extends BaseTimeEntity {
    @CreatedBy
    @Column(updatable = false)
    private String createdBy;
    @LastModifiedBy
    private String lastModifiedBy;
}
```

> 참고: 저장시점에 등록일, 등록자는 물론이고, 수정일, 수정자도 같은 데이터가 저장된다. 데이터가 중복 저장되는 것 같지만, 이렇게 해두면 변경 컬럼만 확인해도 마지막에 업데이트한 유저를 확인할 수 있으므로 유지보수 관점에서 편리하다. 이렇게 하지 않으면 변경 컬럼이 `null`일때 등록 컬럼을 또 찾아야 한다.
>
> 참고로 저장시점에 저장 데이터만 입력하고 싶으면 `@EnableJpaAuditing(modifyOnCreate = false)`옵션을 사용하면 된다.

### 5-3. Web 확장 - 도메인 클래스 컨버터

HTTP 파라미터로 넘어온 엔티티의 아이디로 엔티티 객체를 찾아서 바인딩

#### MemberController.java (추가) - 도메인 클래스 컨버터 사용 전

* `src/main/java/study/datajpa/controller/MemberController.java`

```java
@RestController
@RequiredArgsConstructor
public class MemberController {
    private final MemberRepository memberRepository;

    @GetMapping("/members/{id}")
    public String findMember(@PathVariable("id") Long id) {
        Member member = memberRepository.findById(id).get();
        return member.getUsername();
    }
}
```

#### MemberController.java (추가) - 도메인 클래스 컨버터 사용 후

```java
@RestController
@RequiredArgsConstructor
public class MemberController {
    private final MemberRepository memberRepository;

    @GetMapping("/members/{id}")
    public String findMember(@PathVariable("id") Member member) {
        return member.getUsername();
    }
}
```

* HTTP 요청은 회원 `id`를 받지만 도메인 클래스 컨버터가 중간에 동작해서 회원 엔티티 객체를 반환
* 도메인 클래스 컨버터도 리퍼지토리를 사용해서 엔티티를 찾음

> 주의: 도메인 클래스 컨버터로 엔티티를 파라미터로 받으면, 이 엔티티는 단순 조회용으로만 사용해다 한다.    
> (트랜잭션이 없는 범위에서 엔티티를 조회했으므로, 엔티티를 변경해도 DB에 반영되지 않는다.)

## Note