package com.jhelper.jserve.web.code;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.jhelper.jserve.web.dto.LargeCodeDto;
import com.jhelper.jserve.web.entity.Lrgclas;
import com.jhelper.jserve.web.entity.Smlclas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.GenericPropertyMatchers;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Transactional
@SpringBootTest
@Slf4j
public class CodeRepositoryTests {

    @Autowired
    LargeCodeRepository largeCodeRepository;

    @Autowired
    SmallCodeRepository smallCodeRepository;

    @BeforeEach
    @DisplayName("before")
    void beforeCode() {
        log.info("before code");
        
        String[] largeCodes = { "WWW01", "WWW02" };

        List<Lrgclas> lrgclasCds = Arrays.stream(largeCodes).map(largeCode -> {
            String largeName = largeCode + "_NM";
            return Lrgclas.builder()
                    .lrgclasCd(largeCode).lrgclasCdNm(largeName).cdDv("SYS").srtSeq(1).useYn("Y")
                    .smlclass(new ArrayList<Smlclas>())
                    .build();
        }).collect(Collectors.toList());

        lrgclasCds.forEach(lrgclasCd -> {
            List<Smlclas> smlclasCds = makeSmallCodes(lrgclasCd.getLrgclasCd());
            lrgclasCd.getSmlclass().addAll(smlclasCds); // cascade 설정했을때 largeCode save시 자동으로 같이 저장한다.
            // lrgclasCd.getSmlclass().addAll( smallCodeRepository.saveAll(smlclasCds) );
        });

        /*
         * casecade = CascadeType.ALL 일 경우에 연관관계 저장시 모두 저장 가능하다.
         * 그렇지 않을 경우 large code와 small code 따로 저장해야 한다.
         * ex)
         * 1. cascade persist 또는 all 일경우
         * largeCode.addAll(smallCodes)
         * largeCode = largeRepository.save(largeCode); // smallCode 까지 모두 저장함
         * 2. cascade 설정이 안할경우
         * smallCodes = smallReposory.saveAll(smallCodes);
         * largeCode.getSmlclass().addAll(smallCodes);
         * laregeCode = largeRepository.save(largeCode);
         */
        lrgclasCds = largeCodeRepository.saveAll(lrgclasCds);
    }

    List<Smlclas> makeSmallCodes(String largeCode) {

        String[] smallCodes = { largeCode + "_S_01", largeCode + "_S_02" };

        return Arrays.stream(smallCodes).map(smallCode -> {
            String smallName = smallCode + "_NM";
            return Smlclas.builder()
                    .lrgclasCd(largeCode)
                    .smlclasCd(smallCode)
                    .smlclasCdNm(smallName)
                    .srtSeq(1)
                    .useYn("Y")
                    .build();
        }).collect(Collectors.toList());
    }

    @Test
    void saveUseYn() {

        Smlclas y = Smlclas.builder().lrgclasCd("USE_YN").smlclasCd("Y").smlclasCdNm("사용").useYn("Y")
                .srtSeq(1).build();
        Smlclas n = Smlclas.builder().lrgclasCd("USE_YN").smlclasCd("N").smlclasCdNm("미사용").useYn("Y")
                .srtSeq(1).build();

        Lrgclas useYn = Lrgclas.builder().lrgclasCd("USE_YN").lrgclasCdNm("사용여부").useYn("Y").cdDv("SYS")
                .smlclass(new ArrayList<>()).build();

        useYn.getSmlclass().add(y);
        useYn.getSmlclass().add(n);

        largeCodeRepository.save(useYn);

    }

    @Test
    @DisplayName("find large code")
    void findLargeCode() {

        log.info("find large code");
        checkEntity(largeCodeRepository.findById("WWW01").orElse(null));

        log.info("find large codes");
        List<Lrgclas> codes = largeCodeRepository
                .findAllById(Arrays.asList(new String[] { "WWW01", "WWW02" }));
        assertThat(codes).size().isGreaterThan(0);
        codes.stream().forEach((lrgclasCd) -> checkEntity(lrgclasCd));

        log.info("find large code in small");
        assertThat(largeCodeRepository.findById("WWW01").get().getSmlclass()).hasSize(2);
    }

    @Test
    @DisplayName("delete large code")
    void deleteLargeCode() {
        log.info("delete large code");
        largeCodeRepository.deleteById("WWW02");
        assertNull(largeCodeRepository.findById("WWW02").orElse(null));
    }

    @Test
    @DisplayName("save small code")
    void saveSamllCode() {

        String largeCode = "WWW01";

        smallCodeRepository.save(
                Smlclas.builder()
                        .lrgclasCd(largeCode)
                        .smlclasCd("TEST01")
                        .smlclasCdNm("테스트01")
                        .useYn("Y")
                        .build());

        Smlclas code = smallCodeRepository.findById(new Smlclas.PK(largeCode, "TEST01")).orElse(null);

        assertNotNull(code);
        assertThat(code.getSmlclasCdNm()).isEqualTo("테스트01");
    }

    @Test
    @DisplayName("find small code")
    void findSmallCode() {

        log.info("find small code");
        Smlclas code = smallCodeRepository
                .findById(new Smlclas.PK("WWW01", "WWW01_S_01"))
                .orElse(null);

        checkEntity(code);

        Smlclas newCode = Smlclas.builder()
                .lrgclasCd(code.getLrgclasCd())
                .smlclasCd(code.getSmlclasCd())
                .smlclasCdNm(code.getSmlclasCdNm())
                .srtSeq(1)
                .memo("test")
                .build();

        newCode = smallCodeRepository.save(newCode);

        assertTrue(code == newCode); // 다른 객체를 생성해서 업데이트해도 동일 데이터가 동기화 되었다.
        assertThat(code.getMemo()).isEqualTo("test");

        log.info("find small codes");
        List<Smlclas> codes = smallCodeRepository.findAllByLrgclasCd("WWW01");
        assertThat(codes).size().isGreaterThan(0);
        codes.forEach((smlclasCd) -> checkEntity(smlclasCd));
    }

    @Test
    @DisplayName("delete small code")
    void deleteSamllCode() {

        String largeCode = "WWW01";

        smallCodeRepository.save(
                Smlclas.builder()
                        .lrgclasCd(largeCode)
                        .smlclasCd("TEST01")
                        .smlclasCdNm("테스트01")
                        .useYn("Y")
                        .build());

        smallCodeRepository.deleteById(new Smlclas.PK(largeCode, "TEST01"));

        Smlclas code = smallCodeRepository.findById(new Smlclas.PK(largeCode, "TEST01")).orElse(null);

        assertNull(code);
    }

    void checkEntity(Smlclas smlclas) {

        assertNotNull(smlclas);

        log.info("check entity: {}", smlclas.toString());

        assertThat(smlclas).isNotNull();

        assertNotNull(smlclas.getLrgclasCd().toString());
        assertNotNull(smlclas.getSrtSeq());
    }

    void checkEntity(Lrgclas lrgclas) {

        assertNotNull(lrgclas);

        log.info("check entity: {}", lrgclas.toString());
        log.info("check entity: {}", lrgclas.getRegDtm());

        assertThat(lrgclas).isNotNull();

        assertNotNull(lrgclas.getLrgclasCd());
        assertNotNull(lrgclas.getLrgclasCdNm());
        assertNotNull(lrgclas.getCdDv());
        assertNotNull(lrgclas.getSrtSeq());
    }

    @Test
    void findById() {
        List<Smlclas> codes = smallCodeRepository.findAll();
        Smlclas code = smallCodeRepository.findById(new Smlclas.PK("WWW01", "WWW01_S_01")).orElseGet(null);

        assertThat(codes).isNotNull().size().isGreaterThan(0);
        assertThat(code).isNotNull();
    }

    @Test
    void exampleOf() {
        Smlclas smclasCondition = new Smlclas();
        smclasCondition.setLrgclasCd("WWW01");

        List<Smlclas> codes = smallCodeRepository.findAll(Example.of(smclasCondition));
        assertThat(codes).isNotNull().size().isGreaterThan(0);
    }

    @Test
    void exampleMatcher() {

        ExampleMatcher exampleMatcher = ExampleMatcher.matching()
                .withMatcher("lrgclasCd", GenericPropertyMatchers.ignoreCase())
                .withMatcher("smlclasCd", GenericPropertyMatchers.ignoreCase())
                .withMatcher("smlclasCdNm", GenericPropertyMatchers.contains());

        Smlclas smclasCondition = new Smlclas();
        smclasCondition.setSmlclasCdNm("WWW");

        List<Smlclas> codes = smallCodeRepository.findAll(Example.of(smclasCondition, exampleMatcher));
        assertThat(codes).isNotNull().size().isGreaterThan(0);
    }

    @Test
    void fetchJoin() {
        List<Lrgclas> codes = largeCodeRepository.findAllWithSmlclass();
        assertNotNull(codes);
    }

    @Test
    void entityGraph() {
        Lrgclas code = largeCodeRepository.findWithSmlclassByLrgclasCd("WWW01").orElse(null);
        assertNotNull(code);
    }
}
