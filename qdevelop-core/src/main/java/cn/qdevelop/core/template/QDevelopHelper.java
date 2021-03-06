package cn.qdevelop.core.template;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import cn.qdevelop.common.exception.QDevelopException;
import cn.qdevelop.core.db.bean.DBStrutsBean;
import cn.qdevelop.core.db.bean.DBStrutsLeaf;
import cn.qdevelop.core.db.connect.ConnectFactory;
import cn.qdevelop.core.db.execute.TableColumnType;

public class QDevelopHelper {


	/**
	 * 生成sql配置模版
	 * @param connName qdevelop-database.xml 中的数据库配置名
	 * @param tableName	数据库内的表名称
	 */
	public static void createSQLConfig(String connName,String tableName){
		//		StringBuffer xml = new StringBuffer();
		StringBuffer selectSQL = new StringBuffer();
		StringBuffer selectDirect = new StringBuffer();
		StringBuffer insertSQL = new StringBuffer();
		StringBuffer updateSQL = new StringBuffer();
		StringBuffer deleteSQL = new StringBuffer();
		ArrayList<String> formatter = new ArrayList<String>();
		String cn="";
		Connection conn = null ;
		String autoIncrementName="";
		try {
			selectSQL.append("select ");
			selectDirect.append("select ");
			insertSQL.append("insert into ").append(tableName).append("(");
			updateSQL.append("update ").append(tableName).append(" set ");

			conn = ConnectFactory.getInstance(connName).getConnection();
			DBStrutsBean dbb = TableColumnType.getInstance().getTableStrutsBean(conn, tableName);
			Iterator<DBStrutsLeaf> iter = dbb.values().iterator();
			int idx = 0;
			StringBuffer c = new StringBuffer();
			StringBuffer v = new StringBuffer();
			StringBuffer i = new StringBuffer();
			StringBuffer s = new StringBuffer();
			StringBuffer w = new StringBuffer();
			while(iter.hasNext()){
				DBStrutsLeaf sl = iter.next();
				selectSQL.append(idx>0?",":"").append(sl.getColumnName());
				selectDirect.append(idx>0?",":"").append(sl.getColumnName());
				if(!sl.isAutoIncrement()){
					c.append(c.length()>0?",":"").append(sl.getColumnName());
					v.append(v.length()>0?",":"").append(sl.getColumnType()==2?"'":"").append("$[").append(sl.getColumnName()).append("]").append(sl.getColumnType()==2?"'":"");
					s.append(s.length()>0?",":"").append(sl.getColumnName()).append("=").append(sl.getColumnType()==2?"'":"").append("$[").append(sl.getColumnName()).append("]").append(sl.getColumnType()==2?"'":"");
					w.append(" and ").append(sl.getColumnName()).append("=").append(sl.getColumnType()==2?"'":"").append("$[").append(sl.getColumnName()).append("]").append(sl.getColumnType()==2?"'":"").append(++idx%3==0?"\r\n			":"");
				}else{
					++idx;
					autoIncrementName = sl.getColumnName();
					i.append(sl.getColumnName()).append("=$[").append(sl.getColumnName()).append("]");
				}
				if(sl.getColumnTypeName().equalsIgnoreCase("tinyint")){
					formatter.add("<prop-formatter result-key=\""+sl.getColumnName()+"\" prop-key=\""+tableName+"_"+sl.getColumnName()+"_dict\"/>");
				}else if(sl.getColumnTypeName().equalsIgnoreCase("date") || sl.getColumnTypeName().equalsIgnoreCase("datetime")){
					formatter.add("<date-formatter result-key=\""+sl.getColumnName()+"\" date-style=\"yyyy-MM-dd HH:mm:ss\"/>");
				}
			}
			selectDirect.append(" from ").append(tableName).append(" where \r\n			 ").append(i).append(w);
			selectSQL.append(" from ").append(tableName).append(" where {DYNAMIC}");
			updateSQL.append(s).append(" where ").append(i);
			insertSQL.append(c).append(") value (").append(v).append(")");
			deleteSQL.append("delete from ").append(tableName).append(" where ").append(i);
			cn = conn.getCatalog();
		} catch (QDevelopException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			try {
				if(conn!=null){
					conn.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
//		System.out.println(selectSQL);
//		System.out.println(insertSQL);
//		System.out.println(updateSQL);
//		System.out.println(deleteSQL);
		SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss");
		String random = sdf.format(new Date());
		OutputStreamWriter fw=null;
		File store  = new File(cn+"."+tableName+".sql.xml"); 
		try {
			fw=new OutputStreamWriter(new  FileOutputStream(store),"utf-8");
			fw.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
			fw.write("\r\n");fw.write("<!-- xml中常用转义符写法： （ &  &amp; ）（ <  &lt; ） （ >  &gt; ）-->");
			fw.write("\r\n");fw.write("<SQLConfig>");
			
//			fw.write("\r\n");fw.write("	<!--<property index=\""+tableName+"-search-action\" connect=\""+connName+"\" explain=\""+tableName+"查询\">");
//			if(formatter.size()>0){
//				fw.write("\r\n");fw.write("		<!--");
//				fw.write("\r\n");fw.write("		<formatter>");
//				for(String fs : formatter){
//					fw.write("\r\n");fw.write("			"+fs);
//				}
//				fw.write("\r\n");fw.write("		</formatter>");
//				fw.write("\r\n");fw.write("		-->");
//			}
//			fw.write("\r\n");fw.write("		<sql>"+selectSQL.toString()+"</sql>");
//			fw.write("\r\n");fw.write("	</property>-->");
//			fw.write("\r\n");
			
			fw.write("\r\n");fw.write("	<property index=\""+tableName+"_select_"+random+"\" connect=\""+connName+"\" explain=\""+tableName+"查询\">");
			if(formatter.size()>0){
				fw.write("\r\n");fw.write("		<!--");
				fw.write("\r\n");fw.write("		<formatter>");
				for(String fs : formatter){
					fw.write("\r\n");fw.write("			"+fs);
				}
				fw.write("\r\n");fw.write("		</formatter>");
				fw.write("\r\n");fw.write("		-->");
			}
			fw.write("\r\n");fw.write("		<!--<sql>"+selectSQL.toString()+"</sql>-->");
			fw.write("\r\n");fw.write("		<sql>"+selectDirect.toString()+"</sql>");
			fw.write("\r\n");fw.write("	</property>");
			fw.write("\r\n");
			
			fw.write("\r\n");fw.write("	<property index=\""+tableName+"_insert_"+random+"\" connect=\""+connName+"\" explain=\""+tableName+"插入\">");
			fw.write("\r\n");fw.write("		<sql repeat=\"\"  repeat-concat=\"^\" fetch-zero-err=\"true\" is-full-param=\"false\">");
			fw.write("\r\n");fw.write("			"+insertSQL.toString());
			fw.write("\r\n");fw.write("		</sql>");
			//			fw.write("\r\n");fw.write("		<sql>"+insertSQL.toString()+"</sql>");
			if(autoIncrementName.length()>0){
				fw.write("\r\n");fw.write("		<!--<sql>insert other_table("+autoIncrementName+") value({"+tableName+".LAST_INSERT_ID})</sql>-->");
			}
			fw.write("\r\n");fw.write("	</property>");
			fw.write("\r\n");
			fw.write("\r\n");fw.write("	<property index=\""+tableName+"_update_"+random+"\" connect=\""+connName+"\" explain=\""+tableName+"修改\">");
			fw.write("\r\n");fw.write("		<sql  repeat=\"\"  repeat-concat=\"^\" fetch-zero-err=\"true\" is-full-param=\"false\">");
			fw.write("\r\n");fw.write("			"+updateSQL.toString());
			fw.write("\r\n");fw.write("		</sql>");
			//			fw.write("\r\n");fw.write("		<sql fetch-zero-err=\"true\">"+updateSQL.toString()+"</sql>");
			fw.write("\r\n");fw.write("	</property>");
			fw.write("\r\n");
			fw.write("\r\n");fw.write("	<!--");
			fw.write("\r\n");fw.write("	<property index=\""+tableName+"_delete_"+random+"\" connect=\""+connName+"\" explain=\""+tableName+"删除\">");
			fw.write("\r\n");fw.write("		<sql  repeat=\"\" repeat-concat=\"^\" fetch-zero-err=\"true\">");
			fw.write("\r\n");fw.write("			"+deleteSQL.toString());
			fw.write("\r\n");fw.write("		</sql>");
			//			fw.write("\r\n");fw.write("		<sql fetch-zero-err=\"true\">"+deleteSQL.toString()+"</sql>");
			fw.write("\r\n");fw.write("	</property>");
			fw.write("\r\n");fw.write("	-->");
			
			fw.write("\r\n");fw.write("</SQLConfig>");
			fw.flush();
			System.out.println("生成SQL配置文件："+store.getAbsolutePath());
		} catch (Exception e) {
		}finally{
			try {
				if(fw!=null)
					fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
//	public static void createDocByConfig(String index){
//		
//	}


}
