package smart.old.api.host;public class ServiceDeskHostConfig implements HostConfig {//	private  String host="http://192.168.1.109:8080";//	private  String host="http://10.10.152.67:8080";//	private  String host="http://10.10.152.26:9080";//	private  String host="http://10.10.152.26:8080";	private  String host="http://172.24.24.120:8911";  //fvsd_3.7地址	//	private String projectName="itsm";//	private String projectName="fvsd";	private String projectName="fvsd-feature-3.7";		@Override	public String getHost() {		return host;	}	@Override	public String getProjectName() {	return  projectName;	}	@Override	public String getAPIHost() {		StringBuilder sb=new StringBuilder(host).append("/").append(projectName);		return sb.toString();	}}