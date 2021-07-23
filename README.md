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

### 4-6. 반환 타입

스프링 데이터 JPA는 유연한 반환 타입 지원

```
List<Member> findByUsername(String name); // 컬렉션
Member findByUsername(String name); // 단건
Optional<Member> findByUsername(String name); // 단건 Optional
```

* [스프링 데이터 JPA 공식 문서](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repository-query-return-types)

* 조회 결과가 많거나 없으면?
    * 컬렉션
        * 결과 없음: 빈 컬렉션 반환
    * 단건조회
        * 결과 없음: `null` 반환
        * 결과가 2건 이상: `javax.persistenc.NonUniqueResultException`예외 발생

> 참고: 단건으로 지정한 메서드를 호출하면 스프링 데이터 JPA는 내부에서 JPQL의 `Query.getSingleReuslt()`메서드를 호출한다. 이 메서드를 호출했을 때 조회 결과가 없으면 `javax.persistence.NoResultException`예외가 발생하는데 개발자 입장에서 다루기가 상당히 불현하다. 스프링 데이터 JPA는 단건을 조회할 때 이 예외가 발생하면 예외를 무시하고 대신에 `null`을 반환한다.

### 4-7. 순수 JPA 페이징과 정렬

JPA에서 페이징을 어떻게 할 것인가?

다음 조건으로 페이징과 정렬을 사용하는 예제 코드를 본다.

* 검색 조건: 나이가 10살
* 정렬 조건: 이름으로 내림차순
* 페이징 조건: 첫 번째 페이지, 페이지당 보여줄 데이터는 3건

#### MemberJpaRepository.java (추가) - JPA 페이징 리포지토리 코드

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

    public List<Member> findByPage(int age, int offset, int limit) {
        return em.createQuery("select m from Member m where m.age = :age order by m.username desc", Member.class)
                .setParameter("age", age)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    public long totalCount(int age) {
        return em.createQuery("select count(m) from Member m where m.age = :age", Long.class)
                .setParameter("age", age)
                .getSingleResult();
    }
}

```

#### MemberJpaRepositoryTest.java (추가) - JPA 페이징 테스트 코드

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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Rollback(false)
class MemberJpaRepositoryTest {

    @Autowired MemberJpaRepository memberJpaRepository;

    // ...

    @Test
    @DisplayName("페이징")
    public void paging() {
        // given
        IntStream.rangeClosed(1, 10).forEach(i -> memberJpaRepository.save(new Member("member" + i, 10)));

        int age = 10;
        int offset = 1;
        int limit = 3;

        // when
        List<Member> members = memberJpaRepository.findByPage(age, offset, limit);
        long totalCount = memberJpaRepository.totalCount(age);

        // then
        assertThat(members.size()).isEqualTo(3);
        assertThat(totalCount).isEqualTo(10);
    }
}
```

### 4-8. 스프링 데이터 JPA 페이징과 정렬

* 페이징과 정렬 파라미터
    * `org.springframework.data.domain.Sort`: 정렬 기능
    * `org.springframework.data.domain.Pageable`: 페이징 기능(내부에 `Sort` 포함)

* 특별한 반환 타입
    * `org.springframework.data.domain.Page`: 추가 count 쿼리 결과를 포함하는 페이징
    * `org.springframework.data.domain.Slice`: 추가 count 쿼리 없이 다음 페이지만 확인 가능(내부적으로 limit + 1 조회)
    * `List`(자바 컬렉션): 추가 count 쿼리 없이 결과만 반환

* 페이징과 정렬 사용 예제

```
Page<Member> findByUsername(String name, Pageable pageable); // count 쿼리 사용

Slice<Member> findByUsername(String name, Pageable pageable); // count 쿼리 사용 안함

List<Member> findByUsername(String name, Pageable pageable); // count 쿼리 사용 안함

List<Member> findByUsername(String name, Sort sort);
```

* 다음 조건으로 페이징과 정렬을 사용하는 예제 코드를 보자.
    * 검색 조건: 나이가 10살
    * 정렬 조건: 이름으로 내림차순
    * 페이징 조건: 첫 번째 페이지, 페이지당 보여줄 데이터는 3건

#### MemberRepository.java (추가) - Page 사용 예제 정의 추가

```java
package study.datajpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import study.datajpa.dto.MemberDto;
import study.datajpa.entity.Member;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // ...

    Page<Member> findByAge(int age, Pageable pageable);
}

```

#### MemberRepositoryTest.java - Page 사용 예제 테스트 코드

```java
public class MemberRepositoryTest {

    // ...

    @Test
    @DisplayName("페이징")
    public void paging() {
        // given
        IntStream.range(0, 5).forEach(i -> memberRepository.save(new Member("member" + (i + 1), 10)));

        int age = 10;
        PageRequest pageRequest = PageRequest.of(0, 3, Sort.Direction.DESC, "username");

        // when
        Page<Member> page = memberRepository.findByAge(age, pageRequest);

        Page<MemberDto> toMap = page.map(member -> new MemberDto(member.getId(), member.getUsername(), null));

        // then
        List<Member> content = page.getContent();

        assertThat(content.size()).isEqualTo(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getNumber()).isEqualTo(0);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.isFirst()).isTrue();
        assertThat(page.hasNext()).isTrue();
    }
}
```

* 두 번째 파라미터로 받은 `Pagable`은 인터페이스다. 따라서 실제 사용할 때는 해당 인터페이스를 구현한 `org.springframework.data.domain.PageRequest`객체를 사용한다.
* `PageRequest`생성자의 첫 번째 파라미터에는 현재 페이지를 두 번째 파라미터에는 조회할 데이터 수를 입력한다. 여기에 추가로 정렬 정보도 파라미터로 사용할 수 있다. 참고로 페이지는 0부터 시작한다.

> 주의: Page는 1부터 시작이 아니라 0부터 시작이다.

#### Page 인터페이스

```java
public interface Page<T> extends Slice<T> {
    int getTotalPages(); //전체 페이지 수

    long getTotalElements(); //전체 데이터 수

    <U> Page<U> map(Function<? super T, ? extends U> converter); //변환기
}
```

#### Slice 인터페이스

```java
public interface Slice<T> extends Streamable<T> {
    int getNumber(); //현재 페이지

    int getSize(); //페이지 크기

    int getNumberOfElements(); //현재 페이지에 나올 데이터 수

    List<T> getContent(); //조회된 데이터

    boolean hasContent(); //조회된 데이터 존재 여부

    Sort getSort(); //정렬 정보

    boolean isFirst(); //현재 페이지가 첫 페이지 인지 여부

    boolean isLast(); //현재 페이지가 마지막 페이지 인지 여부

    boolean hasNext(); //다음 페이지 여부

    boolean hasPrevious(); //이전 페이지 여부

    Pageable getPageable(); //페이지 요청 정보

    Pageable nextPageable(); //다음 페이지 객체

    Pageable previousPageable();//이전 페이지 객체

    <U> Slice<U> map(Function<? super T, ? extends U> converter); //변환기
}
```

#### count 쿼리 분리

```
@Query(value = "select m from Member m",
       countQuery = "select count(m.username) from Member m")
Page<Member> findMemberAllCountBy(Pageable pageable);
```

* 데이터는 join이 필요하지만 카운터 할때는 join이 필요하지 않는 경우 성능 취적화를 위해서 count 쿼리문을 따로 만들 수 있다.

> 참고: 전체 count 쿼리는 매우 무겁다.

#### Top, First 사용 참고

[참고](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.limit-query-result)

`List<Member> findTop3By();`

#### 페이지를 유지하면서 엔티티를 DTO로 변환하기

```
Page<Member> page = memberRepository.findByAge(10, pageRequest);
page<MemberDto> dtoPage = page.map(MemberDto::new);
```

### 4-9. 벌크성 수정 쿼리

#### MemberJpaRepository.java (추가) - JPA를 사용한 벌크성 수정 쿼리

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

    public int bulkAgePlus(int age) {
        return em.createQuery("update Member m set m.age = m.age + 1 where m.age >= :age")
                .setParameter("age", age)
                .executeUpdate();
    }
}

```

#### MemberRepositoryTest.java (추가) - JPA를 사용한 벌크성 수정 쿼리 테스트

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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Rollback(false)
class MemberJpaRepositoryTest {

    @Autowired MemberJpaRepository memberJpaRepository;

    // ...

    @Test
    @DisplayName("벌크성 수정 쿼리")
    public void bulkUpdate() {
        // given
        memberJpaRepository.save(new Member("member1", 10));
        memberJpaRepository.save(new Member("member2", 19));
        memberJpaRepository.save(new Member("member3", 20));
        memberJpaRepository.save(new Member("member4", 21));
        memberJpaRepository.save(new Member("member5", 40));

        // when
        int resultCount = memberJpaRepository.bulkAgePlus(20);

        // then
        assertThat(resultCount).isEqualTo(3);
    }
}
```

#### MemberRepository.java (추가) - 스프링 데이터 JPA를 사용한 벌크성 수정 쿼리

```java
package study.datajpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import study.datajpa.dto.MemberDto;
import study.datajpa.entity.Member;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // ...

    @Modifying(clearAutomatically = true)
    @Query("update Member m set m.age = m.age + 1 where m.age >= :age")
    int bulkAgePlus(@Param("age") int age);
}

```

* 벌크성 수정, 삭제 쿼리는 `@Modifying`어노테이션을 사용
    * 사용하지 않으면 다음 예외 발생
    * `org.hibernate.hql.internal.QueryExecutionRequestException: Not supported for DML operations`
* 벌크성 쿼리를 실행하고 나서 영속성 컨텍스트 초기화: `@Modifying(clearAutomatically = true)` (이 옵션의 기본값은 `false`)
    * 이 옵션 없이 회원을 `findById`로 조회하면 영속성 컨텍스트에 과거 값이 남아서 문제가 될 수 있다. 만약 다시 조회해야 하면 꼭 영속성 컨텍스트를 초기화하자.

> 참고: 벌크 연산은 영속성 컨텍스트를 무시하고 실행하기 때문에, 영속성 컨텍스트에 있는 엔티티의 상태와 DB에 엔티티 상태가 달라질 수 있다.       
> 권장하는 방안
> 1. 영속성 컨텍스트에 엔티티가 없는 상태에서 벌크 연산을 먼저 실행한다.
> 2. 부득이하게 영속성 컨텍스트에 엔티티가 있으면 벌크 연산 직후 영속성 컨텍스트를 초기화 한다.

## Note