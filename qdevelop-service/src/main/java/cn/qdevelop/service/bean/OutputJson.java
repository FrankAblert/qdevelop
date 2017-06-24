package cn.qdevelop.service.bean;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSON;

import cn.qdevelop.service.IOutput;

public class OutputJson implements IOutput{
	/**返回结果状态标识，default 0 正常**/
	int tag = 0;
	String errMsg;
	Object data;
	HashMap<String,Object> attr = null;
	String jsonpCallback = null;
	boolean isError = false;
	String outType;

	public int getTag() {
		return tag;
	}
	public void setTag(int tag) {
		this.tag = tag;
	}
	public String getErrMsg() {
		return errMsg==null?"":errMsg;
	}

	/**
	 * 设置错误信息
	 * @param errMsgs
	 */
	public void setErrMsg(String ... errMsgs){
		isError = true;
		if(tag==0){//0默认为正常，如果设置错误信息，则主动变更状态为1
			tag = 1;
		}
		StringBuffer sb = new StringBuffer();
		for(int i=0;i<errMsgs.length;i++){
			sb.append(errMsgs[i]);
		}
		this.errMsg = sb.toString();
	}
	
	public Object getData() {
		return data;
	}

	public void addAttr(String key,Object val){
		if(attr==null){
			attr = new HashMap<String,Object>();
		}
		attr.put(key, val);
	}

	public void setData(Object data) {
		this.data = data;
	}


	public String toString(){
		StringBuffer out = new StringBuffer().append(jsonpCallback==null?"":jsonpCallback+"(").append("{")
				.append("\"tag\":").append(this.getTag()).append(",")
				.append("\"data\":{\"result\":").append(JSON.toJSONString(this.getData()));
		if(attr!=null){
			Iterator<Entry<String,Object>> iter = attr.entrySet().iterator();
			while(iter.hasNext()){
				Entry<String,Object> itor = iter.next();
				out.append(",\"").append(itor.getKey()).append("\":").append(JSON.toJSONString(itor.getValue()));
			}
		}
		return out.append("},\"errMsg\":\"").append(this.getErrMsg()).append("\"").append("}").append(jsonpCallback==null?"":");").toString();
	}
	
	@Override
	public boolean isError() {
		return isError;
	}
	
	@Override
	public String getOutType() {
		return outType;
	}
	
	public void setOutType(String outType){
		this.outType = outType;
	}
	
	public void setJsonpCallback(String callback){
		jsonpCallback = callback;
	}

}
