### spring security身份认证大致流程

下面是本人学习spring security时总结的大致认证流程，对于理解spring security认证过程应该可以有初步的了解，代码从网上学习文章摘取。



#### 总结：

1、请求进入UsernamePasswordAuthenticationFilter ，不知道用户名密码是不是对的，所以构造一个未认证的Token

2、this.getAuthenticationManager().authenticate(token);    token交给AuthenticationManager

3、AuthenticationManager根据token注册对应的AuthenticationProvider

4、AuthenticationProvider提取token的值，先用缓存获取UserDetails，没缓存？使用retrieveUser方法获取，retrieveUser方法里有熟悉的loadUserByUsername(添加用户信息权限)

5、additionalAuthenticationChecks方法校验UserDetails密码和权限



![image-20200305154905441](C:\Users\jiang\AppData\Roaming\Typora\typora-user-images\image-20200305154905441.png)



涉及的一些类：

1、UsernamePasswordAuthenticationFilter 

2、AuthenticationManager

3、AuthenticationProvider

4、UserDetailsService

5、UserDetails



1、UsernamePasswordAuthenticationFilter 

```
public class UsernamePasswordAuthenticationFilter extends AbstractAuthenticationProcessingFilter {
    public static final String SPRING_SECURITY_FORM_USERNAME_KEY = "username";
    public static final String SPRING_SECURITY_FORM_PASSWORD_KEY = "password";
    private String usernameParameter = "username";
    private String passwordParameter = "password";
    private boolean postOnly = true;

    public UsernamePasswordAuthenticationFilter() {
        //1.匹配URL和Method
        super(new AntPathRequestMatcher("/login", "POST"));
    }

    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        if (this.postOnly && !request.getMethod().equals("POST")) {
            //啥？你没有用POST方法，给你一个异常，自己反思去
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        } else {
            //从请求中获取参数
            String username = this.obtainUsername(request);
            String password = this.obtainPassword(request);
            //我不知道用户名密码是不是对的，所以构造一个未认证的Token先
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);
            //顺便把请求和Token存起来
            this.setDetails(request, token);
            //Token给谁处理呢？当然是给当前的AuthenticationManager喽
            return this.getAuthenticationManager().authenticate(token);   ---根据Token的类来确定用什么Provider来处理
        }
    }
}
```



2、UsernamePasswordAuthenticationToken,token其实只是一个载体而已

```
public class UsernamePasswordAuthenticationToken extends AbstractAuthenticationToken {
    private static final long serialVersionUID = 510L;
    //随便怎么理解吧，暂且理解为认证标识吧，没看到是一个Object么
    private final Object principal;
    //同上
    private Object credentials;

    //这个构造方法用来初始化一个没有认证的Token实例
    public UsernamePasswordAuthenticationToken(Object principal, Object credentials) {
        super((Collection)null);
        this.principal = principal;
        this.credentials = credentials;
        this.setAuthenticated(false);
    }
    //这个构造方法用来初始化一个已经认证的Token实例，为啥要多此一举，不能直接Set状态么，不着急，往后看
    public UsernamePasswordAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        super.setAuthenticated(true);
    }
    //便于理解无视他
    public Object getCredentials() {
        return this.credentials;
    }
    //便于理解无视他
    public Object getPrincipal() {
        return this.principal;
    }

    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        if (isAuthenticated) {
            //如果是Set认证状态，就无情的给一个异常，意思是：
            //不要在这里设置已认证，不要在这里设置已认证，不要在这里设置已认证
            //应该从构造方法里创建，别忘了要带上用户信息和权限列表哦
            //原来如此，是避免犯错吧
            throw new IllegalArgumentException("Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        } else {
            super.setAuthenticated(false);
        }
    }

    public void eraseCredentials() {
        super.eraseCredentials();
        this.credentials = null;
    }
}
```

```

```

3、AuthenticationManager会注册多种AuthenticationProvider，例如UsernamePassword对应的DaoAuthenticationProvider,继承了AbstractUserDetailsAuthenticationProvider

```
public class DaoAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {
    //熟悉的supports，需要UsernamePasswordAuthenticationToken
    public boolean supports(Class<?> authentication) {
            return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
        }

    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
            //取出Token里保存的值
            String username = authentication.getPrincipal() == null ? "NONE_PROVIDED" : authentication.getName();
            boolean cacheWasUsed = true;
            //从缓存取
            UserDetails user = this.userCache.getUserFromCache(username);
            if (user == null) {
                cacheWasUsed = false;

                //啥，没缓存？使用retrieveUser方法获取呀
                user = this.retrieveUser(username, (UsernamePasswordAuthenticationToken)authentication);
            }
            //...删减了一大部分，这样更简洁
            Object principalToReturn = user;
            if (this.forcePrincipalAsString) {
                principalToReturn = user.getUsername();
            }

            return this.createSuccessAuthentication(principalToReturn, authentication, user);
        }
         protected final UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        try {
            //熟悉的loadUserByUsername
            UserDetails loadedUser = this.getUserDetailsService().loadUserByUsername(username);
            if (loadedUser == null) {
                throw new InternalAuthenticationServiceException("UserDetailsService returned null, which is an interface contract violation");
            } else {
                return loadedUser;
            }
        } catch (UsernameNotFoundException var4) {
            this.mitigateAgainstTimingAttack(authentication);
            throw var4;
        } catch (InternalAuthenticationServiceException var5) {
            throw var5;
        } catch (Exception var6) {
            throw new InternalAuthenticationServiceException(var6.getMessage(), var6);
        }
    }
    //检验密码
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        if (authentication.getCredentials() == null) {
            this.logger.debug("Authentication failed: no credentials provided");
            throw new BadCredentialsException(this.messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
        } else {
            String presentedPassword = authentication.getCredentials().toString();
            if (!this.passwordEncoder.matches(presentedPassword, userDetails.getPassword())) {
                this.logger.debug("Authentication failed: password does not match stored value");
                throw new BadCredentialsException(this.messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
            }
        }
    }
}
```









