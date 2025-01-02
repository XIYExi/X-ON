package com.ruoyi.oauth.justauth.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.enums.UserStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.SysPermissionService;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.auth.common.domain.OauthUser;
import com.ruoyi.auth.common.service.IOauthUserService;
import com.ruoyi.oauth.justauth.utils.AuthUtils;
import com.ruoyi.system.service.ISysUserService;

import jakarta.servlet.http.HttpServletRequest;
import me.zhyd.oauth.cache.AuthDefaultStateCache;
import me.zhyd.oauth.cache.AuthStateCache;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.utils.AuthStateUtils;

/**
 * 第三方认证授权处理
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/auth")
public class SysAuthController extends BaseController
{
    private AuthStateCache authStateCache;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private IOauthUserService oauthUserService;

    private final static Map<String, String> auths = new HashMap<String, String>();
    {
        auths.put("gitee", "{\"clientId\":\"d83265831bf1b09765c27a9fd9860a9fbb2d939b13460454e85d3d551eebb157\",\"clientSecret\":\"23e3d066c85fc5ee6eaad91067ad006bb5475796f9b9c024eaf5bd26acadd7a3\",\"redirectUri\":\"http://127.0.0.1:80/social-login?source=gitee\"}");
        auths.put("github", "{\"clientId\":\"Iv1.1be0cdcd71aca63b\",\"clientSecret\":\"0d59d28b43152bc8906011624db37b0fed88d154\",\"redirectUri\":\"http://127.0.0.1:80/social-login?source=github\"}");
        authStateCache = AuthDefaultStateCache.INSTANCE;
    }

    /**
     * 认证授权
     * 
     * @param source
     * @throws IOException
     */
    @GetMapping("/binding/{source}")
    @ResponseBody
    public AjaxResult authBinding(@PathVariable("source") String source, HttpServletRequest request) throws IOException
    {
        LoginUser tokenUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(tokenUser) && oauthUserService.checkAuthUser(tokenUser.getUserId(), source))
        {
            return error(source + "平台账号已经绑定");
        }

        String obj = auths.get(source);
        if (StringUtils.isEmpty(obj))
        {
            return error(source + "平台账号暂不支持");
        }
        JSONObject json = JSONObject.parseObject(obj);
        AuthRequest authRequest = AuthUtils.getAuthRequest(source, json.getString("clientId"), json.getString("clientSecret"), json.getString("redirectUri"), authStateCache);
        String authorizeUrl = authRequest.authorize(AuthStateUtils.createState());
        return success(authorizeUrl);
    }

    /**
     * 第三方登录回调
     * @param source
     * @param callback
     * @param request
     * @return
     */
    @SuppressWarnings("unchecked")
    @Anonymous
    @GetMapping("/social-login/{source}")
    public AjaxResult socialLogin(@PathVariable("source") String source, AuthCallback callback, HttpServletRequest request)
    {
        String obj = auths.get(source);
        if (StringUtils.isEmpty(obj))
        {
            return AjaxResult.error(10002, "第三方平台系统不支持或未提供来源");
        }
        JSONObject json = JSONObject.parseObject(obj);
        AuthRequest authRequest = AuthUtils.getAuthRequest(source, json.getString("clientId"), json.getString("clientSecret"), json.getString("redirectUri"), authStateCache);
        AuthResponse<AuthUser> response = authRequest.login(callback);
        if (response.ok())
        {
            LoginUser tokenUser = tokenService.getLoginUser(request);
            if (StringUtils.isNotNull(tokenUser))
            {
                SysUser user = oauthUserService.selectSysUserByUUID(source + response.getData().getUuid());
                if (StringUtils.isNotNull(user))
                {
                    String token = tokenService.createToken(SecurityUtils.getLoginUser());
                    return success().put(Constants.TOKEN, token);
                }
                // 若已经登录则直接绑定系统账号
                OauthUser authUser = new OauthUser();
                // SysUser sysUser = new SysUser();
                // sysUser.setAvatar(response.getData().getAvatar());
                authUser.setUuid(source + response.getData().getUuid());
                authUser.setUserId(SecurityUtils.getUserId());
                // sysUser.setUserName(response.getData().getUsername());
                // sysUser.setNickName(response.getData().getNickname());
                // sysUser.setEmail(response.getData().getEmail());
                authUser.setSource(source);
                oauthUserService.insertOauthUser(authUser);
                // userService.insertUser(sysUser);
                String token = tokenService.createToken(SecurityUtils.getLoginUser());
                return success().put(Constants.TOKEN, token);
            }
            SysUser authUser = oauthUserService.selectSysUserByUUID(source + response.getData().getUuid());
            if (StringUtils.isNotNull(authUser))
            {
                SysUser user = userService.selectUserByUserName(authUser.getUserName());
                if (StringUtils.isNull(user))
                {
                    throw new ServiceException("登录用户：" + user.getUserName() + " 不存在");
                }
                else if (UserStatus.DELETED.getCode().equals(user.getDelFlag()))
                {
                    throw new ServiceException("对不起，您的账号：" + user.getUserName() + " 已被删除");
                }
                else if (UserStatus.DISABLE.getCode().equals(user.getStatus()))
                {
                    throw new ServiceException("对不起，您的账号：" + user.getUserName() + " 已停用");
                }
                LoginUser loginUser = new LoginUser(user.getUserId(), user.getDeptId(), user, permissionService.getMenuPermission(user));
                String token = tokenService.createToken(loginUser);
                return success().put(Constants.TOKEN, token);
            }
            else
            {
                return AjaxResult.error(10002, "对不起，您没有绑定注册用户，请先注册后在个人中心绑定第三方授权信息！");
            }
        }
        return AjaxResult.error(10002, "对不起，授权信息验证不通过，请联系管理员");
    }

    /**
     * 取消授权
     */
    @DeleteMapping(value = "/unlock/{authId}")
    public AjaxResult unlockAuth(@PathVariable Long authId)
    {
        return toAjax(oauthUserService.deleteOauthUserById(authId));
    }
}
