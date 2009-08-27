package experimentalcode.marisa.tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import experimentalcode.marisa.index.xtree.util.LargeProperties;

public class LPTests {

  public static void main(String[] args) throws IOException, ClassNotFoundException {
    System.out.println("100000000 = " + 256 + "; << 8: " + (256 << 8));
    System.out.println("11111111 = " + 255 + "; << 8: " + (255 << 8));
    System.out.println("(11111111 << 8) & (1011010100000000) = " + ((255 << 8) & 46336) + "; 11111111 & 1011010100000000 = " + (255 & 46336));
    System.out.println("11111111 & (1011010100000000 >> 8) = " + (255 & (46336 >> 8)) + "; 10110101 = " + 181);
    System.out.println("11111111 & (10110101 << 0) = " + (255 & (181 << 0)) + "; 11111111 & (10110101 >> 0) = " + (255 & (181 >> 0)));

    LargeProperties lps = new LargeProperties(8);
    lps.setProperty(0);
    lps.setProperty(2);
    lps.setProperty(4);
    lps.setProperty(5);
    lps.setProperty(7);
    System.out.println(lps.toString());

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    lps.writeExternal(oos);
    oos.close();
    baos.close();
    byte[] array = baos.toByteArray();
    System.out.println("read in " + array.length + " bytes: " + java.util.Arrays.toString(array));
    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(array));

    LargeProperties lp2 = LargeProperties.readExternal(ois);
    System.out.println("read in lp2: " + lp2.toString());

    lps = new LargeProperties(130);
    lps.setProperty(0);
    lps.setProperty(2);
    lps.setProperty(4);
    lps.setProperty(5);
    lps.setProperty(7);
    lps.setProperty(8);
    lps.setProperty(127);
    lps.setProperty(128);
    System.out.println(lps.toString());

    baos = new ByteArrayOutputStream();
    oos = new ObjectOutputStream(baos);
    lps.writeExternal(oos);
    oos.close();
    baos.close();
    array = baos.toByteArray();
    System.out.println("read in " + array.length + " bytes");
    ois = new ObjectInputStream(new ByteArrayInputStream(array));

    lp2 = LargeProperties.readExternal(ois);
    System.out.println("read in lp2:\n" + lp2.toString());
    System.out.println("contains 0 = " + lp2.hasProperty(0) + ", 1 = " + lp2.hasProperty(1) + ", 2 = " + lp2.hasProperty(2) + ", 128 = " + lp2.hasProperty(128));

    lps = new LargeProperties(70);
    lps.setProperty(0);
    lps.setProperty(2);
    lps.setProperty(4);
    lps.setProperty(5);
    lps.setProperty(7);
    lps.setProperty(67);
    System.out.println(lps.toString());

    baos = new ByteArrayOutputStream();
    oos = new ObjectOutputStream(baos);
    lps.writeExternal(oos);
    oos.close();
    baos.close();
    array = baos.toByteArray();
    System.out.println("read in " + array.length + " bytes");
    ois = new ObjectInputStream(new ByteArrayInputStream(array));

    lp2 = LargeProperties.readExternal(ois);
    System.out.println("read in lp2:\n" + lp2.toString());
    System.out.println("contains 0 = " + lp2.hasProperty(0) + ", 1 = " + lp2.hasProperty(1) + ", 2 = " + lp2.hasProperty(2) + ", 67 = " + lp2.hasProperty(67));

    lps = new LargeProperties(70);
    System.out.println("leer: " + lps.toString());

    System.out.println("1<<-1=" + (((long) 1) << -1) + ", 1<<-2=" + (((long) 1) << -2) + ", 1<<0=" + (((long) 1) << 0) + ", 1<<1=" + (((long) 1) << 1));
    System.out.println("1<<64=" + (((long) 1) << 64) + ", 1<<63=" + (((long) 1) << 63) + ", 1<<62=" + (((long) 1) << 62));

    System.out.println("~0=" + (~(long) 0) + "; ~255=" + ((byte) ~255) + "; 0<<1=" + ((long) 0 << 1) + "; 0<<-1=" + ((long) 0 << -1) + "; 0<<8=" + ((long) 0 << 8) + "; 0<<7=" + ((long) 0 << 7));

    System.out.println("~0=" + (~(byte) 0) + "; ~255=" + ((byte) ~255));

    byte b = -0;
    System.out.println("~b = " + (~b) + "; 0 & (1<<7) = " + ((byte) 0 & ((byte) 1 << 7)) + "; 0 & (1<<8) = " + ((byte) 0 & ((byte) 1 << 8)) + "; 0 & (1<<-1) = " + ((byte) 0 & ((byte) 1 << -1)));
    System.out.println("~b = " + (~b) + "; 0 & ~(1<<7) = " + ((byte) 0 & ~((byte) 1 << 7)) + "; 0 & ~(1<<8) = " + ((byte) 0 & ~((byte) 1 << 8)) + "; 0 & ~(1<<-1) = " + ((byte) 0 & ~((byte) 1 << -1)));
    byte b2 = 0;

    System.out.println("-0 | 1 = " + (b | 1) + "; 0 | 1 = " + (b2 | 1));
    for(int i = 0; i < 8; i++) {
      b |= (byte) (1 << i);
    }
    System.out.println("full ? " + (byte) b);
    for(int i = 0; i < 8; i++) {
      b &= ~(byte) (1 << i);
    }
    System.out.println("empty ? " + b);

    System.out.println((byte) -1 == (byte) 255);

    System.out.println("0xFF=" + 0xFF + ", Long.max=" + Long.MAX_VALUE + ", Int.max=" + Integer.MAX_VALUE + ", Byte.max=" + Byte.MAX_VALUE + ", 0xFFFF=" + 0xFFFF + "; 0xFFFFFFFF=" + 0xFFFFFFFFL + "; 0xFFFFFFFFFFFFFFFFL=" + 0xFFFFFFFFFFFFFFFFL);
    System.out.println("Long.max.hex=" + Long.toHexString(Long.MAX_VALUE) + ", Int.max.hex=" + Long.toHexString(Integer.MAX_VALUE) + ", ");

    long l = 1756301203402L;
    System.out.println("Transforming " + l);
    byte[] bs = new byte[8];
    for(int i = 0; i < 8; i++) {
      System.out.println((byte) (l >>> (i * 8)));
      bs[7 - i] = (byte) (l >>> (i * 8));
    }
    byte[] bs2 = new byte[8];
    for(int i = 0; i < 8; i++) {
      System.out.println((byte) (l >>> (8 * (7 - i))));
      bs2[i] = (byte) (l >>> ((7 - i) * 8));
    }
    System.out.println("bs=" + java.util.Arrays.toString(bs) + "; oder " + l);
    System.out.println("bs2=" + java.util.Arrays.toString(bs2) + "; oder " + l);
    long lc = 0x0L;
    System.out.println("init: " + lc);
    lc = ((bs[7] & 0xFFL) << 0);
    System.out.println("first: " + lc);
    long lc2 = (((long) bs2[0]) << 56);
    System.out.println("first2: " + lc2);
    for(int i = 1; i < 7; i++) {
      lc += ((bs[7 - i] & 0xFFL) << (i * 8));
      lc2 += ((bs2[i] & 0xFFL) << ((7 - i) * 8));
    }
    lc += (((long) bs[0]) << 56);
    lc2 += ((bs2[7] & 0xFFL) << 0);
    System.out.println("lc=" + lc);
    System.out.println("lc2=" + lc2);
    
    bs2 = new byte[8];
    for(int i = 0; i < 8; i++) {
      bs2[i] = (byte) (l >>> (i * 8));
    }
    System.out.println("bs3=" + java.util.Arrays.toString(bs2) + "; oder " + l);
    long lc3 = 0x0L;
    for(int i = 0; i < 7; i++) {
      lc3 += ((bs2[i] & 0xFFL) << (i * 8));
    }
    lc3 += (((long) bs2[7]) << 56);
    System.out.println("lc3=" + lc3);
    
  }
}
