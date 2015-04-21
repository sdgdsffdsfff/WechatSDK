package com.ifp.wechat.service;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.ifp.wechat.entity.menu.Button;
import com.ifp.wechat.entity.menu.Menu;
import com.ifp.wechat.util.WeixinUtil;
import com.ifp.wechat.utils.ConstantUtil;

/**
 * 菜单创建
 * 
 * @author honey.zhao@aliyun.com
 * @version 1.1
 * 
 */
public class MenuService {

	public static Logger log = Logger.getLogger(MenuService.class);

	/**
	 * 菜单创建（POST） 限100（次/天）
	 */
	public static String MENU_CREATE = "https://api.weixin.qq.com/cgi-bin/menu/create?access_token=ACCESS_TOKEN";

	
	/**
	 * 菜单删除（POST） 限100（次/天）
	 */
	public static String MENU_DELETE = "https://api.weixin.qq.com/cgi-bin/menu/delete?access_token=ACCESS_TOKEN";

	/**
	 * 菜单查询
	 */
	public static String MENU_GET = "https://api.weixin.qq.com/cgi-bin/menu/get?access_token=ACCESS_TOKEN";

	/**
	 * 创建菜单
	 * 
	 * @param jsonMenu
	 *            json格式
	 * @return 状态 0 表示成功、其他表示失败
	 */
	public static Integer createMenu(String jsonMenu) {
		int result = 0;
		String token = WeixinUtil.getToken();
		if (token != null) {
			// 拼装创建菜单的url
			String url = MENU_CREATE.replace("ACCESS_TOKEN", token);
			JSONObject jsonObject = WeixinUtil.httpsRequest(url, "POST", jsonMenu);
			if (null != jsonObject) {
				if (0 != jsonObject.getInt("errcode")) {
					result = jsonObject.getInt("errcode");
					log.error("创建菜单失败 errcode:" + jsonObject.getInt("errcode")
							+ "，errmsg:" + jsonObject.getString("errmsg"));
				}
			}
		}
		return result;
	}
	
	

	/**
	 * 创建菜单
	 * 
	 * @param menu
	 *            菜单实例
	 * @return 0表示成功，其他值表示失败
	 */
	public static Integer createMenu(Menu menu) {
		return createMenu(JSONObject.fromObject(menu).toString());
	}
	
	/**
	 * 删除菜单
	 */
	/**
	 * 创建菜单
	 * 
	 * @param jsonMenu
	 *            json格式
	 * @return 状态 0 表示成功、其他表示失败
	 */
	public static Integer deleteMenu() {
		int result = 0;
		String token = WeixinUtil.getToken();
		if (token != null) {
			String urldelete = MENU_DELETE.replace("ACCESS_TOKEN", token);
			JSONObject jsonObjectdelete = WeixinUtil.httpsRequest(urldelete, "POST", null);
			if (null != jsonObjectdelete) {
				if (0 != jsonObjectdelete.getInt("errcode")) {
					result = jsonObjectdelete.getInt("errcode");
					log.error("删除菜单失败 errcode:" + jsonObjectdelete.getInt("errcode")
							+ "，errmsg:" + jsonObjectdelete.getString("errmsg"));
				}
			}
		}
		return result;
	}


	/**
	 * 查询菜单
	 * 
	 * @return 菜单结构json字符串
	 */
	public static JSONObject getMenuJson() {
		JSONObject result = null;
		String token = WeixinUtil.getToken();
		if (token != null) {
			String url = MENU_GET.replace("ACCESS_TOKEN", token);
			result = WeixinUtil.httpsRequest(url, "GET", null);
		}
		return result;
	}

	/**
	 * 查询菜单
	 * @return Menu 菜单对象
	 */
	public static Menu getMenu() {
		JSONObject json = getMenuJson().getJSONObject("menu");
		System.out.println(json);
		Menu menu = (Menu) JSONObject.toBean(json, Menu.class);
		return menu;
	}

	/**
	 * 创建临时菜单
	 */
	public static void createMenuKlcar() {
		//getMenu();
		Button search_goods = new Button("找货", "view", null, ConstantUtil.get("REST_IP")+"/klcarwl/member/searchGoods.do", null);
		
		Button publish_goods = new Button("发货", "view", null, ConstantUtil.get("REST_IP")+"/klcarwl/member/publishGood.do", null);

		/*Button search_cards = new Button("找车", "view", null, ConstantUtil.get("REST_IP")+"/klcarwl/member/searchCars.do", null);
		Button publish_cards = new Button("发车", "view", null, ConstantUtil.get("REST_IP")+"/klcarwl/member/publishCar.do", null);
		Button btn2 = new Button("发货发车", "click", null, null, new Button[] {search_cards, publish_cards });
		*/
		Button member_center = new Button("会员中心", "view", null, ConstantUtil.get("REST_IP")+"/klcarwl/member/memberCenter.do",null);
		Button activity = new Button("活动", "view", null, ConstantUtil.get("REST_IP")+"/klcarwl/activity.do?openidA=oJpIxt89JcMyDgwjDVbMNcC7IxJo",null);
		Button weizhang = new Button("违章查询", "view", null, ConstantUtil.get("REST_IP")+"/klcarwl/weizhang.do",null);
		Button about_us = new Button("关于我们", "click", "about_us", null,null);
		Button about_service = new Button("联系我们", "view",null, ConstantUtil.get("REST_IP")+"/klcarwl/contractUs.do",null);
		Button btn3 = new Button("我的", "click", null, null, new Button[] {activity,member_center,weizhang,about_service,about_us });
		Menu menu = new Menu(new Button[] { search_goods,publish_goods, btn3 });
		createMenu(menu);
		log.info("菜单添加成功!");
	}
}
