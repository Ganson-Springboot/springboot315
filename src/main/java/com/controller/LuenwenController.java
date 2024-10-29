
package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 论文信息
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/luenwen")
public class LuenwenController {
    private static final Logger logger = LoggerFactory.getLogger(LuenwenController.class);

    @Autowired
    private LuenwenService luenwenService;


    @Autowired
    private TokenService tokenService;
    @Autowired
    private DictionaryService dictionaryService;

    //级联表service
    @Autowired
    private YonghuService yonghuService;



    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("教师".equals(role))
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        if(params.get("orderBy")==null || params.get("orderBy")==""){
            params.put("orderBy","id");
        }
        PageUtils page = luenwenService.queryPage(params);

        //字典表数据转换
        List<LuenwenView> list =(List<LuenwenView>)page.getList();
        for(LuenwenView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        LuenwenEntity luenwen = luenwenService.selectById(id);
        if(luenwen !=null){
            //entity转view
            LuenwenView view = new LuenwenView();
            BeanUtils.copyProperties( luenwen , view );//把实体数据重构到view中

                //级联表
                YonghuEntity yonghu = yonghuService.selectById(luenwen.getYonghuId());
                if(yonghu != null){
                    BeanUtils.copyProperties( yonghu , view ,new String[]{ "id", "createTime", "insertTime", "updateTime"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setYonghuId(yonghu.getId());
                }
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody LuenwenEntity luenwen, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,luenwen:{}",this.getClass().getName(),luenwen.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");
        else if("教师".equals(role))
            luenwen.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));

        Wrapper<LuenwenEntity> queryWrapper = new EntityWrapper<LuenwenEntity>()
            .eq("luenwen_name", luenwen.getLuenwenName())
            .eq("luenwen_types", luenwen.getLuenwenTypes())
            .eq("yonghu_id", luenwen.getYonghuId())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        LuenwenEntity luenwenEntity = luenwenService.selectOne(queryWrapper);
        if(luenwenEntity==null){
            luenwen.setCreateTime(new Date());
            luenwenService.insert(luenwen);
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody LuenwenEntity luenwen, HttpServletRequest request){
        logger.debug("update方法:,,Controller:{},,luenwen:{}",this.getClass().getName(),luenwen.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");
//        else if("教师".equals(role))
//            luenwen.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));
        //根据字段查询是否有相同数据
        Wrapper<LuenwenEntity> queryWrapper = new EntityWrapper<LuenwenEntity>()
            .notIn("id",luenwen.getId())
            .andNew()
            .eq("luenwen_name", luenwen.getLuenwenName())
            .eq("luenwen_types", luenwen.getLuenwenTypes())
            .eq("yonghu_id", luenwen.getYonghuId())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        LuenwenEntity luenwenEntity = luenwenService.selectOne(queryWrapper);
        if("".equals(luenwen.getRenyuanFile()) || "null".equals(luenwen.getRenyuanFile())){
                luenwen.setRenyuanFile(null);
        }
        if(luenwenEntity==null){
            luenwenService.updateById(luenwen);//根据id更新
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }



    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        luenwenService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName, HttpServletRequest request){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        Integer yonghuId = Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId")));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            List<LuenwenEntity> luenwenList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("static/upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            LuenwenEntity luenwenEntity = new LuenwenEntity();
//                            luenwenEntity.setLuenwenName(data.get(0));                    //标题 要改的
//                            luenwenEntity.setLuenwenTypes(Integer.valueOf(data.get(0)));   //论文类型 要改的
//                            luenwenEntity.setYonghuId(Integer.valueOf(data.get(0)));   //教师 要改的
//                            luenwenEntity.setRenyuanFile(data.get(0));                    //证明材料 要改的
//                            luenwenEntity.setLuenwenContent("");//详情和图片
//                            luenwenEntity.setCreateTime(date);//时间
                            luenwenList.add(luenwenEntity);


                            //把要查询是否重复的字段放入map中
                        }

                        //查询是否重复
                        luenwenService.insertBatch(luenwenList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }






}
