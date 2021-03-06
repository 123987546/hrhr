package com.wiwj.appinterface.Contoller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.wiwj.appinterface.Dao.DepartmentInfoRepository;
import com.wiwj.appinterface.Dao.DeptCompareInfoRepository;
import com.wiwj.appinterface.Dao.UserCompareInfoRepository;
import com.wiwj.appinterface.Exception.MyException;
import com.wiwj.appinterface.Model.DepartmentInfo;
import com.wiwj.appinterface.Model.ResponseObj;
import com.wiwj.appinterface.Model.StatusCode;
import com.wiwj.appinterface.Result.DeptCompareInfo;
import com.wiwj.appinterface.Result.FeiShuDepartment;
import com.wiwj.appinterface.Result.SimpleFeishuUser;
import com.wiwj.appinterface.ServiceImpl.DepartmentServiceImpl;
import com.wiwj.appinterface.ServiceImpl.FeiShuApi;
import com.wiwj.appinterface.ServiceImpl.UserServiceImpl;
import com.wiwj.appinterface.ToolUtil.LzyApp;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(value = "/api")
public class TestController {

    @Autowired
    FeiShuApi feiShuApi;
    @Autowired
    DepartmentServiceImpl departmentService;
    @Autowired
    UserServiceImpl userService;
    @Autowired
    DeptCompareInfoRepository deptCompareInfoRepository;
    @Autowired
    UserCompareInfoRepository userCompareInfoRepository;
    @Autowired
    DepartmentInfoRepository departmentInfoRepository;
    @Autowired
    LzyApp lzyApp;

    Logger log = LoggerFactory.getLogger(this.getClass());

    @RequestMapping(value = "/doMain",method = RequestMethod.GET)
    public void doMain(@RequestParam(value = "fdDate",defaultValue = "")String fdDate){
        List<FeiShuDepartment> deptAddList=new ArrayList<>();
        List<FeiShuDepartment> deptUpdateList=new ArrayList<>();
        List<FeiShuDepartment> deptDeleteList=new ArrayList<>();
        List<DepartmentInfo> departmentInfos=new ArrayList<>();
        List<DeptCompareInfo> addDeptCompareInfos=new ArrayList<>();
        List<DeptCompareInfo> updateDeptCompareInfos=new ArrayList<>();
        List<DeptCompareInfo> deleteDeptCompareInfos=new ArrayList<>();
        List<SimpleFeishuUser> userAddList=new ArrayList<>();
        List<SimpleFeishuUser> userUpdateList=new ArrayList<>();
        List<SimpleFeishuUser> userDeleteList=new ArrayList<>();
        try {
            //String TenanAccessToken = departmentService.getTenanAccessToken();
            //获得部门数据并新增部门反写数据
            addAllDepartment(fdDate, deptAddList,addDeptCompareInfos, deptUpdateList,updateDeptCompareInfos, deptDeleteList,deleteDeptCompareInfos,departmentInfos);
            log.info("开始获取user信息");
            addAllUserNew(fdDate, userAddList, userUpdateList, userDeleteList);
            log.info("结束获取user信息");
            //修改部门
            log.info("开始修改部门");
            for(FeiShuDepartment feiShuDepartment:deptUpdateList){
                String TenanAccessToken = departmentService.getTenanAccessToken();
                String re=feiShuApi.updateDepartment(TenanAccessToken,feiShuDepartment);
                log.info("修改部门："+JSON.toJSONString(feiShuDepartment));
            }
            log.info("结束修改部门");
            //反写修改部门结果
            for(DeptCompareInfo deptCompareInfo:updateDeptCompareInfos){
                deptCompareInfoRepository.save(deptCompareInfo);
            }
            //新增人员
            log.info("开始新增人员");
                for(SimpleFeishuUser simpleFeishuUser:userAddList) {
                    //log.info("controller中的user："+JSON.toJSONString(simpleFeishuUser));
                    JSONObject jsonObject = JSONObject.parseObject(JSON.toJSONString(simpleFeishuUser));
                    JSONObject custom_attrs=new JSONObject();
                    JSONObject postKey=new JSONObject();
                    if((!StringUtils.isEmpty(simpleFeishuUser.getPost()))&&(!StringUtils.equals("null",simpleFeishuUser.getPost()))) {
                        postKey.put("value",simpleFeishuUser.getPost());
                        custom_attrs.put(lzyApp.getJobpost_key(),postKey);
                        jsonObject.put("custom_attrs",custom_attrs);
                    }
                    //log.info("controller中的jsonObject:"+jsonObject.toString());
                    //获取系统生成的飞书id
                    String feishuId="";
                    try {
                        String TenanAccessToken = departmentService.getTenanAccessToken();
                        feishuId = feiShuApi.addUser(TenanAccessToken, jsonObject);
                        log.info("新增人员："+jsonObject.toString());
                    }catch (Exception e){
                        log.error("飞书新增人员异常："+jsonObject.toString());
                        log.info("飞书新增人员异常："+jsonObject.toString());
                    }
                    log.info("controller中飞书返回的id："+feishuId);
                    //反写新增人员结果
                    if(!(StringUtils.isEmpty(feishuId))){
                        userService.doChangeUserInfoAdd(simpleFeishuUser, feishuId);
                    }
                }
                log.info("结束新增人员");
            //修改人员
            log.info("修改人员开始");
            for (SimpleFeishuUser simpleFeishuUser : userUpdateList) {
                try {
                    String TenanAccessToken = departmentService.getTenanAccessToken();
                    feiShuApi.updateFeiShuUser(TenanAccessToken, simpleFeishuUser);
                    log.info("修改人员："+JSON.toJSONString(simpleFeishuUser));
                }catch (Exception e){
                    log.error("飞书修改人员异常："+JSON.toJSONString(simpleFeishuUser));
                    log.info("飞书修改人员异常："+JSON.toJSONString(simpleFeishuUser));
                }
                log.info("修改人员："+JSON.toJSONString(simpleFeishuUser));
                //反写修改人员结果
                userService.doChangeUserInfo(simpleFeishuUser);
            }
            log.info("修改人员结束");
            //删除人员
            log.info("删除人员开始");
            for (SimpleFeishuUser simpleFeishuUser : userDeleteList) {
                try {
                    String TenanAccessToken = departmentService.getTenanAccessToken();
                    feiShuApi.simpleDeleteUser(TenanAccessToken, simpleFeishuUser.getMyUserId());
                    log.info("删除人员："+JSON.toJSONString(simpleFeishuUser));
                }catch (Exception e){
                    log.error("飞书删除人员异常："+JSON.toJSONString(simpleFeishuUser));
                    log.info("飞书删除人员异常："+JSON.toJSONString(simpleFeishuUser));
                }
                //反写结果
                userCompareInfoRepository.deleteByEMPL_ID(simpleFeishuUser.getEMPL_ID());
            }
            log.info("删除人员结束");
            log.info("删除部门开始");
            //删除部门
            for(FeiShuDepartment feiShuDepartment:deptDeleteList){
                String TenanAccessToken = departmentService.getTenanAccessToken();
                feiShuApi.deleteDepartment(TenanAccessToken,feiShuDepartment.getId());
                log.info("删除部门："+JSON.toJSONString(feiShuDepartment));
            }
            //反写删除部门结果
            for(DeptCompareInfo deptCompareInfo:deleteDeptCompareInfos){
                deptCompareInfoRepository.deleteByDEPT_IDAndSET_ID(deptCompareInfo.getDEPT_ID(),deptCompareInfo.getSET_ID());
            }
            log.info("删除部门结束");
            //更新部门负责人
            updateLeader(deptUpdateList,departmentInfos,fdDate);
            //更新人员直接负责人
            for (SimpleFeishuUser simpleFeishuUser : userUpdateList) {
                String TenanAccessToken = departmentService.getTenanAccessToken();
                feiShuApi.updateFeiShuUserLeader(TenanAccessToken, simpleFeishuUser);
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("主程序出错");
        }
    }

    /**
     * 添加所有层级部门
     * @return
     */
    public ResponseObj addAllDepartment(String fdDate, List<FeiShuDepartment> addList, List<DeptCompareInfo> addDeptCompareInfos, List<FeiShuDepartment> updateList, List<DeptCompareInfo> updateDeptCompareInfos, List<FeiShuDepartment> deleteList, List<DeptCompareInfo> deleteDeptCompareInfos, List<DepartmentInfo> departmentInfos){
        int maxLevel;
        String TenanAccessToken="";
        try {
            TenanAccessToken=departmentService.getTenanAccessToken();
            //获得层级数量
            maxLevel=departmentService.getTreeLevel();
            //获取deleteList
            log.info("获取删除部门开始");
            List<DeptCompareInfo> deptDeleteListInMytest= deptCompareInfoRepository.findAll();
            for(DeptCompareInfo deptCompareInfo:deptDeleteListInMytest){
                List<DepartmentInfo> delDepartmentInfos=departmentInfoRepository.findByDeptIdAndSetIdWithoutStatus(deptCompareInfo.getDEPT_ID(),deptCompareInfo.getSET_ID());
                //OA表中不存在或者失效则加入删除表
                if(delDepartmentInfos.size()==0||StringUtils.equals(delDepartmentInfos.get(0).getEFF_STATUS(),"I")){
                    FeiShuDepartment feiShuDepartment=departmentService.getFeishuDepartment(delDepartmentInfos.get(0),deptCompareInfo.getPARENT_NODE_NAME(),TenanAccessToken);
                    deleteList.add(feiShuDepartment);
                    log.info("删除部门列表新增："+JSON.toJSONString(feiShuDepartment));
                    deleteDeptCompareInfos.add(deptCompareInfo);
                }
            }
            log.info("获取删除部门结束");
            for(int level=1;level<=maxLevel;level++) {
                //获得该层级新增，修改list
                departmentService.addDepartmentByLevelNew(level,fdDate, addList,addDeptCompareInfos, updateList,updateDeptCompareInfos, deleteList,deleteDeptCompareInfos,departmentInfos);
            }

        } catch (Exception ex) {
            MyException mex = new MyException(ex);
            ResponseObj re = new ResponseObj(mex.getStatuscode(),mex.getMessage(),mex.getData());
            ex.printStackTrace();
            log.error("添加到部门列表错误" + re.tojson());
            return  re;
        }
        return  new ResponseObj(StatusCode.success,null,null);
    }

    /**
     * 获取所有人员列表
     * @param FD_HR_DATE
     * @param addList
     * @param updateList
     * @param deleteList
     * @return
     */
    public ResponseObj addAllUserNew(String FD_HR_DATE,List<SimpleFeishuUser> addList,List<SimpleFeishuUser> updateList,List<SimpleFeishuUser> deleteList){
        //String TenanAccessToken="";
        try {
            //TenanAccessToken=userService.getTenanAccessToken();
            //获得新增，修改，删除list
            userService.batchAddUser(FD_HR_DATE,addList,updateList,deleteList);
        } catch (Exception ex) {
            MyException mex = new MyException(ex);
            ResponseObj re = new ResponseObj(mex.getStatuscode(),mex.getMessage(),mex.getData());
            ex.printStackTrace();
            log.error("批量添加部门错误" + re.tojson());
            return  re;
        }
        return  new ResponseObj(StatusCode.success,null,null);
    }

    /**
     * 更新部门负责人
     * @param deptupdateList
     * @param departmentInfos
     * @param fdDate
     */
    public void updateLeader(List<FeiShuDepartment> deptupdateList, List<DepartmentInfo> departmentInfos,String fdDate){
        String TenanAccessToken="";
        try {
            TenanAccessToken=userService.getTenanAccessToken();
            for(int i=0;i<deptupdateList.size();i++){
                FeiShuDepartment feiShuDepartment=deptupdateList.get(i);
                DepartmentInfo departmentInfo=departmentInfos.get(i);
                if((StringUtils.isEmpty(feiShuDepartment.getLeader_user_id())||StringUtils.equals("null",feiShuDepartment.getLeader_user_id()))&& (StringUtils.isEmpty(feiShuDepartment.getLeader_open_id())||StringUtils.equals("null",feiShuDepartment.getLeader_open_id()))){
                    String setId=departmentInfo.getSET_ID();
                    String deptId=departmentInfo.getDEPT_ID();
                    List<SimpleFeishuUser> leaders=userService.getLeader(fdDate,setId,deptId,departmentInfo.getMANAGER_POSN());
                    SimpleFeishuUser leader=leaders.get(0);
                    feiShuDepartment.setLeader_user_id(leader.getLeader_employee_id());
                }
                feiShuApi.updateDepartmentLeader(TenanAccessToken,feiShuDepartment);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
