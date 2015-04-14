package com.ifp.wechat.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.ifp.wechat.constant.ConstantWeChat;
import com.ifp.wechat.util.StringUtil;
import com.ifp.wechat.util.WeixinUtil;

/**
 * 验证签名
 * @author honey.zhao@aliyun.com
 * @version 1.0
 * 
 */
public class SignService {

	public static Logger log = Logger.getLogger(SignService.class);
	/**
	 * 1:参考以下文档获取access_token（有效期7200秒，开发者必须在自己的服务全局缓存access_token）：
	 * 2:用第一步拿到的access_token 采用http GET方式请求获得jsapi_ticket（有效期7200秒，开发者必须在自己的服务全局缓存
	 */
	public static String GET_JSAPI_TICKET ="https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=ACCESS_TOKEN&type=jsapi";

	
	/**
	 * 认证微信签名
	 * @param request
	 * @return	是否成功
	 */
	public static boolean checkSignature(HttpServletRequest request) {
		// 微信加密签名，signature结合了开发者填写的token参数和请求中的timestamp参数、nonce参数。
		String signature = request.getParameter("signature");
		// 时间戳
		String timestamp = request.getParameter("timestamp");
		// 随机数
		String nonce = request.getParameter("nonce");
		
		return checkSignature(signature,timestamp,nonce);
	}

	/**
	 * 验证签名
	 * 
	 * @param signature
	 * @param timestamp
	 * @param nonce
	 * @return 是否验证成功
	 */
	public static boolean checkSignature(String signature, String timestamp,
			String nonce) {
		String[] arr = new String[] { ConstantWeChat.TOKEN, timestamp, nonce };
		// 将token、timestamp、nonce三个参数进行字典排序
		Arrays.sort(arr);
		StringBuilder content = new StringBuilder();
		for (int i = 0; i < arr.length; i++) {
			content.append(arr[i]);
		}
		MessageDigest md = null;
		String tmpStr = null;

		try {
			md = MessageDigest.getInstance("SHA-1");
			// 将三个参数字符串拼接成一个字符串进行sha1加密
			byte[] digest = md.digest(content.toString().getBytes());
			tmpStr = byteToStr(digest);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		content = null;
		// 将sha1加密后的字符串可与signature对比
		return tmpStr != null ? tmpStr.equals(signature.toUpperCase()) : false;
	}
	
	
	/**
	 * @author honey.zhao@aliyun.com
	 * wechat_JSAPI 分享api升级 规范化 找了半天  昵吗蛋 (~.~) 
	 * 生成jsapi_ticket
	 */
	public static String getJsapiTicket(String access_token){
		String ticket="";
		String url = GET_JSAPI_TICKET.replace("ACCESS_TOKEN", access_token);
		
		JSONObject jsonObject = WeixinUtil.httpsRequest(url, "POST", null);
		if (null != jsonObject) {
			if (StringUtil.isNotEmpty(jsonObject.get("errcode"))
					&& jsonObject.get("errcode") != "0") {
				log.error("获取access_token失败 errcode:"
						+ jsonObject.getInt("errcode") + "，errmsg:"
						+ jsonObject.getString("errmsg"));
			} else {
				ticket =jsonObject.getString("ticket"); 
			}
		}
		return ticket;
	}

	/**
	 * 将字节数组转换为十六进制字符串
	 * 
	 * @param byteArray
	 * @return
	 */
	private static String byteToStr(byte[] byteArray) {
		String strDigest = "";
		for (int i = 0; i < byteArray.length; i++) {
			strDigest += byteToHexStr(byteArray[i]);
		}
		return strDigest;
	}

	/**
	 * 将字节转换为十六进制字符串
	 * 
	 * @param mByte
	 * @return
	 */
	private static String byteToHexStr(byte mByte) {
		char[] Digit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A',
				'B', 'C', 'D', 'E', 'F' };
		char[] tempArr = new char[2];
		tempArr[0] = Digit[(mByte >>> 4) & 0X0F];
		tempArr[1] = Digit[mByte & 0X0F];

		String s = new String(tempArr);
		return s;
	}
}
