

http://wechat.klcar.com  【快乐物流 微信服务号SDK封装API】



******************************************************************************************************
*                                                                                                    *
*                            WechatSDK weixin 微信全接口SDK封装 Java                                 *
*                                                                                                    *
*                            QQ 591270247  honey.zhao@aliyun.com                                     *
******************************************************************************************************   

1==========================================接口说明====================================================
1）         br.com.rest.servlet.CervejaServlet.java       微网站可调用该接口获取微信openid用户信息

2）         br.com.rest.servlet.menuServlet.java          菜单刷新接口调用

3）         br.com.rest.servlet.OauthCodeServlet.java     【用户授权后得到的code】获取Access_Token（oAuth认证,此access_token与基础支持的access_token不同）

4）         /** br.com.rest.servlet.OauthServlet.java  
			 * @author honey.zhao@aliyun.com
			 * 获得Oauth认证的URL
			 * @param redirectUrl	跳转的url
			 * @param charset	字符集格式
			 * @param scope	OAUTH scope
			 * @return oauth url
			 * @API 参数说明
			 * 		参数	必须	说明
			 * 		appid	是	公众号的唯一标识
			 * 		redirect_uri	是	授权后重定向的回调链接地址
			 * 		response_type	是	返回类型，请填写code
			 * 		scope	是	应用授权作用域，snsapi_base （不弹出授权页面，直接跳转，只能获取用户openid），snsapi_userinfo （弹出授权页面，可通过openid拿到昵称、性别、所在地。并且，即使在未关注的情况下，只要用户授权，也能获取其信息）
			 * 		state	否	重定向后会带上state参数，开发者可以填写任意参数值
			 * 		#wechat_redirect	否	直接在微信打开链接，可以不填此参数。做页面302重定向时候，必须带此参数
			 */
5)    br.com.rest.servlet.JsApiTicketServlet.java		把定时器获取到的jsapi_ticket 和阿ccesstoken 等参数做成接口调用	 
			
2==========================================初始化调用servlet=============================================

com.ifp.wechat.controller.WeChatServlet.java


3)==========================================API========================================================
此外 doc文件夹下的api 是token监听 api部分 可以帮助 大家读懂token监听部分代码的实现 这部分代码其实wechat4j 的部分 只不过把数据存储部分改成
	了properties文件 的操作 考虑到 每7000s才去操作一次 不是太频繁所以才这么做的 不足之处请 提建议 上边有联系方式
	









			 