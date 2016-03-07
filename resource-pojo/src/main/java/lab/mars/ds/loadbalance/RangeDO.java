package lab.mars.ds.loadbalance;

/**
 * Author:yaoalong.
 * Date:2016/3/3.
 * Email:yaoalong@foxmail.com
 */
public class RangeDO {

    private Long start;

    private Long end;

    public RangeDO(){

    }
    public RangeDO(Long start, Long end){
        this.start=start;
        this.end=end;

    }

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public Long getEnd() {
        return end;
    }

    public void setEnd(Long end) {
        this.end = end;
    }
}
