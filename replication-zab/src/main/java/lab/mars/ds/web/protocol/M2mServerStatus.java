package lab.mars.ds.web.protocol;

/**
 * 服务器状态枚举
 */
public enum M2mServerStatus {

    STARTED(1), STOPED(0);

    private Integer status;

    M2mServerStatus(Integer status) {
        this.status = status;
    }

    public Integer getStatus() {
        return status;
    }


}
