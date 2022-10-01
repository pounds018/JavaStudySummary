package cn.pounds;

import cn.pounds.domain.Goods;
import cn.pounds.mapper.GoodsMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * @Project: 07_elasticsearch
 * @Date: 2020/10/29 17:51
 * @author: by Martin
 * @Description: TODO
 */
@SpringBootTest
public class MybatisEnvironmentTest {

    @Resource
    private GoodsMapper goodsMapper;

    @Test
    public void Test(){
        QueryWrapper<Goods> goodsQueryWrapper = new QueryWrapper<Goods>().eq("id", 536563);
        Goods goods = goodsMapper.selectOne(goodsQueryWrapper);
        System.out.println(goods);
    }
}
