package com.kirikomp.common;


import java.nio.file.Path;


public class FileChunkPackage
		extends DataPackage
{

  private final String filename;
  private final byte[] data;
  private final int num;
  private final boolean last;


  public FileChunkPackage(Path path, byte[] chunk, int num)
  {
	filename = path.getFileName().toString();
	data = chunk;
	this.num = num;
	last = false;
  }


  public FileChunkPackage(Path path, byte[] chunk)
  {
	filename = path.getFileName().toString();
	data = chunk;
	this.num = -1;
	last = true;
  }


  public String getFilename()
  {
	return filename;
  }


  public int getNum()
  {
	return num;
  }


  public byte[] getData()
  {
	return data;
  }


  public boolean isLast()
  {
	return last;
  }


  public boolean isFirst()
  {
	return num == 1;
  }

}