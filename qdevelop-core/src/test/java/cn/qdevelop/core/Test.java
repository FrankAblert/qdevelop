package cn.qdevelop.core;

import java.util.HashMap;

import cn.qdevelop.common.exception.QDevelopException;
import cn.qdevelop.core.standard.IDBResult;
import cn.qdevelop.core.template.QDevelopHelper;

public class Test {

	public static void main(String[] args) {
		HashMap query = new HashMap();
		query.put("index", "products-search-action");
		query.put("pid", "50604|50605");
		try {
			IDBResult rb = DatabaseFactory.getInstance().queryDatabase(query);
			for(int i=0;i<rb.getSize();i++){
				System.out.println(rb.getResult(i));
			}
		} catch (QDevelopException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		QDevelopHelper.createSQLConfig("default", "products_log");
		//		String s = "insert into mytest_log(epuad_id,record,ctime) value ({my_test_0.LAST_INSERT_ID},'xxxxxxxx',now())";
		//		Pattern hasAutoIncrementParam = Pattern.compile("\\{[a-zA-z0-9_]+\\.LAST_INSERT_ID\\}");
		//		Pattern getAutoIncrementKey = Pattern.compile("^.*\\{|\\.LAST_INSERT_ID\\}.*$");
		//		System.out.println(getAutoIncrementKey.matcher(s).replaceAll(""));
		//		System.out.println(hasAutoIncrementParam.matcher(s).replaceAll("1"));
		//		Pattern clearColumnName = Pattern.compile("^.+?\\.|`");
		//		System.out.println(clearColumnName.matcher("e.`sad`").replaceAll(""));
//		Pattern p = Pattern.compile("\\||>");
//		//		String sql = "select * FROM janson where asdadasdasdasd";
//		//		Pattern cleanPrev = Pattern.compile("^select .+ from", Pattern.CASE_INSENSITIVE);
//		//		Matcher m = p.matcher("select * fRom (select * FROM janson)t");
//		//		int i=0;
//		//		while(m.find()){
//		//			i++;
//		//		}
////		String[] tmp = "a".split("\\||>");
////		for(String k : tmp)
////			System.out.println(k);
//		Pattern r = Pattern.compile("'\\?'");
//		System.out.println(r.matcher("a'?'a").replaceAll("?"));
		
//		QDevelopHelper.createSQLConfig("qd_product_center_write", "products");
//		System.out.println(Charsets.UTF_8.toString());
		
	}

}
