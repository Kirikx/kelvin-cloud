package com.kirikomp.common;


import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Consumer;

import static java.nio.file.Files.*;


public class FileSendOptimizer
{

  private static final int CHUNK_SIZE = 4 * 1024 * 1024;


  public static void sendFile(Path path, Consumer<DataPackage> sendAction)
  throws Exception
  {
	if (!exists(path)) return;

	if (size(path) < CHUNK_SIZE * 8)
	  sendFull(path, sendAction);
	else
	  sendByChunks(path, sendAction);
  }


  private static void sendFull(Path path, Consumer<DataPackage> sendAction)
  throws Exception
  {
	DataPackage pack = new FileDataPackage(path);
	sendAction.accept(pack);
  }


  private static void sendByChunks(Path path, Consumer<DataPackage> sendAction)
  throws Exception
  {
	try (InputStream in = newInputStream(path))
	{
	  int availCount = in.available();
	  int rem = availCount % CHUNK_SIZE;

	  byte[] chunk = new byte[CHUNK_SIZE];
	  byte[] chunkLast = rem != 0 ?
						 new byte[rem] : new byte[CHUNK_SIZE];

	  int num = 1;
	  while (availCount > CHUNK_SIZE)
	  {
		in.read(chunk);
		DataPackage pack = new FileChunkPackage(path, chunk, num++);
		sendAction.accept(pack);
		availCount = in.available();
	  }

	  in.read(chunkLast);
	  DataPackage pack = new FileChunkPackage(path, chunkLast);
	  sendAction.accept(pack);
	}
  }

}