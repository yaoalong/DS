import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.*;
import java.util.Arrays;

import static javafx.scene.input.KeyCode.U;

/**
 * Author:yaoalong.
 * Date:2016/5/9.
 * Email:yaoalong@foxmail.com
 */
public class TestSerialize {
    /** Number of runs. */
    private static final int RUN_CNT = 3;

    /** Number of iterations. */
    private static final int ITER_CNT = 200000;

    public static void main(String[] args) throws Exception {
        // Create sample object.
        SampleObject obj = createObject();

        // Run Java serialization test.
        javaSerialization(obj);

        // Run Kryo serialization test.
        kryoSerialization(obj);

        // Run GridGain serialization test.
        //gridGainSerialization(obj);
    }

    private static long javaSerialization(SampleObject obj) throws Exception {
        long avgDur = 0;

        for (int i = 0; i < RUN_CNT; i++) {
            SampleObject newObj = null;

            long start = System.currentTimeMillis();

            for (int j = 0; j < ITER_CNT; j++) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                ObjectOutputStream objOut = null;

                try {
                    objOut = new ObjectOutputStream(out);

                    objOut.writeObject(obj);
                }
                finally {
                   // U.close(objOut, null);
                }

                ObjectInputStream objIn = null;

                try {
                    objIn = new ObjectInputStream(
                            new ByteArrayInputStream(out.toByteArray()));

                    newObj = (SampleObject)objIn.readObject();
                }
                finally {

                }
            }

            long dur = System.currentTimeMillis() - start;

            avgDur += dur;
        }

        avgDur /= RUN_CNT;

        System.out.format("\n>>> Java serialization via Externalizable (average): %,d ms\n\n", avgDur);

        return avgDur;
    }

    private static long kryoSerialization(SampleObject obj) throws Exception {
        Kryo marsh = new Kryo();

        long avgDur = 0;

        for (int i = 0; i < RUN_CNT; i++) {
            SampleObject newObj = null;

            long start = System.currentTimeMillis();

            for (int j = 0; j < ITER_CNT; j++) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                Output kryoOut = null;

                try {
                    kryoOut = new Output(out);

                    marsh.writeObject(kryoOut, obj);
                }
                finally {

                }

                Input kryoIn = null;

                try {
                    kryoIn = new Input(new ByteArrayInputStream(out.toByteArray()));

                    newObj = marsh.readObject(kryoIn, SampleObject.class);
                }
                finally {

                }
            }

            long dur = System.currentTimeMillis() - start;

            avgDur += dur;
        }

        avgDur /= RUN_CNT;

        System.out.format("\n>>> Kryo serialization (average): %,d ms\n\n", avgDur);

        return avgDur;
    }



    private static SampleObject createObject() {
        long[] longArr = new long[3000];

        for (int i = 0; i < longArr.length; i++)
            longArr[i] = i;

        double[] dblArr = new double[3000];

        for (int i = 0; i < dblArr.length; i++)
            dblArr[i] = 0.1 * i;

        return new SampleObject(123, 123.456f, (short)321, longArr, dblArr);
    }

    private static class SampleObject
            implements Externalizable, KryoSerializable {
        private int intVal;
        private float floatVal;
        private Short shortVal;
        private long[] longArr;
        private double[] dblArr;
        private SampleObject selfRef;

        public SampleObject() {}

        SampleObject(int intVal, float floatVal, Short shortVal,
                     long[] longArr, double[] dblArr) {
            this.intVal = intVal;
            this.floatVal = floatVal;
            this.shortVal = shortVal;
            this.longArr = longArr;
            this.dblArr = dblArr;

            selfRef = this;
        }

        // Required by Java Externalizable.
        @Override public void writeExternal(ObjectOutput out)
                throws IOException {
            out.writeInt(intVal);
            out.writeFloat(floatVal);
            out.writeShort(shortVal);
            out.writeObject(longArr);
            out.writeObject(dblArr);
            out.writeObject(selfRef);
        }

        // Required by Java Externalizable.
        @Override public void readExternal(ObjectInput in)
                throws IOException, ClassNotFoundException {
            intVal = in.readInt();
            floatVal = in.readFloat();
            shortVal = in.readShort();
            longArr = (long[])in.readObject();
            dblArr = (double[])in.readObject();
            selfRef = (SampleObject)in.readObject();
        }

        // Required by Kryo serialization.
        @Override public void write(Kryo kryo, Output out) {
            kryo.writeObject(out, intVal);
            kryo.writeObject(out, floatVal);
            kryo.writeObject(out, shortVal);
            kryo.writeObject(out, longArr);
            kryo.writeObject(out, dblArr);
            kryo.writeObject(out, selfRef);
        }

        // Required by Kryo serialization.
        @Override public void read(Kryo kryo, Input in) {
            intVal = kryo.readObject(in, Integer.class);
            floatVal = kryo.readObject(in, Float.class);
            shortVal = kryo.readObject(in, Short.class);
            longArr = kryo.readObject(in, long[].class);
            dblArr = kryo.readObject(in, double[].class);
            selfRef = kryo.readObject(in, SampleObject.class);
        }
    }
}