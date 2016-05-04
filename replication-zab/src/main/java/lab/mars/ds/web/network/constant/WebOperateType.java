package lab.mars.ds.web.network.constant;

public enum WebOperateType {

    getStatus(1, "查询服务器状态"), retriveLocalKey(2, "查询本地是否含有某个key"), retriveRemoteKey(
            3, "查询远程服务器是否含有某个key"), ReplyRetriverRemoteKey(4, "远程服务器对检索key的回复"),
    lookServerLoad(5, "查看本地服务器的Load"),
    lookRemoteServerLoad(6, "查看远程服务器的Load"),
    lookReplicationServers(7, "查看复制服务器列表");

    private Integer code;
    private String desc;

    WebOperateType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

}
