package lab.mars.ds.loadbalance;

/**
 * Author:yaoalong.
 * Date:2016/3/17.
 * Email:yaoalong@foxmail.com
 */

public class LoadBalanceException extends Exception {

    private Code code;
    private String message;

    public LoadBalanceException(Code code) {
        this.code = code;
    }

    public LoadBalanceException(Code code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code.code;
    }

    public void setCode(Code code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public enum Code {

        SERVERSIZE_TOO_BIG(-300),
        SERVER_IS_NULL(-299),
        SERVERSIZE_IS_ERROR(-298),
        REPLICATION_FACTOR_PARAM_ERROR(-297),
        KEY_PARAM_NULL(-296),
        NUM_OF_VIRTURAL_NODE_IS_RROR(-295),
        SERGERS_IS_NOT_INIT(-294);
        private final int code;

        Code(int code) {
            this.code = code;
        }
    }

    public static class ServerSizeTooBig extends LoadBalanceException {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public ServerSizeTooBig() {
            super(Code.SERVERSIZE_TOO_BIG, "server size传入的过大");
        }

    }
}
