package study.datajpa.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.datajpa.dto.MemberDto;
import study.datajpa.entity.Member;
import study.datajpa.entity.Team;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Rollback(false)
class MemberRepositoryTest {

    @Autowired MemberRepository memberRepository;
    @Autowired TeamRepository teamRepository;

    @Test
    public void testMember() {
        Member member = new Member("memberA");
        Member savedMember = memberRepository.save(member);

        Member findMember = memberRepository.findById(savedMember.getId()).orElseThrow();

        assertThat(findMember.getId()).isEqualTo(member.getId());
        assertThat(findMember.getUsername()).isEqualTo(member.getUsername());
        assertThat(findMember).isEqualTo(member);
    }

    @Test
    @DisplayName("단순한 CRUD 테스트")
    public void basicCRUD() {
        Member member1 = new Member("member1");
        Member member2 = new Member("member2");
        memberRepository.save(member1);
        memberRepository.save(member2);

        // 단건 조회 검증
        Member findMember1 = memberRepository.findById(member1.getId()).orElseThrow();
        Member findMember2 = memberRepository.findById(member2.getId()).orElseThrow();
        assertThat(findMember1).isEqualTo(member1);
        assertThat(findMember2).isEqualTo(member2);

        // 리스트 조회 검증
        List<Member> all = memberRepository.findAll();
        assertThat(all.size()).isEqualTo(2);

        // 카우트 검증
        long count = memberRepository.count();
        assertThat(count).isEqualTo(2);

        // 삭제 검증
        memberRepository.delete(member1);
        memberRepository.delete(member2);

        long deletedCount = memberRepository.count();
        assertThat(deletedCount).isEqualTo(0);
    }

    @Test
    @DisplayName("JPA로 구현한 특정 이름을 가진 회원중 특정 나이보다 많은 회원 조회")
    public void findByUsernameAndAgeGreaterThen() {
        // given
        Member m1 = new Member("AAA", 10);
        Member m2 = new Member("AAA", 20);

        memberRepository.save(m1);
        memberRepository.save(m2);

        // when
        List<Member> result = memberRepository.findByUsernameAndAgeGreaterThan("AAA", 15);

        // then
        assertThat(result.get(0).getUsername()).isEqualTo("AAA");
        assertThat(result.get(0).getAge()).isEqualTo(20);
        assertThat(result.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("NamedQuery 테스트 - 이름으로 회원 조회")
    public void testNamedQuery() {
        // given
        Member m1 = new Member("AAA", 10);
        Member m2 = new Member("BBB", 20);

        memberRepository.save(m1);
        memberRepository.save(m2);

        // when
        List<Member> result = memberRepository.findByUsername("AAA");

        // then
        assertThat(result).contains(m1);
    }

    @Test
    @DisplayName("리포지토리 메소드에 쿼리 정의")
    public void testQuery() {
        // given
        Member m1 = new Member("AAA", 10);
        Member m2 = new Member("BBB", 20);

        memberRepository.save(m1);
        memberRepository.save(m2);

        // when
        List<Member> result = memberRepository.findUser("AAA", 10);

        // then
        assertThat(result).contains(m1);
        assertThat(result.get(0)).isEqualTo(m1);
    }

    @Test
    @DisplayName("DTO 조회하기 - String")
    public void findUsernameList() {
        // given
        Member m1 = new Member("AAA", 10);
        Member m2 = new Member("BBB", 20);

        memberRepository.save(m1);
        memberRepository.save(m2);

        // when
        List<String> result = memberRepository.findUsernameList();

        // then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    @DisplayName("DTO 조회하기 - MemberDto")
    public void findMemberDto() {
        // given
        Member m1 = new Member("AAA", 10);
        Team team = new Team("teamA");

        m1.changeTeam(team);

        memberRepository.save(m1);
        teamRepository.save(team);

        // when
        List<MemberDto> result = memberRepository.findMemberDto();

        // then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    @DisplayName("파라미터 바인딩")
    public void findByNames() {
        // given
        Member m1 = new Member("AAA", 10);
        Member m2 = new Member("BBB", 20);

        memberRepository.save(m1);
        memberRepository.save(m2);

        // when
        List<Member> result = memberRepository.findByNames(Arrays.asList("AAA", "BBB"));

        // then
        result.stream().map(m -> "s = " + m).forEach(System.out::println);
    }

    @Test
    @DisplayName("ddd")
    public void returnType() {
        // given
        Member m1 = new Member("AAA", 10);
        Member m2 = new Member("BBB", 20);

        memberRepository.save(m1);
        memberRepository.save(m2);

        // when
        List<Member> resultByCollection = memberRepository.findListByUsername("AAA");
        Member resultByOnlyOne = memberRepository.findMemberByUsername("AAA");
        Optional<Member> resultByOptional = memberRepository.findOptionalByUsername("AAA");

        List<Member> resultByCollectionNoExit = memberRepository.findListByUsername("AAAAA");
        System.out.println("result4 = " + resultByCollectionNoExit.size());

        Member resultByOnlyOneNoExit = memberRepository.findMemberByUsername("dfadf");
        System.out.println("result5 = " + resultByOnlyOneNoExit);

        Optional<Member> resultByOptionalNoExit = memberRepository.findOptionalByUsername("adfadf");
        System.out.println("resultByOptionalNoExit = " + resultByOptionalNoExit);
    }
}