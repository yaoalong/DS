public class VMTest {
    private static final int _1MB = 1024 * 1024;

    public static void main(String[] args) throws InterruptedException {
        Thread.sleep(4000);
        byte[] allocation1;
        for (int i = 0; i < 400; i++) {
            allocation1 = new byte[_1MB];
            System.out.println("Create One" + i);
            Thread.sleep(1000);
        }

    }
}