# 스프링 데이터 JPA

## 6. 스프링 데이터 JPA 분석

### 6-1. 스프링 데이터 JPA 구현체 분석

* 스프링 데이터 JPA가 제공하는 공통 인터페이스의 구현체
* `org.springfrmaework.data.jpa.repository.support.SimpleJpaRepository`

#### SimpleJpaRepository.java

```java
@Repository
@Transactional(readOnly = true)
public class SimpleJpaRepository<T, ID> {
    @Transactional
    public <S extends T> S save(S entity) {
        if (entityInformation.isNew(entity)) {
            em.persist(entity);
            return entity;
        } else {
            return em.merge(entity);
        }
    }
    //...
}

```

* `@Repository`적용: JPA 예외를 스프링이 추상화한 예외로 변환
* `@Transaction`트랜잭션 적용
    * JPA의 모든 변경은 트랜잭션 안에서 동작
    * 스프링 데이터 JPA는 변경(등록, 수정, 삭제) 메서드를 트랜잭션 처리
    * 서비스 계층에서 트랜잭션을 시작하지 않으면 리파지토리에서 트랜잭션 시작
    * 서비스 계층에서 트랜잭션을 시작하면 리파지토리는 해당 트랜잭션을 전파 받아서 사용
    * 그래서 스프링 데이터 JPA를 사용할 때 트랜잭션이 없어도 데이터 등록, 변경이 가능했음(사실은 트랜잭션이 리포지토리 계층에 걸려있는 것임)

* `@Transactional(readOnly = true)`
    * 데이터를 단순히 조회만 하고 변경하지 않는 트랜잭션에서 `readyOnly = true`옵션을 사용하면 플러시를 생략해서 약간의 성능 향상을 얻을 수 있음

* **매우 중요!!!**
    * `save()`메서드
        * 새로운 엔티티면 저장(`persist`)
        * 새로운 엔티티가 아니면 병합(`merge`)

## Note