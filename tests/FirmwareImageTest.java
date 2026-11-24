package dev.local.lunatv.receiver;
import java.io.*;
import java.nio.file.*;
public final class FirmwareImageTest {
    private static int checks;
    private static void rejected(byte[] bytes) throws Exception {
        checks++;
        try { FirmwareImage.readValidated(new ByteArrayInputStream(bytes)); throw new AssertionError("Accepted invalid firmware"); }
        catch (IOException expected) {}
    }
    public static void main(String[] args) throws Exception {
        rejected(new byte[0]); rejected(new byte[FirmwareImage.SIZE-1]);
        rejected(new byte[FirmwareImage.SIZE+1]); rejected(new byte[FirmwareImage.SIZE]);
        try { FirmwareImage.readValidated(null); throw new AssertionError("Accepted null"); }
        catch(IOException expected) {checks++;}
        if(args.length==1) {
            try(var in=Files.newInputStream(Path.of(args[0]))) {
                if(FirmwareImage.readValidated(in).length!=FirmwareImage.SIZE) throw new AssertionError();
                checks++;
            }
            byte[] changed=Files.readAllBytes(Path.of(args[0])); changed[100]^=1; rejected(changed);
        }
        System.out.println("Passed "+checks+" firmware validation checks.");
    }
}
