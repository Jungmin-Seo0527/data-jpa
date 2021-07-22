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

### 4-3. @Query, 리포지토리 메소드에 쿼리 정의하기

#### MemberRepository.java (추가) - 메서드에 JPQL 쿼리 작성

```java
package study.datajpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import study.datajpa.entity.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // ...

    @Query("select m from Member m where m.username = :username and m.age = :age")
    List<Member> findUser(@Param("username") String username, @Param("age") int age);
}

```

* `org.springfrmaework.data.jpa.repository.Query`어노테이션을 사용
* 실행할 메서드에 정적 쿼리를 직접 작성하므로 이름 없는 Named 쿼리라 할 수 있음
* JPA Named 쿼리처럼 애플리케이션 실행 시점에 문법 오류를 발견할 수 있음(매우 큰 장점!)

> 참고: 실무에서는 메소드 이름으로 쿼리 생성 기능은 파라미터가 증가하면 메서드 이름이 매우 지저분해진다. 따라서 `@Query`기능을 자주 사용하게 된다.

### 4-4. @Query, 값, DTO 조회하기

#### MemberRepository.java (추가) - 단순히 값 하나를 조회

```java
package study.datajpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import study.datajpa.dto.MemberDto;
import study.datajpa.entity.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // ...

    @Query("select m.username from Member m")
    List<String> findUsernameList();
}

```

* JPA 값 타입 (`@Embedded`)도 이 방식으로 조회할 수 있다.

#### MemberRepository.java (추가) - DTO 로 직접 조회

```java
package study.datajpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import study.datajpa.dto.MemberDto;
import study.datajpa.entity.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // ...

    @Query("select new study.datajpa.dto.MemberDto(m.id, m.username, t.name) from Member m join m.team t")
    List<MemberDto> findMemberDto();
}

```

> 주의!       
> DTO로 직접 조회 하려면 JPA의 `new`명령어를 사용해야 한다. 그리고 다음과 같이 생성자가 맞는 DTO가 필요하다. (JPA와 사용방식이 동일하다.)

#### MemberDto.java

* `src/main/java/study/datajpa/dto/MemberDto.java`

```java
package study.datajpa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MemberDto {

    private Long id;
    private String username;
    private String teamName;
}

```

### 4-5. 파라미터 바인딩

* 위치 기반
* 이름 기반

```
select m from Member m where m.username = ?0 // 위치 기반 
select m from Member m where m.username = :name // 이름 기반
```

> 참고: 코드 가독성과 유지보수를 위해 이름 기반 파라미터 바인딩을 사용하자 (위치 기반은 순서 실수가 바꾸면...)

#### MemberRepository.java (추가) - 컬렉션 파라미터 바인딩(Collection 타입으로 in절 지원)

```java
package study.datajpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import study.datajpa.dto.MemberDto;
import study.datajpa.entity.Member;

import java.util.Collection;
import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // ...

    @Query("select m from Member m where m.username in :names")
    List<Member> findByNames(@Param("names") Collection<String> names);
}

```

## Note