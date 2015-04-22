/**
 *  Copyright (c) 2014 honey.zhao@aliyun.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.ifp.wechat.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.ifp.wechat.constant.ConstantWeChat;
import com.ifp.wechat.entity.message.resp.Article;
import com.ifp.wechat.entity.message.resp.NewsMessage;
import com.ifp.wechat.entity.message.resp.TextMessage;
import com.ifp.wechat.util.MessageUtil;
import com.ifp.wechat.utils.ConstantUtil;
import com.ifp.wechat.utils.HttpUtil;

/**
 * 调用核心业务类接收消息、处理消息
 * @author honey.zhao@aliyun.com
 * @version 1.0
 * 
 */
public class CoreService {

	public static Logger log = Logger.getLogger(CoreService.class);
	
	
	/**
	 * 处理微信发来的请求
	 * 
	 * @param request
	 * @return String
	 */
	@SuppressWarnings("static-access")
	public static String processWebchatRequest(HttpServletRequest request){
		String respMessage = null;
		String from=null,to=null;
		try {
			// xml请求解析
			Map<String, String> requestMap = MessageUtil.parseXml(request);
			// 消息类型
			String msgType = requestMap.get("MsgType");

			TextMessage textMessage = (TextMessage) MessageService
					.bulidBaseMessage(requestMap,
							ConstantWeChat.RESP_MESSAGE_TYPE_TEXT);
			NewsMessage newsMessage = (NewsMessage) MessageService
					.bulidBaseMessage(requestMap,
							ConstantWeChat.RESP_MESSAGE_TYPE_NEWS);

			String respContent = "";
			String qqlitejsonStr="";
			
			//---关注多图文处理
			String respContent_subscribe= "";
			respContent_subscribe=request.getParameter("respContent_subscribe")==null ? "" : request.getParameter("respContent_subscribe");
			
			// 文本消息
			if (msgType.equals(ConstantWeChat.REQ_MESSAGE_TYPE_TEXT)) {
				// 接收用户发送的文本消息内容
				String content = requestMap.get("Content");
				//消息
				if (content.contains("到")||content.contains("至")||content.contains("-")) {
						if(content.contains("至")){
							content=content.replaceAll("至","到");
						}
						if(content.contains("-")){
							content=content.replaceAll("-","到");
						}
						
						if(!(content==null||"".equals(content))&&content.indexOf("到")>=0){
							String[] places=content.split("到");
							from=places[0];
							if(places.length>1){
								to=places[1];
							}
						}
						//=================begin======================
						if(to.contains("货")||to.contains("么")||to.contains("吗")){
							textMessage.setContent("对不起，["+content+"]格式不对。 \n\n 输入“出发地到目的地”就能实时查找货源哦！ 如：贵阳到昆明 \n\n");
							respMessage = MessageService.bulidSendMessage(textMessage,ConstantWeChat.RESP_MESSAGE_TYPE_TEXT);
						}else{
							/**
							 * qqlite 宝妹查询 数据库添加
							 */
							StringBuffer qqliteUrl = new StringBuffer();
							qqliteUrl.append(ConstantUtil.get("WULIU_IP") + "/qqLuoji56/qqlite?Sender=wx&Message=");
							try {
								qqliteUrl.append(URLEncoder.encode(content, "utf-8"));
								qqlitejsonStr = HttpUtil.getInvoke(qqliteUrl.toString(),"GET");
							}catch (UnsupportedEncodingException e) {
								log.error("QQlite URLEncoder error!\n" + e.getMessage());
							}finally{
								if(qqlitejsonStr !=null){
									Thread.currentThread().sleep(3000);
									/**
									 * -----------------------------
									 * 1调用物流接口
									 * ------------------------------
									 */
									StringBuffer url = new StringBuffer();
									url.append(ConstantUtil.get("WULIU_IP")+"/wuliu/msg/listFromStation1.do?station=");
									try {
										url.append(URLEncoder.encode(content, "utf-8"));
									} catch (UnsupportedEncodingException e) {
										log.error("URLEncoder error!\n" + e.getMessage());
									}
									String jsonStr =HttpUtil.getInvoke(url.toString(),"GET");
									if (jsonStr != null && !"".equals(jsonStr)) {
										JSONObject json = JSONObject.fromObject(jsonStr);
										if (json != null) {
											String flagStr = json.getString("status");
											if (flagStr.equals("success")) {
												
												textMessage.setContent(json.getString("data"));
											}else{
												textMessage.setContent(json.getString("msg"));
											}
										}
									}
									respMessage = MessageService.bulidSendMessage(textMessage,ConstantWeChat.RESP_MESSAGE_TYPE_TEXT);
								}else{
									textMessage.setContent("对不起，暂时没有找到"+content+"的货源。请您尝试更换附近城市作为出发地或目的地再次查询。");
									respMessage = MessageService.bulidSendMessage(textMessage,ConstantWeChat.RESP_MESSAGE_TYPE_TEXT);
								}
							}
						//===============end=================	
						}
				}else{
					/**
					 * 1抢话费
					 */
					if(content.contains("领话费")){
						/**
						 * -----------------------------
						 * 1调用微商城klcarwl接口-获取当前用户的openid的活动话费详情
						 * ------------------------------
						 */
						String fromUserName = requestMap.get("FromUserName");
						StringBuffer wechatuser_url = new StringBuffer();
						wechatuser_url.append(ConstantUtil.get("REST_IP")+"/klcarwl/wechatuser.do?openid=");
						try {
							wechatuser_url.append(URLEncoder.encode(fromUserName, "utf-8"));
						} catch (UnsupportedEncodingException e) {
							log.error("URLEncoder error!\n" + e.getMessage());
						}
						String wechatuser_jsonStr =HttpUtil.getInvoke(wechatuser_url.toString(),"GET");
						if (wechatuser_jsonStr != null && !"".equals(wechatuser_jsonStr)) {
							JSONObject wechatuser_result = JSONObject.fromObject(wechatuser_jsonStr);
							if (wechatuser_result != null && !"".equals(wechatuser_result)) {
								JSONObject wechatuser = JSONObject.fromObject(wechatuser_result.get("wechatuser"));
								if (wechatuser != null) {
									//------获取wechatuser详细信息------------------
									String sumhavefee="0";
									String sumcost = wechatuser.getString("sumcost"); //抢到总话费
									if(wechatuser.toString().contains("sumhavefee")){
										sumhavefee = wechatuser.getString("sumhavefee");//已充值话费
									}
									String name = wechatuser.getString("name");
									String wechatkey = wechatuser.getString("wechatkey");
									String nickname = wechatuser.getString("nickname");
									String mobile = wechatuser.getString("mobile");
									
									if (Double.parseDouble(sumcost) < 10) {
										textMessage.setContent("您好，您目前抢到的话费总额为"+sumcost+"元，话费总的达到10元及10元的整数倍才能领取，请继续找好友抢话费。");
									}else{
										if(Double.parseDouble(sumhavefee)>0){
											//1:已经充值过
											Double differenceFee = Double.parseDouble(sumcost)-Double.parseDouble(sumhavefee);
											Double topupFee = (differenceFee/10)*10;
											textMessage.setContent("您好，您目前抢到的话费总额为 "+sumcost+" 元，已充值 "+sumhavefee+" 元，我们将在7个工作日内将"+topupFee+"元话费充入您的手机号："+mobile+"，敬请关注。您可以继续找好友抢话费，达到10元及10元的整数倍后再次领取。");
										}else{
											Double topupFee = (Double.parseDouble(sumcost)/10)*10;
											//2:没有充值过
											textMessage.setContent("您好，您目前抢到的话费总额为"+sumcost+"元，我们将在7个工作日内将"+topupFee+"元话费充入您的手机号："+mobile+"，敬请关注。您可以继续找好友抢话费，达到10元及10元的整数倍后再次领取。");
										}
										
									}
								}else{
									//还没参加过活动的用户
									textMessage.setContent("您好，您还没参加过活动，点击【我的->活动】 10万话费等你来抢/调皮");
								}
							}
						}else{
							//还没参加过活动的用户
							textMessage.setContent("您好，您还没参加过活动，点击【我的->活动】 10万话费等你来抢/调皮");
						}
						
					}else{
						/**
						 * -----------------------------
						 * 2调用物流接口-单一城市返回 统计货物量
						 * ------------------------------
						 */
						StringBuffer url = new StringBuffer();
						url.append(ConstantUtil.get("WULIU_IP")+"/wuliu/msg/fromStationNums.do?station=");
						try {
							url.append(URLEncoder.encode(content, "utf-8"));
						} catch (UnsupportedEncodingException e) {
							log.error("URLEncoder error!\n" + e.getMessage());
						}
						String jsonStr =HttpUtil.getInvoke(url.toString(),"GET");
						if (jsonStr != null && !"".equals(jsonStr)) {
							JSONObject json = JSONObject.fromObject(jsonStr);
							if (json != null) {
								String flagStr = json.getString("status");
								if (flagStr.equals("success")) {
									textMessage.setContent(json.getString("msg"));
								}else{
									textMessage.setContent(json.getString("msg"));
								}
							}
						}
					}
					
					respMessage = MessageService.bulidSendMessage(textMessage,
							ConstantWeChat.RESP_MESSAGE_TYPE_TEXT);
				}
				
			} else if (msgType.equals(ConstantWeChat.REQ_MESSAGE_TYPE_EVENT)) {
				// 事件类型
				String eventType = requestMap.get("Event");
				// 事件KEY值，与创建自定义菜单时指定的KEY值对应
				String eventKey = requestMap.get("EventKey");
				if (eventType.equals(ConstantWeChat.EVENT_TYPE_SUBSCRIBE)) {
					// 关注
					respContent = "“快乐大车配货服务”是全国道路配货公共平台所提供的免费服务，为全国3百多万辆货运车辆提供海量实时货源，快捷高效配货，以提升用户效益，优化行业资源。/亲亲 /亲亲 么么哒！有货就是这么任性！ \n\n ■ 输入“出发地到目的地”就能实时查找货源哦！/色  如：贵阳到昆明 \n\n ■ 还可加入QQ“快乐大车免费配货群”,QQ群号码：263871178 /鼓掌 \n\n ■ 我们的热线：400 058 0777 等你哦~ /调皮 \n ";
					//多图文展示  找货、发货、违章查询
					StringBuffer url = new StringBuffer();
					url.append(ConstantUtil.get("WECHAT_IMAGES_ADDRESS")+"/WechatSDK/WeChatServlet?respContent_subscribe='welcome_article'");
					HttpUtil.getInvoke(url.toString(),"POST");
					
				} else if (eventType
						.equals(ConstantWeChat.EVENT_TYPE_UNSUBSCRIBE)) {
					// 取消关注,用户接受不到我们发送的消息了，可以在这里记录用户取消关注的日志信息
				} else if (eventType.equals(ConstantWeChat.EVENT_TYPE_CLICK)) {
					// 自定义菜单点击事件
					if (eventKey.equals("about_service")) {
						respContent = "“快乐大车配货服务”是全国道路配货公共平台所提供的免费服务，为全国3百多万辆货运车辆提供海量实时货源，实现快捷高效配货。以此提升用户效益，优化行业资源。";
					}else if (eventKey.equals("about_us")) {
						respContent = "about_us";
					}
				}
				if(respContent =="about_us"){//发送图文
					List<Article> articleList = new ArrayList<Article>();
					Article article = new Article();
					article.setTitle("快乐大车配货-国内物流配货第一平台");
					article.setDescription("北京中斗科技股份有限公司利用微信公众平台，开发的“快乐大车配货”轻应用，是国内基于移动互联网技术开发的手机配货平台，致力于公路运输行业，为车找货（配货）、货找车（托运）提供全面的信息及交易服务。");
					//article.setPicUrl(ConstantUtil.WECHAT_IMAGES_ADDRESS +request.getContextPath()+"/images_tmp/getimgdata.jpg");
					article.setPicUrl(ConstantUtil.get("WECHAT_IMAGES_ADDRESS")+"/WechatSDK/images_tmp/getimgdata.jpg");
					article.setUrl("http://mp.weixin.qq.com/s?__biz=MzA3MzQ3OTIwNg==&mid=201595029&idx=1&sn=a6e6ea23b50f37de185d7036516dc8ca#rd");
					articleList.add(article);
					// 设置图文消息个数
					newsMessage.setArticleCount(articleList.size());
					// 设置图文消息包含的图文集合
					newsMessage.setArticles(articleList);
					
					respMessage = MessageService.bulidSendMessage(newsMessage,ConstantWeChat.RESP_MESSAGE_TYPE_NEWS);
				}else{//发送文本
					textMessage.setContent(respContent);
					respMessage = MessageService.bulidSendMessage(textMessage,ConstantWeChat.RESP_MESSAGE_TYPE_TEXT);
				}
				
			}else if(msgType.equals(ConstantWeChat.REQ_MESSAGE_TYPE_LOCATION)){//获取到地址信息
				System.out.println("-----获取用户位置信息------");
			}else if(msgType.equals(ConstantWeChat.REQ_MESSAGE_TYPE_VOICE)){//获取到音频信息
				
			}
			
			//--------------------关注 多图文
			if(respContent_subscribe.equals("welcome_article")){//欢迎多图文
				List<Article> articleList = new ArrayList<Article>();
				Article article_welcome = new Article();
				article_welcome.setTitle("快乐大车配货-国内物流配货第一平台");
				article_welcome.setDescription("北京中斗科技股份有限公司利用微信公众平台");
				article_welcome.setPicUrl(ConstantUtil.get("WECHAT_IMAGES_ADDRESS")+"/WechatSDK/images_tmp/welcome.jpg");
				article_welcome.setUrl(ConstantUtil.get("REST_IP")+"/klcarwl");
				articleList.add(article_welcome);
				
				Article article_search = new Article();
				article_search.setTitle("快乐物流 --轻松找货");
				article_search.setDescription("快乐物流 --轻松找货");
				article_search.setPicUrl(ConstantUtil.get("WECHAT_IMAGES_ADDRESS")+"/WechatSDK/images_tmp/search.jpg");
				article_search.setUrl(ConstantUtil.get("REST_IP")+"/klcarwl/member/searchGoods.do");
				articleList.add(article_search);

				Article article_publish = new Article();
				article_publish.setTitle("快乐物流 --轻松发货");
				article_publish.setDescription("快乐物流 --轻松发货");
				article_publish.setPicUrl(ConstantUtil.get("WECHAT_IMAGES_ADDRESS")+"/WechatSDK/images_tmp/publish.jpg");
				article_publish.setUrl(ConstantUtil.get("REST_IP")+"/klcarwl/member/publishGood.do");
				articleList.add(article_publish);
				
				Article article_weizhang = new Article();
				article_weizhang.setTitle("快乐物流 --违章查询");
				article_weizhang.setDescription("快乐物流 --违章查询");
				article_weizhang.setPicUrl(ConstantUtil.get("WECHAT_IMAGES_ADDRESS")+"/WechatSDK/images_tmp/weizhang.jpg");
				article_weizhang.setUrl(ConstantUtil.get("REST_IP")+"/klcarwl/weizhang.do");
				articleList.add(article_weizhang);
				// 设置图文消息个数
				newsMessage.setArticleCount(articleList.size());
				// 设置图文消息包含的图文集合
				newsMessage.setArticles(articleList);
				
				Article article_member = new Article();
				article_member.setTitle("快乐物流 --会员中心");
				article_member.setDescription("快乐物流 --会员中心");
				article_member.setPicUrl(ConstantUtil.get("WECHAT_IMAGES_ADDRESS")+"/WechatSDK/images_tmp/member.jpg");
				article_member.setUrl(ConstantUtil.get("REST_IP")+"/klcarwl/member/memberCenter.do");
				articleList.add(article_member);
				// 设置图文消息个数
				newsMessage.setArticleCount(articleList.size());
				// 设置图文消息包含的图文集合
				newsMessage.setArticles(articleList);
				
				respMessage = MessageService.bulidSendMessage(newsMessage,ConstantWeChat.RESP_MESSAGE_TYPE_NEWS);
			}
		} catch (Exception e) {
			log.error("the wechatSDK OF WECHAT error !\n", e);
		}
		return respMessage;
	}
	
	/**
	 * emoji表情转换(hex -> utf-16)
	 * 
	 * @param hexEmoji
	 * @return
	 */
	public static String emoji(int hexEmoji) {
		return String.valueOf(Character.toChars(hexEmoji));
	}
	
	/** 
	 * 判断是否是QQ表情 
	 *  
	 * @param content 
	 * @return 
	 */  
	public static boolean isQqFace(String content) {  
	    boolean result = false;  
	  
	    // 判断QQ表情的正则表达式  
	    String qqfaceRegex = "/::\\)|/::~|/::B|/::\\||/:8-\\)|/::<|/::$|/::X|/::Z|/::'\\(|/::-\\||/::@|/::P|/::D|/::O|/::\\(|/::\\+|/:--b|/::Q|/::T|/:,@P|/:,@-D|/::d|/:,@o|/::g|/:\\|-\\)|/::!|/::L|/::>|/::,@|/:,@f|/::-S|/:\\?|/:,@x|/:,@@|/::8|/:,@!|/:!!!|/:xx|/:bye|/:wipe|/:dig|/:handclap|/:&-\\(|/:B-\\)|/:<@|/:@>|/::-O|/:>-\\||/:P-\\(|/::'\\||/:X-\\)|/::\\*|/:@x|/:8\\*|/:pd|/:<W>|/:beer|/:basketb|/:oo|/:coffee|/:eat|/:pig|/:rose|/:fade|/:showlove|/:heart|/:break|/:cake|/:li|/:bome|/:kn|/:footb|/:ladybug|/:shit|/:moon|/:sun|/:gift|/:hug|/:strong|/:weak|/:share|/:v|/:@\\)|/:jj|/:@@|/:bad|/:lvu|/:no|/:ok|/:love|/:<L>|/:jump|/:shake|/:<O>|/:circle|/:kotow|/:turn|/:skip|/:oY|/:#-0|/:hiphot|/:kiss|/:<&|/:&>";  
	    Pattern p = Pattern.compile(qqfaceRegex);  
	    Matcher m = p.matcher(content);  
	    if (m.matches()) {  
	        result = true;  
	    }  
	    return result;  
	}  
	
}
