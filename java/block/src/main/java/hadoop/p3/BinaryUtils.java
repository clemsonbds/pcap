package hadoop.p3;

import java.nio.ByteOrder;



public class BinaryUtils
{
  public BinaryUtils() {}
  
  public static byte[] flip(byte[] bytes, int length)
  {
    byte[] tmp = new byte[length];
    for (int i = 0; i < length; i++) {
      tmp[i] = bytes[(length - 1 - i)];
    }
    return tmp;
  }
  





  public static byte[] flipBO(byte[] bytes, int length)
  {
    if (ByteOrder.nativeOrder().toString().equals("BIG_ENDIAN")) {
      return bytes;
    }
    byte[] tmp = new byte[length];
    for (int i = 0; i < length; i++) {
      tmp[i] = bytes[(length - 1 - i)];
    }
    return tmp;
  }
  





  public static byte[] flipBO(byte[] bytes)
  {
    return flipBO(bytes, bytes.length);
  }
  





  public static int byteToInt(byte[] b)
  {
    int dataLen = b.length;
    int idx = dataLen;
    int val;
    int sum;
    int bit_pos; int out = bit_pos = sum = val = 0;
    do {
      while (bit_pos < 8) {
        out = b[idx] >> bit_pos & 0x1;
        sum |= out << bit_pos;
        bit_pos++;
      }
      val |= sum << (dataLen - 1 - idx) * 8;
      sum = bit_pos = 0;idx--;
    } while (idx >= 0);
    







    return val;
  }
  




  public static int byteToInt(byte[] b, int len)
  {
    int dataLen = len;
    int idx = dataLen;
    int val;
    int sum;
    int bit_pos; int out = bit_pos = sum = val = 0;
    do {
      while (bit_pos < 8) {
        out = b[idx] >> bit_pos & 0x1;
        sum |= out << bit_pos;
        bit_pos++;
      }
      val |= sum << (dataLen - 1 - idx) * 8;
      sum = bit_pos = 0;idx--;
    } while (idx >= 0);
    







    return val;
  }
  




  public static long ubyteToLong(byte[] b)
  {
    int i = 0;
    long val = 0L;
    
    while (i < b.length) {
      val |= (b[i] & 0xFF) << (b.length - i - 1) * 8;
      i++;
    }
    return val;
  }
  




  public static long ubyteToLong(byte[] b, int len)
  {
    byte[] newb = new byte[len];
    System.arraycopy(b, 0, newb, 0, newb.length);
    return ubyteToLong(newb);
  }
  





  public static long byteToLong(byte[] b)
  {
    if (b.length < 8) {
      return byteToLong(b, b.length);
    }
    int i = 0;
    long f = -72057594037927936L;
    long val = 0L;
    
    while (i < b.length) {
      val |= b[i] << (b.length - i) * 8 & f >> i * 8;
      i++;
    }
    return val;
  }
  




  public static long byteToLong(byte[] b, int len)
  {
    int dataLen = len;
    int idx = dataLen;
    
    int sum;
    int bit_pos;
    int out = bit_pos = sum = 0;
    long val = 0L;
    do {
      while (bit_pos < 8) {
        out = b[idx] >> bit_pos & 0x1;
        sum |= out << bit_pos;
        bit_pos++;
      }
      val |= sum << (dataLen - 1 - idx) * 8;
      sum = bit_pos = 0;idx--;
    } while (idx >= 0);
    







    return val;
  }
  





  public static byte[] LongToBytes(long lval)
  {
    byte[] bytes = new byte[8];
    int i = 0;
    long f = 255L;
    
    while (i < bytes.length) {
      bytes[i] = ((byte)(int)(lval >> (bytes.length - 1 - i) * 8 & f));
      i++;
    }
    return bytes;
  }
  
  public static byte[] IntToBytes(int val)
  {
    byte[] bytes = new byte[4];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = ((byte)(val >> (bytes.length - 1 - i) * 8 & 0xFF));
    }
    return bytes;
  }
  
  public static byte[] uIntToBytes(long val) {
    byte[] bytes = new byte[4];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = ((byte)(int)(val >> (bytes.length - 1 - i) * 8 & 0xFF));
    }
    return bytes;
  }
}
