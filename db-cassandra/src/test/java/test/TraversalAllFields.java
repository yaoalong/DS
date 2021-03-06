package test;

import java.lang.reflect.Field;

/**
 * Author:yaoalong. Date:2016/3/4. Email:yaoalong@foxmail.com
 */
public class TraversalAllFields {
    public static void getObjAttr(Object obj) {
        // 获取对象obj的所有属性域
        Field[] fields = obj.getClass().getDeclaredFields();

        for (Field field : fields) {
            // 对于每个属性，获取属性名
            String varName = field.getName();
            try {
                boolean access = field.isAccessible();
                if (!access)
                    field.setAccessible(true);

                // 从obj中获取field变量
                Object o = field.get(obj);
                System.out.println("变量： " + varName + " = " + o);

                if (!access)
                    field.setAccessible(false);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

}
