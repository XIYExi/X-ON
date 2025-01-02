package com.ruoyi.tfa.phone.service.Impl;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.auth.common.enums.OauthVerificationUse;
import com.ruoyi.auth.common.utils.RandomCodeUtil;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.domain.model.RegisterBody;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.CacheUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.framework.web.service.UserDetailsServiceImpl;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.tfa.phone.enums.DySmsTemplate;
import com.ruoyi.tfa.phone.service.DySmsService;
import com.ruoyi.tfa.phone.utils.DySmsUtil;

/**
 * 手机号认证Servcie
 * 
 * @author zlh
 * @date 2024-04-16
 */
@Service
public class DySmsServiceImpl implements DySmsService {

    @Autowired
    private ISysUserService userService;
    @Autowired
    private UserDetailsServiceImpl userDetailsServiceImpl;
    @Autowired
    private TokenService tokenService;

    private static final Logger log = LoggerFactory.getLogger(DySmsServiceImpl.class);

    @Override
    public boolean sendCode(String phone, String code, OauthVerificationUse use) {
        if (CacheUtils.hasKey(CacheConstants.PHONE_CODES, use.getValue() + phone)) {
            throw new ServiceException("当前验证码未失效，请在1分钟后再发送短信");
        }

        try {
            JSONObject templateParams = new JSONObject();
            templateParams.put("code", code);
            DySmsUtil.sendSms(DySmsTemplate.Test_TEMPLATE_CODE, templateParams, phone);
            CacheUtils.put(CacheConstants.PHONE_CODES, use.getValue() + phone, code, 1, TimeUnit.MINUTES);
            log.info("发送手机验证码成功:{ phone: " + phone + " code:" + code + "}");
            return true;
        } catch (Exception e) {
            throw new ServiceException("发送手机验证码异常：" + phone);
        }
    }

    @Override
    public boolean checkCode(String phone, String code, OauthVerificationUse use) {
        if (StringUtils.isEmpty(code))
            return false;
        String cachedCode = CacheUtils.get(CacheConstants.PHONE_CODES, use.getValue() + phone, String.class); // 从缓存中获取验证码
        boolean isValid = code.equals(cachedCode);
        if (isValid)
            CacheUtils.remove(CacheConstants.PHONE_CODES, use.getValue() + phone);
        return isValid;
    }

    public void doLogin(LoginBody loginBody, boolean isRegister) {
        SysUser sysUser = userService.selectUserByPhone(loginBody.getPhonenumber());
        if (sysUser == null && !isRegister) {
            throw new ServiceException("该手机号未绑定用户");
        } else {
            sendCode(loginBody.getPhonenumber(), RandomCodeUtil.numberCode(6), OauthVerificationUse.LOGIN);
        }
    }

    public String doLoginVerify(LoginBody loginBody, boolean isRegister) {
        if (checkCode(loginBody.getPhonenumber(), loginBody.getCode(), OauthVerificationUse.LOGIN)) {
            SysUser sysUser = userService.selectUserByPhone(loginBody.getPhonenumber());
            if (sysUser == null) {
                if (isRegister) {
                    sysUser = new SysUser();
                    sysUser.setUserName(loginBody.getPhonenumber());
                    sysUser.setPassword(SecurityUtils.encryptPassword(RandomCodeUtil.code(16)));
                    sysUser.setPhonenumber(loginBody.getPhonenumber());
                    userService.registerUser(sysUser);
                } else {
                    throw new ServiceException("该手机号未绑定用户");
                }
            }
            LoginUser loginUser = (LoginUser) userDetailsServiceImpl.createLoginUser(sysUser);
            return tokenService.createToken(loginUser);
        } else {
            throw new ServiceException("验证码错误");
        }
    }

    public void doRegister(RegisterBody registerBody) {
        SysUser sysUser = userService.selectUserByPhone(registerBody.getPhonenumber());
        if (sysUser != null) {
            throw new ServiceException("该手机号已绑定用户");
        } else {
            sendCode(registerBody.getPhonenumber(), RandomCodeUtil.numberCode(6), OauthVerificationUse.REGISTER);
        }
    }

    public boolean doRegisterVerify(RegisterBody registerBody) {
        if (checkCode(registerBody.getPhonenumber(), registerBody.getCode(), OauthVerificationUse.REGISTER)) {
            SysUser sysUser = new SysUser();
            sysUser.setUserName(registerBody.getPhonenumber());
            sysUser.setNickName(registerBody.getUsername());
            sysUser.setPassword(SecurityUtils.encryptPassword(registerBody.getPassword()));
            sysUser.setPhonenumber(registerBody.getPhonenumber());
            return userService.registerUser(sysUser);
        } else {
            throw new ServiceException("验证码错误");
        }
    }

    public void doReset(String phone) {
        SysUser sysUser = userService.selectUserByPhone(phone);
        if (sysUser == null) {
            throw new ServiceException("该手机号未绑定用户");
        } else {
            sendCode(phone, RandomCodeUtil.numberCode(6), OauthVerificationUse.RESET);
        }
    }

    public int doResetVerify(RegisterBody registerBody) {
        if (checkCode(registerBody.getPhonenumber(), registerBody.getCode(), OauthVerificationUse.RESET)) {
            SysUser sysUser = userService.selectUserById(SecurityUtils.getUserId());
            sysUser.setPhonenumber(registerBody.getPhonenumber());
            return userService.updateUser(sysUser);
        } else {
            throw new ServiceException("验证码错误");
        }
    }

    public void doBind(LoginBody loginBody) {
        SysUser sysUser = userService.selectUserByPhone(loginBody.getPhonenumber());
        if (sysUser != null) {
            throw new ServiceException("该手机号已绑定用户");
        }
        sysUser = userService.selectUserById(SecurityUtils.getUserId());
        if (!SecurityUtils.matchesPassword(loginBody.getPassword(), sysUser.getPassword())) {
            throw new ServiceException("密码错误");
        }
        sendCode(loginBody.getPhonenumber(), RandomCodeUtil.numberCode(6), OauthVerificationUse.BIND);
    }

    public int doBindVerify(LoginBody loginBody) {
        if (checkCode(loginBody.getPhonenumber(), loginBody.getCode(), OauthVerificationUse.BIND)) {
            SysUser sysUser = userService.selectUserById(SecurityUtils.getUserId());
            if (!SecurityUtils.matchesPassword(loginBody.getPassword(), sysUser.getPassword())) {
                throw new ServiceException("密码错误");
            }
            sysUser.setPhonenumber(loginBody.getPhonenumber());
            return userService.updateUser(sysUser);
        } else {
            throw new ServiceException("验证码错误");
        }
    }
}
