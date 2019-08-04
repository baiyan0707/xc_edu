package com.xuecheng.ucenter.service;


import com.xuecheng.framework.domain.ucenter.XcCompanyUser;
import com.xuecheng.framework.domain.ucenter.XcMenu;
import com.xuecheng.framework.domain.ucenter.XcUser;
import com.xuecheng.framework.domain.ucenter.ext.XcUserExt;
import com.xuecheng.ucenter.dao.XcCompanyUserRepository;
import com.xuecheng.ucenter.dao.XcMenuMapper;
import com.xuecheng.ucenter.dao.XcUserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class XcUserService {

    @Autowired
    XcUserRepository xcUserRepository;

    @Autowired
    XcCompanyUserRepository xcCompanyUserRepository;

    @Autowired
    XcMenuMapper xcMenuMapper;

    //根据账号查询用户的信息，返回公司信息
    public XcUserExt getUserExt(String username){
        XcUser xcUser = this.findXcUserByUsername(username);
        if(xcUser == null){
            return null;
        }
        String userId = xcUser.getId();
        //查询用户所属公司
        XcCompanyUser companyUser = xcCompanyUserRepository.findByUserId(userId);
        //查询权限
        List<XcMenu> xcMenus = xcMenuMapper.selectPermissionByUserId(userId);
        if(companyUser == null){
            return null;
        }
        //创建对象,传入公司id 返回
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser,xcUserExt);
        xcUserExt.setCompanyId(companyUser.getCompanyId());
        xcUserExt.setPermissions(xcMenus);
        return xcUserExt;
    }

    //根据用户账号查询XcUser信息
    public XcUser findXcUserByUsername(String username){
        return xcUserRepository.findByUsername(username);
    }
}
