package org.lab.mars.onem2m;

/**
 * Author:yaoalong.
 * Date:2016/2/24.
 * Email:yaoalong@foxmail.com
 */
public class M2mKeeperException extends Exception{
    private Code code;
    private String path;
    public M2mKeeperException(Code code){
        this.code=code;
    }
    public M2mKeeperException(Code code, String path){
        this.code=code;
        this.path=path;
    }
    public  enum Code{

        OK(1),
        NODEEXISTS(-110);
        private final int code;
        Code(int code){
            this.code=code;
        }
    }
    public static class NodeExistsException extends M2mKeeperException {
        public NodeExistsException() {
            super(Code.NODEEXISTS);
        }
        public NodeExistsException(String path) {
            super(Code.NODEEXISTS, path);
        }
    }

    public Integer getCode() {
        return code.code;
    }

    public void setCode(Code code) {
        this.code = code;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
