package com.ruoyi.tfa.email.service.impl;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
import com.ruoyi.tfa.email.service.IMailService;
import com.ruoyi.tfa.email.utils.EmailUtil;

@Service
public class MailServiceImpl implements IMailService {

    @Autowired
    private ISysUserService userService;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private UserDetailsServiceImpl userDetailsServiceImpl;
    
    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);

    @Override
    public boolean sendCode(String email, String code, OauthVerificationUse use) {
        if (CacheUtils.hasKey(CacheConstants.EMAIL_CODES, use.getValue() + email)) {
            throw new ServiceException("当前验证码未失效，请在1分钟后再发送短信");
        }

        try {
            EmailUtil.sendMessage(email, "验证码邮件", "您收到的验证码是：" + code);
            CacheUtils.put(CacheConstants.EMAIL_CODES, use.getValue() + email, code, 10, TimeUnit.MINUTES);
            log.info("发送邮箱验证码成功:{ email: " + email + " code:" + code + "}");
            return true;
        } catch (Exception e) {
            throw new ServiceException("发送手机验证码异常：" + email);
        }
    }

    @Override
    public boolean checkCode(String email, String code, OauthVerificationUse use) {
        if (StringUtils.isEmpty(code))
            return false;
        String cachedCode = CacheUtils.get(CacheConstants.EMAIL_CODES, use.getValue() + email, String.class); // 从缓存中获取验证码
        boolean isValid = code.equals(cachedCode);
        if (isValid)
            CacheUtils.remove(CacheConstants.EMAIL_CODES, use.getValue() + email);
        return isValid;
    }

    public void doLogin(LoginBody loginBody, boolean isRegister) {
        SysUser sysUser = userService.selectUserByEmail(loginBody.getEmail());
        if (sysUser == null && !isRegister) {
            throw new ServiceException("该邮箱未绑定用户");
        } else {
            sendCode(loginBody.getEmail(), RandomCodeUtil.numberCode(6), OauthVerificationUse.LOGIN);
        }
    }

    public String doLoginVerify(LoginBody loginBody, boolean isRegister) {
        if (checkCode(loginBody.getEmail(), loginBody.getCode(), OauthVerificationUse.LOGIN)) {
            SysUser sysUser = userService.selectUserByEmail(loginBody.getEmail());
            if (sysUser == null) {
                if (isRegister) {
                    sysUser = new SysUser();
                    sysUser.setUserName(loginBody.getEmail());
                    sysUser.setPassword(SecurityUtils.encryptPassword(RandomCodeUtil.code(16)));
                    sysUser.setEmail(loginBody.getEmail());
                    userService.registerUser(sysUser);
                } else {
                    throw new ServiceException("该邮箱未绑定用户");
                }
            }
            LoginUser loginUser = (LoginUser) userDetailsServiceImpl.createLoginUser(sysUser);
            return tokenService.createToken(loginUser);
        } else {
            throw new ServiceException("验证码错误");
        }
    }

    public void doRegister(RegisterBody registerBody) {
        SysUser sysUser = userService.selectUserByEmail(registerBody.getEmail());
        if (sysUser != null) {
            throw new ServiceException("该邮箱已绑定用户");
        } else {
            sendCode(registerBody.getEmail(), RandomCodeUtil.numberCode(6), OauthVerificationUse.REGISTER);
        }
    }

    public void doRegisterVerify(RegisterBody registerBody) {
        if (checkCode(registerBody.getEmail(), registerBody.getCode(), OauthVerificationUse.REGISTER)) {
            SysUser sysUser = new SysUser();
            sysUser.setUserName(registerBody.getEmail());
            sysUser.setNickName(registerBody.getEmail());
            sysUser.setPassword(SecurityUtils.encryptPassword(registerBody.getPassword()));
            sysUser.setEmail(registerBody.getEmail());
            userService.registerUser(sysUser);
        } else {
            throw new ServiceException("验证码错误");
        }
    }

    public void doReset(String email) {
        SysUser sysUser = userService.selectUserByEmail(email);
        if (sysUser == null) {
            throw new ServiceException("该邮箱未绑定用户");
        } else {
            sendCode(email, RandomCodeUtil.numberCode(6), OauthVerificationUse.RESET);
        }
    }

    public int doResetVerify(RegisterBody registerBody) {
        if (checkCode(registerBody.getEmail(), registerBody.getCode(), OauthVerificationUse.RESET)) {
            SysUser sysUser = userService.selectUserById(SecurityUtils.getUserId());
            sysUser.setEmail(registerBody.getEmail());
            return userService.updateUser(sysUser);
        } else {
            throw new ServiceException("验证码错误");
        }
    }

    public void doBind(LoginBody loginBody) {
        SysUser sysUser = userService.selectUserByEmail(loginBody.getEmail());
        if (sysUser != null) {
            throw new ServiceException("该邮箱已绑定用户");
        }
        sysUser = userService.selectUserById(SecurityUtils.getUserId());
        if (!SecurityUtils.matchesPassword(loginBody.getPassword(), sysUser.getPassword())) {
            throw new ServiceException("密码错误");
        }
        sendCode(loginBody.getEmail(), RandomCodeUtil.numberCode(6), OauthVerificationUse.BIND);
    }

    public void doBindVerify(LoginBody loginBody) {
        if (checkCode(loginBody.getEmail(), loginBody.getCode(), OauthVerificationUse.BIND)) {
            SysUser sysUser = userService.selectUserById(SecurityUtils.getUserId());
            if (!SecurityUtils.matchesPassword(loginBody.getPassword(), sysUser.getPassword())) {
                throw new ServiceException("密码错误");
            }
            sysUser.setEmail(loginBody.getEmail());
            userService.updateUser(sysUser);
        } else {
            throw new ServiceException("验证码错误");
        }
    }

}
