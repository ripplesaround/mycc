public class main {
    public static void main(String[] args) throws Exception {
        scan myscan = new scan();
        String filename  = "test/scanner_example.c";
        myscan.readfile(filename);
        System.out.println(args[0]);
    }
}
