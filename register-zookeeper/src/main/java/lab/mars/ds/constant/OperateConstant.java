package lab.mars.ds.constant;

/**
 * Author:yaoalong.
 * Date:2016/3/8.
 * Email:yaoalong@foxmail.com
 */
public enum OperateConstant {
    DETECT(0), START(1);
    int code;

    private OperateConstant(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
