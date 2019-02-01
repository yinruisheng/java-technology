package com.littlefxc.example.dao;

import com.littlefxc.example.domain.City;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
 * @author fengxuechao
 * @date 2018/12/27
 **/
@Repository
@Mapper
public interface CityDao {

    /**
     * 根据主键查询城市
     *
     * @param id
     * @return
     */
    @Select("select id, province_id, city_name, description from city where id = #{id}")
    City findCityById(Integer id);

    /**
     * 保存城市
     * <p>
     * 返回自增主键
     * </p>
     *
     * @param city
     * @return
     */
    @Insert("insert into city(province_id, city_name, description) values(#{province_id}, #{city_name}, #{description})")
    int save(City city);
}
