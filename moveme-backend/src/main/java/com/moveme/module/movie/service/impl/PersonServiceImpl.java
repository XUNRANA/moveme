package com.moveme.module.movie.service.impl;

import com.moveme.common.constant.RedisKeyConstants;
import com.moveme.module.movie.entity.Person;
import com.moveme.module.movie.mapper.PersonMapper;
import com.moveme.module.movie.service.PersonService;
import com.moveme.module.movie.vo.PersonDetailVO;
import com.moveme.module.movie.vo.PersonDetailVO.FilmographyItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService {

    private final PersonMapper personMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public PersonDetailVO getPersonDetail(Long id) {
        String cacheKey = RedisKeyConstants.PERSON_DETAIL_PREFIX + id;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof PersonDetailVO vo) {
            return vo;
        }

        Person person = personMapper.selectById(id);
        if (person == null) {
            return null;
        }

        PersonDetailVO vo = new PersonDetailVO();
        vo.setId(person.getId());
        vo.setName(person.getName());
        vo.setNameEn(person.getNameEn());
        vo.setAvatarUrl(person.getAvatarUrl());
        vo.setAvatarLocalPath(person.getAvatarLocalPath());
        vo.setGender(formatGender(person.getGender()));
        vo.setBirthDate(person.getBirthDate() != null ? person.getBirthDate().toString() : null);
        vo.setBirthPlace(person.getBirthPlace());
        vo.setBio(person.getBio());
        vo.setMovieCount(person.getMovieCount());
        vo.setAvgMovieRating(person.getAvgMovieRating());

        // Filmography
        List<FilmographyItem> directed = personMapper.selectDirectedMoviesByPersonId(id);
        List<FilmographyItem> written = personMapper.selectWrittenMoviesByPersonId(id);
        List<FilmographyItem> acted = personMapper.selectActedMoviesByPersonId(id);
        vo.setDirected(directed);
        vo.setWritten(written);
        vo.setActed(acted);

        redisTemplate.opsForValue().set(cacheKey, vo, 30, TimeUnit.MINUTES);
        return vo;
    }

    private String formatGender(Integer gender) {
        if (gender == null || gender == 0) return "未知";
        if (gender == 1) return "男";
        if (gender == 2) return "女";
        return "未知";
    }
}
