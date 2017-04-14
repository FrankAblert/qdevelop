package cn.qdevelop.service;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.qdevelop.service.utils.QServiceUitls;

/**
 * 抽象API服务接口类
 * @author janson
 *
 */
public abstract class APIControl extends HttpServlet implements IService{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7445553533896016332L;
	private HttpServletResponse response;
	private HttpServletRequest request;
	private String[] checkColumns,ignoreColumns;

	public void init(ServletConfig config)throws ServletException{  
		super.init(config);  
		String columns = config.getInitParameter(IService.INIT_VALID_REQUIRED);
		if(columns!=null){
			checkColumns = columns.split(",");
		}
		String ignore = config.getInitParameter(IService.INIT_VALID_IGNORE);
		if(ignore!=null){
			ignoreColumns = ignore.split(",");
		}else{
			ignoreColumns = new String[]{};
		}
	}

	/**
	 * 自定义初始化请求参数，在执行execute方法和验证参数之前，做参数的自定义处理
	 * @param files
	 */
	public abstract void init(Map<String,String> args);

	/**
	 * 统一执行入口
	 * @param args	当前请求的所有参数
	 * @param output	需要输出内容收集器
	 * @return 输出内容的格式 暂时没作用，后期增加其他
	 */
	protected abstract String execute(Map<String,String> args,IOutput output);

	private IOutput out ;
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		this.response = response;
		this.request = request;
		out = QServiceUitls.getOutput(request, response);
		if(!out.isError()){
			Map<String,String> args = QServiceUitls.getParameters(request);
			init(args);
			if(QServiceUitls.validParameters(args,out,checkColumns,ignoreColumns)){
				execute(args,out);
			}
		}
		QServiceUitls.output(out.toString(), out.getOutType(), request, response);
	}

	
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request,response);
	}


	public HttpServletResponse getResponse(){
		return response;
	}

	public HttpServletRequest getRequest(){
		return request;
	}

	/**
	 * 获取全局输出内容收集器
	 * @return
	 */
	public IOutput getOutPut(){
		return out;
	}



}
