package cn.qdevelop.service.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.ArrayUtils;

import cn.qdevelop.common.exception.QDevelopException;
import cn.qdevelop.core.db.SQLConfigParser;
import cn.qdevelop.core.db.bean.DBStrutsLeaf;
import cn.qdevelop.service.IOutput;
import cn.qdevelop.service.bean.OutputJson;

public class QServiceUitls {

	public static String getCookie(String key,HttpServletRequest request){
		String val = request.getHeader("Cookie");
		if(val == null || val.indexOf(key+"=")==-1){
			return null;
		}
		val = val.substring(val.indexOf(key+"=")+key.length()+1);
		if(val.indexOf(";")>-1){
			val = val.substring(0, val.indexOf(";"));
		}

		return val;
	}

	public static void setCookie(HttpServletResponse response,String key,String value,int maxAge){
		Cookie cookie = new Cookie(key,value);
		cookie.setMaxAge(maxAge);
		cookie.setPath("/");
		response.addCookie(cookie);
	}

	public static Map<String,String> getParameters(HttpServletRequest request){
		Map<String,String> paramMap = new HashMap<String,String>();
		Enumeration<?> paramNames = request.getParameterNames();
		String key;
		String[] value;
		while (paramNames.hasMoreElements()) {
			key = (String) paramNames.nextElement();
			value = request.getParameterValues(key);
			if(value!=null&&value.length==1){
				paramMap.put(key, value[0]);
			}else {
				StringBuffer tmp = new StringBuffer();
				int len = value.length;
				for(int i=0;i<len;i++){
					if(i>0)tmp.append("&");
					tmp.append(value[i]);
				}
				paramMap.put(key, tmp.toString());
			}
		}
		return paramMap;
	}

	public static IOutput getOutput(HttpServletRequest request,HttpServletResponse response){
		String uri = request.getRequestURI();
		String type = uri.lastIndexOf(".") == -1 ? "" : uri.substring(uri.lastIndexOf("."));
		if(type.equals(".json")){
			OutputJson oj = new OutputJson();
			oj.setOutType("application/json");
			return oj;
		}else if(type.equals(".jsonp")){
			OutputJson oj = new OutputJson();
			String callback =  request.getParameter("callback");
			if(callback==null || callback.length()==0){
				oj.setErrMsg("请求参数[callback]不能为空");
			}
			oj.setJsonpCallback(callback);
			return oj;
		}
		return new OutputJson();
	}

	/**
	 * 输出内容
	 * @param out
	 */
	public static void output(String out,String outType,HttpServletRequest request,HttpServletResponse response){
		response.setCharacterEncoding("utf-8");
		response.setContentType(outType==null?"application/json":outType);
		OutputStream stream = null ;
		try {
			String encoding = request.getHeader("Accept-Encoding");
			if (encoding != null && encoding.indexOf("gzip") != -1){  
				response.setHeader("Content-Encoding" , "gzip");  
				stream = new GZIPOutputStream(response.getOutputStream());  
			}else if (encoding != null && encoding.indexOf("compress") != -1){  
				response.setHeader("Content-Encoding" , "compress");  
				stream = new ZipOutputStream(response.getOutputStream());  
			}else {  
				stream = response.getOutputStream();  
			}
			stream.write(out.getBytes("utf-8"));

			if(request.getAttribute("__startTime")!=null){
				Long s = (Long)request.getAttribute("__startTime");
				response.setHeader("ops", String.valueOf(System.currentTimeMillis()-s));
			}

			stream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try {
				if(stream!=null){
					stream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	private static Pattern isInteger = Pattern.compile("^(([><=&\\^!\\|]+)?[0-9]+?)+?$");
	private static Pattern isDouble = Pattern.compile("^(([><=&\\^!\\|]+)?[0-9]+?\\.[0-9]+?)+?$");
	private static Pattern isTime = Pattern.compile("^(([><=&\\^!\\|]+)?[0-9]{4}-[0-9]{2}-[0-9]{2}( [0-9]{2}:[0-9]{2}:[0-9]{2})?)+?$");

	private static Pattern isAttackValue =
			Pattern.compile(
					"(?:--)|(/\\*(?:.|[\\n\\r])*?\\*/)|(\\b(select|update|delete|insert|trancate|char|substr|ascii|declare|exec|master|into|drop|execute)\\b)",
					Pattern.CASE_INSENSITIVE);

	/**
	 * 进行参数进行注入检查和数值类型校验；仅当args中含有index索引值时，才会根据数据库中，数据表中字段类型和当前参数进行类型校验
	 * @param args
	 * @return
	 */
	public static boolean validParameters(Map<String,String> args,IOutput out,String[] checkColumns,String[] ignoreColumns ){
		if(checkColumns!=null){
			for(String s : checkColumns){
				if(args.get(s)==null){
					out.setErrMsg("请求参数[",s,"]不能为空");
					return false;
				}
			}
		}
		if(args.get("index")!=null){
			try {
				Map<String,DBStrutsLeaf> struts = SQLConfigParser.getInstance().getDBStrutsLeafByIndex(args.get("index"));
				Iterator<Entry<String,String>> iter = args.entrySet().iterator();
				while(iter.hasNext()){
					Entry<String,String> itor = iter.next();
					if(ArrayUtils.contains(ignoreColumns, itor.getKey())){
						continue;
					}
					DBStrutsLeaf dsf = struts.get(itor.getKey());
					String val = itor.getValue();
					if(dsf!=null){
						boolean isRightValue = true;
						if(dsf!=null){
							switch(dsf.getColumnType()){
							case 1:
							case 6:
								isRightValue  = isInteger.matcher(val).find();
								break;
							case 3:
								isRightValue = isDouble.matcher(val).find();
								break;
							case 4:
							case 5:
								isRightValue = isTime.matcher(val).find();
								break;
							default:
								isRightValue = !isAttackValue.matcher(val).find();	
							}
						}
						if(!isRightValue){
							out.setErrMsg("请求参数[",itor.getKey(),"=",val,"]数据类型不合法");
							return false;
						}else if(dsf.getSize() > 0 && val.length() > dsf.getSize()){
							out.setErrMsg("请求参数[",itor.getKey(),"=",val,"]数据超长");
							return false;
						}
					}else if(isAttackValue.matcher(val).find()){
						out.setErrMsg("请求参数[",itor.getKey(),"=",val,"]可能含有恶意字符");
						return false;
					}
				}
			} catch (QDevelopException e) {
				e.printStackTrace();
			}
		}else{
			Iterator<Entry<String,String>> iter = args.entrySet().iterator();
			while(iter.hasNext()){
				Entry<String,String> itor = iter.next();
				if(ArrayUtils.contains(ignoreColumns, itor.getKey())){
					continue;
				}
				if(isAttackValue.matcher(itor.getValue()).find()){
					out.setErrMsg("请求参数[",itor.getKey(),"=",itor.getValue(),"]可能含有恶意字符");
					return false;
				}
			}
		}
		return true;
	}

	static Pattern isIP = Pattern.compile("^[0-9]+?\\.[0-9]+?\\.[0-9]+?\\.[0-9]+?$");
	public static String getUserIP(HttpServletRequest request){
		String ip = request.getHeader("X-Real-IP"); 
		if(ip != null && isIP.matcher(ip).find() & !"127.0.0.1".equals(ip)){
			return ip;
		}
		ip =  request.getHeader("X-Forwarded-For");
		if(ip != null && isIP.matcher(ip).find() & !"127.0.0.1".equals(ip)){
			return ip;
		}
		ip =  request.getHeader("Proxy-Client-IP");
		if(ip != null && isIP.matcher(ip).find() & !"127.0.0.1".equals(ip)){
			return ip;
		}
		ip =   request.getHeader("WL-Proxy-Client-IP");
		if(ip != null && isIP.matcher(ip).find() & !"127.0.0.1".equals(ip)){
			return ip;
		}
		ip =   request.getRemoteAddr();
		return ip;
	}
	//	public static void main(String[] args) {
	////		isDouble.matcher("")
	//		Pattern isDouble = Pattern.compile("^(([><=&\\^!\\|@#\\_]+)?[0-9]{4}-[0-9]{2}-[0-9]{2}( [0-9]{2}:[0-9]{2}:[0-9]{2})?)+?$");
	//		System.out.println(isDouble.matcher("2014-12-12@2014-12-12^2014-12-12@2014-12-12").find());
	//	}
}
