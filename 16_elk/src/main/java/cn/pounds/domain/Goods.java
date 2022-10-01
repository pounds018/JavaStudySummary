package cn.pounds.domain;


import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;
import java.util.Map;

/**
 * @Project: 07_elasticsearch
 * @Date: 2020/10/29 17:31
 * @author: by Martin
 * @Description: TODO
 */
@Data
@TableName("goods")
public class Goods {
    @TableId(value="id",type = IdType.AUTO)
    private Integer id;
    private String title;
    private double price;
    private int stock;
    private int saleNum;
    private Date createTime;
    private String categoryName;
    private String brandName;
    @TableField(exist = false)
    private Map spec;
    @JSONField(serialize = false) // 作用 : 这个字段不会转化成json
    @TableField(value="spec")
    private String specDatabaseStr;


}
