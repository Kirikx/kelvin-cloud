package com.kirikomp.common;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class FileDataPackage
		extends DataPackage
{

  protected final String filename;
  protected final byte[] data;


  public FileDataPackage(Path path)
  throws IOException
  {
	filename = path.getFileName().toString();
	data = Files.readAllBytes(path);
  }


  public String getFilename()
  {
	return filename;
  }


  public byte[] getData()
  {
	return data;
  }

}