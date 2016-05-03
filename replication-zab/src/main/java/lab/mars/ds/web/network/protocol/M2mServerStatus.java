package lab.mars.ds.web.network.protocol;

/**
 *
 * 服务器状态枚举
 *
 */
public enum M2mServerStatus {

    STARTED(1), STOPED(0);

    M2mServerStatus(Integer status) {
        this.status = status;
    }

    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

}
