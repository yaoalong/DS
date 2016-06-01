package lab.mars.ds.web.protocol;

import java.io.Serializable;

public class M2mServerLoadDO implements Serializable ,Comparable<M2mServerLoadDO>{

    /**
     * 
     */
    private static final long serialVersionUID = 7514662116801082052L;
    private String label;
    private Long y;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Long getY() {
        return y;
    }

    public void setY(Long y) {
        this.y = y;
    }

    @Override
    public int compareTo(M2mServerLoadDO o) {
       return this.label.compareTo(o.getLabel());
    }
}
