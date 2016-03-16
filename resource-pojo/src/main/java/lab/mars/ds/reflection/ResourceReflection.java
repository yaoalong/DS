package lab.mars.ds.reflection;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lab.mars.ds.reflection.KryoConfiguration.ClassAccess;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.Sets;
/**
 *
 */

/**
 * Created by haixiao on 2015/6/4. Email: wumo@outlook.com
 */
public class ResourceReflection {
    private static Set<Class> defaultClz;

    
    
    
    static {
        defaultClz = Sets.newHashSet(Integer.class, Float.class, Double.class,
                Boolean.class, Long.class, BigDecimal.class, String.class,
                Date.class,byte.class);
    }

    protected static final ThreadLocal<Kryo> kryo = new ThreadLocal<Kryo>() {
        protected com.esotericsoftware.kryo.Kryo initialValue() {
            Kryo k = new Kryo();
            // k.setRegistrationRequired(true);
            KryoConfiguration.configure(k);
            // k.setDefaultSerializer(PublicFieldSerializer.class);
            // k.setReferences(false);
            // k.setCopyReferences(false);
            return k;
        }
    };

    public static Object deserializeKryo(byte[] data) {
        try (Input in = new Input(data)) {
            return kryo.get().readClassAndObject(in);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object deserializeKryo(byte[] data, int offset, int count) {
        try (Input in = new Input(data, offset, count)) {
            return kryo.get().readClassAndObject(in);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] serializeKryo(Object obj) {
        try (Output out = new Output(4096, -1)) {
            try {
                kryo.get().writeClassAndObject(out, obj);
                return out.toBytes();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <R> R partialRetrieve(R source, R retrieveRef) {
        ClassAccess clzAccess = KryoConfiguration.get(source.getClass());
        if (clzAccess == null)
            return null;
        R instance = (R) clzAccess.constructorAccess.newInstance();
        clzAccess.fieldIndex.forEach((f, i) -> {
            if (clzAccess.fieldAccess.get(retrieveRef, i) != null)
                clzAccess.fieldAccess.set(instance, i,
                        clzAccess.fieldAccess.get(source, i));
        });
        return instance;
    }

    @SuppressWarnings("unchecked")
    public static <R> R partialRetrieve(R source, Set<String> retrieveKeys) {
        ClassAccess clzAccess = KryoConfiguration.get(source.getClass());
        if (clzAccess == null)
            return null;
        R instance = (R) clzAccess.constructorAccess.newInstance();
        clzAccess.fieldIndex.forEach((f, i) -> {
            if (retrieveKeys == null || retrieveKeys.isEmpty()
                    || retrieveKeys.contains(f))
                clzAccess.fieldAccess.set(instance, i,
                        clzAccess.fieldAccess.get(source, i));
        });
        return instance;
    }

    @SuppressWarnings("unchecked")
    public static <R> void partialUpdate(R source, R updateRef) {
        ClassAccess clzAccess = KryoConfiguration.get(source.getClass());
        if (clzAccess == null)
            return;
        clzAccess.fieldIndex.forEach((f, i) -> {
            Object content = clzAccess.fieldAccess.get(updateRef, i);
            if (content != null)
                clzAccess.fieldAccess.set(source, i, content);
        });
    }

    @SuppressWarnings("unchecked")
    public static <R> void partialUpdate(R source, Map<String, Object> updateRef) {
        ClassAccess clzAccess = KryoConfiguration.get(source.getClass());
        if (clzAccess == null)
            return;
        clzAccess.fieldIndex.forEach((f, i) -> {
            if (updateRef.containsKey(f))
                clzAccess.fieldAccess.set(source, i, updateRef.get(f));
        });
    }

    @SuppressWarnings("unchecked")
    public static <R> R deserialize(Class<R> cls, Map<String, Object> result) {
        ClassAccess clzAccess = KryoConfiguration.get(cls);
        if (clzAccess == null)
            return null;
        R instance = (R) clzAccess.constructorAccess.newInstance();
        if (result != null)
            result.forEach((k, v) -> {
                if (clzAccess.fieldIndex.containsKey(k)) {
                    Object fieldObj = v;
                    if (v instanceof ByteBuffer) {
                        ByteBuffer _v = (ByteBuffer) v;
                        fieldObj = deserializeKryo(_v.array(), _v.position(),
                                _v.remaining());
                    }
                    clzAccess.fieldAccess.set(instance,
                            clzAccess.fieldIndex.get(k), fieldObj);
                }
            });
        return instance;
    }

    public static Map<String, Object> serialize(Object instance) {
        Map<String, Object> result = new HashMap<>();
        Class clz = instance.getClass();
        ClassAccess clzAccess = KryoConfiguration.get(clz);
        if (clzAccess == null)
            return null;
        clzAccess.fieldIndex.forEach((field, index) -> {
            Object fieldObj = clzAccess.fieldAccess.get(instance, index);
            if (fieldObj != null)
                result.put(field,
                        defaultClz.contains(fieldObj.getClass()) ? fieldObj
                                : ByteBuffer.wrap(serializeKryo(fieldObj)));
        });
        return result;
    }
}
