package cn.qdevelop.core.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.dom4j.Element;

import cn.qdevelop.common.QLog;
import cn.qdevelop.common.exception.QDevelopException;
import cn.qdevelop.core.Contant;
import cn.qdevelop.core.bean.DBQueryBean;
import cn.qdevelop.core.bean.DBUpdateBean;
import cn.qdevelop.core.db.bean.ComplexParserBean;
import cn.qdevelop.core.db.bean.DBStrutsBean;
import cn.qdevelop.core.db.bean.DBStrutsLeaf;
import cn.qdevelop.core.db.bean.UpdateBean;
import cn.qdevelop.core.db.config.SQLConfigLoader;
import cn.qdevelop.core.db.execute.TableColumnType;
import cn.qdevelop.core.formatter.FormatterLoader;
import cn.qdevelop.core.standard.IDBQuery;
import cn.qdevelop.core.standard.IDBUpdate;
import cn.qdevelop.core.standard.IParamFormatter;
import cn.qdevelop.core.standard.IResultFormatter;
import cn.qdevelop.core.standard.IUpdateHook;

public class SQLConfigParser {
	private static Logger log  = QLog.getLogger(SQLConfigParser.class);
	public static SQLConfigParser getInstance(){return new SQLConfigParser();}

	private final static Map<String,ArrayList<IResultFormatter>> resultFormatterByIndex = new ConcurrentHashMap<String,ArrayList<IResultFormatter>>();
	private final static Map<String,ArrayList<IParamFormatter>> paramFormatterByIndex = new ConcurrentHashMap<String,ArrayList<IParamFormatter>>();
	private final static Map<String,ArrayList<IUpdateHook>> resultHookByIndex = new ConcurrentHashMap<String,ArrayList<IUpdateHook>>();

	private final static Pattern isNumber = Pattern.compile("^[0-9]+?$");
	
	public final static String[] specilQueryKey = new String[]{"index","page","limit","order"}; 

	/**
	 * 判断是否是查询
	 */
	public boolean isSelectByIndex(String index) throws QDevelopException{
		Element config = SQLConfigLoader.getInstance().getSQLConfig(index);
		if(config==null)throw new QDevelopException(1002,"请求没有index");
		return Boolean.parseBoolean(config.attributeValue("is-select"));
	}

	/**
	 * 获取配置内容
	 * @param index
	 * @param attrName
	 * @return
	 * @throws QDevelopException
	 */
	public String getAttrValue(String index,String attrName) throws QDevelopException{
		Element config = SQLConfigLoader.getInstance().getSQLConfig(index);
		if(config==null)throw new QDevelopException(1002,"请求没有index");
		return config.attributeValue(attrName);
	}


	/**
	 * 获取配置的paramformatter
	 * @param index
	 * @return
	 * @throws QDevelopException
	 */
	@SuppressWarnings("unchecked")
	public List<IParamFormatter> getParamFormatter(String index)throws QDevelopException{
		Element config = SQLConfigLoader.getInstance().getSQLConfig(index);
		if(config==null)throw new QDevelopException(1002,"请求没有index");
		ArrayList<IParamFormatter> paramFormatters = paramFormatterByIndex.get(index);
		if(paramFormatters!=null){
			if(paramFormatters.size()==0)return null;
			List<IParamFormatter> paramtters = new ArrayList<IParamFormatter>(paramFormatters.size());
			for(int i=0;i<paramFormatters.size();i++){
				paramtters.add(paramFormatters.get(i).clone());
			}
			return paramtters;
		}
		Element paramFormatterConfig = config.element("param-formatter");
		if(paramFormatterConfig==null){
			paramFormatterByIndex.put(index, new ArrayList<IParamFormatter>(0));
			return null;
		}
		List<Element> itors = paramFormatterConfig.elements();
		paramFormatters = new ArrayList<IParamFormatter>(itors.size());
		for(Element item : itors){
			String name = item.getName();
			item.addAttribute("index", index);
			IParamFormatter paramFormatter = FormatterLoader.getInstance().getParamFormatter(name);
			if(paramFormatter == null){
				throw new QDevelopException(1001,"param-formatter配置不存在："+item.asXML());
			}
			if(!paramFormatter.validConfig(item)){
				throw new QDevelopException(1001,"param-formatter配置参数不全："+item.asXML());
			}
			paramFormatter.initFormatter(item);
			paramFormatters.add(paramFormatter);
		}
		paramFormatterByIndex.put(index, paramFormatters);
		
		List<IParamFormatter> paramtters = new ArrayList<IParamFormatter>(paramFormatters.size());
		for(int i=0;i<paramFormatters.size();i++){
			paramtters.add(paramFormatters.get(i).clone());
		}
		return paramtters;
	}


	@SuppressWarnings("unchecked")
	public List<IUpdateHook> getUpdateHooks(String index)throws QDevelopException{
		Element config = SQLConfigLoader.getInstance().getSQLConfig(index);
		if(config==null)throw new QDevelopException(1002,"请求没有index");
		ArrayList<IUpdateHook> updateHooks =  resultHookByIndex.get(index);
		if(updateHooks!=null){
			if(updateHooks.size()==0)return null;
			List<IUpdateHook> hooks = new ArrayList<IUpdateHook>(updateHooks.size());
			for(int i=0;i<updateHooks.size();i++){
				hooks.add(updateHooks.get(i).clone());
			}
			return hooks;
		}

		Element updateHookConfig = config.element("update-hook");
		if(updateHookConfig==null) return null;
		List<Element> itors = updateHookConfig.elements();
		updateHooks = new ArrayList<IUpdateHook>(itors.size());
		for(Element item : itors){
			String name = item.getName();
			item.addAttribute("index", index);
			IUpdateHook updateHook = FormatterLoader.getInstance().getUpdateHook(name);
			if(updateHook == null){
				throw new QDevelopException(1001,"update-hook配置不存在："+item.asXML());
			}
			if(!updateHook.validConfig(item)){
				throw new QDevelopException(1001,"update-hook配置参数不全："+item.asXML());
			}
			updateHook.initHook(item);
			updateHooks.add(updateHook);
		}
		resultHookByIndex.put(index, updateHooks);
		
		List<IUpdateHook> hooks = new ArrayList<IUpdateHook>(updateHooks.size());
		for(int i=0;i<updateHooks.size();i++){
			hooks.add(updateHooks.get(i).clone());
		}
		return hooks;
	}

	/**
	 * 获取结果formatter结果格式化类
	 * @param index
	 * @return
	 * @throws QDevelopException
	 */
	@SuppressWarnings("unchecked")
	public List<IResultFormatter> getResultFormatter(String index)throws QDevelopException{
		Element config = SQLConfigLoader.getInstance().getSQLConfig(index);
		if(config==null)throw new QDevelopException(1002,"请求没有index");
		ArrayList<IResultFormatter> resultFormatters = resultFormatterByIndex.get(index);
		if(resultFormatters!=null){
			if(resultFormatters.size()==0)return null;
			List<IResultFormatter> formatters = new ArrayList<IResultFormatter>(resultFormatters.size());
			for(int i=0;i<resultFormatters.size();i++){
				formatters.add(resultFormatters.get(i).clone());
			}
			return formatters;
		}
		Element resultFormatterConfig = config.element("result-formatter");
		if(resultFormatterConfig==null){
			resultFormatterByIndex.put(index, new ArrayList<IResultFormatter>(0));
			return null;	
		}
		List<Element> itors = resultFormatterConfig.elements();
		resultFormatters = new ArrayList<IResultFormatter>(itors.size());
		for(Element item : itors){
			String name = item.getName();
			item.addAttribute("index", index);
			IResultFormatter resultFormatter = FormatterLoader.getInstance().getResultFormatter(name);
			if(resultFormatter == null){
				throw new QDevelopException(1001,"result-formatter配置不存在："+item.asXML());
			}
			if(!resultFormatter.validConfig(item)){
				throw new QDevelopException(1001,"result-formatter配置参数不全："+item.asXML());
			}
			resultFormatter.initFormatter(item);
			resultFormatters.add(resultFormatter);
		}
		resultFormatterByIndex.put(index, resultFormatters);
		
		List<IResultFormatter> formatters = new ArrayList<IResultFormatter>(resultFormatters.size());
		for(int i=0;i<resultFormatters.size();i++){
			formatters.add(resultFormatters.get(i).clone());
		}
		return formatters;
	}

	@SuppressWarnings("unchecked")
	public IDBUpdate getDBUpdateBean(Map<String, ?> query,Connection conn)throws QDevelopException{
		try {
			DBUpdateBean dbub;
			String index = (String)query.get("index");
			if(index==null)throw new QDevelopException(1002,"请求没有index");
			Element config = SQLConfigLoader.getInstance().getSQLConfig(index);
			if(config==null)throw new QDevelopException(1003,"["+index+"] SQL配置【"+index+"】不存在");
			if(conn == null){
				throw new QDevelopException(1001,"connect is null");
			}
			dbub = new DBUpdateBean();
			dbub.setIndex(index);
			String connName = config.attributeValue("connect");
			
//			QThreadLogger.add(dbub.getIndex()," ",connName);
			
			List<Element> sqls = config.elements("sql");
			for(Element sql : sqls){
				String repeat = sql.attributeValue("repeat");
				String tableName = sql.attributeValue("tables");
				String sqlStr = sql.getTextTrim();
				DBStrutsBean dbsb = TableColumnType.getInstance().getTableStrutsBean(conn, tableName);
				boolean fetchZeroErr = Boolean.parseBoolean(sql.attributeValue("fetch-zero-err"));
				String[] params = sql.attributeValue("params") == null ? null : sql.attributeValue("params").split("\\|");
				boolean isFullParam = sql.attributeValue("is-full-param").equals("true");
				if(repeat!=null && repeat.length()>0){
					String[] args;
					String repeatSplit =  sql.attributeValue("repeat-split");
					if(repeatSplit!=null){
						repeatSplit = (escapeExprSpecialWord(repeatSplit));
						args = repeat.split(repeatSplit);
					}else{
						args = new String[]{repeat};
					}
					String repeatConcact = sql.attributeValue("repeat-concat");
					if(repeatConcact != null){
						repeatConcact = escapeExprSpecialWord(repeatConcact);
					}
					String[] values = String.valueOf(query.get(repeat)).split(repeatConcact);
					for(String val : values){
						HashMap<String,Object> data = new HashMap<String,Object>(query);
						String[] tmp;
						if(repeatSplit!=null){
							tmp = val.split(repeatSplit);
						}else{
							tmp = new String[]{val};
						}
						if(args.length != tmp.length){
							throw new QDevelopException(1001,"["+index+"] repeat参数错误:"+query.get(repeat));
						}
						for(int i=0;i<args.length;i++){
							data.put(args[i], tmp[i]);
						}
						UpdateBean ub = getUpdateBean(data,sqlStr,params,sql.attributeValue(SQLConfigLoader.caseNonPreBuilderName),dbsb,isFullParam);
						ub.setIndex(index);
						ub.setConnName(connName);
						ub.setFetchZeroError(fetchZeroErr);
						ub.setTableName(tableName);
						dbub.add(ub);
					}
				}else{
					HashMap<String,Object> data = new HashMap<String,Object>(query);
					UpdateBean ub = getUpdateBean(data,sqlStr,params,sql.attributeValue(SQLConfigLoader.caseNonPreBuilderName),dbsb,isFullParam);
					ub.setIndex(index);
					ub.setConnName(connName);
					ub.setFetchZeroError(fetchZeroErr);
					ub.setTableName(tableName);
					dbub.add(ub);
				}
			}
			query = null;
			return dbub;
		} catch (QDevelopException ex) { 
			log.error(ex.getMessage());
			throw ex;
		}catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			throw new QDevelopException(1001,"获取更新SQL报错",e);
		}
	}
	
	
	
	private UpdateBean getUpdateBean(HashMap<String,Object> data,String sql,String[] params,String strParam,DBStrutsBean dbsb,boolean isFullParam) throws QDevelopException{
		UpdateBean ub = new UpdateBean();
		ub.setDbsb(dbsb);
		ub.setInsert(sql.substring(0, 6).equalsIgnoreCase("insert"));
		String preparedSql = new String(sql);
		List<String> columns = new ArrayList<String>();
		List<Object> values = new ArrayList<Object>();
		if(params!=null){
			for(String arg : params){
				Object val = data.get(arg);
				if(val==null){
					if(isFullParam){
						throw new QDevelopException(1001,"["+data.get("index")+"] 请求参数不全,请求需要参数："+arg+" 不能为空");
					}else{
						preparedSql = cleanNULLArgs(preparedSql,arg);
						sql = cleanNULLArgs(sql,arg);
						continue;
					}
				}
				if(isCasePreBuildSQL( arg, dbsb.get(arg),strParam)){
					columns.add(arg);
					values.add(val);
					preparedSql = preparedSql.replaceAll("'?\\$\\["+arg+"\\]'?", "?");
				}else{
					preparedSql = preparedSql.replace("$["+arg+"]", String.valueOf(val));
				}
				sql = sql.replace("$["+arg+"]", String.valueOf(val));
			}
		}
		ub.setPreparedSql(preparedSql);
		ub.setFullSql(sql);
		ub.setColumns(columns.toArray(new String[]{}));
		ub.setValues(values.toArray(new Object[]{}));
		return ub;
	}

	public IDBQuery getDBQueryBean(Map<String,?> query,Connection conn) throws QDevelopException{
		try {
			String index = (String)query.get("index");
			if(index==null)throw new QDevelopException(1002,"请求没有index");
			Element config = SQLConfigLoader.getInstance().getSQLConfig(index);
			if(config==null)throw new QDevelopException(1003,"["+index+"] SQL配置【"+index+"】不存在");
			if(conn == null){
				throw new QDevelopException(1001,"connect is null");
			}
			DBQueryBean dbQuery = new DBQueryBean();
			dbQuery.setIndex(index);
			dbQuery.setConnName(config.attributeValue("connect"));
//			QThreadLogger.add(dbQuery.getIndex()," ",dbQuery.getConnName());
			
			Element sql = config.element("sql");
			dbQuery.setSql(sql.getText());
			String[] params= sql.attributeValue("params")==null? null:sql.attributeValue("params").split("\\|");
			String[] tables =  sql.attributeValue("tables")==null? null:sql.attributeValue("tables").split("\\|");

			dbQuery.setComplexBuild(Boolean.parseBoolean(config.attributeValue("is-complex-build")));
			
			try {
				dbQuery.setTableStruts(TableColumnType.getInstance().getTablesStrutsBean(conn, tables));
			} catch (SQLException e) {
				throw new QDevelopException(1001,"["+index+"] 获取表结构错误",e);
			}
			if(params!=null){
				boolean isFullParam = sql.attributeValue("is-full-param").equals("true");
				ArrayList<String> paramsTemp = new ArrayList<String>(params.length);
				for(String arg : params){
					paramsTemp.add(arg);
					if(query.get(arg)==null){
						if(isFullParam){
							throw new QDevelopException(1001,"["+index+"] 参数不足，请确保所有变量参数数据均存在："+sql.attributeValue("params")+" "+query.toString());
						}else{
							dbQuery.setSql(cleanNULLArgs(dbQuery.getSql(), arg));
							paramsTemp.remove(paramsTemp.size()-1);
						}
					}
				}
				params = paramsTemp.toArray(new String[]{});
				paramsTemp.clear();
			}

			reBuildPreparedSql(dbQuery,query,params==null?new String[]{}:params,sql.attributeValue(SQLConfigLoader.caseNonPreBuilderName));

			/**
			 *  设置分页信息
			 */
			if(query.get("page")!=null ){
				Object page = query.get("page");
				Object pageSize = query.get("limit") != null ? query.get("limit") : query.get("page_size");
				if(pageSize==null){//默认给出每页10条
					pageSize = new Integer(10);
				}
				if(page.getClass().equals(Integer.class)&&pageSize.getClass().equals(Integer.class)){
					dbQuery.setPage((Integer)page, (Integer)pageSize);
				}else{
					if(!isNumber.matcher(String.valueOf(page)).find()||!isNumber.matcher(String.valueOf(pageSize)).find()){
						throw new QDevelopException(1001,"["+index+"] 分页参数错误"+" "+query.toString());
					}
					dbQuery.setPage(Integer.parseInt(String.valueOf(page)), Integer.parseInt(String.valueOf(pageSize)));
				}
			}
			if(query.get("order")!=null){
				Object order = query.get("order");
				if(order.getClass().equals(String.class)){
					dbQuery.setOrder((String)order);
				}else{
					dbQuery.setOrder(String.valueOf(order));
				}
			}
			if(query.get("is-convert-null")!=null){
				dbQuery.setConverNull(Boolean.parseBoolean(String.valueOf(query.get("is-convert-null"))));
			}else{
				dbQuery.setConverNull(Boolean.parseBoolean(config.attributeValue("is-convert-null")));
			}

			if(query.get("is-need-totle")!=null){
				dbQuery.setNeedTotle(Boolean.parseBoolean(String.valueOf(query.get("is-need-totle"))));
			}else{
				dbQuery.setNeedTotle(Boolean.parseBoolean(config.attributeValue("is-need-totle")));
			}

			query=null;
			return dbQuery;
		} catch (QDevelopException e) {
			log.error(e.getMessage());
			throw e;
		}
	}

	private static final Pattern isSpecilKeys = Pattern.compile("^(index|page|page\\_size|order)$");
	private static final Pattern isComplexValue = Pattern.compile("%|&|>|<|!|\\||\\*|=");
	private static final Pattern isFunctionValue = Pattern.compile(".+\\(.+?\\)|.+\\(\\)");
	private static final Pattern clearPreparedSql = Pattern.compile("'\\?'");

	//动态替换正则，提升一丢丢效率
	private static final Map<String,Pattern> patternCache = new ConcurrentHashMap<String,Pattern>();

	private void reBuildPreparedSql(DBQueryBean dbQuery ,Map<String,?> query,String[] params,String strParam) throws QDevelopException{
		Map<String, DBStrutsLeaf> tableStruts = dbQuery.getTableStruts() ;
		ArrayList<String> columns = new ArrayList<String>();
		ArrayList<Object> values = new ArrayList<Object>();
		String sqlModel = dbQuery.getSql();
		String preparedSql = new String(dbQuery.getSql());

		for(String key : params){
			String val = String.valueOf(query.get(key));
			boolean isDBColumn = tableStruts!=null && tableStruts.get(clearColumnName.matcher(key).replaceAll(""))!=null && !isFunctionValue.matcher(val).find() && isCasePreBuildSQL(key,dbQuery.getTableStruts().get(key),strParam);
			String columnKey = getSQLkey(key,sqlModel);
			//	XXX	
			if(columnKey.length()>0 && dbQuery.isComplexBuild() && isComplexValue.matcher(val).find() && !isFunctionValue.matcher(val).find()){
				ComplexParserBean cpb = parserComplexVales(columnKey,val,isDBColumn);
				if(cpb.getColumn()!=null){
					for(int i=0;i<cpb.getColumn().length;i++){
						columns.add(clearColumnName.matcher(cpb.getColumn()[i]).replaceAll(""));
						values.add(cpb.getValues()[i]);
					}
				}
				String target = append(" [a-z|A-Z|`|\\_|\\.]+='?\\$\\[", key,"\\]'?");
				Pattern relpaceReg = patternCache.get(target);
				if(relpaceReg==null){
					relpaceReg = Pattern.compile(target);
					patternCache.put(target, relpaceReg);
				}
				sqlModel =relpaceReg.matcher(sqlModel).replaceAll(cpb.getParseFullValue()); //sqlModel.replaceAll(target, cpb.getParseFullValue());
				preparedSql = relpaceReg.matcher(preparedSql).replaceAll(cpb.getParseVale());//preparedSql.replaceAll(target, cpb.getParseVale());
			}else{
				sqlModel = sqlModel.replace("$["+key+"]", val);
				preparedSql = preparedSql.replace("$["+key+"]",isDBColumn ? "?" : val);
				if(isDBColumn){
					columns.add(clearColumnName.matcher(key).replaceAll(""));
					values.add(val);
				}
			}
		}

		preparedSql = clearPreparedSql.matcher(preparedSql).replaceAll("?");

		boolean isAutoSearch = sqlModel.indexOf(Contant.AUTO_SEARCH_MARK) > -1;
		if(isAutoSearch){
			StringBuilder autoSearch1 = new StringBuilder();
			StringBuilder autoSearch2 = new StringBuilder();
			Iterator<?> iter = query.entrySet().iterator();
			while (iter.hasNext()) {
				@SuppressWarnings("unchecked")
				Map.Entry<String,Object> entry = (Map.Entry<String,Object>) iter.next();
				String key = entry.getKey();
				String val = String.valueOf(entry.getValue());
				if(!isSpecilKeys.matcher(key).find() && !ArrayUtils.contains(params, key) && tableStruts.get(clearColumnName.matcher(key).replaceAll(""))!=null){
					if(dbQuery.isComplexBuild()){
						ComplexParserBean cpb = parserComplexVales(key,val,true);
						if(cpb.getColumn()!=null){
							for(int i=0;i<cpb.getColumn().length;i++){
								columns.add(clearColumnName.matcher(cpb.getColumn()[i]).replaceAll(""));
								values.add(cpb.getValues()[i]);
							}
						}
						if(autoSearch1.length() > 1){
							autoSearch1.append(" and");
							autoSearch2.append(" and");
						}
						autoSearch1.append(" ").append(cpb.getParseFullValue());
						autoSearch2.append(" ").append(cpb.getParseVale());
					}else{
						columns.add(clearColumnName.matcher(key).replaceAll(""));
						values.add(val);
						if(autoSearch1.length() > 1){
							autoSearch1.append(" and");
							autoSearch2.append(" and");
						}
						autoSearch1.append(" ").append(key).append("='").append(val).append("'");
						autoSearch2.append(" ").append(key).append("=?");
					}
				}
			}
			if(autoSearch1.length() > 0){
				sqlModel = sqlModel.replace(Contant.AUTO_SEARCH_MARK, autoSearch1.toString());
				preparedSql = preparedSql.replace(Contant.AUTO_SEARCH_MARK, autoSearch2.toString());
			}
		}
		dbQuery.setSql(sqlModel);
		dbQuery.setPreparedSql(preparedSql);
		dbQuery.setColumns(columns.toArray(new String[]{}));
		dbQuery.setValues(values.toArray(new Object[]{}));
	}

	private static Pattern clear = Pattern.compile("[^&\\|]");
	private static Pattern express = Pattern.compile("[^!<>%\\*=]");
	private static Pattern isPureInSearchReg = Pattern.compile("[^&\\|<>%\\*=]");
	private static Pattern clearColumnName = Pattern.compile("^.+?\\.|`");
	private static Pattern isPureInSearchRegEqual = Pattern.compile("^\\|+?$");

	public ComplexParserBean parserComplexVales(String key, String value,boolean isDBColumn) throws QDevelopException {
		ComplexParserBean cpb = new ComplexParserBean();
		boolean isNotPaser;
		//XXX 判断全部是or查询，改用in的方式
		String t = isPureInSearchReg.matcher(value).replaceAll("");
		if(t.length()>0 && isPureInSearchRegEqual.matcher(t).find()){
			isNotPaser = value.startsWith("!");
			StringBuilder sb = new StringBuilder();
			sb.append(" ").append(key).append(isNotPaser?" not":"").append(" in ('").append((isNotPaser?value.substring(1):value).replaceAll("\\|", "','")).append("')");
			cpb.setKey(key);
			cpb.setParseVale(sb.toString());
			cpb.setParseFullValue(sb.toString());
			cpb.setColumn(null);
			cpb.setValues(null);
			return cpb;
		}
		char[] exp = clear.matcher(value).replaceAll("").toCharArray();
		String[] tmp = value.split("&|\\|");
		if(tmp.length != exp.length + 1 )throw new QDevelopException(1001,"表达式有错误"+key+":"+value);
		String[] columns = null;
		String[] values = null;
		if(isDBColumn){
			columns = new String[tmp.length];
			values = new String[tmp.length];
			for(int i=0;i<tmp.length;i++){
				columns[i] = clearColumnName.matcher(key).replaceAll("");
			}
		}
		StringBuilder parserVale = new StringBuilder();
		StringBuilder fullParserVale = new StringBuilder();
		parserVale.append(" ");
		fullParserVale.append(" ");
		if(tmp.length>1)parserVale.append("(");
		if(tmp.length>1)fullParserVale.append("(");

		for(int i=0;i<tmp.length;i++){
			String v = tmp[i];
			isNotPaser = v.startsWith("!");
			String e = express.matcher(isNotPaser?v.substring(1):v).replaceAll("");
			if(i>0){
				if(exp[i-1] == '&'){
					parserVale.append(" and ");
					fullParserVale.append(" and ");
				}else if(exp[i-1] == '|'){
					parserVale.append(" or ");
					fullParserVale.append(" or ");
				}
			}
			String cv = isComplexValue.matcher(isNotPaser?v.substring(1):v).replaceAll("");
			if(e.length() == 0){
				if(isDBColumn){
					parserVale.append(key).append(isNotPaser?"<>?":"=?");
				}else{
					parserVale.append(key).append(isNotPaser?"<>":"=").append("'").append(cv).append("'");
				}
				fullParserVale.append(key).append(isNotPaser?"<>":"=").append("'").append(cv).append("'");
			}else if(e.length() == 1){
				if(e.equals(">")||e.equals("<")){
					if(isDBColumn){
						parserVale.append(key).append(e).append("?");
					}else{
						parserVale.append(key).append(e).append(cv);
					}
					fullParserVale.append(key).append(e).append(cv);
				}else if(e.equals("%")||e.equals("*")){
					cv = likeReplace.matcher((isNotPaser?v.substring(1):v)).replaceAll("%");
					if(isDBColumn){
						parserVale.append(key).append(isNotPaser?" not":"").append(" like ").append("?");
					}else{
						parserVale.append(key).append(isNotPaser?" not":"").append(" like '").append(cv).append("'");
					}
					fullParserVale.append(key).append(isNotPaser?" not":"").append(" like '").append(cv).append("'");
				}else{
					throw new QDevelopException(1001,"非法参数请求 "+key+"="+value);
				}
			}else if(e.length() > 1){
				if(isLike.matcher(e).find()){
					cv = likeReplace.matcher((isNotPaser?v.substring(1):v)).replaceAll("%");
					if(isDBColumn){
						parserVale.append(key).append(e.startsWith("!")?" not":"").append(" like ").append("?");
					}else{
						parserVale.append(key).append(e.startsWith("!")?" not":"").append(" like '").append(cv).append("'");
					}
					fullParserVale.append(key).append(e.startsWith("!")?" not":"").append(" like '").append(cv).append("'");
				}else if(e.equals(">=")||e.equals("<=")){
					if(isDBColumn){
						parserVale.append(key).append(e).append("?");
					}else{
						parserVale.append(key).append(e).append(cv);
					}
					fullParserVale.append(key).append(e).append(cv);
				}else{
					throw new QDevelopException(1001,"非法参数请求 "+key+"="+value);
				}
			}
			if(isDBColumn){
				values[i] = cv; 
			}
		}
		if(tmp.length>1)parserVale.append(")");
		if(tmp.length>1)fullParserVale.append(")");
		cpb.setKey(key);
		cpb.setParseVale(parserVale.toString());
		cpb.setParseFullValue(fullParserVale.toString());
		cpb.setColumn(columns);
		cpb.setValues(values);
		return cpb;
	}
	private static Pattern likeReplace = Pattern.compile("[%\\*]+"); 
	private static Pattern isLike = Pattern.compile("^\\!?(%|\\*)+$");

	private String escapeExprSpecialWord(String keyword) {  
		String[] fbsArr = { "\\", "$", "(", ")", "*", "+", ".", "[", "]", "?", "^", "{", "}", "|" };  
		for (String key : fbsArr) {  
			if (keyword.contains(key)) {  
				keyword = keyword.replace(key, "\\" + key);  
			}  
		}  
		return keyword;  
	}

	private static String append(Object... s) {
		StringBuffer sb = new StringBuffer();
		for (Object _s : s)
			sb.append(_s);
		return sb.toString();
	}
	private static Pattern ss = Pattern.compile("=(.+)?$| ");
	private String getSQLkey(String key,String sql){
		String target=new StringBuilder().append("$[").append(key).append("]").toString();
		int idx = sql.indexOf(target);
		if(idx < 0)return "";
		String tmp = sql.substring(0,idx);
		tmp = ss.matcher(tmp.substring(tmp.lastIndexOf(" "))).replaceAll("");
		return tmp;
	}




	private final static Map<String,Pattern> patternSelectCache = new ConcurrentHashMap<String,Pattern>();
	private final static Map<String,Pattern> patternInsertCache = new ConcurrentHashMap<String,Pattern>();
	private final static Pattern clearWhere = Pattern.compile("where +?and",Pattern.CASE_INSENSITIVE);
	private final static Pattern clearSet = Pattern.compile("set +?,",Pattern.CASE_INSENSITIVE);
	private final static Pattern clearPrefix = Pattern.compile("\\( +?,",Pattern.CASE_INSENSITIVE);
	private final static Pattern isSelect = Pattern.compile("^select ",Pattern.CASE_INSENSITIVE);
	private final static Pattern isInsert = Pattern.compile("^insert ",Pattern.CASE_INSENSITIVE);
	private final static Pattern wherePrefix = Pattern.compile("^.+ where ",Pattern.CASE_INSENSITIVE);
	private final static Pattern whereSubfix = Pattern.compile(" where .+$",Pattern.CASE_INSENSITIVE);
	private final static Pattern cleanErrClear = Pattern.compile(", +,");
	/**
	 * 根据参数动态清理sql语句中对应的变量
	 * @param sqlTemplate
	 * @param argsName
	 * @param isInsert
	 * @return
	 */
	public static String cleanNULLArgs(String sqlTemplate,String argsName){
		if(isInsert.matcher(sqlTemplate).find()){
			Pattern clear = patternInsertCache.get(argsName);
			if(clear==null){
				clear = Pattern.compile(",?`?'?\\$?\\[?\\b("+argsName+")\\b\\]?'?`?",Pattern.CASE_INSENSITIVE);
				patternInsertCache.put(argsName, clear);
			}
			//			return clearPrefix.matcher(clear.matcher(sqlTemplate).replaceAll(" ")).replaceAll("(");
			return cleanErrClear.matcher(clearPrefix.matcher(clear.matcher(sqlTemplate).replaceAll(" ")).replaceAll("(")).replaceAll(",");
		}else if(isSelect.matcher(sqlTemplate).find()){
			Pattern clear = patternSelectCache.get(argsName);
			if(clear == null){
				clear = Pattern.compile("(and|or|,)?( +)?`?([0-9a-zA-Z]+\\.)?\\b("+argsName+")\\b`? ?= ?'?\\$\\[\\b("+argsName+")\\b\\]'?",Pattern.CASE_INSENSITIVE);
				patternSelectCache.put(argsName, clear);
			}
			return clearWhere.matcher(clear.matcher(sqlTemplate).replaceAll(" ")).replaceAll("where ");
		}else{
			Pattern clear = patternSelectCache.get(argsName);
			if(clear == null){
				clear = Pattern.compile("(and|or|,)?( +)?`?\\b("+argsName+")\\b`? ?= ?'?\\$\\[\\b("+argsName+")\\b\\]'?",Pattern.CASE_INSENSITIVE);
				patternSelectCache.put(argsName, clear);
			}
			return new StringBuffer().append(clearSet.matcher(clear.matcher(whereSubfix.matcher(sqlTemplate).replaceAll("")).replaceAll(" ")).replaceAll("set "))
					.append(" where ").append(wherePrefix.matcher(sqlTemplate).replaceAll("")).toString();
		}
	}
	
	/**
	 * 判断sql参数中的参数是否需要进行预编译处理
	 * @param key
	 * @param dbsl
	 * @param strParam
	 * @return
	 */
	private boolean isCasePreBuildSQL(String key,DBStrutsLeaf dbsl,String strParam){
		if(dbsl == null) return false;
		return strParam==null || strParam.indexOf(append("|",key,"|")) == -1;
	}
	
}
