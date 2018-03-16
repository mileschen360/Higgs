import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.IntStream;

import org.antlr.v4.runtime.*;
import org.json.JSONArray;
import org.json.JSONObject;


public class Main {

	public static String config_file = "/home/mschen/code/higgs/SOCodeExtractor/src/extractor.conf";
	public static String namelist_file = "/home/mschen/code/higgs/SOCodeExtractor/namelist.txt";
	public static List<String> stopwords = null;
	public static List<String> keywords = null;
	
	public static Set<String> knownAPIs = null;
	
	public static String connURL;
	public static String userName;
	public static String password;
	

	public static Connection conn;
	public static Statement stat;
	public static ResultSet res;
//	public static ResultSetMetaData rsmd;
	
	public static String output_dir;
	
	public static final int COL_ID = 1;
	public static final int COL_PARENT_ID = 2;
	public static final int COL_BODY = 3;
	public static final int COL_TAGS = 4;
	public static final int MIN_TOKEN_SIZE = 15;
	
	public static void Load_Config() {
		try{
			Properties prop = new Properties();
			InputStream is = new FileInputStream(config_file);
			//InputStream is = Main.class.getResourceAsStream("/extractor.config");
			
			prop.load(is);
			
			connURL = prop.getProperty("postgreSQL.conn");
			userName = prop.getProperty("postgreSQL.user");
			password = prop.getProperty("postgreSQL.passwd");
			
			knownAPIs = new HashSet<String>();
			output_dir = prop.getProperty("output.dir");
			
			stopwords = Arrays.asList(new String[]{"size", "get", "set", "close", "write", "flush", "read", "getValue", "setValue",
					 							   "match", "add", "<init>", "readLine", "writeLine", "put", "getId", "setId", 
					 							   "getWidth", "setWidth", "execute", "getText", "setText", "hasNext", "next", "clone",
													"getProperty", "setProperty", "getChildren", "valueOf", "lookup", "clear", "Clear"});

			// only interested in questions contains at least 2 of the following tags							
			keywords = Arrays.asList(new String[]{
					"active-directory",
					"aes",
					"android",
					"applet",
					"arrays",
					"asn.1",
					"axis2",
					"authentication",
					"bouncycastle",
					"byte",
					"bytearray",
					"certificate",
					"classnotfoundexception",
					"client",
					"coldfusion",
					"connection",
					"cryptography",
					"cryptoapi",
					"des",
					"diffie-hellman",
					"digital-certificate",
					"eclipse",
					"email",
					"encoding",
					"encryption",
					"gssapi",
					"hash",
					"hmac",
					"http",
					"httpclient",
					"https",
					"java",
					"java1.4",
					"java-7",
					"java-ee",
					"java-metro-framework",
					"javamail",
					"javax.crypto",
					"jax-ws",
					"jce",
					"jdbc",
					"jks",
					"jndi",
					"jni",
					"jsse",
					"kerberos",
					"key",
					"keystore",
					"ldap",
					"md5",
					"mule",
					"mysql",
					"netbeans",
					"native",
					"oauth",
					"ocsp",
					"openssh",
					"openssl",
					"pbkdf2",
					"pkix",
					"policy",
					"private-key",
					"proxy",
					"public-key-encryption",
					"random",
					"rsa",
					"runtime",
					"security", 
					"securitymanager",
					"sha",
					"sha512",
					"sign",
					"single-sign-on",
					"sockets",
					"spnego",
					"spring-boot",
					"spring-security",
					"ssl",
					"ssl-certificate",
					"sslhandshakeexception",
					"string",
					"tls",
					"text-files",
					"tomcat",
					"tripledes",
					"truststore",
					"url",
					"web-services",
					"ws-security",
					"x509",
					"x509certificate",
					"X509certificate",
					"X509certificate2",
			});
			
			BufferedReader in = new BufferedReader(new FileReader(new File(namelist_file)));
			String st = null;
			while ((st = in.readLine()) != null) {
				knownAPIs.add(st.trim());
			}			
			in.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println("hello world");
		Load_Config();
		
		Class.forName("org.postgresql.Driver");
        conn = DriverManager.getConnection(connURL, userName, password);
        
//        String sql = "select id, body from posts where parentid IS NOT NULL";
//        String sql = args[0];
//         String sql = "select id, body from posts "
//		  + "where parentid IS NOT NULL AND"
//		  + " id <= 100" + "AND"
//		  + " id > 0";
//        int COUNT = Integer.valueOf(args[0]); 
//        int COUNT = 1;
//        int idx = 8090;		
//        int idx = Integer.valueOf(args[1]);
//        String sql = "select id, body from posts where parentid IS NOT NULL AND id <= " + (idx * COUNT) 
//        		+ "AND id > " + (idx-1) * COUNT;
//        String sql = "select id, parentid, body, tags from posts";



		String sql = "SELECT posts.id, posts.parentid, posts.body, posts.tags "
				   + "FROM posts INNER JOIN marks on posts.id=marks.postid "
				   + "WHERE posts.id >= "+args[0]+" AND posts.id < "+args[1]+" "  // each time 5000000
				   + "AND posts.parentid IS NULL "
				   + "AND (marks.processed IS NULL OR marks.processed=FALSE)";
		System.out.println(sql);
        //        String sql = "select id, body from posts where id=5453976";
//        String sql = "select id, body from posts where id=9175004";
//        String sql = "select id, body from posts where id=23101";
        conn.setAutoCommit(false);
        stat = conn.createStatement();
        stat.setFetchSize(5000000);
        res = stat.executeQuery(sql);
//        rsmd = res.getMetaData();
        BufferedWriter writer = new BufferedWriter(new FileWriter(output_dir + "security-posts.txt"));
        
        int counter = 0;
        while(res.next()) {
        	// Object parentId = res.getObject(COL_PARENT_ID);
        	// if (parentId != null) {  // this can be achived by SQL
        	// 	continue;
        	// }else{
			// 	//System.out.println("working on question "+res.getObject(COL_ID));
			// }
        	String tags = (String)res.getObject(COL_TAGS);
			Integer parentid = (Integer)res.getObject(COL_ID);
        	if (tags == null){
				//System.out.println("skipped this no tag question");
			}else{
				//System.out.println("tags:"+tags);
				String[] sTags = tags.split(">");
				int tagCounter = 0;
				for (String t : sTags) {
					t = t.substring(1);
					if (keywords.contains(t)) {
						tagCounter ++;
					}
				}
	        	if (tagCounter < 2){
					//System.out.println("skipped this <2 tags question");
				}else{
					// query all answers to this question
					String sql2 = "select id, body from posts where parentid = " + parentid;
					System.out.println(sql2);
					stat = conn.createStatement();
					stat.setFetchSize(500); // at most 
					ResultSet res2 = stat.executeQuery(sql2);
					
					while(res2.next()) {
						String body = (String)res2.getObject(2);
						Integer id = (Integer)res2.getObject(COL_ID);
						if (body.contains("<code>")) {
							//Integer id = (Integer)res2.getObject(COL_ID); //has been moved out of if
							System.out.println(++counter + ": " + id);
							String[] segs = body.split("<code>");
							JSONObject jObj = null;
							boolean idWritten = false;
							for (int i = 1; i < segs.length; i++) {
								String tmp = segs[i];
								int endIndex = tmp.indexOf("</code>");
								if (endIndex == -1) {
									continue;
								}
								tmp = tmp.substring(0, endIndex);
								ANTLRInputStream stream = new ANTLRInputStream(tmp);
								int tokenCount = countTokens(stream);
								if (tokenCount >= MIN_TOKEN_SIZE) {
									try {
										System.out.println("JavaBaker.getSecurityAPIs ...");
										jObj = restAPIAccess.JavaBaker.getSecurityAPIs(tmp);
										System.out.println("Done");
									} catch (Exception e) {// For non-Java code, this exception can be thrown
		//            					System.out.println("This is not java code, so no need to process it");
										System.out.println(e);
										continue;
									}     
									Object val = jObj.get("api_elements");
		//            				System.out.println(val);
									if (val instanceof JSONArray) {
										JSONArray jArr = (JSONArray) jObj.get("api_elements");
										if (jArr.length() == 0) {   
											// do nothing, because it is security irrelevant
										} else {
											if (containsSecureMethod(jArr)) {
												System.out.println("contains secure method");
												
												saveCode(id, i-1);
												if (!idWritten) {
													writer.write(id + "\n");
													idWritten = true;
												}
											}	
										}
									} 
		//            				else { //comment by nmeng: If there is only one API_TYPE inferred, it is not interesting. The reason is the class name may be accidentally identical to a secure API
		//            					if (isKnownAPI ((JSONObject)val)) {
		//            						saveCode(id, i-1);
		//            					}
		//            				}        				
								}
							}
						} // end if has code tag
						String sql_mark_processed_answer = "UPDATE marks SET processed=TRUE WHERE postid="+id;
						stat = conn.createStatement();
						stat.executeUpdate(sql_mark_processed_answer);
					} // next answer post
				} // end if ntags < 2
			} // end if no tags
			String sql_mark_processed_question = "UPDATE marks SET processed=TRUE WHERE postid="+parentid;
			stat = conn.createStatement();
			stat.executeUpdate(sql_mark_processed_question);	
			conn.commit();
        } // next question post
		writer.close();
		stat.close();
		conn.close();
	}

	private static int countTokens(ANTLRInputStream stream) {
		Java8Lexer lexer = new Java8Lexer(stream);
		return lexer.getAllTokens().size();
	}
	
	//This is a filter to remove the code snippets where there is only one method whose name is accidentally identical to one of the secure APIs
	private static boolean containsSecureMethod(JSONArray jArr) {
		List<Set<String>> classAPIs = new ArrayList<Set<String>>();
		List<Set<String>> methodAPIs = new ArrayList<Set<String>>();
		Set<String> cleanAPIs = null;
		Set<String> uniqueClassAPIs = new HashSet<String>();
		Set<String> uniqueMethodAPIs = new HashSet<String>();
		String className = null;
		String methodName = null;
		for (int i = 0; i < jArr.length(); i++) {
			JSONObject elem = jArr.getJSONObject(i);
			ArrayList apis = (ArrayList)elem.get("elements");
			String type = (String)elem.get("type");
			cleanAPIs = new HashSet<String>();
			if (type.equals("api_type")) {
				for (int j = 0; j < apis.size(); j++) {
					className = elem.getString("name");
					String api = (String)apis.get(j);
					api = api.replace("\"", "");
					if (knownAPIs.contains(api)) {
						cleanAPIs.add(api);
//						System.out.println(api + ": " + api.hashCode());
					}
				}
				if (!cleanAPIs.isEmpty()) {
//					char firstChar = className.charAt(0);
					if (uniqueClassAPIs.add(className)) {
						classAPIs.add(cleanAPIs);
					}
				}
			} else if (type.equals("api_method")) {
				for (int j = 0; j < apis.size(); j++) {
					methodName = elem.getString("name");
					String api = (String)apis.get(j);
					api = api.replace("\"", "");
					api = api.substring(0, api.indexOf("("));
					api = api.substring(0, api.lastIndexOf("."));
					if (knownAPIs.contains(api)) {						
						cleanAPIs.add(api);
//						System.out.println(api + ": " + api.hashCode());
					}
				}
				if (!cleanAPIs.isEmpty() && uniqueMethodAPIs.add(methodName)) {
					methodAPIs.add(cleanAPIs);
				}
			}			
		}
		if (classAPIs.isEmpty())
			return false;
		if (methodAPIs.isEmpty()) {
			if (uniqueClassAPIs.size() == 1) {
				return false;
			} else {
				return true;
			}
		}			
		classAPIs.removeAll(methodAPIs);
		if (classAPIs.size() < 2) {
			uniqueMethodAPIs.removeAll(stopwords);
			if (uniqueMethodAPIs.isEmpty())
				return false;
		} 
		return true;
	}
	
	private static boolean isKnownAPI(JSONObject elem) {
		boolean flag = false;
		ArrayList apis = (ArrayList)elem.get("elements");
		for (int k = 0; k < apis.size(); k++) {
			String api = (String)apis.get(k);
			api = api.replace("\"", "");
			if (knownAPIs.contains(api)) {
				flag = true;
				break;
			}
		}
		return flag;
	}


    private static void saveCode(Integer postid, int indx) throws Exception {
		saveCodeToDB(postid, indx);
	}

    private static void saveCodeToFile(Integer id, int idx) throws IOException {
    	String code = restAPIAccess.JavaBaker.getParsedString();
    	code = code.replace("&gt;", ">")
    			   .replace("&lt;", "<")
    			   .replace("&amp;", "&");
//    	System.out.println(code);
    	String path = output_dir + "Snippet" + id + "_" + idx + ".java";
//		System.out.println(path);
    	BufferedWriter writer = new BufferedWriter(new FileWriter(path));
		writer.write(code);
		writer.close();
	}
	

	private static void saveCodeToDB(Integer postid, int indx) throws Exception{
        stat = conn.createStatement();		
    	String code = restAPIAccess.JavaBaker.getParsedString();
    	code = code.replace("&gt;", ">")
    			   .replace("&lt;", "<")
    			   .replace("&amp;", "&");

		String sql = "INSERT INTO snippets(code, indx, postid) VALUES($aesc6$"+code+"$aesc6$,"+indx+","+postid+") ON CONFLICT DO NOTHING";  // upsert supported by postgres 9.5+
		stat.executeUpdate(sql);
		System.out.println("committing to database");
		conn.commit();
	}

} 
