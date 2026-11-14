package local.blackmamba.vpn;
import java.nio.file.*;import java.io.*;import java.util.*;
public final class CompatTest {
 static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  Path file=Files.createTempFile("vezdehod-compat-", ".txt");
  try{String unicode="VPN ✓ Привет\n世界 🌍";Compat.writeString(file,unicode);check(Compat.readString(file).equals(unicode),"UTF-8 corrupted");Compat.writeString(file,"short");check(Compat.readString(file).equals("short"),"File was not truncated");check(Arrays.equals(Compat.readAll(new ByteArrayInputStream(new byte[20000])),new byte[20000]),"Partial stream read");check(Compat.blank("")&&Compat.blank(" \t\n\u2003"),"Unicode whitespace rejected");check(!Compat.blank(" x ")&&!Compat.blank("🌍"),"Visible text treated as blank");check(Compat.set("one","two","one").size()==2,"Set semantics lost");check(Compat.repeat("ab",3).equals("ababab"),"Repeat corrupted");System.out.println("PASS compatible UTF-8 file I/O, truncation, stream reads and Unicode text handling");}finally{Files.delete(file);}
 }
}
