package hadoop.p3;



public class Bytes
{
  public Bytes() {}
  

  public static int toInt(byte[] src, int srcPos)
  {
    int dword = 0;
    for (int i = 0; i < src.length - srcPos; i++) {
      dword = (dword << 8) + (src[(i + srcPos)] & 0x7F);
      if ((src[(i + srcPos)] & 0x80) == 128)
        dword += 128;
    }
    return dword;
  }
  




  public static int toInt(byte[] src)
  {
    return toInt(src, 0);
  }
  




  public static int toInt(byte src)
  {
    byte[] b = new byte[1];
    b[0] = src;
    return toInt(b, 0);
  }
  





  public static long toLong(byte[] src, int srcPos)
  {
    long dword = 0L;
    for (int i = 0; i < src.length - srcPos; i++) {
      dword = (dword << 8) + (src[(i + srcPos)] & 0x7F);
      if ((src[(i + srcPos)] & 0x80) == 128)
        dword += 128L;
    }
    return dword;
  }
  




  public static long toLong(byte[] src)
  {
    return toLong(src, 0);
  }
}
