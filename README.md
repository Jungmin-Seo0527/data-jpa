# 스프링 데이터 JPA

## 4. 쿼리 메소드 기능

* 쿼리 메소드 기능 3가지
    * 메소드 이름으로 쿼리 생성
    * 메소드 이름으로 JPA NamedQuery 호출
    * `@Qeury` 어노테이션을 사용해서 리파지토리 인터페이스에 쿼리 직접 정의

### 4-1. 메소드 이름으로 쿼리 생성

메소드 이름을 분석해서 JPQL 쿼리 실행     
이름과 나이를 기준으로 회원을 조회하려면?

#### MemberJpaRepository.java (추가) - 순수 JPA 리포지토리

```java
package study.datajpa.repository;

import org.springframework.stereotype.Repository;
import study.datajpa.entity.Member;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Repository
public class MemberJpaRepository {

    @PersistenceContext
    private EntityManager em;

    // ...

    public List<Member> findByUsernameAndAgeGreaterThan(String username, int age) {
        return em.createQuery("select m from Member m where m.username = :username and m.age > :age", Member.class)
                .setParameter("username", username)
                .setParameter("age", age)
                .getResultList();
    }
}

```

#### MemberJpaRepositoryTest.java (추가) - 이름과 나이를 조건으로 한 조회 쿼리문 테스트

```java
package study.datajpa.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.datajpa.entity.Member;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Rollback(false)
class MemberJpaRepositoryTest {

    @Autowired MemberJpaRepository memberJpaRepository;

    @Test
    @DisplayName("JPA로 구현한 특정 이름을 가진 회원중 특정 나이보다 많은 회원 조회")
    public void findByUsernameAndAgeGreaterThen() {
        // given
        Member m1 = new Member("AAA", 10);
        Member m2 = new Member("AAA", 20);

        memberJpaRepository.save(m1);
        memberJpaRepository.save(m2);

        // when
        List<Member> result = memberJpaRepository.findByUsernameAndAgeGreaterThan("AAA", 15);

        // then
        assertThat(result.get(0).getUsername()).isEqualTo("AAA");
        assertThat(result.get(0).getAge()).isEqualTo(20);
        assertThat(result.size()).isEqualTo(1);

    }
}
```

#### MemberRepository.java (추가) - 스프링 데이터 JPA

```java
package study.datajpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.datajpa.entity.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findByUsernameAndAgeGreaterThan(String userName, int age);
}

```

* 스프링 데이터 JPA는 메소드 이름을 분석해서 JPQL을 생성하고 실행

* 쿼리 메소드 필터 조건
    * [스프링 데어터 JPA 공식 문서 참고](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.query-creation)

* 스프링 데이터 JPA가 제공하는 쿼리 메소드 기능
    * 조회: find...By, read...By, query...By get...By
        * [공식 문서](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods.query-creation)
        * 예) findHelloBy 처럼 ...에 식별하기 위한 내용(설명)이 들어가도 된다.
* COUNT: count...By: 반환타입 = `long`
* EXISTS: exists...By: 반환타입 = `boolean`
* 삭제: delete...By, remove...By: 반환타입 = `long`
* DISTINCT: findDistinct, findMemberDistinctBy
* LIMIT: findFirst3, findFirst, findTop, findTop3
    * [공식 문서](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.limit-query-result)

> 참고: 이 기능은 엔티티의 필드명이 변경되면 인터페이스에 정의한 메서드 이름도 꼭 함께 변경해야 한다.         
> 그렇지 않으면 애플리케이션을 시작하는 시점에 오류가 발생한다.        
> 이렇게 애플리케이션 로딩 시점에 오류를 인지할 수 있는 것이 스프링 데이터 JPA의 매우 큰 장점이다.

### 4-2. JPA NamedQuery

JPA의 NamedQuery를 호출할 수 있다.

* `@NamedQuery`어노테이션으로 Named 쿼리 정의

#### Member.java (추가) - NamedQuery 추가

```java
package study.datajpa.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;

import static javax.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter @Setter
@NoArgsConstructor(access = PROTECTED)
@ToString(of = {"id", "username", "age"})
@NamedQuery(
        name = "Member.findByUsername",
        query = "select m from Member m where m.username = :username"
)
public class Member {
    // ...
}

```

#### MemberJpaRepository.java (추가) - JPA를 직접 사용해서 Named 쿼리 호출

```java
package study.datajpa.repository;

import org.springframework.stereotype.Repository;
import study.datajpa.entity.Member;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Repository
public class MemberJpaRepository {

    @PersistenceContext
    private EntityManager em;

    // ...

    public List<Member> findByUsername(String username) {
        return em.createNamedQuery("Member.findByUsername", Member.class)
                .setParameter("username", username)
                .getResultList();
    }
}

```

#### MemberRepository.java (추가) - 스프링 데이터 JPA로 NamedQuery 사용

```java
package study.datajpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import study.datajpa.entity.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // ...

    // @Query(name = "Member.findByUsername") // 생략 가능
    List<Member> findByUsername(@Param("username") String username);
}

```

* 스프링 데이터 JPA는 선언한 "도메인 클래서 + .(점) + 메서드 이름"으로 Named 쿼리를 찾아서 실행
* 만약 실행할 Named 쿼리가 없으면 메서드 이름으로 쿼리 생성 전략을 사용한다.
* 필요하면 전략을 변경할 수 있지만 권장하지는 않는다.
    * [참고](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods.query-lookup-strategies)

> 참고: 스프링 데이터 JPA를 사용하면 실무에서 named Query를 직접 등록해서 사용하는 일은 드물다.      
> 대신 `@Query`를 사용해서 리파지토리 메소드에 쿼리를 직접 정의한다.

## Note